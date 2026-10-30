package com.example.basekmp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekmp.data.Languages
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.ui.components.MainTab
import com.example.basekmp.ui.onboarding.OnboardingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BaseAppUiState(
    val route: String = BaseRoutes.Splash,
    val languageCode: String = "en",
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
)

sealed interface BaseAppEvent {
    data object LanguageApplied : BaseAppEvent
}

open class BaseAppViewModel(
    private val repository: BaseAppRepository
) : ViewModel() {
    val deviceLanguageCode: String = supportedLanguageCode(getSystemLanguageCode())

    private val _uiState = MutableStateFlow(BaseAppUiState(languageCode = deviceLanguageCode))
    val uiState: StateFlow<BaseAppUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BaseAppEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var isHandlingAction = false
    private var onboardingProgressSaveJob: Job? = null

    init {
        PlanProgress.get().setPersistenceSink { serialized ->
            viewModelScope.launch {
                runCatchingCancellable { repository.savePlanProgress(serialized) }
            }
        }
        viewModelScope.launch {
            val route = runCatchingCancellable { repository.resolveStartupRoute() }
                .getOrDefault(BaseRoutes.StartupLanguage)
            val savedLanguageCode = runCatchingCancellable { repository.currentLanguageCode() }.getOrDefault("")
            val languageCode = savedLanguageCode.ifBlank { deviceLanguageCode }
            val savedOnboardingProgress = runCatchingCancellable { repository.onboardingProgress() }.getOrDefault("")
            val surveyLanguage = runCatchingCancellable { repository.firstOpenSurveyLanguage() }.getOrDefault("")
            val savedPlanProgress = runCatchingCancellable { repository.planProgress() }.getOrDefault("")
            val savedLearningProgress = runCatchingCancellable { repository.learningProgress() }.getOrDefault("")
            if (savedPlanProgress.isNotBlank()) PlanProgress.get().restore(savedPlanProgress)
            val restoredLearningProgress = savedLearningProgress.isNotBlank() &&
                UserPrefs.restoreLearningProgress(savedLearningProgress)
            applyPlatformLanguage(languageCode)
            Strings.select(languageCode)
            UserPrefs.nativeLanguage = languageCode
            delay(SPLASH_MINIMUM_DURATION_MS)
            UserPrefs.onboardingDone = route == BaseRoutes.Home
            UserPrefs.firstOpenDone = route == BaseRoutes.Onboarding || route == BaseRoutes.Home
            if (!restoredLearningProgress && savedOnboardingProgress.isNotBlank()) {
                applyOnboardingProfile(OnboardingState.deserialize(savedOnboardingProgress))
            }
            UserPrefs.setProgressPersistenceSink { serialized ->
                viewModelScope.launch {
                    runCatchingCancellable { repository.saveLearningProgress(serialized) }
                }
            }
            if (!restoredLearningProgress && savedOnboardingProgress.isNotBlank()) {
                runCatchingCancellable { repository.saveLearningProgress(UserPrefs.serializeLearningProgress()) }
            }
            _uiState.value = _uiState.value.copy(
                route = route,
                languageCode = languageCode,
                selectedLanguageCode = savedLanguageCode,
                firstOpenSurveyLanguage = surveyLanguage,
                onboardingProgress = savedOnboardingProgress,
                isLoading = false
            )
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
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
                _events.emit(BaseAppEvent.LanguageApplied)
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

    fun finishFirstOpenSurvey(skip: Boolean = false) {
        val current = _uiState.value
        if (isHandlingAction || current.route != BaseRoutes.FirstOpenSurvey || current.isSaving) return
        val selectedLanguage = current.firstOpenSurveyLanguage.takeIf { !skip && it.isNotBlank() }
        if (!skip && selectedLanguage == null) return
        val nativeLanguage = current.languageCode.takeIf { code -> Languages.native.any { it.code == code } } ?: "en"
        val initialProfile = if (selectedLanguage != null) {
            OnboardingState(
                step = com.example.basekmp.ui.onboarding.OnboardingStep.NativeLanguage,
                skipLearnLanguage = true,
                learnLanguage = selectedLanguage,
                nativeLanguage = nativeLanguage
            )
        } else {
            OnboardingState(nativeLanguage = nativeLanguage)
        }
        isHandlingAction = true
        _uiState.value = current.copy(isSaving = true, hasError = false)
        viewModelScope.launch {
            val result = runCatchingCancellable {
                repository.completeFirstOpen(selectedLanguage, initialProfile.serialize())
            }
            if (result.isSuccess) {
                applyOnboardingProfile(initialProfile)
                selectedLanguage?.let {
                    UserPrefs.learningLanguage = it
                    UserPrefs.learnLanguageFromSurvey = true
                }
                UserPrefs.firstOpenDone = true
                _uiState.value = current.copy(
                    route = BaseRoutes.Onboarding,
                    firstOpenSurveyLanguage = selectedLanguage.orEmpty(),
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

    fun updateOnboardingProgress(serialized: String) {
        val current = _uiState.value
        if (current.route != BaseRoutes.Onboarding || current.isSaving) return
        _uiState.value = current.copy(onboardingProgress = serialized, hasError = false)
        onboardingProgressSaveJob?.cancel()
        onboardingProgressSaveJob = viewModelScope.launch {
            delay(150)
            val result = runCatchingCancellable { repository.saveOnboardingProgress(serialized) }
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
        _uiState.value = current.copy(onboardingProgress = serialized, isSaving = true, hasError = false)
        viewModelScope.launch {
            val result = runCatchingCancellable { repository.completeOnboarding(serialized) }
            if (result.isSuccess) {
                applyOnboardingProfile(profile)
                UserPrefs.onboardingDone = true
                _uiState.value = current.copy(
                    route = BaseRoutes.Home,
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

    private fun applyOnboardingProfile(profile: OnboardingState) {
        profile.learnLanguage?.let { UserPrefs.learningLanguage = it }
        profile.nativeLanguage?.let { UserPrefs.nativeLanguage = it }
        profile.level?.let { selected ->
            val level = selected.uppercase().takeIf { it in setOf("A1", "A2", "B1", "B2", "C1") }
            UserPrefs.englishLevel = level ?: "A1"
            UserPrefs.englishLevelChosen = level != null
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
