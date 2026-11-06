package com.semavia.ailearn

import com.semavia.ailearn.data.PlanProgress
import com.semavia.ailearn.data.PlanQuizScore
import com.semavia.ailearn.data.UserPrefs
import com.semavia.ailearn.data.LearningItem
import com.semavia.ailearn.localEpochDayAt
import com.semavia.ailearn.ui.plan.averagePlanSpeakingScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressAccountingTest {
    @Test
    fun streakCountsOncePerLocalDayAndResetsAfterAMissedDay() = withCleanPrefs {
        UserPrefs.recordStreakDay(20_000)
        UserPrefs.recordStreakDay(20_000)
        assertEquals(1, UserPrefs.streakCountState.intValue)

        UserPrefs.recordStreakDay(20_001)
        UserPrefs.recordStreakDay(20_001)
        assertEquals(2, UserPrefs.streakCountState.intValue)

        UserPrefs.recordStreakDay(20_003)
        assertEquals(1, UserPrefs.streakCountState.intValue)
        assertEquals(20_003L, UserPrefs.lastActiveDayState.longValue)

        UserPrefs.recordStreakDay(20_002)
        assertEquals(1, UserPrefs.streakCountState.intValue)
        assertEquals(20_003L, UserPrefs.lastActiveDayState.longValue)
    }

    @Test
    fun storeShareUsesInstalledPackageAndPolicyUrlsStayReadyForLater() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.semavia.language.learning.ai",
            AppLinks.playStoreUrl("com.semavia.language.learning.ai")
        )
        assertTrue(AppLinks.shareMessage("com.semavia.language.learning.ai").contains(AppLinks.playStoreUrl("com.semavia.language.learning.ai")))
        assertEquals("", AppLinks.documentUrl("privacy"))
        assertEquals("", AppLinks.documentUrl("terms"))
        assertNull(AppLinks.supportEmailUrl())
    }

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
    fun legacyDuplicatePracticeRowsAreMergedOnceAndKeepTheirUsefulDetails() = withCleanPrefs {
        val duplicatePracticeHistory = """
            {"version":2,"history":[
              {"title":"Speaking Practice","timestamp":4562,"score":44,"detail":"Speak Sentences","total":0,"level":"Intermediate"},
              {"title":"Speaking Practice","timestamp":3162,"score":44,"detail":"","total":100,"level":"B1 Intermediate"},
              {"title":"Grammar Practice","timestamp":9575,"score":2,"detail":"","total":5,"level":"Intermediate"},
              {"title":"Grammar Practice","timestamp":8120,"score":2,"detail":"","total":5,"level":"B1 Intermediate"},
              {"title":"Vocabulary Practice","timestamp":7498,"score":0,"detail":"Daily life","total":10,"level":"Intermediate"},
              {"title":"Vocabulary Practice","timestamp":7493,"score":0,"detail":"","total":10,"level":"B1 Intermediate"},
              {"title":"Speaking Practice","timestamp":20000,"score":70,"detail":"Speak Sentences","total":0,"level":"Intermediate"}
            ]}
        """.trimIndent()

        assertTrue(UserPrefs.restoreLearningProgress(duplicatePracticeHistory))

        val history = UserPrefs.learningHistoryState.toList()
        assertEquals(4, history.size)
        assertEquals(1, history.count { it.title == "Vocabulary Practice" })
        assertEquals(1, history.count { it.title == "Grammar Practice" })
        assertEquals(2, history.count { it.title == "Speaking Practice" })

        val mergedSpeaking = history.single { it.title == "Speaking Practice" && it.score == 44 }
        assertEquals(100, mergedSpeaking.total)
        assertEquals("Speak Sentences", mergedSpeaking.detail)
        assertEquals("Intermediate", mergedSpeaking.level)
        assertEquals(3162L, mergedSpeaking.timestamp)

        val legacySpeaking = history.single { it.title == "Speaking Practice" && it.score == 70 }
        assertEquals(100, legacySpeaking.total)

        val vocabulary = history.single { it.title == "Vocabulary Practice" }
        assertEquals("Daily life", vocabulary.detail)
        assertEquals("Intermediate", vocabulary.level)
        assertEquals(7493L, vocabulary.timestamp)
    }

    @Test
    fun separateMatchingPracticeSessionsAreCountedSeparately() = withCleanPrefs {
        repeat(2) {
            UserPrefs.recordPracticeDone(
                feature = "vocabulary",
                score = 4,
                total = 5,
                learnedMinutes = 6,
                activityEpochDay = 20_000,
                detail = "Daily life",
                level = "Intermediate"
            )
        }

        assertEquals(2, UserPrefs.learningHistoryState.count { it.title == "Vocabulary Practice" })
        assertEquals(12, UserPrefs.learnedMinutesState.intValue)
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
    fun completingADayUnlocksTheNextDayAndSurvivesRestore() = withCleanPrefs {
        val plan = PlanProgress()
        plan.start(goal = "conversation", name = "Learner")
        assertFalse(plan.isUnlocked(2))
        plan.markDone(1, PlanProgress.Activity.QUIZ)
        assertTrue(plan.completeDay(1, setOf(PlanProgress.Activity.QUIZ)))
        assertTrue(plan.isUnlocked(2))
        assertEquals(0, plan.daysUntilUnlocked(2))
        assertFalse(plan.isUnlocked(3))

        val restored = PlanProgress()
        assertTrue(restored.restore(plan.serialize()))
        assertTrue(restored.isUnlocked(2))
        assertFalse(restored.isUnlocked(3))
    }

    @Test
    fun alreadyCompletedDayTwoUnlocksDayThree() {
        val plan = PlanProgress()
        val today = currentLocalEpochDay()
        assertTrue(plan.restore("""{"version":1,"goalId":"conversation","startedEpochDay":$today,"completedDays":[1,2]}"""))
        assertTrue(plan.isUnlocked(3))
        assertEquals(3, plan.currentDay())
        assertFalse(plan.isUnlocked(4))
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
