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
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.fo_ic_logo_app
import ailearn.shared.generated.resources.fo_splash_describe_load_ad
import ailearn.shared.generated.resources.fo_splash_title
import com.example.basekmp.theme.BrandPrimary
import com.example.basekmp.theme.TextPrimary
import com.example.basekmp.theme.TextSecondary
import com.example.basekmp.theme.getPlusJakartaFontFamily
import com.example.basekmp.AppAdsHost
import ailearn.shared.generated.resources.ui_ads_keep_free
import com.example.basekmp.data.Strings
import org.jetbrains.compose.resources.painterResource
import com.example.basekmp.data.appStringResource as stringResource
import kotlin.math.PI
import kotlin.math.cos

private val SplashLineEasing = Easing { fraction ->
    ((1.0 - cos(PI * fraction)) * 0.5).toFloat()
}

@Composable
fun SplashScreen(phase: Int = 2, adsHost: AppAdsHost? = null) {
    val plusJakarta = getPlusJakartaFontFamily()

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
                    modifier = Modifier.size(104.dp).clip(RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(if (phase == 1) 24.dp else 36.dp))

                Text(
                    text = stringResource(Res.string.fo_splash_title),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = plusJakarta,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (phase == 1) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = BrandPrimary, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(14.dp))
                Text(
                    text = com.example.basekmp.data.appStringResource(Res.string.ui_ads_keep_free),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontFamily = plusJakarta,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            adsHost?.NativeAd(
                placement = "N_Splash",
                modifier = Modifier.fillMaxWidth()
            )
            val progress by rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1_500, easing = SplashLineEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            ) {
                val barWidth = size.width * 0.4f
                val left = -barWidth + progress * (size.width + barWidth)
                drawRoundRect(
                    color = BrandPrimary,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
        }
    }
}
