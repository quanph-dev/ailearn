package com.example.basekmp.ui.plan

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
import com.example.basekmp.data.GoalPlan
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.data.trf
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.components.BackButton

/** Day-level activity launcher; activity state changes only from the completed activity screens. */
@Composable
fun PlanDayActivitiesScreen(
    day: Int,
    onBack: () -> Unit,
    onOpenActivity: (PlanProgress.Activity, PlanFlow.DayPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = PlanProgress.get()
    val revision = progress.revision.intValue
    var dayPlan by remember(day) { mutableStateOf<PlanFlow.DayPlan?>(null) }

    LaunchedEffect(day) {
        dayPlan = PlanFlow.dayPlan(day)
    }

    Surface(modifier = modifier.fillMaxSize(), color = AppTheme.colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimens.topBarHeight)
                    .background(AppTheme.colors.surface)
                    .padding(horizontal = AppTheme.dimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(
                    onClick = onBack,
                    background = AppTheme.colors.background,
                    tint = AppTheme.colors.textPrimary
                )
                Text(
                    text = "${tr("Day")} $day",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val plan = dayPlan
            if (plan == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tr("This plan day is unavailable"), color = AppTheme.colors.textSecondary)
                }
                return@Surface
            }

            val completed = remember(revision, day) { progress.done(day) }
            val finished = remember(revision, day) { progress.completedDays().contains(day) }
            if (!finished && !progress.isUnlocked(day)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimens.spacingXl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingMd)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = trf("Day %1\$d", day),
                        color = AppTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("This plan day is unavailable"),
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
                return@Surface
            }
            val unlocked = PlanFlow.unlocked(plan, completed, finished)
            val doneCount = plan.available.count { it in completed }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingMd)
            ) {
                Text(
                    text = plan.stage.displayTitle(GoalPlan.learnCode(UserPrefs.targetLanguage)),
                    color = AppTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr("Complete each activity to finish this day"),
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
                PlanProgressBar(
                    progress = doneCount.toFloat() / plan.available.size.coerceAtLeast(1),
                    modifier = Modifier.fillMaxWidth(),
                    height = 8.dp
                )
                Text(
                    text = trf("%1\$d/%2\$d activities complete", doneCount, plan.available.size),
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(AppTheme.dimens.spacingSm))
                plan.available.forEachIndexed { index, activity ->
                    val isDone = activity in completed
                    val isUnlocked = activity in unlocked
                    val hasContent = activity != PlanProgress.Activity.VIDEO || !plan.videoId.isNullOrBlank()
                    val enabled = isUnlocked && hasContent
                    val title = when (activity) {
                        PlanProgress.Activity.VIDEO -> tr("Video Shadowing")
                        PlanProgress.Activity.QUIZ -> tr("Quick Quiz")
                        PlanProgress.Activity.SPEAKING -> tr("Speaking Practice")
                        PlanProgress.Activity.TALK -> tr("Talk with the Tutor")
                    }
                    val subtitle = when {
                        isDone -> tr("Completed")
                        !hasContent -> tr("Content unavailable")
                        !isUnlocked -> tr("Complete the previous activity first")
                        else -> tr("Start activity")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppTheme.dimens.cornerMd))
                            .background(AppTheme.colors.surfaceCard)
                            .clickable(enabled = enabled) { onOpenActivity(activity, plan) }
                            .padding(AppTheme.dimens.spacingLg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDone) AppTheme.colors.success else AppTheme.colors.surfaceSelected),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isDone -> Icons.Rounded.Check
                                    !isUnlocked -> Icons.Rounded.Lock
                                    else -> Icons.Rounded.PlayArrow
                                },
                                contentDescription = null,
                                tint = if (isDone) AppTheme.colors.textOnPrimary else AppTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = AppTheme.dimens.spacingMd)
                        ) {
                            Text(
                                text = "${index + 1}. $title",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = subtitle,
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
