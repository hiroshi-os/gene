package com.gene.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Interaction
import com.gene.app.data.Person
import com.gene.app.data.TYPE_AUDIO
import com.gene.app.ui.common.asWhatsAppTime
import com.gene.app.ui.theme.WhatsAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    people: List<Person>,
    onPerson: (Long) -> Unit,
    onSettings: () -> Unit,
    onPersonCreated: (String, String) -> Unit,
    onRefresh: () -> Unit,
    db: GeneDatabase? = null
) {
    var showAdd by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var currentNavTab by remember { mutableStateOf(0) }

    val dark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == WhatsAppColors.DarkBackground
    val latestInteractions = remember(people) { db?.latestInteractions().orEmpty() }

    val filteredPeople = remember(people, searchQuery, selectedFilter) {
        var list = people
        if (selectedFilter == "Unread") {
            list = list.filter { it.interactionCount > 0 }
        } else if (selectedFilter == "Favorites") {
            list = list.filter { it.favorite }
        } else if (selectedFilter == "Groups") {
            list = emptyList() // Future groups support
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.name.contains(searchQuery, ignoreCase = true) || it.note.contains(searchQuery, ignoreCase = true) }
        }
        list
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search…", color = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, "Close search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (dark) WhatsAppColors.DarkTopBar else WhatsAppColors.LightTopBar
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "WhatsApp",
                            color = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* Camera action */ }) {
                            Icon(Icons.Outlined.CameraAlt, "Camera", tint = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.LightTextPrimary)
                        }
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Outlined.Search, "Search", tint = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.LightTextPrimary)
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Outlined.MoreVert, "More options", tint = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.LightTextPrimary)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(if (dark) WhatsAppColors.DarkSurface else WhatsAppColors.LightSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New person", color = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.LightTextPrimary) },
                                    onClick = { menuExpanded = false; showAdd = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("New group", color = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.LightTextPrimary) },
                                    onClick = { menuExpanded = false; showAdd = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Starred messages", color = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.LightTextPrimary) },
                                    onClick = { menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings", color = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.LightTextPrimary) },
                                    onClick = { menuExpanded = false; onSettings() }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (dark) WhatsAppColors.DarkTopBar else WhatsAppColors.LightTopBar
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = WhatsAppColors.GreenTeal,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Chat, "New chat", modifier = Modifier.size(24.dp))
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = if (dark) WhatsAppColors.DarkSurface else WhatsAppColors.LightSurface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentNavTab == 0,
                    onClick = { currentNavTab = 0 },
                    icon = {
                        Box {
                            Icon(Icons.Filled.ChatBubble, "Chats")
                            if (people.isNotEmpty()) {
                                Badge(
                                    containerColor = WhatsAppColors.GreenAccent,
                                    contentColor = Color.Black,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(people.size.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    label = { Text("Chats", fontWeight = if (currentNavTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                        selectedTextColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                        indicatorColor = if (dark) WhatsAppColors.DarkChipSelectedBg else WhatsAppColors.LightChipSelectedBg,
                        unselectedIconColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary,
                        unselectedTextColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentNavTab == 1,
                    onClick = { currentNavTab = 1 },
                    icon = { Icon(Icons.Outlined.DonutLarge, "Updates") },
                    label = { Text("Updates", fontWeight = if (currentNavTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                        selectedTextColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                        indicatorColor = if (dark) WhatsAppColors.DarkChipSelectedBg else WhatsAppColors.LightChipSelectedBg,
                        unselectedIconColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary,
                        unselectedTextColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentNavTab == 2,
                    onClick = { currentNavTab = 2 },
                    icon = { Icon(Icons.Outlined.Groups, "Communities") },
                    label = { Text("Communities", fontWeight = if (currentNavTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                        selectedTextColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                        indicatorColor = if (dark) WhatsAppColors.DarkChipSelectedBg else WhatsAppColors.LightChipSelectedBg,
                        unselectedIconColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary,
                        unselectedTextColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentNavTab == 3,
                    onClick = { currentNavTab = 3 },
                    icon = { Icon(Icons.Outlined.Call, "Calls") },
                    label = { Text("Calls", fontWeight = if (currentNavTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                        selectedTextColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.GreenPrimary,
                        indicatorColor = if (dark) WhatsAppColors.DarkChipSelectedBg else WhatsAppColors.LightChipSelectedBg,
                        unselectedIconColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary,
                        unselectedTextColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary
                    )
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (dark) WhatsAppColors.DarkBackground else WhatsAppColors.LightBackground)
        ) {
            // Search Pill Bar (Meta AI / Search)
            if (!isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(CircleShape)
                            .background(if (dark) WhatsAppColors.DarkSearchBg else WhatsAppColors.LightSearchBg)
                            .clickable { isSearching = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Ask Meta AI or Search",
                                color = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Filter Chips (All, Unread, Favorites, Groups)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Unread", "Favorites", "Groups")
                    filters.forEach { filter ->
                        val selected = selectedFilter == filter
                        Surface(
                            shape = CircleShape,
                            color = if (selected) {
                                if (dark) WhatsAppColors.DarkChipSelectedBg else WhatsAppColors.LightChipSelectedBg
                            } else {
                                if (dark) WhatsAppColors.DarkChipBg else WhatsAppColors.LightChipBg
                            },
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                color = if (selected) {
                                    if (dark) WhatsAppColors.DarkChipSelectedText else WhatsAppColors.LightChipSelectedText
                                } else {
                                    if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary
                                },
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (filteredPeople.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                if (searchQuery.isNotBlank()) "No chats match \"$searchQuery\"" else "No chats yet",
                                color = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary,
                                fontSize = 16.sp
                            )
                            Button(
                                onClick = { showAdd = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppColors.GreenTeal)
                            ) {
                                Text("Start a chat")
                            }
                        }
                    }
                }
            }

            // WhatsApp Chat Items
            items(filteredPeople, key = { it.id }) { person ->
                WhatsAppChatRow(
                    person = person,
                    latestInteraction = latestInteractions[person.id],
                    dark = dark,
                    onClick = { onPerson(person.id) }
                )
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showAdd) {
        AddPersonSheet(
            onDismiss = { showAdd = false },
            onCreate = { name, note ->
                showAdd = false
                onPersonCreated(name, note)
            }
        )
    }
}

@Composable
fun WhatsAppChatRow(
    person: Person,
    latestInteraction: Interaction?,
    dark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (dark) WhatsAppColors.DarkTextPrimary else WhatsAppColors.LightTextPrimary
    val secondaryColor = if (dark) WhatsAppColors.DarkTextSecondary else WhatsAppColors.LightTextSecondary
    val dividerColor = if (dark) WhatsAppColors.DarkDivider else WhatsAppColors.LightDivider
    val avatarBg = if (dark) WhatsAppColors.DarkAvatarBg else WhatsAppColors.LightAvatarBg
    val avatarIcon = if (dark) WhatsAppColors.DarkAvatarIcon else WhatsAppColors.LightAvatarIcon

    val displayTime = latestInteraction?.createdAt?.asWhatsAppTime() ?: person.lastSeen.asWhatsAppTime()
    val previewText = when {
        latestInteraction != null -> {
            if (latestInteraction.type == TYPE_AUDIO) "Audio recording"
            else latestInteraction.body.take(60)
        }
        person.note.isNotBlank() -> person.note.take(60)
        else -> "Tap to start chatting"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WhatsApp Contact Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = person.name.take(1).uppercase(),
                    color = avatarIcon,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(14.dp))

            // Contact details + Last message
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = person.name,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = displayTime,
                        color = if (person.interactionCount > 0) WhatsAppColors.GreenAccent else secondaryColor,
                        fontSize = 12.sp,
                        fontWeight = if (person.interactionCount > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (latestInteraction != null) {
                            if (latestInteraction.type == TYPE_AUDIO) {
                                Icon(
                                    Icons.Outlined.Mic,
                                    contentDescription = null,
                                    tint = secondaryColor,
                                    modifier = Modifier.size(16.dp).padding(end = 3.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.DoneAll,
                                    contentDescription = null,
                                    tint = WhatsAppColors.BlueCheck,
                                    modifier = Modifier.size(16.dp).padding(end = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = previewText,
                            color = secondaryColor,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (person.favorite) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = secondaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (person.interactionCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppColors.GreenAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = person.interactionCount.toString(),
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // WhatsApp Indented Divider
        HorizontalDivider(
            modifier = Modifier.padding(start = 78.dp),
            thickness = 0.6.dp,
            color = dividerColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonSheet(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("New contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Add a contact or person to start capturing conversations and memories.", color = WhatsAppColors.LightTextSecondary, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Contact name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Status / note (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, note) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppColors.GreenTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create contact")
            }
        }
    }
}
