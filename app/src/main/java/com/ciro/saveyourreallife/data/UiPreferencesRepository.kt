package com.ciro.saveyourreallife.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uiPreferencesDataStore by preferencesDataStore(name = "ui_preferences")

data class UiPreferences(
    val accessibilityExplanationSeen: Boolean = false,
    val language: String = "it",
    val darkTheme: Boolean = false,
)

class UiPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.uiPreferencesDataStore

    val accessibilityExplanationSeen: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ACCESSIBILITY_EXPLANATION_SEEN] ?: false
    }

    val preferences: Flow<UiPreferences> = dataStore.data.map { preferences ->
        UiPreferences(
            accessibilityExplanationSeen = preferences[ACCESSIBILITY_EXPLANATION_SEEN] ?: false,
            language = preferences[LANGUAGE] ?: "it",
            darkTheme = preferences[DARK_THEME] ?: false,
        )
    }

    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: "it"
    }

    suspend fun markAccessibilityExplanationSeen() {
        dataStore.edit { preferences ->
            preferences[ACCESSIBILITY_EXPLANATION_SEEN] = true
        }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME] = enabled
        }
    }

    private companion object {
        val ACCESSIBILITY_EXPLANATION_SEEN =
            booleanPreferencesKey("accessibility_explanation_seen")
        val LANGUAGE = stringPreferencesKey("language")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
