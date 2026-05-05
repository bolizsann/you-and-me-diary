package com.youandme.diary.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youandme.diary.data.local.DiaryRepository
import com.youandme.diary.data.local.YouAndMeDiaryDatabase
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.data.settings.SettingsRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiaryAppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = YouAndMeDiaryDatabase.getInstance(application)
    private val diaryRepository = DiaryRepository(database.diaryDao())
    private val settingsRepository = SettingsRepository(application)
    private val navState = MutableStateFlow(DiaryNavigationState())

    val uiState: StateFlow<DiaryAppUiState> =
        combine(
            diaryRepository.observeEntries(),
            diaryRepository.observeFavoriteIds(),
            settingsRepository.settings,
            navState,
        ) { entries, favoriteIds, settings, navigation ->
            val safeEntries = entries.ifEmpty { MockDiaryRepository.entries }
            DiaryAppUiState(
                entries = safeEntries,
                favoriteIds = if (entries.isEmpty()) {
                    MockDiaryRepository.defaultFavoriteSlideIds()
                } else {
                    favoriteIds
                },
                route = navigation.route,
                selectedEntryId = navigation.selectedEntryId.ifBlank { safeEntries.last().id },
                selectedSlideIndex = navigation.selectedSlideIndex,
                selectedNoteIndex = navigation.selectedNoteIndex,
                noteMode = navigation.noteMode,
                isEditingNote = navigation.isEditingNote,
                sharePreviewVisible = navigation.sharePreviewVisible,
                recordText = navigation.recordText,
                username = settings.username,
                dueDate = settings.dueDate,
                themeId = settings.themeId,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DiaryAppUiState(),
        )

    init {
        viewModelScope.launch {
            diaryRepository.seedIfEmpty()
        }
    }

    fun openHome() {
        navState.update { it.copy(route = AppScreen.Home.name) }
    }

    fun openRecord() {
        navState.update { it.copy(route = AppScreen.Record.name) }
    }

    fun openTimeline() {
        navState.update { it.copy(route = AppScreen.Timeline.name) }
    }

    fun openMemory() {
        navState.update { it.copy(route = AppScreen.Memory.name) }
    }

    fun openSettings() {
        navState.update { it.copy(route = AppScreen.Settings.name) }
    }

    fun updateRecordText(text: String) {
        navState.update { it.copy(recordText = text) }
    }

    fun submitRecord() {
        viewModelScope.launch {
            val text = navState.value.recordText
            val createdEntry = diaryRepository.createMockEntry(text)
            navState.update {
                it.copy(
                    route = AppScreen.Generating.name,
                    selectedEntryId = createdEntry.id,
                    selectedSlideIndex = 0,
                    selectedNoteIndex = 0,
                    isEditingNote = false,
                    sharePreviewVisible = false,
                )
            }
            delay(850)
            navState.update { it.copy(route = AppScreen.Result.name) }
        }
    }

    fun selectEntry(entry: DiaryEntry) {
        navState.update {
            it.copy(
                selectedEntryId = entry.id,
                selectedSlideIndex = 0,
                selectedNoteIndex = 0,
            )
        }
    }

    fun openResult() {
        navState.update { it.copy(route = AppScreen.Result.name) }
    }

    fun openSlide(entry: DiaryEntry, slideIndex: Int) {
        navState.update {
            it.copy(
                route = AppScreen.Result.name,
                selectedEntryId = entry.id,
                selectedSlideIndex = slideIndex,
                selectedNoteIndex = 0,
                sharePreviewVisible = false,
            )
        }
    }

    fun previousSlide(slideCount: Int) {
        navState.update {
            it.copy(
                selectedSlideIndex = (it.selectedSlideIndex - 1).floorMod(slideCount),
                selectedNoteIndex = 0,
                isEditingNote = false,
                sharePreviewVisible = false,
            )
        }
    }

    fun nextSlide(slideCount: Int) {
        navState.update {
            it.copy(
                selectedSlideIndex = (it.selectedSlideIndex + 1).floorMod(slideCount),
                selectedNoteIndex = 0,
                isEditingNote = false,
                sharePreviewVisible = false,
            )
        }
    }

    fun selectNote(index: Int) {
        navState.update {
            it.copy(
                selectedNoteIndex = index,
                isEditingNote = false,
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun toggleFavorite(entryId: String, slideId: String) {
        viewModelScope.launch {
            diaryRepository.toggleEntryFavorite(entryId)
        }
    }

    fun updateNoteText(
        entryId: String,
        slideId: String,
        noteIndex: Int?,
        text: String,
    ) {
        val safeNoteIndex = noteIndex ?: return
        val mode = navState.value.noteMode
        viewModelScope.launch {
            diaryRepository.updateNoteText(
                entryId = entryId,
                slideId = slideId,
                noteIndex = safeNoteIndex,
                noteMode = mode,
                text = text,
            )
        }
    }

    fun changeNoteMode(noteMode: String) {
        navState.update {
            it.copy(
                noteMode = noteMode,
                isEditingNote = false,
            )
        }
    }

    fun toggleEdit() {
        navState.update { it.copy(isEditingNote = !it.isEditingNote) }
    }

    fun toggleSharePreview() {
        navState.update { it.copy(sharePreviewVisible = !it.sharePreviewVisible) }
    }

    fun updateUsername(username: String) {
        viewModelScope.launch {
            settingsRepository.setUsername(username)
        }
    }

    fun updateDueDate(dueDate: String) {
        viewModelScope.launch {
            settingsRepository.setDueDate(dueDate)
        }
    }

    fun updateTheme(themeId: String) {
        viewModelScope.launch {
            settingsRepository.setThemeId(themeId)
        }
    }

    fun clearLocalTestData() {
        viewModelScope.launch {
            settingsRepository.clear()
            diaryRepository.clearAndSeedMockData()
            navState.value = DiaryNavigationState()
        }
    }
}

data class DiaryAppUiState(
    val entries: List<DiaryEntry> = MockDiaryRepository.entries,
    val favoriteIds: Set<String> = MockDiaryRepository.defaultFavoriteSlideIds(),
    val route: String = AppScreen.Home.name,
    val selectedEntryId: String = MockDiaryRepository.entries.last().id,
    val selectedSlideIndex: Int = 0,
    val selectedNoteIndex: Int? = 0,
    val noteMode: String = "self",
    val isEditingNote: Boolean = false,
    val sharePreviewVisible: Boolean = false,
    val recordText: String = "今天下午好像感觉到了一点点胎动，工作有点累，但那一下让我开心了很久。",
    val username: String = "你",
    val dueDate: String = "",
    val themeId: String = DiaryThemes.Rose.id,
)

private data class DiaryNavigationState(
    val route: String = AppScreen.Home.name,
    val selectedEntryId: String = MockDiaryRepository.entries.last().id,
    val selectedSlideIndex: Int = 0,
    val selectedNoteIndex: Int? = 0,
    val noteMode: String = "self",
    val isEditingNote: Boolean = false,
    val sharePreviewVisible: Boolean = false,
    val recordText: String = "今天下午好像感觉到了一点点胎动，工作有点累，但那一下让我开心了很久。",
)

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
