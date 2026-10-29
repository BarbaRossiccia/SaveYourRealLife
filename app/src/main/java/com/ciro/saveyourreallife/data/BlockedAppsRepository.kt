package com.ciro.saveyourreallife.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.blockedAppsDataStore by preferencesDataStore(name = "blocked_apps")

class BlockedAppsRepository(context: Context) {
    private val dataStore = context.applicationContext.blockedAppsDataStore

    val blockedPackages: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[BLOCKED_PACKAGES].orEmpty()
    }

    suspend fun setBlocked(packageName: String, blocked: Boolean) {
        dataStore.edit { preferences ->
            val updated = preferences[BLOCKED_PACKAGES].orEmpty().toMutableSet()
            if (blocked) updated.add(packageName) else updated.remove(packageName)
            preferences[BLOCKED_PACKAGES] = updated
        }
    }

    private companion object {
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
    }
}
