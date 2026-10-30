package com.example.basekmp.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.data.Strings
import com.example.basekmp.data.UiLanguage
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.theme.BlueSelected
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.ui.components.CircleIconButton

@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    onLanguageSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val languages = Strings.languages
    var currentLang by remember { mutableStateOf(Strings.getLanguage()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                contentDescription = Strings.tr("Back"),
                onClick = onBack
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = Strings.tr("Languages"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(16.dp))

        // Language List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(languages) { lang ->
                val isSelected = lang.code == currentLang
                LanguageRow(
                    uiLanguage = lang,
                    isSelected = isSelected,
                    onClick = {
                        Strings.select(lang.code)
                        UserPrefs.nativeLanguage = lang.code
                        currentLang = lang.code
                        onLanguageSelected(lang.code)
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun LanguageRow(
    uiLanguage: UiLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OptionSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flag pill
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiLanguage.flag,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = uiLanguage.name,
            modifier = Modifier.weight(1f),
            color = TextPrimary,
            fontSize = 15.sp
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(BlueSelected),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = Strings.tr("Selected"),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
