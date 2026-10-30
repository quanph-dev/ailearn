package com.example.basekmp.ui.onboarding

import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.fo_img_onboarding_1
import ailearn.shared.generated.resources.fo_img_onboarding_2
import ailearn.shared.generated.resources.fo_img_onboarding_3
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.basekmp.shared.R

@Composable
actual fun FirstOpenIntroArtwork(page: Int, modifier: Modifier) {
    val pageIndex = page.coerceIn(0, 2)
    val animationRes = when (pageIndex) {
        0 -> R.raw.fo_anim_onboarding_1
        1 -> R.raw.fo_anim_onboarding_2
        else -> R.raw.fo_anim_onboarding_3
    }
    val fallbackImage = when (pageIndex) {
        0 -> Res.drawable.fo_img_onboarding_1
        1 -> Res.drawable.fo_img_onboarding_2
        else -> Res.drawable.fo_img_onboarding_3
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))

    Box(modifier = modifier) {
        if (composition == null) {
            Image(
                painter = org.jetbrains.compose.resources.painterResource(fallbackImage),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
