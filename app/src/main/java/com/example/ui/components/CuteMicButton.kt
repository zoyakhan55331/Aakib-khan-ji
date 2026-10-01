package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.live.VoiceState
import com.example.ui.theme.CuteCyan
import com.example.ui.theme.CuteGlassBorder
import com.example.ui.theme.CuteGlassSurface
import com.example.ui.theme.CuteMicGradient
import com.example.ui.theme.CutePink
import com.example.ui.theme.CutePinkSoft
import com.example.ui.theme.CutePurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CuteMicButton(
    state: VoiceState,
    amplitude: Float,
    isSessionActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 86.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cute_mic_motion")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = size / 2),
                    onClick = onClick
                )
                .testTag("cute_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            // Audio-reactive ripple canvas
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = this.size.minDimension / 2f * 0.78f

                // Outer animated wave ripple when listening/speaking
                if (state == VoiceState.LISTENING || state == VoiceState.SPEAKING) {
                    val rippleR = baseRadius * (1f + wavePulse * 0.45f)
                    val rippleAlpha = (1f - wavePulse) * 0.7f
                    drawCircle(
                        color = (if (state == VoiceState.LISTENING) CuteCyan else CutePink).copy(alpha = rippleAlpha),
                        radius = rippleR,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isSessionActive) CutePink else CutePurple).copy(alpha = 0.5f + amplitude * 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.3f
                    ),
                    radius = baseRadius * 1.3f,
                    center = center
                )

                // Rotating neon gradient border
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = CuteMicGradient + CuteMicGradient.first(),
                        center = center
                    ),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = (3f + amplitude * 4f).dp.toPx())
                )
            }

            // Glass Inner Circle
            Box(
                modifier = Modifier
                    .size(size * 0.72f)
                    .clip(CircleShape)
                    .background(CuteGlassSurface)
                    .border(1.dp, CuteGlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        !isSessionActive -> Icons.Default.MicOff
                        state == VoiceState.SPEAKING -> Icons.Default.Stop
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Voice Session",
                    tint = if (isSessionActive) Color.White else TextSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Subtitle Text matching the artwork: "Tap & Talk to Mahi ♡"
        Text(
            text = when (state) {
                VoiceState.SPEAKING -> "Tap to interrupt Mahi ♡"
                VoiceState.LISTENING -> "Mahi is listening to you ♡"
                VoiceState.THINKING -> "Mahi is thinking ♡"
                else -> "Tap & Talk to Mahi ♡"
            },
            color = CutePinkSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
