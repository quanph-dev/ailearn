package com.semavia.ailearn.ui.speaking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechTextTest {
    @Test fun removesControlTagsWithoutChangingProgressOffsets() {
        val original = "[TDGreat] Hello [TBoops] world."
        val cleaned = speechTextWithoutControlTags(original)
        assertEquals(original.length, cleaned.length)
        assertEquals("          Hello          world.", cleaned)
        assertEquals("Use [subject] and [ok] here.", speechTextWithoutControlTags("Use [subject] and [ok] here."))
    }

    @Test fun skipsStandalonePunctuationButKeepsWordsAndNumbers() {
        assertFalse(hasSpeakableContent(" ... ! ” "))
        assertTrue(hasSpeakableContent("你好。"))
        assertTrue(hasSpeakableContent("3.14"))
    }
}
