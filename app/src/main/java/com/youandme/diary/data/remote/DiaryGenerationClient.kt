package com.youandme.diary.data.remote

import com.youandme.diary.BuildConfig
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DiaryGenerationClient(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
) {
    suspend fun generate(request: GenerateDiaryRemoteRequest): GeneratedDiaryRemoteResult? =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d("DiaryGeneration", "POST ${baseUrl.trimEnd('/')}/generate-diary mode=${request.diaryTextMode} source=${request.inputSource}")
                val connection = (URL("${baseUrl.trimEnd('/')}/generate-diary").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8_000
                    readTimeout = 55_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(request.toJson().toString())
                }
                if (connection.responseCode !in 200..299) {
                    Log.w("DiaryGeneration", "Backend returned HTTP ${connection.responseCode}")
                    return@runCatching null
                }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                JSONObject(body).toGeneratedResult().also { result ->
                    Log.d(
                        "DiaryGeneration",
                        "Result source=${result.source} cardSummary=${result.cardSummary} cardEmoji=${result.cardEmoji} babyTextPresent=${result.babyText.isNotBlank()}",
                    )
                }
            }.onFailure { error ->
                Log.w("DiaryGeneration", "Backend generation failed: ${error.javaClass.simpleName}: ${error.message}")
            }.getOrNull()
        }
}

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
