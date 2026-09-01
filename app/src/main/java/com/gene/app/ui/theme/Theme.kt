package com.gene.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    background = GeneBlack,
    surface = GeneBlack,
    primary = GeneWhite,
    onBackground = GeneWhite,
    onSurface = GeneWhite,
    onPrimary = GeneBlack,
    outline = GeneDarkOutline
)

private val LightColorScheme = lightColorScheme(
    background = GeneWhite,
    surface = GeneWhite,
    primary = GeneBlack,
    onBackground = GeneBlack,
    onSurface = GeneBlack,
    onPrimary = GeneWhite,
    outline = GeneLightOutline
)

@Composable
fun GeneTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = MaterialTheme.typography.copy(
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 25.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, lineHeight = 34.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 26.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 21.sp)
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content
    )
}
