package com.gene.app.ui.person

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gene.app.data.AudioRecorder
import com.gene.app.data.AudioTranscriptionWorker
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Person
import com.gene.app.data.TYPE_TEXT
import com.gene.app.ui.common.GeneBarAction
import com.gene.app.ui.common.GlassThickness
import com.gene.app.ui.common.geneFrostedSurface
import com.gene.app.ui.common.geneGlass
import com.gene.app.ui.theme.GeneFontFamily
import com.gene.app.ui.theme.NotionDarkBg
import com.gene.app.ui.theme.rememberGeneColors
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random

private val ComposerShape = RoundedCornerShape(22.dp)
private val TypeChipShape = RoundedCornerShape(50)
private val SignalFieldShape = RoundedCornerShape(14.dp)

private data class ComposerMode(val value: String, val label: String, val icon: ImageVector)

@Composable
fun MemoryComposer(
    person: Person,
    db: GeneDatabase,
    onSaved: () -> Unit,
    hazeState: HazeState? = null,
    expandTypes: Boolean = false
) {
    val dark = MaterialTheme.colorScheme.background == NotionDarkBg
    val colors = rememberGeneColors(dark)
    var text by remember { mutableStateOf("") }
    var secondText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TYPE_TEXT) }
    var showModes by remember { mutableStateOf(expandTypes) }
    var recording by remember { mutableStateOf(false) }
    var pendingAudio by remember { mutableStateOf<Uri?>(null) }
    var liveAmplitude by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context.applicationContext) }
    val focusRequester = remember { FocusRequester() }

    val modes = remember {
        listOf(
            ComposerMode(TYPE_TEXT, "Note", Icons.Outlined.TextSnippet),
            ComposerMode("quote", "Quote", Icons.Outlined.BookmarkBorder),
            ComposerMode("signal", "Signal", Icons.Outlined.Lightbulb),
            ComposerMode("pattern", "Pattern", Icons.Outlined.History),
            ComposerMode("recommendation", "Recommend", Icons.Outlined.Lightbulb),
            ComposerMode("favorite", "Favorite", Icons.Outlined.FavoriteBorder),
            ComposerMode("feeling", "Feeling", Icons.Outlined.WbSunny),
            ComposerMode("question", "Open question", Icons.Outlined.HelpOutline)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingAudio = null
            recording = recorder.start()
        } else {
            recording = false
        }
    }

    DisposableEffect(Unit) { onDispose { recorder.release() } }

    LaunchedEffect(recording) {
        if (!recording) {
            liveAmplitude = 0f
            return@LaunchedEffect
        }
        while (recording) {
            liveAmplitude = recorder.amplitude()
            delay(48)
        }
    }

    fun saveVoice(uri: Uri) {
        val id = db.addAudioInteraction(person.id, uri.toString())
        AudioTranscriptionWorker.enqueue(context, id, uri)
        pendingAudio = null
        recording = false
        showModes = false
        onSaved()
    }

    fun toggleRecording() {
        if (recording) {
            val uri = recorder.stop()
            recording = false
            pendingAudio = uri
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pendingAudio = null
            recording = recorder.start()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val saveText = when (mode) {
        "quote" -> if (text.isBlank()) "" else "Quote · \"${text.trim()}\""
        "signal" -> if (text.isBlank()) "" else "Signal · ${text.trim()}${if (secondText.isBlank()) "" else " | Possible meaning · ${secondText.trim()}"}"
        "pattern" -> if (text.isBlank()) "" else "Pattern · ${text.trim()}"
        "recommendation" -> if (text.isBlank()) "" else "Recommendation · ${text.trim()}"
        "favorite" -> if (text.isBlank()) "" else "Favorite · ${text.trim()}"
        "feeling" -> if (text.isBlank()) "" else "Feeling · ${text.trim()}"
        "question" -> if (text.isBlank()) "" else "Open question · ${text.trim()}"
        else -> text.trim()
    }
    val canAdd = (saveText.isNotBlank() || pendingAudio != null) && !recording

    LaunchedEffect(expandTypes) { if (expandTypes) showModes = true }

    fun addMemory() {
        if (!canAdd) return
        val audio = pendingAudio
        if (audio != null) {
            saveVoice(audio)
            return
        }
        db.addInteraction(person.id, mode, saveText)
        text = ""
        secondText = ""
        showModes = false
        onSaved()
    }

    val placeholder = when {
        pendingAudio != null -> "Voice ready — tap + to add"
        mode == "quote" -> "What did they say?"
        mode == "signal" -> "What happened?"
        mode == "pattern" -> "What keeps repeating?"
        mode == "recommendation" -> "What did they recommend?"
        mode == "favorite" -> "What do they love?"
        mode == "feeling" -> "What mood did you notice?"
        mode == "question" -> "What are you still wondering?"
        else -> "Capture a memory"
    }

    val glassMod = if (hazeState != null) {
        Modifier.geneGlass(hazeState, colors, GlassThickness.Regular, ComposerShape)
    } else {
        Modifier.geneFrostedSurface(colors, ComposerShape, elevation = 10.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(glassMod)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showModes) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(modes, key = { it.value }) { item ->
                    val selected = mode == item.value
                    val chipInteraction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .clip(TypeChipShape)
                            .background(if (selected) colors.pillActive else colors.pill)
                            .border(
                                width = 0.5.dp,
                                color = if (selected) Color.Transparent else colors.border.copy(alpha = 0.35f),
                                shape = TypeChipShape
                            )
                            .clickable(interactionSource = chipInteraction, indication = null) { mode = item.value }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = if (selected) colors.onPillActive else colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.label,
                            color = if (selected) colors.onPillActive else colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = GeneFontFamily
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GeneBarAction(
                onClick = {
                    showModes = !showModes
                    if (showModes) {
                        try {
                            focusRequester.requestFocus()
                        } catch (_: Exception) {
                        }
                    }
                },
                filled = showModes,
                muted = !showModes,
                colors = colors,
                contentDescription = "Capture type"
            ) {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = if (showModes) colors.onPillActive else colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                when {
                    recording -> {
                        LiveMicWaveform(
                            amplitude = liveAmplitude,
                            color = colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        )
                    }
                    pendingAudio != null -> {
                        PendingVoiceWave(
                            color = colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        )
                    }
                    else -> {
                        if (text.isEmpty()) {
                            Text(
                                placeholder,
                                color = colors.textTertiary,
                                fontSize = 15.sp,
                                fontFamily = GeneFontFamily
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                if (it.isNotEmpty()) showModes = true
                            },
                            enabled = true,
                            singleLine = mode != "signal" && mode != "pattern",
                            maxLines = 4,
                            cursorBrush = SolidColor(colors.accent),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontFamily = GeneFontFamily,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused && text.isNotEmpty()) showModes = true }
                        )
                    }
                }
            }

            GeneBarAction(
                onClick = { toggleRecording() },
                muted = !recording && pendingAudio == null,
                danger = recording,
                filled = pendingAudio != null && !recording,
                colors = colors,
                contentDescription = when {
                    recording -> "Stop recording"
                    pendingAudio != null -> "Record again"
                    else -> "Record voice memory"
                }
            ) {
                Icon(
                    if (recording) Icons.Outlined.Stop else Icons.Outlined.KeyboardVoice,
                    contentDescription = null,
                    tint = when {
                        recording -> colors.danger
                        pendingAudio != null -> colors.onPillActive
                        else -> colors.textPrimary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            GeneBarAction(
                onClick = { addMemory() },
                enabled = canAdd,
                filled = true,
                colors = colors,
                contentDescription = "Add memory"
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = if (canAdd) colors.onPillActive else colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (mode == "signal" && showModes && !recording && pendingAudio == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SignalFieldShape)
                    .background(colors.pill)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (secondText.isEmpty()) {
                    Text(
                        "What might it mean? Optional",
                        color = colors.textTertiary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily
                    )
                }
                BasicTextField(
                    value = secondText,
                    onValueChange = { secondText = it },
                    maxLines = 3,
                    cursorBrush = SolidColor(colors.accent),
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontFamily = GeneFontFamily
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LiveMicWaveform(
    amplitude: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val bars = 28
    val levels = remember { FloatArray(bars) { 0.12f } }
    LaunchedEffect(amplitude) {
        val target = max(0.08f, amplitude)
        // Shift left and append live sample with light jitter for organic motion
        for (i in 0 until bars - 1) levels[i] = levels[i + 1]
        levels[bars - 1] = (target * (0.75f + Random.nextFloat() * 0.5f)).coerceIn(0.08f, 1f)
    }
    Canvas(modifier = modifier) {
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(2f)
        val midY = size.height / 2f
        for (i in 0 until bars) {
            val h = size.height * (0.18f + levels[i] * 0.82f)
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = color.copy(alpha = 0.55f + levels[i] * 0.45f),
                topLeft = Offset(x, midY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun PendingVoiceWave(
    color: Color,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "pendingVoice")
    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier = modifier) {
        val bars = 28
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(2f)
        val midY = size.height / 2f
        for (i in 0 until bars) {
            val wave = kotlin.math.sin((i / bars.toFloat() * Math.PI * 2 + phase * Math.PI * 2).toFloat())
            val level = 0.22f + (wave * 0.5f + 0.5f) * 0.45f
            val h = size.height * level
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = color.copy(alpha = 0.45f + level * 0.35f),
                topLeft = Offset(x, midY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
