package com.example.basekmp.ui.learningpath

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.basekmp.shared.R

@Composable
actual fun PathFeedbackEmoji(isCorrect: Boolean, modifier: Modifier) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(if (isCorrect) R.raw.emoji_party else R.raw.emoji_cry)
    )

    Box(modifier = modifier.size(34.dp), contentAlignment = Alignment.Center) {
        if (composition == null) {
            Text(text = if (isCorrect) "🥳" else "😭", fontSize = 22.sp)
        } else {
            LottieAnimation(
                composition = composition,
                modifier = Modifier.fillMaxSize(),
                iterations = LottieConstants.IterateForever
            )
        }
    }
}
