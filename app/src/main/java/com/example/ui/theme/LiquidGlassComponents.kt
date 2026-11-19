package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentType

/**
 * Shared iOS-inspired spring physics tuned for fluid, zero-jank UI responses.
 */
val LuminaSpringSnappy = spring<Float>(
    dampingRatio = 0.80f,
    stiffness = Spring.StiffnessMedium,
)

val LuminaSpringOffset = spring<IntOffset>(
    dampingRatio = 0.86f,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * Subtle tactile GPU press-scale micro-interaction (no layout invalidation; respects Reduce Motion).
 */
fun Modifier.bouncyPress(
    enabled: Boolean = true,
    pressedScale: Float = 0.972f,
): Modifier = composed {
    val reduceMotion = LocalLuminaAppearance.current.reduceMotion
    if (!enabled || reduceMotion) return@composed this

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = LuminaSpringSnappy,
        label = "luminaPressScale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * Clean M3 surface canvas with cached radial glow when Liquid Glass is active.
 */
@Composable
fun LuminaWorkspaceCanvas(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val appearance = LocalLuminaAppearance.current
    val base = MaterialTheme.colorScheme.background
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = if (appearance.isDark) 0.08f else 0.055f)
    val showGlow = appearance.liquidGlassEnabled && !appearance.powerSavingMode

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawWithCache {
                val center = Offset(size.width * 0.9f, 0f)
                val radius = size.width * 0.85f
                val brush = if (showGlow) {
                    Brush.radialGradient(
                        colors = listOf(primaryGlow, Color.Transparent),
                        center = center,
                        radius = radius.coerceAtLeast(1f),
                    )
                } else null
                onDrawBehind {
                    if (brush != null) {
                        drawCircle(
                            brush = brush,
                            radius = radius,
                            center = center,
                        )
                    }
                }
            },
        content = content,
    )
}

/**
 * Restrained Liquid Glass surface strictly for floating toolbars & sticky bars.
 * Uses M3 surfaceContainer tonal role with a crisp 0.75dp hairline specular border.
 */
@Composable
fun GlassToolbarSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val appearance = LocalLuminaAppearance.current
    val borderBrush = remember(appearance.glassSpecularEdge, appearance.glassHairlineBorder) {
        Brush.verticalGradient(
            listOf(appearance.glassSpecularEdge, appearance.glassHairlineBorder.copy(alpha = 0.65f))
        )
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = appearance.glassBarFill,
        border = BorderStroke(0.75.dp, borderBrush),
    ) {
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/**
 * Clean M3 grouped container (surfaceContainerLowest/Low) with subtle hairline outlineVariant border.
 * Replaces repetitive floating shadow-cards for list groups and settings sections.
 */
@Composable
fun EditorialSectionSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tonalLevel: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = tonalLevel,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        content()
    }
}

/**
 * Custom vector document-format badge with folded corner & crisp monospaced extension label.
 */
@Composable
fun DocumentFormatBadge(
    type: DocumentType,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val accent = formatAccentColor(type)
    val isDark = LocalLuminaAppearance.current.isDark
    val bgTint = accent.copy(alpha = if (isDark) 0.18f else 0.11f)
    val foldTint = accent.copy(alpha = if (isDark) 0.42f else 0.24f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val fold = w * 0.28f
            val r = w * 0.18f

            val sheetPath = Path().apply {
                moveTo(r, 0f)
                lineTo(w - fold, 0f)
                lineTo(w, fold)
                lineTo(w, h - r)
                quadraticTo(w, h, w - r, h)
                lineTo(r, h)
                quadraticTo(0f, h, 0f, h - r)
                lineTo(0f, r)
                quadraticTo(0f, 0f, r, 0f)
                close()
            }
            drawPath(path = sheetPath, color = bgTint)

            val foldPath = Path().apply {
                moveTo(w - fold, 0f)
                lineTo(w, fold)
                lineTo(w - fold + r * 0.25f, fold)
                quadraticTo(w - fold, fold, w - fold, fold - r * 0.25f)
                close()
            }
            drawPath(path = foldPath, color = foldTint)

            drawRoundRect(
                color = accent,
                topLeft = Offset(w * 0.16f, h * 0.78f),
                size = Size(w * 0.68f, h * 0.09f),
                cornerRadius = CornerRadius(h * 0.05f, h * 0.05f),
            )
        }
        Text(
            text = type.shortBadge,
            color = accent,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (size >= 44.dp) 10.sp else 9.sp,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            modifier = Modifier.offset(y = (-2).dp),
        )
    }
}

fun formatAccentColor(type: DocumentType): Color = when (type) {
    DocumentType.PDF -> FormatPdfColor
    DocumentType.XLSX, DocumentType.CSV -> FormatExcelColor
    DocumentType.DOCX -> FormatWordColor
    DocumentType.PPTX -> FormatSlidesColor
    DocumentType.MARKDOWN -> FormatMarkdownColor
    DocumentType.JSON, DocumentType.HTML -> FormatCodeColor
    DocumentType.TEXT -> FormatTextColor
}

data class LuminaNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val testTag: String,
)

/**
 * Native Material 3 Bottom NavigationBar with crisp translucent glass surface and top hairline.
 */
@Composable
fun LuminaBottomNavigationBar(
    destinations: List<LuminaNavDestination>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = LocalLuminaAppearance.current
    val haptics = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 0.75.dp,
            color = appearance.glassHairlineBorder.copy(alpha = 0.65f),
        )
        NavigationBar(
            containerColor = appearance.glassBarFill,
            tonalElevation = 0.dp,
        ) {
            destinations.forEach { dest ->
                val selected = dest.route == selectedRoute
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(dest.route)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.icon,
                            contentDescription = dest.label,
                        )
                    },
                    label = {
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag(dest.testTag),
                )
            }
        }
    }
}

/**
 * Native Material 3 NavigationRail for Tablet / Foldable / Landscape widths (>= 600dp).
 */
@Composable
fun LuminaNavigationRail(
    destinations: List<LuminaNavDestination>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = LocalLuminaAppearance.current
    val haptics = LocalHapticFeedback.current

    NavigationRail(
        modifier = modifier,
        containerColor = appearance.glassBarFill,
    ) {
        destinations.forEach { dest ->
            val selected = dest.route == selectedRoute
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(dest.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.icon,
                        contentDescription = dest.label,
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(dest.testTag),
            )
        }
    }
}
