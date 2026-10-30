package com.example.basekmp.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.data.Strings
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.theme.Blue
import com.example.basekmp.theme.OptionSurface

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    val gradient = if (enabled) {
        colors.primaryBrush
    } else {
        Brush.horizontalGradient(listOf(colors.primary.copy(alpha = 0.4f), colors.secondary.copy(alpha = 0.4f)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.primaryButtonHeight)
            .clip(RoundedCornerShape(dimens.cornerPill))
            .background(gradient)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimens.iconMd),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                color = if (enabled) Color.White else colors.textSecondary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class SelectOption(
    val id: String,
    val label: String,
    val leading: (@Composable () -> Unit)? = null
)

@Composable
fun OptionCard(
    options: List<SelectOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
    ) {
        options.forEach { option ->
            val isSelected = option.id == selectedId
            val borderColor by animateColorAsState(if (isSelected) colors.borderActive else colors.border)
            val bgGradient = if (isSelected) {
                Brush.horizontalGradient(listOf(colors.surface, colors.primaryGradientStart.copy(alpha = 0.25f)))
            } else {
                Brush.horizontalGradient(listOf(colors.surface, colors.surface))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.primaryButtonHeight)
                    .clip(RoundedCornerShape(dimens.cornerMd))
                    .border(1.5.dp, borderColor, RoundedCornerShape(dimens.cornerMd))
                    .background(bgGradient)
                    .clickable { onSelect(option.id) }
                    .padding(horizontal = dimens.spacingLg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (option.leading != null) {
                    option.leading.invoke()
                    Spacer(Modifier.width(dimens.spacingMd))
                }
                Text(
                    text = option.label,
                    color = if (isSelected) colors.primary else colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.5f), CircleShape)
                        .background(if (isSelected) colors.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SoundBars(
    color: Color = Blue,
    modifier: Modifier = Modifier,
    height: Dp = 20.dp
) {
    val transition = rememberInfiniteTransition(label = "sound-bars")
    val offsets = listOf(0, 180, 90, 270)
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        offsets.forEach { offsetMs ->
            val scale by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offsetMs)
                ),
                label = "bar"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height * scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun ListenButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isLoading: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(OptionSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Blue,
                        strokeWidth = 2.5.dp
                    )
                }
                isPlaying -> {
                    SoundBars(color = Blue, height = 20.dp)
                }
                else -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = Strings.tr("Listen"),
                        modifier = Modifier.size(24.dp),
                        tint = Blue
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = when {
                isLoading -> Strings.tr("Loading…")
                isPlaying -> Strings.tr("Playing…")
                else -> Strings.tr("Tap to listen")
            },
            color = Blue,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun HoldToSpeakMic(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(
                Modifier
                    .size(76.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.25f))
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(colors.primary, colors.primaryGradientStart)))
                .clickable {
                    if (isRecording) onStopRecording() else onStartRecording()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(if (isRecording) "⏹" else "🎙️", fontSize = 24.sp, color = Color.White)
        }
    }
}

@Composable
fun ProgressHeader(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Row(
        modifier = modifier.fillMaxWidth().height(dimens.topBarHeight).padding(horizontal = dimens.spacingLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(dimens.minTouchTarget).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", fontSize = 32.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(dimens.spacingMd))
        Box(
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.surface)
        ) {
            val progress = (currentStep.toFloat() / totalSteps.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.fillMaxWidth(progress).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(colors.primaryBrush)
            )
        }
        if (onClose != null) {
            Spacer(Modifier.width(dimens.spacingMd))
            Box(
                modifier = Modifier.size(dimens.minTouchTarget).clip(CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 16.sp, color = colors.textSecondary)
            }
        }
    }
}
