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
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentType
import com.example.ui.theme.DocumentFormatBadge
import com.example.ui.theme.EditorialSectionSurface
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.FormatMarkdownColor
import com.example.ui.theme.FormatPdfColor
import com.example.ui.theme.FormatSlidesColor
import com.example.ui.theme.FormatWordColor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuminaAppearance
import com.example.ui.theme.bouncyPress
import com.example.viewmodel.formatByteSize

@Composable
fun ToolsHubScreen(
    allDocuments: List<DocumentEntity>,
    onConvertDocument: (DocumentEntity, DocumentType) -> Unit,
    onMergePdfs: (List<DocumentEntity>, String) -> Unit,
    onCompressPdf: (DocumentEntity, Float, Int) -> Unit,
    onOpenScanToPdf: () -> Unit,
) {
    val isDark = LocalLuminaAppearance.current.isDark
    val haptics = LocalHapticFeedback.current
    val sectionColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.90f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f)
    }

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
            DocumentType.JSON, DocumentType.HTML, DocumentType.CSS, DocumentType.JS, DocumentType.CODE -> listOf(DocumentType.PDF)
            else -> emptyList()
        }
    }
    var targetType by remember(availableTargets) {
        mutableStateOf(availableTargets.firstOrNull() ?: DocumentType.PDF)
    }

    val pdfDocs = remember(allDocuments) { allDocuments.filter { it.docType == DocumentType.PDF } }
    val selectedPdfIds = remember { mutableStateListOf<Long>() }
    var mergedPdfTitle by remember { mutableStateOf("Combined Document") }
    var compressSourceId by remember(pdfDocs) { mutableStateOf(pdfDocs.firstOrNull()?.id) }
    val compressPresets = remember {
        listOf(
            Triple("WhatsApp Compact", 1.15f, 55),
            Triple("Balanced", 1.45f, 72),
            Triple("High Clarity", 1.85f, 86),
        )
    }
    var selectedCompressPresetIdx by remember { mutableStateOf(1) }

    val totalBytes = remember(allDocuments) { allDocuments.sumOf { it.sizeBytes }.coerceAtLeast(1L) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                Text(
                    text = "Document Lab",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Offline format conversion, PDF merging & workspace breakdown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 1. Universal Format Converter
        item {
            EditorialSectionSurface(
                shape = RoundedCornerShape(26.dp),
                tonalLevel = sectionColor,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Universal Format Converter",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Convert Word, Excel, Slides, Markdown or CSV offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (convertibleDocs.isEmpty()) {
                        Text(
                            text = "Open or create a Word, Excel, Slides, or Markdown file first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "1. Source file",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(convertibleDocs, key = { it.id }) { doc ->
                                FilterChip(
                                    selected = doc.id == selectedSourceId,
                                    onClick = { selectedSourceId = doc.id },
                                    shape = CircleShape,
                                    label = {
                                        Text(
                                            text = "${doc.title.take(18)} (${doc.docType.shortBadge})",
                                            maxLines = 1,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
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
                                    FilterChip(
                                        selected = targetType == format,
                                        onClick = { targetType = format },
                                        shape = CircleShape,
                                        label = { Text(format.shortBadge) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSource?.let { onConvertDocument(it, targetType) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .bouncyPress()
                                    .testTag("convert_now_button"),
                                shape = CircleShape,
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
            EditorialSectionSurface(
                shape = RoundedCornerShape(26.dp),
                tonalLevel = sectionColor,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(FormatPdfColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MergeType,
                                contentDescription = null,
                                tint = FormatPdfColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Multi-PDF Merger",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Combine 2 or more PDF files into one document",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (pdfDocs.size < 2) {
                        Text(
                            text = "You have ${pdfDocs.size} PDF file(s). Convert any file to PDF or open another PDF to merge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            pdfDocs.forEachIndexed { idx, pdf ->
                                val checked = selectedPdfIds.contains(pdf.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (checked) selectedPdfIds.remove(pdf.id) else selectedPdfIds.add(pdf.id)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            if (checked) selectedPdfIds.remove(pdf.id) else selectedPdfIds.add(pdf.id)
                                        },
                                    )
                                    DocumentFormatBadge(type = DocumentType.PDF, size = 34.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
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
                                            fontFamily = JetBrainsMonoFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (idx < pdfDocs.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp, end = 12.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = mergedPdfTitle,
                            onValueChange = { mergedPdfTitle = it },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Merged PDF name") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Button(
                            enabled = selectedPdfIds.size >= 2,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val chosen = selectedPdfIds.mapNotNull { id -> pdfDocs.firstOrNull { it.id == id } }
                                onMergePdfs(chosen, mergedPdfTitle)
                                selectedPdfIds.clear()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .bouncyPress(enabled = selectedPdfIds.size >= 2)
                                .testTag("merge_pdfs_button"),
                            shape = CircleShape,
                        ) {
                            Text("Merge ${selectedPdfIds.size} Selected PDFs")
                        }
                    }
                }
            }
        }

        // 2b. Photos-to-PDF Scanner & PDF Compressor
        item {
            EditorialSectionSurface(
                shape = RoundedCornerShape(26.dp),
                tonalLevel = sectionColor,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Compress,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PDF Scanner & Compressor",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Shrink PDF file sizes for WhatsApp or scan gallery photos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenScanToPdf,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("tools_scan_pdf_button"),
                    ) {
                        Icon(Icons.Rounded.DocumentScanner, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Scan Photos from Gallery to PDF")
                    }

                    if (pdfDocs.isNotEmpty()) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )
                        Text(
                            text = "Select PDF to compress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pdfDocs, key = { it.id }) { pdf ->
                                FilterChip(
                                    selected = pdf.id == compressSourceId,
                                    onClick = { compressSourceId = pdf.id },
                                    shape = CircleShape,
                                    label = {
                                        Text("${pdf.title.take(16)} (${formatByteSize(pdf.sizeBytes)})", maxLines = 1)
                                    },
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(compressPresets.size) { idx ->
                                val (label, _, _) = compressPresets[idx]
                                FilterChip(
                                    selected = selectedCompressPresetIdx == idx,
                                    onClick = { selectedCompressPresetIdx = idx },
                                    shape = CircleShape,
                                    label = { Text(label) },
                                )
                            }
                        }
                        val targetPdf = pdfDocs.firstOrNull { it.id == compressSourceId }
                        val (_, scale, quality) = compressPresets[selectedCompressPresetIdx]
                        Button(
                            enabled = targetPdf != null,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                targetPdf?.let { onCompressPdf(it, scale, quality) }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("compress_pdf_button"),
                        ) {
                            Text("Compress Selected PDF")
                        }
                    }
                }
            }
        }

        // 3. Workspace Storage & Format Inspector
        item {
            EditorialSectionSurface(
                shape = RoundedCornerShape(26.dp),
                tonalLevel = sectionColor,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Widgets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
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
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                                    fontFamily = JetBrainsMonoFamily,
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
