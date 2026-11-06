package com.example.basekmp.ui.progress

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.AppActionDialog
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.data.LearningItem
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.AppAdsHost
import com.example.basekmp.formatDate
import com.example.basekmp.formatTime
import com.example.basekmp.theme.BrandPrimaryPressed
import com.example.basekmp.theme.IncorrectRed
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.OptionSurfaceSelected
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.components.BackTitleBar

private val LESSON_TITLES = setOf("AI Tutor", "Today's Lesson")

private fun formatHistoryWhen(ts: Long): String {
    if (ts <= 0) return ""
    return "${formatDate(ts)} • ${formatTime(ts)}"
}

@Composable
fun LearningHistoryScreen(
    onBack: () -> Unit,
    adsHost: AppAdsHost? = null,
    onOpenLesson: (String) -> Unit = {},
    onStartLearning: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val historyList = UserPrefs.learningHistoryState
    var pendingDelete by remember { mutableStateOf<LearningItem?>(null) }

    val lessonEntries = historyList.filter {
        LESSON_TITLES.contains(it.title) || it.title.startsWith("AI Tutor") || it.title.contains("Lesson")
    }

    val groupedHistory = remember(lessonEntries) {
        val map = LinkedHashMap<String, MutableList<LearningItem>>()
        for (item in lessonEntries) {
            val title = item.title
            map.getOrPut(title) { mutableListOf() }.add(item)
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Spacer(Modifier.height(10.dp))

        BackTitleBar(
            title = Strings.tr("Learning History"),
            onBack = onBack
        )

        Spacer(Modifier.height(16.dp))

        if (lessonEntries.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = Strings.tr("No activity yet — finish a lesson or practice to see it here."),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(20.dp))

                PrimaryButton(
                    text = Strings.tr("Start Now"),
                    onClick = onStartLearning,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                groupedHistory.forEach { (category, items) ->
                    item(key = "h_$category") {
                        Text(
                            text = Strings.tr(category),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(
                        items = items,
                        key = { "${it.title}_${it.timestamp}" }
                    ) { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HistoryCard(
                                category = category,
                                entry = item,
                                onOpen = { onOpenLesson(item.title) },
                                onDelete = { pendingDelete = item }
                            )
                            if (item == lessonEntries.firstOrNull()) {
                                adsHost?.NativeAd("N_Progress", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AppActionDialog(
            title = Strings.tr("Delete this activity?"),
            message = Strings.tr("This will remove it from your Learning History."),
            onDismissRequest = { pendingDelete = null },
            confirmText = Strings.tr("Delete"),
            onConfirm = {
                UserPrefs.removeLearning(item)
                pendingDelete = null
            },
            dismissText = Strings.tr("Cancel"),
            onDismissAction = { pendingDelete = null },
            confirmIsDestructive = true
        )
    }
}

@Composable
private fun HistoryCard(
    category: String,
    entry: LearningItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OptionSurface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(OptionSurfaceSelected)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.tr(category),
                    color = BrandPrimaryPressed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = formatHistoryWhen(entry.timestamp),
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = Strings.tr("Delete"),
                tint = IncorrectRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
