package com.example.basekmp.ui.onboarding

import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.fo_img_onboarding_1
import ailearn.shared.generated.resources.fo_img_onboarding_2
import ailearn.shared.generated.resources.fo_img_onboarding_3
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun FirstOpenIntroArtwork(page: Int, modifier: Modifier) {
    val image = when (page.coerceIn(0, 2)) {
        0 -> Res.drawable.fo_img_onboarding_1
        1 -> Res.drawable.fo_img_onboarding_2
        else -> Res.drawable.fo_img_onboarding_3
    }
    Image(
        painter = painterResource(image),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
