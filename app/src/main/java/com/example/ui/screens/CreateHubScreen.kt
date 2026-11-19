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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FileUpload
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
import com.example.data.repository.TemplateSpec
import com.example.ui.theme.DocumentFormatBadge
import com.example.ui.theme.LiquidGlassCard

@Composable
fun CreateHubScreen(
    templates: List<TemplateSpec>,
    onCreateBlank: (String, DocumentType) -> Unit,
    onCreateFromTemplate: (String) -> Unit,
    onImportFromDevice: () -> Unit,
) {
    var pendingCreateType by remember { mutableStateOf<DocumentType?>(null) }
    var docTitleInput by remember { mutableStateOf("") }

    val quickFormats = remember {
        listOf(
            DocumentType.PDF,
            DocumentType.XLSX,
            DocumentType.DOCX,
            DocumentType.PPTX,
            DocumentType.MARKDOWN,
            DocumentType.CSV,
            DocumentType.JSON,
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
                    text = "Create Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Start blank, launch a smart template, or import from device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Import Local File Banner
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_import_card"),
                elevated = true,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(16.dp),
                onClick = onImportFromDevice,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = "Import file from device",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open Document from Device",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Supports PDF, XLSX, CSV, DOCX, PPTX, MD, JSON, HTML & TXT",
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

        // Quick Blank Formats (2-column grid rows)
        item {
            Text(
                text = "Blank Document",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(quickFormats.chunked(2)) { pair ->
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
                            docTitleInput = "New ${type.displayName}"
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

        // Curated Templates Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Ready-to-Use Templates",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(templates, key = { it.id }) { tpl ->
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("template_${tpl.id}"),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(14.dp),
                onClick = { onCreateFromTemplate(tpl.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DocumentFormatBadge(type = tpl.type, size = 44.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tpl.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = tpl.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "Create ${tpl.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    label = { Text("Document name") },
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
