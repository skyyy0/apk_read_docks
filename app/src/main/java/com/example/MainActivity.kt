package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentType
import com.example.ui.editors.PdfEditorScreen
import com.example.ui.editors.SlidesEditorScreen
import com.example.ui.editors.SpreadsheetEditorScreen
import com.example.ui.editors.TextEditorScreen
import com.example.ui.editors.WordEditorScreen
import com.example.ui.screens.CreateHubScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.LocalLuminaAppearance
import com.example.ui.theme.LuminaBottomNavigationBar
import com.example.ui.theme.LuminaDocsTheme
import com.example.ui.theme.LuminaNavDestination
import com.example.ui.theme.LuminaNavigationRail
import com.example.ui.theme.LuminaSpringOffset
import com.example.ui.theme.LuminaWorkspaceCanvas
import com.example.ui.theme.bouncyPress
import com.example.viewmodel.ActiveEditorState
import com.example.viewmodel.LuminaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LuminaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            LuminaDocsTheme(
                themeMode = settings.themeMode,
                accentPalette = AccentPalette.fromKey(settings.accentKey),
                liquidGlassEnabled = settings.liquidGlassEnabled,
                powerSavingMode = settings.powerSavingMode,
                oledBlackInPowerSaver = settings.oledBlackInPowerSaver,
            ) {
                val isDark = LocalLuminaAppearance.current.isDark
                LaunchedEffect(isDark) {
                    enableEdgeToEdge(
                        statusBarStyle = if (isDark) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                        },
                        navigationBarStyle = if (isDark) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                        },
                    )
                }

                LuminaAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) {
                    viewModel.importFromUri(uri, openImmediately = true)
                }
            }
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: intent.data
                if (uri != null) {
                    viewModel.importFromUri(uri, openImmediately = true)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (!uris.isNullOrEmpty()) {
                    viewModel.importMultipleUris(uris)
                }
            }
        }
    }
}

@Composable
private fun LuminaAppRoot(viewModel: LuminaViewModel) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appearance = LocalLuminaAppearance.current
    val allDocuments by viewModel.allDocuments.collectAsStateWithLifecycle()
    val filteredDocuments by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf("library") }
    var pendingExportDoc by remember { mutableStateOf<DocumentEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importFromUri(uri, openImmediately = true)
    }

    val importMultipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importMultipleUris(uris)
    }

    val exportDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val doc = pendingExportDoc
        if (uri != null && doc != null) {
            viewModel.exportDocumentToUri(doc, uri)
        }
        pendingExportDoc = null
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.restoreBackup(uri)
    }

    val supportedMimeTypes = remember {
        arrayOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/csv",
            "text/plain",
            "text/markdown",
            "application/json",
            "text/html",
            "*/*",
        )
    }

    val launchImport: () -> Unit = { importFileLauncher.launch(supportedMimeTypes) }
    val launchBatchImport: () -> Unit = { importMultipleFilesLauncher.launch(supportedMimeTypes) }

    val shareDocument: (DocumentEntity) -> Unit = { doc ->
        val shareIntent = viewModel.createShareIntent(doc)
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Share ${doc.title}"))
        } else {
            viewModel.emitMessage("File unavailable to share")
        }
    }

    val destinations = remember {
        listOf(
            LuminaNavDestination("library", "Library", Icons.Outlined.Folder, Icons.Rounded.Folder, "nav_library"),
            LuminaNavDestination("create", "Create", Icons.Outlined.AddBox, Icons.Rounded.AddBox, "nav_create"),
            LuminaNavDestination("tools", "Lab", Icons.Outlined.Handyman, Icons.Rounded.Handyman, "nav_tools"),
            LuminaNavDestination("settings", "Settings", Icons.Outlined.Settings, Icons.Rounded.Settings, "nav_settings"),
        )
    }

    val onNavSelect: (String) -> Unit = { route ->
        currentTab = route
        if (route == "settings") viewModel.refreshCacheSize()
    }

    LuminaWorkspaceCanvas {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedWidth = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.navigationBarsPadding(),
                    )
                },
                bottomBar = {
                    if (editorState is ActiveEditorState.None && !isExpandedWidth) {
                        LuminaBottomNavigationBar(
                            destinations = destinations,
                            selectedRoute = currentTab,
                            onSelect = onNavSelect,
                        )
                    }
                },
                floatingActionButton = {
                    if (editorState is ActiveEditorState.None && currentTab == "library") {
                        ExtendedFloatingActionButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentTab = "create"
                            },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("New File") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .bouncyPress()
                                .testTag("library_fab_new"),
                        )
                    }
                },
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val tabOrder = remember { mapOf("library" to 0, "create" to 1, "tools" to 2, "settings" to 3) }
                    AnimatedContent(
                        targetState = editorState,
                        contentKey = { state ->
                            when (state) {
                                is ActiveEditorState.None -> "workspace"
                                is ActiveEditorState.Loading -> "loading_${state.document.id}"
                                is ActiveEditorState.PdfEditor -> "pdf_${state.document.id}"
                                is ActiveEditorState.SpreadsheetEditor -> "sheet_${state.document.id}"
                                is ActiveEditorState.WordEditor -> "word_${state.document.id}"
                                is ActiveEditorState.SlidesEditor -> "slides_${state.document.id}"
                                is ActiveEditorState.TextEditor -> "text_${state.document.id}"
                            }
                        },
                        transitionSpec = {
                            if (appearance.reduceMotion) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else if (initialState is ActiveEditorState.None && targetState !is ActiveEditorState.None) {
                                (slideInVertically(LuminaSpringOffset) { it / 14 } +
                                    scaleIn(initialScale = 0.97f, animationSpec = tween(260)) +
                                    fadeIn(tween(220))) togetherWith
                                    (scaleOut(targetScale = 0.96f, animationSpec = tween(220)) + fadeOut(tween(180)))
                            } else if (initialState !is ActiveEditorState.None && targetState is ActiveEditorState.None) {
                                (scaleIn(initialScale = 0.96f, animationSpec = tween(240)) + fadeIn(tween(200))) togetherWith
                                    (slideOutVertically(tween(240)) { it / 12 } + fadeOut(tween(200)))
                            } else {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(150))
                            }
                        },
                        label = "editorShellTransition",
                    ) { active ->
                        when (active) {
                            is ActiveEditorState.None -> {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (isExpandedWidth) {
                                        LuminaNavigationRail(
                                            destinations = destinations,
                                            selectedRoute = currentTab,
                                            onSelect = onNavSelect,
                                            modifier = Modifier.fillMaxHeight(),
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.TopCenter,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 840.dp)
                                                .fillMaxHeight(),
                                        ) {
                                            AnimatedContent(
                                                targetState = currentTab,
                                                transitionSpec = {
                                                    if (appearance.reduceMotion) {
                                                        EnterTransition.None togetherWith ExitTransition.None
                                                    } else {
                                                        val dir = (tabOrder[targetState] ?: 0) - (tabOrder[initialState] ?: 0)
                                                        if (dir >= 0) {
                                                            (slideInHorizontally(LuminaSpringOffset) { it / 10 } + fadeIn(tween(200))) togetherWith
                                                                (slideOutHorizontally(tween(180)) { -it / 12 } + fadeOut(tween(150)))
                                                        } else {
                                                            (slideInHorizontally(LuminaSpringOffset) { -it / 10 } + fadeIn(tween(200))) togetherWith
                                                                (slideOutHorizontally(tween(180)) { it / 12 } + fadeOut(tween(150)))
                                                        }
                                                    }
                                                },
                                                label = "mainTabTransition",
                                            ) { tab ->
                                            when (tab) {
                                                "library" -> LibraryScreen(
                                                    allDocuments = allDocuments,
                                                    filteredDocuments = filteredDocuments,
                                                    filterState = filterState,
                                                    powerSavingMode = settings.powerSavingMode,
                                                    onSearchChange = viewModel::setSearchQuery,
                                                    onCategorySelect = viewModel::setCategory,
                                                    onSortChange = viewModel::setSortOrder,
                                                    onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                                                    onOpenDocument = viewModel::openDocument,
                                                    onImportDeviceFile = launchImport,
                                                    onJumpToCreate = { currentTab = "create" },
                                                    onRenameDocument = viewModel::renameDocument,
                                                    onDuplicateDocument = viewModel::duplicateDocument,
                                                    onTogglePin = viewModel::togglePinned,
                                                    onToggleFavorite = viewModel::toggleFavorite,
                                                    onConvertToPdf = { doc -> viewModel.convertDocument(doc, DocumentType.PDF) },
                                                    onExportDocument = { doc ->
                                                        pendingExportDoc = doc
                                                        exportDocLauncher.launch(doc.fileName)
                                                    },
                                                    onShareDocument = shareDocument,
                                                    onDeleteDocument = viewModel::deleteDocument,
                                                )
                                                "create" -> CreateHubScreen(
                                                    onCreateBlank = viewModel::createBlankAndOpen,
                                                    onImportSingleFile = launchImport,
                                                    onImportMultipleFiles = launchBatchImport,
                                                )
                                                "tools" -> ToolsHubScreen(
                                                    allDocuments = allDocuments,
                                                    onConvertDocument = { doc, target -> viewModel.convertDocument(doc, target) },
                                                    onMergePdfs = viewModel::mergePdfDocuments,
                                                )
                                                else -> SettingsScreen(
                                                    settings = settings,
                                                    cacheSizeBytes = cacheSizeBytes,
                                                    documentCount = allDocuments.size,
                                                    onThemeModeChange = viewModel::setThemeMode,
                                                    onAccentChange = viewModel::setAccentKey,
                                                    onLiquidGlassToggle = viewModel::setLiquidGlass,
                                                    onPowerSavingToggle = viewModel::setPowerSavingMode,
                                                    onOledBlackToggle = viewModel::setOledBlackInPowerSaver,
                                                    onFontSizeChange = viewModel::setEditorFontSize,
                                                    onAutoSaveToggle = viewModel::setAutoSave,
                                                    onPdfNightDefaultToggle = viewModel::setPdfNightInvertDefault,
                                                    onGridlinesToggle = viewModel::setShowGridlines,
                                                    onLineNumbersToggle = viewModel::setShowLineNumbers,
                                                    onExportBackup = {
                                                        backupExportLauncher.launch("LuminaDocs_Backup_${System.currentTimeMillis()}.luminabak")
                                                    },
                                                    onRestoreBackup = {
                                                        backupRestoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                                    },
                                                    onClearCache = viewModel::clearCache,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        is ActiveEditorState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is ActiveEditorState.PdfEditor -> PdfEditorScreen(
                            state = active,
                            powerSavingMode = settings.powerSavingMode,
                            onClose = viewModel::closeEditor,
                            onPageChange = viewModel::setPdfPage,
                            onToggleNightInvert = viewModel::togglePdfNightInvert,
                            onToggleBookmark = viewModel::togglePdfPageBookmark,
                            onAddAnnotation = viewModel::addPdfAnnotation,
                            onDeleteAnnotation = viewModel::deletePdfAnnotation,
                            onAppendNewPage = viewModel::appendNewPdfPage,
                            onDeleteCurrentPage = viewModel::deleteCurrentPdfPage,
                            onBakeAndSave = viewModel::bakeAndSavePdf,
                            onShare = { shareDocument(active.document) },
                        )

                        is ActiveEditorState.SpreadsheetEditor -> SpreadsheetEditorScreen(
                            state = active,
                            showGridlines = settings.showGridlines,
                            fontSizeSp = settings.editorFontSizeSp,
                            onClose = viewModel::closeEditor,
                            onSelectCell = viewModel::selectSpreadsheetCell,
                            onUpdateCell = viewModel::updateSpreadsheetCell,
                            onSelectSheet = viewModel::selectSpreadsheetSheet,
                            onAddSheet = viewModel::addSpreadsheetSheet,
                            onAddRow = viewModel::addSpreadsheetRow,
                            onAddColumn = viewModel::addSpreadsheetColumn,
                            onDeleteSelectedRow = viewModel::deleteSelectedSpreadsheetRow,
                            onDeleteSelectedColumn = viewModel::deleteSelectedSpreadsheetColumn,
                            onSortColumn = { viewModel.sortSpreadsheetBySelectedColumn(ascending = true) },
                            onSave = viewModel::saveSpreadsheet,
                            onExportPdf = {
                                viewModel.saveSpreadsheet()
                                viewModel.convertDocument(active.document, DocumentType.PDF)
                            },
                            onShare = { shareDocument(active.document) },
                        )

                        is ActiveEditorState.WordEditor -> WordEditorScreen(
                            state = active,
                            fontSizeSp = settings.editorFontSizeSp,
                            onClose = viewModel::closeEditor,
                            onSelectBlock = viewModel::selectWordBlock,
                            onUpdateBlock = viewModel::updateWordBlock,
                            onAddBlock = viewModel::addWordBlock,
                            onMoveBlock = viewModel::moveWordBlock,
                            onDeleteBlock = viewModel::deleteWordBlock,
                            onFindReplace = viewModel::replaceTextInWord,
                            onSave = viewModel::saveWordDocument,
                            onExportPdf = {
                                viewModel.saveWordDocument()
                                viewModel.convertDocument(active.document, DocumentType.PDF)
                            },
                            onShare = { shareDocument(active.document) },
                        )

                        is ActiveEditorState.SlidesEditor -> SlidesEditorScreen(
                            state = active,
                            onClose = viewModel::closeEditor,
                            onSelectSlide = viewModel::selectSlide,
                            onUpdateSlide = viewModel::updateActiveSlide,
                            onAddSlide = viewModel::addSlide,
                            onDeleteSlide = viewModel::deleteActiveSlide,
                            onSave = viewModel::saveSlides,
                            onExportPdf = {
                                viewModel.saveSlides()
                                viewModel.convertDocument(active.document, DocumentType.PDF)
                            },
                            onShare = { shareDocument(active.document) },
                        )

                        is ActiveEditorState.TextEditor -> TextEditorScreen(
                            state = active,
                            fontSizeSp = settings.editorFontSizeSp,
                            showLineNumbers = settings.showLineNumbers,
                            onClose = viewModel::closeEditor,
                            onContentChange = viewModel::updateTextContent,
                            onTogglePreview = viewModel::toggleTextPreviewMode,
                            onSave = viewModel::saveTextDocument,
                            onExportPdf = {
                                viewModel.saveTextDocument()
                                viewModel.convertDocument(active.document, DocumentType.PDF)
                            },
                            onConvertDocx = {
                                viewModel.saveTextDocument()
                                viewModel.convertDocument(active.document, DocumentType.DOCX)
                            },
                            onShare = { shareDocument(active.document) },
                        )
                        }
                    }

                    if (isBusy) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
