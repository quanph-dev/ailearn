package com.example.basekmp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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

    init {
        viewModelScope.launch {
            val route = runCatchingCancellable { repository.resolveStartupRoute() }
                .getOrDefault(BaseRoutes.StartupLanguage)
            val savedLanguageCode = runCatchingCancellable { repository.currentLanguageCode() }.getOrDefault("")
            val languageCode = savedLanguageCode.ifBlank { deviceLanguageCode }
            applyPlatformLanguage(languageCode)
            delay(SPLASH_MINIMUM_DURATION_MS)
            _uiState.value = _uiState.value.copy(
                route = route,
                languageCode = languageCode,
                selectedLanguageCode = savedLanguageCode,
                isLoading = false
            )
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
            _uiState.value = _uiState.value.copy(route = BaseRoutes.Onboarding)
        }
    }

    fun advanceOnboarding() {
        val current = _uiState.value
        if (isHandlingAction || current.route != BaseRoutes.Onboarding || current.isSaving) return
        if (current.onboardingPage < ONBOARDING_PAGE_COUNT - 1) {
            _uiState.value = current.copy(onboardingPage = current.onboardingPage + 1)
            return
        }
        isHandlingAction = true
        _uiState.value = current.copy(isSaving = true, hasError = false)
        viewModelScope.launch {
            if (runCatchingCancellable { repository.completeOnboarding() }.isSuccess) {
                _uiState.value = current.copy(
                    route = BaseRoutes.Home,
                    onboardingPage = 0,
                    isSaving = false,
                    hasError = false
                )
            } else {
                _uiState.value = current.copy(isSaving = false, hasError = true)
            }
            isHandlingAction = false
        }
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

    fun onHomeShown() {
        if (_uiState.value.route == BaseRoutes.Home) {
            viewModelScope.launch { runCatchingCancellable { repository.markHomeShown() } }
        }
    }

    companion object {
        const val SPLASH_MINIMUM_DURATION_MS = 600L
        const val LANGUAGE_CONFIRM_DELAY_MS = 3_000L
        const val ONBOARDING_PAGE_COUNT = 3
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
