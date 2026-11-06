package com.semavia.ailearn.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semavia.ailearn.data.Strings
import com.semavia.ailearn.theme.BrandPrimary
import com.semavia.ailearn.theme.ChipOutlineText
import com.semavia.ailearn.theme.DividerColor
import com.semavia.ailearn.theme.LessonCardBg
import com.semavia.ailearn.theme.TextPrimary
import com.semavia.ailearn.ui.components.TabHeader
import com.semavia.ailearn.ui.components.tabBottomClearance
import com.semavia.ailearn.analytics.LocalAnalyticsEventLogger
import com.semavia.ailearn.analytics.track

/**
 * Model representing a practice lesson card.
 * 100% migrated from original app Lesson.java.
 */
data class Lesson(
    val title: String,
    val level: String,
    val minutes: Int = 5
)

typealias PracticeLesson = Lesson

/**
 * Complete exact 60 practice lessons from decompiled PracticeScreenKt.java.
 * 20 Beginner, 20 Intermediate, 20 Advanced.
 */
val ALL_PRACTICE_LESSONS = listOf(
    // 20 Beginner
    Lesson("Greeting Someone New", "Beginner", 5),
    Lesson("Talking About the Weather", "Beginner", 5),
    Lesson("Making Small Talk", "Beginner", 5),
    Lesson("Introducing Your Family", "Beginner", 5),
    Lesson("Ordering a Coffee", "Beginner", 5),
    Lesson("Buying Groceries", "Beginner", 5),
    Lesson("Asking Someone's Name", "Beginner", 5),
    Lesson("Telling the Time", "Beginner", 5),
    Lesson("Numbers and Prices", "Beginner", 5),
    Lesson("Days, Months and Dates", "Beginner", 5),
    Lesson("Colors and Clothes", "Beginner", 5),
    Lesson("Ordering at a Fast-Food Place", "Beginner", 5),
    Lesson("Saying Goodbye", "Beginner", 5),
    Lesson("Talking About Your Hobbies", "Beginner", 5),
    Lesson("Describing Your Home", "Beginner", 5),
    Lesson("Asking for Help", "Beginner", 5),
    Lesson("At the Bus Stop", "Beginner", 5),
    Lesson("Talking About Your Day", "Beginner", 5),
    Lesson("Meeting a Neighbor", "Beginner", 5),
    Lesson("Ordering Water at a Table", "Beginner", 5),

    // 20 Intermediate
    Lesson("Ordering Food at a Café", "Intermediate", 5),
    Lesson("Asking for Directions", "Intermediate", 5),
    Lesson("At the Doctor's Office", "Intermediate", 5),
    Lesson("Booking a Hotel Room", "Intermediate", 5),
    Lesson("Checking In at the Airport", "Intermediate", 5),
    Lesson("Making a Phone Call", "Intermediate", 5),
    Lesson("Shopping for Clothes", "Intermediate", 5),
    Lesson("Talking About Your Weekend", "Intermediate", 5),
    Lesson("Renting an Apartment", "Intermediate", 5),
    Lesson("At the Pharmacy", "Intermediate", 5),
    Lesson("Giving Your Opinion", "Intermediate", 5),
    Lesson("Planning a Trip", "Intermediate", 5),
    Lesson("At the Bank", "Intermediate", 5),
    Lesson("Describing a Problem", "Intermediate", 5),
    Lesson("Talking About Movies", "Intermediate", 5),
    Lesson("Getting Customer Support", "Intermediate", 5),
    Lesson("Discussing the News", "Intermediate", 5),
    Lesson("Talking About Food Preferences", "Intermediate", 5),
    Lesson("Explaining Where You Live", "Intermediate", 5),
    Lesson("Making Plans With a Friend", "Intermediate", 5),

    // 20 Advanced
    Lesson("Shopping in a Store", "Advanced", 5),
    Lesson("A Job Interview", "Advanced", 5),
    Lesson("Negotiating a Salary", "Advanced", 5),
    Lesson("Giving a Presentation", "Advanced", 5),
    Lesson("Debating a Topic", "Advanced", 5),
    Lesson("Handling a Complaint", "Advanced", 5),
    Lesson("Discussing Current Events", "Advanced", 5),
    Lesson("Making a Business Pitch", "Advanced", 5),
    Lesson("Talking About Your Career", "Advanced", 5),
    Lesson("Resolving a Conflict", "Advanced", 5),
    Lesson("Describing a Complex Process", "Advanced", 5),
    Lesson("Discussing Culture and Traditions", "Advanced", 5),
    Lesson("Interviewing a Candidate", "Advanced", 5),
    Lesson("Talking About the Environment", "Advanced", 5),
    Lesson("Explaining a Technical Problem", "Advanced", 5),
    Lesson("Discussing Future Plans", "Advanced", 5),
    Lesson("Persuading Your Team", "Advanced", 5),
    Lesson("Talking About Books", "Advanced", 5),
    Lesson("Networking at an Event", "Advanced", 5),
    Lesson("Discussing Health and Fitness", "Advanced", 5)
)

private val FILTERS = listOf("All", "Beginner", "Intermediate", "Advanced")

/**
 * Practice Screen (Learning Board) matching decompiled PracticeScreenKt.java 100%.
 */
@Composable
fun PracticeScreen(
    onStartLesson: (Lesson) -> Unit,
    onOpenCategory: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val analytics = LocalAnalyticsEventLogger.current

    val filteredLessons = remember(selectedFilter) {
        if (selectedFilter == "All") {
            ALL_PRACTICE_LESSONS
        } else {
            ALL_PRACTICE_LESSONS.filter { it.level.equals(selectedFilter, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Spacer(Modifier.height(16.dp))

        // Top Header: Learning Board
        TabHeader(
            title = Strings.tr("Learning Board")
        )

        Spacer(Modifier.height(16.dp))

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FILTERS.forEach { filter ->
                FilterChip(
                    text = filter,
                    selected = filter == selectedFilter,
                    onClick = {
                        if (filter != selectedFilter) {
                            analytics.track("practice_filter_selected", "difficulty_filter" to filter.lowercase())
                        }
                        selectedFilter = filter
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 60 Lessons list matching decompiled LessonCard
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = tabBottomClearance)
        ) {
            items(
                items = filteredLessons,
                key = { it.title }
            ) { lesson ->
                LessonCard(
                    lesson = lesson,
                    onStart = { onStartLesson(lesson) }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape
    val backgroundModifier = if (selected) {
        Modifier.background(BrandPrimary)
    } else {
        Modifier
            .border(1.dp, DividerColor, shape)
            .background(Color.White)
    }

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .then(backgroundModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = Strings.tr(text),
            color = if (selected) Color.White else ChipOutlineText,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LessonCardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.tr(lesson.title),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                val annotated = buildAnnotatedString {
                    append(Strings.tr("Level: "))
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(Strings.tr(lesson.level))
                    }
                }
                Text(
                    text = annotated,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = Strings.trf("%1\$d min", lesson.minutes),
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onStart)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.tr("Start"),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.height(16.dp),
                tint = TextPrimary
            )
        }
    }
}
