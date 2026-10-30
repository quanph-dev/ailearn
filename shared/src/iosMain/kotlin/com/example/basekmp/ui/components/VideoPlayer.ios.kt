package com.example.basekmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class IosVideoPlayerController : VideoPlayerController {
    override val isPlaying: Boolean get() = false
    override val isReady: Boolean get() = true
    override val positionMs: Long get() = 0L
    override val durationMs: Long get() = 0L
    override val videoAspect: Float get() = 16f / 9f

    override fun play() {}
    override fun pause() {}
    override fun seekTo(ms: Long) {}
    override fun stopAt(ms: Long?) {}
    override fun setSpeed(speed: Float) {}
}

@Composable
actual fun rememberVideoPlayer(
    assetPath: String,
    cacheKey: String?,
    muted: Boolean,
    onCompletion: () -> Unit,
    onStop: () -> Unit
): VideoPlayerController {
    return remember(assetPath) { IosVideoPlayerController() }
}

@Composable
actual fun VideoSurface(
    controller: VideoPlayerController,
    modifier: Modifier
) {
    Box(modifier = modifier.background(Color.Black))
}
