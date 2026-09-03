package com.gene.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Interaction
import com.gene.app.data.Person
import com.gene.app.data.TYPE_AUDIO
import com.gene.app.ui.common.AcrylicTabBar
import com.gene.app.ui.common.MemojiAvatar
import com.gene.app.ui.common.MemojiConfig
import com.gene.app.ui.common.MemojiMakerSheet
import com.gene.app.ui.common.asIMessageTime
import com.gene.app.ui.theme.GeneBlack
import com.gene.app.ui.theme.GeneWhite
import com.gene.app.ui.theme.IosBlue
import com.gene.app.ui.theme.IosDarkAvatar
import com.gene.app.ui.theme.IosDarkBg
import com.gene.app.ui.theme.IosDarkBlue
import com.gene.app.ui.theme.IosDarkDivider
import com.gene.app.ui.theme.IosDarkSecondary
import com.gene.app.ui.theme.IosLightAvatar
import com.gene.app.ui.theme.IosLightBg
import com.gene.app.ui.theme.IosLightDivider
import com.gene.app.ui.theme.IosLightSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    people: List<Person>,
    dark: Boolean,
    onPerson: (Long) -> Unit,
    onSettings: () -> Unit,
    onPersonCreated: (String, String) -> Unit,
    onRefresh: () -> Unit,
    db: GeneDatabase? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Messages, 1 = Contacts
    var showNewMessage by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var editMenuExpanded by remember { mutableStateOf(false) }

    // Memoji customizer state
    var personForMemoji by remember { mutableStateOf<Person?>(null) }

    // Long press / Action sheet state
    var selectedPersonForAction by remember { mutableStateOf<Person?>(null) }
    var editingPerson by remember { mutableStateOf<Person?>(null) }
    var personToDelete by remember { mutableStateOf<Person?>(null) }

    val latestInteractions = remember(people) { db?.latestInteractions().orEmpty() }

    // Theme-aware Apple iMessage / Contacts tokens
    val bg = if (dark) IosDarkBg else IosLightBg
    val textPrimary = if (dark) GeneWhite else GeneBlack
    val textSecondary = if (dark) IosDarkSecondary else IosLightSecondary
    val dividerColor = if (dark) IosDarkDivider else IosLightDivider
    val accentBlue = if (dark) IosDarkBlue else IosBlue
    val avatarBg = if (dark) IosDarkAvatar else IosLightAvatar

    // Frosted acrylic glass tokens
    val frostedHeaderBg = if (dark) Color(0xE0000000) else Color(0xEBFFFFFF)
    val frostedSearchPillBg = if (dark) Color(0x3D767680) else Color(0x1F767680)
    val frostedMenuBg = if (dark) Color(0xEA1C1C1E) else Color(0xF4F2F2F7)
    val frostedSheetBg = if (dark) Color(0xF01C1C1E) else Color(0xF6FFFFFF)

    val pinnedPeople = remember(people) { people.filter { it.favorite } }
    val filteredPeople = remember(people, searchQuery) {
        if (searchQuery.isBlank()) people
        else people.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.note.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Main Content: Messages (Tab 0) or Contacts (Tab 1)
        if (selectedTab == 0) {
            // PAGE 1: APPLE IMESSAGES
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 108.dp, bottom = 90.dp)
            ) {
                // iOS Large Navigation Title: "Messages"
                item {
                    Text(
                        text = "Messages",
                        color = textPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }

                // iMessage Pinned Contacts Carousel (Floating Memoji Bubbles)
                if (pinnedPeople.isNotEmpty() && searchQuery.isEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(pinnedPeople, key = { "pinned_${it.id}" }) { person ->
                                IMessagePinnedContact(
                                    person = person,
                                    latestInteraction = latestInteractions[person.id],
                                    isDark = dark,
                                    avatarBg = avatarBg,
                                    textPrimary = textPrimary,
                                    onClick = { onPerson(person.id) },
                                    onLongClick = { selectedPersonForAction = person }
                                )
                            }
                        }
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                    }
                }

                // Empty State
                if (filteredPeople.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp, start = 32.dp, end = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    if (searchQuery.isNotBlank()) "No Results for \"$searchQuery\"" else "No Messages",
                                    color = textSecondary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                if (searchQuery.isEmpty()) {
                                    TextButton(
                                        onClick = { showNewMessage = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = accentBlue)
                                    ) {
                                        Text("Start a conversation", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Conversation Rows
                items(filteredPeople, key = { it.id }) { person ->
                    IMessageChatRow(
                        person = person,
                        latestInteraction = latestInteractions[person.id],
                        isDark = dark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        dividerColor = dividerColor,
                        accentBlue = accentBlue,
                        avatarBg = avatarBg,
                        onClick = { onPerson(person.id) },
                        onLongClick = { selectedPersonForAction = person }
                    )
                }
            }
        } else {
            // PAGE 2: APPLE CONTACTS
            AppleContactsView(
                people = filteredPeople,
                dark = dark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                dividerColor = dividerColor,
                accentBlue = accentBlue,
                avatarBg = avatarBg,
                searchQuery = searchQuery,
                onPersonClick = onPerson,
                onAddPerson = { showNewMessage = true },
                onCustomizeMemoji = { personForMemoji = it }
            )
        }

        // Floating Frosted Glass Header (Top Bar + Search Bar)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            frostedHeaderBg,
                            frostedHeaderBg,
                            frostedHeaderBg.copy(alpha = 0.95f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            // Top Navigation Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    TextButton(
                        onClick = { editMenuExpanded = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = accentBlue)
                    ) {
                        Text(if (selectedTab == 0) "Edit" else "Groups", fontSize = 17.sp, fontWeight = FontWeight.Normal)
                    }
                    DropdownMenu(
                        expanded = editMenuExpanded,
                        onDismissRequest = { editMenuExpanded = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(frostedMenuBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings", color = textPrimary) },
                            onClick = { editMenuExpanded = false; onSettings() }
                        )
                    }
                }

                // Top Right Action: Compose Message or Add Contact
                IconButton(onClick = { showNewMessage = true }) {
                    Icon(
                        if (selectedTab == 0) Icons.Outlined.Edit else Icons.Outlined.Add,
                        contentDescription = if (selectedTab == 0) "New Message" else "Add Contact",
                        tint = accentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Floating Frosted Search Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(frostedSearchPillBg)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; isSearchActive = true },
                            placeholder = {
                                Text("Search", color = textSecondary, fontSize = 16.sp)
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            )
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (isSearchActive || searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            isSearchActive = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = accentBlue),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }
                }
            }
        }

        // Floating Centered Shrunk Acrylic Tab Bar at the Bottom
        AcrylicTabBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            isDark = dark,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Modal: iOS "New Message / New Contact" Sheet
    if (showNewMessage) {
        NewMessageSheet(
            isDark = dark,
            sheetBg = frostedSheetBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accentBlue = accentBlue,
            searchBg = frostedSearchPillBg,
            onDismiss = { showNewMessage = false },
            onCreate = { name, note ->
                showNewMessage = false
                onPersonCreated(name, note)
            }
        )
    }

    // Modal: Apple Memoji Character Maker
    personForMemoji?.let { person ->
        MemojiMakerSheet(
            initialConfig = MemojiConfig.deserialize(person.avatar),
            isDark = dark,
            onDismiss = { personForMemoji = null },
            onSave = { newConfig ->
                if (db != null) {
                    db.updatePersonAvatar(person.id, newConfig.serialize())
                    onRefresh()
                }
                personForMemoji = null
            }
        )
    }

    // Modal: iOS Haptic Touch Action Sheet
    selectedPersonForAction?.let { person ->
        IMessageActionSheet(
            person = person,
            isDark = dark,
            sheetBg = frostedSheetBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onDismiss = { selectedPersonForAction = null },
            onToggleFavorite = {
                if (db != null) {
                    db.setPersonFavorite(person.id, !person.favorite)
                    onRefresh()
                }
                selectedPersonForAction = null
            },
            onEdit = {
                editingPerson = person
                selectedPersonForAction = null
            },
            onCustomizeMemoji = {
                personForMemoji = person
                selectedPersonForAction = null
            },
            onDelete = {
                personToDelete = person
                selectedPersonForAction = null
            }
        )
    }

    // Modal: Rename Contact Dialog
    editingPerson?.let { person ->
        EditPersonDialog(
            person = person,
            isDark = dark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accentBlue = accentBlue,
            searchBg = frostedSearchPillBg,
            onDismiss = { editingPerson = null },
            onSave = { newName ->
                if (db != null && newName.isNotBlank()) {
                    db.updatePersonName(person.id, newName)
                    onRefresh()
                }
                editingPerson = null
            }
        )
    }

    // Modal: Confirm Delete Dialog
    personToDelete?.let { person ->
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("Delete Contact?", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Deleting will remove all recorded memories and chat history with ${person.name}.", color = textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (db != null) {
                            db.deletePerson(person.id)
                            onRefresh()
                        }
                        personToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) {
                    Text("Cancel", color = accentBlue)
                }
            },
            containerColor = if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
        )
    }
}

/**
 * Apple Contacts Page with Alphabetical Sections and "My Card" Header
 */
@Composable
fun AppleContactsView(
    people: List<Person>,
    dark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    accentBlue: Color,
    avatarBg: Color,
    searchQuery: String,
    onPersonClick: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onCustomizeMemoji: (Person) -> Unit
) {
    // Group people alphabetically
    val grouped = remember(people) {
        people.sortedBy { it.name.lowercase() }
            .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 108.dp, bottom = 90.dp)
    ) {
        // iOS Large Title: "Contacts"
        item {
            Text(
                text = "Contacts",
                color = textPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // "My Card" Header (Apple Contacts style)
        if (searchQuery.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemojiAvatar(
                        config = MemojiConfig(bgColor = accentBlue),
                        size = 56.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "My Card",
                            color = textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap to view profile & Memoji",
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
            }
        }

        // Alphabetical Contact Sections (A, B, C...)
        grouped.forEach { (letter, contactsInGroup) ->
            item {
                Text(
                    text = letter.toString(),
                    color = textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(contactsInGroup, key = { it.id }) { person ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPersonClick(person.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contact Memoji or Circular Initial
                    if (!person.avatar.isNullOrBlank()) {
                        MemojiAvatar(
                            config = MemojiConfig.deserialize(person.avatar),
                            size = 40.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Text(
                        text = person.name,
                        color = textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    // Quick Memoji customizer button
                    IconButton(
                        onClick = { onCustomizeMemoji(person) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Face,
                            contentDescription = "Edit Memoji",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 70.dp), color = dividerColor, thickness = 0.5.dp)
            }
        }

        // Contact Count Footer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${people.size} Contacts",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * iMessage Pinned Contact Avatar with Floating Speech Bubble
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IMessagePinnedContact(
    person: Person,
    latestInteraction: Interaction?,
    isDark: Boolean,
    avatarBg: Color,
    textPrimary: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val previewText = latestInteraction?.body?.take(22) ?: person.note.take(22)
    val frostedBubbleBg = if (isDark) Color(0xCC2C2C2E) else Color(0xD8E5E5EA)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        // Floating Frosted Glass Speech Bubble Preview (iMessage style)
        if (previewText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(frostedBubbleBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = previewText,
                    color = textPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        // 64dp Circular Memoji / Avatar
        if (!person.avatar.isNullOrBlank()) {
            MemojiAvatar(
                config = MemojiConfig.deserialize(person.avatar),
                size = 64.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = person.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Contact First Name
        Text(
            text = person.name.split(" ").firstOrNull() ?: person.name,
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Apple iMessage Conversation Row
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IMessageChatRow(
    person: Person,
    latestInteraction: Interaction?,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    accentBlue: Color,
    avatarBg: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hasUnread = person.interactionCount > 0
    val displayTime = latestInteraction?.createdAt?.asIMessageTime() ?: person.lastSeen.asIMessageTime()
    val previewText = when {
        latestInteraction != null -> {
            if (latestInteraction.type == TYPE_AUDIO) "Voice message"
            else latestInteraction.body
        }
        person.note.isNotBlank() -> person.note
        else -> "New conversation"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Far Left: iOS Blue Unread Dot (10dp)
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentBlue)
                    )
                }
            }

            // Contact Memoji / Avatar
            if (!person.avatar.isNullOrBlank()) {
                MemojiAvatar(
                    config = MemojiConfig.deserialize(person.avatar),
                    size = 48.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = person.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Thread Content
            Column(modifier = Modifier.weight(1f)) {
                // Top line: Name + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = person.name,
                        color = textPrimary,
                        fontSize = 17.sp,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = displayTime,
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(3.dp))

                // Bottom line: Preview snippet + right chevron
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (latestInteraction?.type == TYPE_AUDIO) {
                            Icon(
                                Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp).padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = previewText,
                            color = textSecondary,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp).padding(start = 4.dp)
                    )
                }
            }
        }

        // iOS Table Inset Divider
        HorizontalDivider(
            modifier = Modifier.padding(start = 88.dp),
            thickness = 0.5.dp,
            color = dividerColor
        )
    }
}

/**
 * iOS New Message Compose Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageSheet(
    isDark: Boolean,
    sheetBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    searchBg: Color,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentBlue)
                ) {
                    Text("Cancel", fontSize = 17.sp)
                }
                Text(
                    "New Message",
                    color = textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = { if (name.isNotBlank()) onCreate(name.trim(), note.trim()) },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(contentColor = accentBlue)
                ) {
                    Text("Done", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = if (isDark) IosDarkDivider else IosLightDivider, thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("To:", color = textSecondary, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name or Phone Number", color = textSecondary) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )
            }

            HorizontalDivider(color = if (isDark) IosDarkDivider else IosLightDivider, thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("Note:", color = textSecondary, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Context or summary (optional)", color = textSecondary) },
                    minLines = 3,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (name.isNotBlank()) onCreate(name.trim(), note.trim())
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )
            }
        }
    }
}

/**
 * iOS Context Action Sheet with Memoji Customization Option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IMessageActionSheet(
    person: Person,
    isDark: Boolean,
    sheetBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onCustomizeMemoji: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                person.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Pin / Unpin
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleFavorite)
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.PushPin, contentDescription = null, tint = textPrimary)
                Spacer(Modifier.width(14.dp))
                Text(
                    if (person.favorite) "Unpin" else "Pin",
                    color = textPrimary,
                    fontSize = 17.sp
                )
            }

            // Customize Memoji
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onCustomizeMemoji)
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Face, contentDescription = null, tint = IosBlue)
                Spacer(Modifier.width(14.dp))
                Text("Customize Memoji", color = IosBlue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            // Edit Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onEdit)
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = textPrimary)
                Spacer(Modifier.width(14.dp))
                Text("Edit Name", color = textPrimary, fontSize = 17.sp)
            }

            // Delete Conversation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDelete)
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFFF3B30))
                Spacer(Modifier.width(14.dp))
                Text("Delete Conversation", color = Color(0xFFFF3B30), fontSize = 17.sp)
            }
        }
    }
}

/**
 * Edit Name Dialog
 */
@Composable
fun EditPersonDialog(
    person: Person,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    searchBg: Color,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(person.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Name", color = textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = searchBg,
                    unfocusedContainerColor = searchBg,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = accentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary)
            }
        },
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    )
}
