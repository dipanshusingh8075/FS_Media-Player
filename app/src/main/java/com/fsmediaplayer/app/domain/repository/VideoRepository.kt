package com.fsmediaplayer.app.domain.repository

import com.fsmediaplayer.app.core.model.VideoMediaItem
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    /**
     * Observes all valid local video files from MediaStore, sorted by date modified descending.
     */
    fun getAllVideos(): Flow<List<VideoMediaItem>>

    /**
     * Observes local videos grouped by their containing folder (bucket).
     */
    fun getVideoFolders(): Flow<Map<String, List<VideoMediaItem>>>
}
