package com.ciro.saveyourreallife.model

object AppEligibility {
    fun isAllowed(
        packageName: String,
        ownPackageName: String,
        flags: Int,
        systemFlag: Int,
        updatedSystemFlag: Int,
    ): Boolean = packageName != ownPackageName &&
        flags and (systemFlag or updatedSystemFlag) == 0
}
