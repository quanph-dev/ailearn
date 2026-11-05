package com.example.basekmp

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.SystemClock
import android.graphics.Rect
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.ads.pro.base.BannerAds
import com.google.ads.pro.application.AppOpenAdsManager
import com.example.basekmp.core.analytics.logFirebaseEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max

/** Activity-scoped bridge between shared Compose routes and the Android Prox SDK. */
class AndroidAppAdsHost(private val activity: Activity) : AppAdsHost {
    private val startupClockOrigin = SystemClock.elapsedRealtime()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _isInterstitialLoading = MutableStateFlow(false)
    override val isInterstitialLoading: StateFlow<Boolean> = _isInterstitialLoading.asStateFlow()
    private val _splashPhase = MutableStateFlow(1)
    override val splashPhase: StateFlow<Int> = _splashPhase.asStateFlow()
    private val _fullscreenNativeState = MutableStateFlow<FullscreenNativeState?>(null)
    override val fullscreenNativeState: StateFlow<FullscreenNativeState?> = _fullscreenNativeState.asStateFlow()
    private val _homeInteractionEpoch = MutableStateFlow(0)
    override val homeInteractionEpoch: StateFlow<Int> = _homeInteractionEpoch.asStateFlow()

    private var pendingStartupCandidate: String? = null
    private var startupHasStarted = false
    private var splashNativeFrame: FrameLayout? = null
    private var splashNativeFailed = false
    private var fullscreenClose: CompletableDeferred<Unit>? = null
    private var fullscreenBind: CompletableDeferred<Boolean>? = null
    private var fullscreenOperation: Job? = null
    private var loadingOperations = 0
    private var loadingFrameReady = CompletableDeferred<Unit>()
    private var fullscreenBusy = false
    private var wasBackgrounded = false
    private var pauseWasAds = false
    private var pauseEpoch = 0L
    private var resumePreparing = false
    private var resumePreparationJob: Job? = null
    private val lastSuccessfulShow = mutableMapOf<String, Long>()
    private var released = false

    override suspend fun showStartupAds(returningUser: Boolean) {
        if (fullscreenBusy || released) return
        fullscreenBusy = true
        try {
        val startedAt = if (!startupHasStarted) {
            startupHasStarted = true
            startupClockOrigin
        } else {
            SystemClock.elapsedRealtime()
        }
        val deadline = startedAt + STARTUP_DEADLINE_MS
        _splashPhase.value = 1

        val candidates = if (returningUser) {
            listOf("I_Splash_High", "I_Splash_Medium", "I_Splash_A")
        } else {
            listOf("I_Splash_High", "I_Splash_A")
        }
        val result = CompletableDeferred<String?>()
        var interReadyAt = Long.MAX_VALUE
        splashNativeFailed = false
        val preparation = scope.launch {
            if (!MyAdsUtils.initializeAfterConsent(activity, trackStartup = true)) {
                splashNativeFailed = true
                result.complete(null)
                return@launch
            }
            scope.launch {
                val loaded = MyAdsUtils.loadNative(
                    activity,
                    "N_Splash",
                    remaining(deadline).coerceAtLeast(1L),
                    trackSplashPreload = true
                )
                if (!loaded) splashNativeFailed = true
            }
            var selected: String? = null
            for (candidate in candidates) {
                if (remaining(deadline) <= 0L) break
                if (MyAdsUtils.loadInterstitial(activity, candidate, minOf(PLACEMENT_LOAD_TIMEOUT_MS, remaining(deadline)))) {
                    selected = candidate
                    interReadyAt = SystemClock.elapsedRealtime()
                    pendingStartupCandidate = candidate
                    break
                }
            }
            result.complete(selected)
        }

        val phaseOneEnd = minOf(startedAt + SPLASH_PHASE_ONE_TIMEOUT_MS, deadline)
        while (!released && SystemClock.elapsedRealtime() < phaseOneEnd) {
            if (MyAdsUtils.readyNative("N_Splash") != null) break
            if (splashNativeFailed && result.isCompleted) break
            if (result.isCompleted && result.await() == null) break
            delay(minOf(50L, phaseOneEnd - SystemClock.elapsedRealtime()))
        }
        _splashPhase.value = 2

        val exposure = SplashNativeExposure(phaseOneEnd)
        val visibilityTracker = scope.launch {
            val bounds = Rect()
            while (true) {
                val now = SystemClock.elapsedRealtime()
                val frame = splashNativeFrame
                val visible = activity.isResumedForAds() && activity.hasWindowFocus() &&
                    frame != null && frame.isShown && frame.width > 0 && frame.height > 0 &&
                    frame.getGlobalVisibleRect(bounds) && bounds.width() > 0 && bounds.height() > 0
                exposure.observe(now, visible, pauseEpoch)
                delay(50L)
            }
        }

        try {
        val timeLeft = remaining(deadline)
        if (timeLeft > 0L) {
            val candidate = withTimeoutOrNull(timeLeft) { result.await() }
            if (candidate == null) {
                val timedOut = !result.isCompleted || remaining(deadline) <= 0L
                if (timedOut) {
                    logFirebaseEvent("spl_timeout", android.os.Bundle().apply {
                        putInt("time_elapsed", (SystemClock.elapsedRealtime() - startedAt).toInt())
                    })
                }
                logFirebaseEvent("spl_next_${if (timedOut) "timeout" else "no_ads"}")
            } else if (activity.isResumedForAds() && remaining(deadline) > 0L) {
                if (interReadyAt < phaseOneEnd) {
                    while (!released && shouldWaitForSplashNative(SystemClock.elapsedRealtime(), interReadyAt,
                            phaseOneEnd, deadline, splashNativeFailed, exposure.visibleMs, exposure.startedBeforeCutoff)) {
                        delay(minOf(50L, remaining(deadline)))
                    }
                    Log.d("AndroidAppAdsHost", "N_Splash visible_ms=${exposure.visibleMs}")
                }
                if (!activity.isResumedForAds() || remaining(deadline) <= 0L) return
                beginLoading()
                try {
                    if (MyAdsUtils.showLoadedInterstitial(activity, candidate, remaining(deadline))) {
                        pendingStartupCandidate = null
                        logFirebaseEvent("spl_next_ads_closed")
                    } else {
                        logFirebaseEvent("spl_next_no_ads")
                    }
                } finally {
                    endLoading()
                }
            } else {
                val timedOut = remaining(deadline) <= 0L
                if (timedOut) {
                    logFirebaseEvent("spl_timeout", android.os.Bundle().apply {
                        putInt("time_elapsed", (SystemClock.elapsedRealtime() - startedAt).toInt())
                    })
                }
                logFirebaseEvent("spl_next_${if (timedOut) "timeout" else "no_ads"}")
            }
        } else {
            logFirebaseEvent("spl_timeout", android.os.Bundle().apply {
                putInt("time_elapsed", (SystemClock.elapsedRealtime() - startedAt).toInt())
            })
            logFirebaseEvent("spl_next_timeout")
        }
        if (!result.isCompleted && remaining(deadline) <= 0L) preparation.cancel()
        } finally {
            visibilityTracker.cancel()
            if (!result.isCompleted) preparation.cancel()
            splashNativeFrame = null
        }
        } finally {
            fullscreenBusy = false
        }
    }

    override suspend fun showPendingStartupFallback() {
        val candidate = pendingStartupCandidate ?: return
        if (!activity.isResumedForAds() || released || fullscreenBusy) return
        fullscreenBusy = true
        beginLoading()
        try {
            val ready = MyAdsUtils.isInterstitialLoaded(candidate) ||
                MyAdsUtils.loadInterstitial(activity, candidate, PLACEMENT_LOAD_TIMEOUT_MS)
            if (ready && activity.isResumedForAds() && MyAdsUtils.showLoadedInterstitial(activity, candidate, PLACEMENT_LOAD_TIMEOUT_MS)) {
                pendingStartupCandidate = null
            }
        } finally {
            endLoading()
            fullscreenBusy = false
        }
    }

    override suspend fun preloadNative(placement: String, fallbacks: List<String>): Boolean {
        if (!MyAdsUtils.initializeAfterConsent(activity)) return false
        val chain = listOf(placement) + fallbacks
        for (candidate in chain) {
            if (MyAdsUtils.readyNative(candidate) != null) return true
            if (MyAdsUtils.loadNative(activity, candidate)) return true
        }
        return false
    }

    override suspend fun showInterstitial(
        placement: String,
        timeoutMs: Long,
        minIntervalMs: Long
    ): Boolean {
        if (!activity.isResumedForAds() || released || fullscreenBusy) return false
        val lastSuccess = lastSuccessfulShow[placement]
            ?: MyAdsUtils.lastInterstitialShowAt(placement)
        if (minIntervalMs > 0L && lastSuccess != null && SystemClock.elapsedRealtime() - lastSuccess < minIntervalMs) {
            return false
        }
        fullscreenBusy = true
        beginLoading()
        val startedAt = SystemClock.elapsedRealtime()
        return try {
            val ready = if (MyAdsUtils.isInterstitialLoaded(placement)) {
                true
            } else {
                MyAdsUtils.initializeAfterConsent(activity) && MyAdsUtils.loadInterstitial(activity, placement, timeoutMs)
            }
            if (!ready || !activity.isResumedForAds()) return false
            val showBudget = (timeoutMs - (SystemClock.elapsedRealtime() - startedAt)).coerceAtLeast(1L)
            val shown = MyAdsUtils.showLoadedInterstitial(activity, placement, showBudget)
            if (shown) lastSuccessfulShow[placement] = MyAdsUtils.lastInterstitialShowAt(placement) ?: SystemClock.elapsedRealtime()
            shown
        } finally {
            endLoading()
            fullscreenBusy = false
        }
    }

    override suspend fun showFullscreenNative(placement: String, closeDelaySeconds: Int): Boolean {
        if (released || fullscreenBusy || _fullscreenNativeState.value != null) return false
        fullscreenBusy = true
        try {
        if (!MyAdsUtils.initializeAfterConsent(activity)) return false
        val ready = MyAdsUtils.readyNative(placement) != null || MyAdsUtils.loadNative(activity, placement)
        if (!ready || !activity.isResumedForAds()) return false

        val close = CompletableDeferred<Unit>()
        val bound = CompletableDeferred<Boolean>()
        fullscreenClose = close
        fullscreenBind = bound
        _fullscreenNativeState.value = FullscreenNativeState(placement, max(1, closeDelaySeconds), false)
        try {
            val isBound = withTimeoutOrNull(FULLSCREEN_BIND_TIMEOUT_MS) { bound.await() } == true
            if (!isBound) return false
            fullscreenOperation = scope.launch {
                for (seconds in max(1, closeDelaySeconds) downTo 1) {
                    _fullscreenNativeState.value = FullscreenNativeState(placement, seconds, false)
                    delay(1_000L)
                }
                _fullscreenNativeState.value = FullscreenNativeState(placement, 0, true)
            }
            close.await()
            return true
        } finally {
            fullscreenOperation?.cancel()
            fullscreenOperation = null
            fullscreenClose = null
            fullscreenBind = null
            _fullscreenNativeState.value = null
        }
        } finally {
            fullscreenBusy = false
        }
    }

    override fun onFullscreenNativeBound(placement: String, success: Boolean) {
        if (_fullscreenNativeState.value?.placement == placement) {
            fullscreenBind?.complete(success)
        }
    }

    override fun closeFullscreenNative() {
        if (_fullscreenNativeState.value?.canClose == true) fullscreenClose?.complete(Unit)
    }

    override fun notifyHomeInteraction() {
        _homeInteractionEpoch.value += 1
    }

    override fun onAppPaused(): Boolean {
        pauseEpoch++
        val sdkFullscreen = MyAdsUtils.isShowingSdkFullscreen() || _fullscreenNativeState.value != null || MyAdsUtils.isShowingConsentForm() || AppOpenAdsManager.isAdOtherClicked
        val backgroundDuringResumeLoad = resumePreparing && !sdkFullscreen
        pauseWasAds = sdkFullscreen || (!backgroundDuringResumeLoad && (fullscreenBusy || loadingOperations > 0))
        if (backgroundDuringResumeLoad) resumePreparationJob?.cancel()
        return pauseWasAds
    }

    override fun onAppBackgrounded() {
        if (!pauseWasAds && !AppOpenAdsManager.isAdOtherClicked) wasBackgrounded = true
    }

    override suspend fun onAppForegrounded(): Boolean {
        val fromAdClick = AppOpenAdsManager.isAdOtherClicked
        AppOpenAdsManager.isAdOtherClicked = false
        val shouldShowResume = wasBackgrounded && !pauseWasAds && !fromAdClick
        wasBackgrounded = false
        pauseWasAds = false
        if (!shouldShowResume || released || fullscreenBusy || loadingOperations > 0 || _fullscreenNativeState.value != null || MyAdsUtils.isShowingSdkFullscreen() || MyAdsUtils.isShowingConsentForm()) {
            Log.d("AndroidAppAdsHost", "I_Resume skipped: background=$shouldShowResume ad_click=$fromAdClick busy=$fullscreenBusy released=$released")
            return false
        }
        val epoch = pauseEpoch
        val deadline = SystemClock.elapsedRealtime() + 8_000L
        fullscreenBusy = true
        beginLoading()
        resumePreparing = true
        resumePreparationJob = currentCoroutineContext()[Job]
        var handedToSdk = false
        return try {
            val ready = withTimeoutOrNull(8_000L) {
                loadingFrameReady.await()
                MyAdsUtils.initializeAfterConsent(activity) && MyAdsUtils.loadInterstitial(activity, "I_Resume", remaining(deadline).coerceAtLeast(1L))
            } == true
            if (!ready || epoch != pauseEpoch || !activity.isResumedForAds() || remaining(deadline) <= 0L) {
                false
            } else {
                resumePreparing = false
                resumePreparationJob = null
                handedToSdk = true
                MyAdsUtils.showLoadedInterstitial(activity, "I_Resume", remaining(deadline))
            }
        } finally {
            resumePreparing = false
            resumePreparationJob = null
            if (!handedToSdk) MyAdsUtils.cancelInterstitial("I_Resume")
            endLoading()
            fullscreenBusy = false
        }
    }

    @Composable
    override fun NativeAd(
        placement: String,
        modifier: Modifier,
        fallbacks: List<String>,
        reuseOnly: Boolean,
        collapsible: Boolean,
        reloadKey: Int,
        reloadAfterIdleMs: Long?,
        onInteraction: () -> Unit,
        onCollapsed: () -> Unit
    ) {
        val candidates = remember(placement, fallbacks) { listOf(placement) + fallbacks }
        var frame by remember(placement, reloadKey) { mutableStateOf<FrameLayout?>(null) }
        var shownPlacement by remember(placement, reloadKey) { mutableStateOf<String?>(null) }
        var isVisible by remember(placement, reloadKey) { mutableStateOf(false) }
        var isCollapsed by remember(placement, reloadKey) { mutableStateOf(false) }
        var hasInteracted by remember(placement, reloadKey) { mutableStateOf(false) }
        var hasReloaded by remember(placement, reloadKey) { mutableStateOf(false) }
        val interactionEpoch = _homeInteractionEpoch.collectAsState().value
        var observedInteractionEpoch by remember(placement, reloadKey) { mutableIntStateOf(interactionEpoch) }
        val isFullscreen = placement.startsWith("N_FS_")
        val outerModifier = if (isFullscreen) {
            modifier.fillMaxSize()
        } else if (isVisible && !isCollapsed) {
            modifier.fillMaxWidth().wrapContentHeight()
        } else {
            Modifier.height(0.dp)
        }
        val currentOnCollapsed by rememberUpdatedState(onCollapsed)
        val collapseAd: (() -> Unit)? = if (collapsible) {
            {
                isCollapsed = true
                hasInteracted = true
                currentOnCollapsed()
            }
        } else null
        Box(
            modifier = outerModifier.pointerInput(reloadKey, onInteraction) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) {
                                hasInteracted = true
                                onInteraction()
                            }
                        }
                    }
                }
        ) {
            AndroidView(
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else if (isVisible && !isCollapsed) {
                    Modifier.fillMaxWidth().wrapContentHeight()
                } else {
                    Modifier.height(0.dp)
                },
                factory = { viewContext ->
                    CollapsePassThroughFrameLayout(viewContext).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        clipChildren = false
                        clipToPadding = false
                        frame = this
                    }
                },
                update = { frame = it }
            )
        }

        LaunchedEffect(frame, placement, candidates, reuseOnly, reloadKey) {
            val targetFrame = frame ?: return@LaunchedEffect
            fun reportFullscreenBound(success: Boolean) {
                if (isFullscreen) onFullscreenNativeBound(placement, success)
            }
            isVisible = false
            shownPlacement = null
            if (!MyAdsUtils.initializeAfterConsent(activity)) {
                if (placement == "N_Splash") splashNativeFailed = true
                reportFullscreenBound(false)
                return@LaunchedEffect
            }
            var selected = candidates.firstOrNull { MyAdsUtils.readyNative(it) != null }
            if (selected == null && !reuseOnly) {
                for (candidate in candidates) {
                    if (MyAdsUtils.loadNative(activity, candidate)) {
                        selected = candidate
                        break
                    }
                }
            }
            val resolved = selected ?: run {
                if (placement == "N_Splash") splashNativeFailed = true
                reportFullscreenBound(false)
                return@LaunchedEffect
            }
            val shown = MyAdsUtils.showNative(resolved, targetFrame, onCollapse = collapseAd)
            if (shown) {
                shownPlacement = resolved
                isVisible = true
                if (placement == "N_Splash") splashNativeFrame = targetFrame
            } else if (placement == "N_Splash") {
                splashNativeFailed = true
            }
            reportFullscreenBound(shown)
        }

        LaunchedEffect(shownPlacement, reloadAfterIdleMs, reloadKey, hasInteracted, isCollapsed) {
            val currentPlacement = shownPlacement ?: return@LaunchedEffect
            val waitMs = reloadAfterIdleMs ?: return@LaunchedEffect
            if (hasReloaded || hasInteracted || isCollapsed) return@LaunchedEffect
            delay(waitMs)
            if (!hasInteracted && !isCollapsed && !released) {
                hasReloaded = true
                isVisible = false
                MyAdsUtils.releaseNative(currentPlacement)
                val targetFrame = frame ?: return@LaunchedEffect
                val nextPlacement = candidates.firstOrNull { MyAdsUtils.readyNative(it) != null }
                    ?: candidates.firstOrNull { MyAdsUtils.loadNative(activity, it) }
                    ?: return@LaunchedEffect
                if (MyAdsUtils.showNative(nextPlacement, targetFrame, onCollapse = collapseAd)) {
                    shownPlacement = nextPlacement
                    isVisible = true
                }
            }
        }

        LaunchedEffect(interactionEpoch) {
            if (interactionEpoch != observedInteractionEpoch) {
                hasInteracted = true
                observedInteractionEpoch = interactionEpoch
            }
        }

        DisposableEffect(placement, reloadKey) {
            onDispose {
                if (placement == "N_Splash" && splashNativeFrame === frame) splashNativeFrame = null
                shownPlacement?.let(MyAdsUtils::releaseNative)
                isVisible = false
            }
        }
    }

    @Composable
    override fun BannerAd(placement: String, modifier: Modifier, visible: Boolean) {
        var frame by remember(placement) { mutableStateOf<FrameLayout?>(null) }
        var banner by remember(placement) { mutableStateOf<BannerAds<*>?>(null) }
        var isVisible by remember(placement) { mutableStateOf(false) }

        AndroidView(
            modifier = modifier.fillMaxWidth().then(
                if (isVisible && visible) Modifier.wrapContentHeight() else Modifier.height(0.dp)
            ),
            factory = { context ->
                FrameLayout(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    frame = this
                }
            },
            update = { frame = it }
        )
        LaunchedEffect(frame, placement) {
            val targetFrame = frame ?: return@LaunchedEffect
            if (!MyAdsUtils.initializeAfterConsent(activity)) return@LaunchedEffect
            val loaded = MyAdsUtils.loadBanner(activity, targetFrame, placement) ?: return@LaunchedEffect
            banner = loaded
            isVisible = MyAdsUtils.showBanner(placement, targetFrame)
        }
        DisposableEffect(placement) {
            onDispose {
                isVisible = false
                banner = null
                MyAdsUtils.releaseBanner(placement)
            }
        }
    }

    override fun release() {
        if (released) return
        released = true
        fullscreenOperation?.cancel()
        fullscreenClose?.cancel()
        scope.cancel()
        MyAdsUtils.onActivityReleased(activity)
        _fullscreenNativeState.value = null
    }

    private fun beginLoading() {
        if (loadingOperations == 0) loadingFrameReady = CompletableDeferred()
        loadingOperations += 1
        _isInterstitialLoading.value = loadingOperations > 0
    }

    private fun endLoading() {
        loadingOperations = (loadingOperations - 1).coerceAtLeast(0)
        _isInterstitialLoading.value = loadingOperations > 0
    }

    override fun onLoadingAdsRendered() {
        loadingFrameReady.complete(Unit)
    }

    private fun remaining(deadline: Long): Long = deadline - SystemClock.elapsedRealtime()

    private fun Activity.isResumedForAds(): Boolean =
        !isFinishing && !isDestroyed && (this as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true

    private companion object {
        const val SPLASH_PHASE_ONE_TIMEOUT_MS = 10_000L
        const val STARTUP_DEADLINE_MS = 30_000L
        const val PLACEMENT_LOAD_TIMEOUT_MS = 8_000L
        const val FULLSCREEN_BIND_TIMEOUT_MS = 2_500L
    }
}
