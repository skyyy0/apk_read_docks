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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DocBlock
import com.example.engine.DocBlockStyle
import com.example.ui.theme.FormatWordColor
import com.example.ui.theme.GlassFilterPill
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.viewmodel.ActiveEditorState

@Composable
fun WordEditorScreen(
    state: ActiveEditorState.WordEditor,
    fontSizeSp: Int,
    onClose: () -> Unit,
    onSelectBlock: (String) -> Unit,
    onUpdateBlock: (String, (DocBlock) -> DocBlock) -> Unit,
    onAddBlock: (DocBlockStyle) -> Unit,
    onMoveBlock: (String, Boolean) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onFindReplace: (String, String) -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val selectedBlock = state.richDocument.blocks.firstOrNull { it.id == state.selectedBlockId }
        ?: state.richDocument.blocks.firstOrNull()

    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Top Liquid Glass Bar
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("editor_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.document.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (state.hasUnsavedChanges) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(FormatWordColor),
                            )
                        }
                    }
                    Text(
                        text = "${state.richDocument.wordCount} words  •  ${state.richDocument.estimatedReadMinutes} min read",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showFindReplace = true }) {
                    Icon(Icons.Rounded.FindReplace, contentDescription = "Find and replace")
                }
                IconButton(onClick = onExportPdf) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export Word to PDF")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Rounded.IosShare, contentDescription = "Share DOCX")
                }
                IconButton(onClick = onSave, modifier = Modifier.testTag("word_save_button")) {
                    Icon(Icons.Rounded.Save, contentDescription = "Save DOCX", tint = FormatWordColor)
                }
            }
        }

        // Editorial Document Sheet
        LiquidGlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            elevated = true,
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.richDocument.blocks, key = { it.id }) { block ->
                    val isSelected = block.id == selectedBlock?.id
                    WordBlockEditorRow(
                        block = block,
                        isSelected = isSelected,
                        baseFontSizeSp = fontSizeSp,
                        onFocus = { onSelectBlock(block.id) },
                        onTextChange = { newText ->
                            onUpdateBlock(block.id) { it.copy(text = newText) }
                        },
                        onToggleCheck = {
                            onUpdateBlock(block.id) { it.copy(isChecked = !it.isChecked) }
                        },
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        // Floating Rich Formatting Toolbar
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(DocBlockStyle.entries) { style ->
                        GlassFilterPill(
                            label = style.label,
                            selected = selectedBlock?.style == style,
                            onClick = {
                                selectedBlock?.let { blk ->
                                    onUpdateBlock(blk.id) {
                                        it.copy(
                                            style = style,
                                            isBold = style in listOf(DocBlockStyle.HEADING1, DocBlockStyle.HEADING2, DocBlockStyle.HEADING3) || it.isBold,
                                            isItalic = style == DocBlockStyle.QUOTE || it.isItalic,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                selectedBlock?.let { blk ->
                                    onUpdateBlock(blk.id) { it.copy(isBold = !it.isBold) }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selectedBlock?.isBold == true) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                ),
                        ) {
                            Icon(Icons.Rounded.FormatBold, contentDescription = "Toggle Bold")
                        }
                        IconButton(
                            onClick = {
                                selectedBlock?.let { blk ->
                                    onUpdateBlock(blk.id) { it.copy(isItalic = !it.isItalic) }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selectedBlock?.isItalic == true) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                ),
                        ) {
                            Icon(Icons.Rounded.FormatItalic, contentDescription = "Toggle Italic")
                        }
                        IconButton(
                            enabled = selectedBlock != null,
                            onClick = { selectedBlock?.let { onMoveBlock(it.id, true) } },
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move block up")
                        }
                        IconButton(
                            enabled = selectedBlock != null,
                            onClick = { selectedBlock?.let { onMoveBlock(it.id, false) } },
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move block down")
                        }
                        IconButton(
                            enabled = state.richDocument.blocks.size > 1 && selectedBlock != null,
                            onClick = { selectedBlock?.let { onDeleteBlock(it.id) } },
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete block",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onAddBlock(DocBlockStyle.BODY) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("word_add_block_button"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Insert block",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Add Block",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }

    if (showFindReplace) {
        AlertDialog(
            onDismissRequest = { showFindReplace = false },
            title = { Text("Find & Replace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = findText,
                        onValueChange = { findText = it },
                        singleLine = true,
                        label = { Text("Find text") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        singleLine = true,
                        label = { Text("Replace with") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onFindReplace(findText, replaceText)
                    showFindReplace = false
                }) { Text("Replace All") }
            },
            dismissButton = {
                TextButton(onClick = { showFindReplace = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun WordBlockEditorRow(
    block: DocBlock,
    isSelected: Boolean,
    baseFontSizeSp: Int,
    onFocus: () -> Unit,
    onTextChange: (String) -> Unit,
    onToggleCheck: () -> Unit,
) {
    val textSize = when (block.style) {
        DocBlockStyle.HEADING1 -> (baseFontSizeSp + 9).sp
        DocBlockStyle.HEADING2 -> (baseFontSizeSp + 5).sp
        DocBlockStyle.HEADING3 -> (baseFontSizeSp + 2).sp
        DocBlockStyle.CODE -> (baseFontSizeSp - 2).coerceAtLeast(11).sp
        else -> baseFontSizeSp.sp
    }
    val fontFamily = if (block.style == DocBlockStyle.CODE) JetBrainsMonoFamily else PlusJakartaSansFamily
    val fontWeight = when {
        block.isBold || block.style in listOf(DocBlockStyle.HEADING1, DocBlockStyle.HEADING2, DocBlockStyle.HEADING3) -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    val fontStyle = if (block.isItalic || block.style == DocBlockStyle.QUOTE) FontStyle.Italic else FontStyle.Normal

    val containerMod = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(
            when {
                block.style == DocBlockStyle.CODE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                else -> Color.Transparent
            }
        )
        .then(
            if (isSelected) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            } else Modifier
        )
        .padding(horizontal = 10.dp, vertical = 8.dp)

    Row(
        modifier = containerMod,
        verticalAlignment = Alignment.Top,
    ) {
        when (block.style) {
            DocBlockStyle.BULLET -> {
                Text(
                    text = "•",
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            DocBlockStyle.CHECKLIST -> {
                Icon(
                    imageVector = if (block.isChecked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = "Toggle checklist item",
                    tint = if (block.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 10.dp, top = 2.dp)
                        .size(20.dp)
                        .clickable(onClick = onToggleCheck),
                )
            }
            DocBlockStyle.QUOTE -> {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp, top = 2.dp)
                        .width(3.dp)
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            else -> Unit
        }

        BasicTextField(
            value = block.text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                fontFamily = fontFamily,
                fontSize = textSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                color = if (block.style == DocBlockStyle.QUOTE) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (block.style == DocBlockStyle.CHECKLIST && block.isChecked) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.None
                },
                lineHeight = (textSize.value * 1.45f).sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (block.text.isEmpty()) {
                        Text(
                            text = "Type ${block.style.label.lowercase()}...",
                            fontSize = textSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { if (it.isFocused) onFocus() },
        )
    }
}
