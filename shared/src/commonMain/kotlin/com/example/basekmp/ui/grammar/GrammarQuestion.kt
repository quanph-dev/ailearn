package com.example.basekmp.ui.grammar

/**
 * Multiple-choice option evaluation state.
 * 100% migrated from original app's OptionState.java.
 */
enum class OptionState {
    Idle,
    Selected,
    Correct,
    Wrong
}

/**
 * Grammar quiz question model.
 * 100% migrated from original app's GrammarQuestion.java.
 */
data class GrammarQuestion(
    val text: String,
    val options: List<String>,
    val answerIndex: Int
)
