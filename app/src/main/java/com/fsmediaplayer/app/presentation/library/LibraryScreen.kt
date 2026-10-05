package com.fsmediaplayer.app.presentation.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.fsmediaplayer.app.core.designsystem.theme.CyberEmerald
import com.fsmediaplayer.app.core.designsystem.theme.ElectricCyan
import com.fsmediaplayer.app.core.designsystem.theme.GlassBlack80
import com.fsmediaplayer.app.core.designsystem.theme.GlassWhite10
import com.fsmediaplayer.app.core.designsystem.theme.ObsidianBackground
import com.fsmediaplayer.app.core.designsystem.theme.SurfaceContainer
import com.fsmediaplayer.app.core.designsystem.theme.SurfaceContainerHigh
import com.fsmediaplayer.app.core.designsystem.theme.TextMuted
import com.fsmediaplayer.app.core.designsystem.theme.TextPrimary
import com.fsmediaplayer.app.core.designsystem.theme.TextSecondary
import com.fsmediaplayer.app.core.model.VideoFolder
import com.fsmediaplayer.app.core.model.VideoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Scoped storage permission launcher (Android 13+ vs Android 12 & below)
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionToRequest)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (uiState.activeFolder != null) {
                                uiState.activeFolder!!.name
                            } else {
                                "FS Media Player"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.activeFolder != null) {
                        IconButton(onClick = { viewModel.closeActiveFolder() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBackground
                )
            )
        },
        containerColor = ObsidianBackground,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.hasStoragePermission) {
                // Permission Request State
                PermissionPromptContent(
                    onRequestPermission = { permissionLauncher.launch(permissionToRequest) }
                )
            } else if (uiState.isLoading) {
                // Loading State
                CircularProgressIndicator(
                    color = ElectricCyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.activeFolder != null) {
                // Videos within active folder
                VideoList(
                    videos = uiState.folderVideos,
                    onVideoClick = onVideoClick
                )
            } else {
                // Folder vs Flat List tabs
                Column(modifier = Modifier.fillMaxSize()) {
                    PrimaryTabRow(
                        selectedTabIndex = uiState.selectedTab.ordinal,
                        containerColor = ObsidianBackground,
                        contentColor = ElectricCyan,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(uiState.selectedTab.ordinal),
                                color = ElectricCyan
                            )
                        }
                    ) {
                        Tab(
                            selected = uiState.selectedTab == LibraryTab.FOLDERS,
                            onClick = { viewModel.selectTab(LibraryTab.FOLDERS) },
                            text = {
                                Text(
                                    "Folders (${uiState.folders.size})",
                                    fontWeight = if (uiState.selectedTab == LibraryTab.FOLDERS) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = { Icon(Icons.Rounded.Folder, null) }
                        )
                        Tab(
                            selected = uiState.selectedTab == LibraryTab.ALL_VIDEOS,
                            onClick = { viewModel.selectTab(LibraryTab.ALL_VIDEOS) },
                            text = {
                                Text(
                                    "All Videos (${uiState.allVideos.size})",
                                    fontWeight = if (uiState.selectedTab == LibraryTab.ALL_VIDEOS) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = { Icon(Icons.Rounded.VideoLibrary, null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedContent(
                        targetState = uiState.selectedTab,
                        label = "LibraryTabAnimation"
                    ) { tab ->
                        when (tab) {
                            LibraryTab.FOLDERS -> {
                                FolderGrid(
                                    folders = uiState.folders,
                                    onFolderClick = { viewModel.openFolder(it) }
                                )
                            }
                            LibraryTab.ALL_VIDEOS -> {
                                VideoList(
                                    videos = uiState.allVideos,
                                    onVideoClick = onVideoClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderGrid(
    folders: List<VideoFolder>,
    onFolderClick: (VideoFolder) -> Unit
) {
    if (folders.isEmpty()) {
        EmptyState(message = "No video folders discovered")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders, key = { it.id }) { folder ->
            FolderCard(folder = folder, onClick = { onFolderClick(folder) })
        }
    }
}

@Composable
private fun FolderCard(
    folder: VideoFolder,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, GlassWhite10, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        // Thumbnail from first video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .background(SurfaceContainerHigh)
        ) {
            if (folder.firstVideoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(folder.firstVideoUri)
                        .videoFrameMillis(2000)
                        .crossfade(true)
                        .build(),
                    contentDescription = folder.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Video count badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassBlack80)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${folder.videoCount} videos",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Folder Title and Size
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = folder.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = folder.formattedTotalSize,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun VideoList(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit
) {
    if (videos.isEmpty()) {
        EmptyState(message = "No videos found in this view")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(videos, key = { it.id }) { video ->
            VideoListItem(video = video, onClick = { onVideoClick(video) })
        }
    }
}

@Composable
private fun VideoListItem(
    video: VideoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, GlassWhite10, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with Duration pill
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerHigh)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.contentUri)
                    .videoFrameMillis(2000)
                    .crossfade(true)
                    .build(),
                contentDescription = video.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GlassBlack80)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Video metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = video.formattedSize,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                video.resolution?.let { res ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = res,
                            color = ElectricCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Play",
            tint = ElectricCyan,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PermissionPromptContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Storage Permission Required",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "FS Media Player needs permission to read video files on your device to display your library.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = ObsidianBackground
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Permission", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextMuted,
            fontSize = 14.sp
        )
    }
}
