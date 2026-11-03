package com.example.basekmp.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.setting_goal
import ailearn.shared.generated.resources.setting_learn
import ailearn.shared.generated.resources.setting_level
import ailearn.shared.generated.resources.setting_native
import com.example.basekmp.data.EnglishLevel
import com.example.basekmp.data.Languages
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.DividerColor
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.ui.components.SelectionDialog
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private data class PickerSpec(
    val title: String,
    val options: List<String>,
    val selected: String,
    val onSelect: (String) -> Unit
)

private data class SettingItem(
    val label: String,
    val icon: DrawableResource? = null,
    val value: String? = null,
    val onClick: () -> Unit
)

@Composable
fun SettingsScreen(
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToLegal: (String) -> Unit = {},
    onContactSupport: () -> Unit = {},
    onShareApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activePicker by remember { mutableStateOf<PickerSpec?>(null) }
    var currentEnglishLevel by remember { mutableStateOf(EnglishLevel.toFullName(UserPrefs.englishLevel)) }
    var currentDailyGoal by remember { mutableStateOf(UserPrefs.getDailyGoal()) }
    var currentLearnLang by remember { mutableStateOf(Languages.learn(UserPrefs.getLearnLanguage()).displayName) }
    var currentNativeLang by remember { mutableStateOf(Languages.nativeFor(UserPrefs.nativeLanguage).displayName) }
    val currentInterfaceLang = Strings.languages.firstOrNull { it.code == Strings.getLanguage() }?.name.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = Strings.tr("Settings"),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))

            // 1. Profile Section
            SectionLabel(Strings.tr("Profile"))

            SettingsGroup(
                items = listOf(
                    SettingItem(
                        label = Strings.tr("Learn Language"),
                        icon = Res.drawable.setting_learn,
                        value = currentLearnLang,
                        onClick = {
                            activePicker = PickerSpec(
                                title = "Select Learn Language",
                                options = Languages.learnable.map { it.displayName },
                                selected = currentLearnLang,
                                onSelect = { selectedName ->
                                    val lang = Languages.learn(selectedName)
                                    UserPrefs.setLearnLanguage(lang.code)
                                    currentLearnLang = lang.displayName
                                }
                            )
                        }
                    ),
                    SettingItem(
                        label = Strings.tr("Level"),
                        icon = Res.drawable.setting_level,
                        value = currentEnglishLevel,
                        onClick = {
                            activePicker = PickerSpec(
                                title = "Select Level",
                                options = EnglishLevel.ALL_SETTINGS,
                                selected = currentEnglishLevel,
                                onSelect = { selectedLevel ->
                                    UserPrefs.englishLevel = selectedLevel
                                    currentEnglishLevel = EnglishLevel.toFullName(selectedLevel)
                                }
                            )
                        }
                    ),
                    SettingItem(
                        label = Strings.tr("Native Language"),
                        icon = Res.drawable.setting_native,
                        value = currentNativeLang,
                        onClick = {
                            activePicker = PickerSpec(
                                title = "Select Native Language",
                                options = Languages.native.map { it.displayName },
                                selected = currentNativeLang,
                                onSelect = { selectedName ->
                                    val lang = Languages.nativeFor(selectedName)
                                    UserPrefs.nativeLanguage = lang.code
                                    currentNativeLang = lang.displayName
                                }
                            )
                        }
                    ),
                    SettingItem(
                        label = Strings.tr("Daily Goal"),
                        icon = Res.drawable.setting_goal,
                        value = currentDailyGoal,
                        onClick = {
                            activePicker = PickerSpec(
                                title = "Select Daily Goal",
                                options = listOf(
                                    "5 min / day",
                                    "10 min / day",
                                    "15 min / day",
                                    "20 min / day",
                                    "30 min / day"
                                ),
                                selected = currentDailyGoal,
                                onSelect = { selectedGoal ->
                                    UserPrefs.setDailyGoal(selectedGoal)
                                    currentDailyGoal = selectedGoal
                                }
                            )
                        }
                    )
                )
            )

            // 2. Appearance Section
            SectionLabel(Strings.tr("Appearance"))

            SettingsGroup(
                items = listOf(
                    SettingItem(
                        label = Strings.tr("Interface Language"),
                        icon = null,
                        value = currentInterfaceLang,
                        onClick = onNavigateToLanguage
                    )
                )
            )

            // 3. Help & Support Section
            SectionLabel(Strings.tr("Help & Support"))

            SettingsGroup(
                items = listOf(
                    SettingItem(
                        label = Strings.tr("Contact Support"),
                        icon = null,
                        value = null,
                        onClick = onContactSupport
                    )
                )
            )

            // 4. Legal Section
            SectionLabel(Strings.tr("Legal"))

            SettingsGroup(
                items = listOf(
                    SettingItem(
                        label = Strings.tr("Privacy Policy"),
                        icon = null,
                        value = null,
                        onClick = { onNavigateToLegal("privacy") }
                    ),
                    SettingItem(
                        label = Strings.tr("Terms & Conditions"),
                        icon = null,
                        value = null,
                        onClick = { onNavigateToLegal("terms") }
                    )
                )
            )

            // 5. Invite Friends Section
            SectionLabel(Strings.tr("Invite Friends"))

            SettingsGroup(
                items = listOf(
                    SettingItem(
                        label = Strings.tr("Share App"),
                        icon = null,
                        value = null,
                        onClick = onShareApp
                    )
                )
            )

            Spacer(Modifier.height(100.dp)) // Bottom Nav clearance
        }
    }

    // Modal Selection Dialog
    activePicker?.let { picker ->
        SelectionDialog(
            title = picker.title,
            options = picker.options,
            selected = picker.selected,
            onSelect = { option ->
                picker.onSelect(option)
                activePicker = null
            },
            onDismiss = { activePicker = null }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp),
        color = TextSecondary,
        fontSize = 13.sp
    )
}

@Composable
private fun SettingsGroup(items: List<SettingItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OptionSurface)
    ) {
        items.forEachIndexed { index, item ->
            SettingRow(item)
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp,
                    color = DividerColor
                )
            }
        }
    }
}

@Composable
private fun SettingRow(item: SettingItem) {
    val stackValueBelowLabel = LocalDensity.current.fontScale >= 1.3f && item.value != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.icon != null) {
            Image(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                color = TextPrimary,
                fontSize = 15.sp,
                maxLines = if (stackValueBelowLabel) 3 else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (stackValueBelowLabel) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Strings.tr(item.value.orEmpty()),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (item.value != null && !stackValueBelowLabel) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = Strings.tr(item.value),
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
