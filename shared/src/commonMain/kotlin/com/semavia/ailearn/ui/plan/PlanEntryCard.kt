package com.semavia.ailearn.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.nav_learning
import ailearn.shared.generated.resources.plan_route
import com.semavia.ailearn.data.Strings
import com.semavia.ailearn.theme.BrandPrimary
import com.semavia.ailearn.theme.Grey
import com.semavia.ailearn.theme.LineBlue
import com.semavia.ailearn.theme.TextPrimary
import com.semavia.ailearn.theme.TileBlue
import org.jetbrains.compose.resources.painterResource

/**
 * Tile icon used in Learning Path components matching decompiled PlanEntryCardKt.java.
 */
@Composable
fun PathTile(size: Int, iconSize: Int, corner: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(TileBlue),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.nav_learning),
            contentDescription = null,
            modifier = Modifier.size(iconSize.dp),
            tint = BrandPrimary
        )
    }
}

/**
 * TopBar Plan path shortcut button with "My Path" and optional "New" badge.
 */
@Composable
fun PlanPathButton(
    isNew: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PathTile(size = 32, iconSize = 20, corner = 9)
            Text(
                text = Strings.tr("My Path"),
                modifier = Modifier.width(56.dp),
                color = BrandPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isNew) {
            Text(
                text = Strings.tr("New"),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-5).dp, y = (-5).dp)
                    .clip(RoundedCornerShape(50))
                    .background(BrandPrimary)
                    .border(1.dp, Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                color = Color.White,
                fontSize = 7.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Pill-shaped action button with right chevron arrow.
 */
@Composable
fun BluePill(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BrandPrimary)
            .padding(start = 12.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White
        )
    }
}

/**
 * Main "Your Learning Path" entry card shown on Home screen.
 */
@Composable
fun PlanEntryCard(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TileBlue)
            .border(1.dp, LineBlue, shape)
            .clickable { onCreate() }
            .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.plan_route),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = BrandPrimary
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(LineBlue)
        )
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = Strings.tr("Your Learning Path"),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = Strings.tr("Personalized for you"),
                color = Grey,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        BluePill(text = Strings.tr("Create"))
    }
}
