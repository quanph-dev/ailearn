package com.example.basekmp

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
import com.example.basekmp.ui.onboarding.FirstOpenIntroScreen
import com.example.basekmp.ui.onboarding.FirstOpenSurveyScreen
import com.example.basekmp.ui.onboarding.OnboardingState
import com.example.basekmp.theme.BrandPrimary
import com.example.basekmp.theme.BrandPrimaryPressed
import com.example.basekmp.theme.DividerColor
import com.example.basekmp.theme.EnglishLearningAppTheme
import com.example.basekmp.theme.GradientBottom
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.OptionSurfaceSelected
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.grammar.GrammarQuizScreen
import com.example.basekmp.ui.grammar.GrammarSetupScreen
import com.example.basekmp.ui.learningpath.PathLessonScreen
import com.example.basekmp.ui.main.MainScreen
import com.example.basekmp.ui.onboarding.OnboardingScreen
import com.example.basekmp.ui.plan.PlanGeneratingScreen
import com.example.basekmp.ui.plan.PlanDayActivitiesScreen
import com.example.basekmp.ui.plan.PlanScreen
import com.example.basekmp.ui.plan.PlanSessionScreen
import com.example.basekmp.ui.plan.PlanSpeakingSessionScreen
import com.example.basekmp.ui.plan.PlanSetupScreen
import com.example.basekmp.data.GoalPlan
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.PracticeConfig
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.ui.plan.PlanFlow
import com.example.basekmp.ui.progress.LearningHistoryScreen
import com.example.basekmp.ui.progress.ProgressDetailScreen
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import com.example.basekmp.data.appStringResource as stringResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun App(
    viewModel: BaseAppViewModel,
    adsHost: AppAdsHost? = null,
    onExitApp: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }
    var introGateInProgress by remember { mutableStateOf(false) }
    val appScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val splashPhase = adsHost?.splashPhase?.collectAsStateWithLifecycle()?.value ?: 2
    val isLoadingAds = adsHost?.isInterstitialLoading?.collectAsStateWithLifecycle()?.value ?: false
    val fullscreenNativeState = adsHost?.fullscreenNativeState?.collectAsStateWithLifecycle()?.value

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
                                if (page > 0 && uiState.firstOpenPage == 0) {
                                    passFirstIntroFullscreen { viewModel.selectFirstOpenPage(1) }
                                } else {
                                    viewModel.selectFirstOpenPage(page)
                                }
                            },
                            onNext = {
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
                            onSelectLanguage = viewModel::selectFirstOpenSurveyLanguage,
                            onContinue = { viewModel.finishFirstOpenSurvey() }
                        )
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
                        onDismissRequest = { showExitConfirmation = false },
                        onExitApp = onExitApp
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
                        onClick = { viewModel.confirmLanguage() }
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
                        onClick = { viewModel.selectLanguage(option.code) }
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

    fun returnToMainFromBack(action: () -> Unit) {
        if (backOperationRunning) return
        backOperationRunning = true
        appScope.launch {
            try {
                adsHost?.showInterstitial("I_Back", timeoutMs = 8_000L)
                action()
            } finally {
                backOperationRunning = false
            }
        }
    }

    fun selectMainTab(tab: com.example.basekmp.ui.components.MainTab) {
        val currentTab = uiState.selectedTab
        if (tab == currentTab || tabOperationRunning) return
        if (tab == com.example.basekmp.ui.components.MainTab.Settings || currentTab == com.example.basekmp.ui.components.MainTab.Settings) {
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
        if (uiState.selectedTab != com.example.basekmp.ui.components.MainTab.Home) {
            if (uiState.selectedTab == com.example.basekmp.ui.components.MainTab.Settings) {
                viewModel.selectTab(com.example.basekmp.ui.components.MainTab.Home)
            } else {
                returnToMainFromBack { viewModel.selectTab(com.example.basekmp.ui.components.MainTab.Home) }
            }
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
                    activeSubScreen = ActiveSubScreen.PlanDay(day)
                },
                onOpenLesson = { title ->
                    activeSubScreen = when {
                        title.equals("AI Tutor", ignoreCase = true) || title.startsWith("AI Tutor ", ignoreCase = true) -> ActiveSubScreen.AiTutor
                        title.equals("Today's Lesson", ignoreCase = true) -> ActiveSubScreen.TodayLesson()
                        title.contains("Vocabulary", ignoreCase = true) -> ActiveSubScreen.VocabSetup
                        title.contains("Speaking", ignoreCase = true) -> ActiveSubScreen.SpeakingSetup
                        title.contains("Grammar", ignoreCase = true) -> ActiveSubScreen.GrammarSetup
                        else -> ActiveSubScreen.TodayLesson(title = title, level = "Intermediate")
                    }
                },
                onFeatureClick = { feature ->
                    activeSubScreen = when {
                        feature.equals("premium", ignoreCase = true) -> null
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
                onNativeLanguageSelected = viewModel::updateNativeLanguage,
                onLearnLanguageSelected = viewModel::updateLearningLanguage,
                languageSaveError = uiState.hasError,
                onNavigateToLegal = { doc ->
                    AppLinks.documentUrl(doc)
                        .takeIf(String::isNotBlank)
                        ?.let(::openExternalUrl)
                },
                onContactSupport = {
                    AppLinks.supportEmailUrl()?.let(::openExternalUrl)
                },
                onShareApp = { sharePlainText(AppLinks.shareMessage(playStorePackageName())) },
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
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
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
                        onBack = { activeSubScreen = ActiveSubScreen.PlanDay(screen.day) },
                        onFinish = { activeSubScreen = ActiveSubScreen.PlanDay(screen.day) }
                    )
                }
                ActiveSubScreen.VocabSetup -> {
                    VocabSetupScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onStart = { count -> activeSubScreen = ActiveSubScreen.VocabCards(count) }
                    )
                }
                is ActiveSubScreen.VocabCards -> {
                    VocabCardsScreen(
                        wordCount = screen.count,
                        onClose = { _, _ ->
                            returnToMainFromBack { activeSubScreen = null }
                        },
                        onFinish = { _, _ -> activeSubScreen = null }
                    )
                }
                ActiveSubScreen.GrammarSetup -> {
                    GrammarSetupScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onStart = { count -> activeSubScreen = ActiveSubScreen.GrammarQuiz(count) }
                    )
                }
                is ActiveSubScreen.GrammarQuiz -> {
                    GrammarQuizScreen(
                        questionCount = screen.count,
                        onClose = { _, _ ->
                            returnToMainFromBack { activeSubScreen = null }
                        },
                        onFinish = { _, _ -> activeSubScreen = null }
                    )
                }
                ActiveSubScreen.SpeakingSetup -> {
                    SpeakingSetupScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onStart = { count -> activeSubScreen = ActiveSubScreen.SpeakingCards(count) }
                    )
                }
                is ActiveSubScreen.SpeakingCards -> {
                    SpeakingCardsScreen(
                        cardCount = screen.count,
                        onClose = { _, finished ->
                            UserPrefs.markFeatureUsed("speaking")
                            if (finished) {
                                UserPrefs.markSpeakingDone(PracticeConfig.speakingDifficulty, PracticeConfig.speakingType)
                                activeSubScreen = null
                            } else {
                                returnToMainFromBack { activeSubScreen = null }
                            }
                        }
                    )
                }
                ActiveSubScreen.ShadowingList -> {
                    VideoShadowingListScreen(
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onOpenVideo = { videoId -> activeSubScreen = ActiveSubScreen.ShadowingPlayer(videoId) },
                        onOpenSavedWords = { showSavedWords = true }
                    )
                }
                is ActiveSubScreen.ShadowingPlayer -> {
                    VideoShadowingPlayerScreen(
                        videoId = screen.videoId,
                        onClose = { activeSubScreen = ActiveSubScreen.ShadowingList },
                        onOpenSavedWords = { showSavedWords = true }
                    )
                }
                is ActiveSubScreen.PlanVideo -> {
                    VideoShadowingPlayerScreen(
                        videoId = screen.videoId,
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
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onChangeGoal = { activeSubScreen = ActiveSubScreen.PlanSetup },
                        onOpenDay = { day -> activeSubScreen = ActiveSubScreen.PlanDay(day) }
                    )
                }
                is ActiveSubScreen.PlanDay -> {
                    PlanDayActivitiesScreen(
                        day = screen.day,
                        adsHost = adsHost,
                        onBack = { activeSubScreen = ActiveSubScreen.PlanRoadmap },
                        onOpenActivity = { activity, plan ->
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
                        onBack = { activeSubScreen = ActiveSubScreen.PlanDay(screen.day) },
                        onFinish = { activeSubScreen = ActiveSubScreen.PlanDay(screen.day) }
                    )
                }
                is ActiveSubScreen.PlanSpeaking -> {
                    PlanSpeakingSessionScreen(
                        day = screen.day,
                        onBack = { activeSubScreen = ActiveSubScreen.PlanDay(screen.day) },
                        onFinish = { activeSubScreen = ActiveSubScreen.PlanDay(screen.day) }
                    )
                }
                is ActiveSubScreen.ProgressDetail -> {
                    ProgressDetailScreen(
                        kind = screen.kind,
                        adsHost = adsHost,
                        onBack = { returnToMainFromBack { activeSubScreen = null } },
                        onStartPractice = {
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
                            activeSubScreen = when {
                                title.equals("AI Tutor", ignoreCase = true) || title.startsWith("AI Tutor ", ignoreCase = true) -> ActiveSubScreen.AiTutor
                                title.equals("Today's Lesson", ignoreCase = true) -> ActiveSubScreen.TodayLesson()
                                title.contains("Vocabulary", ignoreCase = true) -> ActiveSubScreen.VocabSetup
                                title.contains("Speaking", ignoreCase = true) -> ActiveSubScreen.SpeakingSetup
                                title.contains("Grammar", ignoreCase = true) -> ActiveSubScreen.GrammarSetup
                                else -> ActiveSubScreen.TodayLesson(title = title, level = "Intermediate")
                            }
                        },
                        onStartLearning = {
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
