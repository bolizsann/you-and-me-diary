package com.youandme.diary

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.youandme.diary.data.local.toDomain
import com.youandme.diary.data.local.YouAndMeDiaryDatabase
import com.youandme.diary.data.local.toLocalEntities
import com.youandme.diary.data.mock.MockDiaryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiaryDaoTest {
    private lateinit var database: YouAndMeDiaryDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, YouAndMeDiaryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertedEntryCanBeReadFromTimeline() = runBlocking {
        val entry = MockDiaryRepository.entries.first()
        val record = entry.toLocalEntities()

        database.diaryDao().upsertEntry(record.entry, record.slides, record.notes)

        val entries = database.diaryDao().observeEntries().first()
        assertEquals(1, entries.size)
        assertEquals(entry.id, entries.first().entry.id)
        assertEquals(entry.slides.size, entries.first().slides.size)
    }

    @Test
    fun favoriteStateCanBeToggled() = runBlocking {
        val entry = MockDiaryRepository.entries.first()
        val record = entry.toLocalEntities()
        val favoriteSlideKey = record.slides.first().slideKey

        database.diaryDao().upsertEntry(record.entry, record.slides, record.notes)

        assertTrue(database.diaryDao().observeFavoriteIds().first().contains(favoriteSlideKey))
        database.diaryDao().toggleFavorite(favoriteSlideKey)
        assertFalse(database.diaryDao().observeFavoriteIds().first().contains(favoriteSlideKey))
    }

    @Test
    fun entryFavoriteStateAppliesToAllSlides() = runBlocking {
        val entry = MockDiaryRepository.entries.first()
        val record = entry.toLocalEntities()

        database.diaryDao().upsertEntry(record.entry, record.slides, record.notes)
        database.diaryDao().setFavoriteForEntry(entry.id, true)

        val favoriteIds = database.diaryDao().observeFavoriteIds().first()
        assertEquals(record.slides.map { it.slideKey }.toSet(), favoriteIds.toSet())

        database.diaryDao().setFavoriteForEntry(entry.id, false)
        assertTrue(database.diaryDao().observeFavoriteIds().first().isEmpty())
    }

    @Test
    fun editedNoteTextIsReadFromEntry() = runBlocking {
        val entry = MockDiaryRepository.entries.first()
        val record = entry.toLocalEntities()
        val noteId = "${record.slides.first().slideKey}::note-0"
        val editedText = "这是一段被我重新写过的解释。"

        database.diaryDao().upsertEntry(record.entry, record.slides, record.notes)
        database.diaryDao().updateEditedSelfText(noteId, editedText)

        val updatedEntry = database.diaryDao().getEntry(entry.id)!!.toDomain()
        assertEquals(editedText, updatedEntry.slides.first().notes.first().selfText)
    }
}
