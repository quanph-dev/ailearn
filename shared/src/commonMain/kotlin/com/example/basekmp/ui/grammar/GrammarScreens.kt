package com.example.basekmp.ui.grammar

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.CongratsDialog
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.components.ProgressHeader
import com.example.basekmp.data.EnglishLevel
import com.example.basekmp.data.PracticeConfig
import com.example.basekmp.data.StaticContent
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.components.DropdownField
import com.example.basekmp.ui.components.PracticeSetupScaffold

/**
 * Grammar Setup Screen for selecting topic, difficulty, and quiz length.
 * 100% migrated from decompiled GrammarScreensKt.java.
 */
@Composable
fun GrammarSetupScreen(
    onBack: () -> Unit,
    onStart: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf("Prepositions") }
    var selectedDifficulty by remember { mutableStateOf(EnglishLevel.to3TierDifficulty(UserPrefs.englishLevel)) }
    var selectedCountText by remember { mutableStateOf("15 questions") }

    val types = remember { listOf("Prepositions", "Tenses", "Articles", "Conjunctions") }
    val difficulties = remember { listOf("Beginner", "Intermediate", "Advanced") }
    val counts = remember { listOf("5 questions", "10 questions", "15 questions", "20 questions") }

    PracticeSetupScaffold(
        title = Strings.tr("Grammar"),
        onBack = onBack,
        onStart = {
            PracticeConfig.grammarType = selectedType
            PracticeConfig.grammarDifficulty = selectedDifficulty
            val count = selectedCountText.substringBefore(" ").toIntOrNull() ?: 15
            onStart(count)
        }
    ) {
        DropdownField(
            label = "Grammar Type",
            options = types,
            selected = selectedType,
            onSelect = { selectedType = it }
        )
        Spacer(Modifier.height(18.dp))
        DropdownField(
            label = "Difficulty",
            options = difficulties,
            selected = selectedDifficulty,
            onSelect = { selectedDifficulty = it }
        )
        Spacer(Modifier.height(18.dp))
        DropdownField(
            label = "Number of Questions",
            options = counts,
            selected = selectedCountText,
            onSelect = { selectedCountText = it }
        )
    }
}

/**
 * Multiple-choice Grammar Quiz Screen with instant feedback, option transitions,
 * and completion rewards.
 */
@Composable
fun GrammarQuizScreen(
    questionCount: Int,
    onClose: (correct: Int, total: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    var questions by remember { mutableStateOf<List<GrammarQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var correctByQuestion by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    val correctCount = correctByQuestion.values.count { it }
    var isFinished by remember { mutableStateOf(false) }
    var completionRecorded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(questionCount) {
        isLoading = true
        val list = StaticContent.grammar(
            type = PracticeConfig.grammarType,
            difficulty = PracticeConfig.grammarDifficulty,
            count = questionCount
        )
        questions = list
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.primary)
        }
        return
    }

    if (questions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(dimens.spacingLg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.tr("No grammar questions available."),
                color = colors.textSecondary,
                fontSize = 15.sp
            )
        }
        return
    }

    fun finishGrammarSession() {
        if (!completionRecorded) {
            UserPrefs.recordPracticeDone(
                feature = "grammar",
                score = correctCount,
                total = questions.size,
                learnedMinutes = 6,
                level = PracticeConfig.grammarDifficulty
            )
            completionRecorded = true
        }
        isFinished = true
    }

    if (isFinished) {
        CongratsDialog(
            subtitle = Strings.tr("Grammar quiz completed successfully!"),
            buttonText = Strings.tr("Done"),
            onButtonClick = { onClose(correctCount, questions.size) },
            onClose = { onClose(correctCount, questions.size) },
            correct = correctCount,
            incorrect = questions.size - correctCount
        )
    }

    val currentQuestion = questions[currentIndex.coerceIn(0, questions.size - 1)]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Spacer(Modifier.height(8.dp))
        ProgressHeader(
            currentStep = currentIndex + 1,
            totalSteps = questions.size,
            onBack = {
                if (currentIndex > 0) {
                    currentIndex--
                    selectedOptionIndex = null
                    isAnswerRevealed = false
                } else {
                    onClose(correctCount, questions.size)
                }
            },
            onClose = { onClose(correctCount, questions.size) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.spacingLg)
        ) {
            Spacer(Modifier.height(dimens.spacingMd))

            // Question Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.cornerXl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                    .padding(dimens.spacing2Xl)
            ) {
                Column {
                    Text(
                        text = "${Strings.tr("Question")} ${currentIndex + 1}/${questions.size}",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(dimens.spacingMd))

                    Text(
                        text = currentQuestion.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        lineHeight = 28.sp
                    )
                }
            }

            Spacer(Modifier.height(dimens.spacingXl))

            // Multiple-choice options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
            ) {
                val optionLetters = listOf("A", "B", "C", "D")

                currentQuestion.options.forEachIndexed { idx, optionText ->
                    val isSelected = selectedOptionIndex == idx

                    val state = when {
                        !isAnswerRevealed -> if (isSelected) OptionState.Selected else OptionState.Idle
                        idx == currentQuestion.answerIndex -> OptionState.Correct
                        isSelected -> OptionState.Wrong
                        else -> OptionState.Idle
                    }

                    GrammarOptionRow(
                        letter = optionLetters.getOrElse(idx) { "${idx + 1}" },
                        text = optionText,
                        state = state,
                        onClick = {
                            if (!isAnswerRevealed) {
                                selectedOptionIndex = idx
                            }
                        }
                    )
                }
            }
        }

        // Bottom CTA Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spacingLg)
        ) {
            if (!isAnswerRevealed) {
                PrimaryButton(
                    text = Strings.tr("Check Answer"),
                    enabled = selectedOptionIndex != null,
                    onClick = {
                        isAnswerRevealed = true
                        correctByQuestion = correctByQuestion +
                            (currentIndex to (selectedOptionIndex == currentQuestion.answerIndex))
                    }
                )
            } else {
                PrimaryButton(
                    text = if (currentIndex < questions.size - 1) Strings.tr("Continue") else Strings.tr("Finish"),
                    onClick = {
                        if (currentIndex < questions.size - 1) {
                            currentIndex++
                            selectedOptionIndex = null
                            isAnswerRevealed = false
                        } else {
                            finishGrammarSession()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GrammarOptionRow(
    letter: String,
    text: String,
    state: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    val borderColor by animateColorAsState(
        when (state) {
            OptionState.Idle -> colors.border
            OptionState.Selected -> colors.primary
            OptionState.Correct -> colors.success
            OptionState.Wrong -> colors.error
        }
    )

    val bgColor = when (state) {
        OptionState.Idle -> colors.surface
        OptionState.Selected -> colors.surfaceSelected
        OptionState.Correct -> colors.successBg
        OptionState.Wrong -> colors.errorBg
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(dimens.cornerMd))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(dimens.cornerMd))
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spacingLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Option Letter Circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when (state) {
                        OptionState.Correct -> colors.success
                        OptionState.Wrong -> colors.error
                        OptionState.Selected -> colors.primary
                        OptionState.Idle -> colors.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (state == OptionState.Idle) colors.textSecondary else Color.White
            )
        }

        Spacer(Modifier.width(dimens.spacingMd))

        Text(
            text = text,
            fontSize = 16.sp,
            color = when (state) {
                OptionState.Correct -> colors.success
                OptionState.Wrong -> colors.error
                OptionState.Selected -> colors.primary
                OptionState.Idle -> colors.textPrimary
            },
            fontWeight = if (state != OptionState.Idle) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        // Trailing state indicator
        when (state) {
            OptionState.Correct -> Text("✓", color = colors.success, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OptionState.Wrong -> Text("✕", color = colors.error, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            OptionState.Selected -> {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
            }
            OptionState.Idle -> Unit
        }
    }
}

@Composable
private fun GrammarSelectorSection(
    title: String,
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(dimens.spacingSm))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.cornerMd))
                        .background(if (isSelected) colors.surfaceSelected else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.primary else colors.border,
                            shape = RoundedCornerShape(dimens.cornerMd)
                        )
                        .clickable { onSelect(item) }
                        .padding(horizontal = dimens.spacingLg, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.tr(item),
                        color = if (isSelected) colors.primary else colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) colors.primary else colors.textSecondary, CircleShape)
                            .background(if (isSelected) colors.primary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text("✓", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarCountSection(
    title: String,
    counts: List<Int>,
    selectedCount: Int,
    onSelect: (Int) -> Unit
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(dimens.spacingSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            counts.forEach { count ->
                val isSelected = count == selectedCount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(dimens.cornerMd))
                        .background(if (isSelected) colors.primary else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.primary else colors.border,
                            shape = RoundedCornerShape(dimens.cornerMd)
                        )
                        .clickable { onSelect(count) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        color = if (isSelected) Color.White else colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
