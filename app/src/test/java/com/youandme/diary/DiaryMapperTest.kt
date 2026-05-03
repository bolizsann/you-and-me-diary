package com.youandme.diary

import com.youandme.diary.data.local.toLocalEntities
import com.youandme.diary.data.mock.MockDiaryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryMapperTest {
    @Test
    fun domainEntryMapsToRoomEntities() {
        val entry = MockDiaryRepository.entries.first()
        val record = entry.toLocalEntities()

        assertEquals(entry.id, record.entry.id)
        assertEquals(entry.slides.size, record.slides.size)
        assertEquals(entry.slides.flatMap { it.notes }.size, record.notes.size)
        assertTrue(record.slides.all { it.entryId == entry.id })
    }
}
