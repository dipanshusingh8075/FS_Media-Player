package com.fsmediaplayer.app.presentation.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.fsmediaplayer.app.core.designsystem.components.GlassmorphicPlayerControls
import com.fsmediaplayer.app.core.designsystem.components.PlayerGestureOverlay
import com.fsmediaplayer.app.core.model.AspectRatioMode
import com.fsmediaplayer.app.core.model.DecoderMode
import com.fsmediaplayer.app.core.player.FSPlayerManager
import com.fsmediaplayer.app.presentation.player.dialogs.AudioTrackDialog
import com.fsmediaplayer.app.presentation.player.dialogs.PlaybackSpeedDialog
import com.fsmediaplayer.app.presentation.player.dialogs.SubtitleTrackDialog
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    playerManager: FSPlayerManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uiState by playerManager.uiState.collectAsState()

    // Dialog States
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // External Subtitle Picker launcher
    val subtitleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            playerManager.addExternalSubtitle(it, label = "External Subtitle")
        }
    }

    // Auto-hide controls timer (4 seconds)
    LaunchedEffect(uiState.isControlsVisible, uiState.isPlaying) {
        if (uiState.isControlsVisible && uiState.isPlaying && !uiState.isLocked) {
            delay(4000)
            playerManager.setControlsVisibility(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Media3 ExoPlayer Surface View Container
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false // Custom Jetpack Compose controls are used!
                    player = playerManager.getPlayer()
                    resizeMode = uiState.aspectRatioMode.resizeMode
                }
            },
            update = { playerView ->
                playerView.player = playerManager.getPlayer()
                playerView.resizeMode = uiState.aspectRatioMode.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Gesture Overlay (Left: Brightness, Right: Volume, Horizontal: Seek)
        PlayerGestureOverlay(
            isLocked = uiState.isLocked,
            currentPositionMs = uiState.currentPositionMs,
            durationMs = uiState.durationMs,
            onSingleTap = {
                playerManager.toggleControlsVisibility()
            },
            onDoubleTapSeek = { deltaMs ->
                playerManager.seekDelta(deltaMs)
            },
            onSeekCommit = { targetMs ->
                playerManager.seekTo(targetMs)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            // Glassmorphic Player Controller
            GlassmorphicPlayerControls(
                uiState = uiState,
                onBackClick = onBackClick,
                onPlayPauseClick = { playerManager.togglePlayPause() },
                onSeekDelta = { deltaMs -> playerManager.seekDelta(deltaMs) },
                onSeekTo = { targetMs -> playerManager.seekTo(targetMs) },
                onToggleLock = { playerManager.toggleLock() },
                onToggleDecoder = {
                    val nextMode = if (uiState.decoderMode == DecoderMode.HARDWARE) {
                        DecoderMode.SOFTWARE
                    } else {
                        DecoderMode.HARDWARE
                    }
                    playerManager.switchDecoderMode(nextMode)
                },
                onAspectRatioClick = {
                    val nextAspect = when (uiState.aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioMode.FILL
                        AspectRatioMode.FILL -> AspectRatioMode.ZOOM
                        AspectRatioMode.ZOOM -> AspectRatioMode.FIXED_HEIGHT
                        AspectRatioMode.FIXED_HEIGHT -> AspectRatioMode.FIT
                    }
                    playerManager.setAspectRatioMode(nextAspect)
                },
                onSpeedClick = { showSpeedDialog = true },
                onAudioTrackClick = { showAudioDialog = true },
                onSubtitleClick = { showSubtitleDialog = true },
                onPiPClick = {
                    enterPictureInPicture(activity)
                }
            )
        }
    }

    // Dialogs
    if (showAudioDialog) {
        AudioTrackDialog(
            tracks = uiState.audioTracks,
            onSelectTrack = { groupIdx, trackIdx ->
                playerManager.selectAudioTrack(groupIdx, trackIdx)
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    if (showSubtitleDialog) {
        SubtitleTrackDialog(
            tracks = uiState.subtitleTracks,
            onSelectTrack = { groupIdx, trackIdx ->
                playerManager.selectSubtitleTrack(groupIdx, trackIdx)
            },
            onPickExternalSubtitle = {
                subtitleFilePicker.launch(arrayOf("*/*"))
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = { speed ->
                playerManager.setPlaybackSpeed(speed)
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

private fun enterPictureInPicture(activity: Activity?) {
    if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
