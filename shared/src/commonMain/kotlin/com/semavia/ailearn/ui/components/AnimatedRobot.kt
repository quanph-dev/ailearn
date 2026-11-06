package com.semavia.ailearn.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.semavia.ailearn.components.RobotMood
import com.semavia.ailearn.theme.BrandPrimary
import com.semavia.ailearn.theme.BrandPrimarySoft
import org.jetbrains.compose.resources.imageResource
import ailearn.shared.generated.resources.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class Box(val l: Float, val t: Float, val w: Float, val h: Float)

const val RobotFrameAspect: Float = 1.0608696f

private const val ART_L = 0.040983606f
private const val ART_T = 0.01594203f
private const val ART_W = 0.8989071f
private const val ART_H = 0.98405796f

private val IRIS_LEFT = Box(0.418f, 0.2667f, 0.0492f, 0.029f)
private val IRIS_RIGHT = Box(0.5546f, 0.2667f, 0.0492f, 0.029f)
private val BLOOM_LEFT = Box(0.396f, 0.256f, 0.079f, 0.041f)
private val BLOOM_RIGHT = Box(0.538f, 0.256f, 0.079f, 0.041f)
private val LID_LEFT = Box(0.4126f, 0.2594f, 0.0628f, 0.0348f)
private val LID_RIGHT = Box(0.5492f, 0.2594f, 0.0628f, 0.0348f)
private val MOUTH_GAP = Box(0.4658f, 0.4116f, 0.0874f, 0.0261f)
private val JAW = Box(0.4317f, 0.4217f, 0.1557f, 0.0565f)
private val PATCH = Box(0.1571f, 0.4754f, 0.2049f, 0.0667f)

private const val DOT_H = 0.0377f
private const val DOT_W = 0.0355f
private const val DOT_TOP = 0.4884f
private const val DOT_STAGGER = 0.13f
private val DOT_LEFTS = listOf(0.1817f, 0.2398f, 0.3006f)

private const val LED_STAGGER = 0.08f
private val LEDS = listOf(
    0.335f to 0.742f,
    0.306f to 0.784f,
    0.292f to 0.842f
)

private val HALO = listOf(BrandPrimary.copy(alpha = 0.35f), Color.Transparent)
private val BLUSH = listOf(Color(2583662517L), Color(16748469))
private val GLARE = listOf(Color(3875487808L), Color(2164203552L), Color(16711680))
private val BLOOM = listOf(BrandPrimarySoft.copy(alpha = 0.85f), BrandPrimarySoft.copy(alpha = 0.35f), BrandPrimary.copy(alpha = 0f))
private val BUBBLE_GLOW = listOf(BrandPrimarySoft.copy(alpha = 0.55f), BrandPrimary.copy(alpha = 0f))
private val DOT = listOf(Color.White, BrandPrimarySoft.copy(alpha = 0.9f))
private val SPARK = listOf(BrandPrimarySoft.copy(alpha = 0.95f), BrandPrimarySoft.copy(alpha = 0.5f), BrandPrimary.copy(alpha = 0f))
private val FLASH = listOf(BrandPrimarySoft.copy(alpha = 0.9f), BrandPrimarySoft.copy(alpha = 0.45f), BrandPrimary.copy(alpha = 0f))
private val PING_COLOR = BrandPrimarySoft.copy(alpha = 0.85f)
private val LED = listOf(BrandPrimarySoft.copy(alpha = 0.9f), BrandPrimary.copy(alpha = 0f))
private val SCAN = listOf(BrandPrimary.copy(alpha = 0f), BrandPrimarySoft.copy(alpha = 0.9f), BrandPrimary.copy(alpha = 0f))
private val SOCKET = Color(4278719526L)

private val GAZE_EASING = CubicBezierEasing(0.4f, 1.5f, 0.5f, 1.0f)

private val LID = listOf(
    0.0f to 0.0f,
    0.055f to 0.0f,
    0.07f to 1.0f,
    0.084f to 1.0f,
    0.1f to 0.0f,
    0.11f to 0.0f,
    0.155f to 0.34f,
    0.3f to 0.34f,
    0.34f to 0.0f,
    0.38f to 0.0f,
    0.392f to 1.0f,
    0.404f to 0.0f,
    0.42f to 0.0f,
    0.432f to 1.0f,
    0.446f to 0.0f,
    0.71f to 0.0f,
    0.722f to 0.55f,
    0.734f to 0.0f,
    0.76f to 0.0f,
    0.79f to 0.2f,
    0.86f to 0.2f,
    0.89f to 0.0f,
    1.0f to 0.0f
)

private val GAZE_X = listOf(
    0.0f to 0.0f,
    0.11f to 0.0f,
    0.155f to -0.04f,
    0.3f to -0.04f,
    0.34f to 0.0f,
    0.76f to 0.0f,
    0.79f to -0.02f,
    0.86f to -0.02f,
    0.89f to 0.0f,
    1.0f to 0.0f
)

private val GAZE_Y = listOf(
    0.0f to 0.0f,
    0.11f to 0.0f,
    0.155f to 0.22f,
    0.3f to 0.22f,
    0.34f to 0.0f,
    0.76f to 0.0f,
    0.79f to DOT_STAGGER,
    0.86f to DOT_STAGGER,
    0.89f to 0.0f,
    1.0f to 0.0f
)

private val GLOW_DIP = listOf(
    0.0f to 1.0f,
    0.055f to 1.0f,
    0.07f to 0.0f,
    0.084f to 0.0f,
    0.1f to 1.0f,
    0.38f to 1.0f,
    0.392f to 0.0f,
    0.404f to 1.0f,
    0.42f to 1.0f,
    0.432f to 0.0f,
    0.446f to 1.0f,
    0.71f to 1.0f,
    0.722f to 0.35f,
    0.734f to 1.0f,
    1.0f to 1.0f
)

private val TALK_Y = listOf(
    0.0f to 0.0f,
    0.05f to 0.055f,
    0.11f to 0.01f,
    0.16f to 0.085f,
    0.21f to 0.02f,
    0.26f to 0.06f,
    0.31f to 0.005f,
    0.38f to 0.075f,
    0.44f to 0.03f,
    0.5f to 0.0f,
    0.57f to 0.05f,
    0.62f to 0.015f,
    0.68f to 0.08f,
    0.74f to 0.025f,
    0.79f to 0.045f,
    0.84f to 0.008f,
    0.9f to 0.0f,
    1.0f to 0.0f
)

private val TALK_SCALE = listOf(
    0.0f to 1.0f,
    0.05f to 1.012f,
    0.11f to 1.002f,
    0.16f to 1.02f,
    0.21f to 1.004f,
    0.26f to 1.014f,
    0.31f to 1.0f,
    0.38f to 1.018f,
    0.44f to 1.006f,
    0.5f to 1.0f,
    0.57f to 1.012f,
    0.62f to 1.003f,
    0.68f to 1.019f,
    0.74f to 1.005f,
    0.79f to 1.01f,
    0.84f to 1.001f,
    0.9f to 1.0f,
    1.0f to 1.0f
)

private val DOT_RISE = listOf(
    0.0f to 0.0f,
    0.14f to -0.13f,
    0.34f to 0.0f,
    0.58f to 0.0f,
    1.0f to 0.0f
)

private val DOT_SCALE = listOf(
    0.0f to 0.82f,
    0.14f to 1.0f,
    0.34f to 0.9f,
    0.58f to 0.82f,
    1.0f to 0.82f
)

private val DOT_ALPHA = listOf(
    0.0f to 0.5f,
    0.14f to 1.0f,
    0.34f to 0.72f,
    0.58f to 0.5f,
    1.0f to 0.5f
)

private val SPARK_X = listOf(
    0.0f to 0.0f,
    0.62f to 1.0f,
    1.0f to 1.0f
)

private val SPARK_SCALE = listOf(
    0.0f to 0.5f,
    0.62f to 1.05f,
    0.74f to 0.4f,
    1.0f to 0.4f
)

private val SPARK_ALPHA = listOf(
    0.0f to 0.0f,
    0.12f to 0.95f,
    0.62f to 0.9f,
    0.74f to 0.0f,
    1.0f to 0.0f
)

private val FLASH_SCALE = listOf(
    0.0f to 0.9f,
    0.52f to 0.9f,
    0.66f to 1.22f,
    0.86f to 0.9f,
    1.0f to 0.9f
)

private val FLASH_ALPHA = listOf(
    0.0f to 0.55f,
    0.52f to 0.55f,
    0.66f to 1.0f,
    0.86f to 0.55f,
    1.0f to 0.55f
)

private val PING_SCALE = listOf(
    0.0f to 0.35f,
    0.6f to 0.35f,
    1.0f to 2.6f
)

private val PING_ALPHA = listOf(
    0.0f to 0.0f,
    0.6f to 0.0f,
    0.68f to 0.7f,
    1.0f to 0.0f
)

private val LED_ALPHA = listOf(
    0.0f to 0.15f,
    0.72f to 0.15f,
    0.8f to 0.9f,
    1.0f to 0.15f
)

private val BUBBLE_PULSE = listOf(
    0.0f to 0.32f,
    0.5f to 0.6f,
    1.0f to 0.32f
)

private fun normalizeMood(mood: RobotMood): RobotMood {
    return when (mood) {
        RobotMood.SUPER -> RobotMood.EXCITED
        RobotMood.GOOD -> RobotMood.HAPPY
        RobotMood.AVERAGE -> RobotMood.NEUTRAL
        RobotMood.POOR -> RobotMood.SAD
        RobotMood.WORST -> RobotMood.ANGRY
        else -> mood
    }
}

private fun track(p: Float, list: List<Pair<Float, Float>>, easing: Easing = LinearEasing): Float {
    if (p <= list.first().first) return list.first().second
    for (i in 0 until list.size - 1) {
        val (k1, v1) = list[i]
        val (k2, v2) = list[i + 1]
        if (p <= k2) {
            return if (k2 <= k1) v2 else v1 + ((v2 - v1) * easing.transform(((p - k1) / (k2 - k1)).coerceIn(0f, 1f)))
        }
    }
    return list.last().second
}

private fun wave(f: Float): Float = 0.5f - (cos(f * 6.2831855f) * 0.5f)

private fun dizzySpiral(center: Offset, radius: Float, turns: Float, startAngle: Float): Path {
    val path = Path()
    val steps = 48
    for (i in 0..steps) {
        val f = i.toFloat() / steps
        val angle = (f * turns * 2f * 3.1415927f) + startAngle
        val x = center.x + cos(angle) * radius * f
        val y = center.y + sin(angle) * radius * f
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    return path
}

private fun DrawScope.glow(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    colors: List<Color>,
    alpha: Float,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    if (alpha <= 0.004f || rx <= 0f || ry <= 0f) return
    val center = Offset(cx, cy)
    val scaleY = ry / rx
    val drawContext = drawContext
    val s = drawContext.size
    drawContext.canvas.save()
    try {
        drawContext.transform.scale(1f, scaleY, center)
        drawCircle(
            brush = Brush.radialGradient(colors, center, rx),
            radius = rx,
            center = center,
            alpha = alpha.coerceIn(0f, 1f),
            blendMode = blendMode
        )
    } finally {
        drawContext.canvas.restore()
        drawContext.size = s
    }
}

private fun DrawScope.drawPart(
    image: ImageBitmap,
    l: Float,
    t: Float,
    w: Float,
    h: Float,
    dx: Float = 0f,
    dy: Float = 0f
) {
    val widthPx = (size.width * w).roundToInt()
    val heightPx = (size.height * h).roundToInt()
    if (widthPx <= 0 || heightPx <= 0) return
    drawImage(
        image = image,
        dstOffset = IntOffset(
            ((size.width * l) + dx).roundToInt(),
            ((size.height * t) + dy).roundToInt()
        ),
        dstSize = IntSize(widthPx, heightPx),
        filterQuality = FilterQuality.High
    )
}

private fun DrawScope.drawEye(
    image: ImageBitmap,
    box: Box,
    gazeX: Float,
    gazeY: Float
) {
    val rect = Rect(
        box.l * size.width,
        box.t * size.height,
        (box.l + box.w) * size.width,
        (box.t + box.h) * size.height
    )
    val path = Path().apply { addOval(rect) }
    val drawContext = drawContext
    val s = drawContext.size
    drawContext.canvas.save()
    try {
        drawContext.transform.clipPath(path, ClipOp.Intersect)
        drawOval(SOCKET, rect.topLeft, rect.size)
        drawPart(
            image = image,
            l = box.l,
            t = box.t,
            w = box.w,
            h = box.h,
            dx = box.w * gazeX * size.width,
            dy = box.h * gazeY * size.height
        )
    } finally {
        drawContext.canvas.restore()
        drawContext.size = s
    }
}

@Composable
fun AnimatedRobot(
    modifier: Modifier = Modifier,
    talking: Boolean = false,
    showFingerFx: Boolean = false,
    mood: RobotMood = RobotMood.NEUTRAL
) {
    val activeMood = normalizeMood(mood)

    val lidBiasTarget = when (activeMood) {
        RobotMood.HAPPY -> 0.32f
        RobotMood.LAUGHING -> 0.4f
        RobotMood.CELEBRATING -> 0.34f
        RobotMood.SHY -> 0.3f
        RobotMood.BORED -> 0.62f
        RobotMood.MISCHIEVOUS -> 0.5f
        RobotMood.CONFUSED -> 0.15f
        RobotMood.SAD -> 0.45f
        RobotMood.ANGRY -> 0.42f
        RobotMood.THINKING -> 0.12f
        RobotMood.SLEEPY, RobotMood.DIZZY -> 1.0f
        else -> 0.0f
    }
    val lidBias by animateFloatAsState(lidBiasTarget, label = "lidBias")

    val gazeXTarget = when (activeMood) {
        RobotMood.SHY, RobotMood.LISTENING -> -0.12f
        RobotMood.THINKING -> 0.1f
        RobotMood.CURIOUS -> 0.12f
        else -> 0.0f
    }
    val gazeX by animateFloatAsState(gazeXTarget, label = "gazeX")

    val gazeYTarget = when (activeMood) {
        RobotMood.SHY, RobotMood.SAD, RobotMood.SLEEPY -> 0.22f
        RobotMood.BORED -> 0.18f
        RobotMood.CURIOUS -> -0.2f
        RobotMood.THINKING -> -0.22f
        else -> 0.0f
    }
    val gazeY by animateFloatAsState(gazeYTarget, label = "gazeY")

    val bloomTarget = when (activeMood) {
        RobotMood.HAPPY, RobotMood.LAUGHING, RobotMood.SURPRISED, RobotMood.WINKING -> 1.35f
        RobotMood.CELEBRATING, RobotMood.EXCITED -> 1.6f
        RobotMood.SHY, RobotMood.LISTENING -> 1.1f
        RobotMood.BORED -> 0.5f
        RobotMood.MISCHIEVOUS, RobotMood.CURIOUS -> 1.2f
        RobotMood.SAD -> 0.55f
        RobotMood.SLEEPY -> 0.08f
        RobotMood.DIZZY -> 0.3f
        else -> 1.0f
    }
    val bloom by animateFloatAsState(bloomTarget, label = "bloom")

    val blushTarget = when (activeMood) {
        RobotMood.HAPPY, RobotMood.LAUGHING, RobotMood.CELEBRATING, RobotMood.EXCITED, RobotMood.WINKING -> 1.0f
        RobotMood.SHY -> 1.4f
        else -> 0.0f
    }
    val blush by animateFloatAsState(blushTarget, label = "blush")

    val glareTarget = when (activeMood) {
        RobotMood.MISCHIEVOUS -> 0.55f
        RobotMood.ANGRY -> 1.0f
        else -> 0.0f
    }
    val glare by animateFloatAsState(glareTarget, label = "glare")

    val wink by animateFloatAsState(if (activeMood == RobotMood.WINKING) 1.0f else 0.0f, label = "wink")
    val stars by animateFloatAsState(if (activeMood == RobotMood.EXCITED || activeMood == RobotMood.CELEBRATING) 1.0f else 0.0f, label = "stars")
    val dizzy by animateFloatAsState(if (activeMood == RobotMood.DIZZY) 1.0f else 0.0f, label = "dizzy")

    val imgRobot = imageResource(Res.drawable.tutor_hero_robot)
    val imgIrisL = imageResource(Res.drawable.robot_iris_left)
    val imgIrisR = imageResource(Res.drawable.robot_iris_right)
    val imgLidL = imageResource(Res.drawable.robot_lid_left)
    val imgLidR = imageResource(Res.drawable.robot_lid_right)
    val imgMouth = imageResource(Res.drawable.robot_mouth_gap)
    val imgJaw = imageResource(Res.drawable.robot_jaw)
    val imgPatch = imageResource(Res.drawable.robot_bubble_patch)

    val transition = rememberInfiniteTransition(label = "robot")
    val cycle by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "cycle")
    val typing by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "typing")
    val speech by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2900, easing = LinearEasing)), label = "speech")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "pulse")
    val sweep by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(4500, easing = LinearEasing)), label = "sweep")
    val breathe by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "breathe")

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val breatheY = -0.0057971017f * h * wave(breathe)
        drawContext.transform.translate(0f, breatheY)

        try {
            glow(w * 0.5f, 0.26f * h, 0.23f * w, 0.06f * h, HALO, 1.0f, BlendMode.SrcOver)
            drawPart(imgRobot, ART_L, ART_T, ART_W, ART_H)
            val confusedWiggle = if (activeMood == RobotMood.CONFUSED) {
                (sin(sweep * 2f * 3.1415927f * 3f)) * 0.14f
            } else 0f

            val curGazeX = track(cycle, GAZE_X, GAZE_EASING) + gazeX + confusedWiggle
            val curGazeY = track(cycle, GAZE_Y, GAZE_EASING) + gazeY

            drawEye(imgIrisL, IRIS_LEFT, curGazeX, curGazeY)
            drawEye(imgIrisR, IRIS_RIGHT, curGazeX, curGazeY)

            val glowDipFactor = track(cycle, GLOW_DIP)
            val bloomGain = ((wave(pulse) * 0.28f) + 0.72f) * glowDipFactor * bloom

            for (box in listOf(BLOOM_LEFT, BLOOM_RIGHT)) {
                glow(
                    cx = (box.l + (box.w / 2f) + (box.w * curGazeX)) * w,
                    cy = (box.t + (box.h / 2f) + (box.h * curGazeY)) * h,
                    rx = (box.w / 2f) * w,
                    ry = (box.h / 2f) * h,
                    colors = BLOOM,
                    alpha = bloomGain,
                    blendMode = BlendMode.Screen
                )
            }

            if (glare > 0.01f) {
                for (box in listOf(BLOOM_LEFT, BLOOM_RIGHT)) {
                    glow(
                        cx = (box.l + (box.w / 2f)) * w,
                        cy = (box.t + (box.h / 2f)) * h,
                        rx = (box.w / 2f) * w,
                        ry = (box.h / 2f) * h,
                        colors = GLARE,
                        alpha = glare * glowDipFactor,
                        blendMode = BlendMode.Screen
                    )
                }
            }

            val curLid = max(track(cycle, LID), lidBias)
            val curLidR = max(curLid, wink)

            if (curLid > 0.002f) {
                drawPart(imgLidL, LID_LEFT.l, LID_LEFT.t, LID_LEFT.w, LID_LEFT.h * curLid)
            }
            if (curLidR > 0.002f) {
                drawPart(imgLidR, LID_RIGHT.l, LID_RIGHT.t, LID_RIGHT.w, LID_RIGHT.h * curLidR)
            }

            if (stars > 0.01f) {
                for (box in listOf(BLOOM_LEFT, BLOOM_RIGHT)) {
                    val center = Offset((box.l + (box.w / 2f)) * w, (box.t + (box.h / 2f)) * h)
                    val starH = box.h * h * 0.95f * ((sin(sweep * 2f * 3.1415927f * 6f) * 0.1f) + 0.9f) * stars
                    val p = Path().apply {
                        moveTo(center.x, center.y - starH)
                        quadraticTo(center.x, center.y, center.x + starH, center.y)
                        quadraticTo(center.x, center.y, center.x, center.y + starH)
                        quadraticTo(center.x, center.y, center.x - starH, center.y)
                        quadraticTo(center.x, center.y, center.x, center.y - starH)
                        close()
                    }
                    drawPath(p, Color(4286572287L).copy(alpha = stars * 0.5f), style = Stroke(starH * 0.5f, cap = StrokeCap.Round))
                    drawPath(p, Color.White.copy(alpha = stars))
                }
            }

            if (dizzy > 0.01f) {
                var idx = 0
                for (box in listOf(BLOOM_LEFT, BLOOM_RIGHT)) {
                    val center = Offset((box.l + (box.w / 2f)) * w, (box.t + (box.h / 2f)) * h)
                    val radius = min(box.w * w, box.h * h) * 0.6f
                    val sign = if (idx == 0) 1f else -1f
                    val spiralPath = dizzySpiral(center, radius, 2.2f, sweep * 2f * 3.1415927f * 4f * sign)
                    drawPath(spiralPath, Color(4286572287L).copy(alpha = dizzy * 0.4f), style = Stroke(0.45f * radius, cap = StrokeCap.Round))
                    drawPath(spiralPath, Color.White.copy(alpha = dizzy), style = Stroke(0.2f * radius, cap = StrokeCap.Round))
                    idx++
                }
            }

            if (blush > 0.01f) {
                for (xNorm in listOf(0.43f, 0.585f)) {
                    glow(xNorm * w, 0.335f * h, 0.035f * w, 0.014f * h, BLUSH, blush * 0.75f)
                }
            }

            drawPart(imgMouth, MOUTH_GAP.l, MOUTH_GAP.t, MOUTH_GAP.w, MOUTH_GAP.h)

            val talkY = if (talking) track(speech, TALK_Y) else 0f
            val talkScale = if (talking) track(speech, TALK_SCALE) else 1f

            drawPart(
                image = imgJaw,
                l = JAW.l,
                t = JAW.t,
                w = JAW.w,
                h = JAW.h * talkScale,
                dx = 0f,
                dy = talkY * JAW.h * h
            )

            glow(
                cx = 0.24f * w,
                cy = 0.48f * h,
                rx = 0.15f * w,
                ry = 0.12f * h,
                colors = BUBBLE_GLOW,
                alpha = track(typing, BUBBLE_PULSE, FastOutSlowInEasing),
                blendMode = BlendMode.Screen
            )

            drawPart(imgPatch, PATCH.l, PATCH.t, PATCH.w, PATCH.h)

            var dotIdx = 0
            for (dotLeft in DOT_LEFTS) {
                val p = (typing + (dotIdx * DOT_STAGGER)) % 1f
                val dotR = w * 0.01775f * track(p, DOT_SCALE, FastOutSlowInEasing)
                val dotCx = (dotLeft + 0.01775f) * w
                val dotCyBase = 0.50725f * h
                val dotCy = dotCyBase + (track(p, DOT_RISE, FastOutSlowInEasing) * DOT_H * h)
                drawCircle(
                    brush = Brush.radialGradient(DOT, Offset(dotCx, dotCyBase), dotR),
                    radius = dotR,
                    center = Offset(dotCx, dotCy),
                    alpha = track(p, DOT_ALPHA, FastOutSlowInEasing)
                )
                dotIdx++
            }

            if (showFingerFx) {
                val sparkOx = 0.3279f * w
                val sparkOy = 0.7761f * h
                val sparkPivot = Offset(sparkOx, sparkOy)
                val dc = drawContext
                val sz = dc.size
                dc.canvas.save()
                try {
                    dc.transform.rotate(-41.7f, sparkPivot)
                    val sparkLen = 0.1189f * w * 0.14f
                    val halfLen = sparkLen / 2f
                    val sparkCx = sparkOx + halfLen + (track(typing, SPARK_X, FastOutSlowInEasing) * sparkLen)
                    glow(
                        cx = sparkCx,
                        cy = sparkOy,
                        rx = halfLen * track(typing, SPARK_SCALE, FastOutSlowInEasing),
                        ry = 0.008f * h * track(typing, SPARK_SCALE, FastOutSlowInEasing),
                        colors = SPARK,
                        alpha = track(typing, SPARK_ALPHA, FastOutSlowInEasing),
                        blendMode = BlendMode.Screen
                    )
                } finally {
                    dc.canvas.restore()
                    dc.size = sz
                }

                val flashScaleVal = track(typing, FLASH_SCALE, FastOutSlowInEasing)
                val flashCx = 0.417f * w
                val flashCy = 0.685f * h
                glow(
                    cx = flashCx,
                    cy = flashCy,
                    rx = w * 0.033f * flashScaleVal,
                    ry = h * 0.033f * flashScaleVal,
                    colors = FLASH,
                    alpha = track(typing, FLASH_ALPHA, FastOutSlowInEasing),
                    blendMode = BlendMode.Screen
                )

                drawCircle(
                    color = PING_COLOR,
                    radius = 0.021f * w * track(typing, PING_SCALE),
                    center = Offset(flashCx, flashCy),
                    alpha = track(typing, PING_ALPHA),
                    style = Stroke(h * 0.002173913f),
                    blendMode = BlendMode.Screen
                )

                var ledIdx = 0
                for (ledPair in LEDS) {
                    glow(
                        cx = (ledPair.first + 0.011f) * w,
                        cy = (ledPair.second + 0.011f) * h,
                        rx = w * 0.011f,
                        ry = 0.011f * h,
                        colors = LED,
                        alpha = track((typing + (ledIdx * 0.08f)) % 1f, LED_ALPHA),
                        blendMode = BlendMode.Screen
                    )
                    ledIdx++
                }
            }

            // Visor Scan Line
            val scanL = 0.38f * w
            val scanT = 0.245f * h
            val scanR = 0.62f * w
            val scanB = 0.31f * h
            val dc2 = drawContext
            val sz2 = dc2.size
            dc2.canvas.save()
            try {
                dc2.transform.clipRect(scanL, scanT, scanR, scanB, ClipOp.Intersect)
                val scanLineH = h * 0.002173913f
                drawRect(
                    brush = Brush.horizontalGradient(SCAN, scanL, scanR),
                    topLeft = Offset(scanL, (scanT - scanLineH) + (sweep * 15f * scanLineH)),
                    size = Size(w * 0.24f, scanLineH),
                    alpha = 0.5f,
                    blendMode = BlendMode.Screen
                )
            } finally {
                dc2.canvas.restore()
                dc2.size = sz2
            }
        } finally {
            drawContext.transform.translate(0f, -breatheY)
        }
    }
}
