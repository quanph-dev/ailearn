package com.example.basekmp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.basekmp.AppAdsHost
import com.example.basekmp.ui.components.AppBottomNav
import com.example.basekmp.ui.components.MainTab
import com.example.basekmp.ui.home.HomeScreen
import com.example.basekmp.ui.learningpath.LearningPathScreen
import com.example.basekmp.ui.practice.PracticeScreen
import com.example.basekmp.ui.progress.ProgressScreen
import com.example.basekmp.ui.settings.SettingsScreen

/**
 * Main Scaffold Host connecting the 5 tabs:
 * 1. Home
 * 2. Practice
 * 3. Plan / Learning Path
 * 4. Progress
 * 5. Settings
 *
 * Matching original MainScreenKt from decompiled source with 100% fidelity.
 */
@Composable
fun MainScreen(
    onStartLesson: () -> Unit = {},
    onStartPracticeLesson: (title: String, level: String) -> Unit = { _, _ -> },
    onStartPathLesson: (id: String) -> Unit = {},
    onOpenPlan: (hasPlan: Boolean) -> Unit = {},
    onOpenPlanDay: (day: Int) -> Unit = {},
    onOpenLesson: (title: String) -> Unit = {},
    onFeatureClick: (feature: String) -> Unit = {},
    onStatClick: (kind: String) -> Unit = {},
    onSeeAllHistory: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNativeLanguageSelected: (String) -> Unit = {},
    onLearnLanguageSelected: (String) -> Unit = {},
    languageSaveError: Boolean = false,
    onNavigateToLegal: (String) -> Unit = {},
    onContactSupport: () -> Unit = {},
    onShareApp: () -> Unit = {},
    selectedTab: MainTab = MainTab.Home,
    onTabSelected: (MainTab) -> Unit = {},
    adsHost: AppAdsHost? = null,
    onHomeInteraction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
    ) {
        if (selectedTab == MainTab.Home) {
            adsHost?.BannerAd("B_Home", modifier = Modifier.fillMaxWidth())
        }
        // Tab Content with authentic horizontal 20.dp padding
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .pointerInput(selectedTab, onHomeInteraction) {
                    if (selectedTab == MainTab.Home) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) onHomeInteraction()
                            }
                        }
                    }
                }
        ) {
            when (selectedTab) {
                MainTab.Home -> {
                    HomeScreen(
                        onNavigateToTutor = { onFeatureClick("ai_tutor") },
                        onNavigateToVocab = { onFeatureClick("vocabulary") },
                        onNavigateToSpeaking = { onFeatureClick("speaking") },
                        onNavigateToGrammar = { onFeatureClick("grammar") },
                        onNavigateToTodayLesson = onStartLesson,
                        onOpenPlan = onOpenPlan,
                        onOpenPlanDay = onOpenPlanDay,
                        onOpenVideo = { videoId -> onFeatureClick("video_shadowing:$videoId") },
                        onFeatureClick = onFeatureClick,
                        onNotificationsClick = { onFeatureClick("notifications") },
                        onStatClick = onStatClick
                    )
                }

                MainTab.Practice -> {
                    PracticeScreen(
                        onStartLesson = { lesson -> onStartPracticeLesson(lesson.title, lesson.level) },
                        onOpenCategory = onFeatureClick
                    )
                }

                MainTab.Plan -> {
                    LearningPathScreen(
                        onOpenLesson = { lesson -> onStartPathLesson(lesson.id) }
                    )
                }

                MainTab.Progress -> {
                    ProgressScreen(
                        onStatClick = onStatClick,
                        onLessonClick = onOpenLesson,
                        onSeeAllHistory = onSeeAllHistory
                    )
                }

                MainTab.Settings -> {
                    SettingsScreen(
                        onNavigateToLanguage = onNavigateToLanguage,
                        onNativeLanguageSelected = onNativeLanguageSelected,
                        onLearnLanguageSelected = onLearnLanguageSelected,
                        languageSaveError = languageSaveError,
                        onNavigateToLegal = onNavigateToLegal,
                        onContactSupport = onContactSupport,
                        onShareApp = onShareApp
                    )
                }
            }
        }

        if (selectedTab == MainTab.Home) {
            adsHost?.NativeAd(
                placement = "N_Home1_M",
                modifier = Modifier.fillMaxWidth(),
                fallbacks = listOf("N_Home1_A"),
                collapsible = true,
                reloadAfterIdleMs = 15_000L,
                onInteraction = onHomeInteraction
            )
        }
        // Persistent Floating Bottom Navigation Bar (wrapped in 20.dp horizontal, 10.dp bottom padding)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp)
        ) {
            AppBottomNav(
                selectedTab = selectedTab,
                onTabSelect = onTabSelected
            )
        }
    }
}
