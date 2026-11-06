package com.semavia.ailearn.ui.shadowing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource

@Composable
expect fun ShadowingThumbnail(
    remoteUrl: String,
    cacheKey: String,
    fallback: DrawableResource?,
    contentDescription: String?,
    modifier: Modifier
)
