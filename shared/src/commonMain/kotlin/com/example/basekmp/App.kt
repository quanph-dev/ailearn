package com.example.basekmp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.cd_back
import ailearn.shared.generated.resources.language_confirm
import ailearn.shared.generated.resources.language_save_error
import ailearn.shared.generated.resources.language_title
import ailearn.shared.generated.resources.ui_are_you_sure_you_want_to_exit_the_app_now
import ailearn.shared.generated.resources.ui_exit_the_app
import ailearn.shared.generated.resources.ui_no
import ailearn.shared.generated.resources.ui_yes_exit
import com.example.basekmp.ui.onboarding.FirstOpenIntroScreen
import com.example.basekmp.ui.onboarding.FirstOpenSurveyScreen
import com.example.basekmp.ui.onboarding.OnboardingState
import com.example.basekmp.theme.Blue
import com.example.basekmp.theme.BlueSelected
import com.example.basekmp.theme.DividerColor
import com.example.basekmp.theme.EnglishLearningAppTheme
import com.example.basekmp.theme.GradientBottom
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.grammar.GrammarQuizScreen
import com.example.basekmp.ui.grammar.GrammarSetupScreen
import com.example.basekmp.ui.learningpath.PathLessonScreen
import com.example.basekmp.ui.main.MainScreen
import com.example.basekmp.ui.onboarding.OnboardingScreen
import com.example.basekmp.ui.plan.PlanGeneratingScreen
import com.example.basekmp.ui.plan.PlanScreen
import com.example.basekmp.ui.plan.PlanSessionScreen
import com.example.basekmp.ui.plan.PlanSetupScreen
import com.example.basekmp.ui.progress.LearningHistoryScreen
import com.example.basekmp.ui.progress.ProgressDetailScreen
import com.example.basekmp.ui.settings.LegalScreen
import com.example.basekmp.ui.shadowing.SavedWordsScreen
import com.example.basekmp.ui.shadowing.VideoShadowingListScreen
import com.example.basekmp.ui.shadowing.VideoShadowingPlayerScreen
import com.example.basekmp.ui.speaking.SpeakingCardsScreen
import com.example.basekmp.ui.speaking.SpeakingSetupScreen
import com.example.basekmp.ui.splash.SplashScreen
import com.example.basekmp.ui.tutor.AiTutorScreen
import com.example.basekmp.ui.tutor.TodayLessonScreen
import com.example.basekmp.ui.vocab.VocabCardsScreen
import com.example.basekmp.ui.vocab.VocabSetupScreen
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(
    viewModel: BaseAppViewModel,
    onLanguageApplied: () -> Unit = {},
    onExitApp: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is BaseAppEvent.LanguageApplied) {
                onLanguageApplied()
            }
        }
    }
    LaunchedEffect(uiState.languageCode) {
        applyPlatformLanguage(uiState.languageCode)
        com.example.basekmp.data.Strings.select(uiState.languageCode)
    }
    LaunchedEffect(uiState.route) {
        if (uiState.route == BaseRoutes.ApplyLanguage) {
            delay(250)
            viewModel.finishApplyLanguage()
        }
    }

    BackActionHandler(enabled = uiState.route == BaseRoutes.LanguageSettings) {
        viewModel.closeLanguageSettings()
    }
    BackActionHandler(enabled = !uiState.isLoading && uiState.route !in setOf(BaseRoutes.Home, BaseRoutes.LanguageSettings)) {
        when (uiState.route) {
            BaseRoutes.FirstOpenSurvey -> viewModel.backToFirstOpenIntro()
            BaseRoutes.FirstOpenIntro -> if (uiState.firstOpenPage > 0) viewModel.backFirstOpenIntro() else showExitConfirmation = true
            BaseRoutes.Onboarding -> {
                val state = OnboardingState.deserialize(uiState.onboardingProgress)
                if (!state.isFirstStep) viewModel.updateOnboardingProgress(state.previous().serialize())
                else showExitConfirmation = true
            }
            else -> showExitConfirmation = true
        }
    }

    EnglishLearningAppTheme(darkTheme = false) {
        PlatformLocaleProvider(uiState.languageCode) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading || uiState.route == BaseRoutes.Splash) {
                    SplashScreen()
                } else {
                    when (uiState.route) {
                        BaseRoutes.LanguageSettings -> com.example.basekmp.ui.settings.LanguageScreen(
                            onBack = { viewModel.closeLanguageSettings() },
                            onLanguageSelected = { langCode ->
                                viewModel.selectLanguage(langCode)
                                viewModel.confirmLanguage()
                            }
                        )
                        BaseRoutes.Onboarding -> {
                            val onboardingState = OnboardingState.deserialize(uiState.onboardingProgress)
                            OnboardingScreen(
                                state = onboardingState,
                                isSaving = uiState.isSaving,
                                hasError = uiState.hasError,
                                onStateChange = { viewModel.updateOnboardingProgress(it.serialize()) },
                                onFinished = { viewModel.completeOnboarding(it.serialize()) }
                            )
                        }
                        BaseRoutes.Home -> MainAppHost(
                            viewModel = viewModel,
                            onOpenLanguageSettings = { viewModel.openLanguageSettings() },
                            onExitRequested = { showExitConfirmation = true }
                        )
                        else -> SplashScreen()
                    }
                }

                if (showExitConfirmation) {
                    ExitConfirmationDialog(
                        onDismissRequest = { showExitConfirmation = false },
                        onExitApp = onExitApp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitConfirmationDialog(
    onDismissRequest: () -> Unit,
    onExitApp: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.ui_exit_the_app),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.ui_are_you_sure_you_want_to_exit_the_app_now),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    ExitSecondaryButton(
                        text = stringResource(Res.string.ui_no),
                        onClick = onDismissRequest
                    )
                }
                Box(Modifier.weight(1f)) {
                    ExitPrimaryButton(
                        text = stringResource(Res.string.ui_yes_exit),
                        onClick = onExitApp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitSecondaryButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(OptionSurface)
            .border(1.dp, DividerColor, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExitPrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Blue)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LanguageScreen(state: BaseAppUiState, viewModel: BaseAppViewModel) {
    val settingsMode = state.route == BaseRoutes.LanguageSettings
    var canConfirm by remember(state.route, state.selectedLanguageCode) {
        mutableStateOf(settingsMode)
    }
    LaunchedEffect(state.route, state.selectedLanguageCode, state.hasUserSelectedLanguage) {
        canConfirm = settingsMode
        if (!settingsMode && state.hasUserSelectedLanguage) {
            delay(BaseAppViewModel.LANGUAGE_CONFIRM_DELAY_MS)
            canConfirm = true
        }
    }
    val languages = remember(viewModel.deviceLanguageCode) { sortedBaseLanguages(viewModel.deviceLanguageCode) }
    val confirmAccessibilityLabel = stringResource(Res.string.language_confirm)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color.White,
                    0.42f to GradientBottom,
                    1.0f to Color.White
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settingsMode) {
                Text(
                    text = "‹  ${stringResource(Res.string.cd_back)}",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { viewModel.closeLanguageSettings() }
                )
                Spacer(Modifier.width(16.dp))
            }
            Text(
                text = stringResource(Res.string.language_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(
                visible = state.hasUserSelectedLanguage && canConfirm,
                enter = fadeIn(tween(350)),
                exit = fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Blue)
                        .semantics { contentDescription = confirmAccessibilityLabel }
                        .clickable { viewModel.confirmLanguage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(languages, key = { it.code }) { option ->
                val selected = option.code == state.selectedLanguageCode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(OptionSurface)
                        .border(
                            width = 1.5.dp,
                            color = if (selected) BlueSelected else DividerColor,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { viewModel.selectLanguage(option.code) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.flag,
                        fontSize = 26.sp,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            option.nativeName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) BlueSelected else TextPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                        )
                        Text(
                            option.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (selected) {
                        Text("✓", color = BlueSelected, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
        if (state.hasError) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(Res.string.language_save_error), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
    }
}

private sealed interface ActiveSubScreen {
    data object AiTutor : ActiveSubScreen
    data class TodayLesson(val title: String? = null, val level: String? = null) : ActiveSubScreen
    data object VocabSetup : ActiveSubScreen
    data class VocabCards(val count: Int) : ActiveSubScreen
    data object GrammarSetup : ActiveSubScreen
    data class GrammarQuiz(val count: Int) : ActiveSubScreen
    data object SpeakingSetup : ActiveSubScreen
    data class SpeakingCards(val count: Int) : ActiveSubScreen
    data object ShadowingList : ActiveSubScreen
    data class ShadowingPlayer(val videoId: String) : ActiveSubScreen
    data object SavedWords : ActiveSubScreen
    data class PathLesson(val lessonId: String) : ActiveSubScreen
    data object PlanSetup : ActiveSubScreen
    data object PlanGenerating : ActiveSubScreen
    data object PlanRoadmap : ActiveSubScreen
    data class PlanSession(val day: Int) : ActiveSubScreen
    data class ProgressDetail(val kind: String) : ActiveSubScreen
    data object LearningHistory : ActiveSubScreen
    data class Legal(val docType: String) : ActiveSubScreen
}

@Composable
private fun MainAppHost(
    viewModel: BaseAppViewModel,
    onOpenLanguageSettings: () -> Unit,
    onExitRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activeSubScreen by remember { mutableStateOf<ActiveSubScreen?>(null) }

    BackActionHandler(enabled = activeSubScreen != null) {
        activeSubScreen = when (val current = activeSubScreen) {
            is ActiveSubScreen.ShadowingPlayer -> ActiveSubScreen.ShadowingList
            is ActiveSubScreen.SavedWords -> ActiveSubScreen.ShadowingList
            is ActiveSubScreen.PlanSession -> ActiveSubScreen.PlanRoadmap
            is ActiveSubScreen.PlanGenerating -> ActiveSubScreen.PlanSetup
            is ActiveSubScreen.VocabCards -> ActiveSubScreen.VocabSetup
            is ActiveSubScreen.GrammarQuiz -> ActiveSubScreen.GrammarSetup
            is ActiveSubScreen.SpeakingCards -> ActiveSubScreen.SpeakingSetup
            else -> null
        }
    }
    BackActionHandler(enabled = activeSubScreen == null) {
        if (uiState.selectedTab != com.example.basekmp.ui.components.MainTab.Home) {
            viewModel.selectTab(com.example.basekmp.ui.components.MainTab.Home)
        } else {
            onExitRequested()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeSubScreen == null) {
            MainScreen(
                onStartLesson = {
                    activeSubScreen = ActiveSubScreen.TodayLesson()
                },
                onStartPracticeLesson = { title, level ->
                    activeSubScreen = ActiveSubScreen.TodayLesson(title = title, level = level)
                },
                onStartPathLesson = { lessonId ->
                    activeSubScreen = ActiveSubScreen.PathLesson(lessonId)
                },
                onOpenPlan = { hasPlan ->
                    activeSubScreen = if (hasPlan) ActiveSubScreen.PlanRoadmap else ActiveSubScreen.PlanSetup
                },
                onOpenPlanDay = { day ->
                    activeSubScreen = ActiveSubScreen.PlanSession(day)
                },
                onFeatureClick = { feature ->
                    activeSubScreen = when {
                        feature.startsWith("video_shadowing:") -> {
                            val videoId = feature.removePrefix("video_shadowing:")
                            ActiveSubScreen.ShadowingPlayer(videoId)
                        }
                        feature.equals("ai_tutor", ignoreCase = true) -> ActiveSubScreen.AiTutor
                        feature.equals("vocabulary", ignoreCase = true) || feature.equals("vocab", ignoreCase = true) -> ActiveSubScreen.VocabSetup
                        feature.equals("grammar", ignoreCase = true) -> ActiveSubScreen.GrammarSetup
                        feature.equals("speaking", ignoreCase = true) -> ActiveSubScreen.SpeakingSetup
                        feature.equals("shadowing", ignoreCase = true) -> ActiveSubScreen.ShadowingList
                        else -> ActiveSubScreen.AiTutor
                    }
                },
                onStatClick = { kind ->
                    activeSubScreen = ActiveSubScreen.ProgressDetail(kind)
                },
                onSeeAllHistory = {
                    activeSubScreen = ActiveSubScreen.LearningHistory
                },
                onNavigateToLanguage = onOpenLanguageSettings,
                onNavigateToLegal = { doc ->
                    activeSubScreen = ActiveSubScreen.Legal(doc)
                },
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> viewModel.selectTab(tab) }
            )
        } else {
            when (val screen = activeSubScreen) {
                ActiveSubScreen.AiTutor -> {
                    AiTutorScreen(onBack = { activeSubScreen = null })
                }
                is ActiveSubScreen.TodayLesson -> {
                    TodayLessonScreen(
                        title = screen.title,
                        level = screen.level,
                        onBack = { activeSubScreen = null },
                        onFinish = { activeSubScreen = null }
                    )
                }
                ActiveSubScreen.VocabSetup -> {
                    VocabSetupScreen(
                        onBack = { activeSubScreen = null },
                        onStart = { count -> activeSubScreen = ActiveSubScreen.VocabCards(count) }
                    )
                }
                is ActiveSubScreen.VocabCards -> {
                    VocabCardsScreen(
                        wordCount = screen.count,
                        onClose = { _, _ -> activeSubScreen = ActiveSubScreen.VocabSetup }
                    )
                }
                ActiveSubScreen.GrammarSetup -> {
                    GrammarSetupScreen(
                        onBack = { activeSubScreen = null },
                        onStart = { count -> activeSubScreen = ActiveSubScreen.GrammarQuiz(count) }
                    )
                }
                is ActiveSubScreen.GrammarQuiz -> {
                    GrammarQuizScreen(
                        questionCount = screen.count,
                        onClose = { _, _ -> activeSubScreen = ActiveSubScreen.GrammarSetup }
                    )
                }
                ActiveSubScreen.SpeakingSetup -> {
                    SpeakingSetupScreen(
                        onBack = { activeSubScreen = null },
                        onStart = { count -> activeSubScreen = ActiveSubScreen.SpeakingCards(count) }
                    )
                }
                is ActiveSubScreen.SpeakingCards -> {
                    SpeakingCardsScreen(
                        cardCount = screen.count,
                        onClose = { _, _ -> activeSubScreen = ActiveSubScreen.SpeakingSetup }
                    )
                }
                ActiveSubScreen.ShadowingList -> {
                    VideoShadowingListScreen(
                        onBack = { activeSubScreen = null },
                        onOpenVideo = { videoId -> activeSubScreen = ActiveSubScreen.ShadowingPlayer(videoId) },
                        onOpenSavedWords = { activeSubScreen = ActiveSubScreen.SavedWords }
                    )
                }
                is ActiveSubScreen.ShadowingPlayer -> {
                    VideoShadowingPlayerScreen(
                        videoId = screen.videoId,
                        onClose = { activeSubScreen = ActiveSubScreen.ShadowingList },
                        onOpenSavedWords = { activeSubScreen = ActiveSubScreen.SavedWords }
                    )
                }
                ActiveSubScreen.SavedWords -> {
                    SavedWordsScreen(onBack = { activeSubScreen = ActiveSubScreen.ShadowingList })
                }
                is ActiveSubScreen.PathLesson -> {
                    PathLessonScreen(
                        lessonId = screen.lessonId,
                        onClose = { activeSubScreen = null }
                    )
                }
                ActiveSubScreen.PlanSetup -> {
                    PlanSetupScreen(
                        onBack = { activeSubScreen = null },
                        onDone = { activeSubScreen = ActiveSubScreen.PlanGenerating }
                    )
                }
                ActiveSubScreen.PlanGenerating -> {
                    PlanGeneratingScreen(
                        onDone = { activeSubScreen = ActiveSubScreen.PlanRoadmap }
                    )
                }
                ActiveSubScreen.PlanRoadmap -> {
                    PlanScreen(
                        onBack = { activeSubScreen = null },
                        onChangeGoal = { activeSubScreen = ActiveSubScreen.PlanSetup },
                        onOpenDay = { day -> activeSubScreen = ActiveSubScreen.PlanSession(day) }
                    )
                }
                is ActiveSubScreen.PlanSession -> {
                    PlanSessionScreen(
                        day = screen.day,
                        onBack = { activeSubScreen = ActiveSubScreen.PlanRoadmap },
                        onFinish = { activeSubScreen = ActiveSubScreen.PlanRoadmap }
                    )
                }
                is ActiveSubScreen.ProgressDetail -> {
                    ProgressDetailScreen(
                        kind = screen.kind,
                        onBack = { activeSubScreen = null },
                        onStartPractice = {
                            activeSubScreen = ActiveSubScreen.TodayLesson()
                        }
                    )
                }
                ActiveSubScreen.LearningHistory -> {
                    LearningHistoryScreen(
                        onBack = { activeSubScreen = null }
                    )
                }
                is ActiveSubScreen.Legal -> {
                    LegalScreen(
                        kind = screen.docType,
                        onBack = { activeSubScreen = null }
                    )
                }
                null -> {}
            }
        }
    }
}
