package com.youandme.diary.domain.model

data class DiaryEntry(
    val id: String,
    val dateId: String,
    val dateLabel: String,
    val title: String,
    val moodEmoji: String,
    val moodColor: Long,
    val comfortText: String,
    val timelineSummary: String,
    val slides: List<DiarySlide>,
)

data class DiarySlide(
    val id: String,
    val title: String,
    val quote: String,
    val caption: String,
    val gradientStart: Long,
    val gradientEnd: Long,
    val notes: List<DiaryNote>,
    val defaultFavorite: Boolean = false,
)

data class DiaryNote(
    val label: String,
    val selfText: String,
    val babyText: String,
    val x: Float,
    val y: Float,
)

data class DiaryTheme(
    val id: String,
    val label: String,
    val background: Long,
    val surface: Long,
    val primary: Long,
    val accent: Long,
    val text: Long,
    val muted: Long,
)

object DiaryThemes {
    val Rose = DiaryTheme(
        id = "rose",
        label = "奶油雾粉",
        background = 0xFFFBF4EC,
        surface = 0xFFFFFCF8,
        primary = 0xFFD88B91,
        accent = 0xFF95AB95,
        text = 0xFF564239,
        muted = 0xFF8A7262,
    )

    val Sage = DiaryTheme(
        id = "sage",
        label = "鼠尾草",
        background = 0xFFF3F2EB,
        surface = 0xFFFCFCF8,
        primary = 0xFF92A88F,
        accent = 0xFFD2B38A,
        text = 0xFF455143,
        muted = 0xFF6E7A6C,
    )

    val Mist = DiaryTheme(
        id = "mist",
        label = "晨雾蓝",
        background = 0xFFEFF4F7,
        surface = 0xFFFCFEFF,
        primary = 0xFF87A9BD,
        accent = 0xFFD7C19F,
        text = 0xFF415362,
        muted = 0xFF6E8493,
    )

    val Apricot = DiaryTheme(
        id = "apricot",
        label = "落日晚杏",
        background = 0xFFFCF1E7,
        surface = 0xFFFFFBF7,
        primary = 0xFFD6A06F,
        accent = 0xFFD89B8F,
        text = 0xFF5E4537,
        muted = 0xFF967661,
    )

    val all = listOf(Rose, Sage, Mist, Apricot)
}
