package com.gene.app.data

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputPath: String? = null

    fun start(): Boolean {
        if (recorder != null) return false
        val directory = java.io.File(context.filesDir, "audio").apply { mkdirs() }
        val path = java.io.File(directory, "memory_${System.currentTimeMillis()}.m4a").absolutePath
        val next = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        return try {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioEncodingBitRate(128000)
            next.setAudioSamplingRate(44100)
            next.setOutputFile(path)
            next.prepare()
            next.start()
            recorder = next
            outputPath = path
            true
        } catch (_: Exception) {
            runCatching { next.reset() }
            runCatching { next.release() }
            false
        }
    }

    fun stop(): Uri? {
        val current = recorder ?: return null
        val path = outputPath
        recorder = null
        outputPath = null
        return try {
            current.stop()
            current.release()
            path?.let { Uri.fromFile(java.io.File(it)) }
        } catch (_: Exception) {
            runCatching { current.reset() }
            runCatching { current.release() }
            path?.let { java.io.File(it).delete() }
            null
        }
    }

    fun amplitude(): Float {
        val current = recorder ?: return 0f
        return try {
            (current.maxAmplitude / 32768f).coerceIn(0f, 1f)
        } catch (_: Exception) {
            0f
        }
    }

    fun release() { if (recorder != null) stop() }
}

class AudioTranscriptionWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val interactionId = inputData.getLong(KEY_INTERACTION_ID, -1L)
        val uri = inputData.getString(KEY_URI).orEmpty()
        val db = GeneDatabase(applicationContext)
        if (interactionId <= 0 || uri.isBlank()) return Result.failure()
        val prefs = applicationContext.getSharedPreferences("gene_settings", Context.MODE_PRIVATE)
        val base = prefs.getString("llm_endpoint", "").orEmpty().trim()
        val key = prefs.getString("llm_api_key", "").orEmpty().trim()
        if (base.isBlank() || key.isBlank()) {
            db.updateTranscription(interactionId, null, TRANSCRIPTION_UNAVAILABLE)
            return Result.success()
        }
        return try {
            val endpoint = normalize(base) ?: throw IllegalArgumentException("Invalid endpoint")
            val boundary = "----Gene${UUID.randomUUID()}"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            val bytes = applicationContext.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() } ?: throw IllegalStateException("Audio file unavailable")
            BufferedOutputStream(connection.outputStream).use { output ->
                fun part(header: String, value: String) { output.write("--$boundary\r\n$header\r\n\r\n$value\r\n".toByteArray()) }
                part("Content-Disposition: form-data; name=\"model\"", prefs.getString("llm_transcription_model", "whisper-1").orEmpty())
                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"memory.m4a\"\r\nContent-Type: audio/mp4\r\n\r\n".toByteArray())
                output.write(bytes)
                output.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("HTTP $status")
            val transcript = JSONObject(response).optString("text").trim()
            if (transcript.isBlank()) throw IllegalStateException("No transcript")
            db.updateTranscription(interactionId, transcript, TRANSCRIPTION_COMPLETE)
            Result.success()
        } catch (_: Exception) {
            db.updateTranscription(interactionId, null, TRANSCRIPTION_UNAVAILABLE)
            Result.success()
        }
    }

    companion object {
        private const val KEY_INTERACTION_ID = "interaction_id"
        private const val KEY_URI = "uri"
        fun enqueue(context: Context, interactionId: Long, uri: Uri) {
            val data = Data.Builder().putLong(KEY_INTERACTION_ID, interactionId).putString(KEY_URI, uri.toString()).build()
            val request = OneTimeWorkRequestBuilder<AudioTranscriptionWorker>().setInputData(data).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueue(request)
        }

        private fun normalize(value: String): String? {
            val trimmed = value.trim().trimEnd('/')
            if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
            return when {
                trimmed.endsWith("/chat/completions") -> trimmed.removeSuffix("/chat/completions") + "/audio/transcriptions"
                trimmed.endsWith("/v1") -> "$trimmed/audio/transcriptions"
                else -> "$trimmed/v1/audio/transcriptions"
            }
        }
    }
}
