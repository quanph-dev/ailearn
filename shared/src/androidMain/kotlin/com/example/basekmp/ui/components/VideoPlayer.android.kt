package com.example.basekmp.ui.components

import android.content.Context
import android.net.Uri
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.delay
import java.io.File

private object ShadowingMediaCache {
    @Volatile
    private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: SimpleCache(
                File(context.filesDir, "shadowing_remote/clips"),
                LeastRecentlyUsedCacheEvictor(250L * 1024L * 1024L),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { cache = it }
        }
    }
}

class AndroidExoVideoPlayerController(
    context: Context,
    assetPath: String,
    cacheKey: String?,
    muted: Boolean,
    private val onCompletionCallback: () -> Unit,
    val onStopCallback: () -> Unit
) : VideoPlayerController {
    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                CacheDataSource.Factory()
                    .setCache(ShadowingMediaCache.get(context.applicationContext))
                    .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context.applicationContext))
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            )
        )
        .build()

    private val _isPlaying = mutableStateOf(false)
    private val _isReady = mutableStateOf(false)
    private val _positionMs = mutableLongStateOf(0L)
    private val _durationMs = mutableLongStateOf(0L)
    private val _videoAspect = mutableFloatStateOf(16f / 9f)
    private var textureView: TextureView? = null

    var stopAtMs: Long? = null
        private set

    override val isPlaying: Boolean get() = _isPlaying.value
    override val isReady: Boolean get() = _isReady.value
    override val positionMs: Long get() = _positionMs.value
    override val durationMs: Long get() = _durationMs.value
    override val videoAspect: Float get() = _videoAspect.value

    init {
        player.volume = if (muted) 0f else 1f
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true
        )
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isReady.value = playbackState == Player.STATE_READY
                if (playbackState == Player.STATE_READY) {
                    updatePlaybackPosition()
                    player.duration
                        .takeIf { it != C.TIME_UNSET && it >= 0L }
                        ?.let { _durationMs.value = it }
                } else if (playbackState == Player.STATE_ENDED) {
                    _positionMs.value = _durationMs.value
                    stopAtMs = null
                    onCompletionCallback()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    _videoAspect.value =
                        videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _isPlaying.value = false
                _isReady.value = false
            }
        })

        val mediaItem = MediaItem.Builder()
            .setUri(mediaUri(assetPath))
            .apply { if (!cacheKey.isNullOrBlank()) setCustomCacheKey(cacheKey) }
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        updatePlaybackPosition()
        player.pause()
    }

    override fun seekTo(ms: Long) {
        val safePositionMs = ms.coerceAtLeast(0L)
        _positionMs.value = safePositionMs
        player.seekTo(safePositionMs)
    }

    override fun stopAt(ms: Long?) {
        stopAtMs = ms
    }

    override fun setSpeed(speed: Float) {
        if (speed > 0f) {
            player.playbackParameters = PlaybackParameters(speed)
        }
    }

    fun attach(view: TextureView) {
        if (textureView === view) return
        textureView?.let(player::clearVideoTextureView)
        textureView = view
        player.setVideoTextureView(view)
    }

    fun detach(view: TextureView) {
        if (textureView !== view) return
        player.clearVideoTextureView(view)
        textureView = null
    }

    fun updatePlaybackPosition() {
        _positionMs.value = player.currentPosition.coerceAtLeast(0L)
    }

    fun release() {
        textureView?.let(player::clearVideoTextureView)
        textureView = null
        stopAtMs = null
        player.release()
        _isPlaying.value = false
        _isReady.value = false
    }

    private fun mediaUri(path: String): Uri = when {
        path.startsWith("asset://") || path.startsWith("file://") ||
            path.startsWith("content://") || path.startsWith("http://") ||
            path.startsWith("https://") -> Uri.parse(path)
        path.startsWith("/") -> Uri.fromFile(File(path))
        else -> Uri.parse("asset:///${path.trimStart('/')}")
    }
}

@Composable
actual fun rememberVideoPlayer(
    assetPath: String,
    cacheKey: String?,
    muted: Boolean,
    onCompletion: () -> Unit,
    onStop: () -> Unit
): VideoPlayerController {
    val context = LocalContext.current
    val currentOnCompletion = rememberUpdatedState(onCompletion)
    val currentOnStop = rememberUpdatedState(onStop)

    val controller = remember(assetPath, cacheKey, muted) {
        AndroidExoVideoPlayerController(
            context = context.applicationContext,
            assetPath = assetPath,
            cacheKey = cacheKey,
            muted = muted,
            onCompletionCallback = { currentOnCompletion.value() },
            onStopCallback = { currentOnStop.value() }
        )
    }

    LaunchedEffect(controller.isPlaying) {
        while (controller.isPlaying) {
            delay(50)
            controller.updatePlaybackPosition()
            val stopAt = controller.stopAtMs
            if (stopAt != null && controller.positionMs >= stopAt) {
                controller.pause()
                controller.stopAt(null)
                controller.onStopCallback()
            }
        }
    }

    DisposableEffect(controller) {
        onDispose(controller::release)
    }

    return controller
}

@Composable
actual fun VideoSurface(
    controller: VideoPlayerController,
    modifier: Modifier
) {
    val exoController = controller as? AndroidExoVideoPlayerController
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                isOpaque = false
                exoController?.attach(this)
            }
        },
        update = { view -> exoController?.attach(view) },
        onRelease = { view -> exoController?.detach(view) },
        modifier = modifier
    )
}
