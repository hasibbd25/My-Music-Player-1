package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SunsetGold
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerMode {
    BARS,
    WAVEFORM,
    VINYL
}

@Composable
fun EqualizerBarsVisualizer(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = amplitudes.size.coerceAtLeast(1)
        val spacing = 6.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)
        val maxHeight = size.height

        val gradient = Brush.verticalGradient(
            colors = listOf(
                NeonCyan,
                ElectricPurple,
                HotMagenta
            ),
            startY = 0f,
            endY = maxHeight
        )

        amplitudes.forEachIndexed { i, amp ->
            val effectiveAmp = if (isPlaying) amp else 0.08f
            val barHeight = (effectiveAmp * maxHeight).coerceIn(8f, maxHeight)
            val left = i * (barWidth + spacing)
            val top = maxHeight - barHeight

            // Draw Bar
            drawRoundRect(
                brush = gradient,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Draw Peak Cap
            val capTop = (top - 5f).coerceAtLeast(0f)
            drawRoundRect(
                color = SunsetGold,
                topLeft = Offset(left, capTop),
                size = Size(barWidth, 3.dp.toPx()),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2f * Math.PI.toFloat()) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        val path1 = Path()
        val path2 = Path()

        val points = 80
        val step = width / points

        for (i in 0..points) {
            val x = i * step
            val normalizedX = (i.toFloat() / points)
            // Center amplitude envelope
            val envelope = sin(normalizedX * Math.PI.toFloat())

            val amp1 = if (isPlaying) (height * 0.38f * envelope) else (height * 0.05f * envelope)
            val y1 = midY + sin(normalizedX * 4f * Math.PI.toFloat() + phase) * amp1

            val amp2 = if (isPlaying) (height * 0.28f * envelope) else (height * 0.03f * envelope)
            val y2 = midY + cos(normalizedX * 5f * Math.PI.toFloat() - phase * 1.2f) * amp2

            if (i == 0) {
                path1.moveTo(x, y1)
                path2.moveTo(x, y2)
            } else {
                path1.lineTo(x, y1)
                path2.lineTo(x, y2)
            }
        }

        drawPath(
            path = path1,
            color = NeonCyan,
            style = Stroke(width = 3.dp.toPx())
        )
        drawPath(
            path = path2,
            color = HotMagenta.copy(alpha = 0.8f),
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

@Composable
fun SpinningVinylVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Vinyl Outer Record
        Box(
            modifier = Modifier
                .size(240.dp)
                .rotate(currentRotation)
                .clip(CircleShape)
                .background(Color(0xFF111218))
                .border(6.dp, Color(0xFF1E212E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Grooves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radii = listOf(35.dp, 55.dp, 75.dp, 95.dp, 105.dp)
                radii.forEach { r ->
                    drawCircle(
                        color = Color(0xFF262A3C).copy(alpha = 0.45f),
                        radius = r.toPx(),
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }

            // Center Record Label
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricPurple, NeonCyan, HotMagenta)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0B10))
                )
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp).padding(bottom = 12.dp)
                )
            }
        }
    }
}
