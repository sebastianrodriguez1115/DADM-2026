package com.edu.unal.tictactoe

import android.content.Context
import android.view.View
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.data.online.SharedPreferencesNicknameStore
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundSink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val KEYBOARD_TIMEOUT_MILLIS = 5_000L
private const val KEYBOARD_OPENING_MILLIS = 2_000L

/**
 * Stores [nickname] in the real nickname preferences, so that a real `MainActivity` shows the
 * lobby without asking for it. Pair it with [clearStoredNickname] in the test's cleanup.
 */
internal fun seedStoredNickname(context: Context, nickname: String = "Ana") {
    check(nicknamePreferences(context).edit().putString(SharedPreferencesNicknameStore.KEY_NICKNAME, nickname).commit()) {
        "Could not seed the nickname SharedPreferences"
    }
}

/** Removes any stored nickname, leaving the nickname preferences as on a fresh install. */
internal fun clearStoredNickname(context: Context) {
    check(nicknamePreferences(context).edit().clear().commit()) {
        "Could not clear the nickname SharedPreferences"
    }
}

private fun nicknamePreferences(context: Context) = context.applicationContext.getSharedPreferences(
    SharedPreferencesNicknameStore.FILE_NAME,
    Context.MODE_PRIVATE,
)

private fun softKeyboardVisible(host: View): Boolean {
    var visible = false
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        visible = ViewCompat.getRootWindowInsets(host)?.isVisible(WindowInsetsCompat.Type.ime()) == true
    }
    return visible
}

/**
 * On a phone the soft keyboard opens by itself with the nickname dialog, and then the first Back
 * only closes it. Waits briefly for it to open, because it appears after the dialog does, and
 * closes it with a real Back if it did. [host] is any view of the Activity window.
 */
internal fun ComposeTestRule.closeSoftKeyboardWithBack(host: View) {
    waitForIdle()
    val opened = try {
        waitUntil(timeoutMillis = KEYBOARD_OPENING_MILLIS) { softKeyboardVisible(host) }
        true
    } catch (_: androidx.compose.ui.test.ComposeTimeoutException) {
        false
    }
    if (!opened) return
    pressBackAsUser()
    waitUntil(timeoutMillis = KEYBOARD_TIMEOUT_MILLIS) { !softKeyboardVisible(host) }
    waitForIdle()
}

private const val AWAIT_MILLIS = 5_000L

/** Moves of a full match where the host (X, moves first at even indexes) wins; the rival (O) plays the odd ones. */
internal val HOST_WINS_MOVES = listOf(0, 3, 1, 4, 2)

/** Moves of a full match where the rival (O) wins. */
internal val RIVAL_WINS_MOVES = listOf(0, 3, 1, 4, 8, 5)

/** Moves of a full match that ends in a tie. */
internal val TIE_MOVES = listOf(0, 1, 2, 4, 3, 5, 7, 6, 8)

/** Content description of the board cell [location] (0 to 8) holding [mark]. */
internal fun cellDescription(context: Context, location: Int, mark: Mark): String {
    val row = location / 3 + 1
    val column = location % 3 + 1
    return when (mark) {
        Mark.Empty -> context.getString(R.string.cell_empty, row, column)
        Mark.X -> context.getString(R.string.cell_x, row, column)
        Mark.O -> context.getString(R.string.cell_o, row, column)
    }
}

/** Waits until some node shows exactly [text]. */
internal fun ComposeTestRule.awaitTextShown(text: String) {
    waitUntil(AWAIT_MILLIS) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    waitForIdle()
}

/** Waits until the board cell [location] shows [mark]. */
internal fun ComposeTestRule.awaitCell(context: Context, location: Int, mark: Mark) {
    val description = cellDescription(context, location, mark)
    waitUntil(AWAIT_MILLIS) { onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty() }
    waitForIdle()
}

/**
 * Plays [moves] in order on a match where the person on screen is the host (X): even indexes are
 * tapped on the board, odd ones are sent by [rival] (O) to [gameId], and each one is awaited on screen.
 */
internal fun ComposeTestRule.playMoves(context: Context, moves: List<Int>, rival: OnlineGameService, gameId: String) {
    moves.forEachIndexed { index, location ->
        if (index % 2 == 0) {
            onNodeWithTag("tic_tac_toe_cell_$location").performClick()
            awaitCell(context, location, Mark.X)
        } else {
            runBlocking { rival.playMove(gameId, location) }
            awaitCell(context, location, Mark.O)
        }
    }
}

/** Id of the only game in the waiting list, as [service] sees it. */
internal fun singleAvailableGameId(service: OnlineGameService): String =
    runBlocking { service.observeAvailableGames().first().single().id }

/** A match sound destination for tests that do not look at the sound. */
internal object NoMoveSound : MoveSoundSink {
    override fun play(player: Player) = Unit

    override fun stop() = Unit
}
