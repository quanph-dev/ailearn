package com.example.basekmp.ui.shadowing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontWeight
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
import com.example.basekmp.theme.ChipBlueBg
import com.example.basekmp.theme.CardBorder
import com.example.basekmp.theme.LevelPillBg
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary

private val SPEEDS = listOf(1.0f, 0.75f, 0.5f)

/**
 * Screen displaying the remote shadowing catalog with bundled thumbnails as an offline fallback.
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
    var isLoading by remember { mutableStateOf(true) }
    var loadAttempt by remember { mutableIntStateOf(0) }

    val savedWords by UserPrefs.savedWordsState
    val doneVideos by UserPrefs.shadowingDoneState

    LaunchedEffect(loadAttempt) {
        isLoading = true
        videos = runCatching { ShadowingVideos.all() }.getOrDefault(emptyList())
        isLoading = false
    }

    val savedWordCount = savedWords.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = dimens.spacingLg)
    ) {
        Spacer(Modifier.height(8.dp))
        // Top Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.topBarHeight)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(dimens.minTouchTarget)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 32.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }

            Text(
                text = Strings.tr("Video Shadowing"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = if (savedWordCount > 0) 124.dp else dimens.minTouchTarget)
            )

            if (savedWordCount > 0) {
                Text(
                    text = Strings.trf("Saved words (%1\$d)", savedWordCount) + "  →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(dimens.cornerSm))
                        .clickable(onClick = onOpenSavedWords)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            if (videos.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = dimens.spacingLg),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Strings.tr("Video content unavailable."),
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(dimens.spacingMd))
                    PrimaryButton(
                        text = Strings.tr("Try Again"),
                        onClick = { loadAttempt++ },
                        modifier = Modifier.fillMaxWidth(0.72f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd),
                    contentPadding = PaddingValues(bottom = dimens.spacingLg)
                ) {
                    gridItems(
                        items = videos,
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerLg))
            .background(colors.surface)
            .border(1.dp, if (isDone) colors.success.copy(alpha = 0.5f) else colors.border, RoundedCornerShape(dimens.cornerLg))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // The original catalog uses a two-column card with the scene image above its title.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.48f)
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
            val thumbnail = ShadowingVideos.thumbnailDrawable(video.id)
            if (thumbnail == null) {
                Text(text = video.scene.ifBlank { "🎬" }, fontSize = 30.sp)
            }
            ShadowingThumbnail(
                remoteUrl = ShadowingVideos.thumbnailUrl(video),
                cacheKey = "${video.id}.v${video.remote?.version ?: 1}",
                fallback = thumbnail,
                contentDescription = ShadowingVideos.title(video),
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = Strings.tr("Play"),
                    modifier = Modifier.size(26.dp),
                    tint = colors.primary
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = ShadowingVideos.title(video),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            minLines = 1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${video.level.uppercase()} • ${catalogDuration(video.durationMs)}",
                fontSize = 12.sp,
                color = colors.textTertiary,
                maxLines = 1
            )
            if (isDone) {
                Text("✓", fontSize = 14.sp, color = colors.success, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Interactive Video Shadowing Player Screen with real-time subtitle sync,
 * instant word bookmarking, speech grading, and variable playback speed.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoShadowingPlayerScreen(
    videoId: String,
    onClose: () -> Unit,
    onOpenSavedWords: () -> Unit,
    modifier: Modifier = Modifier,
    onSessionCompleted: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    var video by remember { mutableStateOf<ShadowingVideos.Video?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var currentLineIndex by remember { mutableIntStateOf(0) }
    var spokenTranscript by remember(videoId, currentLineIndex) { mutableStateOf("") }
    var playbackSpeed by remember { mutableFloatStateOf(UserPrefs.shadowingSpeed) }
    var showLineTranslation by remember { mutableStateOf(false) }
    var isSpeechCapturing by remember(videoId) { mutableStateOf(false) }
    var speechScore by remember { mutableIntStateOf(0) }
    var lineScores by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var isFinished by remember { mutableStateOf(false) }
    var completionRecorded by remember(videoId) { mutableStateOf(false) }
    var isSeeking by remember(videoId) { mutableStateOf(false) }
    var seekProgress by remember(videoId) { mutableFloatStateOf(0f) }
    val sessionStartedAt = remember(videoId) { currentTimeMillis() }

    LaunchedEffect(videoId, loadAttempt) {
        isLoading = true
        video = runCatching { ShadowingVideos.load(videoId) }.getOrNull()
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
        Column(
            modifier = modifier.fillMaxSize().background(colors.background).padding(dimens.spacingLg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Strings.tr("Video content unavailable."),
                color = colors.textSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimens.spacingMd))
            PrimaryButton(
                text = Strings.tr("Try Again"),
                onClick = { loadAttempt++ },
                modifier = Modifier.fillMaxWidth(0.76f)
            )
            Spacer(Modifier.height(dimens.spacingSm))
            Text(
                text = Strings.tr("Go Back"),
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.cornerPill))
                    .clickable(onClick = onClose)
                    .padding(horizontal = dimens.spacingLg, vertical = dimens.spacingSm)
            )
        }
        return
    }

    val lines = currentVideo.lines
    val averageScore = if (lineScores.isEmpty()) 0 else lineScores.values.average().toInt()

    fun recordShadowingCompletion() {
        if (completionRecorded) return
        completionRecorded = true
        UserPrefs.markShadowingDone(videoId, averageScore)
        UserPrefs.addLearnedMinutes(((currentTimeMillis() - sessionStartedAt) / 60_000L).toInt())
        UserPrefs.recordStreakDay()
        UserPrefs.recordPracticeDone("shadowing", averageScore, 100)
        onSessionCompleted?.invoke()
    }

    fun completeShadowing() {
        recordShadowingCompletion()
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

    val currentLine = lines[currentLineIndex.coerceIn(0, lines.lastIndex)]
    val scriptCode = Languages.learn(UserPrefs.targetLanguage).code
    val displayLineText = currentLine.textIn(scriptCode)
    val savedWords by UserPrefs.savedWordsState
    val vocabularyWords = remember(currentVideo.words, scriptCode) {
        currentVideo.words
            .map { it.wordIn(scriptCode).trim() }
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase() }
    }
    val vocabularyWordKeys = remember(vocabularyWords) {
        vocabularyWords.map(ShadowingTranscript::normalizeToken)
            .filter(String::isNotEmpty)
            .toSet()
    }
    val unsavedWords = remember(vocabularyWords, savedWords) {
        vocabularyWords.filter { it.lowercase() !in savedWords }
    }
    val allWordsSaved = vocabularyWords.isNotEmpty() && unsavedWords.isEmpty()

    val videoAssetPath = remember(currentVideo.id, currentVideo.asset) {
        ShadowingVideos.playbackUrl(currentVideo)
    }
    val videoController = rememberVideoPlayer(
        assetPath = videoAssetPath,
        cacheKey = "shadowing:${currentVideo.id}:v${currentVideo.remote?.version ?: 1}",
        muted = false,
        onCompletion = {},
        onStop = {}
    )
    val wordsList = remember(displayLineText) { ShadowingTranscript.tokens(displayLineText) }
    val wordStatuses = remember(displayLineText, spokenTranscript) {
        ShadowingTranscript.wordStatuses(displayLineText, spokenTranscript)
    }
    val progressedWordCount by remember(
        videoController,
        currentLine.startMs,
        currentLine.endMs,
        wordsList.size
    ) {
        derivedStateOf {
            ShadowingTranscript.progressedWordCount(
                positionMs = videoController.positionMs,
                lineStartMs = currentLine.startMs,
                lineEndMs = currentLine.endMs,
                wordCount = wordsList.size
            )
        }
    }
    val isVideoPlaying = videoController.isPlaying

    val totalDurationMs = maxOf(videoController.durationMs, currentVideo.durationMs).coerceAtLeast(1L)
    val playbackProgress = if (isSeeking) {
        seekProgress
    } else {
        (videoController.positionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
    }

    fun playCurrentLine(lineIndex: Int = currentLineIndex, startAtMs: Long? = null) {
        if (isSpeechCapturing) return
        val line = lines.getOrNull(lineIndex) ?: return
        val startPosition = startAtMs?.coerceIn(line.startMs, line.endMs) ?: line.startMs
        videoController.setSpeed(playbackSpeed)
        videoController.stopAt(line.endMs)
        videoController.seekTo(startPosition)
        videoController.play()
    }

    fun finishSession() {
        if (isFinished) return
        videoController.pause()
        recordShadowingCompletion()
        isFinished = true
    }

    fun advanceToNextLine() {
        if (isSpeechCapturing) return
        videoController.pause()
        if (currentLineIndex < lines.lastIndex) {
            val nextIndex = currentLineIndex + 1
            currentLineIndex = nextIndex
            speechScore = lineScores[nextIndex] ?: 0
            playCurrentLine(nextIndex)
        } else {
            finishSession()
        }
    }

    fun seekToPosition(positionMs: Long) {
        if (isSpeechCapturing) return
        val target = positionMs.coerceIn(0L, totalDurationMs)
        val targetIndex = lines.indexOfLast { it.startMs <= target }.coerceIn(0, lines.lastIndex)
        val line = lines[targetIndex]
        val linePosition = target.coerceIn(line.startMs, line.endMs)
        currentLineIndex = targetIndex
        speechScore = lineScores[targetIndex] ?: 0
        spokenTranscript = ""
        playCurrentLine(targetIndex, linePosition)
    }

    LaunchedEffect(currentVideo.id) {
        playCurrentLine(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        Spacer(Modifier.height(8.dp))

        // Centered player title and the saved-word shortcut used by the original screen.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.topBarHeight)
                .padding(horizontal = dimens.spacingLg)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(dimens.minTouchTarget)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable {
                        videoController.pause()
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 32.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }

            Text(
                text = ShadowingVideos.title(currentVideo),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.minTouchTarget)
            )

            if (savedWords.isNotEmpty()) {
                Text(
                    text = Strings.trf("Saved words (%1\$d)", savedWords.size) + "  →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(dimens.cornerSm))
                        .clickable {
                            videoController.pause()
                            onOpenSavedWords()
                        }
                        .padding(start = 6.dp, top = 8.dp, end = 2.dp, bottom = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // The original player keeps the scene wide and puts its scrubber and time labels over it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
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

            // The video is the quick way to skip speaking and move to the next transcript line.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = !isSpeechCapturing, onClick = ::advanceToNextLine)
            )

            if (!isVideoPlaying && !isSpeechCapturing) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.88f))
                        .clickable(onClick = { playCurrentLine() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = Strings.tr("Play"),
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            val shownPositionMs = if (isSeeking) {
                (seekProgress * totalDurationMs).toLong()
            } else {
                videoController.positionMs
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
                        )
                    )
                    .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(mmss(shownPositionMs), color = Color.White, fontSize = 12.sp)
                val seekBarColors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.65f)
                )
                Slider(
                    value = playbackProgress,
                    enabled = !isSpeechCapturing,
                    onValueChange = { value ->
                        if (!isSeeking) {
                            videoController.pause()
                            seekProgress = playbackProgress
                        }
                        isSeeking = true
                        seekProgress = value
                    },
                    onValueChangeFinished = {
                        val destinationMs = (seekProgress * totalDurationMs).toLong()
                        isSeeking = false
                        seekToPosition(destinationMs)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).height(32.dp),
                    thumb = {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, colors.primary, CircleShape)
                        )
                    },
                    colors = seekBarColors,
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(4.dp),
                            enabled = !isSpeechCapturing,
                            colors = seekBarColors,
                            drawStopIndicator = null,
                            thumbTrackGapSize = 0.dp,
                            trackInsideCornerSize = 0.dp
                        )
                    }
                )
                Text(mmss(totalDurationMs), color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = Strings.trf("Line %1\$d of %2\$d", currentLineIndex + 1, lines.size),
            fontSize = 13.sp,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = dimens.spacingLg, vertical = 4.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.spacingLg),
            horizontalAlignment = Alignment.Start
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                wordsList.forEachIndexed { index, word ->
                    val isNewVocabularyWord = ShadowingTranscript.normalizeToken(word) in vocabularyWordKeys
                    val status = wordStatuses.getOrElse(index) { ShadowingTranscript.UNHEARD }
                    val isIncorrect = status == ShadowingTranscript.WRONG
                    val wordShape = RoundedCornerShape(8.dp)
                    val wordModifier = if (isNewVocabularyWord) {
                        Modifier
                            .clip(wordShape)
                            .background(if (isIncorrect) colors.errorBg else ChipBlueBg)
                            .border(1.dp, if (isIncorrect) colors.error else colors.primary, wordShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    } else {
                        Modifier
                    }
                    Text(
                        text = word,
                        modifier = wordModifier,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isIncorrect -> colors.error
                            isNewVocabularyWord -> colors.textPrimary
                            index < progressedWordCount -> colors.primary
                            else -> colors.textPrimary
                        },
                        lineHeight = 21.sp
                    )
                }
            }

            val nativeCode = Languages.nativeFor(UserPrefs.nativeLanguage).code
            val nativeTranslation = currentLine.translationFor(nativeCode, scriptCode)
            if (showLineTranslation && nativeTranslation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = nativeTranslation,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            val targetCode = scriptCode.uppercase()
            val nativeLabelCode = nativeCode.uppercase()
            val languageLabel = if (showLineTranslation) {
                if (nativeLabelCode == "VI") "VIE" else nativeLabelCode
            } else {
                if (targetCode == "EN") "ENG" else targetCode
            }

            // Match the original three controls: speed, replay, and transcript language.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerSm))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerSm))
                        .clickable {
                            val currentSpeedIndex = SPEEDS.indexOf(playbackSpeed).coerceAtLeast(0)
                            val nextSpeed = SPEEDS[(currentSpeedIndex + 1) % SPEEDS.size]
                            playbackSpeed = nextSpeed
                            UserPrefs.shadowingSpeed = nextSpeed
                            videoController.setSpeed(nextSpeed)
                        }
                        .height(32.dp)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (playbackSpeed == 1f) "1x" else playbackSpeed.toString() + "x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerSm))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerSm))
                    .clickable(enabled = !isSpeechCapturing) { playCurrentLine() }
                        .height(32.dp)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = Strings.tr("Replay"),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerSm))
                        .background(if (showLineTranslation) colors.primary else colors.surface)
                        .border(
                            1.dp,
                            if (showLineTranslation) colors.primary else colors.border,
                            RoundedCornerShape(dimens.cornerSm)
                        )
                        .clickable { showLineTranslation = !showLineTranslation }
                        .height(32.dp)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = languageLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showLineTranslation) Color.White else colors.textPrimary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.cornerMd))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerMd))
                    .padding(dimens.spacingMd),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = Strings.tr("We heard"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = spokenTranscript.takeIf(String::isNotBlank)
                        ?.let { "“$it”" }
                        ?: Strings.tr("Your words will appear here"),
                    fontSize = 14.sp,
                    color = if (spokenTranscript.isBlank()) colors.textTertiary else colors.textPrimary,
                    lineHeight = 20.sp
                )
            }

            ShadowingSpeechCapture(
                targetText = displayLineText,
                onPartialResult = { transcript -> spokenTranscript = transcript },
                onResult = { score, transcript ->
                    spokenTranscript = transcript
                    speechScore = score
                    lineScores = lineScores + (currentLineIndex to score)
                },
                onBeforeStartRecording = {
                    // Keep the source audio out of the microphone capture and speech grading.
                    videoController.pause()
                },
                onRecordingStateChange = { isSpeechCapturing = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            Text(
                text = Strings.tr(
                    when {
                        isSpeechCapturing -> "Wait for your turn"
                        currentLineIndex == lines.lastIndex -> "Tap the video to finish"
                        else -> "Tap the video for the next line"
                    }
                ),
                fontSize = 13.sp,
                color = colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            AnimatedVisibility(visible = currentLineIndex in lineScores) {
                Text(
                    text = Strings.tr("Shadowing Accuracy") + ": " + speechScore + "%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (speechScore >= 60) colors.success else colors.accentGold,
                    modifier = Modifier.fillMaxWidth().padding(top = dimens.spacingMd),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        val actionText = when {
            isFinished -> Strings.tr("Finish")
            vocabularyWords.isEmpty() -> Strings.tr("No new words")
            allWordsSaved -> Strings.trf("All %1\$d words saved", vocabularyWords.size) + " · " + Strings.tr("View")
            else -> Strings.trf("Save all new words (%1\$d)", unsavedWords.size)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingLg, vertical = 8.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(dimens.cornerPill))
                .then(
                    if (isFinished) Modifier.background(colors.success)
                    else Modifier.background(colors.primaryBrush)
                )
                .clickable(enabled = !isSpeechCapturing) {
                    when {
                        isFinished -> {
                            completeShadowing()
                        }
                        allWordsSaved -> {
                            videoController.pause()
                            onOpenSavedWords()
                        }
                        unsavedWords.isNotEmpty() -> UserPrefs.saveWords(unsavedWords)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
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
    var savedWordDetails by remember { mutableStateOf(emptyMap<String, ShadowingVideos.SavedWord>()) }
    val nativeCode = Languages.nativeFor(UserPrefs.nativeLanguage).code

    LaunchedEffect(savedWords, nativeCode) {
        savedWordDetails = runCatching {
            ShadowingVideos.savedWordIndex(nativeCode)
        }.getOrDefault(emptyMap())
    }

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
            val wordEntries = remember(savedWords) {
                savedWords.entries.sortedByDescending { it.value }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd),
                contentPadding = PaddingValues(bottom = dimens.spacing2Xl)
            ) {
                items(
                    items = wordEntries,
                    key = { it.key }
                ) { entry ->
                    val detail = savedWordDetails[entry.key]
                    val videoTitle = detail?.videoTitles?.get(nativeCode)
                        ?.takeIf(String::isNotBlank)
                        ?: detail?.videoTitle.orEmpty()
                    SavedWordRow(
                        word = detail?.display ?: entry.key,
                        meaning = detail?.meaning.orEmpty(),
                        videoTitle = videoTitle,
                        onRemove = {
                            UserPrefs.removeSavedWord(entry.key)
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

private fun catalogDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(1L)
    return "${(totalSeconds / 60).toString().padStart(2, '0')}:${(totalSeconds % 60).toString().padStart(2, '0')}"
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
                        ShadowingThumbnail(
                            remoteUrl = ShadowingVideos.thumbnailUrl(video),
                            cacheKey = "${video.id}.v${video.remote?.version ?: 1}",
                            fallback = ShadowingVideos.thumbnailDrawable(video.id),
                            contentDescription = ShadowingVideos.title(video),
                            modifier = Modifier.fillMaxSize()
                        )

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
