package com.fsmediaplayer.app.core.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.fsmediaplayer.app.core.model.AspectRatioMode
import com.fsmediaplayer.app.core.model.AudioTrackItem
import com.fsmediaplayer.app.core.model.DecoderMode
import com.fsmediaplayer.app.core.model.PlayerUiState
import com.fsmediaplayer.app.core.model.SubtitleTrackItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class FSPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaUri: Uri? = null
    private var currentMediaTitle: String = ""
    private var externalSubtitles = mutableListOf<MediaItem.SubtitleConfiguration>()
    private var tickerJob: Job? = null

    init {
        initializePlayer(DecoderMode.HARDWARE)
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    private fun initializePlayer(decoderMode: DecoderMode) {
        val renderersFactory = RenderersFactoryProvider.buildRenderersFactory(context, decoderMode)
        trackSelector = DefaultTrackSelector(context)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector!!)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        player.addListener(playerListener)
        exoPlayer = player

        _uiState.update { it.copy(decoderMode = decoderMode) }
        startProgressTicker()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            _uiState.update {
                it.copy(
                    isBuffering = isBuffering,
                    durationMs = (exoPlayer?.duration ?: 0L).coerceAtLeast(0L)
                )
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTracksList(tracks)
        }
    }

    fun playMedia(uri: Uri, title: String) {
        currentMediaUri = uri
        currentMediaTitle = title
        externalSubtitles.clear()

        val mediaItem = buildMediaItem(uri, title, externalSubtitles)

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }

        _uiState.update {
            it.copy(
                videoTitle = title,
                currentPositionMs = 0L,
                durationMs = 0L
            )
        }
    }

    private fun buildMediaItem(
        uri: Uri,
        title: String,
        subtitles: List<MediaItem.SubtitleConfiguration>
    ): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(uri.toString())
            .setSubtitleConfigurations(subtitles)
            .build()
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceIn(0L, exoPlayer?.duration ?: Long.MAX_VALUE))
    }

    fun seekDelta(deltaMs: Long) {
        exoPlayer?.let {
            val target = (it.currentPosition + deltaMs).coerceIn(0L, it.duration)
            it.seekTo(target)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setAspectRatioMode(mode: AspectRatioMode) {
        _uiState.update { it.copy(aspectRatioMode = mode) }
    }

    fun toggleControlsVisibility() {
        if (!_uiState.value.isLocked) {
            _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
        }
    }

    fun setControlsVisibility(visible: Boolean) {
        if (!_uiState.value.isLocked) {
            _uiState.update { it.copy(isControlsVisible = visible) }
        }
    }

    fun toggleLock() {
        _uiState.update {
            val newLocked = !it.isLocked
            it.copy(
                isLocked = newLocked,
                isControlsVisible = !newLocked // Hide controls immediately when locked
            )
        }
    }

    /**
     * Seamlessly toggles between Hardware and Software decoding,
     * preserving playback position and state.
     */
    fun switchDecoderMode(newMode: DecoderMode) {
        if (_uiState.value.decoderMode == newMode) return

        val currentPlayer = exoPlayer ?: return
        val currentPosition = currentPlayer.currentPosition
        val shouldPlay = currentPlayer.isPlaying
        val currentSpeed = currentPlayer.playbackParameters.speed

        // Release old player
        currentPlayer.removeListener(playerListener)
        currentPlayer.release()
        exoPlayer = null

        // Recreate with selected decoding engine
        initializePlayer(newMode)

        // Restore media item and state
        currentMediaUri?.let { uri ->
            val mediaItem = buildMediaItem(uri, currentMediaTitle, externalSubtitles)
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                setPlaybackSpeed(currentSpeed)
                prepare()
                seekTo(currentPosition)
                playWhenReady = shouldPlay
            }
        }
    }

    /**
     * Adds an external subtitle file (.srt or .vtt).
     */
    fun addExternalSubtitle(uri: Uri, mimeType: String = MimeTypes.APPLICATION_SUBRIP, label: String = "External Subtitle") {
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage("und")
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        externalSubtitles.add(subtitleConfig)

        // Refresh player media item
        currentMediaUri?.let { mediaUri ->
            val currentPos = exoPlayer?.currentPosition ?: 0L
            val isPlaying = exoPlayer?.isPlaying ?: false
            val mediaItem = buildMediaItem(mediaUri, currentMediaTitle, externalSubtitles)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.seekTo(currentPos)
            if (isPlaying) exoPlayer?.play()
        }
    }

    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        val player = exoPlayer ?: return
        val tracks = player.currentTracks
        var currentAudioGroup = 0

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                if (currentAudioGroup == groupIndex) {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                        .build()
                    break
                }
                currentAudioGroup++
            }
        }
    }

    fun selectSubtitleTrack(groupIndex: Int, trackIndex: Int) {
        val player = exoPlayer ?: return
        if (groupIndex == -1) {
            // Disable subtitles
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            return
        }

        val tracks = player.currentTracks
        var currentTextGroup = 0

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                if (currentTextGroup == groupIndex) {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                        .build()
                    break
                }
                currentTextGroup++
            }
        }
    }

    private fun updateTracksList(tracks: Tracks) {
        val audioList = mutableListOf<AudioTrackItem>()
        val subtitleList = mutableListOf<SubtitleTrackItem>()

        var audioGroupCount = 0
        var textGroupCount = 0

        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = group.isTrackSelected(i)
                        val label = format.label ?: format.language ?: "Audio Track ${audioList.size + 1}"
                        audioList.add(
                            AudioTrackItem(
                                groupIndex = audioGroupCount,
                                trackIndex = i,
                                id = format.id ?: "$audioGroupCount-$i",
                                label = label,
                                language = format.language,
                                channelCount = format.channelCount,
                                isSelected = isSelected
                            )
                        )
                    }
                    audioGroupCount++
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = group.isTrackSelected(i)
                        val label = format.label ?: format.language ?: "Subtitle ${subtitleList.size + 1}"
                        subtitleList.add(
                            SubtitleTrackItem(
                                groupIndex = textGroupCount,
                                trackIndex = i,
                                id = format.id ?: "$textGroupCount-$i",
                                label = label,
                                language = format.language,
                                isSelected = isSelected
                            )
                        )
                    }
                    textGroupCount++
                }
            }
        }

        _uiState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subtitleList
            )
        }
    }

    private fun startProgressTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _uiState.update {
                        it.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L),
                            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                        )
                    }
                }
                delay(300)
            }
        }
    }

    fun release() {
        tickerJob?.cancel()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }
}
