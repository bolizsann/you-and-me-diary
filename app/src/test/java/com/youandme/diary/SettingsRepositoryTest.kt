package com.youandme.diary

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.youandme.diary.data.settings.SettingsRepository
import com.youandme.diary.domain.model.DiaryThemes
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    private val testScope = TestScope()
    private lateinit var tempDir: File
    private lateinit var dataStoreFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        tempDir = createTempDir(prefix = "settings-test")
        dataStoreFile = File(tempDir, "user_settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { dataStoreFile },
        )
        repository = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        tempDir.deleteRecursively()
    }

    @Test
    fun settingsArePersisted() = testScope.runTest {
        repository.setUsername("小雨")
        repository.setDueDate("2026-06-01")
        repository.setThemeId(DiaryThemes.Mist.id)

        val settings = repository.settings.first()
        assertEquals("小雨", settings.username)
        assertEquals("2026-06-01", settings.dueDate)
        assertEquals(DiaryThemes.Mist.id, settings.themeId)
    }

    @Test
    fun clearResetsSettingsToDefaults() = testScope.runTest {
        repository.setUsername("小雨")
        repository.setDueDate("2026-06-01")
        repository.setThemeId(DiaryThemes.Mist.id)

        repository.clear()

        val settings = repository.settings.first()
        assertEquals("你", settings.username)
        assertEquals("", settings.dueDate)
        assertEquals(DiaryThemes.Rose.id, settings.themeId)
    }
}
