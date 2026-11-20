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
import android.util.LruCache
import com.example.data.model.PdfAnnotationEntity
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Path
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class PdfTextPage(
    val title: String,
    val body: String,
)

data class PdfPageOperation(
    val sourceIndex: Int,
    val rotationDeg: Int = 0,
)

object PdfEngine {

    private val maxCacheBytes: Int =
        ((Runtime.getRuntime().maxMemory() / 1024L) / 10L)
            .toInt()
            .coerceIn(4 * 1024, 20 * 1024)

    private val pageCache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    private fun cacheKey(file: File, pageIndex: Int, powerSavingMode: Boolean, thumbnail: Boolean): String =
        "${file.absolutePath}|${file.lastModified()}|p$pageIndex|eco=$powerSavingMode|thumb=$thumbnail"

    fun getCachedPage(
        file: File,
        pageIndex: Int,
        powerSavingMode: Boolean,
        thumbnail: Boolean = false,
    ): Bitmap? {
        val bmp = pageCache.get(cacheKey(file, pageIndex, powerSavingMode, thumbnail))
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun clearRenderCache() {
        pageCache.evictAll()
    }

    fun getPageCount(file: File): Int {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer -> renderer.pageCount }
            }
        }.getOrDefault(0)
    }

    /**
     * Renders a single page from a real PDF file using Android's native PdfRenderer.
     * Uses an in-memory LRU cache so page flipping feels instantaneous.
     */
    fun renderPageBitmap(
        file: File,
        pageIndex: Int,
        powerSavingMode: Boolean,
        thumbnail: Boolean = false,
    ): Bitmap? {
        val key = cacheKey(file, pageIndex, powerSavingMode, thumbnail)
        pageCache.get(key)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        val rendered = runCatching {
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

        if (rendered != null) {
            pageCache.put(key, rendered)
        }
        return rendered
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
     * Bakes all user annotations (text overlays, highlights, sticky notes, vector signatures, stamps)
     * and any appended pages directly into a real updated PDF file on disk.
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
                                "SIGNATURE" -> {
                                    val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = baseColor
                                        style = Paint.Style.STROKE
                                        strokeWidth = 2.4f
                                        strokeCap = Paint.Cap.ROUND
                                        strokeJoin = Paint.Join.ROUND
                                    }
                                    val boxW = (w * 0.28f).coerceIn(110f, 190f)
                                    val boxH = boxW * 0.45f
                                    val left = (ax - boxW * 0.5f).coerceIn(12f, (w - boxW - 12f).coerceAtLeast(12f))
                                    val top = (ay - boxH * 0.5f).coerceIn(12f, (h - boxH - 12f).coerceAtLeast(12f))
                                    val strokes = parseSignatureStrokes(ann.content)
                                    for (stroke in strokes) {
                                        if (stroke.isEmpty()) continue
                                        val path = Path()
                                        stroke.forEachIndexed { ptIdx, (sx, sy) ->
                                            val px = left + sx * boxW
                                            val py = top + sy * boxH
                                            if (ptIdx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                        }
                                        canvas.drawPath(path, sigPaint)
                                    }
                                }
                                "STAMP" -> {
                                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = baseColor
                                        style = Paint.Style.STROKE
                                        strokeWidth = 2.0f
                                    }
                                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = Color.argb(26, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                                    }
                                    val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = baseColor
                                        textSize = 11f
                                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                                    }
                                    val label = ann.content.take(32)
                                    val boxW = (txtPaint.measureText(label) + 22f).coerceIn(88f, 210f)
                                    val rect = RectF(ax, ay - 16f, ax + boxW, ay + 12f)
                                    canvas.save()
                                    canvas.rotate(-6f, rect.centerX(), rect.centerY())
                                    canvas.drawRoundRect(rect, 7f, 7f, bgPaint)
                                    canvas.drawRoundRect(rect, 7f, 7f, borderPaint)
                                    canvas.drawText(label, ax + 11f, ay + 2f, txtPaint)
                                    canvas.restore()
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
        clearRenderCache()
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

    /**
     * Deletes a specific page from a real PDF file and rewrites the updated PDF cleanly.
     */
    fun deletePageFromPdf(sourceFile: File, pageIndexToDelete: Int, outputFile: File): Int {
        val pdf = PdfDocument()
        val tempOut = File(outputFile.parentFile, "tmp_del_${System.currentTimeMillis()}.pdf")
        var writtenPages = 0

        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount <= 1) {
                    pdf.close()
                    return renderer.pageCount
                }
                for (i in 0 until renderer.pageCount) {
                    if (i == pageIndexToDelete) continue
                    renderer.openPage(i).use { page ->
                        writtenPages++
                        val w = page.width
                        val h = page.height
                        val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        val outPage = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, writtenPages).create())
                        outPage.canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                        bmp.recycle()
                        pdf.finishPage(outPage)
                    }
                }
            }
        }

        FileOutputStream(tempOut).use { pdf.writeTo(it) }
        pdf.close()
        if (outputFile.exists()) outputFile.delete()
        tempOut.renameTo(outputFile)
        clearRenderCache()
        return writtenPages
    }

    /**
     * Reorders, rotates, or extracts a subset of pages from a real PDF file.
     */
    fun reorganizePdf(
        sourceFile: File,
        pageOps: List<PdfPageOperation>,
        outputFile: File,
    ): Int {
        if (pageOps.isEmpty()) return 0
        val pdf = PdfDocument()
        val tempOut = File(outputFile.parentFile, "tmp_org_${System.currentTimeMillis()}.pdf")
        var written = 0

        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (op in pageOps) {
                    if (op.sourceIndex !in 0 until renderer.pageCount) continue
                    renderer.openPage(op.sourceIndex).use { srcPage ->
                        written++
                        val rot = ((op.rotationDeg % 360) + 360) % 360
                        val swapDims = rot == 90 || rot == 270
                        val srcW = srcPage.width
                        val srcH = srcPage.height
                        val dstW = if (swapDims) srcH else srcW
                        val dstH = if (swapDims) srcW else srcH

                        val scale = 2
                        val rawBmp = Bitmap.createBitmap(srcW * scale, srcH * scale, Bitmap.Config.ARGB_8888)
                        Canvas(rawBmp).drawColor(Color.WHITE)
                        srcPage.render(rawBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        val finalBmp = if (rot != 0) {
                            val m = Matrix().apply { postRotate(rot.toFloat()) }
                            val rotated = Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, m, true)
                            if (rotated !== rawBmp) rawBmp.recycle()
                            rotated
                        } else rawBmp

                        val outPage = pdf.startPage(PdfDocument.PageInfo.Builder(dstW, dstH, written).create())
                        outPage.canvas.drawBitmap(finalBmp, null, RectF(0f, 0f, dstW.toFloat(), dstH.toFloat()), null)
                        finalBmp.recycle()
                        pdf.finishPage(outPage)
                    }
                }
            }
        }

        if (written > 0) {
            FileOutputStream(tempOut).use { pdf.writeTo(it) }
        }
        pdf.close()
        if (written > 0) {
            if (outputFile.exists()) outputFile.delete()
            tempOut.renameTo(outputFile)
            clearRenderCache()
        } else if (tempOut.exists()) {
            tempOut.delete()
        }
        return written
    }

    /**
     * Compresses an existing PDF by rendering pages at target DPI scale and JPEG quality.
     */
    fun compressPdf(
        sourceFile: File,
        renderScale: Float,
        jpegQuality: Int,
        outputFile: File,
    ): Int {
        val pdf = PdfDocument()
        val tempOut = File(outputFile.parentFile, "tmp_cmp_${System.currentTimeMillis()}.pdf")
        var written = 0

        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { srcPage ->
                        written++
                        val w = srcPage.width
                        val h = srcPage.height
                        val bmpW = (w * renderScale).toInt().coerceIn(220, 1800)
                        val bmpH = (h * renderScale).toInt().coerceIn(280, 2500)
                        val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val jpegStream = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(35, 95), jpegStream)
                        bmp.recycle()
                        val bytes = jpegStream.toByteArray()
                        val compressedBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        val outPage = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, written).create())
                        if (compressedBmp != null) {
                            outPage.canvas.drawBitmap(compressedBmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                            compressedBmp.recycle()
                        }
                        pdf.finishPage(outPage)
                    }
                }
            }
        }

        if (written > 0) {
            FileOutputStream(tempOut).use { pdf.writeTo(it) }
        }
        pdf.close()
        if (written > 0) {
            if (outputFile.exists()) outputFile.delete()
            tempOut.renameTo(outputFile)
            clearRenderCache()
        } else if (tempOut.exists()) {
            tempOut.delete()
        }
        return written
    }

    fun serializeSignatureStrokes(strokes: List<List<Pair<Float, Float>>>): String {
        return strokes
            .filter { it.isNotEmpty() }
            .joinToString("|") { stroke ->
                stroke.joinToString(";") { (x, y) ->
                    String.format(java.util.Locale.US, "%.3f,%.3f", x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
                }
            }
    }

    fun parseSignatureStrokes(raw: String): List<List<Pair<Float, Float>>> {
        if (raw.isBlank()) return emptyList()
        return raw.split('|').mapNotNull { strokeStr ->
            val pts = strokeStr.split(';').mapNotNull { pt ->
                val xy = pt.split(',')
                if (xy.size == 2) {
                    val x = xy[0].toFloatOrNull()
                    val y = xy[1].toFloatOrNull()
                    if (x != null && y != null) x to y else null
                } else null
            }
            pts.takeIf { it.isNotEmpty() }
        }
    }
}
