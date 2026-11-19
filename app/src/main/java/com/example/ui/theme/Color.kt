package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AccentPalette(
    val key: String,
    val label: String,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
) {
    GLACIER(
        key = "GLACIER",
        label = "Glacier Blue",
        lightPrimary = Color(0xFF007AFF),
        darkPrimary = Color(0xFF389BFF),
        secondaryLight = Color(0xFF0EA5E9),
        secondaryDark = Color(0xFF38BDF8),
    ),
    EMERALD(
        key = "EMERALD",
        label = "Emerald Jade",
        lightPrimary = Color(0xFF059669),
        darkPrimary = Color(0xFF34D399),
        secondaryLight = Color(0xFF0D9488),
        secondaryDark = Color(0xFF2DD4BF),
    ),
    AMETHYST(
        key = "AMETHYST",
        label = "Amethyst Slate",
        lightPrimary = Color(0xFF6366F1),
        darkPrimary = Color(0xFF818CF8),
        secondaryLight = Color(0xFF8B5CF6),
        secondaryDark = Color(0xFFA78BFA),
    ),
    AMBER(
        key = "AMBER",
        label = "Amber Copper",
        lightPrimary = Color(0xFFD97706),
        darkPrimary = Color(0xFFFBBF24),
        secondaryLight = Color(0xFFEA580C),
        secondaryDark = Color(0xFFFB923C),
    );

    companion object {
        fun fromKey(key: String): AccentPalette =
            entries.firstOrNull { it.key == key } ?: GLACIER
    }
}

// Atmospheric Canvas Colors
val AlabasterCanvasTop = Color(0xFFF3F6FB)
val AlabasterCanvasBottom = Color(0xFFE7EDF6)
val AlabasterSurface = Color(0xFFFFFFFF)
val AlabasterSurfaceVariant = Color(0xFFEEF2F8)
val AlabasterOnSurface = Color(0xFF0F172A)
val AlabasterOnSurfaceVariant = Color(0xFF526074)
val AlabasterOutline = Color(0xFFCBD5E1)

val ObsidianCanvasTop = Color(0xFF0B0F19)
val ObsidianCanvasBottom = Color(0xFF111827)
val ObsidianSurface = Color(0xFF151C2C)
val ObsidianSurfaceVariant = Color(0xFF1E273B)
val ObsidianOnSurface = Color(0xFFF1F5F9)
val ObsidianOnSurfaceVariant = Color(0xFF94A3B8)
val ObsidianOutline = Color(0xFF2E3B52)

val OledPureBlack = Color(0xFF000000)
val OledElevatedSurface = Color(0xFF0E121B)

// Document Format Jewel Accents
val FormatPdfColor = Color(0xFFE53935)
val FormatExcelColor = Color(0xFF10B981)
val FormatWordColor = Color(0xFF2563EB)
val FormatSlidesColor = Color(0xFFF59E0B)
val FormatMarkdownColor = Color(0xFF7C3AED)
val FormatCodeColor = Color(0xFF0891B2)
val FormatTextColor = Color(0xFF64748B)
