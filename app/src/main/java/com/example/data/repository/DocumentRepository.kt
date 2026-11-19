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

data class TemplateSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: DocumentType,
)

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

    val builtInTemplates: List<TemplateSpec> = listOf(
        TemplateSpec(
            id = "tpl_budget_xlsx",
            title = "Monthly Budget Tracker",
            subtitle = "Live SUM & balance formulas (.xlsx)",
            type = DocumentType.XLSX,
        ),
        TemplateSpec(
            id = "tpl_invoice_xlsx",
            title = "Client Invoice Pro",
            subtitle = "Auto-calculated line totals & tax (.xlsx)",
            type = DocumentType.XLSX,
        ),
        TemplateSpec(
            id = "tpl_proposal_docx",
            title = "Project Proposal",
            subtitle = "Structured headings, tasks & quotes (.docx)",
            type = DocumentType.DOCX,
        ),
        TemplateSpec(
            id = "tpl_meeting_docx",
            title = "Meeting Minutes",
            subtitle = "Action checklists & decisions (.docx)",
            type = DocumentType.DOCX,
        ),
        TemplateSpec(
            id = "tpl_pitch_pptx",
            title = "Product Pitch Deck",
            subtitle = "Widescreen multi-slide presentation (.pptx)",
            type = DocumentType.PPTX,
        ),
        TemplateSpec(
            id = "tpl_guide_pdf",
            title = "Executive Brief PDF",
            subtitle = "Multi-page crisp PDF ready to annotate (.pdf)",
            type = DocumentType.PDF,
        ),
        TemplateSpec(
            id = "tpl_spec_md",
            title = "Technical Spec Note",
            subtitle = "Live Markdown with code & checklists (.md)",
            type = DocumentType.MARKDOWN,
        ),
        TemplateSpec(
            id = "tpl_config_json",
            title = "Structured JSON Config",
            subtitle = "Validated & pretty-printable data (.json)",
            type = DocumentType.JSON,
        ),
    )

    suspend fun ensureStarterDocuments() = withContext(Dispatchers.IO) {
        if (db.documentDao().count() > 0) return@withContext
        createFromTemplate("tpl_guide_pdf", pinned = true)
        createFromTemplate("tpl_budget_xlsx", pinned = true)
        createFromTemplate("tpl_proposal_docx", pinned = false)
        createFromTemplate("tpl_pitch_pptx", pinned = false)
        createFromTemplate("tpl_spec_md", pinned = false)
    }

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
                    listOf(
                        PdfTextPage(
                            title = cleanTitle,
                            body = "Created in Lumina Docs.\nTap the Annotate button to add text overlays, highlights, sticky notes, or append new pages directly into this PDF.",
                        )
                    ),
                    file,
                )
                pages = 1
                words = 24
                snippet = "Blank PDF canvas ready for annotations & pages"
            }
            DocumentType.XLSX -> {
                val wb = WorkbookData(
                    listOf(
                        SheetData(
                            name = "Sheet 1",
                            rows = listOf(
                                listOf("Item", "Qty", "Price", "Total", "Notes", ""),
                                listOf("Sample Item", "2", "50", "=B2*C2", "Edit formula in fx bar", ""),
                                listOf("Total", "", "", "=SUM(D2:D2)", "", ""),
                            ) + List(11) { List(6) { "" } },
                        )
                    )
                )
                SpreadsheetEngine.writeXlsx(wb, file)
                pages = 1
                words = 12
                snippet = "1 sheet • Live Excel formulas enabled"
            }
            DocumentType.CSV -> {
                val wb = WorkbookData(
                    listOf(
                        SheetData(
                            name = "Data",
                            rows = listOf(
                                listOf("Column A", "Column B", "Column C", "Column D"),
                                listOf("", "", "", ""),
                            ) + List(10) { List(4) { "" } },
                        )
                    )
                )
                SpreadsheetEngine.writeCsv(wb, file)
                snippet = "Comma-separated table"
            }
            DocumentType.DOCX -> {
                val doc = RichDocument(
                    listOf(
                        DocBlock(style = DocBlockStyle.HEADING1, text = cleanTitle, isBold = true),
                        DocBlock(style = DocBlockStyle.BODY, text = "Start writing your document here. Tap + to add headings, bullets, checklists, quotes, or code blocks."),
                    )
                )
                WordDocumentEngine.writeDocx(doc, file)
                words = doc.wordCount
                snippet = doc.blocks.last().text.take(80)
            }
            DocumentType.PPTX -> {
                val deck = SlideDeck(
                    listOf(
                        SlideItem(
                            title = cleanTitle,
                            bullets = listOf("Key insight one", "Key insight two", "Next steps"),
                        )
                    )
                )
                PresentationEngine.writePptx(deck, file)
                pages = 1
                words = 10
                snippet = "1 slide • Widescreen deck"
            }
            DocumentType.MARKDOWN -> {
                val md = "# $cleanTitle\n\nWrite with **Markdown** and switch to Live Preview anytime.\n\n- [ ] First task\n- [ ] Export to PDF or DOCX\n"
                TextDocumentEngine.writeText(md, file)
                words = TextDocumentEngine.countWords(md)
                snippet = "Markdown note with live preview"
            }
            DocumentType.JSON -> {
                val json = "{\n  \"name\": \"$cleanTitle\",\n  \"version\": \"1.0.0\",\n  \"items\": []\n}\n"
                TextDocumentEngine.writeText(json, file)
                words = TextDocumentEngine.countWords(json)
                snippet = "Structured JSON document"
            }
            DocumentType.HTML -> {
                val html = "<!DOCTYPE html>\n<html>\n<head><title>$cleanTitle</title></head>\n<body>\n  <h1>$cleanTitle</h1>\n  <p>Edit HTML content here.</p>\n</body>\n</html>\n"
                TextDocumentEngine.writeText(html, file)
                words = TextDocumentEngine.countWords(html)
                snippet = "HTML web document"
            }
            DocumentType.TEXT -> {
                val txt = "$cleanTitle\n\n"
                TextDocumentEngine.writeText(txt, file)
                words = TextDocumentEngine.countWords(txt)
                snippet = "Plain text document"
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
        )
        val id = db.documentDao().insert(entity)
        entity.copy(id = id)
    }

    suspend fun createFromTemplate(templateId: String, pinned: Boolean = false): DocumentEntity =
        withContext(Dispatchers.IO) {
            val spec = builtInTemplates.firstOrNull { it.id == templateId }
                ?: return@withContext createBlankDocument("New Document", DocumentType.DOCX)

            val fileName = uniqueFileName(spec.title, spec.type.extension)
            val file = File(docsDir, fileName)
            var pages = 1
            var words = 0
            var snippet = spec.subtitle

            when (templateId) {
                "tpl_budget_xlsx" -> {
                    val wb = WorkbookData(
                        listOf(
                            SheetData(
                                name = "Monthly Budget",
                                rows = listOf(
                                    listOf("Category", "Planned", "Actual", "Variance", "Status", "Notes"),
                                    listOf("Housing & Utilities", "1200", "1180", "=B2-C2", "=IF(D2>=0,\"On Track\",\"Over\")", "Electricity + Fiber"),
                                    listOf("Groceries & Dining", "550", "490", "=B3-C3", "=IF(D3>=0,\"On Track\",\"Over\")", "Weekly market"),
                                    listOf("Transport & Fuel", "220", "195", "=B4-C4", "=IF(D4>=0,\"On Track\",\"Over\")", "Commute pass"),
                                    listOf("Cloud & Subscriptions", "95", "110", "=B5-C5", "=IF(D5>=0,\"On Track\",\"Over\")", "Annual renewal"),
                                    listOf("Savings & Investments", "800", "850", "=B6-C6", "=IF(D6>=0,\"On Track\",\"Extra\")", "Index fund"),
                                    listOf("TOTAL", "=SUM(B2:B6)", "=SUM(C2:C6)", "=SUM(D2:D6)", "Summary", "Auto-calculated"),
                                    listOf("AVERAGE", "=AVERAGE(B2:B6)", "=AVERAGE(C2:C6)", "=AVERAGE(D2:D6)", "", ""),
                                ) + List(6) { List(6) { "" } },
                            ),
                            SheetData(
                                name = "Income",
                                rows = listOf(
                                    listOf("Source", "Amount", "Tax %", "Net Income", "", ""),
                                    listOf("Primary Salary", "4200", "0.10", "=B2*(1-C2)", "", ""),
                                    listOf("Freelance Projects", "950", "0.05", "=B3*(1-C3)", "", ""),
                                    listOf("Total Net", "=SUM(B2:B3)", "", "=SUM(D2:D3)", "", ""),
                                ) + List(8) { List(6) { "" } },
                            ),
                        )
                    )
                    SpreadsheetEngine.writeXlsx(wb, file)
                    pages = 2
                    words = 48
                    snippet = "2 sheets • Live SUM, AVERAGE & IF formulas"
                }
                "tpl_invoice_xlsx" -> {
                    val wb = WorkbookData(
                        listOf(
                            SheetData(
                                name = "Invoice #2026-01",
                                rows = listOf(
                                    listOf("Service / Item", "Qty", "Unit Price", "Line Total", "Category", ""),
                                    listOf("Mobile UI Architecture", "1", "1800", "=B2*C2", "Design", ""),
                                    listOf("Android Native Engine", "1", "2400", "=B3*C3", "Engineering", ""),
                                    listOf("QA & CI/CD Pipeline", "1", "650", "=B4*C4", "DevOps", ""),
                                    listOf("Subtotal", "", "", "=SUM(D2:D4)", "", ""),
                                    listOf("Tax (11%)", "", "", "=D5*0.11", "", ""),
                                    listOf("GRAND TOTAL", "", "", "=D5+D6", "Due 14 Days", ""),
                                ) + List(7) { List(6) { "" } },
                            )
                        )
                    )
                    SpreadsheetEngine.writeXlsx(wb, file)
                    pages = 1
                    words = 35
                    snippet = "Auto-calculated invoice with tax & grand total"
                }
                "tpl_proposal_docx" -> {
                    val doc = RichDocument(
                        listOf(
                            DocBlock(style = DocBlockStyle.HEADING1, text = "Q4 Mobile Experience Redesign", isBold = true),
                            DocBlock(style = DocBlockStyle.QUOTE, text = "Prepared with Lumina Docs — universal offline-first editing with instant PDF & Office export.", isItalic = true),
                            DocBlock(style = DocBlockStyle.HEADING2, text = "1. Executive Summary", isBold = true),
                            DocBlock(style = DocBlockStyle.BODY, text = "Our objective is to deliver a fluid, liquid-glass document workflow across Android 5 through the latest Android release with sub-second file loading and zero bloat."),
                            DocBlock(style = DocBlockStyle.HEADING2, text = "2. Core Deliverables", isBold = true),
                            DocBlock(style = DocBlockStyle.BULLET, text = "Native PDF reader with page annotations, night-ink invert, and multi-PDF merge"),
                            DocBlock(style = DocBlockStyle.BULLET, text = "Real XLSX/CSV spreadsheet editor with live formula recalculation"),
                            DocBlock(style = DocBlockStyle.BULLET, text = "Rich DOCX & Markdown studio with instant cross-format conversion"),
                            DocBlock(style = DocBlockStyle.HEADING2, text = "3. Launch Checklist", isBold = true),
                            DocBlock(style = DocBlockStyle.CHECKLIST, text = "Finalize liquid-glass light & dark design tokens", isChecked = true),
                            DocBlock(style = DocBlockStyle.CHECKLIST, text = "Verify local backup & restore archive integrity", isChecked = true),
                            DocBlock(style = DocBlockStyle.CHECKLIST, text = "Publish automated GitHub Actions release APK", isChecked = false),
                        )
                    )
                    WordDocumentEngine.writeDocx(doc, file)
                    words = doc.wordCount
                    snippet = "Executive proposal with headings, bullets & checklist"
                }
                "tpl_meeting_docx" -> {
                    val doc = RichDocument(
                        listOf(
                            DocBlock(style = DocBlockStyle.HEADING1, text = "Weekly Engineering Sync", isBold = true),
                            DocBlock(style = DocBlockStyle.BODY, text = "Date: Today  •  Owner: sky  •  Status: Active"),
                            DocBlock(style = DocBlockStyle.HEADING2, text = "Key Decisions", isBold = true),
                            DocBlock(style = DocBlockStyle.BULLET, text = "Keep APK footprint lightweight using zero-bloat native OOXML & PDF engines"),
                            DocBlock(style = DocBlockStyle.BULLET, text = "Enable Power Saving Mode toggle for battery-conscious reading sessions"),
                            DocBlock(style = DocBlockStyle.HEADING2, text = "Action Items", isBold = true),
                            DocBlock(style = DocBlockStyle.CHECKLIST, text = "Review spreadsheet formula parser coverage", isChecked = true),
                            DocBlock(style = DocBlockStyle.CHECKLIST, text = "Export PDF summary for stakeholders", isChecked = false),
                        )
                    )
                    WordDocumentEngine.writeDocx(doc, file)
                    words = doc.wordCount
                    snippet = "Meeting notes with decisions & action items"
                }
                "tpl_pitch_pptx" -> {
                    val deck = SlideDeck(
                        listOf(
                            SlideItem(
                                title = "Lumina Docs Studio",
                                bullets = listOf(
                                    "One lightweight Android suite for PDF, Excel, Word, Slides & Code",
                                    "iOS-inspired Liquid Glass design with silky 60fps micro-interactions",
                                    "Engineered by sky for speed from Android 5 to the latest release",
                                ),
                                speakerNotes = "Open with speed and lightweight architecture highlights.",
                            ),
                            SlideItem(
                                title = "Beyond Traditional PDF Readers",
                                bullets = listOf(
                                    "Edit & burn annotations directly into real PDF files",
                                    "Live Excel formula engine (=SUM, =AVERAGE, =IF) on mobile",
                                    "Instant cross-format converter: DOCX, XLSX, PPTX, MD -> PDF",
                                ),
                                speakerNotes = "Demonstrate live conversion and PDF merge in Tools tab.",
                            ),
                            SlideItem(
                                title = "Privacy, Backup & Battery Care",
                                bullets = listOf(
                                    "100% offline-ready local workspace with zero tracking",
                                    "One-tap full .luminabak archive backup & restore",
                                    "Smart Power Saving Mode lowers render DPI & saves OLED battery",
                                ),
                                speakerNotes = "Show Settings screen with Power Saving Mode toggle.",
                            ),
                        )
                    )
                    PresentationEngine.writePptx(deck, file)
                    pages = deck.slides.size
                    words = 74
                    snippet = "3 widescreen slides • Editable deck & PDF export"
                }
                "tpl_guide_pdf" -> {
                    PdfEngine.createPdfFromTextPages(
                        listOf(
                            PdfTextPage(
                                title = "Welcome to Lumina Docs",
                                body = "Lumina Docs is your all-in-one lightweight document studio crafted by sky.\n\n" +
                                    "Read, edit, annotate, convert, and merge real PDF, Excel (.xlsx/.csv), Word (.docx), PowerPoint (.pptx), Markdown (.md), and JSON files seamlessly on Android.\n\n" +
                                    "Try tapping the Annotate controls below to place text overlays, highlights, or sticky notes directly onto this page, then tap Save PDF to bake your edits permanently into the file.",
                            ),
                            PdfTextPage(
                                title = "Pro Tips & Power Features",
                                body = "1. Night-Ink Reading: Toggle the moon icon in the PDF toolbar to invert bright white pages into an eye-friendly dark canvas.\n\n" +
                                    "2. Universal Converter: Open the Tools tab to convert any Word, Excel, Presentation, or Markdown file into a clean PDF or another format.\n\n" +
                                    "3. Power Saving Mode: Enable Mode Hemat Daya in Settings to switch to low-power render DPI and conserve battery during long reading sessions.\n\n" +
                                    "4. Full Local Backup: Export your entire library and settings into a single .luminabak archive anytime.",
                            ),
                        ),
                        file,
                    )
                    pages = 2
                    words = 148
                    snippet = "2 pages • Quick-start guide ready to annotate"
                }
                "tpl_spec_md" -> {
                    val md = """# Lumina Docs Architecture Spec

> Lightweight native Kotlin engines designed for speed, clarity, and reliability.

## Supported Formats
- **PDF** (`.pdf`) — Render, annotate, append pages, merge & export
- **Excel** (`.xlsx`, `.csv`) — Multi-sheet grid + live `=SUM`, `=AVERAGE`, `=IF` engine
- **Word** (`.docx`) — Block editor with headings, checklists, quotes & PDF export
- **Slides** (`.pptx`) — Widescreen deck editor & 16:9 PDF export
- **Notes & Code** (`.md`, `.json`, `.html`, `.txt`) — Syntax helpers & live preview

## Release Checklist
- [x] Consistent `applicationId` (`com.luminadocs.id`) for seamless in-place updates
- [x] Zero broad storage permissions (Storage Access Framework compliant)
- [ ] Push to GitHub Actions workflow for automated `.apk` release
"""
                    TextDocumentEngine.writeText(md, file)
                    words = TextDocumentEngine.countWords(md)
                    snippet = "Markdown technical specification with live preview"
                }
                "tpl_config_json" -> {
                    val json = """{
  "app": "Lumina Docs",
  "developer": "sky",
  "packageId": "com.luminadocs.id",
  "features": [
    "PDF Reader & Annotator",
    "Excel XLSX/CSV Formula Engine",
    "Word DOCX Rich Editor",
    "PowerPoint PPTX Deck Editor",
    "Universal Format Converter",
    "Power Saving Mode & Full Backup"
  ],
  "version": "1.0.0"
}
"""
                    TextDocumentEngine.writeText(json, file)
                    words = TextDocumentEngine.countWords(json)
                    snippet = "Formatted JSON with live syntax validation"
                }
                else -> return@withContext createBlankDocument(spec.title, spec.type)
            }

            val now = System.currentTimeMillis()
            val entity = DocumentEntity(
                title = spec.title,
                fileName = fileName,
                filePath = file.absolutePath,
                type = spec.type.name,
                sizeBytes = file.length(),
                isPinned = pinned,
                pageCount = pages,
                wordCount = words,
                previewSnippet = snippet,
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = now,
            )
            val id = db.documentDao().insert(entity)
            entity.copy(id = id)
        }

    /**
     * Imports any real document from device storage using zero-permission SAF ContentResolver.
     */
    suspend fun importFromUri(uri: Uri): DocumentEntity? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        val originalName = queryDisplayName(uri) ?: "Imported_${System.currentTimeMillis()}.txt"
        val docType = DocumentType.fromFileName(originalName, mime)
        val baseTitle = originalName.substringBeforeLast('.').ifBlank { "Imported Document" }
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
        )
        val id = db.documentDao().insert(entity)
        entity.copy(id = id)
    }

    private fun inspectFileMetadata(file: File, type: DocumentType): Triple<Int, Int, String> {
        return runCatching {
            when (type) {
                DocumentType.PDF -> {
                    val p = PdfEngine.getPageCount(file).coerceAtLeast(1)
                    Triple(p, 0, "$p page PDF imported from device")
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
     * Universal Format Converter: converts an existing document into a new file of targetType!
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
    ) = withContext(Dispatchers.IO) {
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

    /**
     * Exports a document directly to a user-selected SAF URI (Save As / Export to Device).
     */
    suspend fun exportDocumentToUri(doc: DocumentEntity, destUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(doc.filePath)
        if (!srcFile.exists()) return@withContext false
        runCatching {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                FileInputStream(srcFile).use { input -> input.copyTo(out) }
            } != null
        }.getOrDefault(false)
    }

    /**
     * Builds a native Android Share Intent using FileProvider for sharing real files.
     */
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

    /**
     * REAL FULL BACKUP (.luminabak ZIP archive):
     * Exports metadata JSON (documents, annotations, bookmarks, settings) + every real file in documents/.
     */
    suspend fun exportBackupToUri(destUri: Uri): Int = withContext(Dispatchers.IO) {
        val docs = db.documentDao().getAllDocumentsOnce()
        val annotations = db.annotationDao().getAllOnce()
        val bookmarks = db.bookmarkDao().getAllOnce()
        val settings = prefs.getSettingsOnce()

        val rootJson = JSONObject().apply {
            put("app", "Lumina Docs")
            put("developer", "sky")
            put("backupVersion", 1)
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

    /**
     * REAL FULL RESTORE from a .luminabak ZIP archive.
     */
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

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        calculateDirSize(context.cacheDir)
    }

    suspend fun clearAppCache(): Long = withContext(Dispatchers.IO) {
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
