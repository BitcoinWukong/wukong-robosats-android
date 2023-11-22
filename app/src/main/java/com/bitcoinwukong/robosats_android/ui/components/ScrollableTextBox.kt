package com.bitcoinwukong.robosats_android.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ScrollableTextBox(text: String, modifier: Modifier, textColor: Color) {
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = text) {
        // This block will execute every time 'text' changes
        scrollState.scrollTo(scrollState.maxValue)
    }

    BasicTextField(
        value = text,
        onValueChange = {},
        modifier = modifier
            .verticalScroll(scrollState),
        textStyle = LocalTextStyle.current.copy(color = textColor), // Use textColor
        readOnly = true // Set to true since it's for display only
    )
}