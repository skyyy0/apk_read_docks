package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentType

/**
 * Atmospheric Liquid-Glass backdrop that renders gentle glacial refraction orbs in normal mode,
 * and falls back to an ultra-light flat canvas in Power Saving Mode.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val appearance = LocalLuminaAppearance.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(appearance.canvasTop, appearance.canvasBottom)
                )
            )
            .drawBehind {
                if (appearance.liquidGlassEnabled && !appearance.powerSavingMode) {
                    // Top-right soft glacial light bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(appearance.ambientOrbPrimary, Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.12f),
                            radius = size.width * 0.72f,
                        ),
                        radius = size.width * 0.72f,
                        center = Offset(size.width * 0.88f, size.height * 0.12f),
                    )
                    // Mid-left subtle refraction glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(appearance.ambientOrbSecondary, Color.Transparent),
                            center = Offset(size.width * 0.12f, size.height * 0.56f),
                            radius = size.width * 0.65f,
                        ),
                        radius = size.width * 0.65f,
                        center = Offset(size.width * 0.12f, size.height * 0.56f),
                    )
                }
            },
        content = content,
    )
}

/**
 * Signature Liquid Glass card with specular top-edge refraction gradient and crisp hairline border.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevated: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val appearance = LocalLuminaAppearance.current
    val fill = if (elevated) appearance.glassElevatedFill else appearance.glassFill
    val borderBrush = if (appearance.liquidGlassEnabled) {
        Brush.verticalGradient(
            colors = listOf(
                appearance.glassSpecularTop,
                appearance.glassBorder,
                appearance.glassBorder.copy(alpha = appearance.glassBorder.alpha * 0.6f),
            )
        )
    } else {
        Brush.verticalGradient(listOf(appearance.glassBorder, appearance.glassBorder))
    }

    val sheenBrush = if (appearance.liquidGlassEnabled) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (appearance.isDark) 0.08f else 0.42f),
                Color.Transparent,
                Color.White.copy(alpha = if (appearance.isDark) 0.02f else 0.14f),
            ),
            start = Offset(0f, 0f),
            end = Offset(600f, 500f),
        )
    } else {
        null
    }

    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(
                if (appearance.liquidGlassEnabled && !appearance.isDark) {
                    Modifier.shadow(
                        elevation = if (elevated) 10.dp else 4.dp,
                        shape = shape,
                        ambientColor = Color(0xFF0F172A).copy(alpha = 0.08f),
                        spotColor = Color(0xFF0F172A).copy(alpha = 0.07f),
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(fill)
            .then(if (sheenBrush != null) Modifier.background(sheenBrush) else Modifier)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(clickableMod)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Custom vector document-format jewel badge with folded corner & crisp monospaced extension label.
 */
@Composable
fun DocumentFormatBadge(
    type: DocumentType,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val accent = formatAccentColor(type)
    val isDark = LocalLuminaAppearance.current.isDark
    val bgTint = accent.copy(alpha = if (isDark) 0.18f else 0.12f)
    val foldTint = accent.copy(alpha = if (isDark) 0.45f else 0.28f)

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

            // Crisp micro accent bar on bottom
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

/**
 * Floating iOS-style Liquid Glass capsule navigation bar respecting WindowInsets.navigationBars.
 */
data class LuminaNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val testTag: String,
)

@Composable
fun FloatingGlassNavBar(
    destinations: List<LuminaNavDestination>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = LocalLuminaAppearance.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .then(
                    if (appearance.liquidGlassEnabled) {
                        Modifier.shadow(
                            elevation = if (appearance.isDark) 18.dp else 14.dp,
                            shape = shape,
                            ambientColor = Color.Black.copy(alpha = 0.16f),
                            spotColor = Color.Black.copy(alpha = 0.12f),
                        )
                    } else {
                        Modifier
                    }
                ),
            shape = shape,
            color = appearance.capsuleFill,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(appearance.glassSpecularTop, appearance.glassBorder)
                ),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { dest ->
                    val isSelected = dest.route == selectedRoute
                    val animSpec = if (appearance.powerSavingMode) {
                        snap<Color>()
                    } else {
                        spring(stiffness = Spring.StiffnessMediumLow)
                    }
                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = if (appearance.isDark) 0.24f else 0.14f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = animSpec,
                        label = "navPillBg",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = animSpec,
                        label = "navContentColor",
                    )

                    Row(
                        modifier = Modifier
                            .weight(if (isSelected) 1.32f else 1f)
                            .fillMaxHeight()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(pillBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                            ) {
                                if (!isSelected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(dest.route)
                                }
                            }
                            .testTag(dest.testTag),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isSelected) dest.selectedIcon else dest.icon,
                            contentDescription = dest.label,
                            tint = contentColor,
                            modifier = Modifier.size(21.dp),
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelLarge,
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
}

/**
 * Refined iOS-style tactile pill chip for filters and quick controls.
 */
@Composable
fun GlassFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingDotColor: Color? = null,
) {
    val appearance = LocalLuminaAppearance.current
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else appearance.glassFill,
        animationSpec = if (appearance.powerSavingMode) snap() else spring(stiffness = Spring.StiffnessMedium),
        label = "filterPillBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = if (appearance.powerSavingMode) snap() else spring(stiffness = Spring.StiffnessMedium),
        label = "filterPillFg",
    )
    val padHorizontal by animateDpAsState(
        targetValue = if (selected) 16.dp else 14.dp,
        animationSpec = if (appearance.powerSavingMode) snap() else spring(stiffness = Spring.StiffnessMedium),
        label = "filterPillPad",
    )

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else appearance.glassBorder,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = padHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingDotColor != null) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else leadingDotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
        )
    }
}
