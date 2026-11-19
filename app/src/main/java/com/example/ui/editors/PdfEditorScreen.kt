package com.example.ui.editors

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PdfEngine
import com.example.ui.theme.FormatSlidesColor
import com.example.ui.theme.GlassToolbarSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.viewmodel.ActiveEditorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PdfEditorScreen(
    state: ActiveEditorState.PdfEditor,
    powerSavingMode: Boolean,
    onClose: () -> Unit,
    onPageChange: (Int) -> Unit,
    onToggleNightInvert: () -> Unit,
    onToggleBookmark: () -> Unit,
    onAddAnnotation: (kind: String, content: String, normX: Float, normY: Float, colorHex: Long) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    onAppendNewPage: (title: String, body: String) -> Unit,
    onDeleteCurrentPage: () -> Unit,
    onBakeAndSave: () -> Unit,
    onShare: () -> Unit,
) {
    BackHandler(onBack = onClose)

    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var activeTool by remember { mutableStateOf<String?>(null) }
    var pendingPlacement by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var annotationText by remember { mutableStateOf("") }
    var showAddPageDialog by remember { mutableStateOf(false) }
    var showJumpPageDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var newPageTitle by remember { mutableStateOf("") }
    var newPageBody by remember { mutableStateOf("") }

    val pageAnnotations = remember(state.annotations, state.currentPage) {
        state.annotations.filter { it.pageIndex == state.currentPage }
    }
    val isBookmarked = remember(state.bookmarks, state.currentPage) {
        state.bookmarks.any { it.targetIndex == state.currentPage }
    }

    LaunchedEffect(state.document.filePath, state.currentPage, powerSavingMode, state.renderVersion) {
        isRendering = true
        val rendered = withContext(Dispatchers.IO) {
            PdfEngine.renderPageBitmap(
                file = File(state.document.filePath),
                pageIndex = state.currentPage,
                powerSavingMode = powerSavingMode,
            )
        }
        pageBitmap = rendered
        isRendering = false
    }

    val invertFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Translucent Top Editor Toolbar
        GlassToolbarSurface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("editor_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to library")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.document.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Page ${state.currentPage + 1} / ${state.pageCount}" +
                            if (powerSavingMode) "  •  Eco DPI" else "  •  HD PDF",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleNightInvert, modifier = Modifier.testTag("pdf_night_toggle")) {
                    Icon(
                        imageVector = if (state.nightInvert) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Toggle Night-Ink Invert",
                        tint = if (state.nightInvert) FormatSlidesColor else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag("pdf_bookmark_toggle")) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark current page",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Rounded.IosShare, contentDescription = "Share PDF")
                }
                IconButton(
                    onClick = onBakeAndSave,
                    modifier = Modifier.testTag("pdf_save_button"),
                ) {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = "Save annotations into PDF",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Active Annotation Tool Banner
        if (activeTool != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Tap anywhere on the page to place $activeTool",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    IconButton(onClick = { activeTool = null }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel placement",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        // Bookmarks Quick Jump Row
        if (state.bookmarks.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp)
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.bookmarks, key = { it.id }) { bm ->
                    FilterChip(
                        selected = bm.targetIndex == state.currentPage,
                        onClick = { onPageChange(bm.targetIndex) },
                        label = { Text(bm.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        // PDF Page Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 880.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = pageBitmap
            if (isRendering || bmp == null) {
                CircularProgressIndicator()
            } else {
                val aspect = (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.45f, 2.2f)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect)
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.nightInvert) Color(0xFF0F172A) else Color.White)
                        .border(0.75.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .pointerInput(activeTool) {
                            detectTapGestures { offset ->
                                if (activeTool != null && size.width > 0 && size.height > 0) {
                                    pendingPlacement = (offset.x / size.width) to (offset.y / size.height)
                                    annotationText = ""
                                }
                            }
                        },
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF Page ${state.currentPage + 1}",
                        contentScale = ContentScale.FillBounds,
                        colorFilter = if (state.nightInvert) invertFilter else null,
                        modifier = Modifier.fillMaxSize(),
                    )

                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()

                    pageAnnotations.forEach { ann ->
                        val bgColor = when (ann.kind) {
                            "HIGHLIGHT" -> Color(0x99FDE047)
                            "NOTE" -> Color(0xFFFEF08A)
                            else -> Color.White.copy(alpha = 0.90f)
                        }
                        val textColor = when (ann.kind) {
                            "NOTE" -> Color(0xFF422006)
                            else -> Color(ann.colorHex)
                        }
                        Row(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (ann.normX * widthPx).roundToInt().coerceIn(8, (widthPx - 120f).roundToInt().coerceAtLeast(8)),
                                        y = (ann.normY * heightPx).roundToInt().coerceIn(8, (heightPx - 60f).roundToInt().coerceAtLeast(8)),
                                    )
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { onDeleteAnnotation(ann.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = ann.content,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove annotation",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }

        // Bottom Translucent PDF Studio Controls
        GlassToolbarSurface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            enabled = state.currentPage > 0,
                            onClick = { onPageChange(state.currentPage - 1) },
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous page")
                        }
                        Text(
                            text = "${state.currentPage + 1} / ${state.pageCount}",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFamily,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    jumpPageInput = "${state.currentPage + 1}"
                                    showJumpPageDialog = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                        IconButton(
                            enabled = state.currentPage < state.pageCount - 1,
                            onClick = { onPageChange(state.currentPage + 1) },
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next page")
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f) }) {
                            Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out")
                        }
                        Text(
                            text = "${(zoomScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                        )
                        IconButton(onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f) }) {
                            Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in")
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        PdfToolChip(
                            icon = Icons.Rounded.TextFields,
                            label = "Add Text",
                            selected = activeTool == "TEXT",
                            onClick = { activeTool = if (activeTool == "TEXT") null else "TEXT" },
                            testTag = "pdf_tool_text",
                        )
                    }
                    item {
                        PdfToolChip(
                            icon = Icons.Rounded.BorderColor,
                            label = "Highlight",
                            selected = activeTool == "HIGHLIGHT",
                            onClick = { activeTool = if (activeTool == "HIGHLIGHT") null else "HIGHLIGHT" },
                            testTag = "pdf_tool_highlight",
                        )
                    }
                    item {
                        PdfToolChip(
                            icon = Icons.Rounded.StickyNote2,
                            label = "Sticky Note",
                            selected = activeTool == "NOTE",
                            onClick = { activeTool = if (activeTool == "NOTE") null else "NOTE" },
                            testTag = "pdf_tool_note",
                        )
                    }
                    item {
                        PdfToolChip(
                            icon = Icons.Rounded.NoteAdd,
                            label = "+ Page",
                            selected = false,
                            onClick = { showAddPageDialog = true },
                            testTag = "pdf_tool_add_page",
                        )
                    }
                    if (state.pageCount > 1) {
                        item {
                            PdfToolChip(
                                icon = Icons.Rounded.DeleteOutline,
                                label = "Del Page",
                                selected = false,
                                onClick = onDeleteCurrentPage,
                                testTag = "pdf_tool_delete_page",
                            )
                        }
                    }
                }
            }
        }
    }

    if (showJumpPageDialog) {
        AlertDialog(
            onDismissRequest = { showJumpPageDialog = false },
            title = { Text("Go to Page (1 – ${state.pageCount})") },
            text = {
                OutlinedTextField(
                    value = jumpPageInput,
                    onValueChange = { jumpPageInput = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Page number") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = (jumpPageInput.toIntOrNull() ?: 1) - 1
                    onPageChange(target)
                    showJumpPageDialog = false
                }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpPageDialog = false }) { Text("Cancel") }
            },
        )
    }

    pendingPlacement?.let { (nx, ny) ->
        val kind = activeTool ?: "TEXT"
        AlertDialog(
            onDismissRequest = { pendingPlacement = null },
            title = { Text("Add $kind to Page ${state.currentPage + 1}") },
            text = {
                OutlinedTextField(
                    value = annotationText,
                    onValueChange = { annotationText = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (annotationText.isNotBlank()) {
                        val colorHex = when (kind) {
                            "HIGHLIGHT" -> 0xFFFACC15
                            "NOTE" -> 0xFFEAB308
                            else -> 0xFF007AFF
                        }
                        onAddAnnotation(kind, annotationText, nx, ny, colorHex)
                    }
                    pendingPlacement = null
                    activeTool = null
                }) {
                    Text("Place")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPlacement = null }) { Text("Cancel") }
            },
        )
    }

    if (showAddPageDialog) {
        AlertDialog(
            onDismissRequest = { showAddPageDialog = false },
            title = { Text("Append New PDF Page") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPageTitle,
                        onValueChange = { newPageTitle = it },
                        singleLine = true,
                        label = { Text("Page Heading") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newPageBody,
                        onValueChange = { newPageBody = it },
                        minLines = 4,
                        label = { Text("Page Body Text") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAppendNewPage(newPageTitle, newPageBody)
                    showAddPageDialog = false
                    newPageBody = ""
                }) {
                    Text("Append & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPageDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PdfToolChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.testTag(testTag),
    )
}
