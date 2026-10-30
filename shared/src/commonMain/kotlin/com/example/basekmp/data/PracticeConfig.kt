package com.example.basekmp.data

/**
 * Global configuration settings for Practice sessions.
 * 100% migrated from original app's PracticeConfig.java.
 */
object PracticeConfig {
    var vocabCategory: String = "Daily life"
    var vocabLevel: String = "Intermediate"
    var grammarType: String = "Prepositions"
    var grammarDifficulty: String = "Intermediate"
    var speakingType: String = "Speak Sentences"
    var speakingDifficulty: String = "Intermediate"

    val CATEGORIES = listOf("Daily life", "Business", "Academic", "Travel", "IELTS")
    val LEVELS = listOf("Beginner", "Intermediate", "Advanced")
    val COUNTS = listOf(5, 10, 15, 20)
    val GRAMMAR_TYPES = listOf("Prepositions", "Tenses", "Articles", "Conjunctions")
    val DIFFICULTIES = listOf("Beginner", "Intermediate", "Advanced")
}
