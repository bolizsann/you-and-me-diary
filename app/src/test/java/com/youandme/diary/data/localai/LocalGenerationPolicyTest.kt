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
        assertEquals("好开心啊", result.cardSummary)
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

    @Test
    fun localPromptIncludesVoiceTextAndManualEmoji() {
        val prompt = buildPrompt(
            request(
                text = "😊",
                voiceText = "今天宝宝动了一下，我特别开心。",
                diaryTextMode = "polish",
            ),
        )

        assertTrue(prompt.contains("用户手写：😊"))
        assertTrue(prompt.contains("语音转写：今天宝宝动了一下，我特别开心。"))
        assertTrue(prompt.contains("合并输入：😊 今天宝宝动了一下，我特别开心。"))
        assertTrue(prompt.contains("保留第一人称和用户额外添加的 emoji"))
    }

    @Test
    fun preserveModeKeepsVoiceTextWithManualEmoji() {
        val result = generatedDraft(
            diaryText = "模型改写不应该覆盖 preserve。",
        ).applyLocalGenerationPolicy(
            request = request(
                text = "😊",
                voiceText = "今天宝宝动了一下，我特别开心。",
            ),
        )

        assertEquals("😊 今天宝宝动了一下，我特别开心。", result.diaryText)
    }

    @Test
    fun cardSummaryRejectsFullInputWithManualEmoji() {
        val result = generatedDraft(
            diaryText = "😊 今天宝宝动了一下，我特别开心。",
            cardSummary = "😊 今天宝宝动了一下，我特别开心。",
            cardEmoji = "😊😊😊",
        ).applyLocalGenerationPolicy(
            request = request(
                text = "😊",
                voiceText = "今天宝宝动了一下，我特别开心。",
                diaryTextMode = "polish",
            ),
        )

        assertEquals("😊 今天宝宝动了一下，我特别开心。", result.diaryText)
        assertEquals("好开心啊", result.cardSummary)
        assertEquals("😊", result.cardEmoji)
    }

    @Test
    fun rawJsonPayloadIsNotUsedAsDiaryTextWhenLocalParseFails() {
        val result = generatedDraft(
            diaryText = """
                ```json
                {
                  "titleSuggestion": "与猫咪的放松时刻",
                  "diaryText": "我跟猫猫在一起感觉很放松很开心😀 我
            """.trimIndent(),
            cardSummary = "好开心啊",
            cardEmoji = "😊",
        ).applyLocalGenerationPolicy(
            request = request(
                text = "我跟猫猫在一起感觉很放松很开心😀",
                voiceText = "",
                diaryTextMode = "polish",
                imagePath = "/tmp/cat.jpg",
            ),
        )

        assertEquals("这一页先把今天的话轻轻收好：我跟猫猫在一起感觉很放松很开心😀", result.diaryText)
    }

    private fun request(
        text: String,
        voiceText: String = "",
        diaryTextMode: String = "preserve",
        imagePath: String? = null,
    ): GenerateDiaryLocalRequest =
        GenerateDiaryLocalRequest(
            text = text,
            voiceText = voiceText,
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
