package com.example.ui.editors

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.WrapText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.CodeLanguage
import com.example.engine.CodeSyntaxHighlighter
import com.example.engine.JsonValidationResult
import com.example.engine.TextDocumentEngine
import com.example.engine.VsCodePalette
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuminaAppearance
import com.example.viewmodel.ActiveEditorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeIdeEditorScreen(
    state: ActiveEditorState.TextEditor,
    fontSizeSp: Int,
    showLineNumbers: Boolean,
    onClose: () -> Unit,
    onContentChange: (String) -> Unit,
    onReplaceText: (find: String, replace: String, replaceAll: Boolean) -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val appDark = LocalLuminaAppearance.current.isDark
    var ideDarkTheme by remember(state.document.id) { mutableStateOf(true) }
    val palette = remember(ideDarkTheme) { VsCodePalette(ideDarkTheme) }
    val language = remember(state.document.fileName, state.document.docType) {
        CodeLanguage.detect(state.document.fileName, state.document.docType)
    }
    val visualTransformation = remember(language, palette) {
        CodeSyntaxHighlighter.createVisualTransformation(language, palette)
    }

    var textFieldValue by remember(state.document.id) {
        mutableStateOf(TextFieldValue(text = state.content, selection = TextRange(0)))
    }
    LaunchedEffect(state.content) {
        if (textFieldValue.text != state.content) {
            val safePos = textFieldValue.selection.start.coerceIn(0, state.content.length)
            textFieldValue = TextFieldValue(text = state.content, selection = TextRange(safePos))
        }
    }

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var wordWrap by remember { mutableStateOf(true) }
    var showOutlineSheet by remember { mutableStateOf(false) }
    var showHtmlLivePreview by remember { mutableStateOf(false) }
    val consoleLogs = remember { mutableStateListOf<String>() }

    val cursorPos = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
    val prefixToCursor = remember(textFieldValue.text, cursorPos) {
        textFieldValue.text.substring(0, cursorPos)
    }
    val currentLine = remember(prefixToCursor) { prefixToCursor.count { it == '\n' } + 1 }
    val currentCol = remember(prefixToCursor) {
        val lastNl = prefixToCursor.lastIndexOf('\n')
        if (lastNl < 0) prefixToCursor.length + 1 else prefixToCursor.length - lastNl
    }
    val totalLines = remember(textFieldValue.text) { textFieldValue.text.lines().size.coerceAtLeast(1) }
    val findMatches = remember(textFieldValue.text, findQuery) {
        if (findQuery.isEmpty()) 0 else {
            val parts = textFieldValue.text.split(findQuery, ignoreCase = true)
            (parts.size - 1).coerceAtLeast(0)
        }
    }
    val jsonStatus = remember(textFieldValue.text, language) {
        if (language == CodeLanguage.JSON) TextDocumentEngine.validateAndFormatJson(textFieldValue.text) else null
    }

    val accessoryTokens = remember {
        listOf(
            "TAB" to "  ",
            "<" to "<",
            ">" to ">",
            "/" to "/",
            "{" to "{",
            "}" to "}",
            "(" to "(",
            ")" to ")",
            "[" to "[",
            "]" to "]",
            ";" to ";",
            ":" to ":",
            "=" to "=",
            "\"" to "\"",
            "'" to "'",
            "=>" to " => ",
            "!" to "!",
            "&" to "&",
            "|" to "|",
        )
    }

    fun insertToken(token: String) {
        val cur = textFieldValue
        val start = cur.selection.start.coerceIn(0, cur.text.length)
        val end = cur.selection.end.coerceIn(start, cur.text.length)
        val nextText = cur.text.replaceRange(start, end, token)
        val nextCursor = start + token.length
        textFieldValue = TextFieldValue(nextText, TextRange(nextCursor))
        onContentChange(nextText)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.panelSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("vscode_ide_root"),
    ) {
        // 1. VS Code Top Tab & Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.tabBar)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (showHtmlLivePreview) showHtmlLivePreview = false else onClose()
                },
                modifier = Modifier.testTag("editor_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.defaultText,
                )
            }

            // Active Editor Tab Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.background,
                border = BorderStroke(1.dp, palette.accentBlue.copy(alpha = 0.65f)),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = palette.accentBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = state.document.fileName,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.defaultText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (state.hasUnsavedChanges) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(palette.accentBlue),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.accentBlue.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = language.badge,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentBlue,
                        )
                    }
                }
            }

            if (language == CodeLanguage.HTML) {
                IconButton(
                    onClick = {
                        consoleLogs.clear()
                        showHtmlLivePreview = !showHtmlLivePreview
                    },
                    modifier = Modifier.testTag("vscode_run_html"),
                ) {
                    Icon(
                        imageVector = if (showHtmlLivePreview) Icons.Rounded.Code else Icons.Rounded.PlayArrow,
                        contentDescription = if (showHtmlLivePreview) "Back to Code" else "Run HTML Preview",
                        tint = Color(0xFF4ADE80),
                    )
                }
            }
            IconButton(
                onClick = { showFindBar = !showFindBar },
                modifier = Modifier.testTag("vscode_find_toggle"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FindReplace,
                    contentDescription = "Find and Replace",
                    tint = if (showFindBar) palette.accentBlue else palette.defaultText,
                )
            }
            IconButton(
                onClick = { showOutlineSheet = true },
                modifier = Modifier.testTag("vscode_outline_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountTree,
                    contentDescription = "Code Outline",
                    tint = palette.defaultText,
                )
            }
            IconButton(
                onClick = onSave,
                modifier = Modifier.testTag("text_save_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = "Save code",
                    tint = palette.accentBlue,
                )
            }
        }

        // 2. VS Code Breadcrumb & Quick IDE Utilities Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "workspace › ${state.document.fileName}  •  Ln $currentLine, Col $currentCol",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = palette.gutterText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val formatted = CodeSyntaxHighlighter.formatCode(textFieldValue.text, language)
                        textFieldValue = TextFieldValue(formatted, TextRange(0))
                        onContentChange(formatted)
                    },
                    modifier = Modifier.size(34.dp).testTag("vscode_format_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatAlignLeft,
                        contentDescription = "Beautify Code",
                        tint = palette.gutterText,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    onClick = { wordWrap = !wordWrap },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WrapText,
                        contentDescription = "Toggle Word Wrap",
                        tint = if (wordWrap) palette.accentBlue else palette.gutterText,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    onClick = { ideDarkTheme = !ideDarkTheme },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = if (ideDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Toggle IDE Dark/Light Theme",
                        tint = palette.gutterText,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    onClick = onExportPdf,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Export Code to PDF",
                        tint = palette.gutterText,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.IosShare,
                        contentDescription = "Share Code",
                        tint = palette.gutterText,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // JSON Validator Diagnostics Bar (when JSON)
        if (jsonStatus != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (jsonStatus is JsonValidationResult.Valid) {
                            FormatExcelColor.copy(alpha = 0.14f)
                        } else {
                            Color(0xFFEF4444).copy(alpha = 0.18f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (jsonStatus is JsonValidationResult.Valid) "✓ Valid JSON" else "✗ ${(jsonStatus as JsonValidationResult.Invalid).message}",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = if (jsonStatus is JsonValidationResult.Valid) FormatExcelColor else Color(0xFFF87171),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (jsonStatus is JsonValidationResult.Valid) {
                    Text(
                        text = "Minify",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accentBlue,
                        modifier = Modifier
                            .clickable {
                                textFieldValue = TextFieldValue(jsonStatus.minified, TextRange(0))
                                onContentChange(jsonStatus.minified)
                            }
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }

        // 3. Collapsible VS Code Find & Replace Bar
        AnimatedVisibility(visible = showFindBar && !showHtmlLivePreview) {
            Surface(
                color = palette.panelSurface,
                border = BorderStroke(0.6.dp, palette.gutterText.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = { findQuery = it },
                            singleLine = true,
                            placeholder = { Text("Find...", fontSize = 12.sp) },
                            textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 12.sp, color = palette.defaultText),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "$findMatches found",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = palette.gutterText,
                        )
                        IconButton(onClick = { showFindBar = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close find", tint = palette.gutterText)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            singleLine = true,
                            placeholder = { Text("Replace with...", fontSize = 12.sp) },
                            textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 12.sp, color = palette.defaultText),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onReplaceText(findQuery, replaceQuery, false) }) {
                            Text("Replace", color = palette.accentBlue, fontSize = 12.sp)
                        }
                        TextButton(onClick = { onReplaceText(findQuery, replaceQuery, true) }) {
                            Text("All", color = palette.accentBlue, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 4. Main Workspace: Live HTML WebView Sandbox OR Syntax-Highlighted Code Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(palette.background),
        ) {
            if (showHtmlLivePreview && language == CodeLanguage.HTML) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SandboxHtmlWebView(
                        html = textFieldValue.text,
                        onConsoleLog = { msg -> consoleLogs.add(0, msg) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    if (consoleLogs.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(palette.panelSurface)
                                .border(0.5.dp, palette.gutterText.copy(alpha = 0.3f))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = "CONSOLE OUTPUT",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentBlue,
                            )
                            consoleLogs.take(15).forEach { log ->
                                Text(
                                    text = log,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp,
                                    color = palette.defaultText,
                                )
                            }
                        }
                    }
                }
            } else {
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()
                val editorFontSize = (fontSizeSp - 1).coerceIn(11, 22)
                val lineHeightSp = (editorFontSize * 1.5f).sp

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                        .padding(vertical = 8.dp),
                ) {
                    if (showLineNumbers) {
                        Column(
                            modifier = Modifier
                                .background(palette.background)
                                .padding(start = 10.dp, end = 10.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            for (lineIdx in 1..totalLines) {
                                val isCurLine = lineIdx == currentLine
                                Text(
                                    text = "$lineIdx",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = (editorFontSize - 1).coerceAtLeast(10).sp,
                                    lineHeight = lineHeightSp,
                                    fontWeight = if (isCurLine) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurLine) palette.defaultText else palette.gutterText,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height((totalLines * 24).dp.coerceAtLeast(320.dp))
                                .background(palette.gutterText.copy(alpha = 0.20f)),
                        )
                    }

                    val codeModifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .let { if (!wordWrap) it.horizontalScroll(horizontalScroll) else it }
                        .testTag("text_editor_body")

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { updated ->
                            textFieldValue = updated
                            onContentChange(updated.text)
                        },
                        visualTransformation = visualTransformation,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = editorFontSize.sp,
                            lineHeight = lineHeightSp,
                            color = palette.defaultText,
                        ),
                        cursorBrush = SolidColor(palette.accentBlue),
                        modifier = codeModifier,
                    )
                }
            }
        }

        // 5. VS Code Mobile Accessory Key Ribbon & Status Footer
        if (!showHtmlLivePreview) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.panelSurface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(accessoryTokens) { (label, token) ->
                    Surface(
                        onClick = { insertToken(token) },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.tabBar,
                        border = BorderStroke(0.5.dp, palette.gutterText.copy(alpha = 0.30f)),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.defaultText,
                            )
                        }
                    }
                }
            }
        }

        // VS Code Bottom Status Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.accentBlue)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Lumina Code IDE • ${language.displayLabel}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = "$totalLines lines • UTF-8 • ${if (appDark || ideDarkTheme) "Dark+" else "Light+"}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.92f),
            )
        }
    }

    // Code Outline Sheet
    if (showOutlineSheet) {
        val symbols = remember(textFieldValue.text, language) {
            CodeSyntaxHighlighter.extractSymbols(textFieldValue.text, language)
        }
        ModalBottomSheet(
            onDismissRequest = { showOutlineSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Code Outline (${symbols.size} symbols)",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (symbols.isEmpty()) {
                    Text(
                        text = "No tags, selectors, or functions detected yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(symbols) { sym ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val lines = textFieldValue.text.lines()
                                        var offset = 0
                                        for (i in 0 until (sym.line - 1).coerceAtMost(lines.size)) {
                                            offset += lines[i].length + 1
                                        }
                                        val safeOffset = offset.coerceIn(0, textFieldValue.text.length)
                                        textFieldValue = TextFieldValue(textFieldValue.text, TextRange(safeOffset))
                                        showOutlineSheet = false
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = sym.name,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${sym.kind} • Ln ${sym.line}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SandboxHtmlWebView(
    html: String,
    onConsoleLog: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            onConsoleLog("[${it.messageLevel()}] ${it.message()} (Ln ${it.lineNumber()})")
                        }
                        return true
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
        modifier = modifier,
    )
}
