package com.youandme.diary.feature.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.DiaryPreviewTheme
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.GentleCard
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryIds
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes

@Composable
fun MemoryScreen(
    entries: List<DiaryEntry>,
    favoriteIds: List<String>,
    theme: DiaryTheme,
    onBack: () -> Unit,
    onOpenSlide: (DiaryEntry, Int) -> Unit,
) {
    val favorites = entries.flatMap { entry ->
        entry.slides.mapIndexedNotNull { index, slide ->
            val id = DiaryIds.favoriteId(entry.id, slide.id)
            if (favoriteIds.contains(id)) Triple(entry, index, slide) else null
        }
    }

    DiaryPage(
        title = "纪念册",
        theme = theme,
        onBack = onBack,
        modifier = Modifier.testTag("memory-screen"),
    ) {
        Text("${favorites.size} 页已收藏", color = argb(theme.muted))
        Spacer(Modifier.height(16.dp))
        if (favorites.isEmpty()) {
            GentleCard(theme = theme) {
                Text("还没有收藏。回到结果页，把想留给未来的那一张放进来。", lineHeight = 25.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                favorites.forEach { (entry, index, slide) ->
                    GentleCard(
                        theme = theme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSlide(entry, index) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(78.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(argb(slide.gradientStart), argb(slide.gradientEnd)),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(entry.moodEmoji, fontSize = 24.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.dateLabel, color = argb(theme.muted), fontSize = 13.sp)
                                Text(slide.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                Text(
                                    slide.caption,
                                    color = argb(theme.muted),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "纪念册", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun MemoryScreenPreview() {
    val theme = DiaryThemes.Rose
    DiaryPreviewTheme(theme = theme) {
        MemoryScreen(
            entries = MockDiaryRepository.entries,
            favoriteIds = MockDiaryRepository.defaultFavoriteSlideIds().toList(),
            theme = theme,
            onBack = {},
            onOpenSlide = { _, _ -> },
        )
    }
}
