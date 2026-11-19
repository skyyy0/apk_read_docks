package com.example.engine

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max

data class SheetData(
    val name: String,
    val rows: List<List<String>>,
) {
    val rowCount: Int get() = rows.size
    val colCount: Int get() = rows.maxOfOrNull { it.size } ?: 0

    fun cellAt(row: Int, col: Int): String =
        rows.getOrNull(row)?.getOrNull(col) ?: ""

    fun withCell(row: Int, col: Int, value: String): SheetData {
        val targetRows = max(rows.size, row + 1)
        val targetCols = max(colCount, col + 1)
        val updated = MutableList(targetRows) { r ->
            val existing = rows.getOrNull(r) ?: emptyList()
            MutableList(targetCols) { c -> existing.getOrNull(c) ?: "" }
        }
        updated[row][col] = value
        return copy(rows = updated)
    }

    fun withAddedRow(): SheetData {
        val cols = max(colCount, 6)
        return copy(rows = rows + listOf(List(cols) { "" }))
    }

    fun withAddedColumn(): SheetData {
        val updated = if (rows.isEmpty()) {
            listOf(List(6) { "" })
        } else {
            rows.map { it + "" }
        }
        return copy(rows = updated)
    }

    fun withDeletedRow(index: Int): SheetData {
        if (rows.size <= 1 || index !in rows.indices) return this
        return copy(rows = rows.filterIndexed { i, _ -> i != index })
    }

    fun withDeletedColumn(colIndex: Int): SheetData {
        if (colCount <= 1 || colIndex < 0 || colIndex >= colCount) return this
        return copy(rows = rows.map { row -> row.filterIndexed { c, _ -> c != colIndex } })
    }
}

data class WorkbookData(
    val sheets: List<SheetData>,
)

object SpreadsheetEngine {

    fun columnIndexToName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    fun columnNameToIndex(name: String): Int {
        var result = 0
        for (ch in name.uppercase(Locale.US)) {
            if (ch in 'A'..'Z') {
                result = result * 26 + (ch - 'A' + 1)
            }
        }
        return (result - 1).coerceAtLeast(0)
    }

    fun parseCellRef(ref: String): Pair<Int, Int>? {
        val match = Regex("^([A-Za-z]+)(\\d+)$").matchEntire(ref.trim()) ?: return null
        val col = columnNameToIndex(match.groupValues[1])
        val row = (match.groupValues[2].toIntOrNull() ?: return null) - 1
        if (row < 0 || col < 0) return null
        return row to col
    }

    /**
     * Live Excel-compatible formula evaluator supporting SUM, AVERAGE, MIN, MAX, COUNT, IF,
     * and arithmetic expressions (+, -, *, /, parentheses, cell references).
     */
    fun evaluateCell(
        sheet: SheetData,
        row: Int,
        col: Int,
        visited: MutableSet<Pair<Int, Int>> = mutableSetOf(),
    ): String {
        val raw = sheet.cellAt(row, col).trim()
        if (!raw.startsWith("=")) return raw
        val key = row to col
        if (!visited.add(key)) return "#CYCLE!"
        return try {
            val expr = raw.substring(1).trim()
            val evaluated = evaluateFormulaExpression(expr, sheet, visited)
            formatNumberOrString(evaluated)
        } catch (_: Exception) {
            "#ERR"
        } finally {
            visited.remove(key)
        }
    }

    private fun evaluateFormulaExpression(
        expr: String,
        sheet: SheetData,
        visited: MutableSet<Pair<Int, Int>>,
    ): Any {
        val upper = expr.uppercase(Locale.US)

        // Handle IF(cond, trueVal, falseVal)
        if (upper.startsWith("IF(") && expr.endsWith(")")) {
            val inner = expr.substring(3, expr.length - 1)
            val parts = splitTopLevelArgs(inner)
            if (parts.size == 3) {
                val condResult = evaluateCondition(parts[0], sheet, visited)
                val branch = if (condResult) parts[1].trim() else parts[2].trim()
                if (branch.startsWith("\"") && branch.endsWith("\"") && branch.length >= 2) {
                    return branch.substring(1, branch.length - 1)
                }
                return evaluateFormulaExpression(branch, sheet, visited)
            }
        }

        // Replace aggregate functions SUM/AVERAGE/AVG/MIN/MAX/COUNT
        val fnRegex = Regex("(SUM|AVERAGE|AVG|MIN|MAX|COUNT)\\(([^()]+)\\)", RegexOption.IGNORE_CASE)
        var resolved = expr
        var guard = 0
        while (fnRegex.containsMatchIn(resolved) && guard < 16) {
            guard++
            resolved = fnRegex.replace(resolved) { m ->
                val fnName = m.groupValues[1].uppercase(Locale.US)
                val argsText = m.groupValues[2]
                val nums = collectNumbersFromArgs(argsText, sheet, visited)
                val value = when (fnName) {
                    "SUM" -> nums.sum()
                    "AVERAGE", "AVG" -> if (nums.isEmpty()) 0.0 else nums.sum() / nums.size
                    "MIN" -> nums.minOrNull() ?: 0.0
                    "MAX" -> nums.maxOrNull() ?: 0.0
                    "COUNT" -> nums.size.toDouble()
                    else -> 0.0
                }
                value.toString()
            }
        }

        // Replace individual cell references like B2, AA12
        val cellRegex = Regex("\\b([A-Za-z]{1,3})(\\d{1,5})\\b")
        resolved = cellRegex.replace(resolved) { m ->
            val col = columnNameToIndex(m.groupValues[1])
            val row = (m.groupValues[2].toIntOrNull() ?: 1) - 1
            val cellVal = evaluateCell(sheet, row, col, visited)
            parseNumeric(cellVal)?.toString() ?: "0.0"
        }

        return SimpleMathParser(resolved).parse()
    }

    private fun evaluateCondition(
        cond: String,
        sheet: SheetData,
        visited: MutableSet<Pair<Int, Int>>,
    ): Boolean {
        val ops = listOf(">=", "<=", "!=", "<>", "=", ">", "<")
        for (op in ops) {
            val idx = cond.indexOf(op)
            if (idx > 0) {
                val left = evaluateFormulaExpression(cond.substring(0, idx).trim(), sheet, visited)
                val right = evaluateFormulaExpression(cond.substring(idx + op.length).trim(), sheet, visited)
                val lNum = (left as? Number)?.toDouble() ?: parseNumeric(left.toString())
                val rNum = (right as? Number)?.toDouble() ?: parseNumeric(right.toString())
                if (lNum != null && rNum != null) {
                    return when (op) {
                        ">=" -> lNum >= rNum
                        "<=" -> lNum <= rNum
                        "!=", "<>" -> lNum != rNum
                        "=" -> lNum == rNum
                        ">" -> lNum > rNum
                        "<" -> lNum < rNum
                        else -> false
                    }
                }
                return when (op) {
                    "=" -> left.toString() == right.toString()
                    "!=", "<>" -> left.toString() != right.toString()
                    else -> false
                }
            }
        }
        return false
    }

    private fun collectNumbersFromArgs(
        argsText: String,
        sheet: SheetData,
        visited: MutableSet<Pair<Int, Int>>,
    ): List<Double> {
        val result = mutableListOf<Double>()
        for (token in argsText.split(',', ';')) {
            val part = token.trim()
            if (part.contains(':')) {
                val bounds = part.split(':')
                if (bounds.size == 2) {
                    val start = parseCellRef(bounds[0])
                    val end = parseCellRef(bounds[1])
                    if (start != null && end != null) {
                        val r0 = minOf(start.first, end.first)
                        val r1 = maxOf(start.first, end.first)
                        val c0 = minOf(start.second, end.second)
                        val c1 = maxOf(start.second, end.second)
                        for (r in r0..r1) {
                            for (c in c0..c1) {
                                parseNumeric(evaluateCell(sheet, r, c, visited))?.let { result.add(it) }
                            }
                        }
                    }
                }
            } else {
                val cellRef = parseCellRef(part)
                if (cellRef != null) {
                    parseNumeric(evaluateCell(sheet, cellRef.first, cellRef.second, visited))?.let { result.add(it) }
                } else {
                    parseNumeric(part)?.let { result.add(it) }
                }
            }
        }
        return result
    }

    private fun splitTopLevelArgs(input: String): List<String> {
        val args = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var inQuote = false
        for (ch in input) {
            when {
                ch == '"' -> {
                    inQuote = !inQuote
                    cur.append(ch)
                }
                !inQuote && ch == '(' -> {
                    depth++
                    cur.append(ch)
                }
                !inQuote && ch == ')' -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    cur.append(ch)
                }
                !inQuote && depth == 0 && ch == ',' -> {
                    args.add(cur.toString())
                    cur.clear()
                }
                else -> cur.append(ch)
            }
        }
        args.add(cur.toString())
        return args
    }

    private fun parseNumeric(raw: String): Double? {
        val cleaned = raw.trim()
            .replace("$", "")
            .replace("Rp", "", ignoreCase = true)
            .replace("€", "")
            .replace(",", "")
            .trim()
        return cleaned.toDoubleOrNull()
    }

    private fun formatNumberOrString(value: Any): String {
        if (value is Double) {
            if (value.isNaN() || value.isInfinite()) return "#NUM!"
            return if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            }
        }
        return value.toString()
    }

    private class SimpleMathParser(private val s: String) {
        private var pos = 0

        fun parse(): Double {
            val v = parseExpr()
            return v
        }

        private fun skipSpaces() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        private fun parseExpr(): Double {
            var left = parseTerm()
            while (true) {
                skipSpaces()
                if (pos < s.length && s[pos] == '+') {
                    pos++
                    left += parseTerm()
                } else if (pos < s.length && s[pos] == '-') {
                    pos++
                    left -= parseTerm()
                } else {
                    break
                }
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parseFactor()
            while (true) {
                skipSpaces()
                if (pos < s.length && s[pos] == '*') {
                    pos++
                    left *= parseFactor()
                } else if (pos < s.length && s[pos] == '/') {
                    pos++
                    val right = parseFactor()
                    left = if (right == 0.0) Double.NaN else left / right
                } else {
                    break
                }
            }
            return left
        }

        private fun parseFactor(): Double {
            skipSpaces()
            if (pos < s.length && s[pos] == '+') {
                pos++
                return parseFactor()
            }
            if (pos < s.length && s[pos] == '-') {
                pos++
                return -parseFactor()
            }
            if (pos < s.length && s[pos] == '(') {
                pos++
                val v = parseExpr()
                skipSpaces()
                if (pos < s.length && s[pos] == ')') pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) return 0.0
            return s.substring(start, pos).toDoubleOrNull() ?: 0.0
        }
    }

    // --- Real XLSX Reader (OOXML ZIP + XML) ---
    fun readXlsx(file: File): WorkbookData {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(FileInputStream(file)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val baos = ByteArrayOutputStream()
                    zis.copyTo(baos)
                    entries[entry.name] = baos.toByteArray()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetNames = entries["xl/workbook.xml"]?.let { parseWorkbookSheetNames(it) } ?: emptyList()

        val sheetEntryKeys = entries.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sortedBy { key ->
                Regex("sheet(\\d+)\\.xml").find(key)?.groupValues?.get(1)?.toIntOrNull() ?: 999
            }

        val sheets = sheetEntryKeys.mapIndexed { idx, key ->
            val name = sheetNames.getOrNull(idx) ?: "Sheet ${idx + 1}"
            val rows = parseWorksheetXml(entries[key]!!, sharedStrings)
            SheetData(name = name, rows = normalizeGrid(rows, minRows = 12, minCols = 6))
        }

        return WorkbookData(
            sheets = sheets.ifEmpty {
                listOf(SheetData("Sheet 1", List(14) { List(6) { "" } }))
            }
        )
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var inSi = false
        val current = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            current.clear()
                        }
                        "t" -> if (inSi) current.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        strings.add(current.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return strings
    }

    private fun parseWorkbookSheetNames(xmlBytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name")
                if (!name.isNullOrBlank()) names.add(name)
            }
            event = parser.next()
        }
        return names
    }

    private fun parseWorksheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val cellMap = mutableMapOf<Int, MutableMap<Int, String>>()
        var maxRow = 0
        var maxCol = 0

        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var currentRow = -1
        var currentCol = -1
        var currentType: String? = null
        var formulaText: String? = null
        var valueText: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r")
                            currentType = parser.getAttributeValue(null, "t")
                            formulaText = null
                            valueText = null
                            val parsed = ref?.let { parseCellRef(it) }
                            if (parsed != null) {
                                currentRow = parsed.first
                                currentCol = parsed.second
                            } else {
                                currentCol++
                            }
                        }
                        "f" -> formulaText = parser.nextText()
                        "v" -> valueText = parser.nextText()
                        "t" -> {
                            if (currentType == "inlineStr") {
                                valueText = (valueText ?: "") + parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "c" && currentRow >= 0 && currentCol >= 0) {
                        val finalVal = when {
                            !formulaText.isNullOrBlank() -> "=$formulaText"
                            currentType == "s" -> {
                                val idx = valueText?.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                            }
                            else -> valueText ?: ""
                        }
                        cellMap.getOrPut(currentRow) { mutableMapOf() }[currentCol] = finalVal
                        maxRow = max(maxRow, currentRow)
                        maxCol = max(maxCol, currentCol)
                    }
                }
            }
            event = parser.next()
        }

        if (cellMap.isEmpty()) return emptyList()
        return List(maxRow + 1) { r ->
            val rowData = cellMap[r]
            List(maxCol + 1) { c -> rowData?.get(c) ?: "" }
        }
    }

    // --- Real XLSX Writer (Valid OOXML compatible with Microsoft Excel & Google Sheets) ---
    fun writeXlsx(workbook: WorkbookData, outputFile: File) {
        val sheets = workbook.sheets.ifEmpty { listOf(SheetData("Sheet 1", List(10) { List(6) { "" } })) }
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            fun addEntry(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val sheetOverrides = sheets.indices.joinToString("\n") { i ->
                """  <Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
            }
            addEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$sheetOverrides
</Types>""",
            )

            addEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""",
            )

            val sheetTags = sheets.mapIndexed { idx, s ->
                """    <sheet name="${escapeXml(s.name)}" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>"""
            }.joinToString("\n")
            addEntry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
$sheetTags
  </sheets>
</workbook>""",
            )

            val relTags = sheets.indices.joinToString("\n") { idx ->
                """  <Relationship Id="rId${idx + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${idx + 1}.xml"/>"""
            } + """
  <Relationship Id="rId${sheets.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>"""
            addEntry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$relTags
</Relationships>""",
            )

            addEntry(
                "xl/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>""",
            )

            sheets.forEachIndexed { idx, sheet ->
                val sb = StringBuilder()
                sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                sb.append("\n<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>\n")
                sheet.rows.forEachIndexed { rIdx, row ->
                    val hasContent = row.any { it.isNotEmpty() }
                    if (hasContent) {
                        sb.append("<row r=\"${rIdx + 1}\">")
                        row.forEachIndexed { cIdx, rawVal ->
                            if (rawVal.isNotEmpty()) {
                                val ref = "${columnIndexToName(cIdx)}${rIdx + 1}"
                                val trimmed = rawVal.trim()
                                when {
                                    trimmed.startsWith("=") -> {
                                        val eval = evaluateCell(sheet, rIdx, cIdx)
                                        val evalNum = eval.toDoubleOrNull()
                                        sb.append("<c r=\"$ref\"><f>${escapeXml(trimmed.substring(1))}</f>")
                                        if (evalNum != null) sb.append("<v>$evalNum</v>")
                                        sb.append("</c>")
                                    }
                                    trimmed.toDoubleOrNull() != null -> {
                                        sb.append("<c r=\"$ref\"><v>$trimmed</v></c>")
                                    }
                                    else -> {
                                        sb.append("<c r=\"$ref\" t=\"inlineStr\"><is><t>${escapeXml(rawVal)}</t></is></c>")
                                    }
                                }
                            }
                        }
                        sb.append("</row>\n")
                    }
                }
                sb.append("</sheetData></worksheet>")
                addEntry("xl/worksheets/sheet${idx + 1}.xml", sb.toString())
            }
        }
    }

    // --- Real CSV / TSV Reader & Writer ---
    fun readCsv(file: File): WorkbookData {
        val text = file.readText(Charsets.UTF_8)
        val delimiter = if (file.extension.equals("tsv", ignoreCase = true)) '\t' else ','
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> {
                    if (ch == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            cell.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        cell.append(ch)
                    }
                }
                ch == '"' -> inQuotes = true
                ch == delimiter -> {
                    currentRow.add(cell.toString())
                    cell.clear()
                }
                ch == '\r' -> Unit
                ch == '\n' -> {
                    currentRow.add(cell.toString())
                    cell.clear()
                    rows.add(currentRow.toList())
                    currentRow.clear()
                }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(cell.toString())
            rows.add(currentRow.toList())
        }
        return WorkbookData(
            listOf(
                SheetData(
                    name = file.nameWithoutExtension.ifBlank { "Data" },
                    rows = normalizeGrid(rows, minRows = 14, minCols = 6),
                )
            )
        )
    }

    fun writeCsv(workbook: WorkbookData, outputFile: File) {
        val sheet = workbook.sheets.firstOrNull() ?: return
        val sb = StringBuilder()
        sheet.rows.forEachIndexed { rIdx, row ->
            val trimmedRow = row.dropLastWhile { it.isEmpty() }
            if (trimmedRow.isNotEmpty() || rIdx == 0) {
                sb.append(
                    trimmedRow.joinToString(",") { raw ->
                        if (raw.contains(',') || raw.contains('"') || raw.contains('\n')) {
                            "\"${raw.replace("\"", "\"\"")}\""
                        } else {
                            raw
                        }
                    }
                )
                sb.append('\n')
            }
        }
        outputFile.writeText(sb.toString(), Charsets.UTF_8)
    }

    // --- Export Workbook directly to Real Multi-Page PDF Report ---
    fun exportWorkbookToPdf(title: String, workbook: WorkbookData, outputFile: File) {
        val pdf = PdfDocument()
        val pageWidth = 842 // A4 Landscape for crisp spreadsheet tables
        val pageHeight = 595
        val margin = 36f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
        }
        val headerBgPaint = Paint().apply { color = Color.rgb(239, 246, 255) }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
        }

        var pageNumber = 1
        for (sheet in workbook.sheets) {
            val nonEmptyRows = sheet.rows.dropLastWhile { row -> row.all { it.isBlank() } }
            val activeCols = (nonEmptyRows.maxOfOrNull { r ->
                val lastIdx = r.indexOfLast { it.isNotBlank() }
                if (lastIdx >= 0) lastIdx + 1 else 0
            } ?: 4).coerceIn(1, 10)

            val tableWidth = pageWidth - margin * 2
            val colWidth = tableWidth / activeCols
            val rowHeight = 24f
            val rowsPerPage = ((pageHeight - 120f) / rowHeight).toInt().coerceAtLeast(1)

            val totalRows = max(1, nonEmptyRows.size)
            var startRow = 0
            while (startRow < totalRows) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawText(title, margin, margin + 14f, titlePaint)
                canvas.drawText("Sheet: ${sheet.name}  •  Lumina Docs Export", margin, margin + 30f, subtitlePaint)

                var y = margin + 46f
                val endRow = minOf(startRow + rowsPerPage, nonEmptyRows.size)
                for (r in startRow until endRow) {
                    val isHeader = r == 0
                    if (isHeader) {
                        canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, headerBgPaint)
                    }
                    for (c in 0 until activeCols) {
                        val x = margin + c * colWidth
                        canvas.drawRect(x, y, x + colWidth, y + rowHeight, borderPaint)
                        val displayVal = evaluateCell(sheet, r, c)
                        val paint = if (isHeader) headerTextPaint else cellTextPaint
                        val clipped = ellipsizeText(displayVal, paint, colWidth - 10f)
                        canvas.drawText(clipped, x + 5f, y + 16f, paint)
                    }
                    y += rowHeight
                }
                pdf.finishPage(page)
                startRow = endRow
                if (nonEmptyRows.isEmpty()) break
            }
        }

        FileOutputStream(outputFile).use { fos -> pdf.writeTo(fos) }
        pdf.close()
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }

    private fun normalizeGrid(rows: List<List<String>>, minRows: Int, minCols: Int): List<List<String>> {
        val targetRows = max(rows.size, minRows)
        val targetCols = max(rows.maxOfOrNull { it.size } ?: 0, minCols)
        return List(targetRows) { r ->
            val src = rows.getOrNull(r) ?: emptyList()
            List(targetCols) { c -> src.getOrNull(c) ?: "" }
        }
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
