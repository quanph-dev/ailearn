package com.example.basekmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.basekmp.data.Strings

/**
 * Standard Practice Setup Screen scaffold matching decompiled PracticeSetupScaffoldKt.java.
 */
@Composable
fun PracticeSetupScaffold(
    title: String,
    onBack: () -> Unit,
    onStart: () -> Unit,
    fields: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 14.dp)
        ) {
            BackTitleBar(
                title = title,
                onBack = onBack
            )

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = StandardBackButtonInset)
            ) {
                fields()

                Spacer(Modifier.weight(1f))

                PrimaryButton(
                    text = Strings.tr("Start"),
                    onClick = onStart
                )
            }
        }
    }
}
