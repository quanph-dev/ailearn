package com.semavia.ailearn.ui.speaking

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingMicrophoneStartTest {
    @Test
    fun permissionGrantedBeforeResumeStartsOnceAfterResume() {
        val request = PendingMicrophoneStart()
        request.request()
        assertFalse(request.takeIfResumed(false))
        assertTrue(request.pending)
        assertTrue(request.takeIfResumed(true))
        assertFalse(request.pending)
        assertFalse(request.takeIfResumed(true))
    }

    @Test
    fun deniedOrCancelledPermissionDoesNotStartRecording() {
        val request = PendingMicrophoneStart()
        request.request()
        request.cancel()
        assertFalse(request.takeIfResumed(true))
    }
}
