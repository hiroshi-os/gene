package com.tom7.gene.ui.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tom7.gene.data.ChatMessage
import com.tom7.gene.data.ChatSession
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.Interaction
import com.tom7.gene.data.LocalRelevanceSearch
import com.tom7.gene.data.Person
import com.tom7.gene.data.ROLE_ASSISTANT
import com.tom7.gene.data.ROLE_USER
import com.tom7.gene.data.RemoteInsightEngine
import com.tom7.gene.ui.common.GeneBarAction
import com.tom7.gene.ui.common.GeneGlassDropdownMenu
import com.tom7.gene.ui.common.GeneGlassIconButton
import com.tom7.gene.ui.common.GeneGlassInputBar
import com.tom7.gene.ui.common.geneGlassMenuItemColors
import com.tom7.gene.ui.common.geneHazeSource
import com.tom7.gene.ui.common.rememberGeneHazeState
import com.tom7.gene.ui.navigation.CHAT_ASK
import com.tom7.gene.ui.theme.GeneColors
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.rememberGeneColors
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
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .geneHazeSource(hazeState)
                .statusBarsPadding()
                .padding(top = 56.dp)
                .padding(bottom = 96.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                if (messages.isEmpty()) ChatWelcome(person.name, mode, temporary, colors)
                if (sending) {
                    Text(
                        "Thinking…",
                        color = colors.textTertiary,
                        fontSize = 14.sp,
                        fontFamily = GeneFontFamily,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    memoryMap = memoryMap,
                    expanded = expandedMessageId == message.id,
                    colors = colors,
                    onToggleReferences = {
                        expandedMessageId = if (expandedMessageId == message.id) null else message.id
                    }
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

        // Glass top bar — no opaque scrim (lets Haze blur read through)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = GeneSpace.sm, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GeneGlassIconButton(
                    colors = colors,
                    hazeState = hazeState,
                    onClick = onBack,
                    contentDescription = "Back to person"
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        if (mode == CHAT_ASK) "Ask about ${person.name}" else "Talk with ${person.name}",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = GeneFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (temporary) "Temporary chat" else sessionTitle,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = GeneFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (savedSessionId == null) {
                    GeneGlassIconButton(
                        colors = colors,
                        hazeState = hazeState,
                        onClick = {
                            temporary = !temporary
                            sessionTitle = if (temporary) "Temporary chat" else "New conversation"
                        },
                        contentDescription = if (temporary) "Temporary chat enabled" else "Make temporary"
                    ) {
                        Icon(
                            if (temporary) Icons.Outlined.VisibilityOff else Icons.Outlined.BookmarkBorder,
                            null,
                            tint = if (temporary) colors.accent else colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box {
                        GeneGlassIconButton(
                            colors = colors,
                            hazeState = hazeState,
                            onClick = { menuOpen = true },
                            contentDescription = "Session actions"
                        ) {
                            Icon(Icons.Outlined.MoreHoriz, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                        }
                        GeneGlassDropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            colors = colors,
                            hazeState = hazeState
                        ) {
                            val itemColors = geneGlassMenuItemColors(colors)
                            DropdownMenuItem(
                                text = { Text(if (favorite) "Unfavorite" else "Favorite") },
                                leadingIcon = { Icon(if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) },
                                colors = itemColors,
                                onClick = { favorite = !favorite; db.setSessionFavorite(savedSessionId!!, favorite); menuOpen = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                colors = itemColors,
                                onClick = { menuOpen = false; renameOpen = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                colors = itemColors,
                                onClick = { db.deleteSession(savedSessionId!!); menuOpen = false; onBack() }
                            )
                        }
                    }
                }
            }
        }

        // Composer — liquid glass; no solid scrim so blur reads through message list
        GeneGlassInputBar(
            colors = colors,
            hazeState = hazeState,
            value = draft,
            onValueChange = { draft = it },
            placeholder = "Say something",
            enabled = !sending,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            leading = {
                GeneBarAction(
                    onClick = { startVoice() },
                    enabled = !sending,
                    muted = !listening,
                    colors = colors,
                    contentDescription = if (listening) "Listening" else "Voice input"
                ) {
                    Icon(
                        Icons.Outlined.KeyboardVoice,
                        contentDescription = null,
                        tint = if (listening) colors.accent else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailing = {
                val canSend = draft.isNotBlank() && !sending
                GeneBarAction(
                    onClick = { send() },
                    enabled = canSend,
                    filled = true,
                    colors = colors,
                    contentDescription = "Send"
                ) {
                    Icon(
                        Icons.Outlined.Send,
                        contentDescription = null,
                        tint = if (canSend) colors.onPillActive else colors.textTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        )
    }

    if (renameOpen) {
        ModalBottomSheet(
            onDismissRequest = { renameOpen = false },
            containerColor = colors.surface
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Rename chat", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeneFontFamily)
                SoftPillField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    colors = colors,
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            db.updateSessionTitle(savedSessionId!!, renameText)
                            sessionTitle = renameText.take(60)
                            renameOpen = false
                        }
                    },
                    enabled = renameText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onPillActive),
                    shape = RoundedCornerShape(GeneRadius.sm)
                ) {
                    Text("Save name")
                }
            }
        }
    }
}

@Composable
fun ChatWelcome(name: String, mode: String, temporary: Boolean, colors: GeneColors = rememberGeneColors(MaterialTheme.colorScheme.background.luminance() < 0.5f)) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (mode == CHAT_ASK) "Ask about $name" else "Talk with $name",
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneFontFamily
        )
        Text(
            if (mode == CHAT_ASK) {
                "Ask for a read on patterns, choices, or what may happen next."
            } else if (temporary) {
                "This chat disappears when you leave. Say whatever is on your mind."
            } else {
                "This conversation is saved with the person. Speak naturally and keep the thread."
            },
            color = colors.textSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontFamily = GeneFontFamily
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    memoryMap: Map<Long, Interaction>,
    expanded: Boolean,
    onToggleReferences: () -> Unit,
    colors: GeneColors = rememberGeneColors(MaterialTheme.colorScheme.background.luminance() < 0.5f)
) {
    val isUser = message.role == ROLE_USER
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GeneRadius.md))
                    .background(colors.pill)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    message.body,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontFamily = GeneFontFamily
                )
            }
        } else {
            HorizontalDivider(
                color = colors.divider.copy(alpha = 0.7f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                message.body,
                color = colors.textPrimary,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                fontFamily = GeneFontFamily
            )
            if (message.confidence != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${confidenceLabel(message.confidence)} · ${message.confidence}% · ${if (message.referenceIds.isEmpty()) "No references" else "References"}",
                    color = colors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GeneFontFamily,
                    modifier = Modifier.clickable(onClick = onToggleReferences)
                )
                if (expanded) {
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GeneRadius.sm))
                            .background(colors.pill)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (message.referenceIds.isEmpty()) {
                            Text(
                                "No saved memory was used for this reply.",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontFamily = GeneFontFamily
                            )
                        } else {
                            Text(
                                "References used",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = GeneFontFamily
                            )
                            message.referenceIds.mapNotNull { memoryMap[it] }.forEach { memory ->
                                Text(
                                    "· ${memory.body.take(180)}",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = GeneFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftPillField(
    value: String,
    onValueChange: (String) -> Unit,
    colors: GeneColors,
    singleLine: Boolean = false,
    label: String? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it, color = colors.textSecondary) } },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GeneRadius.sm),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = colors.pill,
            unfocusedContainerColor = colors.pill,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accent,
            focusedLabelColor = colors.textSecondary,
            unfocusedLabelColor = colors.textTertiary
        )
    )
}

fun confidenceLabel(value: Int): String = when {
    value >= 76 -> "High confidence"
    value >= 46 -> "Some confidence"
    else -> "Low confidence"
}
