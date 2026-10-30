package com.example.basekmp.data

import kotlinx.serialization.Serializable

@Serializable
data class LearningItem(
    val title: String,
    val timestamp: Long,
    val score: Int = 0,
    val detail: String = "",
    val total: Int = 0,
    val level: String = ""
) {
    val scorePercent: Int?
        get() = if (total <= 0) {
            null
        } else {
            ((score.toLong() * 100L + total / 2L) / total)
                .toInt()
                .coerceIn(0, 100)
        }
}
