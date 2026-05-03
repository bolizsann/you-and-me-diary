package com.youandme.diary.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.youandme.diary.domain.model.DiaryThemes
import com.youandme.diary.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.userSettingsDataStore

    val settings: Flow<UserSettings> =
        dataStore.data.map { preferences ->
            UserSettings(
                username = preferences[Keys.Username] ?: "你",
                dueDate = preferences[Keys.DueDate] ?: "",
                themeId = preferences[Keys.ThemeId] ?: DiaryThemes.Rose.id,
            )
        }

    suspend fun setUsername(username: String) {
        dataStore.edit { preferences ->
            preferences[Keys.Username] = username
        }
    }

    suspend fun setDueDate(dueDate: String) {
        dataStore.edit { preferences ->
            preferences[Keys.DueDate] = dueDate
        }
    }

    suspend fun setThemeId(themeId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.ThemeId] = themeId
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private object Keys {
        val Username = stringPreferencesKey("username")
        val DueDate = stringPreferencesKey("due_date")
        val ThemeId = stringPreferencesKey("theme_id")
    }
}
