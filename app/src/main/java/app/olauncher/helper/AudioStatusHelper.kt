package app.olauncher.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AudioPipelineStatus(
    val recording: Boolean,
    val sessionCount: Int,
    val peopleCount: Int,
    val queueDepth: Int,
    val lastTranscriptAt: String?,
    val whisperStatus: String,
    val diskFreeGb: Double,
    val bedrockStatus: String,
    val serverReachable: Boolean,
)

suspend fun fetchAudioStatus(baseUrl: String): AudioPipelineStatus = withContext(Dispatchers.IO) {
    try {
        fun get(url: String): JSONObject {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            return JSONObject(conn.inputStream.bufferedReader().readText())
        }
        val health = get("$baseUrl/health")
        val status = get("$baseUrl/today/status")
        AudioPipelineStatus(
            recording = status.optBoolean("recording", false),
            sessionCount = status.optInt("sessions_today", 0),
            peopleCount = status.optInt("people_today", 0),
            queueDepth = status.optInt("queue_depth", 0),
            lastTranscriptAt = status.optString("last_transcript_at").takeIf { it.isNotBlank() && it != "null" },
            whisperStatus = health.optString("whisper_worker_status", "unknown"),
            diskFreeGb = health.optDouble("disk_free_gb", -1.0),
            bedrockStatus = health.optString("bedrock_last_call_status", "unknown"),
            serverReachable = true,
        )
    } catch (e: Exception) {
        AudioPipelineStatus(
            recording = false, sessionCount = 0, peopleCount = 0, queueDepth = 0,
            lastTranscriptAt = null, whisperStatus = "down", diskFreeGb = -1.0,
            bedrockStatus = "unknown", serverReachable = false,
        )
    }
}
