package com.example.awaytime

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.awaytime.ui.main.MainScreen

@Composable
fun MainNavigation(initialPage: String? = null) {
    MainScreen(
        initialPage = initialPage,
        modifier = Modifier.safeDrawingPadding()
    )
}
