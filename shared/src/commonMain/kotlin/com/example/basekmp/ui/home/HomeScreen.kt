package com.example.basekmp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.home_ai_tutor
import ailearn.shared.generated.resources.home_grammar
import ailearn.shared.generated.resources.home_lesson_bg
import ailearn.shared.generated.resources.home_lesson_robot
import ailearn.shared.generated.resources.home_speaking
import ailearn.shared.generated.resources.home_vocab
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.Blue
import com.example.basekmp.theme.CardBorder
import com.example.basekmp.theme.DarkBar
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.components.AppBottomNav
import com.example.basekmp.ui.components.TabHeader
import com.example.basekmp.ui.components.tabBottomClearance
import com.example.basekmp.ui.plan.PlanEntryCard
import com.example.basekmp.ui.plan.PlanPathButton
import com.example.basekmp.ui.shadowing.ShadowingPracticeSection
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private data class PracticeItem(
    val id: String,
    val title: String,
    val icon: DrawableResource,
    val bg: Color
)

private val practices = listOf(
    PracticeItem("ai_tutor", "AI Tutor", Res.drawable.home_ai_tutor, Color(0xFFFFF7C8)),
    PracticeItem("vocabulary", "Vocabulary", Res.drawable.home_vocab, Color(0xFFFDEECB)),
    PracticeItem("speaking", "Speaking", Res.drawable.home_speaking, Color(0xFFE2F4D9)),
    PracticeItem("grammar", "Grammar", Res.drawable.home_grammar, Color(0xFFFCE1E3))
)

@Composable
fun HomeScreen(
    onNavigateToTutor: () -> Unit,
    onNavigateToVocab: () -> Unit,
    onNavigateToSpeaking: () -> Unit,
    onNavigateToGrammar: () -> Unit,
    onNavigateToTodayLesson: () -> Unit,
    onOpenPlan: (hasPlan: Boolean) -> Unit = {},
    onOpenVideo: (String) -> Unit = {},
    onFeatureClick: (String) -> Unit = {},
    onPremiumClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onStatClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val minutesSpent by UserPrefs.learnedMinutesState
    val spentHours = remember(minutesSpent) { minutesSpent / 60.0f }
    val hasPlan = remember { PlanProgress.get().goalId != null }

    val handleFeatureClick: (String) -> Unit = { feature ->
        when (feature) {
            "ai_tutor" -> onNavigateToTutor()
            "vocabulary" -> onNavigateToVocab()
            "speaking" -> onNavigateToSpeaking()
            "grammar" -> onNavigateToGrammar()
            else -> onFeatureClick(feature)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Spacer(Modifier.height(16.dp))

        // TopBar: "Engla" title, "My Path" button with "New" badge, Blue Diamond & Notification bell
        TabHeader(
            title = Strings.tr("Engla"),
            titleSize = 28.sp,
            onPremiumClick = {},
            leadingAction = {
                PlanPathButton(
                    isNew = !hasPlan,
                    onClick = { onOpenPlan(hasPlan) }
                )
            },
            trailing = {
                Icon(
                    imageVector = Icons.Rounded.NotificationsNone,
                    contentDescription = Strings.tr("Notifications"),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onNotificationsClick() }
                        .padding(8.dp),
                    tint = TextPrimary
                )
            }
        )

        // Vertical scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(18.dp))

            // 1. Speak with AI Hero Card (Matching decompiled LessonCard)
            LessonCard(onStartLesson = onNavigateToTodayLesson)

            // 2. Learning Path Entry Card (Matching decompiled PlanEntryCard)
            if (!hasPlan) {
                Spacer(Modifier.height(16.dp))
                PlanEntryCard(onCreate = { onOpenPlan(false) })
            }

            Spacer(Modifier.height(16.dp))

            // 3. Shadowing Practice Section (Matching decompiled ShadowingPracticeSection)
            ShadowingPracticeSection(
                onSeeAll = { onFeatureClick("shadowing") },
                onOpenVideo = onOpenVideo
            )

            Spacer(Modifier.height(16.dp))

            // 4. More Practice 1-row Grid of 4 (Matching decompiled MorePractice)
            MorePractice(onFeatureClick = handleFeatureClick)

            Spacer(Modifier.height(18.dp))

            // 5. Time Spent Section Header
            Text(
                text = Strings.tr("Time Spent"),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // 6. Progress Card (Matching decompiled ProgressCard: aspectRatio 2.44, 2-tone)
            ProgressCard(
                language = Strings.tr("English"),
                spentHours = spentHours,
                goalHours = 8.0f,
                onContinue = onNavigateToTodayLesson
            )

            Spacer(Modifier.height(tabBottomClearance + 16.dp))
        }
    }
}

/**
 * Hero card "Speak with AI" matching decompiled LessonCard in HomeScreenKt.java.
 */
@Composable
private fun LessonCard(
    onStartLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardCorner = RoundedCornerShape(22.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardCorner)
    ) {
        val cardH = maxWidth * 0.36842105f
        val robotH = cardH * 0.8f
        val robotW = robotH * 0.9497042f

        // Background
        Image(
            painter = painterResource(Res.drawable.home_lesson_bg),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        // Text & CTA content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = cardH)
                .padding(start = 16.dp, top = 12.dp, end = robotW + 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.tr("Speak with AI"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Strings.tr("Build confidence every day"),
                    fontSize = 13.sp,
                    color = TextPrimary.copy(alpha = 0.85f),
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))

                // White pill CTA button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable { onStartLesson() }
                        .padding(start = 12.dp, top = 9.dp, end = 16.dp, bottom = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Blue
                    )
                    Text(
                        text = Strings.tr("Start speaking"),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Mascot robot aligned to bottom right
        Image(
            painter = painterResource(Res.drawable.home_lesson_robot),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp)
                .size(width = robotW, height = robotH),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * More Practice section matching decompiled MorePractice in HomeScreenKt.java.
 */
@Composable
private fun MorePractice(
    onFeatureClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, spotColor = Color(0x14000000), ambientColor = Color(0x1F000000))
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder.copy(alpha = 0.6f), shape)
            .padding(14.dp)
    ) {
        Text(
            text = Strings.tr("More practice"),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            practices.forEach { practice ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onFeatureClick(practice.id) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(practice.bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(practice.icon),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.84f),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = Strings.tr(practice.title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(3.dp))
                }
            }
        }
    }
}

/**
 * Two-tone progress card matching decompiled ProgressCard in HomeScreenKt.java.
 */
@Composable
private fun ProgressCard(
    language: String,
    spentHours: Float,
    goalHours: Float,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .aspectRatio(2.4358974f)
    ) {
        // Top section: Blue surface with progress
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Blue)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = language,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = Strings.tr("Your Progress"),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    val spentStr = ((spentHours * 10).toInt() / 10f).toString()
                    val goalStr = ((goalHours * 10).toInt() / 10f).toString().removeSuffix(".0")
                    Text(
                        text = spentStr,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "/${goalStr}h",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Progress bar track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                val progressFraction = (spentHours / goalHours).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        // Bottom section: Dark bar with "Continue" CTA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBar)
                .clickable { onContinue() }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.tr("Continue"),
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
    }
}
