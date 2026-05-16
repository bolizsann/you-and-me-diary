package com.youandme.diary.data.local

import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryIds
import com.youandme.diary.domain.model.DiaryNote
import com.youandme.diary.domain.model.DiarySlide
import com.youandme.diary.domain.model.EntryMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DiaryRepository(
    private val diaryDao: DiaryDao,
) {
    fun observeEntries(): Flow<List<DiaryEntry>> =
        diaryDao.observeEntries().map { entries ->
            entries.map { it.toDomain() }
        }

    fun observeFavoriteIds(): Flow<Set<String>> =
        diaryDao.observeFavoriteIds().map { it.toSet() }

    suspend fun seedIfEmpty() {
        if (diaryDao.entryCount() == 0) {
            MockDiaryRepository.entries.forEach { entry ->
                upsertEntry(entry.copy(createdAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun createMockEntry(rawText: String): DiaryEntry {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val template = MockDiaryRepository.entries.last()
        val entry = template.copy(
            id = "entry-${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}-$now",
            dateId = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
            dateLabel = "${today.monthValue} 月 ${today.dayOfMonth} 日",
            rawText = rawText,
            createdAt = now,
            slides = template.slides.mapIndexed { index, slide ->
                slide.copy(
                    id = "${slide.id}-$now-$index",
                    defaultFavorite = false,
                )
            },
        )
        upsertEntry(entry)
        return entry
    }

    suspend fun createOrAppendTodayEntry(
        rawText: String,
        localImagePath: String?,
        dominantColor: Long?,
        roiScale: Float = 1f,
        roiOffsetX: Float = 0f,
        roiOffsetY: Float = 0f,
    ): DiaryEntry {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val dateId = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existingEntry = diaryDao.getEntryByDate(dateId)?.toDomain()
        val template = MockDiaryRepository.entries.last()
        val media = localImagePath?.let { path ->
            EntryMedia(
                id = "media-$dateId-$now",
                entryId = existingEntry?.id ?: "entry-$dateId",
                localPath = path,
                type = "image",
                dominantColor = dominantColor,
                createdAt = now,
                roiScale = roiScale,
                roiOffsetX = roiOffsetX,
                roiOffsetY = roiOffsetY,
            )
        }
        val slide = buildSubmittedSlide(
            id = "slide-$dateId-$now",
            rawText = rawText,
            dominantColor = dominantColor,
            mediaId = media?.id,
            fallback = template.slides[(existingEntry?.slides?.size ?: 0).floorMod(template.slides.size)],
        )
        val entry = if (existingEntry == null) {
            DiaryEntry(
                id = "entry-$dateId",
                dateId = dateId,
                dateLabel = "${today.monthValue} 月 ${today.dayOfMonth} 日",
                title = slide.title,
                moodEmoji = template.moodEmoji,
                moodColor = dominantColor ?: template.moodColor,
                comfortText = slide.notes.firstOrNull()?.babyText ?: template.comfortText,
                timelineSummary = slide.caption,
                rawText = rawText,
                createdAt = now,
                slides = listOf(slide),
                media = listOfNotNull(media),
            )
        } else {
            existingEntry.copy(
                title = existingEntry.title,
                moodColor = dominantColor ?: existingEntry.moodColor,
                comfortText = slide.notes.firstOrNull()?.babyText ?: existingEntry.comfortText,
                timelineSummary = slide.caption,
                rawText = listOf(existingEntry.rawText, rawText)
                    .filter { it.isNotBlank() }
                    .joinToString(separator = "\n"),
                slides = existingEntry.slides + slide,
                media = existingEntry.media + listOfNotNull(media),
            )
        }
        upsertEntry(entry)
        return entry
    }

    suspend fun toggleFavorite(favoriteId: String) {
        diaryDao.toggleFavorite(favoriteId)
    }

    suspend fun toggleEntryFavorite(entryId: String) {
        val shouldFavorite = diaryDao.favoriteSlideCountForEntry(entryId) == 0
        diaryDao.setFavoriteForEntry(entryId, shouldFavorite)
    }

    suspend fun updateNoteText(
        entryId: String,
        slideId: String,
        noteIndex: Int,
        noteMode: String,
        text: String,
    ) {
        val noteId = "${DiaryIds.favoriteId(entryId, slideId)}::note-$noteIndex"
        if (noteMode == "baby") {
            diaryDao.updateEditedBabyText(noteId, text)
        } else {
            diaryDao.updateEditedSelfText(noteId, text)
        }
    }

    suspend fun updateDiaryTitleAndText(
        entryId: String,
        slideId: String,
        noteIndex: Int,
        title: String,
        text: String,
    ) {
        diaryDao.updateEntryTitle(entryId, title.toEditableDiaryTitle())
        val noteId = "${DiaryIds.favoriteId(entryId, slideId)}::note-$noteIndex"
        diaryDao.updateEditedSelfText(noteId, text)
    }

    suspend fun deleteSlide(entryId: String, slideId: String): DiaryEntry? {
        val entry = diaryDao.getEntry(entryId)?.toDomain() ?: return null
        val removedSlide = entry.slides.firstOrNull { it.id == slideId } ?: return entry
        val remainingSlides = entry.slides.filterNot { it.id == slideId }
        val removedMediaIds = setOfNotNull(removedSlide.mediaId)
        val removedMediaPaths = entry.media
            .filter { it.id in removedMediaIds }
            .map { it.localPath }

        if (remainingSlides.isEmpty()) {
            diaryDao.deleteEntry(entryId)
            removedMediaPaths.forEach { path -> File(path).delete() }
            return null
        }

        val remainingMediaIds = remainingSlides.mapNotNull { it.mediaId }.toSet()
        val remainingMedia = entry.media.filter { it.id in remainingMediaIds }
        val lastSlide = remainingSlides.last()
        val updatedEntry = entry.copy(
            title = entry.title,
            moodColor = lastSlide.gradientStart,
            comfortText = lastSlide.notes.firstOrNull()?.babyText ?: entry.comfortText,
            timelineSummary = lastSlide.caption,
            rawText = remainingSlides
                .mapNotNull { slide -> slide.notes.firstOrNull()?.selfText?.toRemainingRawText() }
                .joinToString(separator = "\n"),
            slides = remainingSlides,
            media = remainingMedia,
        )
        upsertEntry(updatedEntry)
        removedMediaPaths.forEach { path -> File(path).delete() }
        return updatedEntry
    }

    suspend fun clearAndSeedMockData() {
        diaryDao.deleteAllEntries()
        MockDiaryRepository.entries.forEach { entry ->
            upsertEntry(entry.copy(createdAt = System.currentTimeMillis()))
        }
    }

    private suspend fun upsertEntry(entry: DiaryEntry) {
        val record = entry.toLocalEntities()
        diaryDao.upsertEntry(
            entry = record.entry,
            slides = record.slides,
            notes = record.notes,
            media = record.media,
        )
    }

    private fun buildSubmittedSlide(
        id: String,
        rawText: String,
        dominantColor: Long?,
        mediaId: String?,
        fallback: DiarySlide,
    ): DiarySlide {
        val cleanText = rawText.trim()
        val quote = cleanText
            .takeIf { it.isNotBlank() }
            ?.toGentleQuote()
            ?: "What you see can hold what you feel."
        val title = when {
            cleanText.hasAny("胎动", "动了一下", "踢") -> "一次小小胎动"
            cleanText.hasAny("累", "疲惫", "困") -> "慢一点也认真"
            cleanText.hasAny("吃", "胃口", "饭") -> "一点点胃口"
            cleanText.isBlank() && mediaId != null -> "今天看见的颜色"
            else -> "今天也留一页"
        }
        val shouldShowBabyText = cleanText.shouldShowBabyText(id)
        val caption = cleanText
            .takeIf { it.isNotBlank() }
            ?.let { "这一页把今天的心情轻轻收起来。" }
            ?: "这张图先替今天开口。"
        val color = dominantColor ?: fallback.gradientStart
        return fallback.copy(
            id = id,
            title = title,
            quote = quote,
            caption = caption,
            gradientStart = color,
            gradientEnd = fallback.gradientEnd,
            defaultFavorite = false,
            mediaId = mediaId,
            notes = listOf(
                DiaryNote(
                    label = "今天这一页",
                    selfText = cleanText.ifBlank { IMAGE_ONLY_SELF_TEXT },
                    babyText = if (shouldShowBabyText) {
                        "宝宝陪妈妈一起把今天收好。慢慢来，我们已经在好好生活了。"
                    } else {
                        ""
                    },
                    x = 0.5f,
                    y = 0.5f,
                ),
            ),
        )
    }
}

private const val IMAGE_ONLY_SELF_TEXT = "这张图里有今天的光、颜色和情绪。先把它留下来，就已经很好。"

private fun String.toRemainingRawText(): String? =
    trim()
        .takeIf { it.isNotBlank() && it != IMAGE_ONLY_SELF_TEXT }

private fun String.toGentleQuote(): String {
    val compact = replace("\n", " ").trim()
    return when {
        compact.length <= 18 -> compact
        else -> compact.take(18).trimEnd('，', '。', ',', '.', ' ') + "..."
    }
}

private fun String.toEditableDiaryTitle(): String =
    lineSequence()
        .joinToString(separator = " ") { it.trim() }
        .trim()
        .take(20)

private fun String.toDiaryTitle(): String =
    toEditableDiaryTitle().ifBlank { "今天也留一页" }

private fun String.shouldShowBabyText(seed: String): Boolean =
    hasAny("累", "疲惫", "困", "胎动", "动了一下", "踢") ||
        (seed.hashCode().floorMod(10) < 2)

private fun String.hasAny(vararg keywords: String): Boolean =
    keywords.any { contains(it, ignoreCase = true) }

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
