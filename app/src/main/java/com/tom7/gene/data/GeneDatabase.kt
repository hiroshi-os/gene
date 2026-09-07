package com.tom7.gene.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

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

data class Person(
    val id: Long,
    val name: String,
    val note: String,
    val createdAt: Long,
    val lastSeen: Long,
    val interactionCount: Int,
    val summary: String?,
    val summaryMemoryCount: Int,
    val favorite: Boolean = false,
    val avatar: String? = null,
    val isSelf: Boolean = false
)
data class Interaction(val id: Long, val personId: Long, val type: String, val body: String, val createdAt: Long, val audioUri: String? = null, val transcript: String? = null, val transcriptionStatus: String = TRANSCRIPTION_NONE)
data class ChatSession(
    val id: Long,
    val personId: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int,
    val favorite: Boolean,
    val mode: String = "talk",
    val isGroup: Boolean = false
)
data class SessionWithPerson(
    val session: ChatSession,
    val personName: String,
    val personAvatar: String?,
    val memberNames: List<String> = emptyList()
)
data class ChatMessage(
    val id: Long,
    val sessionId: Long,
    val role: String,
    val body: String,
    val createdAt: Long,
    val confidence: Int?,
    val referenceIds: List<Long>,
    val speakerPersonId: Long? = null,
    val replyToMessageId: Long? = null
)
data class ImportSummary(val peopleCount: Int, val memoryCount: Int, val sessionCount: Int, val messageCount: Int)

/** Undirected link between two people with a relationship label (friend, sibling, …). */
data class Relationship(
    val id: Long,
    val personAId: Long,
    val personBId: Long,
    val label: String,
    val createdAt: Long
)

class GeneDatabase(context: Context) : SQLiteOpenHelper(context, "gene.db", null, 13) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        createPeopleAndMemories(db)
        createSessions(db)
        createRelationships(db)
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
        if (oldVersion < 9) db.execSQL("ALTER TABLE people ADD COLUMN avatar TEXT")
        if (oldVersion < 10) db.execSQL("ALTER TABLE people ADD COLUMN is_self INTEGER NOT NULL DEFAULT 0")
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN is_group INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE messages ADD COLUMN speaker_person_id INTEGER")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS session_members (
                    session_id INTEGER NOT NULL,
                    person_id INTEGER NOT NULL,
                    PRIMARY KEY(session_id, person_id),
                    FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE,
                    FOREIGN KEY(person_id) REFERENCES people(id) ON DELETE CASCADE
                )"""
            )
            // Seed 1:1 memberships for existing sessions
            db.execSQL(
                """INSERT OR IGNORE INTO session_members (session_id, person_id)
                   SELECT id, person_id FROM sessions WHERE is_group = 0"""
            )
        }
        if (oldVersion < 12) createRelationships(db)
        if (oldVersion < 13) {
            db.execSQL("ALTER TABLE messages ADD COLUMN reply_to_message_id INTEGER")
        }
    }

    private fun createPeopleAndMemories(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS people (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, last_seen INTEGER NOT NULL, summary TEXT, summary_memory_count INTEGER NOT NULL DEFAULT 0, favorite INTEGER NOT NULL DEFAULT 0, avatar TEXT, is_self INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS interactions (id INTEGER PRIMARY KEY AUTOINCREMENT, person_id INTEGER NOT NULL, type TEXT NOT NULL, body TEXT NOT NULL, created_at INTEGER NOT NULL, audio_uri TEXT, transcript TEXT, transcription_status TEXT NOT NULL DEFAULT 'none', FOREIGN KEY(person_id) REFERENCES people(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_interactions_person ON interactions(person_id, created_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_interactions_type ON interactions(person_id, type, created_at DESC)")
    }

    private fun createRelationships(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS relationships (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                person_a_id INTEGER NOT NULL,
                person_b_id INTEGER NOT NULL,
                label TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(person_a_id, person_b_id),
                FOREIGN KEY(person_a_id) REFERENCES people(id) ON DELETE CASCADE,
                FOREIGN KEY(person_b_id) REFERENCES people(id) ON DELETE CASCADE
            )"""
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_relationships_a ON relationships(person_a_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_relationships_b ON relationships(person_b_id)")
    }

    private fun createSessions(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, person_id INTEGER NOT NULL, title TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, favorite INTEGER NOT NULL DEFAULT 0, mode TEXT NOT NULL DEFAULT 'talk', is_group INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(person_id) REFERENCES people(id) ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER NOT NULL, role TEXT NOT NULL, body TEXT NOT NULL, created_at INTEGER NOT NULL, confidence INTEGER, reference_ids TEXT NOT NULL DEFAULT '', speaker_person_id INTEGER, reply_to_message_id INTEGER, FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS session_members (
                session_id INTEGER NOT NULL,
                person_id INTEGER NOT NULL,
                PRIMARY KEY(session_id, person_id),
                FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(person_id) REFERENCES people(id) ON DELETE CASCADE
            )"""
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_person ON sessions(person_id, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_session ON messages(session_id, created_at ASC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_session_members_person ON session_members(person_id)")
    }

    fun addPerson(name: String, note: String = "", isSelf: Boolean = false): Long {
        val now = System.currentTimeMillis()
        return writableDatabase.insert(
            "people",
            null,
            ContentValues().apply {
                put("name", name.trim())
                put("note", note.trim())
                put("created_at", now)
                put("last_seen", now)
                put("is_self", if (isSelf) 1 else 0)
            }
        )
    }

    /** Ensures a single self profile exists and returns it. */
    fun getOrCreateSelf(): Person {
        selfPerson()?.let { return it }
        val id = addPerson(name = "You", note = "Notes about yourself", isSelf = true)
        return person(id)!!
    }

    fun selfPerson(): Person? = readableDatabase.rawQuery(
        """SELECT p.id, p.name, p.note, p.created_at, p.last_seen,
           (SELECT COUNT(*) FROM interactions WHERE person_id = p.id),
           p.summary, p.summary_memory_count, p.favorite, p.avatar, p.is_self
           FROM people p WHERE p.is_self = 1 LIMIT 1""",
        null
    ).use { c -> if (c.moveToFirst()) readPerson(c) else null }

    fun setPersonFavorite(personId: Long, favorite: Boolean) { writableDatabase.update("people", ContentValues().apply { put("favorite", if (favorite) 1 else 0) }, "id = ?", arrayOf(personId.toString())) }
    fun updatePersonName(personId: Long, name: String) { writableDatabase.update("people", ContentValues().apply { put("name", name.trim()) }, "id = ?", arrayOf(personId.toString())) }

    fun deletePerson(id: Long) {
        if (person(id)?.isSelf == true) return
        writableDatabase.delete("relationships", "person_a_id = ? OR person_b_id = ?", arrayOf(id.toString(), id.toString()))
        writableDatabase.delete("interactions", "person_id = ?", arrayOf(id.toString()))
        writableDatabase.delete("sessions", "person_id = ?", arrayOf(id.toString()))
        writableDatabase.delete("people", "id = ?", arrayOf(id.toString()))
    }

    /** Upsert an undirected relationship; stores ids ordered so (a,b) == (b,a). Returns id or -1. */
    fun upsertRelationship(personId1: Long, personId2: Long, label: String): Long {
        if (personId1 == personId2) return -1L
        val clean = label.trim().ifBlank { "Connected" }.take(40)
        val a = minOf(personId1, personId2)
        val b = maxOf(personId1, personId2)
        val existing = relationshipBetween(a, b)
        if (existing != null) {
            writableDatabase.update(
                "relationships",
                ContentValues().apply { put("label", clean) },
                "id = ?",
                arrayOf(existing.id.toString())
            )
            return existing.id
        }
        return writableDatabase.insert(
            "relationships",
            null,
            ContentValues().apply {
                put("person_a_id", a)
                put("person_b_id", b)
                put("label", clean)
                put("created_at", System.currentTimeMillis())
            }
        )
    }

    fun updateRelationshipLabel(id: Long, label: String) {
        val clean = label.trim().ifBlank { "Connected" }.take(40)
        writableDatabase.update(
            "relationships",
            ContentValues().apply { put("label", clean) },
            "id = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteRelationship(id: Long) {
        writableDatabase.delete("relationships", "id = ?", arrayOf(id.toString()))
    }

    fun relationshipBetween(personId1: Long, personId2: Long): Relationship? {
        val a = minOf(personId1, personId2)
        val b = maxOf(personId1, personId2)
        return readableDatabase.rawQuery(
            "SELECT id, person_a_id, person_b_id, label, created_at FROM relationships WHERE person_a_id = ? AND person_b_id = ?",
            arrayOf(a.toString(), b.toString())
        ).use { c -> if (c.moveToFirst()) readRelationship(c) else null }
    }

    fun relationships(): List<Relationship> = readableDatabase.rawQuery(
        "SELECT id, person_a_id, person_b_id, label, created_at FROM relationships ORDER BY created_at DESC",
        null
    ).use { c -> buildList { while (c.moveToNext()) add(readRelationship(c)) } }

    fun relationshipsFor(personId: Long): List<Relationship> = readableDatabase.rawQuery(
        "SELECT id, person_a_id, person_b_id, label, created_at FROM relationships WHERE person_a_id = ? OR person_b_id = ? ORDER BY created_at DESC",
        arrayOf(personId.toString(), personId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add(readRelationship(c)) } }

    private fun readRelationship(c: android.database.Cursor): Relationship = Relationship(
        id = c.getLong(0),
        personAId = c.getLong(1),
        personBId = c.getLong(2),
        label = c.getString(3),
        createdAt = c.getLong(4)
    )

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

    fun updatePersonAvatar(personId: Long, avatar: String?) {
        writableDatabase.update("people", ContentValues().apply { put("avatar", avatar) }, "id = ?", arrayOf(personId.toString()))
    }

    fun people(includeSelf: Boolean = false): List<Person> {
        val where = if (includeSelf) "" else "WHERE p.is_self = 0"
        return readableDatabase.rawQuery(
            """SELECT p.id, p.name, p.note, p.created_at, p.last_seen, COUNT(i.id),
               p.summary, p.summary_memory_count, p.favorite, p.avatar, p.is_self
               FROM people p LEFT JOIN interactions i ON p.id = i.person_id
               $where
               GROUP BY p.id ORDER BY p.favorite DESC, p.last_seen DESC""",
            null
        ).use { c -> buildList { while (c.moveToNext()) add(readPerson(c)) } }
    }

    fun person(id: Long): Person? = readableDatabase.rawQuery(
        """SELECT p.id, p.name, p.note, p.created_at, p.last_seen,
           (SELECT COUNT(*) FROM interactions WHERE person_id = p.id),
           p.summary, p.summary_memory_count, p.favorite, p.avatar, p.is_self
           FROM people p WHERE p.id = ?""",
        arrayOf(id.toString())
    ).use { c -> if (c.moveToFirst()) readPerson(c) else null }

    private fun readPerson(c: android.database.Cursor): Person = Person(
        id = c.getLong(0),
        name = c.getString(1),
        note = c.getString(2),
        createdAt = c.getLong(3),
        lastSeen = c.getLong(4),
        interactionCount = c.getInt(5),
        summary = if (c.isNull(6)) null else c.getString(6),
        summaryMemoryCount = c.getInt(7),
        favorite = c.getInt(8) == 1,
        avatar = if (c.isNull(9)) null else c.getString(9),
        isSelf = c.columnCount > 10 && !c.isNull(10) && c.getInt(10) == 1
    )

    fun interactions(personId: Long): List<Interaction> = readableDatabase.rawQuery("SELECT id, person_id, type, body, created_at, audio_uri, transcript, transcription_status FROM interactions WHERE person_id = ? ORDER BY created_at DESC", arrayOf(personId.toString())).use { c -> buildList { while (c.moveToNext()) add(readInteraction(c)) } }

    fun latestInteraction(personId: Long): Interaction? = readableDatabase.rawQuery("SELECT id, person_id, type, body, created_at, audio_uri, transcript, transcription_status FROM interactions WHERE person_id = ? ORDER BY created_at DESC LIMIT 1", arrayOf(personId.toString())).use { c -> if (c.moveToFirst()) readInteraction(c) else null }

    fun latestInteractions(): Map<Long, Interaction> = readableDatabase.rawQuery("SELECT id, person_id, type, body, created_at, audio_uri, transcript, transcription_status FROM interactions WHERE id IN (SELECT MAX(id) FROM interactions GROUP BY person_id)", null).use { c -> buildMap { while (c.moveToNext()) { val i = readInteraction(c); put(i.personId, i) } } }

    fun allRecentInteractions(limit: Int = 50): List<Pair<Interaction, Person>> = readableDatabase.rawQuery(
        """
        SELECT i.id, i.person_id, i.type, i.body, i.created_at, i.audio_uri, i.transcript, i.transcription_status,
               p.id, p.name, p.note, p.created_at, p.last_seen, (SELECT COUNT(*) FROM interactions WHERE person_id = p.id), p.summary, p.summary_memory_count, p.favorite, p.avatar, p.is_self
        FROM interactions i
        JOIN people p ON i.person_id = p.id
        ORDER BY i.created_at DESC
        LIMIT ?
        """.trimIndent(), arrayOf(limit.toString())
    ).use { c ->
        buildList {
            while (c.moveToNext()) {
                val interaction = Interaction(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getLong(4), if (c.isNull(5)) null else c.getString(5), if (c.isNull(6)) null else c.getString(6), c.getString(7))
                val person = Person(
                    c.getLong(8), c.getString(9), c.getString(10), c.getLong(11), c.getLong(12), c.getInt(13),
                    if (c.isNull(14)) null else c.getString(14), c.getInt(15), c.getInt(16) == 1,
                    if (c.isNull(17)) null else c.getString(17), c.getInt(18) == 1
                )
                add(interaction to person)
            }
        }
    }

    fun interaction(id: Long): Interaction? = readableDatabase.rawQuery("SELECT id, person_id, type, body, created_at, audio_uri, transcript, transcription_status FROM interactions WHERE id = ?", arrayOf(id.toString())).use { c -> if (c.moveToFirst()) readInteraction(c) else null }

    private fun readInteraction(c: android.database.Cursor): Interaction = Interaction(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getLong(4), if (c.isNull(5)) null else c.getString(5), if (c.isNull(6)) null else c.getString(6), c.getString(7))

    fun addSession(personId: Long, title: String = "New conversation", mode: String = "talk"): Long {
        val now = System.currentTimeMillis()
        val id = writableDatabase.insert(
            "sessions",
            null,
            ContentValues().apply {
                put("person_id", personId)
                put("title", title.trim().ifBlank { "New conversation" })
                put("created_at", now)
                put("updated_at", now)
                put("mode", mode)
                put("is_group", 0)
            }
        )
        if (id > 0) {
            writableDatabase.insert(
                "session_members",
                null,
                ContentValues().apply {
                    put("session_id", id)
                    put("person_id", personId)
                }
            )
        }
        return id
    }

    /** Create a group chat with 2+ people. Returns session id or -1. */
    fun addGroupSession(memberIds: List<Long>, title: String = "Group chat"): Long {
        val unique = memberIds.distinct().filter { it > 0 }
        if (unique.size < 2) return -1L
        val now = System.currentTimeMillis()
        val hostId = unique.first()
        val names = unique.mapNotNull { person(it)?.name }.joinToString(", ")
        val id = writableDatabase.insert(
            "sessions",
            null,
            ContentValues().apply {
                put("person_id", hostId)
                put("title", title.trim().ifBlank { names.take(60).ifBlank { "Group chat" } })
                put("created_at", now)
                put("updated_at", now)
                put("mode", "talk")
                put("is_group", 1)
            }
        )
        if (id <= 0) return -1L
        unique.forEach { pid ->
            writableDatabase.insert(
                "session_members",
                null,
                ContentValues().apply {
                    put("session_id", id)
                    put("person_id", pid)
                }
            )
        }
        return id
    }

    fun sessionMembers(sessionId: Long): List<Person> = readableDatabase.rawQuery(
        """SELECT p.id, p.name, p.note, p.created_at, p.last_seen,
           (SELECT COUNT(*) FROM interactions WHERE person_id = p.id),
           p.summary, p.summary_memory_count, p.favorite, p.avatar, p.is_self
           FROM session_members sm
           JOIN people p ON p.id = sm.person_id
           WHERE sm.session_id = ?
           ORDER BY p.is_self DESC, p.name COLLATE NOCASE ASC""",
        arrayOf(sessionId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add(readPerson(c)) } }

    fun updateSessionTitle(sessionId: Long, title: String) { writableDatabase.update("sessions", ContentValues().apply { put("title", title.trim().take(60)); put("updated_at", System.currentTimeMillis()) }, "id = ?", arrayOf(sessionId.toString())) }
    fun setSessionFavorite(sessionId: Long, favorite: Boolean) { writableDatabase.update("sessions", ContentValues().apply { put("favorite", if (favorite) 1 else 0); put("updated_at", System.currentTimeMillis()) }, "id = ?", arrayOf(sessionId.toString())) }
    fun deleteSession(sessionId: Long) {
        writableDatabase.delete("session_members", "session_id = ?", arrayOf(sessionId.toString()))
        writableDatabase.delete("sessions", "id = ?", arrayOf(sessionId.toString()))
    }

    fun addMessage(
        sessionId: Long,
        role: String,
        body: String,
        confidence: Int? = null,
        referenceIds: List<Long> = emptyList(),
        speakerPersonId: Long? = null,
        replyToMessageId: Long? = null
    ): Long {
        val clean = body.trim()
        if (clean.isBlank()) return -1L
        val now = System.currentTimeMillis()
        return writableDatabase.insert(
            "messages",
            null,
            ContentValues().apply {
                put("session_id", sessionId)
                put("role", role)
                put("body", clean)
                if (confidence == null) putNull("confidence") else put("confidence", confidence.coerceIn(0, 100))
                put("reference_ids", referenceIds.joinToString(","))
                put("created_at", now)
                if (speakerPersonId == null) putNull("speaker_person_id") else put("speaker_person_id", speakerPersonId)
                if (replyToMessageId == null) putNull("reply_to_message_id") else put("reply_to_message_id", replyToMessageId)
            }
        ).also {
            writableDatabase.update("sessions", ContentValues().apply { put("updated_at", now) }, "id = ?", arrayOf(sessionId.toString()))
        }
    }

    private fun readSession(c: android.database.Cursor): ChatSession = ChatSession(
        id = c.getLong(0),
        personId = c.getLong(1),
        title = c.getString(2),
        createdAt = c.getLong(3),
        updatedAt = c.getLong(4),
        messageCount = c.getInt(5),
        favorite = c.getInt(6) == 1,
        mode = c.getString(7),
        isGroup = c.columnCount > 8 && c.getInt(8) == 1
    )

    fun sessions(personId: Long): List<ChatSession> = readableDatabase.rawQuery(
        """SELECT s.id, s.person_id, s.title, s.created_at, s.updated_at, COUNT(m.id), s.favorite, s.mode, s.is_group
           FROM sessions s
           LEFT JOIN messages m ON s.id = m.session_id
           WHERE s.person_id = ? OR s.id IN (SELECT session_id FROM session_members WHERE person_id = ?)
           GROUP BY s.id
           ORDER BY s.favorite DESC, s.updated_at DESC""",
        arrayOf(personId.toString(), personId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add(readSession(c)) } }

    /** All chat sessions across people, newest first. Favorites float to the top. */
    fun allSessions(): List<SessionWithPerson> = readableDatabase.rawQuery(
        """
        SELECT s.id, s.person_id, s.title, s.created_at, s.updated_at, COUNT(m.id), s.favorite, s.mode, s.is_group,
               p.name, p.avatar
        FROM sessions s
        JOIN people p ON p.id = s.person_id
        LEFT JOIN messages m ON s.id = m.session_id
        GROUP BY s.id
        ORDER BY s.favorite DESC, s.updated_at DESC
        """.trimIndent(),
        null
    ).use { c ->
        buildList {
            while (c.moveToNext()) {
                val session = readSession(c)
                val members = if (session.isGroup) sessionMembers(session.id) else emptyList()
                add(
                    SessionWithPerson(
                        session = session,
                        personName = if (session.isGroup) {
                            members.joinToString(", ") { it.name }.ifBlank { c.getString(9) }
                        } else {
                            c.getString(9)
                        },
                        personAvatar = if (c.isNull(10)) null else c.getString(10),
                        memberNames = members.map { it.name }
                    )
                )
            }
        }
    }

    fun session(id: Long): ChatSession? = readableDatabase.rawQuery(
        """SELECT s.id, s.person_id, s.title, s.created_at, s.updated_at, COUNT(m.id), s.favorite, s.mode, s.is_group
           FROM sessions s LEFT JOIN messages m ON s.id = m.session_id
           WHERE s.id = ? GROUP BY s.id""",
        arrayOf(id.toString())
    ).use { c -> if (c.moveToFirst()) readSession(c) else null }

    fun messages(sessionId: Long): List<ChatMessage> = readableDatabase.rawQuery(
        "SELECT id, session_id, role, body, created_at, confidence, reference_ids, speaker_person_id, reply_to_message_id FROM messages WHERE session_id = ? ORDER BY created_at ASC",
        arrayOf(sessionId.toString())
    ).use { c -> buildList { while (c.moveToNext()) add(readMessage(c)) } }

    fun recentMessagesFromOtherSessions(personId: Long, excludedSessionId: Long, limit: Int = 8): List<ChatMessage> =
        readableDatabase.rawQuery(
            """SELECT m.id, m.session_id, m.role, m.body, m.created_at, m.confidence, m.reference_ids, m.speaker_person_id, m.reply_to_message_id
               FROM messages m JOIN sessions s ON s.id = m.session_id
               WHERE s.person_id = ? AND s.id != ? AND s.is_group = 0
               ORDER BY m.created_at DESC LIMIT ?""",
            arrayOf(personId.toString(), excludedSessionId.toString(), limit.toString())
        ).use { c -> buildList { while (c.moveToNext()) add(readMessage(c)) }.asReversed() }

    private fun readMessage(c: android.database.Cursor): ChatMessage = ChatMessage(
        id = c.getLong(0),
        sessionId = c.getLong(1),
        role = c.getString(2),
        body = c.getString(3),
        createdAt = c.getLong(4),
        confidence = if (c.isNull(5)) null else c.getInt(5),
        referenceIds = c.getString(6).split(",").mapNotNull { it.trim().toLongOrNull() },
        speakerPersonId = if (c.columnCount > 7 && !c.isNull(7)) c.getLong(7) else null,
        replyToMessageId = if (c.columnCount > 8 && !c.isNull(8)) c.getLong(8) else null
    )

    fun exportToJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "Gene")
        root.put("exported_at", System.currentTimeMillis())

        val peopleArray = JSONArray()
        val allPeople = people(includeSelf = true)
        for (person in allPeople) {
            val pObj = JSONObject()
            pObj.put("id", person.id)
            pObj.put("name", person.name)
            pObj.put("note", person.note)
            pObj.put("created_at", person.createdAt)
            pObj.put("last_seen", person.lastSeen)
            if (person.summary != null) pObj.put("summary", person.summary) else pObj.put("summary", JSONObject.NULL)
            pObj.put("summary_memory_count", person.summaryMemoryCount)
            pObj.put("favorite", person.favorite)
            pObj.put("is_self", person.isSelf)
            if (person.avatar != null) pObj.put("avatar", person.avatar) else pObj.put("avatar", JSONObject.NULL)

            val interactionsList = interactions(person.id)
            val iArray = JSONArray()
            for (i in interactionsList) {
                val iObj = JSONObject()
                iObj.put("id", i.id)
                iObj.put("type", i.type)
                iObj.put("body", i.body)
                iObj.put("created_at", i.createdAt)
                if (i.audioUri != null) iObj.put("audio_uri", i.audioUri) else iObj.put("audio_uri", JSONObject.NULL)
                if (i.transcript != null) iObj.put("transcript", i.transcript) else iObj.put("transcript", JSONObject.NULL)
                iObj.put("transcription_status", i.transcriptionStatus)
                iArray.put(iObj)
            }
            pObj.put("interactions", iArray)

            val sessionsList = sessions(person.id)
            val sArray = JSONArray()
            for (s in sessionsList) {
                val sObj = JSONObject()
                sObj.put("id", s.id)
                sObj.put("title", s.title)
                sObj.put("created_at", s.createdAt)
                sObj.put("updated_at", s.updatedAt)
                sObj.put("favorite", s.favorite)
                sObj.put("mode", s.mode)

                val messagesList = messages(s.id)
                val mArray = JSONArray()
                for (m in messagesList) {
                    val mObj = JSONObject()
                    mObj.put("id", m.id)
                    mObj.put("role", m.role)
                    mObj.put("body", m.body)
                    mObj.put("created_at", m.createdAt)
                    if (m.confidence != null) mObj.put("confidence", m.confidence) else mObj.put("confidence", JSONObject.NULL)
                    val refArray = JSONArray()
                    m.referenceIds.forEach { refId -> refArray.put(refId) }
                    mObj.put("reference_ids", refArray)
                    mArray.put(mObj)
                }
                sObj.put("messages", mArray)
                sArray.put(sObj)
            }
            pObj.put("sessions", sArray)

            peopleArray.put(pObj)
        }
        root.put("people", peopleArray)
        return root.toString(2)
    }

    fun parseBackupSummary(jsonStr: String): ImportSummary? {
        return try {
            val root = JSONObject(jsonStr)
            val peopleArr = root.optJSONArray("people") ?: return null
            var memoriesCount = 0
            var sessionsCount = 0
            var messagesCount = 0
            for (i in 0 until peopleArr.length()) {
                val p = peopleArr.optJSONObject(i) ?: continue
                val iArr = p.optJSONArray("interactions")
                if (iArr != null) memoriesCount += iArr.length()
                val sArr = p.optJSONArray("sessions")
                if (sArr != null) {
                    sessionsCount += sArr.length()
                    for (j in 0 until sArr.length()) {
                        val s = sArr.optJSONObject(j) ?: continue
                        val mArr = s.optJSONArray("messages")
                        if (mArr != null) messagesCount += mArr.length()
                    }
                }
            }
            ImportSummary(peopleArr.length(), memoriesCount, sessionsCount, messagesCount)
        } catch (_: Exception) {
            null
        }
    }

    fun importFromJson(jsonStr: String, replaceExisting: Boolean = false): ImportSummary {
        val root = JSONObject(jsonStr)
        val peopleArr = root.optJSONArray("people") ?: throw IllegalArgumentException("Missing people data in backup")

        var totalMemories = 0
        var totalSessions = 0
        var totalMessages = 0

        val db = writableDatabase
        db.beginTransaction()
        try {
            if (replaceExisting) {
                db.delete("messages", null, null)
                db.delete("sessions", null, null)
                db.delete("interactions", null, null)
                db.delete("people", null, null)
            }

            for (i in 0 until peopleArr.length()) {
                val p = peopleArr.optJSONObject(i) ?: continue
                val pCv = ContentValues().apply {
                    put("name", p.optString("name", "Unknown"))
                    put("note", p.optString("note", ""))
                    put("created_at", p.optLong("created_at", System.currentTimeMillis()))
                    put("last_seen", p.optLong("last_seen", System.currentTimeMillis()))
                    if (p.has("summary") && !p.isNull("summary")) put("summary", p.optString("summary"))
                    put("summary_memory_count", p.optInt("summary_memory_count", 0))
                    put("favorite", if (p.optBoolean("favorite", false)) 1 else 0)
                    put("is_self", if (p.optBoolean("is_self", false)) 1 else 0)
                    if (p.has("avatar") && !p.isNull("avatar")) put("avatar", p.optString("avatar"))
                }
                val newPersonId = db.insert("people", null, pCv)
                if (newPersonId <= 0) continue

                val memoryIdMap = mutableMapOf<Long, Long>()
                val iArr = p.optJSONArray("interactions")
                if (iArr != null) {
                    for (j in 0 until iArr.length()) {
                        val item = iArr.optJSONObject(j) ?: continue
                        val oldId = item.optLong("id", -1L)
                        val iCv = ContentValues().apply {
                            put("person_id", newPersonId)
                            put("type", item.optString("type", TYPE_TEXT))
                            put("body", item.optString("body", ""))
                            put("created_at", item.optLong("created_at", System.currentTimeMillis()))
                            if (item.has("audio_uri") && !item.isNull("audio_uri")) put("audio_uri", item.optString("audio_uri"))
                            if (item.has("transcript") && !item.isNull("transcript")) put("transcript", item.optString("transcript"))
                            put("transcription_status", item.optString("transcription_status", TRANSCRIPTION_NONE))
                        }
                        val newMemoryId = db.insert("interactions", null, iCv)
                        if (newMemoryId > 0) {
                            if (oldId > 0) memoryIdMap[oldId] = newMemoryId
                            totalMemories++
                        }
                    }
                }

                val sArr = p.optJSONArray("sessions")
                if (sArr != null) {
                    for (j in 0 until sArr.length()) {
                        val s = sArr.optJSONObject(j) ?: continue
                        val sCv = ContentValues().apply {
                            put("person_id", newPersonId)
                            put("title", s.optString("title", "New conversation"))
                            put("created_at", s.optLong("created_at", System.currentTimeMillis()))
                            put("updated_at", s.optLong("updated_at", System.currentTimeMillis()))
                            put("favorite", if (s.optBoolean("favorite", false)) 1 else 0)
                            put("mode", s.optString("mode", "talk"))
                        }
                        val newSessionId = db.insert("sessions", null, sCv)
                        if (newSessionId <= 0) continue
                        totalSessions++

                        val mArr = s.optJSONArray("messages")
                        if (mArr != null) {
                            for (k in 0 until mArr.length()) {
                                val m = mArr.optJSONObject(k) ?: continue
                                val refList = mutableListOf<Long>()
                                val refArr = m.optJSONArray("reference_ids")
                                if (refArr != null) {
                                    for (r in 0 until refArr.length()) {
                                        val oldRef = refArr.optLong(r)
                                        val mapped = memoryIdMap[oldRef] ?: oldRef
                                        refList.add(mapped)
                                    }
                                } else {
                                    val refStr = m.optString("reference_ids", "")
                                    if (refStr.isNotBlank()) {
                                        refStr.split(",").forEach { token ->
                                            token.trim().toLongOrNull()?.let { oldRef ->
                                                refList.add(memoryIdMap[oldRef] ?: oldRef)
                                            }
                                        }
                                    }
                                }
                                val mCv = ContentValues().apply {
                                    put("session_id", newSessionId)
                                    put("role", m.optString("role", ROLE_USER))
                                    put("body", m.optString("body", ""))
                                    put("created_at", m.optLong("created_at", System.currentTimeMillis()))
                                    if (m.has("confidence") && !m.isNull("confidence")) {
                                        put("confidence", m.optInt("confidence"))
                                    } else {
                                        putNull("confidence")
                                    }
                                    put("reference_ids", refList.joinToString(","))
                                }
                                if (db.insert("messages", null, mCv) > 0) {
                                    totalMessages++
                                }
                            }
                        }
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ImportSummary(peopleArr.length(), totalMemories, totalSessions, totalMessages)
    }
}
