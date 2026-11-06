package com.semavia.ailearn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semavia.ailearn.data.Strings
import com.semavia.ailearn.theme.OptionSurface
import com.semavia.ailearn.theme.TextPrimary

internal val StandardBackButtonInset = 20.dp

/**
 * Standard top header bar for primary tabs.
 */
@Composable
fun TabHeader(
    title: String,
    modifier: Modifier = Modifier,
    onTitleTripleTap: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    leadingAction: (@Composable () -> Unit)? = null,
    titleSize: TextUnit = 20.sp
) {
    val sideSlot = if (leadingAction != null || trailing != null) 72.dp else 12.dp
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(horizontal = sideSlot),
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (leadingAction != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart).size(width = 72.dp, height = 56.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                leadingAction()
            }
        }

        if (trailing != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).size(width = 72.dp, height = 56.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                trailing()
            }
        }
    }
}

/**
 * Circular icon button matching CircleIconButton in TopBarsKt.java.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = OptionSurface,
    tint: Color = TextPrimary,
    iconSize: Int = 18
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize.dp),
            tint = tint
        )
    }
}

/** Standard 40 dp back control used by every navigable screen header. */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = OptionSurface,
    tint: Color = TextPrimary
) {
    CircleIconButton(
        icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
        contentDescription = Strings.tr("Back"),
        onClick = onClick,
        modifier = modifier,
        background = background,
        tint = tint
    )
}

/** Standard 40 dp close control for lesson and modal headers. */
@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = OptionSurface,
    tint: Color = TextPrimary
) {
    CircleIconButton(
        icon = Icons.Rounded.Close,
        contentDescription = Strings.tr("Close"),
        onClick = onClick,
        modifier = modifier,
        background = background,
        tint = tint
    )
}

/**
 * Standard Back navigation title bar.
 */
@Composable
fun BackTitleBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    rightContent: (@Composable () -> Unit)? = null,
    titleSize: TextUnit = 18.sp,
    titleColor: Color = TextPrimary,
    buttonBackground: Color = OptionSurface,
    buttonTint: Color = TextPrimary,
    minHeight: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = StandardBackButtonInset)
            .heightIn(min = minHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp),
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            modifier = Modifier.align(Alignment.CenterStart).size(width = 56.dp, height = 56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BackButton(
                onClick = onBack,
                background = buttonBackground,
                tint = buttonTint
            )
        }
        Box(
            modifier = Modifier.align(Alignment.CenterEnd).size(width = 56.dp, height = 56.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            rightContent?.invoke()
        }
    }
}

/**
 * Standard Close navigation title bar.
 */
@Composable
fun CloseTitleBar(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 18.sp,
    titleColor: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = StandardBackButtonInset)
            .heightIn(min = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp),
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            modifier = Modifier.align(Alignment.CenterStart).size(width = 56.dp, height = 56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            CloseButton(onClick = onClose)
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd).size(56.dp))
    }
}
