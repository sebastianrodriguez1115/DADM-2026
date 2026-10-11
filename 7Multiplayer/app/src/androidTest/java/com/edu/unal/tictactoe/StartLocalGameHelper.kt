package com.edu.unal.tictactoe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry

private const val LOCAL_BOARD_TAG = "tic_tac_toe_board"
private const val START_LOCAL_TIMEOUT_MILLIS = 5_000L

/**
 * Enters the local mode from the start screen the way a person does: presses "play against the
 * computer" and waits for the board. Call it right after launching `MainActivity` with a fresh
 * state; after a recreation with saved state the app already restores the local game, so it must
 * not be called again. Works with any compose rule, since both `createAndroidComposeRule` and
 * `createEmptyComposeRule` implement [ComposeTestRule].
 */
internal fun ComposeTestRule.startLocalGame() {
    val entry = InstrumentationRegistry.getInstrumentation().targetContext
        .getString(R.string.start_play_local)
    onNodeWithText(entry).performClick()
    waitUntil(timeoutMillis = START_LOCAL_TIMEOUT_MILLIS) {
        onAllNodesWithTag(LOCAL_BOARD_TAG).fetchSemanticsNodes().isNotEmpty()
    }
    waitForIdle()
    onNodeWithTag(LOCAL_BOARD_TAG).assertIsDisplayed()
}
