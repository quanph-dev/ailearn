package com.example.basekmp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    onNavigateToLegal: (String) -> Unit = {},
    onContactSupport: () -> Unit = {},
    onShareApp: () -> Unit = {},
    selectedTab: MainTab = MainTab.Home,
    onTabSelected: (MainTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
    ) {
        // Tab Content with authentic horizontal 20.dp padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
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
                        onNavigateToLegal = onNavigateToLegal,
                        onContactSupport = onContactSupport,
                        onShareApp = onShareApp
                    )
                }
            }
        }

        // Persistent Floating Bottom Navigation Bar (wrapped in 20.dp horizontal, 10.dp bottom padding)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
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
}
