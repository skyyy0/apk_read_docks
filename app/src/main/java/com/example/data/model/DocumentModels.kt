package com.example.data.model

import androidx.compose.runtime.Immutable
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
    HTML("html", "HTML", "HTML5 Page", "text/html", DocumentCategory.NOTES),
    CSS("css", "CSS", "CSS3 Stylesheet", "text/css", DocumentCategory.NOTES),
    JS("js", "JS", "JavaScript", "application/javascript", DocumentCategory.NOTES),
    CODE("kt", "CODE", "Source Code", "text/plain", DocumentCategory.NOTES),
    TEXT("txt", "TXT", "Plain Text", "text/plain", DocumentCategory.NOTES);

    val isCodeFormat: Boolean
        get() = this == HTML || this == CSS || this == JS || this == JSON || this == CODE

    fun isCodeDocument(fileName: String): Boolean {
        if (isCodeFormat) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in CODE_EXTENSIONS
    }

    companion object {
        val CODE_EXTENSIONS = setOf(
            "html", "htm", "css", "scss", "sass", "less",
            "js", "jsx", "mjs", "cjs", "ts", "tsx",
            "json", "xml", "svg", "yaml", "yml",
            "kt", "kts", "java", "py", "sql", "sh", "bash",
            "php", "c", "cpp", "h", "hpp", "rs", "go", "dart", "rb", "ini", "toml",
        )

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
                "html", "htm", "xml", "svg" -> HTML
                "css", "scss", "sass", "less" -> CSS
                "js", "jsx", "mjs", "cjs", "ts", "tsx" -> JS
                "kt", "kts", "java", "py", "sql", "sh", "bash", "php", "c", "cpp", "h", "rs", "go", "dart", "rb", "yaml", "yml", "toml", "ini" -> CODE
                "txt", "log" -> TEXT
                else -> when {
                    mimeType?.contains("pdf", ignoreCase = true) == true -> PDF
                    mimeType?.contains("spreadsheet", ignoreCase = true) == true ||
                        mimeType?.contains("excel", ignoreCase = true) == true -> XLSX
                    mimeType?.contains("csv", ignoreCase = true) == true -> CSV
                    mimeType?.contains("wordprocessing", ignoreCase = true) == true ||
                        mimeType?.contains("msword", ignoreCase = true) == true -> DOCX
                    mimeType?.contains("presentation", ignoreCase = true) == true -> PPTX
                    mimeType?.contains("json", ignoreCase = true) == true -> JSON
                    mimeType?.contains("css", ignoreCase = true) == true -> CSS
                    mimeType?.contains("javascript", ignoreCase = true) == true -> JS
                    mimeType?.contains("html", ignoreCase = true) == true ||
                        mimeType?.contains("xml", ignoreCase = true) == true -> HTML
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

@Immutable
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

@Immutable
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

@Immutable
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val documentId: Long,
    val targetIndex: Int, // pageIndex or sheetIndex or blockIndex
    val label: String,
    val createdAt: Long = System.currentTimeMillis(),
)
