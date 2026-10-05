package com.fsmediaplayer.app.domain.usecase

import com.fsmediaplayer.app.core.model.VideoItem
import com.fsmediaplayer.app.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVideosUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(folderId: String? = null): Flow<List<VideoItem>> {
        return if (folderId.isNullOrEmpty()) {
            repository.getAllVideos()
        } else {
            repository.getVideosInFolder(folderId)
        }
    }
}
