package com.example.basekmp.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.basekmp.components.AppActionDialog
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.data.LearningItem
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.AppAdsHost
import com.example.basekmp.formatDate
import com.example.basekmp.formatDateTime
import com.example.basekmp.formatTime
import com.example.basekmp.theme.BrandPrimaryPressed
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.components.BackTitleBar

private val ScoreGreenBg = Color(0xFFDCFAE6)
private val ScoreGreenText = Color(0xFF12864B)
private val OrangeChipBg = Color(0xFFF5A623)
private val OrangeChipText = Color.White
private val CategoryPinkBg = Color(0xFFFCE3F1)
private val CategoryPinkText = Color(0xFFC13B85)
private val ScoreChipBg = Color(0xFFFFF4CC)
private val ScoreChipText = Color(0xFFB98A00)

@Composable
fun ProgressDetailScreen(
    kind: String,
    onBack: () -> Unit,
    onStartPractice: () -> Unit,
    adsHost: AppAdsHost? = null,
    modifier: Modifier = Modifier
) {
    val displayKind = if (kind.isNotEmpty()) {
        kind.substring(0, 1).uppercase() + kind.substring(1)
    } else kind
    val targetTitle = "$displayKind Practice"
    val titleHeader = Strings.trf("%1\$s Session", Strings.tr(displayKind))
    val screenTitle = Strings.trf("%1\$s Progress", Strings.tr(displayKind))

    val allHistory = UserPrefs.learningHistoryState
    val items = allHistory.filter { it.title == targetTitle }

    var pendingDelete by remember { mutableStateOf<LearningItem?>(null) }
    var detailEntry by remember { mutableStateOf<LearningItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        BackTitleBar(
            title = screenTitle,
            onBack = onBack,
            modifier = Modifier.fillMaxWidth(),
            titleSize = 20.sp
        )

        Spacer(Modifier.height(18.dp))

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = Strings.tr("No sessions yet — finish a practice to see it here."),
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(20.dp))

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(items, key = { _, item -> "${item.title}_${item.timestamp}" }) { index, item ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EntryCard(
                            sessionTitle = titleHeader,
                            category = item.detail,
                            level = item.level.ifBlank { UserPrefs.englishLevel },
                            score = item.score,
                            total = item.total,
                            timestamp = item.timestamp,
                            onDetail = { detailEntry = item },
                            onDelete = { pendingDelete = item },
                            kind = kind
                        )
                        if (index == 0) adsHost?.NativeAd("N_Progress", modifier = Modifier.fillMaxWidth())
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // Confirmation dialog for deletion
    pendingDelete?.let { item ->
        AppActionDialog(
            title = Strings.tr("Delete this record?"),
            message = Strings.tr("This will remove it from your learning history."),
            onDismissRequest = { pendingDelete = null },
            confirmText = Strings.tr("Delete"),
            confirmIsDestructive = true,
            onConfirm = {
                UserPrefs.removeLearning(item)
                pendingDelete = null
            },
            dismissText = Strings.tr("Cancel"),
            onDismissAction = { pendingDelete = null }
        )
    }

    // Detail dialog
    detailEntry?.let { item ->
        Dialog(
            onDismissRequest = { detailEntry = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = titleHeader,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(20.dp))

                    DetailRow(
                        label = Strings.tr("Level"),
                        value = Strings.tr(item.level.ifBlank { UserPrefs.englishLevel })
                    )

                    if (item.score >= 0) {
                        Spacer(Modifier.height(14.dp))
                        val scoreDisplay = if (item.total >= 0) "${item.score}/${item.total}" else "${item.score}"
                        DetailRow(
                            label = Strings.tr("Score"),
                            value = scoreDisplay
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    DetailRow(
                        label = Strings.tr("Date"),
                        value = formatDate(item.timestamp)
                    )

                    Spacer(Modifier.height(14.dp))
                    DetailRow(
                        label = Strings.tr("Time"),
                        value = formatTime(item.timestamp)
                    )

                    Spacer(Modifier.height(24.dp))

                    PrimaryButton(
                        text = Strings.tr("Close"),
                        onClick = { detailEntry = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryCard(
    sessionTitle: String,
    category: String,
    level: String,
    score: Int,
    total: Int,
    timestamp: Long,
    onDetail: () -> Unit,
    onDelete: () -> Unit,
    kind: String,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isGrammar = kind.equals("grammar", ignoreCase = true)
    val isVocab = kind.equals("vocabulary", ignoreCase = true)

    val levelChipBg = if (isGrammar) OrangeChipBg else ScoreChipBg
    val levelChipText = if (isGrammar) OrangeChipText else ScoreChipText
    val scoreChipBg = if (isGrammar) OrangeChipBg else ScoreGreenBg
    val scoreChipText = if (isGrammar) OrangeChipText else ScoreGreenText

    val formattedTime = remember(timestamp) { formatDateTime(timestamp) }
    val scoreText = when {
        total >= 0 -> Strings.trf("Score: %1\$d/%2\$d", score, total)
        score >= 0 -> Strings.trf("Score: %1\$d", score)
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OptionSurface)
            .clickable(onClick = onDetail)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (category.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subLabel = "${Strings.tr(category)} • ${Strings.tr(level)}".uppercase()
                    Text(
                        text = subLabel,
                        color = BrandPrimaryPressed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = sessionTitle,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sessionTitle,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfoChip(
                    text = Strings.trf("Level: %1\$s", Strings.tr(level)),
                    bg = levelChipBg,
                    fg = levelChipText
                )
                if (scoreText != null) {
                    InfoChip(
                        text = scoreText,
                        bg = scoreChipBg,
                        fg = scoreChipText
                    )
                }
                if (category.isNotBlank()) {
                    InfoChip(
                        text = Strings.tr(category),
                        bg = CategoryPinkBg,
                        fg = CategoryPinkText
                    )
                }
            }
        }

        if (!isVocab) {
            Box(contentAlignment = Alignment.TopStart) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = Strings.tr("More"),
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.tr("Detail"), color = TextPrimary) },
                        onClick = {
                            menuOpen = false
                            onDetail()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.tr("Delete"), color = Color(0xFFE53935)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
