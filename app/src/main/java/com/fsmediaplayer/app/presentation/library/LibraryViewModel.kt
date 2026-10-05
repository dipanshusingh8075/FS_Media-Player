package com.fsmediaplayer.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsmediaplayer.app.core.model.VideoFolder
import com.fsmediaplayer.app.core.model.VideoItem
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

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.FOLDERS,
    val folders: List<VideoFolder> = emptyList(),
    val allVideos: List<VideoItem> = emptyList(),
    val folderVideos: List<VideoItem> = emptyList(),
    val activeFolder: VideoFolder? = null,
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

    fun openFolder(folder: VideoFolder) {
        _uiState.update { it.copy(activeFolder = folder) }
        viewModelScope.launch {
            getVideosUseCase(folder.id)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { videos ->
                    _uiState.update { it.copy(folderVideos = videos) }
                }
        }
    }

    fun closeActiveFolder() {
        _uiState.update { it.copy(activeFolder = null, folderVideos = emptyList()) }
    }

    private fun loadMediaLibrary() {
        viewModelScope.launch {
            // Load Grouped Folders
            launch {
                getVideoFoldersUseCase()
                    .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                    .collect { folders ->
                        _uiState.update { it.copy(folders = folders, isLoading = false) }
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
