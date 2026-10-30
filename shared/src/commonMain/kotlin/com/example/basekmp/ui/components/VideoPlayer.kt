package com.example.basekmp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface VideoPlayerController {
    val isPlaying: Boolean
    val isReady: Boolean
    val positionMs: Long
    val durationMs: Long
    val videoAspect: Float
    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun stopAt(ms: Long?)
    fun setSpeed(speed: Float)
}

@Composable
expect fun rememberVideoPlayer(
    assetPath: String,
    muted: Boolean = false,
    onCompletion: () -> Unit = {},
    onStop: () -> Unit = {}
): VideoPlayerController

@Composable
expect fun VideoSurface(
    controller: VideoPlayerController,
    modifier: Modifier = Modifier
)
