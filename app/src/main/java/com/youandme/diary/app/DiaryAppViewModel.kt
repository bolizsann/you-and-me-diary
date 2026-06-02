package com.youandme.diary.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youandme.diary.data.generation.DiaryGenerationGateway
import com.youandme.diary.data.generation.DiaryGenerationImageProcessor
import com.youandme.diary.data.generation.DiaryGenerationRequest
import com.youandme.diary.data.local.DiaryRepository
import com.youandme.diary.data.local.GeneratedDiaryDraft
import com.youandme.diary.data.local.YouAndMeDiaryDatabase
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.data.settings.SettingsRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryThemes
import com.youandme.diary.domain.model.GenerationModes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min

class DiaryAppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = YouAndMeDiaryDatabase.getInstance(application)
    private val diaryRepository = DiaryRepository(database.diaryDao())
    private val diaryGenerationGateway = DiaryGenerationGateway(application)
    private val settingsRepository = SettingsRepository(application)
    private val navState = MutableStateFlow(DiaryNavigationState())
    private var localGemmaWarmUpJob: Job? = null

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
                recordImagePath = navigation.recordImagePath,
                recordImageRoiScale = navigation.recordImageRoiScale,
                recordImageRoiOffsetX = navigation.recordImageRoiOffsetX,
                recordImageRoiOffsetY = navigation.recordImageRoiOffsetY,
                username = settings.username,
                dueDate = settings.dueDate,
                themeId = settings.themeId,
                generationMode = settings.generationMode,
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
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.generationMode }
                .distinctUntilChanged()
                .collect { generationMode ->
                    if (generationMode == GenerationModes.Offline) {
                        warmUpLocalGemma()
                    }
                }
        }
    }

    fun openHome() {
        navState.update { it.copy(route = AppScreen.Home.name, isEditingNote = false) }
    }

    fun openRecord() {
        navState.update {
            it.copy(
                route = AppScreen.Record.name,
                isEditingNote = false,
                recordImagePath = "",
                recordImageDominantColor = null,
                recordImageRoiScale = 1f,
                recordImageRoiOffsetX = 0f,
                recordImageRoiOffsetY = 0f,
            )
        }
    }

    fun openTimeline() {
        navState.update { it.copy(route = AppScreen.Timeline.name, isEditingNote = false) }
    }

    fun openMemory() {
        navState.update { it.copy(route = AppScreen.Memory.name, isEditingNote = false) }
    }

    fun openSettings() {
        navState.update { it.copy(route = AppScreen.Settings.name, isEditingNote = false) }
    }

    fun updateRecordText(text: String) {
        navState.update { it.copy(recordText = text) }
    }

    fun selectRecordImage(uri: Uri) {
        viewModelScope.launch {
            val image = copyImageToLocalStorage(uri)
            navState.update {
                it.copy(
                    recordImagePath = image?.path.orEmpty(),
                    recordImageDominantColor = image?.dominantColor,
                    recordImageRoiScale = 1f,
                    recordImageRoiOffsetX = 0f,
                    recordImageRoiOffsetY = 0f,
                )
            }
        }
    }

    fun updateRecordImageRoi(scale: Float, offsetX: Float, offsetY: Float) {
        navState.update {
            it.copy(
                recordImageRoiScale = scale,
                recordImageRoiOffsetX = offsetX,
                recordImageRoiOffsetY = offsetY,
            )
        }
    }

    fun submitRecord() {
        viewModelScope.launch {
            val navigation = navState.value
            val text = navigation.recordText
            val imagePath = navigation.recordImagePath.ifBlank { null }
            if (text.isBlank() && imagePath == null) return@launch
            navState.update {
                it.copy(
                    route = AppScreen.Generating.name,
                    isEditingNote = false,
                    sharePreviewVisible = false,
                    recordText = "",
                    recordImagePath = "",
                    recordImageDominantColor = null,
                    recordImageRoiScale = 1f,
                    recordImageRoiOffsetX = 0f,
                    recordImageRoiOffsetY = 0f,
                )
            }
            val dominantColor = imagePath?.let {
                DiaryGenerationImageProcessor.estimateDominantColor(
                    path = it,
                    roiScale = navigation.recordImageRoiScale,
                    roiOffsetX = navigation.recordImageRoiOffsetX,
                    roiOffsetY = navigation.recordImageRoiOffsetY,
                )
            } ?: navigation.recordImageDominantColor
            val generated = requestGeneratedDiary(
                text = text,
                imagePath = imagePath,
                dominantColor = dominantColor,
                roiScale = navigation.recordImageRoiScale,
                roiOffsetX = navigation.recordImageRoiOffsetX,
                roiOffsetY = navigation.recordImageRoiOffsetY,
            )
            val createdEntry = diaryRepository.createOrAppendTodayEntry(
                rawText = text,
                localImagePath = imagePath,
                dominantColor = dominantColor,
                roiScale = navigation.recordImageRoiScale,
                roiOffsetX = navigation.recordImageRoiOffsetX,
                roiOffsetY = navigation.recordImageRoiOffsetY,
                generated = generated,
            )
            navState.update {
                it.copy(
                    route = AppScreen.Result.name,
                    selectedEntryId = createdEntry.id,
                    selectedSlideIndex = createdEntry.slides.lastIndex.coerceAtLeast(0),
                    selectedNoteIndex = 0,
                    isEditingNote = false,
                    sharePreviewVisible = false,
                )
            }
        }
    }

    private suspend fun requestGeneratedDiary(
        text: String,
        imagePath: String?,
        dominantColor: Long?,
        roiScale: Float,
        roiOffsetX: Float,
        roiOffsetY: Float,
    ): GeneratedDiaryDraft? {
        val today = LocalDate.now()
        val dateId = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dateLabel = "${today.monthValue} 月 ${today.dayOfMonth} 日"
        val snapshot = uiState.value
        val existingTodayEntry = snapshot.entries.firstOrNull { it.dateId == dateId }
        return diaryGenerationGateway.generate(
            DiaryGenerationRequest(
                text = text,
                voiceText = "",
                inputSource = if (text.isBlank() && imagePath != null) "imageOnly" else "typed",
                diaryTextMode = diaryTextModeFor(text = text, voiceText = "", hasImage = imagePath != null),
                dateId = dateId,
                dateLabel = dateLabel,
                currentTitle = existingTodayEntry?.title.orEmpty(),
                isFirstRecordForDay = existingTodayEntry == null,
                username = snapshot.username,
                estimatedDueDate = snapshot.dueDate.toEstimatedDueDateIso(),
                generationMode = snapshot.generationMode,
                imagePath = imagePath,
                dominantColor = dominantColor,
                roiScale = roiScale,
                roiOffsetX = roiOffsetX,
                roiOffsetY = roiOffsetY,
            ),
        )
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
        navState.update { it.copy(route = AppScreen.Result.name, isEditingNote = false) }
    }

    fun openSlide(entry: DiaryEntry, slideIndex: Int) {
        navState.update {
            it.copy(
                route = AppScreen.Result.name,
                selectedEntryId = entry.id,
                selectedSlideIndex = slideIndex,
                selectedNoteIndex = 0,
                isEditingNote = false,
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

    fun updateDiaryTitleAndText(
        entryId: String,
        slideId: String,
        noteIndex: Int?,
        title: String,
        text: String,
    ) {
        val safeNoteIndex = noteIndex ?: return
        viewModelScope.launch {
            diaryRepository.updateDiaryTitleAndText(
                entryId = entryId,
                slideId = slideId,
                noteIndex = safeNoteIndex,
                title = title,
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

    fun deleteCurrentSlide() {
        val snapshot = uiState.value
        val entry = snapshot.entries.firstOrNull { it.id == snapshot.selectedEntryId } ?: return
        val slideIndex = snapshot.selectedSlideIndex.coerceIn(0, entry.slides.lastIndex)
        val slide = entry.slides.getOrNull(slideIndex) ?: return
        viewModelScope.launch {
            val updatedEntry = diaryRepository.deleteSlide(entry.id, slide.id)
            navState.update {
                if (updatedEntry == null) {
                    it.copy(
                        route = AppScreen.Home.name,
                        selectedSlideIndex = 0,
                        selectedNoteIndex = 0,
                        isEditingNote = false,
                        sharePreviewVisible = false,
                    )
                } else {
                    it.copy(
                        selectedEntryId = updatedEntry.id,
                        selectedSlideIndex = min(slideIndex, updatedEntry.slides.lastIndex),
                        selectedNoteIndex = 0,
                        isEditingNote = false,
                        sharePreviewVisible = false,
                    )
                }
            }
        }
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

    fun updateGenerationMode(generationMode: String) {
        viewModelScope.launch {
            val normalizedMode = GenerationModes.normalize(generationMode)
            settingsRepository.setGenerationMode(normalizedMode)
            if (normalizedMode == GenerationModes.Offline) {
                warmUpLocalGemma()
            }
        }
    }

    private fun warmUpLocalGemma() {
        if (localGemmaWarmUpJob?.isActive == true) return
        localGemmaWarmUpJob = viewModelScope.launch(Dispatchers.IO) {
            diaryGenerationGateway.warmUpLocalIfNeeded(GenerationModes.Offline)
        }
    }

    fun clearLocalTestData() {
        viewModelScope.launch {
            settingsRepository.clear()
            diaryRepository.clearAndSeedMockData()
            navState.value = DiaryNavigationState()
        }
    }

    private suspend fun copyImageToLocalStorage(uri: Uri): LocalImageResult? =
        withContext(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val directory = File(getApplication<Application>().filesDir, "entry_media").apply {
                mkdirs()
            }
            val target = File(directory, "record-${System.currentTimeMillis()}.jpg")
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            LocalImageResult(
                path = target.absolutePath,
                dominantColor = DiaryGenerationImageProcessor.estimateDominantColor(target.absolutePath, 1f, 0f, 0f),
            )
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
    val recordText: String = "",
    val recordImagePath: String = "",
    val recordImageRoiScale: Float = 1f,
    val recordImageRoiOffsetX: Float = 0f,
    val recordImageRoiOffsetY: Float = 0f,
    val username: String = "你",
    val dueDate: String = "",
    val themeId: String = DiaryThemes.Rose.id,
    val generationMode: String = GenerationModes.Offline,
)

private data class DiaryNavigationState(
    val route: String = AppScreen.Home.name,
    val selectedEntryId: String = MockDiaryRepository.entries.last().id,
    val selectedSlideIndex: Int = 0,
    val selectedNoteIndex: Int? = 0,
    val noteMode: String = "self",
    val isEditingNote: Boolean = false,
    val sharePreviewVisible: Boolean = false,
    val recordText: String = "",
    val recordImagePath: String = "",
    val recordImageDominantColor: Long? = null,
    val recordImageRoiScale: Float = 1f,
    val recordImageRoiOffsetX: Float = 0f,
    val recordImageRoiOffsetY: Float = 0f,
)

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other

private fun String.toEstimatedDueDateIso(): String? =
    takeIf { it.isNotBlank() }?.let { value ->
        runCatching {
            LocalDate.parse(value, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull() ?: value
    }

private fun diaryTextModeFor(text: String, voiceText: String, hasImage: Boolean): String =
    when {
        voiceText.isNotBlank() -> "polish"
        text.isBlank() && hasImage -> "generate"
        text.length > 100 -> "polish"
        else -> "preserve"
    }

private data class LocalImageResult(
    val path: String,
    val dominantColor: Long?,
)
