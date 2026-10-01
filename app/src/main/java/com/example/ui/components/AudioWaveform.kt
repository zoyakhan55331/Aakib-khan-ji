package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.live.VoiceState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlin.math.sin

@Composable
fun AudioWaveform(
    state: VoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f, // 2*PI
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barColors = when (state) {
        VoiceState.LISTENING -> listOf(NeonCyan, NeonViolet)
        VoiceState.SPEAKING -> listOf(NeonMagenta, NeonAmber)
        VoiceState.THINKING -> listOf(NeonAmber, NeonViolet)
        else -> listOf(NeonCyan.copy(alpha = 0.3f), NeonViolet.copy(alpha = 0.2f))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val barCount = 28
        val totalWidth = size.width
        val barWidth = (totalWidth / (barCount * 1.6f)).coerceIn(4f, 10f)
        val spacing = (totalWidth - (barCount * barWidth)) / (barCount - 1)
        val centerY = size.height / 2f
        val maxHeight = size.height * 0.85f

        val isActive = state == VoiceState.LISTENING || state == VoiceState.SPEAKING || state == VoiceState.THINKING

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount
            // Bell curve window so edges are smaller
            val window = sin(normalizedIndex * Math.PI).toFloat().coerceIn(0.15f, 1f)

            val wave = if (isActive) {
                val s1 = sin(phase + i * 0.35f)
                val s2 = sin(phase * 1.5f + i * 0.2f)
                ((s1 + s2) / 2f + 1f) / 2f // 0..1
            } else {
                0.08f
            }

            val dynamicAmp = if (isActive) (wave * 0.4f + amplitude * 0.8f * window).coerceIn(0.08f, 1f) else 0.06f
            val barHeight = (maxHeight * dynamicAmp).coerceAtLeast(6f)

            val x = i * (barWidth + spacing)
            val y = centerY - (barHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = barColors,
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
