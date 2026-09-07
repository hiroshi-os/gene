package com.tom7.gene.ui.navigation

const val CHAT_TALK = "talk"
const val CHAT_ASK = "ask"

sealed interface AppScreen {
    data object Home : AppScreen
    /** Global search across people, chats, and memories. */
    data object GlobalSearch : AppScreen
    /** Obsidian-style people relationship graph. */
    data object RelationshipGraph : AppScreen
    /** [openCapture] jumps straight into memory capture after picking a person. */
    data class PersonDetail(val id: Long, val openCapture: Boolean = false) : AppScreen
    data class Chat(val personId: Long, val sessionId: Long?, val mode: String = CHAT_TALK) : AppScreen
    data class GroupChat(val sessionId: Long) : AppScreen
    data class Search(val personId: Long) : AppScreen
    data class AllChats(val personId: Long) : AppScreen
    data class Calendar(val personId: Long) : AppScreen
    data class MemoryDetail(val personId: Long, val memoryId: Long) : AppScreen
    data class AllMemories(val personId: Long) : AppScreen
    data object Settings : AppScreen
}
