package com.semavia.ailearn.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.semavia.ailearn.data.Strings
import com.semavia.ailearn.theme.AppTheme
import kotlin.random.Random
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// 1. CongratsDialog
// ---------------------------------------------------------------------------

private val ConfettiColors = listOf(
    Color(0xFF00F0FF),
    Color(0xFF0266FF),
    Color(0xFFFFD600),
    Color(0xFFFF4081),
    Color(0xFF00E676),
    Color(0xFF7C4DFF)
)

private data class ConfettiParticle(
    val xRatio: Float,
    val initialY: Float,
    val size: Float,
    val color: Color,
    val speed: Float
)

@Composable
fun CongratsDialog(
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    onClose: () -> Unit,
    correct: Int? = null,
    incorrect: Int? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val particles = remember {
        List(28) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                initialY = Random.nextFloat() * 40f,
                size = Random.nextFloat() * 6f + 4f,
                color = ConfettiColors[it % ConfettiColors.size],
                speed = Random.nextFloat() * 120f + 60f
            )
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(dimens.cornerXl))
                .background(Brush.verticalGradient(listOf(colors.surface, colors.background)))
                .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                .padding(dimens.spacing2Xl),
            contentAlignment = Alignment.Center
        ) {
            // Confetti Canvas Overlay
            Canvas(modifier = Modifier.matchParentSize()) {
                val t = progressAnim.value
                particles.forEach { p ->
                    val x = p.xRatio * size.width
                    val y = p.initialY + (t * p.speed)
                    drawCircle(
                        color = p.color.copy(alpha = (1f - (t * 0.7f)).coerceIn(0f, 1f)),
                        radius = p.size,
                        center = Offset(x, y)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Close button top right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceVariant)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = colors.textSecondary, fontSize = 14.sp)
                    }
                }

                // Trophy or star icon badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.accentGold.copy(alpha = 0.16f))
                        .border(1.5.dp, colors.accentGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎉", fontSize = 32.sp)
                }

                Spacer(Modifier.height(dimens.spacingLg))

                Text(
                    text = Strings.tr("Congratulations") + if (Strings.getLanguage() == "en") "!" else "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(dimens.spacingSm))

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                // Optional score chips
                if (correct != null && incorrect != null) {
                    Spacer(Modifier.height(dimens.spacingLg))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ResultChip(
                            text = "$correct ${Strings.tr("Correct")}",
                            color = colors.success,
                            bg = colors.successBg
                        )
                        ResultChip(
                            text = "$incorrect ${Strings.tr("Incorrect")}",
                            color = colors.error,
                            bg = colors.errorBg
                        )
                    }
                }

                Spacer(Modifier.height(dimens.spacing2Xl))

                PrimaryButton(
                    text = buttonText,
                    onClick = onButtonClick
                )
            }
        }
    }
}

@Composable
private fun ResultChip(
    text: String,
    color: Color,
    bg: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppTheme.dimens.cornerSm))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(AppTheme.dimens.cornerSm))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
// 2. ExitSessionDialog
// ---------------------------------------------------------------------------

@Composable
fun ExitSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppActionDialog(
        title = Strings.tr("Exit session?"),
        message = Strings.tr("Your progress so far will be saved."),
        onDismissRequest = onDismiss,
        confirmText = Strings.tr("Exit"),
        onConfirm = onConfirm,
        dismissText = Strings.tr("Cancel"),
        onDismissAction = onDismiss,
        confirmIsDestructive = true,
        modifier = modifier
    )
}

/** Shared centered confirmation dialog used for exits and single-action notices. */
@Composable
fun AppActionDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismissAction: (() -> Unit)? = null,
    confirmIsDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 400.dp)
                    .clip(RoundedCornerShape(dimens.cornerXl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                    .padding(dimens.spacing2Xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(dimens.spacingSm))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(dimens.spacing2Xl))

                if (dismissText != null && onDismissAction != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                    ) {
                        ActionDialogButton(
                            text = dismissText,
                            onClick = onDismissAction,
                            isPrimary = false,
                            isDestructive = false,
                            modifier = Modifier.weight(1f)
                        )
                        ActionDialogButton(
                            text = confirmText,
                            onClick = onConfirm,
                            isPrimary = true,
                            isDestructive = confirmIsDestructive,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    ActionDialogButton(
                        text = confirmText,
                        onClick = onConfirm,
                        isPrimary = true,
                        isDestructive = confirmIsDestructive,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionDialogButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    isDestructive: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.dimens.cornerPill)
    val fill = when {
        !isPrimary -> colors.surface
        isDestructive -> colors.error
        else -> colors.primary
    }

    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(fill)
            .then(if (isPrimary) Modifier else Modifier.border(1.dp, colors.border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPrimary) Color.White else colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
// 3. RateUsDialog
// ---------------------------------------------------------------------------

private data class MoodOption(val level: Int, val label: String, val emoji: String)

private val MoodOptions = listOf(
    MoodOption(1, "Worst", "😡"),
    MoodOption(2, "Poor", "🙁"),
    MoodOption(3, "Average", "😐"),
    MoodOption(4, "Good", "😊"),
    MoodOption(5, "Super", "🤩")
)

@Composable
fun RateUsDialog(
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    var selectedMood by remember { mutableIntStateOf(5) }

    val isHighRating = selectedMood >= 4
    val dialogTitle = if (isHighRating) Strings.tr("Enjoying the App?") else Strings.tr("We're sorry!")
    val dialogMessage = if (isHighRating) {
        Strings.tr("Would you mind leaving us a high rating? It helps us immensely!")
    } else {
        Strings.tr("Please let us know how we can improve your learning experience.")
    }
    val actionButtonText = if (isHighRating) Strings.tr("Rate Us") else Strings.tr("Send Feedback")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(dimens.cornerXl))
                .background(Brush.verticalGradient(listOf(colors.surface, colors.background)))
                .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                .padding(dimens.spacing2Xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = colors.textSecondary, fontSize = 14.sp)
                }
            }

            Text(
                text = dialogTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(dimens.spacingSm))

            Text(
                text = dialogMessage,
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(dimens.spacingXl))

            // 5 Mood Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoodOptions.forEach { opt ->
                    val isSelected = opt.level == selectedMood
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.cornerSm))
                            .clickable { selectedMood = opt.level }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) colors.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(opt.emoji, fontSize = 24.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Strings.tr(opt.label),
                            fontSize = 11.sp,
                            color = if (isSelected) colors.primary else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimens.spacing2Xl))

            PrimaryButton(
                text = actionButtonText,
                onClick = {
                    onRate(selectedMood)
                    onDismiss()
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. ReportIssueDialog
// ---------------------------------------------------------------------------

private val DefaultReportCategories = listOf(
    "Inappropriate Content",
    "Incorrect or Misleading Information",
    "Offensive or Unsafe Language",
    "Technical Problem",
    "Other"
)

internal fun canSubmitReport(category: String, details: String): Boolean =
    category != "Other" || details.isNotBlank()

@Composable
fun ReportIssueDialog(
    place: String,
    onDismiss: () -> Unit,
    onSubmit: (category: String, details: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    var selectedCategory by remember { mutableStateOf(DefaultReportCategories.first()) }
    var detailsText by remember { mutableStateOf("") }
    var detailsFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val detailsBringIntoView = remember { BringIntoViewRequester() }
    val requiresDetails = selectedCategory == "Other"
    val canSubmit = canSubmitReport(selectedCategory, detailsText)

    LaunchedEffect(detailsFocused) {
        if (detailsFocused) {
            delay(250)
            detailsBringIntoView.bringIntoView()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .imePadding()
                .clip(RoundedCornerShape(dimens.cornerXl))
                .background(Brush.verticalGradient(listOf(colors.surface, colors.background)))
                .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerXl))
                .verticalScroll(rememberScrollState())
                .padding(dimens.spacing2Xl),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.tr("Report an Issue"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = colors.textSecondary, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(dimens.spacingLg))

            Text(
                text = Strings.tr("Category"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )

            Spacer(Modifier.height(dimens.spacingSm))

            // Categories list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DefaultReportCategories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerSm))
                            .background(if (isSelected) colors.surfaceSelected else colors.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colors.primary else Color.Transparent,
                                shape = RoundedCornerShape(dimens.cornerSm)
                            )
                            .clickable {
                                selectedCategory = cat
                                if (cat != "Other") focusManager.clearFocus()
                            }
                            .padding(horizontal = dimens.spacingMd, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (isSelected) colors.primary else colors.textSecondary, CircleShape)
                                .padding(3.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                )
                            }
                        }
                        Spacer(Modifier.width(dimens.spacingSm))
                        Text(
                            text = Strings.tr(cat),
                            fontSize = 13.sp,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
            }

            if (requiresDetails) {
                Spacer(Modifier.height(dimens.spacingLg))
                Text(
                    text = Strings.tr("Details") + " *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )

                Spacer(Modifier.height(dimens.spacingSm))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .bringIntoViewRequester(detailsBringIntoView)
                        .clip(RoundedCornerShape(dimens.cornerMd))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerMd))
                        .padding(dimens.spacingMd)
                ) {
                    if (detailsText.isEmpty()) {
                        Text(
                            text = Strings.tr("Explain what happened..."),
                            fontSize = 13.sp,
                            color = colors.textTertiary
                        )
                    }
                    BasicTextField(
                        value = detailsText,
                        onValueChange = { detailsText = it },
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(colors.primary),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { detailsFocused = it.isFocused }
                    )
                }
            }

            Spacer(Modifier.height(dimens.spacing2Xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(dimens.primaryButtonHeight)
                        .clip(RoundedCornerShape(dimens.cornerPill))
                        .background(colors.surfaceVariant)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Strings.tr("Cancel"),
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(dimens.primaryButtonHeight)
                        .clip(RoundedCornerShape(dimens.cornerPill))
                        .background(if (canSubmit) colors.primaryBrush else Brush.linearGradient(listOf(colors.surfaceVariant, colors.surfaceVariant)))
                        .clickable(enabled = canSubmit) {
                            onSubmit(selectedCategory, if (requiresDetails) detailsText.trim() else "")
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Strings.tr("Submit"),
                        color = if (canSubmit) Color.White else colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
