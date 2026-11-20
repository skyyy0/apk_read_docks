package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.ScanFilterMode
import com.example.engine.ScanPageInput
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun ScanToPdfDialog(
    initialUris: List<Uri>,
    onPickMorePhotos: () -> Unit,
    onDismiss: () -> Unit,
    onBuildPdf: (title: String, pages: List<ScanPageInput>, filterMode: ScanFilterMode) -> Unit,
) {
    val pages = remember(initialUris) {
        mutableStateListOf<ScanPageInput>().apply {
            initialUris.forEach { add(ScanPageInput(it)) }
        }
    }
    var pdfTitle by remember { mutableStateOf("Scanned Document") }
    var filterMode by remember { mutableStateOf(ScanFilterMode.DOCUMENT_BW) }

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
                        Text("Photos to PDF Scanner", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${pages.size} photo(s) selected • Zero-permission Photo Picker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }

                OutlinedTextField(
                    value = pdfTitle,
                    onValueChange = { pdfTitle = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Output PDF Title") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Scan Filter", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanFilterMode.entries.forEach { mode ->
                        FilterChip(
                            selected = filterMode == mode,
                            onClick = { filterMode = mode },
                            shape = CircleShape,
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(pages) { idx, item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Page ${idx + 1}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Rotate ${item.rotationDeg}° • ${item.uri.lastPathSegment ?: "Photo"}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    enabled = idx > 0,
                                    onClick = {
                                        val moved = pages.removeAt(idx)
                                        pages.add(idx - 1, moved)
                                    },
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                                }
                                IconButton(
                                    enabled = idx < pages.lastIndex,
                                    onClick = {
                                        val moved = pages.removeAt(idx)
                                        pages.add(idx + 1, moved)
                                    },
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                                }
                                IconButton(
                                    onClick = {
                                        pages[idx] = item.copy(rotationDeg = (item.rotationDeg + 90) % 360)
                                    },
                                ) {
                                    Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90°")
                                }
                                IconButton(
                                    enabled = pages.size > 1,
                                    onClick = { pages.removeAt(idx) },
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove photo", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onPickMorePhotos,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text("Repick Photos")
                    }
                    Button(
                        enabled = pages.isNotEmpty(),
                        onClick = { onBuildPdf(pdfTitle, pages.toList(), filterMode) },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("build_scan_pdf_button"),
                    ) {
                        Text("Create PDF (${pages.size})")
                    }
                }
            }
        }
    }
}
