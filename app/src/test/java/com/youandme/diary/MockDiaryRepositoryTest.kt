package com.youandme.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDiaryRepositoryTest {
    @Test
    fun mockRepositoryProvidesDayOneDataset() {
        val entries = MockDiaryRepository.entries

        assertTrue(entries.size >= 3)
        assertTrue(entries.all { it.slides.isNotEmpty() })
        assertTrue(entries.flatMap { it.slides }.all { it.notes.isNotEmpty() })
    }

    @Test
    fun favoriteStoreTogglesSlideState() {
        val favoriteId = MockDiaryRepository.favoriteId("entry", "slide")
        val store = FavoriteStore()

        assertFalse(store.isFavorite(favoriteId))
        assertTrue(store.toggle(favoriteId))
        assertTrue(store.isFavorite(favoriteId))
        assertFalse(store.toggle(favoriteId))
        assertFalse(store.isFavorite(favoriteId))
        assertEquals(emptySet<String>(), store.all())
    }
}
