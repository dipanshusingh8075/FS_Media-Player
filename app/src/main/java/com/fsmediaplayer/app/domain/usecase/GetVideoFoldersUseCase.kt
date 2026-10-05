package com.fsmediaplayer.app.domain.usecase

import com.fsmediaplayer.app.core.model.VideoMediaItem
import com.fsmediaplayer.app.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVideoFoldersUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(): Flow<Map<String, List<VideoMediaItem>>> {
        return repository.getVideoFolders()
    }
}
