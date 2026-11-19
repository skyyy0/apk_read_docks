package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class LuminaAppearance(
    val isDark: Boolean,
    val liquidGlassEnabled: Boolean,
    val powerSavingMode: Boolean,
    val accent: AccentPalette,
    val canvasTop: Color,
    val canvasBottom: Color,
    val glassFill: Color,
    val glassElevatedFill: Color,
    val glassSpecularTop: Color,
    val glassBorder: Color,
    val capsuleFill: Color,
    val ambientOrbPrimary: Color,
    val ambientOrbSecondary: Color,
)

val LocalLuminaAppearance = staticCompositionLocalOf {
    LuminaAppearance(
        isDark = false,
        liquidGlassEnabled = true,
        powerSavingMode = false,
        accent = AccentPalette.GLACIER,
        canvasTop = AlabasterCanvasTop,
        canvasBottom = AlabasterCanvasBottom,
        glassFill = Color.White.copy(alpha = 0.78f),
        glassElevatedFill = Color.White.copy(alpha = 0.90f),
        glassSpecularTop = Color.White.copy(alpha = 0.95f),
        glassBorder = Color(0xFF94A3B8).copy(alpha = 0.22f),
        capsuleFill = Color.White.copy(alpha = 0.86f),
        ambientOrbPrimary = AccentPalette.GLACIER.lightPrimary.copy(alpha = 0.14f),
        ambientOrbSecondary = AccentPalette.GLACIER.secondaryLight.copy(alpha = 0.10f),
    )
}

@Composable
fun LuminaDocsTheme(
    themeMode: String = "SYSTEM",
    accentPalette: AccentPalette = AccentPalette.GLACIER,
    liquidGlassEnabled: Boolean = true,
    powerSavingMode: Boolean = false,
    oledBlackInPowerSaver: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }

    val effectiveGlass = liquidGlassEnabled && !powerSavingMode
    val useOled = darkTheme && powerSavingMode && oledBlackInPowerSaver

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentPalette.darkPrimary,
            onPrimary = Color(0xFF041322),
            primaryContainer = accentPalette.darkPrimary.copy(alpha = 0.20f),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = accentPalette.secondaryDark,
            onSecondary = Color(0xFF041322),
            secondaryContainer = accentPalette.secondaryDark.copy(alpha = 0.16f),
            onSecondaryContainer = Color(0xFFE0F2FE),
            background = if (useOled) OledPureBlack else ObsidianCanvasTop,
            onBackground = ObsidianOnSurface,
            surface = if (useOled) OledElevatedSurface else ObsidianSurface,
            onSurface = ObsidianOnSurface,
            surfaceVariant = if (useOled) Color(0xFF141923) else ObsidianSurfaceVariant,
            onSurfaceVariant = ObsidianOnSurfaceVariant,
            outline = ObsidianOutline,
            error = Color(0xFFFF6B6B),
            onError = Color(0xFF2B0000),
        )
    } else {
        lightColorScheme(
            primary = accentPalette.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = accentPalette.lightPrimary.copy(alpha = 0.13f),
            onPrimaryContainer = accentPalette.lightPrimary,
            secondary = accentPalette.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = accentPalette.secondaryLight.copy(alpha = 0.12f),
            onSecondaryContainer = accentPalette.secondaryLight,
            background = AlabasterCanvasTop,
            onBackground = AlabasterOnSurface,
            surface = AlabasterSurface,
            onSurface = AlabasterOnSurface,
            surfaceVariant = AlabasterSurfaceVariant,
            onSurfaceVariant = AlabasterOnSurfaceVariant,
            outline = AlabasterOutline,
            error = Color(0xFFDC2626),
            onError = Color.White,
        )
    }

    val appearance = if (darkTheme) {
        LuminaAppearance(
            isDark = true,
            liquidGlassEnabled = effectiveGlass,
            powerSavingMode = powerSavingMode,
            accent = accentPalette,
            canvasTop = if (useOled) OledPureBlack else ObsidianCanvasTop,
            canvasBottom = if (useOled) OledPureBlack else ObsidianCanvasBottom,
            glassFill = if (effectiveGlass) Color(0xFF192235).copy(alpha = 0.72f) else if (useOled) OledElevatedSurface else ObsidianSurface,
            glassElevatedFill = if (effectiveGlass) Color(0xFF202B42).copy(alpha = 0.84f) else if (useOled) Color(0xFF141A26) else ObsidianSurfaceVariant,
            glassSpecularTop = Color.White.copy(alpha = if (effectiveGlass) 0.18f else 0.08f),
            glassBorder = Color.White.copy(alpha = if (effectiveGlass) 0.12f else 0.09f),
            capsuleFill = if (effectiveGlass) Color(0xFF151E30).copy(alpha = 0.88f) else if (useOled) Color(0xFF101520) else ObsidianSurface,
            ambientOrbPrimary = accentPalette.darkPrimary.copy(alpha = 0.14f),
            ambientOrbSecondary = accentPalette.secondaryDark.copy(alpha = 0.09f),
        )
    } else {
        LuminaAppearance(
            isDark = false,
            liquidGlassEnabled = effectiveGlass,
            powerSavingMode = powerSavingMode,
            accent = accentPalette,
            canvasTop = AlabasterCanvasTop,
            canvasBottom = if (powerSavingMode) AlabasterCanvasTop else AlabasterCanvasBottom,
            glassFill = if (effectiveGlass) Color.White.copy(alpha = 0.76f) else Color.White,
            glassElevatedFill = if (effectiveGlass) Color.White.copy(alpha = 0.90f) else Color.White,
            glassSpecularTop = Color.White.copy(alpha = if (effectiveGlass) 0.96f else 0.50f),
            glassBorder = Color(0xFF64748B).copy(alpha = if (effectiveGlass) 0.16f else 0.14f),
            capsuleFill = if (effectiveGlass) Color.White.copy(alpha = 0.88f) else Color.White,
            ambientOrbPrimary = accentPalette.lightPrimary.copy(alpha = 0.12f),
            ambientOrbSecondary = accentPalette.secondaryLight.copy(alpha = 0.09f),
        )
    }

    CompositionLocalProvider(LocalLuminaAppearance provides appearance) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LuminaTypography,
            content = content,
        )
    }
}
