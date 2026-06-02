package com.youandme.diary.data.localai

import com.youandme.diary.data.local.GeneratedDiaryDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalGenerationPolicyTest {
    @Test
    fun preserveModeKeepsUserTextInsteadOfModelRewrite() {
        val result = generatedDraft(
            diaryText = "今天阳光很好，我穿着裙子拍了照片，心里很幸福。",
            cardSummary = "这是一个很长很长的图卡短句",
            cardEmoji = "",
        ).applyLocalGenerationPolicy(
            request = request(text = "今天很开心，穿了一件可爱的裙子。"),
        )

        assertEquals("今天很开心，穿了一件可爱的裙子。", result.diaryText)
        assertEquals("这是一个很长很长", result.cardSummary)
    }

    @Test
    fun safetyNoteClearsBabyText() {
        val result = generatedDraft(
            diaryText = "今天有点担心。",
            babyText = "我陪着你。",
        ).applyLocalGenerationPolicy(
            request = request(text = "肚子好痛"),
        )

        assertEquals("", result.babyText)
        assertTrue(result.safetyNote.isNotBlank())
    }

    @Test
    fun harmlessModelSafetyNoteIsIgnored() {
        val result = generatedDraft(
            diaryText = "周日的时候锻炼的我，我有在为你好好锻炼拉～",
            safetyNote = "建议及时联系医生确认。",
        ).applyLocalGenerationPolicy(
            request = request(text = "周日的时候锻炼的我，我有在为你好好锻炼拉～"),
        )

        assertEquals("", result.safetyNote)
    }

    @Test
    fun imageOnlyKeepsGeneratedTextShortAndAllowsEmptyBabyText() {
        val result = generatedDraft(
            diaryText = "图片里有柔和的光和可爱的衣裙，这一页先把今天的开心留下来。",
            babyText = "",
        ).applyLocalGenerationPolicy(
            request = request(text = "", diaryTextMode = "generate", imagePath = "/tmp/image.jpg"),
        )

        assertTrue(result.diaryText.isNotBlank())
    }

    private fun request(
        text: String,
        diaryTextMode: String = "preserve",
        imagePath: String? = null,
    ): GenerateDiaryLocalRequest =
        GenerateDiaryLocalRequest(
            text = text,
            voiceText = "",
            inputSource = if (text.isBlank() && imagePath != null) "imageOnly" else "typed",
            diaryTextMode = diaryTextMode,
            dateId = "2026-06-01",
            dateLabel = "6 月 1 日",
            currentTitle = "",
            isFirstRecordForDay = true,
            username = "你",
            estimatedDueDate = null,
            imagePath = imagePath,
            dominantColor = null,
        )

    private fun generatedDraft(
        diaryText: String,
        cardSummary: String = "",
        cardEmoji: String = "",
        babyText: String = "",
        safetyNote: String = "",
    ): GeneratedDiaryDraft =
        GeneratedDiaryDraft(
            titleSuggestion = "今天也留一页",
            diaryText = diaryText,
            cardSummary = cardSummary,
            cardEmoji = cardEmoji,
            babyText = babyText,
            safetyNote = safetyNote,
            source = "local-gemma-gpu",
        )
}
