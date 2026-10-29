package com.ciro.saveyourreallife.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import com.ciro.saveyourreallife.data.BlockedAppsRepository
import com.ciro.saveyourreallife.model.BlockEventGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AppBlockAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val eventGuard = BlockEventGuard()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var blockedAppsJob: Job? = null
    private var popupView: View? = null
    @Volatile private var blockedPackages: Set<String> = emptySet()

    private val removePopup = Runnable { removeBlockPopup() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceConnection.setConnected(true)
        blockedAppsJob?.cancel()
        blockedAppsJob = serviceScope.launch {
            BlockedAppsRepository(applicationContext).blockedPackages.collectLatest {
                blockedPackages = it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        val isBlocked = packageName in blockedPackages && packageName != this.packageName
        if (!eventGuard.shouldBlock(packageName, isBlocked, SystemClock.elapsedRealtime())) return

        performGlobalAction(GLOBAL_ACTION_HOME)
        showBlockPopup()
    }

    override fun onInterrupt() {
        removeBlockPopup()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AccessibilityServiceConnection.setConnected(false)
        blockedAppsJob?.cancel()
        removeBlockPopup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AccessibilityServiceConnection.setConnected(false)
        blockedAppsJob?.cancel()
        removeBlockPopup()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showBlockPopup() {
        removeBlockPopup()

        val density = resources.displayMetrics.density
        val popup = TextView(this).apply {
            text = "Blocked by SaveYourRealLife"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(
                (20 * density).toInt(),
                (12 * density).toInt(),
                (20 * density).toInt(),
                (12 * density).toInt(),
            )
            background = GradientDrawable().apply {
                setColor(Color.argb(230, 32, 32, 32))
                cornerRadius = 16 * density
            }
            elevation = 6 * density
        }
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching {
            windowManager.addView(popup, layoutParams)
            popupView = popup
            mainHandler.postDelayed(removePopup, POPUP_DURATION_MILLIS)
        }
    }

    private fun removeBlockPopup() {
        mainHandler.removeCallbacks(removePopup)
        val view = popupView ?: return
        popupView = null
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { windowManager.removeViewImmediate(view) }
    }

    private companion object {
        const val POPUP_DURATION_MILLIS = 2_000L
    }
}
