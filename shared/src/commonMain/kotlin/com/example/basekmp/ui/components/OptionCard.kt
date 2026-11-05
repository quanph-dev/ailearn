package com.example.basekmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.example.basekmp.data.tr
import com.example.basekmp.theme.BlueSelected
import com.example.basekmp.theme.DividerColor
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.TextPrimary

data class SelectOption(
    val id: String,
    val label: String,
    val leading: (@Composable () -> Unit)? = null
)

object LanguageChoiceMetrics {
    val rowHeight = 72.dp
    val contentVerticalPadding = 8.dp
    val leadingImageSize = 52.dp
    val labelFontSize = 16.sp
    val labelLineHeight = 20.sp
    val horizontalPadding = 16.dp
    val leadingSpacing = 14.dp
    val selectionSize = 22.dp
}

@Composable
fun OptionCard(
    options: List<SelectOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    rowMinHeight: Dp? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OptionSurface)
    ) {
        options.forEachIndexed { index, option ->
            val selected = option.id == selectedId
            OptionRow(
                option = option,
                selected = selected,
                rowMinHeight = rowMinHeight,
                onClick = { onSelect(option.id) }
            )
            if (index < options.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = DividerColor
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    option: SelectOption,
    selected: Boolean,
    rowMinHeight: Dp?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (rowMinHeight == null) Modifier.defaultMinSize(minHeight = 56.dp)
                else Modifier.heightIn(min = rowMinHeight)
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = LanguageChoiceMetrics.horizontalPadding,
                vertical = if (rowMinHeight == null) 12.dp else LanguageChoiceMetrics.contentVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (option.leading != null) {
            option.leading.invoke()
            Spacer(modifier = Modifier.width(LanguageChoiceMetrics.leadingSpacing))
        }
        Text(
            text = option.label,
            modifier = Modifier.weight(1f),
            color = if (selected) BlueSelected else TextPrimary,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = tr("Selected"),
                modifier = Modifier.size(24.dp),
                tint = BlueSelected
            )
        }
    }
}

@Composable
fun CefrBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
