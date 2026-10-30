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
import com.example.basekmp.components.ExitSessionDialog
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.data.LearningItem
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.AppTheme

private fun LearningItem.isLessonEntry(): Boolean =
    title.equals("AI Tutor", ignoreCase = true) ||
        title.startsWith("AI Tutor ", ignoreCase = true) ||
        title.equals("Today's Lesson", ignoreCase = true) ||
        title.contains("Lesson", ignoreCase = true) ||
        detail.contains("Lesson", ignoreCase = true)

private fun LearningItem.isShadowingEntry(): Boolean =
    title.contains("Shadowing", ignoreCase = true) || detail.contains("Shadowing", ignoreCase = true)

@Composable
fun LearningHistoryScreen(
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit = {},
    onStartLearning: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    val historyList = UserPrefs.learningHistoryState
    var showClearConfirmation by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = listOf("All", "Lesson", "Shadowing", "Practice")
    val historySnapshot = historyList.toList()

    val filteredList = when (selectedFilter) {
        "Lesson" -> historySnapshot.filter { it.isLessonEntry() }
        "Shadowing" -> historySnapshot.filter { it.isShadowingEntry() }
        "Practice" -> historySnapshot.filter { !it.isLessonEntry() && !it.isShadowingEntry() }
        else -> historySnapshot
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = dimens.spacingLg, vertical = dimens.spacingSm)
    ) {
        Spacer(Modifier.height(8.dp))
        // Top Bar: Back, Title, Clear All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimens.spacingSm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = Strings.tr("Learning History"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            if (historyList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerPill))
                        .background(colors.surfaceVariant)
                        .clickable { showClearConfirmation = true }
                        .padding(horizontal = dimens.spacingMd, vertical = dimens.spacingXs)
                ) {
                    Text(
                        text = Strings.tr("Clear"),
                        color = colors.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spacingSm))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            filterOptions.forEach { opt ->
                val isSelected = opt == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerPill))
                        .background(if (isSelected) colors.primary else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.primary else colors.border,
                            shape = RoundedCornerShape(dimens.cornerPill)
                        )
                        .clickable { selectedFilter = opt }
                        .padding(horizontal = dimens.spacingMd, vertical = dimens.spacingXs)
                ) {
                    Text(
                        text = Strings.tr(opt),
                        color = if (isSelected) colors.textOnPrimary else colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spacingMd))

        // Session list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimens.spacing4Xl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Strings.tr("No history found"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Strings.tr("Complete lessons and practice to see your progress here!"),
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(dimens.spacingXl))
                        PrimaryButton(
                            text = Strings.tr("Start Learning"),
                            onClick = onStartLearning,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            } else {
                items(filteredList) { item ->
                    HistoryCard(
                        entry = item,
                        onOpen = { onOpenLesson(item.title) },
                        onDelete = { UserPrefs.removeLearning(item) }
                    )
                }
            }
        }
    }

    if (showClearConfirmation) {
        ExitSessionDialog(
            onConfirm = {
                UserPrefs.clearAllHistory()
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false }
        )
    }
}

@Composable
private fun HistoryCard(
    entry: LearningItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val metadata = listOf(entry.detail, entry.level).filter(String::isNotBlank).joinToString(" • ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerMd))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerMd))
            .clickable(onClick = onOpen)
            .padding(dimens.spacingMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            if (metadata.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = metadata,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            entry.scorePercent?.let { scorePercent ->
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
