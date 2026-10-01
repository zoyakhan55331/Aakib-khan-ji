package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CuteCyan
import com.example.ui.theme.CutePink
import com.example.ui.theme.CutePinkSoft
import com.example.ui.theme.CutePurple
import kotlin.random.Random

private data class Particle(
    val initialX: Float, // 0..1
    val initialY: Float, // 0..1
    val speed: Float,
    val size: Float,
    val color: Color
)

@Composable
fun CuteFloatingParticles(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_progress"
    )

    val particles = remember {
        val colors = listOf(CutePink, CuteCyan, CutePurple, CutePinkSoft)
        List(18) {
            Particle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.7f,
                size = 2f + Random.nextFloat() * 3.5f,
                color = colors[it % colors.size]
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (p in particles) {
            val yOffset = ((p.initialY - (progress * p.speed)) % 1f + 1f) % 1f
            val x = p.initialX * width
            val y = yOffset * height
            val alpha = (1f - (y / height)).coerceIn(0.1f, 0.6f)

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * density,
                center = Offset(x, y)
            )
        }
    }
}
