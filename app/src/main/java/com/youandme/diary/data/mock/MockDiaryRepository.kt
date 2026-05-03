package com.youandme.diary.data.mock

import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryIds
import com.youandme.diary.domain.model.DiaryNote
import com.youandme.diary.domain.model.DiarySlide

object MockDiaryRepository {
    val entries: List<DiaryEntry> = listOf(
        DiaryEntry(
            id = "entry-2026-04-23",
            dateId = "2026-04-23",
            dateLabel = "4 月 23 日",
            title = "午后一点点恢复",
            moodEmoji = "☁",
            moodColor = 0xFFD88B91,
            comfortText = "今天不需要证明自己很厉害。能把身体和情绪都安顿下来，就已经是在照顾你们两个人。",
            timelineSummary = "午饭后有点累，但傍晚散步时轻了一些。",
            slides = listOf(
                DiarySlide(
                    id = "slide-restore",
                    title = "恢复感",
                    quote = "今天慢一点，也算一种认真。",
                    caption = "这一页像把下午那一点点松动留下来，不宏大，但很真实。",
                    gradientStart = 0xFFE4B8B9,
                    gradientEnd = 0xFF95AB95,
                    defaultFavorite = true,
                    notes = listOf(
                        DiaryNote(
                            label = "恢复感",
                            selfText = "你的身体今天很努力，允许节奏慢下来不是退步，是在重新找回平衡。",
                            babyText = "宝宝知道妈妈今天有点累，也知道妈妈已经很温柔地照顾我们了。",
                            x = 0.32f,
                            y = 0.36f,
                        ),
                        DiaryNote(
                            label = "小口呼吸",
                            selfText = "先把今晚变简单：热水、早睡、少一点任务，都可以成为今天的结束方式。",
                            babyText = "我们一起把今天收起来，明天再轻轻开始。",
                            x = 0.70f,
                            y = 0.58f,
                        ),
                    ),
                ),
                DiarySlide(
                    id = "slide-evening",
                    title = "傍晚的光",
                    quote = "谢谢你，用自己的方式陪着我。",
                    caption = "像一张可以放进纪念册的小照片。",
                    gradientStart = 0xFFE8CFA4,
                    gradientEnd = 0xFFD79A91,
                    notes = listOf(
                        DiaryNote(
                            label = "礼物感",
                            selfText = "这不是一个完美的一天，但它有一个可以被记住的瞬间。",
                            babyText = "以后我们也许会一起翻到这里，说这是妈妈和宝宝的小小 hello。",
                            x = 0.28f,
                            y = 0.30f,
                        ),
                    ),
                ),
            ),
        ),
        DiaryEntry(
            id = "entry-2026-04-24",
            dateId = "2026-04-24",
            dateLabel = "4 月 24 日",
            title = "把标准放低一点",
            moodEmoji = "✦",
            moodColor = 0xFF92A88F,
            comfortText = "今天可以不用做那个总是把事情安排好的人。把标准放低一点，也是在给身体腾位置。",
            timelineSummary = "工作沟通多，晚上决定早点躺下。",
            slides = listOf(
                DiarySlide(
                    id = "slide-workday",
                    title = "工作日",
                    quote = "被消耗过，也还是好好走到了晚上。",
                    caption = "这页记录的是一种安静的坚持。",
                    gradientStart = 0xFF92A88F,
                    gradientEnd = 0xFFD2B38A,
                    notes = listOf(
                        DiaryNote(
                            label = "边界",
                            selfText = "你不需要把每个问题都当天解决。先照顾当下的身体，是更重要的优先级。",
                            babyText = "宝宝陪妈妈一起练习：不是每件事都要马上变好。",
                            x = 0.62f,
                            y = 0.42f,
                        ),
                    ),
                ),
            ),
        ),
        DiaryEntry(
            id = "entry-2026-04-25",
            dateId = "2026-04-25",
            dateLabel = "4 月 25 日",
            title = "一点点胃口",
            moodEmoji = "◦",
            moodColor = 0xFFD6A06F,
            comfortText = "能吃下一点喜欢的东西，就已经值得被记录。照顾自己不一定隆重，有时只是多喝几口温水。",
            timelineSummary = "胃口稍微回来，晚餐吃得慢但舒服。",
            slides = listOf(
                DiarySlide(
                    id = "slide-food",
                    title = "晚餐",
                    quote = "小小的胃口，也是一种好消息。",
                    caption = "把这一口舒服留下来，提醒自己今天并不全是难受。",
                    gradientStart = 0xFFF0DCC6,
                    gradientEnd = 0xFFD6A06F,
                    notes = listOf(
                        DiaryNote(
                            label = "被照顾",
                            selfText = "你已经在用很细小的方式照顾自己了，这些都算数。",
                            babyText = "宝宝也收到这份温热啦，我们慢慢来。",
                            x = 0.44f,
                            y = 0.54f,
                        ),
                    ),
                ),
            ),
        ),
        DiaryEntry(
            id = "entry-2026-04-26",
            dateId = "2026-04-26",
            dateLabel = "4 月 26 日",
            title = "一次小小胎动",
            moodEmoji = "♡",
            moodColor = 0xFF87A9BD,
            comfortText = "那一下很轻，但足够把今天变得不一样。你没有白白辛苦，你们正在一点点认识彼此。",
            timelineSummary = "下午感觉到一点胎动，心里忽然亮了一下。",
            slides = listOf(
                DiarySlide(
                    id = "slide-kick",
                    title = "小小胎动",
                    quote = "那一下轻轻的，好像在说 hello。",
                    caption = "今天最值得留下来的，是这个很小很确定的回应。",
                    gradientStart = 0xFF87A9BD,
                    gradientEnd = 0xFFD7C19F,
                    defaultFavorite = true,
                    notes = listOf(
                        DiaryNote(
                            label = "连接感",
                            selfText = "你感受到的不只是身体变化，也是你们之间第一次更清楚的互相回应。",
                            babyText = "妈妈，我在这里。虽然还很小，但我已经在用自己的方式和你打招呼。",
                            x = 0.52f,
                            y = 0.38f,
                        ),
                        DiaryNote(
                            label = "值得记住",
                            selfText = "把这一刻记下来，未来的你会感谢今天愿意停下来感受的自己。",
                            babyText = "等以后我们一起看这一页，就知道这一天是我们的秘密纪念日。",
                            x = 0.25f,
                            y = 0.70f,
                        ),
                    ),
                ),
            ),
        ),
    )

    fun defaultFavoriteSlideIds(): Set<String> =
        entries.flatMap { entry ->
            entry.slides.filter { it.defaultFavorite }.map { slide -> favoriteId(entry.id, slide.id) }
        }.toSet()

    fun favoriteId(entryId: String, slideId: String): String = DiaryIds.favoriteId(entryId, slideId)
}

class FavoriteStore(initialIds: Set<String> = emptySet()) {
    private val favoriteIds = initialIds.toMutableSet()

    fun all(): Set<String> = favoriteIds.toSet()

    fun isFavorite(id: String): Boolean = id in favoriteIds

    fun toggle(id: String): Boolean {
        return if (favoriteIds.contains(id)) {
            favoriteIds.remove(id)
            false
        } else {
            favoriteIds.add(id)
            true
        }
    }
}
