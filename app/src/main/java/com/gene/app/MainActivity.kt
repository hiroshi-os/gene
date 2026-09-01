package com.gene.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.CalendarContract
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.gene.app.data.AudioRecorder
import com.gene.app.data.AudioTranscriptionWorker
import com.gene.app.data.GeneDatabase
import com.gene.app.data.InsightEngine
import com.gene.app.data.ChatMessage
import com.gene.app.data.ChatSession
import com.gene.app.data.Interaction
import com.gene.app.data.ImportSummary
import com.gene.app.data.LocalRelevanceSearch
import com.gene.app.data.Person
import com.gene.app.data.ROLE_ASSISTANT
import com.gene.app.data.ROLE_USER
import com.gene.app.data.RemoteInsightEngine
import com.gene.app.data.TYPE_AUDIO
import com.gene.app.data.TYPE_TEXT
import com.gene.app.data.TYPE_QUOTE
import com.gene.app.data.TYPE_SIGNAL
import com.gene.app.data.TYPE_PATTERN
import com.gene.app.data.TRANSCRIPTION_COMPLETE
import com.gene.app.data.TRANSCRIPTION_PENDING
import com.gene.app.data.TRANSCRIPTION_UNAVAILABLE
import com.gene.app.service.FloatingCaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GeneBlack = Color(0xFF080808)
private val GeneWhite = Color(0xFFFDFDFB)
private val GeneGray = Color(0xFF777777)
private const val CHAT_TALK = "talk"
private const val CHAT_ASK = "ask"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GeneApp(savedInstanceState?.getLong("person_id", intent?.getLongExtra("person_id", -1L) ?: -1L) ?: -1L) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}

private sealed interface AppScreen {
    data object Home : AppScreen
    data class PersonDetail(val id: Long) : AppScreen
    data class Chat(val personId: Long, val sessionId: Long?, val mode: String = CHAT_TALK) : AppScreen
    data class Search(val personId: Long) : AppScreen
    data class AllChats(val personId: Long) : AppScreen
    data class Calendar(val personId: Long) : AppScreen
    data class MemoryDetail(val personId: Long, val memoryId: Long) : AppScreen
    data class AllMemories(val personId: Long) : AppScreen
    data object Settings : AppScreen
}

@Composable
private fun GeneApp(initialPersonId: Long = -1L) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("gene_settings", android.content.Context.MODE_PRIVATE) }
    val db = remember { GeneDatabase(context.applicationContext) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var screen by remember { mutableStateOf<AppScreen>(if (initialPersonId > 0) AppScreen.PersonDetail(initialPersonId) else AppScreen.Home) }
    var refresh by remember { mutableStateOf(0) }
    val people = remember(refresh) { db.people() }
    val colors = if (dark) darkColorScheme(background = GeneBlack, surface = GeneBlack, primary = GeneWhite, onBackground = GeneWhite, onSurface = GeneWhite, onPrimary = GeneBlack, outline = Color(0xFF555555)) else lightColorScheme(background = GeneWhite, surface = GeneWhite, primary = GeneBlack, onBackground = GeneBlack, onSurface = GeneBlack, onPrimary = GeneWhite, outline = Color(0xFFB4B4B0))
    val toggleDark = { val next = !dark; dark = next; prefs.edit().putBoolean("dark_mode", next).apply() }

    LaunchedEffect(screen) {
        val detail = screen as? AppScreen.PersonDetail
        if (detail != null) db.person(detail.id)?.let { prefs.edit().putLong("selected_person_id", it.id).putString("selected_person_name", it.name).apply() }
    }

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = if (dark) GeneBlack.toArgb() else GeneWhite.toArgb()
        window.navigationBarColor = if (dark) GeneBlack.toArgb() else GeneWhite.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }

    BackHandler(enabled = screen != AppScreen.Home) {
        screen = when (val current = screen) {
            is AppScreen.Chat -> AppScreen.PersonDetail(current.personId)
            is AppScreen.Search -> AppScreen.PersonDetail(current.personId)
            is AppScreen.AllChats -> AppScreen.PersonDetail(current.personId)
            is AppScreen.Calendar -> AppScreen.PersonDetail(current.personId)
            is AppScreen.AllMemories -> AppScreen.PersonDetail(current.personId)
            is AppScreen.MemoryDetail -> AppScreen.PersonDetail(current.personId)
            is AppScreen.PersonDetail, AppScreen.Settings -> AppScreen.Home
            AppScreen.Home -> AppScreen.Home
        }
    }

    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography.copy(
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 25.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, lineHeight = 34.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 26.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 21.sp)
    )) {
        when (val current = screen) {
            AppScreen.Home -> HomeScreen(
                people = people,
                onPerson = { screen = AppScreen.PersonDetail(it) },
                onSettings = { screen = AppScreen.Settings },
                onPersonCreated = { name, note -> val id = db.addPerson(name, note); refresh++; screen = AppScreen.PersonDetail(id) },
                onRefresh = { refresh++ }
            )
            is AppScreen.PersonDetail -> {
                val person = db.person(current.id)
                if (person == null) screen = AppScreen.Home else PersonScreen(
                    person = person,
                    memories = db.interactions(person.id),
                    dark = dark,
                    context = context,
                    onOpenChat = { sessionId, mode -> screen = AppScreen.Chat(person.id, sessionId, mode) },
                    onAsk = { screen = AppScreen.Chat(person.id, null, CHAT_ASK) },
                    onSearch = { screen = AppScreen.Search(person.id) },
                    onCalendar = { screen = AppScreen.Calendar(person.id) },
                    onMemory = { memoryId -> screen = AppScreen.MemoryDetail(person.id, memoryId) },
                    onAllMemories = { screen = AppScreen.AllMemories(person.id) },
                    onAllChats = { screen = AppScreen.AllChats(person.id) },
                    onBack = { screen = AppScreen.Home },
                    onChanged = { refresh++ },
                    db = db
                )
            }
            is AppScreen.Search -> {
                val person = db.person(current.personId)
                if (person == null) screen = AppScreen.Home else SearchScreen(person = person, db = db, onBack = { screen = AppScreen.PersonDetail(person.id) }, onOpenChat = { screen = AppScreen.Chat(person.id, it, CHAT_TALK) }, onOpenMemory = { screen = AppScreen.MemoryDetail(person.id, it) })
            }
            is AppScreen.AllChats -> {
                val person = db.person(current.personId)
                if (person == null) screen = AppScreen.Home else AllChatsScreen(person = person, db = db, onBack = { screen = AppScreen.PersonDetail(person.id) }, onOpenChat = { screen = AppScreen.Chat(person.id, it, CHAT_TALK) })
            }
            is AppScreen.Calendar -> {
                val person = db.person(current.personId)
                if (person == null) screen = AppScreen.Home else CalendarScreen(person, db, onBack = { screen = AppScreen.PersonDetail(person.id) }, onOpenMemory = { screen = AppScreen.MemoryDetail(person.id, it) })
            }
            is AppScreen.AllMemories -> {
                val person = db.person(current.personId)
                if (person == null) screen = AppScreen.Home else AllMemoriesScreen(person, db, onBack = { screen = AppScreen.PersonDetail(person.id) }, onOpenMemory = { screen = AppScreen.MemoryDetail(person.id, it) })
            }
            is AppScreen.MemoryDetail -> {
                val memory = db.interaction(current.memoryId)
                val person = db.person(current.personId)
                if (person == null || memory == null) screen = AppScreen.Home else MemoryDetailScreen(memory, person, db, onBack = { screen = AppScreen.PersonDetail(person.id) })
            }
            is AppScreen.Chat -> {
                val person = db.person(current.personId)
                val session = current.sessionId?.let { db.session(it) }
                if (person == null || (current.sessionId != null && session == null)) screen = AppScreen.Home else ChatScreen(
                    person = person,
                    session = session,
                    mode = session?.mode ?: current.mode,
                    db = db,
                    context = context,
                    onBack = { screen = AppScreen.PersonDetail(person.id) }
                )
            }
            AppScreen.Settings -> SettingsScreen(
                dark = dark,
                onToggleTheme = toggleDark,
                onBack = { screen = AppScreen.Home },
                context = context,
                db = db,
                onDataChanged = { refresh++ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    people: List<Person>,
    onPerson: (Long) -> Unit,
    onSettings: () -> Unit,
    onPersonCreated: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("gene", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, shape = CircleShape, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Outlined.PersonAdd, "Add person")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Spacer(Modifier.height(18.dp))
                Text("People", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("A private memory for understanding patterns over time.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
            }
            if (people.isEmpty()) item { EmptyState(onAdd = { showAdd = true }) }
            items(people, key = { it.id }) { person -> PersonCard(person, onClick = { onPerson(person.id) }) }
            item { Spacer(Modifier.height(86.dp)) }
        }
    }
    if (showAdd) AddPersonSheet(onDismiss = { showAdd = false }, onCreate = { n, note -> showAdd = false; onPersonCreated(n, note) })
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Start with one person", style = MaterialTheme.typography.titleMedium)
            Text("Add someone you want to understand better. Capture only what you choose.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onAdd) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add person") }
        }
    }
}

@Composable
private fun PersonCard(person: Person, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text(person.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 21.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(person.name, style = MaterialTheme.typography.titleMedium)
                Text(if (person.interactionCount == 0) "No memories yet" else "${person.interactionCount} memor${if (person.interactionCount == 1) "y" else "ies"}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Outlined.MoreHoriz, "Open")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPersonSheet(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Add a person", style = MaterialTheme.typography.titleLarge)
            Text("Start a private thread of context. You can add details later.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("First impression, optional") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(onClick = { if (name.isNotBlank()) onCreate(name, note) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Begin thread") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonScreen(person: Person, memories: List<Interaction>, dark: Boolean, context: android.content.Context, onOpenChat: (Long?, String) -> Unit, onAsk: () -> Unit, onSearch: () -> Unit, onCalendar: () -> Unit, onMemory: (Long) -> Unit, onAllMemories: () -> Unit, onAllChats: () -> Unit, onBack: () -> Unit, onChanged: () -> Unit, db: GeneDatabase) {
    var capture by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("gene_settings", android.content.Context.MODE_PRIVATE) }
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
            while (true) { delay(5500); quoteIndex = (quoteIndex + 1) % quoteSet.size }
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
        if (memories.size >= 10 && (person.summary == null || memories.size >= person.summaryMemoryCount + 10)) regenerateSummary()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(person.name, style = MaterialTheme.typography.titleMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }, actions = { IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search memories and chats") }; Box { IconButton(onClick = { personaMenuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Person actions") }; DropdownMenu(expanded = personaMenuOpen, onDismissRequest = { personaMenuOpen = false }) { DropdownMenuItem(text = { Text(if (personFavorite) "Unfavorite" else "Favorite") }, leadingIcon = { Icon(if (personFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) }, onClick = { personFavorite = !personFavorite; db.setPersonFavorite(person.id, personFavorite); personaMenuOpen = false; onChanged() }); DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { personaMenuOpen = false; renameOpen = true }); DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = { personaMenuOpen = false; showDelete = true }) } } })
    }) { padding ->
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
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Summary", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { regenerateSummary() }, enabled = !summaryRefreshing) { Icon(Icons.Outlined.Refresh, "Regenerate summary") }
                            }
                            Text(displayedSummary ?: "Building a clearer picture…", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    Text("Add ${10 - memories.size} more memor${if (10 - memories.size == 1) "y" else "ies"} for a generated summary.", style = MaterialTheme.typography.bodyLarge, color = GeneGray)
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { persona.traits.forEach { trait -> Surface(color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(50), modifier = Modifier.padding(end = 2.dp)) { Text(trait, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium) } } }
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
                        Row(Modifier.clickable(onClick = onAllChats).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text("See all", color = GeneGray, style = MaterialTheme.typography.bodyMedium); Icon(Icons.Outlined.ChevronRight, "See all chats", tint = GeneGray, modifier = Modifier.size(20.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (sessions.isNotEmpty()) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    items(sessions.take(5), key = { "preview-${it.id}" }) { session -> ChatPreviewCard(session, onClick = { onOpenChat(session.id, CHAT_TALK) }) }
                    if (sessions.size > 5) item { ChatPreviewMoreCard(onClick = onAllChats) }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Memories", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (memories.size > 10) TextButton(onClick = onAllMemories) { Text("Show all") }
                    IconButton(onClick = { memoryGrid = !memoryGrid; prefs.edit().putBoolean(memoryLayoutKey, memoryGrid).apply() }) { Icon(if (memoryGrid) Icons.Outlined.ViewList else Icons.Outlined.ViewModule, if (memoryGrid) "List view" else "Grid view") }
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
    if (renameOpen) ModalBottomSheet(onDismissRequest = { renameOpen = false }, containerColor = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) { Text("Rename person", style = MaterialTheme.typography.titleLarge); OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, modifier = Modifier.fillMaxWidth()); Button(onClick = { if (renameText.isNotBlank()) { db.updatePersonName(person.id, renameText); renameOpen = false; onChanged() } }, enabled = renameText.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save name") } } }
    if (showDelete) DeletePersonSheet(person, db, onDismiss = { showDelete = false }, onDeleted = { showDelete = false; onBack() })
}

@Composable
private fun QuickActionsGrid(onCalendar: () -> Unit, onChat: () -> Unit, onAsk: () -> Unit, onSearch: () -> Unit, onRemember: () -> Unit) {
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
private fun ActionBento(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(82.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, title, tint = GeneGray, modifier = Modifier.size(22.dp)); Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun QuoteCarousel(quotes: List<Interaction>, index: Int) {
    val quote = quotes[index.coerceIn(0, quotes.lastIndex)]
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("“${quote.body.removePrefix("Quote · ").trim().trim('"')}”", style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic), textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Text("${index + 1} / ${quotes.size}  ·  ${quote.createdAt.asDate()}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun Long.asDate(): String = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(this))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryComposer(person: Person, db: GeneDatabase, onSaved: () -> Unit, onStartChat: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var secondText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TYPE_TEXT) }
    var showModes by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var audioUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val recorder = remember { AudioRecorder(context.applicationContext) }
    val modes = listOf(TYPE_TEXT to "Note", TYPE_AUDIO to "Voice", "quote" to "Quote", "signal" to "Signal", "pattern" to "Pattern", "recommendation" to "Recommend", "favorite" to "Favorite", "feeling" to "Feeling", "question" to "Open question")
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recording = recorder.start() else recording = false
    }
    DisposableEffect(Unit) { onDispose { recorder.release() } }
    fun toggleRecording() {
        if (recording) { audioUri = recorder.stop(); recording = false }
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) recording = recorder.start()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    val saveText = when (mode) { "quote" -> if (text.isBlank()) "" else "Quote · \"${text.trim()}\""; "signal" -> if (text.isBlank()) "" else "Signal · ${text.trim()}${if (secondText.isBlank()) "" else " | Possible meaning · ${secondText.trim()}"}"; "pattern" -> if (text.isBlank()) "" else "Pattern · ${text.trim()}"; "recommendation" -> if (text.isBlank()) "" else "Recommendation · ${text.trim()}"; "favorite" -> if (text.isBlank()) "" else "Favorite · ${text.trim()}"; "feeling" -> if (text.isBlank()) "" else "Feeling · ${text.trim()}"; "question" -> if (text.isBlank()) "" else "Open question · ${text.trim()}"; TYPE_AUDIO -> audioUri?.toString().orEmpty(); else -> text.trim() }
    val canSend = saveText.isNotBlank() && !recording
    LaunchedEffect(imeVisible) { if (!imeVisible) showModes = false }
    fun saveMemory() { if (!canSend) return; if (mode == TYPE_AUDIO) { val id = db.addAudioInteraction(person.id, saveText); AudioTranscriptionWorker.enqueue(context, id, android.net.Uri.parse(saveText)) } else db.addInteraction(person.id, mode, saveText); text = ""; secondText = ""; audioUri = null; showModes = false; onSaved() }
    val placeholder = when (mode) { TYPE_AUDIO -> if (recording) "Recording… tap stop when you are done" else if (audioUri != null) "Recording ready to save" else "Tap the mic to record"; "quote" -> "What did they say?"; "signal" -> "What happened?"; "pattern" -> "What keeps repeating?"; "recommendation" -> "What did they recommend?"; "favorite" -> "What do they love?"; "feeling" -> "What mood did you notice?"; "question" -> "What are you still wondering?"; else -> "Capture a memory" }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showModes) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(modes, key = { it.first }) { (value, label) -> AssistChip(onClick = { mode = value }, label = { Text(label) }, leadingIcon = { Icon(when (value) { TYPE_AUDIO -> Icons.Outlined.Mic; "quote" -> Icons.Outlined.BookmarkBorder; "signal" -> Icons.Outlined.Lightbulb; "pattern" -> Icons.Outlined.History; "recommendation" -> Icons.Outlined.Lightbulb; "favorite" -> Icons.Outlined.FavoriteBorder; "feeling" -> Icons.Outlined.WbSunny; "question" -> Icons.Outlined.HelpOutline; else -> Icons.Outlined.TextSnippet }, null) }, colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(containerColor = if (mode == value) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface, labelColor = MaterialTheme.colorScheme.onSurface, leadingIconContentColor = MaterialTheme.colorScheme.primary)) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it; showModes = true },
                        placeholder = { Text(placeholder) },
                        singleLine = mode != "signal" && mode != "pattern",
                        maxLines = 4,
                        modifier = Modifier.weight(1f).onFocusChanged { showModes = it.isFocused },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    if (mode == TYPE_AUDIO) IconButton(onClick = { toggleRecording() }) { Icon(if (recording) Icons.Outlined.Stop else Icons.Outlined.KeyboardVoice, if (recording) "Stop recording" else "Record memory") }
                }
            }
            Surface(
                onClick = { if (canSend) saveMemory() else onStartChat() },
                enabled = canSend || !recording,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (canSend) Icons.Outlined.Send else Icons.Outlined.SmartToy, if (canSend) "Save memory" else "Start a new chat")
                }
            }
        }
        if (mode == "signal" && showModes) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                OutlinedTextField(
                    value = secondText,
                    onValueChange = { secondText = it },
                    label = { Text("What might it mean? Optional") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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

@Composable
private fun ChatPreviewCard(session: ChatSession, onClick: () -> Unit) {
    Card(Modifier.size(146.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (session.favorite) Icons.Outlined.Star else Icons.Outlined.History, "Chat", tint = GeneGray, modifier = Modifier.size(22.dp))
                Text(session.title, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Text(if (session.messageCount == 0) "New chat" else "${session.messageCount} messages", color = GeneGray, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable
private fun ChatPreviewMoreCard(onClick: () -> Unit) {
    Card(Modifier.size(146.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) { Icon(Icons.Outlined.ChevronRight, "See all chats", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)); Text("Load more", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllChatsScreen(person: Person, db: GeneDatabase, onBack: () -> Unit, onOpenChat: (Long) -> Unit) {
    var refresh by remember { mutableStateOf(0) }
    val sessions = remember(refresh) { db.sessions(person.id) }
    Scaffold(topBar = { TopAppBar(title = { Text("Chats · ${person.name}", style = MaterialTheme.typography.titleMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Spacer(Modifier.height(16.dp)); Text("All chats", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(4.dp)); Text("Your saved conversations with ${person.name}.", color = GeneGray, style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(14.dp)) }
            if (sessions.isEmpty()) item { Text("No saved chats yet.", color = GeneGray, style = MaterialTheme.typography.bodyLarge) }
            items(sessions, key = { it.id }) { session -> SessionRow(session, db, onClick = { onOpenChat(session.id) }, onChanged = { refresh++ }) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(person: Person, db: GeneDatabase, onBack: () -> Unit, onOpenChat: (Long) -> Unit, onOpenMemory: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    val memories = remember(person.id) { db.interactions(person.id) }
    val sessions = db.sessions(person.id)
    val memoryHits = if (query.isBlank()) memories.take(20) else LocalRelevanceSearch.relevant(memories, query, 20)
    val sessionHits = if (query.isBlank()) sessions else sessions.filter { session -> session.title.contains(query, true) || db.messages(session.id).any { it.body.contains(query, true) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Search ${person.name}", style = MaterialTheme.typography.titleMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Search memories and chats") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") } }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp))
                Text("Chats", style = MaterialTheme.typography.titleMedium)
            }
            if (sessionHits.isEmpty()) item { Text("No chats match this search.", color = GeneGray, style = MaterialTheme.typography.bodyLarge) }
            items(sessionHits, key = { "session-${it.id}" }) { session ->
                Card(Modifier.fillMaxWidth().clickable { onOpenChat(session.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.History, "Chat", tint = GeneGray); Spacer(Modifier.width(12.dp)); Text(session.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
            item { Spacer(Modifier.height(10.dp)); Text("Memories", style = MaterialTheme.typography.titleMedium) }
            if (memoryHits.isEmpty()) item { Text("No memories match this search.", color = GeneGray, style = MaterialTheme.typography.bodyLarge) }
            items(memoryHits, key = { "memory-${it.id}" }) { memory ->
                MemoryCard(memory, onClick = { onOpenMemory(memory.id) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionRow(session: ChatSession, db: GeneDatabase, onClick: () -> Unit, onChanged: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(session.id) { mutableStateOf(session.title) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 17.dp, top = 15.dp, bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.History, "Chat session", tint = GeneGray, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (session.favorite) Icon(Icons.Outlined.Star, "Favorite", modifier = Modifier.size(18.dp))
                }
                Text(if (session.messageCount == 0) "New conversation" else "${session.messageCount} message${if (session.messageCount == 1) "" else "s"}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Session actions") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(if (session.favorite) "Unfavorite" else "Favorite") }, leadingIcon = { Icon(if (session.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) }, onClick = { db.setSessionFavorite(session.id, !session.favorite); menuOpen = false; onChanged() })
                    DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menuOpen = false; renameOpen = true })
                    DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = { db.deleteSession(session.id); menuOpen = false; onChanged() })
                }
            }
        }
    }
    if (renameOpen) {
        ModalBottomSheet(onDismissRequest = { renameOpen = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("Rename chat", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (renameText.isNotBlank()) { db.updateSessionTitle(session.id, renameText); renameOpen = false; onChanged() } }, enabled = renameText.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save name") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(person: Person, session: ChatSession?, mode: String, db: GeneDatabase, context: android.content.Context, onBack: () -> Unit) {
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
        if (result.resultCode == Activity.RESULT_OK) result.data?.getStringArrayListExtra("android.speech.extra.RESULTS")?.firstOrNull()?.let { transcript -> draft = listOf(draft.trim(), transcript).filter { it.isNotBlank() }.joinToString(" ") }
        listening = false
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) speechLauncher.launch(Intent("android.speech.action.RECOGNIZE_SPEECH").apply { putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form"); putExtra("android.speech.extra.PROMPT", "Speak your message") }) else listening = false
    }
    fun startVoice() {
        listening = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) speechLauncher.launch(Intent("android.speech.action.RECOGNIZE_SPEECH").apply { putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form"); putExtra("android.speech.extra.PROMPT", "Speak your message") }) else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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

    Scaffold(topBar = {
        TopAppBar(title = { Column(verticalArrangement = Arrangement.spacedBy(1.dp)) { Text(if (mode == CHAT_ASK) "Ask about ${person.name}" else "Talk with ${person.name}", style = MaterialTheme.typography.titleMedium); Text(if (temporary) "Temporary chat" else sessionTitle, color = GeneGray, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back to person") } }, actions = {
            if (savedSessionId == null) {
                IconButton(onClick = { temporary = !temporary; if (temporary) sessionTitle = "Temporary chat" else sessionTitle = "New conversation" }) { Icon(if (temporary) Icons.Outlined.VisibilityOff else Icons.Outlined.BookmarkBorder, if (temporary) "Temporary chat enabled" else "Make temporary") }
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
        })
    }, bottomBar = {
        Surface(Modifier.fillMaxWidth().imePadding(), color = MaterialTheme.colorScheme.background) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { startVoice() }, enabled = !sending, modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.KeyboardVoice, if (listening) "Listening" else "Voice input") }
                OutlinedTextField(value = draft, onValueChange = { draft = it }, placeholder = { Text("Say something") }, minLines = 1, maxLines = 5, modifier = Modifier.weight(1f))
                IconButton(onClick = { send() }, enabled = draft.isNotBlank() && !sending, modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.Send, "Send") }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                if (messages.isEmpty()) ChatWelcome(person.name, mode, temporary)
                if (sending) Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Start) { Text("Thinking…", color = GeneGray, style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(12.dp))
            }
            items(messages, key = { it.id }) { message -> ChatBubble(message, memoryMap, expandedMessageId == message.id) { expandedMessageId = if (expandedMessageId == message.id) null else message.id } }
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
private fun ChatWelcome(name: String, mode: String, temporary: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (mode == CHAT_ASK) "Ask about $name" else "Talk with $name", style = MaterialTheme.typography.titleLarge)
        Text(if (mode == CHAT_ASK) "Ask for a read on patterns, choices, or what may happen next." else if (temporary) "This chat disappears when you leave. Say whatever is on your mind." else "This conversation is saved with the person. Speak naturally and keep the thread.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, memoryMap: Map<Long, Interaction>, expanded: Boolean, onToggleReferences: () -> Unit) {
    val isUser = message.role == ROLE_USER
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Surface(color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, shape = RoundedCornerShape(18.dp), border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.widthIn(max = 340.dp)) {
            Column(Modifier.padding(horizontal = 17.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message.body, style = MaterialTheme.typography.bodyLarge)
                if (!isUser && message.confidence != null) {
                    Text("${confidenceLabel(message.confidence)} · ${message.confidence}% · ${if (message.referenceIds.isEmpty()) "No references" else "References"}", color = if (isUser) MaterialTheme.colorScheme.onPrimary else GeneGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable(onClick = onToggleReferences))
                    if (expanded) {
                        Divider()
                        if (message.referenceIds.isEmpty()) Text("No saved memory was used for this reply.", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                        else {
                            Text("References used", style = MaterialTheme.typography.bodyMedium)
                            message.referenceIds.mapNotNull { memoryMap[it] }.forEach { memory -> Text("· ${memory.body.take(180)}", color = GeneGray, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
    }
}

private fun confidenceLabel(value: Int): String = when {
    value >= 76 -> "High confidence"
    value >= 46 -> "Some confidence"
    else -> "Low confidence"
}

@Composable
private fun MemoryCard(memory: Interaction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = memoryTypeLabel(memory.type)
    val icon = when (memory.type) { TYPE_AUDIO -> Icons.Outlined.GraphicEq; TYPE_QUOTE -> Icons.Outlined.BookmarkBorder; "recommendation" -> Icons.Outlined.Lightbulb; "favorite" -> Icons.Outlined.FavoriteBorder; else -> Icons.Outlined.TextSnippet }
    Card(modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(icon, label, tint = GeneGray, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(9.dp)); Text(label, color = GeneGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)); Text(memory.createdAt.asDate(), color = GeneGray, style = MaterialTheme.typography.bodyMedium) }
            if (memory.type == TYPE_AUDIO) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) { listOf(18, 28, 14, 34, 22, 40, 26, 16, 31, 20, 36, 24, 13, 29).forEach { height -> Box(Modifier.width(4.dp).height(height.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))) } }
                Text(when { memory.transcriptionStatus == TRANSCRIPTION_COMPLETE && !memory.transcript.isNullOrBlank() -> memory.transcript.take(120); memory.transcriptionStatus == TRANSCRIPTION_PENDING -> "Audio saved · transcription queued"; memory.transcriptionStatus == TRANSCRIPTION_UNAVAILABLE -> "Audio saved · tap to open"; else -> "Audio saved · tap to open" }, color = if (memory.transcriptionStatus == TRANSCRIPTION_COMPLETE) MaterialTheme.colorScheme.onSurface else GeneGray, style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            } else {
                Text(memory.body.removePrefix("$label · ").trim(), style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun memoryTypeLabel(type: String): String = when (type) { TYPE_AUDIO -> "Voice"; TYPE_QUOTE -> "Quote"; TYPE_SIGNAL -> "Signal"; TYPE_PATTERN -> "Pattern"; "recommendation" -> "Recommendation"; "favorite" -> "Favorite"; "feeling" -> "Feeling"; "question" -> "Open question"; else -> "Note" }

@Composable
private fun MasonryMemoryGallery(memories: List<Interaction>, onMemory: (Long) -> Unit) {
    val columns = remember(memories) {
        val left = mutableListOf<Interaction>()
        val right = mutableListOf<Interaction>()
        var leftHeight = 0
        var rightHeight = 0
        memories.forEach { memory ->
            val estimate = 90 + (memory.body.length / 34).coerceIn(0, 5) * 24 + if (memory.type == TYPE_AUDIO) 50 else 0
            if (leftHeight <= rightHeight) { left += memory; leftHeight += estimate } else { right += memory; rightHeight += estimate }
        }
        left to right
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { columns.first.forEach { memory -> MemoryCard(memory, onClick = { onMemory(memory.id) }) } }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { columns.second.forEach { memory -> MemoryCard(memory, onClick = { onMemory(memory.id) }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllMemoriesScreen(person: Person, db: GeneDatabase, onBack: () -> Unit, onOpenMemory: (Long) -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("gene_settings", android.content.Context.MODE_PRIVATE) }
    val memoryLayoutKey = "memory_grid_${person.id}"
    var grid by remember(person.id) { mutableStateOf(prefs.getBoolean(memoryLayoutKey, true)) }
    val memories = db.interactions(person.id)
    Scaffold(topBar = { TopAppBar(title = { Text("Memories · ${person.name}", style = MaterialTheme.typography.titleMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }, actions = { IconButton(onClick = { grid = !grid; prefs.edit().putBoolean(memoryLayoutKey, grid).apply() }) { Icon(if (grid) Icons.Outlined.ViewList else Icons.Outlined.ViewModule, if (grid) "List view" else "Grid view") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Spacer(Modifier.height(16.dp)); Text("All memories", style = MaterialTheme.typography.titleLarge); Text("${memories.size} saved moments", color = GeneGray, style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(12.dp)) }
            if (!grid) items(memories, key = { it.id }) { memory -> MemoryCard(memory, onClick = { onOpenMemory(memory.id) }) }
            else items(memories.chunked(2), key = { it.firstOrNull()?.id ?: 0L }) { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { memory -> MemoryCard(memory, onClick = { onOpenMemory(memory.id) }, modifier = Modifier.weight(1f)) }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScreen(person: Person, db: GeneDatabase, onBack: () -> Unit, onOpenMemory: (Long) -> Unit) {
    var monthCursor by remember {
        mutableStateOf(java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) })
    }
    var selectedDay by remember { mutableStateOf<String?>(null) }
    val memories = remember(person.id) { db.interactions(person.id) }
    val dayFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
    val titleFormat = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()) }
    val dayLabelFormat = remember { java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()) }
    val byDay = remember(memories) { memories.groupBy { dayFormat.format(java.util.Date(it.createdAt)) } }
    val firstDay = remember(monthCursor.timeInMillis) { monthCursor.clone() as java.util.Calendar }
    val offset = (firstDay.get(java.util.Calendar.DAY_OF_WEEK) - java.util.Calendar.SUNDAY)
    val daysInMonth = firstDay.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val cells = (0 until 42).map { index ->
        val day = index - offset + 1
        if (day in 1..daysInMonth) day else null
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Calendar · ${person.name}", style = MaterialTheme.typography.titleMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthCursor = (monthCursor.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -1) } }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
                Text(titleFormat.format(monthCursor.time), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { monthCursor = (monthCursor.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, 1) } }) { Icon(Icons.Outlined.ChevronRight, "Next month") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, color = GeneGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center) } }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { day ->
                            val date = if (day == null) null else (monthCursor.clone() as java.util.Calendar).apply { set(java.util.Calendar.DAY_OF_MONTH, day) }
                            val key = date?.let { dayFormat.format(it.time) }
                            val count = key?.let { byDay[it]?.size ?: 0 } ?: 0
                            Box(Modifier.width(40.dp).height(58.dp).clickable(enabled = day != null && count > 0) { selectedDay = key }, contentAlignment = Alignment.TopCenter) {
                                if (day != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(day.toString(), style = MaterialTheme.typography.bodyLarge)
                                        if (count > 0) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                        if (count > 0) Text(count.toString(), color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (byDay.isEmpty()) Text("Saved memories will appear on their days.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            else Text("Tap a marked day to see its memories.", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
        }
    }
    selectedDay?.let { key ->
        val dayMemories = byDay[key].orEmpty()
        ModalBottomSheet(onDismissRequest = { selectedDay = null }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(dayMemories.firstOrNull()?.let { dayLabelFormat.format(java.util.Date(it.createdAt)) } ?: "Memories", style = MaterialTheme.typography.titleLarge)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 430.dp)) { items(dayMemories, key = { it.id }) { memory -> MemoryCard(memory, onClick = { selectedDay = null; onOpenMemory(memory.id) }) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryDetailScreen(memory: Interaction, person: Person, db: GeneDatabase, onBack: () -> Unit) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    DisposableEffect(memory.id) { onDispose { player?.release(); player = null } }
    Scaffold(topBar = { TopAppBar(title = { Text(memoryTypeLabel(memory.type), style = MaterialTheme.typography.titleMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }, actions = { IconButton(onClick = { db.deleteInteraction(memory.id); onBack() }) { Icon(Icons.Outlined.DeleteOutline, "Delete memory") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 22.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(14.dp))
            Text("${person.name} · ${memory.createdAt.asDate()}", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            if (memory.type == TYPE_AUDIO && memory.audioUri != null) {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { listOf(26, 42, 18, 34, 48, 22, 38, 30, 16, 40, 24, 32).forEach { height -> Box(Modifier.width(5.dp).height(height.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))) } }
                        Button(onClick = { if (playing) { player?.pause(); playing = false } else { player?.release(); player = android.media.MediaPlayer.create(context, android.net.Uri.parse(memory.audioUri)); player?.setOnCompletionListener { playing = false }; player?.start(); playing = true } }, modifier = Modifier.fillMaxWidth()) { Text(if (playing) "Pause recording" else "Play recording") }
                    }
                }
            }
            Text(if (memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_PENDING) "Transcription is still processing in the background." else memory.transcript?.takeIf { it.isNotBlank() } ?: memory.body, style = MaterialTheme.typography.titleMedium)
            if (memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_UNAVAILABLE) Text("The audio is saved on this device. Add a compatible transcription provider in Settings if you want a text transcript.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureSheet(person: Person, db: GeneDatabase, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var secondText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TYPE_TEXT) }
    var pendingSpeech by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.getStringArrayListExtra("android.speech.extra.RESULTS")?.firstOrNull()?.let { text = it }
        pendingSpeech = false
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) speechLauncher.launch(Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
            putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form")
            putExtra("android.speech.extra.PROMPT", "Speak a memory")
        }) else pendingSpeech = false
    }
    LaunchedEffect(pendingSpeech) {
        if (pendingSpeech) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) speechLauncher.launch(Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
                putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form")
                putExtra("android.speech.extra.PROMPT", "Speak a memory")
            }) else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val title = when (mode) { TYPE_AUDIO -> "Say it"; "quote" -> "Save a quote"; "signal" -> "Notice a signal"; "pattern" -> "Name a pattern"; "recommendation" -> "Save a recommendation"; "favorite" -> "Save a favorite"; "feeling" -> "Notice a feeling"; "question" -> "Keep an open question"; else -> "Capture something" }
    val saveText = when (mode) { "quote" -> if (text.isBlank()) "" else "Quote · \"${text.trim()}\""; "signal" -> if (text.isBlank()) "" else "Signal · ${text.trim()}${if (secondText.isBlank()) "" else " | Possible meaning · ${secondText.trim()}"}"; "pattern" -> if (text.isBlank()) "" else "Pattern · ${text.trim()}"; "recommendation" -> if (text.isBlank()) "" else "Recommendation · ${text.trim()}"; "favorite" -> if (text.isBlank()) "" else "Favorite · ${text.trim()}"; "feeling" -> if (text.isBlank()) "" else "Feeling · ${text.trim()}"; "question" -> if (text.isBlank()) "" else "Open question · ${text.trim()}"; else -> text.trim() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
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
                    OutlinedButton(onClick = { pendingSpeech = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Mic, null); Spacer(Modifier.width(8.dp)); Text("Speak now") }
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
            Button(onClick = { if (saveText.isNotBlank()) { db.addInteraction(person.id, mode, saveText); onSaved() } }, enabled = saveText.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Keep memory") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeletePersonSheet(person: Person, db: GeneDatabase, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Text("Remove ${person.name}?", style = MaterialTheme.typography.titleLarge)
            Text("This removes the person and every saved memory from this device. This cannot be undone.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { db.deletePerson(person.id); onDeleted() }, modifier = Modifier.fillMaxWidth()) { Text("Remove person") }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Keep person") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    context: android.content.Context,
    db: GeneDatabase,
    onDataChanged: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("gene_settings", android.content.Context.MODE_PRIVATE) }
    var bubble by remember { mutableStateOf(prefs.getBoolean("bubble_enabled", false)) }
    var endpoint by remember { mutableStateOf(prefs.getString("llm_endpoint", "").orEmpty()) }
    var apiKey by remember { mutableStateOf(prefs.getString("llm_api_key", "").orEmpty()) }
    var model by remember { mutableStateOf(prefs.getString("llm_model", "gpt-5-mini").orEmpty()) }
    var saved by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var pendingImportSummary by remember { mutableStateOf<ImportSummary?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch(Dispatchers.IO) {
                try {
                    val json = db.exportToJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Could not open file for writing")
                    launch(Dispatchers.Main) {
                        isExporting = false
                        Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        isExporting = false
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            scope.launch(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: throw IllegalStateException("Could not read selected file")
                    val summary = db.parseBackupSummary(content)
                    launch(Dispatchers.Main) {
                        isImporting = false
                        if (summary == null) {
                            Toast.makeText(context, "Invalid Gene backup file", Toast.LENGTH_SHORT).show()
                        } else {
                            pendingImportContent = content
                            pendingImportSummary = summary
                            showImportDialog = true
                        }
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        isImporting = false
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.titleMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item { Spacer(Modifier.height(18.dp)); SettingsSectionTitle("Appearance", "Keep Gene quiet and focused") }
            item { SettingsCard { SettingRow("Dark interface", "Pure black background", dark, onToggleTheme) } }
            item { Spacer(Modifier.height(28.dp)); SettingsSectionTitle("Quick capture", "Save a thought without leaving another app") }
            item { SettingsCard {
                SettingRow("Floating bubble", "Capture a note from any screen", bubble) {
                    val next = !bubble
                    bubble = next
                    prefs.edit().putBoolean("bubble_enabled", next).apply()
                    if (next && Settings.canDrawOverlays(context)) context.startService(Intent(context, FloatingCaptureService::class.java)) else if (!next) context.stopService(Intent(context, FloatingCaptureService::class.java))
                }
                if (bubble && !Settings.canDrawOverlays(context)) { Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }) { Text("Allow overlay access") } }
            } }
            item { Spacer(Modifier.height(28.dp)); SettingsSectionTitle("Intelligence", "Local by default, remote only when you choose it") }
            item { SettingsCard {
                Text("Leave the endpoint and key blank to use Gene's offline reflection engine. Otherwise enter either an OpenAI-compatible /v1 base URL or a full /v1/chat/completions URL.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = endpoint, onValueChange = { endpoint = it; saved = false }, label = { Text("Endpoint URL") }, placeholder = { Text("https://.../v1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = model, onValueChange = { model = it; saved = false }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it; saved = false }, label = { Text("API key") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(14.dp))
                Button(onClick = { prefs.edit().putString("llm_endpoint", endpoint.trim()).putString("llm_model", model.trim()).putString("llm_api_key", apiKey.trim()).apply(); saved = true }, modifier = Modifier.fillMaxWidth()) { Text(if (saved) "Saved" else "Save intelligence settings") }
            } }
            item { Spacer(Modifier.height(28.dp)); SettingsSectionTitle("Data & Backup", "Export your memories or restore from a file") }
            item { SettingsCard {
                Text("Export creates a complete JSON backup containing all people, memories, and conversations. Import restores or merges records back into Gene.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                            exportLauncher.launch("gene_backup_$timestamp.json")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isExporting && !isImporting
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isExporting) "Exporting..." else "Export data")
                    }
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isExporting && !isImporting
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isImporting) "Reading..." else "Import data")
                    }
                }
            } }
            item { Spacer(Modifier.height(28.dp)); SettingsSectionTitle("Privacy", "Your context stays yours") }
            item { SettingsCard {
                Text("Gene saves people, notes, transcripts, sessions, and message references locally on this device. It does not record continuously. Audio is used only to create a transcript when you choose Audio memory.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text("Remote analysis is opt-in. When enabled, only locally selected context is sent to the endpoint you entered. Persona replies are working hypotheses, not certainty about another person's private thoughts.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            } }
            item { Spacer(Modifier.height(34.dp)) }
        }
    }

    if (showImportDialog && pendingImportSummary != null) {
        val summary = pendingImportSummary!!
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportContent = null
                pendingImportSummary = null
            },
            title = { Text("Import data", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This backup contains:", style = MaterialTheme.typography.bodyMedium)
                    Text("• ${summary.peopleCount} ${if (summary.peopleCount == 1) "person" else "people"}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                    Text("• ${summary.memoryCount} ${if (summary.memoryCount == 1) "memory" else "memories"}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                    Text("• ${summary.sessionCount} ${if (summary.sessionCount == 1) "conversation" else "conversations"} (${summary.messageCount} messages)", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("How would you like to import this backup?", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val content = pendingImportContent ?: return@Button
                    showImportDialog = false
                    scope.launch(Dispatchers.IO) {
                        try {
                            val result = db.importFromJson(content, replaceExisting = false)
                            launch(Dispatchers.Main) {
                                onDataChanged()
                                Toast.makeText(context, "Merged ${result.peopleCount} people and ${result.memoryCount} memories", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            pendingImportContent = null
                            pendingImportSummary = null
                        }
                    }
                }) {
                    Text("Merge")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showImportDialog = false
                        pendingImportContent = null
                        pendingImportSummary = null
                    }) {
                        Text("Cancel")
                    }
                    OutlinedButton(onClick = {
                        val content = pendingImportContent ?: return@OutlinedButton
                        showImportDialog = false
                        scope.launch(Dispatchers.IO) {
                            try {
                                val result = db.importFromJson(content, replaceExisting = true)
                                launch(Dispatchers.Main) {
                                    onDataChanged()
                                    Toast.makeText(context, "Restored ${result.peopleCount} people and ${result.memoryCount} memories", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                pendingImportContent = null
                                pendingImportSummary = null
                            }
                        }
                    }) {
                        Text("Replace all")
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = GeneGray, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content) }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChecked: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(subtitle, color = GeneGray, style = MaterialTheme.typography.bodyMedium) }
        Switch(checked = checked, onCheckedChange = { onChecked() })
    }
}

