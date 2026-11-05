package com.example.basekmp.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.home_ai_tutor
import com.example.basekmp.components.AppLinearProgressBar
import com.example.basekmp.data.PlanCatalogRepository
import com.example.basekmp.data.tr
import com.example.basekmp.theme.AppTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/**
 * Animated generating screen showing roadmap generation milestones.
 * 100% migrated from decompiled PlanGeneratingScreenKt.java.
 */
@Composable
fun PlanGeneratingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var currentMilestoneIndex by remember { mutableIntStateOf(0) }

    val milestones = listOf(
        "Analyzing your goal",
        "Matching lessons to your level",
        "Planning your daily practice",
        "Building your 30-day roadmap"
    )

    LaunchedEffect(Unit) {
        // Preload catalog
        PlanCatalogRepository.getCatalog()

        for (i in milestones.indices) {
            currentMilestoneIndex = i
            val targetProgress = (i + 1) / milestones.size.toFloat()
            val steps = 20
            val delta = (targetProgress - progress) / steps
            for (step in 1..steps) {
                progress += delta
                delay(30)
            }
            delay(400)
        }
        delay(300)
        onDone()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(AppTheme.dimens.spacing2Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.home_ai_tutor),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(AppTheme.dimens.spacing2Xl))

            Text(
                text = tr("Creating your learning path…"),
                color = AppTheme.colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppTheme.dimens.spacingSm))

            Text(
                text = tr("Our AI is tailoring lessons to your answers."),
                color = AppTheme.colors.textSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(AppTheme.dimens.spacing3Xl))

            AppLinearProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AppTheme.colors.primary
            )

            Spacer(modifier = Modifier.height(AppTheme.dimens.spacing2Xl))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingMd)
            ) {
                milestones.forEachIndexed { index, milestone ->
                    val isDone = index < currentMilestoneIndex
                    val isCurrent = index == currentMilestoneIndex

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> AppTheme.colors.primary
                                        isCurrent -> AppTheme.colors.primary.copy(alpha = 0.2f)
                                        else -> AppTheme.colors.surfaceCard
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = AppTheme.colors.textOnPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (isCurrent) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = AppTheme.colors.primary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(AppTheme.dimens.spacingMd))

                        Text(
                            text = tr(milestone),
                            color = if (isDone || isCurrent) AppTheme.colors.textPrimary else AppTheme.colors.textTertiary,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
