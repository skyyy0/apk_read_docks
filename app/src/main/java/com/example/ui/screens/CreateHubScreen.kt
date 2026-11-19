package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentType
import com.example.ui.theme.DocumentFormatBadge
import com.example.ui.theme.LiquidGlassCard

@Composable
fun CreateHubScreen(
    onCreateBlank: (String, DocumentType) -> Unit,
    onImportSingleFile: () -> Unit,
    onImportMultipleFiles: () -> Unit,
) {
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
            DocumentType.JSON,
            DocumentType.HTML,
            DocumentType.TEXT,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Create & Open",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Open real documents from device storage or create a clean blank file",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Open Single Document from Device
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_import_single_card"),
                elevated = true,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(16.dp),
                onClick = onImportSingleFile,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = "Open file from device",
                        tint = MaterialTheme.colorScheme.primary,
                    )
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
                    )
                }
            }
        }

        // Batch Import Multiple Documents from Device
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_import_batch_card"),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(16.dp),
                onClick = onImportMultipleFiles,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryAdd,
                        contentDescription = "Batch import multiple files",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
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
                    )
                }
            }
        }

        // Clean Blank Document Creators
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "New Blank Document",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(blankFormats.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { type ->
                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_blank_${type.extension}"),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(14.dp),
                        onClick = {
                            pendingCreateType = type
                            docTitleInput = ""
                        },
                    ) {
                        Row(
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
            title = { Text("New ${type.displayName}") },
            text = {
                OutlinedTextField(
                    value = docTitleInput,
                    onValueChange = { docTitleInput = it },
                    singleLine = true,
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
