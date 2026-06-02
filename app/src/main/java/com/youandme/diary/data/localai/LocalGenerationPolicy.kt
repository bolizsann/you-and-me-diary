package com.youandme.diary.data.localai

import com.youandme.diary.data.local.GeneratedDiaryDraft
import java.security.MessageDigest

internal fun GeneratedDiaryDraft.applyLocalGenerationPolicy(
    request: GenerateDiaryLocalRequest,
): GeneratedDiaryDraft {
    val diaryText = selectDiaryText(request, diaryText.trim())
    val safetyNote = safetyNoteFor(request)
    val cardSummary = cardSummary.trim().take(MAX_CARD_SUMMARY_LENGTH)
        .ifBlank { fallbackCardSummary(request) }
    val cardEmoji = cardEmoji.trim().ifBlank { fallbackCardEmoji(request) }
    val babyText = if (safetyNote.isNotBlank()) {
        ""
    } else {
        applyBabyReplyPolicy(
            request = request,
            cardSummary = cardSummary,
            cardEmoji = cardEmoji,
            candidate = babyText.trim().ifBlank { fallbackBabyText(request) },
        )
    }
    return copy(
        diaryText = diaryText,
        cardSummary = cardSummary,
        cardEmoji = cardEmoji,
        babyText = babyText,
        safetyNote = safetyNote,
    )
}

private fun selectDiaryText(request: GenerateDiaryLocalRequest, generatedText: String): String {
    if (request.diaryTextMode == "preserve") {
        request.combinedText().trim().takeIf { it.isNotBlank() }?.let { return it }
    }
    return generatedText.ifBlank { fallbackDiaryText(request) }
}

private fun fallbackDiaryText(request: GenerateDiaryLocalRequest): String {
    val combined = request.combinedText().trim()
    if (request.diaryTextMode == "preserve" && combined.isNotBlank()) return combined
    if (combined.isNotBlank()) return "这一页先把今天的话轻轻收好：$combined"
    if (!request.imagePath.isNullOrBlank()) return "这张图里有今天的光、颜色和一点点心情。先把它留下来，就已经很好。"
    return "今天也被认真地留在这里。"
}

private fun fallbackCardSummary(request: GenerateDiaryLocalRequest): String {
    val combined = request.combinedText()
    return when {
        combined.hasAny("开心", "高兴", "快乐", "幸福") -> "好开心啊"
        combined.hasAny("伤心", "难过", "委屈", "害怕") -> "抱抱今天"
        combined.hasAny("胎动", "动了一下", "踢") -> "小小胎动"
        combined.hasAny("累", "疲惫", "困", "撑不住") -> "有点累了"
        else -> ""
    }
}

private fun fallbackCardEmoji(request: GenerateDiaryLocalRequest): String {
    val combined = request.combinedText()
    return when {
        combined.hasAny("开心", "高兴", "快乐", "幸福") -> "😊"
        combined.hasAny("伤心", "难过", "委屈", "害怕") -> "🤍"
        combined.hasAny("胎动", "动了一下", "踢") -> "✨"
        combined.hasAny("累", "疲惫", "困", "撑不住") -> "☁️"
        else -> ""
    }
}

private fun fallbackBabyText(request: GenerateDiaryLocalRequest): String {
    val combined = request.combinedText()
    return when {
        combined.hasAny("胎动", "动了一下", "踢") -> "妈妈，我也在轻轻回应你。"
        combined.hasAny("累", "疲惫", "困", "撑不住") -> "妈妈辛苦啦，我陪你慢慢来。"
        combined.hasAny("伤心", "难过", "委屈", "害怕", "失落", "孤单", "不开心", "哭") -> "我在这里陪着你。"
        combined.hasAny("开心", "高兴", "快乐", "幸福", "可爱", "喜欢", "真好") -> "我也跟着开心了一下。"
        else -> ""
    }
}

private fun safetyNoteFor(request: GenerateDiaryLocalRequest): String {
    val combined = request.combinedText()
    if (combined.hasAny("出血", "流血", "剧烈疼痛", "胎动明显减少", "持续头晕", "明显加重", "肚子好痛", "肚子很痛")) {
        return "如果这些感受持续或加重，建议及时联系医生或产检机构确认。"
    }
    return ""
}

private fun applyBabyReplyPolicy(
    request: GenerateDiaryLocalRequest,
    cardSummary: String,
    cardEmoji: String,
    candidate: String,
): String {
    val mood = classifyBabyReplyMood(request, cardSummary, cardEmoji, candidate)
    val bucket = babyReplyBucket(request)
    return when (mood) {
        "sad" -> chooseBabyReply(
            bucket = bucket,
            textThreshold = 70,
            reactionThreshold = 90,
            candidate = textBabyCandidate(candidate, "我在这里陪着你。", rejectGeneric = true),
            reactions = SAD_BABY_REACTIONS,
        )

        "tired" -> chooseBabyReply(
            bucket = bucket,
            textThreshold = 60,
            reactionThreshold = 85,
            candidate = textBabyCandidate(candidate, "妈妈辛苦啦，我陪你慢慢来。", rejectGeneric = true),
            reactions = TIRED_BABY_REACTIONS,
        )

        "movement" -> chooseBabyReply(
            bucket = bucket,
            textThreshold = 60,
            reactionThreshold = 85,
            candidate = textBabyCandidate(candidate, "妈妈，我也在轻轻回应你。", rejectGeneric = true),
            reactions = MOVEMENT_BABY_REACTIONS,
        )

        "happy" -> chooseBabyReply(
            bucket = bucket,
            textThreshold = 30,
            reactionThreshold = 65,
            candidate = textBabyCandidate(candidate, "我也跟着开心了一下。", rejectGeneric = true),
            reactions = HAPPY_BABY_REACTIONS,
        )

        else -> chooseBabyReply(
            bucket = bucket,
            textThreshold = 30,
            reactionThreshold = 65,
            candidate = textBabyCandidate(candidate, "我在这里听着呢。"),
            reactions = NEUTRAL_BABY_REACTIONS,
        )
    }
}

private fun chooseBabyReply(
    bucket: Int,
    textThreshold: Int,
    reactionThreshold: Int,
    candidate: String,
    reactions: List<String>,
): String {
    if (bucket < textThreshold) return candidate
    if (bucket < reactionThreshold) return reactions[bucket % reactions.size]
    return ""
}

private fun textBabyCandidate(candidate: String, fallback: String, rejectGeneric: Boolean = false): String {
    val clean = candidate.trim()
    if (clean.isBlank() || clean.isLightReaction() || rejectGeneric && clean in GENERIC_BABY_TEXTS) return fallback
    return clean
}

private fun classifyBabyReplyMood(
    request: GenerateDiaryLocalRequest,
    cardSummary: String,
    cardEmoji: String,
    babyText: String,
): String {
    val combined = listOf(request.combinedText(), cardSummary, cardEmoji, babyText)
        .filter { it.isNotBlank() }
        .joinToString(separator = " ")
    return when {
        combined.hasAny("伤心", "难过", "委屈", "害怕", "失落", "孤单", "不开心", "哭", "没有", "忘了", "冷落") -> "sad"
        combined.hasAny("胎动", "动了一下", "踢") -> "movement"
        combined.hasAny("累", "疲惫", "困", "撑不住", "沉", "难受") -> "tired"
        combined.hasAny("开心", "高兴", "快乐", "幸福", "可爱", "喜欢", "期待", "真好", "陪在我身边") -> "happy"
        else -> "neutral"
    }
}

private fun babyReplyBucket(request: GenerateDiaryLocalRequest): Int {
    val key = listOf(request.dateId, request.inputSource, request.text, request.voiceText).joinToString(separator = "|")
    val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
    return digest.take(4).fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xff) }.floorMod(100)
}

private fun GenerateDiaryLocalRequest.combinedText(): String =
    listOf(text, voiceText).filter { it.isNotBlank() }.joinToString(separator = " ")

private fun String.hasAny(vararg keywords: String): Boolean =
    keywords.any { contains(it, ignoreCase = true) }

private fun String.isLightReaction(): Boolean =
    trim().let { clean -> clean.startsWith("（") && clean.endsWith("）") || clean in ALL_BABY_REACTIONS }

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other

private const val MAX_CARD_SUMMARY_LENGTH = 8
private val GENERIC_BABY_TEXTS = setOf(
    "我在这里听着呢。",
)

private val HAPPY_BABY_REACTIONS = listOf(
    "😊", "✨", "🌷", "🥰", "🫧", "💕", "💫", "🌟", "💛", "🌸",
    "（噗噜噗噜）", "（小手挥挥）", "（咕噜咕噜）", "（开心冒泡）",
    "（蹬蹬腿）", "（伸伸小手）", "（翻了个身）", "（轻轻转圈）",
    "（晃晃小脚）", "（眨眨眼）", "（轻轻点点）", "（咕噜冒泡）",
    "（软软点头）", "（小脚踢踢）", "（悄悄拍手）", "（转了个圈）",
    "（蹭蹭妈妈）", "（小手拍拍）", "（肚肚冒泡）", "（伸个小懒）",
    "（轻轻摆摆）", "（翻身贴贴）", "（小脚晃晃）", "（呼噜一下）",
    "（冒个小泡）", "（软软贴住）", "（开心蹭蹭）", "（小手贴贴）",
    "（咕噜翻身）", "（贴贴肚肚）", "（轻轻踢踢）", "（甜甜打盹）",
)

private val NEUTRAL_BABY_REACTIONS = listOf(
    "✨", "🌙", "🫧", "🤍", "☁️", "🌿", "💫", "🌼", "🩷", "💛",
    "（噗噜噗噜）", "（小手挥挥）", "（咕噜一下）", "（慢慢打盹）",
    "（安静漂浮）", "（轻轻翻身）", "（眨眨眼）", "（慢慢贴贴）",
    "（软软呼吸）", "（悄悄陪着）", "（睡成一团）", "（小脚收好）",
    "（伸伸小手）", "（翻了个身）", "（呼噜呼噜）", "（肚肚贴贴）",
    "（小手贴贴）", "（轻轻靠着）", "（软软打盹）", "（慢慢漂漂）",
    "（咕噜冒泡）", "（小脚晃晃）", "（安静贴住）", "（翻身贴贴）",
    "（呼呼睡着）", "（轻轻动动）", "（贴住肚肚）", "（慢慢眨眼）",
    "（小手握握）", "（软软靠着）", "（安静呼吸）", "（贴着听听）",
)

private val SAD_BABY_REACTIONS = listOf(
    "🤍", "🫶", "☁️", "🌙", "🫧", "💛", "🌧️", "🩷", "💫", "✨",
    "（轻轻贴贴）", "（小手握握）", "（蹭蹭妈妈）", "（安静陪着）",
    "（靠近一点）", "（软软贴住）", "（小手贴贴）", "（轻轻呼呼）",
    "（贴住肚肚）", "（小脚停停）", "（慢慢靠近）", "（抱抱肚肚）",
    "（翻身贴贴）", "（悄悄贴住）", "（轻轻蹭蹭）", "（呼噜呼噜）",
    "（陪你一会）", "（安静呼吸）", "（小手握住）", "（软软靠着）",
    "（慢慢打盹）", "（肚肚贴贴）", "（小脚收好）", "（轻轻贴住）",
    "（睡成一团）", "（靠近妈妈）", "（小手抱抱）", "（呼呼陪着）",
    "（软软陪着）", "（小脚停一停）",
)

private val TIRED_BABY_REACTIONS = listOf(
    "☁️", "🤍", "🌙", "🫧", "✨", "💤", "🌿", "💛", "🩷", "💫",
    "（轻轻蹭蹭）", "（慢慢呼呼）", "（呼噜呼噜）", "（陪你歇歇）",
    "（软软打盹）", "（小脚收好）", "（慢慢翻身）", "（睡成一团）",
    "（小手贴贴）", "（软软靠着）", "（安静打盹）", "（肚肚抱抱）",
    "（轻轻摇摇）", "（呼呼睡着）", "（贴住肚肚）", "（翻身贴贴）",
    "（小手握握）", "（慢慢眨眼）", "（轻轻呼吸）", "（伸个小懒）",
    "（小脚缩缩）", "（安静靠着）", "（肚肚贴贴）", "（咕噜一下）",
    "（软软贴住）", "（慢慢漂漂）", "（小手收好）", "（轻轻打盹）",
    "（呼噜一下）", "（抱抱肚肚）", "（一起慢慢）",
)

private val MOVEMENT_BABY_REACTIONS = listOf(
    "✨", "🫧", "😊", "🌷", "🤍", "💫", "🌟", "💕", "💛", "🥰",
    "（小脚踢踢）", "（噗噜噗噜）", "（小手挥挥）", "（蹬蹬腿）",
    "（轻轻冒泡）", "（转个小圈）", "（翻了个身）", "（肚肚敲敲）",
    "（软软动动）", "（小脚晃晃）", "（伸伸小手）", "（咕噜一下）",
    "（轻轻踢踢）", "（冒个小泡）", "（转了个圈）", "（小手拍拍）",
    "（翻身贴贴）", "（肚肚冒泡）", "（伸个小懒）", "（蹬了蹬腿）",
    "（悄悄回应）", "（小脚点点）", "（咕噜翻身）", "（轻轻摆摆）",
    "（软软伸手）", "（贴贴肚肚）", "（踢了两下）", "（晃晃小脚）",
    "（小手贴贴）", "（翻身冒泡）", "（轻轻点点）",
)

private val ALL_BABY_REACTIONS =
    HAPPY_BABY_REACTIONS + NEUTRAL_BABY_REACTIONS + SAD_BABY_REACTIONS + TIRED_BABY_REACTIONS + MOVEMENT_BABY_REACTIONS
