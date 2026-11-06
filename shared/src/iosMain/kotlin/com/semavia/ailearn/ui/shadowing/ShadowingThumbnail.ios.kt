package com.semavia.ailearn.ui.shadowing

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun ShadowingThumbnail(
    remoteUrl: String,
    cacheKey: String,
    fallback: DrawableResource?,
    contentDescription: String?,
    modifier: Modifier
) {
    if (fallback != null) {
        Image(
            painter = painterResource(fallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}
