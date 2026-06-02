package com.youandme.diary.data.generation

import android.content.Context
import com.youandme.diary.data.local.GeneratedDiaryDraft
import com.youandme.diary.domain.model.GenerationModes

class DiaryGenerationGateway(
    context: Context,
    private val remoteGenerator: DiaryGenerator = RemoteDiaryGenerator(),
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
}
