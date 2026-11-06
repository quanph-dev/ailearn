package com.semavia.ailearn.ui.speaking

import java.util.Locale
import com.semavia.ailearn.data.PlanStage
import com.semavia.ailearn.data.Strings
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeechSegmentsTest {
    private val english = Locale.US
    private val vietnamese = Locale.forLanguageTag("vi-VN")

    @Test
    fun openingGreetingUsesVietnameseForQuotedTitle() {
        val text = "Hello! Ready for the lesson on \"Chào hỏi và trò chuyện\"? Tap the mic."
        val segments = speechSegments(text, english, vietnamese, vietnamese)
        assertEquals(listOf(english, vietnamese, english), segments.map { it.second })
        assertEquals(text, segments.joinToString("") { it.first })
    }

    @Test
    fun explicitTitleLanguageWorksWithoutDistinctiveCharacters() {
        val spanish = Locale.forLanguageTag("es-ES")
        val segments = speechSegments("Hello! \"Hola amigo\". Please speak.", english, vietnamese, spanish)
        assertEquals(listOf(english, spanish, english), segments.map { it.second })
    }

    @Test
    fun englishGreetingDetectsVietnameseQuoteWithoutExplicitHint() {
        val segments = speechSegments("Hello! \"Chào bạn\". Please speak.", english, vietnamese)
        assertEquals(listOf(english, vietnamese, english), segments.map { it.second })
    }

    @Test
    fun vietnameseReplyKeepsEnglishExampleInEnglish() {
        val segments = speechSegments("Bạn hãy nói \"Hello there\" nhé.", english, vietnamese)
        assertEquals(listOf(vietnamese, english, vietnamese), segments.map { it.second })
    }

    @Test
    fun vietnameseTranslationInQuotesAndParenthesesUsesVietnamese() {
        val text = "Rất tốt! \"Nice to meet you, Peter!\" (Ý nghĩa: \"Thật vui được gặp bạn, Peter!\") Câu này dùng để chào hỏi."
        val segments = speechSegments(text, english, vietnamese)
        assertEquals(listOf(vietnamese, english, vietnamese), segments.map { it.second })
        assertEquals(text, segments.joinToString("") { it.first })
        assertEquals(true, segments.last().first.contains("Thật vui được gặp bạn"))
    }

    @Test
    fun parenthesizedVietnameseIsNotReadWithTheSurroundingEnglishVoice() {
        val text = "Say hello (Xin chào) to your friend."
        val segments = speechSegments(text, english, vietnamese)
        assertEquals(listOf(english, vietnamese, english), segments.map { it.second })
        assertEquals(text, segments.joinToString("") { it.first })
    }

    @Test
    fun parenthesizedEnglishInVietnameseKeepsItsEnglishVoice() {
        val text = "Hãy nói câu này (Nice to meet you) nhé."
        val segments = speechSegments(text, english, vietnamese)
        assertEquals(listOf(vietnamese, english, vietnamese), segments.map { it.second })
        assertEquals(text, segments.joinToString("") { it.first })
    }

    @Test
    fun accentedEnglishLoanwordInQuoteDoesNotBecomeVietnamese() {
        val segments = speechSegments("Hãy nói \"café culture\" nhé.", english, vietnamese)
        assertEquals(listOf(vietnamese, english, vietnamese), segments.map { it.second })
    }

    @Test
    fun nonLatinNativeScriptAndEnglishRemainSeparate() {
        val hindi = Locale.forLanguageTag("hi-IN")
        val segments = speechSegments("नमस्ते \"hello there\" धन्यवाद", english, hindi)
        assertEquals(listOf(hindi, english, hindi), segments.map { it.second })
    }

    @Test
    fun speechChunksPreserveTextOffsetsAcrossLongSentences() {
        val text = "A longer sentence with many different words for checking the precise speaking position ".repeat(5)
        assertEquals(text, splitForSpeech(text).joinToString(""))
    }

    @Test
    fun englishQuoteStaysEnglish() {
        val text = "Please say \"Good morning\"."
        assertEquals(listOf(text to english), speechSegments(text, english, vietnamese))
    }

    @Test
    fun explicitEnglishTitleOverridesHeuristic() {
        val text = "Talk about \"café culture\"."
        assertEquals(listOf(text to english), speechSegments(text, english, vietnamese, english))
    }

    @Test
    fun titleLanguageFollowsTheSameTranslationFallbackAsTheTitle() {
        val previous = Strings.getLanguage()
        try {
            Strings.select("vi")
            val stage = PlanStage(id = "test", title = "Greetings", learnTitles = mapOf(
                "en" to mapOf("vi" to "Chào hỏi", "en" to "Greetings")
            ))
            assertEquals("Chào hỏi", stage.displayTitle("en"))
            assertEquals("vi", stage.displayTitleLanguage("en"))
            Strings.select("es")
            assertEquals("Greetings", stage.displayTitle("en"))
            assertEquals("en", stage.displayTitleLanguage("en"))
        } finally {
            Strings.select(previous)
        }
    }
}
