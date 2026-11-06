package com.semavia.ailearn.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.semavia.ailearn.theme.NeonCyan
import com.semavia.ailearn.theme.RoyalBlue
import com.semavia.ailearn.theme.SurfaceContainer
import kotlinx.coroutines.delay

enum class RobotMood {
    SUPER,
    GOOD,
    AVERAGE,
    POOR,
    WORST,
    NEUTRAL,
    HAPPY,
    SAD,
    ANGRY,
    THINKING,
    SURPRISED,
    SLEEPY,
    DIZZY,
    CRYING,
    LOVE,
    SCARED,
    LAUGHING,
    CURIOUS,
    CONFUSED,
    EXCITED,
    SHY,
    BORED,
    MISCHIEVOUS,
    LISTENING,
    WINKING,
    CELEBRATING
}

@Composable
fun AnimatedBuddy(
    mood: RobotMood = RobotMood.GOOD,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val infiniteTransition = rememberInfiniteTransition()
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val blinkAnim = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            blinkAnim.animateTo(0.1f, tween(100))
            blinkAnim.animateTo(1f, tween(100))
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasW = this.size.width
            val canvasH = this.size.height

            // Head outer body
            val headW = canvasW * 0.78f
            val headH = canvasH * 0.65f
            val headLeft = (canvasW - headW) / 2f
            val headTop = (canvasH - headH) / 2f + floatOffset

            // Antenna
            drawLine(
                color = NeonCyan,
                start = Offset(canvasW / 2f, headTop),
                end = Offset(canvasW / 2f, headTop - 14f),
                strokeWidth = 4f
            )
            drawCircle(
                color = RoyalBlue,
                radius = 6f,
                center = Offset(canvasW / 2f, headTop - 16f)
            )

            // Head rounded rect
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(SurfaceContainer, Color(0xFF161C28))),
                topLeft = Offset(headLeft, headTop),
                size = Size(headW, headH),
                cornerRadius = CornerRadius(24f, 24f)
            )
            drawRoundRect(
                color = NeonCyan.copy(alpha = 0.5f),
                topLeft = Offset(headLeft, headTop),
                size = Size(headW, headH),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 3f)
            )

            // Visor screen
            val visorW = headW * 0.82f
            val visorH = headH * 0.45f
            val visorLeft = headLeft + (headW - visorW) / 2f
            val visorTop = headTop + 18f
            drawRoundRect(
                color = Color(0xFF090D14),
                topLeft = Offset(visorLeft, visorTop),
                size = Size(visorW, visorH),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Eyes
            val eyeRadius = 10f
            val eyeY = visorTop + visorH / 2f
            val eyeScale = blinkAnim.value

            val eyeColor = when (mood) {
                RobotMood.SUPER -> NeonCyan
                RobotMood.GOOD -> Color(0xFF00E676)
                RobotMood.AVERAGE -> Color(0xFFFFD600)
                RobotMood.POOR -> Color(0xFFFF9100)
                RobotMood.WORST -> Color(0xFFFF5252)
                else -> Color(0xFF00E676)
            }

            // Left Eye
            drawOval(
                color = eyeColor,
                topLeft = Offset(visorLeft + visorW * 0.28f - eyeRadius, eyeY - eyeRadius * eyeScale),
                size = Size(eyeRadius * 2, eyeRadius * 2 * eyeScale)
            )

            // Right Eye
            drawOval(
                color = eyeColor,
                topLeft = Offset(visorLeft + visorW * 0.72f - eyeRadius, eyeY - eyeRadius * eyeScale),
                size = Size(eyeRadius * 2, eyeRadius * 2 * eyeScale)
            )

            // Mouth
            val mouthY = headTop + headH * 0.78f
            when (mood) {
                RobotMood.SUPER, RobotMood.GOOD -> {
                    drawArc(
                        color = NeonCyan,
                        startAngle = 10f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(canvasW / 2f - 18f, mouthY - 10f),
                        size = Size(36f, 20f),
                        style = Stroke(width = 3.5f)
                    )
                }
                RobotMood.AVERAGE -> {
                    drawLine(
                        color = Color(0xFFFFD600),
                        start = Offset(canvasW / 2f - 14f, mouthY),
                        end = Offset(canvasW / 2f + 14f, mouthY),
                        strokeWidth = 3.5f
                    )
                }
                RobotMood.POOR, RobotMood.WORST -> {
                    drawArc(
                        color = Color(0xFFFF5252),
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(canvasW / 2f - 16f, mouthY),
                        size = Size(32f, 16f),
                        style = Stroke(width = 3.5f)
                    )
                }
                else -> {
                    drawArc(
                        color = NeonCyan,
                        startAngle = 10f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(canvasW / 2f - 18f, mouthY - 10f),
                        size = Size(36f, 20f),
                        style = Stroke(width = 3.5f)
                    )
                }
            }
        }
    }
}
