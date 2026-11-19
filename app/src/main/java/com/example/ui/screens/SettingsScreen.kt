package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.prefs.UserSettings
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.FormatExcelColor
import com.example.ui.theme.GlassFilterPill
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassCard
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
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Appearance, Power Saving Mode, Backup & App Information",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 1. Appearance & Liquid Glass
        item {
            SettingsGroup(title = "APPEARANCE & LIQUID GLASS") {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (key, label) ->
                        GlassFilterPill(
                            label = label,
                            selected = settings.themeMode == key,
                            onClick = { onThemeModeChange(key) },
                            modifier = Modifier.testTag("theme_mode_${key.lowercase()}"),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Accent Palette",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentPalette.entries.forEach { palette ->
                        val selected = settings.accentKey == palette.key
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.lightPrimary)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape,
                                )
                                .clickable { onAccentChange(palette.key) }
                                .testTag("accent_${palette.key.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = palette.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                SettingsDivider()

                SettingsToggleRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Liquid Glass Surfaces",
                    subtitle = if (settings.powerSavingMode) {
                        "Paused automatically while Power Saving Mode is active"
                    } else {
                        "Frosted translucent cards with specular refraction borders"
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
                    subtitle = "Lowers PDF render DPI to 1.25x, calms animations & reduces GPU overdraw",
                    checked = settings.powerSavingMode,
                    onCheckedChange = onPowerSavingToggle,
                    testTag = "toggle_power_saving",
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "True OLED Black in Power Saver",
                    subtitle = "Uses pure #000000 canvas in Dark Mode when Power Saving is on",
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
                                text = "Currently using ${formatByteSize(cacheSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onClearCache,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("clear_cache_button"),
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
                    text = "Packs all $documentCount local document(s), PDF annotations, bookmarks & preferences into one portable backup file.",
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
                        onClick = onExportBackup,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup Data")
                    }
                    OutlinedButton(
                        onClick = onRestoreBackup,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("restore_backup_button"),
                        shape = RoundedCornerShape(16.dp),
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
                    subtitle = "Automatically saves & syncs unsaved edits when leaving any editor",
                    checked = settings.autoSaveEnabled,
                    onCheckedChange = onAutoSaveToggle,
                    testTag = "toggle_auto_save",
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "PDF Night-Ink Invert by Default",
                    subtitle = "Automatically open PDFs with eye-friendly dark page inversion",
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
                    text = "Upgrade-Safe Architecture: Consistent Package ID & signing preserve your local documents and settings seamlessly across updates without uninstalling.",
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
    )
}
