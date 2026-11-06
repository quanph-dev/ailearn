package com.example.basekmp.ui.speaking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.basekmp.components.CongratsDialog
import com.example.basekmp.components.AppLinearProgressBar
import com.example.basekmp.components.HoldToSpeakMic
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.components.ProgressHeader
import com.example.basekmp.data.EnglishLevel
import com.example.basekmp.data.PracticeConfig
import com.example.basekmp.data.Languages
import com.example.basekmp.data.PronunciationScorer
import com.example.basekmp.data.StaticContent
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.components.DropdownField
import com.example.basekmp.ui.components.PracticeSetupScaffold

val SPEAKING_TYPES = listOf("Speak Sentences", "Read Aloud", "Daily Conversation")
val SPEAKING_LEVELS = listOf("Beginner", "Intermediate", "Advanced")

/**
 * Speaking practice setup screen.
 * 100% migrated from decompiled SpeakingScreensKt.java.
 */
@Composable
fun SpeakingSetupScreen(
    onBack: () -> Unit,
    onStart: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(SPEAKING_TYPES.first()) }
    var selectedDifficulty by remember { mutableStateOf(EnglishLevel.to3TierDifficulty(UserPrefs.englishLevel)) }
    var selectedCountText by remember { mutableStateOf("10 cards") }

    val counts = remember { listOf("5 cards", "10 cards", "15 cards") }

    PracticeSetupScaffold(
        title = Strings.tr("Speaking"),
        onBack = onBack,
        onStart = {
            PracticeConfig.speakingType = selectedType
            PracticeConfig.speakingDifficulty = selectedDifficulty
            val count = selectedCountText.substringBefore(" ").toIntOrNull() ?: 10
            onStart(count)
        }
    ) {
        DropdownField(
            label = "Practice Type",
            options = SPEAKING_TYPES,
            selected = selectedType,
            onSelect = { selectedType = it }
        )
        Spacer(Modifier.height(18.dp))
        DropdownField(
            label = "Difficulty",
            options = SPEAKING_LEVELS,
            selected = selectedDifficulty,
            onSelect = { selectedDifficulty = it }
        )
        Spacer(Modifier.height(18.dp))
        DropdownField(
            label = "Number of Speaking Cards",
            options = counts,
            selected = selectedCountText,
            onSelect = { selectedCountText = it }
        )
    }
}

/**
 * Interactive Speaking Cards screen with hold-to-speak recording,
 * acoustic feedback, and pure KMP pronunciation grading.
 */
@Composable
fun SpeakingCardsScreen(
    cardCount: Int,
    onClose: (score: Int, finished: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    var deck by remember { mutableStateOf<List<SpeakingCard>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var currentScore by remember { mutableIntStateOf(0) }
    var totalScores by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var speechError by remember { mutableStateOf("") }
    var isFinished by remember { mutableStateOf(false) }
    var completionRecorded by remember { mutableStateOf(false) }

    LaunchedEffect(cardCount) {
        isLoading = true
        val cards = StaticContent.speaking(
            type = PracticeConfig.speakingType,
            difficulty = PracticeConfig.speakingDifficulty,
            count = cardCount
        )
        deck = cards
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.primary)
        }
        return
    }

    if (deck.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(dimens.spacingLg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.tr("No speaking cards available."),
                color = colors.textSecondary,
                fontSize = 15.sp
            )
        }
        return
    }

    val avgScore = if (totalScores.isEmpty()) 0 else totalScores.values.average().toInt()

    fun finishSpeakingSession() {
        if (!completionRecorded) {
            UserPrefs.recordPracticeDone(
                feature = "speaking",
                score = avgScore,
                total = 100,
                learnedMinutes = 6,
                detail = PracticeConfig.speakingType,
                level = PracticeConfig.speakingDifficulty
            )
            completionRecorded = true
        }
        isFinished = true
    }

    if (isFinished) {
        CongratsDialog(
            subtitle = Strings.tr("Average pronunciation score") + ": $avgScore%",
            buttonText = Strings.tr("Done"),
            onButtonClick = { onClose(avgScore, true) },
            onClose = { onClose(avgScore, true) },
            correct = totalScores.values.count { it >= 60 },
            incorrect = deck.size - totalScores.values.count { it >= 60 }
        )
    }

    val currentCard = deck[currentIndex.coerceIn(0, deck.size - 1)]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Spacer(Modifier.height(8.dp))
        ProgressHeader(
            currentStep = currentIndex + 1,
            totalSteps = deck.size,
            onBack = {
                if (currentIndex > 0) {
                    val previousIndex = currentIndex - 1
                    currentIndex = previousIndex
                    currentScore = totalScores[previousIndex] ?: 0
                } else onClose(avgScore, false)
            },
            onClose = { onClose(avgScore, false) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimens.spacingMd))

            // Sentence Prompt Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.cornerXl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                    .padding(dimens.spacing2Xl)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${Strings.tr("Sentence")} ${currentIndex + 1}/${deck.size}",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        SpeechPlaybackButton(
                            text = currentCard.sentence,
                            languageTag = Languages.learn(UserPrefs.targetLanguage).speechTag
                        )
                    }

                    Spacer(Modifier.height(dimens.spacingXl))

                    Text(
                        text = "“${currentCard.sentence}”",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(Modifier.height(dimens.spacingLg))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerMd))
                            .background(colors.surfaceVariant)
                            .padding(dimens.spacingMd)
                    ) {
                        Text(
                            text = currentCard.meaning,
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimens.spacing2Xl))

            // Hold-to-speak recording controller
            PlatformSpeechRecognition(
                languageTag = Languages.learn(UserPrefs.targetLanguage).speechTag,
                onResult = { transcript ->
                    speechError = ""
                    currentScore = PronunciationScorer.score(currentCard.sentence, transcript)
                    totalScores = totalScores + (currentIndex to currentScore)
                    UserPrefs.markSpeakingDone(currentCard.sentence)
                },
                onError = { speechError = it }
            ) { isListening, startRecording, stopRecording, speechAvailable ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        isListening -> Strings.tr("Listening... release to evaluate")
                        !speechAvailable -> Strings.tr("Speech recognition isn't available on this device")
                        speechError == "permission_required" -> Strings.tr("Microphone permission is required")
                        speechError == "no_match" -> Strings.tr("Try speaking a little slower and clearer.")
                        else -> Strings.tr("Tap mic and speak the sentence aloud")
                    },
                    fontSize = 13.sp,
                    color = if (isListening) colors.primary else colors.textSecondary,
                    fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(Modifier.height(dimens.spacingLg))

                HoldToSpeakMic(
                    isRecording = isListening,
                    onStartRecording = startRecording,
                    onStopRecording = stopRecording
                )

                // Animated Score and Feedback
                AnimatedVisibility(visible = currentIndex in totalScores) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = dimens.spacingLg)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("⭐", fontSize = 20.sp)
                            Text(
                                text = "$currentScore%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentScore >= 60) colors.success else colors.accentGold
                            )
                        }

                        Spacer(Modifier.height(dimens.spacingSm))

                        val animProgress by animateFloatAsState(targetValue = currentScore / 100f)
                        AppLinearProgressBar(
                            progress = animProgress,
                            modifier = Modifier
                                .width(200.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (currentScore >= 60) colors.success else colors.accentGold
                        )

                        Spacer(Modifier.height(dimens.spacingSm))

                        Text(
                            text = when {
                                currentScore >= 80 -> Strings.tr("Excellent clarity and accent!")
                                currentScore >= 60 -> Strings.tr("Good pronunciation!")
                                else -> Strings.tr("Try speaking a little slower and clearer.")
                            },
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spacingLg),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(dimens.primaryButtonHeight)
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerPill))
                    .clickable {
                        currentScore = 0
                        speechError = ""
                        if (currentIndex < deck.size - 1) {
                            currentIndex++
                            currentScore = totalScores[currentIndex] ?: 0
                        } else {
                            finishSpeakingSession()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.tr("Skip"),
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(dimens.primaryButtonHeight)
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .background(colors.primaryBrush)
                    .clickable(enabled = currentIndex in totalScores) {
                        currentScore = 0
                        speechError = ""
                        if (currentIndex < deck.size - 1) {
                            currentIndex++
                            currentScore = totalScores[currentIndex] ?: 0
                        } else {
                            finishSpeakingSession()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentIndex < deck.size - 1) Strings.tr("Next") else Strings.tr("Complete"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SpeakingSelectorSection(
    title: String,
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(dimens.spacingSm))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.cornerMd))
                        .background(if (isSelected) colors.surfaceSelected else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.primary else colors.border,
                            shape = RoundedCornerShape(dimens.cornerMd)
                        )
                        .clickable { onSelect(item) }
                        .padding(horizontal = dimens.spacingLg, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.tr(item),
                        color = if (isSelected) colors.primary else colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) colors.primary else colors.textSecondary, CircleShape)
                            .background(if (isSelected) colors.primary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text("✓", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeakingCountSection(
    title: String,
    counts: List<Int>,
    selectedCount: Int,
    onSelect: (Int) -> Unit
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(dimens.spacingSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            counts.forEach { count ->
                val isSelected = count == selectedCount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(dimens.cornerMd))
                        .background(if (isSelected) colors.primary else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.primary else colors.border,
                            shape = RoundedCornerShape(dimens.cornerMd)
                        )
                        .clickable { onSelect(count) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        color = if (isSelected) Color.White else colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
