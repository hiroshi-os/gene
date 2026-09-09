package com.tom7.gene.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.ui.common.MemojiAvatar
import com.tom7.gene.ui.common.MemojiConfig
import com.tom7.gene.ui.common.MemojiLabControls
import com.tom7.gene.ui.common.randomMemojiConfig
import com.tom7.gene.ui.theme.GeneColors
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.rememberGeneColors
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private enum class OnboardStep { Constellation, YouLab, SomeoneLab }

@Composable
fun OnboardingScreen(
    dark: Boolean,
    onSkip: () -> Unit,
    onSelfReady: (avatar: String) -> Unit,
    onPersonCreated: (name: String, note: String, avatar: String) -> Unit
) {
    val colors = rememberGeneColors(dark)
    var step by remember { mutableStateOf(OnboardStep.Constellation) }
    var selfConfig by remember { mutableStateOf(randomMemojiConfig()) }
    var otherConfig by remember { mutableStateOf(randomMemojiConfig()) }
    var otherName by remember { mutableStateOf("") }

    BackHandler {
        when (step) {
            OnboardStep.Constellation -> onSkip()
            OnboardStep.YouLab -> step = OnboardStep.Constellation
            OnboardStep.SomeoneLab -> step = OnboardStep.YouLab
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        DotGridBackdrop(colors)

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(280)) + slideInHorizontally { it / 8 }) togetherWith
                    (fadeOut(tween(200)) + slideOutHorizontally { -it / 8 })
            },
            label = "onboard"
        ) { current ->
            when (current) {
                OnboardStep.Constellation -> ConstellationStep(
                    colors = colors,
                    onEnter = { step = OnboardStep.YouLab },
                    onSkip = onSkip
                )
                OnboardStep.YouLab -> AvatarLabStep(
                    colors = colors,
                    dark = dark,
                    title = "Make yourself",
                    subtitle = "This is You — Gene’s self profile.",
                    config = selfConfig,
                    onConfigChange = { selfConfig = it },
                    primaryLabel = "That’s me",
                    onPrimary = {
                        onSelfReady(selfConfig.serialize())
                        otherConfig = randomMemojiConfig()
                        step = OnboardStep.SomeoneLab
                    },
                    secondaryLabel = "Skip for now",
                    onSecondary = {
                        onSelfReady(selfConfig.serialize())
                        step = OnboardStep.SomeoneLab
                    },
                    showNameField = false,
                    name = "",
                    onNameChange = {}
                )
                OnboardStep.SomeoneLab -> AvatarLabStep(
                    colors = colors,
                    dark = dark,
                    title = "Who’s on your mind?",
                    subtitle = "Invent someone. Then capture a memory.",
                    config = otherConfig,
                    onConfigChange = { otherConfig = it },
                    primaryLabel = "Capture a memory",
                    onPrimary = {
                        val name = otherName.trim().ifBlank { "Someone" }
                        onPersonCreated(name, "", otherConfig.serialize())
                    },
                    secondaryLabel = "Skip to home",
                    onSecondary = onSkip,
                    showNameField = true,
                    name = otherName,
                    onNameChange = { otherName = it },
                    primaryEnabled = true
                )
            }
        }
    }
}

@Composable
private fun DotGridBackdrop(colors: GeneColors) {
    Canvas(Modifier.fillMaxSize()) {
        val step = 28.dp.toPx()
        val r = 1.2f
        val c = colors.border.copy(alpha = 0.45f)
        var x = step / 2f
        while (x < size.width) {
            var y = step / 2f
            while (y < size.height) {
                drawCircle(c, r, Offset(x, y))
                y += step
            }
            x += step
        }
    }
}

@Composable
private fun ConstellationStep(
    colors: GeneColors,
    onEnter: () -> Unit,
    onSkip: () -> Unit
) {
    val faces = remember {
        List(6) { randomMemojiConfig() }
    }
    val infinite = rememberInfiniteTransition(label = "orbit")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = GeneSpace.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val orbitR = minOf(maxWidth, maxHeight).value * 0.32f
            faces.forEachIndexed { i, cfg ->
                val angle = phase + i * (Math.PI * 2 / faces.size).toFloat()
                val jitter = 8f * sin(phase * 1.7f + i)
                val dx = (orbitR * cos(angle) + jitter).dp
                val dy = (orbitR * 0.72f * sin(angle)).dp
                val size = (44 + (i % 3) * 10).dp
                MemojiAvatar(
                    config = cfg,
                    size = size,
                    modifier = Modifier
                        .offset(dx, dy)
                        .graphicsLayer {
                            alpha = 0.88f
                            translationY = 4f * sin(phase * 2f + i)
                        }
                )
            }

            // Charcoal Gene mark — same DNA as floating bubble
            Box(
                Modifier
                    .scale(pulse)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.pillActive),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "G",
                    color = colors.onPillActive,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GeneFontFamily
                )
            }
        }

        Text(
            "Gene",
            color = colors.textPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = GeneFontFamily,
            letterSpacing = (-0.6).sp
        )
        Spacer(Modifier.height(GeneSpace.xs))
        Text(
            "Private context about people —\nbuilt one memory at a time.",
            color = colors.textSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            fontFamily = GeneFontFamily
        )
        Spacer(Modifier.height(GeneSpace.xl))

        LabPrimaryButton(label = "Enter the lab", colors = colors, onClick = onEnter)
        Spacer(Modifier.height(GeneSpace.sm))
        LabTextAction(label = "Skip", colors = colors, onClick = onSkip)
        Spacer(Modifier.height(GeneSpace.lg))
    }
}

@Composable
private fun AvatarLabStep(
    colors: GeneColors,
    dark: Boolean,
    title: String,
    subtitle: String,
    config: MemojiConfig,
    onConfigChange: (MemojiConfig) -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    showNameField: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    primaryEnabled: Boolean = true
) {
    // Idle expression cycle — avatar-lab energy without leaving the user’s choice forever
    var displayConfig by remember(config) { mutableStateOf(config) }
    LaunchedEffect(config) {
        displayConfig = config
        while (true) {
            delay(2800)
            val cycle = listOf("happy", "wink", "happy", "neutral")
            for (expr in cycle) {
                displayConfig = config.copy(eyeExpression = expr)
                delay(380)
            }
            displayConfig = config
            delay(3200)
        }
    }

    val infinite = rememberInfiniteTransition(label = "labPulse")
    val breath by infinite.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = GeneSpace.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(GeneSpace.md))
        Text(
            title,
            color = colors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = GeneFontFamily,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.padding(horizontal = GeneSpace.lg)
        )
        Spacer(Modifier.height(GeneSpace.xs))
        Text(
            subtitle,
            color = colors.textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = GeneFontFamily,
            modifier = Modifier.padding(horizontal = GeneSpace.xl)
        )

        Spacer(Modifier.height(GeneSpace.lg))

        Box(contentAlignment = Alignment.Center) {
            // Soft pastel halo behind the face
            Box(
                Modifier
                    .size(168.dp)
                    .scale(breath)
                    .clip(CircleShape)
                    .background(config.bgColor.copy(alpha = 0.55f))
            )
            MemojiAvatar(
                config = displayConfig,
                size = 140.dp,
                modifier = Modifier.scale(breath)
            )
        }

        IconButton(
            onClick = {
                val next = randomMemojiConfig()
                onConfigChange(next)
            }
        ) {
            Icon(Icons.Outlined.Casino, contentDescription = "Surprise me", tint = colors.accent)
        }

        if (showNameField) {
            Spacer(Modifier.height(GeneSpace.xs))
            NameField(
                value = name,
                onValueChange = onNameChange,
                colors = colors,
                placeholder = "Their name"
            )
            Spacer(Modifier.height(GeneSpace.md))
        } else {
            Spacer(Modifier.height(GeneSpace.sm))
        }

        MemojiLabControls(
            config = config,
            onConfigChange = onConfigChange,
            isDark = dark
        )

        Spacer(Modifier.height(GeneSpace.xl))

        Column(Modifier.padding(horizontal = GeneSpace.lg)) {
            LabPrimaryButton(
                label = primaryLabel,
                colors = colors,
                onClick = onPrimary,
                enabled = primaryEnabled
            )
            Spacer(Modifier.height(GeneSpace.sm))
            LabTextAction(label = secondaryLabel, colors = colors, onClick = onSecondary)
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    colors: GeneColors,
    placeholder: String
) {
    Box(
        Modifier
            .padding(horizontal = GeneSpace.lg)
            .fillMaxWidth()
            .clip(RoundedCornerShape(GeneRadius.md))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(GeneRadius.md))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneFontFamily
            ),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = colors.textTertiary, fontSize = 16.sp, fontFamily = GeneFontFamily)
                }
                inner()
            }
        )
    }
}

@Composable
private fun LabPrimaryButton(
    label: String,
    colors: GeneColors,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(GeneRadius.pill))
            .background(if (enabled) colors.pillActive else colors.pill)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) colors.onPillActive else colors.textTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneFontFamily
        )
    }
}

@Composable
private fun LabTextAction(label: String, colors: GeneColors, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        label,
        color = colors.textSecondary,
        fontSize = 14.sp,
        fontFamily = GeneFontFamily,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = GeneSpace.xs)
    )
}
