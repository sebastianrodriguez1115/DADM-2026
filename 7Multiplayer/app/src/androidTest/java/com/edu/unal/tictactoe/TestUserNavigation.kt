package com.edu.unal.tictactoe

import android.accessibilityservice.AccessibilityService
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue

/** Sends Android's global Back action instead of invoking a composable callback. */
internal fun pressBackAsUser() {
    assertTrue(
        "Android did not accept the global Back action",
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_BACK,
        ),
    )
}
