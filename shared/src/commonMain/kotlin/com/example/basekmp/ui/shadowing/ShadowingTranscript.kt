package com.example.basekmp.ui.shadowing

import com.example.basekmp.data.PronunciationScorer

/**
 * Maps real player time and recognized speech to the word states used by the
 * shadowing karaoke transcript.
 */
internal object ShadowingTranscript {
    const val UNHEARD = 0
    const val GOOD = 1
    const val CLOSE = 2
    const val WRONG = 3

    fun tokens(text: String): List<String> =
        text.split(Regex("\\s+")).filter(String::isNotBlank)

    /** Matches catalog vocabulary to transcript tokens such as `unfortunately,`. */
    fun normalizeToken(token: String): String = token
        .lowercase()
        .trim { character ->
            !character.isLetterOrDigit() && character != '\'' && character != '’'
        }

    /** Returns how many target words have elapsed on the media timeline. */
    fun progressedWordCount(
        positionMs: Long,
        lineStartMs: Long,
        lineEndMs: Long,
        wordCount: Int
    ): Int {
        if (wordCount <= 0 || lineEndMs <= lineStartMs || positionMs <= lineStartMs) return 0
        if (positionMs >= lineEndMs) return wordCount

        val elapsed = positionMs - lineStartMs
        val duration = lineEndMs - lineStartMs
        return ((elapsed.toDouble() / duration) * wordCount)
            .toInt()
            .coerceIn(0, wordCount)
    }

    /**
     * Aligns the target and recognized words in order. Inserted or dropped
     * words must not shift the status of every word that follows them.
     */
    fun wordStatuses(target: String, spoken: String, isFinal: Boolean = false): List<Int> {
        val targetTokens = tokens(target)
        val spokenTokens = tokens(spoken)
        if (targetTokens.isEmpty()) return emptyList()
        if (spokenTokens.isEmpty()) {
            return List(targetTokens.size) { if (isFinal) WRONG else UNHEARD }
        }

        val targetCount = targetTokens.size
        val spokenCount = spokenTokens.size
        val targetGapCost = 68
        val spokenGapCost = 38
        val scores = Array(targetCount) { targetIndex ->
            IntArray(spokenCount) { spokenIndex ->
                PronunciationScorer.score(targetTokens[targetIndex], spokenTokens[spokenIndex])
            }
        }
        val costs = Array(targetCount + 1) { IntArray(spokenCount + 1) }
        val actions = Array(targetCount + 1) { IntArray(spokenCount + 1) }
        val match = 0
        val dropTarget = 1
        val skipSpoken = 2

        for (targetIndex in 1..targetCount) {
            costs[targetIndex][0] = targetIndex * targetGapCost
            actions[targetIndex][0] = dropTarget
        }
        for (spokenIndex in 1..spokenCount) {
            costs[0][spokenIndex] = spokenIndex * spokenGapCost
            actions[0][spokenIndex] = skipSpoken
        }

        for (targetIndex in 1..targetCount) {
            for (spokenIndex in 1..spokenCount) {
                var bestCost = costs[targetIndex - 1][spokenIndex - 1] +
                    (100 - scores[targetIndex - 1][spokenIndex - 1])
                var bestAction = match

                val dropTargetCost = costs[targetIndex - 1][spokenIndex] + targetGapCost
                if (dropTargetCost < bestCost) {
                    bestCost = dropTargetCost
                    bestAction = dropTarget
                }

                val skipSpokenCost = costs[targetIndex][spokenIndex - 1] + spokenGapCost
                if (skipSpokenCost < bestCost) {
                    bestCost = skipSpokenCost
                    bestAction = skipSpoken
                }

                costs[targetIndex][spokenIndex] = bestCost
                actions[targetIndex][spokenIndex] = bestAction
            }
        }

        val statuses = MutableList(targetCount) { UNHEARD }
        var targetIndex = targetCount
        var spokenIndex = spokenCount
        while (targetIndex > 0 || spokenIndex > 0) {
            when {
                targetIndex == 0 -> spokenIndex--
                spokenIndex == 0 -> {
                    statuses[targetIndex - 1] = if (isFinal) WRONG else UNHEARD
                    targetIndex--
                }
                actions[targetIndex][spokenIndex] == match -> {
                    val score = scores[targetIndex - 1][spokenIndex - 1]
                    statuses[targetIndex - 1] = when {
                        score >= 80 -> GOOD
                        score >= 40 -> CLOSE
                        else -> WRONG
                    }
                    targetIndex--
                    spokenIndex--
                }
                actions[targetIndex][spokenIndex] == dropTarget -> {
                    statuses[targetIndex - 1] = if (isFinal || spokenIndex < spokenCount) WRONG else UNHEARD
                    targetIndex--
                }
                else -> spokenIndex--
            }
        }
        return statuses
    }
}
