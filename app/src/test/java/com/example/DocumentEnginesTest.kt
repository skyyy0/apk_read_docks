package com.example

import com.example.engine.DocBlock
import com.example.engine.DocBlockStyle
import com.example.engine.JsonValidationResult
import com.example.engine.PresentationEngine
import com.example.engine.RichDocument
import com.example.engine.SheetData
import com.example.engine.SlideDeck
import com.example.engine.SlideItem
import com.example.engine.SpreadsheetEngine
import com.example.engine.TextDocumentEngine
import com.example.engine.WordDocumentEngine
import com.example.engine.WorkbookData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DocumentEnginesTest {

    @Test
    fun spreadsheetFormulaEngineEvaluatesCorrectly() {
        val sheet = SheetData(
            name = "Budget",
            rows = listOf(
                listOf("Item", "Planned", "Actual", "Diff", "Status"),
                listOf("Rent", "1000", "950", "=B2-C2", "=IF(D2>=0,\"OK\",\"OVER\")"),
                listOf("Food", "400", "450", "=B3-C3", "=IF(D3>=0,\"OK\",\"OVER\")"),
                listOf("Total", "=SUM(B2:B3)", "=SUM(C2:C3)", "=AVERAGE(D2:D3)", "=COUNT(B2:B3)"),
            )
        )

        assertEquals("50", SpreadsheetEngine.evaluateCell(sheet, 1, 3))
        assertEquals("OK", SpreadsheetEngine.evaluateCell(sheet, 1, 4))
        assertEquals("-50", SpreadsheetEngine.evaluateCell(sheet, 2, 3))
        assertEquals("OVER", SpreadsheetEngine.evaluateCell(sheet, 2, 4))
        assertEquals("1400", SpreadsheetEngine.evaluateCell(sheet, 3, 1))
        assertEquals("1400", SpreadsheetEngine.evaluateCell(sheet, 3, 2))
        assertEquals("0", SpreadsheetEngine.evaluateCell(sheet, 3, 3))
        assertEquals("2", SpreadsheetEngine.evaluateCell(sheet, 3, 4))
    }

    @Test
    fun xlsxRoundTripPreservesSheetsAndFormulas() {
        val tmp = File.createTempFile("lumina_test_", ".xlsx")
        try {
            val wb = WorkbookData(
                listOf(
                    SheetData(
                        name = "Sales",
                        rows = listOf(
                            listOf("Product", "Qty", "Price", "Total"),
                            listOf("Notebook", "3", "25", "=B2*C2"),
                        )
                    )
                )
            )
            SpreadsheetEngine.writeXlsx(wb, tmp)
            val loaded = SpreadsheetEngine.readXlsx(tmp)
            assertEquals("Sales", loaded.sheets.first().name)
            assertEquals("Notebook", loaded.sheets.first().cellAt(1, 0))
            assertEquals("=B2*C2", loaded.sheets.first().cellAt(1, 3))
            assertEquals("75", SpreadsheetEngine.evaluateCell(loaded.sheets.first(), 1, 3))
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun docxRoundTripPreservesHeadingsAndChecklists() {
        val tmp = File.createTempFile("lumina_test_", ".docx")
        try {
            val doc = RichDocument(
                listOf(
                    DocBlock(style = DocBlockStyle.HEADING1, text = "Project Lumina", isBold = true),
                    DocBlock(style = DocBlockStyle.CHECKLIST, text = "Ship Android 5+ APK", isChecked = true),
                    DocBlock(style = DocBlockStyle.BODY, text = "Lightweight and fast."),
                )
            )
            WordDocumentEngine.writeDocx(doc, tmp)
            val loaded = WordDocumentEngine.readDocx(tmp)
            assertEquals(3, loaded.blocks.size)
            assertEquals(DocBlockStyle.HEADING1, loaded.blocks[0].style)
            assertEquals("Project Lumina", loaded.blocks[0].text)
            assertEquals(DocBlockStyle.CHECKLIST, loaded.blocks[1].style)
            assertTrue(loaded.blocks[1].isChecked)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun pptxRoundTripPreservesSlides() {
        val tmp = File.createTempFile("lumina_test_", ".pptx")
        try {
            val deck = SlideDeck(
                listOf(
                    SlideItem(title = "Intro", bullets = listOf("Point A", "Point B")),
                    SlideItem(title = "Summary", bullets = listOf("Fast", "Offline")),
                )
            )
            PresentationEngine.writePptx(deck, tmp)
            val loaded = PresentationEngine.readPptx(tmp)
            assertEquals(2, loaded.slides.size)
            assertEquals("Intro", loaded.slides[0].title)
            assertEquals("Summary", loaded.slides[1].title)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun jsonValidatorFormatsAndDetectsInvalidSyntax() {
        val valid = TextDocumentEngine.validateAndFormatJson("""{"app":"Lumina","ok":true}""")
        assertTrue(valid is JsonValidationResult.Valid)
        val invalid = TextDocumentEngine.validateAndFormatJson("""{"broken": """)
        assertTrue(invalid is JsonValidationResult.Invalid)
    }
}
