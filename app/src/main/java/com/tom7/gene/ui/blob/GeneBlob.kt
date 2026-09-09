package com.tom7.gene.ui.blob

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import com.tom7.gene.ui.theme.BlobBlush
import com.tom7.gene.ui.theme.BlobCream
import com.tom7.gene.ui.theme.BlobDeep
import com.tom7.gene.ui.theme.BlobEye
import com.tom7.gene.ui.theme.BlobGlow
import com.tom7.gene.ui.theme.BlobHighlight
import com.tom7.gene.ui.theme.BlobPeach
import com.tom7.gene.ui.theme.BlobSpeck
import com.tom7.gene.ui.theme.BlobThink
import com.tom7.gene.ui.theme.BlobWarm
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class GeneBlobMood {
    Idle,
    Curious,
    Happy,
    Thinking,
    Listening
}

/**
 * Animated clay blob mascot. Cream/peach lighting, glossy eyes, springy morph —
 * the same construction Grok Bot uses (organic ring, gaze, blink, state-as-motion).
 */
@Composable
fun GeneBlob(
    modifier: Modifier = Modifier.size(220.dp),
    mood: GeneBlobMood = GeneBlobMood.Idle,
    interactive: Boolean = false,
    onInteract: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val breathe = rememberInfiniteTransition(label = "blob-breathe")
    val breatheAmt by breathe.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(mood.breatheMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val wobbleTime by breathe.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(mood.wobbleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wobble"
    )

    val gazeX = remember { Animatable(0f) }
    val gazeY = remember { Animatable(0f) }
    val blink = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val press = remember { Animatable(0f) }
    var tracking by remember { mutableStateOf(false) }

    LaunchedEffect(mood, interactive, tracking) {
        if (interactive && tracking) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(mood.gazeHoldMin, mood.gazeHoldMax))
            val tx = Random.nextFloat() * mood.gazeRange * 2f - mood.gazeRange
            val ty = Random.nextFloat() * mood.gazeRange * 1.2f - mood.gazeRange * 0.4f
            launch { gazeX.animateTo(tx, gazeSpring) }
            launch { gazeY.animateTo(ty, gazeSpring) }
            delay(Random.nextLong(520, 1400))
            if (!(interactive && tracking)) {
                launch { gazeX.animateTo(0f, gazeSpring) }
                launch { gazeY.animateTo(0f, gazeSpring) }
            }
        }
    }

    LaunchedEffect(mood) {
        while (true) {
            delay(Random.nextLong(mood.blinkMin, mood.blinkMax))
            blink.animateTo(1f, tween(90, easing = FastOutSlowInEasing))
            delay(40)
            blink.animateTo(0f, tween(190, easing = FastOutSlowInEasing))
            if (mood == GeneBlobMood.Happy && Random.nextFloat() < 0.35f) {
                delay(90)
                blink.animateTo(1f, tween(70))
                delay(30)
                blink.animateTo(0f, tween(160))
            }
        }
    }

    LaunchedEffect(mood) {
        squash.animateTo(
            when (mood) {
                GeneBlobMood.Happy -> 0.08f
                GeneBlobMood.Thinking -> -0.05f
                GeneBlobMood.Listening -> 0.03f
                GeneBlobMood.Curious -> 0.02f
                GeneBlobMood.Idle -> 0f
            },
            spring(stiffness = Spring.StiffnessMediumLow)
        )
    }

    fun applyGaze(position: Offset, width: Float, height: Float) {
        val nx = ((position.x / width) * 2f - 1f).coerceIn(-1f, 1f)
        val ny = ((position.y / height) * 2f - 1f).coerceIn(-1f, 1f)
        scope.launch {
            gazeX.snapTo(nx)
            gazeY.snapTo(ny)
        }
    }

    val pointerMod = if (interactive) {
        Modifier.pointerInput(mood) {
            awaitEachGesture {
                val down = awaitFirstDown()
                tracking = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onInteract()
                scope.launch {
                    press.snapTo(1f)
                    squash.snapTo(0.16f)
                    squash.animateTo(0.02f, spring(dampingRatio = 0.42f, stiffness = 380f))
                }
                applyGaze(down.position, size.width.toFloat(), size.height.toFloat())
                var dragged = false
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (change.pressed) {
                        if (change.positionChange() != Offset.Zero) dragged = true
                        applyGaze(change.position, size.width.toFloat(), size.height.toFloat())
                        change.consume()
                    }
                    if (!change.pressed) break
                } while (true)
                tracking = false
                if (dragged) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    gazeX.animateTo(0f, gazeSpring)
                    gazeY.animateTo(0f, gazeSpring)
                    press.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    } else Modifier

    Box(
        modifier = modifier
            .semantics { contentDescription = "Gene, a cream clay blob" }
            .then(pointerMod)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawGeneBlob(
                time = wobbleTime,
                breathe = breatheAmt,
                gaze = Offset(gazeX.value, gazeY.value),
                blink = blink.value,
                squash = squash.value,
                press = press.value,
                mood = mood
            )
        }
    }
}

@Composable
fun GeneBlobMark(size: Dp, mood: GeneBlobMood = GeneBlobMood.Idle, modifier: Modifier = Modifier) {
    GeneBlob(modifier = modifier.size(size), mood = mood, interactive = false)
}

private val gazeSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 220f)

private val GeneBlobMood.breatheMs: Int
    get() = when (this) {
        GeneBlobMood.Thinking -> 2200
        GeneBlobMood.Happy -> 900
        GeneBlobMood.Listening -> 1400
        else -> 1700
    }

private val GeneBlobMood.wobbleMs: Int
    get() = when (this) {
        GeneBlobMood.Thinking -> 5200
        GeneBlobMood.Happy -> 2800
        else -> 4200
    }

private val GeneBlobMood.gazeRange: Float
    get() = when (this) {
        GeneBlobMood.Curious, GeneBlobMood.Listening -> 0.72f
        GeneBlobMood.Thinking -> 0.28f
        GeneBlobMood.Happy -> 0.4f
        GeneBlobMood.Idle -> 0.5f
    }

private val GeneBlobMood.gazeHoldMin: Long
    get() = if (this == GeneBlobMood.Thinking) 1600 else 1100

private val GeneBlobMood.gazeHoldMax: Long
    get() = if (this == GeneBlobMood.Thinking) 3200 else 2800

private val GeneBlobMood.blinkMin: Long
    get() = if (this == GeneBlobMood.Thinking) 2800 else 1800

private val GeneBlobMood.blinkMax: Long
    get() = if (this == GeneBlobMood.Thinking) 6200 else 4800

private fun DrawScope.drawGeneBlob(
    time: Float,
    breathe: Float,
    gaze: Offset,
    blink: Float,
    squash: Float,
    press: Float,
    mood: GeneBlobMood
) {
    val min = size.minDimension
    val cx = size.width * 0.5f
    val cy = size.height * 0.52f
    val baseR = min * 0.34f
    val breatheScale = 1f + breathe * 0.035f - press * 0.04f
    val rx = baseR * (1.08f + squash * 0.55f) * breatheScale
    val ry = baseR * (1.18f - squash * 0.7f) * breatheScale

    val body = blobPath(cx, cy, rx, ry, time, mood)

    val glowColor = when (mood) {
        GeneBlobMood.Thinking -> BlobThink.copy(alpha = 0.42f)
        GeneBlobMood.Happy -> BlobGlow.copy(alpha = 0.38f)
        GeneBlobMood.Listening -> BlobPeach.copy(alpha = 0.32f)
        else -> BlobGlow.copy(alpha = 0.22f)
    }
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = Offset(cx, cy + ry * 0.95f),
            radius = rx * 1.55f
        ),
        topLeft = Offset(cx - rx * 1.15f, cy + ry * 0.35f),
        size = Size(rx * 2.3f, ry * 0.95f)
    )
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(cx - rx * 0.72f, cy + ry * 0.72f),
        size = Size(rx * 1.44f, ry * 0.28f)
    )

    drawPath(
        path = body,
        brush = Brush.radialGradient(
            colors = listOf(BlobHighlight, BlobCream, BlobPeach, BlobWarm),
            center = Offset(cx - rx * 0.28f, cy - ry * 0.42f),
            radius = rx * 2.15f
        )
    )

    clipPath(body) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BlobWarm.copy(alpha = 0.72f), Color.Transparent),
                center = Offset(cx + rx * 0.28f, cy + ry * 0.48f),
                radius = rx * 0.95f
            ),
            radius = rx * 0.95f,
            center = Offset(cx + rx * 0.28f, cy + ry * 0.48f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BlobHighlight.copy(alpha = 0.9f), Color.Transparent),
                center = Offset(cx - rx * 0.32f, cy - ry * 0.4f),
                radius = rx * 0.55f
            ),
            radius = rx * 0.55f,
            center = Offset(cx - rx * 0.32f, cy - ry * 0.4f)
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to Color.Transparent,
                1f to BlobDeep.copy(alpha = 0.18f)
            )
        )
        if (mood == GeneBlobMood.Thinking) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BlobThink.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = rx * 1.1f
                ),
                radius = rx * 1.1f,
                center = Offset(cx, cy)
            )
        }
        val blushAlpha = if (mood == GeneBlobMood.Happy) 0.55f else 0.32f
        val eyeSpread = rx * 0.34f
        val eyeY = cy - ry * 0.12f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BlobBlush.copy(alpha = blushAlpha), Color.Transparent),
                center = Offset(cx - eyeSpread * 1.35f, eyeY + ry * 0.28f),
                radius = rx * 0.28f
            ),
            radius = rx * 0.28f,
            center = Offset(cx - eyeSpread * 1.35f, eyeY + ry * 0.28f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BlobBlush.copy(alpha = blushAlpha), Color.Transparent),
                center = Offset(cx + eyeSpread * 1.35f, eyeY + ry * 0.28f),
                radius = rx * 0.28f
            ),
            radius = rx * 0.28f,
            center = Offset(cx + eyeSpread * 1.35f, eyeY + ry * 0.28f)
        )

        drawEyes(
            cx = cx,
            eyeY = eyeY,
            spread = eyeSpread,
            radius = rx * 0.168f,
            gaze = gaze,
            blink = blink,
            mood = mood
        )
        drawSmile(cx, eyeY + rx * 0.34f, rx * 0.16f, mood)
    }
}

private fun blobPath(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    time: Float,
    mood: GeneBlobMood
): Path {
    val n = 48
    val amp = when (mood) {
        GeneBlobMood.Happy -> 1.25f
        GeneBlobMood.Thinking -> 0.55f
        GeneBlobMood.Listening -> 1.1f
        else -> 1f
    }
    val pts = ArrayList<Offset>(n)
    for (i in 0 until n) {
        val a = (i.toFloat() / n) * (PI * 2).toFloat() - (PI / 2).toFloat()
        val lump =
            0.07f * sin(a * 2f + 0.4f) +
                0.055f * sin(a * 3f + time * 0.9f) * amp +
                0.04f * cos(a * 5f - time * 1.1f) * amp +
                0.025f * sin(a * 7f + time * 0.6f)
        val egg = 1f + 0.06f * sin(a + 0.2f)
        val x = cx + cos(a) * rx * (egg + lump)
        val y = cy + sin(a) * ry * (egg + lump * 0.85f)
        pts.add(Offset(x, y))
    }
    return Path().also { it.addSmoothClosed(pts) }
}

private fun Path.addSmoothClosed(points: List<Offset>) {
    if (points.size < 3) return
    val n = points.size
    moveTo(points[0].x, points[0].y)
    for (i in 0 until n) {
        val p0 = points[(i - 1 + n) % n]
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val p3 = points[(i + 2) % n]
        cubicTo(
            p1.x + (p2.x - p0.x) / 6f,
            p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f,
            p2.y - (p3.y - p1.y) / 6f,
            p2.x,
            p2.y
        )
    }
    close()
}

private fun DrawScope.drawEyes(
    cx: Float,
    eyeY: Float,
    spread: Float,
    radius: Float,
    gaze: Offset,
    blink: Float,
    mood: GeneBlobMood
) {
    val open = (1f - blink).coerceIn(0.08f, 1f)
    val lookScale = if (mood == GeneBlobMood.Curious) 1.08f else 1f
    val pupilTravel = radius * 0.38f
    listOf(-1f, 1f).forEach { side ->
        val ex = cx + side * spread
        val ey = eyeY
        withTransform({
            translate(ex, ey)
            scale(lookScale, open, Offset.Zero)
        }) {
            drawCircle(color = BlobEye, radius = radius)
            val px = gaze.x * pupilTravel
            val py = gaze.y * pupilTravel * 0.7f
            drawCircle(
                color = Color(0xFF2A2118),
                radius = radius * 0.42f,
                center = Offset(px * 0.35f, py * 0.35f)
            )
            drawCircle(
                color = BlobSpeck,
                radius = radius * 0.22f,
                center = Offset(-radius * 0.28f + px * 0.15f, -radius * 0.32f + py * 0.1f)
            )
            drawCircle(
                color = BlobSpeck.copy(alpha = 0.7f),
                radius = radius * 0.09f,
                center = Offset(radius * 0.22f, -radius * 0.08f)
            )
        }
    }
}

private fun DrawScope.drawSmile(cx: Float, cy: Float, width: Float, mood: GeneBlobMood) {
    val lift = when (mood) {
        GeneBlobMood.Happy -> 1.25f
        GeneBlobMood.Curious -> 0.7f
        GeneBlobMood.Thinking -> 0.15f
        GeneBlobMood.Listening -> 0.55f
        GeneBlobMood.Idle -> 0.45f
    }
    if (lift < 0.2f) return
    val path = Path().apply {
        moveTo(cx - width, cy)
        quadraticTo(cx, cy + width * 0.55f * lift, cx + width, cy)
    }
    drawPath(
        path,
        color = BlobDeep.copy(alpha = 0.55f),
        style = Stroke(width = size.minDimension * 0.012f, cap = StrokeCap.Round)
    )
}

