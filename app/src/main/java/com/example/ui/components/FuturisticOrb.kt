package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.live.VoiceState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanLight
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FuturisticOrb(
    state: VoiceState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_pulse"
    )

    // Dynamic scale based on live audio amplitude
    val targetScale = when (state) {
        VoiceState.LISTENING -> (1f + amplitude * 0.45f).coerceIn(1f, 1.45f)
        VoiceState.SPEAKING -> (breathingPulse + amplitude * 0.35f).coerceIn(0.95f, 1.4f)
        VoiceState.THINKING -> 1.05f
        VoiceState.DISCONNECTED -> 0.92f
        else -> breathingPulse
    }

    val animatedScale = remember { Animatable(1f) }
    LaunchedEffect(targetScale) {
        animatedScale.animateTo(targetScale, tween(120))
    }

    // Colors depending on assistant state
    val (coreColors, glowColor, ringColor) = when (state) {
        VoiceState.LISTENING -> Triple(
            listOf(NeonCyanLight, NeonCyan, Color(0xFF0072FF)),
            NeonCyan.copy(alpha = 0.6f + amplitude * 0.3f),
            NeonCyan
        )
        VoiceState.SPEAKING -> Triple(
            listOf(NeonRose, NeonMagenta, NeonViolet),
            NeonMagenta.copy(alpha = 0.65f + amplitude * 0.3f),
            NeonMagenta
        )
        VoiceState.THINKING -> Triple(
            listOf(NeonAmber, NeonRose, NeonViolet),
            NeonAmber.copy(alpha = 0.7f),
            NeonAmber
        )
        VoiceState.CONNECTING -> Triple(
            listOf(NeonPurple, NeonCyan, NeonViolet),
            NeonPurple.copy(alpha = 0.5f),
            NeonPurple
        )
        VoiceState.INTERRUPTED -> Triple(
            listOf(NeonAmber, NeonCyan, NeonPurple),
            NeonAmber.copy(alpha = 0.7f),
            NeonAmber
        )
        VoiceState.ERROR -> Triple(
            listOf(Color(0xFFFF3366), Color(0xFFE60000), Color(0xFF7A0000)),
            Color(0xFFFF3366).copy(alpha = 0.6f),
            Color(0xFFFF3366)
        )
        else -> Triple(
            listOf(NeonViolet, NeonPurple, Color(0xFF0B1933)),
            NeonViolet.copy(alpha = 0.35f),
            NeonCyan.copy(alpha = 0.5f)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            )
            .testTag("ai_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.65f * animatedScale.value

            // 1. Ambient outer diffuse glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        glowColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                radius = baseRadius * 1.55f,
                center = center
            )

            // 2. Ripple pulse ring when listening or speaking
            if (state == VoiceState.LISTENING || state == VoiceState.SPEAKING) {
                val rippleRadius = baseRadius * (1f + wavePulse * 0.5f)
                val rippleAlpha = (1f - wavePulse).coerceIn(0f, 1f) * 0.8f
                drawCircle(
                    color = ringColor.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // 3. Rotating energy rings (chromatic orbit)
            val ringRadius = baseRadius * 1.15f
            for (i in 0..2) {
                val angleRad = Math.toRadians((rotationAngle + i * 120.0)).toFloat()
                val offset = Offset(
                    center.x + ringRadius * 0.15f * cos(angleRad),
                    center.y + ringRadius * 0.15f * sin(angleRad)
                )
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.8f),
                            Color.Transparent,
                            ringColor.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        center = offset
                    ),
                    radius = ringRadius,
                    center = offset,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // 4. Counter rotating decorative dash ring
            drawCircle(
                color = coreColors.first().copy(alpha = 0.45f),
                radius = baseRadius * 1.05f,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 25f), counterRotationAngle)
                )
            )

            // 5. Solid Core Glowing Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreColors[0],
                        coreColors[1],
                        coreColors.getOrElse(2) { Color.Black }
                    ),
                    center = Offset(center.x - baseRadius * 0.22f, center.y - baseRadius * 0.25f),
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // 6. Center specular glossy highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.35f, center.y - baseRadius * 0.4f),
                    radius = baseRadius * 0.45f
                ),
                radius = baseRadius * 0.45f,
                center = Offset(center.x - baseRadius * 0.35f, center.y - baseRadius * 0.4f)
            )
        }
    }
}
