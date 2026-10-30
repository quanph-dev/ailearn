package com.example.basekmp.ui.onboarding

import org.jetbrains.compose.resources.DrawableResource

enum class OnboardingStep {
    LearnLanguage,
    NativeLanguage,
    Level,
    DailyTime,
    TutorVoice
}

data class TutorOption(
    val id: String,
    val label: String,
    val imageRes: DrawableResource
)

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.LearnLanguage,
    val skipLearnLanguage: Boolean = false,
    val learnLanguage: String? = null,
    val nativeLanguage: String? = null,
    val level: String? = null,
    val dailyMinutes: String? = null,
    val tutorVoice: String? = null
) {
    val currentSelection: String?
        get() = when (step) {
            OnboardingStep.LearnLanguage -> learnLanguage
            OnboardingStep.NativeLanguage -> nativeLanguage
            OnboardingStep.Level -> level
            OnboardingStep.DailyTime -> dailyMinutes
            OnboardingStep.TutorVoice -> tutorVoice
        }

    val visibleSteps: List<OnboardingStep>
        get() = if (skipLearnLanguage) {
            OnboardingStep.entries.filter { it != OnboardingStep.LearnLanguage }
        } else {
            OnboardingStep.entries
        }

    val progress: Float
        get() {
            val list = visibleSteps
            val index = list.indexOf(step)
            return if (index >= 0) (index + 1).toFloat() / list.size.toFloat() else 0f
        }

    val isFirstStep: Boolean
        get() = step == visibleSteps.firstOrNull()

    val isLastStep: Boolean
        get() = step == visibleSteps.lastOrNull()

    fun next(): OnboardingState {
        val list = visibleSteps
        val nextIndex = list.indexOf(step) + 1
        return if (nextIndex in list.indices) {
            copy(step = list[nextIndex])
        } else {
            this
        }
    }

    fun previous(): OnboardingState {
        val list = visibleSteps
        val prevIndex = list.indexOf(step) - 1
        return if (prevIndex in list.indices) {
            copy(step = list[prevIndex])
        } else {
            this
        }
    }

    fun withSelection(id: String): OnboardingState {
        return when (step) {
            OnboardingStep.LearnLanguage -> copy(learnLanguage = id)
            OnboardingStep.NativeLanguage -> copy(nativeLanguage = id)
            OnboardingStep.Level -> copy(level = id)
            OnboardingStep.DailyTime -> copy(dailyMinutes = id)
            OnboardingStep.TutorVoice -> copy(tutorVoice = id)
        }
    }

    fun serialize(): String {
        return listOf(
            step.name,
            learnLanguage.orEmpty(),
            nativeLanguage.orEmpty(),
            level.orEmpty(),
            dailyMinutes.orEmpty(),
            tutorVoice.orEmpty(),
            skipLearnLanguage.toString()
        ).joinToString("|")
    }

    companion object {
        fun deserialize(raw: String): OnboardingState {
            if (raw.isBlank()) return OnboardingState()
            val parts = raw.split("|")
            if (parts.size < 6) return OnboardingState()
            val parsedStep = try {
                OnboardingStep.valueOf(parts[0])
            } catch (_: Exception) {
                OnboardingStep.LearnLanguage
            }
            val parsed = OnboardingState(
                step = parsedStep,
                skipLearnLanguage = parts.getOrNull(6)?.toBooleanStrictOrNull() ?: false,
                learnLanguage = parts[1].ifEmpty { null },
                nativeLanguage = parts[2].ifEmpty { null },
                level = parts[3].ifEmpty { null },
                dailyMinutes = parts[4].ifEmpty { null },
                tutorVoice = parts[5].ifEmpty { null }
            )
            return if (parsed.step in parsed.visibleSteps) parsed else parsed.copy(step = parsed.visibleSteps.first())
        }
    }
}
