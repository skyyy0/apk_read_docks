package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentType
import com.example.ui.theme.DocumentFormatBadge
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.FormatMarkdownColor
import com.example.ui.theme.FormatPdfColor
import com.example.ui.theme.FormatSlidesColor
import com.example.ui.theme.FormatWordColor
import com.example.ui.theme.GlassFilterPill
import com.example.ui.theme.LiquidGlassCard
import com.example.viewmodel.formatByteSize

@Composable
fun ToolsHubScreen(
    allDocuments: List<DocumentEntity>,
    onConvertDocument: (DocumentEntity, DocumentType) -> Unit,
    onMergePdfs: (List<DocumentEntity>, String) -> Unit,
) {
    val convertibleDocs = remember(allDocuments) {
        allDocuments.filter { it.docType != DocumentType.PDF }
    }
    var selectedSourceId by remember(convertibleDocs) {
        mutableStateOf(convertibleDocs.firstOrNull()?.id)
    }
    val selectedSource = convertibleDocs.firstOrNull { it.id == selectedSourceId }

    val availableTargets = remember(selectedSource) {
        when (selectedSource?.docType) {
            DocumentType.DOCX -> listOf(DocumentType.PDF, DocumentType.MARKDOWN, DocumentType.HTML)
            DocumentType.XLSX -> listOf(DocumentType.PDF, DocumentType.CSV)
            DocumentType.CSV -> listOf(DocumentType.XLSX, DocumentType.PDF)
            DocumentType.PPTX -> listOf(DocumentType.PDF)
            DocumentType.MARKDOWN, DocumentType.TEXT -> listOf(DocumentType.PDF, DocumentType.DOCX, DocumentType.HTML)
            DocumentType.JSON, DocumentType.HTML -> listOf(DocumentType.PDF)
            else -> emptyList()
        }
    }
    var targetType by remember(availableTargets) {
        mutableStateOf(availableTargets.firstOrNull() ?: DocumentType.PDF)
    }

    val pdfDocs = remember(allDocuments) { allDocuments.filter { it.docType == DocumentType.PDF } }
    val selectedPdfIds = remember { mutableStateListOf<Long>() }
    var mergedPdfTitle by remember { mutableStateOf("Combined Document") }

    val totalBytes = remember(allDocuments) { allDocuments.sumOf { it.sizeBytes }.coerceAtLeast(1L) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Document Lab",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Universal format conversion, PDF merging & workspace inspector",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 1. Universal Format Converter
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevated = true,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(
                                text = "Universal Format Converter",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Convert Word, Excel, Slides, Markdown or CSV natively offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (convertibleDocs.isEmpty()) {
                        Text(
                            text = "Create or import a Word, Excel, Slides, or Markdown file first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "1. Select source file",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(convertibleDocs, key = { it.id }) { doc ->
                                GlassFilterPill(
                                    label = "${doc.title.take(18)} (${doc.docType.shortBadge})",
                                    selected = doc.id == selectedSourceId,
                                    onClick = { selectedSourceId = doc.id },
                                )
                            }
                        }

                        if (availableTargets.isNotEmpty()) {
                            Text(
                                text = "2. Output format",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableTargets.forEach { format ->
                                    GlassFilterPill(
                                        label = format.shortBadge,
                                        selected = targetType == format,
                                        onClick = { targetType = format },
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    selectedSource?.let { onConvertDocument(it, targetType) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("convert_now_button"),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Convert to ${targetType.displayName}")
                            }
                        }
                    }
                }
            }
        }

        // 2. Multi-PDF Merger
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevated = true,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MergeType,
                            contentDescription = null,
                            tint = FormatPdfColor,
                        )
                        Column {
                            Text(
                                text = "Multi-PDF Merger",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Combine 2 or more PDF files into one crisp document",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (pdfDocs.size < 2) {
                        Text(
                            text = "You currently have ${pdfDocs.size} PDF file(s). Convert any document to PDF or create another PDF to merge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pdfDocs.forEach { pdf ->
                                val checked = selectedPdfIds.contains(pdf.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (checked) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable {
                                            if (checked) selectedPdfIds.remove(pdf.id) else selectedPdfIds.add(pdf.id)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    DocumentFormatBadge(type = DocumentType.PDF, size = 34.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pdf.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${pdf.pageCount} page(s) • ${formatByteSize(pdf.sizeBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = mergedPdfTitle,
                            onValueChange = { mergedPdfTitle = it },
                            singleLine = true,
                            label = { Text("Merged PDF name") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Button(
                            enabled = selectedPdfIds.size >= 2,
                            onClick = {
                                val chosen = selectedPdfIds.mapNotNull { id -> pdfDocs.firstOrNull { it.id == id } }
                                onMergePdfs(chosen, mergedPdfTitle)
                                selectedPdfIds.clear()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("merge_pdfs_button"),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Merge ${selectedPdfIds.size} Selected PDFs")
                        }
                    }
                }
            }
        }

        // 3. Workspace Storage & Format Inspector
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(
                                text = "Workspace Storage Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${allDocuments.size} local files  •  ${formatByteSize(allDocuments.sumOf { it.sizeBytes })}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    val segments = listOf(
                        Triple("PDF", allDocuments.filter { it.docType.category == DocumentCategory.PDF }.sumOf { it.sizeBytes }, FormatPdfColor),
                        Triple("Excel", allDocuments.filter { it.docType.category == DocumentCategory.SPREADSHEET }.sumOf { it.sizeBytes }, FormatExcelColor),
                        Triple("Word", allDocuments.filter { it.docType.category == DocumentCategory.WORD }.sumOf { it.sizeBytes }, FormatWordColor),
                        Triple("Slides", allDocuments.filter { it.docType.category == DocumentCategory.SLIDES }.sumOf { it.sizeBytes }, FormatSlidesColor),
                        Triple("Notes", allDocuments.filter { it.docType.category == DocumentCategory.NOTES }.sumOf { it.sizeBytes }, FormatMarkdownColor),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        segments.forEach { (_, bytes, color) ->
                            val frac = (bytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            if (frac > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .weight(frac)
                                        .fillMaxHeight()
                                        .background(color),
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        segments.forEach { (label, bytes, color) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                                Text(
                                    text = formatByteSize(bytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
