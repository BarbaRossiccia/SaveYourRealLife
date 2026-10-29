package com.ciro.saveyourreallife.model

class BlockEventGuard(private val duplicateWindowMillis: Long = 1_500L) {
    private var foregroundPackageName: String? = null
    private var lastBlockedPackageName: String? = null
    private var lastBlockTime = Long.MIN_VALUE

    fun shouldBlock(packageName: String, blocked: Boolean, nowMillis: Long): Boolean {
        if (packageName == foregroundPackageName) return false
        foregroundPackageName = packageName
        if (!blocked) return false

        val isDuplicate = packageName == lastBlockedPackageName &&
            nowMillis - lastBlockTime in 0 until duplicateWindowMillis
        if (isDuplicate) return false

        lastBlockedPackageName = packageName
        lastBlockTime = nowMillis
        return true
    }
}
