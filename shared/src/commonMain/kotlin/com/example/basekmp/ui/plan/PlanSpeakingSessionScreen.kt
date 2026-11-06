package com.example.basekmp.ui.plan

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.basekmp.data.GoalPlan
import com.example.basekmp.components.AppLinearProgressBar
import com.example.basekmp.data.GoalPlanFormat
import com.example.basekmp.data.GoalPlanSession
import com.example.basekmp.data.Languages
import com.example.basekmp.data.PlanCatalogRepository
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.PlanQuestion
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.data.trf
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.components.CloseButton
import com.example.basekmp.ui.components.StandardBackButtonInset
import com.example.basekmp.ui.speaking.SpeechCaptureMic
import kotlinx.coroutines.launch

internal fun averagePlanSpeakingScore(scores: Collection<Int>, questionCount: Int): Int =
    if (questionCount <= 0) 0 else scores.sum().coerceAtLeast(0) / questionCount

/** Plan speaking uses the stage's reviewed remote speaking bank; it never substitutes Path lesson data. */
@Composable
fun PlanSpeakingSessionScreen(
    day: Int,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<PlanQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var isLoading by remember(day) { mutableStateOf(true) }
    var stageId by remember { mutableStateOf<String?>(null) }
    var learnCode by remember { mutableStateOf("") }
    var levelKey by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(-1) }
    var scores by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var availableActivities by remember { mutableStateOf<List<PlanProgress.Activity>>(emptyList()) }
    var isCompleting by remember { mutableStateOf(false) }
    var showCompletion by remember { mutableStateOf(false) }
    var dayCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(day, loadAttempt) {
        isLoading = true
        questions = emptyList()
        currentIndex = 0
        scores = emptyMap()
        transcript = ""
        score = -1
        showCompletion = false
        dayCompleted = false
        isCompleting = false
        stageId = null
        learnCode = GoalPlan.learnCode(UserPrefs.targetLanguage).lowercase()
        levelKey = ""

        val dayPlan = PlanFlow.dayPlan(day)
        availableActivities = dayPlan?.available.orEmpty()
        val stage = dayPlan?.stage
        stageId = stage?.id
        if (stage != null) {
            val bank = PlanCatalogRepository.bank(stage, learnCode)
            val selectedLevel = bank?.let { source ->
                GoalPlan.bankLevels(UserPrefs.englishLevel)
                    .distinct()
                    .firstNotNullOfOrNull { key -> source.levels[key]?.let { key to it } }
            }
            if (bank != null && selectedLevel != null) {
                val (key, level) = selectedLevel
                levelKey = key
                val countFromCatalog = PlanCatalogRepository.getCatalog()?.let { catalog ->
                    GoalPlanFormat.count(
                        rows = catalog.speakingCounts,
                        minutes = UserPrefs.dailyGoalMinutes,
                        cap = level.speaking.size
                    )
                } ?: 0
                val count = countFromCatalog.coerceIn(0, level.speaking.size)
                if (count > 0) {
                    questions = GoalPlanSession.speaking(
                        level = level,
                        nativeLang = Languages.nativeFor(UserPrefs.nativeLanguage).code,
                        count = count,
                        seen = PlanProgress.get().seen(stage.id, learnCode, key),
                        seenContent = PlanProgress.get().seenContent(learnCode, key)
                    ).filter { it.model.isNotBlank() }
                }
            }
        }
        isLoading = false
    }

    fun finishSession() {
        if (isCompleting) return
        isCompleting = true
        val completedQuestions = questions
        val completedStageId = stageId
        val completedLevelKey = levelKey
        val completedLearnCode = learnCode
        val available = availableActivities
        val activityEpochDay = UserPrefs.todayEpochDay()
        scope.launch {
            if (completedStageId != null && completedLevelKey.isNotBlank()) {
                PlanProgress.get().addSeen(
                    stage = completedStageId,
                    learn = completedLearnCode,
                    level = completedLevelKey,
                    ids = completedQuestions.map { it.itemId }
                )
                PlanProgress.get().addSeenContent(
                    learn = completedLearnCode,
                    level = completedLevelKey,
                    content = completedQuestions.map { it.content }
                )
            }
            val progress = PlanProgress.get()
            val wasSpeakingDone = PlanProgress.Activity.SPEAKING in progress.done(day)
            dayCompleted = PlanFlow.finish(day, PlanProgress.Activity.SPEAKING, activityEpochDay).dayCompleted
            val speakingWasCompleted = !wasSpeakingDone && PlanProgress.Activity.SPEAKING in progress.done(day)
            if (speakingWasCompleted) {
                UserPrefs.addLearnedMinutes(
                    PlanProgress.activityMinutes(
                        activity = PlanProgress.Activity.SPEAKING,
                        dailyMinutes = UserPrefs.dailyGoalMinutes,
                        available = available
                    ),
                    activityEpochDay
                )
            }
            showCompletion = true
        }
    }

    fun advance() {
        if (isCompleting) return
        if (currentIndex < questions.lastIndex) {
            currentIndex++
            transcript = ""
            score = -1
        } else {
            finishSession()
        }
    }

    fun skipCurrentQuestion(questionIndex: Int) {
        if (questionIndex != currentIndex || score >= 0 || isCompleting) return
        scores = scores + (questionIndex to 0)
        advance()
    }

    Surface(modifier = modifier.fillMaxSize(), color = AppTheme.colors.background) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
            questions.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(AppTheme.dimens.spacing2Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tr("Speaking content is unavailable"),
                    color = AppTheme.colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(AppTheme.dimens.spacingLg))
                Button(
                    onClick = { isLoading = true; loadAttempt++ },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                ) { Text(tr("Try Again"), color = AppTheme.colors.textOnPrimary) }
                Spacer(Modifier.height(AppTheme.dimens.spacingSm))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surfaceCard)
                ) { Text(tr("Go Back"), color = AppTheme.colors.textPrimary) }
            }
            else -> {
                val question = questions[currentIndex]
                val questionIndex = currentIndex
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppTheme.dimens.topBarHeight)
                            .padding(horizontal = StandardBackButtonInset),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CloseButton(onClick = onBack)
                        AppLinearProgressBar(
                            progress = (currentIndex + 1) / questions.size.toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AppTheme.colors.primary
                        )
                        Text(
                            text = "${currentIndex + 1}/${questions.size}",
                            modifier = Modifier.padding(horizontal = AppTheme.dimens.spacingMd),
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(AppTheme.dimens.spacing2Xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tr("Speak the sentence aloud"),
                            color = AppTheme.colors.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(AppTheme.dimens.spacingLg))
                        Text(
                            text = question.question.text,
                            color = AppTheme.colors.textSecondary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(AppTheme.dimens.spacingLg))
                        Text(
                            text = "“${question.model}”",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(AppTheme.dimens.spacing2Xl))
                        SpeechCaptureMic(
                            targetText = question.model,
                            instruction = tr("Tap the microphone and repeat the sentence"),
                            onResult = { resultScore, recognized ->
                                if (!isCompleting && questionIndex == currentIndex && questionIndex !in scores) {
                                    score = resultScore
                                    transcript = recognized
                                    scores = scores + (questionIndex to resultScore)
                                }
                            },
                            onSkip = if (score < 0) ({ skipCurrentQuestion(questionIndex) }) else null
                        )
                        if (score >= 0) {
                            Spacer(Modifier.height(AppTheme.dimens.spacingLg))
                            Text(
                                text = "${tr("You said")}: $transcript",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${tr("Pronunciation score")}: $score%",
                                color = AppTheme.colors.primary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { advance() },
                        enabled = score >= 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.dimens.spacing2Xl),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                        shape = RoundedCornerShape(AppTheme.dimens.cornerPill)
                    ) {
                        Text(
                            text = if (currentIndex == questions.lastIndex) tr("Finish") else tr("Continue"),
                            color = AppTheme.colors.textOnPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showCompletion) {
        Dialog(onDismissRequest = { showCompletion = false; onFinish() }) {
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
                    Text(
                        text = if (dayCompleted) trf("Day %1\$d complete! 🎉", day) else tr("Speaking complete"),
                        color = AppTheme.colors.textPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (dayCompleted) {
                            tr("You've earned +50 XP and +10 Diamonds!")
                        } else {
                            tr("Your speaking activity is saved. Complete the remaining activities to finish this day.")
                        },
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    val average = averagePlanSpeakingScore(scores.values, questions.size)
                    Text(
                        text = "${tr("Average pronunciation score")}: $average%",
                        color = AppTheme.colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { showCompletion = false; onFinish() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                        shape = RoundedCornerShape(AppTheme.dimens.cornerPill)
                    ) { Text(tr("Continue"), color = AppTheme.colors.textOnPrimary, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
