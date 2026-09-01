package com.gene.app.ui.navigation

const val CHAT_TALK = "talk"
const val CHAT_ASK = "ask"

sealed interface AppScreen {
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
