package com.example.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.model.DocumentType

enum class CodeLanguage(val badge: String, val displayLabel: String) {
    HTML("HTML", "HTML5"),
    CSS("CSS", "CSS3"),
    JAVASCRIPT("JS", "JavaScript / TS"),
    JSON("JSON", "JSON"),
    KOTLIN_JAVA("KT", "Kotlin / Java"),
    PYTHON("PY", "Python"),
    SQL("SQL", "SQL"),
    SHELL_YAML("CFG", "YAML / Shell"),
    PLAIN("CODE", "Plain Code");

    companion object {
        fun detect(fileName: String, docType: DocumentType): CodeLanguage {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "html", "htm", "xml", "svg" -> HTML
                "css", "scss", "sass", "less" -> CSS
                "js", "jsx", "mjs", "cjs", "ts", "tsx" -> JAVASCRIPT
                "json" -> JSON
                "kt", "kts", "java" -> KOTLIN_JAVA
                "py" -> PYTHON
                "sql" -> SQL
                "yaml", "yml", "sh", "bash", "toml", "ini" -> SHELL_YAML
                else -> when (docType) {
                    DocumentType.HTML -> HTML
                    DocumentType.CSS -> CSS
                    DocumentType.JS -> JAVASCRIPT
                    DocumentType.JSON -> JSON
                    DocumentType.CODE -> KOTLIN_JAVA
                    else -> PLAIN
                }
            }
        }
    }
}

data class CodeSymbol(
    val name: String,
    val kind: String,
    val line: Int,
)

class VsCodePalette(darkTheme: Boolean) {
    val background: Color = if (darkTheme) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val panelSurface: Color = if (darkTheme) Color(0xFF252526) else Color(0xFFF3F3F3)
    val tabBar: Color = if (darkTheme) Color(0xFF2D2D2D) else Color(0xFFECECEC)
    val activeLine: Color = if (darkTheme) Color(0xFF2A2D2E) else Color(0xFFF5F7FA)
    val gutterText: Color = if (darkTheme) Color(0xFF858585) else Color(0xFF6E7681)
    val defaultText: Color = if (darkTheme) Color(0xFFD4D4D4) else Color(0xFF1F2328)
    val accentBlue: Color = Color(0xFF007ACC)
    val keyword: Color = if (darkTheme) Color(0xFF569CD6) else Color(0xFF0000FF)
    val controlFlow: Color = if (darkTheme) Color(0xFFC586C0) else Color(0xFFAF00DB)
    val functionName: Color = if (darkTheme) Color(0xFFDCDCAA) else Color(0xFF795E26)
    val stringLiteral: Color = if (darkTheme) Color(0xFFCE9178) else Color(0xFFA31515)
    val numberLiteral: Color = if (darkTheme) Color(0xFFB5CEA8) else Color(0xFF098658)
    val comment: Color = if (darkTheme) Color(0xFF6A9955) else Color(0xFF008000)
    val tag: Color = if (darkTheme) Color(0xFF569CD6) else Color(0xFF800000)
    val attribute: Color = if (darkTheme) Color(0xFF9CDCFE) else Color(0xFFE50000)
    val property: Color = if (darkTheme) Color(0xFF9CDCFE) else Color(0xFF0451A5)
    val bracket: Color = if (darkTheme) Color(0xFFFFD700) else Color(0xFF0431FA)
}

object CodeSyntaxHighlighter {

    private val htmlTagRegex = Regex("</?([A-Za-z][A-Za-z0-9:-]*)")
    private val htmlAttrRegex = Regex("\\s([A-Za-z_:][A-Za-z0-9_.:-]*)\\s*=")
    private val cssPropertyRegex = Regex("([a-z-]+)\\s*(?=:)")
    private val cssSelectorRegex = Regex("([.#@][A-Za-z_][A-Za-z0-9_-]*)")
    private val cssColorRegex = Regex("#[0-9A-Fa-f]{3,8}\\b")
    private val stringRegex = Regex("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`")
    private val commentRegex = Regex("//[^\\n]*|/\\*[\\s\\S]*?\\*/|<!--([\\s\\S]*?)-->|#[^\\n]*")
    private val numberRegex = Regex("\\b\\d+(\\.\\d+)?(px|rem|em|vh|vw|%|ms|s)?\\b")
    private val fnCallRegex = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\()")
    private val jsonKeyRegex = Regex("\"([^\"]+)\"\\s*(?=:)")

    private val jsControlWords = Regex("\\b(if|else|for|while|do|switch|case|break|continue|return|try|catch|finally|throw|import|export|from|default|await|async|yield)\\b")
    private val jsDeclWords = Regex("\\b(const|let|var|function|class|extends|new|this|super|typeof|instanceof|true|false|null|undefined|void|interface|type|enum|def|lambda|SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|JOIN|ON|GROUP|BY|ORDER|CREATE|TABLE|fun|val|object|package|public|private|override)\\b")

    fun createVisualTransformation(
        language: CodeLanguage,
        palette: VsCodePalette,
    ): VisualTransformation = VisualTransformation { text ->
        TransformedText(
            text = highlight(text.text, language, palette),
            offsetMapping = OffsetMapping.Identity,
        )
    }

    fun highlight(
        code: String,
        language: CodeLanguage,
        palette: VsCodePalette,
    ): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        // Avoid heavy regex passes on very huge files so typing stays instantaneous
        val safeTarget = if (code.length > 48_000) code.substring(0, 48_000) else code

        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = palette.defaultText), 0, code.length)

            when (language) {
                CodeLanguage.HTML -> {
                    htmlTagRegex.findAll(safeTarget).forEach { m ->
                        val g = m.groups[1] ?: return@forEach
                        addStyle(SpanStyle(color = palette.tag, fontWeight = FontWeight.Medium), g.range.first, g.range.last + 1)
                    }
                    htmlAttrRegex.findAll(safeTarget).forEach { m ->
                        val g = m.groups[1] ?: return@forEach
                        addStyle(SpanStyle(color = palette.attribute), g.range.first, g.range.last + 1)
                    }
                    applyMatches(stringRegex, safeTarget, SpanStyle(color = palette.stringLiteral))
                    applyMatches(Regex("<!--([\\s\\S]*?)-->"), safeTarget, SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic))
                }

                CodeLanguage.CSS -> {
                    applyMatches(cssSelectorRegex, safeTarget, SpanStyle(color = palette.functionName, fontWeight = FontWeight.Medium))
                    cssPropertyRegex.findAll(safeTarget).forEach { m ->
                        val g = m.groups[1] ?: return@forEach
                        addStyle(SpanStyle(color = palette.property), g.range.first, g.range.last + 1)
                    }
                    applyMatches(numberRegex, safeTarget, SpanStyle(color = palette.numberLiteral))
                    applyMatches(cssColorRegex, safeTarget, SpanStyle(color = palette.stringLiteral, fontWeight = FontWeight.SemiBold))
                    applyMatches(stringRegex, safeTarget, SpanStyle(color = palette.stringLiteral))
                    applyMatches(Regex("/\\*[\\s\\S]*?\\*/"), safeTarget, SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic))
                }

                CodeLanguage.JSON -> {
                    applyMatches(numberRegex, safeTarget, SpanStyle(color = palette.numberLiteral))
                    applyMatches(Regex("\\b(true|false|null)\\b"), safeTarget, SpanStyle(color = palette.keyword, fontWeight = FontWeight.Medium))
                    applyMatches(stringRegex, safeTarget, SpanStyle(color = palette.stringLiteral))
                    jsonKeyRegex.findAll(safeTarget).forEach { m ->
                        addStyle(SpanStyle(color = palette.property, fontWeight = FontWeight.Medium), m.range.first, m.range.last + 1)
                    }
                }

                else -> {
                    applyMatches(numberRegex, safeTarget, SpanStyle(color = palette.numberLiteral))
                    fnCallRegex.findAll(safeTarget).forEach { m ->
                        val g = m.groups[1] ?: return@forEach
                        addStyle(SpanStyle(color = palette.functionName), g.range.first, g.range.last + 1)
                    }
                    applyMatches(jsDeclWords, safeTarget, SpanStyle(color = palette.keyword, fontWeight = FontWeight.Medium))
                    applyMatches(jsControlWords, safeTarget, SpanStyle(color = palette.controlFlow, fontWeight = FontWeight.Medium))
                    applyMatches(stringRegex, safeTarget, SpanStyle(color = palette.stringLiteral))
                    val cmt = if (language == CodeLanguage.PYTHON || language == CodeLanguage.SHELL_YAML) {
                        Regex("#[^\\n]*")
                    } else {
                        Regex("//[^\\n]*|/\\*[\\s\\S]*?\\*/")
                    }
                    applyMatches(cmt, safeTarget, SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic))
                }
            }
        }
    }

    private fun AnnotatedString.Builder.applyMatches(
        regex: Regex,
        target: String,
        style: SpanStyle,
    ) {
        regex.findAll(target).forEach { m ->
            if (m.range.first <= m.range.last && m.range.last < target.length) {
                addStyle(style, m.range.first, m.range.last + 1)
            }
        }
    }

    fun extractSymbols(code: String, language: CodeLanguage): List<CodeSymbol> {
        val symbols = mutableListOf<CodeSymbol>()
        val lines = code.lines()
        lines.forEachIndexed { idx, raw ->
            val line = raw.trim()
            val lineNum = idx + 1
            when (language) {
                CodeLanguage.HTML -> {
                    Regex("<([A-Za-z][A-Za-z0-9-]*)[^>]*(id|class)=[\"']([^\"']+)[\"']").find(line)?.let { m ->
                        symbols.add(CodeSymbol("<${m.groupValues[1]}> ${m.groupValues[2]}=\"${m.groupValues[3]}\"", "TAG", lineNum))
                    }
                    Regex("<(script|style|head|body|header|main|section|footer)\\b").find(line)?.let { m ->
                        if (symbols.none { it.line == lineNum }) {
                            symbols.add(CodeSymbol("<${m.groupValues[1]}>", "SECTION", lineNum))
                        }
                    }
                }
                CodeLanguage.CSS -> {
                    if (line.endsWith("{") && !line.startsWith("/*")) {
                        val sel = line.removeSuffix("{").trim()
                        if (sel.isNotBlank()) symbols.add(CodeSymbol(sel, "SELECTOR", lineNum))
                    }
                }
                CodeLanguage.JSON -> {
                    Regex("^\"([^\"]+)\"\\s*:\\s*[{\\[]").find(line)?.let { m ->
                        symbols.add(CodeSymbol(m.groupValues[1], "OBJECT", lineNum))
                    }
                }
                else -> {
                    Regex("(function|class|interface|fun|def|CREATE TABLE)\\s+([A-Za-z_][A-Za-z0-9_]*)").find(line)?.let { m ->
                        symbols.add(CodeSymbol(m.groupValues[2], m.groupValues[1].uppercase(), lineNum))
                    }
                    Regex("(const|let|var)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(async\\s*)?(\\(|function)").find(line)?.let { m ->
                        symbols.add(CodeSymbol(m.groupValues[2], "FN", lineNum))
                    }
                }
            }
        }
        return symbols.take(60)
    }

    fun formatCode(code: String, language: CodeLanguage): String {
        return when (language) {
            CodeLanguage.JSON -> {
                val res = TextDocumentEngine.validateAndFormatJson(code)
                if (res is JsonValidationResult.Valid) res.formatted else code
            }
            CodeLanguage.HTML -> formatMarkup(code)
            else -> formatBraceCode(code)
        }
    }

    private fun formatMarkup(raw: String): String {
        val compact = raw.replace(Regex(">\\s+<"), ">\n<")
        val sb = StringBuilder()
        var indent = 0
        for (rawLine in compact.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val isClosing = line.startsWith("</")
            if (isClosing) indent = (indent - 1).coerceAtLeast(0)
            sb.append("  ".repeat(indent)).append(line).append('\n')
            val isOpening = line.startsWith("<") &&
                !isClosing &&
                !line.startsWith("<!") &&
                !line.startsWith("<?") &&
                !line.endsWith("/>") &&
                !Regex("<(meta|link|br|hr|img|input)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) &&
                !line.contains("</")
            if (isOpening) indent++
        }
        return sb.toString().trimEnd() + "\n"
    }

    private fun formatBraceCode(raw: String): String {
        val sb = StringBuilder()
        var indent = 0
        for (rawLine in raw.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                sb.append('\n')
                continue
            }
            if (line.startsWith("}") || line.startsWith("]") || line.startsWith(")")) {
                indent = (indent - 1).coerceAtLeast(0)
            }
            sb.append("  ".repeat(indent)).append(line).append('\n')
            val openCount = line.count { it == '{' }
            val closeCount = line.count { it == '}' }
            val delta = openCount - closeCount
            if (delta > 0 && !line.startsWith("}")) {
                indent += delta
            } else if (delta > 0 && line.startsWith("}")) {
                indent += delta
            }
        }
        return sb.toString().trimEnd() + "\n"
    }
}
