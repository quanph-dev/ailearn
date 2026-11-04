package com.example.basekmp.data

import kotlinx.serialization.Serializable

@Serializable
data class PlanStage(
    val id: String,
    val title: String,
    val titles: Map<String, String> = emptyMap(),
    val videos: List<String> = emptyList(),
    val languages: Set<String> = emptySet(),
    val version: Int = 1,
    val learnTitles: Map<String, Map<String, String>> = emptyMap()
) {
    fun displayTitle(learn: String): String {
        return GoalPlanFormat.title(title, titles, learnTitles[learn])
    }

    fun displayTitleLanguage(learn: String): String {
        val language = Strings.getLanguage()
        val translations = learnTitles[learn]
        return when {
            translations?.containsKey(language) == true -> language
            translations?.containsKey("en") == true -> "en"
            titles.containsKey(language) -> language
            else -> "en"
        }
    }
}
