package com.ciro.saveyourreallife.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockEventGuardTest {
    @Test
    fun repeatedEventsForSamePackageAreIgnoredInsideWindow() {
        val guard = BlockEventGuard(1_500)

        assertTrue(guard.shouldBlock("example.one", blocked = true, nowMillis = 1_000))
        assertFalse(guard.shouldBlock("example.one", blocked = true, nowMillis = 2_000))
        assertFalse(guard.shouldBlock("example.one", blocked = true, nowMillis = 3_000))
    }

    @Test
    fun anotherPackageCanBeBlockedImmediately() {
        val guard = BlockEventGuard(1_500)

        assertTrue(guard.shouldBlock("example.one", blocked = true, nowMillis = 1_000))
        assertTrue(guard.shouldBlock("example.two", blocked = true, nowMillis = 1_100))
    }

    @Test
    fun homeAllowsImmediateReopeningOfSameBlockedPackageInsideWindow() {
        val guard = BlockEventGuard(1_500)

        assertTrue(guard.shouldBlock("example.one", blocked = true, nowMillis = 1_000))
        assertFalse(guard.shouldBlock("example.one", blocked = true, nowMillis = 1_050))
        assertFalse(guard.shouldBlock("launcher", blocked = false, nowMillis = 1_100))
        assertTrue(guard.shouldBlock("example.one", blocked = true, nowMillis = 1_200))
        assertFalse(guard.shouldBlock("example.one", blocked = true, nowMillis = 1_250))
        assertFalse(guard.shouldBlock("launcher", blocked = false, nowMillis = 1_300))
        assertTrue(guard.shouldBlock("example.one", blocked = true, nowMillis = 1_400))
    }
}
