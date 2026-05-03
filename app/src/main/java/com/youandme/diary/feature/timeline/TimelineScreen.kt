package com.youandme.diary.feature.timeline

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
    var visibleMonthText by rememberSaveable { mutableStateOf(selectedEntry.dateId.take(7)) }
    val visibleMonth = remember(visibleMonthText) { YearMonth.parse(visibleMonthText) }
    val entriesInMonth = entries.filter { it.dateId.startsWith(visibleMonthText) }
    val entryByDay = entriesInMonth.associateBy { it.dateId.takeLast(2).toInt() }
    val displayedEntry = entriesInMonth.firstOrNull { it.id == selectedEntry.id } ?: entriesInMonth.firstOrNull()

    DiaryPage(
        title = "时间线",
        theme = theme,
        onBack = onBack,
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
                fontSize = 22.sp,
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
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        Spacer(Modifier.height(8.dp))

        val leadingEmptyDays = visibleMonth.atDay(1).dayOfWeek.value - 1
        val cells = List(leadingEmptyDays) { null } + (1..visibleMonth.lengthOfMonth()).map { it }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    week.forEach { day ->
                        val entry = day?.let { entryByDay[it] }
                        val isSelected = entry?.id == selectedEntry.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        isSelected -> argb(theme.primary).copy(alpha = 0.22f)
                                        entry != null -> argb(entry.moodColor).copy(alpha = 0.16f)
                                        else -> Color.White.copy(alpha = 0.35f)
                                    },
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) argb(theme.primary) else Color.White.copy(alpha = 0.38f),
                                    ),
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable(enabled = entry != null) { entry?.let(onSelectEntry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day?.toString().orEmpty(), fontSize = 13.sp)
                                Text(entry?.moodEmoji ?: "", fontSize = 13.sp)
                            }
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        GentleCard(theme = theme) {
            if (displayedEntry == null) {
                Text("这个月还没有记录", color = argb(theme.muted), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text("切换回有颜色的小圆点月份，或从今天开始写一页。", lineHeight = 24.sp)
            } else {
                Text(displayedEntry.dateLabel, color = argb(theme.muted), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(displayedEntry.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(displayedEntry.timelineSummary, lineHeight = 24.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onSelectEntry(displayedEntry)
                        onOpenResult()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = argb(theme.primary)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("打开这一天")
                }
            }
        }
    }
}
