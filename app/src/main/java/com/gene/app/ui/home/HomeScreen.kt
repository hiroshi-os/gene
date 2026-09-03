package com.gene.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.gene.app.ui.theme.GeneGray
import com.gene.app.ui.theme.GeneMutedGray
import com.gene.app.ui.theme.NotionDarkAvatar
import com.gene.app.ui.theme.NotionDarkBg
import com.gene.app.ui.theme.NotionDarkFrosted
import com.gene.app.ui.theme.NotionDarkPill
import com.gene.app.ui.theme.NotionDarkPillActive
import com.gene.app.ui.theme.NotionDarkTextPrimary
import com.gene.app.ui.theme.NotionDarkTextSecondary
import com.gene.app.ui.theme.NotionLightAvatar
import com.gene.app.ui.theme.NotionLightBg
import com.gene.app.ui.theme.NotionLightFrosted
import com.gene.app.ui.theme.NotionLightPill
import com.gene.app.ui.theme.NotionLightPillActive
import com.gene.app.ui.theme.NotionLightTextPrimary
import com.gene.app.ui.theme.NotionLightTextSecondary

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
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var currentNavTab by remember { mutableStateOf(0) }

    val dark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == NotionDarkBg
    val latestInteractions = remember(people) { db?.latestInteractions().orEmpty() }

    val textPrimary = if (dark) NotionDarkTextPrimary else NotionLightTextPrimary
    val textSecondary = if (dark) NotionDarkTextSecondary else NotionLightTextSecondary
    val frostedBg = if (dark) NotionDarkFrosted else NotionLightFrosted
    val appBg = if (dark) NotionDarkBg else NotionLightBg

    val filteredPeople = remember(people, searchQuery, selectedFilter) {
        var list = people
        if (selectedFilter == "Unread") {
            list = list.filter { it.interactionCount > 0 }
        } else if (selectedFilter == "Favorites") {
            list = list.filter { it.favorite }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.note.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }

    Scaffold(
        containerColor = appBg,
        topBar = {
            if (isSearching) {
                Surface(
                    color = frostedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search memories, chats, people…", color = textSecondary, fontSize = 15.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, "Close search", tint = textSecondary)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = if (dark) NotionDarkPill else NotionLightPill,
                                unfocusedContainerColor = if (dark) NotionDarkPill else NotionLightPill,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            )
                        )
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "gene",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Outlined.Search, "Search", tint = textPrimary)
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Outlined.Settings, "Settings", tint = textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = appBg
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, "New person", modifier = Modifier.size(24.dp))
            }
        },
        bottomBar = {
            Surface(
                color = frostedBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentNavTab == 0,
                        onClick = { currentNavTab = 0 },
                        icon = {
                            Box {
                                Icon(if (currentNavTab == 0) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline, "Chats")
                                if (people.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text(people.size.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        label = { Text("Chats", fontWeight = if (currentNavTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = textPrimary,
                            selectedTextColor = textPrimary,
                            indicatorColor = if (dark) NotionDarkPill else NotionLightPill,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = currentNavTab == 1,
                        onClick = { currentNavTab = 1 },
                        icon = { Icon(Icons.Outlined.Lightbulb, "Memories") },
                        label = { Text("Memories", fontWeight = if (currentNavTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = textPrimary,
                            selectedTextColor = textPrimary,
                            indicatorColor = if (dark) NotionDarkPill else NotionLightPill,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 110.dp, bottom = 90.dp)
            ) {
                if (filteredPeople.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp, start = 32.dp, end = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    if (searchQuery.isNotBlank()) "No one matches \"$searchQuery\"" else "No people or chats yet",
                                    color = textSecondary,
                                    fontSize = 16.sp
                                )
                                Button(
                                    onClick = { showAdd = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Add someone")
                                }
                            }
                        }
                    }
                }

                // Chat rows (WhatsApp structure, Notion aesthetics: zero borders)
                items(filteredPeople, key = { it.id }) { person ->
                    NotionChatRow(
                        person = person,
                        latestInteraction = latestInteractions[person.id],
                        dark = dark,
                        onClick = { onPerson(person.id) }
                    )
                }
            }

            // Floating Frosted Glass Search Bar & Filter Tags
            if (!isSearching) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(frostedBg)
                        .padding(bottom = 8.dp)
                ) {
                    // Frosted Glass Search Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (dark) NotionDarkPill else NotionLightPill)
                            .clickable { isSearching = true }
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Search memories, chats, people…",
                                color = textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Notion Filter Tags (Seamless, zero borders)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All", "Unread", "Favorites")
                        filters.forEach { filter ->
                            val selected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (selected) {
                                            if (dark) NotionDarkPillActive else NotionLightPillActive
                                        } else {
                                            if (dark) NotionDarkPill else NotionLightPill
                                        }
                                    )
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    color = if (selected) {
                                        if (dark) NotionDarkBg else NotionLightBg
                                    } else {
                                        textSecondary
                                    },
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
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
fun NotionChatRow(
    person: Person,
    latestInteraction: Interaction?,
    dark: Boolean,
    onClick: () -> Unit
) {
    val textPrimary = if (dark) NotionDarkTextPrimary else NotionLightTextPrimary
    val textSecondary = if (dark) NotionDarkTextSecondary else NotionLightTextSecondary
    val avatarBg = if (dark) NotionDarkAvatar else NotionLightAvatar

    val displayTime = latestInteraction?.createdAt?.asWhatsAppTime() ?: person.lastSeen.asWhatsAppTime()
    val previewText = when {
        latestInteraction != null -> {
            if (latestInteraction.type == TYPE_AUDIO) "Voice recording"
            else latestInteraction.body.take(65)
        }
        person.note.isNotBlank() -> person.note.take(65)
        else -> "No memories recorded yet"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Notion Minimalist Avatar (Zero border)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = person.name.take(1).uppercase(),
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.width(14.dp))

        // Content details + message snippet
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = person.name,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = displayTime,
                    color = textSecondary,
                    fontSize = 12.sp
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
                                Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(15.dp).padding(end = 4.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.DoneAll,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(15.dp).padding(end = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = previewText,
                        color = textSecondary,
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
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (person.interactionCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(if (dark) NotionDarkPillActive else NotionLightPillActive),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.interactionCount.toString(),
                                color = if (dark) NotionDarkBg else NotionLightBg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonSheet(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val dark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == NotionDarkBg
    val textPrimary = if (dark) NotionDarkTextPrimary else NotionLightTextPrimary
    val textSecondary = if (dark) NotionDarkTextSecondary else NotionLightTextSecondary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (dark) NotionDarkBg else NotionLightBg
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("New person", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textPrimary)
            Text("Begin a private thread of context. You can capture memories and chats over time.", color = textSecondary, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textSecondary,
                    unfocusedBorderColor = if (dark) NotionDarkPill else NotionLightPill,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / context (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textSecondary,
                    unfocusedBorderColor = if (dark) NotionDarkPill else NotionLightPill,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, note) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Start thread")
            }
        }
    }
}
