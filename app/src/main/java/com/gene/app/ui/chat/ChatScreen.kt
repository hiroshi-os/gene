package com.gene.app.ui.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gene.app.data.ChatMessage
import com.gene.app.data.ChatSession
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Interaction
import com.gene.app.data.LocalRelevanceSearch
import com.gene.app.data.Person
import com.gene.app.data.ROLE_ASSISTANT
import com.gene.app.data.ROLE_USER
import com.gene.app.data.RemoteInsightEngine
import com.gene.app.ui.navigation.CHAT_ASK
import com.gene.app.ui.theme.GeneGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    person: Person,
    session: ChatSession?,
    mode: String,
    db: GeneDatabase,
    context: Context,
    onBack: () -> Unit
) {
    var temporary by remember(session?.id) { mutableStateOf(false) }
    var savedSessionId by remember(session?.id) { mutableStateOf(session?.id) }
    var sessionTitle by remember(session?.id) { mutableStateOf(session?.title ?: "New conversation") }
    var messages by remember(session?.id) { mutableStateOf(session?.let { db.messages(it.id) } ?: emptyList()) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var expandedMessageId by remember { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var favorite by remember(session?.id) { mutableStateOf(session?.favorite == true) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(session?.id) { mutableStateOf(session?.title ?: "New conversation") }
    var listening by remember { mutableStateOf(false) }
    var autoResponded by remember(session?.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val memories = remember(person.id) { db.interactions(person.id) }
    val memoryMap = remember(memories) { memories.associateBy { it.id } }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra("android.speech.extra.RESULTS")?.firstOrNull()?.let { transcript ->
                draft = listOf(draft.trim(), transcript).filter { it.isNotBlank() }.joinToString(" ")
            }
        }
        listening = false
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            speechLauncher.launch(
                Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
                    putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form")
                    putExtra("android.speech.extra.PROMPT", "Speak your message")
                }
            )
        } else {
            listening = false
        }
    }
    fun startVoice() {
        listening = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechLauncher.launch(
                Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
                    putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form")
                    putExtra("android.speech.extra.PROMPT", "Speak your message")
                }
            )
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun requestReply(question: String, id: Long?, wasTemporary: Boolean) {
        sending = true
        scope.launch {
            val relevant = LocalRelevanceSearch.relevant(memories, question, limit = 8)
            val currentMessages = messages.filter { it.role == ROLE_USER || it.role == ROLE_ASSISTANT }.dropLast(1).takeLast(10)
            val earlierMessages = if (savedSessionId == null) db.recentMessagesFromOtherSessions(person.id, -1L, limit = 8) else db.recentMessagesFromOtherSessions(person.id, savedSessionId!!, limit = 8)
            val reply = if (mode == CHAT_ASK) RemoteInsightEngine.askAbout(context, person, relevant, currentMessages, earlierMessages, question) else RemoteInsightEngine.personaReply(context, person, relevant, currentMessages, earlierMessages, question)
            if (id != null && !wasTemporary) db.addMessage(id, ROLE_ASSISTANT, reply.text, reply.confidence, reply.referenceIds)
            messages = messages + ChatMessage(-(messages.size + 1).toLong(), id ?: -1L, ROLE_ASSISTANT, reply.text, System.currentTimeMillis(), reply.confidence, reply.referenceIds)
            sending = false
        }
    }

    fun send() {
        val question = draft.trim()
        if (question.isBlank() || sending) return
        draft = ""
        val wasTemporary = temporary
        val id = if (wasTemporary) null else savedSessionId ?: db.addSession(person.id, mode = mode)
        if (id != null) {
            savedSessionId = id
            db.addMessage(id, ROLE_USER, question)
            if (sessionTitle == "New conversation") { db.updateSessionTitle(id, question); sessionTitle = question.take(60) }
        }
        messages = messages + ChatMessage(-(messages.size + 1).toLong(), id ?: -1L, ROLE_USER, question, System.currentTimeMillis(), null, emptyList())
        requestReply(question, id, wasTemporary)
    }

    LaunchedEffect(session?.id, messages.size) {
        if (!autoResponded && !temporary && session != null && messages.size == 1 && messages.first().role == ROLE_USER) {
            autoResponded = true
            requestReply(messages.first().body, session.id, false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(if (mode == CHAT_ASK) "Ask about ${person.name}" else "Talk with ${person.name}", style = MaterialTheme.typography.titleMedium)
                        Text(if (temporary) "Temporary chat" else sessionTitle, color = GeneGray, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back to person") } },
                actions = {
                    if (savedSessionId == null) {
                        IconButton(onClick = {
                            temporary = !temporary
                            sessionTitle = if (temporary) "Temporary chat" else "New conversation"
                        }) {
                            Icon(if (temporary) Icons.Outlined.VisibilityOff else Icons.Outlined.BookmarkBorder, if (temporary) "Temporary chat enabled" else "Make temporary")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Session actions") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(if (favorite) "Unfavorite" else "Favorite") }, leadingIcon = { Icon(if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) }, onClick = { favorite = !favorite; db.setSessionFavorite(savedSessionId!!, favorite); menuOpen = false })
                                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menuOpen = false; renameOpen = true })
                                DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = { db.deleteSession(savedSessionId!!); menuOpen = false; onBack() })
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(Modifier.fillMaxWidth().imePadding(), color = MaterialTheme.colorScheme.background) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { startVoice() }, enabled = !sending, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Outlined.KeyboardVoice, if (listening) "Listening" else "Voice input")
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Say something") },
                        minLines = 1,
                        maxLines = 5,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { send() }, enabled = draft.isNotBlank() && !sending, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Outlined.Send, "Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (messages.isEmpty()) ChatWelcome(person.name, mode, temporary)
                if (sending) Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Start) { Text("Thinking…", color = GeneGray, style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(12.dp))
            }
            items(messages, key = { it.id }) { message ->
                ChatBubble(message, memoryMap, expandedMessageId == message.id) {
                    expandedMessageId = if (expandedMessageId == message.id) null else message.id
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
        }
    }
    if (renameOpen) {
        ModalBottomSheet(onDismissRequest = { renameOpen = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("Rename chat", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (renameText.isNotBlank()) { db.updateSessionTitle(savedSessionId!!, renameText); sessionTitle = renameText.take(60); renameOpen = false } }, enabled = renameText.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save name") }
            }
        }
    }
}

@Composable
fun ChatWelcome(name: String, mode: String, temporary: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (mode == CHAT_ASK) "Ask about $name" else "Talk with $name", style = MaterialTheme.typography.titleLarge)
        Text(if (mode == CHAT_ASK) "Ask for a read on patterns, choices, or what may happen next." else if (temporary) "This chat disappears when you leave. Say whatever is on your mind." else "This conversation is saved with the person. Speak naturally and keep the thread.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    memoryMap: Map<Long, Interaction>,
    expanded: Boolean,
    onToggleReferences: () -> Unit
) {
    val isUser = message.role == ROLE_USER
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(18.dp),
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(Modifier.padding(horizontal = 17.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message.body, style = MaterialTheme.typography.bodyLarge)
                if (!isUser && message.confidence != null) {
                    Text(
                        "${confidenceLabel(message.confidence)} · ${message.confidence}% · ${if (message.referenceIds.isEmpty()) "No references" else "References"}",
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else GeneGray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(onClick = onToggleReferences)
                    )
                    if (expanded) {
                        Divider()
                        if (message.referenceIds.isEmpty()) {
                            Text("No saved memory was used for this reply.", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("References used", style = MaterialTheme.typography.bodyMedium)
                            message.referenceIds.mapNotNull { memoryMap[it] }.forEach { memory ->
                                Text("· ${memory.body.take(180)}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun confidenceLabel(value: Int): String = when {
    value >= 76 -> "High confidence"
    value >= 46 -> "Some confidence"
    else -> "Low confidence"
}
