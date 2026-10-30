package com.example.basekmp.ui.progress

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.data.LearningItem
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.AppTheme

@Composable
fun ProgressDetailScreen(
    kind: String,
    onBack: () -> Unit,
    onStartPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    val allHistory = UserPrefs.learningHistoryState
    val filteredItems = remember(kind, allHistory.size) {
        if (kind.equals("Streak", ignoreCase = true) || kind.equals("Minutes", ignoreCase = true) || kind.equals("Words", ignoreCase = true)) {
            allHistory.toList()
        } else {
            allHistory.filter { it.title.contains(kind, ignoreCase = true) || it.detail.contains(kind, ignoreCase = true) }
        }
    }

    var selectedItemForDetail by remember { mutableStateOf<LearningItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = dimens.spacingLg, vertical = dimens.spacingSm)
    ) {
        Spacer(Modifier.height(8.dp))
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(dimens.spacingMd))

            Text(
                text = "${Strings.tr(kind)} ${Strings.tr("Details")}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Spacer(Modifier.height(dimens.spacingMd))

        // Analytics Overview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimens.cornerLg))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerLg))
                .padding(dimens.spacingLg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${filteredItems.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        text = Strings.tr("Completed"),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(colors.divider)
                )

                val scoredItems = filteredItems.mapNotNull { it.scorePercent }
                val avgScore = scoredItems.takeIf { it.isNotEmpty() }?.average()?.toInt()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = avgScore?.let { "$it%" } ?: "—",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.success
                    )
                    Text(
                        text = Strings.tr("Avg Score"),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(colors.divider)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = UserPrefs.englishLevel,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentGold
                    )
                    Text(
                        text = Strings.tr("Current Level"),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spacingLg))

        Text(
            text = Strings.tr("Session Logs"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(dimens.spacingSm))

        // Session Log List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimens.spacing3Xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings.tr("No records found for this category yet."),
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                items(filteredItems) { item ->
                    EntryCard(
                        item = item,
                        onDetail = { selectedItemForDetail = item },
                        onDelete = { UserPrefs.removeLearning(item) }
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spacingMd))

        // Bottom CTA Button
        PrimaryButton(
            text = Strings.tr("Start Practice"),
            onClick = onStartPractice
        )
    }

    // Session Detail Dialog
    selectedItemForDetail?.let { item ->
        Dialog(onDismissRequest = { selectedItemForDetail = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(dimens.cornerXl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                    .padding(dimens.spacing2Xl)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = item.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(dimens.spacingSm))
                    Text(
                        text = "${Strings.tr("Category")}: ${item.detail}",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${Strings.tr("Level")}: ${item.level}",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                    if (item.scorePercent != null) {
                        Text(
                            text = "${Strings.tr("Score")}: ${item.score} / ${item.total}",
                            fontSize = 14.sp,
                            color = colors.success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(dimens.spacingXl))
                    PrimaryButton(
                        text = Strings.tr("Close"),
                        onClick = { selectedItemForDetail = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryCard(
    item: LearningItem,
    onDetail: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerMd))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerMd))
            .clickable(onClick = onDetail)
            .padding(dimens.spacingMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${item.detail} • ${item.level}",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            item.scorePercent?.let { scorePercent ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerXs))
                        .background(colors.successBg)
                        .border(1.dp, colors.success.copy(alpha = 0.3f), RoundedCornerShape(dimens.cornerXs))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$scorePercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.success
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 11.sp, color = colors.textSecondary)
            }
        }
    }
}
