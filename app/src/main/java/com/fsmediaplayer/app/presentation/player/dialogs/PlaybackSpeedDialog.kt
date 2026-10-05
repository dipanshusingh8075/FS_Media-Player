package com.fsmediaplayer.app.presentation.player.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
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

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = {
            Text(
                text = "Playback Speed",
                color = TextPrimary,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(speeds) { speed ->
                    val isSelected = (currentSpeed == speed)
                    val label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceContainerHigh else SurfaceContainer)
                            .clickable {
                                onSelectSpeed(speed)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectSpeed(speed)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ElectricCyan,
                                unselectedColor = TextSecondary
                            )
                        )
                        Text(
                            text = label,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = ElectricCyan
                            )
                        }
                    }
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
