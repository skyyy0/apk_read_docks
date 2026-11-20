package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentType
import com.example.ui.theme.DocumentFormatBadge
import com.example.ui.theme.EditorialSectionSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuminaAppearance
import com.example.ui.theme.bouncyPress

@Composable
fun CreateHubScreen(
    onCreateBlank: (String, DocumentType) -> Unit,
    onImportSingleFile: () -> Unit,
    onImportMultipleFiles: () -> Unit,
    onOpenScanToPdf: () -> Unit,
) {
    val appearance = LocalLuminaAppearance.current
    val isDark = appearance.isDark
    val haptics = LocalHapticFeedback.current
    val surfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f)
    }

    var pendingCreateType by remember { mutableStateOf<DocumentType?>(null) }
    var docTitleInput by remember { mutableStateOf("") }

    val blankFormats = remember {
        listOf(
            DocumentType.PDF,
            DocumentType.XLSX,
            DocumentType.DOCX,
            DocumentType.PPTX,
            DocumentType.CSV,
            DocumentType.MARKDOWN,
            DocumentType.HTML,
            DocumentType.CSS,
            DocumentType.JS,
            DocumentType.CODE,
            DocumentType.JSON,
            DocumentType.TEXT,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("create_hub_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                Text(
                    text = "Create & Open",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Open documents from device storage or start with a blank file",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Grouped Device Storage Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "FROM DEVICE STORAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                EditorialSectionSurface(
                    shape = RoundedCornerShape(26.dp),
                    tonalLevel = surfaceColor,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyPress(pressedScale = 0.985f)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onImportSingleFile()
                                }
                                .padding(16.dp)
                                .testTag("create_import_single_card"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileOpen,
                                    contentDescription = "Open file from device",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open File from Device",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "PDF, Excel (.xlsx/.csv), Word (.docx), Slides (.pptx), MD, JSON & Text",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyPress(pressedScale = 0.985f)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onImportMultipleFiles()
                                }
                                .padding(16.dp)
                                .testTag("create_import_batch_card"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryAdd,
                                    contentDescription = "Batch import multiple files",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Batch Import Multiple Files",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Select several documents at once from Downloads, Drive, or SD storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyPress(pressedScale = 0.985f)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenScanToPdf()
                                }
                                .padding(16.dp)
                                .testTag("create_scan_to_pdf_card"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DocumentScanner,
                                    contentDescription = "Scan photos to PDF",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scan Photos to PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Pick multiple gallery photos, apply B&W scan filter & export PDF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // Clean Blank Document Creators
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "NEW BLANK DOCUMENT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }

        items(blankFormats.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { type ->
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pendingCreateType = type
                            docTitleInput = ""
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = surfaceColor,
                        border = BorderStroke(0.6.dp, appearance.glassSpecularEdge.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .bouncyPress()
                            .testTag("create_blank_${type.extension}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DocumentFormatBadge(type = type, size = 40.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = ".${type.extension}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    pendingCreateType?.let { type ->
        AlertDialog(
            onDismissRequest = { pendingCreateType = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("New ${type.displayName}") },
            text = {
                OutlinedTextField(
                    value = docTitleInput,
                    onValueChange = { docTitleInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Untitled ${type.shortBadge}") },
                    label = { Text("File name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateBlank(docTitleInput, type)
                        pendingCreateType = null
                    },
                    modifier = Modifier.testTag("confirm_create_blank"),
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCreateType = null }) { Text("Cancel") }
            },
        )
    }
}
