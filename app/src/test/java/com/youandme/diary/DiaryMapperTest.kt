package com.youandme.diary

import com.youandme.diary.data.local.toLocalEntities
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.EntryMedia
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

    @Test
    fun mediaIdMapsToRoomEntities() {
        val entry = MockDiaryRepository.entries.first()
        val media = EntryMedia(
            id = "media-test",
            entryId = entry.id,
            localPath = "C:\\tmp\\image.jpg",
            type = "image",
            dominantColor = 0xFF87A9BD,
            createdAt = 123L,
        )
        val entryWithMedia = entry.copy(
            slides = listOf(entry.slides.first().copy(mediaId = media.id)),
            media = listOf(media),
        )
        val record = entryWithMedia.toLocalEntities()

        assertEquals(media.id, record.slides.first().mediaId)
        assertEquals(media.id, record.media.first().id)
        assertEquals(media.localPath, record.media.first().localPath)
    }
}
