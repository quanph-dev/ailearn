package com.example.basekmp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import basekmp.shared.generated.resources.Res
import basekmp.shared.generated.resources.bg_onboard1
import basekmp.shared.generated.resources.bg_onboard2
import basekmp.shared.generated.resources.bg_onboard3
import basekmp.shared.generated.resources.cd_back
import basekmp.shared.generated.resources.flag_brazil
import basekmp.shared.generated.resources.flag_english
import basekmp.shared.generated.resources.flag_france
import basekmp.shared.generated.resources.flag_germany
import basekmp.shared.generated.resources.flag_india
import basekmp.shared.generated.resources.flag_indonesia
import basekmp.shared.generated.resources.flag_portuguese
import basekmp.shared.generated.resources.flag_saudi
import basekmp.shared.generated.resources.flag_spain
import basekmp.shared.generated.resources.flag_vietnam
import basekmp.shared.generated.resources.inter_bold
import basekmp.shared.generated.resources.inter_regular
import basekmp.shared.generated.resources.inter_semibold
import basekmp.shared.generated.resources.language_title
import basekmp.shared.generated.resources.language_confirm
import basekmp.shared.generated.resources.language_save_error
import basekmp.shared.generated.resources.network_offline_message
import basekmp.shared.generated.resources.network_offline_title
import basekmp.shared.generated.resources.network_try_again
import basekmp.shared.generated.resources.ui_are_you_sure_you_want_to_exit_the_app_now
import basekmp.shared.generated.resources.ui_exit_the_app
import basekmp.shared.generated.resources.ui_no
import basekmp.shared.generated.resources.ui_yes_exit
import basekmp.shared.generated.resources.home_description
import basekmp.shared.generated.resources.home_welcome
import basekmp.shared.generated.resources.onboarding_next
import basekmp.shared.generated.resources.onboarding_start
import basekmp.shared.generated.resources.onboarding_subtitle_1
import basekmp.shared.generated.resources.onboarding_subtitle_2
import basekmp.shared.generated.resources.onboarding_subtitle_3
import basekmp.shared.generated.resources.onboarding_title_1
import basekmp.shared.generated.resources.onboarding_title_2
import basekmp.shared.generated.resources.onboarding_title_3
import basekmp.shared.generated.resources.settings_general_language
import basekmp.shared.generated.resources.splash_loading

private val InkBlack = Color(0xFF0A0C10)
private val DeepSurface = Color(0xFF111318)
private val SurfaceContainer = Color(0xFF1E2024)
private val NeonCyan = Color(0xFF00F0FF)
private val RoyalBlue = Color(0xFF0266FF)
private val TextPrimary = Color(0xFFE2E2E8)
private val TextSecondary = Color(0xFFB9CACB)

@Composable
fun App(
    viewModel: BaseAppViewModel,
    onLanguageApplied: () -> Unit = {},
    onExitApp: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val networkConnection = rememberNetworkConnectionState()
    val isOnline by networkConnection.isOnline
    var hasConfirmedConnection by remember(networkConnection) {
        mutableStateOf(networkConnection.isOnline.value)
    }
    LaunchedEffect(isOnline) {
        if (!isOnline) hasConfirmedConnection = false
    }
    val canUseApp = isOnline && hasConfirmedConnection
    var appliedLanguageCode by remember { mutableStateOf<String?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(canUseApp) {
        if (!canUseApp) showExitConfirmation = false
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is BaseAppEvent.LanguageApplied) {
                onLanguageApplied()
            }
        }
    }
    LaunchedEffect(uiState.languageCode) {
        applyPlatformLanguage(uiState.languageCode)
        appliedLanguageCode = uiState.languageCode
    }
    LaunchedEffect(uiState.route, canUseApp) {
        if (!canUseApp) return@LaunchedEffect
        when (uiState.route) {
            BaseRoutes.ApplyLanguage -> {
                delay(APPLY_LANGUAGE_SCREEN_DURATION_MS)
                viewModel.finishApplyLanguage()
            }

            BaseRoutes.Home -> viewModel.onHomeShown()
        }
    }
    BackActionHandler(enabled = canUseApp && uiState.route == BaseRoutes.LanguageSettings) {
        viewModel.closeLanguageSettings()
    }
    BackActionHandler(enabled = canUseApp && uiState.route == BaseRoutes.Onboarding) {
        viewModel.advanceOnboarding()
    }
    BackActionHandler(
        enabled = canUseApp && uiState.route in setOf(
            BaseRoutes.Splash,
            BaseRoutes.StartupLanguage,
            BaseRoutes.ApplyLanguage,
            BaseRoutes.Home
        )
    ) {
        showExitConfirmation = true
    }

    if (appliedLanguageCode == uiState.languageCode) {
        PlatformLocaleProvider(uiState.languageCode) {
            BaseTheme {
                AppRootContainer(
                    canUseApp = canUseApp,
                    onTryAgain = {
                        networkConnection.refresh()
                        if (networkConnection.isOnline.value) hasConfirmedConnection = true
                    },
                    showExitConfirmation = showExitConfirmation,
                    onDismissExitConfirmation = { showExitConfirmation = false },
                    onExitApp = onExitApp
                ) {
                    when (uiState.route) {
                        BaseRoutes.Splash -> SplashScreen()
                        BaseRoutes.StartupLanguage,
                        BaseRoutes.LanguageSettings -> LanguageScreen(uiState, viewModel)

                        BaseRoutes.ApplyLanguage -> LoadingScreen()
                        BaseRoutes.Onboarding -> OnboardingScreen(uiState, viewModel)
                        else -> HomeScreen(viewModel)
                    }
                }
            }
        }
    } else {
        BaseTheme {
            AppRootContainer(
                canUseApp = canUseApp,
                onTryAgain = {
                    networkConnection.refresh()
                    if (networkConnection.isOnline.value) hasConfirmedConnection = true
                },
                showExitConfirmation = showExitConfirmation,
                onDismissExitConfirmation = { showExitConfirmation = false },
                onExitApp = onExitApp
            ) {
                Box(
                    Modifier.fillMaxSize().background(DeepSurface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }
        }
    }
}

@Composable
private fun AppRootContainer(
    canUseApp: Boolean,
    onTryAgain: () -> Unit,
    showExitConfirmation: Boolean,
    onDismissExitConfirmation: () -> Unit,
    onExitApp: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        content()
        if (showExitConfirmation && canUseApp) {
            ExitConfirmationDialog(
                onDismissRequest = onDismissExitConfirmation,
                onExitApp = onExitApp
            )
        }
        if (!canUseApp) {
            NetworkUnavailableDialog(onTryAgain = onTryAgain)
        }
    }
}

@Composable
private fun ExitConfirmationDialog(
    onDismissRequest: () -> Unit,
    onExitApp: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(SurfaceContainer, DeepSurface)))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.ui_exit_the_app),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.ui_are_you_sure_you_want_to_exit_the_app_now),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    ExitSecondaryButton(
                        text = stringResource(Res.string.ui_no),
                        onClick = onDismissRequest
                    )
                }
                Box(Modifier.weight(1f)) {
                    ExitPrimaryButton(
                        text = stringResource(Res.string.ui_yes_exit),
                        onClick = onExitApp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitSecondaryButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(DeepSurface)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExitPrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(RoyalBlue, NeonCyan)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NetworkUnavailableDialog(onTryAgain: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = stringResource(Res.string.network_offline_title),
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.network_offline_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onTryAgain) {
                Text(
                    text = stringResource(Res.string.network_try_again),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        containerColor = SurfaceContainer
    )
}

@Composable
private fun BaseTheme(content: @Composable () -> Unit) {
    val fontFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold)
    )
    val baseTypography = Typography()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00DBE9),
            onPrimary = Color(0xFF002022),
            background = DeepSurface,
            onBackground = TextPrimary,
            surface = DeepSurface,
            onSurface = TextPrimary,
            surfaceVariant = Color(0xFF282A2E),
            onSurfaceVariant = TextSecondary,
            outlineVariant = Color(0xFF3B494B)
        ),
        typography = Typography(
            displayMedium = baseTypography.displayMedium.copy(fontFamily = fontFamily, fontSize = 32.sp),
            headlineMedium = baseTypography.headlineMedium.copy(fontFamily = fontFamily, fontSize = 24.sp),
            headlineSmall = baseTypography.headlineSmall.copy(fontFamily = fontFamily, fontSize = 20.sp),
            titleMedium = baseTypography.titleMedium.copy(fontFamily = fontFamily, fontSize = 16.sp),
            bodyLarge = baseTypography.bodyLarge.copy(fontFamily = fontFamily, fontSize = 16.sp),
            bodyMedium = baseTypography.bodyMedium.copy(fontFamily = fontFamily, fontSize = 14.sp)
        ),
        content = content
    )
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(InkBlack, DeepSurface, Color(0xFF0D1821))))
            .safeContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(RoyalBlue, NeonCyan))),
                contentAlignment = Alignment.Center
            ) {
                Text("W", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.splash_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        Modifier.fillMaxSize().background(DeepSurface).safeContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NeonCyan)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.splash_loading), color = TextSecondary)
        }
    }
}

@Composable
private fun LanguageScreen(state: BaseAppUiState, viewModel: BaseAppViewModel) {
    val settingsMode = state.route == BaseRoutes.LanguageSettings
    var canConfirm by remember(state.route, state.selectedLanguageCode) {
        mutableStateOf(settingsMode)
    }
    LaunchedEffect(state.route, state.selectedLanguageCode, state.hasUserSelectedLanguage) {
        canConfirm = settingsMode
        if (!settingsMode && state.hasUserSelectedLanguage) {
            delay(BaseAppViewModel.LANGUAGE_CONFIRM_DELAY_MS)
            canConfirm = true
        }
    }
    val languages = remember(viewModel.deviceLanguageCode) { sortedBaseLanguages(viewModel.deviceLanguageCode) }
    val confirmAccessibilityLabel = stringResource(Res.string.language_confirm)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSurface)
            .safeContentPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settingsMode) {
                Text(
                    text = "‹  ${stringResource(Res.string.cd_back)}",
                    color = TextPrimary,
                    modifier = Modifier.clickable { viewModel.closeLanguageSettings() }
                )
                Spacer(Modifier.width(16.dp))
            }
            Text(
                text = stringResource(Res.string.language_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(
                visible = state.hasUserSelectedLanguage && canConfirm,
                enter = fadeIn(tween(350)),
                exit = fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .semantics { contentDescription = confirmAccessibilityLabel }
                        .clickable { viewModel.confirmLanguage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = NeonCyan, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(languages, key = { it.code }) { option ->
                val selected = option.code == state.selectedLanguageCode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceContainer)
                        .border(
                            width = 1.dp,
                            color = if (selected) NeonCyan else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { viewModel.selectLanguage(option.code) }
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(flagFor(option)),
                        contentDescription = null,
                        modifier = Modifier.size(width = 48.dp, height = 32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(option.nativeName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(option.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Text("✓", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (state.hasError) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(Res.string.language_save_error), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun flagFor(language: BaseLanguage): DrawableResource = when (language.flagName) {
    "flag_english" -> Res.drawable.flag_english
    "flag_france" -> Res.drawable.flag_france
    "flag_saudi" -> Res.drawable.flag_saudi
    "flag_indonesia" -> Res.drawable.flag_indonesia
    "flag_spain" -> Res.drawable.flag_spain
    "flag_germany" -> Res.drawable.flag_germany
    "flag_vietnam" -> Res.drawable.flag_vietnam
    "flag_india" -> Res.drawable.flag_india
    "flag_portuguese" -> Res.drawable.flag_portuguese
    else -> Res.drawable.flag_brazil
}

@Composable
private fun OnboardingScreen(state: BaseAppUiState, viewModel: BaseAppViewModel) {
    val pageIndex = state.onboardingPage.coerceIn(0, BaseAppViewModel.ONBOARDING_PAGE_COUNT - 1)
    val title = when (pageIndex) {
        0 -> stringResource(Res.string.onboarding_title_1)
        1 -> stringResource(Res.string.onboarding_title_2)
        else -> stringResource(Res.string.onboarding_title_3)
    }
    val description = when (pageIndex) {
        0 -> stringResource(Res.string.onboarding_subtitle_1)
        1 -> stringResource(Res.string.onboarding_subtitle_2)
        else -> stringResource(Res.string.onboarding_subtitle_3)
    }
    val art = when (pageIndex) {
        0 -> Res.drawable.bg_onboard1
        1 -> Res.drawable.bg_onboard2
        else -> Res.drawable.bg_onboard3
    }
    val isMiddlePage = pageIndex == 1

    Column(
        modifier = Modifier.fillMaxSize().background(DeepSurface).safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Image(painterResource(art), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, DeepSurface.copy(alpha = 0.92f))
                    )
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonCyan,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))
                Text(description, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
            }
        }
        if (isMiddlePage) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(pageIndex)
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    text = stringResource(Res.string.onboarding_next).uppercase(),
                    onClick = viewModel::advanceOnboarding
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageIndicator(pageIndex)
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (pageIndex == BaseAppViewModel.ONBOARDING_PAGE_COUNT - 1) {
                        stringResource(Res.string.onboarding_start).uppercase()
                    } else stringResource(Res.string.onboarding_next).uppercase(),
                    modifier = Modifier.clickable { viewModel.advanceOnboarding() },
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Black
                )
            }
        }
        if (state.hasError) {
            Text(
                stringResource(Res.string.language_save_error),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PageIndicator(index: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(BaseAppViewModel.ONBOARDING_PAGE_COUNT) { page ->
            Box(
                Modifier
                    .size(width = if (page == index) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (page == index) NeonCyan else Color.White.copy(alpha = 0.28f))
            )
        }
    }
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(RoyalBlue, NeonCyan)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeScreen(viewModel: BaseAppViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().background(DeepSurface).safeContentPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(Res.string.home_welcome), style = MaterialTheme.typography.headlineMedium, color = NeonCyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.home_description), style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainer).clickable { viewModel.openLanguageSettings() }.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.settings_general_language), color = TextPrimary, modifier = Modifier.weight(1f))
            Text("›", color = NeonCyan, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

private const val APPLY_LANGUAGE_SCREEN_DURATION_MS = 350L
