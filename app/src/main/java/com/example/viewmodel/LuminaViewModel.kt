package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.LuminaDatabase
import com.example.data.model.BookmarkEntity
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentType
import com.example.data.model.PdfAnnotationEntity
import com.example.data.model.SortOrder
import com.example.data.prefs.AppPreferences
import com.example.data.prefs.UserSettings
import com.example.data.repository.DocumentRepository
import com.example.engine.DocBlock
import com.example.engine.DocBlockStyle
import com.example.engine.PdfEngine
import com.example.engine.PdfTextPage
import com.example.engine.PresentationEngine
import com.example.engine.RichDocument
import com.example.engine.SheetData
import com.example.engine.SlideDeck
import com.example.engine.SlideItem
import com.example.engine.SpreadsheetEngine
import com.example.engine.TextDocumentEngine
import com.example.engine.WordDocumentEngine
import com.example.engine.WorkbookData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ActiveEditorState {
    data object None : ActiveEditorState
    data class Loading(val document: DocumentEntity) : ActiveEditorState

    data class PdfEditor(
        val document: DocumentEntity,
        val pageCount: Int,
        val currentPage: Int = 0,
        val nightInvert: Boolean = false,
        val annotations: List<PdfAnnotationEntity> = emptyList(),
        val bookmarks: List<BookmarkEntity> = emptyList(),
        val pendingPages: List<PdfTextPage> = emptyList(),
        val renderVersion: Int = 0,
    ) : ActiveEditorState

    data class SpreadsheetEditor(
        val document: DocumentEntity,
        val workbook: WorkbookData,
        val activeSheetIndex: Int = 0,
        val selectedRow: Int = 0,
        val selectedCol: Int = 0,
        val hasUnsavedChanges: Boolean = false,
    ) : ActiveEditorState

    data class WordEditor(
        val document: DocumentEntity,
        val richDocument: RichDocument,
        val selectedBlockId: String? = null,
        val hasUnsavedChanges: Boolean = false,
    ) : ActiveEditorState

    data class SlidesEditor(
        val document: DocumentEntity,
        val deck: SlideDeck,
        val activeSlideIndex: Int = 0,
        val hasUnsavedChanges: Boolean = false,
    ) : ActiveEditorState

    data class TextEditor(
        val document: DocumentEntity,
        val content: String,
        val previewMode: Boolean = false,
        val hasUnsavedChanges: Boolean = false,
    ) : ActiveEditorState
}

data class LibraryFilterState(
    val query: String = "",
    val category: DocumentCategory = DocumentCategory.ALL,
    val sortOrder: SortOrder = SortOrder.RECENT,
    val favoritesOnly: Boolean = false,
)

class LuminaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LuminaDatabase.getInstance(application)
    val prefs = AppPreferences(application)
    val repository = DocumentRepository(application, db, prefs)

    val settings: StateFlow<UserSettings> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings(),
    )

    private val _filterState = MutableStateFlow(LibraryFilterState())
    val filterState: StateFlow<LibraryFilterState> = _filterState.asStateFlow()

    val allDocuments: StateFlow<List<DocumentEntity>> = repository.documentsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val filteredDocuments: StateFlow<List<DocumentEntity>> = combine(
        allDocuments,
        _filterState,
    ) { docs, filter ->
        val filtered = docs.filter { doc ->
            val matchesQuery = filter.query.isBlank() ||
                doc.title.contains(filter.query, ignoreCase = true) ||
                doc.fileName.contains(filter.query, ignoreCase = true) ||
                doc.previewSnippet.contains(filter.query, ignoreCase = true)
            val matchesCategory = filter.category == DocumentCategory.ALL ||
                doc.docType.category == filter.category
            val matchesFav = !filter.favoritesOnly || doc.isFavorite
            matchesQuery && matchesCategory && matchesFav
        }
        when (filter.sortOrder) {
            SortOrder.RECENT -> filtered.sortedWith(compareByDescending<DocumentEntity> { it.isPinned }.thenByDescending { it.lastOpenedAt })
            SortOrder.NAME -> filtered.sortedWith(compareByDescending<DocumentEntity> { it.isPinned }.thenBy { it.title.lowercase() })
            SortOrder.SIZE -> filtered.sortedWith(compareByDescending<DocumentEntity> { it.isPinned }.thenByDescending { it.sizeBytes })
            SortOrder.TYPE -> filtered.sortedWith(compareByDescending<DocumentEntity> { it.isPinned }.thenBy { it.type })
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val _editorState = MutableStateFlow<ActiveEditorState>(ActiveEditorState.None)
    val editorState: StateFlow<ActiveEditorState> = _editorState.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var pdfObserverJob: Job? = null

    init {
        viewModelScope.launch {
            refreshCacheSize()
        }
    }

    fun emitMessage(msg: String) {
        _messages.tryEmit(msg)
    }

    // --- Library Filter Controls ---
    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun setCategory(category: DocumentCategory) {
        _filterState.update { it.copy(category = category) }
    }

    fun setSortOrder(order: SortOrder) {
        _filterState.update { it.copy(sortOrder = order) }
    }

    fun toggleFavoritesFilter() {
        _filterState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    // --- Document Lifecycle ---
    fun createBlankAndOpen(title: String, type: DocumentType) {
        viewModelScope.launch {
            _isBusy.value = true
            val created = repository.createBlankDocument(title, type)
            _isBusy.value = false
            emitMessage("Created ${created.title}")
            openDocument(created)
        }
    }

    fun importFromUri(uri: Uri, openImmediately: Boolean = true) {
        viewModelScope.launch {
            _isBusy.value = true
            val imported = repository.importFromUri(uri)
            _isBusy.value = false
            if (imported != null) {
                emitMessage("Opened ${imported.title} (${imported.docType.shortBadge})")
                if (openImmediately) openDocument(imported)
            } else {
                emitMessage("Could not read selected file")
            }
        }
    }

    fun importMultipleUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            importFromUri(uris.first(), openImmediately = true)
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            var count = 0
            for (uri in uris) {
                if (repository.importFromUri(uri) != null) count++
            }
            _isBusy.value = false
            emitMessage("Imported $count of ${uris.size} real file(s) into workspace")
        }
    }

    fun openDocument(doc: DocumentEntity) {
        pdfObserverJob?.cancel()
        _editorState.value = ActiveEditorState.Loading(doc)
        viewModelScope.launch {
            repository.touchOpened(doc.id)
            val file = File(doc.filePath)
            if (!file.exists()) {
                _editorState.value = ActiveEditorState.None
                emitMessage("Document file no longer exists")
                return@launch
            }
            when (doc.docType) {
                DocumentType.PDF -> {
                    val count = withContext(Dispatchers.IO) { PdfEngine.getPageCount(file) }.coerceAtLeast(1)
                    _editorState.value = ActiveEditorState.PdfEditor(
                        document = doc,
                        pageCount = count,
                        currentPage = 0,
                        nightInvert = settings.value.pdfNightInvertDefault,
                    )
                    pdfObserverJob = viewModelScope.launch {
                        combine(
                            repository.observeAnnotations(doc.id),
                            repository.observeBookmarks(doc.id),
                        ) { anns, bms -> anns to bms }.collect { (anns, bms) ->
                            _editorState.update { state ->
                                if (state is ActiveEditorState.PdfEditor && state.document.id == doc.id) {
                                    state.copy(annotations = anns, bookmarks = bms)
                                } else {
                                    state
                                }
                            }
                        }
                    }
                }
                DocumentType.XLSX, DocumentType.CSV -> {
                    val wb = withContext(Dispatchers.IO) {
                        if (doc.docType == DocumentType.XLSX) {
                            SpreadsheetEngine.readXlsx(file)
                        } else {
                            SpreadsheetEngine.readCsv(file)
                        }
                    }
                    _editorState.value = ActiveEditorState.SpreadsheetEditor(
                        document = doc,
                        workbook = wb,
                    )
                }
                DocumentType.DOCX -> {
                    val rich = withContext(Dispatchers.IO) { WordDocumentEngine.readDocx(file) }
                    _editorState.value = ActiveEditorState.WordEditor(
                        document = doc,
                        richDocument = rich,
                        selectedBlockId = rich.blocks.firstOrNull()?.id,
                    )
                }
                DocumentType.PPTX -> {
                    val deck = withContext(Dispatchers.IO) { PresentationEngine.readPptx(file) }
                    _editorState.value = ActiveEditorState.SlidesEditor(
                        document = doc,
                        deck = deck,
                    )
                }
                DocumentType.MARKDOWN, DocumentType.TEXT, DocumentType.JSON, DocumentType.HTML -> {
                    val text = withContext(Dispatchers.IO) { TextDocumentEngine.readText(file) }
                    _editorState.value = ActiveEditorState.TextEditor(
                        document = doc,
                        content = text,
                        previewMode = false,
                    )
                }
            }
        }
    }

    /**
     * Closes the active editor. If Auto-Save is enabled in Settings and there are unsaved changes,
     * persists edits to disk (and syncs back to sourceUri if writable) before closing.
     */
    fun closeEditor() {
        val current = _editorState.value
        val autoSave = settings.value.autoSaveEnabled
        pdfObserverJob?.cancel()

        if (autoSave) {
            when (current) {
                is ActiveEditorState.SpreadsheetEditor -> if (current.hasUnsavedChanges) saveSpreadsheetInternal(current, silent = true)
                is ActiveEditorState.WordEditor -> if (current.hasUnsavedChanges) saveWordInternal(current, silent = true)
                is ActiveEditorState.SlidesEditor -> if (current.hasUnsavedChanges) saveSlidesInternal(current, silent = true)
                is ActiveEditorState.TextEditor -> if (current.hasUnsavedChanges) saveTextInternal(current, silent = true)
                else -> Unit
            }
        }
        _editorState.value = ActiveEditorState.None
    }

    // --- PDF Editor Actions ---
    fun setPdfPage(index: Int) {
        _editorState.update { state ->
            if (state is ActiveEditorState.PdfEditor) {
                state.copy(currentPage = index.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0)))
            } else state
        }
    }

    fun togglePdfNightInvert() {
        _editorState.update { state ->
            if (state is ActiveEditorState.PdfEditor) state.copy(nightInvert = !state.nightInvert) else state
        }
    }

    fun addPdfAnnotation(kind: String, content: String, normX: Float, normY: Float, colorHex: Long) {
        val state = _editorState.value as? ActiveEditorState.PdfEditor ?: return
        viewModelScope.launch {
            repository.addPdfAnnotation(
                PdfAnnotationEntity(
                    documentId = state.document.id,
                    pageIndex = state.currentPage,
                    kind = kind,
                    content = content.trim(),
                    normX = normX.coerceIn(0.06f, 0.86f),
                    normY = normY.coerceIn(0.06f, 0.92f),
                    colorHex = colorHex,
                )
            )
            emitMessage("Added $kind overlay on Page ${state.currentPage + 1}")
        }
    }

    fun deletePdfAnnotation(annotationId: Long) {
        viewModelScope.launch { repository.deletePdfAnnotation(annotationId) }
    }

    fun togglePdfPageBookmark() {
        val state = _editorState.value as? ActiveEditorState.PdfEditor ?: return
        viewModelScope.launch {
            val existing = state.bookmarks.firstOrNull { it.targetIndex == state.currentPage }
            if (existing != null) {
                repository.deleteBookmark(existing.id)
                emitMessage("Removed bookmark for Page ${state.currentPage + 1}")
            } else {
                repository.addBookmark(state.document.id, state.currentPage, "Page ${state.currentPage + 1}")
                emitMessage("Bookmarked Page ${state.currentPage + 1}")
            }
        }
    }

    fun appendNewPdfPage(title: String, body: String) {
        _editorState.update { state ->
            if (state is ActiveEditorState.PdfEditor) {
                state.copy(pendingPages = state.pendingPages + PdfTextPage(title.ifBlank { "New Page" }, body))
            } else state
        }
        bakeAndSavePdf()
    }

    fun deleteCurrentPdfPage() {
        val state = _editorState.value as? ActiveEditorState.PdfEditor ?: return
        if (state.pageCount <= 1) {
            emitMessage("Cannot delete the only page in a PDF")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val file = File(state.document.filePath)
            val remaining = withContext(Dispatchers.IO) {
                PdfEngine.deletePageFromPdf(file, state.currentPage, file)
            }.coerceAtLeast(1)
            val synced = repository.updateDocumentAfterSave(
                doc = state.document,
                pageCount = remaining,
                previewSnippet = "$remaining page PDF",
            )
            _editorState.update { current ->
                if (current is ActiveEditorState.PdfEditor) {
                    current.copy(
                        pageCount = remaining,
                        currentPage = current.currentPage.coerceAtMost(remaining - 1),
                        renderVersion = current.renderVersion + 1,
                    )
                } else current
            }
            _isBusy.value = false
            emitMessage(if (synced) "Page deleted & synced to device file" else "Page deleted ($remaining pages left)")
        }
    }

    fun bakeAndSavePdf() {
        val state = _editorState.value as? ActiveEditorState.PdfEditor ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val file = File(state.document.filePath)
            withContext(Dispatchers.IO) {
                PdfEngine.bakeAnnotationsAndPages(
                    sourceFile = file,
                    annotations = state.annotations,
                    appendedPages = state.pendingPages,
                    outputFile = file,
                )
            }
            repository.clearAnnotationsForDocument(state.document.id)
            val newPageCount = withContext(Dispatchers.IO) { PdfEngine.getPageCount(file) }.coerceAtLeast(1)
            val synced = repository.updateDocumentAfterSave(
                doc = state.document,
                pageCount = newPageCount,
                previewSnippet = "$newPageCount page PDF",
            )
            _editorState.update { current ->
                if (current is ActiveEditorState.PdfEditor) {
                    current.copy(
                        pageCount = newPageCount,
                        pendingPages = emptyList(),
                        renderVersion = current.renderVersion + 1,
                    )
                } else current
            }
            _isBusy.value = false
            emitMessage(if (synced) "PDF saved & synced to original file ($newPageCount pages)" else "PDF saved ($newPageCount pages)")
        }
    }

    // --- Spreadsheet Editor Actions ---
    fun selectSpreadsheetCell(row: Int, col: Int) {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                state.copy(selectedRow = row, selectedCol = col)
            } else state
        }
    }

    fun updateSpreadsheetCell(row: Int, col: Int, value: String) {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                val sheets = state.workbook.sheets.toMutableList()
                if (state.activeSheetIndex in sheets.indices) {
                    sheets[state.activeSheetIndex] = sheets[state.activeSheetIndex].withCell(row, col, value)
                }
                state.copy(
                    workbook = WorkbookData(sheets),
                    hasUnsavedChanges = true,
                )
            } else state
        }
    }

    fun selectSpreadsheetSheet(index: Int) {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                state.copy(
                    activeSheetIndex = index.coerceIn(0, (state.workbook.sheets.size - 1).coerceAtLeast(0)),
                    selectedRow = 0,
                    selectedCol = 0,
                )
            } else state
        }
    }

    fun addSpreadsheetSheet(name: String) {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                val sheetName = name.trim().ifBlank { "Sheet ${state.workbook.sheets.size + 1}" }
                val newSheet = SheetData(sheetName, List(20) { List(8) { "" } })
                val updated = state.workbook.sheets + newSheet
                state.copy(
                    workbook = WorkbookData(updated),
                    activeSheetIndex = updated.lastIndex,
                    selectedRow = 0,
                    selectedCol = 0,
                    hasUnsavedChanges = true,
                )
            } else state
        }
    }

    fun addSpreadsheetRow() {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                val sheets = state.workbook.sheets.toMutableList()
                sheets[state.activeSheetIndex] = sheets[state.activeSheetIndex].withAddedRow()
                state.copy(workbook = WorkbookData(sheets), hasUnsavedChanges = true)
            } else state
        }
    }

    fun addSpreadsheetColumn() {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                val sheets = state.workbook.sheets.toMutableList()
                sheets[state.activeSheetIndex] = sheets[state.activeSheetIndex].withAddedColumn()
                state.copy(workbook = WorkbookData(sheets), hasUnsavedChanges = true)
            } else state
        }
    }

    fun deleteSelectedSpreadsheetRow() {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                val sheets = state.workbook.sheets.toMutableList()
                val updated = sheets[state.activeSheetIndex].withDeletedRow(state.selectedRow)
                sheets[state.activeSheetIndex] = updated
                state.copy(
                    workbook = WorkbookData(sheets),
                    selectedRow = state.selectedRow.coerceAtMost((updated.rowCount - 1).coerceAtLeast(0)),
                    hasUnsavedChanges = true,
                )
            } else state
        }
    }

    fun deleteSelectedSpreadsheetColumn() {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                val sheets = state.workbook.sheets.toMutableList()
                val updated = sheets[state.activeSheetIndex].withDeletedColumn(state.selectedCol)
                sheets[state.activeSheetIndex] = updated
                state.copy(
                    workbook = WorkbookData(sheets),
                    selectedCol = state.selectedCol.coerceAtMost((updated.colCount - 1).coerceAtLeast(0)),
                    hasUnsavedChanges = true,
                )
            } else state
        }
    }

    fun sortSpreadsheetBySelectedColumn(ascending: Boolean = true) {
        _editorState.update { state ->
            if (state is ActiveEditorState.SpreadsheetEditor) {
                val sheets = state.workbook.sheets.toMutableList()
                val sheet = sheets[state.activeSheetIndex]
                if (sheet.rows.size > 2) {
                    val header = sheet.rows.first()
                    val body = sheet.rows.drop(1)
                    val nonEmpty = body.filter { row -> row.any { it.isNotBlank() } }
                    val empty = body.filter { row -> row.all { it.isBlank() } }
                    val col = state.selectedCol
                    val sortedNonEmpty = if (ascending) {
                        nonEmpty.sortedBy { it.getOrNull(col)?.lowercase() ?: "" }
                    } else {
                        nonEmpty.sortedByDescending { it.getOrNull(col)?.lowercase() ?: "" }
                    }
                    sheets[state.activeSheetIndex] = sheet.copy(rows = listOf(header) + sortedNonEmpty + empty)
                }
                state.copy(workbook = WorkbookData(sheets), hasUnsavedChanges = true)
            } else state
        }
    }

    fun saveSpreadsheet() {
        val state = _editorState.value as? ActiveEditorState.SpreadsheetEditor ?: return
        saveSpreadsheetInternal(state, silent = false)
    }

    private fun saveSpreadsheetInternal(state: ActiveEditorState.SpreadsheetEditor, silent: Boolean) {
        viewModelScope.launch {
            val file = File(state.document.filePath)
            withContext(Dispatchers.IO) {
                if (state.document.docType == DocumentType.XLSX) {
                    SpreadsheetEngine.writeXlsx(state.workbook, file)
                } else {
                    SpreadsheetEngine.writeCsv(state.workbook, file)
                }
            }
            val synced = repository.updateDocumentAfterSave(
                doc = state.document,
                pageCount = state.workbook.sheets.size,
                wordCount = state.workbook.sheets.sumOf { it.rowCount },
                previewSnippet = "${state.workbook.sheets.size} sheets",
            )
            _editorState.update {
                if (it is ActiveEditorState.SpreadsheetEditor) it.copy(hasUnsavedChanges = false) else it
            }
            if (!silent) {
                emitMessage(if (synced) "Saved & synced ${state.document.title}" else "Saved ${state.document.title}")
            }
        }
    }

    // --- Word Editor Actions ---
    fun updateWordBlock(blockId: String, transform: (DocBlock) -> DocBlock) {
        _editorState.update { state ->
            if (state is ActiveEditorState.WordEditor) {
                val updated = state.richDocument.blocks.map { if (it.id == blockId) transform(it) else it }
                state.copy(richDocument = RichDocument(updated), hasUnsavedChanges = true)
            } else state
        }
    }

    fun selectWordBlock(blockId: String) {
        _editorState.update { state ->
            if (state is ActiveEditorState.WordEditor) state.copy(selectedBlockId = blockId) else state
        }
    }

    fun addWordBlock(style: DocBlockStyle = DocBlockStyle.BODY) {
        _editorState.update { state ->
            if (state is ActiveEditorState.WordEditor) {
                val newBlock = DocBlock(
                    style = style,
                    text = "",
                    isBold = style in listOf(DocBlockStyle.HEADING1, DocBlockStyle.HEADING2, DocBlockStyle.HEADING3),
                    isItalic = style == DocBlockStyle.QUOTE,
                )
                val blocks = state.richDocument.blocks.toMutableList()
                val idx = blocks.indexOfFirst { it.id == state.selectedBlockId }
                if (idx >= 0) blocks.add(idx + 1, newBlock) else blocks.add(newBlock)
                state.copy(
                    richDocument = RichDocument(blocks),
                    selectedBlockId = newBlock.id,
                    hasUnsavedChanges = true,
                )
            } else state
        }
    }

    fun moveWordBlock(blockId: String, moveUp: Boolean) {
        _editorState.update { state ->
            if (state is ActiveEditorState.WordEditor) {
                val blocks = state.richDocument.blocks.toMutableList()
                val idx = blocks.indexOfFirst { it.id == blockId }
                val targetIdx = if (moveUp) idx - 1 else idx + 1
                if (idx in blocks.indices && targetIdx in blocks.indices) {
                    val item = blocks.removeAt(idx)
                    blocks.add(targetIdx, item)
                    state.copy(richDocument = RichDocument(blocks), hasUnsavedChanges = true)
                } else state
            } else state
        }
    }

    fun deleteWordBlock(blockId: String) {
        _editorState.update { state ->
            if (state is ActiveEditorState.WordEditor && state.richDocument.blocks.size > 1) {
                val filtered = state.richDocument.blocks.filterNot { it.id == blockId }
                state.copy(
                    richDocument = RichDocument(filtered),
                    selectedBlockId = filtered.firstOrNull()?.id,
                    hasUnsavedChanges = true,
                )
            } else state
        }
    }

    fun replaceTextInWord(find: String, replace: String) {
        if (find.isEmpty()) return
        var count = 0
        _editorState.update { state ->
            if (state is ActiveEditorState.WordEditor) {
                val updated = state.richDocument.blocks.map { block ->
                    if (block.text.contains(find, ignoreCase = true)) {
                        count++
                        block.copy(text = block.text.replace(find, replace, ignoreCase = true))
                    } else block
                }
                state.copy(richDocument = RichDocument(updated), hasUnsavedChanges = true)
            } else state
        }
        emitMessage("Replaced in $count block(s)")
    }

    fun saveWordDocument() {
        val state = _editorState.value as? ActiveEditorState.WordEditor ?: return
        saveWordInternal(state, silent = false)
    }

    private fun saveWordInternal(state: ActiveEditorState.WordEditor, silent: Boolean) {
        viewModelScope.launch {
            val file = File(state.document.filePath)
            withContext(Dispatchers.IO) {
                WordDocumentEngine.writeDocx(state.richDocument, file)
            }
            val snippet = state.richDocument.blocks.firstOrNull { it.text.isNotBlank() }?.text?.take(80) ?: "Word document"
            val synced = repository.updateDocumentAfterSave(
                doc = state.document,
                wordCount = state.richDocument.wordCount,
                previewSnippet = snippet,
            )
            _editorState.update {
                if (it is ActiveEditorState.WordEditor) it.copy(hasUnsavedChanges = false) else it
            }
            if (!silent) {
                emitMessage(if (synced) "Saved & synced ${state.document.title}.docx" else "Saved ${state.document.title}.docx")
            }
        }
    }

    // --- Slides Editor Actions ---
    fun selectSlide(index: Int) {
        _editorState.update { state ->
            if (state is ActiveEditorState.SlidesEditor) {
                state.copy(activeSlideIndex = index.coerceIn(0, (state.deck.slides.size - 1).coerceAtLeast(0)))
            } else state
        }
    }

    fun updateActiveSlide(title: String, bulletsText: String, notes: String) {
        _editorState.update { state ->
            if (state is ActiveEditorState.SlidesEditor) {
                val slides = state.deck.slides.toMutableList()
                if (state.activeSlideIndex in slides.indices) {
                    val bullets = bulletsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    slides[state.activeSlideIndex] = slides[state.activeSlideIndex].copy(
                        title = title,
                        bullets = bullets,
                        speakerNotes = notes,
                    )
                }
                state.copy(deck = SlideDeck(slides), hasUnsavedChanges = true)
            } else state
        }
    }

    fun addSlide() {
        _editorState.update { state ->
            if (state is ActiveEditorState.SlidesEditor) {
                val newSlide = SlideItem(
                    title = "Slide ${state.deck.slides.size + 1}",
                    bullets = emptyList(),
                )
                val updated = state.deck.slides + newSlide
                state.copy(
                    deck = SlideDeck(updated),
                    activeSlideIndex = updated.lastIndex,
                    hasUnsavedChanges = true,
                )
            } else state
        }
    }

    fun deleteActiveSlide() {
        _editorState.update { state ->
            if (state is ActiveEditorState.SlidesEditor && state.deck.slides.size > 1) {
                val updated = state.deck.slides.filterIndexed { i, _ -> i != state.activeSlideIndex }
                state.copy(
                    deck = SlideDeck(updated),
                    activeSlideIndex = state.activeSlideIndex.coerceAtMost(updated.lastIndex),
                    hasUnsavedChanges = true,
                )
            } else state
        }
    }

    fun saveSlides() {
        val state = _editorState.value as? ActiveEditorState.SlidesEditor ?: return
        saveSlidesInternal(state, silent = false)
    }

    private fun saveSlidesInternal(state: ActiveEditorState.SlidesEditor, silent: Boolean) {
        viewModelScope.launch {
            val file = File(state.document.filePath)
            withContext(Dispatchers.IO) { PresentationEngine.writePptx(state.deck, file) }
            val synced = repository.updateDocumentAfterSave(
                doc = state.document,
                pageCount = state.deck.slides.size,
                previewSnippet = "${state.deck.slides.size} slides",
            )
            _editorState.update {
                if (it is ActiveEditorState.SlidesEditor) it.copy(hasUnsavedChanges = false) else it
            }
            if (!silent) {
                emitMessage(if (synced) "Saved & synced presentation" else "Saved presentation (${state.deck.slides.size} slides)")
            }
        }
    }

    // --- Text / Markdown / JSON Editor Actions ---
    fun updateTextContent(newContent: String) {
        _editorState.update { state ->
            if (state is ActiveEditorState.TextEditor) {
                state.copy(content = newContent, hasUnsavedChanges = true)
            } else state
        }
    }

    fun toggleTextPreviewMode() {
        _editorState.update { state ->
            if (state is ActiveEditorState.TextEditor) state.copy(previewMode = !state.previewMode) else state
        }
    }

    fun saveTextDocument() {
        val state = _editorState.value as? ActiveEditorState.TextEditor ?: return
        saveTextInternal(state, silent = false)
    }

    private fun saveTextInternal(state: ActiveEditorState.TextEditor, silent: Boolean) {
        viewModelScope.launch {
            val file = File(state.document.filePath)
            withContext(Dispatchers.IO) { TextDocumentEngine.writeText(state.content, file) }
            val words = TextDocumentEngine.countWords(state.content)
            val snippet = state.content.lines().firstOrNull { it.isNotBlank() }?.take(80) ?: state.document.docType.displayName
            val synced = repository.updateDocumentAfterSave(
                doc = state.document,
                wordCount = words,
                previewSnippet = snippet,
            )
            _editorState.update {
                if (it is ActiveEditorState.TextEditor) it.copy(hasUnsavedChanges = false) else it
            }
            if (!silent) {
                emitMessage(if (synced) "Saved & synced ${state.document.title}" else "Saved ${state.document.title}")
            }
        }
    }

    // --- Universal Tools, Conversion, Merge, Share & Backup ---
    fun convertDocument(doc: DocumentEntity, targetType: DocumentType, openResult: Boolean = true) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.convertDocument(doc, targetType)
            _isBusy.value = false
            if (result != null) {
                emitMessage("Converted to ${targetType.shortBadge}: ${result.title}")
                if (openResult) openDocument(result)
            } else {
                emitMessage("Conversion from ${doc.docType.shortBadge} to ${targetType.shortBadge} is not supported")
            }
        }
    }

    fun mergePdfDocuments(docs: List<DocumentEntity>, outputTitle: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val merged = repository.mergePdfs(docs, outputTitle)
            _isBusy.value = false
            if (merged != null) {
                emitMessage("Merged ${docs.size} PDFs into ${merged.title}")
                openDocument(merged)
            } else {
                emitMessage("Select at least 2 valid PDF files to merge")
            }
        }
    }

    fun renameDocument(doc: DocumentEntity, newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(doc.id, newTitle)
            emitMessage("Renamed to $newTitle")
        }
    }

    fun duplicateDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.duplicateDocument(doc)
            emitMessage("Duplicated ${doc.title}")
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            emitMessage("Deleted ${doc.title}")
        }
    }

    fun togglePinned(doc: DocumentEntity) {
        viewModelScope.launch { repository.togglePinned(doc) }
    }

    fun toggleFavorite(doc: DocumentEntity) {
        viewModelScope.launch { repository.toggleFavorite(doc) }
    }

    fun exportDocumentToUri(doc: DocumentEntity, destUri: Uri) {
        viewModelScope.launch {
            val ok = repository.exportDocumentToUri(doc, destUri)
            emitMessage(if (ok) "Exported ${doc.fileName} to device" else "Export failed")
        }
    }

    fun createShareIntent(doc: DocumentEntity): Intent? = repository.createShareIntent(doc)

    fun exportBackup(destUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val count = runCatching { repository.exportBackupToUri(destUri) }.getOrDefault(-1)
            _isBusy.value = false
            if (count >= 0) {
                emitMessage("Backup saved ($count documents + settings)")
            } else {
                emitMessage("Backup export failed")
            }
        }
    }

    fun restoreBackup(srcUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val count = runCatching { repository.restoreBackupFromUri(srcUri) }.getOrDefault(-1)
            _isBusy.value = false
            if (count > 0) {
                emitMessage("Restored $count documents & preferences")
            } else {
                emitMessage("No valid Lumina backup archive found")
            }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = repository.getCacheSizeBytes()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freed = repository.clearAppCache()
            _cacheSizeBytes.value = repository.getCacheSizeBytes()
            emitMessage("Cleared ${formatByteSize(freed)} cache")
        }
    }

    // --- Settings Updaters ---
    fun setThemeMode(mode: String) = viewModelScope.launch { prefs.updateThemeMode(mode) }
    fun setAccentKey(key: String) = viewModelScope.launch { prefs.updateAccentKey(key) }
    fun setLiquidGlass(enabled: Boolean) = viewModelScope.launch { prefs.updateLiquidGlass(enabled) }
    fun setPowerSavingMode(enabled: Boolean) = viewModelScope.launch {
        prefs.updatePowerSavingMode(enabled)
        emitMessage(if (enabled) "Power Saving Mode active (low DPI & calm motion)" else "Standard Liquid Glass performance restored")
    }
    fun setOledBlackInPowerSaver(enabled: Boolean) = viewModelScope.launch { prefs.updateOledBlackInPowerSaver(enabled) }
    fun setEditorFontSize(sizeSp: Int) = viewModelScope.launch { prefs.updateEditorFontSize(sizeSp) }
    fun setAutoSave(enabled: Boolean) = viewModelScope.launch { prefs.updateAutoSave(enabled) }
    fun setPdfNightInvertDefault(enabled: Boolean) = viewModelScope.launch { prefs.updatePdfNightInvertDefault(enabled) }
    fun setShowGridlines(enabled: Boolean) = viewModelScope.launch { prefs.updateShowGridlines(enabled) }
    fun setShowLineNumbers(enabled: Boolean) = viewModelScope.launch { prefs.updateShowLineNumbers(enabled) }
}

fun formatByteSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(java.util.Locale.US, "%.2f MB", mb)
}
