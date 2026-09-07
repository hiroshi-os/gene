package com.tom7.gene.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

// ─── Core Gene (kept for compatibility) ───────────────────────────────────────
val GeneBlack = Color(0xFF191919)
val GeneWhite = Color(0xFFF7F6F3)
val GeneGray = Color(0xFF787774)
val GeneMutedGray = Color(0xFF9B9A97)
val GeneDarkOutline = Color(0xFF37352F)
val GeneLightOutline = Color(0xFFE3E2E0)

// ─── Notion Design System — Light ─────────────────────────────────────────────
val NotionLightBg = Color(0xFFF7F6F3)
val NotionLightSurface = Color(0xFFFFFFFF)
val NotionLightElevated = Color(0xFFFFFFFF)
val NotionLightFrosted = Color(0xE8FFFFFF)
val NotionLightPill = Color(0x0F37352F)
val NotionLightPillActive = Color(0xFF37352F)
val NotionLightAvatar = Color(0xFFE9E9E7)
val NotionLightTextPrimary = Color(0xFF37352F)
val NotionLightTextSecondary = Color(0xFF787774)
val NotionLightTextTertiary = Color(0xFF9B9A97)
val NotionLightDivider = Color(0xFFE3E2E0)
val NotionLightBorder = Color(0xFFE9E9E7)
val NotionLightHover = Color(0x0A37352F)
val NotionLightShadow = Color(0x1A000000)

// ─── Notion Design System — Dark ──────────────────────────────────────────────
val NotionDarkBg = Color(0xFF191919)
val NotionDarkSurface = Color(0xFF202020)
val NotionDarkElevated = Color(0xFF2A2A2A)
val NotionDarkFrosted = Color(0xE8202020)
val NotionDarkPill = Color(0x14FFFFFF)
val NotionDarkPillActive = Color(0xFFECEBEA)
val NotionDarkAvatar = Color(0xFF2F2F2F)
val NotionDarkTextPrimary = Color(0xFFECEBEA)
val NotionDarkTextSecondary = Color(0xFF9B9A97)
val NotionDarkTextTertiary = Color(0xFF6F6E6B)
val NotionDarkDivider = Color(0xFF2F2F2F)
val NotionDarkBorder = Color(0xFF373737)
val NotionDarkHover = Color(0x14FFFFFF)
val NotionDarkShadow = Color(0x66000000)

// Accent (Notion link / interactive blue)
val NotionBlue = Color(0xFF2383E2)
val NotionBlueDark = Color(0xFF529CCA)
val NotionRed = Color(0xFFE03E3E)
val NotionRedDark = Color(0xFFFF7369)

// Soft pastel covers for Recents cards (Notion-like)
val NotionPastelYellow = Color(0xFFFBF3DB)
val NotionPastelPink = Color(0xFFF5E0E9)
val NotionPastelPurple = Color(0xFFE8DEEE)
val NotionPastelBlue = Color(0xFFD3E5EF)
val NotionPastelGreen = Color(0xFFE2ECDC)
val NotionPastelOrange = Color(0xFFFADEC9)
val NotionPastelGray = Color(0xFFEBEBEA)

val NotionPastelYellowDark = Color(0xFF3D3B2F)
val NotionPastelPinkDark = Color(0xFF3D2F35)
val NotionPastelPurpleDark = Color(0xFF352F3D)
val NotionPastelBlueDark = Color(0xFF2F353D)
val NotionPastelGreenDark = Color(0xFF2F3D32)
val NotionPastelOrangeDark = Color(0xFF3D322F)
val NotionPastelGrayDark = Color(0xFF2F2F2F)

private val LightPastels = listOf(
    NotionPastelYellow, NotionPastelPink, NotionPastelPurple,
    NotionPastelBlue, NotionPastelGreen, NotionPastelOrange, NotionPastelGray
)
private val DarkPastels = listOf(
    NotionPastelYellowDark, NotionPastelPinkDark, NotionPastelPurpleDark,
    NotionPastelBlueDark, NotionPastelGreenDark, NotionPastelOrangeDark, NotionPastelGrayDark
)

@Immutable
data class GeneColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val elevated: Color,
    val frosted: Color,
    val pill: Color,
    val pillActive: Color,
    val avatar: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val border: Color,
    val hover: Color,
    val shadow: Color,
    val accent: Color,
    val danger: Color,
    val onPillActive: Color,
) {
    fun pastel(index: Int): Color {
        val list = if (dark) DarkPastels else LightPastels
        return list[kotlin.math.abs(index) % list.size]
    }
}

fun geneColors(dark: Boolean): GeneColors = if (dark) {
    GeneColors(
        dark = true,
        bg = NotionDarkBg,
        surface = NotionDarkSurface,
        elevated = NotionDarkElevated,
        frosted = NotionDarkFrosted,
        pill = NotionDarkPill,
        pillActive = NotionDarkPillActive,
        avatar = NotionDarkAvatar,
        textPrimary = NotionDarkTextPrimary,
        textSecondary = NotionDarkTextSecondary,
        textTertiary = NotionDarkTextTertiary,
        divider = NotionDarkDivider,
        border = NotionDarkBorder,
        hover = NotionDarkHover,
        shadow = NotionDarkShadow,
        accent = NotionBlueDark,
        danger = NotionRedDark,
        onPillActive = NotionDarkBg,
    )
} else {
    GeneColors(
        dark = false,
        bg = NotionLightBg,
        surface = NotionLightSurface,
        elevated = NotionLightElevated,
        frosted = NotionLightFrosted,
        pill = NotionLightPill,
        pillActive = NotionLightPillActive,
        avatar = NotionLightAvatar,
        textPrimary = NotionLightTextPrimary,
        textSecondary = NotionLightTextSecondary,
        textTertiary = NotionLightTextTertiary,
        divider = NotionLightDivider,
        border = NotionLightBorder,
        hover = NotionLightHover,
        shadow = NotionLightShadow,
        accent = NotionBlue,
        danger = NotionRed,
        onPillActive = Color.White,
    )
}

@Composable
fun rememberGeneColors(dark: Boolean): GeneColors = remember(dark) { geneColors(dark) }
