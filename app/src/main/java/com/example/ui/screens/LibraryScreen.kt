package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentType
import com.example.data.model.SortOrder
import com.example.ui.theme.DocumentFormatBadge
import com.example.ui.theme.EditorialSectionSurface
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.FormatMarkdownColor
import com.example.ui.theme.FormatPdfColor
import com.example.ui.theme.FormatSlidesColor
import com.example.ui.theme.FormatWordColor
import com.example.ui.theme.GlassToolbarSurface
import com.example.ui.theme.JetBrainsMonoFamily
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
    val isDark = LocalLuminaAppearance.current.isDark
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val pinnedDocs = remember(allDocuments) { allDocuments.filter { it.isPinned } }
    val totalSize = remember(allDocuments) { allDocuments.sumOf { it.sizeBytes } }
    val listSurfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Editorial Workspace Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FormatExcelColor.copy(alpha = 0.14f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
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
                        text = "${allDocuments.size} documents  •  ${formatByteSize(totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(
                        onClick = onToggleFavoritesFilter,
                        modifier = Modifier.testTag("favorites_filter_button"),
                    ) {
                        Icon(
                            imageVector = if (filterState.favoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = "Filter Favorites",
                            tint = if (filterState.favoritesOnly) FormatSlidesColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onImportDeviceFile,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("import_device_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        // Restrained Translucent Search Bar + Sort IconButton
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassToolbarSurface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
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
                                    .size(20.dp)
                                    .clickable { onSearchChange("") },
                            )
                        }
                    }
                }

                Box {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    ) {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("sort_menu_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sort,
                                contentDescription = "Sort documents",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
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

        // Native M3 Format FilterChips
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
                    val selected = filterState.category == cat
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelect(cat) },
                        label = {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        },
                        leadingIcon = if (dotColor != null) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor),
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
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
                        modifier = Modifier.padding(horizontal = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Pinned",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(pinnedDocs, key = { "pin_${it.id}" }) { doc ->
                            Surface(
                                onClick = { onOpenDocument(doc) },
                                shape = RoundedCornerShape(16.dp),
                                color = listSurfaceColor,
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .width(184.dp)
                                    .testTag("pinned_doc_${doc.id}"),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        DocumentFormatBadge(type = doc.docType, size = 38.dp)
                                        Text(
                                            text = formatByteSize(doc.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = doc.previewSnippet.ifBlank { doc.docType.displayName },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        // Section Label
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (filterState.category == DocumentCategory.ALL) "Documents" else "${filterState.category.label} Files",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${filteredDocuments.size} item(s)",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Clean Empty State or Grouped Editorial Document List
        if (filteredDocuments.isEmpty()) {
            item {
                EditorialSectionSurface(
                    shape = RoundedCornerShape(20.dp),
                    tonalLevel = listSurfaceColor,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "Empty workspace",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = if (allDocuments.isEmpty()) "No Documents Yet" else "No matching documents",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (allDocuments.isEmpty()) {
                                "Open PDF, Excel, Word, PowerPoint, or Text files from storage, or create a clean blank file."
                            } else {
                                "Try clearing your search query or switching category filters."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (allDocuments.isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onImportDeviceFile,
                                    modifier = Modifier.testTag("empty_import_button"),
                                ) {
                                    Text("Open File")
                                }
                                OutlinedButton(
                                    onClick = onJumpToCreate,
                                    modifier = Modifier.testTag("empty_create_button"),
                                ) {
                                    Text("Create Blank")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "grouped_document_list") {
                EditorialSectionSurface(
                    shape = RoundedCornerShape(20.dp),
                    tonalLevel = listSurfaceColor,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredDocuments.forEachIndexed { index, doc ->
                            DocumentListRow(
                                doc = doc,
                                onOpen = { onOpenDocument(doc) },
                                onToggleFavorite = { onToggleFavorite(doc) },
                                onMoreActions = { actionTarget = doc },
                            )
                            if (index < filteredDocuments.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp, end = 14.dp),
                                    thickness = 0.75.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet for Document Actions
    actionTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { actionTarget = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            .testTag("document_card_${doc.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocumentFormatBadge(type = doc.docType, size = 44.dp)
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
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                imageVector = if (doc.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                contentDescription = "Toggle favorite",
                tint = if (doc.isFavorite) FormatSlidesColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onMoreActions,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More options for ${doc.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}
