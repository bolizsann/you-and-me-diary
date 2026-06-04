package com.youandme.diary.data.remote

import android.util.Base64
import android.util.Log
import com.youandme.diary.BuildConfig
import com.youandme.diary.data.voice.toWavBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class VoiceTranscriptionClient(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
    private val appToken: String = BuildConfig.BACKEND_APP_TOKEN,
) {
    suspend fun transcribe(audioBytes: ByteArray): String? =
        withContext(Dispatchers.IO) {
            val startedAt = android.os.SystemClock.elapsedRealtime()
            if (audioBytes.isEmpty()) {
                Log.w(TAG, "Audio bytes are empty")
                return@withContext null
            }
            val bodyBytes = runCatching {
                val wavBytes = audioBytes.toWavBytes()
                JSONObject().apply {
                    put("mimeType", "audio/wav")
                    put("dataBase64", Base64.encodeToString(wavBytes, Base64.NO_WRAP))
                    put("locale", "zh-CN")
                }.toString().toByteArray(Charsets.UTF_8)
            }.onFailure { error ->
                Log.w(TAG, "Failed to prepare audio payload: ${error.javaClass.simpleName}: ${error.message}")
            }.getOrNull()
                ?: return@withContext null

            for (attempt in 1..MAX_TRANSCRIPTION_ATTEMPTS) {
                val result = requestTranscription(bodyBytes = bodyBytes, startedAt = startedAt, attempt = attempt)
                result.transcript?.let { return@withContext it }
                if (!result.retryable || attempt == MAX_TRANSCRIPTION_ATTEMPTS) {
                    return@withContext null
                }
                delay(TRANSCRIPTION_RETRY_DELAY_MS)
            }
            null
        }

    private fun requestTranscription(
        bodyBytes: ByteArray,
        startedAt: Long,
        attempt: Int,
    ): VoiceTranscriptionAttemptResult {
        var phase = "open"
        return runCatching {
            val connection = (URL("${baseUrl.trimEnd('/')}/transcribe-voice").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 55_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (appToken.isNotBlank()) {
                    setRequestProperty("X-App-Token", appToken)
                }
            }
            phase = "write"
            connection.outputStream.use { output ->
                output.write(bodyBytes)
                output.flush()
            }
            phase = "response"
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                Log.w(
                    TAG,
                    "Backend returned HTTP $responseCode attempt=$attempt elapsedMs=${android.os.SystemClock.elapsedRealtime() - startedAt} " +
                        "errorBody=${errorBody.take(160)}",
                )
                return@runCatching VoiceTranscriptionAttemptResult(retryable = responseCode.isRetryableHttpStatus())
            }
            phase = "read"
            val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val data = JSONObject(responseBody)
            val transcript = data.optString("transcript").trim()
            val errorType = data.optString("debugErrorType")
            Log.i(
                TAG,
                "Transcription completed source=${data.optString("source")} transcriptChars=${transcript.length} " +
                    "attempt=$attempt elapsedMs=${android.os.SystemClock.elapsedRealtime() - startedAt} errorType=$errorType",
            )
            VoiceTranscriptionAttemptResult(
                transcript = transcript.ifBlank { null },
                retryable = transcript.isBlank() && errorType.isRetryableBackendError(),
            )
        }.onFailure { error ->
            Log.w(
                TAG,
                "Voice transcription failed phase=$phase attempt=$attempt elapsedMs=${android.os.SystemClock.elapsedRealtime() - startedAt}: " +
                    "${error.javaClass.simpleName}: ${error.message}",
            )
        }.getOrNull() ?: VoiceTranscriptionAttemptResult(retryable = true)
    }
}

private data class VoiceTranscriptionAttemptResult(
    val transcript: String? = null,
    val retryable: Boolean = false,
)

private fun Int.isRetryableHttpStatus(): Boolean =
    this == 408 || this == 429 || this in 500..599

private fun String.isRetryableBackendError(): Boolean =
    this in setOf("model_temporarily_unavailable", "timeout", "network_error", "unknown_gemma_error")

private const val TAG = "VoiceTranscription"
private const val MAX_TRANSCRIPTION_ATTEMPTS = 2
private const val TRANSCRIPTION_RETRY_DELAY_MS = 900L
