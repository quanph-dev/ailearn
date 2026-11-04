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
import androidx.compose.foundation.Image
import com.example.basekmp.BackActionHandler
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.plan_start_robot
import ailearn.shared.generated.resources.plan_survey_assistant
import ailearn.shared.generated.resources.plan_survey_level
import ailearn.shared.generated.resources.plan_survey_name
import ailearn.shared.generated.resources.plan_survey_time
import com.example.basekmp.components.AppLinearProgressBar
import com.example.basekmp.data.EnglishLevel
import com.example.basekmp.data.GoalPlan
import com.example.basekmp.data.Languages
import com.example.basekmp.data.PlanCatalogData
import com.example.basekmp.data.PlanCatalogRepository
import com.example.basekmp.data.PlanGoal
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.data.trf
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.components.BackButton
import org.jetbrains.compose.resources.painterResource

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
    var selectedGoalId by remember {
        mutableStateOf(GoalPlan.canonicalGoalId(PlanProgress.get().goalId) ?: "everyday")
    }
    var goalNote by remember { mutableStateOf(PlanProgress.get().goalNote) }
    var selectedLevel by remember { mutableStateOf(UserPrefs.englishLevel) }
    var selectedMinutes by remember { mutableIntStateOf(UserPrefs.dailyGoalMinutes) }
    var planCatalog by remember { mutableStateOf<PlanCatalogData?>(null) }
    var isCatalogLoading by remember { mutableStateOf(true) }
    var catalogRetryKey by remember { mutableIntStateOf(0) }
    val learnCode = GoalPlan.learnCode(UserPrefs.targetLanguage)
    val goalChoices = remember(planCatalog, learnCode) {
        planCatalog?.let { setupGoalChoices(it, learnCode) }.orEmpty()
    }
    val planAssistants = remember {
        Assistant.ALL.filter { it.id == "emma" || it.id == "james" }
    }
    var selectedAssistantId by remember {
        mutableStateOf(
            if (UserPrefs.tutorVoice.equals("Male", ignoreCase = true) ||
                UserPrefs.tutorVoice.equals("James", ignoreCase = true)
            ) "james" else "emma"
        )
    }

    val currentStep = steps[currentStepIndex]
    val selectedGoalAvailable = goalChoices.any { it.id == selectedGoalId && it.hasContent }
    val canAdvance = when (currentStep) {
        SetupStep.NAME -> studentName.isNotBlank()
        SetupStep.GOAL -> selectedGoalAvailable && (selectedGoalId != "custom" || goalNote.isNotBlank())
        else -> true
    }

    LaunchedEffect(catalogRetryKey) {
        isCatalogLoading = true
        planCatalog = runCatching {
            PlanCatalogRepository.getCatalog(forceRefresh = catalogRetryKey > 0)
        }.getOrNull()
        isCatalogLoading = false
    }

    LaunchedEffect(planCatalog, learnCode) {
        val availableGoalIds = goalChoices.filter { it.hasContent }.map { it.id }.toSet()
        if (availableGoalIds.isNotEmpty() && selectedGoalId !in availableGoalIds) {
            selectedGoalId = availableGoalIds.firstOrNull { it == "everyday" }
                ?: availableGoalIds.first()
        }
    }

    fun advance() {
        if (!canAdvance) return
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
        } else {
            // Finish and save
            val progress = PlanProgress.get()
            progress.start(
                goal = selectedGoalId,
                name = studentName,
                goalNote = if (selectedGoalId == "custom") goalNote else ""
            )
            UserPrefs.englishLevel = selectedLevel
            UserPrefs.dailyGoalMinutes = selectedMinutes
            val chosenAssistant = planAssistants.firstOrNull { it.id == selectedAssistantId }
            UserPrefs.tutorVoice = chosenAssistant?.voice ?: "Female"
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
                BackButton(onClick = { goBack() })

                if (currentStep != SetupStep.START) {
                    AppLinearProgressBar(
                        progress = currentStepIndex / (steps.size - 1).toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AppTheme.colors.primary
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
                        choices = goalChoices,
                        isLoading = isCatalogLoading,
                        hasCatalog = planCatalog != null,
                        selectedGoalId = selectedGoalId,
                        goalNote = goalNote,
                        onSelectGoal = {
                            selectedGoalId = it
                            if (it != "custom") goalNote = ""
                        },
                        onGoalNoteChange = { goalNote = it.take(150) },
                        onRetry = { catalogRetryKey++ }
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
                        assistants = planAssistants,
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
                    enabled = canAdvance,
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
        Image(
            painter = painterResource(ailearn.shared.generated.resources.Res.drawable.plan_start_robot),
            contentDescription = null,
            modifier = Modifier.size(220.dp)
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingXl))

        Text(
            text = tr("Great to meet you"),
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
        Image(
            painter = painterResource(ailearn.shared.generated.resources.Res.drawable.plan_survey_name),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(154.dp)
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingMd))
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
    choices: List<SetupGoalChoice>,
    isLoading: Boolean,
    hasCatalog: Boolean,
    selectedGoalId: String,
    goalNote: String,
    onSelectGoal: (String) -> Unit,
    onGoalNoteChange: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingXl))
        Text(
            text = trf("Why do you want to learn %1\$s?", tr(Languages.learn(UserPrefs.targetLanguage).displayName)),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("Choose your main goal. You can change it later."),
            color = AppTheme.colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingLg))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.primary, strokeWidth = 2.dp)
            }
        } else if (!hasCatalog || choices.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tr("Plan goals couldn't be loaded. Check your connection and try again."),
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
                TextButton(onClick = onRetry) {
                    Text(tr("Try Again"), color = AppTheme.colors.primary)
                }
            }
        } else {
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val scope = rememberCoroutineScope()
            val listState = rememberLazyListState()
            LaunchedEffect(selectedGoalId, choices) {
                if (selectedGoalId == "custom" && choices.any { it.id == "custom" && it.hasContent }) {
                    listState.animateScrollToItem(choices.size)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingSm)
            ) {
                items(choices, key = { it.id }) { goal ->
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
                        .clickable(enabled = goal.hasContent) { onSelectGoal(goal.id) }
                        .padding(AppTheme.dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(goal.title),
                            color = if (goal.hasContent) AppTheme.colors.textPrimary else AppTheme.colors.textTertiary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(goal.description),
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                    if (!goal.hasContent) {
                        Text(tr("Coming soon"), color = AppTheme.colors.textTertiary, fontSize = 11.sp)
                    } else if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                }
                if (selectedGoalId == "custom" && choices.any { it.id == "custom" && it.hasContent }) {
                    item(key = "custom_goal_input") {
                BasicTextField(
                    value = goalNote,
                    onValueChange = onGoalNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged { focus ->
                            if (focus.isFocused) scope.launch {
                                delay(250)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                        .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
                        .background(AppTheme.colors.surfaceCard)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(AppTheme.dimens.cornerMd))
                        .padding(horizontal = AppTheme.dimens.spacingLg, vertical = AppTheme.dimens.spacingMd),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = AppTheme.colors.textPrimary,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    maxLines = 1,
                    cursorBrush = SolidColor(AppTheme.colors.primary),
                    decorationBox = { innerTextField ->
                        if (goalNote.isEmpty()) {
                            Text(
                                tr("Tell us your personal goal"),
                                color = AppTheme.colors.textTertiary,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
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
        Image(
            painter = painterResource(ailearn.shared.generated.resources.Res.drawable.plan_survey_level),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(154.dp)
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingMd))
        Text(
            text = tr("Current level"),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = trf("How much %s do you know?", tr(UserPrefs.targetLanguage)),
            color = AppTheme.colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingLg))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingSm)
        ) {
            items(LevelOption.ALL) { opt ->
                val isSelected = opt.setting.equals(selectedLevel, ignoreCase = true) ||
                    EnglishLevel.toFullName(opt.setting).equals(EnglishLevel.toFullName(selectedLevel), ignoreCase = true)
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
                            text = EnglishLevel.toCefrCode(opt.setting),
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
        Image(
            painter = painterResource(ailearn.shared.generated.resources.Res.drawable.plan_survey_time),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(154.dp)
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingMd))
        Text(
            text = tr("Daily study time"),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("How much time can you study each day?"),
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
    assistants: List<Assistant>,
    selectedId: String,
    onSelectAssistant: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(ailearn.shared.generated.resources.Res.drawable.plan_survey_assistant),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(154.dp)
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingMd))
        Text(
            text = tr("AI assistant"),
            color = AppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("Who would you like to learn with?"),
            color = AppTheme.colors.textSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingLg))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingSm)
        ) {
            items(assistants) { assistant ->
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

private data class SetupGoalChoice(
    val id: String,
    val title: String,
    val description: String,
    val hasContent: Boolean
)

private fun setupGoalChoices(catalog: PlanCatalogData, learnCode: String): List<SetupGoalChoice> {
    val catalogGoals = catalog.goals.associateBy { it.id }
    return GoalOption.ALL.mapNotNull { option ->
        val goal: PlanGoal = catalogGoals[GoalPlan.canonicalGoalId(option.id)] ?: return@mapNotNull null
        SetupGoalChoice(
            id = goal.id,
            title = option.title,
            description = option.desc,
            hasContent = goal.hasContent(learnCode)
        )
    }
}
