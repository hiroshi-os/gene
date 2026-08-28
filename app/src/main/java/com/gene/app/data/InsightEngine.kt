package com.gene.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PersonaSnapshot(val headline: String, val traits: List<String>, val patterns: List<String>, val evidence: List<String>)
data class PersonaReply(val text: String, val confidence: Int, val referenceIds: List<Long>)
data class InsightAnswer(val answer: String, val basis: String, val caution: String)

object InsightEngine {
    fun persona(person: Person, memories: List<Interaction>): PersonaSnapshot {
        if (memories.isEmpty()) return PersonaSnapshot("Not enough context yet", listOf("Add a few observations"), listOf("The profile will become clearer over time"), emptyList())
        val text = memories.joinToString(" ") { it.body.lowercase() }
        val traits = mutableListOf<String>()
        val patterns = mutableListOf<String>()
        if (listOf("busy", "work", "deadline", "class").any(text::contains)) traits += "Often has a full schedule"
        if (listOf("family", "friend", "weekend", "home").any(text::contains)) traits += "Talks about close relationships"
        if (listOf("fun", "laugh", "joke", "music", "movie").any(text::contains)) traits += "Responds to shared interests"
        if (traits.isEmpty()) traits += "Personality is still emerging"
        val audioCount = memories.count { it.type == TYPE_AUDIO }
        if (audioCount > 0) patterns += "$audioCount audio note${if (audioCount == 1) "" else "s"} captured"
        patterns += "${memories.size} interaction${if (memories.size == 1) "" else "s"} recorded"
        return PersonaSnapshot("A working model based on ${memories.size} memory${if (memories.size == 1) "" else "ies"}", traits.take(3), patterns, memories.take(3).map { "${formatDate(it.createdAt)} · ${it.body.take(100)}" })
    }

    fun generatedSummary(person: Person, memories: List<Interaction>): String {
        val snapshot = persona(person, memories)
        val tone = snapshot.traits.take(2).joinToString(" and ")
        val pattern = snapshot.patterns.firstOrNull().orEmpty()
        return "${person.name} comes across as $tone. $pattern. This is a working read from your notes, not a complete picture."
    }

    fun personaReply(person: Person, memories: List<Interaction>, question: String): PersonaReply {
        val ids = memories.map { it.id }
        val q = question.trim().lowercase()
        if (q in setOf("hi", "hello", "hey", "yo")) return PersonaReply("Hey. What's up?", 20, emptyList())
        if (memories.isEmpty()) return PersonaReply("I don't know enough yet. Tell me what happened.", 10, emptyList())
        val recent = memories.first().body.take(180)
        val text = when {
            listOf("like me", "interested", "crush", "feel about me", "romantic").any(q::contains) -> "I can't know that for sure. From what you've told me, there may be warmth, but I'd need a clearer, more direct moment to say more."
            listOf("what should", "should i", "what do i").any(q::contains) -> "I'd keep it simple and direct. Say what you mean, leave room for an honest answer, and notice whether the effort feels mutual."
            listOf("what happened", "what stands out", "who am i").any(q::contains) -> "What stands out is ${recent.lowercase()}. That's the clearest thread I have right now."
            else -> "I’d probably say ${recent.lowercase()}. What do you think?"
        }
        return PersonaReply(text, LocalRelevanceSearch.confidence(memories, question), ids)
    }

    fun askAbout(person: Person, memories: List<Interaction>, question: String): PersonaReply {
        if (memories.isEmpty()) return PersonaReply("There isn’t enough here yet to make a useful read.", 10, emptyList())
        val relevant = LocalRelevanceSearch.relevant(memories, question, 3)
        val strongest = relevant.firstOrNull()?.let { it.transcript ?: it.body }.orEmpty().take(150)
        val q = question.lowercase()
        val text = when {
            listOf("like me", "interested", "feel about me", "romantic").any(q::contains) -> "There are signs of warmth in the notes, but they do not prove romantic interest. A direct, low-pressure conversation would tell you more."
            listOf("what should", "should i", "what do i", "next").any(q::contains) -> "The clearest next step is ${strongest.lowercase()}. Keep it simple and leave room for an honest response."
            else -> "The strongest signal I have is ${strongest.lowercase()}. That is evidence, not a complete picture."
        }
        return PersonaReply(text, LocalRelevanceSearch.confidence(relevant, question), relevant.map { it.id })
    }

    fun answer(person: Person, memories: List<Interaction>, question: String): InsightAnswer {
        val reply = personaReply(person, memories, question)
        return InsightAnswer(reply.text, if (reply.referenceIds.isEmpty()) "No saved memory used." else "${reply.referenceIds.size} saved memor${if (reply.referenceIds.size == 1) "y" else "ies"} used.", "Confidence ${reply.confidence}% · This is a reflection, not certainty.")
    }

    private fun formatDate(time: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
}
