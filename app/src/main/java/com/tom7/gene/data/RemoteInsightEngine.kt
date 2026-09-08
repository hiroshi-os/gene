package com.tom7.gene.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object RemoteInsightEngine {
    private const val CONFIGURE_HINT =
        "Open Settings → Intelligence and choose Gene AI, or set up BYOK with your own endpoint and API key."

    suspend fun personaReply(
        context: Context,
        person: Person,
        memories: List<Interaction>,
        sessionMessages: List<ChatMessage>,
        earlierMessages: List<ChatMessage>,
        question: String,
        peerNames: List<String> = emptyList()
    ): PersonaReply {
        val localConfidence = LocalRelevanceSearch.confidence(memories, question)
        return complete(
            context,
            person,
            memories,
            sessionMessages,
            earlierMessages,
            question,
            personaMode = true,
            localConfidence,
            peerNames = peerNames
        )
    }

    suspend fun askAbout(
        context: Context,
        person: Person,
        memories: List<Interaction>,
        sessionMessages: List<ChatMessage>,
        earlierMessages: List<ChatMessage>,
        question: String
    ): PersonaReply {
        val localConfidence = LocalRelevanceSearch.confidence(memories, question)
        return complete(
            context,
            person,
            memories,
            sessionMessages,
            earlierMessages,
            question,
            personaMode = false,
            localConfidence
        )
    }

    suspend fun generatedSummary(context: Context, person: Person, memories: List<Interaction>): String = withContext(Dispatchers.IO) {
        val llm = LlmSettings.resolve(context)
        if (!llm.ready || llm.chatEndpoint == null) return@withContext InsightEngine.generatedSummary(person, memories)
        try {
            val selected = LocalRelevanceSearch.relevant(memories, "personality habits interests communication patterns", 12)
            val prompt = selected.joinToString("\n") { "- ${it.transcript ?: it.body.take(420)}" }.ifBlank { "No memories yet." }
            val response = rawCompletion(
                llm.chatEndpoint,
                llm.apiKey,
                llm.chatModel,
                "You are Gene's summary writer. Write exactly two concise sentences in third person about the person reconstructed from the supplied memories. Mention only supported patterns. Do not use markdown, labels, disclaimers, or phrases about being an AI. Do not claim certainty.",
                "Person: ${person.name}\nMemories:\n$prompt"
            )
            response.ifBlank { InsightEngine.generatedSummary(person, memories) }
        } catch (_: Exception) {
            InsightEngine.generatedSummary(person, memories)
        }
    }

    private fun providerUnavailable(llm: LlmSettings.Resolved): PersonaReply {
        val text = when {
            !llm.ready && llm.mode == LlmSettings.MODE_BYOK ->
                "BYOK isn’t set up yet — Gene needs an endpoint and API key before it can reply. $CONFIGURE_HINT"
            !llm.ready ->
                "No AI provider is available right now. $CONFIGURE_HINT"
            llm.mode == LlmSettings.MODE_BYOK ->
                "Couldn’t reach your BYOK provider. Check the endpoint and key, or switch to Gene AI. $CONFIGURE_HINT"
            else ->
                "Couldn’t reach Gene AI right now. Check your connection, or switch to BYOK in Settings. $CONFIGURE_HINT"
        }
        return PersonaReply(text, confidence = 0, referenceIds = emptyList())
    }

    private suspend fun complete(
        context: Context,
        person: Person,
        memories: List<Interaction>,
        sessionMessages: List<ChatMessage>,
        earlierMessages: List<ChatMessage>,
        question: String,
        personaMode: Boolean,
        localConfidence: Int,
        peerNames: List<String> = emptyList()
    ): PersonaReply = withContext(Dispatchers.IO) {
        val llm = LlmSettings.resolve(context)
        if (!llm.ready || llm.chatEndpoint == null) return@withContext providerUnavailable(llm)
        try {
            val selectedContext = memories.joinToString("\n") { "- ${it.transcript ?: it.body.take(420)}" }.ifBlank { "No saved memory matched closely." }
            val peerHint = peerNames
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(person.name, ignoreCase = true) }
                .distinct()
                .take(12)
                .joinToString(", ") { "@$it" }
            val system = if (personaMode) {
                buildString {
                    append("You are a conversational simulation of ${person.name}, reconstructed only from user-provided memories. Speak as a concise first-person approximation of this person, not as an analyst. Reply in one to three short natural sentences. Do not mention context, memories, references, confidence, prompts, or being an AI unless directly asked. Do not claim certainty about hidden thoughts. Do not use markdown, asterisks, bullets, labels, section headings, or phrases like Observation or Inference.")
                    if (peerHint.isNotBlank()) {
                        append(" This is a group chat. Other people present: $peerHint. If you want another person to weigh in, include their exact @Name in your reply.")
                    }
                }
            } else {
                "You are Gene's concise relationship-context assistant. Answer the user's question about ${person.name} in third person using only the supplied memories. Be direct and useful in one to three short sentences. Separate observed evidence from speculation naturally without labels. Do not pretend to know hidden thoughts. Do not use markdown, asterisks, bullets, or long disclaimers."
            }
            val conversation = buildString {
                append("Relevant memories:\n").append(selectedContext)
                if (earlierMessages.isNotEmpty()) append("\nEarlier conversation:\n").append(earlierMessages.joinToString("\n") { "${it.role}: ${it.body.take(240)}" })
                if (sessionMessages.isNotEmpty()) append("\nCurrent conversation:\n").append(sessionMessages.takeLast(10).joinToString("\n") { "${it.role}: ${it.body.take(700)}" })
                append("\nUser: ").append(question)
            }
            val content = rawCompletion(llm.chatEndpoint, llm.apiKey, llm.chatModel, system, conversation)
            if (content.isBlank()) providerUnavailable(llm)
            else PersonaReply(compactPersonaText(content), localConfidence, memories.map { it.id })
        } catch (_: Exception) {
            providerUnavailable(llm)
        }
    }

    private fun rawCompletion(endpoint: String, apiKey: String?, model: String, system: String, user: String): String {
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", system) })
                put(JSONObject().apply { put("role", "user"); put("content", user) })
            })
            put("max_tokens", 512)
        }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (!apiKey.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }
        connection.outputStream.use { it.write(payload.toString().toByteArray()) }
        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) return ""
        return JSONObject(body).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
    }

    private fun compactPersonaText(value: String): String {
        val clean = value.replace("**", "").replace("*", "").replace(Regex("(?m)^\\s*[-•]\\s*"), "").replace(Regex("(?i)^from what you('ve| have) (shared|told me)[, ]*"), "").trim()
        return clean.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }.take(3).joinToString(" ").take(640)
    }
}
