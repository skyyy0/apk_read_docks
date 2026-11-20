package com.example.ui.editors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.SheetData
import com.example.engine.SpreadsheetEngine
import com.example.ui.theme.FormatCodeColor
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.FormatMarkdownColor
import com.example.ui.theme.FormatPdfColor
import com.example.ui.theme.FormatSlidesColor
import com.example.ui.theme.FormatWordColor
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

private enum class SheetChartKind(val label: String) {
    BAR("Bar Chart"),
    LINE("Line Trend"),
    DONUT("Donut"),
}

@Composable
fun SpreadsheetChartDialog(
    sheet: SheetData,
    evaluatedCells: Map<Int, String>,
    onDismiss: () -> Unit,
) {
    val colCount = sheet.colCount.coerceAtLeast(1)
    var labelCol by remember { mutableIntStateOf(0) }
    var valueCol by remember { mutableIntStateOf((1).coerceAtMost(colCount - 1)) }
    var chartKind by remember { mutableStateOf(SheetChartKind.BAR) }

    val chartPoints = remember(sheet, evaluatedCells, labelCol, valueCol) {
        val list = mutableListOf<Pair<String, Float>>()
        for (r in 1 until sheet.rowCount) {
            val rawLabel = evaluatedCells[(r shl 16) or labelCol] ?: sheet.cellAt(r, labelCol)
            val rawVal = evaluatedCells[(r shl 16) or valueCol] ?: sheet.cellAt(r, valueCol)
            val num = rawVal.trim()
                .replace("$", "")
                .replace("Rp", "", ignoreCase = true)
                .replace(",", "")
                .toFloatOrNull()
            if (num != null) {
                list.add((rawLabel.ifBlank { "Row ${r + 1}" }) to num)
            }
        }
        list.take(12)
    }

    val chartColors = remember {
        listOf(FormatExcelColor, FormatWordColor, FormatSlidesColor, FormatMarkdownColor, FormatCodeColor, FormatPdfColor)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(6.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("${sheet.name} Analytics", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Live chart from sheet formulas & numeric cells",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetChartKind.entries.forEach { kind ->
                        FilterChip(
                            selected = chartKind == kind,
                            onClick = { chartKind = kind },
                            shape = CircleShape,
                            label = { Text(kind.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }

                Text("Label Column", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((0 until colCount).toList()) { c ->
                        val header = sheet.cellAt(0, c).ifBlank { SpreadsheetEngine.columnIndexToName(c) }
                        FilterChip(
                            selected = labelCol == c,
                            onClick = { labelCol = c },
                            shape = CircleShape,
                            label = { Text("${SpreadsheetEngine.columnIndexToName(c)}: $header", maxLines = 1) },
                        )
                    }
                }

                Text("Numeric Value Column", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((0 until colCount).toList()) { c ->
                        val header = sheet.cellAt(0, c).ifBlank { SpreadsheetEngine.columnIndexToName(c) }
                        FilterChip(
                            selected = valueCol == c,
                            onClick = { valueCol = c },
                            shape = CircleShape,
                            label = { Text("${SpreadsheetEngine.columnIndexToName(c)}: $header", maxLines = 1) },
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (chartPoints.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No numeric values found in column ${SpreadsheetEngine.columnIndexToName(valueCol)} (rows 2+).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp),
                            ) {
                                val maxVal = chartPoints.maxOf { kotlin.math.abs(it.second) }.coerceAtLeast(1f)
                                when (chartKind) {
                                    SheetChartKind.BAR -> {
                                        val count = chartPoints.size
                                        val slotW = size.width / count
                                        val barW = (slotW * 0.58f).coerceAtLeast(10f)
                                        chartPoints.forEachIndexed { idx, (_, v) ->
                                            val frac = (v.coerceAtLeast(0f) / maxVal).coerceIn(0.03f, 1f)
                                            val h = size.height * 0.88f * frac
                                            val x = slotW * idx + (slotW - barW) / 2f
                                            val y = size.height - h
                                            drawRoundRect(
                                                color = chartColors[idx % chartColors.size],
                                                topLeft = Offset(x, y),
                                                size = Size(barW, h),
                                                cornerRadius = CornerRadius(12f, 12f),
                                            )
                                        }
                                    }
                                    SheetChartKind.LINE -> {
                                        val path = Path()
                                        val stepX = if (chartPoints.size > 1) size.width / (chartPoints.size - 1) else size.width / 2f
                                        chartPoints.forEachIndexed { idx, (_, v) ->
                                            val x = idx * stepX
                                            val frac = (v.coerceAtLeast(0f) / maxVal).coerceIn(0f, 1f)
                                            val y = size.height * 0.88f * (1f - frac) + size.height * 0.06f
                                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        }
                                        drawPath(path = path, color = FormatExcelColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
                                        chartPoints.forEachIndexed { idx, (_, v) ->
                                            val x = idx * stepX
                                            val frac = (v.coerceAtLeast(0f) / maxVal).coerceIn(0f, 1f)
                                            val y = size.height * 0.88f * (1f - frac) + size.height * 0.06f
                                            drawCircle(color = chartColors[idx % chartColors.size], radius = 9f, center = Offset(x, y))
                                        }
                                    }
                                    SheetChartKind.DONUT -> {
                                        val total = chartPoints.sumOf { it.second.coerceAtLeast(0f).toDouble() }.toFloat().coerceAtLeast(1f)
                                        val diameter = kotlin.math.min(size.width, size.height) * 0.84f
                                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                                        var startAngle = -90f
                                        chartPoints.forEachIndexed { idx, (_, v) ->
                                            val sweep = (v.coerceAtLeast(0f) / total) * 360f
                                            drawArc(
                                                color = chartColors[idx % chartColors.size],
                                                startAngle = startAngle,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                topLeft = topLeft,
                                                size = Size(diameter, diameter),
                                                style = Stroke(width = 32f, cap = StrokeCap.Butt),
                                            )
                                            startAngle += sweep
                                        }
                                    }
                                }
                            }

                            // Compact Legend & Summary
                            val totalSum = chartPoints.sumOf { it.second.toDouble() }
                            val avg = totalSum / chartPoints.size
                            Text(
                                text = String.format(Locale.US, "Count: %d  •  Sum: %.2f  •  Avg: %.2f", chartPoints.size, totalSum, avg),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(chartPoints.size) { idx ->
                                    val (lbl, v) = chartPoints[idx]
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(chartColors[idx % chartColors.size]),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "$lbl (${String.format(Locale.US, "%.1f", v)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
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
        }
    }
}
