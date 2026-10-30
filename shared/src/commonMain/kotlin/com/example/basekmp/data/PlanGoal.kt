package com.example.basekmp.data

import kotlinx.serialization.Serializable

@Serializable
data class PlanGoal(
    val id: String,
    val title: String,
    val titles: Map<String, String> = emptyMap(),
    val emoji: String,
    val stages: List<PlanStage> = emptyList(),
    val learnTitles: Map<String, Map<String, String>> = emptyMap()
) {
    fun displayTitle(learn: String): String {
        return GoalPlanFormat.title(title, titles, learnTitles[learn])
    }

    fun hasContent(learn: String): Boolean {
        return stages.any { it.languages.contains(learn) }
    }

    fun stageFor(day: Int): PlanStage? {
        return stages.getOrNull(GoalPlan.stageIndex(day))
    }
}
