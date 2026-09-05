package com.gene.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gene.app.ui.theme.rememberGeneColors
import com.gene.app.ui.theme.GeneFontFamily

data class MemojiConfig(
    val skinColor: Color = Color(0xFFF0D5BE),
    val hairStyle: String = "short", // short, curly, bob, wavy, buzz, bald
    val hairColor: Color = Color(0xFF2C1B18),
    val eyeExpression: String = "happy", // happy, neutral, wink
    val glasses: String = "none", // none, round, square, sunglasses
    val bgColor: Color = Color(0xFFD3E5EF)
) {
    fun serialize(): String = listOf(
        skinColor.value.toLong().toString(16),
        hairStyle,
        hairColor.value.toLong().toString(16),
        eyeExpression,
        glasses,
        bgColor.value.toLong().toString(16)
    ).joinToString(";")

    companion object {
        fun deserialize(str: String?): MemojiConfig {
            if (str.isNullOrBlank()) return MemojiConfig()
            val parts = str.split(";")
            return try {
                MemojiConfig(
                    skinColor = Color(parts.getOrElse(0) { "FFF0D5BE" }.toLong(16)),
                    hairStyle = parts.getOrElse(1) { "short" },
                    hairColor = Color(parts.getOrElse(2) { "FF2C1B18" }.toLong(16)),
                    eyeExpression = parts.getOrElse(3) { "happy" },
                    glasses = parts.getOrElse(4) { "none" },
                    bgColor = Color(parts.getOrElse(5) { "FFD3E5EF" }.toLong(16))
                )
            } catch (e: Exception) {
                MemojiConfig()
            }
        }
    }
}

val SkinPalette = listOf(
    Color(0xFFFFDFC4), Color(0xFFF0D5BE), Color(0xFFEECEB3),
    Color(0xFFE0AC69), Color(0xFFC68642), Color(0xFF8D5524),
    Color(0xFF5C381E), Color(0xFF3B2219)
)

val HairColorPalette = listOf(
    Color(0xFF1A1A1A), Color(0xFF3D2314), Color(0xFF6A3B1E),
    Color(0xFFA0522D), Color(0xFFD4A373), Color(0xFFC0C0C0),
    Color(0xFF8E3B46), Color(0xFF2E4057)
)

val BgColorPalette = listOf(
    Color(0xFFFBF3DB), Color(0xFFF5E0E9), Color(0xFFE8DEEE),
    Color(0xFFD3E5EF), Color(0xFFE2ECDC), Color(0xFFFADEC9),
    Color(0xFFEBEBEA), Color(0xFF37352F)
)

val HairStyles = listOf("short", "curly", "bob", "wavy", "buzz", "bald")
val EyeExpressions = listOf("happy", "neutral", "wink")
val GlassesOptions = listOf("none", "round", "square", "sunglasses")

@Composable
fun MemojiAvatar(
    config: MemojiConfig,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(config.bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.82f)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Head / Face
            val headRadius = w * 0.36f
            val headCenter = Offset(w * 0.5f, h * 0.52f)
            drawCircle(
                color = config.skinColor,
                radius = headRadius,
                center = headCenter
            )

            // 2. Ears
            val earRadius = w * 0.08f
            drawCircle(color = config.skinColor, radius = earRadius, center = Offset(headCenter.x - headRadius * 0.95f, headCenter.y))
            drawCircle(color = config.skinColor, radius = earRadius, center = Offset(headCenter.x + headRadius * 0.95f, headCenter.y))

            // 3. Cheeks
            val blushColor = Color(0xFFFF6B6B).copy(alpha = 0.28f)
            drawCircle(color = blushColor, radius = w * 0.06f, center = Offset(w * 0.32f, h * 0.58f))
            drawCircle(color = blushColor, radius = w * 0.06f, center = Offset(w * 0.68f, h * 0.58f))

            // 4. Eyes & Eyebrows
            drawEyes(config.eyeExpression, w, h)

            // 5. Mouth / Smile
            drawMouth(w, h)

            // 6. Hair
            drawHair(config.hairStyle, config.hairColor, w, h, headCenter, headRadius)

            // 7. Glasses
            if (config.glasses != "none") {
                drawGlasses(config.glasses, w, h)
            }
        }
    }
}

private fun DrawScope.drawEyes(expression: String, w: Float, h: Float) {
    val eyeY = h * 0.48f
    val leftEyeX = w * 0.36f
    val rightEyeX = w * 0.64f
    val eyeColor = Color(0xFF1E1E1E)

    // Eyebrows
    drawLine(color = eyeColor, start = Offset(leftEyeX - w * 0.06f, eyeY - h * 0.09f), end = Offset(leftEyeX + w * 0.06f, eyeY - h * 0.08f), strokeWidth = w * 0.035f)
    drawLine(color = eyeColor, start = Offset(rightEyeX - w * 0.06f, eyeY - h * 0.08f), end = Offset(rightEyeX + w * 0.06f, eyeY - h * 0.09f), strokeWidth = w * 0.035f)

    when (expression) {
        "wink" -> {
            // Left eye winking curve
            val path = Path().apply {
                moveTo(leftEyeX - w * 0.05f, eyeY)
                quadraticBezierTo(leftEyeX, eyeY - h * 0.04f, leftEyeX + w * 0.05f, eyeY)
            }
            drawPath(path, color = eyeColor, style = Stroke(width = w * 0.035f))
            // Right eye open
            drawCircle(color = eyeColor, radius = w * 0.045f, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = w * 0.015f, center = Offset(rightEyeX - w * 0.015f, eyeY - h * 0.015f))
        }
        "neutral" -> {
            drawCircle(color = eyeColor, radius = w * 0.042f, center = Offset(leftEyeX, eyeY))
            drawCircle(color = eyeColor, radius = w * 0.042f, center = Offset(rightEyeX, eyeY))
        }
        else -> { // happy
            drawCircle(color = eyeColor, radius = w * 0.046f, center = Offset(leftEyeX, eyeY))
            drawCircle(color = Color.White, radius = w * 0.016f, center = Offset(leftEyeX - w * 0.014f, eyeY - h * 0.015f))
            drawCircle(color = eyeColor, radius = w * 0.046f, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = w * 0.016f, center = Offset(rightEyeX - w * 0.014f, eyeY - h * 0.015f))
        }
    }
}

private fun DrawScope.drawMouth(w: Float, h: Float) {
    val mouthPath = Path().apply {
        moveTo(w * 0.42f, h * 0.65f)
        quadraticBezierTo(w * 0.5f, h * 0.72f, w * 0.58f, h * 0.65f)
    }
    drawPath(mouthPath, color = Color(0xFFC2410C), style = Stroke(width = w * 0.03f))
}

private fun DrawScope.drawHair(
    style: String,
    color: Color,
    w: Float,
    h: Float,
    headCenter: Offset,
    headRadius: Float
) {
    if (style == "bald") return

    when (style) {
        "buzz" -> {
            drawArc(
                color = color.copy(alpha = 0.55f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 1.15f),
                size = Size(headRadius * 2.1f, headRadius * 1.5f),
                style = Stroke(width = w * 0.06f)
            )
        }
        "curly" -> {
            val curlRadius = w * 0.09f
            for (i in 0..5) {
                val angle = Math.PI * (0.85 + i * 0.25)
                val cx = (headCenter.x + headRadius * 0.95f * Math.cos(angle)).toFloat()
                val cy = (headCenter.y + headRadius * 0.95f * Math.sin(angle)).toFloat()
                drawCircle(color = color, radius = curlRadius, center = Offset(cx, cy))
            }
        }
        "bob" -> {
            // Side drapes + top cap
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 1.05f),
                size = Size(headRadius * 2.1f, headRadius * 2f)
            )
            // Left & right bob strands
            drawRoundRect(
                color = color,
                topLeft = Offset(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 0.2f),
                size = Size(w * 0.14f, h * 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(headCenter.x + headRadius * 0.72f, headCenter.y - headRadius * 0.2f),
                size = Size(w * 0.14f, h * 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f)
            )
        }
        else -> { // short
            val hairPath = Path().apply {
                moveTo(headCenter.x - headRadius * 1.02f, headCenter.y - headRadius * 0.1f)
                cubicTo(
                    headCenter.x - headRadius * 1.1f, headCenter.y - headRadius * 1.2f,
                    headCenter.x + headRadius * 1.1f, headCenter.y - headRadius * 1.2f,
                    headCenter.x + headRadius * 1.02f, headCenter.y - headRadius * 0.1f
                )
                quadraticBezierTo(headCenter.x, headCenter.y - headRadius * 0.5f, headCenter.x - headRadius * 1.02f, headCenter.y - headRadius * 0.1f)
                close()
            }
            drawPath(hairPath, color = color)
        }
    }
}

private fun DrawScope.drawGlasses(style: String, w: Float, h: Float) {
    val glassesY = h * 0.48f
    val leftX = w * 0.36f
    val rightX = w * 0.64f
    val gRadius = w * 0.09f
    val frameColor = if (style == "sunglasses") Color(0xDD111111) else Color(0xEE1F2937)

    // Left lens
    drawCircle(
        color = if (style == "sunglasses") frameColor else Color.Transparent,
        radius = gRadius,
        center = Offset(leftX, glassesY)
    )
    drawCircle(
        color = frameColor,
        radius = gRadius,
        center = Offset(leftX, glassesY),
        style = Stroke(width = w * 0.03f)
    )

    // Right lens
    drawCircle(
        color = if (style == "sunglasses") frameColor else Color.Transparent,
        radius = gRadius,
        center = Offset(rightX, glassesY)
    )
    drawCircle(
        color = frameColor,
        radius = gRadius,
        center = Offset(rightX, glassesY),
        style = Stroke(width = w * 0.03f)
    )

    // Bridge
    drawLine(
        color = frameColor,
        start = Offset(leftX + gRadius, glassesY),
        end = Offset(rightX - gRadius, glassesY),
        strokeWidth = w * 0.03f
    )
}

/**
 * Memoji maker — Notion chrome
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemojiMakerSheet(
    initialConfig: MemojiConfig = MemojiConfig(),
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (MemojiConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }
    val colors = rememberGeneColors(isDark)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Skin", "Hair", "Eyes", "Glasses", "Color")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: Cancel | Memoji | Done
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.accent, fontSize = 15.sp, fontFamily = GeneFontFamily)
                }
                Text(
                    "Memoji",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.textPrimary,
                    fontFamily = GeneFontFamily
                )
                TextButton(onClick = { onSave(config) }) {
                    Text("Done", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = GeneFontFamily)
                }
            }

            // Live Large Memoji Preview Bubble (100dp)
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(108.dp),
                contentAlignment = Alignment.Center
            ) {
                MemojiAvatar(config = config, size = 100.dp)
            }

            // Quick Randomize Button
            IconButton(
                onClick = {
                    config = config.copy(
                        skinColor = SkinPalette.random(),
                        hairStyle = HairStyles.random(),
                        hairColor = HairColorPalette.random(),
                        eyeExpression = EyeExpressions.random(),
                        glasses = GlassesOptions.random(),
                        bgColor = BgColorPalette.random()
                    )
                }
            ) {
                Icon(Icons.Outlined.Casino, "Randomize", tint = colors.accent)
            }

            // Category Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) colors.accent else colors.textSecondary,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Customization Options
            when (selectedTab) {
                0 -> { // Skin
                    ColorPaletteRow(colors = SkinPalette, selectedColor = config.skinColor) {
                        config = config.copy(skinColor = it)
                    }
                }
                1 -> { // Hair
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Style", color = if (isDark) Color.Gray else Color.DarkGray, fontSize = 13.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HairStyles.forEach { style ->
                                OptionChip(title = style.replaceFirstChar { it.uppercase() }, isSelected = config.hairStyle == style, isDark = isDark) {
                                    config = config.copy(hairStyle = style)
                                }
                            }
                        }
                        Text("Color", color = if (isDark) Color.Gray else Color.DarkGray, fontSize = 13.sp)
                        ColorPaletteRow(colors = HairColorPalette, selectedColor = config.hairColor) {
                            config = config.copy(hairColor = it)
                        }
                    }
                }
                2 -> { // Eyes
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EyeExpressions.forEach { expr ->
                            OptionChip(title = expr.replaceFirstChar { it.uppercase() }, isSelected = config.eyeExpression == expr, isDark = isDark) {
                                config = config.copy(eyeExpression = expr)
                            }
                        }
                    }
                }
                3 -> { // Glasses
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassesOptions.forEach { g ->
                            OptionChip(title = g.replaceFirstChar { it.uppercase() }, isSelected = config.glasses == g, isDark = isDark) {
                                config = config.copy(glasses = g)
                            }
                        }
                    }
                }
                4 -> { // Background Color
                    ColorPaletteRow(colors = BgColorPalette, selectedColor = config.bgColor) {
                        config = config.copy(bgColor = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteRow(
    colors: List<Color>,
    selectedColor: Color,
    onSelect: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(color) }
                    .then(
                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                        else Modifier
                    )
            )
        }
    }
}

@Composable
private fun OptionChip(
    title: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val colors = rememberGeneColors(isDark)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.pillActive else colors.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) colors.onPillActive else colors.textPrimary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            fontFamily = GeneFontFamily
        )
    }
}
