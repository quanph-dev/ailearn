package com.example.basekmp.ui.tutor

import com.example.basekmp.AppAdsHost
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.RobotMood
import com.example.basekmp.components.AppActionDialog
import com.example.basekmp.components.ReportIssueDialog
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.data.trf
import com.example.basekmp.theme.BrandAccent
import com.example.basekmp.theme.BrandDarkElevated
import com.example.basekmp.theme.BrandDarkSurface
import com.example.basekmp.theme.BrandPrimary
import com.example.basekmp.theme.BubbleAi
import com.example.basekmp.theme.BubbleUser
import com.example.basekmp.theme.DividerColor
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.components.AnimatedRobot
import com.example.basekmp.ui.components.RobotFrameAspect
import com.example.basekmp.ui.lifecycle.rememberIsAppResumed
import com.example.basekmp.ui.plan.PlanFlow
import com.example.basekmp.ui.speaking.PlatformSpeechRecognition
import com.example.basekmp.ui.speaking.rememberPlatformSpeaker
import com.example.basekmp.ui.speaking.rememberTypewriter
import com.example.basekmp.ui.speaking.speechLocaleTagFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal data class TimedLessonCompletionState(
    val remainingSeconds: Int,
    val isResumed: Boolean,
    val responsePending: Boolean = false,
    val speechPending: Boolean = false,
    val playbackPending: Boolean = false,
    val dialogVisible: Boolean = false,
    val completing: Boolean = false,
    val editing: Boolean = false
) {
    val canComplete: Boolean get() = remainingSeconds == 0 && isResumed &&
        !responsePending && !speechPending && !playbackPending && !dialogVisible && !completing && !editing
}

private suspend fun LazyListState.scrollToConversationEnd(index: Int, animate: Boolean) {
    if (index !in 0 until layoutInfo.totalItemsCount) return
    var item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        scrollToItem(index)
        item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    val offset = item?.size ?: 0
    if (animate) animateScrollToItem(index, offset) else scrollToItem(index, offset)
}

/** Interactive Today's Lesson conversation with voice/text input, hints, and a countdown. */
@Composable
fun TodayLessonScreen(
    title: String? = null,
    level: String? = null,
    day: Int? = null,
    titleLanguage: String? = null,
    loadedDayPlan: PlanFlow.DayPlan? = null,
    systemBackRequest: Int = 0,
    adsHost: AppAdsHost? = null,
    onBack: (Boolean) -> Unit,
    onFinish: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val isAppResumed = rememberIsAppResumed()
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val lessonTitle = title ?: "Greeting Someone New"
    val lessonLevel = level ?: "Beginner"

    val speeds = remember { listOf("1x", "1.5x", "2x") }
    var speedIndex by remember { mutableIntStateOf(0) }

    var currentMood by remember { mutableStateOf(RobotMood.HAPPY) }
    var hintText by remember { mutableStateOf<String?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    var isHintLoading by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var textMode by remember { mutableStateOf(false) }

    var remainingSeconds by remember { mutableIntStateOf(5 * 60) } // 5 minutes
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showCompletionError by remember { mutableStateOf(false) }
    var dayCompleted by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isCompleting by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var speechInteractionBusy by remember { mutableStateOf(false) }
    var copiedMessageIndex by remember { mutableStateOf<Int?>(null) }

    val speaker = rememberPlatformSpeaker(UserPrefs.tutorVoice)
    var speakingMessageIndex by remember { mutableStateOf<Int?>(null) }

    val targetLocaleTag = remember(UserPrefs.targetLanguage) {
        speechLocaleTagFor(UserPrefs.targetLanguage)
    }
    val nativeLocaleTag = remember(UserPrefs.nativeLanguage) {
        speechLocaleTagFor(UserPrefs.nativeLanguage)
    }

    fun currentSpeedFactor(): Float = when (speeds[speedIndex]) {
        "1.5x" -> 1.5f
        "2x" -> 2.0f
        else -> 1.0f
    }

    val openingGreeting = remember(lessonTitle) {
        "Hello! Ready for today's five-minute lesson on \"$lessonTitle\"? Tap the mic and tell me what you'd say first!"
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = openingGreeting,
                fromUser = false,
                animate = true
            )
        )
    }
    var greetingStarted by remember { mutableStateOf(false) }

    fun stopMessageSpeech() {
        val activeIndex = speakingMessageIndex
        speaker.stop()
        speakingMessageIndex = null
        if (activeIndex != null && activeIndex in messages.indices && messages[activeIndex].animate) {
            messages[activeIndex] = messages[activeIndex].copy(animate = false)
        }
    }

    fun activateMessageSpeech(index: Int) {
        val previousIndex = speakingMessageIndex
        if (previousIndex != null && previousIndex != index &&
            previousIndex in messages.indices && messages[previousIndex].animate
        ) {
            messages[previousIndex] = messages[previousIndex].copy(animate = false)
        }
        speakingMessageIndex = index
    }

    DisposableEffect(Unit) {
        onDispose {
            speaker.stop()
        }
    }

    // Warm up TTS locales & auto-speak the opening greeting message immediately on screen entry
    LaunchedEffect(isAppResumed) {
        if (!isAppResumed || greetingStarted) return@LaunchedEffect
        greetingStarted = true
        speaker.warmUp("en-US", targetLocaleTag, nativeLocaleTag, titleLanguage ?: "en-US")
        activateMessageSpeech(messages.lastIndex)
        speaker.speak(
            text = openingGreeting,
            languageTag = "en-US",
            voicePreference = UserPrefs.tutorVoice,
            speed = currentSpeedFactor(),
            altLanguageTag = nativeLocaleTag,
            quotedLanguageTag = titleLanguage
        )
    }

    // Clear the active message once speech and its typewriter reveal finish.
    LaunchedEffect(messages.size, speaker.isSpeaking, speakingMessageIndex, isAppResumed) {
        if (!isAppResumed) return@LaunchedEffect
        val finishedIndex = speakingMessageIndex
        if (!speaker.isSpeaking && finishedIndex != null) {
            if (finishedIndex in messages.indices && !messages[finishedIndex].fromUser && messages[finishedIndex].animate) {
                val len = messages[finishedIndex].text.length
                if (speaker.spokenChars < len) {
                    delay((len * 18L).coerceAtMost(1500L))
                }
                if (speakingMessageIndex == finishedIndex && !speaker.isSpeaking &&
                    finishedIndex in messages.indices && messages[finishedIndex].animate
                ) {
                    messages[finishedIndex] = messages[finishedIndex].copy(animate = false)
                }
            }
            if (speakingMessageIndex == finishedIndex) speakingMessageIndex = null
        }
    }

    val initialSystemBackRequest = remember { systemBackRequest }
    LaunchedEffect(systemBackRequest) {
        if (systemBackRequest != initialSystemBackRequest && !isCompleting && !showCompletionDialog) {
            stopMessageSpeech()
            if (messages.any { it.fromUser }) {
                showExitDialog = true
            } else {
                onBack(false)
            }
        }
    }

    // Timer countdown
    LaunchedEffect(isAppResumed) {
        while (isAppResumed && remainingSeconds > 0) {
            delay(1000L)
            if (!isAppResumed) return@LaunchedEffect
            remainingSeconds--
        }
    }

    // Scroll to the actual last row, including the thinking row.
    LaunchedEffect(messages.size, isThinking, isAppResumed) {
        if (!isAppResumed) return@LaunchedEffect
        val thinkingItemCount = if (isThinking) 1 else 0
        val targetItemIndex = messages.lastIndex + thinkingItemCount
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { itemCount -> itemCount > targetItemIndex }
        listState.scrollToConversationEnd(targetItemIndex, animate = true)
    }

    // The newest assistant bubble grows as TTS/typewriter text is revealed. Follow its
    // measured height so newly revealed lines stay visible without scrolling each frame.
    LaunchedEffect(messages.size, isAppResumed) {
        if (!isAppResumed) return@LaunchedEffect
        val latestMessage = messages.lastOrNull() ?: return@LaunchedEffect
        if (latestMessage.fromUser) return@LaunchedEffect

        val itemIndex = messages.lastIndex
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { item -> item.index == itemIndex }
                ?.size
        }
            .distinctUntilChanged()
            .filterNotNull()
            .collect {
                listState.scrollToConversationEnd(itemIndex, animate = true)
            }
    }

    // Follow the newest message as the keyboard changes the available chat height.
    LaunchedEffect(isKeyboardVisible, messages.size, isThinking) {
        if (!isKeyboardVisible) return@LaunchedEffect
        val target = messages.lastIndex + if (isThinking) 1 else 0
        snapshotFlow { listState.layoutInfo.viewportSize.height }
            .filter { it > 0 }
            .distinctUntilChanged()
            .collectLatest { listState.scrollToConversationEnd(target, animate = false) }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || isThinking || isCompleting || showCompletionDialog) return

        stopMessageSpeech()

        messages.add(ChatMessage(text = userText, fromUser = true))
        inputText = ""
        hintText = null
        isThinking = true

        coroutineScope.launch {
            val rawReply = TutorAi.reply(
                history = messages.toList(),
                voice = UserPrefs.tutorVoice,
                language = com.example.basekmp.data.Languages.learn(UserPrefs.targetLanguage).englishName,
                nativeLanguage = com.example.basekmp.data.Languages.nativeFor(UserPrefs.nativeLanguage).englishName,
                lessonTitle = lessonTitle,
                lessonLevel = lessonLevel,
                replyInNative = false,
                withMood = true
            )
            if (isCompleting || showCompletionDialog) {
                isThinking = false
                return@launch
            }

            val cleanReply = if (rawReply != null) {
                val (mood, text) = TutorAi.splitMood(rawReply)
                if (mood != null) {
                    currentMood = mood
                }
                text
            } else {
                "Good effort! Say a full sentence in English and I'll help you make it sound natural."
            }
            messages.add(ChatMessage(text = cleanReply, fromUser = false, animate = true))
            activateMessageSpeech(messages.lastIndex)
            speaker.speak(
                text = cleanReply,
                languageTag = targetLocaleTag,
                voicePreference = UserPrefs.tutorVoice,
                speed = currentSpeedFactor(),
                altLanguageTag = nativeLocaleTag
            )
            isThinking = false
        }
    }

    fun requestHint() {
        if (isHintLoading || isThinking || isCompleting || showCompletionDialog || remainingSeconds <= 0) return
        isHintLoading = true
        coroutineScope.launch {
            val hint = TutorAi.hint(
                history = messages,
                language = "English",
                lessonTitle = lessonTitle,
                lessonLevel = lessonLevel
            )
            hintText = hint ?: "Try asking: \"How's your day going so far?\""
            isHintLoading = false
        }
    }

    fun completeLesson() {
        if (isCompleting || showCompletionDialog) return
        stopMessageSpeech()
        isThinking = false
        showExitDialog = false
        showReportDialog = false
        isCompleting = true
        val activityEpochDay = UserPrefs.todayEpochDay()
        coroutineScope.launch {
            try {
                val result = day?.let { PlanFlow.finish(it, PlanProgress.Activity.TALK, activityEpochDay, loadedDayPlan) }
                if (day != null && PlanProgress.Activity.TALK !in PlanProgress.get().done(day)) {
                    showCompletionError = true
                    return@launch
                }
                dayCompleted = result?.dayCompleted == true
                val minutesSpent = ((5 * 60 - remainingSeconds) / 60).coerceAtLeast(1)
                UserPrefs.addLearnedMinutes(minutesSpent, activityEpochDay)
                UserPrefs.addLearning(
                    title = "Today's Lesson",
                    detail = lessonTitle,
                    total = 0,
                    level = lessonLevel
                )
                showCompletionDialog = true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                showCompletionError = true
            } finally {
                isCompleting = false
            }
        }
    }

    PlatformSpeechRecognition(
        languageTag = "en-US",
        onResult = { recognizedText ->
            if (recognizedText.isNotBlank()) {
                sendMessage(recognizedText)
            }
        },
        onError = { /* ignore or soft fail */ },
        onInteractionStateChange = { speechInteractionBusy = it }
    ) { isListening, startListening, stopListening, _ ->

        val completionState = TimedLessonCompletionState(
            remainingSeconds = remainingSeconds,
            isResumed = isAppResumed,
            responsePending = isThinking || isHintLoading,
            speechPending = isListening || speechInteractionBusy,
            playbackPending = speaker.isSpeaking || speakingMessageIndex != null,
            dialogVisible = showExitDialog || showReportDialog || showCompletionDialog || showCompletionError,
            completing = isCompleting,
            editing = isKeyboardVisible || inputText.isNotBlank()
        )
        LaunchedEffect(completionState) {
            if (completionState.canComplete) completeLesson()
        }

        Surface(
            modifier = modifier.fillMaxSize(),
            color = BrandDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandDarkSurface)
                    .imePadding()
            ) {
                // 1. Hero Section at top: Dark gradient + Animated Robot + Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isKeyboardVisible) 110.dp else 240.dp)
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(BrandDarkSurface, BrandDarkElevated),
                                    start = Offset.Zero,
                                    end = Offset(size.width, size.height)
                                )
                            )
                            val center = Offset(size.width / 2f, size.height * 0.42f)
                            val radius = size.height * 0.78f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x8C2366FC), Color(0x002076FC)),
                                    center = center,
                                    radius = radius
                                ),
                                radius = radius,
                                center = center
                            )
                        }
                ) {
                    // 3D Humanoid Robot Avatar
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val robotH = maxHeight * 0.96f
                        AnimatedRobot(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = maxHeight * 0.059f)
                                .size(width = robotH * RobotFrameAspect, height = robotH),
                            talking = speaker.isSpeaking,
                            mood = currentMood,
                            showFingerFx = false
                        )
                    }

                    // Top Bar Row with Close and Speed buttons (Clear of notch & status bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close button (40.dp circle, black alpha 0.35f, 18.dp white Close icon)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .clickable(enabled = !isCompleting) {
                                    stopMessageSpeech()
                                    if (messages.any { it.fromUser }) {
                                        showExitDialog = true
                                    } else {
                                        onBack(false)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = tr("End lesson"),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // Speed button (40.dp circle, black alpha 0.35f, 12.sp white semi-bold text)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .clickable(enabled = !speaker.isSpeaking) {
                                    speedIndex = (speedIndex + 1) % speeds.size
                                    val factor = when (speeds[speedIndex]) {
                                        "1.5x" -> 1.5f
                                        "2x" -> 2.0f
                                        else -> 1.0f
                                    }
                                    speaker.setSpeed(factor)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = speeds[speedIndex],
                                color = if (speaker.isSpeaking) Color.White.copy(alpha = 0.4f) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Countdown Timer Pill at bottom-start of hero
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    val timerStr = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

                    Box(
                        modifier = Modifier
                            .align(if (isKeyboardVisible) Alignment.BottomCenter else Alignment.BottomStart)
                            .padding(start = if (isKeyboardVisible) 0.dp else 20.dp, bottom = if (isKeyboardVisible) 2.dp else 14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = timerStr,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 2. Chat Timeline in rounded white container
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Message Bubbles
                        itemsIndexed(messages) { index, msg ->
                            val isCopied = copiedMessageIndex == index

                            if (msg.fromUser) {
                                // User Bubble (Right Aligned)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Spacer(Modifier.weight(0.18f))
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 300.dp)
                                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp))
                                            .background(BubbleUser)
                                            .padding(horizontal = 15.dp, vertical = 12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = msg.text,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.End)
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        clipboardManager.setText(AnnotatedString(msg.text))
                                                        copiedMessageIndex = index
                                                    }
                                                    .padding(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                                    contentDescription = tr("Copy"),
                                                    tint = if (isCopied) BrandPrimary else TextPrimary.copy(alpha = 0.55f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                if (isCopied) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = tr("Copied"),
                                                        color = BrandPrimary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // AI Bubble (Left Aligned)
                                val isThisPlaying = speakingMessageIndex == index && speaker.isSpeaking
                                val displayedText = rememberTypewriter(
                                    text = msg.text,
                                    enabled = msg.animate,
                                    spokenChars = if (speakingMessageIndex == index) speaker.spokenChars else null
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 300.dp)
                                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp))
                                            .background(BubbleAi)
                                            .border(1.dp, DividerColor, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp))
                                            .padding(horizontal = 15.dp, vertical = 12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = displayedText,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Audio Stop / Speak Button
                                                if (isThisPlaying) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(BrandPrimary)
                                                            .clickable {
                                                                stopMessageSpeech()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Stop,
                                                            contentDescription = tr("Stop"),
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .clickable {
                                                                activateMessageSpeech(index)
                                                                speaker.speak(
                                                                    text = msg.text,
                                                                    languageTag = if (index == 0) "en-US" else targetLocaleTag,
                                                                    voicePreference = UserPrefs.tutorVoice,
                                                                    speed = currentSpeedFactor(),
                                                                    altLanguageTag = nativeLocaleTag,
                                                                    quotedLanguageTag = if (index == 0) titleLanguage else null
                                                                )
                                                            }
                                                            .padding(4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                            contentDescription = tr("Listen"),
                                                            tint = TextPrimary.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                // Copy Button
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .clickable {
                                                            clipboardManager.setText(AnnotatedString(msg.text))
                                                            copiedMessageIndex = index
                                                        }
                                                        .padding(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                                        contentDescription = tr("Copy"),
                                                        tint = if (isCopied) BrandPrimary else TextPrimary.copy(alpha = 0.55f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    if (isCopied) {
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                            text = tr("Copied"),
                                                            color = BrandPrimary,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.weight(1f))

                                                // Report Flag Icon
                                                IconButton(onClick = { showReportDialog = true }) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Flag,
                                                        contentDescription = tr("Report"),
                                                        tint = TextPrimary.copy(alpha = 0.55f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(0.18f))
                                }
                            }
                        }

                        // Thinking indicator
                        if (isThinking) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(BubbleAi)
                                            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "...",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hint Banner (if requested)
                    AnimatedVisibility(
                        visible = hintText != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        hintText?.let { hint ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF9E6))
                                    .border(1.dp, Color(0xFFFFE58F), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lightbulb,
                                        contentDescription = tr("Hint"),
                                        tint = Color(0xFFFAAD14),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = hint,
                                        fontSize = 13.sp,
                                        color = Color(0xFF874D00),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = tr("Close"),
                                        tint = Color(0xFF874D00).copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { hintText = null }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Bottom Controls Bar & Banner Ad
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .navigationBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        if (textMode) {
                            // Typing Mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF2F4F7))
                                        .clickable { textMode = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = tr("Close keyboard"),
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF2F4F7))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = tr("Type a message..."),
                                            color = Color(0xFF98A2B3),
                                            fontSize = 14.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = {
                                            sendMessage(inputText)
                                        }),
                                        cursorBrush = SolidColor(BrandPrimary),
                                        singleLine = true
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (inputText.isNotBlank()) BrandPrimary else Color(0xFFE4E7EC))
                                        .clickable(enabled = inputText.isNotBlank() && !isThinking) {
                                            sendMessage(inputText)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = tr("Send"),
                                        tint = if (inputText.isNotBlank()) Color.White else Color(0xFF98A2B3),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            // Non-typing Voice & Hint Mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Hint button
                                Row(
                                    modifier = Modifier
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(Color(0xFFF2F4F7))
                                        .clickable(enabled = !isHintLoading && !isThinking) {
                                            requestHint()
                                        }
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lightbulb,
                                        contentDescription = tr("Hint"),
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = tr("Hint"),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Large Voice Mic Pill Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(if (isListening) Color(0xFFE53935) else BrandPrimary)
                                        .clickable(enabled = isListening || remainingSeconds > 0) {
                                            if (isListening) {
                                                stopListening()
                                            } else {
                                                stopMessageSpeech()
                                                startListening()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Mic,
                                            contentDescription = "Mic",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (isListening) {
                                            Text(
                                                text = tr("Listening..."),
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Keyboard Toggle Button
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF2F4F7))
                                        .clickable(enabled = remainingSeconds > 0) { textMode = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Keyboard,
                                        contentDescription = "Keyboard",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                    adsHost?.BannerAd(
                        "B_Speak",
                        modifier = Modifier.fillMaxWidth(),
                        visible = !isKeyboardVisible
                    )
                }
            }
        }

            // Exit Dialog
            if (showExitDialog) {
                AppActionDialog(
                    title = tr("Leave lesson?"),
                    message = tr("Your progress in this session will be saved."),
                    onDismissRequest = { showExitDialog = false },
                    confirmText = tr("Complete"),
                    onConfirm = {
                        showExitDialog = false
                        completeLesson()
                    },
                    dismissText = tr("Exit"),
                    onDismissAction = {
                        showExitDialog = false
                        stopMessageSpeech()
                        onBack(false)
                    }
                )
            }

            if (showCompletionError) {
                AppActionDialog(
                    title = tr("Could not complete lesson"),
                    message = tr("Please try again."),
                    onDismissRequest = { showCompletionError = false; onBack(false) },
                    confirmText = tr("Try Again"),
                    onConfirm = { showCompletionError = false; completeLesson() },
                    dismissText = tr("Go Back"),
                    onDismissAction = { showCompletionError = false; onBack(false) }
                )
            }

            // Completion Dialog
            if (showCompletionDialog) {
                AppActionDialog(
                    title = if (dayCompleted && day != null) trf("Day %1\$d complete! 🎉", day) else tr("Lesson Complete!"),
                    message = tr("Great job completing today's English speaking lesson!"),
                    onDismissRequest = {
                        showCompletionDialog = false
                        onFinish?.invoke() ?: onBack(true)
                    },
                    confirmText = tr("Awesome"),
                    onConfirm = {
                        showCompletionDialog = false
                        onFinish?.invoke() ?: onBack(true)
                    }
                )
            }

            // Report Dialog
            if (showReportDialog) {
                ReportIssueDialog(
                    place = tr("AI Tutor"),
                    onDismiss = { showReportDialog = false },
                    // The report form is local only; no review service is configured in this build.
                    onSubmit = { _, _ -> }
                )
            }
        }
    }
}
