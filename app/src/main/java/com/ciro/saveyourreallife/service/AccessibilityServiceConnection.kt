package com.ciro.saveyourreallife.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessibilityServiceConnection {
    private val connectedState = MutableStateFlow(false)
    val isConnected = connectedState.asStateFlow()

    internal fun setConnected(connected: Boolean) {
        connectedState.value = connected
    }
}
