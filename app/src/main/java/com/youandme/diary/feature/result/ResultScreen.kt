package com.youandme.diary.feature.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.CircleIconAction
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.DiaryPreviewTheme
import com.youandme.diary.core.designsystem.GentleCard
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryIds
import com.youandme.diary.domain.model.DiarySlide
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes

@Composable
@Suppress("UNUSED_PARAMETER")
fun ResultScreen(
    entry: DiaryEntry,
    slide: DiarySlide,
    slideIndex: Int,
    theme: DiaryTheme,
    favoriteIds: List<String>,
    selectedNoteIndex: Int?,
    noteMode: String,
    isEditingNote: Boolean,
    sharePreviewVisible: Boolean,
    onBack: () -> Unit,
    onPreviousSlide: () -> Unit,
    onNextSlide: () -> Unit,
    onSelectNote: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onNoteModeChange: (String) -> Unit,
    onEditTextChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onToggleSharePreview: () -> Unit,
) {
    val currentNote = selectedNoteIndex?.let { slide.notes.getOrNull(it) } ?: slide.notes.firstOrNull()
    val diaryText = currentNote?.selfText
        ?.ifBlank { entry.rawText.ifBlank { entry.timelineSummary } }
        ?: entry.rawText.ifBlank { entry.timelineSummary }
    val babyText = currentNote?.babyText?.ifBlank { entry.comfortText } ?: entry.comfortText
    val isFavorite = entry.slides.any { entrySlide ->
        favoriteIds.contains(DiaryIds.favoriteId(entry.id, entrySlide.id))
    }

    DiaryPage(
        theme = theme,
        modifier = Modifier.testTag("result-screen"),
    ) {
        ResultTopBar(
            dateLabel = entry.dateLabel,
            theme = theme,
            isFavorite = isFavorite,
            sharePreviewVisible = sharePreviewVisible,
            onBack = onBack,
            onToggleFavorite = onToggleFavorite,
            onToggleSharePreview = onToggleSharePreview,
        )
        Spacer(Modifier.height(8.dp))

        ResultImageCard(
            entry = entry,
            slide = slide,
            slideIndex = slideIndex,
            slideCount = entry.slides.size,
            theme = theme,
            onPreviousSlide = onPreviousSlide,
            onNextSlide = onNextSlide,
        )
        Spacer(Modifier.height(8.dp))
        SlideDots(count = entry.slides.size, selectedIndex = slideIndex, theme = theme)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                entry.title,
                modifier = Modifier.weight(1f),
                fontSize = 22.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold,
            )
            TextButton(
                onClick = onToggleEdit,
                modifier = Modifier
                    .height(34.dp)
                    .testTag("note-edit-toggle"),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text(if (isEditingNote) "完成" else "编辑", color = argb(theme.muted), fontSize = 13.sp)
            }
        }

        if (isEditingNote) {
            OutlinedTextField(
                value = diaryText,
                onValueChange = onEditTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note-editor"),
                minLines = 4,
                shape = RoundedCornerShape(16.dp),
            )
        } else {
            Text(
                text = diaryText,
                fontSize = 17.sp,
                lineHeight = 28.sp,
            )
        }

        Spacer(Modifier.height(14.dp))
        GentleCard(theme = theme) {
            Text("宝宝说", color = argb(theme.muted), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(babyText, fontSize = 15.sp, lineHeight = 24.sp)
        }

        if (sharePreviewVisible) {
            Spacer(Modifier.height(12.dp))
            GentleCard(theme = theme, modifier = Modifier.testTag("share-preview")) {
                Text("分享预览已准备好", color = argb(theme.muted), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text("今天先保留预览占位，后续接入长图导出和系统分享。", fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
private fun ResultTopBar(
    dateLabel: String,
    theme: DiaryTheme,
    isFavorite: Boolean,
    sharePreviewVisible: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleSharePreview: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircleIconAction(
            label = "<",
            theme = theme,
            modifier = Modifier
                .testTag("page-back-button"),
            onClick = onBack,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(dateLabel, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text("已自动记入时间线", color = argb(theme.muted), fontSize = 12.sp)
        }
        CircleIconAction(
            label = if (isFavorite) "★" else "☆",
            theme = theme,
            modifier = Modifier
                .testTag("favorite-button"),
            contentColor = if (isFavorite) argb(0xFFD6A06F) else argb(theme.text),
            onClick = onToggleFavorite,
        )
        CircleIconAction(
            label = if (sharePreviewVisible) "✓" else "↗",
            theme = theme,
            onClick = onToggleSharePreview,
        )
    }
}

@Composable
private fun ResultImageCard(
    entry: DiaryEntry,
    slide: DiarySlide,
    slideIndex: Int,
    slideCount: Int,
    theme: DiaryTheme,
    onPreviousSlide: () -> Unit,
    onNextSlide: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(548.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(argb(theme.surface))
            .pointerInput(slide.id) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        when {
                            totalDrag < -48f -> onNextSlide()
                            totalDrag > 48f -> onPreviousSlide()
                        }
                    },
                )
            }
            .testTag("diary-slide-card"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.44f)
                    .background(Brush.linearGradient(listOf(argb(slide.gradientStart), argb(slide.gradientEnd)))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(26.dp)) {
                    Text(
                        slide.quote,
                        color = Color.White,
                        fontSize = 30.sp,
                        lineHeight = 39.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(entry.moodEmoji, color = Color.White.copy(alpha = 0.70f), fontSize = 22.sp)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.56f)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                argb(slide.gradientStart).copy(alpha = 0.26f),
                                argb(slide.gradientEnd).copy(alpha = 0.44f),
                            ),
                        ),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(34.dp)
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.42f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 140.dp, topEnd = 140.dp))
                        .background(Color.White.copy(alpha = 0.30f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.72f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)), CircleShape),
                )
                Text(
                    slide.caption,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.50f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Text("${slideIndex + 1}/$slideCount", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SlideDots(
    count: Int,
    selectedIndex: Int,
    theme: DiaryTheme,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selectedIndex) 7.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (index == selectedIndex) argb(theme.primary) else argb(theme.muted).copy(alpha = 0.24f)),
            )
            if (index != count - 1) Spacer(Modifier.width(7.dp))
        }
    }
}

@Preview(name = "结果页 / 大图", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun ResultScreenPreview() {
    val theme = DiaryThemes.Rose
    val entry = MockDiaryRepository.entries.last()
    val slide = entry.slides.first()
    DiaryPreviewTheme(theme = theme) {
        ResultScreen(
            entry = entry,
            slide = slide,
            slideIndex = 0,
            theme = theme,
            favoriteIds = MockDiaryRepository.defaultFavoriteSlideIds().toList(),
            selectedNoteIndex = 0,
            noteMode = "self",
            isEditingNote = false,
            sharePreviewVisible = false,
            onBack = {},
            onPreviousSlide = {},
            onNextSlide = {},
            onSelectNote = {},
            onToggleFavorite = {},
            onNoteModeChange = {},
            onEditTextChange = {},
            onToggleEdit = {},
            onToggleSharePreview = {},
        )
    }
}
