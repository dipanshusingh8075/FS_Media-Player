package com.fsmediaplayer.app.core.designsystem.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsmediaplayer.app.core.designsystem.theme.ElectricCyan
import com.fsmediaplayer.app.core.designsystem.theme.GlassBlack80
import com.fsmediaplayer.app.core.designsystem.theme.GlassWhite20
import com.fsmediaplayer.app.core.designsystem.theme.TextPrimary
import kotlin.math.abs

private enum class DragMode {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK
}

sealed class ActiveGestureHUD {
    data class Brightness(val progressFraction: Float) : ActiveGestureHUD()
    data class Volume(val current: Int, val max: Int) : ActiveGestureHUD()
    data class Seek(val targetMs: Long, val durationMs: Long, val deltaMs: Long) : ActiveGestureHUD()
}

@Composable
fun PlayerGestureOverlay(
    isLocked: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (deltaMs: Long) -> Unit,
    onSeekCommit: (targetPositionMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    // Screen brightness (0.01f to 1.0f) without WRITE_SETTINGS permission
    var currentBrightness by remember {
        val initial = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableFloatStateOf(if (initial < 0f) 0.5f else initial)
    }

    var activeHUD by remember { mutableStateOf<ActiveGestureHUD?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Gesture tracking state
    var dragMode by remember { mutableStateOf(DragMode.NONE) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var initialSeekPosition by remember { mutableLongStateOf(0L) }
    var targetSeekPosition by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(isLocked) {
                if (isLocked) {
                    // When locked, only pass single tap to trigger unlock button overlay
                    detectTapGestures(onTap = { onSingleTap() })
                    return@pointerInput
                }

                // Double tap and single tap gesture detection
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        when {
                            offset.x < screenWidth * 0.35f -> {
                                // Double tap left 35% -> Seek back 10 seconds
                                onDoubleTapSeek(-10_000L)
                            }
                            offset.x > screenWidth * 0.65f -> {
                                // Double tap right 35% -> Seek forward 10 seconds
                                onDoubleTapSeek(10_000L)
                            }
                            else -> {
                                // Center double tap -> Toggle controls / PlayPause
                                onSingleTap()
                            }
                        }
                    }
                )
            }
            .pointerInput(isLocked, durationMs, currentPositionMs) {
                if (isLocked) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPosition = down.position

                        dragMode = DragMode.NONE
                        totalDragX = 0f
                        totalDragY = 0f
                        initialSeekPosition = currentPositionMs
                        targetSeekPosition = currentPositionMs

                        val pointer = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.firstOrNull { it.id == pointer }
                            if (dragChange == null || !dragChange.pressed) {
                                // Gesture released or cancelled
                                if (dragMode == DragMode.SEEK && durationMs > 0) {
                                    onSeekCommit(targetSeekPosition)
                                }
                                dragMode = DragMode.NONE
                                activeHUD = null
                                break
                            }

                            val deltaX = dragChange.position.x - dragChange.previousPosition.x
                            val deltaY = dragChange.position.y - dragChange.previousPosition.y

                            totalDragX += deltaX
                            totalDragY += deltaY

                            // Threshold detection to distinguish horizontal from vertical drag
                            val touchSlop = 16f
                            if (dragMode == DragMode.NONE) {
                                if (abs(totalDragX) > touchSlop && abs(totalDragX) > abs(totalDragY)) {
                                    dragMode = DragMode.SEEK
                                } else if (abs(totalDragY) > touchSlop) {
                                    val isLeftSide = startPosition.x < (size.width / 2f)
                                    dragMode = if (isLeftSide) DragMode.BRIGHTNESS else DragMode.VOLUME
                                }
                            }

                            when (dragMode) {
                                DragMode.BRIGHTNESS -> {
                                    dragChange.consume()
                                    // Moving finger up reduces Y (deltaY < 0), which increases brightness
                                    val brightnessDelta = -deltaY / (size.height * 0.75f)
                                    currentBrightness = (currentBrightness + brightnessDelta).coerceIn(0.01f, 1.0f)

                                    activity?.let { act ->
                                        val layoutParams: WindowManager.LayoutParams = act.window.attributes
                                        layoutParams.screenBrightness = currentBrightness
                                        act.window.attributes = layoutParams
                                    }

                                    activeHUD = ActiveGestureHUD.Brightness(currentBrightness)
                                }

                                DragMode.VOLUME -> {
                                    dragChange.consume()
                                    // Moving finger up reduces Y, which increases volume
                                    val volumeDeltaFraction = -deltaY / (size.height * 0.6f)
                                    val step = volumeDeltaFraction * maxVolume
                                    val rawVolume = (currentVolume + step).coerceIn(0f, maxVolume.toFloat())
                                    currentVolume = rawVolume.toInt()

                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        currentVolume,
                                        0 // Silent update to show our own clean HUD
                                    )

                                    activeHUD = ActiveGestureHUD.Volume(currentVolume, maxVolume)
                                }

                                DragMode.SEEK -> {
                                    dragChange.consume()
                                    if (durationMs > 0) {
                                        // 1 full screen width drag = 90 seconds seek delta
                                        val seekRatio = totalDragX / size.width
                                        val maxSeekSpanMs = 90_000L
                                        val deltaMs = (seekRatio * maxSeekSpanMs).toLong()
                                        targetSeekPosition = (initialSeekPosition + deltaMs).coerceIn(0L, durationMs)

                                        activeHUD = ActiveGestureHUD.Seek(
                                            targetMs = targetSeekPosition,
                                            durationMs = durationMs,
                                            deltaMs = targetSeekPosition - initialSeekPosition
                                        )
                                    }
                                }

                                DragMode.NONE -> {
                                    // Below threshold
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()

        // Floating Glassmorphic Pill HUD Indicator
        GestureHUDOverlay(
            activeHUD = activeHUD,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun GestureHUDOverlay(
    activeHUD: ActiveGestureHUD?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeHUD != null,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(GlassBlack80)
                .border(1.dp, GlassWhite20, RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            when (activeHUD) {
                is ActiveGestureHUD.Brightness -> BrightnessHUD(fraction = activeHUD.progressFraction)
                is ActiveGestureHUD.Volume -> VolumeHUD(current = activeHUD.current, max = activeHUD.max)
                is ActiveGestureHUD.Seek -> SeekHUD(
                    targetMs = activeHUD.targetMs,
                    durationMs = activeHUD.durationMs,
                    deltaMs = activeHUD.deltaMs
                )
                null -> {}
            }
        }
    }
}

@Composable
private fun BrightnessHUD(fraction: Float) {
    val percentage = (fraction * 100).toInt().coerceIn(0, 100)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.BrightnessMedium,
            contentDescription = "Brightness",
            tint = ElectricCyan,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .width(130.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ElectricCyan,
            trackColor = Color(0x33FFFFFF),
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$percentage%",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun VolumeHUD(current: Int, max: Int) {
    val fraction = if (max > 0) current.toFloat() / max.toFloat() else 0f
    val percentage = (fraction * 100).toInt().coerceIn(0, 100)

    val icon = when {
        current == 0 -> Icons.Rounded.VolumeMute
        fraction < 0.5f -> Icons.Rounded.VolumeDown
        else -> Icons.Rounded.VolumeUp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Volume",
            tint = ElectricCyan,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .width(130.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ElectricCyan,
            trackColor = Color(0x33FFFFFF),
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$percentage%",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SeekHUD(targetMs: Long, durationMs: Long, deltaMs: Long) {
    val isForward = deltaMs >= 0
    val deltaSeconds = kotlin.math.abs(deltaMs / 1000)
    val deltaString = if (isForward) "+${deltaSeconds}s" else "-${deltaSeconds}s"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = deltaString,
                color = ElectricCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${formatDuration(targetMs)} / ${formatDuration(durationMs)}",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitFirstDown(
    requireUnconsumed: Boolean = true
): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        val down = event.changes.firstOrNull { if (requireUnconsumed) it.pressed && !it.isConsumed else it.pressed }
        if (down != null) return down
    }
}
