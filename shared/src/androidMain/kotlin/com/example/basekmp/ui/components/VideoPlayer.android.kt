package com.example.basekmp.ui.components

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

class AndroidVideoPlayerController(
    val context: Context,
    val assetPath: String,
    val muted: Boolean,
    val onCompletionCallback: () -> Unit,
    val onStopCallback: () -> Unit
) : VideoPlayerController {
    var player: MediaPlayer? = null
    var surface: Surface? = null
    val _isPlaying = mutableStateOf(false)
    val _isReady = mutableStateOf(false)
    val _positionMs = mutableLongStateOf(0L)
    val _durationMs = mutableLongStateOf(0L)
    val _videoAspect = mutableFloatStateOf(16f / 9f)
    var stopAtMs: Long? = null
    private var currentSpeed: Float = 1.0f
    private var pendingSeekMs: Long? = null
    private var pendingPlay: Boolean = false

    override val isPlaying: Boolean get() = _isPlaying.value
    override val isReady: Boolean get() = _isReady.value
    override val positionMs: Long get() = _positionMs.value
    override val durationMs: Long get() = _durationMs.value
    override val videoAspect: Float get() = _videoAspect.value

    fun attach(st: SurfaceTexture) {
        val s = Surface(st)
        surface = s
        build(s)
    }

    private fun build(s: Surface) {
        try {
            val mp = MediaPlayer()
            mp.setSurface(s)
            if (muted) {
                mp.setVolume(0f, 0f)
            }

            if (assetPath.startsWith("/")) {
                mp.setDataSource(assetPath)
            } else {
                var afd: AssetFileDescriptor? = null
                try {
                    afd = context.assets.openFd(assetPath)
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                } catch (e: Exception) {
                    // Try alternative prefix or strip leading slash
                    val cleanPath = assetPath.trimStart('/')
                    afd = context.assets.openFd(cleanPath)
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                } finally {
                    afd?.close()
                }
            }

            mp.setOnVideoSizeChangedListener { _, width, height ->
                if (width > 0 && height > 0) {
                    _videoAspect.value = width.toFloat() / height.toFloat()
                }
            }
            mp.setOnPreparedListener { preparedMp ->
                _durationMs.value = preparedMp.duration.toLong()
                _isReady.value = true
                pendingSeekMs?.let { seekMs ->
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            preparedMp.seekTo(seekMs, MediaPlayer.SEEK_CLOSEST)
                        } else {
                            preparedMp.seekTo(seekMs.toInt())
                        }
                    } catch (_: Throwable) {}
                    pendingSeekMs = null
                }
                if (pendingPlay) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                            preparedMp.playbackParams = PlaybackParams().setSpeed(currentSpeed)
                        }
                        preparedMp.start()
                        _isPlaying.value = true
                    } catch (_: Throwable) {}
                    pendingPlay = false
                }
            }
            mp.setOnCompletionListener {
                _isPlaying.value = false
                stopAtMs = null
                onCompletionCallback()
            }
            mp.setOnErrorListener { _, _, _ ->
                _isPlaying.value = false
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (e: Throwable) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    override fun play() {
        val p = player
        if (p != null && _isReady.value) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                    p.playbackParams = PlaybackParams().setSpeed(currentSpeed)
                }
                p.start()
                _isPlaying.value = true
                pendingPlay = false
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        } else {
            pendingPlay = true
        }
    }

    override fun pause() {
        pendingPlay = false
        try {
            player?.let {
                if (it.isPlaying) it.pause()
            }
        } catch (_: Throwable) {}
        _isPlaying.value = false
    }

    override fun seekTo(ms: Long) {
        val safeMs = ms.coerceAtLeast(0L)
        _positionMs.value = safeMs
        val p = player
        if (p != null && _isReady.value) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    p.seekTo(safeMs, MediaPlayer.SEEK_CLOSEST)
                } else {
                    p.seekTo(safeMs.toInt())
                }
            } catch (e: Throwable) {}
        } else {
            pendingSeekMs = safeMs
        }
    }

    override fun stopAt(ms: Long?) {
        stopAtMs = ms
    }

    override fun setSpeed(newSpeed: Float) {
        currentSpeed = newSpeed
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                player?.playbackParams = PlaybackParams().setSpeed(newSpeed)
            }
        } catch (_: Throwable) {}
    }

    fun release() {
        pendingPlay = false
        pendingSeekMs = null
        try { player?.stop() } catch (_: Throwable) {}
        try { player?.release() } catch (_: Throwable) {}
        try { surface?.release() } catch (_: Throwable) {}
        player = null
        surface = null
        _isPlaying.value = false
        _isReady.value = false
    }
}

@Composable
actual fun rememberVideoPlayer(
    assetPath: String,
    muted: Boolean,
    onCompletion: () -> Unit,
    onStop: () -> Unit
): VideoPlayerController {
    val context = LocalContext.current
    val currentOnCompletion = rememberUpdatedState(onCompletion)
    val currentOnStop = rememberUpdatedState(onStop)

    val controller = remember(assetPath) {
        AndroidVideoPlayerController(
            context = context.applicationContext,
            assetPath = assetPath,
            muted = muted,
            onCompletionCallback = { currentOnCompletion.value() },
            onStopCallback = { currentOnStop.value() }
        )
    }

    LaunchedEffect(controller.isPlaying) {
        while (controller.isPlaying) {
            delay(50)
            val p = controller.player
            if (p != null && p.isPlaying) {
                val pos = p.currentPosition.toLong()
                controller._positionMs.value = pos
                val stopAt = controller.stopAtMs
                if (stopAt != null && pos >= stopAt) {
                    controller.pause()
                    controller.stopAt(null)
                    controller.onStopCallback()
                }
            }
        }
    }

    DisposableEffect(assetPath) {
        onDispose {
            controller.release()
        }
    }

    return controller
}

@Composable
actual fun VideoSurface(
    controller: VideoPlayerController,
    modifier: Modifier
) {
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        (controller as? AndroidVideoPlayerController)?.attach(st)
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        (controller as? AndroidVideoPlayerController)?.release()
                        return true
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}
