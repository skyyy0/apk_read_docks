package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DocumentType(
    val extension: String,
    val shortBadge: String,
    val displayName: String,
    val mimeType: String,
    val category: DocumentCategory,
) {
    PDF("pdf", "PDF", "PDF Document", "application/pdf", DocumentCategory.PDF),
    XLSX(
        "xlsx",
        "XLS",
        "Excel Workbook",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        DocumentCategory.SPREADSHEET,
    ),
    CSV("csv", "CSV", "CSV Table", "text/csv", DocumentCategory.SPREADSHEET),
    DOCX(
        "docx",
        "DOC",
        "Word Document",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        DocumentCategory.WORD,
    ),
    PPTX(
        "pptx",
        "PPT",
        "Presentation Deck",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        DocumentCategory.SLIDES,
    ),
    MARKDOWN("md", "MD", "Markdown Note", "text/markdown", DocumentCategory.NOTES),
    JSON("json", "JSON", "JSON Data", "application/json", DocumentCategory.NOTES),
    HTML("html", "HTML", "HTML Page", "text/html", DocumentCategory.NOTES),
    TEXT("txt", "TXT", "Plain Text", "text/plain", DocumentCategory.NOTES);

    companion object {
        fun fromFileName(fileName: String, mimeType: String? = null): DocumentType {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "pdf" -> PDF
                "xlsx", "xlsm" -> XLSX
                "csv", "tsv" -> CSV
                "docx", "doc" -> DOCX
                "pptx", "ppt" -> PPTX
                "md", "markdown" -> MARKDOWN
                "json" -> JSON
                "html", "htm", "xml" -> HTML
                "txt", "log", "ini", "yaml", "yml", "kt", "java", "py", "js", "ts" -> TEXT
                else -> when {
                    mimeType?.contains("pdf", ignoreCase = true) == true -> PDF
                    mimeType?.contains("spreadsheet", ignoreCase = true) == true ||
                        mimeType?.contains("excel", ignoreCase = true) == true -> XLSX
                    mimeType?.contains("csv", ignoreCase = true) == true -> CSV
                    mimeType?.contains("wordprocessing", ignoreCase = true) == true ||
                        mimeType?.contains("msword", ignoreCase = true) == true -> DOCX
                    mimeType?.contains("presentation", ignoreCase = true) == true -> PPTX
                    mimeType?.contains("json", ignoreCase = true) == true -> JSON
                    mimeType?.contains("html", ignoreCase = true) == true -> HTML
                    mimeType?.contains("markdown", ignoreCase = true) == true -> MARKDOWN
                    else -> TEXT
                }
            }
        }
    }
}

enum class DocumentCategory(val label: String) {
    ALL("All"),
    PDF("PDF"),
    SPREADSHEET("Excel"),
    WORD("Word"),
    SLIDES("Slides"),
    NOTES("Notes"),
}

enum class SortOrder(val label: String) {
    RECENT("Recent"),
    NAME("Name"),
    SIZE("Size"),
    TYPE("Format"),
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val fileName: String,
    val filePath: String,
    val type: String, // DocumentType.name
    val sizeBytes: Long = 0L,
    val folderName: String = "Workspace",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val pageCount: Int = 1,
    val wordCount: Int = 0,
    val previewSnippet: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val sourceUri: String? = null,
) {
    val docType: DocumentType
        get() = runCatching { DocumentType.valueOf(type) }.getOrDefault(DocumentType.TEXT)
}

@Entity(tableName = "pdf_annotations")
data class PdfAnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val documentId: Long,
    val pageIndex: Int,
    val kind: String, // "TEXT", "HIGHLIGHT", "NOTE"
    val content: String,
    val normX: Float, // 0f..1f relative page X
    val normY: Float, // 0f..1f relative page Y
    val colorHex: Long = 0xFF007AFF,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val documentId: Long,
    val targetIndex: Int, // pageIndex or sheetIndex or blockIndex
    val label: String,
    val createdAt: Long = System.currentTimeMillis(),
)
