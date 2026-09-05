package com.gene.app.ui.chat

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gene.app.data.ChatMessage
import com.gene.app.data.ChatSession
import com.gene.app.data.GeneDatabase
import com.gene.app.data.LocalRelevanceSearch
import com.gene.app.data.MentionParser
import com.gene.app.data.Person
import com.gene.app.data.ROLE_ASSISTANT
import com.gene.app.data.ROLE_USER
import com.gene.app.data.RemoteInsightEngine
import com.gene.app.ui.common.GeneBarAction
import com.gene.app.ui.common.GeneGlassDropdownMenu
import com.gene.app.ui.common.GeneGlassIconButton
import com.gene.app.ui.common.GeneGlassInputBar
import com.gene.app.ui.common.geneGlassMenuItemColors
import com.gene.app.ui.common.geneHazeSource
import com.gene.app.ui.common.rememberGeneHazeState
import com.gene.app.ui.theme.GeneColors
import com.gene.app.ui.theme.GeneFontFamily
import com.gene.app.ui.theme.GeneRadius
import com.gene.app.ui.theme.GeneSpace
import com.gene.app.ui.theme.rememberGeneColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    session: ChatSession,
    db: GeneDatabase,
    context: Context,
    onBack: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var members by remember(session.id) { mutableStateOf(db.sessionMembers(session.id)) }
    var messages by remember(session.id) { mutableStateOf(db.messages(session.id)) }
    var draft by remember { mutableStateOf(TextFieldValue("")) }
    var sending by remember { mutableStateOf(false) }
    var sessionTitle by remember(session.id) { mutableStateOf(session.title) }
    var favorite by remember(session.id) { mutableStateOf(session.favorite) }
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(session.id) { mutableStateOf(session.title) }
    var replyingTo by remember(session.id) { mutableStateOf<ChatMessage?>(null) }

    val memberMap = remember(members) { members.associateBy { it.id } }
    val messageMap = remember(messages) { messages.associateBy { it.id } }
    val chipColor = colors.accent
    val chipBg = colors.accent.copy(alpha = 0.16f)
    val mentionQuery = remember(draft.text) { MentionParser.activeMentionQuery(draft.text) }
    val mentionSuggestions = remember(draft.text, members, mentionQuery) {
        if (mentionQuery == null) emptyList()
        else MentionParser.suggestions(mentionQuery, members)
    }

    fun refresh() {
        members = db.sessionMembers(session.id)
        messages = db.messages(session.id)
    }

    fun updateDraft(incoming: TextFieldValue) {
        draft = MentionField.reconcile(draft, incoming, members, chipColor, chipBg)
    }

    fun send() {
        val text = draft.text.trim()
        if (text.isBlank() || sending) return
        val replyTarget = replyingTo
        draft = TextFieldValue("")
        replyingTo = null
        db.addMessage(
            sessionId = session.id,
            role = ROLE_USER,
            body = text,
            replyToMessageId = replyTarget?.id
        )
        if (sessionTitle == "Group chat" || sessionTitle == "New conversation") {
            db.updateSessionTitle(session.id, text)
            sessionTitle = text.take(60)
        }
        refresh()

        // Reply-to a persona counts as pinging them even without an explicit @.
        val pinged = buildList {
            addAll(MentionParser.mentionedPeople(text, members))
            replyTarget?.speakerPersonId?.let { sid ->
                memberMap[sid]?.let { if (none { p -> p.id == it.id }) add(it) }
            }
        }
        if (pinged.isEmpty()) return

        sending = true
        scope.launch {
            val repliedIds = linkedSetOf<Long>()
            val queue = ArrayDeque(pinged)
            val userQuestion = buildString {
                if (replyTarget != null) {
                    val who = when {
                        replyTarget.role == ROLE_USER -> "User"
                        replyTarget.speakerPersonId != null ->
                            memberMap[replyTarget.speakerPersonId]?.name ?: "Member"
                        else -> "Assistant"
                    }
                    append("Replying to $who: \"${replyTarget.body.take(280)}\"\n")
                }
                append(text)
            }
            while (queue.isNotEmpty()) {
                val person = queue.removeFirst()
                if (!repliedIds.add(person.id)) continue

                val thread = db.messages(session.id)
                val memories = db.interactions(person.id)
                val relevant = LocalRelevanceSearch.relevant(memories, userQuestion, limit = 8)
                val conversation = thread.map { msg ->
                    val label = when {
                        msg.role == ROLE_USER -> "User"
                        msg.speakerPersonId != null -> memberMap[msg.speakerPersonId]?.name ?: "Member"
                        else -> "Assistant"
                    }
                    msg.copy(body = "$label: ${msg.body}")
                }
                val peers = members.map { it.name }
                val reply = RemoteInsightEngine.personaReply(
                    context = context,
                    person = person,
                    memories = relevant,
                    sessionMessages = conversation.takeLast(12),
                    earlierMessages = emptyList(),
                    question = userQuestion,
                    peerNames = peers
                )
                db.addMessage(
                    sessionId = session.id,
                    role = ROLE_ASSISTANT,
                    body = reply.text,
                    confidence = reply.confidence,
                    referenceIds = reply.referenceIds,
                    speakerPersonId = person.id
                )
                refresh()

                MentionParser.mentionedPeople(reply.text, members)
                    .filter { it.id !in repliedIds }
                    .forEach { queue.addLast(it) }
            }
            sending = false
        }
    }

    fun replyTo(message: ChatMessage) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        replyingTo = message
        // Soft-ping the speaker with an @chip; quote itself lives in the reply bar, not the draft.
        message.speakerPersonId?.let { sid ->
            memberMap[sid]?.let { person ->
                draft = MentionField.insertMention(draft, person, members, chipColor, chipBg)
            }
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
                .padding(bottom = 120.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Group chat",
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily
                )
                Text(
                    members.joinToString(" · ") { it.name }.ifBlank { "No members" },
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = GeneFontFamily
                )
                Text(
                    "People only reply when @mentioned. They still see the full thread.",
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    fontFamily = GeneFontFamily,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )
            }
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Say something and @someone to get a reply.",
                        color = colors.textTertiary,
                        fontSize = 14.sp,
                        fontFamily = GeneFontFamily,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
            items(messages, key = { it.id }) { message ->
                val quoted = message.replyToMessageId?.let { messageMap[it] }
                GroupBubble(
                    message = message,
                    speakerName = message.speakerPersonId?.let { memberMap[it]?.name },
                    quotedMessage = quoted,
                    quotedSpeakerName = when {
                        quoted == null -> null
                        quoted.role == ROLE_USER -> "You"
                        quoted.speakerPersonId != null -> memberMap[quoted.speakerPersonId]?.name
                        else -> "Someone"
                    },
                    members = members,
                    colors = colors,
                    onLongPress = { replyTo(message) }
                )
            }
            if (sending) {
                item {
                    Text(
                        "Waiting for replies…",
                        color = colors.textTertiary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

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
                    contentDescription = "Back"
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        sessionTitle,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = GeneFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${members.size} people",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = GeneFontFamily
                    )
                }
                Box {
                    GeneGlassIconButton(
                        colors = colors,
                        hazeState = hazeState,
                        onClick = { menuOpen = true },
                        contentDescription = "Group actions"
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
                            onClick = {
                                favorite = !favorite
                                db.setSessionFavorite(session.id, favorite)
                                menuOpen = false
                            }
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
                            onClick = {
                                db.deleteSession(session.id)
                                menuOpen = false
                                onBack()
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            replyingTo?.let { target ->
                ReplyComposerBar(
                    colors = colors,
                    author = when {
                        target.role == ROLE_USER -> "You"
                        target.speakerPersonId != null ->
                            memberMap[target.speakerPersonId]?.name ?: "Member"
                        else -> "Message"
                    },
                    preview = target.body,
                    onDismiss = { replyingTo = null }
                )
            }
            if (mentionSuggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GeneRadius.md))
                        .background(colors.surface)
                        .border(0.5.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(GeneRadius.md))
                        .padding(vertical = 4.dp)
                ) {
                    mentionSuggestions.forEach { person ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    draft = MentionField.insertMention(
                                        draft, person, members, chipColor, chipBg
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(colors.pastel(person.id.toInt())),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    person.name.take(1).uppercase(),
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = GeneFontFamily
                                )
                            }
                            Text(
                                "@${person.name}",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontFamily = GeneFontFamily
                            )
                        }
                    }
                }
            }
            GeneGlassInputBar(
                colors = colors,
                hazeState = hazeState,
                value = draft,
                onValueChange = { updateDraft(it) },
                placeholder = "use @mention to ping",
                enabled = !sending,
                mentionChipBackground = chipBg,
                leading = {
                    Icon(
                        Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(18.dp)
                    )
                },
                trailing = {
                    val canSend = draft.text.isNotBlank() && !sending
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
                Text(
                    "Rename group",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily
                )
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GeneRadius.sm),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.border,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accent,
                        focusedContainerColor = colors.pill,
                        unfocusedContainerColor = colors.pill
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(GeneRadius.sm))
                        .background(if (renameText.isNotBlank()) colors.pillActive else colors.pill)
                        .clickable(enabled = renameText.isNotBlank()) {
                            db.updateSessionTitle(session.id, renameText)
                            sessionTitle = renameText.take(60)
                            renameOpen = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Save name",
                        color = if (renameText.isNotBlank()) colors.onPillActive else colors.textTertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = GeneFontFamily
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupBubble(
    message: ChatMessage,
    speakerName: String?,
    quotedMessage: ChatMessage?,
    quotedSpeakerName: String?,
    members: List<Person>,
    colors: GeneColors,
    onLongPress: () -> Unit
) {
    val isUser = message.role == ROLE_USER
    val chipBg = colors.accent.copy(alpha = 0.14f)
    val annotated = remember(message.body, members, colors.accent) {
        MentionField.annotate(
            text = message.body,
            members = members,
            chipColor = colors.accent,
            chipBackground = chipBg
        )
    }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser && !speakerName.isNullOrBlank()) {
            Text(
                speakerName,
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneFontFamily,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(GeneRadius.md))
                .background(if (isUser) colors.pill else colors.surface)
                .border(
                    width = if (isUser) 0.dp else 0.5.dp,
                    color = colors.border.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(GeneRadius.md)
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (quotedMessage != null) {
                    InlineReplyQuote(
                        colors = colors,
                        author = quotedSpeakerName ?: "Message",
                        preview = quotedMessage.body,
                        onUserSide = isUser
                    )
                }
                Text(
                    text = annotated,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontFamily = GeneFontFamily,
                    onTextLayout = { textLayout = it },
                    modifier = Modifier.drawMentionChips(
                        annotated = annotated,
                        layoutResult = textLayout,
                        background = chipBg
                    )
                )
            }
        }
    }
}

@Composable
private fun ReplyComposerBar(
    colors: GeneColors,
    author: String,
    preview: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GeneRadius.md))
            .background(colors.surface)
            .border(0.5.dp, colors.border.copy(alpha = 0.55f), RoundedCornerShape(GeneRadius.md))
            .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.accent)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Outlined.Reply,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Replying to $author",
                    color = colors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                preview.trim().replace('\n', ' '),
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = GeneFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        GeneBarAction(
            onClick = onDismiss,
            muted = true,
            colors = colors,
            contentDescription = "Cancel reply",
            size = 36.dp
        ) {
            Icon(Icons.Outlined.Close, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun InlineReplyQuote(
    colors: GeneColors,
    author: String,
    preview: String,
    onUserSide: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (onUserSide) colors.bg.copy(alpha = 0.55f)
                else colors.pill
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.accent)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                author,
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                preview.trim().replace('\n', ' '),
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = GeneFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
