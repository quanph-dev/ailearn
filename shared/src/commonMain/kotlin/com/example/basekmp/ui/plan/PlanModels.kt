package com.example.basekmp.ui.plan

import com.example.basekmp.data.GoalPlan
import com.example.basekmp.data.PlanCatalogRepository
import com.example.basekmp.data.PlanGoal
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.PlanStage
import com.example.basekmp.data.UserPrefs

/**
 * 30-Day Plan Domain & Navigation Models.
 * 100% migrated from original app's DayState, Assistant, LevelOption, SetupStep, and PlanFlow.
 */

enum class DayState {
    DONE,
    CURRENT,
    OPEN,
    LOCKED
}

data class PathDay(
    val day: Int,
    val state: DayState,
    val started: Boolean = false
)

data class Assistant(
    val id: String,
    val name: String,
    val text: String,
    val voice: String
) {
    companion object {
        val ALL: List<Assistant> = listOf(
            Assistant("emma", "Emma", "Friendly and encouraging", "Female"),
            Assistant("james", "James", "Clear and patient", "Male"),
            Assistant("alex", "Alex", "Casual and modern", "Neutral"),
            Assistant("sparky", "Sparky", "Fun and energetic", "Robot")
        )
    }
}

data class LevelOption(
    val title: String,
    val text: String,
    val setting: String
) {
    companion object {
        val ALL: List<LevelOption> = listOf(
            LevelOption("I'm just starting", "I know little or no English", "A1 Beginner"),
            LevelOption("I know the basics", "I can use simple words and sentences", "A2 Elementary"),
            LevelOption("I can have conversations", "I can talk about familiar topics", "B1 Intermediate"),
            LevelOption("I'm quite confident", "I can discuss complex topics", "C1 Advanced"),
            LevelOption("I'm not sure", "We'll start at a comfortable pace", "Not Sure")
        )
    }
}

enum class SetupStep(val event: String) {
    START("start"),
    NAME("name"),
    GOAL("goal"),
    LEVEL("level"),
    TIME("daily_time"),
    ASSISTANT("ai_assistant")
}

data class GoalOption(
    val id: String,
    val title: String,
    val desc: String
) {
    companion object {
        val ALL: List<GoalOption> = listOf(
            GoalOption("everyday", "Everyday Conversation", "Talk confidently in daily life"),
            GoalOption("travel", "Travel", "Communicate when traveling abroad"),
            GoalOption("work_career", "Work & Career", "Use English at work or in interviews"),
            GoalOption("school_study", "School & Study", "Learn English for classes or studying abroad"),
            GoalOption("language_exams", "Prepare for IELTS, TOEIC, TOEFL", "Get ready for test day"),
            GoalOption("improve_english", "Improve My English", "Build stronger overall English skills"),
            GoalOption("custom", "Something Else", "Tell us your personal goal")
        )
    }
}

object PlanFlow {
    data class Finish(
        val dayCompleted: Boolean,
        val nextActivity: PlanProgress.Activity? = null,
        val totalActivities: Int = 4,
        val activities: List<PlanProgress.Activity> = emptyList()
    )

    data class DayPlan(
        val goal: PlanGoal,
        val stage: PlanStage,
        val dayInStage: Int,
        val available: List<PlanProgress.Activity>,
        val videoId: String? = null
    )

    fun activitiesFor(videoId: String?): List<PlanProgress.Activity> = buildList {
        if (!videoId.isNullOrBlank()) add(PlanProgress.Activity.VIDEO)
        add(PlanProgress.Activity.QUIZ)
        add(PlanProgress.Activity.SPEAKING)
        add(PlanProgress.Activity.TALK)
    }

    suspend fun dayPlan(day: Int): DayPlan? {
        if (day !in 1..GoalPlan.DAYS) return null
        val progress = PlanProgress.get()
        val catalog = PlanCatalogRepository.getCatalog() ?: return null
        val goal = catalog.goal(progress.goalId) ?: return null
        val stage = goal.stageFor(day) ?: return null
        val dayInStage = GoalPlan.dayInStage(day)
        val videoId = GoalPlan.videoFor(stage, day)

        val activities = activitiesFor(videoId)

        return DayPlan(
            goal = goal,
            stage = stage,
            dayInStage = dayInStage,
            available = activities,
            videoId = videoId
        )
    }

    fun unlocked(plan: DayPlan, done: Set<PlanProgress.Activity>, dayFinished: Boolean): Set<PlanProgress.Activity> {
        if (dayFinished) return plan.available.toSet()
        val unlockedSet = mutableSetOf<PlanProgress.Activity>()
        for (activity in plan.available) {
            unlockedSet.add(activity)
            if (!done.contains(activity)) break
        }
        return unlockedSet
    }

    fun next(plan: DayPlan, done: Set<PlanProgress.Activity>): PlanProgress.Activity? {
        for (activity in plan.available) {
            if (!done.contains(activity)) return activity
        }
        return null
    }

    suspend fun finish(
        day: Int,
        activity: PlanProgress.Activity,
        activityEpochDay: Long = UserPrefs.todayEpochDay()
    ): Finish {
        val progress = PlanProgress.get()
        val plan = dayPlan(day) ?: return Finish(
            dayCompleted = false,
            nextActivity = null,
            totalActivities = 0,
            activities = emptyList()
        )
        val activities = plan.available
        if (!progress.isUnlocked(day) || activity !in activities) {
            return Finish(
                dayCompleted = false,
                nextActivity = activities.firstOrNull { it !in progress.done(day) },
                totalActivities = activities.size,
                activities = activities
            )
        }
        val wasDayCompleted = progress.completedDays().contains(day)
        progress.markDone(day, activity)

        val allDone = activities.all { it in progress.done(day) }

        val newlyCompleted = allDone && !wasDayCompleted &&
            progress.completeDay(day, activities.toSet(), activityEpochDay)
        if (newlyCompleted) {
            return Finish(
                dayCompleted = true,
                nextActivity = null,
                totalActivities = plan.available.size,
                activities = plan.available
            )
        }

        val nextAct = activities.firstOrNull { it !in progress.done(day) }
        return Finish(
            dayCompleted = newlyCompleted,
            nextActivity = nextAct,
            totalActivities = activities.size,
            activities = activities
        )
    }
}
