package com.tom7.gene.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Resolves how the app should talk to a remote LLM.
 *
 * - [MODE_GENE_AI]: Gene-hosted FastAPI + Groq gateway (no user API key).
 * - [MODE_BYOK]: user-supplied OpenAI-compatible endpoint + key (previous behavior).
 */
object LlmSettings {
    const val PREFS = "gene_settings"

    const val MODE_GENE_AI = "gene_ai"
    const val MODE_BYOK = "byok"

    // Hosted gateway base (/v1). Kept private — never surface in Settings or copy.
    private const val GENE_AI_ENDPOINT = "https://gene-jctr.onrender.com/v1"

    const val DEFAULT_BYOK_MODEL = "gpt-5-mini"
    const val DEFAULT_GENE_AI_MODEL = "openai/gpt-oss-20b"
    const val DEFAULT_BYOK_TRANSCRIPTION_MODEL = "whisper-1"
    const val DEFAULT_GENE_AI_TRANSCRIPTION_MODEL = "whisper-large-v3-turbo"

    data class Resolved(
        val mode: String,
        val chatEndpoint: String?,
        val transcriptionEndpoint: String?,
        val apiKey: String?,
        val chatModel: String,
        val transcriptionModel: String,
        val ready: Boolean
    )

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mode(prefs: SharedPreferences): String {
        val stored = prefs.getString("llm_mode", null)?.trim().orEmpty()
        if (stored == MODE_BYOK || stored == MODE_GENE_AI) return stored
        // Migrate: users who already configured a key stay on BYOK.
        val hasByok = prefs.getString("llm_api_key", "").orEmpty().isNotBlank() &&
            prefs.getString("llm_endpoint", "").orEmpty().isNotBlank()
        return if (hasByok) MODE_BYOK else MODE_GENE_AI
    }

    fun resolve(context: Context): Resolved = resolve(prefs(context))

    fun resolve(prefs: SharedPreferences): Resolved {
        return when (mode(prefs)) {
            MODE_BYOK -> resolveByok(prefs)
            else -> resolveGeneAi(prefs)
        }
    }

    private fun resolveGeneAi(prefs: SharedPreferences): Resolved {
        val chat = normalizeChatEndpoint(GENE_AI_ENDPOINT)
        val transcription = normalizeTranscriptionEndpoint(GENE_AI_ENDPOINT)
        val model = prefs.getString("gene_ai_model", DEFAULT_GENE_AI_MODEL)
            .orEmpty()
            .ifBlank { DEFAULT_GENE_AI_MODEL }
        val transcriptionModel = prefs.getString("gene_ai_transcription_model", DEFAULT_GENE_AI_TRANSCRIPTION_MODEL)
            .orEmpty()
            .ifBlank { DEFAULT_GENE_AI_TRANSCRIPTION_MODEL }
        val ready = chat != null
        return Resolved(
            mode = MODE_GENE_AI,
            chatEndpoint = chat,
            transcriptionEndpoint = transcription,
            apiKey = null,
            chatModel = model,
            transcriptionModel = transcriptionModel,
            ready = ready
        )
    }

    private fun resolveByok(prefs: SharedPreferences): Resolved {
        val base = prefs.getString("llm_endpoint", "").orEmpty()
        val key = prefs.getString("llm_api_key", "").orEmpty().trim()
        val chat = normalizeChatEndpoint(base)
        val transcription = normalizeTranscriptionEndpoint(base)
        val model = prefs.getString("llm_model", DEFAULT_BYOK_MODEL)
            .orEmpty()
            .ifBlank { DEFAULT_BYOK_MODEL }
        val transcriptionModel = prefs.getString("llm_transcription_model", DEFAULT_BYOK_TRANSCRIPTION_MODEL)
            .orEmpty()
            .ifBlank { DEFAULT_BYOK_TRANSCRIPTION_MODEL }
        val ready = chat != null && key.isNotBlank()
        return Resolved(
            mode = MODE_BYOK,
            chatEndpoint = chat,
            transcriptionEndpoint = transcription,
            apiKey = key.takeIf { it.isNotBlank() },
            chatModel = model,
            transcriptionModel = transcriptionModel,
            ready = ready
        )
    }

    fun normalizeChatEndpoint(value: String): String? {
        val trimmed = value.trim().trimEnd('/')
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
        return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
    }

    fun normalizeTranscriptionEndpoint(value: String): String? {
        val trimmed = value.trim().trimEnd('/')
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
        return when {
            trimmed.endsWith("/chat/completions") ->
                trimmed.removeSuffix("/chat/completions") + "/audio/transcriptions"
            trimmed.endsWith("/audio/transcriptions") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/audio/transcriptions"
            else -> "$trimmed/v1/audio/transcriptions"
        }
    }
}
