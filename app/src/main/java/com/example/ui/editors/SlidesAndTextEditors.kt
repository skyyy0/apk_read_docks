package com.example.ui.editors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentType
import com.example.engine.DocBlockStyle
import com.example.engine.JsonValidationResult
import com.example.engine.TextDocumentEngine
import com.example.engine.WordDocumentEngine
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.FormatMarkdownColor
import com.example.ui.theme.FormatSlidesColor
import com.example.ui.theme.GlassFilterPill
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassCard
import com.example.viewmodel.ActiveEditorState

@Composable
fun SlidesEditorScreen(
    state: ActiveEditorState.SlidesEditor,
    onClose: () -> Unit,
    onSelectSlide: (Int) -> Unit,
    onUpdateSlide: (title: String, bulletsText: String, notes: String) -> Unit,
    onAddSlide: () -> Unit,
    onDeleteSlide: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val slide = state.deck.slides.getOrNull(state.activeSlideIndex) ?: state.deck.slides.first()
    val bulletsJoined = remember(slide.id, slide.bullets) { slide.bullets.joinToString("\n") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Top Bar
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("editor_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.document.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Slide ${state.activeSlideIndex + 1} / ${state.deck.slides.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onExportPdf) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export Slides to PDF")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Rounded.IosShare, contentDescription = "Share PPTX")
                }
                IconButton(onClick = onSave, modifier = Modifier.testTag("slides_save_button")) {
                    Icon(Icons.Rounded.Save, contentDescription = "Save PPTX", tint = FormatSlidesColor)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Widescreen 16:9 Live Slide Preview Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F294A))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                    .padding(20.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(FormatSlidesColor),
                        )
                        Text(
                            text = slide.title.ifBlank { "Slide Title" },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        slide.bullets.take(4).forEach { b ->
                            Text(
                                text = "•  $b",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = "${state.document.title}  •  ${state.activeSlideIndex + 1}/${state.deck.slides.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                    )
                }
            }

            // Slide Content Editor Form
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = slide.title,
                        onValueChange = { onUpdateSlide(it, bulletsJoined, slide.speakerNotes) },
                        label = { Text("Slide Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = bulletsJoined,
                        onValueChange = { onUpdateSlide(slide.title, it, slide.speakerNotes) },
                        label = { Text("Bullet Points (one per line)") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = slide.speakerNotes,
                        onValueChange = { onUpdateSlide(slide.title, bulletsJoined, it) },
                        label = { Text("Speaker Notes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Slide Deck Strip
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(state.deck.slides) { idx, _ ->
                        GlassFilterPill(
                            label = "Slide ${idx + 1}",
                            selected = idx == state.activeSlideIndex,
                            onClick = { onSelectSlide(idx) },
                        )
                    }
                }
                IconButton(
                    enabled = state.deck.slides.size > 1,
                    onClick = onDeleteSlide,
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete slide", tint = MaterialTheme.colorScheme.error)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onAddSlide)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add slide", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TextEditorScreen(
    state: ActiveEditorState.TextEditor,
    fontSizeSp: Int,
    showLineNumbers: Boolean,
    onClose: () -> Unit,
    onContentChange: (String) -> Unit,
    onTogglePreview: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onConvertDocx: () -> Unit,
    onShare: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val isJson = state.document.docType == DocumentType.JSON
    val isMarkdown = state.document.docType == DocumentType.MARKDOWN
    val wordCount = remember(state.content) { TextDocumentEngine.countWords(state.content) }
    val jsonStatus = remember(state.content, isJson) {
        if (isJson) TextDocumentEngine.validateAndFormatJson(state.content) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Top Bar
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("editor_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.document.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$wordCount words  •  ${state.content.lines().size} lines",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isMarkdown) {
                    IconButton(onClick = onTogglePreview, modifier = Modifier.testTag("toggle_md_preview")) {
                        Icon(
                            imageVector = if (state.previewMode) Icons.Rounded.Edit else Icons.Rounded.Visibility,
                            contentDescription = if (state.previewMode) "Edit Markdown" else "Live Preview",
                            tint = FormatMarkdownColor,
                        )
                    }
                }
                if (isMarkdown || state.document.docType == DocumentType.TEXT) {
                    IconButton(onClick = onConvertDocx) {
                        Icon(Icons.Rounded.Description, contentDescription = "Convert to Word DOCX")
                    }
                }
                IconButton(onClick = onExportPdf) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export to PDF")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Rounded.IosShare, contentDescription = "Share")
                }
                IconButton(onClick = onSave, modifier = Modifier.testTag("text_save_button")) {
                    Icon(Icons.Rounded.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // JSON Validator & Pretty-Print Strip
        if (isJson && jsonStatus != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (jsonStatus is JsonValidationResult.Valid) FormatExcelColor.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = if (jsonStatus is JsonValidationResult.Valid) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = if (jsonStatus is JsonValidationResult.Valid) FormatExcelColor else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (jsonStatus is JsonValidationResult.Valid) "Valid JSON" else (jsonStatus as JsonValidationResult.Invalid).message,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (jsonStatus is JsonValidationResult.Valid) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Format",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onContentChange(jsonStatus.formatted) },
                        )
                        Text(
                            text = "Minify",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onContentChange(jsonStatus.minified) },
                        )
                    }
                }
            }
        }

        // Editor or Live Markdown Preview Area
        LiquidGlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            elevated = true,
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            if (isMarkdown && state.previewMode) {
                val parsed = remember(state.content) { WordDocumentEngine.fromMarkdown(state.content) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(parsed.blocks, key = { it.id }) { blk ->
                        when (blk.style) {
                            DocBlockStyle.HEADING1 -> Text(blk.text, style = MaterialTheme.typography.headlineSmall)
                            DocBlockStyle.HEADING2 -> Text(blk.text, style = MaterialTheme.typography.titleLarge)
                            DocBlockStyle.HEADING3 -> Text(blk.text, style = MaterialTheme.typography.titleMedium)
                            DocBlockStyle.BULLET -> Text("•  ${blk.text}", fontSize = fontSizeSp.sp)
                            DocBlockStyle.CHECKLIST -> Text(
                                text = (if (blk.isChecked) "[✓]  " else "[ ]  ") + blk.text,
                                fontSize = fontSizeSp.sp,
                                color = if (blk.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            DocBlockStyle.QUOTE -> Row {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(22.dp)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(blk.text, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DocBlockStyle.CODE -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp),
                            ) {
                                Text(blk.text, fontFamily = JetBrainsMonoFamily, fontSize = 13.sp)
                            }
                            DocBlockStyle.BODY -> Text(blk.text, fontSize = fontSizeSp.sp, lineHeight = (fontSizeSp * 1.5f).sp)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (showLineNumbers) {
                        val lineCount = state.content.lines().size.coerceAtLeast(1)
                        Column(
                            modifier = Modifier.padding(end = 12.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = (fontSizeSp - 2).coerceAtLeast(11).sp,
                                    lineHeight = (fontSizeSp * 1.48f).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }
                    BasicTextField(
                        value = state.content,
                        onValueChange = onContentChange,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = (fontSizeSp - 1).coerceAtLeast(11).sp,
                            lineHeight = (fontSizeSp * 1.48f).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_editor_body"),
                    )
                }
            }
        }

        // Quick Markdown Formatting Bar
        if (isMarkdown && !state.previewMode) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val snippets = listOf(
                    "H1" to "\n# ",
                    "H2" to "\n## ",
                    "Bold" to "**bold**",
                    "Bullet" to "\n- ",
                    "Task" to "\n- [ ] ",
                    "Quote" to "\n> ",
                    "Code" to "\n```\n\n```\n",
                )
                items(snippets) { (label, snippet) ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                            .clickable { onContentChange(state.content + snippet) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, fontFamily = JetBrainsMonoFamily)
                    }
                }
            }
        }
    }
}
