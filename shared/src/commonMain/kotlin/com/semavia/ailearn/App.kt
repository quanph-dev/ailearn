package com.semavia.ailearn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.cd_back
import ailearn.shared.generated.resources.language_confirm
import ailearn.shared.generated.resources.language_save_error
import ailearn.shared.generated.resources.language_title
import ailearn.shared.generated.resources.fo_ic_logo_app
import ailearn.shared.generated.resources.ui_are_you_sure_you_want_to_exit_the_app_now
import ailearn.shared.generated.resources.ui_exit_the_app
import ailearn.shared.generated.resources.ui_no
import ailearn.shared.generated.resources.ui_yes_exit
import com.semavia.ailearn.ui.onboarding.FirstOpenIntroScreen
import com.semavia.ailearn.ui.onboarding.FirstOpenSurveyScreen
import com.semavia.ailearn.ui.onboarding.OnboardingState
import com.semavia.ailearn.theme.BrandPrimary
import com.semavia.ailearn.theme.BrandPrimaryPressed
import com.semavia.ailearn.theme.DividerColor
import com.semavia.ailearn.theme.EnglishLearningAppTheme
import com.semavia.ailearn.theme.GradientBottom
import com.semavia.ailearn.theme.OptionSurface
import com.semavia.ailearn.theme.OptionSurfaceSelected
import com.semavia.ailearn.theme.TextPrimary
import com.semavia.ailearn.theme.TextSecondary
import com.semavia.ailearn.ui.grammar.GrammarQuizScreen
import com.semavia.ailearn.ui.grammar.GrammarSetupScreen
import com.semavia.ailearn.ui.learningpath.PathLessonScreen
import com.semavia.ailearn.ui.main.MainScreen
import com.semavia.ailearn.ui.onboarding.OnboardingScreen
import com.semavia.ailearn.ui.plan.PlanGeneratingScreen
import com.semavia.ailearn.ui.plan.PlanDayActivitiesScreen
import com.semavia.ailearn.ui.plan.PlanScreen
import com.semavia.ailearn.ui.plan.PlanSessionScreen
import com.semavia.ailearn.ui.plan.PlanSpeakingSessionScreen
import com.semavia.ailearn.ui.plan.PlanSetupScreen
import com.semavia.ailearn.data.GoalPlan
import com.semavia.ailearn.data.PlanProgress
import com.semavia.ailearn.data.PracticeConfig
import com.semavia.ailearn.data.Strings
import com.semavia.ailearn.data.UserPrefs
import com.semavia.ailearn.ui.plan.PlanFlow
import com.semavia.ailearn.ui.progress.LearningHistoryScreen
import com.semavia.ailearn.ui.progress.ProgressDetailScreen
import com.semavia.ailearn.ui.shadowing.SavedWordsScreen
import com.semavia.ailearn.ui.shadowing.VideoShadowingListScreen
import com.semavia.ailearn.ui.shadowing.VideoShadowingPlayerScreen
import com.semavia.ailearn.ui.speaking.SpeakingCardsScreen
import com.semavia.ailearn.ui.speaking.SpeakingSetupScreen
import com.semavia.ailearn.ui.splash.SplashScreen
import com.semavia.ailearn.ui.tutor.AiTutorScreen
import com.semavia.ailearn.ui.tutor.TodayLessonScreen
import com.semavia.ailearn.ui.vocab.VocabCardsScreen
import com.semavia.ailearn.ui.vocab.VocabSetupScreen
import com.semavia.ailearn.analytics.AnalyticsEventLogger
import com.semavia.ailearn.analytics.LocalAnalyticsEventLogger
import com.semavia.ailearn.analytics.NoOpAnalyticsEventLogger
import com.semavia.ailearn.analytics.track
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import com.semavia.ailearn.data.appStringResource as stringResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun App(
    viewModel: BaseAppViewModel,
    adsHost: AppAdsHost? = null,
    onExitApp: () -> Unit = {},
    analyticsLogger: AnalyticsEventLogger = NoOpAnalyticsEventLogger
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }
    var introGateInProgress by remember { mutableStateOf(false) }
    val appScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val splashPhase = adsHost?.splashPhase?.collectAsStateWithLifecycle()?.value ?: 2
    val isLoadingAds = adsHost?.isInterstitialLoading?.collectAsStateWithLifecycle()?.value ?: false
    val fullscreenNativeState = adsHost?.fullscreenNativeState?.collectAsStateWithLifecycle()?.value
    var lastTrackedRouteScreen by remember { mutableStateOf<String?>(null) }

    val routeScreenName = when {
        uiState.isLoading || uiState.route == BaseRoutes.Splash -> "splash"
        uiState.route == BaseRoutes.StartupLanguage -> "native_language_selection"
        uiState.route == BaseRoutes.FirstOpenIntro -> "first_open_intro"
        uiState.route == BaseRoutes.FirstOpenSurvey -> "first_open_survey"
        uiState.route == BaseRoutes.Onboarding -> "onboarding"
        uiState.route == BaseRoutes.LanguageSettings -> "language_settings"
        else -> null
    }
    LaunchedEffect(routeScreenName) {
        if (routeScreenName != lastTrackedRouteScreen) {
            routeScreenName?.let { screen ->
                analyticsLogger.track("app_page_view", "screen_name" to screen)
            }
            lastTrackedRouteScreen = routeScreenName
        }
    }

    LaunchedEffect(uiState.route, uiState.firstOpenPage) {
        if (uiState.route == BaseRoutes.FirstOpenIntro) {
            analyticsLogger.track(
                "onboarding_step_view",
                "flow_name" to "first_open_intro",
                "step_index" to (uiState.firstOpenPage + 1)
            )
        }
    }

    fun passFirstIntroFullscreen(continueAction: () -> Unit) {
        if (introGateInProgress) return
        introGateInProgress = true
        appScope.launch {
            try {
                adsHost?.showFullscreenNative("N_FS_12", closeDelaySeconds = 5)
                continueAction()
            } finally {
                introGateInProgress = false
            }
        }
    }

    LaunchedEffect(uiState.route) {
        if (uiState.route == BaseRoutes.ApplyLanguage) {
            delay(250)
            viewModel.finishApplyLanguage()
        }
    }

    LaunchedEffect(uiState.isLoading, uiState.route, uiState.startupTargetRoute, uiState.isReturningUser, adsHost) {
        if (!uiState.isLoading && uiState.route == BaseRoutes.Splash && uiState.startupTargetRoute.isNotBlank()) {
            adsHost?.showStartupAds(uiState.isReturningUser)
            viewModel.finishStartup()
        }
    }
    LaunchedEffect(uiState.route, adsHost) {
        when (uiState.route) {
            BaseRoutes.FirstOpenIntro -> adsHost?.showPendingStartupFallback()
            BaseRoutes.FirstOpenSurveyAd -> {
                adsHost?.showFullscreenNative("N_FS_Survey", closeDelaySeconds = 5)
                viewModel.finishFirstOpenSurveyAd()
            }
            BaseRoutes.Starting -> {
                adsHost?.showInterstitial("I_Start", timeoutMs = 8_000L)
                viewModel.finishStartInterstitial()
            }
        }
    }
    LaunchedEffect(uiState.route, lifecycleOwner) {
        if (uiState.route == BaseRoutes.Home) {
            lifecycleOwner.lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            withFrameNanos { }
            viewModel.markHomeViewed()
        }
    }

    val isAdsBlockingUi = isLoadingAds || fullscreenNativeState != null

    BackActionHandler(enabled = uiState.route == BaseRoutes.LanguageSettings && !isAdsBlockingUi) {
        viewModel.closeLanguageSettings()
    }
    BackActionHandler(enabled = !isAdsBlockingUi && !uiState.isLoading && uiState.route !in setOf(
        BaseRoutes.Home,
        BaseRoutes.LanguageSettings,
        BaseRoutes.Splash,
        BaseRoutes.Starting,
        BaseRoutes.FirstOpenSurveyAd
    )) {
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

    CompositionLocalProvider(LocalAnalyticsEventLogger provides analyticsLogger) {
      EnglishLearningAppTheme(darkTheme = false) {
        PlatformLocaleProvider(uiState.languageCode) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading || uiState.route == BaseRoutes.Splash) {
                    SplashScreen(phase = if (uiState.isLoading) 1 else splashPhase, adsHost = adsHost)
                } else {
                    when (uiState.route) {
                        BaseRoutes.StartupLanguage -> LanguageScreen(uiState, viewModel)
                        BaseRoutes.FirstOpenIntro -> FirstOpenIntroScreen(
                            page = uiState.firstOpenPage,
                            onPageSelected = { page ->
                                analyticsLogger.track(
                                    "onboarding_intro_page_selected",
                                    "step_index" to (page + 1),
                                    "input_method" to "pager_navigation"
                                )
                                if (page > 0 && uiState.firstOpenPage == 0) {
                                    passFirstIntroFullscreen { viewModel.selectFirstOpenPage(1) }
                                } else {
                                    viewModel.selectFirstOpenPage(page)
                                }
                            },
                            onNext = {
                                analyticsLogger.track(
                                    "onboarding_intro_continue",
                                    "step_index" to (uiState.firstOpenPage + 1)
                                )
                                if (uiState.firstOpenPage == 0) {
                                    passFirstIntroFullscreen(viewModel::advanceFirstOpenIntro)
                                } else {
                                    viewModel.advanceFirstOpenIntro()
                                }
                            },
                            adsHost = adsHost
                        )
                        BaseRoutes.FirstOpenSurvey -> FirstOpenSurveyScreen(
                            selectedLanguage = uiState.firstOpenSurveyLanguage,
                            isSaving = uiState.isSaving,
                            hasError = uiState.hasError,
                            onSelectLanguage = { languageCode ->
                                analyticsLogger.track(
                                    "first_open_language_selected",
                                    "language_code" to languageCode
                                )
                                viewModel.selectFirstOpenSurveyLanguage(languageCode)
                            },
                            onContinue = {
                                analyticsLogger.track("first_open_survey_completed")
                                viewModel.finishFirstOpenSurvey()
                            }
                        )
                        BaseRoutes.LanguageSettings -> com.semavia.ailearn.ui.settings.LanguageScreen(
                            onBack = { viewModel.closeLanguageSettings() },
                            onLanguageSelected = { langCode ->
                                if (langCode != UserPrefs.nativeLanguage) {
                                    analyticsLogger.track(
                                        "language_preference_updated",
                                        "language_role" to "native",
                                        "language_code" to langCode
                                    )
                                }
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
                                onFinished = {
                                    analyticsLogger.track("onboarding_completed")
                                    viewModel.completeOnboarding(it.serialize())
                                }
                            )
                        }
                        BaseRoutes.Home -> MainAppHost(
                            viewModel = viewModel,
                            adsHost = adsHost,
                            isAdsBlockingUi = isAdsBlockingUi,
                            onOpenLanguageSettings = { viewModel.openLanguageSettings() },
                            onExitRequested = { showExitConfirmation = true }
                        )
                        BaseRoutes.Starting,
                        BaseRoutes.FirstOpenSurveyAd -> Box(Modifier.fillMaxSize().background(Color.White))
                        else -> SplashScreen(phase = splashPhase, adsHost = adsHost)
                    }
                }

                if (showExitConfirmation) {
                    ExitConfirmationDialog(
                        adsHost = adsHost,
                            onDismissRequest = {
                                analyticsLogger.track("app_exit_confirmation_dismissed")
                                showExitConfirmation = false
                            },
                        onExitApp = {
                            analyticsLogger.track("app_exit_confirmed")
                            onExitApp()
                        }
                    )
                }
                if (fullscreenNativeState != null) {
                    FullscreenNativeAdDialog(
                        state = fullscreenNativeState,
                        adsHost = adsHost
                    )
                }
                if (isLoadingAds && uiState.route != BaseRoutes.Splash) {
                    LaunchedEffect(adsHost) {
                        androidx.compose.runtime.withFrameNanos { }
                        androidx.compose.runtime.withFrameNanos { }
                        adsHost?.onLoadingAdsRendered()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.fo_ic_logo_app),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(24.dp))
                            )
                            Spacer(Modifier.height(28.dp))
                            CircularProgressIndicator(
                                color = BrandPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = Strings.tr("Loading ads"),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = Strings.tr("Ads help keep AI Learn free"),
                                color = TextSecondary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun ExitConfirmationDialog(
    adsHost: AppAdsHost?,
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
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                adsHost?.NativeAd("N_Exit", modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(20.dp))
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
private fun FullscreenNativeAdDialog(
    state: FullscreenNativeState,
    adsHost: AppAdsHost
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF101828))) {
            adsHost.NativeAd(
                placement = state.placement,
                modifier = Modifier.fillMaxSize(),
                reuseOnly = true
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 50.dp, end = 18.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE4E4E7))
                    .clickable(enabled = state.canClose) { adsHost.closeFullscreenNative() },
                contentAlignment = Alignment.Center
            ) {
                if (state.canClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = Strings.tr("Close ad"),
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF70717A)
                    )
                } else {
                    Text(
                        text = state.secondsRemaining.toString(),
                        color = Color(0xFF70717A),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
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
            .background(BrandPrimary)
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
    val analytics = LocalAnalyticsEventLogger.current
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
            .padding(horizontal = if (settingsMode) 20.dp else 16.dp)
    ) {
        if (settingsMode) Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(if (settingsMode) 64.dp else 52.dp),
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
                text = if (settingsMode) stringResource(Res.string.language_title) else Strings.tr("Select Native Language"),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = if (canConfirm) 1f else 0.5f))
                    .semantics { contentDescription = confirmAccessibilityLabel }
                    .clickable(
                        enabled = canConfirm,
                        role = Role.Button,
                        onClick = {
                            analytics.track("native_language_confirmed", "language_code" to state.selectedLanguageCode)
                            viewModel.confirmLanguage()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✓",
                    color = Color.White.copy(alpha = if (canConfirm) 1f else 0.7f),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(languages, key = { it.code }) { option ->
                val selected = option.code == state.selectedLanguageCode
                if (settingsMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(OptionSurface)
                            .border(
                                width = 1.5.dp,
                                color = if (selected) BrandPrimaryPressed else DividerColor,
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
                                color = if (selected) BrandPrimaryPressed else TextPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                            )
                            Text(
                                option.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (selected) {
                            Text("✓", color = BrandPrimaryPressed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                } else {
                    StartupLanguageOptionRow(
                        option = option,
                        selected = selected,
                        onClick = {
                            if (option.code != state.selectedLanguageCode) {
                                analytics.track(
                                    "language_preference_updated",
                                    "language_role" to "native",
                                    "language_code" to option.code
                                )
                            }
                            viewModel.selectLanguage(option.code)
                        }
                    )
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

@Composable
private fun StartupLanguageOptionRow(
    option: BaseLanguage,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) OptionSurfaceSelected else Color.White)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) BrandPrimaryPressed else DividerColor,
                shape = shape
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, DividerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(option.flag, fontSize = 18.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = option.nativeName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) BrandPrimaryPressed else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) BrandPrimaryPressed else Color.Transparent)
                .border(1.5.dp, if (selected) BrandPrimaryPressed else DividerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
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
    data class PathLesson(val lessonId: String) : ActiveSubScreen
    data object PlanSetup : ActiveSubScreen
    data object PlanGenerating : ActiveSubScreen
    data object PlanRoadmap : ActiveSubScreen
    data class PlanDay(val day: Int) : ActiveSubScreen
    data class PlanSession(val day: Int) : ActiveSubScreen
    data class PlanSpeaking(val day: Int) : ActiveSubScreen
    data class PlanVideo(val day: Int, val videoId: String) : ActiveSubScreen
    data class PlanTalk(val day: Int, val title: String, val level: String, val titleLanguage: String, val plan: PlanFlow.DayPlan) : ActiveSubScreen
    data class ProgressDetail(val kind: String) : ActiveSubScreen
    data object LearningHistory : ActiveSubScreen
}

private fun ActiveSubScreen.analyticsScreenName(): String = when (this) {
    ActiveSubScreen.AiTutor -> "ai_tutor"
    is ActiveSubScreen.TodayLesson -> "today_lesson"
    ActiveSubScreen.VocabSetup -> "vocabulary_setup"
    is ActiveSubScreen.VocabCards -> "vocabulary_session"
    ActiveSubScreen.GrammarSetup -> "grammar_setup"
    is ActiveSubScreen.GrammarQuiz -> "grammar_session"
    ActiveSubScreen.SpeakingSetup -> "speaking_setup"
    is ActiveSubScreen.SpeakingCards -> "speaking_session"
    ActiveSubScreen.ShadowingList -> "shadowing_catalog"
    is ActiveSubScreen.ShadowingPlayer -> "shadowing_player"
    is ActiveSubScreen.PathLesson -> "learning_path_lesson"
    ActiveSubScreen.PlanSetup -> "learning_plan_setup"
    ActiveSubScreen.PlanGenerating -> "learning_plan_generating"
    ActiveSubScreen.PlanRoadmap -> "learning_plan_roadmap"
    is ActiveSubScreen.PlanDay -> "learning_plan_day"
    is ActiveSubScreen.PlanSession -> "learning_plan_quiz"
    is ActiveSubScreen.PlanSpeaking -> "learning_plan_speaking"
    is ActiveSubScreen.PlanVideo -> "learning_plan_shadowing"
    is ActiveSubScreen.PlanTalk -> "learning_plan_conversation"
    is ActiveSubScreen.ProgressDetail -> "progress_detail"
    ActiveSubScreen.LearningHistory -> "learning_history"
}

@Composable
private fun MainAppHost(
    viewModel: BaseAppViewModel,
    adsHost: AppAdsHost?,
    isAdsBlockingUi: Boolean = false,
    onOpenLanguageSettings: () -> Unit,
    onExitRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activeSubScreen by remember { mutableStateOf<ActiveSubScreen?>(null) }
    var showSavedWords by remember { mutableStateOf(false) }
    var lessonBackRequest by remember { mutableIntStateOf(0) }
    val planScope = androidx.compose.runtime.rememberCoroutineScope()
    var backOperationRunning by remember { mutableStateOf(false) }
    var tabOperationRunning by remember { mutableStateOf(false) }
    val appScope = rememberCoroutineScope()
    val analytics = LocalAnalyticsEventLogger.current

    LaunchedEffect(uiState.selectedTab) {
        analytics.track("main_tab_view", "tab_name" to uiState.selectedTab.name.lowercase())
    }
    LaunchedEffect(activeSubScreen) {
        activeSubScreen?.let { screen ->
            analytics.track("app_page_view", "screen_name" to screen.analyticsScreenName())
        }
    }
    LaunchedEffect(showSavedWords) {
        if (showSavedWords) analytics.track("app_page_view", "screen_name" to "saved_words")
    }

    fun returnToMainFromBack(action: () -> Unit) {
        if (backOperationRunning) return
        backOperationRunning = true
        analytics.track(
            "feature_back_navigation",
            "screen_name" to (activeSubScreen?.analyticsScreenName() ?: uiState.selectedTab.name.lowercase())
        )
        when (activeSubScreen) {
            ActiveSubScreen.PlanSetup -> analytics.track("learning_plan_setup_abandoned")
            ActiveSubScreen.VocabSetup -> analytics.track("learning_practice_setup_abandoned", "activity_type" to "vocabulary")
            ActiveSubScreen.GrammarSetup -> analytics.track("learning_practice_setup_abandoned", "activity_type" to "grammar_quiz")
            ActiveSubScreen.SpeakingSetup -> analytics.track("learning_practice_setup_abandoned", "activity_type" to "speaking")
            else -> Unit
        }
        appScope.launch {
            try {
                adsHost?.showInterstitial("I_Back", timeoutMs = 8_000L)
                action()
            } finally {
                backOperationRunning = false
            }
        }
    }

    fun selectMainTab(tab: com.semavia.ailearn.ui.components.MainTab) {
        val currentTab = uiState.selectedTab
        if (tab == currentTab || tabOperationRunning) return
        analytics.track(
            "main_tab_selected",
            "tab_name" to tab.name.lowercase(),
            "previous_tab" to currentTab.name.lowercase()
        )
        if (tab == com.semavia.ailearn.ui.components.MainTab.Settings || currentTab == com.semavia.ailearn.ui.components.MainTab.Settings) {
            viewModel.selectTab(tab)
            return
        }
        tabOperationRunning = true
        appScope.launch {
            try {
                adsHost?.showInterstitial("I_Switchtab", timeoutMs = 8_000L, minIntervalMs = 30_000L)
                viewModel.selectTab(tab)
            } finally {
                tabOperationRunning = false
            }
        }
    }

    BackActionHandler(enabled = showSavedWords && !isAdsBlockingUi) {
        showSavedWords = false
    }
    BackActionHandler(enabled = activeSubScreen != null && !showSavedWords && !isAdsBlockingUi) {
        val current = activeSubScreen
        if (current is ActiveSubScreen.TodayLesson || current is ActiveSubScreen.PlanTalk) {
            lessonBackRequest++
        } else {
            val parent = when (current) {
            is ActiveSubScreen.ShadowingPlayer -> ActiveSubScreen.ShadowingList
            is ActiveSubScreen.PlanDay -> ActiveSubScreen.PlanRoadmap
            is ActiveSubScreen.PlanSession -> ActiveSubScreen.PlanDay(current.day)
            is ActiveSubScreen.PlanSpeaking -> ActiveSubScreen.PlanDay(current.day)
            is ActiveSubScreen.PlanVideo -> ActiveSubScreen.PlanDay(current.day)
            is ActiveSubScreen.PlanTalk -> ActiveSubScreen.PlanDay(current.day)
            is ActiveSubScreen.PlanGenerating -> ActiveSubScreen.PlanSetup
            is ActiveSubScreen.VocabCards -> ActiveSubScreen.VocabSetup
            is ActiveSubScreen.GrammarQuiz -> ActiveSubScreen.GrammarSetup
            is ActiveSubScreen.SpeakingCards -> ActiveSubScreen.SpeakingSetup
            else -> null
            }
            if (parent == null) returnToMainFromBack { activeSubScreen = null } else activeSubScreen = parent
        }
    }
    BackActionHandler(enabled = activeSubScreen == null && !showSavedWords && !isAdsBlockingUi) {
        if (uiState.selectedTab != com.semavia.ailearn.ui.components.MainTab.Home) {
            if (uiState.selectedTab == com.semavia.ailearn.ui.components.MainTab.Settings) {
                viewModel.selectTab(com.semavia.ailearn.ui.components.MainTab.Home)
            } else {
                returnToMainFromBack { viewModel.selectTab(com.semavia.ailearn.ui.components.MainTab.Home) }
            }
        } else {
            onExitRequested()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeSubScreen == null) {
            MainScreen(
                onStartLesson = {
                    analytics.track("learning_session_start", "activity_type" to "daily_lesson")
                    activeSubScreen = ActiveSubScreen.TodayLesson()
                },
                onStartPracticeLesson = { title, level ->
                    analytics.track(
                        "learning_session_start",
                        "activity_type" to "practice_lesson",
                        "difficulty" to level.lowercase()
                    )
                    activeSubScreen = ActiveSubScreen.TodayLesson(title = title, level = level)
                },
                onStartPathLesson = { lessonId ->
                    analytics.track("learning_session_start", "activity_type" to "learning_path_lesson")
                    activeSubScreen = ActiveSubScreen.PathLesson(lessonId)
                },
                onOpenPlan = { hasPlan ->
                    analytics.track(
                        "learning_plan_opened",
                        "has_existing_plan" to hasPlan.toString()
                    )
                    activeSubScreen = if (hasPlan) ActiveSubScreen.PlanRoadmap else ActiveSubScreen.PlanSetup
                },
                onOpenPlanDay = { day ->
                    analytics.track("learning_plan_day_opened", "day_number" to day)
                    activeSubScreen = ActiveSubScreen.PlanDay(day)
                },
                onOpenLesson = { title ->
                    val destination = when {
                        title.equals("AI Tutor", ignoreCase = true) || title.startsWith("AI Tutor ", ignoreCase = true) -> ActiveSubScreen.AiTutor
                        title.equals("Today's Lesson", ignoreCase = true) -> ActiveSubScreen.TodayLesson()
                        title.contains("Vocabulary", ignoreCase = true) -> ActiveSubScreen.VocabSetup
                        title.contains("Speaking", ignoreCase = true) -> ActiveSubScreen.SpeakingSetup
                        title.contains("Grammar", ignoreCase = true) -> ActiveSubScreen.GrammarSetup
                        else -> ActiveSubScreen.TodayLesson(title = title, level = "Intermediate")
                    }
                    analytics.track("progress_learning_item_opened", "screen_name" to destination.analyticsScreenName())
                    activeSubScreen = destination
                },
                onFeatureClick = { feature ->
                    val featureName = when {
                        feature.startsWith("video_shadowing:") -> "shadowing_video"
                        feature.equals("ai_tutor", ignoreCase = true) -> "ai_tutor"
                        feature.equals("vocabulary", ignoreCase = true) || feature.equals("vocab", ignoreCase = true) -> "vocabulary"
                        feature.equals("grammar", ignoreCase = true) -> "grammar"
                        feature.equals("speaking", ignoreCase = true) -> "speaking"
                        feature.equals("shadowing", ignoreCase = true) -> "shadowing"
                        feature.equals("notifications", ignoreCase = true) -> "notifications"
                        feature.equals("premium", ignoreCase = true) -> "premium"
                        else -> "other"
                    }
                    val destination = when {
                        feature.equals("premium", ignoreCase = true) -> null
                        feature.startsWith("video_shadowing:") -> {
                            val videoId = feature.removePrefix("video_shadowing:")
                            analytics.track("shadowing_video_selected", "entry_point" to "home")
                            ActiveSubScreen.ShadowingPlayer(videoId)
                        }
                        feature.equals("ai_tutor", ignoreCase = true) -> ActiveSubScreen.AiTutor
                        feature.equals("vocabulary", ignoreCase = true) || feature.equals("vocab", ignoreCase = true) -> ActiveSubScreen.VocabSetup
                        feature.equals("grammar", ignoreCase = true) -> ActiveSubScreen.GrammarSetup
                        feature.equals("speaking", ignoreCase = true) -> ActiveSubScreen.SpeakingSetup
                        feature.equals("shadowing", ignoreCase = true) -> ActiveSubScreen.ShadowingList
                        else -> ActiveSubScreen.AiTutor
                    }
                    if (featureName == "premium") {
                        analytics.track("upgrade_entry_tapped", "entry_point" to "main_content")
                    } else {
                        analytics.track(
                            "feature_opened",
                            "feature_name" to featureName,
                            "entry_point" to "main_content"
                        )
                    }
                    activeSubScreen = destination
                },
                onStatClick = { kind ->
                    analytics.track("progress_metric_opened", "metric_name" to kind.lowercase())
                    activeSubScreen = ActiveSubScreen.ProgressDetail(kind)
                },
                onSeeAllHistory = {
                    analytics.track("learning_history_opened", "entry_point" to "progress")
                    activeSubScreen = ActiveSubScreen.LearningHistory
                },
                onNavigateToLanguage = {
                    analytics.track("language_settings_opened", "entry_point" to "settings")
                    onOpenLanguageSettings()
                },
                onNativeLanguageSelected = { languageCode ->
                    if (languageCode != UserPrefs.nativeLanguage) {
                        analytics.track(
                            "language_preference_updated",
                            "language_role" to "native",
                            "language_code" to languageCode
                        )
                    }
                    viewModel.updateNativeLanguage(languageCode)
                },
                onLearnLanguageSelected = { languageCode ->
                    if (languageCode != UserPrefs.targetLanguage) {
                        analytics.track(
                            "language_preference_updated",
                            "language_role" to "learning",
                            "language_code" to languageCode
                        )
                    }
                    viewModel.updateLearningLanguage(languageCode)
                },
                languageSaveError = uiState.hasError,
                onNavigateToLegal = { doc ->
                    analytics.track("legal_document_opened", "document_type" to doc.lowercase())
                    AppLinks.documentUrl(doc)
                        .takeIf(String::isNotBlank)
                        ?.let(::openExternalUrl)
                },
                onContactSupport = {
                    analytics.track("support_contact_tapped")
                    AppLinks.supportEmailUrl()?.let(::openExternalUrl)
                },
                onShareApp = {
                    analytics.track("app_share_tapped")
                    sharePlainText(AppLinks.shareMessage(playStorePackageName()))
                },
                selectedTab = uiState.selectedTab,
                adsHost = adsHost,
                onHomeInteraction = { adsHost?.notifyHomeInteraction() },
                onTabSelected = ::selectMainTab
            )
        } else {
            when (val screen = activeSubScreen) {
                ActiveSubScreen.AiTutor -> {
                    AiTutorScreen(adsHost = adsHost, onBack = { completed ->
                        if (completed) {
                            UserPrefs.addLearnedMinutes(6)
                            UserPrefs.addLearning(title = "AI Tutor", score = 0, total = 0)
                        }
                        returnToMainFromBack { activeSubScreen = null }
                    })
                }
                is ActiveSubScreen.TodayLesson -> {
                    TodayLessonScreen(
                        title = screen.title,
                        level = screen.level,
                        systemBackRequest = lessonBackRequest,
                        adsHost = adsHost,
                        onBack = { completed ->
                            if (!completed) {
                                analytics.track(
                                    "learning_session_abandoned",
                                    "activity_type" to if (screen.title == null) "daily_lesson" else "practice_lesson"
                                )
                            }
                            returnToMainFromBack { activeSubScreen = null }
                        },
                        onFinish = { activeSubScreen = null }
                    )
                }
                is ActiveSubScreen.PlanTalk -> {
                    TodayLessonScreen(
                        title = screen.title,
                        level = screen.level,
                        day = screen.day,
                        titleLanguage = screen.titleLanguage,
                        loadedDayPlan = screen.plan,
                        systemBackRequest = lessonBackRequest,
                        adsHost = adsHost,
                        onBack = { completed ->
                            if (!completed) analytics.track("learning_session_abandoned", "activity_type" to "learning_plan_conversation")
                            activeSubScreen = ActiveSubScreen.PlanDay(screen.day)
                        },
                        onFinish = { activeSubScreen = ActiveSubScreen.PlanDay(screen.day) }
                    )
                }
                ActiveSubScreen.VocabSetup -> {
                    VocabSetupScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onStart = { count ->
                            analytics.track(
                                "learning_practice_setup_completed",
                                "activity_type" to "vocabulary",
                                "item_count" to count
                            )
                            analytics.track(
                                "learning_session_start",
                                "activity_type" to "vocabulary",
                                "item_count" to count,
                                "content_category" to PracticeConfig.vocabCategory.lowercase(),
                                "difficulty" to PracticeConfig.vocabLevel.lowercase()
                            )
                            activeSubScreen = ActiveSubScreen.VocabCards(count)
                        }
                    )
                }
                is ActiveSubScreen.VocabCards -> {
                    VocabCardsScreen(
                        wordCount = screen.count,
                        onClose = { _, _ ->
                            analytics.track("learning_session_abandoned", "activity_type" to "vocabulary")
                            returnToMainFromBack { activeSubScreen = null }
                        },
                        onFinish = { correct, total ->
                            analytics.track(
                                "learning_session_completed",
                                "activity_type" to "vocabulary",
                                "correct_count" to correct,
                                "total_items" to total,
                                "score_percent" to if (total > 0) correct * 100 / total else 0
                            )
                            activeSubScreen = null
                        }
                    )
                }
                ActiveSubScreen.GrammarSetup -> {
                    GrammarSetupScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onStart = { count ->
                            analytics.track(
                                "learning_practice_setup_completed",
                                "activity_type" to "grammar_quiz",
                                "item_count" to count
                            )
                            analytics.track(
                                "learning_session_start",
                                "activity_type" to "grammar_quiz",
                                "item_count" to count,
                                "grammar_topic" to PracticeConfig.grammarType.lowercase(),
                                "difficulty" to PracticeConfig.grammarDifficulty.lowercase()
                            )
                            activeSubScreen = ActiveSubScreen.GrammarQuiz(count)
                        }
                    )
                }
                is ActiveSubScreen.GrammarQuiz -> {
                    GrammarQuizScreen(
                        questionCount = screen.count,
                        onClose = { _, _ ->
                            analytics.track("learning_session_abandoned", "activity_type" to "grammar_quiz")
                            returnToMainFromBack { activeSubScreen = null }
                        },
                        onFinish = { correct, total ->
                            analytics.track(
                                "learning_session_completed",
                                "activity_type" to "grammar_quiz",
                                "correct_count" to correct,
                                "total_items" to total,
                                "score_percent" to if (total > 0) correct * 100 / total else 0
                            )
                            activeSubScreen = null
                        }
                    )
                }
                ActiveSubScreen.SpeakingSetup -> {
                    SpeakingSetupScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onStart = { count ->
                            analytics.track(
                                "learning_practice_setup_completed",
                                "activity_type" to "speaking",
                                "item_count" to count
                            )
                            analytics.track(
                                "learning_session_start",
                                "activity_type" to "speaking",
                                "item_count" to count,
                                "practice_type" to PracticeConfig.speakingType.lowercase(),
                                "difficulty" to PracticeConfig.speakingDifficulty.lowercase()
                            )
                            activeSubScreen = ActiveSubScreen.SpeakingCards(count)
                        }
                    )
                }
                is ActiveSubScreen.SpeakingCards -> {
                    SpeakingCardsScreen(
                        cardCount = screen.count,
                        onClose = { score, finished ->
                            UserPrefs.markFeatureUsed("speaking")
                            if (finished) {
                                analytics.track(
                                    "learning_session_completed",
                                    "activity_type" to "speaking",
                                    "average_score" to score,
                                    "total_items" to screen.count
                                )
                                UserPrefs.markSpeakingDone(PracticeConfig.speakingDifficulty, PracticeConfig.speakingType)
                                activeSubScreen = null
                            } else {
                                analytics.track("learning_session_abandoned", "activity_type" to "speaking")
                                returnToMainFromBack { activeSubScreen = null }
                            }
                        }
                    )
                }
                ActiveSubScreen.ShadowingList -> {
                    VideoShadowingListScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onOpenVideo = { videoId ->
                            analytics.track("shadowing_video_selected", "entry_point" to "shadowing_catalog")
                            activeSubScreen = ActiveSubScreen.ShadowingPlayer(videoId)
                        },
                        onOpenSavedWords = { showSavedWords = true }
                    )
                }
                is ActiveSubScreen.ShadowingPlayer -> {
                    VideoShadowingPlayerScreen(
                        videoId = screen.videoId,
                        analyticsActivityType = "video_shadowing",
                        onClose = { activeSubScreen = ActiveSubScreen.ShadowingList },
                        onOpenSavedWords = { showSavedWords = true }
                    )
                }
                is ActiveSubScreen.PlanVideo -> {
                    VideoShadowingPlayerScreen(
                        videoId = screen.videoId,
                        analyticsActivityType = "learning_plan_shadowing",
                        analyticsDayNumber = screen.day,
                        onClose = { activeSubScreen = ActiveSubScreen.PlanDay(screen.day) },
                        onOpenSavedWords = { showSavedWords = true },
                        onSessionCompleted = {
                            // Publish completion before the player routes directly to the quick quiz.
                            PlanProgress.get().markDone(screen.day, PlanProgress.Activity.VIDEO)
                            planScope.launch {
                                PlanFlow.finish(screen.day, PlanProgress.Activity.VIDEO)
                            }
                        },
                        onContinueToQuickQuiz = {
                            activeSubScreen = ActiveSubScreen.PlanSession(screen.day)
                        }
                    )
                }
                is ActiveSubScreen.PathLesson -> {
                    PathLessonScreen(
                        lessonId = screen.lessonId,
                        onClose = { returnToMainFromBack { activeSubScreen = null } },
                        onFinish = { activeSubScreen = null }
                    )
                }
                ActiveSubScreen.PlanSetup -> {
                    PlanSetupScreen(
                        adsHost = adsHost,
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onDone = {
                            analytics.track("learning_plan_generation_started")
                            activeSubScreen = ActiveSubScreen.PlanGenerating
                        }
                    )
                }
                ActiveSubScreen.PlanGenerating -> {
                    PlanGeneratingScreen(
                        onDone = {
                            analytics.track("learning_plan_generation_completed")
                            activeSubScreen = ActiveSubScreen.PlanRoadmap
                        }
                    )
                }
                ActiveSubScreen.PlanRoadmap -> {
                    PlanScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onChangeGoal = {
                            analytics.track("learning_plan_edit_started")
                            activeSubScreen = ActiveSubScreen.PlanSetup
                        },
                        onOpenDay = { day ->
                            analytics.track("learning_plan_day_opened", "day_number" to day)
                            activeSubScreen = ActiveSubScreen.PlanDay(day)
                        }
                    )
                }
                is ActiveSubScreen.PlanDay -> {
                    PlanDayActivitiesScreen(
                        day = screen.day,
                        adsHost = adsHost,
                        onBack = { activeSubScreen = ActiveSubScreen.PlanRoadmap },
                        onOpenActivity = { activity, plan ->
                            analytics.track(
                                "learning_plan_activity_started",
                                "activity_type" to activity.name.lowercase(),
                                "day_number" to screen.day
                            )
                            activeSubScreen = when (activity) {
                                PlanProgress.Activity.VIDEO -> plan.videoId?.let { ActiveSubScreen.PlanVideo(screen.day, it) }
                                    ?: ActiveSubScreen.PlanDay(screen.day)
                                PlanProgress.Activity.QUIZ -> ActiveSubScreen.PlanSession(screen.day)
                                PlanProgress.Activity.SPEAKING -> ActiveSubScreen.PlanSpeaking(screen.day)
                                PlanProgress.Activity.TALK -> ActiveSubScreen.PlanTalk(
                                    day = screen.day,
                                    title = plan.stage.displayTitle(GoalPlan.learnCode(UserPrefs.targetLanguage)),
                                    level = UserPrefs.englishLevel,
                                    titleLanguage = plan.stage.displayTitleLanguage(GoalPlan.learnCode(UserPrefs.targetLanguage)),
                                    plan = plan
                                )
                            }
                        }
                    )
                }
                is ActiveSubScreen.PlanSession -> {
                    PlanSessionScreen(
                        day = screen.day,
                        onBack = {
                            analytics.track("learning_session_abandoned", "activity_type" to "learning_plan_quiz")
                            activeSubScreen = ActiveSubScreen.PlanDay(screen.day)
                        },
                        onFinish = {
                            analytics.track("learning_session_completed", "activity_type" to "learning_plan_quiz")
                            activeSubScreen = ActiveSubScreen.PlanDay(screen.day)
                        }
                    )
                }
                is ActiveSubScreen.PlanSpeaking -> {
                    PlanSpeakingSessionScreen(
                        day = screen.day,
                        onBack = {
                            analytics.track("learning_session_abandoned", "activity_type" to "learning_plan_speaking")
                            activeSubScreen = ActiveSubScreen.PlanDay(screen.day)
                        },
                        onFinish = {
                            analytics.track("learning_session_completed", "activity_type" to "learning_plan_speaking")
                            activeSubScreen = ActiveSubScreen.PlanDay(screen.day)
                        }
                    )
                }
                is ActiveSubScreen.ProgressDetail -> {
                    ProgressDetailScreen(
                        kind = screen.kind,
                        adsHost = adsHost,
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onStartPractice = {
                            analytics.track(
                                "progress_practice_started",
                                "metric_name" to screen.kind.lowercase()
                            )
                            activeSubScreen = when (screen.kind.lowercase()) {
                                "grammar" -> ActiveSubScreen.GrammarSetup
                                "speaking" -> ActiveSubScreen.SpeakingSetup
                                else -> ActiveSubScreen.VocabSetup
                            }
                        }
                    )
                }
                ActiveSubScreen.LearningHistory -> {
                    LearningHistoryScreen(
                        adsHost = adsHost,
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onOpenLesson = { title ->
                            val destination = when {
                                title.equals("AI Tutor", ignoreCase = true) || title.startsWith("AI Tutor ", ignoreCase = true) -> ActiveSubScreen.AiTutor
                                title.equals("Today's Lesson", ignoreCase = true) -> ActiveSubScreen.TodayLesson()
                                title.contains("Vocabulary", ignoreCase = true) -> ActiveSubScreen.VocabSetup
                                title.contains("Speaking", ignoreCase = true) -> ActiveSubScreen.SpeakingSetup
                                title.contains("Grammar", ignoreCase = true) -> ActiveSubScreen.GrammarSetup
                                else -> ActiveSubScreen.TodayLesson(title = title, level = "Intermediate")
                            }
                            analytics.track("learning_history_item_opened", "screen_name" to destination.analyticsScreenName())
                            activeSubScreen = destination
                        },
                        onStartLearning = {
                            analytics.track("learning_session_start", "activity_type" to "daily_lesson")
                            activeSubScreen = ActiveSubScreen.TodayLesson()
                        }
                    )
                }
                null -> {}
            }
        }

        if (showSavedWords) {
            SavedWordsScreen(onBack = { showSavedWords = false })
        }
    }
}
