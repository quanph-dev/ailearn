package com.example.basekmp.ui.speaking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.components.HoldToSpeakMic
import com.example.basekmp.data.Languages
import com.example.basekmp.data.PronunciationScorer
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.AppTheme

@Composable
fun SpeechCaptureMic(
    targetText: String,
    instruction: String = Strings.tr("Tap mic and speak the sentence aloud"),
    onResult: (score: Int, transcript: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var error by remember(targetText) { mutableStateOf("") }
    PlatformSpeechRecognition(
        languageTag = Languages.learn(UserPrefs.targetLanguage).speechTag,
        onResult = { transcript ->
            error = ""
            onResult(PronunciationScorer.score(targetText, transcript), transcript)
        },
        onError = { error = it }
    ) { isListening, start, stop, isAvailable ->
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    isListening -> Strings.tr("Listening... release to evaluate")
                    error == "permission_required" -> Strings.tr("Microphone permission is required")
                    error == "no_match" -> Strings.tr("Try speaking a little slower and clearer.")
                    !isAvailable -> Strings.tr("Speech recognition isn't available on this device")
                    else -> instruction
                },
                color = if (isListening) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            HoldToSpeakMic(
                isRecording = isListening,
                onStartRecording = start,
                onStopRecording = stop
            )
        }
    }
}
