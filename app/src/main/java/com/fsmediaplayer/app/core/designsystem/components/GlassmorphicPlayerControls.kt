package com.fsmediaplayer.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsmediaplayer.app.core.designsystem.theme.CyberEmerald
import com.fsmediaplayer.app.core.designsystem.theme.ElectricCyan
import com.fsmediaplayer.app.core.designsystem.theme.GlassBlack60
import com.fsmediaplayer.app.core.designsystem.theme.GlassBlack80
import com.fsmediaplayer.app.core.designsystem.theme.GlassWhite10
import com.fsmediaplayer.app.core.designsystem.theme.GlassWhite20
import com.fsmediaplayer.app.core.designsystem.theme.ObsidianBackground
import com.fsmediaplayer.app.core.designsystem.theme.TextPrimary
import com.fsmediaplayer.app.core.designsystem.theme.TextSecondary
import com.fsmediaplayer.app.core.model.AspectRatioMode
import com.fsmediaplayer.app.core.model.DecoderMode
import com.fsmediaplayer.app.core.model.PlayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassmorphicPlayerControls(
    uiState: PlayerUiState,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeekDelta: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleLock: () -> Unit,
    onToggleDecoder: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onAudioTrackClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onPiPClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var draggedPositionMs by remember { mutableFloatStateOf(0f) }
    var isMenuOpen by remember { mutableStateOf(false) }

    // Floating Lock button if player is locked
    if (uiState.isLocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Surface(
                onClick = onToggleLock,
                shape = CircleShape,
                color = GlassBlack80,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite20),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Unlock Controls",
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        return
    }

    AnimatedVisibility(
        visible = uiState.isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GlassBlack80,
                            Color.Transparent,
                            Color.Transparent,
                            GlassBlack80
                        )
                    )
                )
        ) {
            // ================= TOP HEADER BAR =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = uiState.videoTitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // HW / SW Decoder Toggle Chip
                DecoderChip(
                    mode = uiState.decoderMode,
                    onClick = onToggleDecoder
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Picture-in-Picture
                IconButton(onClick = onPiPClick) {
                    Icon(
                        imageVector = Icons.Rounded.PictureInPictureAlt,
                        contentDescription = "Picture in Picture",
                        tint = TextPrimary
                    )
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { isMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More Options",
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        modifier = Modifier.background(ObsidianBackground)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Audio Track", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Rounded.Audiotrack, null, tint = ElectricCyan) },
                            onClick = {
                                isMenuOpen = false
                                onAudioTrackClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Subtitles", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Rounded.Subtitles, null, tint = ElectricCyan) },
                            onClick = {
                                isMenuOpen = false
                                onSubtitleClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Playback Speed (${uiState.playbackSpeed}x)", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Rounded.Speed, null, tint = ElectricCyan) },
                            onClick = {
                                isMenuOpen = false
                                onSpeedClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Aspect Ratio (${uiState.aspectRatioMode.title})", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Rounded.AspectRatio, null, tint = ElectricCyan) },
                            onClick = {
                                isMenuOpen = false
                                onAspectRatioClick()
                            }
                        )
                    }
                }
            }

            // ================= CENTER CONTROLS =================
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s
                IconButton(
                    onClick = { onSeekDelta(-10_000L) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GlassBlack60)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Center Play / Pause / Buffering button
                Surface(
                    onClick = onPlayPauseClick,
                    shape = CircleShape,
                    color = GlassBlack80,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (uiState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = ElectricCyan,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = ElectricCyan,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }

                // Forward 10s
                IconButton(
                    onClick = { onSeekDelta(10_000L) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GlassBlack60)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Forward 10s",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // ================= BOTTOM CONTROLLER BAR =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Time stamps & Scrubber Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentDisplay = if (isDraggingSlider) {
                        formatPosition(draggedPositionMs.toLong())
                    } else {
                        uiState.formattedCurrentPosition
                    }

                    Text(
                        text = currentDisplay,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    val sliderValue = if (isDraggingSlider) {
                        draggedPositionMs
                    } else {
                        uiState.currentPositionMs.toFloat()
                    }

                    Slider(
                        value = sliderValue.coerceIn(0f, uiState.durationMs.coerceAtLeast(1L).toFloat()),
                        onValueChange = {
                            isDraggingSlider = true
                            draggedPositionMs = it
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onSeekTo(draggedPositionMs.toLong())
                        },
                        valueRange = 0f..uiState.durationMs.coerceAtLeast(1L).toFloat(),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = GlassWhite20
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = uiState.formattedDuration,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Quick Tool Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock Toggle
                    IconButton(onClick = onToggleLock) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aspect Ratio Chip
                        ActionPill(
                            icon = Icons.Rounded.AspectRatio,
                            text = uiState.aspectRatioMode.title,
                            onClick = onAspectRatioClick
                        )

                        // Speed Chip
                        ActionPill(
                            icon = Icons.Rounded.Speed,
                            text = "${uiState.playbackSpeed}x",
                            onClick = onSpeedClick
                        )

                        // Audio Track Shortcut
                        IconButton(onClick = onAudioTrackClick) {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = "Audio Stream",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Subtitle Shortcut
                        IconButton(onClick = onSubtitleClick) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = "Subtitles",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecoderChip(
    mode: DecoderMode,
    onClick: () -> Unit
) {
    val isHw = mode == DecoderMode.HARDWARE
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHw) CyberEmerald.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f))
            .border(
                1.dp,
                if (isHw) CyberEmerald else ElectricCyan,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.label,
            color = if (isHw) CyberEmerald else ElectricCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBlack60)
            .border(1.dp, GlassWhite10, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatPosition(timeMs: Long): String {
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
