package com.example.basekmp.ui.learningpath

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.basekmp.components.AppActionDialog
import com.example.basekmp.components.CongratsDialog
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.data.PathLessonContent
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.currentTimeMillis
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.theme.BrandPrimary
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.theme.TrackColor
import com.example.basekmp.ui.components.CircleIconButton
import com.example.basekmp.ui.grammar.OptionState
import com.example.basekmp.ui.speaking.SpeechCaptureMic

private val PathLessonCardBorder = Color(4293717745L) // 0xFFECEEF1
private val PathLessonPillBackground = Color(4293653502L) // 0xFFEBF3FE
private val PathOptionBackground = Color(4294572795L) // 0xFFF9FAFB
private val PathOptionBorder = Color(4293257195L) // 0xFFE5E7EB
private val PathOptionLetterBackground = Color(4293849073L) // 0xFFEEEFF1
private val PathOptionSelectedBackground = Color(4293456382L) // 0xFFE8F1FE
private val PathOptionSelectedLetterBackground = Color(4291813628L) // 0xFFCFE0FC
private val PathOptionWrongBackground = Color(4294829284L) // 0xFFFDE4E4
private val PathOptionWrongLetterBackground = Color(4294494665L) // 0xFFF8C9C9
private val PathOptionWrong = Color(4294189342L) // 0xFFF4211E
private val PathFeedbackRightBackground = Color(4293064154L) // 0xFFE2F5DA
private val PathFeedbackRightText = Color(4280263958L) // 0xFF1FA516
private val PathFeedbackRightButton = Color(4280463886L) // 0xFF22B20E
private val PathFeedbackWrongBackground = Color(4294829798L) // 0xFFFDE6E6
private val PathProgressTrack = TrackColor

@Composable
expect fun PathFeedbackEmoji(isCorrect: Boolean, modifier: Modifier)

/**
 * Path lesson flow for bundled vocabulary, grammar, speaking, and translation questions.
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
    var isLoading by remember(lessonId) { mutableStateOf(true) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var speechScore by remember { mutableIntStateOf(0) }
    var speechScores by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var completionRecorded by remember { mutableStateOf(false) }
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
        PathLessonExitDialog(
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

    fun recordCompletionOnce() {
        if (completionRecorded) return
        completionRecorded = true
        val activityEpochDay = UserPrefs.todayEpochDay()
        UserPrefs.recordPathLesson(lessonId, finalScore)
        UserPrefs.addLearnedMinutes(
            ((currentTimeMillis() - sessionStartedAt) / 60_000L).toInt(),
            activityEpochDay
        )
        UserPrefs.recordPracticeDone(
            feature = "learning path",
            score = correctCount,
            total = questions.size,
            activityEpochDay = activityEpochDay
        )
    }

    if (isFinished) {
        CongratsDialog(
            subtitle = Strings.tr("You've Completed Today's Session!"),
            buttonText = Strings.tr("Continue"),
            onButtonClick = onClose,
            onClose = onClose,
        )
    }

    val currentQuestion = questions[currentIndex.coerceIn(0, questions.size - 1)]

    fun advanceQuestion() {
        if (currentIndex < questions.lastIndex) {
            currentIndex++
            selectedOptionIndex = null
            isAnswerRevealed = false
            speechScore = 0
        } else {
            recordCompletionOnce()
            isFinished = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        PathLessonTopBar(
            currentStep = currentIndex + 1,
            totalSteps = questions.size,
            onClose = { showExitDialog = true },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp)
        ) {
            // Main Prompt Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, PathLessonCardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PathLessonPillBackground)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = Strings.tr(currentQuestion.kind.label),
                            color = BrandPrimary,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = currentQuestion.text,
                        fontSize = 18.sp,
                        fontWeight = if (currentQuestion.kind == PathLessonContent.Kind.SPEAKING) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Medium
                        },
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 25.sp
                    )

                    if (currentQuestion.kind == PathLessonContent.Kind.SPEAKING && currentQuestion.start.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = Strings.trf("Start with: “%1\$s”", currentQuestion.start),
                            fontSize = 16.sp,
                            color = Color(4286218119L), // 0xFF7A7F87
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
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
                            onSkip = if (currentIndex in speechScores) null else ({ advanceQuestion() }),
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
                                isSelected && selectedOptionIndex == currentQuestion.answer -> OptionState.Selected
                                isSelected -> OptionState.Wrong
                                idx == currentQuestion.answer -> OptionState.Correct
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

        }

        // Bottom CTA Button
        if (isAnswerRevealed) {
            val isCorrect = if (currentQuestion.kind == PathLessonContent.Kind.SPEAKING) {
                speechScore >= 60
            } else {
                selectedOptionIndex == currentQuestion.answer
            }
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(220)) + fadeIn(tween(220))
            ) {
                PathFeedbackPanel(
                    isCorrect = isCorrect,
                    onContinue = { advanceQuestion() }
                )
            }
        } else if (currentQuestion.kind != PathLessonContent.Kind.SPEAKING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = dimens.spacingLg)
            ) {
                PrimaryButton(
                    text = Strings.tr("Continue"),
                    enabled = selectedOptionIndex != null,
                    onClick = {
                        isAnswerRevealed = true
                        if (selectedOptionIndex == currentQuestion.answer) {
                            correctCount++
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PathLessonTopBar(
    currentStep: Int,
    totalSteps: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (currentStep.toFloat() / totalSteps.coerceAtLeast(1)).coerceIn(0.04f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "pathLessonProgress"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = Strings.tr("Close"),
            onClick = onClose
        )
        Spacer(Modifier.width(22.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(PathProgressTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(CircleShape)
                    .background(BrandPrimary)
            )
        }
        Spacer(Modifier.width(22.dp))
        Text(
            text = "$currentStep/$totalSteps",
            color = TextSecondary,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun PathFeedbackPanel(
    isCorrect: Boolean,
    onContinue: () -> Unit
) {
    val background = if (isCorrect) PathFeedbackRightBackground else PathFeedbackWrongBackground
    val textColor = if (isCorrect) PathFeedbackRightText else PathOptionWrong
    val buttonColor = if (isCorrect) PathFeedbackRightButton else PathOptionWrong

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(background)
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PathFeedbackEmoji(isCorrect = isCorrect, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = Strings.tr(
                    if (isCorrect) "Well done! Continue with the quiz"
                    else "Whoops! Do better next time."
                ),
                modifier = Modifier.weight(1f),
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .clickable(onClick = onContinue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.tr("Continue"),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
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
    val borderColor by animateColorAsState(
        when (state) {
            OptionState.Idle -> PathOptionBorder
            OptionState.Selected, OptionState.Correct -> BrandPrimary
            OptionState.Wrong -> PathOptionWrong
        }
    )

    val bgColor = when (state) {
        OptionState.Idle -> PathOptionBackground
        OptionState.Selected, OptionState.Correct -> PathOptionSelectedBackground
        OptionState.Wrong -> PathOptionWrongBackground
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    when (state) {
                        OptionState.Idle -> PathOptionLetterBackground
                        OptionState.Selected, OptionState.Correct -> PathOptionSelectedLetterBackground
                        OptionState.Wrong -> PathOptionWrongLetterBackground
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = 18.sp,
            color = TextPrimary,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        when (state) {
            OptionState.Correct -> Unit
            OptionState.Wrong, OptionState.Selected -> {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (state == OptionState.Wrong) PathOptionWrong else BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
            OptionState.Idle -> Unit
        }
    }
}

@Composable
private fun PathLessonExitDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppActionDialog(
        title = Strings.tr("Leave this lesson?"),
        message = Strings.tr("Your answers in this lesson won't be saved."),
        onDismissRequest = onDismiss,
        confirmText = Strings.tr("Leave"),
        onConfirm = onConfirm,
        dismissText = Strings.tr("Stay"),
        onDismissAction = onDismiss,
        confirmIsDestructive = true
    )
}
