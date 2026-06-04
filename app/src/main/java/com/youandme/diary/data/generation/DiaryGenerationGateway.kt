package com.youandme.diary.data.generation

import android.content.Context
import com.youandme.diary.data.local.GeneratedDiaryDraft
import com.youandme.diary.domain.model.GenerationModes

class DiaryGenerationGateway(
    context: Context,
    private val remoteGenerator: RemoteDiaryGenerator = RemoteDiaryGenerator(),
    private val localGenerator: LocalDiaryGenerator = LocalDiaryGenerator(context),
) {
    suspend fun generate(request: DiaryGenerationRequest): GeneratedDiaryDraft? =
        when (GenerationModes.normalize(request.generationMode)) {
            GenerationModes.Online -> remoteGenerator.generate(request)
            else -> localGenerator.generate(request)
        }

    suspend fun warmUpLocalIfNeeded(generationMode: String) {
        if (GenerationModes.normalize(generationMode) == GenerationModes.Offline) {
            localGenerator.warmUp()
        }
    }

    suspend fun transcribeVoice(generationMode: String, audioBytes: ByteArray): DiaryVoiceTranscriptionResult {
        val normalizedMode = GenerationModes.normalize(generationMode)
        val transcript = when (normalizedMode) {
            GenerationModes.Online -> remoteGenerator.transcribeVoice(audioBytes)
            else -> localGenerator.transcribeVoice(audioBytes)
        }
        return DiaryVoiceTranscriptionResult(
            transcript = transcript,
            failureMessage = if (normalizedMode == GenerationModes.Offline) {
                "端侧语音转写暂不可用"
            } else {
                "转录失败，再试一次"
            },
        )
    }
}

data class DiaryVoiceTranscriptionResult(
    val transcript: String?,
    val failureMessage: String,
)
