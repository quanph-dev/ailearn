package com.example.basekmp.data

import kotlin.math.roundToInt

/**
 * Pronunciation scoring algorithm using Levenshtein distance and word overlap.
 * 100% migrated from original app's PronunciationScorer.java.
 */
object PronunciationScorer {
    fun score(target: String, spoken: String): Int {
        val normTarget = normalize(target)
        val normSpoken = normalize(spoken)
        if (normTarget.isEmpty() || normSpoken.isEmpty()) {
            return 0
        }
        if (normTarget == normSpoken) {
            return 100
        }
        val maxLen = maxOf(normTarget.length, normSpoken.length)
        val lev = levenshtein(normTarget, normSpoken)
        val charScore = 1.0f - (lev.toFloat() / maxLen.toFloat())
        val wordScore = wordOverlap(normTarget, normSpoken)
        val finalScore = ((charScore * 0.6f) + (wordScore * 0.4f)) * 100.0f
        return finalScore.roundToInt().coerceIn(0, 100)
    }

    private fun normalize(s: String): String {
        val lower = s.lowercase()
        val stripped = Regex("[^\\p{L}\\p{N}\\s]").replace(lower, "")
        return Regex("\\s+").replace(stripped.trim(), " ")
    }

    private fun wordOverlap(a: String, b: String): Float {
        val wordsA = a.split(" ").filter { it.isNotEmpty() }.toSet()
        val wordsB = b.split(" ").filter { it.isNotEmpty() }.toSet()
        if (wordsA.isEmpty()) return 0.0f
        val common = wordsA.intersect(wordsB).size
        return common.toFloat() / wordsA.size.toFloat()
    }

    private fun levenshtein(a: String, b: String): Int {
        val lenA = a.length
        val lenB = b.length
        if (lenA == 0) return lenB
        if (lenB == 0) return lenA

        var costs = IntArray(lenB + 1) { it }
        var newCosts = IntArray(lenB + 1)

        for (i in 1..lenA) {
            newCosts[0] = i
            for (j in 1..lenB) {
                val match = if (a[i - 1] == b[j - 1]) 0 else 1
                val costReplace = costs[j - 1] + match
                val costInsert = costs[j] + 1
                val costDelete = newCosts[j - 1] + 1
                newCosts[j] = minOf(minOf(costInsert, costDelete), costReplace)
            }
            val swap = costs
            costs = newCosts
            newCosts = swap
        }
        return costs[lenB]
    }
}
