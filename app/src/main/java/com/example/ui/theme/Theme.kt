package com.example.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class LuminaAppearance(
    val isDark: Boolean,
    val liquidGlassEnabled: Boolean,
    val powerSavingMode: Boolean,
    val reduceMotion: Boolean,
    val accent: AccentPalette,
    val glassBarFill: Color,
    val glassSpecularEdge: Color,
    val glassHairlineBorder: Color,
)

val LocalLuminaAppearance = staticCompositionLocalOf {
    LuminaAppearance(
        isDark = false,
        liquidGlassEnabled = true,
        powerSavingMode = false,
        reduceMotion = false,
        accent = AccentPalette.GLACIER,
        glassBarFill = Color.White.copy(alpha = 0.88f),
        glassSpecularEdge = Color.White.copy(alpha = 0.95f),
        glassHairlineBorder = LightOutlineVariant,
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
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }

    val systemReduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }

    val effectiveGlass = liquidGlassEnabled && !powerSavingMode
    val effectiveReduceMotion = powerSavingMode || systemReduceMotion
    val useOled = darkTheme && powerSavingMode && oledBlackInPowerSaver

    val dynamicScheme = if (accentPalette == AccentPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null

    val colorScheme = when {
        dynamicScheme != null && !useOled -> dynamicScheme
        dynamicScheme != null && useOled -> dynamicScheme.copy(
            background = OledPureBlack,
            surface = OledPureBlack,
            surfaceContainerLowest = OledPureBlack,
            surfaceContainerLow = OledSurfaceLow,
            surfaceContainer = OledSurfaceContainer,
            surfaceContainerHigh = OledSurfaceHigh,
            surfaceContainerHighest = DarkSurfaceHigh,
        )
        darkTheme -> darkColorScheme(
            primary = accentPalette.darkPrimary,
            onPrimary = Color(0xFF041322),
            primaryContainer = accentPalette.darkPrimary.copy(alpha = 0.18f),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = accentPalette.secondaryDark,
            onSecondary = Color(0xFF041322),
            secondaryContainer = accentPalette.secondaryDark.copy(alpha = 0.15f),
            onSecondaryContainer = Color(0xFFE0F2FE),
            background = if (useOled) OledPureBlack else DarkSurface,
            onBackground = DarkOnSurface,
            surface = if (useOled) OledPureBlack else DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = if (useOled) OledSurfaceContainer else DarkSurfaceContainer,
            onSurfaceVariant = DarkOnSurfaceVariant,
            surfaceContainerLowest = if (useOled) OledPureBlack else DarkSurfaceLowest,
            surfaceContainerLow = if (useOled) OledSurfaceLow else DarkSurfaceLow,
            surfaceContainer = if (useOled) OledSurfaceContainer else DarkSurfaceContainer,
            surfaceContainerHigh = if (useOled) OledSurfaceHigh else DarkSurfaceHigh,
            surfaceContainerHighest = if (useOled) DarkSurfaceHigh else DarkSurfaceHighest,
            outline = DarkOutline,
            outlineVariant = DarkOutlineVariant,
            error = Color(0xFFF87171),
            onError = Color(0xFF3F0606),
            errorContainer = Color(0xFF5C1515),
            onErrorContainer = Color(0xFFFEE2E2),
        )
        else -> lightColorScheme(
            primary = accentPalette.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = accentPalette.lightPrimary.copy(alpha = 0.12f),
            onPrimaryContainer = accentPalette.lightPrimary,
            secondary = accentPalette.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = accentPalette.secondaryLight.copy(alpha = 0.11f),
            onSecondaryContainer = accentPalette.secondaryLight,
            background = LightSurface,
            onBackground = LightOnSurface,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceContainer,
            onSurfaceVariant = LightOnSurfaceVariant,
            surfaceContainerLowest = LightSurfaceLowest,
            surfaceContainerLow = LightSurfaceLow,
            surfaceContainer = LightSurfaceContainer,
            surfaceContainerHigh = LightSurfaceHigh,
            surfaceContainerHighest = LightSurfaceHighest,
            outline = LightOutline,
            outlineVariant = LightOutlineVariant,
            error = Color(0xFFDC2626),
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF7F1D1D),
        )
    }

    val appearance = LuminaAppearance(
        isDark = darkTheme,
        liquidGlassEnabled = effectiveGlass,
        powerSavingMode = powerSavingMode,
        reduceMotion = effectiveReduceMotion,
        accent = accentPalette,
        glassBarFill = if (effectiveGlass) {
            colorScheme.surfaceContainer.copy(alpha = if (darkTheme) 0.84f else 0.88f)
        } else {
            colorScheme.surfaceContainerLow
        },
        glassSpecularEdge = if (darkTheme) {
            Color.White.copy(alpha = if (effectiveGlass) 0.13f else 0.06f)
        } else {
            Color.White.copy(alpha = if (effectiveGlass) 0.92f else 0.50f)
        },
        glassHairlineBorder = colorScheme.outlineVariant,
    )

    CompositionLocalProvider(LocalLuminaAppearance provides appearance) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LuminaTypography,
            content = content,
        )
    }
}
