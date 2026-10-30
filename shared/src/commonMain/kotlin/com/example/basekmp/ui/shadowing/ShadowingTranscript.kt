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
     * Follows the original SpeechGrading flow: compare each target token with
     * the next three recognized tokens, then advance the transcript cursor.
     */
    fun wordStatuses(target: String, spoken: String): List<Int> {
        val targetTokens = tokens(target)
        val spokenTokens = tokens(spoken)
        var spokenIndex = 0

        return targetTokens.map { expected ->
            if (spokenIndex >= spokenTokens.size) {
                UNHEARD
            } else {
                val candidateEnd = minOf(spokenTokens.size, spokenIndex + 3)
                var bestIndex = spokenIndex
                var bestScore = -1

                for (index in spokenIndex until candidateEnd) {
                    val score = PronunciationScorer.score(expected, spokenTokens[index])
                    if (score > bestScore) {
                        bestScore = score
                        bestIndex = index
                    }
                }

                val status = when {
                    bestScore >= 80 -> GOOD
                    bestScore >= 40 -> CLOSE
                    else -> WRONG
                }
                spokenIndex = if (bestScore >= 40) bestIndex + 1 else spokenIndex + 1
                status
            }
        }
    }
}
