package com.fsmediaplayer.app.presentation.player.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsmediaplayer.app.core.designsystem.theme.ElectricCyan
import com.fsmediaplayer.app.core.designsystem.theme.SurfaceContainer
import com.fsmediaplayer.app.core.designsystem.theme.SurfaceContainerHigh
import com.fsmediaplayer.app.core.designsystem.theme.TextPrimary
import com.fsmediaplayer.app.core.designsystem.theme.TextSecondary
import com.fsmediaplayer.app.core.model.SubtitleTrackItem

@Composable
fun SubtitleTrackDialog(
    tracks: List<SubtitleTrackItem>,
    onSelectTrack: (groupIndex: Int, trackIndex: Int) -> Unit,
    onPickExternalSubtitle: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAnySelected = tracks.any { it.isSelected }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = {
            Text(
                text = "Subtitles & Captions",
                color = TextPrimary,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Option: OFF
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isAnySelected) SurfaceContainerHigh else SurfaceContainer)
                                .clickable {
                                    onSelectTrack(-1, -1) // Disable subtitles
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !isAnySelected,
                                onClick = {
                                    onSelectTrack(-1, -1)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ElectricCyan,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Text(
                                text = "Off (Disabled)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    items(tracks) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (track.isSelected) SurfaceContainerHigh else SurfaceContainer)
                                .clickable {
                                    onSelectTrack(track.groupIndex, track.trackIndex)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = track.isSelected,
                                onClick = {
                                    onSelectTrack(track.groupIndex, track.trackIndex)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ElectricCyan,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = track.label,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                track.language?.let { lang ->
                                    Text(
                                        text = "Language: $lang",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (track.isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Active",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = {
                        onPickExternalSubtitle()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = ElectricCyan
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load External File (.srt / .vtt)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ElectricCyan)
            }
        }
    )
}
