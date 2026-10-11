package com.edu.unal.tictactoe

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"
private const val WAIT_MILLIS = 10_000L

/**
 * Rotating the device (a real configuration change, forced per Activity without touching the
 * system settings) with the lobby showing its list, or "waiting for a rival", keeps the same
 * screen and neither duplicates nor cancels the waiting game. The ViewModels are held by the test
 * across the recreation, like the Activity-scoped ones of the app, and the new Activity composes
 * [AppNavigation] again over them.
 */
@RunWith(AndroidJUnit4::class)
class OnlineLobbyRotationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val backend = FakeOnlineBackend()
    private val service = backend.serviceFor("me")
    private val rival = backend.serviceFor("rival")
    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private lateinit var navigation: NavigationViewModel
    private lateinit var lobby: OnlineLobbyViewModel
    private lateinit var game: GameViewModel
    private lateinit var match: OnlineMatchViewModel
    private var originalOrientation: Int? = null

    @Before
    fun launch() {
        navigation = NavigationViewModel(SavedStateHandle())
        lobby = OnlineLobbyViewModel(service, FakeNicknameStore("Ana"))
        game = GameViewModel(InMemoryGamePreferences(), SavedStateHandle())
        match = OnlineMatchViewModel(service)
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { originalOrientation = it.requestedOrientation }
        mountContent()
    }

    @After
    fun close() {
        if (scenario.state != Lifecycle.State.DESTROYED) {
            originalOrientation?.let { value -> scenario.onActivity { it.requestedOrientation = value } }
        }
        scenario.close()
    }

    @Test
    fun rotatingWithTheGamesListKeepsTheListAndTheGames() {
        runBlocking { rival.createGame("Luis") }
        enterLobby()
        awaitText(R.string.lobby_games_header)
        val created = backend.createCount
        val cancelled = backend.cancelCount

        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE) {
            assertListShown()
        }
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT) {
            assertListShown()
        }

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals(created, backend.createCount)
        assertEquals(cancelled, backend.cancelCount)
        assertEquals(1, availableCount())
    }

    @Test
    fun rotatingWhileWaitingKeepsTheWaitingScreenAndDoesNotDuplicateOrCancelTheGame() {
        enterLobby()
        awaitText(R.string.lobby_games_header)
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitText(R.string.lobby_waiting_for_opponent)
        assertEquals(1, backend.createCount)

        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE) {
            assertWaitingShown()
        }
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT) {
            assertWaitingShown()
        }

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals(1, backend.createCount)
        assertEquals(0, backend.cancelCount)
        assertEquals(1, availableCount())
    }

    private fun availableCount(): Int = runBlocking { rival.observeAvailableGames().first().size }

    private fun mountContent() {
        scenario.onActivity { activity ->
            activity.setContent {
                TicTacToeTheme(dynamicColor = false) {
                    AppNavigation(navigationViewModel = navigation, gameViewModel = game, lobbyViewModel = lobby, matchViewModel = match, onlineMoveSound = NoMoveSound)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun enterLobby() {
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        composeRule.waitForIdle()
    }

    private fun awaitText(resId: Int) {
        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodesWithText(context.getString(resId)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun assertListShown() {
        awaitText(R.string.lobby_games_header)
        composeRule.onNodeWithText(context.getString(R.string.lobby_games_header)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.lobby_game_item, "Luis")).assertIsDisplayed()
        assertNoBoardOrWaiting()
    }

    private fun assertWaitingShown() {
        awaitText(R.string.lobby_waiting_for_opponent)
        composeRule.onNodeWithText(context.getString(R.string.lobby_waiting_for_opponent)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.lobby_cancel)).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText(context.getString(R.string.lobby_games_header)).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
    }

    private fun assertNoBoardOrWaiting() {
        assertEquals(0, composeRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText(context.getString(R.string.lobby_waiting_for_opponent)).fetchSemanticsNodes().size)
    }

    /**
     * Forces a real configuration change, composes the app again in the recreated Activity and
     * runs [check] there.
     */
    private fun rotate(requested: Int, expected: Int, check: () -> Unit) {
        scenario.onActivity { it.requestedOrientation = requested }
        composeRule.waitUntil(WAIT_MILLIS) {
            var orientation = Configuration.ORIENTATION_UNDEFINED
            scenario.onActivity { orientation = it.resources.configuration.orientation }
            orientation == expected && scenario.state == Lifecycle.State.RESUMED
        }
        mountContent()
        check()
    }
}
