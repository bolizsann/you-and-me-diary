package com.youandme.diary.app

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youandme.diary.data.local.DiaryRepository
import com.youandme.diary.data.local.YouAndMeDiaryDatabase
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.data.settings.SettingsRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

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
                recordImagePath = navigation.recordImagePath,
                recordImageRoiScale = navigation.recordImageRoiScale,
                recordImageRoiOffsetX = navigation.recordImageRoiOffsetX,
                recordImageRoiOffsetY = navigation.recordImageRoiOffsetY,
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
            val text = navState.value.recordText
            val imagePath = navState.value.recordImagePath.ifBlank { null }
            if (text.isBlank() && imagePath == null) return@launch
            val dominantColor = imagePath?.let {
                estimateDominantColor(
                    path = it,
                    roiScale = navState.value.recordImageRoiScale,
                    roiOffsetX = navState.value.recordImageRoiOffsetX,
                    roiOffsetY = navState.value.recordImageRoiOffsetY,
                )
            } ?: navState.value.recordImageDominantColor
            val createdEntry = diaryRepository.createOrAppendTodayEntry(
                rawText = text,
                localImagePath = imagePath,
                dominantColor = dominantColor,
                roiScale = navState.value.recordImageRoiScale,
                roiOffsetX = navState.value.recordImageRoiOffsetX,
                roiOffsetY = navState.value.recordImageRoiOffsetY,
            )
            navState.update {
                it.copy(
                    route = AppScreen.Generating.name,
                    selectedEntryId = createdEntry.id,
                    selectedSlideIndex = createdEntry.slides.lastIndex.coerceAtLeast(0),
                    selectedNoteIndex = 0,
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
                dominantColor = estimateDominantColor(target.absolutePath, 1f, 0f, 0f),
            )
        }

    private fun estimateDominantColor(
        path: String,
        roiScale: Float,
        roiOffsetX: Float,
        roiOffsetY: Float,
    ): Long? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val roi = calculateSquareRoi(
            imageWidth = bounds.outWidth,
            imageHeight = bounds.outHeight,
            roiScale = roiScale,
            roiOffsetX = roiOffsetX,
            roiOffsetY = roiOffsetY,
        )
        val options = BitmapFactory.Options().apply { inSampleSize = 16 }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
        val scaleX = bitmap.width.toFloat() / bounds.outWidth.toFloat()
        val scaleY = bitmap.height.toFloat() / bounds.outHeight.toFloat()
        val left = (roi.left * scaleX).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (roi.top * scaleY).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = ((roi.left + roi.size) * scaleX).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = ((roi.top + roi.size) * scaleY).roundToInt().coerceIn(top + 1, bitmap.height)
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L
        val stepX = ((right - left) / 24).coerceAtLeast(1)
        val stepY = ((bottom - top) / 24).coerceAtLeast(1)
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val color = bitmap.getPixel(x, y)
                red += android.graphics.Color.red(color)
                green += android.graphics.Color.green(color)
                blue += android.graphics.Color.blue(color)
                count += 1
                x += stepX
            }
            y += stepY
        }
        bitmap.recycle()
        if (count == 0L) return null
        return 0xFF000000L or
            ((red / count).coerceIn(0, 255) shl 16) or
            ((green / count).coerceIn(0, 255) shl 8) or
            (blue / count).coerceIn(0, 255)
    }
}

private fun calculateSquareRoi(
    imageWidth: Int,
    imageHeight: Int,
    roiScale: Float,
    roiOffsetX: Float,
    roiOffsetY: Float,
): SquareRoi {
    val safeScale = roiScale.coerceIn(1f, 4f)
    val baseScale = maxOf(1f / imageWidth.toFloat(), 1f / imageHeight.toFloat())
    val renderedWidth = imageWidth * baseScale * safeScale
    val renderedHeight = imageHeight * baseScale * safeScale
    val cropSize = (1f / (baseScale * safeScale)).roundToInt().coerceAtLeast(1)
    val left = ((renderedWidth / 2f - 0.5f - roiOffsetX) / (baseScale * safeScale))
        .roundToInt()
        .coerceIn(0, (imageWidth - cropSize).coerceAtLeast(0))
    val top = ((renderedHeight / 2f - 0.5f - roiOffsetY) / (baseScale * safeScale))
        .roundToInt()
        .coerceIn(0, (imageHeight - cropSize).coerceAtLeast(0))
    return SquareRoi(left = left, top = top, size = cropSize.coerceAtMost(min(imageWidth, imageHeight)))
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

private data class LocalImageResult(
    val path: String,
    val dominantColor: Long?,
)

private data class SquareRoi(
    val left: Int,
    val top: Int,
    val size: Int,
)
