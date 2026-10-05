package com.fsmediaplayer.app.domain.usecase

import com.fsmediaplayer.app.core.model.VideoFolder
import com.fsmediaplayer.app.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVideoFoldersUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(): Flow<List<VideoFolder>> {
        return repository.getVideoFolders()
    }
}
