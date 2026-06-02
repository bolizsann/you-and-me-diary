package com.youandme.diary.data.generation

import com.youandme.diary.data.local.GeneratedDiaryDraft

interface DiaryGenerator {
    suspend fun generate(request: DiaryGenerationRequest): GeneratedDiaryDraft?
}
