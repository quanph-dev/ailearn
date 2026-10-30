package com.example.basekmp.ui.learningpath

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.data.PathCourse
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.Blue
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.ui.components.TabHeader
import com.example.basekmp.ui.components.tabBottomClearance
import kotlinx.coroutines.launch

// 100% Authentic Colors matching decompiled LearningPathScreenKt.java
private val UnitCardBg = Color(4292405244L) // 0xFFD7E5FB
private val ChipSelectedBg = Color(4293587710L) // 0xFFEFF5FE
private val ProgressGreen = Color(4279681567L) // 0xFF12B76A
private val MutedText = Color(4286283912L) // 0xFF717688
private val DoneGreen = Color(4279411995L) // 0xFF0E931B
private val LineGrey = Color(4291086284L) // 0xFFC9CCD4

private val NODE = 68.dp
private val LIP = 7.dp
private val ROW = 110.dp
private val SIDE_INSET = 62.dp
private val LABEL_WIDTH = 120.dp

private data class NodeLook(
    val face: Color,
    val lip: Color,
    val icon: ImageVector,
    val tint: Color
)

private val DoneLook = NodeLook(
    face = Color(4283076088L), // 0xFF4575F8
    lip = Color(4290368507L),
    icon = Icons.Rounded.Star,
    tint = Color.White
)

private val CurrentLook = NodeLook(
    face = Color(4294556208L), // 0xFFF5A623 (Golden Orange)
    lip = Color(4292321596L), // 0xFFD4823C
    icon = Icons.Rounded.PlayArrow,
    tint = Color.White
)

private val LockedLook = NodeLook(
    face = Color(4293125350L), // 0xFFDFE2E6
    lip = Color(4290757061L), // 0xFFB9BDC5
    icon = Icons.Rounded.Lock,
    tint = Color(4288586150L) // 0xFF979CA6
)

/**
 * 100% Authentic Learning Path Screen migrated from decompiled LearningPathScreenKt.java.
 */
@Composable
fun LearningPathScreen(
    onOpenLesson: (PathCourse.Lesson) -> Unit,
    onPremiumClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedLevel by remember {
        mutableStateOf(UserPrefs.pathLevel?.ifBlank { "Beginner" } ?: "Beginner")
    }

    val completedMap by UserPrefs.pathLessonsState

    val lessonStates = remember(selectedLevel, completedMap) {
        PathCourse.states(selectedLevel, UserPrefs)
    }

    val units = remember(selectedLevel) {
        PathCourse.units(selectedLevel)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedLevel) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Spacer(Modifier.height(16.dp))

        // TopBar: "Learning Path", Notification bell (Diamond button hidden per IAP requirement)
        TabHeader(
            title = Strings.tr("Learning Path"),
            onPremiumClick = onPremiumClick,
            trailing = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OptionSurface)
                        .clickable { onNotificationsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = Strings.tr("Notifications"),
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // Level chips horizontal row: 🌱 Beginner, 🌿 Intermediate, 🏆 Advanced
        LevelChips(
            selected = selectedLevel,
            onSelect = { level ->
                selectedLevel = level
                UserPrefs.pathLevel = level
            }
        )

        Spacer(Modifier.height(16.dp))

        // Scrollable unit cards
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = tabBottomClearance)
        ) {
            itemsIndexed(
                items = units,
                key = { _, unit -> unit.id }
            ) { index, unit ->
                val unitLessons = lessonStates.filter { it.lesson.unitNumber == unit.number }
                UnitCard(
                    unit = unit,
                    isFirst = index == 0,
                    isLast = index == units.size - 1,
                    states = unitLessons,
                    allUnits = units,
                    onJump = { targetUnitNumber ->
                        val targetIndex = units.indexOfFirst { it.number == targetUnitNumber }
                        if (targetIndex >= 0) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                    },
                    onLessonClick = onOpenLesson
                )
            }
        }
    }
}

@Composable
private fun LevelChips(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PathCourse.LEVELS.forEach { level ->
            val isSelected = level == selected
            val shape = RoundedCornerShape(10.dp)
            val selectedModifier = if (isSelected) {
                Modifier
                    .background(ChipSelectedBg)
                    .border(1.dp, Blue, shape)
            } else {
                Modifier
            }

            Row(
                modifier = Modifier
                    .height(42.dp)
                    .clip(shape)
                    .then(selectedModifier)
                    .clickable { onSelect(level) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = PathCourse.levelEmoji(level),
                    fontSize = 17.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Strings.tr(level),
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun UnitCard(
    unit: PathCourse.PathUnit,
    isFirst: Boolean,
    isLast: Boolean,
    states: List<PathCourse.LessonState>,
    allUnits: List<PathCourse.PathUnit>,
    onJump: (Int) -> Unit,
    onLessonClick: (PathCourse.Lesson) -> Unit
) {
    val unitProgress = remember(unit, states) {
        PathCourse.unitProgress(unit, states)
    }

    val topCorner = if (isFirst) 20.dp else 0.dp
    val bottomCorner = if (isLast) 20.dp else 0.dp
    val outerShape = RoundedCornerShape(
        topStart = topCorner,
        topEnd = topCorner,
        bottomStart = bottomCorner,
        bottomEnd = bottomCorner
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(UnitCardBg)
            .padding(
                start = 4.dp,
                top = if (isFirst) 4.dp else 0.dp,
                end = 4.dp,
                bottom = if (isLast) 4.dp else 0.dp
            )
    ) {
        // Blue Top Header Box of Unit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 10.dp, end = 8.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.tr("Unit ${unit.number}: ${Strings.tr(unit.title)}"),
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                UnitJumpPill(
                    currentUnitNumber = unit.number,
                    units = allUnits,
                    onJump = onJump
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = Strings.tr("Task ${unitProgress.done} of ${unitProgress.total}"),
                    color = MutedText,
                    fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = Strings.tr("${unitProgress.percent}% Complete"),
                    color = MutedText,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // White progress track with green progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                if (unitProgress.percent > 0) {
                    val fillFrac = (unitProgress.percent / 100f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillFrac)
                            .clip(CircleShape)
                            .background(ProgressGreen)
                    )
                }
            }
        }

        // Inner White Container holding Unit Title and Serpentine Lesson Trail
        val innerBottomCorner = if (isLast) 16.dp else 0.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = innerBottomCorner, bottomEnd = innerBottomCorner))
                .background(Color.White)
                .padding(top = 18.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Unit ${unit.number}\n${Strings.tr(unit.title)}",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(14.dp))

            LessonTrail(
                lessons = states,
                onLessonClick = onLessonClick
            )
        }
    }
}

@Composable
private fun UnitJumpPill(
    currentUnitNumber: Int,
    units: List<PathCourse.PathUnit>,
    onJump: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Blue)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.tr("Unit $currentUnitNumber"),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = Strings.tr("Unit ${unit.number}: ${Strings.tr(unit.title)}"),
                            color = if (unit.number == currentUnitNumber) Blue else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (unit.number == currentUnitNumber) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onJump(unit.number)
                    }
                )
            }
        }
    }
}

@Composable
private fun LessonTrail(
    lessons: List<PathCourse.LessonState>,
    onLessonClick: (PathCourse.Lesson) -> Unit
) {
    if (lessons.isEmpty()) return

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW * (lessons.size - 1) + NODE + LIP + 52.dp)
    ) {
        val totalWidth = maxWidth

        // Canvas for dashed serpentine path connecting nodes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(
                floatArrayOf(7.dp.toPx(), 6.dp.toPx()),
                0f
            )
            val stroke = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dashEffect
            )

            for (i in 0 until lessons.size - 1) {
                val startY = (ROW * i + NODE / 2).toPx()
                val endY = (ROW * (i + 1) + NODE / 2).toPx()

                val startCenterX = if (i % 2 == 0) SIDE_INSET else totalWidth - SIDE_INSET
                val endCenterX = if ((i + 1) % 2 == 0) SIDE_INSET else totalWidth - SIDE_INSET

                val startOffset = if (i % 2 == 0) (NODE / 2) else -(NODE / 2)
                val endOffset = if (i % 2 == 0) -(NODE / 2) else (NODE / 2)

                var startX = (startCenterX + startOffset).toPx()
                var endX = (endCenterX + endOffset).toPx()

                if (isRtl) {
                    startX = size.width - startX
                    endX = size.width - endX
                }

                val path = Path().apply {
                    moveTo(startX, startY)
                    val midX = (startX + endX) / 2f
                    cubicTo(midX, startY, midX, endY, endX, endY)
                }

                val nextLessonDone = lessons[i + 1].status == PathCourse.Status.DONE
                val pathColor = if (nextLessonDone) Blue else LineGrey
                drawPath(path = path, color = pathColor, style = stroke)
            }
        }

        // Nodes placed on top of path
        lessons.forEachIndexed { index, lessonState ->
            val centerX = if (index % 2 == 0) SIDE_INSET else totalWidth - SIDE_INSET
            val nodeX = centerX - (LABEL_WIDTH / 2)
            val nodeY = ROW * index

            Box(
                modifier = Modifier
                    .offset(x = nodeX, y = nodeY)
                    .width(LABEL_WIDTH),
                contentAlignment = Alignment.TopCenter
            ) {
                LessonNode(
                    state = lessonState,
                    onClick = {
                        if (lessonState.status != PathCourse.Status.LOCKED) {
                            onLessonClick(lessonState.lesson)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LessonNode(
    state: PathCourse.LessonState,
    onClick: () -> Unit
) {
    val look = when (state.status) {
        PathCourse.Status.DONE -> DoneLook
        PathCourse.Status.CURRENT -> CurrentLook
        PathCourse.Status.LOCKED -> LockedLook
    }

    val pulseTransition = rememberInfiniteTransition("nodePulse")
    val pulseHalo by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )

    Column(
        modifier = Modifier.width(LABEL_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(width = NODE + 12.dp, height = NODE + LIP),
            contentAlignment = Alignment.TopCenter
        ) {
            // Pulse glow for current active lesson
            if (state.status == PathCourse.Status.CURRENT) {
                Box(
                    modifier = Modifier
                        .size(NODE)
                        .scale(pulseHalo)
                        .clip(CircleShape)
                        .background(look.face.copy(alpha = 0.22f))
                )
            }

            // Bottom 3D Lip
            Box(
                modifier = Modifier
                    .padding(top = LIP)
                    .size(NODE)
                    .clip(CircleShape)
                    .background(look.lip)
            )

            // Top Face (clickable)
            Box(
                modifier = Modifier
                    .size(NODE)
                    .clip(CircleShape)
                    .background(look.face)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = look.icon,
                    contentDescription = state.lesson.title,
                    modifier = Modifier.size(24.dp),
                    tint = look.tint
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = Strings.tr(state.lesson.title),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (state.status == PathCourse.Status.DONE) {
            val scoreText = if (state.score != null) {
                Strings.tr("Completed · ${state.score}%")
            } else {
                Strings.tr("Completed")
            }
            Text(
                text = scoreText,
                color = DoneGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
