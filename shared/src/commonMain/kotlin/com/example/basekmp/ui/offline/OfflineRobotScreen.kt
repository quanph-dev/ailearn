package com.example.basekmp.ui.offline

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.data.Strings
import com.example.basekmp.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class RobotState {
    IDLE,
    POKED,
    TICKLED,
    SPINNING,
    HOPPING,
    SQUEAKING,
    SLEEPING,
    NODDING
}

@Composable
fun OfflineRobotScreen(
    onRetryConnection: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val scope = rememberCoroutineScope()

    var robotState by remember { mutableStateOf(RobotState.IDLE) }
    var speechTextOverride by remember { mutableStateOf<String?>(null) }
    val speechText = speechTextOverride ?: Strings.tr("I'm offline! Touch me to play!")
    val tooManyPokesText = Strings.tr("Hey! That's too much poking!")
    val pokedText = Strings.tr("Ouch! You poked me!")
    val tickledText = Strings.tr("Hehehe! Stop, that tickles!")
    val dizzyText = Strings.tr("Wheee! I'm getting dizzy!")
    val hoppingText = Strings.tr("Boing! Up we go!")
    val squeakText = Strings.tr("*Squeeeak!* Beep boop!")
    val awakeText = Strings.tr("Yawn! I'm wide awake now!")
    val nodText = Strings.tr("Yes! Exactly right!")
    var pokeCount by remember { mutableIntStateOf(0) }

    // Animations for touch actions
    val hopY = remember { Animatable(0f) }
    val spinAngle = remember { Animatable(0f) }
    val nodAngle = remember { Animatable(0f) }
    val squishScale = remember { Animatable(1f) }
    val wobbleX = remember { Animatable(0f) }

    // Idle breathing float animation
    val infiniteTransition = rememberInfiniteTransition()
    val breatheFloat by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Touch Action Handlers:
    // 1. Poke
    fun onPoke() {
        pokeCount++
        scope.launch {
            robotState = RobotState.POKED
            speechTextOverride = if (pokeCount >= 4) tooManyPokesText else pokedText
            squishScale.animateTo(0.85f, tween(80))
            squishScale.animateTo(1.1f, tween(100))
            squishScale.animateTo(1f, spring(0.4f, 200f))
            delay(1200)
            if (robotState == RobotState.POKED) robotState = RobotState.IDLE
        }
    }

    // 2. Tickle
    fun onTickle() {
        scope.launch {
            robotState = RobotState.TICKLED
            speechTextOverride = tickledText
            repeat(3) {
                wobbleX.animateTo(12f, tween(60))
                wobbleX.animateTo(-12f, tween(60))
            }
            wobbleX.animateTo(0f, spring(0.5f, 300f))
            delay(1000)
            if (robotState == RobotState.TICKLED) robotState = RobotState.IDLE
        }
    }

    // 3. Spin
    fun onSpin() {
        scope.launch {
            robotState = RobotState.SPINNING
            speechTextOverride = dizzyText
            spinAngle.snapTo(0f)
            spinAngle.animateTo(360f, tween(600, easing = FastOutSlowInEasing))
            delay(800)
            if (robotState == RobotState.SPINNING) robotState = RobotState.IDLE
        }
    }

    // 4. Hop
    fun onHop() {
        scope.launch {
            robotState = RobotState.HOPPING
            speechTextOverride = hoppingText
            hopY.animateTo(-60f, tween(250, easing = FastOutSlowInEasing))
            hopY.animateTo(0f, spring(0.6f, 300f))
            delay(600)
            if (robotState == RobotState.HOPPING) robotState = RobotState.IDLE
        }
    }

    // 5. Squeak
    fun onSqueak() {
        scope.launch {
            robotState = RobotState.SQUEAKING
            speechTextOverride = squeakText
            squishScale.animateTo(1.15f, tween(100))
            squishScale.animateTo(1f, spring(0.5f, 250f))
            delay(1000)
            if (robotState == RobotState.SQUEAKING) robotState = RobotState.IDLE
        }
    }

    // 6. Wake
    fun onWake() {
        scope.launch {
            robotState = RobotState.IDLE
            speechTextOverride = awakeText
            hopY.animateTo(-20f, tween(150))
            hopY.animateTo(0f, spring(0.5f, 200f))
        }
    }

    // 7. Nod
    fun onNod() {
        scope.launch {
            robotState = RobotState.NODDING
            speechTextOverride = nodText
            repeat(2) {
                nodAngle.animateTo(15f, tween(120))
                nodAngle.animateTo(-10f, tween(120))
            }
            nodAngle.animateTo(0f, spring(0.6f, 200f))
            delay(800)
            if (robotState == RobotState.NODDING) robotState = RobotState.IDLE
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = dimens.spacingLg, vertical = dimens.spacingSm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top status row: Offline status pill + close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimens.spacingSm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.error.copy(alpha = 0.5f), RoundedCornerShape(dimens.cornerPill))
                    .padding(horizontal = dimens.spacingMd, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.error)
                )
                Spacer(Modifier.width(dimens.spacingSm))
                Text(
                    text = Strings.tr("Offline Mode"),
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = colors.textSecondary, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(dimens.spacingLg))

        // Speech Bubble
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(dimens.cornerLg))
                .background(colors.surface)
                .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(dimens.cornerLg))
                .padding(horizontal = dimens.spacingLg, vertical = dimens.spacingMd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = speechText,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(dimens.spacing2Xl))

        // Interactive Canvas Robot Stage
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset { IntOffset(wobbleX.value.toInt(), hopY.value.toInt()) }
                .graphicsLayer {
                    rotationZ = nodAngle.value
                    rotationY = spinAngle.value
                    scaleX = squishScale.value
                    scaleY = squishScale.value
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onPoke() },
                        onDoubleTap = { onHop() },
                        onLongPress = { onSqueak() }
                    )
                }
                .pointerInput(Unit) {
                    var totalDragX = 0f
                    detectDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            if (totalDragX > 100f || totalDragX < -100f) {
                                onSpin()
                                totalDragX = 0f
                            } else if (dragAmount.x > 8f || dragAmount.x < -8f) {
                                onTickle()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height

                // Robot dimensions
                val headW = cw * 0.65f
                val headH = ch * 0.46f
                val headLeft = (cw - headW) / 2f
                val headTop = (ch * 0.16f) + breatheFloat

                // Antenna stem & glowing bulb
                drawLine(
                    color = colors.primary,
                    start = Offset(cw / 2f, headTop),
                    end = Offset(cw / 2f, headTop - 22f),
                    strokeWidth = 5f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.primary, colors.primaryGradientStart),
                        center = Offset(cw / 2f, headTop - 26f),
                        radius = 12f
                    ),
                    radius = 9f,
                    center = Offset(cw / 2f, headTop - 26f)
                )

                // Head outer body
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF242C3F), Color(0xFF161C2A))
                    ),
                    topLeft = Offset(headLeft, headTop),
                    size = Size(headW, headH),
                    cornerRadius = CornerRadius(26f, 26f)
                )
                drawRoundRect(
                    color = colors.primary.copy(alpha = 0.5f),
                    topLeft = Offset(headLeft, headTop),
                    size = Size(headW, headH),
                    cornerRadius = CornerRadius(26f, 26f),
                    style = Stroke(width = 3.5f)
                )

                // Face visor screen
                val visorW = headW * 0.78f
                val visorH = headH * 0.52f
                val visorLeft = headLeft + (headW - visorW) / 2f
                val visorTop = headTop + (headH - visorH) / 2.3f

                drawRoundRect(
                    color = Color(0xFF0C101A),
                    topLeft = Offset(visorLeft, visorTop),
                    size = Size(visorW, visorH),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Eyes (changes based on state)
                val eyeY = visorTop + visorH / 2.2f
                val leftEyeX = visorLeft + visorW * 0.3f
                val rightEyeX = visorLeft + visorW * 0.7f

                when (robotState) {
                    RobotState.TICKLED -> {
                        // Laughing curved eyes ^^
                        drawLine(colors.primary, Offset(leftEyeX - 10f, eyeY + 4f), Offset(leftEyeX, eyeY - 6f), strokeWidth = 4f)
                        drawLine(colors.primary, Offset(leftEyeX, eyeY - 6f), Offset(leftEyeX + 10f, eyeY + 4f), strokeWidth = 4f)
                        drawLine(colors.primary, Offset(rightEyeX - 10f, eyeY + 4f), Offset(rightEyeX, eyeY - 6f), strokeWidth = 4f)
                        drawLine(colors.primary, Offset(rightEyeX, eyeY - 6f), Offset(rightEyeX + 10f, eyeY + 4f), strokeWidth = 4f)
                    }
                    RobotState.POKED -> {
                        // Surprised wide eyes
                        drawCircle(color = colors.primary, radius = 9f, center = Offset(leftEyeX, eyeY))
                        drawCircle(color = Color.White, radius = 3.5f, center = Offset(leftEyeX - 2f, eyeY - 2f))
                        drawCircle(color = colors.primary, radius = 9f, center = Offset(rightEyeX, eyeY))
                        drawCircle(color = Color.White, radius = 3.5f, center = Offset(rightEyeX - 2f, eyeY - 2f))
                    }
                    RobotState.SPINNING -> {
                        // Dizzy spiral / cross eyes
                        drawLine(colors.accentGold, Offset(leftEyeX - 8f, eyeY - 8f), Offset(leftEyeX + 8f, eyeY + 8f), strokeWidth = 4f)
                        drawLine(colors.accentGold, Offset(leftEyeX + 8f, eyeY - 8f), Offset(leftEyeX - 8f, eyeY + 8f), strokeWidth = 4f)
                        drawLine(colors.accentGold, Offset(rightEyeX - 8f, eyeY - 8f), Offset(rightEyeX + 8f, eyeY + 8f), strokeWidth = 4f)
                        drawLine(colors.accentGold, Offset(rightEyeX + 8f, eyeY - 8f), Offset(rightEyeX - 8f, eyeY + 8f), strokeWidth = 4f)
                    }
                    else -> {
                        // Regular glowing cyan eyes
                        drawCircle(color = colors.primary, radius = 8f, center = Offset(leftEyeX, eyeY))
                        drawCircle(color = Color.White, radius = 3f, center = Offset(leftEyeX - 2f, eyeY - 2f))
                        drawCircle(color = colors.primary, radius = 8f, center = Offset(rightEyeX, eyeY))
                        drawCircle(color = Color.White, radius = 3f, center = Offset(rightEyeX - 2f, eyeY - 2f))
                    }
                }

                // Robot body
                val bodyW = headW * 0.72f
                val bodyH = ch * 0.28f
                val bodyLeft = (cw - bodyW) / 2f
                val bodyTop = headTop + headH + 8f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF1E2638), Color(0xFF141926))
                    ),
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = colors.primary.copy(alpha = 0.35f),
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = 2.5f)
                )

                // Chest meter light
                drawCircle(
                    color = if (robotState == RobotState.IDLE) colors.success else colors.accentPink,
                    radius = 7f,
                    center = Offset(cw / 2f, bodyTop + bodyH * 0.45f)
                )
            }
        }

        Spacer(Modifier.height(dimens.spacingXl))

        // Quick touch action triggers row (Poke, Tickle, Spin, Hop, Squeak, Wake, Nod)
        Text(
            text = Strings.tr("Actions"),
            fontSize = 12.sp,
            color = colors.textSecondary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(dimens.spacingSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ActionButton(label = Strings.tr("Poke"), icon = "👉", onClick = { onPoke() })
            ActionButton(label = Strings.tr("Tickle"), icon = "🪶", onClick = { onTickle() })
            ActionButton(label = Strings.tr("Spin"), icon = "🔄", onClick = { onSpin() })
            ActionButton(label = Strings.tr("Hop"), icon = "🦘", onClick = { onHop() })
            ActionButton(label = Strings.tr("Nod"), icon = "👍", onClick = { onNod() })
        }

        Spacer(Modifier.weight(1f))

        // Reconnect CTA Button
        PrimaryButton(
            text = Strings.tr("Try Reconnecting"),
            onClick = onRetryConnection
        )

        Spacer(Modifier.height(dimens.spacingLg))
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(dimens.cornerSm))
            .clickable(onClick = onClick)
            .padding(dimens.spacingXs)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}
