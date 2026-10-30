package com.example.basekmp.data

import kotlin.random.Random

/**
 * 100% faithful port of GoalPlanSession.java.
 * Builds quiz and speaking question lists for 30-day plan daily sessions.
 */
object GoalPlanSession {

    fun quiz(
        level: LevelBank,
        nativeLang: String,
        learn: String,
        count: Int,
        seen: Set<String>,
        random: Random = Random.Default,
        day: Int = 1,
        seenContent: Set<String> = emptySet()
    ): List<PlanQuestion> {
        val translationMatches = level.translation.filter { it.source.containsKey(nativeLang) }
        val transPool = if (translationMatches.isNotEmpty()) translationMatches else level.translation

        val transQuota = if (transPool.isNotEmpty()) {
            val desired = if (day % 2 == 1) count / 2 else count - (count / 2)
            desired.coerceAtMost(transPool.size)
        } else 0

        val vocabQuota = (count - transQuota).coerceAtMost(level.vocab.size)

        val pickedVocab = pick(
            items = level.vocab,
            n = vocabQuota,
            seen = seen,
            seenContent = seenContent,
            random = random,
            id = { it.id },
            content = { vocabKey(it) },
            inNative = { it.question.isEmpty() || it.question.containsKey(nativeLang) }
        )

        val vocabQuestions = pickedVocab.map { vocab ->
            val shuffled = shuffleOptions(vocab.options, vocab.answer, random)
            val questionText = if (vocab.question.isNotEmpty()) {
                text(vocab.question, nativeLang)
            } else {
                "Which word means \"${text(vocab.meaning, nativeLang)}\"?"
            }

            PlanQuestion(
                question = PathLessonContent.Question(
                    kind = PathLessonContent.Kind.VOCABULARY,
                    text = questionText,
                    options = shuffled.first,
                    answer = shuffled.second
                ),
                itemId = vocab.id,
                content = vocabKey(vocab)
            )
        }

        val pickedTrans = pick(
            items = transPool,
            n = transQuota,
            seen = seen,
            seenContent = seenContent,
            random = random,
            id = { it.id },
            content = { translationKey(it) },
            inNative = { it.source.containsKey(nativeLang) }
        )

        val transQuestions = pickedTrans.map { trans ->
            val shuffled = shuffleOptions(trans.options, trans.answer, random)
            PlanQuestion(
                question = PathLessonContent.Question(
                    kind = PathLessonContent.Kind.TRANSLATION,
                    text = "\"${text(trans.source, nativeLang)}\"",
                    options = shuffled.first,
                    answer = shuffled.second
                ),
                itemId = trans.id,
                content = translationKey(trans)
            )
        }

        // Interleave vocab and translation
        val result = mutableListOf<PlanQuestion>()
        val maxLen = maxOf(vocabQuestions.size, transQuestions.size)
        for (i in 0 until maxLen) {
            vocabQuestions.getOrNull(i)?.let { result.add(it) }
            transQuestions.getOrNull(i)?.let { result.add(it) }
        }
        return result
    }

    fun speaking(
        level: LevelBank,
        nativeLang: String,
        count: Int,
        seen: Set<String>,
        random: Random = Random.Default,
        seenContent: Set<String> = emptySet()
    ): List<PlanQuestion> {
        val picked = pick(
            items = level.speaking,
            n = count,
            seen = seen,
            seenContent = seenContent,
            random = random,
            id = { it.id },
            content = { speakingKey(it) },
            inNative = { it.task.containsKey(nativeLang) }
        )

        return picked.map { item ->
            PlanQuestion(
                question = PathLessonContent.Question(
                    kind = PathLessonContent.Kind.SPEAKING,
                    text = text(item.task, nativeLang),
                    options = emptyList(),
                    answer = 0,
                    start = item.start
                ),
                itemId = item.id,
                model = item.say,
                alternatives = item.alt,
                content = speakingKey(item)
            )
        }
    }

    private fun text(m: Map<String, String>, lang: String): String {
        return m[lang] ?: m["en"] ?: m.values.firstOrNull() ?: ""
    }

    private fun norm(s: String): String {
        return s.lowercase().filter { it.isLetterOrDigit() }
    }

    private fun vocabKey(v: VocabItem): String {
        val ans = v.options.getOrNull(v.answer) ?: ""
        return "v:" + norm(ans)
    }

    private fun translationKey(t: TranslationItem): String {
        val ans = t.options.getOrNull(t.answer) ?: ""
        return "t:" + norm(ans)
    }

    private fun speakingKey(s: SpeakingItem): String {
        return "s:" + norm(s.say)
    }

    private fun <T> pick(
        items: List<T>,
        n: Int,
        seen: Set<String>,
        seenContent: Set<String>,
        random: Random,
        id: (T) -> String,
        content: (T) -> String,
        inNative: (T) -> Boolean
    ): List<T> {
        val (seenItems, unseenItems) = items.partition { seen.contains(id(it)) }
        val (unseenFresh, unseenOld) = unseenItems.partition { !seenContent.contains(content(it)) && inNative(it) }

        val pool = (unseenFresh.shuffled(random) + unseenOld.shuffled(random) + seenItems.shuffled(random))
        return pool.take(n.coerceAtLeast(0))
    }

    private fun shuffleOptions(options: List<String>, answer: Int, random: Random): Pair<List<String>, Int> {
        val indexed = options.indices.toList().shuffled(random)
        val shuffledOptions = indexed.map { options[it] }
        val newAnswerIndex = indexed.indexOf(answer)
        return Pair(shuffledOptions, newAnswerIndex)
    }
}
