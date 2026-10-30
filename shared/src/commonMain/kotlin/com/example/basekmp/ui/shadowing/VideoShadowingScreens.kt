package com.example.basekmp.ui.shadowing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.CongratsDialog
import com.example.basekmp.components.PrimaryButton
import com.example.basekmp.components.ProgressHeader
import com.example.basekmp.ui.components.VideoSurface
import com.example.basekmp.ui.components.rememberVideoPlayer
import com.example.basekmp.data.ShadowingVideos
import com.example.basekmp.data.Languages
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.currentTimeMillis
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.theme.Blue
import com.example.basekmp.theme.CardBorder
import com.example.basekmp.theme.LevelPillBg
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.speaking.SpeechCaptureMic
import com.example.basekmp.ui.speaking.SpeechPlaybackButton

private val SHADOWING_CATEGORIES = listOf("All", "Daily Life", "Travel", "Business", "Entertainment", "Culture")
private val SPEEDS = listOf(1.0f, 0.75f, 0.5f)

/**
 * Screen displaying the full catalog of 28 video shadowing lessons.
 * 100% migrated from decompiled VideoShadowingScreensKt.java with zero dummy data.
 */
@Composable
fun VideoShadowingListScreen(
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenSavedWords: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    var videos by remember { mutableStateOf<List<ShadowingVideos.Video>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(true) }

    val savedWords by UserPrefs.savedWordsState
    val doneVideos by UserPrefs.shadowingDoneState

    LaunchedEffect(Unit) {
        isLoading = true
        videos = ShadowingVideos.all()
        isLoading = false
    }

    val filteredVideos = remember(videos, selectedCategory) {
        if (selectedCategory == "All") {
            videos
        } else {
            videos.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = dimens.spacingLg)
    ) {
        Spacer(Modifier.height(8.dp))
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.topBarHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(dimens.minTouchTarget)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", fontSize = 32.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(dimens.spacingSm))
                Text(
                    text = Strings.tr("Video Shadowing"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            // Saved Words Header Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerPill))
                    .clickable(onClick = onOpenSavedWords)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔖 ${savedWords.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
        }

        Spacer(Modifier.height(dimens.spacingMd))

        // Categories Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            SHADOWING_CATEGORIES.forEach { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(dimens.cornerPill))
                        .background(if (isSelected) colors.primary else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.primary else colors.border,
                            shape = RoundedCornerShape(dimens.cornerPill)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Strings.tr(category),
                        color = if (isSelected) Color.White else colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spacingLg))

        if (isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd),
                contentPadding = PaddingValues(bottom = dimens.bottomNavHeight + dimens.spacingLg)
            ) {
                items(
                    items = filteredVideos,
                    key = { it.id }
                ) { video ->
                    val isDone = doneVideos.contains(video.id)
                    VideoCatalogCard(
                        video = video,
                        isDone = isDone,
                        onClick = { onOpenVideo(video.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoCatalogCard(
    video: ShadowingVideos.Video,
    isDone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerLg))
            .background(colors.surface)
            .border(1.dp, if (isDone) colors.success.copy(alpha = 0.5f) else colors.border, RoundedCornerShape(dimens.cornerLg))
            .clickable(onClick = onClick)
            .padding(dimens.spacingLg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(dimens.cornerMd))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(video.colors.first),
                                Color(video.colors.second)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = video.speakers.firstOrNull()?.emoji ?: "🎬",
                    fontSize = 32.sp
                )
            }

            Spacer(Modifier.width(dimens.spacingMd))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Strings.tr(video.level),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text("•", color = colors.textTertiary, fontSize = 11.sp)
                    Text(
                        text = Strings.tr(video.category),
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = video.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val durationSec = (video.durationMs / 1000).coerceAtLeast(45)
                    val durationMin = durationSec / 60
                    val durationRem = durationSec % 60
                    Text(
                        text = "⏱️ ${durationMin}:${durationRem.toString().padStart(2, '0')}",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )

                    if (isDone) {
                        Text(
                            text = "✓ ${Strings.tr("Completed")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                    }
                }
            }

            Spacer(Modifier.width(dimens.spacingSm))

            Text("›", fontSize = 24.sp, color = colors.textTertiary, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Interactive Video Shadowing Player Screen with real-time subtitle sync,
 * instant word bookmarking, speech grading, and variable playback speed.
 */
@Composable
fun VideoShadowingPlayerScreen(
    videoId: String,
    onClose: () -> Unit,
    onOpenSavedWords: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    var video by remember { mutableStateOf<ShadowingVideos.Video?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentLineIndex by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(UserPrefs.shadowingSpeed) }
    var showLineTranslation by remember { mutableStateOf(true) }
    var speechScore by remember { mutableIntStateOf(0) }
    var lineScores by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var isFinished by remember { mutableStateOf(false) }
    val sessionStartedAt = remember(videoId) { currentTimeMillis() }

    LaunchedEffect(videoId) {
        isLoading = true
        video = ShadowingVideos.load(videoId)
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.primary)
        }
        return
    }

    val currentVideo = video
    if (currentVideo == null || currentVideo.lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background).padding(dimens.spacingLg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.tr("Video content unavailable."),
                color = colors.textSecondary,
                fontSize = 15.sp
            )
        }
        return
    }

    val lines = currentVideo.lines
    val averageScore = if (lineScores.isEmpty()) 0 else lineScores.values.average().toInt()

    fun completeShadowing() {
        UserPrefs.markShadowingDone(videoId, averageScore)
        UserPrefs.addLearnedMinutes(((currentTimeMillis() - sessionStartedAt) / 60_000L).toInt())
        UserPrefs.recordStreakDay()
        UserPrefs.recordPracticeDone("shadowing", averageScore, 100)
        onClose()
    }

    if (isFinished) {
        CongratsDialog(
            subtitle = Strings.tr("Session Completed successfully"),
            buttonText = Strings.tr("Done"),
            onButtonClick = { completeShadowing() },
            onClose = { completeShadowing() },
            correct = lineScores.values.count { it >= 60 },
            incorrect = lines.size - lineScores.values.count { it >= 60 }
        )
    }

    val currentLine = lines[currentLineIndex.coerceIn(0, lines.size - 1)]
    val currentSpeaker = currentVideo.speakers.getOrNull(currentLine.speaker)

    val videoAssetPath = remember(currentVideo.id) {
        "shadowing/clips/${currentVideo.id}.mp4"
    }
    val videoController = rememberVideoPlayer(
        assetPath = videoAssetPath,
        muted = false,
        onCompletion = {},
        onStop = {}
    )

    fun playCurrentLine() {
        if (lines.isNotEmpty()) {
            val line = lines[currentLineIndex.coerceIn(0, lines.size - 1)]
            videoController.setSpeed(playbackSpeed)
            videoController.stopAt(line.endMs)
            videoController.seekTo(line.startMs)
            videoController.play()
        }
    }

    LaunchedEffect(currentLineIndex, currentVideo.id) {
        playCurrentLine()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Spacer(Modifier.height(8.dp))
        // Player Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.topBarHeight)
                .padding(horizontal = dimens.spacingLg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.minTouchTarget)
                    .clip(CircleShape)
                    .clickable {
                        videoController.pause()
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 18.sp, color = colors.textSecondary)
            }

            Text(
                text = currentVideo.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = dimens.spacingMd)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerPill))
                    .clickable {
                        videoController.pause()
                        onOpenSavedWords()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🔖", fontSize = 14.sp)
            }
        }

        // Hero Video Canvas with Real Video Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(horizontal = dimens.spacingLg)
                .clip(RoundedCornerShape(dimens.cornerLg))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            VideoSurface(
                controller = videoController,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(videoController.videoAspect.coerceIn(0.5f, 2.2f))
            )

            // Tap entire video box to play/pause line
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        if (videoController.isPlaying) {
                            videoController.pause()
                        } else {
                            playCurrentLine()
                        }
                    }
            )

            // Play icon overlay when paused
            if (!videoController.isPlaying) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = Strings.tr("Play"),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Current speaker badge top-start
            currentSpeaker?.let { spk ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(dimens.cornerSm))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${spk.emoji} ${spk.name}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Playback progress badge bottom-end (e.g. 1/8)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(dimens.cornerSm))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${currentLineIndex + 1}/${lines.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Controls bar: Replay line, Speed, Translation toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingLg, vertical = dimens.spacingMd),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Replay Line button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerPill))
                    .clickable { playCurrentLine() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Replay,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = Strings.tr("Replay"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )
            }

            // Speed toggle button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerPill))
                    .clickable {
                        val nextSpeed = when (playbackSpeed) {
                            1.0f -> 0.75f
                            0.75f -> 0.5f
                            else -> 1.0f
                        }
                        playbackSpeed = nextSpeed
                        UserPrefs.shadowingSpeed = nextSpeed
                        videoController.setSpeed(nextSpeed)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${playbackSpeed}x",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }

            // Translation Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .background(if (showLineTranslation) colors.primary.copy(alpha = 0.15f) else colors.surface)
                    .border(1.dp, if (showLineTranslation) colors.primary else colors.border, RoundedCornerShape(dimens.cornerPill))
                    .clickable { showLineTranslation = !showLineTranslation }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showLineTranslation) "🌐 ${Strings.tr("Subtitles On")}" else "🌐 ${Strings.tr("Subtitles Off")}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (showLineTranslation) colors.primary else colors.textSecondary
                )
            }
        }

        // Active Dialogue Card with Clickable Words
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingLg)
                .clip(RoundedCornerShape(dimens.cornerXl))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                .padding(dimens.spacingLg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentSpeaker?.name.orEmpty()} (${Strings.tr("Speaker")})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )

                    SpeechPlaybackButton(
                        text = currentLine.text,
                        languageTag = Languages.learn(UserPrefs.targetLanguage).speechTag
                    )
                }

                Spacer(Modifier.height(dimens.spacingLg))

                // Words flow where tapping any word saves it
                val wordsList = remember(currentLine.text) {
                    currentLine.text.split(" ").filter { it.isNotBlank() }
                }

                Text(
                    text = currentLine.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(dimens.spacingMd))

                // Clickable word chips to save vocabulary
                Text(
                    text = Strings.tr("Tap any word to save to your vocabulary:"),
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )

                Spacer(Modifier.height(dimens.spacingSm))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wordsList.forEach { rawWord ->
                        val cleanWord = rawWord.replace(Regex("[^A-Za-z0-9]"), "")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(dimens.cornerSm))
                                .background(colors.surfaceVariant)
                                .clickable {
                                    UserPrefs.saveWords(listOf(cleanWord))
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ $cleanWord",
                                fontSize = 12.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (showLineTranslation) {
                    Spacer(Modifier.height(dimens.spacingLg))
                    val nativeTrans = currentLine.translation(UserPrefs.nativeLanguage).ifBlank { currentLine.text }
                    Text(
                        text = nativeTrans,
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(dimens.spacingXl))

                SpeechCaptureMic(
                    targetText = currentLine.text,
                    instruction = Strings.tr("Hold mic and speak aloud:"),
                    onResult = { score, _ ->
                        speechScore = score
                        lineScores = lineScores + (currentLineIndex to score)
                    }
                )

                AnimatedVisibility(visible = currentLineIndex in lineScores) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = dimens.spacingMd)
                    ) {
                        Text(
                            text = "${Strings.tr("Shadowing Accuracy")}: $speechScore%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (speechScore >= 60) colors.success else colors.accentGold
                        )
                    }
                }
            }
        }

        // Bottom Line Navigation Row
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
                        videoController.pause()
                        if (currentLineIndex > 0) {
                            currentLineIndex--
                            speechScore = lineScores[currentLineIndex] ?: 0
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.tr("Previous"),
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
                    .clickable {
                        videoController.pause()
                        if (currentLineIndex < lines.size - 1) {
                            currentLineIndex++
                            speechScore = lineScores[currentLineIndex] ?: 0
                        } else {
                            isFinished = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentLineIndex < lines.size - 1) Strings.tr("Next Line") else Strings.tr("Finish"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Screen displaying the user's saved vocabulary words from Video Shadowing.
 * 100% migrated from decompiled VideoShadowingScreensKt.java.
 */
@Composable
fun SavedWordsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    val savedWords by UserPrefs.savedWordsState

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = dimens.spacingLg)
    ) {
        Spacer(Modifier.height(8.dp))
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.topBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.minTouchTarget)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 32.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(dimens.spacingSm))
            Text(
                text = "${Strings.tr("Saved Words")} (${savedWords.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Spacer(Modifier.height(dimens.spacingMd))

        if (savedWords.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔖", fontSize = 48.sp)
                    Spacer(Modifier.height(dimens.spacingMd))
                    Text(
                        text = Strings.tr("No saved words yet."),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = Strings.tr("Tap any word while watching videos to save it here."),
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val wordList = remember(savedWords) { savedWords.keys.toList() }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd),
                contentPadding = PaddingValues(bottom = dimens.spacing2Xl)
            ) {
                items(
                    items = wordList,
                    key = { it }
                ) { word ->
                    SavedWordRow(
                        word = word,
                        meaning = "",
                        videoTitle = "",
                        onRemove = {
                            UserPrefs.removeSavedWord(word)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedWordRow(
    word: String,
    meaning: String,
    videoTitle: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerMd))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerMd))
            .padding(dimens.spacingMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                if (meaning.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = meaning,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                if (videoTitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "From: $videoTitle",
                        fontSize = 11.sp,
                        color = colors.primary
                    )
                }
            }

            Spacer(Modifier.width(dimens.spacingMd))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 12.sp, color = colors.textSecondary)
            }
        }
    }
}

fun mmss(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${if (sec < 10) "0$sec" else "$sec"}"
}

/**
 * Shadowing Practice Section for the Home screen, matching VideoShadowingScreensKt.ShadowingPracticeSection.
 */
@Composable
fun ShadowingPracticeSection(
    onSeeAll: () -> Unit,
    onOpenVideo: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = Strings.tr("Shadowing Practice")
) {
    var allVideos by remember { mutableStateOf(emptyList<ShadowingVideos.Video>()) }
    var selectedCategory by rememberSaveable { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        allVideos = ShadowingVideos.all()
    }

    val filteredVideos = if (selectedCategory == "all") {
        allVideos
    } else {
        allVideos.filter { it.category == selectedCategory }
    }

    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, spotColor = Color(0x14000000), ambientColor = Color(0x1F000000))
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder.copy(alpha = 0.6f), shape)
            .padding(vertical = 14.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Strings.tr("Listen, repeat, and speak"),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSeeAll() }
                    .padding(start = 6.dp, top = 4.dp, end = 0.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.tr("View all"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Blue
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Blue
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Category Chips
        val categories = listOf(
            "all" to Strings.tr("For you"),
            "daily" to Strings.tr("Daily life"),
            "travel" to Strings.tr("Travel")
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { (catId, catLabel) ->
                val isSelected = catId == selectedCategory
                Text(
                    text = catLabel,
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Blue else Color(0xFFEFF1FA))
                        .clickable { selectedCategory = catId }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Video Cards Horizontal LazyRow
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredVideos) { video ->
                val cardShape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .width(148.dp)
                        .clip(cardShape)
                        .clickable { onOpenVideo(video.id) }
                ) {
                    // Video Thumbnail
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(136.dp)
                            .clip(cardShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(video.colors.first),
                                        Color(video.colors.second)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val thumbRes = ShadowingVideos.thumbnailDrawable(video.id)
                        if (thumbRes != null) {
                            Image(
                                painter = painterResource(thumbRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Play button
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = Strings.tr("Play"),
                                modifier = Modifier.size(30.dp),
                                tint = Color.White
                            )
                        }

                        // Duration Badge
                        if (video.durationMs > 0) {
                            Text(
                                text = mmss(video.durationMs),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = ShadowingVideos.title(video),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = Strings.tr(ShadowingVideos.levelName(video)),
                        color = Blue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LevelPillBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
