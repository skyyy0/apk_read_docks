package com.example.engine

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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

data class SlideItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val bullets: List<String>,
    val speakerNotes: String = "",
)

data class SlideDeck(
    val slides: List<SlideItem>,
)

object PresentationEngine {

    /**
     * Reads a REAL .pptx OOXML file by extracting ppt/slides/slide*.xml text paragraphs.
     */
    fun readPptx(file: File): SlideDeck {
        val slideXmlMap = mutableMapOf<Int, ByteArray>()
        ZipInputStream(FileInputStream(file)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val match = Regex("ppt/slides/slide(\\d+)\\.xml").matchEntire(entry.name)
                if (match != null) {
                    val idx = match.groupValues[1].toIntOrNull() ?: 0
                    val baos = ByteArrayOutputStream()
                    zis.copyTo(baos)
                    slideXmlMap[idx] = baos.toByteArray()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val slides = slideXmlMap.entries.sortedBy { it.key }.mapIndexed { index, (_, bytes) ->
            val paragraphs = extractSlideParagraphs(bytes)
            val title = paragraphs.firstOrNull().orEmpty().ifBlank { "Slide ${index + 1}" }
            val bullets = if (paragraphs.size > 1) paragraphs.drop(1) else emptyList()
            SlideItem(title = title, bullets = bullets)
        }

        return SlideDeck(
            slides = slides.ifEmpty {
                listOf(SlideItem(title = "Untitled Presentation", bullets = listOf("Tap to edit slide content")))
            }
        )
    }

    private fun extractSlideParagraphs(xmlBytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var inPara = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "p" -> {
                            inPara = true
                            sb.clear()
                        }
                        "t" -> if (inPara) sb.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "p" && inPara) {
                        val txt = sb.toString().trim()
                        if (txt.isNotEmpty()) result.add(txt)
                        inPara = false
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    /**
     * Writes a REAL lightweight OOXML .pptx archive openable in PowerPoint, Google Slides, and Lumina Docs.
     */
    fun writePptx(deck: SlideDeck, outputFile: File) {
        val slides = deck.slides.ifEmpty { listOf(SlideItem(title = "Slide 1", bullets = emptyList())) }
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            fun addEntry(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val slideOverrides = slides.indices.joinToString("\n") { i ->
                """  <Override PartName="/ppt/slides/slide${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>"""
            }
            addEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
$slideOverrides
</Types>""",
            )

            addEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""",
            )

            val sldIdLst = slides.indices.joinToString("\n") { i ->
                """    <p:sldId id="${256 + i}" r:id="rId${i + 1}"/>"""
            }
            addEntry(
                "ppt/presentation.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldIdLst>
$sldIdLst
  </p:sldIdLst>
  <p:sldSz cx="9144000" cy="5143500"/>
</p:presentation>""",
            )

            val presRels = slides.indices.joinToString("\n") { i ->
                """  <Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide${i + 1}.xml"/>"""
            }
            addEntry(
                "ppt/_rels/presentation.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$presRels
</Relationships>""",
            )

            slides.forEachIndexed { idx, slide ->
                val bulletXml = slide.bullets.joinToString("\n") { b ->
                    """          <a:p><a:r><a:t>${escapeXml(b)}</a:t></a:r></a:p>"""
                }
                addEntry(
                    "ppt/slides/slide${idx + 1}.xml",
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr/>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2" name="Title"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="457200" y="365760"/><a:ext cx="8229600" cy="914400"/></a:xfrm></p:spPr>
        <p:txBody>
          <a:bodyPr/>
          <a:p><a:r><a:rPr b="1" sz="3200"/><a:t>${escapeXml(slide.title)}</a:t></a:r></a:p>
$bulletXml
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>""",
                )
            }
        }
    }

    /**
     * Exports the SlideDeck as a crisp 16:9 widescreen PDF presentation.
     */
    fun exportDeckToPdf(deckTitle: String, deck: SlideDeck, outputFile: File) {
        val pdf = PdfDocument()
        val width = 960
        val height = 540
        val bgPaint = Paint().apply { color = Color.rgb(15, 23, 42) }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(24, 34, 54) }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(56, 155, 255) }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 17f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 11f
        }

        deck.slides.forEachIndexed { idx, slide ->
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, idx + 1).create())
            val c = page.canvas
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            c.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 24f, 24f, cardPaint)
            c.drawRoundRect(RectF(68f, 72f, 128f, 78f), 3f, 3f, accentPaint)
            c.drawText(slide.title, 68f, 122f, titlePaint)

            var y = 180f
            for (bullet in slide.bullets) {
                if (y > height - 72f) break
                c.drawText("•  $bullet", 76f, y, bulletPaint)
                y += 34f
            }
            c.drawText("$deckTitle  •  Slide ${idx + 1} / ${deck.slides.size}", 68f, height - 56f, footerPaint)
            pdf.finishPage(page)
        }

        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close()
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
