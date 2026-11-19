package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentType
import com.example.data.model.SortOrder
import com.example.ui.theme.DocumentFormatBadge
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.FormatMarkdownColor
import com.example.ui.theme.FormatPdfColor
import com.example.ui.theme.FormatSlidesColor
import com.example.ui.theme.FormatWordColor
import com.example.ui.theme.GlassFilterPill
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLuminaAppearance
import com.example.viewmodel.LibraryFilterState
import com.example.viewmodel.formatByteSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    allDocuments: List<DocumentEntity>,
    filteredDocuments: List<DocumentEntity>,
    filterState: LibraryFilterState,
    powerSavingMode: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (DocumentCategory) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onOpenDocument: (DocumentEntity) -> Unit,
    onImportDeviceFile: () -> Unit,
    onJumpToCreate: () -> Unit,
    onRenameDocument: (DocumentEntity, String) -> Unit,
    onDuplicateDocument: (DocumentEntity) -> Unit,
    onTogglePin: (DocumentEntity) -> Unit,
    onToggleFavorite: (DocumentEntity) -> Unit,
    onConvertToPdf: (DocumentEntity) -> Unit,
    onExportDocument: (DocumentEntity) -> Unit,
    onShareDocument: (DocumentEntity) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
) {
    val appearance = LocalLuminaAppearance.current
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val pinnedDocs = remember(allDocuments) { allDocuments.filter { it.isPinned } }
    val totalSize = remember(allDocuments) { allDocuments.sumOf { it.sizeBytes } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Brand Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Lumina Docs",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (powerSavingMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(FormatExcelColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BatteryChargingFull,
                                    contentDescription = "Power Saving Active",
                                    tint = FormatExcelColor,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "ECO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FormatExcelColor,
                                )
                            }
                        }
                    }
                    Text(
                        text = "${allDocuments.size} documents  •  ${formatByteSize(totalSize)}  •  by sky",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassCard(
                        shape = CircleShape,
                        contentPadding = PaddingValues(10.dp),
                        onClick = onToggleFavoritesFilter,
                        modifier = Modifier.testTag("favorites_filter_button"),
                    ) {
                        Icon(
                            imageVector = if (filterState.favoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = "Filter Favorites",
                            tint = if (filterState.favoritesOnly) FormatSlidesColor else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    LiquidGlassCard(
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = onImportDeviceFile,
                        modifier = Modifier.testTag("import_device_button"),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.FileOpen,
                                contentDescription = "Import file from device",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // Signature Liquid Glass Studio Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .clip(RoundedCornerShape(26.dp)),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_liquid_glass_1795071801139),
                    contentDescription = "Lumina Docs liquid glass workspace banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = if (appearance.isDark) {
                                    listOf(
                                        Color(0xFF0A1222).copy(alpha = 0.88f),
                                        Color(0xFF0F1B33).copy(alpha = 0.56f),
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF0F1E38).copy(alpha = 0.80f),
                                        Color(0xFF1E3A6D).copy(alpha = 0.42f),
                                    )
                                }
                            )
                        )
                        .padding(18.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "UNIVERSAL STUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF93C5FD),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Read, Edit & Convert Everything",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                            )
                            Text(
                                text = "PDF  •  Excel XLSX  •  Word DOCX  •  Slides  •  Markdown",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable(onClick = onJumpToCreate)
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                    .testTag("hero_new_doc_button"),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "New Document",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "New Document",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFF0F172A),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .clickable(onClick = onImportDeviceFile)
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                    .testTag("hero_open_file_button"),
                            ) {
                                Text(
                                    text = "Open Local File",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Frosted Glass Search Pill + Sort Trigger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiquidGlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search documents",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (filterState.query.isEmpty()) {
                                Text(
                                    text = "Search PDF, Excel, Word, Notes...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BasicTextField(
                                value = filterState.query,
                                onValueChange = onSearchChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("library_search_input"),
                            )
                        }
                        AnimatedVisibility(visible = filterState.query.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onSearchChange("") },
                            )
                        }
                    }
                }

                Box {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(12.dp),
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.testTag("sort_menu_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = "Sort documents",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Sort by ${order.label}",
                                        fontWeight = if (order == filterState.sortOrder) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onSortChange(order)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        // Format Filter Pills Strip
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(DocumentCategory.entries) { cat ->
                    val dotColor = when (cat) {
                        DocumentCategory.ALL -> null
                        DocumentCategory.PDF -> FormatPdfColor
                        DocumentCategory.SPREADSHEET -> FormatExcelColor
                        DocumentCategory.WORD -> FormatWordColor
                        DocumentCategory.SLIDES -> FormatSlidesColor
                        DocumentCategory.NOTES -> FormatMarkdownColor
                    }
                    GlassFilterPill(
                        label = cat.label,
                        selected = filterState.category == cat,
                        leadingDotColor = dotColor,
                        onClick = { onCategorySelect(cat) },
                        modifier = Modifier.testTag("filter_pill_${cat.name.lowercase()}"),
                    )
                }
            }
        }

        // Pinned Quick Access Shelf
        if (pinnedDocs.isNotEmpty() && filterState.query.isBlank() && filterState.category == DocumentCategory.ALL) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Pinned Workspace",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(pinnedDocs, key = { "pin_${it.id}" }) { doc ->
                            LiquidGlassCard(
                                modifier = Modifier
                                    .width(196.dp)
                                    .testTag("pinned_doc_${doc.id}"),
                                elevated = true,
                                shape = RoundedCornerShape(22.dp),
                                contentPadding = PaddingValues(14.dp),
                                onClick = { onOpenDocument(doc) },
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        DocumentFormatBadge(type = doc.docType, size = 40.dp)
                                        Text(
                                            text = formatByteSize(doc.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = doc.previewSnippet.ifBlank { doc.docType.displayName },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
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

        // Section Label
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (filterState.category == DocumentCategory.ALL) "All Documents" else "${filterState.category.label} Files",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${filteredDocuments.size} item(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Empty State or Document Rows
        if (filteredDocuments.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(24.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "Empty workspace",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp),
                        )
                        Text(
                            text = if (allDocuments.isEmpty()) "Your Workspace is Ready" else "No matching documents",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (allDocuments.isEmpty()) {
                                "Open real PDF, Excel, Word, PowerPoint, or Text files from your phone storage, or create a brand-new blank file."
                            } else {
                                "Try clearing your search or category filter."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (allDocuments.isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable(onClick = onImportDeviceFile)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .testTag("empty_import_button"),
                                ) {
                                    Text(
                                        text = "Open File",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable(onClick = onJumpToCreate)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .testTag("empty_create_button"),
                                ) {
                                    Text(
                                        text = "Create Blank",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredDocuments, key = { it.id }) { doc ->
                DocumentListRow(
                    doc = doc,
                    onOpen = { onOpenDocument(doc) },
                    onToggleFavorite = { onToggleFavorite(doc) },
                    onMoreActions = { actionTarget = doc },
                )
            }
        }
    }

    // Bottom Sheet for Document Actions
    actionTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { actionTarget = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    DocumentFormatBadge(type = target.docType, size = 44.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${target.docType.displayName}  •  ${formatByteSize(target.sizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                SheetActionRow(Icons.Rounded.PushPin, if (target.isPinned) "Unpin from top" else "Pin to top") {
                    onTogglePin(target)
                    actionTarget = null
                }
                SheetActionRow(Icons.Rounded.DriveFileRenameOutline, "Rename document") {
                    renameTarget = target
                    renameInput = target.title
                    actionTarget = null
                }
                SheetActionRow(Icons.Rounded.ContentCopy, "Duplicate copy") {
                    onDuplicateDocument(target)
                    actionTarget = null
                }
                if (target.docType != DocumentType.PDF) {
                    SheetActionRow(Icons.Rounded.PictureAsPdf, "Convert to PDF") {
                        onConvertToPdf(target)
                        actionTarget = null
                    }
                }
                SheetActionRow(Icons.Rounded.SaveAlt, "Export to device storage") {
                    onExportDocument(target)
                    actionTarget = null
                }
                SheetActionRow(Icons.Rounded.IosShare, "Share file") {
                    onShareDocument(target)
                    actionTarget = null
                }
                SheetActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Delete document",
                    tint = MaterialTheme.colorScheme.error,
                ) {
                    onDeleteDocument(target)
                    actionTarget = null
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Rename Dialog
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("Document title") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameInput.isNotBlank()) onRenameDocument(target, renameInput)
                    renameTarget = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DocumentListRow(
    doc: DocumentEntity,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreActions: () -> Unit,
) {
    val dateLabel = remember(doc.updatedAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(doc.updatedAt))
    }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("document_card_${doc.id}"),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DocumentFormatBadge(type = doc.docType, size = 46.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (doc.isPinned) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                Text(
                    text = doc.previewSnippet.ifBlank { doc.fileName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = formatByteSize(doc.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (doc.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "Toggle favorite",
                    tint = if (doc.isFavorite) FormatSlidesColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onMoreActions) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options for ${doc.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}
