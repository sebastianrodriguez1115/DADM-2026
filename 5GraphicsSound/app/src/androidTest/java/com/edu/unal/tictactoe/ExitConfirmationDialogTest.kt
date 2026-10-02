package com.edu.unal.tictactoe

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.game.ExitConfirmationDialog
import com.edu.unal.tictactoe.ui.game.ExitConfirmationDialogTags
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExitConfirmationDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun noInvokesOnlyCancelClosesDialogAndKeepsHostStateIntact() {
        val host = HostState()
        setHost(host)
        assertDialogVisible()
        assertText(R.string.exit_dialog_title)
        assertText(R.string.exit_dialog_message)
        assertText(R.string.exit_yes)
        assertText(R.string.exit_no)

        composeTestRule.onNodeWithTag(ExitConfirmationDialogTags.CANCEL)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertDialogClosed()
        assertEquals(1, host.cancelCount)
        assertEquals(0, host.confirmCount)
        assertEquals(0, host.matchState)
    }

    @Test
    fun yesInvokesOnlyConfirmWithoutClosingOrChangingHostState() {
        val host = HostState()
        setHost(host)

        composeTestRule.onNodeWithTag(ExitConfirmationDialogTags.CONFIRM)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertDialogVisible()
        assertEquals(1, host.confirmCount)
        assertEquals(0, host.cancelCount)
        assertEquals(0, host.matchState)
    }

    @Test
    fun backPressKeepsDialogOpenAndInvokesNoCallback() {
        val host = HostState()
        setHost(host)

        assertDialogVisible()
        composeTestRule.waitForIdle()
        pressBack()
        composeTestRule.waitForIdle()

        assertDialogVisible()
        assertEquals(0, host.cancelCount)
        assertEquals(0, host.confirmCount)
    }

    @Test
    fun outsideTouchKeepsDialogOpenThenNoCloses() {
        val host = HostState()
        setHost(host)

        assertDialogVisible()
        tapOutsideDialog()

        assertDialogVisible()
        assertEquals(0, host.cancelCount)
        assertEquals(0, host.confirmCount)
        assertEquals(0, host.matchState)

        composeTestRule.onNodeWithTag(ExitConfirmationDialogTags.CANCEL)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertDialogClosed()
        assertEquals(1, host.cancelCount)
        assertEquals(0, host.confirmCount)
    }

    private fun assertDialogVisible() {
        composeTestRule.onNodeWithTag(ExitConfirmationDialogTags.DIALOG).assertIsDisplayed()
    }

    private fun assertText(@StringRes id: Int) {
        composeTestRule.onNodeWithText(context.getString(id)).assertExists()
    }

    private fun assertDialogClosed() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(ExitConfirmationDialogTags.DIALOG)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun tapOutsideDialog() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val x = 4f
        val y = context.resources.displayMetrics.heightPixels / 2f
        val downTime = SystemClock.uptimeMillis()
        val down = touchEvent(MotionEvent.ACTION_DOWN, downTime, downTime, x, y)
        val up = touchEvent(MotionEvent.ACTION_UP, downTime, downTime + 16, x, y)
        try {
            assertTrue(instrumentation.uiAutomation.injectInputEvent(down, true))
            SystemClock.sleep(16)
            assertTrue(instrumentation.uiAutomation.injectInputEvent(up, true))
        } finally {
            down.recycle()
            up.recycle()
        }
        composeTestRule.waitForIdle()
    }

    private fun touchEvent(
        action: Int,
        downTime: Long,
        eventTime: Long,
        x: Float,
        y: Float,
    ): MotionEvent =
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
            .apply { source = InputDevice.SOURCE_TOUCHSCREEN }

    private fun setHost(host: HostState) {
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (host.visible) {
                        ExitConfirmationDialog(
                            onCancel = {
                                host.cancelCount++
                                host.visible = false
                            },
                            onConfirm = { host.confirmCount++ },
                        )
                    }
                }
            }
        }
    }

    private class HostState {
        var visible by mutableStateOf(true)
        var cancelCount = 0
        var confirmCount = 0
        var matchState by mutableStateOf(0)
    }
}
