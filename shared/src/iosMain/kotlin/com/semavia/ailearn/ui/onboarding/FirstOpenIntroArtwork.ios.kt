package com.semavia.ailearn.ui.onboarding

import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.fo_img_onboarding_1
import ailearn.shared.generated.resources.fo_img_onboarding_2
import ailearn.shared.generated.resources.fo_img_onboarding_3
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.sin

@Composable
actual fun FirstOpenIntroArtwork(page: Int, isActive: Boolean, modifier: Modifier) {
    val image = when (page.coerceIn(0, 2)) {
        0 -> Res.drawable.fo_img_onboarding_1
        1 -> Res.drawable.fo_img_onboarding_2
        else -> Res.drawable.fo_img_onboarding_3
    }
    val reducedMotion = isReducedMotionEnabled()
    val floatProgress = remember(page) { Animatable(0f) }

    LaunchedEffect(page, isActive, reducedMotion) {
        if (!isActive || reducedMotion) {
            floatProgress.snapTo(0f)
            return@LaunchedEffect
        }
        floatProgress.snapTo(0f)
        while (true) {
            floatProgress.animateTo(1f, tween(1_800, easing = FastOutSlowInEasing))
            floatProgress.animateTo(0f, tween(1_800, easing = FastOutSlowInEasing))
            delay(250)
        }
    }

    Image(
        painter = painterResource(image),
        contentDescription = null,
        modifier = modifier.graphicsLayer {
            val floatOffset = sin(floatProgress.value * PI).toFloat()
            translationY = -floatOffset * 8.dp.toPx()
            scaleX = 0.995f + floatOffset * 0.005f
            scaleY = 0.995f + floatOffset * 0.005f
        },
        contentScale = ContentScale.Fit
    )
}
