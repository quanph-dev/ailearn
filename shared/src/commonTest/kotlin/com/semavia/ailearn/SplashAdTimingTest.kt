package com.semavia.ailearn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplashAdTimingTest {
    @Test
    fun visibilityExcludesBackgroundAndNewForegroundEpoch() {
        val exposure = SplashNativeExposure(10_000)
        exposure.observe(2_000, true, 0)
        exposure.observe(3_000, true, 0)
        exposure.observe(4_000, false, 1)
        exposure.observe(9_000, true, 1)
        exposure.observe(10_000, true, 1)
        assertEquals(2_000L, exposure.visibleMs)
        assertTrue(exposure.startedBeforeCutoff)
    }

    @Test
    fun earlyInterWaitsForThreeSecondsButNotForAbsentNativeAfterCutoff() {
        fun wait(now: Long, ready: Long, visible: Long, started: Boolean, failed: Boolean = false) =
            shouldWaitForSplashNative(now, ready, 10_000, 30_000, failed, visible, started)
        assertTrue(wait(3_000, 3_000, 1_000, true))
        assertFalse(wait(5_000, 3_000, 3_000, true))
        assertTrue(wait(11_000, 2_000, 2_000, true))
        assertFalse(wait(10_000, 2_000, 0, false))
        assertFalse(wait(10_000, 10_000, 0, false))
        assertFalse(wait(3_000, 2_000, 0, false, failed = true))
        assertFalse(wait(30_000, 2_000, 0, true))
    }
}
