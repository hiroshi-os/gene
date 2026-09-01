package com.gene.app.ui.person

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gene.app.data.AudioRecorder
import com.gene.app.data.AudioTranscriptionWorker
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Person
import com.gene.app.data.TYPE_AUDIO
import com.gene.app.data.TYPE_TEXT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryComposer(
    person: Person,
    db: GeneDatabase,
    onSaved: () -> Unit,
    onStartChat: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var secondText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TYPE_TEXT) }
    var showModes by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val recorder = remember { AudioRecorder(context.applicationContext) }
    val modes = listOf(
        TYPE_TEXT to "Note",
        TYPE_AUDIO to "Voice",
        "quote" to "Quote",
        "signal" to "Signal",
        "pattern" to "Pattern",
        "recommendation" to "Recommend",
        "favorite" to "Favorite",
        "feeling" to "Feeling",
        "question" to "Open question"
    )
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recording = recorder.start() else recording = false
    }
    DisposableEffect(Unit) { onDispose { recorder.release() } }
    fun toggleRecording() {
        if (recording) {
            audioUri = recorder.stop()
            recording = false
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
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
        TYPE_AUDIO -> audioUri?.toString().orEmpty()
        else -> text.trim()
    }
    val canSend = saveText.isNotBlank() && !recording
    LaunchedEffect(imeVisible) { if (!imeVisible) showModes = false }
    fun saveMemory() {
        if (!canSend) return
        if (mode == TYPE_AUDIO) {
            val id = db.addAudioInteraction(person.id, saveText)
            AudioTranscriptionWorker.enqueue(context, id, Uri.parse(saveText))
        } else {
            db.addInteraction(person.id, mode, saveText)
        }
        text = ""
        secondText = ""
        audioUri = null
        showModes = false
        onSaved()
    }
    val placeholder = when (mode) {
        TYPE_AUDIO -> if (recording) "Recording… tap stop when you are done" else if (audioUri != null) "Recording ready to save" else "Tap the mic to record"
        "quote" -> "What did they say?"
        "signal" -> "What happened?"
        "pattern" -> "What keeps repeating?"
        "recommendation" -> "What did they recommend?"
        "favorite" -> "What do they love?"
        "feeling" -> "What mood did you notice?"
        "question" -> "What are you still wondering?"
        else -> "Capture a memory"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showModes) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(modes, key = { it.first }) { (value, label) ->
                    AssistChip(
                        onClick = { mode = value },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                when (value) {
                                    TYPE_AUDIO -> Icons.Outlined.Mic
                                    "quote" -> Icons.Outlined.BookmarkBorder
                                    "signal" -> Icons.Outlined.Lightbulb
                                    "pattern" -> Icons.Outlined.History
                                    "recommendation" -> Icons.Outlined.Lightbulb
                                    "favorite" -> Icons.Outlined.FavoriteBorder
                                    "feeling" -> Icons.Outlined.WbSunny
                                    "question" -> Icons.Outlined.HelpOutline
                                    else -> Icons.Outlined.TextSnippet
                                },
                                null
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (mode == value) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it; showModes = true },
                        placeholder = { Text(placeholder) },
                        singleLine = mode != "signal" && mode != "pattern",
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { showModes = it.isFocused },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    if (mode == TYPE_AUDIO) {
                        IconButton(onClick = { toggleRecording() }) {
                            Icon(
                                if (recording) Icons.Outlined.Stop else Icons.Outlined.KeyboardVoice,
                                if (recording) "Stop recording" else "Record memory"
                            )
                        }
                    }
                }
            }
            Surface(
                onClick = { if (canSend) saveMemory() else onStartChat() },
                enabled = canSend || !recording,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (canSend) Icons.Outlined.Send else Icons.Outlined.SmartToy,
                        if (canSend) "Save memory" else "Start a new chat"
                    )
                }
            }
        }
        if (mode == "signal" && showModes) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                OutlinedTextField(
                    value = secondText,
                    onValueChange = { secondText = it },
                    label = { Text("What might it mean? Optional") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}
