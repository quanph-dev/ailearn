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
    private const val REWRITE_WITHIN_MS: Long = 9000L
    private const val SYSTEM_PROMPT =
        "You are a warm, encouraging language tutor. Keep replies short (1-3 sentences). Gently correct mistakes, give one natural example, and end by inviting the learner to keep going. Use simple, clear language. Reply in plain conversational text — no markdown, asterisks, or bullet points."
    private const val MOOD_LINE =
        "\nBefore your reply, write exactly one tag that judges the student's LAST message: [great] if it was correct and natural, [ok] if it was understandable but had small mistakes, [oops] if it had clear mistakes, was off-topic or was not in the language being practised, or [hi] if the student has not said anything yet. Write the tag first, then a space, then your reply. The tag is hidden from the student."

    private val MOOD_TAG = Regex("^\\s*\\[(great|ok|oops|hi)]\\s*", RegexOption.IGNORE_CASE)
    private val QUOTED = Regex("[\"“”„«][^\"“”„«»]*[\"“”»]|「[^」]*」|『[^』]*』")

    private val nativeReminder: Map<String, String> = mapOf(
        "English" to "(Please answer in English.)",
        "Spanish" to "(Por favor, responde en español.)",
        "French" to "(Réponds en français, s'il te plaît.)",
        "Turkish" to "(Lütfen Türkçe cevap verin.)",
        "Italian" to "(Per favore, rispondi in italiano.)",
        "German" to "(Bitte antworte auf Deutsch.)",
        "Portuguese (Brazilian)" to "(Por favor, responda em português.)",
        "Vietnamese" to "(Vui lòng trả lời bằng tiếng Việt.)",
        "Urdu" to "(براہ کرم اردو میں جواب دیں)",
        "Hindi" to "(कृपया हिंदी में जवाब दें)",
        "Arabic" to "(من فضلك أجب باللغة العربية)",
        "Chinese (Mandarin)" to "（请用中文回答）",
        "Japanese" to "（日本語で答えてください）",
        "Korean" to "(한국어로 답해 주세요)",
        "Russian" to "(Пожалуйста, отвечайте по-русски)",
        "Thai" to "(กรุณาตอบเป็นภาษาไทย)"
    )

    private val scriptLanguages: Set<String> = setOf(
        "Urdu", "Arabic", "Hindi", "Chinese (Mandarin)", "Japanese", "Korean", "Russian", "Thai"
    )

    private val functionWords: Map<String, Set<String>> = mapOf(
        "English" to setOf(
            "the", "you", "we", "will", "is", "are", "can", "it", "how", "now", "of", "this",
            "that", "your", "what", "when", "with", "for", "and", "be", "let's", "i", "my",
            "our", "say", "try", "today", "learn", "it's", "which", "use", "out", "they",
            "there", "have", "has", "was", "would", "could", "should", "phrase", "word"
        ),
        "Spanish" to setOf(
            "el", "los", "las", "que", "y", "es", "para", "con", "por", "como", "puedes",
            "tu", "del", "esta", "este", "muy", "vamos", "hoy"
        ),
        "French" to setOf(
            "le", "les", "des", "est", "et", "pour", "avec", "vous", "tu", "nous", "une",
            "dans", "ce", "cette", "aujourd'hui", "peux"
        ),
        "German" to setOf(
            "der", "die", "das", "und", "ist", "du", "wir", "mit", "für", "ein", "eine",
            "nicht", "heute", "kannst", "wie"
        ),
        "Italian" to setOf(
            "il", "gli", "che", "è", "per", "con", "puoi", "oggi", "una", "questo",
            "questa", "come", "sono"
        ),
        "Portuguese (Brazilian)" to setOf(
            "o", "os", "as", "que", "é", "para", "com", "você", "uma", "hoje", "pode",
            "como", "isso", "este", "esta"
        )
    )

    data class ScenarioBrief(
        val description: String,
        val vocabulary: String
    )

    fun splitMood(text: String): Pair<RobotMood?, String> {
        val match = MOOD_TAG.find(text) ?: return Pair(null, text)
        val tag = match.groupValues.getOrNull(1)?.lowercase() ?: ""
        val mood = when (tag) {
            "great", "hi" -> RobotMood.HAPPY
            "ok" -> RobotMood.NEUTRAL
            "oops" -> RobotMood.SAD
            else -> RobotMood.NEUTRAL
        }
        val cleanText = text.substring(match.range.last + 1).replace(Regex("\\[(great|ok|oops|hi)]\\s*", RegexOption.IGNORE_CASE), "").trim()
        return Pair(mood, cleanText)
    }

    suspend fun reply(
        history: List<ChatMessage>,
        voice: String? = null,
        mode: String? = null,
        language: String = "English",
        nativeLanguage: String = "Urdu",
        lessonTitle: String? = null,
        lessonLevel: String? = null,
        replyInNative: Boolean = false,
        withMood: Boolean = false,
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
            val started = com.example.basekmp.currentTimeMillis()
            val response = LlmApi.chat(
                messages = msgs,
                system = systemPrompt,
                maxTokens = 600
            ) ?: return@withContext null

            var firstClean = clean(response) ?: return@withContext null
            if (!replyInNative && nativeLanguage != language && strayLearnLanguage(firstClean, language) &&
                com.example.basekmp.currentTimeMillis() - started <= REWRITE_WITHIN_MS
            ) {
                val retryMsgs = msgs +
                    LlmApi.Msg("assistant", firstClean) +
                    LlmApi.Msg("user", rewriteAsk(nativeLanguage, language))
                val retryResp = LlmApi.chat(
                    messages = retryMsgs,
                    system = systemPrompt,
                    maxTokens = 600
                )
                val retryClean = retryResp?.let(::clean)?.takeUnless { strayLearnLanguage(it, language) }
                if (retryClean != null) {
                    val moodPrefix = MOOD_TAG.find(firstClean)?.value?.trim()
                    firstClean = if (moodPrefix != null && MOOD_TAG.find(retryClean) == null) {
                        "$moodPrefix $retryClean"
                    } else {
                        retryClean
                    }
                }
            }
            firstClean
        } catch (_: Exception) {
            null
        }
    }

    fun request(
        history: List<ChatMessage>,
        voice: String? = null,
        mode: String? = null,
        language: String = "English",
        nativeLanguage: String = "Urdu",
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

        val finalMsgs = if (!replyInNative) {
            withNativeReminder(msgs, language, nativeLanguage)
        } else {
            msgs
        }

        return Pair(systemPrompt, finalMsgs)
    }

    private fun withNativeReminder(
        messages: List<LlmApi.Msg>,
        language: String,
        nativeLanguage: String
    ): List<LlmApi.Msg> {
        val reminder = nativeReminder[nativeLanguage] ?: return messages
        val lastMsg = messages.lastOrNull()?.takeIf { it.role == "user" } ?: return messages
        return if (nativeLanguage == language && language == "English") {
            messages
        } else {
            messages.dropLast(1) + lastMsg.copy(content = "${lastMsg.content}\n\n$reminder")
        }
    }

    fun strayLearnLanguage(text: String, language: String): Boolean {
        val set = functionWords[language] ?: return false
        val unquoted = QUOTED.replace(MOOD_TAG.replace(text, ""), " ")
        val sentences = unquoted.split(Regex("[.!?\\n]+"))
        for (sentence in sentences) {
            val words = sentence.lowercase().split(Regex("[^\\p{L}']+")).filter { it.isNotEmpty() }
            if (words.size >= 5) {
                val hits = words.count { it in set }
                if (hits * 10 >= words.size * 3) {
                    return true
                }
            }
        }
        return false
    }

    private fun rewriteAsk(nativeLanguage: String, language: String): String {
        val reminder = nativeReminder[nativeLanguage] ?: ""
        return "Rewrite your last message for me: every word of yours in $nativeLanguage, written in $nativeLanguage's own script; only the $language words or phrases you teach stay in $language, inside quotation marks. No pronunciation spellings. Write only the rewritten message. $reminder"
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
        val cleaned = Regex("\\*+").replace(str, "").trim()
        return if (cleaned.isNotEmpty()) cleaned else null
    }

    private fun beginCue(language: String): String = when (language) {
        "Korean" -> "시작해요."
        "Vietnamese" -> "Bắt đầu nào."
        "Russian" -> "Давайте начнём."
        "Portuguese (Brazilian)" -> "Vamos começar."
        "Chinese (Mandarin)" -> "开始吧。"
        "Japanese" -> "始めましょう。"
        "Italian" -> "Cominciamo."
        "Spanish" -> "Empecemos."
        "Thai" -> "เริ่มกันเลย"
        "Urdu" -> "شروع کریں۔"
        "Hindi" -> "शुरू कीजिए।"
        "Turkish" -> "Başlayalım."
        "Arabic" -> "لنبدأ."
        "French" -> "Commençons."
        "German" -> "Fangen wir an."
        else -> "Begin."
    }

    private fun greetingCue(nativeLanguage: String): String = when (nativeLanguage) {
        "Korean" -> "안녕하세요!"
        "Vietnamese" -> "Xin chào!"
        "Russian" -> "Здравствуйте!"
        "Portuguese (Brazilian)" -> "Olá!"
        "Chinese (Mandarin)" -> "你好！"
        "Japanese" -> "こんにちは！"
        "Italian" -> "Ciao!"
        "Spanish" -> "¡Hola!"
        "Thai" -> "สวัสดี"
        "Urdu" -> "السلام علیکم!"
        "Hindi" -> "नमस्ते!"
        "Turkish" -> "Merhaba!"
        "Arabic" -> "مرحبًا!"
        "French" -> "Bonjour !"
        "German" -> "Hallo!"
        else -> "Hello!"
    }

    private fun personaLine(voice: String?, language: String, nativeLanguage: String, greet: Boolean): String {
        val persona = when (voice?.lowercase()) {
            "female" -> "a friendly, encouraging female $language tutor"
            "male" -> "a friendly, encouraging male $language tutor"
            "robot" -> "a cheerful robot $language tutor"
            else -> "a friendly, encouraging $language tutor"
        }
        val greetText = if (greet) {
            "The conversation is just starting: greet the student warmly in $nativeLanguage, say you will help them practise $language, teach one useful everyday $language phrase and invite them to say something. "
        } else ""

        val scriptNote = if (nativeLanguage != language) {
            ", and whenever $language uses a non-Latin script also include an easy romanized pronunciation in parentheses (like \"Ohayō gozaimasu (おはようございます)\")"
        } else ""

        val effectiveNative = if (nativeLanguage == language) language else nativeLanguage
        return "Reply as $persona. ${greetText}For everything the student says: FIRST answer warmly and briefly in $effectiveNative so they always understand you; THEN teach them how to say the key phrase in $language — give the $language phrase$scriptNote — plus one short note on how or when to use it. Keep the whole reply to 2-4 short sentences and finish by inviting the student to try. The language you teach is ALWAYS $language — teach in $language even if earlier messages in this conversation used a different language. "
    }

    private fun nativePersonaLine(voice: String?, nativeLanguage: String, lessonTitle: String?, lessonLevel: String?, isFirstTurn: Boolean, mode: String?): String {
        val persona = when (voice?.lowercase()) {
            "female" -> "a friendly, encouraging female tutor"
            "male" -> "a friendly, encouraging male tutor"
            "robot" -> "a cheerful robot tutor"
            else -> "a friendly, encouraging tutor"
        }
        val scriptRule = "Write $nativeLanguage in its OWN native script, the way it is actually written by native speakers. NEVER romanize or transliterate it into Latin/English letters — for example, write Urdu in Urdu script and Hindi in Devanagari, never as \"aap kaise hain\". Latin letters may appear only inside a word that is genuinely written that way (a brand or a loanword). "
        val langRule = if (mode == null) {
            "Write your ENTIRE reply in $nativeLanguage — every single sentence must be in $nativeLanguage, and never use any other language. $scriptRule"
        } else {
            "Write all of your own words — your conversation, explanations and questions — in $nativeLanguage. Only a short example, correction or translation in the language being learned may appear in that language, and only when the task below asks for it. $scriptRule"
        }

        val sceneRule = if (!lessonTitle.isNullOrBlank()) {
            val level = lessonLevel?.takeIf { it.isNotBlank() } ?: "Intermediate"
            if (isFirstTurn) {
                "This is a short role-play lesson titled \"$lessonTitle\" for a $level learner. Warmly set the scene for \"$lessonTitle\" in $nativeLanguage and invite the student to begin. "
            } else {
                "You are in the middle of a role-play about \"$lessonTitle\" for a $level learner. Stay in character for this scenario, but keep every word in $nativeLanguage. "
            }
        } else if (isFirstTurn) {
            "If the conversation is just starting, greet them warmly in $nativeLanguage and invite them to tell you about their day. "
        } else ""

        return "Reply as $persona having a short lesson chat with a student. ${langRule}Respond naturally and warmly to what the student said, gently encourage them, and finish with one simple question in $nativeLanguage to keep the conversation going. ${sceneRule}Keep it to 2-4 short sentences. Plain conversational text — no markdown, asterisks, or bullet points. "
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
        val lvl = level?.takeIf { it.isNotBlank() } ?: "Intermediate"
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
            return sameLanguageRule(language)
        }
        val extra = if (functionWords.containsKey(language)) {
            " Do not add made-up pronunciation spellings to $language phrases (like \"Gud mawnin\")."
        } else ""
        return "\nLANGUAGE RULE: the student's native language is $nativeLanguage. Write all of your own words — greetings, introductions, explanations, corrections, encouragement and questions — in $nativeLanguage, in EVERY sentence of EVERY message. Only the $language words and phrases you teach, model or say in a role-play are in $language, each inside quotation marks. Write $nativeLanguage in its OWN native script, the way native speakers write it. NEVER romanize or transliterate it into Latin/English letters — for example, write Urdu in Urdu script and Hindi in Devanagari, never as \"aap kaise hain\".$extra"
    }

    private fun sameLanguageRule(language: String): String {
        if (language == "English") return ""
        val extra = if (scriptLanguages.contains(language)) {
            " Write $language in its OWN native script, the way native speakers write it. NEVER romanize or transliterate it into Latin/English letters and add no pronunciation spellings — for example write Urdu in Urdu script and Hindi in Devanagari, never as \"aap kaise hain\"."
        } else ""
        return "\nLANGUAGE RULE: the student speaks $language as their own language. Write EVERY word of EVERY message in $language and never in English, with no English translations or glosses in brackets.$extra"
    }
}
