package com.example.basekmp.ui.tutor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.example.basekmp.ui.components.AnimatedRobot
import com.example.basekmp.ui.components.RobotFrameAspect
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.ReportIssueDialog
import com.example.basekmp.components.RobotMood
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.components.BackTitleBar
import com.example.basekmp.ui.lifecycle.rememberIsAppResumed
import com.example.basekmp.ui.speaking.PlatformSpeechRecognition
import com.example.basekmp.ui.speaking.rememberPlatformSpeaker
import com.example.basekmp.ui.speaking.rememberTypewriter
import com.example.basekmp.ui.speaking.speechLocaleTagFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AI_TUTOR_GREETING =
    "Hi! I'm your AI English tutor. Ask me anything, or pick a quick action below to get started."

private val AI_TUTOR_CANNED_REPLIES = listOf(
    "Great question! Let's break it down together — could you give me an example sentence?",
    "Nice try! A more natural way to say that would be: \"I have been learning English for two years.\"",
    "Remember: use \"since\" with a point in time and \"for\" with a duration. Want a quick practice question?",
    "Well done! Your sentence is grammatically correct. Let's make it sound even more natural."
)

/**
 * Conversational AI Tutor Screen.
 * 100% migrated from decompiled AiTutorScreenKt.java.
 * Supports quick action chips, bubble timeline, typewriter effects, and unlimited generation bypass.
 */
@Composable
fun AiTutorScreen(
    onBack: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = AI_TUTOR_GREETING,
                fromUser = false,
                animate = true
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf(false) }
    var reportFormMessage by remember { mutableStateOf<String?>(null) }
    var closeReported by remember { mutableStateOf(false) }
    var assistantMood by remember { mutableStateOf(RobotMood.NEUTRAL) }
    val speaker = rememberPlatformSpeaker(UserPrefs.tutorVoice)
    val isAppResumed = rememberIsAppResumed()
    var greetingStarted by remember { mutableStateOf(false) }
    var speakingMessageIndex by remember { mutableStateOf<Int?>(null) }

    val targetLocaleTag = remember(UserPrefs.targetLanguage) {
        speechLocaleTagFor(UserPrefs.targetLanguage)
    }
    val nativeLocaleTag = remember(UserPrefs.nativeLanguage) {
        speechLocaleTagFor(UserPrefs.nativeLanguage)
    }

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
        speaker.warmUp(targetLocaleTag, nativeLocaleTag)
        activateMessageSpeech(messages.lastIndex)
        speaker.speak(
            text = AI_TUTOR_GREETING,
            languageTag = targetLocaleTag,
            voicePreference = UserPrefs.tutorVoice,
            altLanguageTag = nativeLocaleTag
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

    fun closeTutor() {
        stopMessageSpeech()
        if (!closeReported) {
            closeReported = true
            onBack(messages.any { it.fromUser })
        }
    }

    val quickChips = listOf(
        "📷 Spot Your Problem",
        "✍️ Fix My Sentence",
        "🎯 Explain This",
        "🔄 Rephrase Naturally",
        "🎓 Quiz Me",
        "💡 Give Examples",
        "🌐 Translate",
        "🗣️ Roleplay"
    )

    fun sendMessage(userText: String, mode: String? = null) {
        if (userText.isBlank() || isThinking) return

        stopMessageSpeech()

        messages.add(ChatMessage(text = userText, fromUser = true))
        inputText = ""
        connectionError = false
        isThinking = true

        coroutineScope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)

            val reply = TutorAi.reply(
                history = messages.toList(),
                voice = UserPrefs.tutorVoice,
                mode = mode,
                language = com.example.basekmp.data.Languages.learn(UserPrefs.targetLanguage).englishName,
                nativeLanguage = com.example.basekmp.data.Languages.nativeFor(UserPrefs.nativeLanguage).englishName,
                studentName = null,
                withMood = true,
                greetInNative = true
            )

            val cleanReply = if (reply != null) {
                val (mood, text) = TutorAi.splitMood(reply)
                assistantMood = mood ?: RobotMood.NEUTRAL
                text
            } else {
                assistantMood = RobotMood.HAPPY
                AI_TUTOR_CANNED_REPLIES[messages.count { !it.fromUser } % AI_TUTOR_CANNED_REPLIES.size]
            }
            messages.add(ChatMessage(text = cleanReply, fromUser = false, animate = true))
            activateMessageSpeech(messages.lastIndex)
            speaker.speak(
                text = cleanReply,
                languageTag = targetLocaleTag,
                voicePreference = UserPrefs.tutorVoice,
                altLanguageTag = nativeLocaleTag
            )
            isThinking = false
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            Spacer(Modifier.height(8.dp))

            BackTitleBar(
                title = tr("AI Tutor"),
                onBack = ::closeTutor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimens.topBarHeight)
                    .background(AppTheme.colors.surface)
                    .padding(horizontal = AppTheme.dimens.spacingSm),
                rightContent = {
                    IconButton(onClick = {
                        stopMessageSpeech()
                        messages.clear()
                        connectionError = false
                        assistantMood = RobotMood.NEUTRAL
                        messages.add(
                            ChatMessage(
                                text = AI_TUTOR_GREETING,
                                fromUser = false,
                                animate = true
                            )
                        )
                        activateMessageSpeech(messages.lastIndex)
                        speaker.speak(
                            text = AI_TUTOR_GREETING,
                            languageTag = targetLocaleTag,
                            voicePreference = UserPrefs.tutorVoice,
                            altLanguageTag = nativeLocaleTag
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = tr("Clear chat"),
                            tint = AppTheme.colors.textTertiary
                        )
                    }
                }
            )

            // Animated Robot Character Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val robotMood = when {
                    isThinking -> RobotMood.THINKING
                    else -> assistantMood
                }
                val robotH = if (isKeyboardVisible) 64.dp else 150.dp
                AnimatedRobot(
                    modifier = Modifier.size(width = robotH * RobotFrameAspect, height = robotH),
                    talking = speaker.isSpeaking,
                    mood = robotMood,
                    showFingerFx = false
                )
            }

            // Chat Messages Timeline
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimens.spacingLg),
                contentPadding = PaddingValues(vertical = AppTheme.dimens.spacingMd),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingMd)
            ) {
                itemsIndexed(messages) { index, msg ->
                    val isMsgSpeaking = speaker.isSpeaking && speakingMessageIndex == index
                    ChatBubble(
                        message = msg,
                        isSpeaking = isMsgSpeaking,
                        spokenChars = if (speakingMessageIndex == index) speaker.spokenChars else null,
                        onSpeak = {
                            activateMessageSpeech(index)
                            speaker.speak(
                                text = it,
                                languageTag = targetLocaleTag,
                                voicePreference = UserPrefs.tutorVoice,
                                altLanguageTag = nativeLocaleTag
                            )
                        },
                        onStop = {
                            stopMessageSpeech()
                        },
                        onReport = { reportFormMessage = msg.text }
                    )
                }

                if (isThinking) {
                    item {
                        ThinkingBubble()
                    }
                }
            }

            if (connectionError) {
                Text(
                    text = tr("Please check your internet connection and try again."),
                    color = AppTheme.colors.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = AppTheme.dimens.spacingLg)
                )
            }

            // Bottom Input Container with Keyboard & Navigation Bar insets
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (!isKeyboardVisible) {
                    // Keep the conversation visible while the user types; quick actions return on close.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppTheme.dimens.spacingXs)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingSm)
                    ) {
                        Spacer(modifier = Modifier.width(AppTheme.dimens.spacingMd))
                        quickChips.forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(AppTheme.dimens.cornerPill))
                                    .background(AppTheme.colors.surfaceCard)
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(AppTheme.dimens.cornerPill))
                                    .clickable { sendMessage(chip, mode = chip) }
                                    .padding(horizontal = AppTheme.dimens.spacingMd, vertical = AppTheme.dimens.spacingSm)
                            ) {
                                Text(
                                    text = chip,
                                    color = AppTheme.colors.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(AppTheme.dimens.spacingMd))
                    }
                }

                // Bottom Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.surface)
                        .padding(horizontal = AppTheme.dimens.spacingLg, vertical = AppTheme.dimens.spacingSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(AppTheme.dimens.cornerLg))
                            .background(AppTheme.colors.surfaceCard)
                            .padding(horizontal = AppTheme.dimens.spacingLg, vertical = AppTheme.dimens.spacingMd),
                        maxLines = 3,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = AppTheme.colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(AppTheme.colors.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) }),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = tr("Type a message…"),
                                    color = AppTheme.colors.textTertiary,
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(AppTheme.dimens.spacingSm))

                    PlatformSpeechRecognition(
                        languageTag = "en-US",
                        onResult = { transcript ->
                            if (transcript.isNotBlank()) {
                                sendMessage(transcript)
                            }
                        },
                        onError = { }
                    ) { isListening, startListening, stopListening, _ ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank()) AppTheme.colors.primary
                                    else if (isListening) AppTheme.colors.error
                                    else AppTheme.colors.primary
                                )
                                .clickable(enabled = !isThinking) {
                                    if (inputText.isNotBlank()) {
                                        sendMessage(inputText)
                                    } else if (isListening) {
                                        stopListening()
                                    } else {
                                        stopMessageSpeech()
                                        startListening()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (inputText.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = tr("Send"),
                                    tint = AppTheme.colors.textOnPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = tr("Voice Input"),
                                    tint = AppTheme.colors.textOnPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (reportFormMessage != null) {
        ReportIssueDialog(
            place = tr("AI Tutor"),
            onDismiss = { reportFormMessage = null },
            // This build has no report submission service; opening the in-app form is the available action.
            onSubmit = { _, _ -> }
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean = false,
    spokenChars: Int? = null,
    onSpeak: (String) -> Unit = {},
    onStop: () -> Unit = {},
    onReport: () -> Unit = {}
) {
    val isUser = message.fromUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000L)
            copied = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 5.dp,
                        bottomEnd = if (isUser) 5.dp else 18.dp
                    )
                )
                .background(if (isUser) Color(0xFFE8F0FE) else Color(0xFFF7F9FC))
                .border(
                    width = 1.dp,
                    color = if (isUser) Color(0xFFD0E2FF) else Color(0xFFEAECF0),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 5.dp,
                        bottomEnd = if (isUser) 5.dp else 18.dp
                    )
                )
                .padding(horizontal = 15.dp, vertical = 12.dp)
        ) {
            Column {
                val displayedText = rememberTypewriter(
                    text = message.text,
                    enabled = message.animate && !isUser,
                    spokenChars = spokenChars
                )
                Text(
                    text = displayedText,
                    color = Color(0xFF1D2939),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start
                )

                Spacer(Modifier.height(8.dp))

                if (isUser) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(message.text))
                                copied = true
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = "Copy",
                            tint = if (copied) Color(0xFF2B5CFF) else Color(0xFF1D2939).copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                        if (copied) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tr("Copied"),
                                color = Color(0xFF2B5CFF),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Audio Stop / Speak Button
                        if (isSpeaking) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B5CFF))
                                    .clickable { onStop() },
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
                                    .clickable { onSpeak(message.text) }
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
                                    clipboardManager.setText(AnnotatedString(message.text))
                                    copied = true
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (copied) Color(0xFF2B5CFF) else Color(0xFF1D2939).copy(alpha = 0.55f),
                                modifier = Modifier.size(16.dp)
                            )
                            if (copied) {
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
                        IconButton(onClick = onReport) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = tr("Report"),
                                tint = Color(0xFF1D2939).copy(alpha = 0.55f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypewriterText(
    fullText: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    var displayedChars by remember { mutableStateOf(0) }

    LaunchedEffect(fullText) {
        displayedChars = 0
        while (displayedChars < fullText.length) {
            displayedChars = minOf(displayedChars + 2, fullText.length)
            delay(20)
        }
    }

    Text(
        text = fullText.take(displayedChars),
        color = color,
        fontSize = fontSize,
        lineHeight = 20.sp
    )
}

@Composable
private fun ThinkingBubble() {
    val isAppResumed = rememberIsAppResumed()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surfaceCard)
            .border(1.dp, AppTheme.colors.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val anim = remember { Animatable(0f) }
            LaunchedEffect(isAppResumed) {
                if (!isAppResumed) return@LaunchedEffect
                delay(index * 150L)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.primary.copy(alpha = 0.3f + 0.7f * anim.value))
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = tr("Thinking…"),
            color = AppTheme.colors.textSecondary,
            fontSize = 13.sp
        )
    }
}
