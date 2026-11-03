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
import androidx.compose.ui.text.style.TextAlign
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
    val historySnapshot = allHistory.toList()
    val filteredItems = historySnapshot.filter {
        it.title.contains(kind, ignoreCase = true) || it.detail.contains(kind, ignoreCase = true)
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
                text = Strings.trf("%1\$s Progress", Strings.tr(kind)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Spacer(Modifier.height(dimens.spacingMd))

        if (filteredItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = Strings.tr("No sessions yet — finish a practice to see it here."),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spacingMd),
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(dimens.spacingMd))
                PrimaryButton(
                    text = Strings.tr("Start Now"),
                    onClick = onStartPractice,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
            ) {
                items(filteredItems) { item ->
                    EntryCard(
                        item = item,
                        onDetail = { selectedItemForDetail = item },
                        onDelete = { UserPrefs.removeLearning(item) }
                    )
                }
            }
        }
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
                    if (item.detail.isNotBlank()) {
                        Text(
                            text = "${Strings.tr("Category")}: ${item.detail}",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = "${Strings.tr("Level")}: ${Strings.tr(item.level.ifBlank { UserPrefs.englishLevel })}",
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
    val metadata = listOf(item.detail, item.level).filter(String::isNotBlank).joinToString(" • ")

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
