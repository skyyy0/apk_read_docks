package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.db.LuminaDatabase
import com.example.data.model.BookmarkEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentType
import com.example.data.model.PdfAnnotationEntity
import com.example.data.prefs.AppPreferences
import com.example.data.prefs.UserSettings
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DocumentRepository(
    private val context: Context,
    private val db: LuminaDatabase,
    private val prefs: AppPreferences,
) {
    private val docsDir: File
        get() = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    private val exportsDir: File
        get() = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }

    val documentsFlow: Flow<List<DocumentEntity>> = db.documentDao().observeAllDocuments()

    fun observeAnnotations(docId: Long): Flow<List<PdfAnnotationEntity>> =
        db.annotationDao().observeForDocument(docId)

    fun observeBookmarks(docId: Long): Flow<List<BookmarkEntity>> =
        db.bookmarkDao().observeForDocument(docId)

    /**
     * Creates a genuinely clean, blank real document file on disk with zero dummy or simulated content.
     */
    suspend fun createBlankDocument(title: String, type: DocumentType): DocumentEntity = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim().ifBlank { "Untitled ${type.shortBadge}" }
        val fileName = uniqueFileName(cleanTitle, type.extension)
        val file = File(docsDir, fileName)

        var pages = 1
        var words = 0
        var snippet = ""

        when (type) {
            DocumentType.PDF -> {
                PdfEngine.createPdfFromTextPages(
                    listOf(PdfTextPage(title = cleanTitle, body = "")),
                    file,
                )
                pages = 1
                snippet = "1 page PDF"
            }
            DocumentType.XLSX -> {
                val wb = WorkbookData(
                    listOf(
                        SheetData(
                            name = "Sheet 1",
                            rows = List(20) { List(8) { "" } },
                        )
                    )
                )
                SpreadsheetEngine.writeXlsx(wb, file)
                pages = 1
                snippet = "1 sheet Excel workbook"
            }
            DocumentType.CSV -> {
                val wb = WorkbookData(
                    listOf(
                        SheetData(
                            name = cleanTitle,
                            rows = List(16) { List(6) { "" } },
                        )
                    )
                )
                SpreadsheetEngine.writeCsv(wb, file)
                snippet = "CSV table"
            }
            DocumentType.DOCX -> {
                val doc = RichDocument(
                    listOf(DocBlock(style = DocBlockStyle.BODY, text = ""))
                )
                WordDocumentEngine.writeDocx(doc, file)
                snippet = "Word document"
            }
            DocumentType.PPTX -> {
                val deck = SlideDeck(
                    listOf(SlideItem(title = cleanTitle, bullets = emptyList()))
                )
                PresentationEngine.writePptx(deck, file)
                pages = 1
                snippet = "1 slide presentation"
            }
            DocumentType.MARKDOWN -> {
                TextDocumentEngine.writeText("", file)
                snippet = "Markdown note"
            }
            DocumentType.JSON -> {
                val json = "{\n  \n}\n"
                TextDocumentEngine.writeText(json, file)
                snippet = "JSON document"
            }
            DocumentType.HTML -> {
                val html = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>$cleanTitle</title>\n</head>\n<body>\n  \n</body>\n</html>\n"
                TextDocumentEngine.writeText(html, file)
                snippet = "HTML document"
            }
            DocumentType.TEXT -> {
                TextDocumentEngine.writeText("", file)
                snippet = "Plain text"
            }
        }

        val now = System.currentTimeMillis()
        val entity = DocumentEntity(
            title = cleanTitle,
            fileName = fileName,
            filePath = file.absolutePath,
            type = type.name,
            sizeBytes = file.length(),
            pageCount = pages,
            wordCount = words,
            previewSnippet = snippet,
            createdAt = now,
            updatedAt = now,
            lastOpenedAt = now,
            sourceUri = null,
        )
        val id = db.documentDao().insert(entity)
        entity.copy(id = id)
    }

    /**
     * Imports any real document from device storage using zero-permission SAF ContentResolver,
     * preserving persistable read/write URI permission when offered by the document provider.
     */
    suspend fun importFromUri(uri: Uri): DocumentEntity? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            resolver.takePersistableUriPermission(uri, flags)
        }.onFailure {
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val mime = resolver.getType(uri)
        val rawName = queryDisplayName(uri)
        val docType = DocumentType.fromFileName(rawName.orEmpty(), mime)
        val originalName = rawName ?: "Document_${System.currentTimeMillis()}.${docType.extension}"
        val baseTitle = originalName.substringBeforeLast('.').ifBlank { originalName }
        val fileName = uniqueFileName(baseTitle, docType.extension)
        val destFile = File(docsDir, fileName)

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null

        val (pages, words, snippet) = inspectFileMetadata(destFile, docType)
        val now = System.currentTimeMillis()
        val entity = DocumentEntity(
            title = baseTitle,
            fileName = fileName,
            filePath = destFile.absolutePath,
            type = docType.name,
            sizeBytes = destFile.length(),
            pageCount = pages,
            wordCount = words,
            previewSnippet = snippet,
            createdAt = now,
            updatedAt = now,
            lastOpenedAt = now,
            sourceUri = uri.toString(),
        )
        val id = db.documentDao().insert(entity)
        entity.copy(id = id)
    }

    /**
     * Writes edits back directly to the original external SAF file on device storage when writable.
     */
    suspend fun syncBackToSourceUri(doc: DocumentEntity): Boolean = withContext(Dispatchers.IO) {
        val uriStr = doc.sourceUri ?: return@withContext false
        val localFile = File(doc.filePath)
        if (!localFile.exists()) return@withContext false
        runCatching {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                FileInputStream(localFile).use { input -> input.copyTo(out) }
            } != null
        }.getOrDefault(false)
    }

    private fun inspectFileMetadata(file: File, type: DocumentType): Triple<Int, Int, String> {
        return runCatching {
            when (type) {
                DocumentType.PDF -> {
                    val p = PdfEngine.getPageCount(file).coerceAtLeast(1)
                    Triple(p, 0, "$p page PDF")
                }
                DocumentType.XLSX -> {
                    val wb = SpreadsheetEngine.readXlsx(file)
                    Triple(wb.sheets.size, wb.sheets.sumOf { it.rowCount }, "${wb.sheets.size} sheet Excel workbook")
                }
                DocumentType.CSV -> {
                    val wb = SpreadsheetEngine.readCsv(file)
                    val rows = wb.sheets.firstOrNull()?.rowCount ?: 0
                    Triple(1, rows, "CSV table • $rows rows")
                }
                DocumentType.DOCX -> {
                    val doc = WordDocumentEngine.readDocx(file)
                    val preview = doc.blocks.firstOrNull { it.text.isNotBlank() }?.text?.take(80) ?: "Word document"
                    Triple(1, doc.wordCount, preview)
                }
                DocumentType.PPTX -> {
                    val deck = PresentationEngine.readPptx(file)
                    Triple(deck.slides.size, deck.slides.size * 12, "${deck.slides.size} slides presentation")
                }
                else -> {
                    val text = TextDocumentEngine.readText(file)
                    val w = TextDocumentEngine.countWords(text)
                    val preview = text.lines().firstOrNull { it.isNotBlank() }?.take(80) ?: type.displayName
                    Triple(1, w, preview)
                }
            }
        }.getOrDefault(Triple(1, 0, type.displayName))
    }

    /**
     * Universal Format Converter: converts an existing real document into a new file of targetType.
     */
    suspend fun convertDocument(source: DocumentEntity, targetType: DocumentType): DocumentEntity? =
        withContext(Dispatchers.IO) {
            val srcFile = File(source.filePath)
            if (!srcFile.exists()) return@withContext null

            val newTitle = "${source.title} (${targetType.shortBadge})"
            val outName = uniqueFileName(newTitle, targetType.extension)
            val outFile = File(docsDir, outName)
            val srcType = source.docType

            val converted = runCatching {
                when {
                    srcType == DocumentType.DOCX && targetType == DocumentType.PDF -> {
                        val doc = WordDocumentEngine.readDocx(srcFile)
                        WordDocumentEngine.exportToPdf(source.title, doc, outFile)
                        true
                    }
                    srcType == DocumentType.DOCX && targetType == DocumentType.MARKDOWN -> {
                        val doc = WordDocumentEngine.readDocx(srcFile)
                        TextDocumentEngine.writeText(WordDocumentEngine.toMarkdown(doc), outFile)
                        true
                    }
                    srcType == DocumentType.DOCX && targetType == DocumentType.HTML -> {
                        val doc = WordDocumentEngine.readDocx(srcFile)
                        TextDocumentEngine.writeText(WordDocumentEngine.toHtml(source.title, doc), outFile)
                        true
                    }
                    (srcType == DocumentType.XLSX || srcType == DocumentType.CSV) && targetType == DocumentType.PDF -> {
                        val wb = if (srcType == DocumentType.XLSX) SpreadsheetEngine.readXlsx(srcFile) else SpreadsheetEngine.readCsv(srcFile)
                        SpreadsheetEngine.exportWorkbookToPdf(source.title, wb, outFile)
                        true
                    }
                    srcType == DocumentType.XLSX && targetType == DocumentType.CSV -> {
                        val wb = SpreadsheetEngine.readXlsx(srcFile)
                        SpreadsheetEngine.writeCsv(wb, outFile)
                        true
                    }
                    srcType == DocumentType.CSV && targetType == DocumentType.XLSX -> {
                        val wb = SpreadsheetEngine.readCsv(srcFile)
                        SpreadsheetEngine.writeXlsx(wb, outFile)
                        true
                    }
                    srcType == DocumentType.PPTX && targetType == DocumentType.PDF -> {
                        val deck = PresentationEngine.readPptx(srcFile)
                        PresentationEngine.exportDeckToPdf(source.title, deck, outFile)
                        true
                    }
                    (srcType == DocumentType.MARKDOWN || srcType == DocumentType.TEXT || srcType == DocumentType.JSON || srcType == DocumentType.HTML) &&
                        targetType == DocumentType.PDF -> {
                        val txt = TextDocumentEngine.readText(srcFile)
                        TextDocumentEngine.exportTextToPdf(source.title, txt, outFile)
                        true
                    }
                    (srcType == DocumentType.MARKDOWN || srcType == DocumentType.TEXT) &&
                        targetType == DocumentType.DOCX -> {
                        val txt = TextDocumentEngine.readText(srcFile)
                        val rich = WordDocumentEngine.fromMarkdown(txt)
                        WordDocumentEngine.writeDocx(rich, outFile)
                        true
                    }
                    (srcType == DocumentType.MARKDOWN || srcType == DocumentType.TEXT) &&
                        targetType == DocumentType.HTML -> {
                        val txt = TextDocumentEngine.readText(srcFile)
                        val rich = WordDocumentEngine.fromMarkdown(txt)
                        TextDocumentEngine.writeText(WordDocumentEngine.toHtml(source.title, rich), outFile)
                        true
                    }
                    else -> false
                }
            }.getOrDefault(false)

            if (!converted || !outFile.exists()) return@withContext null

            val (pages, words, snippet) = inspectFileMetadata(outFile, targetType)
            val now = System.currentTimeMillis()
            val entity = DocumentEntity(
                title = newTitle,
                fileName = outName,
                filePath = outFile.absolutePath,
                type = targetType.name,
                sizeBytes = outFile.length(),
                pageCount = pages,
                wordCount = words,
                previewSnippet = "Converted from ${srcType.shortBadge} • $snippet",
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = now,
            )
            val id = db.documentDao().insert(entity)
            entity.copy(id = id)
        }

    /**
     * Merges 2+ PDF documents from library into a single combined real PDF file.
     */
    suspend fun mergePdfs(sources: List<DocumentEntity>, mergedTitle: String): DocumentEntity? =
        withContext(Dispatchers.IO) {
            val pdfFiles = sources.filter { it.docType == DocumentType.PDF }
                .map { File(it.filePath) }
                .filter { it.exists() }
            if (pdfFiles.size < 2) return@withContext null

            val title = mergedTitle.trim().ifBlank { "Merged PDF" }
            val outName = uniqueFileName(title, "pdf")
            val outFile = File(docsDir, outName)
            val totalPages = PdfEngine.mergePdfs(pdfFiles, outFile)

            val now = System.currentTimeMillis()
            val entity = DocumentEntity(
                title = title,
                fileName = outName,
                filePath = outFile.absolutePath,
                type = DocumentType.PDF.name,
                sizeBytes = outFile.length(),
                pageCount = totalPages,
                wordCount = 0,
                previewSnippet = "Merged ${pdfFiles.size} PDFs • $totalPages total pages",
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = now,
            )
            val id = db.documentDao().insert(entity)
            entity.copy(id = id)
        }

    suspend fun duplicateDocument(source: DocumentEntity): DocumentEntity? = withContext(Dispatchers.IO) {
        val srcFile = File(source.filePath)
        if (!srcFile.exists()) return@withContext null
        val newTitle = "${source.title} Copy"
        val outName = uniqueFileName(newTitle, source.docType.extension)
        val outFile = File(docsDir, outName)
        srcFile.copyTo(outFile, overwrite = true)
        val now = System.currentTimeMillis()
        val copyEntity = source.copy(
            id = 0L,
            title = newTitle,
            fileName = outName,
            filePath = outFile.absolutePath,
            sizeBytes = outFile.length(),
            isPinned = false,
            sourceUri = null,
            createdAt = now,
            updatedAt = now,
            lastOpenedAt = now,
        )
        val id = db.documentDao().insert(copyEntity)
        copyEntity.copy(id = id)
    }

    suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        runCatching { File(doc.filePath).delete() }
        db.annotationDao().deleteForDocument(doc.id)
        db.bookmarkDao().deleteForDocument(doc.id)
        db.documentDao().deleteById(doc.id)
    }

    suspend fun renameDocument(docId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        if (newTitle.isNotBlank()) db.documentDao().rename(docId, newTitle.trim())
    }

    suspend fun togglePinned(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        db.documentDao().setPinned(doc.id, !doc.isPinned)
    }

    suspend fun toggleFavorite(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        db.documentDao().setFavorite(doc.id, !doc.isFavorite)
    }

    suspend fun touchOpened(docId: Long) = withContext(Dispatchers.IO) {
        db.documentDao().touchOpened(docId)
    }

    suspend fun updateDocumentAfterSave(
        doc: DocumentEntity,
        pageCount: Int = doc.pageCount,
        wordCount: Int = doc.wordCount,
        previewSnippet: String = doc.previewSnippet,
    ): Boolean = withContext(Dispatchers.IO) {
        val file = File(doc.filePath)
        db.documentDao().update(
            doc.copy(
                sizeBytes = file.length(),
                pageCount = pageCount,
                wordCount = wordCount,
                previewSnippet = previewSnippet,
                updatedAt = System.currentTimeMillis(),
            )
        )
        syncBackToSourceUri(doc)
    }

    suspend fun addPdfAnnotation(annotation: PdfAnnotationEntity) = withContext(Dispatchers.IO) {
        db.annotationDao().insert(annotation)
    }

    suspend fun deletePdfAnnotation(annotationId: Long) = withContext(Dispatchers.IO) {
        db.annotationDao().deleteById(annotationId)
    }

    suspend fun clearAnnotationsForDocument(docId: Long) = withContext(Dispatchers.IO) {
        db.annotationDao().deleteForDocument(docId)
    }

    suspend fun addBookmark(docId: Long, targetIndex: Int, label: String) = withContext(Dispatchers.IO) {
        db.bookmarkDao().insert(BookmarkEntity(documentId = docId, targetIndex = targetIndex, label = label))
    }

    suspend fun deleteBookmark(bookmarkId: Long) = withContext(Dispatchers.IO) {
        db.bookmarkDao().deleteById(bookmarkId)
    }

    suspend fun exportDocumentToUri(doc: DocumentEntity, destUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(doc.filePath)
        if (!srcFile.exists()) return@withContext false
        runCatching {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                FileInputStream(srcFile).use { input -> input.copyTo(out) }
            } != null
        }.getOrDefault(false)
    }

    fun createShareIntent(doc: DocumentEntity): Intent? {
        val file = File(doc.filePath)
        if (!file.exists()) return null
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = doc.docType.mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, doc.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun exportBackupToUri(destUri: Uri): Int = withContext(Dispatchers.IO) {
        val docs = db.documentDao().getAllDocumentsOnce()
        val annotations = db.annotationDao().getAllOnce()
        val bookmarks = db.bookmarkDao().getAllOnce()
        val settings = prefs.getSettingsOnce()

        val rootJson = JSONObject().apply {
            put("app", "Lumina Docs")
            put("developer", "sky")
            put("backupVersion", 2)
            put("exportedAt", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("themeMode", settings.themeMode)
                put("accentKey", settings.accentKey)
                put("liquidGlassEnabled", settings.liquidGlassEnabled)
                put("powerSavingMode", settings.powerSavingMode)
                put("oledBlackInPowerSaver", settings.oledBlackInPowerSaver)
                put("editorFontSizeSp", settings.editorFontSizeSp)
                put("autoSaveEnabled", settings.autoSaveEnabled)
                put("pdfNightInvertDefault", settings.pdfNightInvertDefault)
                put("showGridlines", settings.showGridlines)
                put("showLineNumbers", settings.showLineNumbers)
            })
            put("documents", JSONArray().apply {
                docs.forEach { d ->
                    put(JSONObject().apply {
                        put("oldId", d.id)
                        put("title", d.title)
                        put("fileName", d.fileName)
                        put("type", d.type)
                        put("folderName", d.folderName)
                        put("isPinned", d.isPinned)
                        put("isFavorite", d.isFavorite)
                        put("pageCount", d.pageCount)
                        put("wordCount", d.wordCount)
                        put("previewSnippet", d.previewSnippet)
                        put("createdAt", d.createdAt)
                        put("updatedAt", d.updatedAt)
                    })
                }
            })
            put("annotations", JSONArray().apply {
                annotations.forEach { a ->
                    put(JSONObject().apply {
                        put("oldDocId", a.documentId)
                        put("pageIndex", a.pageIndex)
                        put("kind", a.kind)
                        put("content", a.content)
                        put("normX", a.normX.toDouble())
                        put("normY", a.normY.toDouble())
                        put("colorHex", a.colorHex)
                    })
                }
            })
            put("bookmarks", JSONArray().apply {
                bookmarks.forEach { b ->
                    put(JSONObject().apply {
                        put("oldDocId", b.documentId)
                        put("targetIndex", b.targetIndex)
                        put("label", b.label)
                    })
                }
            })
        }

        context.contentResolver.openOutputStream(destUri)?.use { os ->
            ZipOutputStream(os).use { zos ->
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                for (doc in docs) {
                    val f = File(doc.filePath)
                    if (f.exists()) {
                        zos.putNextEntry(ZipEntry("files/${doc.fileName}"))
                        FileInputStream(f).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
        prefs.updateLastBackupTimestamp(System.currentTimeMillis())
        docs.size
    }

    suspend fun restoreBackupFromUri(srcUri: Uri): Int = withContext(Dispatchers.IO) {
        var manifestJson: JSONObject? = null
        val restoredFiles = mutableMapOf<String, ByteArray>()

        context.contentResolver.openInputStream(srcUri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        if (entry.name == "manifest.json") {
                            manifestJson = JSONObject(baos.toString("UTF-8"))
                        } else if (entry.name.startsWith("files/")) {
                            restoredFiles[entry.name.removePrefix("files/")] = baos.toByteArray()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        val manifest = manifestJson ?: return@withContext 0

        manifest.optJSONObject("settings")?.let { s ->
            prefs.applyRestoredSettings(
                UserSettings(
                    themeMode = s.optString("themeMode", "SYSTEM"),
                    accentKey = s.optString("accentKey", "GLACIER"),
                    liquidGlassEnabled = s.optBoolean("liquidGlassEnabled", true),
                    powerSavingMode = s.optBoolean("powerSavingMode", false),
                    oledBlackInPowerSaver = s.optBoolean("oledBlackInPowerSaver", true),
                    editorFontSizeSp = s.optInt("editorFontSizeSp", 15),
                    autoSaveEnabled = s.optBoolean("autoSaveEnabled", true),
                    pdfNightInvertDefault = s.optBoolean("pdfNightInvertDefault", false),
                    showGridlines = s.optBoolean("showGridlines", true),
                    showLineNumbers = s.optBoolean("showLineNumbers", true),
                )
            )
        }

        val idMap = mutableMapOf<Long, Long>()
        val docsArr = manifest.optJSONArray("documents") ?: JSONArray()
        var restoredCount = 0
        for (i in 0 until docsArr.length()) {
            val obj = docsArr.getJSONObject(i)
            val oldId = obj.optLong("oldId", -1L)
            val origName = obj.optString("fileName")
            val bytes = restoredFiles[origName] ?: continue
            val typeStr = obj.optString("type", DocumentType.TEXT.name)
            val docType = runCatching { DocumentType.valueOf(typeStr) }.getOrDefault(DocumentType.TEXT)
            val title = obj.optString("title", "Restored Document")
            val uniqueName = uniqueFileName(title, docType.extension)
            val targetFile = File(docsDir, uniqueName)
            targetFile.writeBytes(bytes)

            val newId = db.documentDao().insert(
                DocumentEntity(
                    title = title,
                    fileName = uniqueName,
                    filePath = targetFile.absolutePath,
                    type = docType.name,
                    sizeBytes = targetFile.length(),
                    folderName = obj.optString("folderName", "Workspace"),
                    isPinned = obj.optBoolean("isPinned", false),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    pageCount = obj.optInt("pageCount", 1),
                    wordCount = obj.optInt("wordCount", 0),
                    previewSnippet = obj.optString("previewSnippet", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    lastOpenedAt = System.currentTimeMillis(),
                )
            )
            if (oldId >= 0) idMap[oldId] = newId
            restoredCount++
        }

        val annArr = manifest.optJSONArray("annotations") ?: JSONArray()
        for (i in 0 until annArr.length()) {
            val obj = annArr.getJSONObject(i)
            val newDocId = idMap[obj.optLong("oldDocId", -1L)] ?: continue
            db.annotationDao().insert(
                PdfAnnotationEntity(
                    documentId = newDocId,
                    pageIndex = obj.optInt("pageIndex", 0),
                    kind = obj.optString("kind", "TEXT"),
                    content = obj.optString("content", ""),
                    normX = obj.optDouble("normX", 0.2).toFloat(),
                    normY = obj.optDouble("normY", 0.2).toFloat(),
                    colorHex = obj.optLong("colorHex", 0xFF007AFF),
                )
            )
        }

        val bmArr = manifest.optJSONArray("bookmarks") ?: JSONArray()
        for (i in 0 until bmArr.length()) {
            val obj = bmArr.getJSONObject(i)
            val newDocId = idMap[obj.optLong("oldDocId", -1L)] ?: continue
            db.bookmarkDao().insert(
                BookmarkEntity(
                    documentId = newDocId,
                    targetIndex = obj.optInt("targetIndex", 0),
                    label = obj.optString("label", "Bookmark"),
                )
            )
        }

        restoredCount
    }

    suspend fun purgeOrphanedLegacySeedFiles() = withContext(Dispatchers.IO) {
        val legacyTitles = setOf(
            "Executive Brief PDF",
            "Monthly Budget Tracker",
            "Client Invoice Pro",
            "Project Proposal",
            "Meeting Minutes",
            "Product Pitch Deck",
            "Technical Spec Note",
            "Structured JSON Config",
        )
        val allDocs = db.documentDao().getAllDocumentsOnce()
        for (doc in allDocs) {
            if (doc.sourceUri == null && doc.title in legacyTitles) {
                runCatching { File(doc.filePath).delete() }
                db.annotationDao().deleteForDocument(doc.id)
                db.bookmarkDao().deleteForDocument(doc.id)
                db.documentDao().deleteById(doc.id)
            }
        }
        val activePaths = db.documentDao().getAllDocumentsOnce().map { it.filePath }.toHashSet()
        val legacyPrefixes = listOf(
            "executive_brief",
            "monthly_budget",
            "client_invoice",
            "project_proposal",
            "meeting_minutes",
            "product_pitch",
            "technical_spec",
            "structured_json",
        )
        docsDir.listFiles()?.forEach { file ->
            if (file.isFile &&
                file.absolutePath !in activePaths &&
                legacyPrefixes.any { prefix -> file.name.startsWith(prefix) }
            ) {
                runCatching { file.delete() }
            }
        }
    }

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        calculateDirSize(context.cacheDir)
    }

    suspend fun clearAppCache(): Long = withContext(Dispatchers.IO) {
        PdfEngine.clearRenderCache()
        val before = calculateDirSize(context.cacheDir)
        context.cacheDir.listFiles()?.forEach { child ->
            runCatching { child.deleteRecursively() }
        }
        exportsDir.mkdirs()
        before
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.listFiles()?.sumOf { calculateDirSize(it) } ?: 0L
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun uniqueFileName(title: String, ext: String): String {
        val slug = title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(32)
            .ifBlank { "document" }
        return "${slug}_${System.currentTimeMillis()}.$ext"
    }
}
