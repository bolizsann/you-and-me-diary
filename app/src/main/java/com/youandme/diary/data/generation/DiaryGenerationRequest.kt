package com.youandme.diary.data.generation

data class DiaryGenerationRequest(
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
    val generationMode: String,
    val imagePath: String?,
    val dominantColor: Long?,
    val roiScale: Float,
    val roiOffsetX: Float,
    val roiOffsetY: Float,
)
