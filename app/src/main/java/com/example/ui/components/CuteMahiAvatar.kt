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
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.live.VoiceState
import com.example.ui.theme.CuteCyan
import com.example.ui.theme.CuteGlassBorder
import com.example.ui.theme.CuteGlassSurface
import com.example.ui.theme.CutePink
import com.example.ui.theme.CutePinkHot
import com.example.ui.theme.CutePinkSoft
import com.example.ui.theme.CutePurple
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CuteMahiAvatar(
    state: VoiceState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cute_avatar_motion")

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val targetAmpScale = when (state) {
        VoiceState.LISTENING -> (1f + amplitude * 0.25f).coerceIn(1f, 1.25f)
        VoiceState.SPEAKING -> (breathingScale + amplitude * 0.2f).coerceIn(0.98f, 1.2f)
        VoiceState.THINKING -> 1.03f
        else -> breathingScale
    }

    val animatedScale = remember { Animatable(1f) }
    LaunchedEffect(targetAmpScale) {
        animatedScale.animateTo(targetAmpScale, tween(120))
    }

    val activeColors = when (state) {
        VoiceState.LISTENING -> listOf(CuteCyan, CutePink, CutePurple)
        VoiceState.SPEAKING -> listOf(CutePinkHot, CutePurple, CuteCyan)
        VoiceState.THINKING -> listOf(NeonAmber, CutePink, CutePurple)
        VoiceState.CONNECTING -> listOf(CutePurple, CuteCyan, CutePink)
        VoiceState.ERROR -> listOf(NeonRose, Color.Red, CutePurple)
        else -> listOf(CutePinkSoft, CutePurple, CuteCyan)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar with animated neon glowing energy rings
        Box(
            modifier = Modifier
                .size(size)
                .scale(animatedScale.value)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = size / 2),
                    onClick = onClick
                )
                .testTag("cute_avatar_view"),
            contentAlignment = Alignment.Center
        ) {
            // Background Canvas: Neon glow & animated orbital energy rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = (this.size.minDimension / 2f) * 0.88f

                // Ambient halo glow behind avatar
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColors[0].copy(alpha = pulseAlpha * 0.6f + amplitude * 0.2f),
                            activeColors[1].copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.35f
                    ),
                    radius = radius * 1.35f,
                    center = center
                )

                // Outer rotating chromatic energy rings
                val numDots = 16
                for (i in 0 until numDots) {
                    val angle = Math.toRadians((ringRotation + (i * 360f / numDots)).toDouble())
                    val dotX = center.x + (radius * 1.05f) * cos(angle).toFloat()
                    val dotY = center.y + (radius * 1.05f) * sin(angle).toFloat()
                    val dotColor = if (i % 2 == 0) activeColors[0] else activeColors[1]
                    drawCircle(
                        color = dotColor.copy(alpha = 0.7f),
                        radius = (3f + amplitude * 4f).dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                // Dual-tone neon stroke ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            activeColors[0],
                            activeColors[1],
                            activeColors[2],
                            activeColors[0]
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = (2.5f + amplitude * 3f).dp.toPx())
                )
            }

            // Central Mahi Avatar Artwork
            Box(
                modifier = Modifier
                    .size(size * 0.82f)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(CutePink.copy(alpha = 0.8f), CuteCyan.copy(alpha = 0.8f))
                        ),
                        shape = CircleShape
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mahi_avatar),
                    contentDescription = "Mahi AI Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Status Badge Pill
        Box(
            modifier = Modifier
                .background(
                    color = CuteGlassSurface,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = CuteGlassBorder,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            val (statusText, statusSubtext, statusColor) = when (state) {
                VoiceState.LISTENING -> Triple("Listening...", "I'm listening ♡", CuteCyan)
                VoiceState.SPEAKING -> Triple("Speaking...", "Mahi is talking ♡", CutePink)
                VoiceState.THINKING -> Triple("Thinking...", "Just a moment...", NeonAmber)
                VoiceState.CONNECTING -> Triple("Connecting...", "Just a sec...", CutePurple)
                VoiceState.INTERRUPTED -> Triple("Interrupted", "Ready for you ♡", CutePinkSoft)
                VoiceState.ERROR -> Triple("Connection Issue", "Tap to retry", NeonRose)
                else -> Triple("Always Here For You ♡", "Ready to chat", CutePinkSoft)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Column {
                    Text(
                        text = statusText,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = statusSubtext,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
