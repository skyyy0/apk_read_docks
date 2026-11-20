package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMedium,
)

val LuminaSpringLiquidDp = spring<Dp>(
    dampingRatio = 0.76f,
    stiffness = 380f,
)

val LuminaSpringOffset = spring<IntOffset>(
    dampingRatio = 0.85f,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * Subtle tactile GPU press-scale micro-interaction (no layout invalidation; respects Reduce Motion).
 */
fun Modifier.bouncyPress(
    enabled: Boolean = true,
    pressedScale: Float = 0.968f,
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
 * Atmospheric canvas with dual GPU-cached ambient glows so floating Liquid Glass bars refract depth.
 */
@Composable
fun LuminaWorkspaceCanvas(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val appearance = LocalLuminaAppearance.current
    val base = MaterialTheme.colorScheme.background
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = if (appearance.isDark) 0.11f else 0.08f)
    val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = if (appearance.isDark) 0.08f else 0.055f)
    val showGlow = appearance.liquidGlassEnabled && !appearance.powerSavingMode

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawWithCache {
                val topRight = Offset(size.width * 0.92f, size.height * 0.02f)
                val topRadius = (size.width * 0.95f).coerceAtLeast(1f)
                val bottomLeft = Offset(size.width * 0.08f, size.height * 0.94f)
                val bottomRadius = (size.width * 0.80f).coerceAtLeast(1f)

                val topBrush = if (showGlow) {
                    Brush.radialGradient(
                        colors = listOf(primaryGlow, Color.Transparent),
                        center = topRight,
                        radius = topRadius,
                    )
                } else null
                val bottomBrush = if (showGlow) {
                    Brush.radialGradient(
                        colors = listOf(secondaryGlow, Color.Transparent),
                        center = bottomLeft,
                        radius = bottomRadius,
                    )
                } else null

                onDrawBehind {
                    if (topBrush != null) {
                        drawCircle(brush = topBrush, radius = topRadius, center = topRight)
                    }
                    if (bottomBrush != null) {
                        drawCircle(brush = bottomBrush, radius = bottomRadius, center = bottomLeft)
                    }
                }
            },
        content = content,
    )
}

/**
 * Liquid Glass optical capsule / floating toolbar with multi-stop specular rim & subtle inner frost sheen.
 */
@Composable
fun GlassToolbarSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val appearance = LocalLuminaAppearance.current
    val isDark = appearance.isDark
    val borderBrush = remember(appearance.glassSpecularEdge, appearance.glassHairlineBorder, isDark) {
        Brush.verticalGradient(
            listOf(
                appearance.glassSpecularEdge,
                appearance.glassHairlineBorder.copy(alpha = if (isDark) 0.35f else 0.55f),
                appearance.glassSpecularEdge.copy(alpha = if (isDark) 0.25f else 0.70f),
            )
        )
    }
    val innerSheenTop = Color.White.copy(alpha = if (isDark) 0.07f else 0.38f)

    Surface(
        modifier = modifier,
        shape = shape,
        color = appearance.glassBarFill,
        shadowElevation = if (appearance.liquidGlassEnabled && !isDark) 6.dp else if (appearance.liquidGlassEnabled) 3.dp else 0.dp,
        border = BorderStroke(1.dp, borderBrush),
    ) {
        Box(
            modifier = Modifier
                .drawWithCache {
                    val sheen = if (appearance.liquidGlassEnabled) {
                        Brush.verticalGradient(
                            colors = listOf(innerSheenTop, Color.Transparent),
                            startY = 0f,
                            endY = (size.height * 0.45f).coerceAtLeast(1f),
                        )
                    } else null
                    onDrawBehind {
                        if (sheen != null) drawRect(brush = sheen)
                    }
                }
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Circular Liquid Glass icon orb (48dp touch target) for crisp iOS-style floating controls.
 */
@Composable
fun LiquidGlassIconOrb(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    active: Boolean = false,
    size: Dp = 48.dp,
) {
    val haptics = LocalHapticFeedback.current
    val appearance = LocalLuminaAppearance.current
    val fillColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primaryContainer else appearance.glassBarFill,
        label = "orbFill",
    )
    val borderBrush = remember(appearance.glassSpecularEdge, appearance.glassHairlineBorder) {
        Brush.verticalGradient(
            listOf(appearance.glassSpecularEdge, appearance.glassHairlineBorder.copy(alpha = 0.45f))
        )
    }

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = CircleShape,
        color = fillColor,
        shadowElevation = if (appearance.liquidGlassEnabled && !appearance.isDark) 4.dp else 0.dp,
        border = BorderStroke(0.9.dp, borderBrush),
        modifier = modifier
            .size(size)
            .bouncyPress(pressedScale = 0.90f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Soft iOS Inset Grouped section container (continuous 26dp corners, delicate hairline border).
 */
@Composable
fun EditorialSectionSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    tonalLevel: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = tonalLevel,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        content()
    }
}

/**
 * iOS-style Liquid Glass capsule segmented control with elastic spring-sliding thumb.
 */
@Composable
fun LuminaSegmentedCapsule(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "segment",
) {
    val haptics = LocalHapticFeedback.current
    val appearance = LocalLuminaAppearance.current
    val selectedIndex = options.indexOfFirst { it.first == selectedKey }.coerceAtLeast(0)

    val trackColor = if (appearance.isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    }
    val thumbColor = if (appearance.isDark) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.White
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(0.75.dp, appearance.glassHairlineBorder.copy(alpha = 0.4f), CircleShape)
            .padding(4.dp),
    ) {
        val count = options.size.coerceAtLeast(1)
        val slotWidth = maxWidth / count
        val thumbOffset by animateDpAsState(
            targetValue = slotWidth * selectedIndex,
            animationSpec = if (appearance.reduceMotion) spring(stiffness = Spring.StiffnessHigh) else LuminaSpringLiquidDp,
            label = "segmentedThumbOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(slotWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(thumbColor)
                .border(0.75.dp, appearance.glassSpecularEdge.copy(alpha = 0.7f), CircleShape),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { (key, label) ->
                val isSelected = key == selectedKey
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (appearance.isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "segmentTextColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(key)
                            }
                        }
                        .testTag("${testTagPrefix}_${key.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Smooth iOS-inspired spring pill toggle switch with 48dp accessibility target.
 */
@Composable
fun LuminaIosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDesc: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    val appearance = LocalLuminaAppearance.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            Color(0xFF34C759)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "iosSwitchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = if (appearance.reduceMotion) spring(stiffness = Spring.StiffnessHigh) else LuminaSpringLiquidDp,
        label = "iosSwitchThumb",
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .width(56.dp)
            .semantics {
                if (contentDesc != null) contentDescription = contentDesc
            }
            .clickable(
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(trackColor)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape),
            )
        }
    }
}

/**
 * Soft iOS-inspired vector document-format badge with rounded corners, folded leaf & crisp extension badge.
 */
@Composable
fun DocumentFormatBadge(
    type: DocumentType,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val accent = formatAccentColor(type)
    val isDark = LocalLuminaAppearance.current.isDark
    val bgTint = accent.copy(alpha = if (isDark) 0.20f else 0.12f)
    val foldTint = accent.copy(alpha = if (isDark) 0.45f else 0.26f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val fold = w * 0.28f
            val r = w * 0.24f

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
                topLeft = Offset(w * 0.18f, h * 0.78f),
                size = Size(w * 0.64f, h * 0.09f),
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
 * Floating iOS 26 Liquid Glass Pill Dock with elastic sliding glass selection indicator.
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
    val selectedIndex = destinations.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)

    GlassToolbarSurface(
        modifier = modifier.height(64.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(5.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val count = destinations.size.coerceAtLeast(1)
            val slotWidth = maxWidth / count
            val pillOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = if (appearance.reduceMotion) spring(stiffness = Spring.StiffnessHigh) else LuminaSpringLiquidDp,
                label = "glassDockPillOffset",
            )

            val pillFill = if (appearance.isDark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
            }

            Box(
                modifier = Modifier
                    .offset(x = pillOffset)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(pillFill)
                    .border(
                        width = 0.8.dp,
                        color = appearance.glassSpecularEdge.copy(alpha = if (appearance.isDark) 0.25f else 0.85f),
                        shape = CircleShape,
                    ),
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { dest ->
                    val selected = dest.route == selectedRoute
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected && !appearance.reduceMotion) 1.1f else 1f,
                        animationSpec = LuminaSpringSnappy,
                        label = "dockIconScale",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "dockItemColor",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (!selected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(dest.route)
                                }
                            }
                            .testTag(dest.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.icon,
                            contentDescription = dest.label,
                            tint = contentColor,
                            modifier = Modifier
                                .size(21.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
