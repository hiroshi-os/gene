package com.tom7.gene

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.ui.calendar.CalendarScreen
import com.tom7.gene.ui.chat.AllChatsScreen
import com.tom7.gene.ui.chat.ChatScreen
import com.tom7.gene.ui.chat.GroupChatScreen
import com.tom7.gene.ui.graph.RelationshipGraphScreen
import com.tom7.gene.ui.home.HomeScreen
import com.tom7.gene.ui.memory.AllMemoriesScreen
import com.tom7.gene.ui.memory.MemoryDetailScreen
import com.tom7.gene.ui.navigation.AppScreen
import com.tom7.gene.ui.navigation.CHAT_ASK
import com.tom7.gene.ui.navigation.CHAT_TALK
import com.tom7.gene.ui.person.PersonScreen
import com.tom7.gene.ui.search.GlobalSearchScreen
import com.tom7.gene.ui.search.SearchScreen
import com.tom7.gene.ui.settings.SettingsScreen
import com.tom7.gene.ui.theme.GeneTheme
import com.tom7.gene.ui.theme.NotionDarkBg
import com.tom7.gene.ui.theme.NotionLightBg

@Composable
fun GeneApp(initialPersonId: Long = -1L, initialOpenCapture: Boolean = false) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("gene_settings", Context.MODE_PRIVATE) }
    val db = remember { GeneDatabase(context.applicationContext) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var screen by remember {
        mutableStateOf<AppScreen>(
            if (initialPersonId > 0) AppScreen.PersonDetail(initialPersonId, openCapture = initialOpenCapture)
            else AppScreen.Home
        )
    }
    var refresh by remember { mutableStateOf(0) }
    var homeTab by remember { mutableIntStateOf(0) }
    val people = remember(refresh) { db.people() }
    LaunchedEffect(Unit) { db.getOrCreateSelf() }
    val toggleDark = {
        val next = !dark
        dark = next
        prefs.edit().putBoolean("dark_mode", next).apply()
    }

    LaunchedEffect(screen) {
        val detail = screen as? AppScreen.PersonDetail
        if (detail != null) {
            db.person(detail.id)?.let {
                prefs.edit().putLong("selected_person_id", it.id).putString("selected_person_name", it.name).apply()
            }
        }
    }

    SideEffect {
        val window = (view.context as Activity).window
        val bar = if (dark) NotionDarkBg else NotionLightBg
        window.statusBarColor = bar.toArgb()
        window.navigationBarColor = bar.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }

    BackHandler(enabled = screen != AppScreen.Home) {
        screen = when (val current = screen) {
            is AppScreen.Chat -> AppScreen.PersonDetail(current.personId)
            is AppScreen.GroupChat -> AppScreen.Home
            is AppScreen.RelationshipGraph -> AppScreen.Home
            is AppScreen.Search -> AppScreen.PersonDetail(current.personId)
            is AppScreen.AllChats -> AppScreen.PersonDetail(current.personId)
            is AppScreen.Calendar -> AppScreen.PersonDetail(current.personId)
            is AppScreen.AllMemories -> AppScreen.PersonDetail(current.personId)
            is AppScreen.MemoryDetail -> AppScreen.PersonDetail(current.personId)
            AppScreen.GlobalSearch, is AppScreen.PersonDetail, AppScreen.Settings -> AppScreen.Home
            AppScreen.Home -> AppScreen.Home
        }
    }

    GeneTheme(darkTheme = dark) {
        when (val current = screen) {
            AppScreen.Home -> HomeScreen(
                people = people,
                dark = dark,
                selectedTab = homeTab,
                onTabSelected = { homeTab = it },
                onPerson = { screen = AppScreen.PersonDetail(it) },
                onSettings = { screen = AppScreen.Settings },
                onSearch = { screen = AppScreen.GlobalSearch },
                onSelf = {
                    val self = db.getOrCreateSelf()
                    screen = AppScreen.PersonDetail(self.id)
                },
                onPersonCreated = { name, note ->
                    val id = db.addPerson(name, note)
                    refresh++
                    screen = AppScreen.PersonDetail(id, openCapture = true)
                },
                onRefresh = { refresh++ },
                onOpenChat = { personId, sessionId ->
                    val session = sessionId?.let { db.session(it) }
                    if (session?.isGroup == true) {
                        homeTab = 1
                        screen = AppScreen.GroupChat(session.id)
                    } else {
                        screen = AppScreen.Chat(personId, sessionId, CHAT_TALK)
                    }
                },
                onOpenGroupChat = { sessionId ->
                    homeTab = 1
                    screen = AppScreen.GroupChat(sessionId)
                },
                onStartGroupChat = { memberIds ->
                    val id = db.addGroupSession(memberIds)
                    refresh++
                    if (id > 0) {
                        homeTab = 1
                        screen = AppScreen.GroupChat(id)
                    }
                },
                onOpenGraph = { screen = AppScreen.RelationshipGraph },
                onCapturePerson = { personId ->
                    screen = AppScreen.PersonDetail(personId, openCapture = true)
                },
                onAskPerson = { personId ->
                    screen = AppScreen.Chat(personId, null, CHAT_ASK)
                },
                db = db
            )
            AppScreen.GlobalSearch -> GlobalSearchScreen(
                db = db,
                onBack = { screen = AppScreen.Home },
                onOpenPerson = { screen = AppScreen.PersonDetail(it) },
                onOpenChat = { personId, sessionId ->
                    val session = db.session(sessionId)
                    if (session?.isGroup == true) {
                        homeTab = 1
                        screen = AppScreen.GroupChat(sessionId)
                    } else screen = AppScreen.Chat(personId, sessionId, CHAT_TALK)
                },
                onOpenMemory = { personId, memoryId ->
                    screen = AppScreen.MemoryDetail(personId, memoryId)
                }
            )
            is AppScreen.PersonDetail -> {
                val person = db.person(current.id)
                if (person == null) {
                    screen = AppScreen.Home
                } else {
                    PersonScreen(
                        person = person,
                        memories = db.interactions(person.id),
                        dark = dark,
                        context = context,
                        openCapture = current.openCapture,
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
            }
            is AppScreen.Search -> {
                val person = db.person(current.personId)
                if (person == null) {
                    screen = AppScreen.Home
                } else {
                    SearchScreen(
                        person = person,
                        db = db,
                        onBack = { screen = AppScreen.PersonDetail(person.id) },
                        onOpenChat = { screen = AppScreen.Chat(person.id, it, CHAT_TALK) },
                        onOpenMemory = { screen = AppScreen.MemoryDetail(person.id, it) }
                    )
                }
            }
            is AppScreen.AllChats -> {
                val person = db.person(current.personId)
                if (person == null) {
                    screen = AppScreen.Home
                } else {
                    AllChatsScreen(
                        person = person,
                        db = db,
                        onBack = { screen = AppScreen.PersonDetail(person.id) },
                        onOpenChat = { screen = AppScreen.Chat(person.id, it, CHAT_TALK) }
                    )
                }
            }
            is AppScreen.Calendar -> {
                val person = db.person(current.personId)
                if (person == null) {
                    screen = AppScreen.Home
                } else {
                    CalendarScreen(
                        person = person,
                        db = db,
                        onBack = { screen = AppScreen.PersonDetail(person.id) },
                        onOpenMemory = { screen = AppScreen.MemoryDetail(person.id, it) }
                    )
                }
            }
            is AppScreen.AllMemories -> {
                val person = db.person(current.personId)
                if (person == null) {
                    screen = AppScreen.Home
                } else {
                    AllMemoriesScreen(
                        person = person,
                        db = db,
                        onBack = { screen = AppScreen.PersonDetail(person.id) },
                        onOpenMemory = { screen = AppScreen.MemoryDetail(person.id, it) }
                    )
                }
            }
            is AppScreen.MemoryDetail -> {
                val memory = db.interaction(current.memoryId)
                val person = db.person(current.personId)
                if (person == null || memory == null) {
                    screen = AppScreen.Home
                } else {
                    MemoryDetailScreen(
                        memory = memory,
                        person = person,
                        db = db,
                        onBack = { screen = AppScreen.PersonDetail(person.id) }
                    )
                }
            }
            is AppScreen.Chat -> {
                val person = db.person(current.personId)
                val session = current.sessionId?.let { db.session(it) }
                if (person == null || (current.sessionId != null && session == null)) {
                    screen = AppScreen.Home
                } else if (session?.isGroup == true) {
                    screen = AppScreen.GroupChat(session.id)
                } else {
                    ChatScreen(
                        person = person,
                        session = session,
                        mode = session?.mode ?: current.mode,
                        db = db,
                        context = context,
                        onBack = { screen = AppScreen.PersonDetail(person.id) }
                    )
                }
            }
            is AppScreen.GroupChat -> {
                val session = db.session(current.sessionId)
                if (session == null || !session.isGroup) {
                    screen = AppScreen.Home
                } else {
                    GroupChatScreen(
                        session = session,
                        db = db,
                        context = context,
                        onBack = {
                            refresh++
                            screen = AppScreen.Home
                        }
                    )
                }
            }
            AppScreen.RelationshipGraph -> RelationshipGraphScreen(
                db = db,
                onBack = {
                    refresh++
                    screen = AppScreen.Home
                },
                onOpenPerson = { personId ->
                    screen = AppScreen.PersonDetail(personId)
                }
            )
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
