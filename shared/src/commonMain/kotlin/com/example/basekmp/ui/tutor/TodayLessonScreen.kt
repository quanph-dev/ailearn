package com.example.basekmp.ui.tutor

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.RobotMood
import com.example.basekmp.currentTimeMillis
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.ui.components.AnimatedRobot
import com.example.basekmp.ui.components.RobotFrameAspect
import com.example.basekmp.ui.plan.PlanFlow
import com.example.basekmp.ui.speaking.PlatformSpeechRecognition
import com.example.basekmp.ui.speaking.rememberPlatformSpeaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Interactive Today's Lesson conversation with voice/text input, hints, and a countdown. */
@Composable
fun TodayLessonScreen(
    title: String? = null,
    level: String? = null,
    day: Int? = null,
    systemBackRequest: Int = 0,
    onBack: (Boolean) -> Unit,
    onFinish: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val sessionStartedAt = remember { currentTimeMillis() }

    val lessonTitle = title ?: "Greeting Someone New"
    val lessonLevel = level ?: "Beginner"

    val speeds = remember { listOf("1x", "1.5x", "2x") }
    var speedIndex by remember { mutableIntStateOf(0) }

    var currentMood by remember { mutableStateOf(RobotMood.HAPPY) }
    var brief by remember { mutableStateOf<TutorAi.ScenarioBrief?>(null) }
    var hintText by remember { mutableStateOf<String?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    var isHintLoading by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var textMode by remember { mutableStateOf(false) }

    var remainingSeconds by remember { mutableIntStateOf(5 * 60) } // 5 minutes
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isCompleting by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var copiedMessageIndex by remember { mutableStateOf<Int?>(null) }

    val speaker = rememberPlatformSpeaker()
    var speakingMessageText by remember { mutableStateOf<String?>(null) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Hello! Ready for today's five-minute lesson on \"$lessonTitle\"? Tap the mic and tell me what you'd say first!",
                fromUser = false,
                animate = false
            )
        )
    }

    val initialSystemBackRequest = remember { systemBackRequest }
    LaunchedEffect(systemBackRequest) {
        if (systemBackRequest != initialSystemBackRequest && !isCompleting && !showCompletionDialog) {
            if (messages.any { it.fromUser }) {
                showExitDialog = true
            } else {
                speaker.stop()
                onBack(false)
            }
        }
    }

    // Timer countdown
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }

    // Auto-scroll when messages update
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            delay(50)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Load scenario brief
    LaunchedEffect(lessonTitle, lessonLevel) {
        brief = TutorAi.scenarioBrief(
            title = lessonTitle,
            level = lessonLevel,
            language = "English",
            nativeLanguage = UserPrefs.nativeLanguage
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || isThinking) return

        messages.add(ChatMessage(text = userText, fromUser = true))
        inputText = ""
        hintText = null
        isThinking = true

        coroutineScope.launch {
            val rawReply = TutorAi.reply(
                history = messages,
                voice = UserPrefs.tutorVoice,
                language = "English",
                nativeLanguage = UserPrefs.nativeLanguage,
                lessonTitle = lessonTitle,
                lessonLevel = lessonLevel,
                replyInNative = false,
                withMood = true
            )

            if (rawReply != null) {
                val (mood, cleanReply) = TutorAi.splitMood(rawReply)
                if (mood != null) {
                    currentMood = mood
                }
                messages.add(ChatMessage(text = cleanReply, fromUser = false, animate = true))
                speakingMessageText = cleanReply
                speaker.speak(cleanReply, "en-US", UserPrefs.tutorVoice)
            } else {
                messages.add(
                    ChatMessage(
                        text = tr("Couldn't get a reply. Please try again."),
                        fromUser = false,
                        animate = true
                    )
                )
            }
            isThinking = false
        }
    }

    fun requestHint() {
        if (isHintLoading || isThinking) return
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

    fun completeLesson(showDialog: Boolean = true) {
        if (isCompleting || showCompletionDialog) return
        isCompleting = true
        coroutineScope.launch {
            try {
                day?.let { PlanFlow.finish(it, PlanProgress.Activity.TALK) }
                val minutesSpent = ((currentTimeMillis() - sessionStartedAt) / 60_000L).toInt().coerceAtLeast(1)
                UserPrefs.addLearnedMinutes(minutesSpent)
                UserPrefs.addLearning(
                    title = "Today's Lesson",
                    detail = lessonTitle,
                    total = 0,
                    level = lessonLevel
                )
                UserPrefs.recordStreakDay()

                if (showDialog) {
                    showCompletionDialog = true
                } else {
                    onFinish?.invoke() ?: onBack(true)
                }
            } finally {
                isCompleting = false
            }
        }
    }

    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds == 0) {
            completeLesson(showDialog = false)
        }
    }

    PlatformSpeechRecognition(
        languageTag = "en-US",
        onResult = { recognizedText ->
            if (recognizedText.isNotBlank()) {
                sendMessage(recognizedText)
            }
        },
        onError = { /* ignore or soft fail */ }
    ) { isListening, startListening, stopListening, _ ->

        Surface(
            modifier = modifier.fillMaxSize(),
            color = Color(0xFF001438)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF001438))
                    .imePadding()
            ) {
                // 1. Hero Section at top: Dark gradient + Animated Robot + Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF001438), Color(0xFF001E52)),
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
                                    if (messages.any { it.fromUser }) {
                                        showExitDialog = true
                                    } else {
                                        speaker.stop()
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
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, bottom = 14.dp)
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
                        // Scenario Info Card
                        brief?.let { b ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF7F9FC))
                                        .border(1.dp, Color(0xFFEAECF0), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = lessonTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1D2939)
                                        )
                                        Text(
                                            text = b.description,
                                            fontSize = 13.sp,
                                            color = Color(0xFF475467),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

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
                                            .background(Color(0xFFE8F0FE))
                                            .padding(horizontal = 15.dp, vertical = 12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = msg.text,
                                                color = Color(0xFF1D2939),
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
                                                    contentDescription = "Copy",
                                                    tint = if (isCopied) Color(0xFF2B5CFF) else Color(0xFF1D2939).copy(alpha = 0.55f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                if (isCopied) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = tr("Copied"),
                                                        color = Color(0xFF2B5CFF),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // AI Bubble (Left Aligned)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 300.dp)
                                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp))
                                            .background(Color(0xFFF7F9FC))
                                            .border(1.dp, Color(0xFFEAECF0), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp))
                                            .padding(horizontal = 15.dp, vertical = 12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = msg.text,
                                                color = Color(0xFF1D2939),
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Audio Stop / Speak Button
                                                val isThisPlaying = speakingMessageText == msg.text && speaker.isSpeaking
                                                if (isThisPlaying) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF2B5CFF))
                                                            .clickable {
                                                                speaker.stop()
                                                                speakingMessageText = null
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
                                                                speakingMessageText = msg.text
                                                                speaker.speak(msg.text, "en-US", UserPrefs.tutorVoice)
                                                            }
                                                            .padding(4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                            contentDescription = tr("Listen"),
                                                            tint = Color(0xFF1D2939).copy(alpha = 0.7f),
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
                                                        contentDescription = "Copy",
                                                        tint = if (isCopied) Color(0xFF2B5CFF) else Color(0xFF1D2939).copy(alpha = 0.55f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    if (isCopied) {
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                            text = tr("Copied"),
                                                            color = Color(0xFF2B5CFF),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.weight(1f))

                                                // Report Flag Icon
                                                Icon(
                                                    imageVector = Icons.Rounded.Flag,
                                                    contentDescription = tr("Report"),
                                                    tint = Color(0xFF1D2939).copy(alpha = 0.4f),
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable { showReportDialog = true }
                                                )
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
                                            .background(Color(0xFFF7F9FC))
                                            .border(1.dp, Color(0xFFEAECF0), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "...",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2B5CFF)
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
                                        contentDescription = "Hint",
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
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF874D00).copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { hintText = null }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Bottom Controls Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .navigationBarsPadding()
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
                                        contentDescription = "Close keyboard",
                                        tint = Color(0xFF1D2939),
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
                                            color = Color(0xFF1D2939),
                                            fontSize = 14.sp
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = {
                                            sendMessage(inputText)
                                        }),
                                        cursorBrush = SolidColor(Color(0xFF2B5CFF)),
                                        singleLine = true
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (inputText.isNotBlank()) Color(0xFF2B5CFF) else Color(0xFFE4E7EC))
                                        .clickable(enabled = inputText.isNotBlank() && !isThinking) {
                                            sendMessage(inputText)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Send",
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
                                        contentDescription = "Hint",
                                        tint = Color(0xFF1D2939),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = tr("Hint"),
                                        color = Color(0xFF1D2939),
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
                                        .background(if (isListening) Color(0xFFE53935) else Color(0xFF2B5CFF))
                                        .clickable {
                                            if (isListening) {
                                                stopListening()
                                            } else {
                                                speaker.stop()
                                                speakingMessageText = null
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
                                        .clickable { textMode = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Keyboard,
                                        contentDescription = "Keyboard",
                                        tint = Color(0xFF1D2939),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Exit Dialog
            if (showExitDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text(tr("Leave lesson?")) },
                    text = { Text(tr("Your progress in this session will be saved.")) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitDialog = false
                                speaker.stop()
                                completeLesson()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B5CFF))
                        ) {
                            Text(tr("Complete"), color = Color.White)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showExitDialog = false
                                speaker.stop()
                                onBack(false)
                            }
                        ) {
                            Text(tr("Exit"), color = Color(0xFF667085))
                        }
                    }
                )
            }

            // Completion Dialog
            if (showCompletionDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {
                        showCompletionDialog = false
                        onFinish?.invoke() ?: onBack(true)
                    },
                    title = { Text(tr("Lesson Complete!")) },
                    text = { Text(tr("Great job completing today's English speaking lesson!")) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCompletionDialog = false
                                onFinish?.invoke() ?: onBack(true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B5CFF))
                        ) {
                            Text(tr("Awesome"), color = Color.White)
                        }
                    }
                )
            }

            // Report Dialog
            if (showReportDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showReportDialog = false },
                    title = { Text(tr("Report an issue")) },
                    text = { Text(tr("Thank you for your feedback. We will review this conversation response.")) },
                    confirmButton = {
                        Button(
                            onClick = { showReportDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B5CFF))
                        ) {
                            Text(tr("OK"), color = Color.White)
                        }
                    }
                )
            }
        }
    }
}
