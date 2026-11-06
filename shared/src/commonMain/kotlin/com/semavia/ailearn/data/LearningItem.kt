package com.semavia.ailearn.data

import kotlinx.serialization.Serializable

@Serializable
data class LearningItem(
    val title: String,
    val timestamp: Long,
    // The original app uses -1 for a score that does not apply (for example,
    // an AI Tutor session). Keep that distinct from a real zero score.
    val score: Int = -1,
    val detail: String = "",
    val total: Int = -1,
    val level: String = ""
) {
    val scorePercent: Int?
        get() = if (score < 0 || total <= 0) {
            null
        } else {
            ((score.toLong() * 100L + total / 2L) / total)
                .toInt()
                .coerceIn(0, 100)
        }
}
