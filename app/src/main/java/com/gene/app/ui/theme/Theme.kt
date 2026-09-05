package com.gene.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ColorWhite = Color.White

private val DarkColorScheme = darkColorScheme(
    background = NotionDarkBg,
    surface = NotionDarkSurface,
    primary = NotionDarkPillActive,
    onBackground = NotionDarkTextPrimary,
    onSurface = NotionDarkTextPrimary,
    onPrimary = NotionDarkBg,
    secondary = NotionBlueDark,
    onSecondary = NotionDarkBg,
    outline = NotionDarkBorder,
    surfaceVariant = NotionDarkElevated,
    onSurfaceVariant = NotionDarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    background = NotionLightBg,
    surface = NotionLightSurface,
    primary = NotionLightPillActive,
    onBackground = NotionLightTextPrimary,
    onSurface = NotionLightTextPrimary,
    onPrimary = ColorWhite,
    secondary = NotionBlue,
    onSecondary = ColorWhite,
    outline = NotionLightBorder,
    surfaceVariant = NotionLightAvatar,
    onSurfaceVariant = NotionLightTextSecondary
)

private fun geneTypography(dark: Boolean): Typography {
    val primary = if (dark) NotionDarkTextPrimary else NotionLightTextPrimary
    val secondary = if (dark) NotionDarkTextSecondary else NotionLightTextSecondary
    return Typography(
        displayLarge = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.6).sp,
            color = primary
        ),
        headlineLarge = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.4).sp,
            color = primary
        ),
        titleLarge = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.2).sp,
            color = primary
        ),
        titleMedium = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = primary
        ),
        titleSmall = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = primary
        ),
        bodyLarge = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = primary
        ),
        bodyMedium = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = primary
        ),
        bodySmall = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = secondary
        ),
        labelLarge = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = primary
        ),
        labelMedium = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = secondary
        ),
        labelSmall = TextStyle(
            fontFamily = GeneFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = secondary
        )
    )
}

@Composable
fun GeneTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = geneTypography(darkTheme),
        content = content
    )
}
