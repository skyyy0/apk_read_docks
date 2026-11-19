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
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class DocBlockStyle(val label: String) {
    HEADING1("H1"),
    HEADING2("H2"),
    HEADING3("H3"),
    BODY("Body"),
    BULLET("Bullet"),
    CHECKLIST("Task"),
    QUOTE("Quote"),
    CODE("Code"),
}

data class DocBlock(
    val id: String = UUID.randomUUID().toString(),
    val style: DocBlockStyle = DocBlockStyle.BODY,
    val text: String = "",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isChecked: Boolean = false,
)

data class RichDocument(
    val blocks: List<DocBlock>,
) {
    val wordCount: Int
        get() = blocks.sumOf { block ->
            block.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        }

    val charCount: Int
        get() = blocks.sumOf { it.text.length }

    val estimatedReadMinutes: Int
        get() = ((wordCount + 199) / 200).coerceAtLeast(1)
}

object WordDocumentEngine {

    /**
     * Reads a REAL .docx OOXML archive by parsing word/document.xml natively.
     */
    fun readDocx(file: File): RichDocument {
        var documentXml: ByteArray? = null
        ZipInputStream(FileInputStream(file)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val baos = ByteArrayOutputStream()
                    zis.copyTo(baos)
                    documentXml = baos.toByteArray()
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val xmlBytes = documentXml ?: return RichDocument(
            listOf(DocBlock(style = DocBlockStyle.BODY, text = runCatching { file.readText() }.getOrDefault("")))
        )

        val blocks = mutableListOf<DocBlock>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var inParagraph = false
        val paraText = StringBuilder()
        var pStyle = ""
        var hasNumPr = false
        var hasBold = false
        var hasItalic = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "p" -> {
                            inParagraph = true
                            paraText.clear()
                            pStyle = ""
                            hasNumPr = false
                            hasBold = false
                            hasItalic = false
                        }
                        "pStyle" -> {
                            if (inParagraph) {
                                pStyle = parser.getAttributeValue(null, "val")
                                    ?: (0 until parser.attributeCount)
                                        .firstOrNull { parser.getAttributeName(it).endsWith("val") }
                                        ?.let { parser.getAttributeValue(it) }
                                    ?: ""
                            }
                        }
                        "numPr" -> if (inParagraph) hasNumPr = true
                        "b" -> if (inParagraph) hasBold = true
                        "i" -> if (inParagraph) hasItalic = true
                        "t" -> if (inParagraph) paraText.append(parser.nextText())
                        "tab" -> if (inParagraph) paraText.append("    ")
                        "br" -> if (inParagraph) paraText.append("\n")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "p" && inParagraph) {
                        val raw = paraText.toString()
                        val styleLower = pStyle.lowercase()
                        val block = when {
                            styleLower.contains("heading1") || styleLower == "title" ->
                                DocBlock(style = DocBlockStyle.HEADING1, text = raw, isBold = true)
                            styleLower.contains("heading2") || styleLower == "subtitle" ->
                                DocBlock(style = DocBlockStyle.HEADING2, text = raw, isBold = true)
                            styleLower.contains("heading3") ->
                                DocBlock(style = DocBlockStyle.HEADING3, text = raw, isBold = true)
                            styleLower.contains("quote") ->
                                DocBlock(style = DocBlockStyle.QUOTE, text = raw, isItalic = true)
                            styleLower.contains("code") ->
                                DocBlock(style = DocBlockStyle.CODE, text = raw)
                            raw.startsWith("[x] ") || raw.startsWith("[X] ") ->
                                DocBlock(style = DocBlockStyle.CHECKLIST, text = raw.substring(4), isChecked = true)
                            raw.startsWith("[ ] ") ->
                                DocBlock(style = DocBlockStyle.CHECKLIST, text = raw.substring(4), isChecked = false)
                            hasNumPr || styleLower.contains("list") || raw.startsWith("• ") ->
                                DocBlock(
                                    style = DocBlockStyle.BULLET,
                                    text = raw.removePrefix("• "),
                                    isBold = hasBold,
                                    isItalic = hasItalic,
                                )
                            else ->
                                DocBlock(
                                    style = DocBlockStyle.BODY,
                                    text = raw,
                                    isBold = hasBold,
                                    isItalic = hasItalic,
                                )
                        }
                        if (raw.isNotEmpty() || blocks.isEmpty()) {
                            blocks.add(block)
                        }
                        inParagraph = false
                    }
                }
            }
            event = parser.next()
        }

        return RichDocument(blocks.ifEmpty { listOf(DocBlock(style = DocBlockStyle.BODY, text = "")) })
    }

    /**
     * Writes a REAL standards-compliant .docx OOXML archive openable in Microsoft Word & Google Docs.
     */
    fun writeDocx(document: RichDocument, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            fun addEntry(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>""",
            )

            addEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""",
            )

            addEntry(
                "word/_rels/document.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""",
            )

            addEntry(
                "word/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:rPr><w:b/><w:sz w:val="36"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:rPr><w:b/><w:sz w:val="28"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:rPr><w:b/><w:sz w:val="24"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Quote"><w:name w:val="Quote"/><w:rPr><w:i/><w:color w:val="475569"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Code"><w:name w:val="Code"/><w:rPr><w:rFonts w:ascii="Courier New"/><w:sz w:val="20"/></w:rPr></w:style>
</w:styles>""",
            )

            val bodyXml = StringBuilder()
            bodyXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            bodyXml.append("\n<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>\n")
            for (block in document.blocks) {
                val pStyleId = when (block.style) {
                    DocBlockStyle.HEADING1 -> "Heading1"
                    DocBlockStyle.HEADING2 -> "Heading2"
                    DocBlockStyle.HEADING3 -> "Heading3"
                    DocBlockStyle.QUOTE -> "Quote"
                    DocBlockStyle.CODE -> "Code"
                    else -> null
                }
                val prefix = when (block.style) {
                    DocBlockStyle.BULLET -> "• "
                    DocBlockStyle.CHECKLIST -> if (block.isChecked) "[x] " else "[ ] "
                    else -> ""
                }
                val runText = escapeXml(prefix + block.text)
                val rPr = buildString {
                    if (block.isBold || block.style in listOf(DocBlockStyle.HEADING1, DocBlockStyle.HEADING2, DocBlockStyle.HEADING3)) {
                        append("<w:b/>")
                    }
                    if (block.isItalic || block.style == DocBlockStyle.QUOTE) {
                        append("<w:i/>")
                    }
                }
                bodyXml.append("<w:p>")
                if (pStyleId != null) {
                    bodyXml.append("<w:pPr><w:pStyle w:val=\"$pStyleId\"/></w:pPr>")
                }
                bodyXml.append("<w:r>")
                if (rPr.isNotEmpty()) bodyXml.append("<w:rPr>$rPr</w:rPr>")
                bodyXml.append("<w:t xml:space=\"preserve\">$runText</w:t>")
                bodyXml.append("</w:r></w:p>\n")
            }
            bodyXml.append("<w:sectPr/></w:body></w:document>")
            addEntry("word/document.xml", bodyXml.toString())
        }
    }

    fun toMarkdown(document: RichDocument): String = document.blocks.joinToString("\n\n") { b ->
        when (b.style) {
            DocBlockStyle.HEADING1 -> "# ${b.text}"
            DocBlockStyle.HEADING2 -> "## ${b.text}"
            DocBlockStyle.HEADING3 -> "### ${b.text}"
            DocBlockStyle.BULLET -> "- ${b.text}"
            DocBlockStyle.CHECKLIST -> if (b.isChecked) "- [x] ${b.text}" else "- [ ] ${b.text}"
            DocBlockStyle.QUOTE -> "> ${b.text}"
            DocBlockStyle.CODE -> "```\n${b.text}\n```"
            DocBlockStyle.BODY -> {
                var t = b.text
                if (b.isBold) t = "**$t**"
                if (b.isItalic) t = "*$t*"
                t
            }
        }
    }

    fun fromMarkdown(markdown: String): RichDocument {
        val lines = markdown.lines()
        val blocks = mutableListOf<DocBlock>()
        var inCode = false
        val codeBuf = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (inCode) {
                    blocks.add(DocBlock(style = DocBlockStyle.CODE, text = codeBuf.toString().trimEnd()))
                    codeBuf.clear()
                    inCode = false
                } else {
                    inCode = true
                }
                continue
            }
            if (inCode) {
                codeBuf.appendLine(line)
                continue
            }
            if (trimmed.isEmpty()) continue
            when {
                trimmed.startsWith("### ") -> blocks.add(DocBlock(style = DocBlockStyle.HEADING3, text = trimmed.drop(4), isBold = true))
                trimmed.startsWith("## ") -> blocks.add(DocBlock(style = DocBlockStyle.HEADING2, text = trimmed.drop(3), isBold = true))
                trimmed.startsWith("# ") -> blocks.add(DocBlock(style = DocBlockStyle.HEADING1, text = trimmed.drop(2), isBold = true))
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ->
                    blocks.add(DocBlock(style = DocBlockStyle.CHECKLIST, text = trimmed.drop(6), isChecked = true))
                trimmed.startsWith("- [ ] ") ->
                    blocks.add(DocBlock(style = DocBlockStyle.CHECKLIST, text = trimmed.drop(6), isChecked = false))
                trimmed.startsWith("- ") || trimmed.startsWith("* ") ->
                    blocks.add(DocBlock(style = DocBlockStyle.BULLET, text = trimmed.drop(2)))
                trimmed.startsWith("> ") ->
                    blocks.add(DocBlock(style = DocBlockStyle.QUOTE, text = trimmed.drop(2), isItalic = true))
                else -> blocks.add(DocBlock(style = DocBlockStyle.BODY, text = trimmed))
            }
        }
        return RichDocument(blocks.ifEmpty { listOf(DocBlock(style = DocBlockStyle.BODY, text = "")) })
    }

    fun toHtml(title: String, document: RichDocument): String {
        val body = document.blocks.joinToString("\n") { b ->
            val safe = escapeXml(b.text)
            when (b.style) {
                DocBlockStyle.HEADING1 -> "<h1>$safe</h1>"
                DocBlockStyle.HEADING2 -> "<h2>$safe</h2>"
                DocBlockStyle.HEADING3 -> "<h3>$safe</h3>"
                DocBlockStyle.BULLET -> "<ul><li>$safe</li></ul>"
                DocBlockStyle.CHECKLIST -> "<p>${if (b.isChecked) "&#9745;" else "&#9744;"} $safe</p>"
                DocBlockStyle.QUOTE -> "<blockquote>$safe</blockquote>"
                DocBlockStyle.CODE -> "<pre><code>$safe</code></pre>"
                DocBlockStyle.BODY -> "<p>$safe</p>"
            }
        }
        return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>${escapeXml(title)}</title>
<style>body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;max-width:740px;margin:40px auto;padding:0 20px;color:#0f172a;line-height:1.6;}blockquote{border-left:3px solid #007AFF;padding-left:14px;color:#475569;}pre{background:#f1f5f9;padding:12px;border-radius:8px;overflow:auto;}</style>
</head>
<body>
$body
</body>
</html>"""
    }

    /**
     * Exports a RichDocument to a cleanly paginated real A4 PDF file.
     */
    fun exportToPdf(title: String, document: RichDocument, outputFile: File) {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val marginX = 48f
        val topMargin = 54f
        val bottomLimit = pageHeight - 54f
        val maxLineWidth = pageWidth - marginX * 2

        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = topMargin

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
        }
        val quoteBarPaint = Paint().apply {
            color = Color.rgb(0, 122, 255)
            strokeWidth = 3f
        }

        fun finishCurrentAndStartNew() {
            canvas.drawText("$title  •  Page $pageNumber", marginX, pageHeight - 24f, footerPaint)
            pdf.finishPage(page)
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = topMargin
        }

        for (block in document.blocks) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (block.style) {
                    DocBlockStyle.QUOTE -> Color.rgb(71, 85, 105)
                    DocBlockStyle.CODE -> Color.rgb(15, 23, 42)
                    else -> Color.rgb(15, 23, 42)
                }
                textSize = when (block.style) {
                    DocBlockStyle.HEADING1 -> 22f
                    DocBlockStyle.HEADING2 -> 17f
                    DocBlockStyle.HEADING3 -> 14f
                    DocBlockStyle.CODE -> 10.5f
                    else -> 12f
                }
                val tfStyle = when {
                    block.isBold && block.isItalic -> Typeface.BOLD_ITALIC
                    block.isBold || block.style in listOf(DocBlockStyle.HEADING1, DocBlockStyle.HEADING2, DocBlockStyle.HEADING3) -> Typeface.BOLD
                    block.isItalic || block.style == DocBlockStyle.QUOTE -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
                val family = if (block.style == DocBlockStyle.CODE) Typeface.MONOSPACE else Typeface.SANS_SERIF
                typeface = Typeface.create(family, tfStyle)
            }

            val prefix = when (block.style) {
                DocBlockStyle.BULLET -> "•  "
                DocBlockStyle.CHECKLIST -> if (block.isChecked) "[✓]  " else "[ ]  "
                else -> ""
            }
            val indentX = when (block.style) {
                DocBlockStyle.BULLET, DocBlockStyle.CHECKLIST, DocBlockStyle.QUOTE, DocBlockStyle.CODE -> 14f
                else -> 0f
            }

            val lines = wrapLines(prefix + block.text, paint, maxLineWidth - indentX)
            val lineHeight = paint.textSize * 1.48f
            val blockStartY = y

            for (line in lines) {
                if (y + lineHeight > bottomLimit) {
                    finishCurrentAndStartNew()
                }
                canvas.drawText(line, marginX + indentX, y, paint)
                y += lineHeight
            }
            if (block.style == DocBlockStyle.QUOTE && y > blockStartY) {
                canvas.drawLine(marginX + 4f, blockStartY - 10f, marginX + 4f, y - 6f, quoteBarPaint)
            }
            y += when (block.style) {
                DocBlockStyle.HEADING1 -> 12f
                DocBlockStyle.HEADING2 -> 10f
                else -> 8f
            }
        }

        canvas.drawText("$title  •  Page $pageNumber", marginX, pageHeight - 24f, footerPaint)
        pdf.finishPage(page)
        FileOutputStream(outputFile).use { fos -> pdf.writeTo(fos) }
        pdf.close()
    }

    private fun wrapLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val output = mutableListOf<String>()
        for (rawLine in text.split('\n')) {
            if (rawLine.isEmpty()) {
                output.add("")
                continue
            }
            val words = rawLine.split(' ')
            val current = StringBuilder()
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(word)
                } else {
                    if (current.isNotEmpty()) {
                        output.add(current.toString())
                        current.clear()
                    }
                    current.append(word)
                }
            }
            if (current.isNotEmpty()) output.add(current.toString())
        }
        return output.ifEmpty { listOf("") }
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
