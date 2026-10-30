package com.example.basekmp.ui.shadowing

import kotlin.test.Test
import kotlin.test.assertEquals

class ShadowingTranscriptTest {
    @Test
    fun playbackPositionAdvancesOnlyWordsInsideTheCurrentLine() {
        assertEquals(0, ShadowingTranscript.progressedWordCount(0, 1_000, 5_000, 4))
        assertEquals(0, ShadowingTranscript.progressedWordCount(1_000, 1_000, 5_000, 4))
        assertEquals(2, ShadowingTranscript.progressedWordCount(3_000, 1_000, 5_000, 4))
        assertEquals(4, ShadowingTranscript.progressedWordCount(5_000, 1_000, 5_000, 4))
    }

    @Test
    fun partialRecognitionColorsMatchedWordsAndLeavesTheRestUnheard() {
        assertEquals(
            listOf(ShadowingTranscript.GOOD, ShadowingTranscript.GOOD, ShadowingTranscript.UNHEARD),
            ShadowingTranscript.wordStatuses("Hello there friend", "Hello there")
        )
    }

    @Test
    fun recognitionAlignmentMarksWrongWordsInsteadOfPretendingTheyMatch() {
        assertEquals(
            listOf(ShadowingTranscript.WRONG, ShadowingTranscript.GOOD),
            ShadowingTranscript.wordStatuses("Good morning", "Bad morning")
        )
    }
}
