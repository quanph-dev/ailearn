package com.example.basekmp

import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.PlanQuizScore
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.localEpochDayAt
import com.example.basekmp.ui.plan.averagePlanSpeakingScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressAccountingTest {
    @Test
    fun completedPracticeAddsFixedMinutesAndTracksLocalDayAndStreak() = withCleanPrefs {
        UserPrefs.dailyGoalMinutes = 12

        UserPrefs.recordPracticeDone("grammar", score = 3, total = 5, learnedMinutes = 6, activityEpochDay = 20_000)
        assertEquals(6, UserPrefs.learnedMinutesState.intValue)
        assertEquals(6, UserPrefs.minutesToday(20_000))
        assertFalse(UserPrefs.dailyGoalReachedToday(20_000))
        assertEquals(1, UserPrefs.streakCountState.intValue)

        UserPrefs.recordPracticeDone("vocabulary", score = 4, total = 5, learnedMinutes = 6, activityEpochDay = 20_000)
        assertEquals(12, UserPrefs.learnedMinutesState.intValue)
        assertEquals(12, UserPrefs.minutesToday(20_000))
        assertTrue(UserPrefs.dailyGoalReachedToday(20_000))
        assertEquals(1, UserPrefs.streakCountState.intValue)

        UserPrefs.recordPracticeDone("speaking", score = 70, total = 100, learnedMinutes = 6, activityEpochDay = 20_001)
        assertEquals(18, UserPrefs.learnedMinutesState.intValue)
        assertEquals(6, UserPrefs.minutesToday(20_001))
        assertEquals(0, UserPrefs.minutesToday(20_000))
        assertEquals(2, UserPrefs.streakCountState.intValue)

        val snapshot = UserPrefs.serializeLearningProgress()
        assertTrue(UserPrefs.restoreLearningProgress("""{"version":2}"""))
        assertTrue(UserPrefs.restoreLearningProgress(snapshot))
        assertEquals(18, UserPrefs.learnedMinutesState.intValue)
        assertEquals(6, UserPrefs.minutesToday(20_001))
    }

    @Test
    fun oldLearningSnapshotRestoresWithEmptyDailyMinuteFields() = withCleanPrefs {
        assertTrue(UserPrefs.restoreLearningProgress("""{"version":1,"learnedMinutes":37,"streakCount":2}"""))

        assertEquals(37, UserPrefs.learnedMinutesState.intValue)
        assertEquals(0, UserPrefs.minutesToday(20_000))
        assertFalse(UserPrefs.dailyGoalReachedToday(20_000))
    }

    @Test
    fun planStartPreservesEarnedRewardsAndClearsOldPlanProgress() {
        val plan = PlanProgress()
        assertTrue(
            plan.restore(
                """{"version":1,"goalId":"old","xp":150,"diamonds":30,"completedDays":[1],"wordsPerDay":{"1":4}}"""
            )
        )

        plan.start(goal = "new", name = "Learner")

        assertEquals(150, plan.xp)
        assertEquals(30, plan.diamonds)
        assertTrue(plan.completedDays().isEmpty())
        assertEquals(0, plan.words(1))
        assertNull(plan.quizScore(1))
    }

    @Test
    fun dateUnlockedDayCanBeCompletedBeforeEarlierIncompleteDay() = withCleanPrefs {
        val plan = PlanProgress()
        val yesterday = currentLocalEpochDay() - 1L
        assertTrue(
            plan.restore(
                """{"version":1,"goalId":"travel","startedEpochDay":$yesterday}"""
            )
        )

        assertEquals(1, plan.currentDay())
        assertTrue(plan.isUnlocked(2))
        plan.markDone(2, PlanProgress.Activity.VIDEO)

        assertTrue(plan.completeDay(2, setOf(PlanProgress.Activity.VIDEO)))
        assertTrue(plan.completedDays().contains(2))
        assertEquals(1, plan.currentDay())
    }

    @Test
    fun completedPlanQuizPersistsRightTotalAndVocabularyWordsAcrossRestore() = withCleanPrefs {
        val plan = PlanProgress()
        plan.start(goal = "conversation", name = "Learner")
        assertFalse(plan.saveQuizScore(day = 1, correct = 4, total = 6, vocabularyCorrect = 2))
        plan.markDone(1, PlanProgress.Activity.QUIZ)

        assertTrue(plan.saveQuizScore(day = 1, correct = 4, total = 6, vocabularyCorrect = 2))
        assertEquals(PlanQuizScore(correct = 4, total = 6), plan.quizScore(1))
        assertEquals(2, plan.words(1))
        assertEquals(2, UserPrefs.wordsMasteredState.intValue)

        val restored = PlanProgress()
        assertTrue(restored.restore(plan.serialize()))
        assertEquals(PlanQuizScore(correct = 4, total = 6), restored.quizScore(1))
        assertEquals(2, restored.words(1))
        assertFalse(restored.saveQuizScore(day = 1, correct = 4, total = 6, vocabularyCorrect = 2))
    }

    @Test
    fun planActivityMinutesMatchOriginalWeightingAndTalkFloor() {
        val allActivities = PlanProgress.Activity.entries
        val noVideo = allActivities.filter { it != PlanProgress.Activity.VIDEO }

        assertEquals(6, PlanProgress.activityMinutes(PlanProgress.Activity.QUIZ, 30, allActivities))
        assertEquals(9, PlanProgress.activityMinutes(PlanProgress.Activity.QUIZ, 30, noVideo))
        assertEquals(5, PlanProgress.activityMinutes(PlanProgress.Activity.TALK, 10, noVideo))
    }

    @Test
    fun skippedPlanSpeakingQuestionsContributeZeroToAverage() {
        assertEquals(40, averagePlanSpeakingScore(listOf(80, 0), questionCount = 2))
        assertEquals(0, averagePlanSpeakingScore(emptyList(), questionCount = 0))
    }

    @Test
    fun timestampEpochDayUsesSameLocalCalendarAsCurrentDay() {
        assertEquals(currentLocalEpochDay(), localEpochDayAt(currentTimeMillis()))
    }

    private inline fun <T> withCleanPrefs(block: () -> T): T {
        val original = UserPrefs.serializeLearningProgress()
        return try {
            assertTrue(UserPrefs.restoreLearningProgress("""{"version":2}"""))
            block()
        } finally {
            UserPrefs.restoreLearningProgress(original)
        }
    }
}
