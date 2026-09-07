package com.tom7.gene.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.Person
import com.tom7.gene.data.SessionWithPerson
import com.tom7.gene.ui.common.GeneBarAction
import com.tom7.gene.ui.common.GeneGlassInputBar
import com.tom7.gene.ui.common.MemojiAvatar
import com.tom7.gene.ui.common.MemojiConfig
import com.tom7.gene.ui.common.MemojiMakerSheet
import com.tom7.gene.ui.common.NotionAcrylicDock
import com.tom7.gene.ui.common.NotionListRow
import com.tom7.gene.ui.common.NotionRecentCard
import com.tom7.gene.ui.common.NotionSectionHeader
import com.tom7.gene.ui.common.NotionTopChrome
import com.tom7.gene.ui.common.asRelativeTime
import com.tom7.gene.ui.common.geneHazeSource
import com.tom7.gene.ui.common.rememberGeneHazeState
import com.tom7.gene.ui.theme.GeneColors
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.rememberGeneColors
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    people: List<Person>,
    dark: Boolean,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onPerson: (Long) -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit = {},
    onSelf: () -> Unit,
    onPersonCreated: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onOpenChat: (personId: Long, sessionId: Long?) -> Unit = { _, _ -> },
    onOpenGroupChat: (sessionId: Long) -> Unit = {},
    onStartGroupChat: (memberIds: List<Long>) -> Unit = {},
    onOpenGraph: () -> Unit = {},
    onCapturePerson: (personId: Long) -> Unit = {},
    onAskPerson: (personId: Long) -> Unit = {},
    db: GeneDatabase? = null
) {
    var showNewPerson by remember { mutableStateOf(false) }
    var showCapturePicker by remember { mutableStateOf(false) }
    var showChatPicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var favoritesExpanded by remember { mutableStateOf(true) }
    var allExpanded by remember { mutableStateOf(true) }

    var personForMemoji by remember { mutableStateOf<Person?>(null) }
    var selectedPersonForAction by remember { mutableStateOf<Person?>(null) }
    var editingPerson by remember { mutableStateOf<Person?>(null) }
    var personToDelete by remember { mutableStateOf<Person?>(null) }

    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    val self = remember(db) { db?.getOrCreateSelf() }
    val captureTargets = remember(people, self) {
        listOfNotNull(self) + people
    }
    val allSessions = remember(people, selectedTab) { db?.allSessions().orEmpty() }
    val favorites = remember(people) { people.filter { it.favorite } }
    val recentPeople = remember(people) { people.sortedByDescending { it.lastSeen }.take(8) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Box(Modifier.fillMaxSize().geneHazeSource(hazeState)) {
            if (selectedTab == 0) {
                PeopleHomeContent(
                    colors = colors,
                    recent = recentPeople,
                    favorites = favorites,
                    people = people,
                    favoritesExpanded = favoritesExpanded,
                    allExpanded = allExpanded,
                    onToggleFavorites = { favoritesExpanded = !favoritesExpanded },
                    onToggleAll = { allExpanded = !allExpanded },
                    searchQuery = "",
                    onPerson = onPerson,
                    onCapture = onCapturePerson,
                    onChat = { onOpenChat(it, null) },
                    onLongPress = { selectedPersonForAction = it },
                    onOpenGraph = onOpenGraph,
                    onOpenSelf = onSelf
                )
            } else {
                ChatsHomeContent(
                    colors = colors,
                    sessions = allSessions,
                    searchQuery = "",
                    onOpenChat = { pid, sid -> onOpenChat(pid, sid) },
                    onOpenGroupChat = onOpenGroupChat,
                    onStartChat = {
                        when {
                            captureTargets.isEmpty() -> showNewPerson = true
                            captureTargets.size == 1 -> onOpenChat(captureTargets.first().id, null)
                            else -> showChatPicker = true
                        }
                    },
                    onStartGroupChat = {
                        if (people.size < 2) showNewPerson = true
                        else showGroupPicker = true
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 4.dp)
        ) {
            NotionTopChrome(
                colors = colors,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onSettings = onSettings,
                onNewPerson = { showNewPerson = true },
                onSelf = onSelf,
                selfName = self?.name ?: "You",
                hazeState = hazeState
            )
        }

        NotionAcrylicDock(
            colors = colors,
            hazeState = hazeState,
            onSearch = onSearch,
            onCapture = {
                when {
                    captureTargets.isEmpty() -> showNewPerson = true
                    captureTargets.size == 1 -> onCapturePerson(captureTargets.first().id)
                    else -> showCapturePicker = true
                }
            },
            onChat = {
                when {
                    captureTargets.isEmpty() -> showNewPerson = true
                    captureTargets.size == 1 -> onOpenChat(captureTargets.first().id, null)
                    else -> showChatPicker = true
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showNewPerson) {
        NewPersonSheet(colors, onDismiss = { showNewPerson = false }) { name, note ->
            showNewPerson = false
            onPersonCreated(name, note)
        }
    }
    if (showCapturePicker) {
        PersonPickerSheet(
            title = "Capture about…",
            colors = colors,
            people = captureTargets,
            onDismiss = { showCapturePicker = false },
            onPick = { showCapturePicker = false; onCapturePerson(it) }
        )
    }
    if (showChatPicker) {
        PersonPickerSheet(
            title = "Chat about…",
            colors = colors,
            people = captureTargets,
            onDismiss = { showChatPicker = false },
            onPick = { showChatPicker = false; onOpenChat(it, null) }
        )
    }
    if (showGroupPicker) {
        GroupMemberPickerSheet(
            colors = colors,
            people = people,
            onDismiss = { showGroupPicker = false },
            onCreate = { ids ->
                showGroupPicker = false
                onStartGroupChat(ids)
            }
        )
    }

    personForMemoji?.let { person ->
        MemojiMakerSheet(
            initialConfig = MemojiConfig.deserialize(person.avatar),
            isDark = dark,
            onDismiss = { personForMemoji = null },
            onSave = { cfg ->
                db?.updatePersonAvatar(person.id, cfg.serialize())
                onRefresh()
                personForMemoji = null
            }
        )
    }

    selectedPersonForAction?.let { person ->
        PersonActionSheet(
            person = person,
            colors = colors,
            onDismiss = { selectedPersonForAction = null },
            onToggleFavorite = {
                db?.setPersonFavorite(person.id, !person.favorite)
                onRefresh()
                selectedPersonForAction = null
            },
            onEdit = { editingPerson = person; selectedPersonForAction = null },
            onCustomizeMemoji = { personForMemoji = person; selectedPersonForAction = null },
            onCapture = { onCapturePerson(person.id); selectedPersonForAction = null },
            onChat = { onOpenChat(person.id, null); selectedPersonForAction = null },
            onDelete = { personToDelete = person; selectedPersonForAction = null }
        )
    }

    editingPerson?.let { person ->
        EditPersonDialog(person, colors, onDismiss = { editingPerson = null }) { newName ->
            if (newName.isNotBlank()) {
                db?.updatePersonName(person.id, newName)
                onRefresh()
            }
            editingPerson = null
        }
    }

    personToDelete?.let { person ->
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("Delete ${person.name}?", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily) },
            text = { Text("Removes all memories and chats for ${person.name}.", color = colors.textSecondary, fontFamily = GeneFontFamily) },
            confirmButton = {
                TextButton(onClick = {
                    db?.deletePerson(person.id)
                    onRefresh()
                    personToDelete = null
                }) { Text("Delete", color = colors.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) { Text("Cancel", color = colors.textSecondary) }
            },
            containerColor = colors.surface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PeopleHomeContent(
    colors: GeneColors,
    recent: List<Person>,
    favorites: List<Person>,
    people: List<Person>,
    favoritesExpanded: Boolean,
    allExpanded: Boolean,
    onToggleFavorites: () -> Unit,
    onToggleAll: () -> Unit,
    searchQuery: String,
    onPerson: (Long) -> Unit,
    onCapture: (Long) -> Unit,
    onChat: (Long) -> Unit,
    onLongPress: (Person) -> Unit,
    onOpenGraph: () -> Unit,
    onOpenSelf: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = GeneSpace.lg, vertical = GeneSpace.xs)) {
                Text(
                    "Gene",
                    color = colors.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GeneFontFamily,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Capture · patterns · talk about someone",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = GeneFontFamily
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChatStartBento(
                        title = "Relationships",
                        subtitle = "People graph",
                        icon = Icons.Outlined.Hub,
                        colors = colors,
                        filled = true,
                        onClick = onOpenGraph,
                        modifier = Modifier.weight(1f)
                    )
                    ChatStartBento(
                        title = "You",
                        subtitle = "Self profile",
                        icon = Icons.Outlined.AccountCircle,
                        colors = colors,
                        filled = false,
                        onClick = onOpenSelf,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (recent.isNotEmpty()) {
            item { NotionSectionHeader("Recents", colors) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = GeneSpace.md, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recent, key = { "r_${it.id}" }) { person ->
                        NotionRecentCard(
                            title = person.name,
                            coverColor = colors.pastel(person.id.toInt()),
                            colors = colors,
                            onClick = { onPerson(person.id) },
                            coverContent = { PersonAvatar(person, 52.dp, colors) },
                            footerIcon = {
                                Icon(Icons.Outlined.Person, null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        if (favorites.isNotEmpty()) {
            item {
                NotionSectionHeader("Favorites", colors, favoritesExpanded, onToggle = onToggleFavorites)
            }
            if (favoritesExpanded) {
                items(favorites, key = { "f_${it.id}" }) { person ->
                    PersonRow(
                        person, colors,
                        onClick = { onPerson(person.id) },
                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress(person) },
                        onCapture = { onCapture(person.id) },
                        onChat = { onChat(person.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
        }

        item {
            NotionSectionHeader(
                if (searchQuery.isBlank()) "People" else "Results",
                colors,
                allExpanded,
                onToggle = if (searchQuery.isBlank()) onToggleAll else null
            )
        }
        if (allExpanded || searchQuery.isNotBlank()) {
            if (people.isEmpty()) {
                item {
                    EmptyHint(
                        colors,
                        if (searchQuery.isNotBlank()) "No people match \"$searchQuery\""
                        else "Add someone — then capture a memory in one tap"
                    )
                }
            } else {
                items(people, key = { it.id }) { person ->
                    PersonRow(
                        person, colors,
                        onClick = { onPerson(person.id) },
                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress(person) },
                        onCapture = { onCapture(person.id) },
                        onChat = { onChat(person.id) }
                    )
                }
            }
        }
        item {
            Text(
                "${people.size} ${if (people.size == 1) "person" else "people"}",
                color = colors.textTertiary,
                fontSize = 12.sp,
                fontFamily = GeneFontFamily,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChatsHomeContent(
    colors: GeneColors,
    sessions: List<SessionWithPerson>,
    searchQuery: String,
    onOpenChat: (Long, Long) -> Unit,
    onOpenGroupChat: (Long) -> Unit,
    onStartChat: () -> Unit,
    onStartGroupChat: () -> Unit
) {
    val recent = remember(sessions) { sessions.take(8) }
    val favs = remember(sessions) { sessions.filter { it.session.favorite } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = GeneSpace.lg, vertical = GeneSpace.xs)) {
                Text("Chats", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily, letterSpacing = (-0.5).sp)
                Text("Sessions across everyone", color = colors.textSecondary, fontSize = 13.sp, fontFamily = GeneFontFamily)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChatStartBento(
                        title = "Start chat",
                        subtitle = "One person",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        colors = colors,
                        filled = true,
                        onClick = onStartChat,
                        modifier = Modifier.weight(1f)
                    )
                    ChatStartBento(
                        title = "Start group chat",
                        subtitle = "With @mentions",
                        icon = Icons.Outlined.Groups,
                        colors = colors,
                        filled = false,
                        onClick = onStartGroupChat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (searchQuery.isBlank() && recent.isNotEmpty()) {
            item { NotionSectionHeader("Recents", colors) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = GeneSpace.md, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recent, key = { "rs_${it.session.id}" }) { item ->
                        NotionRecentCard(
                            title = item.session.title.ifBlank { "Chat" },
                            coverColor = colors.pastel(item.session.id.toInt()),
                            colors = colors,
                            onClick = {
                                if (item.session.isGroup) onOpenGroupChat(item.session.id)
                                else onOpenChat(item.session.personId, item.session.id)
                            },
                            coverContent = {
                                Icon(
                                    if (item.session.isGroup) Icons.Outlined.Groups else Icons.Outlined.ChatBubbleOutline,
                                    null,
                                    tint = colors.textPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            footerIcon = {
                                Icon(
                                    if (item.session.isGroup) Icons.Outlined.Groups else Icons.Outlined.ChatBubbleOutline,
                                    null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        if (searchQuery.isBlank() && favs.isNotEmpty()) {
            item { NotionSectionHeader("Favorites", colors) }
            items(favs, key = { "fs_${it.session.id}" }) { item ->
                SessionRow(item, colors) {
                    if (item.session.isGroup) onOpenGroupChat(item.session.id)
                    else onOpenChat(item.session.personId, item.session.id)
                }
            }
        }
        item { NotionSectionHeader(if (searchQuery.isBlank()) "All chats" else "Results", colors) }
        if (sessions.isEmpty()) {
            item {
                EmptyHint(
                    colors,
                    if (searchQuery.isNotBlank()) "No chats match \"$searchQuery\""
                    else "Start a chat or group — they’ll land here"
                )
            }
        } else {
            items(sessions, key = { it.session.id }) { item ->
                SessionRow(item, colors) {
                    if (item.session.isGroup) onOpenGroupChat(item.session.id)
                    else onOpenChat(item.session.personId, item.session.id)
                }
            }
        }
    }
}

@Composable
private fun ChatStartBento(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: GeneColors,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(shape)
            .background(if (filled) colors.pillActive else colors.surface)
            .border(0.5.dp, colors.border.copy(alpha = if (filled) 0f else 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (filled) colors.onPillActive else colors.textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                color = if (filled) colors.onPillActive else colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneFontFamily
            )
            Text(
                subtitle,
                color = if (filled) colors.onPillActive.copy(alpha = 0.75f) else colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = GeneFontFamily
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonRow(
    person: Person,
    colors: GeneColors,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCapture: () -> Unit,
    onChat: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.pastel(person.id.toInt())),
            contentAlignment = Alignment.Center
        ) {
            if (!person.avatar.isNullOrBlank()) {
                MemojiAvatar(config = MemojiConfig.deserialize(person.avatar), size = 26.dp)
            } else {
                Text(person.name.take(1).uppercase(), color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeneFontFamily)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(person.name, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = GeneFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                if (person.favorite) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.Star, null, tint = colors.textTertiary, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                when {
                    person.interactionCount > 0 -> "${person.interactionCount} memories"
                    person.note.isNotBlank() -> person.note
                    else -> "No memories yet"
                },
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = GeneFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        GeneBarAction(
            onClick = onCapture,
            colors = colors,
            contentDescription = "Capture"
        ) {
            Icon(Icons.Outlined.EditNote, null, tint = colors.accent, modifier = Modifier.size(18.dp))
        }
        GeneBarAction(
            onClick = onChat,
            colors = colors,
            contentDescription = "Chat"
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun SessionRow(item: SessionWithPerson, colors: GeneColors, onClick: () -> Unit) {
    NotionListRow(
        title = item.session.title.ifBlank { if (item.session.isGroup) "Group chat" else "Untitled chat" },
        subtitle = "${item.personName} · ${item.session.updatedAt.asRelativeTime()}" +
            if (item.session.messageCount > 0) " · ${item.session.messageCount} msgs" else "",
        colors = colors,
        tileColor = colors.pastel(item.session.id.toInt()),
        onClick = onClick,
        leading = {
            Icon(
                when {
                    item.session.favorite -> Icons.Outlined.Star
                    item.session.isGroup -> Icons.Outlined.Groups
                    else -> Icons.Outlined.ChatBubbleOutline
                },
                null,
                tint = colors.textPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    )
}

@Composable
private fun PersonAvatar(person: Person, size: Dp, colors: GeneColors) {
    if (!person.avatar.isNullOrBlank()) {
        MemojiAvatar(config = MemojiConfig.deserialize(person.avatar), size = size)
    } else {
        Box(
            Modifier.size(size).clip(CircleShape).background(colors.avatar),
            contentAlignment = Alignment.Center
        ) {
            Text(person.name.take(1).uppercase(), color = colors.textPrimary, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily)
        }
    }
}

@Composable
private fun HomeSearchField(
    colors: GeneColors,
    hazeState: HazeState?,
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GeneGlassInputBar(
            colors = colors,
            hazeState = hazeState,
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search people or chats",
            singleLine = true,
            maxLines = 1,
            modifier = Modifier.weight(1f),
            leading = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(18.dp)
                )
            },
            trailing = {
                if (query.isNotEmpty()) {
                    GeneBarAction(
                        onClick = { onQueryChange("") },
                        muted = true,
                        colors = colors,
                        contentDescription = "Clear"
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        )
        TextButton(onClick = onClear) { Text("Cancel", color = colors.accent, fontSize = 14.sp, fontFamily = GeneFontFamily) }
    }
}

@Composable
private fun EmptyHint(colors: GeneColors, message: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = colors.textSecondary, fontSize = 15.sp, fontFamily = GeneFontFamily)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPersonSheet(colors: GeneColors, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("New person", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeneFontFamily)
            Spacer(Modifier.height(6.dp))
            Text("Then capture a memory right away.", color = colors.textSecondary, fontSize = 13.sp, fontFamily = GeneFontFamily)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                placeholder = { Text("Name", color = colors.textTertiary) },
                singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                shape = RoundedCornerShape(10.dp), colors = fieldColors(colors)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                placeholder = { Text("Note (optional)", color = colors.textTertiary) },
                minLines = 2, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (name.isNotBlank()) onCreate(name.trim(), note.trim())
                }),
                colors = fieldColors(colors)
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
                TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim(), note.trim()) }, enabled = name.isNotBlank()) {
                    Text("Create", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonPickerSheet(
    title: String,
    colors: GeneColors,
    people: List<Person>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp)) {
            Text(title, color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeneFontFamily, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            if (people.isEmpty()) {
                Text("Add a person first.", color = colors.textSecondary, modifier = Modifier.padding(20.dp), fontFamily = GeneFontFamily)
            } else {
                people.forEach { person ->
                    val label = if (person.isSelf) person.name.ifBlank { "You" } else person.name
                    PersonRow(
                        person = person.copy(name = if (person.isSelf && person.name == "You") "You · self" else label),
                        colors = colors,
                        onClick = { onPick(person.id) },
                        onLongClick = {},
                        onCapture = { onPick(person.id) },
                        onChat = { onPick(person.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupMemberPickerSheet(
    colors: GeneColors,
    people: List<Person>,
    onDismiss: () -> Unit,
    onCreate: (List<Long>) -> Unit
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            Text(
                "New group chat",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneFontFamily,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Text(
                "Pick at least two people. @mention them in the chat to get replies.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = GeneFontFamily,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
            )
            if (people.isEmpty()) {
                Text("Add people first.", color = colors.textSecondary, modifier = Modifier.padding(20.dp), fontFamily = GeneFontFamily)
            } else {
                people.forEach { person ->
                    val checked = person.id in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (checked) selected - person.id else selected + person.id
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.pastel(person.id.toInt())),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                person.name.take(1).uppercase(),
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = GeneFontFamily
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            person.name,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = GeneFontFamily,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (checked) colors.pillActive else colors.pill)
                                .border(0.5.dp, colors.border.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) {
                                Text("✓", color = colors.onPillActive, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = GeneFontFamily)
                }
                TextButton(
                    onClick = { onCreate(selected.toList()) },
                    enabled = selected.size >= 2
                ) {
                    Text(
                        "Create group",
                        color = if (selected.size >= 2) colors.accent else colors.textTertiary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GeneFontFamily
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonActionSheet(
    person: Person,
    colors: GeneColors,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onCustomizeMemoji: () -> Unit,
    onCapture: () -> Unit,
    onChat: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(person.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary, fontFamily = GeneFontFamily, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp))
            ActionRow(Icons.Outlined.EditNote, "Capture memory", colors.accent, onCapture)
            ActionRow(Icons.Outlined.ChatBubbleOutline, "Chat", colors.textPrimary, onChat)
            ActionRow(Icons.Outlined.PushPin, if (person.favorite) "Unpin" else "Pin", colors.textPrimary, onToggleFavorite)
            ActionRow(Icons.Outlined.Face, "Customize Memoji", colors.accent, onCustomizeMemoji)
            ActionRow(Icons.Outlined.Edit, "Rename", colors.textPrimary, onEdit)
            ActionRow(Icons.Outlined.Delete, "Delete", colors.danger, onDelete)
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 16.sp, fontFamily = GeneFontFamily)
    }
}

@Composable
private fun EditPersonDialog(person: Person, colors: GeneColors, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(person.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = fieldColors(colors)
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Save", color = colors.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) } },
        containerColor = colors.surface
    )
}

@Composable
private fun fieldColors(colors: GeneColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = colors.textPrimary,
    unfocusedTextColor = colors.textPrimary
)
