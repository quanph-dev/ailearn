package com.example.basekmp.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.fo_ic_logo_app
import ailearn.shared.generated.resources.fo_splash_describe_load_ad
import ailearn.shared.generated.resources.fo_splash_title
import com.example.basekmp.theme.Blue
import com.example.basekmp.theme.OptionSurface
import com.example.basekmp.theme.getInterFontFamily
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SplashScreen() {
    val inter = getInterFontFamily()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(Res.drawable.fo_ic_logo_app),
                    contentDescription = null,
                    modifier = Modifier.size(104.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = stringResource(Res.string.fo_splash_title),
                    color = Color(0xFF222222),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = inter,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = stringResource(Res.string.fo_splash_describe_load_ad),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            color = Color(0xFF9AA0A6),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = inter,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = Blue,
            trackColor = OptionSurface
        )
    }
}
