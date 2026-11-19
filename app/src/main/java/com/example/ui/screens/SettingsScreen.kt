package com.example.ui.screens

import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.prefs.UserSettings
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.EditorialSectionSurface
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuminaAppearance
import com.example.ui.theme.bouncyPress
import com.example.viewmodel.formatByteSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: UserSettings,
    cacheSizeBytes: Long,
    documentCount: Int,
    onThemeModeChange: (String) -> Unit,
    onAccentChange: (String) -> Unit,
    onLiquidGlassToggle: (Boolean) -> Unit,
    onPowerSavingToggle: (Boolean) -> Unit,
    onOledBlackToggle: (Boolean) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onAutoSaveToggle: (Boolean) -> Unit,
    onPdfNightDefaultToggle: (Boolean) -> Unit,
    onGridlinesToggle: (Boolean) -> Unit,
    onLineNumbersToggle: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onClearCache: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val lastBackupLabel = remember(settings.lastBackupTimestamp) {
        if (settings.lastBackupTimestamp <= 0L) {
            "No backup created yet"
        } else {
            "Last backup: " + SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US).format(Date(settings.lastBackupTimestamp))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("settings_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Theme, Power Saving Mode, Backup & App Information",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 1. Appearance & Material 3 Dynamic Color
        item {
            SettingsGroup(title = "APPEARANCE") {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                val themeOptions = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeOptions.forEachIndexed { index, (key, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                            selected = settings.themeMode == key,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onThemeModeChange(key)
                            },
                            modifier = Modifier.testTag("theme_mode_${key.lowercase()}"),
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                val palettes = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        AccentPalette.entries
                    } else {
                        AccentPalette.entries.filter { it != AccentPalette.DYNAMIC }
                    }
                }
                Text(
                    text = "Accent Palette (${AccentPalette.fromKey(settings.accentKey).label})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    palettes.forEach { palette ->
                        val selected = settings.accentKey == palette.key
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .bouncyPress(pressedScale = 0.9f)
                                .clip(CircleShape)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAccentChange(palette.key)
                                }
                                .testTag("accent_${palette.key.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (palette == AccentPalette.DYNAMIC) {
                                            Modifier.background(
                                                Brush.sweepGradient(
                                                    listOf(
                                                        Color(0xFF0A66C2),
                                                        Color(0xFF059669),
                                                        Color(0xFFD97706),
                                                        Color(0xFF7C3AED),
                                                        Color(0xFF0A66C2),
                                                    )
                                                )
                                            )
                                        } else {
                                            Modifier.background(palette.lightPrimary)
                                        }
                                    )
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.45f),
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "${palette.label} selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else if (palette == AccentPalette.DYNAMIC) {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = "Material You Dynamic Color",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsDivider()

                SettingsToggleRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Translucent Glass Bars",
                    subtitle = if (settings.powerSavingMode) {
                        "Paused automatically while Power Saving Mode is active"
                    } else {
                        "Subtle frosted tint & specular hairline on toolbars and navigation bar"
                    },
                    checked = settings.liquidGlassEnabled,
                    onCheckedChange = onLiquidGlassToggle,
                    testTag = "toggle_liquid_glass",
                )
            }
        }

        // 2. Power Saving & Performance
        item {
            SettingsGroup(title = "POWER & PERFORMANCE") {
                SettingsToggleRow(
                    icon = Icons.Rounded.BatterySaver,
                    iconTint = FormatExcelColor,
                    title = "Power Saving Mode (Mode Hemat Daya)",
                    subtitle = "Lowers PDF render DPI to 1.25x, reduces motion & limits GPU overdraw",
                    checked = settings.powerSavingMode,
                    onCheckedChange = onPowerSavingToggle,
                    testTag = "toggle_power_saving",
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "True OLED Black in Power Saver",
                    subtitle = "Uses pure #000000 canvas in Dark Mode when Power Saving is active",
                    checked = settings.oledBlackInPowerSaver,
                    onCheckedChange = onOledBlackToggle,
                    testTag = "toggle_oled_black",
                )
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Column {
                            Text(
                                text = "Render & Export Cache",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Using ${formatByteSize(cacheSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClearCache()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .bouncyPress()
                            .testTag("clear_cache_button"),
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        // 3. Data Backup & Restore
        item {
            SettingsGroup(title = "BACKUP & RESTORE DATA") {
                Text(
                    text = "Local Archive (.luminabak)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Packs all $documentCount local document(s), PDF annotations, bookmarks & preferences into one portable archive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lastBackupLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onExportBackup()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bouncyPress()
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup Data")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRestoreBackup()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bouncyPress()
                            .testTag("restore_backup_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore")
                    }
                }
            }
        }

        // 4. Editor & Reader Defaults
        item {
            SettingsGroup(title = "EDITOR & READER DEFAULTS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Editor Font Size",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${settings.editorFontSizeSp} sp",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = settings.editorFontSizeSp.toFloat(),
                    onValueChange = { onFontSizeChange(it.roundToInt()) },
                    valueRange = 11f..22f,
                    steps = 10,
                )
                Text(
                    text = "Sample document typography preview",
                    fontSize = settings.editorFontSizeSp.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Rounded.Save,
                    title = "Auto-Save on Exit",
                    subtitle = "Automatically saves & syncs unsaved edits when leaving an editor",
                    checked = settings.autoSaveEnabled,
                    onCheckedChange = onAutoSaveToggle,
                    testTag = "toggle_auto_save",
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "PDF Night-Ink Invert by Default",
                    subtitle = "Open PDFs with eye-friendly dark page inversion",
                    checked = settings.pdfNightInvertDefault,
                    onCheckedChange = onPdfNightDefaultToggle,
                    testTag = "toggle_pdf_night_default",
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Rounded.GridOn,
                    title = "Spreadsheet Gridlines",
                    subtitle = "Display crisp cell borders in Excel & CSV worksheets",
                    checked = settings.showGridlines,
                    onCheckedChange = onGridlinesToggle,
                    testTag = "toggle_gridlines",
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Rounded.Numbers,
                    title = "Line Numbers in Markdown & JSON",
                    subtitle = "Show monospace line index gutter in code & note editor",
                    checked = settings.showLineNumbers,
                    onCheckedChange = onLineNumbersToggle,
                    testTag = "toggle_line_numbers",
                )
            }
        }

        // 5. About & Seamless Update Info
        item {
            SettingsGroup(title = "ABOUT LUMINA DOCS") {
                InfoRow(Icons.Rounded.Info, "Developer", "sky")
                SettingsDivider()
                InfoRow(Icons.Rounded.SystemUpdateAlt, "Version & Build", "v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})")
                SettingsDivider()
                InfoRow(Icons.Rounded.AutoAwesome, "Package ID", BuildConfig.APPLICATION_ID)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Upgrade-Safe Architecture: Consistent Package ID & release signing preserve your local documents and settings across updates without uninstalling.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    val isDark = LocalLuminaAppearance.current.isDark
    val groupColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        EditorialSectionSurface(
            shape = RoundedCornerShape(20.dp),
            tonalLevel = groupColor,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 0.75.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
}
