package com.youandme.diary.data.generation

import android.content.Context
import android.util.Log
import com.youandme.diary.data.local.GeneratedDiaryDraft
import com.youandme.diary.data.localai.GenerateDiaryLocalRequest
import com.youandme.diary.data.localai.LocalGemmaClient
import com.youandme.diary.data.localai.LocalGemmaWarmUpResult
import java.io.File

class LocalDiaryGenerator(
    context: Context,
    private val localGemmaClient: LocalGemmaClient = LocalGemmaClient(context),
) : DiaryGenerator {
    private val cacheDir = context.applicationContext.cacheDir

    override suspend fun generate(request: DiaryGenerationRequest): GeneratedDiaryDraft? {
        val localImagePath = request.imagePath?.let { path ->
            DiaryGenerationImageProcessor.buildLocalModelImage(
                cacheDir = cacheDir,
                path = path,
                roiScale = request.roiScale,
                roiOffsetX = request.roiOffsetX,
                roiOffsetY = request.roiOffsetY,
            )
        }
        return try {
            val result = localGemmaClient.generateWithMetrics(
                GenerateDiaryLocalRequest(
                    text = request.text,
                    voiceText = request.voiceText,
                    inputSource = request.inputSource,
                    diaryTextMode = request.diaryTextMode,
                    dateId = request.dateId,
                    dateLabel = request.dateLabel,
                    currentTitle = request.currentTitle,
                    isFirstRecordForDay = request.isFirstRecordForDay,
                    username = request.username,
                    estimatedDueDate = request.estimatedDueDate,
                    imagePath = localImagePath,
                    dominantColor = request.dominantColor?.toDiaryHexColor(),
                ),
            )
            if (result == null) {
                Log.w(TAG, "Local generation returned null hasImage=${request.imagePath != null}")
            } else {
                Log.i(
                    TAG,
                    "Local generation completed source=${result.draft.source} backend=${result.backend} " +
                        "initMs=${result.initMs} inferenceMs=${result.inferenceMs} totalMs=${result.totalMs} rawChars=${result.rawLength} " +
                        "diaryChars=${result.draft.diaryText.length} card=${result.draft.cardSummary}${result.draft.cardEmoji} " +
                        "babyTextPresent=${result.draft.babyText.isNotBlank()} safetyNotePresent=${result.draft.safetyNote.isNotBlank()}",
                )
            }
            result?.draft
        } finally {
            localImagePath?.let { path ->
                runCatching {
                    File(path).delete()
                }.onFailure { error ->
                    Log.w(TAG, "Failed to delete local generation image: ${error.javaClass.simpleName}: ${error.message}")
                }
            }
        }
    }

    suspend fun warmUp(): LocalGemmaWarmUpResult? =
        localGemmaClient.warmUp()

    suspend fun transcribeVoice(audioBytes: ByteArray): String? {
        val result = localGemmaClient.transcribeAudio(audioBytes)
        if (result == null) {
            Log.w(TAG, "Local voice transcription returned null")
            return null
        }
        Log.i(
            TAG,
            "Local voice transcription completed backend=${result.backend} initMs=${result.initMs} " +
                "inferenceMs=${result.inferenceMs} totalMs=${result.totalMs} rawChars=${result.rawLength} " +
                "transcriptChars=${result.transcript.length}",
        )
        return result.transcript.ifBlank { null }
    }
}

private const val TAG = "DiaryGeneration"
