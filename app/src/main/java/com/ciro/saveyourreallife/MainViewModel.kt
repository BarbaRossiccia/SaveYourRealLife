package com.ciro.saveyourreallife

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ciro.saveyourreallife.data.BlockedAppsRepository
import com.ciro.saveyourreallife.data.InstalledAppsRepository
import com.ciro.saveyourreallife.data.UiPreferencesRepository
import com.ciro.saveyourreallife.model.InstalledApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val apps: List<InstalledApp> = emptyList(),
    val blockedPackages: Set<String> = emptySet(),
    val query: String = "",
    val loading: Boolean = true,
    val accessibilityExplanationSeen: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val blockedAppsRepository = BlockedAppsRepository(application)
    private val installedAppsRepository = InstalledAppsRepository(application)
    private val uiPreferencesRepository = UiPreferencesRepository(application)
    private val apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<MainUiState> = combine(
        apps,
        blockedAppsRepository.blockedPackages,
        query,
        loading,
        uiPreferencesRepository.accessibilityExplanationSeen,
    ) { installedApps, blockedPackages, searchQuery, isLoading, explanationSeen ->
        MainUiState(
            apps = installedApps.filter {
                searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
            },
            blockedPackages = blockedPackages,
            query = searchQuery,
            loading = isLoading,
            accessibilityExplanationSeen = explanationSeen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        refreshApps()
    }

    fun updateQuery(value: String) {
        query.value = value
    }

    fun setBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            blockedAppsRepository.setBlocked(packageName, blocked)
        }
    }

    fun markAccessibilityExplanationSeen() {
        viewModelScope.launch {
            uiPreferencesRepository.markAccessibilityExplanationSeen()
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            loading.value = true
            apps.value = installedAppsRepository.loadLaunchableApps()
            loading.value = false
        }
    }
}
