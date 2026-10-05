package com.fsmediaplayer.app.core.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val contentUri: Uri,
    val name: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val resolution: String?,
    val dateModified: Long,
    val bucketId: String,
    val bucketName: String,
    val mimeType: String
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}
