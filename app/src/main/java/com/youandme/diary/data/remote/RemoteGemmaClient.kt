package com.youandme.diary.data.remote

import android.util.Log
import com.youandme.diary.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RemoteGemmaClient(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
    private val appToken: String = BuildConfig.BACKEND_APP_TOKEN,
) {
    suspend fun generate(request: GenerateDiaryRemoteRequest): GeneratedDiaryRemoteResult? =
        withContext(Dispatchers.IO) {
            val startedAt = android.os.SystemClock.elapsedRealtime()
            var phase = "start"
            runCatching {
                val body = request.toJson().toString()
                val bodyBytes = body.toByteArray(Charsets.UTF_8)
                phase = "open"
                val connection = (URL("${baseUrl.trimEnd('/')}/generate-diary").openConnection() as HttpURLConnection).apply {
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
                if (connection.responseCode !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    Log.w(
                        "DiaryGeneration",
                        "Backend returned HTTP ${connection.responseCode} elapsedMs=${android.os.SystemClock.elapsedRealtime() - startedAt} " +
                            "errorBody=${errorBody.take(160)}",
                    )
                    return@runCatching null
                }
                phase = "read"
                val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                JSONObject(responseBody).toGeneratedResult()
            }.onFailure { error ->
                Log.w(
                    "DiaryGeneration",
                    "Backend generation failed phase=$phase elapsedMs=${android.os.SystemClock.elapsedRealtime() - startedAt}: " +
                        "${error.javaClass.simpleName}: ${error.message}",
                )
            }.getOrNull()
        }
}

typealias DiaryGenerationClient = RemoteGemmaClient

data class GenerateDiaryRemoteRequest(
    val text: String,
    val voiceText: String,
    val inputSource: String,
    val diaryTextMode: String,
    val dateId: String,
    val dateLabel: String,
    val currentTitle: String,
    val isFirstRecordForDay: Boolean,
    val username: String,
    val estimatedDueDate: String?,
    val image: DiaryRemoteImage?,
)

data class DiaryRemoteImage(
    val mimeType: String,
    val dataBase64: String,
    val dominantColor: String?,
)

data class GeneratedDiaryRemoteResult(
    val titleSuggestion: String,
    val diaryText: String,
    val cardSummary: String,
    val cardEmoji: String,
    val babyText: String,
    val safetyNote: String,
    val source: String,
)

private fun GenerateDiaryRemoteRequest.toJson(): JSONObject =
    JSONObject().apply {
        put("text", text)
        put("voiceText", voiceText)
        put("inputSource", inputSource)
        put("diaryTextMode", diaryTextMode)
        put("dateId", dateId)
        put("dateLabel", dateLabel)
        put("currentTitle", currentTitle)
        put("isFirstRecordForDay", isFirstRecordForDay)
        put("username", username)
        put("estimatedDueDate", estimatedDueDate)
        put(
            "image",
            image?.let { item ->
                JSONObject().apply {
                    put("mimeType", item.mimeType)
                    put("dataBase64", item.dataBase64)
                    put("dominantColor", item.dominantColor)
                }
            },
        )
    }

private fun JSONObject.toGeneratedResult(): GeneratedDiaryRemoteResult =
    GeneratedDiaryRemoteResult(
        titleSuggestion = optString("titleSuggestion"),
        diaryText = optString("diaryText"),
        cardSummary = optString("cardSummary"),
        cardEmoji = optString("cardEmoji"),
        babyText = optString("babyText"),
        safetyNote = optString("safetyNote"),
        source = optString("source"),
    )
