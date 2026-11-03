package com.example.basekmp.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.ob_clock_10
import ailearn.shared.generated.resources.ob_clock_20
import ailearn.shared.generated.resources.ob_clock_30
import ailearn.shared.generated.resources.ob_clock_5
import ailearn.shared.generated.resources.tutor_female
import ailearn.shared.generated.resources.tutor_male
import ailearn.shared.generated.resources.tutor_random
import ailearn.shared.generated.resources.tutor_robot
import com.example.basekmp.data.Languages
import com.example.basekmp.data.Strings
import com.example.basekmp.theme.BlueSelected
import com.example.basekmp.theme.CefrA1
import com.example.basekmp.theme.CefrA2
import com.example.basekmp.theme.CefrB1
import com.example.basekmp.theme.CefrB2
import com.example.basekmp.theme.CefrC1
import com.example.basekmp.theme.GradientBottom
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.OptionSurfaceSelected
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.getScreenTitleStyle
import com.example.basekmp.ui.components.CefrBadge
import com.example.basekmp.ui.components.OptionCard
import com.example.basekmp.ui.components.LanguageChoiceMetrics
import com.example.basekmp.ui.components.PrimaryButton
import com.example.basekmp.ui.components.ProgressHeader
import com.example.basekmp.ui.components.SelectOption
import org.jetbrains.compose.resources.painterResource
import ailearn.shared.generated.resources.language_save_error
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    isSaving: Boolean,
    hasError: Boolean,
    onStateChange: (OnboardingState) -> Unit,
    onFinished: (OnboardingState) -> Unit
) {
    val uiLanguage = Strings.getLanguage()

    fun titleFor(step: OnboardingStep): String = Strings.tr(when (step) {
        OnboardingStep.LearnLanguage -> "Which Language Do You Want to Learn?"
        OnboardingStep.NativeLanguage -> "What is Your Native Language?"
        OnboardingStep.Level -> if (uiLanguage == "en") "Choose Your Level" else "Choose Your English Level"
        OnboardingStep.DailyTime -> "How Much Time Can You Spend Learning Each Day?"
        OnboardingStep.TutorVoice -> "Choose Your AI Tutor Voice"
    })

    fun translated(text: String): String = Strings.trIn(uiLanguage, text)

    val learnLanguageOptions = remember(uiLanguage) {
        Languages.learnable.map { lang ->
            SelectOption(
                id = lang.code,
                label = translated(lang.name),
                leading = { Text(lang.flag, fontSize = 22.sp) }
            )
        }
    }

    val nativeLanguageOptions = remember(uiLanguage) {
        Languages.native.map { lang ->
            SelectOption(
                id = lang.code,
                label = translated(lang.name),
                leading = { Text(lang.flag, fontSize = 22.sp) }
            )
        }
    }

    val levelOptions = remember(uiLanguage) {
        listOf(
            SelectOption("a1", translated("Beginner"), leading = { CefrBadge("A1", CefrA1) }),
            SelectOption("a2", translated("Elementary"), leading = { CefrBadge("A2", CefrA2) }),
            SelectOption("b1", translated("Intermediate"), leading = { CefrBadge("B1", CefrB1) }),
            SelectOption("b2", translated("Upper Intermediate"), leading = { CefrBadge("B2", CefrB2) }),
            SelectOption("c1", translated("Advanced"), leading = { CefrBadge("C1", CefrC1) }),
            SelectOption("unsure", translated("Not Sure"))
        )
    }

    val dailyTimeOptions = remember(uiLanguage) {
        listOf(
            SelectOption("5", translated("5 min"), leading = {
                Image(painterResource(Res.drawable.ob_clock_5), null, Modifier.size(30.dp))
            }),
            SelectOption("10", translated("10 min"), leading = {
                Image(painterResource(Res.drawable.ob_clock_10), null, Modifier.size(30.dp))
            }),
            SelectOption("20", translated("20 min"), leading = {
                Image(painterResource(Res.drawable.ob_clock_20), null, Modifier.size(30.dp))
            }),
            SelectOption("30", translated("30 min"), leading = {
                Image(painterResource(Res.drawable.ob_clock_30), null, Modifier.size(30.dp))
            })
        )
    }

    val tutorOptions = remember(uiLanguage) {
        listOf(
            TutorOption("male", translated("Male"), Res.drawable.tutor_male),
            TutorOption("female", translated("Female"), Res.drawable.tutor_female),
            TutorOption("random", translated("Random"), Res.drawable.tutor_random),
            TutorOption("robot", translated("Robot"), Res.drawable.tutor_robot)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color.White,
                    0.42f to GradientBottom,
                    1.0f to Color.White
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 14.dp)
        ) {
            ProgressHeader(
                progress = state.progress,
                onBack = {
                    if (!state.isFirstStep) {
                        onStateChange(state.previous())
                    }
                },
                showBack = !state.isFirstStep
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = titleFor(state.step),
                modifier = Modifier.fillMaxWidth(),
                color = TextPrimary,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                style = getScreenTitleStyle()
            )

            Spacer(modifier = Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state.step) {
                    OnboardingStep.LearnLanguage -> {
                        OptionCard(
                            options = learnLanguageOptions,
                            selectedId = state.learnLanguage,
                            onSelect = { onStateChange(state.withSelection(it)) },
                            rowMinHeight = LanguageChoiceMetrics.rowHeight
                        )
                    }
                    OnboardingStep.NativeLanguage -> {
                        OptionCard(
                            options = nativeLanguageOptions,
                            selectedId = state.nativeLanguage,
                            onSelect = { onStateChange(state.withSelection(it)) },
                            rowMinHeight = LanguageChoiceMetrics.rowHeight
                        )
                    }
                    OnboardingStep.Level -> {
                        OptionCard(
                            options = levelOptions,
                            selectedId = state.level,
                            onSelect = { onStateChange(state.withSelection(it)) }
                        )
                    }
                    OnboardingStep.DailyTime -> {
                        OptionCard(
                            options = dailyTimeOptions,
                            selectedId = state.dailyMinutes,
                            onSelect = { onStateChange(state.withSelection(it)) }
                        )
                    }
                    OnboardingStep.TutorVoice -> {
                        TutorGrid(
                            options = tutorOptions,
                            selectedId = state.tutorVoice,
                            onSelect = { onStateChange(state.withSelection(it)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PrimaryButton(
                text = if (isSaving) translated("One sec") else translated("Continue"),
                enabled = state.currentSelection != null && !isSaving,
                onClick = {
                    if (state.isLastStep) {
                        onFinished(state)
                    } else {
                        onStateChange(state.next())
                    }
                }
            )
            if (hasError) {
                Text(
                    text = stringResource(Res.string.language_save_error),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TutorGrid(
    options: List<TutorOption>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowOptions.forEach { tutor ->
                    val selected = tutor.id == selectedId
                    TutorCell(
                        option = tutor,
                        selected = selected,
                        onClick = { onSelect(tutor.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorCell(
    option: TutorOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val background = if (selected) OptionSurfaceSelected else OptionSurface
    val borderModifier = if (selected) {
        Modifier.border(1.5.dp, BlueSelected, shape)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .aspectRatio(1.03f)
            .clip(shape)
            .background(background)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(option.imageRes),
                contentDescription = option.label,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd),
                    tint = BlueSelected
                )
            }
        }

        Text(
            text = option.label,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) BlueSelected else TextPrimary
        )
    }
}
