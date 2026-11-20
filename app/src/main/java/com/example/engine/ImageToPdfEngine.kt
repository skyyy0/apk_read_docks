package com.example.engine

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

enum class ScanFilterMode(val label: String) {
    ORIGINAL("Original Color"),
    DOCUMENT_BW("Scan B&W"),
    HIGH_CONTRAST("Crisp Docs"),
}

data class ScanPageInput(
    val uri: Uri,
    val rotationDeg: Int = 0,
)

object ImageToPdfEngine {

    /**
     * Converts selected gallery/camera images (zero-permission Photo Picker URIs)
     * into a crisp A4 multi-page PDF with optional document B&W/contrast enhancement.
     */
    fun buildPdfFromImageUris(
        contentResolver: ContentResolver,
        pages: List<ScanPageInput>,
        filterMode: ScanFilterMode,
        outputFile: File,
    ): Int {
        if (pages.isEmpty()) return 0

        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 20f
        var written = 0

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = when (filterMode) {
                ScanFilterMode.ORIGINAL -> null
                ScanFilterMode.DOCUMENT_BW -> {
                    val grayscale = ColorMatrix().apply { setSaturation(0f) }
                    val contrast = 1.45f
                    val translate = (-0.5f * contrast + 0.5f) * 255f
                    val boost = ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, translate,
                            0f, contrast, 0f, 0f, translate,
                            0f, 0f, contrast, 0f, translate,
                            0f, 0f, 0f, 1f, 0f,
                        )
                    )
                    grayscale.postConcat(boost)
                    ColorMatrixColorFilter(grayscale)
                }
                ScanFilterMode.HIGH_CONTRAST -> {
                    val contrast = 1.24f
                    val translate = (-0.5f * contrast + 0.5f) * 255f
                    ColorMatrixColorFilter(
                        ColorMatrix(
                            floatArrayOf(
                                contrast, 0f, 0f, 0f, translate,
                                0f, contrast, 0f, 0f, translate,
                                0f, 0f, contrast, 0f, translate,
                                0f, 0f, 0f, 1f, 0f,
                            )
                        )
                    )
                }
            }
        }

        val tempOut = File(outputFile.parentFile, "tmp_scan_${System.currentTimeMillis()}.pdf")

        pages.forEach { input ->
            val rawBitmap = decodeSampledBitmap(contentResolver, input.uri, maxDim = 1650) ?: return@forEach
            val bitmap = if (input.rotationDeg % 360 != 0) {
                val matrix = Matrix().apply { postRotate((input.rotationDeg % 360).toFloat()) }
                val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                if (rotated !== rawBitmap) rawBitmap.recycle()
                rotated
            } else {
                rawBitmap
            }

            written++
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, written).create())
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val availW = pageWidth - margin * 2f
            val availH = pageHeight - margin * 2f
            val scale = min(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat())
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val left = (pageWidth - drawW) / 2f
            val top = (pageHeight - drawH) / 2f
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), paint)

            pdf.finishPage(page)
            bitmap.recycle()
        }

        if (written > 0) {
            FileOutputStream(tempOut).use { pdf.writeTo(it) }
        }
        pdf.close()

        if (written > 0) {
            if (outputFile.exists()) outputFile.delete()
            tempOut.renameTo(outputFile)
        } else if (tempOut.exists()) {
            tempOut.delete()
        }
        return written
    }

    private fun decodeSampledBitmap(
        contentResolver: ContentResolver,
        uri: Uri,
        maxDim: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }
}
