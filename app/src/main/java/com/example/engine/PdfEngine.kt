package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.PdfAnnotationEntity
import java.io.File
import java.io.FileOutputStream

data class PdfTextPage(
    val title: String,
    val body: String,
)

object PdfEngine {

    fun getPageCount(file: File): Int {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer -> renderer.pageCount }
            }
        }.getOrDefault(0)
    }

    /**
     * Renders a single page from a real PDF file using Android's native PdfRenderer.
     * Respects Power Saving Mode by scaling down render DPI cleanly for battery & speed.
     */
    fun renderPageBitmap(
        file: File,
        pageIndex: Int,
        powerSavingMode: Boolean,
        thumbnail: Boolean = false,
    ): Bitmap? {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex !in 0 until renderer.pageCount) return null
                    renderer.openPage(pageIndex).use { page ->
                        val scale = when {
                            thumbnail -> 0.35f
                            powerSavingMode -> 1.25f
                            else -> 2.0f
                        }
                        val width = (page.width * scale).toInt().coerceIn(120, 2200)
                        val height = (page.height * scale).toInt().coerceIn(120, 3000)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }.getOrNull()
    }

    /**
     * Creates a real multi-page PDF document from editable text pages.
     */
    fun createPdfFromTextPages(pages: List<PdfTextPage>, outputFile: File) {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 48f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val accentPaint = Paint().apply {
            color = Color.rgb(0, 122, 255)
            strokeWidth = 2.5f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9.5f
        }

        pages.forEachIndexed { idx, item ->
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create())
            val canvas = page.canvas
            canvas.drawText(item.title, margin, margin + 18f, titlePaint)
            canvas.drawLine(margin, margin + 28f, margin + 64f, margin + 28f, accentPaint)

            var y = margin + 54f
            val maxW = pageWidth - margin * 2
            for (paragraph in item.body.split('\n')) {
                val words = paragraph.split(' ')
                val line = StringBuilder()
                for (w in words) {
                    val candidate = if (line.isEmpty()) w else "$line $w"
                    if (bodyPaint.measureText(candidate) <= maxW) {
                        if (line.isNotEmpty()) line.append(' ')
                        line.append(w)
                    } else {
                        if (y < pageHeight - 54f) {
                            canvas.drawText(line.toString(), margin, y, bodyPaint)
                            y += 18f
                        }
                        line.clear()
                        line.append(w)
                    }
                }
                if (line.isNotEmpty() && y < pageHeight - 54f) {
                    canvas.drawText(line.toString(), margin, y, bodyPaint)
                    y += 18f
                }
                y += 6f
            }

            canvas.drawText("Lumina Docs  •  Page ${idx + 1} of ${pages.size}", margin, pageHeight - 26f, footerPaint)
            pdf.finishPage(page)
        }

        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close()
    }

    /**
     * Bakes all user annotations (text overlays, highlights, sticky notes) and any appended pages
     * directly into a real updated PDF file on disk!
     */
    fun bakeAnnotationsAndPages(
        sourceFile: File,
        annotations: List<PdfAnnotationEntity>,
        appendedPages: List<PdfTextPage>,
        outputFile: File,
    ) {
        val pdf = PdfDocument()
        val tempOut = File(outputFile.parentFile, "tmp_bake_${System.currentTimeMillis()}.pdf")

        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                var outPageNum = 1
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { srcPage ->
                        val w = srcPage.width
                        val h = srcPage.height
                        val scale = 2
                        val bmp = Bitmap.createBitmap(w * scale, h * scale, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        val outPage = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, outPageNum++).create())
                        val canvas = outPage.canvas
                        canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                        bmp.recycle()

                        // Draw annotations for page i
                        val pageAnnotations = annotations.filter { it.pageIndex == i }
                        for (ann in pageAnnotations) {
                            val ax = (ann.normX * w).coerceIn(16f, w - 40f)
                            val ay = (ann.normY * h).coerceIn(24f, h - 24f)
                            val baseColor = ann.colorHex.toInt()
                            when (ann.kind) {
                                "HIGHLIGHT" -> {
                                    val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color.argb(95, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                                    }
                                    val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color.rgb(15, 23, 42)
                                        textSize = 11f
                                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                                    }
                                    val boxW = (txtPaint.measureText(ann.content) + 18f).coerceAtLeast(88f)
                                    canvas.drawRoundRect(RectF(ax - 6f, ay - 14f, ax + boxW, ay + 6f), 5f, 5f, hlPaint)
                                    if (ann.content.isNotBlank()) {
                                        canvas.drawText(ann.content, ax + 2f, ay, txtPaint)
                                    }
                                }
                                "NOTE" -> {
                                    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color.rgb(254, 249, 195)
                                    }
                                    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color.rgb(234, 179, 8)
                                        style = Paint.Style.STROKE
                                        strokeWidth = 1.2f
                                    }
                                    val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color.rgb(66, 32, 6)
                                        textSize = 10.5f
                                    }
                                    val boxW = (txtPaint.measureText(ann.content) + 20f).coerceIn(96f, 220f)
                                    val rect = RectF(ax, ay - 16f, ax + boxW, ay + 12f)
                                    canvas.drawRoundRect(rect, 6f, 6f, bg)
                                    canvas.drawRoundRect(rect, 6f, 6f, border)
                                    canvas.drawText(ann.content.take(36), ax + 8f, ay + 2f, txtPaint)
                                }
                                else -> { // TEXT
                                    val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = baseColor
                                        textSize = 13f
                                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                                    }
                                    canvas.drawText(ann.content, ax, ay, txtPaint)
                                }
                            }
                        }

                        pdf.finishPage(outPage)
                    }
                }

                // Append any newly added text pages
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(15, 23, 42)
                    textSize = 19f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(30, 41, 59)
                    textSize = 12f
                }
                for (extra in appendedPages) {
                    val outPage = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, outPageNum++).create())
                    val canvas = outPage.canvas
                    canvas.drawText(extra.title, 48f, 64f, titlePaint)
                    var y = 96f
                    for (line in extra.body.lines()) {
                        if (y > 790f) break
                        canvas.drawText(line, 48f, y, bodyPaint)
                        y += 18f
                    }
                    pdf.finishPage(outPage)
                }
            }
        }

        FileOutputStream(tempOut).use { pdf.writeTo(it) }
        pdf.close()
        if (outputFile.exists()) outputFile.delete()
        tempOut.renameTo(outputFile)
    }

    /**
     * Merges multiple real PDF files into a single combined PDF file!
     */
    fun mergePdfs(sourceFiles: List<File>, outputFile: File): Int {
        val pdf = PdfDocument()
        var totalPages = 0
        for (src in sourceFiles) {
            if (!src.exists()) continue
            ParcelFileDescriptor.open(src, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            totalPages++
                            val w = page.width
                            val h = page.height
                            val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                            Canvas(bmp).drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            val outPage = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, totalPages).create())
                            outPage.canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                            bmp.recycle()
                            pdf.finishPage(outPage)
                        }
                    }
                }
            }
        }
        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close()
        return totalPages
    }
}
