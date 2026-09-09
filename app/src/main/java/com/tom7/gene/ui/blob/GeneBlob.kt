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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.tom7.gene.ui.theme.BlobCream
import com.tom7.gene.ui.theme.BlobEye
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
 * Flat Grok-style blob: one filled body, two solid black eyes.
 * Motion (morph, blink, gaze) carries state — no lighting, blush, or highlights.
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
            .semantics { contentDescription = "Gene" }
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
    val breatheScale = 1f + breathe * 0.03f - press * 0.04f
    val rx = baseR * (1.08f + squash * 0.45f) * breatheScale
    val ry = baseR * (1.16f - squash * 0.6f) * breatheScale
    val body = blobPath(cx, cy, rx, ry, time, mood)

    drawPath(path = body, color = BlobCream)
    drawEyes(
        cx = cx,
        eyeY = cy - ry * 0.10f,
        spread = rx * 0.32f,
        radiusX = rx * 0.155f,
        radiusY = ry * 0.175f,
        gaze = gaze,
        blink = blink,
        mood = mood
    )
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
    radiusX: Float,
    radiusY: Float,
    gaze: Offset,
    blink: Float,
    mood: GeneBlobMood
) {
    val open = (1f - blink).coerceIn(0.08f, 1f)
    val look = if (mood == GeneBlobMood.Curious) 1.06f else 1f
    val dx = gaze.x * radiusX * 0.55f
    val dy = gaze.y * radiusY * 0.4f
    listOf(-1f, 1f).forEach { side ->
        val ex = cx + side * spread + dx
        val ey = eyeY + dy
        withTransform({
            translate(ex, ey)
            scale(look, open * look, Offset.Zero)
        }) {
            drawOval(
                color = BlobEye,
                topLeft = Offset(-radiusX, -radiusY),
                size = Size(radiusX * 2f, radiusY * 2f)
            )
        }
    }
}

