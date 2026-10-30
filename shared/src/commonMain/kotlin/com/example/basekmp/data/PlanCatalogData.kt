package com.example.basekmp.data

import kotlinx.serialization.Serializable

@Serializable
data class PlanCatalogData(
    val version: Int = 1,
    val showDrafts: Boolean = false,
    val quizCounts: List<List<Int>> = emptyList(),
    val speakingCounts: List<List<Int>> = emptyList(),
    val goals: List<PlanGoal> = emptyList()
) {
    fun goal(id: String?): PlanGoal? {
        val catalogId = GoalPlan.canonicalGoalId(id) ?: return null
        return goals.firstOrNull { it.id == catalogId }
    }
}
