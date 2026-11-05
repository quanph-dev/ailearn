package com.example.basekmp.ui.plan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.example.basekmp.data.GoalPlan
import com.example.basekmp.data.PlanCatalogRepository
import com.example.basekmp.data.PlanGoal
import com.example.basekmp.data.PlanProgress
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.data.tr
import com.example.basekmp.data.trf
import com.example.basekmp.theme.BrandAccent
import com.example.basekmp.theme.BrandPrimary
import com.example.basekmp.theme.BrandPrimaryPressed
import com.example.basekmp.theme.BrandPrimarySoft
import com.example.basekmp.theme.CardCyan
import com.example.basekmp.theme.ChipOutlineText
import com.example.basekmp.theme.DividerColor
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.OptionSurfaceSelected
import com.example.basekmp.theme.SurfaceMint
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.theme.TrackColor
import com.example.basekmp.ui.components.BackTitleBar
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.plan_cel_body
import ailearn.shared.generated.resources.plan_james_arm
import ailearn.shared.generated.resources.plan_james_body
import ailearn.shared.generated.resources.plan_stage_plane
import ailearn.shared.generated.resources.plan_stage_suitcase
import ailearn.shared.generated.resources.tutor_robot
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.CancellationException

private val PathBlue = BrandPrimary
private val PathBlueDark = BrandPrimaryPressed
private val PathBlueSoft = OptionSurfaceSelected
private val PathBackgroundTop = SurfaceMint
private val PathBackgroundBottom = Color.White
private val PathText = TextPrimary
private val PathMuted = TextSecondary
private val PathLocked = ChipOutlineText
private val PathRowStep = 104.dp
private val PathCurrentNodeSize = 92.dp
private val PathNodeSize = 58.dp
private val PathNodeCenterTop = 102.dp

/** My Learning Path, aligned to the original app's roadmap layout and assets. */
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    onChangeGoal: () -> Unit,
    onOpenDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = PlanProgress.get()
    val revision = progress.revision.intValue
    var isCatalogLoading by remember { mutableStateOf(true) }
    var currentGoal by remember { mutableStateOf<PlanGoal?>(null) }
    var assistantVoice by remember { mutableStateOf(UserPrefs.tutorVoice) }

    LaunchedEffect(revision) {
        isCatalogLoading = true
        val loadedCatalog = try {
            PlanCatalogRepository.getCatalog()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        currentGoal = loadedCatalog?.goal(progress.goalId)
        isCatalogLoading = false
    }

    val completedDays = remember(revision) { progress.completedDays() }
    val currentDay = remember(revision) { progress.currentDay() }
    val doneActivitiesByDay = remember(revision) {
        (1..GoalPlan.DAYS).associateWith(progress::done)
    }
    val stageCount = currentGoal?.stages?.size?.takeIf { it > 0 }
        ?: (GoalPlan.DAYS / GoalPlan.DAYS_PER_STAGE)
    val activeStageIndex = ((currentDay.coerceIn(1, GoalPlan.DAYS) - 1) / GoalPlan.DAYS_PER_STAGE)
        .coerceIn(0, stageCount - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeStageIndex)
    val dailyProgress = completedDays.size.toFloat() / GoalPlan.DAYS.toFloat()

    LaunchedEffect(activeStageIndex, stageCount, currentGoal) {
        if (currentGoal != null) listState.animateScrollToItem(activeStageIndex)
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PathBackgroundTop, PathBackgroundBottom)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                PathTopBar(onBack = onBack)
                Spacer(Modifier.height(8.dp))
                PersonalizeCards(
                    goalTitle = currentGoal?.displayTitle(UserPrefs.targetLanguage)
                        ?: tr("Everyday Conversation"),
                    voice = assistantVoice,
                    onGoal = onChangeGoal,
                    onAssistant = {
                        assistantVoice = if (assistantVoice.equals("Male", ignoreCase = true)) "Female" else "Male"
                        UserPrefs.tutorVoice = assistantVoice
                    }
                )
                Spacer(Modifier.height(14.dp))
                PathProgressHeader(progress = dailyProgress, completedDays = completedDays.size)
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    val stages = currentGoal?.stages.orEmpty()
                    if (currentGoal == null) {
                        item(key = "plan_catalog_state") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 52.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isCatalogLoading) {
                                    CircularProgressIndicator(color = PathBlue)
                                    Text(tr("Loading plan..."), color = PathMuted, fontSize = 14.sp)
                                } else {
                                    Text(tr("Plan content unavailable"), color = PathMuted, fontSize = 14.sp)
                                    TextButton(onClick = onChangeGoal) {
                                        Text(tr("Choose another goal"), color = PathBlue)
                                    }
                                }
                            }
                        }
                    }

                    stages.forEachIndexed { index, stage ->
                        val goal = currentGoal ?: return@forEachIndexed
                        val firstDay = index * GoalPlan.DAYS_PER_STAGE + 1
                        val lastDay = ((index + 1) * GoalPlan.DAYS_PER_STAGE).coerceAtMost(GoalPlan.DAYS)
                        item(key = "stage_${index + 1}") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                StageBanner(
                                    stageNumber = index + 1,
                                    firstDay = firstDay,
                                    lastDay = lastDay,
                                    title = stage.displayTitle(UserPrefs.targetLanguage)
                                )
                                Spacer(Modifier.height(20.dp))
                                PlanStagePath(
                                    startDay = firstDay,
                                    endDay = lastDay,
                                    goal = goal,
                                    currentDay = currentDay,
                                    completedDays = completedDays,
                                    doneActivitiesByDay = doneActivitiesByDay,
                                    progress = progress,
                                    showRobot = currentDay in firstDay..lastDay,
                                    onOpenDay = onOpenDay
                                )
                            }
                        }
                    }
                }

                if (currentDay <= GoalPlan.DAYS && currentGoal != null) {
                    PathStartBar(
                        currentDay = currentDay,
                        goal = currentGoal,
                        progress = progress,
                        doneActivities = doneActivitiesByDay[currentDay].orEmpty(),
                        onStart = { if (progress.isUnlocked(currentDay)) onOpenDay(currentDay) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PathTopBar(onBack: () -> Unit) {
    BackTitleBar(
        title = tr("My Learning Path"),
        onBack = onBack,
        modifier = Modifier.padding(horizontal = 20.dp),
        titleSize = 21.sp,
        titleColor = PathText,
        buttonBackground = Color.White.copy(alpha = 0.72f),
        buttonTint = PathText,
        minHeight = 56.dp
    )
}

@Composable
private fun PersonalizeCards(
    goalTitle: String,
    voice: String,
    onGoal: () -> Unit,
    onAssistant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(70.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onGoal),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoalTargetMark()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = tr("Goal").uppercase(),
                        color = PathMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = goalTitle,
                        color = PathText,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = tr("Change goal"),
                    tint = PathBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        val assistantName = when {
            voice.equals("Male", ignoreCase = true) -> "James"
            voice.equals("Robot", ignoreCase = true) -> "Sparky"
            else -> "Emma"
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onAssistant),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlanAssistantAvatar(voice = voice, size = 38.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = tr("Assistant").uppercase(),
                        color = PathMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = tr(assistantName),
                        color = PathText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = tr("Change assistant"),
                    tint = PathBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalTargetMark() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(OptionSurfaceSelected, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            listOf(0.82f, 0.52f, 0.2f).forEach { fraction ->
                drawCircle(
                    color = PathBlue,
                    radius = size.minDimension * fraction / 2f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(color = PathBlue, radius = size.minDimension * 0.07f, center = center)
        }
    }
}

@Composable
private fun PlanAssistantAvatar(voice: String, size: Dp) {
    val image = when {
        voice.equals("Robot", ignoreCase = true) -> Res.drawable.tutor_robot
        voice.equals("Male", ignoreCase = true) -> Res.drawable.plan_james_body
        else -> Res.drawable.plan_cel_body
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceMint),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(image),
            contentDescription = tr("AI assistant"),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (voice.equals("Male", ignoreCase = true)) {
            RobotFaceOverlay(sleeping = false, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PathProgressHeader(progress: Float, completedDays: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Your Progress"),
                color = PathText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = trf("%1\$d/%2\$d days", completedDays, GoalPlan.DAYS),
                color = PathMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        PlanProgressBar(progress = progress, modifier = Modifier.fillMaxWidth(), height = 8.dp)
    }
}

@Composable
internal fun PlanProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "planProgress"
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(TrackColor)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(height / 2))
                    .background(Brush.horizontalGradient(listOf(BrandPrimarySoft, PathBlue)))
            )
        }
    }
}

@Composable
private fun StageBanner(stageNumber: Int, firstDay: Int, lastDay: Int, title: String) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(BrandPrimarySoft, PathBlueDark)))
            .padding(start = 15.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${tr("Stage")} $stageNumber · ${tr("Days")} $firstDay–$lastDay".uppercase(),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.45.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        StageArt(title = title)
    }
}

@Composable
private fun StageArt(title: String) {
    val lower = title.lowercase()
    val asset = when {
        "travel" in lower || "trip" in lower || "journey" in lower || "du lịch" in lower -> Res.drawable.plan_stage_suitcase
        "plane" in lower || "flight" in lower -> Res.drawable.plan_stage_plane
        else -> null
    }
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (asset != null) {
            androidx.compose.foundation.Image(
                painter = painterResource(asset),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(42.dp)
            )
        } else {
            Text("👋", fontSize = 30.sp)
        }
    }
}

@Composable
private fun PlanStagePath(
    startDay: Int,
    endDay: Int,
    goal: PlanGoal,
    currentDay: Int,
    completedDays: Set<Int>,
    doneActivitiesByDay: Map<Int, Set<PlanProgress.Activity>>,
    progress: PlanProgress,
    showRobot: Boolean,
    onOpenDay: (Int) -> Unit
) {
    if (startDay > endDay) return
    val days = (startDay..endDay).toList()
    val xFractions = listOf(0.254f, 0.40f, 0.13f)
    val dashTransition = rememberInfiniteTransition(label = "pathDash")
    val dashPhase by dashTransition.animateFloat(
        initialValue = 0f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_250, easing = LinearEasing)
        ),
        label = "pathDashPhase"
    )
    val pathHeight = PathNodeCenterTop + PathRowStep * days.lastIndex + PathCurrentNodeSize / 2 + 18.dp
    var robotAwake by remember(showRobot, currentDay) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(pathHeight)
    ) {
        val availableWidth = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val nodeCenters = days.mapIndexed { index, _ ->
                Offset(
                    x = size.width * xFractions[index % xFractions.size],
                    y = PathNodeCenterTop.toPx() + PathRowStep.toPx() * index
                )
            }
            val style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 7.dp.toPx()), dashPhase)
            )
            for (index in 0 until nodeCenters.lastIndex) {
                val from = nodeCenters[index]
                val to = nodeCenters[index + 1]
                val route = Path().apply {
                    moveTo(from.x, from.y)
                    val midY = (from.y + to.y) / 2f
                    cubicTo(from.x, midY, to.x, midY, to.x, to.y)
                }
                drawPath(route, DividerColor, style = style)
                val day = days[index]
                val nextDayOpen = days.getOrNull(index + 1)?.let(progress::isUnlocked) == true
                if (day in completedDays || (day == currentDay && nextDayOpen)) {
                    drawPath(route, BrandPrimarySoft, style = style)
                }
            }
        }

        days.forEachIndexed { index, day ->
            val isDone = day in completedDays
            val isUnlocked = progress.isUnlocked(day) || isDone
            val isCurrent = day == currentDay && isUnlocked
            val size = if (isCurrent) PathCurrentNodeSize else PathNodeSize
            val centerX = availableWidth * xFractions[index % xFractions.size]
            val nodeTop = PathNodeCenterTop + PathRowStep * index - size / 2
            val nodeLeft = centerX - size / 2
            val activities = goal.stageFor(day)?.let { PlanFlow.activitiesFor(GoalPlan.videoFor(it, day)) }
                ?: PlanProgress.Activity.entries
            val minutes = activities.sumOf {
                PlanProgress.activityMinutes(it, UserPrefs.dailyGoalMinutes, activities)
            }
            val labelX = (nodeLeft + size + 10.dp).coerceAtMost(availableWidth - 90.dp)
            val labelWidth = (availableWidth - labelX).coerceAtLeast(82.dp)

            if (isCurrent && doneActivitiesByDay[day].orEmpty().isEmpty()) {
                StartHereTip(
                    modifier = Modifier.offset(
                        x = (centerX - 48.dp).coerceAtLeast(0.dp),
                        y = (nodeTop - 60.dp).coerceAtLeast(0.dp)
                    )
                )
            }

            DayPathNode(
                modifier = Modifier.offset(x = nodeLeft, y = nodeTop),
                size = size,
                state = when {
                    isDone -> DayState.DONE
                    isCurrent -> DayState.CURRENT
                    isUnlocked -> DayState.OPEN
                    else -> DayState.LOCKED
                },
                enabled = isUnlocked,
                day = day,
                onClick = { if (isUnlocked) onOpenDay(day) }
            )

            Column(
                modifier = Modifier
                    .offset(x = labelX, y = PathNodeCenterTop + PathRowStep * index - 18.dp)
                    .width(labelWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = isUnlocked) { onOpenDay(day) },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = trf("Day %1\$d", day),
                    color = if (isUnlocked) PathText else PathLocked,
                    fontSize = if (isCurrent) 16.sp else 15.sp,
                    fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = trf("%1\$d min", minutes),
                    color = if (isUnlocked) PathMuted else PathLocked,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        if (showRobot) {
            PathCloud(
                modifier = Modifier.offset(
                    x = (availableWidth * 0.70f).coerceAtMost(availableWidth - 70.dp),
                    y = PathNodeCenterTop + PathRowStep * 1.52f
                )
            )
            val bubbleX = availableWidth * 0.47f + 8.dp
            val robotCenterX = availableWidth - 65.dp
            RobotSpeechBubble(
                modifier = Modifier
                    .offset(x = bubbleX, y = PathNodeCenterTop - 82.dp),
                tailCenterFromEnd = (160.dp - (robotCenterX - bubbleX)).coerceIn(16.dp, 144.dp),
                awake = robotAwake,
                onTap = { robotAwake = !robotAwake }
            )
            PathRobot(
                modifier = Modifier.offset(
                    x = (availableWidth - 105.dp).coerceAtLeast(0.dp),
                    y = PathNodeCenterTop - 22.dp
                ),
                awake = robotAwake,
                onTap = { robotAwake = !robotAwake }
            )
        }
    }
}

@Composable
private fun DayPathNode(
    modifier: Modifier,
    size: Dp,
    state: DayState,
    enabled: Boolean,
    day: Int,
    onClick: () -> Unit
) {
    val isCurrent = state == DayState.CURRENT
    val isDone = state == DayState.DONE
    val isOpen = state == DayState.OPEN
    val bottomColor = when {
        isCurrent || isDone -> BrandPrimary
        isOpen -> BrandPrimarySoft
        else -> TrackColor
    }
    Box(
        modifier = modifier
            .size(size + 8.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .offset(y = if (isCurrent) 7.dp else 6.dp)
                .size(size)
                .clip(CircleShape)
                .background(bottomColor)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> Brush.verticalGradient(listOf(BrandPrimarySoft, PathBlue))
                        isDone -> Brush.verticalGradient(listOf(BrandPrimarySoft, BrandPrimaryPressed))
                        isOpen -> Brush.verticalGradient(listOf(Color.White, SurfaceMint))
                        else -> Brush.verticalGradient(listOf(OptionSurface, TrackColor))
                    }
                )
                .border(
                    width = if (isCurrent) 4.dp else 2.dp,
                    color = when {
                        isCurrent -> OptionSurfaceSelected
                        isDone -> Color.White.copy(alpha = 0.86f)
                        isOpen -> Color.White
                        else -> Color.White.copy(alpha = 0.9f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxSize()) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(size - 12.dp)
                            .border(3.dp, Color.White.copy(alpha = 0.95f), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (isCurrent) 15.dp else 9.dp)
                        .size(if (isCurrent) 38.dp else 27.dp, if (isCurrent) 15.dp else 10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (isCurrent) 0.22f else 0.36f))
                )
                Icon(
                    imageVector = when {
                        isDone -> Icons.Rounded.Check
                        isOpen || isCurrent -> Icons.Rounded.PlayArrow
                        else -> Icons.Rounded.Lock
                    },
                    contentDescription = when {
                        isDone -> trf("Day %1\$d completed", day)
                        isUnlockedDayState(state) -> trf("Open day %1\$d", day)
                        else -> trf("Day %1\$d locked", day)
                    },
                    tint = when {
                        isCurrent || isDone -> Color.White
                        isOpen -> PathBlue
                        else -> ChipOutlineText
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (isCurrent) 34.dp else 24.dp)
                )
            }
        }
    }
}

private fun isUnlockedDayState(state: DayState): Boolean =
    state == DayState.CURRENT || state == DayState.OPEN || state == DayState.DONE

@Composable
private fun StartHereTip(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tr("START HERE").uppercase(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp,
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(PathBlue)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        Canvas(Modifier.size(14.dp, 7.dp)) {
            val pointer = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(pointer, PathBlue)
        }
    }
}

@Composable
private fun RobotSpeechBubble(
    modifier: Modifier = Modifier,
    tailCenterFromEnd: Dp,
    awake: Boolean,
    onTap: () -> Unit
) {
    val shape = remember(tailCenterFromEnd) { RobotSpeechBubbleShape(tailCenterFromEnd) }
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .width(160.dp)
            .heightIn(min = 54.dp)
            .shadow(
                elevation = 4.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x304B8DF5),
                spotColor = Color(0x1F3678E7)
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            ),
        shape = shape,
        color = Color.White,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (awake) tr("Ready to learn?") else tr("Zzz... Tap me to wake me up"),
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
            )
        }
    }
}

private class RobotSpeechBubbleShape(
    private val tailCenterFromEnd: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadius = with(density) { 16.dp.toPx() }.coerceAtMost(size.height / 2f)
        val tailHeight = with(density) { 10.dp.toPx() }.coerceAtMost(size.height / 3f)
        val tailHalfWidth = with(density) { 7.dp.toPx() }.coerceAtMost(size.width / 8f)
        val bodyBottom = size.height - tailHeight
        val tailCenter = (size.width - with(density) { tailCenterFromEnd.toPx() })
            .coerceIn(tailHalfWidth, size.width - tailHalfWidth)
        val path = Path().apply {
            moveTo(cornerRadius, 0f)
            lineTo(size.width - cornerRadius, 0f)
            quadraticTo(size.width, 0f, size.width, cornerRadius)
            lineTo(size.width, bodyBottom - cornerRadius)
            quadraticTo(size.width, bodyBottom, size.width - cornerRadius, bodyBottom)
            lineTo(tailCenter + tailHalfWidth, bodyBottom)
            lineTo(tailCenter, size.height)
            lineTo(tailCenter - tailHalfWidth, bodyBottom)
            lineTo(cornerRadius, bodyBottom)
            quadraticTo(0f, bodyBottom, 0f, bodyBottom - cornerRadius)
            lineTo(0f, cornerRadius)
            quadraticTo(0f, 0f, cornerRadius, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun PathRobot(modifier: Modifier = Modifier, awake: Boolean, onTap: () -> Unit) {
    val floatTransition = rememberInfiniteTransition(label = "pathRobot")
    val floatY by floatTransition.animateFloat(
        initialValue = -2f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "robotFloat"
    )
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width = 100.dp, height = 108.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 4.dp)
                .size(width = 64.dp, height = 8.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x2252769A),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = floatY.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.plan_james_body),
                contentDescription = tr("AI assistant robot"),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            RobotFaceOverlay(sleeping = !awake, modifier = Modifier.fillMaxSize())
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.plan_james_arm),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = 31.dp)
                    .size(38.dp)
            )
            if (!awake) {
                Text(
                    text = "Zzz",
                    color = PathBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-3).dp, y = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun RobotFaceOverlay(sleeping: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val eyeY = size.height * 0.365f
        val eyeWidth = size.width * 0.09f
        val eyeHeight = size.height * 0.045f
        val eyeLefts = listOf(size.width * 0.385f, size.width * 0.555f)
        eyeLefts.forEach { left ->
            if (sleeping) {
                val eye = Path().apply {
                    moveTo(left, eyeY)
                    cubicTo(
                        left + eyeWidth * 0.25f,
                        eyeY + eyeHeight,
                        left + eyeWidth * 0.75f,
                        eyeY + eyeHeight,
                        left + eyeWidth,
                        eyeY
                    )
                }
                drawPath(
                    eye,
                    color = BrandAccent,
                    style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                drawOval(
                    color = BrandAccent,
                    topLeft = Offset(left + eyeWidth * 0.25f, eyeY - eyeHeight * 0.35f),
                    size = androidx.compose.ui.geometry.Size(eyeWidth * 0.55f, eyeHeight * 0.75f)
                )
            }
        }
        drawCircle(
            color = BrandAccent,
            radius = size.width * 0.013f,
            center = Offset(size.width * 0.50f, size.height * 0.425f)
        )
    }
}

@Composable
private fun PathCloud(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(66.dp, 34.dp)) {
        val cloud = Color.White.copy(alpha = 0.78f)
        drawRoundRect(
            color = cloud,
            topLeft = Offset(size.width * 0.10f, size.height * 0.43f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.78f, size.height * 0.48f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.22f)
        )
        drawCircle(cloud, size.height * 0.32f, Offset(size.width * 0.34f, size.height * 0.48f))
        drawCircle(cloud, size.height * 0.43f, Offset(size.width * 0.57f, size.height * 0.39f))
    }
}

@Composable
private fun PathStartBar(
    currentDay: Int,
    goal: PlanGoal?,
    progress: PlanProgress,
    doneActivities: Set<PlanProgress.Activity>,
    onStart: () -> Unit
) {
    val stage = goal?.stageFor(currentDay)
    val activities = stage?.let { PlanFlow.activitiesFor(GoalPlan.videoFor(it, currentDay)) }
        ?: PlanProgress.Activity.entries
    val minutes = activities.sumOf {
        PlanProgress.activityMinutes(it, UserPrefs.dailyGoalMinutes, activities)
    }
    val level = when {
        UserPrefs.englishLevel.startsWith("A1", ignoreCase = true) -> tr("Beginner")
        UserPrefs.englishLevel.startsWith("A2", ignoreCase = true) -> tr("Elementary")
        UserPrefs.englishLevel.startsWith("B2", ignoreCase = true) -> tr("Upper Intermediate")
        UserPrefs.englishLevel.startsWith("C", ignoreCase = true) -> tr("Advanced")
        else -> tr("Intermediate")
    }
    val unlocked = progress.isUnlocked(currentDay)
    val action = if (doneActivities.isEmpty()) "Start" else "Continue"
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape),
        shape = shape,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = trf("%1\$d min", minutes) + " · " + level,
                color = PathMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Button(
                onClick = onStart,
                enabled = unlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PathBlue,
                    disabledContainerColor = DividerColor,
                    disabledContentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
            Text(
                text = tr(action) + " " + trf("Day %1\$d", currentDay),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
