package com.example.basekmp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekmp.data.EnglishLevel
import com.example.basekmp.data.Languages
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.RemoteConfig
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.ui.components.MainTab
import com.example.basekmp.ui.onboarding.OnboardingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BaseAppUiState(
    val route: String = BaseRoutes.Splash,
    val startupTargetRoute: String = "",
    val isReturningUser: Boolean = false,
    val firstOpenSurveyAdTargetRoute: String = "",
    val languageCode: String = "en",
    val learningLanguageCode: String = "en",
    val selectedLanguageCode: String = "",
    val hasUserSelectedLanguage: Boolean = false,
    val onboardingPage: Int = 0,
    val firstOpenPage: Int = 0,
    val firstOpenSurveyLanguage: String = "",
    val onboardingProgress: String = "",
    val selectedTab: MainTab = MainTab.Home,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasError: Boolean = false
) {
    val nativeLanguageCode: String get() = languageCode
}

open class BaseAppViewModel(
    private val repository: BaseAppRepository
) : ViewModel() {
    val deviceLanguageCode: String = supportedLanguageCode(getSystemLanguageCode())

    private val _uiState = MutableStateFlow(BaseAppUiState(languageCode = deviceLanguageCode))
    val uiState: StateFlow<BaseAppUiState> = _uiState.asStateFlow()

    private var isHandlingAction = false
    private var onboardingProgressSaveJob: Job? = null
    private val languageSaveMutex = Mutex()
    private val learningProgressSaveMutex = Mutex()

    init {
        UserPrefs.setProgressPersistenceSink(null)
        PlanProgress.get().setPersistenceSink { serialized ->
            viewModelScope.launch {
                runCatchingCancellable { repository.savePlanProgress(serialized) }
            }
        }
        viewModelScope.launch {
            val route = runCatchingCancellable { repository.resolveStartupRoute() }
                .getOrDefault(BaseRoutes.FirstOpenIntro)
            val isReturningUser = runCatchingCancellable { repository.isReturningAppOpen() }.getOrDefault(false)
            val savedLanguageCode = runCatchingCancellable { repository.currentLanguageCode() }.getOrDefault("")
            val savedLearningLanguageCode = runCatchingCancellable { repository.currentLearningLanguageCode() }.getOrDefault("")
            val savedOnboardingProgress = runCatchingCancellable { repository.onboardingProgress() }.getOrDefault("")
            val surveyLanguage = runCatchingCancellable { repository.firstOpenSurveyLanguage() }.getOrDefault("")
            val savedPlanProgress = runCatchingCancellable { repository.planProgress() }.getOrDefault("")
            val savedLearningProgress = runCatchingCancellable { repository.learningProgress() }.getOrDefault("")
            if (savedPlanProgress.isNotBlank()) PlanProgress.get().restore(savedPlanProgress)
            val restoredLearningProgress = savedLearningProgress.isNotBlank() &&
                UserPrefs.restoreLearningProgress(savedLearningProgress)
            val restoredOnboarding = savedOnboardingProgress.takeIf { it.isNotBlank() }?.let { OnboardingState.deserialize(it) }
            val profileNativeWasConfirmed = restoredOnboarding?.step?.ordinal?.let {
                it > com.example.basekmp.ui.onboarding.OnboardingStep.NativeLanguage.ordinal
            } == true
            val restoredNative = when {
                profileNativeWasConfirmed && restoredLearningProgress -> UserPrefs.nativeLanguage
                profileNativeWasConfirmed -> restoredOnboarding?.nativeLanguage
                restoredOnboarding == null && route == BaseRoutes.Home && restoredLearningProgress -> UserPrefs.nativeLanguage
                else -> null
            }
            val restoredNativeWasConfirmed = restoredNative != null
            val languageCode = Languages.resolveNativeLanguage(
                saved = savedLanguageCode,
                restored = restoredNative,
                system = getSystemLanguageCode()
            )
            applyPlatformLanguage(languageCode)
            Strings.select(languageCode)
            delay(SPLASH_MINIMUM_DURATION_MS)
            UserPrefs.onboardingDone = route == BaseRoutes.Home
            UserPrefs.firstOpenDone = route == BaseRoutes.Home || route == BaseRoutes.Onboarding
            if (!restoredLearningProgress && savedOnboardingProgress.isNotBlank()) {
                val restoredProfile = OnboardingState.deserialize(savedOnboardingProgress)
                applyOnboardingProfile(
                    restoredProfile,
                    includeNativeLanguage = restoredProfile.step !=
                        com.example.basekmp.ui.onboarding.OnboardingStep.NativeLanguage
                )
            }
            if (!restoredLearningProgress && surveyLanguage.isNotBlank()) {
                UserPrefs.learningLanguage = surveyLanguage
                UserPrefs.learnLanguageFromSurvey = true
            }
            if (Languages.learnable.any { it.code == savedLearningLanguageCode }) {
                UserPrefs.learningLanguage = savedLearningLanguageCode
            } else {
                runCatchingCancellable { repository.saveLearningLanguageCode(UserPrefs.learningLanguage) }
            }
            // The selected UI locale is also the native language; restore it after
            // the learning snapshot so an older snapshot cannot overwrite it.
            UserPrefs.nativeLanguage = languageCode
            if (savedLanguageCode != languageCode && restoredNativeWasConfirmed) {
                runCatchingCancellable { repository.saveLanguage(languageCode, completeSetup = false) }
            }
            UserPrefs.setProgressPersistenceSink { serialized ->
                viewModelScope.launch {
                    learningProgressSaveMutex.withLock {
                        runCatchingCancellable { repository.saveLearningProgress(serialized) }
                    }
                }
            }
            if (!restoredLearningProgress && savedOnboardingProgress.isNotBlank()) {
                runCatchingCancellable { repository.saveLearningProgress(UserPrefs.serializeLearningProgress()) }
            }
            _uiState.value = _uiState.value.copy(
                route = BaseRoutes.Splash,
                startupTargetRoute = route,
                isReturningUser = isReturningUser,
                languageCode = languageCode,
                learningLanguageCode = UserPrefs.learningLanguage,
                selectedLanguageCode = savedLanguageCode,
                firstOpenSurveyLanguage = surveyLanguage,
                onboardingProgress = savedOnboardingProgress,
                isLoading = false
            )
        }
    }

    fun finishStartup() {
        val current = _uiState.value
        if (current.route != BaseRoutes.Splash || current.startupTargetRoute.isBlank()) return
        _uiState.value = current.copy(route = current.startupTargetRoute)
    }

    fun markHomeViewed() {
        if (_uiState.value.route != BaseRoutes.Home) return
        viewModelScope.launch {
            runCatchingCancellable { repository.markHomeViewed() }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun updateNativeLanguage(code: String) {
        if (Languages.native.none { it.code == code }) return
        viewModelScope.launch {
            languageSaveMutex.withLock {
                val saved = runCatchingCancellable { repository.saveLanguage(code, completeSetup = false) }.isSuccess
                if (!saved) {
                    _uiState.value = _uiState.value.copy(hasError = true)
                    return@withLock
                }
                UserPrefs.nativeLanguage = code
                applyPlatformLanguage(code)
                Strings.select(code)
                _uiState.value = _uiState.value.copy(languageCode = code, selectedLanguageCode = code, hasError = false)
            }
        }
    }

    fun updateLearningLanguage(code: String) {
        if (Languages.learnable.none { it.code == code }) return
        viewModelScope.launch {
            languageSaveMutex.withLock {
                val saved = runCatchingCancellable { repository.saveLearningLanguageCode(code) }.isSuccess
                if (!saved) {
                    _uiState.value = _uiState.value.copy(hasError = true)
                    return@withLock
                }
                UserPrefs.setLearnLanguage(code)
                _uiState.value = _uiState.value.copy(learningLanguageCode = code, hasError = false)
            }
        }
    }

    fun selectLanguage(code: String) {
        if (_uiState.value.isSaving) return
        if (code in BaseLanguages.map { it.code }) {
            _uiState.value = _uiState.value.copy(
                selectedLanguageCode = code,
                hasUserSelectedLanguage = true,
                hasError = false
            )
        }
    }

    fun confirmLanguage() {
        val current = _uiState.value
        if (
            isHandlingAction ||
            current.route !in setOf(BaseRoutes.StartupLanguage, BaseRoutes.LanguageSettings) ||
            current.selectedLanguageCode.isBlank() ||
            !current.hasUserSelectedLanguage
        ) return
        isHandlingAction = true
        _uiState.value = current.copy(isSaving = true, hasError = false)
        viewModelScope.launch {
            val isSettings = current.route == BaseRoutes.LanguageSettings
            val completed = runCatchingCancellable {
                repository.saveLanguage(
                    current.selectedLanguageCode,
                    completeSetup = !isSettings
                )
            }.isSuccess
            if (completed) {
                applyPlatformLanguage(current.selectedLanguageCode)
                Strings.select(current.selectedLanguageCode)
                UserPrefs.nativeLanguage = current.selectedLanguageCode
                _uiState.value = current.copy(
                    route = if (isSettings) BaseRoutes.Home else BaseRoutes.ApplyLanguage,
                    languageCode = current.selectedLanguageCode,
                    selectedLanguageCode = current.selectedLanguageCode,
                    isSaving = false,
                    hasError = false
                )
            } else {
                _uiState.value = current.copy(isSaving = false, hasError = true)
            }
            isHandlingAction = false
        }
    }

    fun finishApplyLanguage() {
        if (_uiState.value.route == BaseRoutes.ApplyLanguage) {
            _uiState.value = _uiState.value.copy(
                route = BaseRoutes.FirstOpenIntro,
                firstOpenPage = 0
            )
        }
    }

    fun advanceFirstOpenIntro() {
        val current = _uiState.value
        if (isHandlingAction || current.route != BaseRoutes.FirstOpenIntro || current.isSaving) return
        if (current.firstOpenPage < FIRST_OPEN_PAGE_COUNT - 1) {
            _uiState.value = current.copy(firstOpenPage = current.firstOpenPage + 1)
            return
        }
        _uiState.value = current.copy(route = BaseRoutes.FirstOpenSurvey)
    }

    fun backFirstOpenIntro() {
        val current = _uiState.value
        if (current.route == BaseRoutes.FirstOpenIntro && current.firstOpenPage > 0) {
            _uiState.value = current.copy(firstOpenPage = current.firstOpenPage - 1)
        }
    }

    fun selectFirstOpenPage(page: Int) {
        if (_uiState.value.route != BaseRoutes.FirstOpenIntro) return
        _uiState.value = _uiState.value.copy(firstOpenPage = page.coerceIn(0, FIRST_OPEN_PAGE_COUNT - 1))
    }

    fun backToFirstOpenIntro() {
        val current = _uiState.value
        if (current.route == BaseRoutes.FirstOpenSurvey) {
            _uiState.value = current.copy(
                route = BaseRoutes.FirstOpenIntro,
                firstOpenPage = FIRST_OPEN_PAGE_COUNT - 1
            )
        }
    }

    fun selectFirstOpenSurveyLanguage(code: String) {
        if (_uiState.value.route != BaseRoutes.FirstOpenSurvey) return
        if (Languages.surveyOptions.none { it.code == code }) return
        _uiState.value = _uiState.value.copy(
            firstOpenSurveyLanguage = code,
            hasError = false
        )
    }

    fun finishFirstOpenSurvey() {
        val current = _uiState.value
        if (isHandlingAction || current.route != BaseRoutes.FirstOpenSurvey || current.isSaving) return
        val selectedLanguage = current.firstOpenSurveyLanguage.takeIf { it.isNotBlank() } ?: return
        val nativeLanguage = Languages.defaultNativeLanguageCode(getSystemLanguageCode())
        val initialProfile = OnboardingState(
            step = com.example.basekmp.ui.onboarding.OnboardingStep.NativeLanguage,
            skipLearnLanguage = true,
            learnLanguage = selectedLanguage,
            nativeLanguage = nativeLanguage
        )
        isHandlingAction = true
        _uiState.value = current.copy(isSaving = true, hasError = false)
        viewModelScope.launch {
            val result = runCatchingCancellable {
                repository.completeFirstOpen(selectedLanguage, initialProfile.serialize())
            }
            if (result.isSuccess) {
                applyOnboardingProfile(initialProfile)
                UserPrefs.learningLanguage = selectedLanguage
                UserPrefs.learnLanguageFromSurvey = true
                UserPrefs.firstOpenDone = true
                val nextRoute = if (RemoteConfig.surveyBeforeHome) BaseRoutes.Onboarding else BaseRoutes.Home
                if (!RemoteConfig.surveyBeforeHome) {
                    UserPrefs.onboardingDone = true
                }
                _uiState.value = current.copy(
                    route = BaseRoutes.FirstOpenSurveyAd,
                    firstOpenSurveyAdTargetRoute = nextRoute,
                    firstOpenSurveyLanguage = selectedLanguage,
                    learningLanguageCode = selectedLanguage,
                    onboardingProgress = initialProfile.serialize(),
                    isSaving = false,
                    hasError = false
                )
            } else {
                _uiState.value = current.copy(isSaving = false, hasError = true)
            }
            isHandlingAction = false
        }
    }

    fun finishFirstOpenSurveyAd() {
        val current = _uiState.value
        if (current.route == BaseRoutes.FirstOpenSurveyAd) {
            val nextRoute = current.firstOpenSurveyAdTargetRoute
                .takeIf { it in setOf(BaseRoutes.Onboarding, BaseRoutes.Home) }
                ?: BaseRoutes.Onboarding
            _uiState.value = current.copy(route = nextRoute, firstOpenSurveyAdTargetRoute = "")
        }
    }

    fun updateOnboardingProgress(serialized: String) {
        val current = _uiState.value
        if (current.route != BaseRoutes.Onboarding || current.isSaving) return
        val profile = OnboardingState.deserialize(serialized)
        val previousProfile = OnboardingState.deserialize(current.onboardingProgress)
        val confirmedNativeLanguage = profile.nativeLanguage
            ?.takeIf {
                previousProfile.step == com.example.basekmp.ui.onboarding.OnboardingStep.NativeLanguage &&
                    profile.step == previousProfile.next().step &&
                    it in Languages.native.map { language -> language.code }
            }
        val newLangCode = confirmedNativeLanguage ?: current.languageCode
        if (confirmedNativeLanguage != null) {
            UserPrefs.nativeLanguage = confirmedNativeLanguage
            viewModelScope.launch {
                languageSaveMutex.withLock {
                    if (runCatchingCancellable { repository.saveLanguage(confirmedNativeLanguage, completeSetup = false) }.isFailure) {
                        _uiState.value = _uiState.value.copy(hasError = true)
                    }
                }
            }
        }
        if (newLangCode != current.languageCode) {
            applyPlatformLanguage(newLangCode)
            Strings.select(newLangCode)
        }
        _uiState.value = current.copy(
            languageCode = newLangCode,
            onboardingProgress = serialized,
            hasError = false
        )
        onboardingProgressSaveJob?.cancel()
        onboardingProgressSaveJob = viewModelScope.launch {
            delay(150)
            val result = runCatchingCancellable {
                repository.saveOnboardingProgress(serialized)
            }
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(hasError = true)
            }
        }
    }

    fun completeOnboarding(serialized: String) {
        val current = _uiState.value
        val profile = OnboardingState.deserialize(serialized)
        if (
            isHandlingAction || current.route != BaseRoutes.Onboarding || current.isSaving ||
            !profile.isLastStep || profile.currentSelection.isNullOrBlank()
        ) return
        onboardingProgressSaveJob?.cancel()
        isHandlingAction = true
        val targetLang = profile.nativeLanguage?.takeIf { code -> Languages.native.any { it.code == code } }
            ?: current.languageCode
        _uiState.value = current.copy(onboardingProgress = serialized, isSaving = true, hasError = false)
        viewModelScope.launch {
            val result = runCatchingCancellable {
                repository.completeOnboarding(serialized)
                languageSaveMutex.withLock {
                    repository.saveLanguage(targetLang, completeSetup = true)
                    profile.learnLanguage?.let { repository.saveLearningLanguageCode(it) }
                }
            }
            if (result.isSuccess) {
                applyOnboardingProfile(profile)
                applyPlatformLanguage(targetLang)
                Strings.select(targetLang)
                UserPrefs.nativeLanguage = targetLang
                UserPrefs.onboardingDone = true
                _uiState.value = current.copy(
                    route = BaseRoutes.Starting,
                    languageCode = targetLang,
                    learningLanguageCode = UserPrefs.learningLanguage,
                    selectedLanguageCode = targetLang,
                    onboardingProgress = serialized,
                    isSaving = false,
                    hasError = false
                )
            } else {
                _uiState.value = current.copy(
                    onboardingProgress = serialized,
                    isSaving = false,
                    hasError = true
                )
            }
            isHandlingAction = false
        }
    }

    fun finishStartInterstitial() {
        if (_uiState.value.route == BaseRoutes.Starting) {
            _uiState.value = _uiState.value.copy(route = BaseRoutes.Home)
        }
    }

    private fun applyOnboardingProfile(
        profile: OnboardingState,
        includeNativeLanguage: Boolean = true
    ) {
        profile.learnLanguage?.let { UserPrefs.learningLanguage = it }
        if (includeNativeLanguage) {
            profile.nativeLanguage?.let { UserPrefs.nativeLanguage = it }
        }
        profile.level?.let { selected ->
            UserPrefs.englishLevel = EnglishLevel.toFullName(selected)
            UserPrefs.englishLevelChosen = true
        }
        profile.dailyMinutes?.toIntOrNull()?.let { UserPrefs.dailyGoalMinutes = it }
        profile.tutorVoice?.let { UserPrefs.tutorVoice = it.replaceFirstChar { first -> first.uppercase() } }
    }

    fun openLanguageSettings() {
        if (_uiState.value.route != BaseRoutes.Home) return
        _uiState.value = _uiState.value.copy(
            route = BaseRoutes.LanguageSettings,
            selectedLanguageCode = _uiState.value.languageCode,
            hasUserSelectedLanguage = true,
            hasError = false
        )
    }

    fun closeLanguageSettings() {
        if (_uiState.value.route == BaseRoutes.LanguageSettings && !_uiState.value.isSaving) {
            _uiState.value = _uiState.value.copy(route = BaseRoutes.Home)
        }
    }

    companion object {
        const val SPLASH_MINIMUM_DURATION_MS = 2_200L
        const val LANGUAGE_CONFIRM_DELAY_MS = 3_000L
        const val FIRST_OPEN_PAGE_COUNT = 3
    }
}

private suspend inline fun <T> runCatchingCancellable(
    crossinline block: suspend () -> T
): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    Result.failure(failure)
}
