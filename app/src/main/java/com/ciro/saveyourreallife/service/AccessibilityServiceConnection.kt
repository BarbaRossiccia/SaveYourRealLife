package com.ciro.saveyourreallife.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessibilityServiceConnection {
    private val connectedState = MutableStateFlow(false)
    val isConnected = connectedState.asStateFlow()
    private var disableAction: (() -> Unit)? = null

    internal fun setConnected(connected: Boolean, onDisable: (() -> Unit)? = null) {
        disableAction = if (connected) onDisable else null
        connectedState.value = connected
    }

    fun disableService() {
        disableAction?.invoke()
    }
}
