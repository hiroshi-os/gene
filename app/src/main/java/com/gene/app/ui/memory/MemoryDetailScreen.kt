package com.gene.app.ui.memory

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Interaction
import com.gene.app.data.Person
import com.gene.app.data.TRANSCRIPTION_PENDING
import com.gene.app.data.TRANSCRIPTION_UNAVAILABLE
import com.gene.app.data.TYPE_AUDIO
import com.gene.app.data.TYPE_TEXT
import com.gene.app.ui.common.asDate
import com.gene.app.ui.theme.GeneGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memory: Interaction,
    person: Person,
    db: GeneDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(memory.id) { onDispose { player?.release(); player = null } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(memoryTypeLabel(memory.type), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { db.deleteInteraction(memory.id); onBack() }) {
                        Icon(Icons.Outlined.DeleteOutline, "Delete memory")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Text("${person.name} · ${memory.createdAt.asDate()}", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            if (memory.type == TYPE_AUDIO && memory.audioUri != null) {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(26, 42, 18, 34, 48, 22, 38, 30, 16, 40, 24, 32).forEach { height ->
                                Box(Modifier.width(5.dp).height(height.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                            }
                        }
                        Button(
                            onClick = {
                                if (playing) {
                                    player?.pause()
                                    playing = false
                                } else {
                                    player?.release()
                                    player = MediaPlayer.create(context, Uri.parse(memory.audioUri))
                                    player?.setOnCompletionListener { playing = false }
                                    player?.start()
                                    playing = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (playing) "Pause recording" else "Play recording")
                        }
                    }
                }
            }
            Text(
                if (memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_PENDING) "Transcription is still processing in the background."
                else memory.transcript?.takeIf { it.isNotBlank() } ?: memory.body,
                style = MaterialTheme.typography.titleMedium
            )
            if (memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_UNAVAILABLE) {
                Text(
                    "The audio is saved on this device. Add a compatible transcription provider in Settings if you want a text transcript.",
                    color = GeneGray,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    person: Person,
    db: GeneDatabase,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var secondText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TYPE_TEXT) }
    var pendingSpeech by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra("android.speech.extra.RESULTS")?.firstOrNull()?.let { text = it }
        }
        pendingSpeech = false
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            speechLauncher.launch(Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
                putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form")
                putExtra("android.speech.extra.PROMPT", "Speak a memory")
            })
        } else {
            pendingSpeech = false
        }
    }
    LaunchedEffect(pendingSpeech) {
        if (pendingSpeech) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                speechLauncher.launch(Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
                    putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form")
                    putExtra("android.speech.extra.PROMPT", "Speak a memory")
                })
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    val title = when (mode) {
        TYPE_AUDIO -> "Say it"
        "quote" -> "Save a quote"
        "signal" -> "Notice a signal"
        "pattern" -> "Name a pattern"
        "recommendation" -> "Save a recommendation"
        "favorite" -> "Save a favorite"
        "feeling" -> "Notice a feeling"
        "question" -> "Keep an open question"
        else -> "Capture something"
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text("Choose the shape that fits the moment.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { AssistChip(onClick = { mode = TYPE_TEXT }, label = { Text("Note") }, leadingIcon = { Icon(Icons.Outlined.TextSnippet, null) }) }
                item { AssistChip(onClick = { mode = TYPE_AUDIO; pendingSpeech = true }, label = { Text("Voice") }, leadingIcon = { Icon(Icons.Outlined.Mic, null) }) }
                item { AssistChip(onClick = { mode = "quote" }, label = { Text("Quote") }) }
                item { AssistChip(onClick = { mode = "signal" }, label = { Text("Signal") }) }
                item { AssistChip(onClick = { mode = "pattern" }, label = { Text("Pattern") }) }
                item { AssistChip(onClick = { mode = "recommendation" }, label = { Text("Recommend") }) }
                item { AssistChip(onClick = { mode = "favorite" }, label = { Text("Favorite") }) }
                item { AssistChip(onClick = { mode = "feeling" }, label = { Text("Feeling") }) }
                item { AssistChip(onClick = { mode = "question" }, label = { Text("Open question") }) }
            }
            when (mode) {
                TYPE_AUDIO -> {
                    OutlinedButton(onClick = { pendingSpeech = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Mic, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Speak now")
                    }
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Transcript") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    Text("Only the transcript is saved.", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                }
                "quote" -> OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("What did they say?") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                "signal" -> {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("What happened?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = secondText, onValueChange = { secondText = it }, label = { Text("What might it mean? Optional") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
                "pattern" -> OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("What keeps repeating?") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                "recommendation" -> OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("What did they recommend?") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                "favorite" -> OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("What do they love?") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                "feeling" -> OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("What mood did you notice?") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                "question" -> OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("What are you still wondering?") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                else -> OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("What happened?") }, minLines = 4, modifier = Modifier.fillMaxWidth())
            }
            Button(
                onClick = {
                    if (saveText.isNotBlank()) {
                        db.addInteraction(person.id, mode, saveText)
                        onSaved()
                    }
                },
                enabled = saveText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keep memory")
            }
        }
    }
}
