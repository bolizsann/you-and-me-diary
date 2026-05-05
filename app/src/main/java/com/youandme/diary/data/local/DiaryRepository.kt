package com.youandme.diary.data.local

import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
}
