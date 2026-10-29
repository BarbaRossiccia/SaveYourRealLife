package com.ciro.saveyourreallife.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppEligibilityTest {
    private val system = 1
    private val updatedSystem = 128

    @Test
    fun allowsOnlyThirdPartyAppsOtherThanThisApp() {
        assertTrue(AppEligibility.isAllowed("other", "self", 0, system, updatedSystem))
        assertFalse(AppEligibility.isAllowed("self", "self", 0, system, updatedSystem))
        assertFalse(AppEligibility.isAllowed("system", "self", system, system, updatedSystem))
        assertFalse(
            AppEligibility.isAllowed(
                "updated.system",
                "self",
                updatedSystem,
                system,
                updatedSystem,
            ),
        )
    }
}
