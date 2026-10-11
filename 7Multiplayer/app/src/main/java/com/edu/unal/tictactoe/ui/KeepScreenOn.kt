package com.edu.unal.tictactoe.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the device screen on while it is part of the composition, through the hosting view's
 * `keepScreenOn` flag (no wake lock, no permission). The flag is cleared when it leaves.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
