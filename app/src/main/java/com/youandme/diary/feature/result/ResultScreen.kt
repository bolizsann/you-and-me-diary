package com.youandme.diary.feature.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.GentleCard
import com.youandme.diary.core.designsystem.OutlineAction
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryNote
import com.youandme.diary.domain.model.DiarySlide
import com.youandme.diary.domain.model.DiaryTheme

@Composable
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
    onToggleEdit: () -> Unit,
    onToggleSharePreview: () -> Unit,
) {
    val favoriteId = MockDiaryRepository.favoriteId(entry.id, slide.id)
    val currentNote = selectedNoteIndex?.let { slide.notes.getOrNull(it) }
    var editedText by rememberSaveable(entry.id, slide.id, selectedNoteIndex, noteMode) {
        mutableStateOf(if (noteMode == "self") currentNote?.selfText.orEmpty() else currentNote?.babyText.orEmpty())
    }

    DiaryPage(
        title = entry.dateLabel,
        theme = theme,
        onBack = onBack,
        modifier = Modifier.testTag("result-screen"),
    ) {
        Text("已自动记入时间线", color = argb(theme.muted), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(entry.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        DiarySlideCard(
            entry = entry,
            slide = slide,
            slideIndex = slideIndex,
            theme = theme,
            selectedNoteIndex = selectedNoteIndex,
            onSelectNote = onSelectNote,
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPreviousSlide) { Text("上一张") }
            Text("${slideIndex + 1} / ${entry.slides.size}", color = argb(theme.muted))
            TextButton(onClick = onNextSlide) { Text("下一张") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineAction(
                label = if (favoriteIds.contains(favoriteId)) "已收藏" else "收藏",
                theme = theme,
                modifier = Modifier
                    .weight(1f)
                    .testTag("favorite-button"),
                onClick = onToggleFavorite,
            )
            OutlineAction(
                label = "分享预览",
                theme = theme,
                modifier = Modifier.weight(1f),
                onClick = onToggleSharePreview,
            )
        }

        Spacer(Modifier.height(12.dp))
        GentleCard(theme = theme) {
            Text("给你的小安慰", color = argb(theme.muted), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(entry.comfortText, fontSize = 15.sp, lineHeight = 23.sp)
        }

        Spacer(Modifier.height(12.dp))
        NotePanel(
            note = currentNote,
            noteMode = noteMode,
            editedText = editedText,
            isEditing = isEditingNote,
            theme = theme,
            onModeChange = onNoteModeChange,
            onEditChange = { editedText = it },
            onToggleEdit = onToggleEdit,
        )

        if (sharePreviewVisible) {
            Spacer(Modifier.height(16.dp))
            SharePreview(entry, slide, currentNote, noteMode, editedText, theme)
        }
    }
}

@Composable
private fun DiarySlideCard(
    entry: DiaryEntry,
    slide: DiarySlide,
    slideIndex: Int,
    theme: DiaryTheme,
    selectedNoteIndex: Int?,
    onSelectNote: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(argb(slide.gradientStart), argb(slide.gradientEnd))))
            .testTag("diary-slide-card"),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .fillMaxWidth(0.78f),
        ) {
            Text(entry.moodEmoji, fontSize = 24.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                slide.quote,
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(slide.caption, color = Color.White.copy(alpha = 0.90f), fontSize = 14.sp, lineHeight = 21.sp)
        }

        Text(
            text = "0${slideIndex + 1}",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
        )

        slide.notes.forEachIndexed { index, note ->
            Button(
                onClick = { onSelectNote(index) },
                modifier = Modifier
                    .offset(
                        x = maxWidth * note.x - 18.dp,
                        y = maxHeight * note.y - 18.dp,
                    )
                    .size(34.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedNoteIndex == index) Color.White else Color.White.copy(alpha = 0.82f),
                    contentColor = argb(theme.primary),
                ),
            ) {
                Text("${index + 1}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NotePanel(
    note: DiaryNote?,
    noteMode: String,
    editedText: String,
    isEditing: Boolean,
    theme: DiaryTheme,
    onModeChange: (String) -> Unit,
    onEditChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
) {
    GentleCard(theme = theme) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(note?.label ?: "图片解释", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { onModeChange("self") }) {
                Text(if (noteMode == "self") "我*" else "我")
            }
            TextButton(onClick = { onModeChange("baby") }) {
                Text(if (noteMode == "baby") "宝*" else "宝")
            }
            TextButton(onClick = onToggleEdit) {
                Text(if (isEditing) "返回" else "编辑")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (isEditing) {
            OutlinedTextField(
                value = editedText,
                onValueChange = onEditChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(16.dp),
            )
        } else {
            Text(
                text = editedText.ifBlank { "选择一处注释。" },
                fontSize = 15.sp,
                lineHeight = 23.sp,
            )
        }
    }
}

@Composable
private fun SharePreview(
    entry: DiaryEntry,
    slide: DiarySlide,
    note: DiaryNote?,
    noteMode: String,
    editedText: String,
    theme: DiaryTheme,
) {
    GentleCard(theme = theme, modifier = Modifier.testTag("share-preview")) {
        Text("长图预览", color = argb(theme.muted), fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(argb(slide.gradientStart), argb(slide.gradientEnd)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                slide.quote,
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(28.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(entry.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(if (noteMode == "self") "给自己的解释" else "给宝宝的解释", color = argb(theme.muted))
        Spacer(Modifier.height(6.dp))
        Text(editedText.ifBlank { note?.selfText.orEmpty() }, lineHeight = 25.sp)
        Spacer(Modifier.height(10.dp))
        Text("今天先展示预览占位，后续接入 Bitmap 导出和 Android ShareSheet。", color = argb(theme.muted), fontSize = 13.sp)
    }
}
