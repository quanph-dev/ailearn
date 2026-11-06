package com.semavia.ailearn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.semavia.ailearn.data.GoalPlan
import com.semavia.ailearn.data.PlanCatalogData
import com.semavia.ailearn.data.PlanGoal
import com.semavia.ailearn.data.PlanStage
import com.semavia.ailearn.data.UserPrefs
import com.semavia.ailearn.data.PlanProgress
import com.semavia.ailearn.data.Strings
import com.semavia.ailearn.ui.plan.PlanFlow

class SharedCommonTest {

    @Test
    fun freshInstallStartsAtFirstOpenIntro() {
        assertEquals(
            BaseRoutes.FirstOpenIntro,
            StartupRouteResolver.resolve(
                languageSetup = false,
                onboardingCompleted = false,
                firstOpenCompleted = false
            )
        )
    }

    @Test
    fun languageSetupBeforeFirstOpenContinuesToFirstOpenIntro() {
        assertEquals(
            BaseRoutes.FirstOpenIntro,
            StartupRouteResolver.resolve(
                languageSetup = true,
                onboardingCompleted = false,
                firstOpenCompleted = false
            )
        )
    }

    @Test
    fun completedFirstOpenWithSurveyBeforeHomeContinuesToOnboarding() {
        assertEquals(
            BaseRoutes.Onboarding,
            StartupRouteResolver.resolve(
                languageSetup = false,
                onboardingCompleted = false,
                firstOpenCompleted = true,
                surveyBeforeHome = true
            )
        )
    }

    @Test
    fun completedFirstOpenWithoutSurveyBeforeHomeContinuesDirectlyToHome() {
        assertEquals(
            BaseRoutes.Home,
            StartupRouteResolver.resolve(
                languageSetup = false,
                onboardingCompleted = false,
                firstOpenCompleted = true,
                surveyBeforeHome = false
            )
        )
    }

    @Test
    fun completedFirstOpenAndOnboardingReturnsHome() {
        assertEquals(
            BaseRoutes.Home,
            StartupRouteResolver.resolve(
                languageSetup = true,
                onboardingCompleted = true,
                firstOpenCompleted = true,
                surveyBeforeHome = true
            )
        )
    }

    @Test
    fun deviceLanguageIsPrioritizedAfterEnglishAndFrench() {
        val languages = sortedBaseLanguages("vi").map { it.code }

        assertEquals(listOf("en", "fr", "vi", "ar"), languages.take(4))
        assertEquals(BaseLanguages.size, languages.distinct().size)
    }

    @Test
    fun brazilianPortugueseDeviceLocaleFallsBackToSupportedPortugueseOption() {
        val languages = sortedBaseLanguages("pt-BR").map { it.code }

        assertEquals(listOf("en", "fr", "pt", "ar"), languages.take(4))
        assertEquals("pt", supportedLanguageCode("pt-BR"))
    }

    @Test
    fun unsupportedDeviceLanguageFallsBackToEnglish() {
        assertEquals("en", supportedLanguageCode("sv-SE"))
    }

    @Test
    fun planVideoForUsesAbsoluteOneBasedDayAndDoesNotThrowForInvalidDay() {
        val stage = PlanStage(
            id = "stage",
            title = "Stage",
            videos = listOf("video-1", "video-2", "video-3")
        )

        assertEquals("video-1", GoalPlan.videoFor(stage, 1))
        assertEquals("video-2", GoalPlan.videoFor(stage, 2))
        assertEquals("video-3", GoalPlan.videoFor(stage, 3))
        assertEquals("video-1", GoalPlan.videoFor(stage, 4))
        assertEquals(null, GoalPlan.videoFor(stage, 0))
        assertEquals(null, GoalPlan.videoFor(stage, -1))
        assertEquals(null, GoalPlan.videoFor(stage, GoalPlan.DAYS + 1))
    }

    @Test
    fun planDaysWithoutVideoDoNotContainAnUnfinishableVideoActivity() {
        assertEquals(
            listOf(PlanProgress.Activity.QUIZ, PlanProgress.Activity.SPEAKING, PlanProgress.Activity.TALK),
            PlanFlow.activitiesFor(null)
        )
        assertEquals(
            listOf(
                PlanProgress.Activity.VIDEO,
                PlanProgress.Activity.QUIZ,
                PlanProgress.Activity.SPEAKING,
                PlanProgress.Activity.TALK
            ),
            PlanFlow.activitiesFor("video-id")
        )
    }

    @Test
    fun finishingVideoUnlocksTheNextActivityInTheSameDay() {
        val activities = PlanFlow.activitiesFor("video-id")
        val plan = PlanFlow.DayPlan(
            goal = PlanGoal(id = "goal", title = "Goal", emoji = "🎯"),
            stage = PlanStage(id = "stage", title = "Stage"),
            dayInStage = 1,
            available = activities,
            videoId = "video-id"
        )
        val completed = setOf(PlanProgress.Activity.VIDEO)

        assertEquals(
            setOf(PlanProgress.Activity.VIDEO, PlanProgress.Activity.QUIZ),
            PlanFlow.unlocked(plan, completed, dayFinished = false)
        )
        assertEquals(PlanProgress.Activity.QUIZ, PlanFlow.next(plan, completed))
    }

    @Test
    fun goalQuestionResourceKeyFormatsItsArgument() {
        assertTrue(Strings.hasTranslationResource("Arabic"))
        assertTrue(Strings.hasTranslationResource("Why do you want to learn %1\$s?"))
        val question = com.semavia.ailearn.data.formatString("Why do you want to learn %1\$s?", arrayOf("Arabic"))
        assertTrue(question.contains("Arabic"))
        assertFalse(question.contains("%1\$s"))
    }

    @Test
    fun finalPlanActivityCompletesDayUsingLoadedPlan() = kotlinx.coroutines.runBlocking {
        val progress = PlanProgress.get()
        val oldPlan = progress.serialize()
        val oldLearning = UserPrefs.serializeLearningProgress()
        try {
            progress.start(goal = "everyday", name = "Test")
            val activities = PlanFlow.activitiesFor("video-id")
            val plan = PlanFlow.DayPlan(
                goal = PlanGoal(id = "everyday", title = "Goal", emoji = "🎯"),
                stage = PlanStage(id = "stage", title = "Stage"),
                dayInStage = 1,
                available = activities,
                videoId = "video-id"
            )
            activities.filterNot { it == PlanProgress.Activity.TALK }.forEach { progress.markDone(1, it) }
            val result = PlanFlow.finish(1, PlanProgress.Activity.TALK, loadedDayPlan = plan)
            assertTrue(result.dayCompleted)
            assertTrue(progress.completedDays().contains(1))
            assertTrue(progress.isUnlocked(2))
        } finally {
            progress.restore(oldPlan)
            UserPrefs.restoreLearningProgress(oldLearning)
        }
    }

    @Test
    fun rtlFollowsTheSelectedInterfaceLanguage() {
        assertTrue(Strings.isRtl("ar"))
        assertTrue(Strings.isRtl("ur"))
        assertFalse(Strings.isRtl("hi"))
    }

    @Test
    fun congratulationsDialogLabelsHaveResourceBackedEntries() {
        listOf("Congratulations", "Correct", "Incorrect").forEach { label ->
            assertTrue(Strings.hasTranslationResource(label), label)
        }
    }

    @Test
    fun legacyPlanGoalIdsResolveToCatalogIds() {
        val catalog = PlanCatalogData(
            goals = listOf(
                PlanGoal(id = "everyday", title = "Everyday", emoji = "💬"),
                PlanGoal(id = "work_career", title = "Work", emoji = "💼")
            )
        )

        assertEquals("everyday", GoalPlan.canonicalGoalId("conversation"))
        assertEquals("work_career", GoalPlan.canonicalGoalId("career"))
        assertEquals("everyday", catalog.goal("conversation")?.id)
        assertEquals("work_career", catalog.goal("career")?.id)
    }

    @Test
    fun startingPlanWithLegacyGoalPersistsCanonicalId() {
        val progress = PlanProgress()

        progress.start(goal = "conversation", name = "Learner")

        assertEquals("everyday", progress.goalId)
    }

    @Test
    fun lessonRetryMessageHasXmlResourceEntry() {
        val retryMessage = "Couldn't get a reply. Please try again."

        assertTrue(Strings.hasTranslationResource(retryMessage))
    }

    @Test
    fun speakingSkipPromptHasXmlResourceEntry() {
        val skipPrompt = "Can't speak now? Skip"

        assertTrue(Strings.hasTranslationResource(skipPrompt))
    }

    @Test
    fun planHomeAndActivityCopyHasXmlResourceEntries() {
        val planCopy = listOf(
            "Day %1\$d",
            "%1\$d min",
            "%1\$d/%2\$d activities",
            "Plan complete!",
            "%1\$d/%2\$d days",
            "View path",
            "Level %1\$d · %2\$d XP to go",
            "Closer to your goal!",
            "Loading plan...",
            "Plan content unavailable",
            "Complete each activity to finish this day",
            "Complete the previous activity first",
            "Completed",
            "Content unavailable",
            "Day",
            "Quick Quiz",
            "Start activity",
            "Talk with the Tutor",
            "This plan day is unavailable",
            "Video Shadowing",
            "%1\$d/%2\$d activities complete",
            "Average pronunciation score",
            "Day %1\$d complete! 🎉",
            "Pronunciation score",
            "Speak the sentence aloud",
            "Speaking complete",
            "Speaking content is unavailable",
            "Tap the microphone and repeat the sentence",
            "You said",
            "You've earned +50 XP and +10 Diamonds!",
            "Your speaking activity is saved. Complete the remaining activities to finish this day.",
            "Quiz content is unavailable",
            "Check Answer"
        )

        planCopy.forEach { key -> assertTrue(Strings.hasTranslationResource(key), key) }
    }
}
