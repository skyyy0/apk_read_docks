package com.example.ui.editors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SpreadsheetEngine
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.GlassToolbarSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.bouncyPress
import com.example.viewmodel.ActiveEditorState

@Composable
fun SpreadsheetEditorScreen(
    state: ActiveEditorState.SpreadsheetEditor,
    showGridlines: Boolean,
    fontSizeSp: Int,
    onClose: () -> Unit,
    onSelectCell: (row: Int, col: Int) -> Unit,
    onUpdateCell: (row: Int, col: Int, value: String) -> Unit,
    onSelectSheet: (Int) -> Unit,
    onAddSheet: (String) -> Unit,
    onAddRow: () -> Unit,
    onAddColumn: () -> Unit,
    onDeleteSelectedRow: () -> Unit,
    onDeleteSelectedColumn: () -> Unit,
    onSortColumn: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val activeSheet = state.workbook.sheets.getOrNull(state.activeSheetIndex)
        ?: state.workbook.sheets.first()
    val evaluatedCells = remember(activeSheet) {
        buildMap {
            activeSheet.rows.forEachIndexed { r, row ->
                row.forEachIndexed { c, raw ->
                    if (raw.startsWith("=")) {
                        put((r shl 16) or c, SpreadsheetEngine.evaluateCell(activeSheet, r, c))
                    }
                }
            }
        }
    }
    val horizontalScroll = rememberScrollState()
    val selectedRaw = activeSheet.cellAt(state.selectedRow, state.selectedCol)
    val selectedEvaluated = evaluatedCells[(state.selectedRow shl 16) or state.selectedCol] ?: selectedRaw
    val cellRefLabel = "${SpreadsheetEngine.columnIndexToName(state.selectedCol)}${state.selectedRow + 1}"

    var showNewSheetDialog by remember { mutableStateOf(false) }
    var newSheetName by remember { mutableStateOf("Sheet ${state.workbook.sheets.size + 1}") }

    val quickFormulas = remember { listOf("=SUM(", "=AVERAGE(", "=MIN(", "=MAX(", "=COUNT(", "=IF(") }
    val colWidth = 112.dp
    val rowIndexWidth = 42.dp
    val cellHeight = 40.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Translucent Top Bar
        GlassToolbarSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("editor_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.document.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (state.hasUnsavedChanges) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(FormatExcelColor),
                            )
                        }
                    }
                    Text(
                        text = "${activeSheet.name}  •  ${activeSheet.rowCount}×${activeSheet.colCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSortColumn) {
                    Icon(Icons.Rounded.SortByAlpha, contentDescription = "Sort selected column")
                }
                IconButton(onClick = onExportPdf) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export sheet to PDF")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Rounded.IosShare, contentDescription = "Share spreadsheet")
                }
                IconButton(onClick = onSave, modifier = Modifier.testTag("excel_save_button")) {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = "Save spreadsheet",
                        tint = FormatExcelColor,
                    )
                }
            }
        }

        // Live Formula Bar (fx)
        GlassToolbarSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(FormatExcelColor.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = cellRefLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFamily,
                            color = FormatExcelColor,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Functions,
                        contentDescription = "Formula bar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    BasicTextField(
                        value = selectedRaw,
                        onValueChange = { onUpdateCell(state.selectedRow, state.selectedCol, it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(FormatExcelColor),
                        decorationBox = { inner ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (selectedRaw.isEmpty()) {
                                    Text(
                                        text = "Type value or =SUM(B2:B6)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("formula_bar_input"),
                    )
                    if (selectedRaw.startsWith("=")) {
                        Text(
                            text = "= $selectedEvaluated",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = FormatExcelColor,
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(quickFormulas) { _, fn ->
                        SuggestionChip(
                            onClick = { onUpdateCell(state.selectedRow, state.selectedCol, selectedRaw + fn) },
                            shape = CircleShape,
                            label = {
                                Text(
                                    text = fn,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                )
                            },
                        )
                    }
                }
            }
        }

        // 2D Scrollable Spreadsheet Grid
        val borderColor = if (showGridlines) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        } else {
            Color.Transparent
        }
        val headerBg = MaterialTheme.colorScheme.surfaceContainer

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sticky Column Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .horizontalScroll(horizontalScroll),
                ) {
                    Box(
                        modifier = Modifier
                            .width(rowIndexWidth)
                            .height(34.dp)
                            .border(0.5.dp, borderColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    for (c in 0 until activeSheet.colCount) {
                        val isColSelected = c == state.selectedCol
                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .height(34.dp)
                                .background(if (isColSelected) FormatExcelColor.copy(alpha = 0.16f) else Color.Transparent)
                                .border(0.5.dp, borderColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = SpreadsheetEngine.columnIndexToName(c),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFamily,
                                color = if (isColSelected) FormatExcelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(activeSheet.rows) { rIdx, rowCells ->
                        val isRowSelected = rIdx == state.selectedRow
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScroll),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(rowIndexWidth)
                                    .height(cellHeight)
                                    .background(if (isRowSelected) FormatExcelColor.copy(alpha = 0.16f) else headerBg)
                                    .border(0.5.dp, borderColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${rIdx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = if (isRowSelected) FormatExcelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            for (cIdx in 0 until activeSheet.colCount) {
                                val isSelected = rIdx == state.selectedRow && cIdx == state.selectedCol
                                val rawValue = rowCells.getOrNull(cIdx) ?: ""
                                val displayValue = if (rawValue.startsWith("=")) {
                                    evaluatedCells[(rIdx shl 16) or cIdx] ?: ""
                                } else {
                                    rawValue
                                }
                                val isHeaderRow = rIdx == 0
                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .height(cellHeight)
                                        .background(
                                            when {
                                                isSelected -> FormatExcelColor.copy(alpha = 0.13f)
                                                isHeaderRow -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) FormatExcelColor else borderColor,
                                        )
                                        .clickable { onSelectCell(rIdx, cIdx) }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        text = displayValue,
                                        fontSize = (fontSizeSp - 2).coerceIn(11, 16).sp,
                                        fontWeight = if (isHeaderRow || rawValue.startsWith("=")) FontWeight.SemiBold else FontWeight.Normal,
                                        fontFamily = if (rawValue.startsWith("=") || displayValue.toDoubleOrNull() != null) {
                                            JetBrainsMonoFamily
                                        } else {
                                            MaterialTheme.typography.bodyMedium.fontFamily
                                        },
                                        color = if (displayValue.startsWith("#")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Multi-Sheet Tabs & Row/Col Controls
        GlassToolbarSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    itemsIndexed(state.workbook.sheets) { idx, sheet ->
                        FilterChip(
                            selected = idx == state.activeSheetIndex,
                            onClick = { onSelectSheet(idx) },
                            shape = CircleShape,
                            label = { Text(sheet.name, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                newSheetName = "Sheet ${state.workbook.sheets.size + 1}"
                                showNewSheetDialog = true
                            },
                            shape = CircleShape,
                            leadingIcon = {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Sheet") },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SheetActionButton(Icons.Rounded.ViewStream, "+ Row", onAddRow, Modifier.weight(1f))
                    SheetActionButton(Icons.Rounded.ViewColumn, "+ Col", onAddColumn, Modifier.weight(1f))
                    SheetActionButton(Icons.Rounded.DeleteSweep, "- Row", onDeleteSelectedRow, Modifier.weight(1f))
                    SheetActionButton(Icons.Rounded.DeleteSweep, "- Col", onDeleteSelectedColumn, Modifier.weight(1f))
                }
            }
        }
    }

    if (showNewSheetDialog) {
        AlertDialog(
            onDismissRequest = { showNewSheetDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Add Worksheet") },
            text = {
                OutlinedTextField(
                    value = newSheetName,
                    onValueChange = { newSheetName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Sheet name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddSheet(newSheetName)
                    showNewSheetDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showNewSheetDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SheetActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.bouncyPress(pressedScale = 0.94f),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        shape = CircleShape,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
