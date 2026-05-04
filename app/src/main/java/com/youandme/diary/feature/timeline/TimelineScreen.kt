package com.youandme.diary.feature.timeline

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.GentleCard
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TimelineScreen(
    entries: List<DiaryEntry>,
    selectedEntry: DiaryEntry,
    theme: DiaryTheme,
    onBack: () -> Unit,
    onSelectEntry: (DiaryEntry) -> Unit,
    onOpenResult: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val todayMonthText = remember(today) { YearMonth.from(today).toString() }
    var visibleMonthText by rememberSaveable { mutableStateOf(todayMonthText) }
    val visibleMonth = remember(visibleMonthText) { YearMonth.parse(visibleMonthText) }
    val entriesInMonth = entries.filter { it.dateId.startsWith(visibleMonthText) }
    val entryByDay = entriesInMonth.associateBy { it.dateId.takeLast(2).toInt() }
    val displayedEntry = entriesInMonth.firstOrNull { it.id == selectedEntry.id } ?: entriesInMonth.firstOrNull()
    val isTodayVisible = visibleMonth == YearMonth.from(today)

    DiaryPage(
        title = "时间线",
        theme = theme,
        onBack = onBack,
        scrollEnabled = false,
        modifier = Modifier.testTag("timeline-screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = {
                    visibleMonthText = visibleMonth.minusMonths(1).toString()
                },
                modifier = Modifier.testTag("timeline-previous-month"),
            ) {
                Text("上月")
            }
            Text(
                "${visibleMonth.year} 年 ${visibleMonth.monthValue} 月",
                modifier = Modifier.testTag("timeline-current-month"),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(
                onClick = {
                    visibleMonthText = visibleMonth.plusMonths(1).toString()
                },
                modifier = Modifier.testTag("timeline-next-month"),
            ) {
                Text("下月")
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = argb(theme.muted),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        val leadingEmptyDays = visibleMonth.atDay(1).dayOfWeek.value - 1
        val cells = List(leadingEmptyDays) { null } + (1..visibleMonth.lengthOfMonth()).map { it }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    week.forEach { day ->
                        val entry = day?.let { entryByDay[it] }
                        val isSelected = entry?.id == selectedEntry.id
                        val isToday = isTodayVisible && day == today.dayOfMonth
                        val moodStyle = entry?.let { timelineMoodStyle(it, theme) }
                        val borderWidth by animateDpAsState(
                            targetValue = if (isSelected) 2.dp else 1.dp,
                            label = "timeline-day-border-width",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    when {
                                        moodStyle != null -> moodStyle.color.copy(alpha = 0.18f)
                                        isToday -> argb(theme.accent).copy(alpha = 0.11f)
                                        else -> Color.White.copy(alpha = 0.32f)
                                    },
                                )
                                .border(
                                    BorderStroke(
                                        borderWidth,
                                        when {
                                            isSelected -> argb(theme.primary)
                                            isToday -> argb(theme.accent).copy(alpha = 0.72f)
                                            moodStyle != null -> moodStyle.color.copy(alpha = 0.42f)
                                            else -> Color.White.copy(alpha = 0.34f)
                                        },
                                    ),
                                    RoundedCornerShape(13.dp),
                                )
                                .clickable(enabled = entry != null) { entry?.let(onSelectEntry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.align(Alignment.Center),
                            ) {
                                Text(day?.toString().orEmpty(), fontSize = 12.sp, lineHeight = 13.sp)
                                if (entry != null) {
                                    Text(moodStyle?.emoji.orEmpty(), fontSize = 12.sp, lineHeight = 13.sp)
                                }
                            }
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .size(4.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(argb(theme.accent)),
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GentleCard(theme = theme) {
            if (displayedEntry == null) {
                Text("这个月还没有记录", color = argb(theme.muted), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("今天用小圆点标出来。等写下一页，它会在这里变成有颜色的记号。", fontSize = 14.sp, lineHeight = 21.sp)
            } else {
                Text(displayedEntry.dateLabel, color = argb(theme.muted), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(displayedEntry.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(displayedEntry.timelineSummary, fontSize = 14.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSelectEntry(displayedEntry)
                        onOpenResult()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = argb(theme.primary)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("打开这一天")
                }
            }
        }
    }
}

private data class TimelineMoodStyle(
    val emoji: String,
    val color: Color,
)

private fun timelineMoodStyle(entry: DiaryEntry, theme: DiaryTheme): TimelineMoodStyle {
    val text = listOf(entry.title, entry.timelineSummary, entry.comfortText, entry.rawText)
        .joinToString(separator = " ")
    return when {
        text.hasAny("胎动", "回应", "hello", "认识彼此", "亮") ->
            TimelineMoodStyle("♡", argb(0xFF87A9BD))

        text.hasAny("胃口", "晚餐", "温水", "舒服", "吃") ->
            TimelineMoodStyle("◦", argb(0xFFD6A06F))

        text.hasAny("工作", "消耗", "标准", "沟通", "边界") ->
            TimelineMoodStyle("✦", argb(0xFF92A88F))

        text.hasAny("累", "恢复", "散步", "慢", "安顿") ->
            TimelineMoodStyle("☁", argb(0xFFD88B91))

        else -> TimelineMoodStyle(entry.moodEmoji, argb(entry.moodColor).takeUnless { entry.moodColor == 0L } ?: argb(theme.primary))
    }
}

private fun String.hasAny(vararg keywords: String): Boolean =
    keywords.any { contains(it, ignoreCase = true) }
