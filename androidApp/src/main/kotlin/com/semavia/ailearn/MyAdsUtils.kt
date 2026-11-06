package com.semavia.ailearn

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAdView
import androidx.lifecycle.LifecycleOwner
import com.google.ads.pro.base.BannerAds
import com.google.ads.pro.base.NativeAds
import com.google.ads.pro.callback.LoadAdsCallback
import com.google.ads.pro.callback.ShowAdsCallback
import com.google.ads.pro.manager.AdsManager
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.semavia.ailearn.core.analytics.logFirebaseEvent
import com.semavia.ailearn.core.analytics.logFirstFlowAdResult
import com.proxglobal.ads.AdsUtils
import com.semavia.ailearn.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** Prox SDK adapter. SDK calls and their callbacks are serialized on the main thread. */
object MyAdsUtils {
    private const val TAG = "MyAdsUtils"
    private const val INIT_TIMEOUT_MS = 15_000L
    private const val DEFAULT_LOAD_TIMEOUT_MS = 8_000L

    private enum class NativeStatus { LOADING, READY_UNUSED, SHOWN, FAILED }

    private data class InterstitialLoad(
        val completion: CompletableDeferred<Boolean>,
        var terminalLogged: Boolean = false
    )

    private data class NativeEntry(
        val completion: CompletableDeferred<Boolean>,
        val trackSplashPreload: Boolean,
        val callbackTerminal: AtomicBoolean = AtomicBoolean(false),
        var ad: NativeAds<*>? = null,
        var frame: FrameLayout? = null,
        var status: NativeStatus = NativeStatus.LOADING,
        var terminalLogged: Boolean = false
    )

    private data class BannerLoadEntry(
        val completion: CompletableDeferred<BannerAds<*>?> = CompletableDeferred(),
        val terminalLogged: AtomicBoolean = AtomicBoolean(false),
        val callbackTerminal: AtomicBoolean = AtomicBoolean(false),
        var banner: BannerAds<*>? = null,
        var loadReturned: Boolean = false,
        var success: Boolean? = null,
        var failureReason: String? = null
    )

    private val initMutex = Mutex()
    private val lock = Any()
    private val interstitialLoads = mutableMapOf<String, InterstitialLoad>()
    private val loadedInterstitials = mutableSetOf<String>()
    private val nativeAds = mutableMapOf<String, NativeEntry>()
    private val banners = mutableMapOf<String, BannerAds<*>>()
    private val bannerLoads = mutableMapOf<String, BannerLoadEntry>()
    private val bannerFailures = mutableSetOf<String>()
    private val shownBanners = mutableMapOf<String, FrameLayout>()
    private val interstitialShowSuccessAt = mutableMapOf<String, Long>()

    @Volatile private var initialized = false
    @Volatile private var remoteConfigObserved = false
    @Volatile private var activityShowingSdkFullscreen = false
    @Volatile private var consentFormVisible = false
    @Volatile private var consentResolved = false
    @Volatile private var canRequestAds = false
    private val remoteConfigFirstResultLogged = AtomicBoolean(false)
    private val sdkInitFirstResultLogged = AtomicBoolean(false)

    fun isAdsReady(): Boolean = initialized

    fun isShowingSdkFullscreen(): Boolean = activityShowingSdkFullscreen

    fun isShowingConsentForm(): Boolean = consentFormVisible

    fun lastInterstitialShowAt(placement: String): Long? = synchronized(lock) {
        interstitialShowSuccessAt[placement]
    }

    /** Consent, the SDK, and either remote or local ads config must be ready before placements. */
    suspend fun initializeAfterConsent(activity: Activity, trackStartup: Boolean = false): Boolean = initMutex.withLock {
        if (initialized) return@withLock true
        if (!activity.isUsable()) return@withLock false

        observeRemoteConfigOnce(activity, trackStartup)
        if (!consentResolved) {
            val consentResult = withTimeoutOrNull(INIT_TIMEOUT_MS) { requestConsent(activity) }
            if (consentResult != null) {
                canRequestAds = consentResult
                consentResolved = true
            }
        }
        if (!canRequestAds || !activity.isUsable()) {
            Log.i(TAG, "Ads initialization skipped: consent unavailable")
            return@withLock false
        }

        val initialization = CompletableDeferred<Boolean>()
        try {
            AdsManager.initAds(activity) {
                if (trackStartup && sdkInitFirstResultLogged.compareAndSet(false, true)) {
                    logFirebaseEvent("spl_ads_lib_init_done")
                }
                initialization.complete(true)
            }
        } catch (failure: Exception) {
            Log.w(TAG, "AdsManager init failed", failure)
            if (trackStartup && sdkInitFirstResultLogged.compareAndSet(false, true)) {
                logFirebaseEvent("spl_ads_lib_init_fail")
            }
            initialization.complete(false)
        }
        val initializedBySdk = withTimeoutOrNull(INIT_TIMEOUT_MS) { initialization.await() } == true
        val managerReady = if (initializedBySdk) {
            withTimeoutOrNull(INIT_TIMEOUT_MS) {
                AdsManager.isReadyLoadAds.first { it }
            } == true
        } else {
            false
        }
        if (!managerReady) return@withLock false

        // The SDK can report itself initialized before it has selected remote or local
        // placement data. Do not let status checks race that configuration load.
        val adsConfigReady = withTimeoutOrNull(INIT_TIMEOUT_MS) {
            AdsManager.isReadyLoadRemoteConfig.first { it }
        } == true
        initialized = adsConfigReady
        Log.i(TAG, "Ads SDK ready=$initialized")
        initialized
    }

    private fun observeRemoteConfigOnce(activity: Activity, trackStartup: Boolean) {
        synchronized(lock) {
            if (remoteConfigObserved) return
            remoteConfigObserved = true
        }
        try {
            AdsUtils.observeLoadRemoteConfigAds(activity as LifecycleOwner) { loaded ->
                Log.d(TAG, "Remote ads config callback ready=$loaded")
                if (trackStartup && remoteConfigFirstResultLogged.compareAndSet(false, true)) {
                    logFirebaseEvent("spl_rm_config_load_${if (loaded) "done" else "fail"}")
                }
            }
        } catch (failure: Exception) {
            Log.w(TAG, "Remote ads config observer unavailable", failure)
            if (trackStartup && remoteConfigFirstResultLogged.compareAndSet(false, true)) {
                logFirebaseEvent("spl_rm_config_load_fail")
            }
        }
    }

    private suspend fun requestConsent(activity: Activity): Boolean = suspendCancellableCoroutine { continuation ->
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val parameters = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        val finished = java.util.concurrent.atomic.AtomicBoolean(false)
        fun finish() {
            if (finished.compareAndSet(false, true) && continuation.isActive) {
                continuation.resume(runCatching { consentInformation.canRequestAds() }.getOrDefault(false))
            }
        }
        try {
            consentInformation.requestConsentInfoUpdate(
                activity,
                parameters,
                {
                    consentFormVisible = true
                    try {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            if (formError != null) Log.i(TAG, "Consent form ended with ${formError.errorCode}")
                            consentFormVisible = false
                            finish()
                        }
                    } catch (failure: Exception) {
                        consentFormVisible = false
                        Log.w(TAG, "Consent form unavailable", failure)
                        finish()
                    }
                },
                { consentError ->
                    Log.i(TAG, "Consent info update ended with ${consentError.errorCode}")
                    finish()
                }
            )
        } catch (failure: Exception) {
            Log.w(TAG, "Consent request failed", failure)
            finish()
        }
        continuation.invokeOnCancellation { finished.set(true) }
    }

    suspend fun loadInterstitial(
        activity: Activity,
        placement: String,
        timeoutMs: Long = DEFAULT_LOAD_TIMEOUT_MS
    ): Boolean = withContext(Dispatchers.Main.immediate) {
        if (!initialized) {
            AdsTechnicalLogger.log("load", placement, "fail", "ads_sdk_not_ready")
            return@withContext false
        }
        if (!activity.isUsable()) {
            AdsTechnicalLogger.log("load", placement, "fail", "activity_unavailable")
            return@withContext false
        }
        synchronized(lock) {
            if (placement in loadedInterstitials) return@withContext true
        }
        val load: InterstitialLoad
        val shouldStart: Boolean
        synchronized(lock) {
            val existing = interstitialLoads[placement]
            if (existing != null) {
                load = existing
                shouldStart = false
            } else {
                load = InterstitialLoad(CompletableDeferred())
                interstitialLoads[placement] = load
                shouldStart = true
            }
        }
        if (shouldStart) {
            if (AdsUtils.isInterstitialAdsStatus(placement) != true) {
                finishInterstitialLoad(placement, load, false, "disabled", fromSdkResult = false)
            } else {
                AdsTechnicalLogger.log("load", placement, "start")
                try {
                    AdsUtils.loadInterstitialAds(activity, placement, object : LoadAdsCallback() {
                        override fun onLoadSuccess(adsMeta: Boolean?) {
                            // Prox uses adsMeta for mediation metadata; callback itself means loaded.
                            finishInterstitialLoad(placement, load, true, null, fromSdkResult = true)
                        }

                        override fun onLoadFailed(message: String?) {
                            finishInterstitialLoad(placement, load, false, message ?: "load_failed", fromSdkResult = true)
                        }
                    })
                } catch (failure: Exception) {
                    finishInterstitialLoad(placement, load, false, failure.javaClass.simpleName, fromSdkResult = true)
                }
            }
        }
        val loaded = withTimeoutOrNull(timeoutMs.coerceAtLeast(1L)) { load.completion.await() }
        if (loaded == null) {
            val timedOut = synchronized(lock) {
                if (interstitialLoads[placement] !== load || load.completion.isCompleted) return@synchronized false
                if (!load.terminalLogged) {
                    load.terminalLogged = true
                }
                interstitialLoads.remove(placement)
                load.completion.complete(false)
                true
            }
            if (timedOut) {
                AdsTechnicalLogger.log("load", placement, "fail", "timeout")
                runCatching { AdsUtils.releaseInterstitialAds(placement) }
            }
            false
        } else loaded
    }

    private fun finishInterstitialLoad(
        placement: String,
        operation: InterstitialLoad,
        success: Boolean,
        reason: String?,
        fromSdkResult: Boolean
    ) {
        synchronized(lock) {
            if (interstitialLoads[placement] !== operation || operation.completion.isCompleted) return
            if (success) loadedInterstitials += placement
            else interstitialLoads.remove(placement)
            val shouldLog = !operation.terminalLogged
            operation.terminalLogged = true
            if (shouldLog) {
                AdsTechnicalLogger.log("load", placement, if (success) "success" else "fail", reason)
                if (fromSdkResult) logFirstFlowAdResult("load", placement, success)
            }
            operation.completion.complete(success)
        }
    }

    suspend fun showLoadedInterstitial(
        activity: Activity,
        placement: String,
        showCallbackTimeoutMs: Long = DEFAULT_LOAD_TIMEOUT_MS
    ): Boolean =
        withContext(Dispatchers.Main.immediate) {
            if (!activity.isUsable()) {
                AdsTechnicalLogger.log("show", placement, "fail", "activity_unavailable")
                return@withContext false
            }
            if (!synchronized(lock) { placement in loadedInterstitials }) {
                AdsTechnicalLogger.log("show", placement, "fail", "ad_not_ready")
                return@withContext false
            }
            val showStarted = CompletableDeferred<Boolean>()
            val closed = CompletableDeferred<Boolean>()
            val showResultLogged = AtomicBoolean(false)
            val adClosed = AtomicBoolean(false)
            var shown = false
            activityShowingSdkFullscreen = true
            AdsTechnicalLogger.log("show", placement, "start")
            try {
                AdsUtils.showInterstitialAdsNoLoad(activity, placement, object : ShowAdsCallback() {
                    override fun onShowSuccess() {
                        if (adClosed.get()) return
                        activityShowingSdkFullscreen = true
                        if (!showResultLogged.compareAndSet(false, true)) return
                        shown = true
                        synchronized(lock) { interstitialShowSuccessAt[placement] = SystemClock.elapsedRealtime() }
                        AdsTechnicalLogger.log("show", placement, "success")
                        logFirstFlowAdResult("show", placement, true)
                        showStarted.complete(true)
                    }

                    override fun onShowFailed(message: String?) {
                        if (!showResultLogged.compareAndSet(false, true)) return
                        adClosed.set(true)
                        activityShowingSdkFullscreen = false
                        AdsTechnicalLogger.log("show", placement, "fail", message ?: "show_failed")
                        logFirstFlowAdResult("show", placement, false)
                        showStarted.complete(false)
                        closed.complete(false)
                    }

                    override fun onAdClosed() {
                        adClosed.set(true)
                        activityShowingSdkFullscreen = false
                        showStarted.complete(shown)
                        closed.complete(shown)
                    }
                })
            } catch (failure: Exception) {
                if (showResultLogged.compareAndSet(false, true)) {
                    AdsTechnicalLogger.log("show", placement, "fail", failure.javaClass.simpleName)
                    logFirstFlowAdResult("show", placement, false)
                }
                showStarted.complete(false)
                closed.complete(false)
            }
            val result = try {
                val opened = withTimeoutOrNull(showCallbackTimeoutMs.coerceAtLeast(1L)) { showStarted.await() } ?: false
                if (!opened && showResultLogged.compareAndSet(false, true)) {
                    AdsTechnicalLogger.log("show", placement, "fail", "show_callback_timeout")
                    closed.complete(false)
                }
                if (opened) closed.await() else false
            } finally {
                if (!shown) {
                    activityShowingSdkFullscreen = false
                    runCatching { AdsUtils.releaseInterstitialAds(placement) }
                }
                synchronized(lock) {
                    loadedInterstitials.remove(placement)
                    interstitialLoads.remove(placement)
                }
            }
            result
        }

    fun isInterstitialLoaded(placement: String): Boolean = synchronized(lock) {
        placement in loadedInterstitials
    }

    fun cancelInterstitial(placement: String) {
        val cancelledLoad = synchronized(lock) {
            val operation = interstitialLoads.remove(placement)
            val pending = operation != null && !operation.completion.isCompleted && !operation.terminalLogged
            if (pending && operation != null) {
                operation.terminalLogged = true
                operation.completion.complete(false)
            }
            loadedInterstitials.remove(placement)
            pending
        }
        if (cancelledLoad) AdsTechnicalLogger.log("load", placement, "fail", "cancelled_or_timeout")
        runCatching { AdsUtils.releaseInterstitialAds(placement) }
    }

    suspend fun loadNative(
        activity: Activity,
        placement: String,
        timeoutMs: Long = DEFAULT_LOAD_TIMEOUT_MS,
        trackSplashPreload: Boolean = false
    ): Boolean = withContext(Dispatchers.Main.immediate) {
        if (!initialized) {
            AdsTechnicalLogger.log("load", placement, "fail", "ads_sdk_not_ready")
            return@withContext false
        }
        if (!activity.isUsable()) {
            AdsTechnicalLogger.log("load", placement, "fail", "activity_unavailable")
            return@withContext false
        }
        if (synchronized(lock) { nativeAds[placement]?.status == NativeStatus.SHOWN }) {
            return@withContext false
        }
        var shouldStart = false
        val entry: NativeEntry
        synchronized(lock) {
            val existing = nativeAds[placement]
            if (existing != null && existing.status in setOf(NativeStatus.LOADING, NativeStatus.READY_UNUSED)) {
                entry = existing
            } else {
                entry = NativeEntry(CompletableDeferred(), trackSplashPreload)
                nativeAds[placement] = entry
                shouldStart = true
            }
        }
        if (shouldStart) {
            if (AdsUtils.isNativeAdsStatus(placement) != true) {
                finishNativeLoad(placement, entry, null, false, "disabled", fromSdkResult = false)
            } else {
                AdsTechnicalLogger.log("load", placement, "start")
                try {
                    var callbackResult: Boolean? = null
                    var callbackReason: String? = null
                    val nativeAd = AdsUtils.loadNativeAds(
                        activity,
                        null,
                        placement,
                        object : LoadAdsCallback() {
                            override fun onLoadSuccess(adsMeta: Boolean?) {
                                if (!entry.callbackTerminal.compareAndSet(false, true)) return
                                val loadedAd = nativeAdRef(entry)
                                if (loadedAd == null) {
                                    callbackResult = true
                                    callbackReason = null
                                } else {
                                    finishNativeLoad(placement, entry, loadedAd, true, null, fromSdkResult = true)
                                }
                            }

                            override fun onLoadFailed(message: String?) {
                                if (!entry.callbackTerminal.compareAndSet(false, true)) return
                                val error = message ?: "load_failed"
                                val loadedAd = nativeAdRef(entry)
                                if (loadedAd == null) {
                                    callbackResult = false
                                    callbackReason = error
                                } else {
                                    finishNativeLoad(placement, entry, loadedAd, false, error, fromSdkResult = true)
                                }
                            }
                        }
                    )
                    synchronized(lock) { entry.ad = nativeAd }
                    val resultFromCallback = callbackResult
                    if (resultFromCallback != null) {
                        finishNativeLoad(placement, entry, nativeAd, resultFromCallback, callbackReason, fromSdkResult = true)
                    } else if (nativeAd == null) {
                        finishNativeLoad(placement, entry, null, false, "empty_result", fromSdkResult = true)
                    }
                } catch (failure: Exception) {
                    finishNativeLoad(placement, entry, nativeAdRef(entry), false, failure.javaClass.simpleName, fromSdkResult = true)
                }
            }
        }
        val result = withTimeoutOrNull(timeoutMs.coerceAtLeast(1L)) { entry.completion.await() }
        if (result == null) {
            synchronized(lock) {
                if (!entry.terminalLogged) {
                    entry.terminalLogged = true
                    AdsTechnicalLogger.log("load", placement, "fail", "timeout")
                }
            }
            false
        } else result
    }

    private fun nativeAdRef(entry: NativeEntry): NativeAds<*>? = synchronized(lock) { entry.ad }

    private fun finishNativeLoad(
        placement: String,
        entry: NativeEntry,
        ad: NativeAds<*>?,
        success: Boolean,
        reason: String?,
        fromSdkResult: Boolean
    ) {
        var succeeded = false
        var rejectedAd: NativeAds<*>? = null
        val shouldLog = synchronized(lock) {
            if (nativeAds[placement] !== entry || entry.completion.isCompleted) return
            if (ad != null) entry.ad = ad
            succeeded = success && entry.ad != null
            entry.status = if (succeeded) NativeStatus.READY_UNUSED else NativeStatus.FAILED
            if (!succeeded) {
                rejectedAd = entry.ad
                entry.ad = null
            }
            val logResult = !entry.terminalLogged
            entry.terminalLogged = true
            logResult
        }
        runCatching { rejectedAd?.destroyAds() }
        if (shouldLog) {
            AdsTechnicalLogger.log("load", placement, if (succeeded) "success" else "fail", if (succeeded) null else reason ?: "empty_result")
            if (fromSdkResult && entry.trackSplashPreload) logFirstFlowAdResult("load", placement, succeeded)
        }
        entry.completion.complete(succeeded)
    }

    fun readyNative(placement: String): NativeAds<*>? = synchronized(lock) {
        nativeAds[placement]?.takeIf { it.status == NativeStatus.READY_UNUSED }?.ad
    }

    suspend fun showNative(
        placement: String,
        frame: FrameLayout,
        timeoutMs: Long = 1_500L,
        onCollapse: (() -> Unit)? = null
    ): Boolean = withContext(Dispatchers.Main.immediate) {
        val entry = synchronized(lock) {
            nativeAds[placement]?.takeIf { it.status == NativeStatus.READY_UNUSED && it.ad != null }
        } ?: run {
            AdsTechnicalLogger.log("show", placement, "fail", "ad_not_ready")
            return@withContext false
        }
        if (!frame.isAttachedToWindow) {
            AdsTechnicalLogger.log("show", placement, "fail", "container_detached")
            logFirstFlowAdResult("show", placement, false)
            return@withContext false
        }
        val ad = entry.ad ?: run {
            AdsTechnicalLogger.log("show", placement, "fail", "ad_unavailable")
            logFirstFlowAdResult("show", placement, false)
            return@withContext false
        }
        AdsTechnicalLogger.log("show", placement, "start")
        try {
            frame.removeAllViews()
            (frame as? CollapsePassThroughFrameLayout)?.clearPassThroughTap()
            ad.showAds(frame)
            bindCollapseControl(frame, onCollapse)
            synchronized(lock) {
                entry.status = NativeStatus.SHOWN
                entry.frame = frame
            }
        } catch (failure: Exception) {
            synchronized(lock) { if (nativeAds[placement] === entry) nativeAds.remove(placement) }
            frame.removeAllViews()
            runCatching { ad.destroyAds() }
            AdsTechnicalLogger.log("show", placement, "fail", failure.javaClass.simpleName)
            logFirstFlowAdResult("show", placement, false)
            return@withContext false
        }
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            val nativeView = frame.getChildAt(0) as? NativeAdView
            val headline = nativeView?.headlineView as? TextView
            if (nativeView != null && headline?.text?.isNotBlank() == true) {
                AdsTechnicalLogger.log("show", placement, "success")
                logFirstFlowAdResult("show", placement, true)
                return@withContext true
            }
            kotlinx.coroutines.delay(50)
        }
        synchronized(lock) { if (nativeAds[placement] === entry) nativeAds.remove(placement) }
        frame.removeAllViews()
        runCatching { ad.destroyAds() }
        AdsTechnicalLogger.log("show", placement, "fail", "bind_timeout")
        logFirstFlowAdResult("show", placement, false)
        false
    }

    /**
     * Wires `iv_collapse` (overlaid on `ad_media`).
     * - With [CollapsePassThroughFrameLayout]: the overlay stays non-clickable so the real touch reaches
     *   the SDK-registered MediaView (ad click handled by the SDK), then [onCollapse] runs after that click.
     * - Other containers: legacy behaviour, the overlay consumes the tap and only collapses.
     */
    private fun bindCollapseControl(frame: FrameLayout, onCollapse: (() -> Unit)?) {
        val collapseView = frame.findViewById<android.view.View?>(R.id.iv_collapse) ?: return
        if (onCollapse == null) {
            collapseView.visibility = android.view.View.GONE
            return
        }
        if (frame is CollapsePassThroughFrameLayout) {
            // setOnClickListener(null) re-enables clickable, so reset the flags afterwards.
            collapseView.setOnClickListener(null)
            collapseView.isClickable = false
            collapseView.isLongClickable = false
            collapseView.isFocusable = false
            frame.setPassThroughTap(R.id.iv_collapse, onCollapse)
        } else {
            collapseView.setOnClickListener { onCollapse() }
        }
    }

    fun releaseNative(placement: String) {
        val entry = synchronized(lock) { nativeAds.remove(placement) } ?: return
        val cancelledLoad = synchronized(lock) {
            if (entry.status == NativeStatus.LOADING && !entry.terminalLogged) {
                entry.terminalLogged = true
                entry.completion.complete(false)
                true
            } else false
        }
        if (cancelledLoad) AdsTechnicalLogger.log("load", placement, "fail", "placement_released")
        (entry.frame as? CollapsePassThroughFrameLayout)?.clearPassThroughTap()
        entry.frame?.removeAllViews()
        runCatching { entry.ad?.destroyAds() }
    }

    suspend fun loadBanner(activity: Activity, frame: FrameLayout, placement: String): BannerAds<*>? =
        withContext(Dispatchers.Main.immediate) {
            if (!initialized) {
                AdsTechnicalLogger.log("load", placement, "fail", "ads_sdk_not_ready")
                return@withContext null
            }
            if (!activity.isUsable()) {
                AdsTechnicalLogger.log("load", placement, "fail", "activity_unavailable")
                return@withContext null
            }
            if (AdsUtils.isBannerAdsStatus(placement) != true) {
                AdsTechnicalLogger.log("load", placement, "fail", "disabled")
                return@withContext null
            }

            val cached = synchronized(lock) { banners[placement] }
            if (cached != null) return@withContext cached
            if (synchronized(lock) { placement in bannerFailures }) return@withContext null

            val (entry, ownsLoad) = synchronized(lock) {
                val current = bannerLoads[placement]
                if (current != null) {
                    current to false
                } else {
                    BannerLoadEntry().also { bannerLoads[placement] = it } to true
                }
            }

            if (ownsLoad) {
                AdsTechnicalLogger.log("load", placement, "start")
                try {
                    val displayMetrics = activity.resources.displayMetrics
                    val widthPixels = if (frame.width > 0) frame.width.toFloat() else displayMetrics.widthPixels.toFloat()
                    val adWidth = (widthPixels / displayMetrics.density).toInt()
                    val adSize = com.google.android.gms.ads.AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
                    val banner = AdsUtils.loadBannerAds(activity, frame, placement, adSize, object : LoadAdsCallback() {
                        override fun onLoadSuccess(adsMeta: Boolean?) {
                            if (!entry.callbackTerminal.compareAndSet(false, true)) return
                            finishBannerLoad(placement, entry, true, null)
                        }

                        override fun onLoadFailed(message: String?) {
                            if (!entry.callbackTerminal.compareAndSet(false, true)) return
                            finishBannerLoad(placement, entry, false, message ?: "load_failed")
                        }
                    }, AdsUtils.shimmerBanner)

                    val callbackSuccess = synchronized(lock) {
                        entry.banner = banner
                        entry.loadReturned = true
                        entry.success
                    }
                    if (callbackSuccess != null) finishBannerLoad(placement, entry, callbackSuccess, null)
                } catch (failure: Exception) {
                    if (failure is kotlin.coroutines.cancellation.CancellationException) throw failure
                    if (entry.callbackTerminal.compareAndSet(false, true)) {
                        synchronized(lock) { entry.loadReturned = true }
                        finishBannerLoad(placement, entry, false, failure.javaClass.simpleName)
                    }
                }
            }

            val loaded = withTimeoutOrNull(DEFAULT_LOAD_TIMEOUT_MS) { entry.completion.await() }
            val timedOut = synchronized(lock) {
                if (loaded != null || entry.completion.isCompleted || entry.terminalLogged.get()) {
                    false
                } else {
                    entry.terminalLogged.set(true)
                    entry.success = false
                    bannerFailures += placement
                    if (bannerLoads[placement] === entry) bannerLoads.remove(placement)
                    true
                }
            }
            if (timedOut) {
                AdsTechnicalLogger.log("load", placement, "fail", "timeout")
                entry.completion.complete(null)
                runCatching { entry.banner?.destroyAds() }
            }
            loaded
        }

    private fun finishBannerLoad(
        placement: String,
        entry: BannerLoadEntry,
        callbackSuccess: Boolean,
        reason: String?
    ) {
        var result: BannerAds<*>? = null
        var succeeded = false
        val shouldFinish = synchronized(lock) {
            if (entry.terminalLogged.get()) return
            entry.success = callbackSuccess
            if (!reason.isNullOrBlank()) entry.failureReason = reason
            if (!entry.loadReturned) return@synchronized false

            val loadedBanner = entry.banner
            succeeded = callbackSuccess && loadedBanner != null && bannerLoads[placement] === entry
            if (succeeded) {
                result = loadedBanner
                banners[placement] = loadedBanner!!
            } else {
                bannerFailures += placement
            }
            if (bannerLoads[placement] === entry) bannerLoads.remove(placement)
            entry.terminalLogged.set(true)
            true
        }
        if (!shouldFinish) return

        AdsTechnicalLogger.log(
            "load",
            placement,
            if (succeeded) "success" else "fail",
            if (succeeded) null else entry.failureReason ?: if (callbackSuccess) "empty_result" else "load_failed"
        )
        if (!succeeded) runCatching { entry.banner?.destroyAds() }
        entry.completion.complete(result)
    }

    fun showBanner(placement: String, frame: FrameLayout): Boolean {
        val banner = synchronized(lock) { banners[placement] } ?: run {
            AdsTechnicalLogger.log("show", placement, "fail", "ad_not_ready")
            return false
        }
        if (!frame.isAttachedToWindow) {
            AdsTechnicalLogger.log("show", placement, "fail", "container_detached")
            return false
        }
        val alreadyShown = synchronized(lock) {
            val previousFrame = shownBanners[placement]
            when {
                previousFrame === frame -> true
                previousFrame?.isAttachedToWindow == true -> true
                else -> {
                    shownBanners[placement] = frame
                    false
                }
            }
        }
        if (alreadyShown) return true

        AdsTechnicalLogger.log("show", placement, "start")
        return try {
            banner.showAds(frame)
            for (i in 0 until frame.childCount) {
                val child = frame.getChildAt(i)
                child.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.CENTER
                )
            }
            true
        } catch (failure: Exception) {
            synchronized(lock) { if (shownBanners[placement] === frame) shownBanners.remove(placement) }
            AdsTechnicalLogger.log("show", placement, "fail", failure.javaClass.simpleName)
            false
        }
    }

    fun releaseBanner(placement: String) {
        var logReleasedLoad = false
        val (banner, pending) = synchronized(lock) {
            shownBanners.remove(placement)
            bannerFailures.remove(placement)
            val activeLoad = bannerLoads.remove(placement)
            activeLoad?.let {
                it.success = false
                if (!it.terminalLogged.get()) {
                    it.terminalLogged.set(true)
                    logReleasedLoad = true
                }
            }
            banners.remove(placement) to activeLoad
        }
        if (logReleasedLoad) AdsTechnicalLogger.log("load", placement, "fail", "released")
        pending?.completion?.complete(null)
        runCatching { pending?.banner?.destroyAds() }
        runCatching { banner?.destroyAds() }
    }

    fun onActivityReleased(activity: Activity) {
        runCatching { AdsUtils.removeObserversLoadAds(activity as LifecycleOwner) }
        val (interstitialPlacements, pendingLoads) = synchronized(lock) {
            if (!initialized) remoteConfigObserved = false
            val pending = interstitialLoads.toMap()
            val placements = loadedInterstitials + pending.keys
            val pendingLoads = pending.filterValues { !it.completion.isCompleted }.keys
            pending.filterKeys { it in pendingLoads }.values.forEach { operation ->
                if (!operation.terminalLogged && !operation.completion.isCompleted) {
                    operation.terminalLogged = true
                    operation.completion.complete(false)
                }
            }
            interstitialLoads.clear()
            loadedInterstitials.clear()
            placements.toSet() to pendingLoads
        }
        pendingLoads.forEach { placement -> AdsTechnicalLogger.log("load", placement, "fail", "activity_released") }
        interstitialPlacements.forEach { placement ->
            runCatching { AdsUtils.removeObserversLoadInterstitialAds(placement, activity as LifecycleOwner) }
            runCatching { AdsUtils.releaseInterstitialAds(placement) }
        }
        val nativePlacements = synchronized(lock) { nativeAds.keys.toList() }
        nativePlacements.forEach(::releaseNative)
        val bannerPlacements = synchronized(lock) {
            (bannerLoads.keys + banners.keys + shownBanners.keys + bannerFailures).toList()
        }
        bannerPlacements.forEach(::releaseBanner)
        activityShowingSdkFullscreen = false
        consentFormVisible = false
    }

    private fun Activity.isUsable(): Boolean = !isFinishing && !isDestroyed
}
