package com.example.basekmp.ui.vocab

/**
 * Vocabulary word model with targeted highlighting in example sentence.
 * 100% migrated from original app's VocabWord.java.
 */
data class VocabWord(
    val word: String,
    val meaning: String,
    val exampleBefore: String,
    val exampleWord: String,
    val exampleAfter: String,
    val translation: String
)
