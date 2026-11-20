package com.example.ui.editors

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.PdfEngine
import com.example.engine.PdfPageOperation
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfSignaturePadDialog(
    savedSignatureStrokes: String,
    onDismiss: () -> Unit,
    onConfirmSignature: (serializedStrokes: String, colorHex: Long, saveTemplate: Boolean) -> Unit,
    onConfirmStamp: (stampText: String, colorHex: Long) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) } // 0 = Draw Signature, 1 = Stamps
    val strokes = remember { mutableStateListOf<List<Pair<Float, Float>>>() }
    val activeStroke = remember { mutableStateListOf<Pair<Float, Float>>() }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var selectedColorHex by remember { mutableStateOf(0xFF0F172A) }
    var saveForReuse by remember { mutableStateOf(true) }

    val dateTag = remember { SimpleDateFormat("dd MMM yy", Locale.US).format(Date()) }
    val stamps = remember(dateTag) {
        listOf(
            "APPROVED • $dateTag" to 0xFF16A34A,
            "VERIFIED • $dateTag" to 0xFF007AFF,
            "PAID • $dateTag" to 0xFF059669,
            "CONFIDENTIAL" to 0xFFDC2626,
            "DRAFT COPY" to 0xFFD97706,
        )
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
                .fillMaxWidth(0.92f)
                .padding(8.dp),
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
                    Text(
                        text = "Sign & Stamp PDF",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        shape = CircleShape,
                        leadingIcon = { Icon(Icons.Rounded.Draw, null, Modifier.size(16.dp)) },
                        label = { Text("Signature") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        shape = CircleShape,
                        leadingIcon = { Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp)) },
                        label = { Text("Quick Stamp") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }

                if (tab == 0) {
                    // Ink Color & Reuse Saved Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(0xFF0F172A, 0xFF1D4ED8, 0xFFDC2626).forEach { hex ->
                                val selected = selectedColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            shape = CircleShape,
                                        )
                                        .clickable { selectedColorHex = hex },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (savedSignatureStrokes.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        strokes.clear()
                                        strokes.addAll(PdfEngine.parseSignatureStrokes(savedSignatureStrokes))
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text("Use Saved", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            IconButton(onClick = {
                                strokes.clear()
                                activeStroke.clear()
                            }) {
                                Icon(Icons.Rounded.CleaningServices, contentDescription = "Clear signature")
                            }
                        }
                    }

                    // Drawing Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        activeStroke.clear()
                                        val nx = (offset.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                        val ny = (offset.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                        activeStroke.add(nx to ny)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val nx = (change.position.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                        val ny = (change.position.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                        activeStroke.add(nx to ny)
                                    },
                                    onDragEnd = {
                                        if (activeStroke.isNotEmpty()) {
                                            strokes.add(activeStroke.toList())
                                            activeStroke.clear()
                                        }
                                    },
                                )
                            }
                            .testTag("signature_canvas"),
                    ) {
                        if (strokes.isEmpty() && activeStroke.isEmpty()) {
                            Text(
                                text = "Draw your signature here",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val drawColor = Color(selectedColorHex)
                            val strokeStyle = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            (strokes + listOfNotNull(activeStroke.takeIf { it.isNotEmpty() })).forEach { pts ->
                                val path = Path()
                                pts.forEachIndexed { idx, (nx, ny) ->
                                    val px = nx * size.width
                                    val py = ny * size.height
                                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                }
                                drawPath(path = path, color = drawColor, style = strokeStyle)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { saveForReuse = !saveForReuse },
                    ) {
                        Checkbox(checked = saveForReuse, onCheckedChange = { saveForReuse = it })
                        Text("Save signature locally for reuse", style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        enabled = strokes.isNotEmpty(),
                        onClick = {
                            val serialized = PdfEngine.serializeSignatureStrokes(strokes)
                            onConfirmSignature(serialized, selectedColorHex, saveForReuse)
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("signature_place_button"),
                    ) {
                        Text("Tap Page to Place Signature")
                    }
                } else {
                    Text(
                        text = "Choose a stamp, then tap anywhere on the PDF page to place it:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        stamps.forEach { (label, colorHex) ->
                            Surface(
                                onClick = { onConfirmStamp(label, colorHex) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(colorHex).copy(alpha = 0.12f),
                                border = BorderStroke(1.2.dp, Color(colorHex)),
                            ) {
                                Text(
                                    text = label,
                                    color = Color(colorHex),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class OrganizerPageItem(
    val id: Int,
    val sourceIndex: Int,
    val rotationDeg: Int = 0,
    val selected: Boolean = false,
)

@Composable
fun PdfPageOrganizerDialog(
    pdfFile: File,
    pageCount: Int,
    onDismiss: () -> Unit,
    onApplyChanges: (ops: List<PdfPageOperation>, replaceOriginal: Boolean, splitTitle: String) -> Unit,
) {
    val pages = remember(pdfFile.absolutePath, pageCount) {
        mutableStateListOf<OrganizerPageItem>().apply {
            for (i in 0 until pageCount) add(OrganizerPageItem(id = i, sourceIndex = i))
        }
    }
    var focusedIdx by remember { mutableIntStateOf(0) }
    var splitTitle by remember { mutableStateOf("${pdfFile.nameWithoutExtension} Split") }
    var showSplitNameDialog by remember { mutableStateOf(false) }

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
                        Text("Organize & Split PDF", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${pages.size} pages • Tap to focus or check to split",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(pages, key = { _, it -> it.id }) { idx, item ->
                        var thumb by remember(item.sourceIndex, pdfFile.lastModified()) {
                            mutableStateOf<Bitmap?>(PdfEngine.getCachedPage(pdfFile, item.sourceIndex, powerSavingMode = false, thumbnail = true))
                        }
                        LaunchedEffect(item.sourceIndex) {
                            if (thumb == null) {
                                thumb = withContext(Dispatchers.IO) {
                                    PdfEngine.renderPageBitmap(pdfFile, item.sourceIndex, powerSavingMode = false, thumbnail = true)
                                }
                            }
                        }

                        val isFocused = idx == focusedIdx
                        Surface(
                            onClick = { focusedIdx = idx },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = if (isFocused) 2.dp else 0.8.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White),
                                ) {
                                    thumb?.let { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Page ${item.sourceIndex + 1}",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer { rotationZ = item.rotationDeg.toFloat() },
                                        )
                                    }
                                    Checkbox(
                                        checked = item.selected,
                                        onCheckedChange = { checked ->
                                            pages[idx] = item.copy(selected = checked)
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    )
                                }
                                Text(
                                    text = "P.${item.sourceIndex + 1}" + if (item.rotationDeg % 360 != 0) " (${item.rotationDeg}°)" else "",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // Focused Page Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(
                        enabled = focusedIdx > 0,
                        onClick = {
                            val moving = pages.removeAt(focusedIdx)
                            focusedIdx--
                            pages.add(focusedIdx, moving)
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Move page left")
                    }
                    IconButton(
                        enabled = focusedIdx < pages.lastIndex,
                        onClick = {
                            val moving = pages.removeAt(focusedIdx)
                            focusedIdx++
                            pages.add(focusedIdx, moving)
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Move page right")
                    }
                    IconButton(
                        enabled = focusedIdx in pages.indices,
                        onClick = {
                            val cur = pages[focusedIdx]
                            pages[focusedIdx] = cur.copy(rotationDeg = (cur.rotationDeg + 90) % 360)
                        },
                    ) {
                        Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90 degrees")
                    }
                    IconButton(
                        enabled = pages.size > 1 && focusedIdx in pages.indices,
                        onClick = {
                            pages.removeAt(focusedIdx)
                            focusedIdx = focusedIdx.coerceAtMost(pages.lastIndex)
                        },
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove page", tint = MaterialTheme.colorScheme.error)
                    }
                }

                val selectedPages = pages.filter { it.selected }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        enabled = selectedPages.isNotEmpty(),
                        onClick = { showSplitNameDialog = true },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.CallSplit, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Split (${selectedPages.size})", maxLines = 1)
                    }
                    Button(
                        enabled = pages.isNotEmpty(),
                        onClick = {
                            onApplyChanges(
                                pages.map { PdfPageOperation(it.sourceIndex, it.rotationDeg) },
                                true,
                                "",
                            )
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text("Apply Order", maxLines = 1)
                    }
                }
            }
        }
    }

    if (showSplitNameDialog) {
        val selectedOps = pages.filter { it.selected }.map { PdfPageOperation(it.sourceIndex, it.rotationDeg) }
        AlertDialog(
            onDismissRequest = { showSplitNameDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Extract ${selectedOps.size} Page(s)") },
            text = {
                OutlinedTextField(
                    value = splitTitle,
                    onValueChange = { splitTitle = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("New PDF Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSplitNameDialog = false
                    onApplyChanges(selectedOps, false, splitTitle)
                }) {
                    Text("Create Split PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSplitNameDialog = false }) { Text("Cancel") }
            },
        )
    }
}
