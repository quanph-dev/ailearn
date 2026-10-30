package com.example.basekmp.ui.onboarding

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.fo_img_onboarding_1
import ailearn.shared.generated.resources.fo_img_onboarding_2
import ailearn.shared.generated.resources.fo_img_onboarding_3
import ailearn.shared.generated.resources.fo_onboarding_button_next
import ailearn.shared.generated.resources.fo_onboarding_button_start
import ailearn.shared.generated.resources.fo_onboarding_description_1
import ailearn.shared.generated.resources.fo_onboarding_description_2
import ailearn.shared.generated.resources.fo_onboarding_description_3
import ailearn.shared.generated.resources.fo_onboarding_title_1
import ailearn.shared.generated.resources.fo_onboarding_title_2
import ailearn.shared.generated.resources.fo_onboarding_title_3
import ailearn.shared.generated.resources.fo_survey_continue
import ailearn.shared.generated.resources.fo_survey_skip
import ailearn.shared.generated.resources.fo_survey_subtitle
import ailearn.shared.generated.resources.fo_survey_title
import ailearn.shared.generated.resources.ic_survey_option_1
import ailearn.shared.generated.resources.ic_survey_option_10
import ailearn.shared.generated.resources.ic_survey_option_2
import ailearn.shared.generated.resources.ic_survey_option_3
import ailearn.shared.generated.resources.ic_survey_option_4
import ailearn.shared.generated.resources.ic_survey_option_5
import ailearn.shared.generated.resources.ic_survey_option_6
import ailearn.shared.generated.resources.ic_survey_option_7
import ailearn.shared.generated.resources.ic_survey_option_8
import ailearn.shared.generated.resources.ic_survey_option_9
import ailearn.shared.generated.resources.ic_survey_option_de
import ailearn.shared.generated.resources.ic_survey_option_pt
import ailearn.shared.generated.resources.language_save_error
import com.example.basekmp.data.Languages
import com.example.basekmp.theme.TextPrimary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val SurveyBlue = Color(0xFF4C8DF6)
private val SurveyOptionBackground = Color(0xFFF8F8F8)
private val SurveyText = Color(0xFF222222)
private val SurveySkipBackground = Color(0xFFF3F3F4)
private val SurveyFooterBackground = Color(0xFFFAFAFA)
private val IntroSubtitle = Color(0xFF9AA0A6)

@Composable
fun FirstOpenIntroScreen(
    page: Int,
    onPageSelected: (Int) -> Unit,
    onNext: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = page.coerceIn(0, 2), pageCount = { 3 })
    LaunchedEffect(page) {
        val targetPage = page.coerceIn(0, 2)
        if (pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect(onPageSelected)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            key = { it }
        ) { currentPage ->
            FirstOpenIntroPage(page = currentPage)
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 8.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == index) SurveyBlue else Color(0xFFD5D5D5))
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    if (pagerState.currentPage == 2) Res.string.fo_onboarding_button_start
                    else Res.string.fo_onboarding_button_next
                ),
                color = SurveyBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onNext)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun FirstOpenIntroPage(page: Int) {
    val image = when (page) {
        0 -> Res.drawable.fo_img_onboarding_1
        1 -> Res.drawable.fo_img_onboarding_2
        else -> Res.drawable.fo_img_onboarding_3
    }
    val title = when (page) {
        0 -> Res.string.fo_onboarding_title_1
        1 -> Res.string.fo_onboarding_title_2
        else -> Res.string.fo_onboarding_title_3
    }
    val description = when (page) {
        0 -> Res.string.fo_onboarding_description_1
        1 -> Res.string.fo_onboarding_description_2
        else -> Res.string.fo_onboarding_description_3
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(title),
                color = TextPrimary,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(description),
                color = IntroSubtitle,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun FirstOpenSurveyScreen(
    selectedLanguage: String,
    isSaving: Boolean,
    hasError: Boolean,
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
      Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.fo_survey_title),
                    color = SurveyText,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.fo_survey_subtitle),
                    color = SurveyText,
                    fontSize = 14.sp,
                )
            }
            Text(
                text = stringResource(Res.string.fo_survey_skip).uppercase(),
                color = SurveyText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(start = 12.dp, top = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurveySkipBackground)
                    .clickable(enabled = !isSaving, onClick = onSkip)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Languages.surveyOptions, key = { it.code }) { language ->
                val selected = selectedLanguage == language.code
                val shape = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .clip(shape)
                        .background(SurveyOptionBackground)
                        .clickable(enabled = !isSaving) { onSelectLanguage(language.code) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(surveyIcon(language.code)),
                        contentDescription = null,
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(20.dp))
                    Text(
                        text = language.name,
                        modifier = Modifier.weight(1f),
                        color = SurveyText,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) SurveyBlue else SurveyOptionBackground)
                            .then(
                                if (selected) Modifier
                                else Modifier.border(1.5.dp, Color(0xFFC6C5C9), RoundedCornerShape(6.dp))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (hasError) {
            Text(
                text = stringResource(Res.string.language_save_error),
                color = Color(0xFFB3261E),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
      }
      Box(
            modifier = Modifier.fillMaxWidth().background(SurveyFooterBackground),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (selectedLanguage.isNotBlank()) SurveyBlue else SurveyBlue.copy(alpha = 0.24f))
                    .clickable(enabled = selectedLanguage.isNotBlank() && !isSaving, onClick = onContinue),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(Res.string.fo_survey_continue).uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun surveyIcon(code: String): DrawableResource = when (code) {
    "en" -> Res.drawable.ic_survey_option_1
    "es" -> Res.drawable.ic_survey_option_2
    "fr" -> Res.drawable.ic_survey_option_3
    "ar" -> Res.drawable.ic_survey_option_4
    "ur" -> Res.drawable.ic_survey_option_5
    "hi" -> Res.drawable.ic_survey_option_6
    "zh" -> Res.drawable.ic_survey_option_7
    "ja" -> Res.drawable.ic_survey_option_8
    "ko" -> Res.drawable.ic_survey_option_9
    "it" -> Res.drawable.ic_survey_option_10
    "de" -> Res.drawable.ic_survey_option_de
    else -> Res.drawable.ic_survey_option_pt
}
