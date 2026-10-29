package com.ciro.saveyourreallife.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uiPreferencesDataStore by preferencesDataStore(name = "ui_preferences")

class UiPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.uiPreferencesDataStore

    val accessibilityExplanationSeen: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ACCESSIBILITY_EXPLANATION_SEEN] ?: false
    }

    suspend fun markAccessibilityExplanationSeen() {
        dataStore.edit { preferences ->
            preferences[ACCESSIBILITY_EXPLANATION_SEEN] = true
        }
    }

    private companion object {
        val ACCESSIBILITY_EXPLANATION_SEEN =
            booleanPreferencesKey("accessibility_explanation_seen")
    }
}
