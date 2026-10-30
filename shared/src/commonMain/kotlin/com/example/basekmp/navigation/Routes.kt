package com.example.basekmp.navigation

/**
 * Complete route constants matching Engla decompiled Routes.java with 28 routes
 * plus typed route constructor helpers.
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val LANGUAGE = "language"
    const val AI_TUTOR = "ai_tutor"
    const val TODAY_LESSON = "today_lesson"
    const val LESSON_RUN = "lesson_run/{title}/{level}"
    const val GRAMMAR_SETUP = "grammar_setup"
    const val GRAMMAR_QUIZ = "grammar_quiz/{count}"
    const val SPEAKING_SETUP = "speaking_setup"
    const val SPEAKING_CARDS = "speaking_cards/{count}"
    const val VOCAB_SETUP = "vocab_setup"
    const val VOCAB_CARDS = "vocab_cards/{count}"
    const val PROGRESS_DETAIL = "progress_detail/{kind}"
    const val LEARNING_HISTORY = "learning_history"
    const val LEGAL = "legal/{kind}"
    const val PATH_LESSON = "path_lesson/{id}"
    const val VIDEO_SHADOWING = "video_shadowing"
    const val VIDEO_SHADOWING_PLAYER = "video_shadowing_player/{id}"
    const val SAVED_WORDS = "saved_words"
    const val PLAN = "plan"
    const val PLAN_SETUP = "plan_setup"
    const val PLAN_GENERATING = "plan_generating"
    const val PLAN_STAGE = "plan_stage/{index}"
    const val PLAN_DAY = "plan_day/{day}"
    const val PLAN_SESSION = "plan_session/{day}/{kind}"
    const val PLAN_VIDEO = "plan_video/{day}/{id}"
    const val PLAN_TALK = "plan_talk/{day}/{title}/{level}"

    // Route construction utilities
    fun lessonRun(title: String, level: String): String = "lesson_run/$title/$level"
    fun grammarQuiz(count: Int): String = "grammar_quiz/$count"
    fun speakingCards(count: Int): String = "speaking_cards/$count"
    fun vocabCards(count: Int): String = "vocab_cards/$count"
    fun progressDetail(kind: String): String = "progress_detail/$kind"
    fun legal(kind: String): String = "legal/$kind"
    fun pathLesson(id: String): String = "path_lesson/$id"
    fun videoShadowingPlayer(id: String): String = "video_shadowing_player/$id"
    fun planStage(index: Int): String = "plan_stage/$index"
    fun planDay(day: Int): String = "plan_day/$day"
    fun planSession(day: Int, kind: String): String = "plan_session/$day/$kind"
    fun planVideo(day: Int, id: String): String = "plan_video/$day/$id"
    fun planTalk(day: Int, title: String, level: String): String = "plan_talk/$day/$title/$level"
}
