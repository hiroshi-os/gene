package com.gene.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

const val TYPE_TEXT = "text"
const val TYPE_AUDIO = "audio"
const val TYPE_QUOTE = "quote"
const val TYPE_SIGNAL = "signal"
const val TYPE_PATTERN = "pattern"
const val ROLE_USER = "user"
const val ROLE_ASSISTANT = "assistant"
const val TRANSCRIPTION_NONE = "none"
const val TRANSCRIPTION_PENDING = "pending"
const val TRANSCRIPTION_COMPLETE = "complete"
const val TRANSCRIPTION_UNAVAILABLE = "unavailable"

data class Person(val id: Long, val name: String, val note: String, val createdAt: Long, val lastSeen: Long, val interactionCount: Int, val summary: String?, val summaryMemoryCount: Int, val favorite: Boolean = false)
data class Interaction(val id: Long, val personId: Long, val type: String, val body: String, val createdAt: Long, val audioUri: String? = null, val transcript: String? = null, val transcriptionStatus: String = TRANSCRIPTION_NONE)
data class ChatSession(val id: Long, val personId: Long, val title: String, val createdAt: Long, val updatedAt: Long, val messageCount: Int, val favorite: Boolean, val mode: String = "talk")
data class ChatMessage(val id: Long, val sessionId: Long, val role: String, val body: String, val createdAt: Long, val confidence: Int?, val referenceIds: List<Long>)

class GeneDatabase(context: Context) : SQLiteOpenHelper(context, "gene.db", null, 8) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        createPeopleAndMemories(db)
        createSessions(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createSessions(db)
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE messages ADD COLUMN confidence INTEGER")
            db.execSQL("ALTER TABLE messages ADD COLUMN reference_ids TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 4) db.execSQL("ALTER TABLE sessions ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE people ADD COLUMN summary TEXT")
            db.execSQL("ALTER TABLE people ADD COLUMN summary_memory_count INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE interactions ADD COLUMN audio_uri TEXT")
            db.execSQL("ALTER TABLE interactions ADD COLUMN transcript TEXT")
            db.execSQL("ALTER TABLE interactions ADD COLUMN transcription_status TEXT NOT NULL DEFAULT 'none'")
        }
        if (oldVersion < 7) db.execSQL("ALTER TABLE sessions ADD COLUMN mode TEXT NOT NULL DEFAULT 'talk'")
        if (oldVersion < 8) db.execSQL("ALTER TABLE people ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
    }

    private fun createPeopleAndMemories(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS people (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, last_seen INTEGER NOT NULL, summary TEXT, summary_memory_count INTEGER NOT NULL DEFAULT 0, favorite INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS interactions (id INTEGER PRIMARY KEY AUTOINCREMENT, person_id INTEGER NOT NULL, type TEXT NOT NULL, body TEXT NOT NULL, created_at INTEGER NOT NULL, audio_uri TEXT, transcript TEXT, transcription_status TEXT NOT NULL DEFAULT 'none', FOREIGN KEY(person_id) REFERENCES people(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_interactions_person ON interactions(person_id, created_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_interactions_type ON interactions(person_id, type, created_at DESC)")
    }

    private fun createSessions(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, person_id INTEGER NOT NULL, title TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, favorite INTEGER NOT NULL DEFAULT 0, mode TEXT NOT NULL DEFAULT 'talk', FOREIGN KEY(person_id) REFERENCES people(id) ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER NOT NULL, role TEXT NOT NULL, body TEXT NOT NULL, created_at INTEGER NOT NULL, confidence INTEGER, reference_ids TEXT NOT NULL DEFAULT '', FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_person ON sessions(person_id, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_session ON messages(session_id, created_at ASC)")
    }

    fun addPerson(name: String, note: String = ""): Long {
        val now = System.currentTimeMillis()
        return writableDatabase.insert("people", null, ContentValues().apply { put("name", name.trim()); put("note", note.trim()); put("created_at", now); put("last_seen", now) })
    }

    fun setPersonFavorite(personId: Long, favorite: Boolean) { writableDatabase.update("people", ContentValues().apply { put("favorite", if (favorite) 1 else 0) }, "id = ?", arrayOf(personId.toString())) }
    fun updatePersonName(personId: Long, name: String) { writableDatabase.update("people", ContentValues().apply { put("name", name.trim()) }, "id = ?", arrayOf(personId.toString())) }

    fun deletePerson(id: Long) {
        writableDatabase.delete("interactions", "person_id = ?", arrayOf(id.toString()))
        writableDatabase.delete("sessions", "person_id = ?", arrayOf(id.toString()))
        writableDatabase.delete("people", "id = ?", arrayOf(id.toString()))
    }

    fun addInteraction(personId: Long, type: String, body: String): Long {
        val clean = body.trim()
        if (clean.isBlank()) return -1L
        val now = System.currentTimeMillis()
        return writableDatabase.insert("interactions", null, ContentValues().apply { put("person_id", personId); put("type", type); put("body", clean); put("created_at", now); put("transcription_status", TRANSCRIPTION_NONE) }).also { touchPerson(personId, now) }
    }

    fun addAudioInteraction(personId: Long, audioUri: String): Long {
        val now = System.currentTimeMillis()
        return writableDatabase.insert("interactions", null, ContentValues().apply { put("person_id", personId); put("type", TYPE_AUDIO); put("body", "Audio recording"); put("created_at", now); put("audio_uri", audioUri); put("transcription_status", TRANSCRIPTION_PENDING) }).also { touchPerson(personId, now) }
    }

    fun updateTranscription(interactionId: Long, transcript: String?, status: String) {
        val clean = transcript?.trim().orEmpty()
        writableDatabase.update("interactions", ContentValues().apply {
            put("transcript", if (clean.isBlank()) null else clean)
            put("body", if (clean.isBlank()) "Audio recording" else clean)
            put("transcription_status", status)
        }, "id = ?", arrayOf(interactionId.toString()))
    }

    fun deleteInteraction(id: Long) { writableDatabase.delete("interactions", "id = ?", arrayOf(id.toString())) }

    private fun touchPerson(personId: Long, now: Long) { writableDatabase.update("people", ContentValues().apply { put("last_seen", now) }, "id = ?", arrayOf(personId.toString())) }

    fun saveSummary(personId: Long, summary: String, memoryCount: Int) {
        writableDatabase.update("people", ContentValues().apply { put("summary", summary.trim()); put("summary_memory_count", memoryCount) }, "id = ?", arrayOf(personId.toString()))
    }

    fun people(): List<Person> = readableDatabase.rawQuery("SELECT p.id, p.name, p.note, p.created_at, p.last_seen, COUNT(i.id), p.summary, p.summary_memory_count, p.favorite FROM people p LEFT JOIN interactions i ON p.id = i.person_id GROUP BY p.id ORDER BY p.favorite DESC, p.last_seen DESC", null).use { c -> buildList { while (c.moveToNext()) add(readPerson(c)) } }

    fun person(id: Long): Person? = readableDatabase.rawQuery("SELECT p.id, p.name, p.note, p.created_at, p.last_seen, (SELECT COUNT(*) FROM interactions WHERE person_id = p.id), p.summary, p.summary_memory_count, p.favorite FROM people p WHERE p.id = ?", arrayOf(id.toString())).use { c -> if (c.moveToFirst()) readPerson(c) else null }

    private fun readPerson(c: android.database.Cursor): Person = Person(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4), c.getInt(5), if (c.isNull(6)) null else c.getString(6), c.getInt(7), c.getInt(8) == 1)

    fun interactions(personId: Long): List<Interaction> = readableDatabase.rawQuery("SELECT id, person_id, type, body, created_at, audio_uri, transcript, transcription_status FROM interactions WHERE person_id = ? ORDER BY created_at DESC", arrayOf(personId.toString())).use { c -> buildList { while (c.moveToNext()) add(readInteraction(c)) } }

    fun interaction(id: Long): Interaction? = readableDatabase.rawQuery("SELECT id, person_id, type, body, created_at, audio_uri, transcript, transcription_status FROM interactions WHERE id = ?", arrayOf(id.toString())).use { c -> if (c.moveToFirst()) readInteraction(c) else null }

    private fun readInteraction(c: android.database.Cursor): Interaction = Interaction(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getLong(4), if (c.isNull(5)) null else c.getString(5), if (c.isNull(6)) null else c.getString(6), c.getString(7))

    fun addSession(personId: Long, title: String = "New conversation", mode: String = "talk"): Long {
        val now = System.currentTimeMillis()
        return writableDatabase.insert("sessions", null, ContentValues().apply { put("person_id", personId); put("title", title.trim().ifBlank { "New conversation" }); put("created_at", now); put("updated_at", now); put("mode", mode) })
    }

    fun updateSessionTitle(sessionId: Long, title: String) { writableDatabase.update("sessions", ContentValues().apply { put("title", title.trim().take(60)); put("updated_at", System.currentTimeMillis()) }, "id = ?", arrayOf(sessionId.toString())) }
    fun setSessionFavorite(sessionId: Long, favorite: Boolean) { writableDatabase.update("sessions", ContentValues().apply { put("favorite", if (favorite) 1 else 0); put("updated_at", System.currentTimeMillis()) }, "id = ?", arrayOf(sessionId.toString())) }
    fun deleteSession(sessionId: Long) { writableDatabase.delete("sessions", "id = ?", arrayOf(sessionId.toString())) }

    fun addMessage(sessionId: Long, role: String, body: String, confidence: Int? = null, referenceIds: List<Long> = emptyList()): Long {
        val clean = body.trim()
        if (clean.isBlank()) return -1L
        val now = System.currentTimeMillis()
        return writableDatabase.insert("messages", null, ContentValues().apply { put("session_id", sessionId); put("role", role); put("body", clean); if (confidence == null) putNull("confidence") else put("confidence", confidence.coerceIn(0, 100)); put("reference_ids", referenceIds.joinToString(",")); put("created_at", now) }).also { writableDatabase.update("sessions", ContentValues().apply { put("updated_at", now) }, "id = ?", arrayOf(sessionId.toString())) }
    }

    fun sessions(personId: Long): List<ChatSession> = readableDatabase.rawQuery("SELECT s.id, s.person_id, s.title, s.created_at, s.updated_at, COUNT(m.id), s.favorite, s.mode FROM sessions s LEFT JOIN messages m ON s.id = m.session_id WHERE s.person_id = ? GROUP BY s.id ORDER BY s.favorite DESC, s.updated_at DESC", arrayOf(personId.toString())).use { c -> buildList { while (c.moveToNext()) add(ChatSession(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getLong(4), c.getInt(5), c.getInt(6) == 1, c.getString(7))) } }
    fun session(id: Long): ChatSession? = readableDatabase.rawQuery("SELECT s.id, s.person_id, s.title, s.created_at, s.updated_at, COUNT(m.id), s.favorite, s.mode FROM sessions s LEFT JOIN messages m ON s.id = m.session_id WHERE s.id = ? GROUP BY s.id", arrayOf(id.toString())).use { c -> if (c.moveToFirst()) ChatSession(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getLong(4), c.getInt(5), c.getInt(6) == 1, c.getString(7)) else null }
    fun messages(sessionId: Long): List<ChatMessage> = readableDatabase.rawQuery("SELECT id, session_id, role, body, created_at, confidence, reference_ids FROM messages WHERE session_id = ? ORDER BY created_at ASC", arrayOf(sessionId.toString())).use { c -> buildList { while (c.moveToNext()) add(readMessage(c)) } }
    fun recentMessagesFromOtherSessions(personId: Long, excludedSessionId: Long, limit: Int = 8): List<ChatMessage> = readableDatabase.rawQuery("SELECT m.id, m.session_id, m.role, m.body, m.created_at, m.confidence, m.reference_ids FROM messages m JOIN sessions s ON s.id = m.session_id WHERE s.person_id = ? AND s.id != ? ORDER BY m.created_at DESC LIMIT ?", arrayOf(personId.toString(), excludedSessionId.toString(), limit.toString())).use { c -> buildList { while (c.moveToNext()) add(readMessage(c)) }.asReversed() }
    private fun readMessage(c: android.database.Cursor): ChatMessage = ChatMessage(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getLong(4), if (c.isNull(5)) null else c.getInt(5), c.getString(6).split(",").mapNotNull { it.trim().toLongOrNull() })
}
