package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.luminaDataStore by preferencesDataStore(name = "lumina_settings")

data class UserSettings(
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val accentKey: String = "GLACIER", // GLACIER, EMERALD, AMETHYST, AMBER
    val liquidGlassEnabled: Boolean = true,
    val powerSavingMode: Boolean = false,
    val oledBlackInPowerSaver: Boolean = true,
    val editorFontSizeSp: Int = 15,
    val autoSaveEnabled: Boolean = true,
    val pdfNightInvertDefault: Boolean = false,
    val showGridlines: Boolean = true,
    val showLineNumbers: Boolean = true,
    val lastBackupTimestamp: Long = 0L,
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_KEY = stringPreferencesKey("accent_key")
        val LIQUID_GLASS = booleanPreferencesKey("liquid_glass")
        val POWER_SAVING = booleanPreferencesKey("power_saving")
        val OLED_BLACK = booleanPreferencesKey("oled_black")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val PDF_NIGHT_INVERT = booleanPreferencesKey("pdf_night_invert")
        val SHOW_GRIDLINES = booleanPreferencesKey("show_gridlines")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val LAST_BACKUP = longPreferencesKey("last_backup_ts")
    }

    val settingsFlow: Flow<UserSettings> = context.luminaDataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            accentKey = prefs[Keys.ACCENT_KEY] ?: "GLACIER",
            liquidGlassEnabled = prefs[Keys.LIQUID_GLASS] ?: true,
            powerSavingMode = prefs[Keys.POWER_SAVING] ?: false,
            oledBlackInPowerSaver = prefs[Keys.OLED_BLACK] ?: true,
            editorFontSizeSp = prefs[Keys.EDITOR_FONT_SIZE] ?: 15,
            autoSaveEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            pdfNightInvertDefault = prefs[Keys.PDF_NIGHT_INVERT] ?: false,
            showGridlines = prefs[Keys.SHOW_GRIDLINES] ?: true,
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP] ?: 0L,
        )
    }

    suspend fun getSettingsOnce(): UserSettings = settingsFlow.first()

    suspend fun updateThemeMode(mode: String) {
        context.luminaDataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun updateAccentKey(accent: String) {
        context.luminaDataStore.edit { it[Keys.ACCENT_KEY] = accent }
    }

    suspend fun updateLiquidGlass(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.LIQUID_GLASS] = enabled }
    }

    suspend fun updatePowerSavingMode(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.POWER_SAVING] = enabled }
    }

    suspend fun updateOledBlackInPowerSaver(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.OLED_BLACK] = enabled }
    }

    suspend fun updateEditorFontSize(sizeSp: Int) {
        context.luminaDataStore.edit { it[Keys.EDITOR_FONT_SIZE] = sizeSp.coerceIn(11, 24) }
    }

    suspend fun updateAutoSave(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun updatePdfNightInvertDefault(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.PDF_NIGHT_INVERT] = enabled }
    }

    suspend fun updateShowGridlines(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.SHOW_GRIDLINES] = enabled }
    }

    suspend fun updateShowLineNumbers(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.SHOW_LINE_NUMBERS] = enabled }
    }

    suspend fun updateLastBackupTimestamp(ts: Long) {
        context.luminaDataStore.edit { it[Keys.LAST_BACKUP] = ts }
    }

    suspend fun applyRestoredSettings(settings: UserSettings) {
        context.luminaDataStore.edit {
            it[Keys.THEME_MODE] = settings.themeMode
            it[Keys.ACCENT_KEY] = settings.accentKey
            it[Keys.LIQUID_GLASS] = settings.liquidGlassEnabled
            it[Keys.POWER_SAVING] = settings.powerSavingMode
            it[Keys.OLED_BLACK] = settings.oledBlackInPowerSaver
            it[Keys.EDITOR_FONT_SIZE] = settings.editorFontSizeSp
            it[Keys.AUTO_SAVE] = settings.autoSaveEnabled
            it[Keys.PDF_NIGHT_INVERT] = settings.pdfNightInvertDefault
            it[Keys.SHOW_GRIDLINES] = settings.showGridlines
            it[Keys.SHOW_LINE_NUMBERS] = settings.showLineNumbers
        }
    }
}
