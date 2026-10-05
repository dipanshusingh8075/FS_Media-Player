package com.fsmediaplayer.app.core.model

import android.net.Uri

data class AudioTrackItem(
    val groupIndex: Int,
    val trackIndex: Int,
    val id: String,
    val label: String,
    val language: String?,
    val channelCount: Int,
    val isSelected: Boolean
)

data class SubtitleTrackItem(
    val groupIndex: Int,
    val trackIndex: Int,
    val id: String,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
    val isExternal: Boolean = false,
    val uri: Uri? = null
)
