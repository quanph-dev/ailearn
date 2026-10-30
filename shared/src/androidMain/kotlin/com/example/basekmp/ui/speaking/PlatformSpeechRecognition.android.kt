package com.example.basekmp.ui.speaking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun PlatformSpeechRecognition(
    languageTag: String,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (Boolean, () -> Unit, () -> Unit, Boolean) -> Unit
) {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)
    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        } else {
            null
        }
    }
    var isListening by remember { mutableStateOf(false) }

    DisposableEffect(recognizer) {
        if (recognizer != null) {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val transcript = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (transcript.isBlank()) currentOnError("no_match") else currentOnResult(transcript)
                }

                override fun onError(error: Int) {
                    isListening = false
                    currentOnError(if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "permission_required" else "no_match")
                }
            })
        }
        onDispose {
            runCatching { recognizer?.cancel() }
            runCatching { recognizer?.destroy() }
        }
    }

    val start: () -> Unit = {
        when {
            recognizer == null -> currentOnError("unavailable")
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED -> {
                val act = context.findActivity()
                act?.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION)
                currentOnError("permission_required")
            }
            else -> {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                isListening = true
                runCatching { recognizer.startListening(intent) }
                    .onFailure {
                        isListening = false
                        currentOnError("unavailable")
                    }
            }
        }
    }
    val stop: () -> Unit = {
        if (isListening) runCatching { recognizer?.stopListening() }
    }

    content(isListening, start, stop, recognizer != null)
}

private const val REQUEST_AUDIO_PERMISSION = 7101

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
