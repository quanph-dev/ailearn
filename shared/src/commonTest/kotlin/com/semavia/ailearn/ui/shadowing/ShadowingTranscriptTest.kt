package com.semavia.ailearn.ui.shadowing

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

    @Test
    fun scriptCodeReturnsEnglishWhenLearningEnglishOrOther() {
        val video = com.semavia.ailearn.data.ShadowingVideos.Video(
            id = "test",
            title = "Test",
            level = "A1",
            category = "daily",
            scene = "☕",
            colors = 0L to 0L,
            asset = "test.mp4",
            thumbMs = 500L,
            speakers = emptyList(),
            lines = listOf(
                com.semavia.ailearn.data.ShadowingVideos.Line(
                    speaker = 0,
                    text = "Hello",
                    translations = mapOf("es" to "Hola", "fr" to "Bonjour"),
                    startMs = 0,
                    endMs = 1000
                )
            ),
            words = emptyList()
        )
        assertEquals("en", com.semavia.ailearn.data.ShadowingVideos.scriptCode(video, "en"))
        assertEquals("en", com.semavia.ailearn.data.ShadowingVideos.scriptCode(video, "other"))
        assertEquals("en", com.semavia.ailearn.data.ShadowingVideos.scriptCode(video, ""))
        assertEquals("es", com.semavia.ailearn.data.ShadowingVideos.scriptCode(video, "es"))
        assertEquals("fr", com.semavia.ailearn.data.ShadowingVideos.scriptCode(video, "fr"))
        assertEquals("en", com.semavia.ailearn.data.ShadowingVideos.scriptCode(video, "de"))
        val blankTranslation = video.copy(lines = video.lines.map { it.copy(translations = mapOf("es" to "   ")) })
        assertEquals("en", com.semavia.ailearn.data.ShadowingVideos.scriptCode(blankTranslation, "es"))
    }
}
