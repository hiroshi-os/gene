package com.tom7.gene.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tom7.gene.ui.theme.GeneColors
import com.tom7.gene.ui.theme.GeneRadius
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun rememberGeneHazeState(): HazeState = rememberHazeState()

enum class GlassThickness {
    Thin,
    Regular,
    Thick,
    Ultra
}

fun Modifier.geneHazeSource(state: HazeState?): Modifier =
    if (state != null) this.hazeSource(state = state) else this

/**
 * Official Haze Cupertino materials — real backdrop blur (the selling point).
 * Content behind must use [geneHazeSource] with the same [HazeState].
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun geneMaterialStyle(thickness: GlassThickness): HazeStyle {
    val container = MaterialTheme.colorScheme.surface
    return when (thickness) {
        GlassThickness.Thin -> CupertinoMaterials.ultraThin(container)
        GlassThickness.Regular -> CupertinoMaterials.thin(container)
        GlassThickness.Thick -> CupertinoMaterials.regular(container)
        GlassThickness.Ultra -> CupertinoMaterials.thick(container)
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
fun Modifier.geneGlass(
    state: HazeState,
    colors: GeneColors,
    thickness: GlassThickness = GlassThickness.Regular,
    shape: Shape = RoundedCornerShape(GeneRadius.md)
): Modifier = composed {
    val style = geneMaterialStyle(thickness)
    val borderColor = if (colors.dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.65f)
    this
        .shadow(8.dp, shape, ambientColor = colors.shadow.copy(alpha = 0.25f), spotColor = colors.shadow.copy(alpha = 0.25f))
        .clip(shape)
        .hazeEffect(state = state, style = style)
        .border(0.5.dp, borderColor, shape)
}

/** Fallback only when no HazeState — prefer [geneGlass] for real blur. */
fun Modifier.geneFrostedSurface(
    colors: GeneColors,
    shape: Shape = RoundedCornerShape(GeneRadius.md),
    elevation: Dp = 6.dp
): Modifier {
    val borderColor = if (colors.dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.75f)
    val acrylic = if (colors.dark) {
        Brush.verticalGradient(listOf(Color(0xCC2A2A2A), Color(0xB8202020)))
    } else {
        Brush.verticalGradient(listOf(Color(0xE6FFFFFF), Color(0xCCF7F6F3)))
    }
    return this
        .shadow(elevation, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .clip(shape)
        .background(acrylic)
        .border(0.5.dp, borderColor, shape)
}

fun Modifier.geneCardSurface(
    colors: GeneColors,
    shape: Shape = RoundedCornerShape(GeneRadius.md)
): Modifier = this
    .clip(shape)
    .background(colors.surface)
    .border(0.5.dp, colors.border.copy(alpha = 0.85f), shape)

@Composable
fun GeneGlassSurface(
    state: HazeState?,
    colors: GeneColors,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GeneRadius.lg),
    thickness: GlassThickness = GlassThickness.Regular,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val base = if (state != null) {
        modifier.geneGlass(state, colors, thickness, shape)
    } else {
        modifier.geneFrostedSurface(colors, shape)
    }
    Box(
        modifier = base.then(
            if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
            else Modifier
        ),
        content = content
    )
}
