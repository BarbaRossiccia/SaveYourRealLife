package com.ciro.saveyourreallife.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import com.ciro.saveyourreallife.model.AppEligibility
import com.ciro.saveyourreallife.model.InstalledApp
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {
    suspend fun loadLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance(Locale.getDefault())

        packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .filter { applicationInfo ->
                AppEligibility.isAllowed(
                    packageName = applicationInfo.packageName,
                    ownPackageName = context.packageName,
                    flags = applicationInfo.flags,
                    systemFlag = ApplicationInfo.FLAG_SYSTEM,
                    updatedSystemFlag = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
                )
            }
            .distinctBy { it.packageName }
            .mapNotNull { applicationInfo ->
                runCatching {
                    InstalledApp(
                        packageName = applicationInfo.packageName,
                        name = packageManager.getApplicationLabel(applicationInfo).toString(),
                        icon = packageManager.getApplicationIcon(applicationInfo).toBitmap(96, 96),
                    )
                }.getOrNull()
            }
            .sortedWith { first, second -> collator.compare(first.name, second.name) }
            .toList()
    }
}
