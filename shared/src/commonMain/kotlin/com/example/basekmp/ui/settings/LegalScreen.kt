package com.example.basekmp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basekmp.data.Strings
import com.example.basekmp.theme.AppTheme
import com.example.basekmp.ui.components.BackTitleBar

private val PrivacySections = listOf(
    "Information We Collect" to "Semavia: Learn Languages stores your learning preferences (target language, native language, English level, daily goal and tutor voice) locally on your device. Your chat history with the AI tutor, practice scores, streaks and learning history are stored privately on your device.",
    "Voice & Microphone" to "When you use the microphone to practise speaking, your voice is processed by the on-device speech recognizer to produce a text transcript. We do not record, store or upload your voice audio to external servers.",
    "AI Tutor & Generated Content" to "Messages you send to the AI Tutor and the text needed to generate lessons, vocabulary, grammar and speaking practice are processed by Google's Gemini LLM to generate replies. This content is used solely to produce learning responses and is never linked to your personal identity.",
    "Analytics" to "We collect anonymous product interaction events to understand how features are used (such as which lesson or practice is completed) so we can continuously refine the learning experience.",
    "Data Control" to "You can clear your local progress and preferences at any time in Settings. Uninstalling the app also removes all cached session logs.",
    "Children's Privacy" to "Semavia: Learn Languages is not directed at children under 13, and we do not knowingly collect personal information from children under 13.",
    "Contact Us" to "Questions about this privacy policy? Contact us at support@example.com."
)

private val TermsSections = listOf(
    "Acceptance of Terms" to "By downloading or using Semavia: Learn Languages, you agree to these terms. If you do not agree, please discontinue using the app.",
    "Educational Disclaimer" to "Semavia: Learn Languages provides automated language practice, vocabulary quizzes, grammar explanations and AI chat. Lesson content and scoring are generated algorithmically and are intended for self-study and educational enrichment only.",
    "Acceptable Use" to "The app is provided for personal, non-commercial language learning. You may not reverse-engineer, exploit, or attempt to circumvent application security or limits.",
    "Limitation of Liability" to "To the maximum extent permitted by law, the app and services are provided 'as is' without warranty of any kind.",
    "Changes" to "We may update these terms periodically. Continued use of the app signifies acceptance of any revisions."
)

@Composable
fun LegalScreen(
    kind: String, // "privacy" or "terms"
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val scrollState = rememberScrollState()

    val isPrivacy = kind.equals("privacy", ignoreCase = true)
    val title = if (isPrivacy) Strings.tr("Privacy Policy") else Strings.tr("Terms & Conditions")
    val sections = if (isPrivacy) PrivacySections else TermsSections

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(vertical = dimens.spacingSm)
    ) {
        Spacer(Modifier.height(8.dp))
        BackTitleBar(
            title = title,
            onBack = onBack,
            modifier = Modifier.padding(vertical = dimens.spacingSm)
        )

        Spacer(Modifier.height(dimens.spacingSm))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingLg)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "${Strings.tr("Last updated")}: October 2026",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(Modifier.height(dimens.spacingLg))

            sections.forEach { (heading, body) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimens.spacingSm)
                        .clip(RoundedCornerShape(dimens.cornerMd))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(dimens.cornerMd))
                        .padding(dimens.spacingLg)
                ) {
                    Column {
                        Text(
                            text = heading,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        Spacer(Modifier.height(dimens.spacingSm))
                        Text(
                            text = body,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimens.spacing2Xl))
        }
    }
}
