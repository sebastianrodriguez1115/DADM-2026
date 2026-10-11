package com.edu.unal.tictactoe

import android.app.Instrumentation
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.lobby.WaitingForOpponentTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression for the screen staying held after the app went to the background (D-4). It reads what
 * the system sees, not the hosting view's `keepScreenOn`: the flags of the Activity's window in
 * `dumpsys window` and the `SCREEN_BRIGHT_WAKE_LOCK 'WindowManager/...'` entry in `dumpsys power`,
 * which the window manager holds for a visible window that asks to keep the screen on.
 */
@RunWith(AndroidJUnit4::class)
class KeepScreenOnBackgroundTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val instrumentation: Instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private val backend = FakeOnlineBackend()
    private val me = backend.serviceFor("me")
    private val rival = backend.serviceFor("rival")

    private fun mountApp() {
        val navigation = NavigationViewModel(SavedStateHandle())
        val lobby = OnlineLobbyViewModel(me, FakeNicknameStore("Ana"))
        val match = OnlineMatchViewModel(me)
        val game = GameViewModel(InMemoryGamePreferences(), SavedStateHandle())
        composeRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                AppNavigation(
                    navigationViewModel = navigation,
                    gameViewModel = game,
                    lobbyViewModel = lobby,
                    matchViewModel = match,
                    onlineMoveSound = NoMoveSound,
                )
            }
        }
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().decodeToString() }
    }

    /** The `fl=` line of this Activity's window, as the window manager holds it. */
    private fun windowFlags(): String {
        val title = "${context.packageName}/${composeRule.activity.javaClass.name}"
        val lines = shell("dumpsys window windows").lines()
        val header = lines.indexOfFirst { it.contains("Window{") && it.contains(title) }
        if (header < 0) return "(window not listed)"
        return lines.drop(header + 1).take(40).firstOrNull { it.trim().startsWith("fl=") }?.trim()
            ?: "(no flags line)"
    }

    private fun windowManagerLocks(): List<String> = shell("dumpsys power").lines()
        .filter { it.contains("SCREEN_BRIGHT_WAKE_LOCK") && it.contains("'WindowManager/") }
        .map { it.trim().substringBefore(" ACQ=") }

    /** Waits (up to a few seconds) until both indicators show [expected]; fails with what it saw. */
    private fun assertHeld(label: String, expected: Boolean) {
        val deadline = System.currentTimeMillis() + SETTLE_MILLIS
        var flags: String
        var locks: List<String>
        var held: Boolean
        var flagged: Boolean
        do {
            composeRule.waitForIdle()
            flags = windowFlags()
            locks = windowManagerLocks()
            flagged = flags.contains("KEEP_SCREEN_ON")
            held = locks.isNotEmpty()
            if (flagged == expected && held == expected) return
            Thread.sleep(POLL_MILLIS)
        } while (System.currentTimeMillis() < deadline)
        fail(
            "$label: expected the screen ${if (expected) "held" else "released"} but the window " +
                "flag was ${if (flagged) "KEEP_SCREEN_ON" else "absent"} and the window manager " +
                "wake lock was ${if (held) "present" else "absent"} (window: $flags; locks: $locks)",
        )
    }

    /**
     * Sends the app to the background for real (Home), waits, and brings its task back to the
     * front, as QA did with `KEYCODE_HOME` and `am start`. Pausing the Activity from the test
     * harness alone does not reproduce the defect.
     */
    private fun goToBackgroundAndBack() {
        val activity = composeRule.activity
        val component = "${context.packageName}/${activity.javaClass.name}"
        shell("input keyevent KEYCODE_HOME")
        awaitLifecycle(Lifecycle.State.CREATED)
        Thread.sleep(BACKGROUND_MILLIS)
        val started = shell("am start -n $component -f 0x10020000")
        returnOutput = started
        awaitLifecycle(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    private var returnOutput = ""

    private fun awaitLifecycle(state: Lifecycle.State) {
        val deadline = System.currentTimeMillis() + SETTLE_MILLIS
        while (composeRule.activity.lifecycle.currentState != state) {
            if (System.currentTimeMillis() > deadline) fail("the Activity did not reach $state (am start said: $returnOutput)")
            Thread.sleep(POLL_MILLIS)
        }
    }

    private fun openLobbyAndCreateGame() {
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_waiting_for_opponent))
    }

    @Test
    fun cancellingTheWaitAfterTheBackgroundReleasesTheScreen() {
        mountApp()
        assertHeld("start", false)
        openLobbyAndCreateGame()
        assertHeld("waiting", true)

        goToBackgroundAndBack()
        composeRule.awaitTextShown(context.getString(R.string.lobby_waiting_for_opponent))
        assertHeld("waiting after the background", true)

        composeRule.onNodeWithTag(WaitingForOpponentTags.CANCEL).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        assertHeld("lobby after cancelling", false)

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.awaitTextShown(context.getString(R.string.start_play_online))
        assertHeld("start after the lobby", false)
    }

    @Test
    fun finishingTheMatchAfterTheBackgroundReleasesTheScreen() {
        mountApp()
        openLobbyAndCreateGame()
        val gameId = singleAvailableGameId(rival)
        runBlocking { rival.joinGame(gameId, "Luis") }
        composeRule.awaitTextShown(context.getString(R.string.match_mark_x))
        assertHeld("playing", true)

        goToBackgroundAndBack()
        composeRule.awaitTextShown(context.getString(R.string.match_mark_x))
        assertHeld("playing after the background", true)

        composeRule.playMoves(context, HOST_WINS_MOVES, rival, gameId)
        composeRule.awaitTextShown(context.getString(R.string.match_result_win))
        assertHeld("finished after the background", false)

        composeRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        assertHeld("lobby after the match", false)

        // A second wait in the same process, cancelled, must not leave the screen held either.
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_waiting_for_opponent))
        assertHeld("second wait", true)
        composeRule.onNodeWithTag(WaitingForOpponentTags.CANCEL).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        assertHeld("lobby after the second wait", false)
    }

    @Test
    fun returningFromTheBackgroundKeepsTheScreenHeldWhileWaitingAndPlaying() {
        mountApp()
        openLobbyAndCreateGame()
        repeat(2) { round ->
            goToBackgroundAndBack()
            assertHeld("waiting, return ${round + 1}", true)
        }

        runBlocking { rival.joinGame(singleAvailableGameId(rival), "Luis") }
        composeRule.awaitTextShown(context.getString(R.string.match_mark_x))
        assertHeld("playing (waiting handed over to the match)", true)
        repeat(2) { round ->
            goToBackgroundAndBack()
            assertHeld("playing, return ${round + 1}", true)
        }
    }

    @Test
    fun connectionLostAfterTheBackgroundReleasesTheScreen() {
        mountApp()
        val gameId = runBlocking { rival.createGame("Luis") }
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        composeRule.onNodeWithText(context.getString(R.string.lobby_game_item, "Luis")).performClick()
        composeRule.awaitTextShown(context.getString(R.string.match_mark_o))
        assertHeld("playing", true)

        goToBackgroundAndBack()
        assertHeld("playing after the background", true)

        backend.disconnect("me")
        backend.deleteGame(gameId)
        composeRule.awaitTextShown(context.getString(R.string.match_connection_lost))
        assertHeld("connection lost after the background", false)
    }

    private companion object {
        const val SETTLE_MILLIS = 6_000L
        const val POLL_MILLIS = 250L
        const val BACKGROUND_MILLIS = 3_500L
    }
}
