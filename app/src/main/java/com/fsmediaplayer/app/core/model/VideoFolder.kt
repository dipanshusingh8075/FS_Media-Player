package com.fsmediaplayer.app.core.model

import android.net.Uri

data class VideoFolder(
    val id: String,
    val name: String,
    val videoCount: Int,
    val firstVideoUri: Uri?,
    val totalSizeBytes: Long
) {
    val formattedTotalSize: String
        get() {
            val kb = totalSizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}
