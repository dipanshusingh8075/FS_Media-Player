package com.fsmediaplayer.app.core.designsystem.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

private enum class DragMode {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK
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

    // Screen brightness (0.0f to 1.0f)
    var currentBrightness by remember {
        val initial = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableFloatStateOf(if (initial < 0f) 0.5f else initial)
    }

    var activeHUD by remember { mutableStateOf<ActiveGestureHUD?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Gesture tracking variables
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
                    // When locked, only pass single tap to show the unlock button
                    detectTapGestures(onTap = { onSingleTap() })
                    return@pointerInput
                }

                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        when {
                            offset.x < screenWidth * 0.35f -> {
                                // Left 35% -> Rewind 10s
                                onDoubleTapSeek(-10_000L)
                            }
                            offset.x > screenWidth * 0.65f -> {
                                // Right 35% -> Forward 10s
                                onDoubleTapSeek(10_000L)
                            }
                            else -> {
                                // Center -> Play/Pause or toggle
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

                        var pointer = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.firstOrNull { it.id == pointer }
                            if (dragChange == null || !dragChange.pressed) {
                                // Pointer lifted or cancelled
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

                            // Determine gesture type once threshold is exceeded
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
                                    // Moving finger up reduces Y (deltaY < 0), which should increase brightness
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
                                    // Moving finger up reduces Y, which should increase volume
                                    val volumeDeltaFraction = -deltaY / (size.height * 0.6f)
                                    val step = volumeDeltaFraction * maxVolume
                                    val rawVolume = (currentVolume + step).coerceIn(0f, maxVolume.toFloat())
                                    currentVolume = rawVolume.toInt()

                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        currentVolume,
                                        0 // no system popup to keep original sleek HUD
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

        // Render Centered Gesture Feedback HUD
        GestureHUDContainer(
            activeHUD = activeHUD,
            modifier = Modifier.align(Alignment.Center)
        )
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
