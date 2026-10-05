package com.fsmediaplayer.app.domain.usecase

import com.fsmediaplayer.app.core.model.VideoMediaItem
import com.fsmediaplayer.app.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVideosUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(): Flow<List<VideoMediaItem>> {
        return repository.getAllVideos()
    }
}
