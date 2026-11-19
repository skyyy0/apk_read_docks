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
    DYNAMIC(
        key = "DYNAMIC",
        label = "Dynamic",
        lightPrimary = Color(0xFF0A66C2),
        darkPrimary = Color(0xFF60A5FA),
        secondaryLight = Color(0xFF0E7490),
        secondaryDark = Color(0xFF38BDF8),
    ),
    GLACIER(
        key = "GLACIER",
        label = "Glacier Blue",
        lightPrimary = Color(0xFF0066CC),
        darkPrimary = Color(0xFF58A6FF),
        secondaryLight = Color(0xFF0284C7),
        secondaryDark = Color(0xFF38BDF8),
    ),
    EMERALD(
        key = "EMERALD",
        label = "Emerald Jade",
        lightPrimary = Color(0xFF047857),
        darkPrimary = Color(0xFF34D399),
        secondaryLight = Color(0xFF0F766E),
        secondaryDark = Color(0xFF2DD4BF),
    ),
    AMETHYST(
        key = "AMETHYST",
        label = "Indigo Slate",
        lightPrimary = Color(0xFF4F46E5),
        darkPrimary = Color(0xFF818CF8),
        secondaryLight = Color(0xFF6366F1),
        secondaryDark = Color(0xFFA5B4FC),
    ),
    AMBER(
        key = "AMBER",
        label = "Amber Copper",
        lightPrimary = Color(0xFFB45309),
        darkPrimary = Color(0xFFFBBF24),
        secondaryLight = Color(0xFFC2410C),
        secondaryDark = Color(0xFFFB923C),
    );

    companion object {
        fun fromKey(key: String): AccentPalette =
            entries.firstOrNull { it.key == key } ?: GLACIER
    }
}

// Editorial Paper Light Surface Ladder (M3 Semantic Roles)
val LightSurface = Color(0xFFF6F8FC)
val LightSurfaceLowest = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFF0F4FA)
val LightSurfaceContainer = Color(0xFFE9EFF8)
val LightSurfaceHigh = Color(0xFFE1E8F3)
val LightSurfaceHighest = Color(0xFFD8E1EF)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFF94A3B8)
val LightOutlineVariant = Color(0xFFD6DFEB)

// Midnight Slate Dark Surface Ladder (M3 Semantic Roles)
val DarkSurface = Color(0xFF0B0F17)
val DarkSurfaceLowest = Color(0xFF070A10)
val DarkSurfaceLow = Color(0xFF111824)
val DarkSurfaceContainer = Color(0xFF172030)
val DarkSurfaceHigh = Color(0xFF1F2A3E)
val DarkSurfaceHighest = Color(0xFF28354D)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF475569)
val DarkOutlineVariant = Color(0xFF232F44)

// True OLED Power Saver Surface Ladder
val OledPureBlack = Color(0xFF000000)
val OledSurfaceLow = Color(0xFF090D14)
val OledSurfaceContainer = Color(0xFF101622)
val OledSurfaceHigh = Color(0xFF171F2E)

// Document Format Jewel Accents
val FormatPdfColor = Color(0xFFDC2626)
val FormatExcelColor = Color(0xFF059669)
val FormatWordColor = Color(0xFF2563EB)
val FormatSlidesColor = Color(0xFFD97706)
val FormatMarkdownColor = Color(0xFF7C3AED)
val FormatCodeColor = Color(0xFF0891B2)
val FormatTextColor = Color(0xFF475569)
