package com.fsmediaplayer.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsmediaplayer.app.core.model.VideoMediaItem
import com.fsmediaplayer.app.domain.usecase.GetVideoFoldersUseCase
import com.fsmediaplayer.app.domain.usecase.GetVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab {
    FOLDERS,
    ALL_VIDEOS
}

data class VideoFolderItem(
    val name: String,
    val videos: List<VideoMediaItem>
) {
    val videoCount: Int get() = videos.size
    val firstVideoUri get() = videos.firstOrNull()?.contentUri
    val totalSizeBytes: Long get() = videos.sumOf { it.sizeBytes }

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

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.FOLDERS,
    val folderMap: Map<String, List<VideoMediaItem>> = emptyMap(),
    val folders: List<VideoFolderItem> = emptyList(),
    val allVideos: List<VideoMediaItem> = emptyList(),
    val activeFolder: VideoFolderItem? = null,
    val isLoading: Boolean = true,
    val hasStoragePermission: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getVideosUseCase: GetVideosUseCase,
    private val getVideoFoldersUseCase: GetVideoFoldersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasStoragePermission = true, isLoading = true) }
        loadMediaLibrary()
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab, activeFolder = null) }
    }

    fun openFolder(folder: VideoFolderItem) {
        _uiState.update { it.copy(activeFolder = folder) }
    }

    fun closeActiveFolder() {
        _uiState.update { it.copy(activeFolder = null) }
    }

    private fun loadMediaLibrary() {
        viewModelScope.launch {
            // Load Grouped Folders
            launch {
                getVideoFoldersUseCase()
                    .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                    .collect { folderMap ->
                        val folderList = folderMap.map { (name, videos) ->
                            VideoFolderItem(name = name, videos = videos)
                        }.sortedByDescending { it.videoCount }

                        _uiState.update {
                            it.copy(
                                folderMap = folderMap,
                                folders = folderList,
                                isLoading = false
                            )
                        }
                    }
            }

            // Load All Videos (flat list)
            launch {
                getVideosUseCase()
                    .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                    .collect { videos ->
                        _uiState.update { it.copy(allVideos = videos, isLoading = false) }
                    }
            }
        }
    }
}
