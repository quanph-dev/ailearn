package com.example.basekmp.ui.tutor

/**
 * Chat message model for conversational AI Tutor screens.
 * 100% faithful port from ChatMessage.java.
 */
data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val animate: Boolean = false
)
