package com.example.basekmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.nav_home
import ailearn.shared.generated.resources.nav_learning
import ailearn.shared.generated.resources.nav_practice
import ailearn.shared.generated.resources.nav_progress
import ailearn.shared.generated.resources.nav_settings
import com.example.basekmp.data.Strings
import com.example.basekmp.theme.Blue
import com.example.basekmp.theme.NavBarBg
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val NavBarClearance = 96.dp
val BannerClearance = 64.dp
val tabBottomClearance = 96.dp

enum class MainTab(
    val labelKey: String,
    val icon: DrawableResource
) {
    Home("Home", Res.drawable.nav_home),
    Practice("Practice", Res.drawable.nav_practice),
    Plan("Lesson", Res.drawable.nav_learning),
    Progress("Progress", Res.drawable.nav_progress),
    Settings("Settings", Res.drawable.nav_settings)
}

/**
 * Floating bottom navigation pill bar matching decompiled AppBottomNavKt.java 100%.
 */
@Composable
fun AppBottomNav(
    selectedTab: MainTab,
    onTabSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(50)
    val unselectedColor = Blue.copy(alpha = 0.45f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, pillShape)
            .clip(pillShape)
            .background(NavBarBg)
            .border(1.dp, Blue.copy(alpha = 0.12f), pillShape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val contentColor = if (isSelected) Blue else unselectedColor

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(pillShape)
                    .then(if (isSelected) Modifier.background(Color.White) else Modifier)
                    .clickable { onTabSelect(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(tab.icon),
                    contentDescription = Strings.tr(tab.labelKey),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = Strings.tr(tab.labelKey),
                    color = contentColor,
                    fontSize = 8.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
