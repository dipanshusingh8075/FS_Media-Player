package com.fsmediaplayer.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsmediaplayer.app.core.designsystem.theme.ElectricCyan
import com.fsmediaplayer.app.core.designsystem.theme.GlassBlack80
import com.fsmediaplayer.app.core.designsystem.theme.GlassWhite20
import com.fsmediaplayer.app.core.designsystem.theme.TextPrimary
import com.fsmediaplayer.app.core.designsystem.theme.TextSecondary

sealed class ActiveGestureHUD {
    data class Brightness(val progressFraction: Float) : ActiveGestureHUD()
    data class Volume(val current: Int, val max: Int) : ActiveGestureHUD()
    data class Seek(val targetMs: Long, val durationMs: Long, val deltaMs: Long) : ActiveGestureHUD()
}

@Composable
fun GestureHUDContainer(
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
                is ActiveGestureHUD.Brightness -> BrightnessHUDContent(fraction = activeHUD.progressFraction)
                is ActiveGestureHUD.Volume -> VolumeHUDContent(current = activeHUD.current, max = activeHUD.max)
                is ActiveGestureHUD.Seek -> SeekHUDContent(
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
private fun BrightnessHUDContent(fraction: Float) {
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
private fun VolumeHUDContent(current: Int, max: Int) {
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
private fun SeekHUDContent(targetMs: Long, durationMs: Long, deltaMs: Long) {
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
