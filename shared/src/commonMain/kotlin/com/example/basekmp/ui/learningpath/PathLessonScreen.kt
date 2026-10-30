package com.example.basekmp.ui.learningpath

import androidx.compose.animation.AnimatedVisibility
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
import com.example.basekmp.components.ExitSessionDialog
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.components.ProgressHeader
import com.example.basekmp.data.PathLessonContent
import com.example.basekmp.data.Languages
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.currentTimeMillis
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.grammar.OptionState
import com.example.basekmp.ui.speaking.SpeechCaptureMic
import com.example.basekmp.ui.speaking.SpeechPlaybackButton

/**
 * Interactive Duolingo-style Path Lesson Screen.
 * Executes multi-modal questions (Vocabulary, Grammar, Speaking, Translation)
 * directly loaded from bundled JSON files.
 * 100% migrated from decompiled PathLessonScreenKt.java.
 */
@Composable
fun PathLessonScreen(
    lessonId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    var questions by remember { mutableStateOf<List<PathLessonContent.Question>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var speechScore by remember { mutableIntStateOf(0) }
    var speechScores by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    val sessionStartedAt = remember(lessonId) { currentTimeMillis() }

    LaunchedEffect(lessonId) {
        isLoading = true
        var list = PathLessonContent.load(
            lessonId = lessonId,
            level = UserPrefs.pathLevel ?: "beginner",
            lang = UserPrefs.nativeLanguage
        )
        if (list.isEmpty()) {
            list = PathLessonContent.load(
                lessonId = lessonId,
                level = "beginner",
                lang = "en"
            )
        }
        questions = list
        isLoading = false
    }

    if (showExitDialog) {
        ExitSessionDialog(
            onConfirm = {
                showExitDialog = false
                onClose()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.primary)
        }
        return
    }

    if (questions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background).padding(dimens.spacingLg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Strings.tr("Lesson content is preparing."),
                    color = colors.textSecondary,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(dimens.spacingLg))
                PrimaryButton(
                    text = Strings.tr("Back"),
                    onClick = onClose
                )
            }
        }
        return
    }

    val finalScore = if (questions.isEmpty()) 0 else (correctCount * 100) / questions.size

    if (isFinished) {
        CongratsDialog(
            subtitle = Strings.tr("You've mastered this lesson node! Score: $finalScore%"),
            buttonText = Strings.tr("Done"),
            onButtonClick = {
                UserPrefs.recordPathLesson(lessonId, finalScore)
                UserPrefs.addLearnedMinutes(((currentTimeMillis() - sessionStartedAt) / 60_000L).toInt())
                UserPrefs.recordStreakDay()
                UserPrefs.recordPracticeDone("learning path", correctCount, questions.size)
                onClose()
            },
            onClose = {
                UserPrefs.recordPathLesson(lessonId, finalScore)
                UserPrefs.addLearnedMinutes(((currentTimeMillis() - sessionStartedAt) / 60_000L).toInt())
                UserPrefs.recordStreakDay()
                UserPrefs.recordPracticeDone("learning path", correctCount, questions.size)
                onClose()
            },
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
                if (currentIndex > 0) currentIndex-- else showExitDialog = true
            },
            onClose = { showExitDialog = true }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.spacingLg)
        ) {
            // Kind badge: VOCABULARY, GRAMMAR, SPEAKING, TRANSLATION
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.cornerSm))
                    .background(colors.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = Strings.tr(currentQuestion.kind.label),
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(dimens.spacingMd))

            // Main Prompt Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.cornerXl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                    .padding(dimens.spacing2Xl)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.tr("Question ${currentIndex + 1}/${questions.size}"),
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )

                        SpeechPlaybackButton(
                            text = currentQuestion.text,
                            languageTag = Languages.learn(UserPrefs.targetLanguage).speechTag
                        )
                    }

                    Spacer(Modifier.height(dimens.spacingLg))

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

            // Render based on Question Kind
            when (currentQuestion.kind) {
                PathLessonContent.Kind.SPEAKING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Strings.tr("Hold mic and speak aloud:"),
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )

                        Spacer(Modifier.height(dimens.spacingLg))

                        SpeechCaptureMic(
                            targetText = currentQuestion.text,
                            instruction = Strings.tr("Hold mic and speak aloud:"),
                            onResult = { score, _ ->
                                val wasCorrect = speechScores[currentIndex]?.let { it >= 60 } == true
                                val isCorrect = score >= 60
                                val updatedScores = speechScores + (currentIndex to score)
                                speechScores = updatedScores
                                speechScore = score
                                correctCount += when {
                                    !wasCorrect && isCorrect -> 1
                                    wasCorrect && !isCorrect -> -1
                                    else -> 0
                                }
                                isAnswerRevealed = true
                            }
                        )

                        AnimatedVisibility(visible = currentIndex in speechScores) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = dimens.spacingMd)
                            ) {
                                Text(
                                    text = "${Strings.tr("Pronunciation Score")}: $speechScore%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (speechScore >= 60) colors.success else colors.accentGold
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Multiple Choice Options (Vocabulary, Grammar, Translation)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
                    ) {
                        val letters = listOf("A", "B", "C", "D")

                        currentQuestion.options.forEachIndexed { idx, optionText ->
                            val isSelected = selectedOptionIndex == idx

                            val state = when {
                                !isAnswerRevealed -> if (isSelected) OptionState.Selected else OptionState.Idle
                                idx == currentQuestion.answer -> OptionState.Correct
                                isSelected -> OptionState.Wrong
                                else -> OptionState.Idle
                            }

                            PathOptionItem(
                                letter = letters.getOrElse(idx) { "${idx + 1}" },
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
            }

            // Answer Feedback Banner
            AnimatedVisibility(visible = isAnswerRevealed && currentQuestion.kind != PathLessonContent.Kind.SPEAKING) {
                val isCorrect = selectedOptionIndex == currentQuestion.answer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimens.spacingLg)
                        .clip(RoundedCornerShape(dimens.cornerMd))
                        .background(if (isCorrect) colors.successBg else colors.errorBg)
                        .border(1.dp, if (isCorrect) colors.success else colors.error, RoundedCornerShape(dimens.cornerMd))
                        .padding(dimens.spacingMd)
                ) {
                    Text(
                        text = if (isCorrect) "🎉 ${Strings.tr("Correct! Great job!")}" else "❌ ${Strings.tr("Incorrect. Correct answer:")} ${currentQuestion.options.getOrNull(currentQuestion.answer).orEmpty()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) colors.success else colors.error
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
            if (!isAnswerRevealed && currentQuestion.kind != PathLessonContent.Kind.SPEAKING) {
                PrimaryButton(
                    text = Strings.tr("Check Answer"),
                    enabled = selectedOptionIndex != null,
                    onClick = {
                        isAnswerRevealed = true
                        if (selectedOptionIndex == currentQuestion.answer) {
                            correctCount++
                        }
                    }
                )
            } else {
                PrimaryButton(
                    text = if (currentIndex < questions.size - 1) Strings.tr("Continue") else Strings.tr("Complete Lesson"),
                    enabled = isAnswerRevealed || currentQuestion.kind == PathLessonContent.Kind.SPEAKING,
                    onClick = {
                        if (currentIndex < questions.size - 1) {
                            currentIndex++
                            selectedOptionIndex = null
                            isAnswerRevealed = false
                            speechScore = 0
                        } else {
                            isFinished = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PathOptionItem(
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
            fontSize = 15.sp,
            color = when (state) {
                OptionState.Correct -> colors.success
                OptionState.Wrong -> colors.error
                OptionState.Selected -> colors.primary
                OptionState.Idle -> colors.textPrimary
            },
            fontWeight = if (state != OptionState.Idle) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        when (state) {
            OptionState.Correct -> Text("✓", color = colors.success, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OptionState.Wrong -> Text("✕", color = colors.error, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            OptionState.Selected -> {
                Box(
                    modifier = Modifier.size(12.dp).clip(CircleShape).background(colors.primary)
                )
            }
            OptionState.Idle -> Unit
        }
    }
}
