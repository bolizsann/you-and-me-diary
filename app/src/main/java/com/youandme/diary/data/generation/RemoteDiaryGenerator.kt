package com.youandme.diary.data.generation

import android.util.Log
import com.youandme.diary.data.local.GeneratedDiaryDraft
import com.youandme.diary.data.remote.GenerateDiaryRemoteRequest
import com.youandme.diary.data.remote.GeneratedDiaryRemoteResult
import com.youandme.diary.data.remote.RemoteGemmaClient
import com.youandme.diary.data.remote.VoiceTranscriptionClient

class RemoteDiaryGenerator(
    private val remoteGemmaClient: RemoteGemmaClient = RemoteGemmaClient(),
    private val voiceTranscriptionClient: VoiceTranscriptionClient = VoiceTranscriptionClient(),
) : DiaryGenerator {
    override suspend fun generate(request: DiaryGenerationRequest): GeneratedDiaryDraft? {
        val remoteImage = request.imagePath?.let { path ->
            DiaryGenerationImageProcessor.buildRemoteImage(
                path = path,
                dominantColor = request.dominantColor,
                roiScale = request.roiScale,
                roiOffsetX = request.roiOffsetX,
                roiOffsetY = request.roiOffsetY,
            )
        }
        val remoteResult = remoteGemmaClient.generate(
            GenerateDiaryRemoteRequest(
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
                image = remoteImage,
            ),
        ) ?: return null
        Log.i(
            TAG,
            "Remote generation completed source=${remoteResult.source} diaryChars=${remoteResult.diaryText.length} " +
                "card=${remoteResult.cardSummary}${remoteResult.cardEmoji} babyTextPresent=${remoteResult.babyText.isNotBlank()} " +
                "safetyNotePresent=${remoteResult.safetyNote.isNotBlank()}",
        )
        return remoteResult.toGeneratedDiaryDraft()
    }

    suspend fun transcribeVoice(audioBytes: ByteArray): String? {
        val transcript = voiceTranscriptionClient.transcribe(audioBytes = audioBytes)
        if (transcript == null) {
            Log.w(TAG, "Remote voice transcription returned null")
        } else {
            Log.i(TAG, "Remote voice transcription completed transcriptChars=${transcript.length}")
        }
        return transcript
    }
}

private fun GeneratedDiaryRemoteResult.toGeneratedDiaryDraft(): GeneratedDiaryDraft =
    GeneratedDiaryDraft(
        titleSuggestion = titleSuggestion,
        diaryText = diaryText,
        cardSummary = cardSummary,
        cardEmoji = cardEmoji,
        babyText = babyText,
        safetyNote = safetyNote,
        source = source,
    )

private const val TAG = "DiaryGeneration"
