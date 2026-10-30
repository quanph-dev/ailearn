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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.AnimatedBuddy
import com.example.basekmp.components.RobotMood
import com.example.basekmp.data.GoalPlan
import com.example.basekmp.data.PlanCatalogData
import com.example.basekmp.data.PlanCatalogRepository
import com.example.basekmp.data.PlanGoal
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.data.trf
import com.example.basekmp.theme.AppTheme

/**
 * 30-Day Timeline Roadmap Screen.
 * 100% migrated from decompiled PlanScreenKt.java and PlanPathKt.java.
 * Connects 10 Stages × 3 Days per Stage with real completion tracking,
 * XP, diamonds, and day launch actions.
 */
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    onChangeGoal: () -> Unit,
    onOpenDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = PlanProgress.get()
    val revision = progress.revision.intValue // triggers recomposition when progress updates

    var catalog by remember { mutableStateOf<PlanCatalogData?>(null) }
    var currentGoal by remember { mutableStateOf<PlanGoal?>(null) }

    LaunchedEffect(revision) {
        val cat = PlanCatalogRepository.getCatalog()
        catalog = cat
        val goalId = progress.goalId
        currentGoal = cat?.goal(goalId) ?: cat?.goals?.firstOrNull()
    }

    val completedDays = remember(revision) { progress.completedDays() }
    val currentDay = remember(revision) { progress.currentDay() }
    val percentDone = (completedDays.size * 100) / GoalPlan.DAYS

    val listState = rememberLazyListState()

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
                    .background(AppTheme.colors.surface)
                    .padding(horizontal = AppTheme.dimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = tr("Back"),
                        tint = AppTheme.colors.textPrimary
                    )
                }

                Text(
                    text = tr("My 30-day plan"),
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // XP Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppTheme.dimens.cornerPill))
                        .background(AppTheme.colors.surfaceCard)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = AppTheme.colors.warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${progress.xp} XP",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Diamonds Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppTheme.dimens.cornerPill))
                        .background(AppTheme.colors.surfaceCard)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Diamond,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${progress.diamonds}",
                        color = AppTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Goal Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.surfaceCard)
                    .padding(AppTheme.dimens.spacingLg)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentGoal?.title ?: "Everyday Conversation",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = trf("Day %1\$d of %2\$d", currentDay.coerceAtMost(30), GoalPlan.DAYS),
                                color = AppTheme.colors.textSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = tr("Change goal"),
                            color = AppTheme.colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppTheme.dimens.cornerSm))
                                .clickable { onChangeGoal() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppTheme.dimens.spacingMd))

                    LinearProgressIndicator(
                        progress = { percentDone / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AppTheme.colors.primary,
                        trackColor = AppTheme.colors.surface
                    )
                }
            }

            // 30-Day Timeline Stages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(AppTheme.dimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingXl)
            ) {
                val stages = currentGoal?.stages ?: emptyList()
                val totalStages = if (stages.isNotEmpty()) stages.size else 10

                for (stageIdx in 0 until totalStages) {
                    val stage = stages.getOrNull(stageIdx)
                    val stageNum = stageIdx + 1
                    val stageTitle = stage?.title ?: "Stage $stageNum"
                    val startDay = stageIdx * GoalPlan.DAYS_PER_STAGE + 1
                    val endDay = (stageIdx + 1) * GoalPlan.DAYS_PER_STAGE

                    item(key = "stage_$stageNum") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Stage Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppTheme.dimens.spacingSm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trf("Stage %1\$d · Days %2\$d–%3\$d", stageNum, startDay, endDay),
                                    color = AppTheme.colors.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stageTitle,
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            // 3 Days Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (day in startDay..endDay) {
                                    val isDone = completedDays.contains(day)
                                    val isCurrent = day == currentDay
                                    val isUnlocked = progress.isUnlocked(day)

                                    DayNode(
                                        day = day,
                                        isDone = isDone,
                                        isCurrent = isCurrent,
                                        isUnlocked = isUnlocked,
                                        onClick = {
                                            if (isUnlocked || isDone) {
                                                onOpenDay(day)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating "Continue Day" CTA
            if (currentDay <= GoalPlan.DAYS) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.surface)
                        .padding(AppTheme.dimens.spacingLg)
                ) {
                    Button(
                        onClick = { onOpenDay(currentDay) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppTheme.dimens.primaryButtonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                        shape = RoundedCornerShape(AppTheme.dimens.cornerPill)
                    ) {
                        Text(
                            text = trf("Start Day %1\$d", currentDay),
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

@Composable
private fun DayNode(
    day: Int,
    isDone: Boolean,
    isCurrent: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
            .clickable(enabled = isUnlocked || isDone) { onClick() }
            .padding(AppTheme.dimens.spacingSm)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> AppTheme.colors.success
                        isCurrent -> AppTheme.colors.primary
                        isUnlocked -> AppTheme.colors.surfaceCard
                        else -> AppTheme.colors.surface
                    }
                )
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = when {
                        isDone -> AppTheme.colors.success
                        isCurrent -> AppTheme.colors.primary
                        else -> AppTheme.colors.border
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDone -> {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = AppTheme.colors.textOnPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                isCurrent -> {
                    AnimatedBuddy(mood = RobotMood.SUPER, size = 48.dp)
                }
                isUnlocked -> {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = AppTheme.colors.textTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Day $day",
            color = if (isCurrent || isDone) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
        )
    }
}
