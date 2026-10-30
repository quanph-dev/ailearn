package com.example.basekmp.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.basekmp.BackActionHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.AnimatedBuddy
import com.example.basekmp.components.RobotMood
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.theme.AppTheme

/**
 * 6-Step Onboarding Questionnaire to generate a personalized 30-Day Plan.
 * 100% migrated from decompiled PlanSetupScreenKt.java.
 */
@Composable
fun PlanSetupScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = SetupStep.entries
    var currentStepIndex by remember { mutableIntStateOf(0) }

    var studentName by remember { mutableStateOf(PlanProgress.get().name.ifEmpty { "Learner" }) }
    var selectedGoalId by remember { mutableStateOf(PlanProgress.get().goalId ?: "travel") }
    var selectedLevel by remember { mutableStateOf(UserPrefs.englishLevel) }
    var selectedMinutes by remember { mutableIntStateOf(UserPrefs.dailyGoalMinutes) }
    var selectedAssistantId by remember { mutableStateOf("emma") }

    val currentStep = steps[currentStepIndex]

    fun advance() {
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
        } else {
            // Finish and save
            val progress = PlanProgress.get()
            progress.start(
                goal = selectedGoalId,
                name = studentName,
                goalNote = ""
            )
            UserPrefs.englishLevel = selectedLevel
            UserPrefs.dailyGoalMinutes = selectedMinutes
            val chosenAssistant = Assistant.ALL.firstOrNull { it.id == selectedAssistantId }
            UserPrefs.tutorVoice = chosenAssistant?.name ?: "Emma"
            onDone()
        }
    }

    fun goBack() {
        if (currentStepIndex > 0) {
            currentStepIndex--
        } else {
            onBack()
        }
    }

    BackActionHandler(enabled = currentStepIndex > 0) {
        goBack()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Spacer(Modifier.height(8.dp))
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimens.topBarHeight)
                    .padding(horizontal = AppTheme.dimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { goBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = tr("Back"),
                        tint = AppTheme.colors.textPrimary
                    )
                }

                if (currentStep != SetupStep.START) {
                    LinearProgressIndicator(
                        progress = { (currentStepIndex) / (steps.size - 1).toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AppTheme.colors.primary,
                        trackColor = AppTheme.colors.surfaceCard
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimens.spacingLg))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimens.spacing2Xl)
            ) {
                when (currentStep) {
                    SetupStep.START -> StartStepContent()
                    SetupStep.NAME -> NameStepContent(
                        name = studentName,
                        onNameChange = { studentName = it }
                    )
                    SetupStep.GOAL -> GoalStepContent(
                        selectedGoalId = selectedGoalId,
                        onSelectGoal = { selectedGoalId = it }
                    )
                    SetupStep.LEVEL -> LevelStepContent(
                        selectedLevel = selectedLevel,
                        onSelectLevel = { selectedLevel = it }
                    )
                    SetupStep.TIME -> TimeStepContent(
                        selectedMinutes = selectedMinutes,
                        onSelectMinutes = { selectedMinutes = it }
                    )
                    SetupStep.ASSISTANT -> AssistantStepContent(
                        selectedId = selectedAssistantId,
                        onSelectAssistant = { selectedAssistantId = it }
                    )
                }
            }

            // Bottom CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(AppTheme.dimens.spacing2Xl)
            ) {
                val buttonText = when (currentStep) {
                    SetupStep.START -> tr("Let's start!")
                    SetupStep.ASSISTANT -> tr("Create my plan")
                    else -> tr("Continue")
                }

                Button(
                    onClick = { advance() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.dimens.primaryButtonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                    shape = RoundedCornerShape(AppTheme.dimens.cornerPill)
                ) {
                    Text(
                        text = buttonText,
                        color = AppTheme.colors.textOnPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StartStepContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedBuddy(mood = RobotMood.SUPER, size = 120.dp)
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacing2Xl))

        Text(
            text = tr("Personalize your path"),
            color = AppTheme.colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingSm))

        Text(
            text = tr("Answer a few quick questions so I can personalise your learning path for the best fit."),
            color = AppTheme.colors.textSecondary,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun NameStepContent(
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingXl))
        Text(
            text = tr("What should we call you?"),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("We'll use it to greet you."),
            color = AppTheme.colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacing2Xl))

        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
                .background(AppTheme.colors.surfaceCard)
                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(AppTheme.dimens.cornerMd))
                .padding(horizontal = AppTheme.dimens.spacingLg, vertical = AppTheme.dimens.spacingLg),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = AppTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(AppTheme.colors.primary),
            decorationBox = { innerTextField ->
                if (name.isEmpty()) {
                    Text(
                        text = tr("Your name"),
                        color = AppTheme.colors.textTertiary,
                        fontSize = 18.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun GoalStepContent(
    selectedGoalId: String,
    onSelectGoal: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingXl))
        Text(
            text = tr("What's your learning goal?"),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("We'll build a 30-day plan around it."),
            color = AppTheme.colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingLg))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingSm)
        ) {
            items(GoalOption.ALL) { goal ->
                val isSelected = goal.id == selectedGoalId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
                        .background(if (isSelected) AppTheme.colors.surfaceSelected else AppTheme.colors.surfaceCard)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.border,
                            shape = RoundedCornerShape(AppTheme.dimens.cornerMd)
                        )
                        .clickable { onSelectGoal(goal.id) }
                        .padding(AppTheme.dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(goal.title),
                            color = AppTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(goal.desc),
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelStepContent(
    selectedLevel: String,
    onSelectLevel: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingXl))
        Text(
            text = tr("What's your level?"),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("Questions get harder as your level goes up."),
            color = AppTheme.colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingLg))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingSm)
        ) {
            items(LevelOption.ALL) { opt ->
                val isSelected = opt.setting.equals(selectedLevel, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
                        .background(if (isSelected) AppTheme.colors.surfaceSelected else AppTheme.colors.surfaceCard)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.border,
                            shape = RoundedCornerShape(AppTheme.dimens.cornerMd)
                        )
                        .clickable { onSelectLevel(opt.setting) }
                        .padding(AppTheme.dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt.setting,
                            color = if (isSelected) AppTheme.colors.textOnPrimary else AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(AppTheme.dimens.spacingMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(opt.title),
                            color = AppTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(opt.text),
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeStepContent(
    selectedMinutes: Int,
    onSelectMinutes: (Int) -> Unit
) {
    val times = listOf(5, 10, 15, 20, 30)

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingXl))
        Text(
            text = tr("How much time can you study each day?"),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("This sets how many questions you get each day."),
            color = AppTheme.colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingLg))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingSm)
        ) {
            items(times) { mins ->
                val isSelected = mins == selectedMinutes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
                        .background(if (isSelected) AppTheme.colors.surfaceSelected else AppTheme.colors.surfaceCard)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.border,
                            shape = RoundedCornerShape(AppTheme.dimens.cornerMd)
                        )
                        .clickable { onSelectMinutes(mins) }
                        .padding(AppTheme.dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$mins min / day",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantStepContent(
    selectedId: String,
    onSelectAssistant: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingXl))
        Text(
            text = tr("Choose your AI assistant"),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("Your assistant guides your AI conversations."),
            color = AppTheme.colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingLg))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingSm)
        ) {
            items(Assistant.ALL) { assistant ->
                val isSelected = assistant.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
                        .background(if (isSelected) AppTheme.colors.surfaceSelected else AppTheme.colors.surfaceCard)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.border,
                            shape = RoundedCornerShape(AppTheme.dimens.cornerMd)
                        )
                        .clickable { onSelectAssistant(assistant.id) }
                        .padding(AppTheme.dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AppTheme.colors.primary else AppTheme.colors.border),
                        contentAlignment = Alignment.Center
                    ) {
                        if (assistant.id == "sparky") {
                            Icon(
                                imageVector = Icons.Rounded.SmartToy,
                                contentDescription = null,
                                tint = if (isSelected) AppTheme.colors.textOnPrimary else AppTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = if (isSelected) AppTheme.colors.textOnPrimary else AppTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(AppTheme.dimens.spacingMd))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = assistant.name,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(assistant.text),
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
