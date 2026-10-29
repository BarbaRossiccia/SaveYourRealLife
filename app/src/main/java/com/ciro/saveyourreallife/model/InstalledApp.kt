package com.ciro.saveyourreallife.model

import android.graphics.Bitmap

data class InstalledApp(
    val packageName: String,
    val name: String,
    val icon: Bitmap,
)
