package com.example.basekmp.ui.speaking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.basekmp.data.Strings
import com.example.basekmp.theme.AppTheme

internal class PendingMicrophoneStart {
    var pending by mutableStateOf(false)
        private set

    fun request() { pending = true }
    fun cancel() { pending = false }
    fun takeIfResumed(isResumed: Boolean): Boolean {
        if (!pending || !isResumed) return false
        pending = false
        return true
    }
}

@Composable
actual fun PlatformSpeechRecognition(
    languageTag: String,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onPartialResult: (String) -> Unit,
    onInteractionStateChange: (Boolean) -> Unit,
    content: @Composable (
        isListening: Boolean,
        start: () -> Unit,
        stop: () -> Unit,
        isAvailable: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val isAppResumed = remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val appContext = remember(context) { context.applicationContext }
    val currentLanguageTag by rememberUpdatedState(languageTag)
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnPartialResult by rememberUpdatedState(onPartialResult)
    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        } else {
            null
        }
    }
    val permissionPreferences = remember(appContext) {
        appContext.getSharedPreferences(PERMISSION_PREFERENCES, Context.MODE_PRIVATE)
    }
    var permissionWasRequested by remember(permissionPreferences) {
        mutableStateOf(permissionPreferences.getBoolean(RECORD_AUDIO_REQUESTED, false))
    }
    var isListening by remember { mutableStateOf(false) }
    var recognitionGeneration by remember { mutableIntStateOf(0) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var awaitingPermissionResult by remember { mutableStateOf(false) }
    val startAfterPermission = remember { PendingMicrophoneStart() }

    val startRecognition: () -> Unit = {
        val speechRecognizer = recognizer
        when {
            !isAppResumed.value -> Unit
            speechRecognizer == null -> currentOnError("unavailable")
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED -> {
                currentOnError("permission_required")
                showPermissionRationale = true
            }
            else -> {
                val session = ++recognitionGeneration
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer.setRecognitionListener(object : RecognitionListener {
                    private fun isCurrentSession(): Boolean =
                        isAppResumed.value && recognitionGeneration == session

                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (!isCurrentSession()) return
                        val transcript = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (transcript.isNotBlank()) currentOnPartialResult(transcript)
                    }

                    override fun onResults(results: Bundle?) {
                        if (!isCurrentSession()) return
                        isListening = false
                        val transcript = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (transcript.isBlank()) currentOnError("no_match") else currentOnResult(transcript)
                    }

                    override fun onError(error: Int) {
                        if (!isCurrentSession()) return
                        isListening = false
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            currentOnError("permission_required")
                            val permanentlyDenied = permissionPreferences.getBoolean(RECORD_AUDIO_REQUESTED, false) &&
                                context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false
                            if (permanentlyDenied) showSettingsDialog = true else showPermissionRationale = true
                        } else {
                            currentOnError("no_match")
                        }
                    }
                })
                isListening = true
                runCatching { speechRecognizer.startListening(intent) }
                    .onFailure {
                        recognitionGeneration++
                        isListening = false
                        currentOnError("unavailable")
                    }
            }
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        awaitingPermissionResult = false
        showSettingsDialog = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Activity results can arrive before ON_RESUME; start once the screen is active.
            startAfterPermission.request()
        } else {
            startAfterPermission.cancel()
            currentOnError("permission_required")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        awaitingPermissionResult = false
        permissionWasRequested = true
        if (granted) {
            showPermissionRationale = false
            showSettingsDialog = false
            startAfterPermission.request()
        } else {
            startAfterPermission.cancel()
            isListening = false
            currentOnError("permission_required")
            val permanentlyDenied = permissionPreferences.getBoolean(RECORD_AUDIO_REQUESTED, false) &&
                context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false
            if (permanentlyDenied) {
                showPermissionRationale = false
                showSettingsDialog = true
            } else {
                // Keep the next request behind a clear user action instead of prompting again automatically.
                showPermissionRationale = true
            }
        }
    }

    LaunchedEffect(startAfterPermission.pending, isAppResumed.value) {
        if (startAfterPermission.takeIfResumed(isAppResumed.value)) {
            startRecognition()
        }
    }

    DisposableEffect(recognizer, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppResumed.value = true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    isAppResumed.value = false
                    recognitionGeneration++
                    isListening = false
                    runCatching { recognizer?.cancel() }
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            recognitionGeneration++
            isListening = false
            runCatching { recognizer?.cancel() }
            runCatching { recognizer?.destroy() }
        }
    }

    val start: () -> Unit = {
        when {
            !isAppResumed.value -> Unit
            recognizer == null -> currentOnError("unavailable")
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> startRecognition()
            else -> {
                val permanentlyDenied = permissionWasRequested &&
                    context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false
                if (permanentlyDenied) showSettingsDialog = true else showPermissionRationale = true
            }
        }
    }
    val stop: () -> Unit = {
        if (isListening) runCatching { recognizer?.stopListening() }
    }

    if (showPermissionRationale) {
        MicrophonePermissionDialog(
            title = Strings.tr("Microphone permission is required"),
            message = Strings.tr(
                "Microphone access lets Android recognize your speech and show the transcript for this practice. It starts only when you tap the mic."
            ),
            dismissLabel = Strings.tr("Cancel"),
            confirmLabel = Strings.tr("Continue"),
            onDismiss = { showPermissionRationale = false },
            onConfirm = {
                    showPermissionRationale = false
                    permissionPreferences.edit().putBoolean(RECORD_AUDIO_REQUESTED, true).apply()
                    permissionWasRequested = true
                    awaitingPermissionResult = true
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }

    if (showSettingsDialog) {
        MicrophonePermissionDialog(
            title = Strings.tr("Microphone permission is required"),
            message = Strings.tr("Enable microphone access in Settings to use voice practice."),
            dismissLabel = Strings.tr("Cancel"),
            confirmLabel = Strings.tr("Open Settings"),
            onDismiss = { showSettingsDialog = false },
            onConfirm = {
                    showSettingsDialog = false
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    awaitingPermissionResult = true
                    runCatching { settingsLauncher.launch(intent) }
                        .onFailure {
                            awaitingPermissionResult = false
                            currentOnError("permission_required")
                        }
            }
        )
    }

    SideEffect {
        onInteractionStateChange(isListening || showPermissionRationale || showSettingsDialog || awaitingPermissionResult || startAfterPermission.pending)
    }
    content(isListening, start, stop, recognizer != null)
}

@Composable
private fun MicrophonePermissionDialog(
    title: String,
    message: String,
    dismissLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(24.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .border(1.dp, Color(0xFFE7ECF4), shape),
            shape = shape,
            color = colors.surfaceCard,
            shadowElevation = 20.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color(0xFFEDF3FF)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = Color(0xFF2B5CFF)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = title,
                        color = Color(0xFF14161B),
                        fontSize = 19.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color(0xFF475467),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF475467)),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Text(
                            text = dismissLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2B5CFF),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        Text(
                            text = confirmLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private const val PERMISSION_PREFERENCES = "speech_permission_state"
private const val RECORD_AUDIO_REQUESTED = "record_audio_requested"

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
