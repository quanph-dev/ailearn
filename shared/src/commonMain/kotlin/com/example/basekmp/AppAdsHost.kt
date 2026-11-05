package com.example.basekmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

/** Android supplies Prox; other targets keep the default no-ad behavior. */
interface AppAdsHost {
    val isInterstitialLoading: StateFlow<Boolean>
    val splashPhase: StateFlow<Int>
    val fullscreenNativeState: StateFlow<FullscreenNativeState?>
    val homeInteractionEpoch: StateFlow<Int>
    fun onLoadingAdsRendered() {}

    suspend fun showStartupAds(returningUser: Boolean)
    suspend fun showPendingStartupFallback()
    suspend fun preloadNative(placement: String, fallbacks: List<String> = emptyList()): Boolean
    suspend fun showInterstitial(
        placement: String,
        timeoutMs: Long = 8_000L,
        minIntervalMs: Long = 0L
    ): Boolean
    suspend fun showFullscreenNative(placement: String, closeDelaySeconds: Int = 5): Boolean
    fun onFullscreenNativeBound(placement: String, success: Boolean)
    fun closeFullscreenNative()
    fun notifyHomeInteraction()
    fun onAppPaused(): Boolean
    fun onAppBackgrounded()
    suspend fun onAppForegrounded(): Boolean

    @Composable
    fun NativeAd(
        placement: String,
        modifier: Modifier = Modifier,
        fallbacks: List<String> = emptyList(),
        reuseOnly: Boolean = false,
        collapsible: Boolean = false,
        reloadKey: Int = 0,
        reloadAfterIdleMs: Long? = null,
        onInteraction: () -> Unit = {},
        onCollapsed: () -> Unit = {}
    )

    @Composable
    fun BannerAd(placement: String, modifier: Modifier = Modifier, visible: Boolean = true)

    fun release()
}

data class FullscreenNativeState(
    val placement: String,
    val secondsRemaining: Int,
    val canClose: Boolean
)
