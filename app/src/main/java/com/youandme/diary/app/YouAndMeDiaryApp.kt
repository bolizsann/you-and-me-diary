package com.youandme.diary.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryThemes
import com.youandme.diary.feature.generating.GeneratingScreen
import com.youandme.diary.feature.home.HomeScreen
import com.youandme.diary.feature.memory.MemoryScreen
import com.youandme.diary.feature.record.RecordScreen
import com.youandme.diary.feature.result.ResultScreen
import com.youandme.diary.feature.settings.SettingsScreen
import com.youandme.diary.feature.timeline.TimelineScreen

@Composable
fun YouAndMeDiaryApp(
    viewModel: DiaryAppViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val entries = uiState.entries
    val theme = DiaryThemes.all.firstOrNull { it.id == uiState.themeId } ?: DiaryThemes.Rose
    val selectedEntry = entries.firstOrNull { it.id == uiState.selectedEntryId } ?: entries.last()
    val selectedSlideIndex = uiState.selectedSlideIndex.coerceIn(0, selectedEntry.slides.lastIndex)
    val selectedSlide = selectedEntry.slides.getOrElse(selectedSlideIndex) { selectedEntry.slides.first() }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = argb(theme.primary),
            secondary = argb(theme.accent),
            background = argb(theme.background),
            surface = argb(theme.surface),
            onPrimary = Color.White,
            onSecondary = argb(theme.text),
            onBackground = argb(theme.text),
            onSurface = argb(theme.text),
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = argb(theme.background),
        ) {
            Scaffold(
                containerColor = argb(theme.background),
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(argb(theme.background)),
                ) {
                    when (uiState.route) {
                        AppScreen.Record.name -> RecordScreen(
                            text = uiState.recordText,
                            imagePath = uiState.recordImagePath,
                            imageRoiScale = uiState.recordImageRoiScale,
                            imageRoiOffsetX = uiState.recordImageRoiOffsetX,
                            imageRoiOffsetY = uiState.recordImageRoiOffsetY,
                            theme = theme,
                            onTextChange = viewModel::updateRecordText,
                            onImageSelected = viewModel::selectRecordImage,
                            onImageRoiChange = viewModel::updateRecordImageRoi,
                            onBack = viewModel::openHome,
                            onSubmit = viewModel::submitRecord,
                        )

                        AppScreen.Generating.name -> GeneratingScreen(theme)

                        AppScreen.Result.name -> ResultScreen(
                            entry = selectedEntry,
                            slide = selectedSlide,
                            slideIndex = selectedSlideIndex,
                            theme = theme,
                            favoriteIds = uiState.favoriteIds.toList(),
                            selectedNoteIndex = uiState.selectedNoteIndex,
                            noteMode = uiState.noteMode,
                            isEditingNote = uiState.isEditingNote,
                            sharePreviewVisible = uiState.sharePreviewVisible,
                            onBack = viewModel::openHome,
                            onPreviousSlide = { viewModel.previousSlide(selectedEntry.slides.size) },
                            onNextSlide = { viewModel.nextSlide(selectedEntry.slides.size) },
                            onSelectNote = viewModel::selectNote,
                            onToggleFavorite = { viewModel.toggleFavorite(selectedEntry.id, selectedSlide.id) },
                            onNoteModeChange = viewModel::changeNoteMode,
                            onEditTextChange = { text ->
                                viewModel.updateNoteText(
                                    entryId = selectedEntry.id,
                                    slideId = selectedSlide.id,
                                    noteIndex = uiState.selectedNoteIndex,
                                    text = text,
                                )
                            },
                            onEditDiaryContentChange = { title, text ->
                                viewModel.updateDiaryTitleAndText(
                                    entryId = selectedEntry.id,
                                    slideId = selectedSlide.id,
                                    noteIndex = uiState.selectedNoteIndex,
                                    title = title,
                                    text = text,
                                )
                            },
                            onToggleEdit = viewModel::toggleEdit,
                            onToggleSharePreview = viewModel::toggleSharePreview,
                            onDeleteSlide = viewModel::deleteCurrentSlide,
                        )

                        AppScreen.Timeline.name -> TimelineScreen(
                            entries = entries,
                            selectedEntry = selectedEntry,
                            theme = theme,
                            onBack = viewModel::openHome,
                            onSelectEntry = viewModel::selectEntry,
                            onOpenResult = viewModel::openResult,
                        )

                        AppScreen.Memory.name -> MemoryScreen(
                            entries = entries,
                            favoriteIds = uiState.favoriteIds.toList(),
                            theme = theme,
                            onBack = viewModel::openHome,
                            onOpenSlide = viewModel::openSlide,
                        )

                        AppScreen.Settings.name -> SettingsScreen(
                            username = uiState.username,
                            dueDate = uiState.dueDate,
                            themeId = uiState.themeId,
                            theme = theme,
                            onUsernameChange = viewModel::updateUsername,
                            onDueDateChange = viewModel::updateDueDate,
                            onThemeChange = viewModel::updateTheme,
                            onClearLocalData = viewModel::clearLocalTestData,
                            onBack = viewModel::openHome,
                        )

                        else -> HomeScreen(
                            username = uiState.username,
                            theme = theme,
                            onRecord = viewModel::openRecord,
                            onTimeline = viewModel::openTimeline,
                            onMemory = viewModel::openMemory,
                            onSettings = viewModel::openSettings,
                        )
                    }
                }
            }
        }
    }
}
