package com.fsmediaplayer.app.domain.repository

import com.fsmediaplayer.app.core.model.VideoFolder
import com.fsmediaplayer.app.core.model.VideoItem
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    /**
     * Observes all video files available on device storage.
     */
    fun getAllVideos(): Flow<List<VideoItem>>

    /**
     * Observes all video folders grouped by bucket.
     */
    fun getVideoFolders(): Flow<List<VideoFolder>>

    /**
     * Observes video files within a specific folder/bucket ID.
     */
    fun getVideosInFolder(bucketId: String): Flow<List<VideoItem>>

    /**
     * Fetches a single video by its MediaStore ID.
     */
    fun getVideoById(id: Long): Flow<VideoItem?>
}
