package com.semavia.ailearn.ui.shadowing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semavia.ailearn.components.HoldToSpeakMic
import com.semavia.ailearn.data.Languages
import com.semavia.ailearn.data.PronunciationScorer
import com.semavia.ailearn.data.Strings
import com.semavia.ailearn.data.UserPrefs
import com.semavia.ailearn.theme.AppTheme
import com.semavia.ailearn.ui.speaking.PlatformSpeechRecognition

@Composable
internal fun ShadowingSpeechCapture(
    targetText: String,
    onPartialResult: (String) -> Unit,
    onResult: (score: Int, transcript: String) -> Unit,
    onBeforeStartRecording: () -> Unit = {},
    onRecordingStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var error by remember(targetText) { mutableStateOf("") }

    key(targetText) {
        PlatformSpeechRecognition(
            languageTag = Languages.learn(UserPrefs.targetLanguage).speechTag,
            onResult = { transcript ->
                error = ""
                onResult(PronunciationScorer.score(targetText, transcript), transcript)
            },
            onError = { error = it },
            onPartialResult = { transcript ->
                error = ""
                onPartialResult(transcript)
            }
        ) { isListening, start, stop, isAvailable ->
            LaunchedEffect(isListening) {
                onRecordingStateChange(isListening)
            }
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = Strings.tr("Your turn!"),
                    color = AppTheme.colors.primary,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Strings.tr("Repeat the sentence aloud"),
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(10.dp))
                HoldToSpeakMic(
                    isRecording = isListening,
                    onStartRecording = {
                        error = ""
                        onPartialResult("")
                        onBeforeStartRecording()
                        start()
                    },
                    onStopRecording = stop,
                    buttonSize = 96.dp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when {
                        isListening -> Strings.tr("Listening... release to evaluate")
                        error == "permission_required" -> Strings.tr("Microphone permission is required")
                        error == "no_match" -> Strings.tr("Try speaking a little slower and clearer.")
                        !isAvailable -> Strings.tr("Speech recognition isn't available on this device")
                        else -> Strings.tr("Tap or hold to speak")
                    },
                    color = if (isListening) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
