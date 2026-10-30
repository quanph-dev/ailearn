package com.example.basekmp.data

object GoalPlan {
    const val DAYS = 30
    const val DAYS_PER_STAGE = 3

    fun bankLevel(englishLevel: String): String {
        return when {
            englishLevel.startsWith("A1", ignoreCase = true) -> "L1"
            englishLevel.startsWith("A2", ignoreCase = true) -> "L2"
            englishLevel.startsWith("B1", ignoreCase = true) -> "L3"
            englishLevel.startsWith("B", ignoreCase = true) || englishLevel.startsWith("C", ignoreCase = true) -> "L4"
            else -> "UNSURE"
        }
    }

    fun bankLevels(englishLevel: String): List<String> {
        val level = bankLevel(englishLevel)
        val name = when (level) {
            "L3" -> "intermediate"
            "L4" -> "advanced"
            else -> "beginner"
        }
        return listOf(level, name, "L1", "beginner")
    }

    fun learnCode(learnLanguage: String): String {
        val code = Languages.learn(learnLanguage).code
        return if (code == "other") "en" else code
    }

    fun stageIndex(day: Int): Int = (day - 1) / DAYS_PER_STAGE

    fun dayInStage(day: Int): Int = (day - 1) % DAYS_PER_STAGE

    fun videoFor(stage: PlanStage, day: Int): String? {
        val videos = stage.videos
        if (videos.isEmpty()) return null
        return videos[dayInStage(day) % videos.size]
    }
}
