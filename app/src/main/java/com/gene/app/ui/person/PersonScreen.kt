package com.gene.app.ui.person

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gene.app.data.GeneDatabase
import com.gene.app.data.InsightEngine
import com.gene.app.data.Interaction
import com.gene.app.data.Person
import com.gene.app.data.RemoteInsightEngine
import com.gene.app.ui.chat.ChatPreviewCard
import com.gene.app.ui.chat.ChatPreviewMoreCard
import com.gene.app.ui.common.asDate
import com.gene.app.ui.memory.CaptureSheet
import com.gene.app.ui.memory.MasonryMemoryGallery
import com.gene.app.ui.memory.MemoryCard
import com.gene.app.ui.navigation.CHAT_TALK
import com.gene.app.ui.theme.GeneGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    person: Person,
    memories: List<Interaction>,
    dark: Boolean,
    context: Context,
    onOpenChat: (Long?, String) -> Unit,
    onAsk: () -> Unit,
    onSearch: () -> Unit,
    onCalendar: () -> Unit,
    onMemory: (Long) -> Unit,
    onAllMemories: () -> Unit,
    onAllChats: () -> Unit,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    db: GeneDatabase
) {
    var capture by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("gene_settings", Context.MODE_PRIVATE) }
    val memoryLayoutKey = "memory_grid_${person.id}"
    var memoryGrid by remember(person.id) { mutableStateOf(prefs.getBoolean(memoryLayoutKey, true)) }
    var showDelete by remember { mutableStateOf(false) }
    var personaMenuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(person.id, person.name) { mutableStateOf(person.name) }
    var personFavorite by remember(person.id, person.favorite) { mutableStateOf(person.favorite) }
    var summaryRefreshing by remember { mutableStateOf(false) }
    var displayedSummary by remember(person.id, person.summary) { mutableStateOf(person.summary) }
    val persona = remember(memories) { InsightEngine.persona(person, memories) }
    val sessions = db.sessions(person.id)
    val quoteSet = remember(memories) { memories.filter { it.type == "quote" }.shuffled().take(3) }
    var quoteIndex by remember(person.id) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(quoteSet.size) {
        if (quoteSet.size > 1) {
            while (true) {
                delay(5500)
                quoteIndex = (quoteIndex + 1) % quoteSet.size
            }
        }
    }

    fun regenerateSummary() {
        if (memories.size < 10 || summaryRefreshing) return
        summaryRefreshing = true
        scope.launch {
            val generated = RemoteInsightEngine.generatedSummary(context, person, memories)
            db.saveSummary(person.id, generated, memories.size)
            displayedSummary = generated
            summaryRefreshing = false
            onChanged()
        }
    }

    LaunchedEffect(person.id, memories.size, person.summaryMemoryCount) {
        if (memories.size >= 10 && (person.summary == null || memories.size >= person.summaryMemoryCount + 10)) {
            regenerateSummary()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.name, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search memories and chats") }
                    Box {
                        IconButton(onClick = { personaMenuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Person actions") }
                        DropdownMenu(expanded = personaMenuOpen, onDismissRequest = { personaMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (personFavorite) "Unfavorite" else "Favorite") },
                                leadingIcon = { Icon(if (personFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) },
                                onClick = {
                                    personFavorite = !personFavorite
                                    db.setPersonFavorite(person.id, personFavorite)
                                    personaMenuOpen = false
                                    onChanged()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                onClick = { personaMenuOpen = false; renameOpen = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                onClick = { personaMenuOpen = false; showDelete = true }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .padding(horizontal = 22.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    if (memories.size >= 10) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Summary", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { regenerateSummary() }, enabled = !summaryRefreshing) {
                                        Icon(Icons.Outlined.Refresh, "Regenerate summary")
                                    }
                                }
                                Text(displayedSummary ?: "Building a clearer picture…", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        Text("Add ${10 - memories.size} more memor${if (10 - memories.size == 1) "y" else "ies"} for a generated summary.", style = MaterialTheme.typography.bodyLarge, color = GeneGray)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        persona.traits.forEach { trait ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(end = 2.dp)
                            ) {
                                Text(trait, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Quick actions", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    QuickActionsGrid(
                        onCalendar = onCalendar,
                        onChat = { onOpenChat(null, CHAT_TALK) },
                        onAsk = onAsk,
                        onSearch = onSearch,
                        onRemember = { capture = true }
                    )
                    if (quoteSet.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Quotes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        QuoteCarousel(quoteSet, quoteIndex)
                    }
                    Spacer(Modifier.height(24.dp))
                    if (sessions.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Chats", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Row(Modifier.clickable(onClick = onAllChats).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("See all", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Outlined.ChevronRight, "See all chats", tint = GeneGray, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
                if (sessions.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) {
                            items(sessions.take(5), key = { "preview-${it.id}" }) { session ->
                                ChatPreviewCard(session, onClick = { onOpenChat(session.id, CHAT_TALK) })
                            }
                            if (sessions.size > 5) {
                                item { ChatPreviewMoreCard(onClick = onAllChats) }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Memories", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (memories.size > 10) TextButton(onClick = onAllMemories) { Text("Show all") }
                        IconButton(onClick = {
                            memoryGrid = !memoryGrid
                            prefs.edit().putBoolean(memoryLayoutKey, memoryGrid).apply()
                        }) {
                            Icon(if (memoryGrid) Icons.Outlined.ViewList else Icons.Outlined.ViewModule, if (memoryGrid) "List view" else "Grid view")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (memories.isEmpty()) item { Text("Your notes will appear here.", color = GeneGray, style = MaterialTheme.typography.bodyLarge) }
                if (memoryGrid) {
                    item { MasonryMemoryGallery(memories.take(10), onMemory) }
                } else {
                    items(memories.take(10), key = { it.id }) { memory -> MemoryCard(memory, onClick = { onMemory(memory.id) }) }
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                        )
                    )
            )
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding()) {
                MemoryComposer(person, db, onSaved = onChanged, onStartChat = { onOpenChat(null, CHAT_TALK) })
            }
        }
    }
    if (capture) CaptureSheet(person, db, onDismiss = { capture = false }, onSaved = { capture = false; onChanged() })
    if (renameOpen) {
        ModalBottomSheet(onDismissRequest = { renameOpen = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("Rename person", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            db.updatePersonName(person.id, renameText)
                            renameOpen = false
                            onChanged()
                        }
                    },
                    enabled = renameText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save name")
                }
            }
        }
    }
    if (showDelete) DeletePersonSheet(person, db, onDismiss = { showDelete = false }, onDeleted = { showDelete = false; onBack() })
}

@Composable
fun QuickActionsGrid(
    onCalendar: () -> Unit,
    onChat: () -> Unit,
    onAsk: () -> Unit,
    onSearch: () -> Unit,
    onRemember: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionBento(Icons.Outlined.Event, "Calendar", onCalendar, Modifier.weight(1f))
            ActionBento(Icons.Outlined.SmartToy, "Chat with", onChat, Modifier.weight(1f))
            ActionBento(Icons.Outlined.HelpOutline, "Ask about", onAsk, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionBento(Icons.Outlined.Search, "Search", onSearch, Modifier.weight(1f))
            ActionBento(Icons.Outlined.Lightbulb, "Remember", onRemember, Modifier.weight(1f))
        }
    }
}

@Composable
fun ActionBento(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(82.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, title, tint = GeneGray, modifier = Modifier.size(22.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun QuoteCarousel(quotes: List<Interaction>, index: Int) {
    val quote = quotes[index.coerceIn(0, quotes.lastIndex)]
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "“${quote.body.removePrefix("Quote · ").trim().trim('"')}”",
                style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text("${index + 1} / ${quotes.size}  ·  ${quote.createdAt.asDate()}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletePersonSheet(
    person: Person,
    db: GeneDatabase,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text("Remove ${person.name}?", style = MaterialTheme.typography.titleLarge)
            Text("This removes the person and every saved memory from this device. This cannot be undone.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { db.deletePerson(person.id); onDeleted() }, modifier = Modifier.fillMaxWidth()) {
                Text("Remove person")
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Keep person")
            }
        }
    }
}
