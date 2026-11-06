package com.semavia.ailearn.ui.tutor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimedLessonCompletionTest {
    private val expired = TimedLessonCompletionState(remainingSeconds = 0, isResumed = true)

    @Test
    fun completesOnlyAfterExpiryAndWhenAppIsActive() {
        assertTrue(expired.canComplete)
        assertFalse(expired.copy(remainingSeconds = 1).canComplete)
        assertFalse(expired.copy(isResumed = false).canComplete)
    }

    @Test
    fun waitsForResponsesSpeechDialogsAndEditing() {
        listOf(
            expired.copy(responsePending = true),
            expired.copy(speechPending = true),
            expired.copy(playbackPending = true),
            expired.copy(dialogVisible = true),
            expired.copy(completing = true),
            expired.copy(editing = true)
        ).forEach { assertFalse(it.canComplete) }
    }
}
