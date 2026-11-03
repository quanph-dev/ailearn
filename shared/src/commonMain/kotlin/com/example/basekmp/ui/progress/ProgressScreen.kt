package com.example.basekmp.ui.progress

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.streak_fire
import com.example.basekmp.formatTime
import com.example.basekmp.formatWeekdayShort
import com.example.basekmp.localEpochDayAt
import com.example.basekmp.data.LearningItem
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.BlueSelected
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.ProgressBlue
import com.example.basekmp.theme.ProgressGreen
import com.example.basekmp.theme.ProgressPink
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.components.TabHeader
import org.jetbrains.compose.resources.painterResource

private val WEEKDAY_INITIALS = arrayOf("T", "F", "S", "S", "M", "T", "W")
private val VIETNAMESE_WEEKDAY_LABELS = arrayOf("T5", "T6", "T7", "CN", "T2", "T3", "T4")

private fun shortWeekdayLabel(epochDay: Long): String {
    val index = (((epochDay % 7L) + 7L) % 7L).toInt()
    val labels = if (Strings.getLanguage() == "vi") VIETNAMESE_WEEKDAY_LABELS else WEEKDAY_INITIALS
    return labels[index]
}

private const val CAT_AI_TUTOR = "AI Tutor"
private const val CAT_LESSON = "Today's Lesson"
private const val CAT_PRACTICE = "Practice"

private fun epochDayOf(ts: Long): Long = localEpochDayAt(ts)

private fun formatWhen(ts: Long): String {
    if (ts <= 0) return ""
    val timeStr = formatTime(ts)
    val today = UserPrefs.todayEpochDay()
    val day = epochDayOf(ts)
    return when (day) {
        today -> "${Strings.tr("Today")}, $timeStr"
        today - 1L -> "${Strings.tr("Yesterday")}, $timeStr"
        else -> "${formatWeekdayShort(ts)}, $timeStr"
    }
}

private fun categoryOf(title: String): String {
    return when (title) {
        CAT_AI_TUTOR -> CAT_AI_TUTOR
        CAT_LESSON -> CAT_LESSON
        else -> CAT_PRACTICE
    }
}

@Composable
fun ProgressScreen(
    onStatClick: (String) -> Unit = {},
    onLessonClick: (String) -> Unit = {},
    onSeeAllHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = UserPrefs.todayEpochDay()
    val lastActive = UserPrefs.lastActiveDayState.longValue
    val hasActiveStreak = lastActive == today || lastActive == today - 1L
    val streak = if (hasActiveStreak) UserPrefs.streakCountState.intValue else 0
    val historyList = UserPrefs.learningHistoryState

    val vocabCount = historyList.count { it.title == "Vocabulary Practice" }
    val grammarCount = historyList.count { it.title == "Grammar Practice" }
    val speakingCount = historyList.count { it.title == "Speaking Practice" }

    val tutorSessions = historyList.filter {
        it.timestamp > 0 && epochDayOf(it.timestamp) == today && categoryOf(it.title) == CAT_AI_TUTOR
    }
    val todayLessons = historyList.filter {
        it.timestamp > 0 && epochDayOf(it.timestamp) == today && categoryOf(it.title) == CAT_LESSON
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Spacer(Modifier.height(16.dp))

        TabHeader(
            title = Strings.tr("Your Progress")
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // Keep the established 2–1 grouping while rendering all metrics consistently.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatCard(
                    title = Strings.tr("Vocabulary"),
                    value = vocabCount,
                    color = ProgressPink,
                    onClick = { onStatClick("vocabulary") },
                    modifier = Modifier.weight(1f).height(120.dp)
                )

                StatCard(
                    title = Strings.tr("Grammar"),
                    value = grammarCount,
                    color = ProgressGreen,
                    onClick = { onStatClick("grammar") },
                    modifier = Modifier.weight(1f).height(120.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            StatCard(
                title = Strings.tr("Speaking"),
                value = speakingCount,
                color = ProgressBlue,
                onClick = { onStatClick("speaking") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Spacer(Modifier.height(24.dp))

            // 3. Your Streak Section
            Text(
                text = Strings.tr("Your Streak"),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            StreakCard(
                streak = streak,
                today = today,
                lastActive = lastActive
            )

            Spacer(Modifier.height(24.dp))

            // 4. Learning Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.tr("Learning"),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(OptionSurface)
                        .clickable(onClick = onSeeAllHistory)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Strings.tr("See All"),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 5. Learning Sessions
            if (tutorSessions.isEmpty() && todayLessons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Strings.tr("No AI tutor sessions yet"),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LearningGroup(
                    label = Strings.tr("AI tutor sessions"),
                    entries = tutorSessions.take(3),
                    emptyText = Strings.tr("No AI tutor sessions yet"),
                    onOpen = onLessonClick
                )

                Spacer(Modifier.height(18.dp))

                LearningGroup(
                    label = Strings.tr("Today Lesson"),
                    entries = todayLessons.take(3),
                    emptyText = Strings.tr("No lessons yet"),
                    onOpen = onLessonClick
                )
            }

            Spacer(Modifier.height(100.dp)) // Bottom Nav clearance
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value.toString(),
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = Strings.tr("times"),
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StreakCard(
    streak: Int,
    today: Long,
    lastActive: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(OptionSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(Res.drawable.streak_fire),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 44.dp, height = 56.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Strings.trf("%1\$d - Days Streak. Keep Learning!", streak),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                for (i in 0 until 7) {
                    val dayEpoch = (today - 6L) + i.toLong()
                    val weekdayLabel = shortWeekdayLabel(dayEpoch)
                    val isDayActive = streak > 0 && dayEpoch <= lastActive && dayEpoch > lastActive - streak.toLong()
                    val isToday = dayEpoch == today

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = weekdayLabel,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(Modifier.height(5.dp))

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isDayActive) BlueSelected else Color(0xFFEFF1F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDayActive) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(BlueSelected)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningGroup(
    label: String,
    entries: List<LearningItem>,
    emptyText: String,
    onOpen: (String) -> Unit
) {
    Text(
        text = label,
        color = TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(10.dp))

    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyText,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    } else {
        entries.forEachIndexed { index, item ->
            LearningRow(
                title = item.title,
                subtitle = formatWhen(item.timestamp),
                onClick = { onOpen(item.title) }
            )

            if (index < entries.lastIndex) {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun LearningRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OptionSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Strings.tr(title),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
