package com.edu.unal.tictactoe

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.ui.game.DifficultyDialog
import com.edu.unal.tictactoe.ui.game.DifficultyDialogTags
import com.edu.unal.tictactoe.ui.game.DifficultyLabels
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DifficultyDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun opensWithFourOrderedLabelsAndOnlyCurrentSelected() {
        setHost(HostState(DifficultyLevel.Hard))

        composeTestRule.onNodeWithTag(DifficultyDialogTags.DIALOG).assertExists()
        val tops = DifficultyLevel.entries.map { level ->
            composeTestRule.onNodeWithTag(DifficultyDialogTags.option(level)).assertExists()
            composeTestRule.onNodeWithText(context.getString(DifficultyLabels.labelRes(level)))
                .assertExists()
            composeTestRule.onNodeWithTag(DifficultyDialogTags.option(level))
                .getUnclippedBoundsInRoot().top
        }
        assertEquals(tops.sorted(), tops)
        assertOnlySelected(DifficultyLevel.Hard)
    }

    @Test
    fun selectingEachOptionIncludingCurrentUpdatesClosesAndNotifiesOnce() {
        val host = HostState(DifficultyLevel.Easy)
        setHost(host)

        DifficultyLevel.entries.forEach { level ->
            if (!host.visible) {
                composeTestRule.runOnIdle { host.visible = true }
                composeTestRule.waitForIdle()
            }
            assertOnlySelected(host.current)
            host.toasts.clear()

            clickOption(level)

            assertEquals(level, host.current)
            assertDialogClosed()
            assertEquals(listOf(level), host.toasts)
        }

        composeTestRule.runOnIdle { host.visible = true }
        composeTestRule.waitForIdle()
        assertOnlySelected(DifficultyLevel.Impossible)
        assertEquals(listOf(DifficultyLevel.Impossible), host.toasts)
    }

    @Test
    fun rejectedSelectionClosesWithoutChangingValueOrNotifying() {
        val host = HostState(DifficultyLevel.Expert)
        host.accept = false
        setHost(host)

        clickOption(DifficultyLevel.Impossible)
        assertEquals(DifficultyLevel.Expert, host.current)
        assertDialogClosed()
        assertEquals(emptyList<DifficultyLevel>(), host.toasts)

        composeTestRule.runOnIdle { host.visible = true }
        composeTestRule.waitForIdle()
        clickOption(DifficultyLevel.Expert)
        assertEquals(DifficultyLevel.Expert, host.current)
        assertDialogClosed()
        assertEquals(emptyList<DifficultyLevel>(), host.toasts)
    }

    @Test
    fun backDismissesWithoutSelectionOrToast() {
        val host = HostState(DifficultyLevel.Impossible)
        setHost(host)

        composeTestRule.onNodeWithTag(DifficultyDialogTags.DIALOG).assertIsDisplayed()
        composeTestRule.waitForIdle()
        pressBackAsUser()
        composeTestRule.waitForIdle()

        assertEquals(DifficultyLevel.Impossible, host.current)
        assertDialogClosed()
        assertEquals(emptyList<DifficultyLevel>(), host.toasts)
    }

    @Test
    fun outsideTouchDismissesWithoutSelectionOrToast() {
        val host = HostState(DifficultyLevel.Impossible)
        setHost(host)

        composeTestRule.onNodeWithTag(DifficultyDialogTags.DIALOG).assertIsDisplayed()
        tapOutsideDialog()

        assertEquals(DifficultyLevel.Impossible, host.current)
        assertDialogClosed()
        assertEquals(emptyList<DifficultyLevel>(), host.toasts)
    }

    @Test
    fun hostRecreationAndRecompositionKeepCurrentWithoutNotifying() {
        val host = HostState(DifficultyLevel.Hard)
        setHost(host)

        composeTestRule.runOnIdle { host.generation++ }
        composeTestRule.waitForIdle()
        assertOnlySelected(DifficultyLevel.Hard)
        assertEquals(emptyList<DifficultyLevel>(), host.toasts)

        composeTestRule.runOnIdle { host.revision++ }
        composeTestRule.waitForIdle()
        assertOnlySelected(DifficultyLevel.Hard)
        assertEquals(emptyList<DifficultyLevel>(), host.toasts)
    }

    private fun clickOption(level: DifficultyLevel) {
        composeTestRule.onNodeWithTag(DifficultyDialogTags.option(level))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun assertOnlySelected(selected: DifficultyLevel) {
        DifficultyLevel.entries.forEach { level ->
            val option = composeTestRule.onNodeWithTag(DifficultyDialogTags.option(level))
            if (level == selected) {
                option.assertIsSelected()
            } else {
                option.assertIsNotSelected()
            }
        }
    }

    private fun assertDialogClosed() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(DifficultyDialogTags.DIALOG)
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
                    // Reading `revision` recomposes the host when it changes.
                    check(host.revision >= 0)
                    if (host.visible) {
                        key(host.generation) {
                            DifficultyDialog(
                                currentDifficulty = host.current,
                                onDifficultySelected = { level ->
                                    if (host.accept) {
                                        host.current = level
                                    }
                                    host.accept
                                },
                                onDismiss = { host.visible = false },
                                onShowToast = { host.toasts += it },
                            )
                        }
                    }
                }
            }
        }
    }

    private class HostState(initial: DifficultyLevel) {
        var current by mutableStateOf(initial)
        var visible by mutableStateOf(true)
        var generation by mutableStateOf(0)
        var revision by mutableStateOf(0)
        var accept: Boolean = true
        val toasts = mutableListOf<DifficultyLevel>()
    }
}
