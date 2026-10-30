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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.AnnotatedString
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
import com.example.basekmp.components.RobotMood
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.components.BackTitleBar
import com.example.basekmp.ui.speaking.PlatformSpeechRecognition
import com.example.basekmp.ui.speaking.rememberPlatformSpeaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AI_TUTOR_GREETING =
    "Hi! I'm your AI English tutor. Ask me anything, or pick a quick action below to get started."

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

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = AI_TUTOR_GREETING,
                fromUser = false,
                animate = false
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf(false) }
    var closeReported by remember { mutableStateOf(false) }
    var assistantMood by remember { mutableStateOf(RobotMood.NEUTRAL) }
    val speaker = rememberPlatformSpeaker()
    var speakingMessageText by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            speaker.stop()
        }
    }

    fun closeTutor() {
        speaker.stop()
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

        messages.add(ChatMessage(text = userText, fromUser = true))
        inputText = ""
        connectionError = false
        isThinking = true

        coroutineScope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)

            val reply = TutorAi.reply(
                history = messages,
                voice = UserPrefs.tutorVoice,
                mode = mode,
                language = "English",
                nativeLanguage = UserPrefs.nativeLanguage,
                studentName = null,
                withMood = true
            )

            if (reply == null) {
                connectionError = true
            } else {
                val (mood, cleanReply) = TutorAi.splitMood(reply)
                assistantMood = mood ?: RobotMood.NEUTRAL
                messages.add(ChatMessage(text = cleanReply, fromUser = false, animate = true))
                speakingMessageText = cleanReply
                speaker.speak(cleanReply, "en-US", UserPrefs.tutorVoice)
            }
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
                        speaker.stop()
                        speakingMessageText = null
                        messages.clear()
                        connectionError = false
                        assistantMood = RobotMood.NEUTRAL
                        messages.add(
                            ChatMessage(
                                text = AI_TUTOR_GREETING,
                                fromUser = false,
                                animate = false
                            )
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
                val robotH = 150.dp
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
                itemsIndexed(messages) { _, msg ->
                    val isMsgSpeaking = speaker.isSpeaking && speakingMessageText == msg.text
                    ChatBubble(
                        message = msg,
                        isSpeaking = isMsgSpeaking,
                        onSpeak = {
                            speakingMessageText = it
                            speaker.speak(it, "en-US", UserPrefs.tutorVoice)
                        },
                        onStop = {
                            speaker.stop()
                            speakingMessageText = null
                        }
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
                // Quick Chips Horizontal Scroll
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
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean = false,
    onSpeak: (String) -> Unit = {},
    onStop: () -> Unit = {}
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
                if (message.animate && !isUser) {
                    TypewriterText(
                        fullText = message.text,
                        color = Color(0xFF1D2939),
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = message.text,
                        color = Color(0xFF1D2939),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

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
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = tr("Report"),
                            tint = Color(0xFF1D2939).copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
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
            LaunchedEffect(Unit) {
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
