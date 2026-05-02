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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.core.designsystem.floorMod
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.DiaryThemes
import com.youandme.diary.feature.generating.GeneratingScreen
import com.youandme.diary.feature.home.HomeScreen
import com.youandme.diary.feature.memory.MemoryScreen
import com.youandme.diary.feature.record.RecordScreen
import com.youandme.diary.feature.result.ResultScreen
import com.youandme.diary.feature.settings.SettingsScreen
import com.youandme.diary.feature.timeline.TimelineScreen
import kotlinx.coroutines.delay

@Composable
fun YouAndMeDiaryApp() {
    val entries = remember { MockDiaryRepository.entries }
    var route by rememberSaveable { mutableStateOf(AppScreen.Home.name) }
    var selectedEntryId by rememberSaveable { mutableStateOf(entries.last().id) }
    var selectedSlideIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedNoteIndex by rememberSaveable { mutableStateOf<Int?>(0) }
    var noteMode by rememberSaveable { mutableStateOf("self") }
    var isEditingNote by rememberSaveable { mutableStateOf(false) }
    var sharePreviewVisible by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("你") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var themeId by rememberSaveable { mutableStateOf(DiaryThemes.Rose.id) }
    var recordText by rememberSaveable {
        mutableStateOf("今天下午好像感觉到了一点点胎动，工作有点累，但那一下让我开心了很久。")
    }
    val favoriteIds = remember {
        mutableStateListOf<String>().apply {
            addAll(MockDiaryRepository.defaultFavoriteSlideIds())
        }
    }

    val theme = DiaryThemes.all.firstOrNull { it.id == themeId } ?: DiaryThemes.Rose
    val selectedEntry = entries.firstOrNull { it.id == selectedEntryId } ?: entries.last()
    val selectedSlide = selectedEntry.slides.getOrElse(selectedSlideIndex) { selectedEntry.slides.first() }

    if (route == AppScreen.Generating.name) {
        LaunchedEffect(selectedEntryId, recordText) {
            delay(850)
            route = AppScreen.Result.name
        }
    }

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
                    when (route) {
                        AppScreen.Record.name -> RecordScreen(
                            text = recordText,
                            theme = theme,
                            onTextChange = { recordText = it },
                            onBack = { route = AppScreen.Home.name },
                            onSubmit = {
                                selectedEntryId = entries.last().id
                                selectedSlideIndex = 0
                                selectedNoteIndex = 0
                                isEditingNote = false
                                sharePreviewVisible = false
                                route = AppScreen.Generating.name
                            },
                        )

                        AppScreen.Generating.name -> GeneratingScreen(theme)

                        AppScreen.Result.name -> ResultScreen(
                            entry = selectedEntry,
                            slide = selectedSlide,
                            slideIndex = selectedSlideIndex,
                            theme = theme,
                            favoriteIds = favoriteIds,
                            selectedNoteIndex = selectedNoteIndex,
                            noteMode = noteMode,
                            isEditingNote = isEditingNote,
                            sharePreviewVisible = sharePreviewVisible,
                            onBack = { route = AppScreen.Home.name },
                            onPreviousSlide = {
                                selectedSlideIndex =
                                    (selectedSlideIndex - 1).floorMod(selectedEntry.slides.size)
                                selectedNoteIndex = 0
                                isEditingNote = false
                                sharePreviewVisible = false
                            },
                            onNextSlide = {
                                selectedSlideIndex =
                                    (selectedSlideIndex + 1).floorMod(selectedEntry.slides.size)
                                selectedNoteIndex = 0
                                isEditingNote = false
                                sharePreviewVisible = false
                            },
                            onSelectNote = {
                                selectedNoteIndex = it
                                isEditingNote = false
                            },
                            onToggleFavorite = {
                                val favoriteId = MockDiaryRepository.favoriteId(selectedEntry.id, selectedSlide.id)
                                if (favoriteIds.contains(favoriteId)) {
                                    favoriteIds.remove(favoriteId)
                                } else {
                                    favoriteIds.add(favoriteId)
                                }
                            },
                            onNoteModeChange = {
                                noteMode = it
                                isEditingNote = false
                            },
                            onToggleEdit = { isEditingNote = !isEditingNote },
                            onToggleSharePreview = { sharePreviewVisible = !sharePreviewVisible },
                        )

                        AppScreen.Timeline.name -> TimelineScreen(
                            entries = entries,
                            selectedEntry = selectedEntry,
                            theme = theme,
                            onBack = { route = AppScreen.Home.name },
                            onSelectEntry = {
                                selectedEntryId = it.id
                                selectedSlideIndex = 0
                                selectedNoteIndex = 0
                            },
                            onOpenResult = { route = AppScreen.Result.name },
                        )

                        AppScreen.Memory.name -> MemoryScreen(
                            entries = entries,
                            favoriteIds = favoriteIds,
                            theme = theme,
                            onBack = { route = AppScreen.Home.name },
                            onOpenSlide = { entry, slideIndex ->
                                selectedEntryId = entry.id
                                selectedSlideIndex = slideIndex
                                selectedNoteIndex = 0
                                sharePreviewVisible = false
                                route = AppScreen.Result.name
                            },
                        )

                        AppScreen.Settings.name -> SettingsScreen(
                            username = username,
                            dueDate = dueDate,
                            themeId = themeId,
                            theme = theme,
                            onUsernameChange = { username = it },
                            onDueDateChange = { dueDate = it },
                            onThemeChange = { themeId = it },
                            onBack = { route = AppScreen.Home.name },
                        )

                        else -> HomeScreen(
                            username = username,
                            theme = theme,
                            onRecord = { route = AppScreen.Record.name },
                            onTimeline = { route = AppScreen.Timeline.name },
                            onMemory = { route = AppScreen.Memory.name },
                            onSettings = { route = AppScreen.Settings.name },
                        )
                    }
                }
            }
        }
    }
}
