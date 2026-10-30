package com.example.basekmp.ui.plan

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.AnimatedBuddy
import com.example.basekmp.components.RobotMood
import com.example.basekmp.data.GoalPlanSession
import com.example.basekmp.data.PathLessonContent
import com.example.basekmp.data.PlanCatalogRepository
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.PlanQuestion
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * Daily interactive plan session with Animated Buddy mood responses,
 * vocabulary/translation/speaking quiz options, and completion celebration.
 * 100% migrated from decompiled PlanSessionScreenKt.java and PlanCelebrationKt.java.
 */
@Composable
fun PlanSessionScreen(
    day: Int,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var questions by remember { mutableStateOf<List<PlanQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableIntStateOf(-1) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isAnswerCorrect by remember { mutableStateOf(false) }
    var currentMood by remember { mutableStateOf(RobotMood.GOOD) }
    var showCelebration by remember { mutableStateOf(false) }
    var dayCompleted by remember { mutableStateOf(false) }
    var planStageId by remember { mutableStateOf<String?>(null) }
    var planLevelKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Load questions for the day
    LaunchedEffect(day) {
        val plan = PlanFlow.dayPlan(day)
        val stage = plan?.stage
        planStageId = stage?.id
        if (stage != null) {
            val bank = PlanCatalogRepository.bank(stage, UserPrefs.targetLanguage)
            if (bank != null) {
                val levelKey = UserPrefs.englishLevel.lowercase()
                planLevelKey = levelKey
                val levelBank = bank.levels[levelKey] ?: bank.levels.values.firstOrNull()
                if (levelBank != null) {
                    val generated = GoalPlanSession.quiz(
                        level = levelBank,
                        nativeLang = UserPrefs.nativeLanguage,
                        learn = UserPrefs.targetLanguage,
                        count = 6,
                        seen = PlanProgress.get().seen(stage.id, UserPrefs.targetLanguage, levelKey),
                        day = day,
                        seenContent = PlanProgress.get().seenContent(UserPrefs.targetLanguage, levelKey)
                    )
                    if (generated.isNotEmpty()) {
                        questions = generated
                    }
                }
            }
        }

        // Fallback questions directly from PathLessonContent if bank was empty
        if (questions.isEmpty()) {
            val loaded = PathLessonContent.load(
                lessonId = stage?.id ?: "beginner-u1-l1",
                level = UserPrefs.englishLevel.lowercase(),
                lang = UserPrefs.targetLanguage
            )
            questions = loaded.map { q ->
                PlanQuestion(
                    question = q,
                    itemId = q.text.hashCode().toString()
                )
            }
        }
        isLoading = false
    }

    val currentQuestion = questions.getOrNull(currentIndex)
    val totalCount = questions.size

    fun checkAnswer() {
        if (currentQuestion == null || selectedOptionIndex == -1) return
        val correctIndex = currentQuestion.question.answer
        isAnswerCorrect = (selectedOptionIndex == correctIndex)
        isSubmitted = true

        currentMood = if (isAnswerCorrect) RobotMood.SUPER else RobotMood.POOR
    }

    fun nextQuestion() {
        if (currentIndex < totalCount - 1) {
            currentIndex++
            selectedOptionIndex = -1
            isSubmitted = false
            currentMood = RobotMood.GOOD
        } else {
            // Completed day session
            coroutineScope.launch {
                planStageId?.let { stageId ->
                    PlanProgress.get().addSeen(
                        stage = stageId,
                        learn = UserPrefs.targetLanguage,
                        level = planLevelKey,
                        ids = questions.map { it.itemId }
                    )
                    PlanProgress.get().addSeenContent(
                        learn = UserPrefs.targetLanguage,
                        level = planLevelKey,
                        content = questions.map { it.content }
                    )
                }
                val result = PlanFlow.finish(day, PlanProgress.Activity.QUIZ)
                dayCompleted = result.dayCompleted
                showCelebration = true
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppTheme.colors.background
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
            return@Surface
        }

        if (questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr("Couldn't load your plan"),
                        color = AppTheme.colors.textPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(AppTheme.dimens.spacingMd))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                    ) {
                        Text(text = tr("Go Back"), color = AppTheme.colors.textOnPrimary)
                    }
                }
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Spacer(Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimens.topBarHeight)
                    .padding(horizontal = AppTheme.dimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = tr("Close"),
                        tint = AppTheme.colors.textPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentIndex + 1) / totalCount.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AppTheme.colors.primary,
                    trackColor = AppTheme.colors.surfaceCard
                )

                Spacer(modifier = Modifier.width(AppTheme.dimens.spacingMd))

                Text(
                    text = "${currentIndex + 1}/$totalCount",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(AppTheme.dimens.spacingSm))
            }

            // Interactive Buddy
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.dimens.spacingMd),
                contentAlignment = Alignment.Center
            ) {
                AnimatedBuddy(mood = currentMood, size = 96.dp)
            }

            // Question Card & Options
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimens.spacing2Xl),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingMd)
            ) {
                val q = currentQuestion
                if (q != null) {
                    item {
                        Column {
                            Text(
                                text = tr("Quick Quiz"),
                                color = AppTheme.colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = q.question.text,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp
                            )
                            if (q.question.start.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = q.question.start,
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    itemsIndexed(q.question.options) { optIdx, optionText ->
                        val isSelected = optIdx == selectedOptionIndex
                        val isCorrectOption = optIdx == q.question.answer

                        val backgroundColor = when {
                            isSubmitted && isCorrectOption -> AppTheme.colors.success.copy(alpha = 0.2f)
                            isSubmitted && isSelected && !isCorrectOption -> AppTheme.colors.error.copy(alpha = 0.2f)
                            isSelected -> AppTheme.colors.surfaceSelected
                            else -> AppTheme.colors.surfaceCard
                        }

                        val borderColor = when {
                            isSubmitted && isCorrectOption -> AppTheme.colors.success
                            isSubmitted && isSelected && !isCorrectOption -> AppTheme.colors.error
                            isSelected -> AppTheme.colors.primary
                            else -> AppTheme.colors.border
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
                                .background(backgroundColor)
                                .border(1.5.dp, borderColor, RoundedCornerShape(AppTheme.dimens.cornerMd))
                                .clickable(enabled = !isSubmitted) {
                                    selectedOptionIndex = optIdx
                                }
                                .padding(AppTheme.dimens.spacingLg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = optionText,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSubmitted && isCorrectOption) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = AppTheme.colors.success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.surface)
                    .padding(AppTheme.dimens.spacing2Xl)
            ) {
                if (!isSubmitted) {
                    Button(
                        onClick = { checkAnswer() },
                        enabled = selectedOptionIndex != -1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppTheme.dimens.primaryButtonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                        shape = RoundedCornerShape(AppTheme.dimens.cornerPill)
                    ) {
                        Text(
                            text = tr("Check Answer"),
                            color = AppTheme.colors.textOnPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val correctText = currentQuestion?.let { cq ->
                        cq.question.options.getOrNull(cq.question.answer).orEmpty()
                    }.orEmpty()

                    Column {
                        Text(
                            text = if (isAnswerCorrect) "Correct! Excellent work!" else "Not quite. Correct answer: $correctText",
                            color = if (isAnswerCorrect) AppTheme.colors.success else AppTheme.colors.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = { nextQuestion() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppTheme.dimens.primaryButtonHeight),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                            shape = RoundedCornerShape(AppTheme.dimens.cornerPill)
                        ) {
                            Text(
                                text = tr("Continue"),
                                color = AppTheme.colors.textOnPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Celebration Popup
    if (showCelebration) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {
            showCelebration = false
            onFinish()
        }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppTheme.dimens.cornerXl)),
                color = AppTheme.colors.surfaceCard
            ) {
                Column(
                    modifier = Modifier.padding(AppTheme.dimens.spacing2Xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingMd)
                ) {
                    AnimatedBuddy(mood = RobotMood.SUPER, size = 120.dp)

                    Text(
                        text = if (dayCompleted) "Day $day complete! 🎉" else "Quiz complete",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (dayCompleted) {
                            "You've earned +50 XP and +10 Diamonds!"
                        } else {
                            "Your quiz is saved. Finish the video, speaking, and tutor activities to complete this day."
                        },
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(AppTheme.dimens.spacingSm))

                    Button(
                        onClick = {
                            showCelebration = false
                            onFinish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                        shape = RoundedCornerShape(AppTheme.dimens.cornerPill),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr("Continue"),
                            color = AppTheme.colors.textOnPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
