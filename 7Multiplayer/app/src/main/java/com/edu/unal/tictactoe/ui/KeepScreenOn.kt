package com.edu.unal.tictactoe.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.WeakHashMap

/**
 * Keeps the device screen on while it is part of the composition, by asking the Activity's window
 * for `FLAG_KEEP_SCREEN_ON` (no wake lock, no permission). The flag is cleared when the last
 * composable that asked for it leaves.
 *
 * It does not use the hosting view's `keepScreenOn`: after the Activity goes to the background and
 * comes back, the platform folds that view-derived flag into the window's own attributes, and
 * clearing the view's flag no longer releases the screen. A flag requested on the window itself is
 * the client's own request and is cleared reliably.
 *
 * Requests are counted per window, so a screen that leaves and another that arrives in the same
 * recomposition (the waiting screen handing over to the match) leave the flag set whatever the
 * order of the two callbacks. Without an Activity (a preview) it does nothing.
 */
@Composable
fun KeepScreenOn() {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window) {
        if (window != null) KeepScreenOnRequests.acquire(window)
        onDispose { if (window != null) KeepScreenOnRequests.release(window) }
    }
}

/** Per-window count of the composables that want the screen on. Main thread only. */
private object KeepScreenOnRequests {
    private val counts = WeakHashMap<Window, Int>()

    fun acquire(window: Window) {
        val count = (counts[window] ?: 0) + 1
        counts[window] = count
        if (count == 1) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun release(window: Window) {
        val count = (counts[window] ?: 0) - 1
        if (count > 0) {
            counts[window] = count
        } else {
            counts.remove(window)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
