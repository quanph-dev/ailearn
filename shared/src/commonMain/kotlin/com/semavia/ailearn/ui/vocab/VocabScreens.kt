package com.semavia.ailearn.ui.vocab

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semavia.ailearn.components.CongratsDialog
import com.semavia.ailearn.components.PrimaryButton
import com.semavia.ailearn.components.ProgressHeader
import com.semavia.ailearn.data.EnglishLevel
import com.semavia.ailearn.data.PracticeConfig
import com.semavia.ailearn.data.Languages
import com.semavia.ailearn.data.StaticContent
import com.semavia.ailearn.data.Strings
import com.semavia.ailearn.data.UserPrefs
import com.semavia.ailearn.data.VocabularyTranslations
import com.semavia.ailearn.theme.AppTheme
import com.semavia.ailearn.ui.components.DropdownField
import com.semavia.ailearn.ui.components.PracticeSetupScaffold
import com.semavia.ailearn.ui.speaking.SpeechCaptureMic
import com.semavia.ailearn.ui.speaking.SpeechPlaybackButton
import kotlinx.coroutines.launch

/**
 * Vocabulary practice setup screen with Category, Level, and Count selectors.
 * 100% migrated from decompiled VocabScreensKt.java.
 */
@Composable
fun VocabSetupScreen(
    onBack: () -> Unit,
    onStart: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Daily life") }
    var selectedLevel by remember { mutableStateOf(EnglishLevel.to3TierDifficulty(UserPrefs.englishLevel)) }
    var selectedCountText by remember { mutableStateOf("10 words") }

    val categories = remember { listOf("Daily life", "Business", "Travel", "IELTS", "Academic") }
    val levels = remember { listOf("Beginner", "Intermediate", "Advanced") }
    val counts = remember { listOf("5 words", "10 words", "15 words") }

    PracticeSetupScaffold(
        title = Strings.tr("Vocabulary"),
        onBack = onBack,
        onStart = {
            PracticeConfig.vocabCategory = selectedCategory
            PracticeConfig.vocabLevel = selectedLevel
            val count = selectedCountText.substringBefore(" ").toIntOrNull() ?: 10
            onStart(count)
        }
    ) {
        DropdownField(
            label = "Category",
            options = categories,
            selected = selectedCategory,
            onSelect = { selectedCategory = it }
        )
        Spacer(Modifier.height(18.dp))
        DropdownField(
            label = "Level of Difficulty",
            options = levels,
            selected = selectedLevel,
            onSelect = { selectedLevel = it }
        )
        Spacer(Modifier.height(18.dp))
        DropdownField(
            label = "Number of Words",
            options = counts,
            selected = selectedCountText,
            onSelect = { selectedCountText = it }
        )
    }
}

/**
 * Interactive Vocabulary Flashcard screen with TTS, word highlighting,
 * and pure KMP pronunciation scoring.
 */
@Composable
fun VocabCardsScreen(
    wordCount: Int,
    onClose: (correct: Int, total: Int) -> Unit,
    onFinish: (correct: Int, total: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    var deck by remember { mutableStateOf<List<VocabWord>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var speechScore by remember { mutableIntStateOf(0) }
    var speechScores by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var showTranslation by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var completionRecorded by remember { mutableStateOf(false) }
    var retryingIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    val learnLanguage = UserPrefs.targetLanguage
    val nativeLanguage = UserPrefs.nativeLanguage
    LaunchedEffect(wordCount, learnLanguage, nativeLanguage) {
        isLoading = true
        val words = StaticContent.vocab(
            learnCode = learnLanguage,
            nativeCode = nativeLanguage,
            category = PracticeConfig.vocabCategory,
            level = PracticeConfig.vocabLevel,
            n = wordCount
        )
        deck = words
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Strings.tr("Content unavailable"),
                    color = colors.textSecondary,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(dimens.spacingMd))
                PrimaryButton(text = Strings.tr("Go Back"), onClick = { onClose(0, 0) })
            }
        }
        return
    }

    fun finishVocabSession() {
        if (!completionRecorded) {
            UserPrefs.recordPracticeDone(
                feature = "vocabulary",
                score = correctCount,
                total = deck.size,
                learnedMinutes = 6,
                detail = PracticeConfig.vocabCategory,
                level = PracticeConfig.vocabLevel
            )
            completionRecorded = true
        }
        isFinished = true
    }

    fun closeCompletedSession() {
        onFinish(correctCount, deck.size)
    }

    if (isFinished) {
        CongratsDialog(
            subtitle = Strings.tr("Session Completed successfully"),
            buttonText = Strings.tr("Done"),
            onButtonClick = ::closeCompletedSession,
            onClose = ::closeCompletedSession,
            correct = correctCount,
            incorrect = deck.size - correctCount
        )
    }

    val currentWord = deck[currentIndex.coerceIn(0, deck.size - 1)]

    fun retryTranslation() {
        if (retryingIndex != null) return
        val index = currentIndex
        val word = currentWord
        retryingIndex = index
        scope.launch {
            try {
                val item = mapOf(
                    "word" to word.word,
                    "example" to (word.exampleBefore + word.exampleWord + word.exampleAfter)
                )
                val translated = VocabularyTranslations.translate(listOf(item), learnLanguage, nativeLanguage)[0]
                if (translated != null && deck.getOrNull(index) == word) {
                    deck = deck.toMutableList().apply {
                        this[index] = word.copy(meaning = translated.meaning, translation = translated.translation)
                    }
                }
            } finally {
                retryingIndex = null
            }
        }
    }

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
                    speechScore = speechScores[previousIndex] ?: 0
                    showTranslation = false
                } else {
                    onClose(correctCount, deck.size)
                }
            },
            onClose = { onClose(correctCount, deck.size) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimens.spacingMd))

            // Main Flashcard Box
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
                    // Header Row with Audio button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${Strings.tr("Word")} ${currentIndex + 1}/${deck.size}",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        SpeechPlaybackButton(
                            text = currentWord.word,
                            languageTag = Languages.learn(UserPrefs.targetLanguage).speechTag
                        )
                    }

                    Spacer(Modifier.height(dimens.spacingLg))

                    // Target Word
                    Text(
                        text = currentWord.word,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(dimens.spacingSm))

                    // Meaning Definition
                    Text(
                        text = currentWord.meaning.ifBlank { Strings.tr("Translation unavailable") },
                        fontSize = 15.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(dimens.spacingXl))

                    // Example Sentence with Highlighted Target Word
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerMd))
                            .background(colors.surfaceVariant)
                            .padding(dimens.spacingMd)
                    ) {
                        val annotatedExample = buildAnnotatedString {
                            append(currentWord.exampleBefore)
                            withStyle(
                                SpanStyle(
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(currentWord.exampleWord)
                            }
                            append(currentWord.exampleAfter)
                        }

                        Text(
                            text = annotatedExample,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(Modifier.height(dimens.spacingMd))

                    // Toggle Translation Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerMd))
                            .background(colors.surfaceVariant)
                            .clickable { showTranslation = !showTranslation }
                            .padding(dimens.spacingMd),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showTranslation) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentWord.translation.ifBlank { Strings.tr("Translation unavailable") },
                                    fontSize = 14.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                if (currentWord.translation.isBlank()) {
                                    Text(
                                        text = Strings.tr(if (retryingIndex == currentIndex) "Translating…" else "Try Again"),
                                        modifier = Modifier.clickable(enabled = retryingIndex == null) { retryTranslation() }
                                            .padding(top = 8.dp),
                                        color = colors.primary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "👁️ ${Strings.tr("Tap to see translation")}",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(dimens.spacingXl))

            // Speech Pronunciation Evaluation
            SpeechCaptureMic(
                targetText = currentWord.word,
                instruction = Strings.tr("Speak the word to check pronunciation"),
                onResult = { score, _ ->
                    speechScore = score
                    val updatedScores = speechScores + (currentIndex to score)
                    speechScores = updatedScores
                    correctCount = updatedScores.values.count { it >= 60 }
                    UserPrefs.markSpeakingDone(currentWord.word)
                }
            )

                AnimatedVisibility(visible = currentIndex in speechScores) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = dimens.spacingMd)
                    ) {
                        Text(
                            text = "${Strings.tr("Accuracy Score")}: $speechScore%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (speechScore >= 60) colors.success else colors.accentGold
                        )
                        Text(
                            text = if (speechScore >= 80) Strings.tr("Excellent pronunciation!") else if (speechScore >= 60) Strings.tr("Good job!") else Strings.tr("Keep practicing!"),
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }
        }

        // Bottom Advance Buttons
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
                        showTranslation = false
                        speechScore = 0
                        if (currentIndex < deck.size - 1) {
                            currentIndex++
                            speechScore = speechScores[currentIndex] ?: 0
                        } else {
                            finishVocabSession()
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
                    .clickable(enabled = currentIndex in speechScores) {
                        showTranslation = false
                        speechScore = 0
                        if (currentIndex < deck.size - 1) {
                            currentIndex++
                            speechScore = speechScores[currentIndex] ?: 0
                        } else {
                            finishVocabSession()
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
private fun SelectorSection(
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
private fun CountSelectorSection(
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
