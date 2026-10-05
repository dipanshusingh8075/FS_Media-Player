package com.fsmediaplayer.app.core.model

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val videoTitle: String = "",
    val playbackSpeed: Float = 1.0f,
    val decoderMode: DecoderMode = DecoderMode.HARDWARE,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val isLocked: Boolean = false,
    val isControlsVisible: Boolean = true,
    val audioTracks: List<AudioTrackItem> = emptyList(),
    val subtitleTracks: List<SubtitleTrackItem> = emptyList()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedCurrentPosition: String
        get() = formatTime(currentPositionMs)

    val formattedDuration: String
        get() = formatTime(durationMs)

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
