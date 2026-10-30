package com.example.basekmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.ic_premium_diamond
import com.example.basekmp.data.Strings
import com.example.basekmp.theme.Blue
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.TextPrimary
import org.jetbrains.compose.resources.painterResource

/**
 * Standard top header bar for primary tabs matching decompiled TopBarsKt.java.
 */
@Composable
fun TabHeader(
    title: String,
    onPremiumClick: (() -> Unit)? = {},
    modifier: Modifier = Modifier,
    onTitleTripleTap: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    leadingAction: (@Composable () -> Unit)? = null,
    titleSize: TextUnit = 20.sp,
    isPro: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.weight(1f))

        if (leadingAction != null) {
            leadingAction()
            if (!isPro && onPremiumClick != null) {
                Spacer(Modifier.width(10.dp))
            }
        }

        if (!isPro && onPremiumClick != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Blue)
                    .clickable { onPremiumClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_premium_diamond),
                    contentDescription = Strings.tr("Premium"),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
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

/**
 * Standard Back navigation title bar.
 */
@Composable
fun BackTitleBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backIcon: ImageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
    rightContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(
            icon = backIcon,
            contentDescription = Strings.tr("Back"),
            onClick = onBack
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (rightContent != null) {
            rightContent()
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = Strings.tr("Close"),
            onClick = onClose
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
