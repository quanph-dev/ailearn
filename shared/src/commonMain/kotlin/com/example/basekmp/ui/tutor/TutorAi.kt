package com.example.basekmp.ui.tutor

import com.example.basekmp.components.RobotMood
import com.example.basekmp.data.LlmApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * High-fidelity Prompt Engine and Tutor Personality Controller.
 * 100% migrated from decompiled TutorAi.java.
 * Zero dummy data; communicates with real serverless LLM backend.
 */
object TutorAi {
    const val HISTORY_WINDOW: Int = 12
    private const val SYSTEM_PROMPT =
        "You are a warm, encouraging language tutor. Keep replies short (1-3 sentences). Gently correct mistakes, give one natural example, and end by inviting the learner to keep going. Use simple, clear language. Reply in plain conversational text — no markdown, asterisks, or bullet points."
    private const val MOOD_LINE =
        "\nBefore your reply, write exactly one tag that judges the student's LAST message: [great] if it was correct and natural, [ok] if it was understandable but had small mistakes, [oops] if it had clear mistakes, was off-topic or was not in the language being practised, or [hi] if the student has not said anything yet. Write the tag first, then a space, then your reply. The tag is hidden from the student."

    private val MOOD_TAG = Regex("^\\s*\\[(great|ok|oops|hi)]\\s*", RegexOption.IGNORE_CASE)

    data class ScenarioBrief(
        val description: String,
        val vocabulary: String
    )

    fun splitMood(text: String): Pair<RobotMood?, String> {
        val match = MOOD_TAG.find(text) ?: return Pair(null, text)
        val tag = match.groupValues.getOrNull(1)?.lowercase() ?: ""
        val mood = when (tag) {
            "great" -> RobotMood.SUPER
            "ok" -> RobotMood.GOOD
            "oops" -> RobotMood.POOR
            "hi" -> RobotMood.GOOD
            else -> RobotMood.AVERAGE
        }
        val cleanText = text.substring(match.range.last + 1).replace(Regex("\\[(great|ok|oops|hi)]\\s*", RegexOption.IGNORE_CASE), "").trim()
        return Pair(mood, cleanText)
    }

    suspend fun reply(
        history: List<ChatMessage>,
        voice: String? = null,
        mode: String? = null,
        language: String = "English",
        nativeLanguage: String = "English",
        lessonTitle: String? = null,
        lessonLevel: String? = null,
        replyInNative: Boolean = false,
        withMood: Boolean = true,
        studentName: String? = null,
        greetInNative: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        try {
            val (systemPrompt, msgs) = request(
                history = history,
                voice = voice,
                mode = mode,
                language = language,
                nativeLanguage = nativeLanguage,
                lessonTitle = lessonTitle,
                lessonLevel = lessonLevel,
                replyInNative = replyInNative,
                withMood = withMood,
                studentName = studentName,
                greetInNative = greetInNative
            )
            val response = LlmApi.chat(
                messages = msgs,
                system = systemPrompt,
                maxTokens = 600
            ) ?: return@withContext null

            clean(response)
        } catch (_: Exception) {
            null
        }
    }

    fun request(
        history: List<ChatMessage>,
        voice: String? = null,
        mode: String? = null,
        language: String = "English",
        nativeLanguage: String = "English",
        lessonTitle: String? = null,
        lessonLevel: String? = null,
        replyInNative: Boolean = false,
        withMood: Boolean = false,
        studentName: String? = null,
        greetInNative: Boolean = false
    ): Pair<String, List<LlmApi.Msg>> {
        val isFirstTurn = history.isEmpty()
        val isGreetingInNative = greetInNative && lessonTitle.isNullOrBlank()

        val sb = StringBuilder(SYSTEM_PROMPT).append("\n")
        if (replyInNative) {
            sb.append(nativePersonaLine(voice, nativeLanguage, lessonTitle, lessonLevel, isFirstTurn, mode))
            sb.append(modeLine(mode, language, nativeLanguage))
        } else {
            sb.append(personaLine(voice, language, nativeLanguage, isFirstTurn && isGreetingInNative))
            sb.append(modeLine(mode, language, nativeLanguage))
            sb.append(lessonLine(lessonTitle, lessonLevel, language, nativeLanguage, isFirstTurn))
        }

        if (withMood) {
            sb.append(MOOD_LINE)
        }

        if (!studentName.isNullOrBlank()) {
            sb.append("\nThe student's name is \"").append(studentName).append("\". Greet them by name at the start, and use it now and then.")
        }

        if (!replyInNative) {
            sb.append(languageRule(language, nativeLanguage))
        }

        val systemPrompt = sb.toString()
        val recentHistory = history.takeLast(HISTORY_WINDOW)
        val msgs = mutableListOf<LlmApi.Msg>()
        for (m in recentHistory) {
            msgs.add(LlmApi.Msg(if (m.fromUser) "user" else "assistant", m.text))
        }

        if (msgs.isEmpty()) {
            val cue = if (isGreetingInNative) {
                greetingCue(nativeLanguage)
            } else if (replyInNative || nativeLanguage != language) {
                beginCue(nativeLanguage)
            } else {
                beginCue(language)
            }
            msgs.add(LlmApi.Msg("user", cue))
        }

        return Pair(systemPrompt, msgs)
    }

    suspend fun hint(
        history: List<ChatMessage>,
        language: String = "English",
        lessonTitle: String? = null,
        lessonLevel: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val transcript = history.takeLast(8).joinToString("\n") {
                (if (it.fromUser) "Student: " else "Tutor: ") + it.text
            }
            val scenario = if (!lessonTitle.isNullOrBlank()) {
                " for the lesson \"$lessonTitle\" (${lessonLevel ?: "Intermediate"} level)"
            } else ""

            val prompt = buildString {
                append("You are helping a language learner practise$scenario. Based on the ")
                append("conversation so far, suggest ONE short, natural sentence the STUDENT could ")
                append("say next in $language to continue the conversation. ")
                append("Reply with ONLY that sentence — no quotes, no labels, no explanation.\n\n")
                if (transcript.isNotEmpty()) {
                    append(transcript).append('\n')
                }
            }

            val reply = LlmApi.complete(prompt, maxTokens = 500) ?: return@withContext null
            val cleanStr = reply.replace("*", "").trim().trim('"')
            if (cleanStr.isNotEmpty()) cleanStr else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun scenarioBrief(
        title: String,
        level: String,
        language: String,
        nativeLanguage: String = "English"
    ): ScenarioBrief? = withContext(Dispatchers.IO) {
        try {
            val prompt = "A learner is about to practise a $language conversation lesson titled \"$title\" ($level level). Reply with EXACTLY two lines and nothing else:\nLine 1: one short sentence in $nativeLanguage (the learner's own language, in its own script) describing what they will practise.\nLine 2: 3 to 5 key $language words or phrases for this scenario, separated by commas.\nNo labels, no numbering, no markdown."
            val reply = LlmApi.complete(prompt, maxTokens = 500) ?: return@withContext null
            val lines = reply.replace("*", "").lines().map { it.trim() }.filter { it.isNotEmpty() }
            val desc = lines.getOrNull(0) ?: return@withContext null
            val vocabLine = lines.getOrNull(1)?.replace(Regex("(?i)^vocabulary[^:]*:\\s*"), "")?.trim() ?: ""
            ScenarioBrief(description = desc, vocabulary = vocabLine)
        } catch (_: Exception) {
            null
        }
    }

    private fun clean(str: String): String? {
        val cleaned = str.replace("*", "").trim()
        return if (cleaned.isNotEmpty()) cleaned else null
    }

    private fun beginCue(language: String): String = when (language.lowercase()) {
        "korean" -> "시작해요."
        "vietnamese" -> "Bắt đầu nào."
        "russian" -> "Давайте начнём."
        "portuguese" -> "Vamos começar."
        "chinese" -> "开始吧。"
        "japanese" -> "始めましょう。"
        "italian" -> "Cominciamo."
        "spanish" -> "Empecemos."
        "thai" -> "เริ่มกันเลย"
        "urdu" -> "شروع کریں۔"
        "hindi" -> "शुरू कीजिए।"
        "turkish" -> "Başlayalım."
        "arabic" -> "لنبدأ."
        "french" -> "Commençons."
        "german" -> "Fangen wir an."
        else -> "Begin."
    }

    private fun greetingCue(nativeLanguage: String): String = when (nativeLanguage.lowercase()) {
        "korean" -> "안녕하세요!"
        "vietnamese" -> "Xin chào!"
        "russian" -> "Здравствуйте!"
        "portuguese" -> "Olá!"
        "chinese" -> "你好！"
        "japanese" -> "こんにちは！"
        "italian" -> "Ciao!"
        "spanish" -> "¡Hola!"
        "thai" -> "สวัสดี"
        "urdu" -> "السلام علیکم!"
        "hindi" -> "नमस्ते!"
        "turkish" -> "Merhaba!"
        "arabic" -> "مرحبًا!"
        "french" -> "Bonjour !"
        "german" -> "Hallo!"
        else -> "Hello!"
    }

    private fun personaLine(voice: String?, language: String, nativeLanguage: String, greet: Boolean): String {
        val persona = when (voice?.lowercase()) {
            "female", "emma" -> "a friendly, encouraging female $language tutor named Emma"
            "male", "james" -> "a friendly, patient male $language tutor named James"
            "neutral", "alex" -> "a casual, modern $language tutor named Alex"
            "robot", "sparky" -> "a cheerful robot $language tutor named Sparky"
            else -> "a friendly, encouraging $language tutor"
        }
        val greetText = if (greet) {
            "The conversation is just starting: greet the student warmly in $nativeLanguage, say you will help them practise $language, teach one useful everyday $language phrase and invite them to say something. "
        } else ""

        val scriptNote = if (nativeLanguage != language) {
            ", and whenever $language uses a non-Latin script also include an easy romanized pronunciation in parentheses"
        } else ""

        return "Reply as $persona. $greetText For everything the student says: FIRST answer warmly and briefly in $nativeLanguage so they always understand you; THEN teach them how to say the key phrase in $language — give the $language phrase$scriptNote — plus one short note on how or when to use it. Keep the whole reply to 2-4 short sentences and finish by inviting the student to try. The language you teach is ALWAYS $language — teach in $language even if earlier messages in this conversation used a different language. "
    }

    private fun nativePersonaLine(voice: String?, nativeLanguage: String, lessonTitle: String?, lessonLevel: String?, isFirstTurn: Boolean, mode: String?): String {
        val persona = when (voice?.lowercase()) {
            "female", "emma" -> "a friendly, encouraging female tutor named Emma"
            "male", "james" -> "a patient male tutor named James"
            "robot", "sparky" -> "a cheerful robot tutor named Sparky"
            else -> "a friendly, encouraging tutor"
        }
        val scriptRule = "Write $nativeLanguage in its OWN native script, the way it is actually written by native speakers. NEVER romanize or transliterate it into Latin/English letters. "
        val langRule = if (mode == null) {
            "Write your ENTIRE reply in $nativeLanguage — every single sentence must be in $nativeLanguage, and never use any other language. $scriptRule"
        } else {
            "Write all of your own words in $nativeLanguage. Only a short example or correction in the language being learned may appear in that language. $scriptRule"
        }

        val sceneRule = if (!lessonTitle.isNullOrBlank()) {
            val level = lessonLevel ?: "Intermediate"
            if (isFirstTurn) {
                "This is a short role-play lesson titled \"$lessonTitle\" for a $level learner. Warmly set the scene for \"$lessonTitle\" in $nativeLanguage and invite the student to begin. "
            } else {
                "You are in the middle of a role-play about \"$lessonTitle\" for a $level learner. Stay in character for this scenario, but keep every word in $nativeLanguage. "
            }
        } else if (isFirstTurn) {
            "If the conversation is just starting, greet them warmly in $nativeLanguage and invite them to tell you about their day. "
        } else ""

        return "Reply as $persona having a short lesson chat with a student. $langRule Respond naturally and warmly to what the student said, gently encourage them, and finish with one simple question in $nativeLanguage to keep the conversation going. $sceneRule Keep it to 2-4 short sentences. Plain conversational text — no markdown, asterisks, or bullet points. "
    }

    private fun modeLine(mode: String?, language: String, nativeLanguage: String): String {
        if (mode == null) return ""
        return when {
            mode.contains("Spot", ignoreCase = true) ->
                " Review the student's messages in this conversation and clearly point out and explain any language mistakes. If there are no mistakes, say what they did well; if they haven't written anything to check yet, warmly invite them to send a sentence you can look over."
            mode.contains("Fix", ignoreCase = true) ->
                " Correct the student's most recent original sentence, show the corrected version, then briefly say what changed and why. If they haven't written a sentence to fix yet, warmly ask them to type one."
            mode.contains("Explain", ignoreCase = true) ->
                " Explain the meaning, usage and any nuance of the student's most recent message; if they only asked you to explain something, explain your previous message instead. Keep it clear and give one short example."
            mode.contains("Rephrase", ignoreCase = true) ->
                " Rewrite the student's most recent original sentence the way a natural $language speaker would say it, then give one alternative phrasing. If they haven't written anything to rephrase yet, warmly ask them to type a sentence."
            mode.contains("Quiz", ignoreCase = true) ->
                " Act as a quiz-master: ask the student ONE short $language question at their level — use the vocabulary or grammar from the conversation if there is any, otherwise pick something useful for their level. Ask only the question now and wait for their answer before scoring."
            mode.contains("Example", ignoreCase = true) ->
                " Give 2–3 short, natural $language example sentences, each on its own line. Use the key word or phrase from the student's most recent message if they gave one; otherwise pick a useful everyday word or phrase for their level and give examples with it."
            mode.contains("Translate", ignoreCase = true) ->
                " Translate the student's most recent message: if it is in $language, translate it into clear $nativeLanguage; otherwise translate it into natural $language. Show only the translation. If there is nothing to translate yet, warmly ask them what they would like translated."
            mode.contains("Roleplay", ignoreCase = true) ->
                " Start a short role-play in $language. Use the student's most recent message as the scene or topic if they gave one; otherwise pick a common everyday situation (ordering at a café, shopping, meeting someone new). Stay in character, keep replies to 1–2 sentences, and always end with a question to keep the conversation going."
            else -> ""
        }
    }

    private fun lessonLine(title: String?, level: String?, language: String, nativeLanguage: String, isFirstTurn: Boolean): String {
        if (title.isNullOrBlank()) return ""
        val lvl = level ?: "Intermediate"
        return if (isFirstTurn && nativeLanguage != language) {
            " You are running a short $language lesson titled \"$title\" for a $lvl learner. In $nativeLanguage: start with a warm 1-2 sentence intro, teach one key $language phrase for this scenario (the phrase itself in $language, in quotes) with a short $nativeLanguage note on when to use it, then invite the student to try it by asking a simple practice question."
        } else if (isFirstTurn) {
            " You are running a short $language lesson titled \"$title\" for a $lvl learner. Start with a warm 1-2 sentence intro, teach one key phrase for this scenario, then invite the student to try it by asking a simple practice question."
        } else if (nativeLanguage != language) {
            " You are running a role-play lesson on \"$title\" for a $lvl learner. Stay in character for this scenario: say your character's next line in $language, in quotes, with a short $nativeLanguage explanation so the student understands it; gently correct any mistakes and model one natural reply."
        } else {
            " You are running a role-play lesson on \"$title\" for a $lvl learner. Stay in character for this scenario, gently correct any mistakes, model one natural reply, and ask the student the next question to keep the practice going."
        }
    }

    private fun languageRule(language: String, nativeLanguage: String): String {
        if (nativeLanguage == language) {
            if (language == "English") return ""
            return "\nLANGUAGE RULE: the student speaks $language as their own language. Write EVERY word of EVERY message in $language and never in English, with no English translations or glosses in brackets."
        }
        return "\nLANGUAGE RULE: the student's native language is $nativeLanguage. Write all of your own words — greetings, introductions, explanations, corrections, encouragement and questions — in $nativeLanguage, in EVERY sentence of EVERY message. Only the $language words and phrases you teach, model or say in a role-play are in $language, each inside quotation marks."
    }
}
