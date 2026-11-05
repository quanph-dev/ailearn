package com.example.basekmp

/** Counts only intervals whose endpoints were both visible in the same foreground session. */
class SplashNativeExposure(private val cutoffMs: Long) {
    var visibleMs: Long = 0L
        private set
    var startedBeforeCutoff: Boolean = false
        private set
    private var lastTime: Long? = null
    private var lastVisible = false
    private var lastEpoch = 0L

    fun observe(now: Long, visible: Boolean, epoch: Long) {
        val previous = lastTime
        if (previous != null && visible && lastVisible && epoch == lastEpoch) {
            visibleMs += (now - previous).coerceAtLeast(0L)
        }
        if (visible && now < cutoffMs) startedBeforeCutoff = true
        lastTime = now
        lastVisible = visible
        lastEpoch = epoch
    }
}

fun shouldWaitForSplashNative(
    now: Long,
    interReadyAt: Long,
    cutoff: Long,
    deadline: Long,
    failed: Boolean,
    visibleMs: Long,
    startedBeforeCutoff: Boolean
): Boolean = interReadyAt < cutoff && now < deadline && !failed && visibleMs < 3_000L &&
    (startedBeforeCutoff || now < cutoff)
