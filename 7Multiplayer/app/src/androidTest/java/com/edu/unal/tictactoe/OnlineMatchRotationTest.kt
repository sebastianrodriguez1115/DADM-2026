package com.edu.unal.tictactoe

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.online.MoveOutcome
import com.edu.unal.tictactoe.data.online.OnlineGame
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.match.LeaveMatchDialogTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WAIT_MILLIS = 10_000L

/**
 * Rotating (a real configuration change, forced per Activity without touching the system
 * settings) and sending the Activity to the background while an online match is on screen. The
 * ViewModels are held by the test across the recreation, like the Activity-scoped ones of the
 * app, and the new Activity composes [AppNavigation] again over them. The match goes through a
 * [CountingService], so the calls to `observeGame` (one per `openMatch`) and to `playMove` can be
 * counted.
 */
@RunWith(AndroidJUnit4::class)
class OnlineMatchRotationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val backend = FakeOnlineBackend()
    private val me = backend.serviceFor("me")
    private val rival = backend.serviceFor("rival")
    private val matchService = CountingService(me)
    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private lateinit var navigation: NavigationViewModel
    private lateinit var lobby: OnlineLobbyViewModel
    private lateinit var match: OnlineMatchViewModel
    private lateinit var game: GameViewModel
    private var originalOrientation: Int? = null

    @Before
    fun launch() {
        navigation = NavigationViewModel(SavedStateHandle())
        lobby = OnlineLobbyViewModel(me, FakeNicknameStore("Ana"))
        match = OnlineMatchViewModel(matchService)
        game = GameViewModel(InMemoryGamePreferences(), SavedStateHandle())
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
    fun rotatingWithMovesKeepsBoardTurnAndNicknamesWithoutRepeatingMoves() {
        val gameId = startMatch()
        composeRule.playMoves(context, listOf(0, 4, 1), rival, gameId)
        awaitShown(context.getString(R.string.match_turn_opponent, "Luis"))
        val boardBefore = backend.gameById(gameId)?.board

        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE) {
            assertMovesShown()
        }
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT) {
            assertMovesShown()
        }

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertEquals(boardBefore, backend.gameById(gameId)?.board)
        assertEquals("the match is opened once", 1, matchService.observeGameCalls)
        assertEquals("no move is sent again", 2, matchService.playMoveCalls)
        assertEquals(0, backend.cancelCount)
    }

    @Test
    fun rotatingWithTheResultVisibleKeepsItEvenIfTheRivalAlreadyLeft() {
        val gameId = startMatch()
        composeRule.playMoves(context, HOST_WINS_MOVES, rival, gameId)
        awaitShown(context.getString(R.string.match_result_win))
        runBlocking { rival.leaveGame(gameId) }
        composeRule.waitForIdle()

        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE) {
            assertResultShown()
        }
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT) {
            assertResultShown()
        }

        assertEquals(1, matchService.observeGameCalls)
    }

    @Test
    fun rotatingWithTheLeaveDialogOpenKeepsItOpen() {
        startMatch()
        composeRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()
        composeRule.onNodeWithTag(LeaveMatchDialogTags.DIALOG).assertIsDisplayed()

        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE) {
            composeRule.onNodeWithTag(LeaveMatchDialogTags.DIALOG).assertIsDisplayed()
        }
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT) {
            composeRule.onNodeWithTag(LeaveMatchDialogTags.DIALOG).assertIsDisplayed()
        }

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertEquals(1, matchService.observeGameCalls)
    }

    @Test
    fun rotatingAfterLosingTheConnectionKeepsTheLostConnectionState() {
        val gameId = startMatch()
        composeRule.playMoves(context, listOf(0, 4), rival, gameId)
        backend.disconnect("me")
        backend.deleteGame(gameId)
        awaitShown(context.getString(R.string.match_connection_lost))

        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE) {
            assertConnectionLostShown()
        }
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT) {
            assertConnectionLostShown()
        }

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertEquals(1, matchService.observeGameCalls)
    }

    @Test
    fun aRivalMoveMadeInTheBackgroundIsShownOnReturning() {
        val gameId = startMatch()
        composeRule.playMoves(context, listOf(0), rival, gameId)

        scenario.moveToState(Lifecycle.State.CREATED)
        runBlocking { rival.playMove(gameId, 4) }
        scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.awaitCell(context, 4, Mark.O)
        awaitShown(context.getString(R.string.turn_human))
        assertEquals(1, matchService.observeGameCalls)
    }

    @Test
    fun theRivalLeavingInTheBackgroundShowsTheNoticeOnReturning() {
        val gameId = startMatch()

        scenario.moveToState(Lifecycle.State.CREATED)
        runBlocking { rival.leaveGame(gameId) }
        scenario.moveToState(Lifecycle.State.RESUMED)

        awaitShown(context.getString(R.string.match_opponent_left))
        awaitShown(context.getString(R.string.match_back_to_lobby))
        assertEquals(AppScreen.Match, navigation.currentScreen.value)
    }

    private fun startMatch(): String {
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        awaitShown(context.getString(R.string.lobby_games_header))
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitShown(context.getString(R.string.lobby_waiting_for_opponent))
        val gameId = singleAvailableGameId(rival)
        runBlocking { rival.joinGame(gameId, "Luis") }
        awaitShown(context.getString(R.string.match_mark_x))
        return gameId
    }

    private fun assertMovesShown() {
        awaitShown(context.getString(R.string.match_mark_x))
        composeRule.onNodeWithText(context.getString(R.string.match_opponent, "Luis")).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.match_turn_opponent, "Luis")).assertIsDisplayed()
        listOf(0 to Mark.X, 1 to Mark.X, 4 to Mark.O).forEach { (location, mark) ->
            assertEquals(1, composeRule.onAllNodesWithContentDescription(cellDescription(context, location, mark)).fetchSemanticsNodes().size)
        }
        assertEquals(0, composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().size)
    }

    private fun assertConnectionLostShown() {
        awaitShown(context.getString(R.string.match_connection_lost))
        composeRule.onNodeWithText(context.getString(R.string.match_back_to_lobby)).assertIsDisplayed()
        listOf(R.string.match_opponent_left, R.string.lobby_offline).forEach { id ->
            assertEquals(0, composeRule.onAllNodesWithText(context.getString(id)).fetchSemanticsNodes().size)
        }
        assertEquals(0, composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().size)
        listOf(0 to Mark.X, 4 to Mark.O).forEach { (location, mark) ->
            assertEquals(1, composeRule.onAllNodesWithContentDescription(cellDescription(context, location, mark)).fetchSemanticsNodes().size)
        }
    }

    private fun assertResultShown() {
        awaitShown(context.getString(R.string.match_result_win))
        composeRule.onNodeWithText(context.getString(R.string.match_back_to_lobby)).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText(context.getString(R.string.match_opponent_left)).fetchSemanticsNodes().size)
        listOf(0, 1, 2).forEach { location ->
            assertEquals(1, composeRule.onAllNodesWithContentDescription(cellDescription(context, location, Mark.X)).fetchSemanticsNodes().size)
        }
    }

    private fun awaitShown(text: String) = composeRule.awaitTextShown(text)

    private fun mountContent() {
        scenario.onActivity { activity ->
            activity.setContent {
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
        composeRule.waitForIdle()
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

/** Delegates to [delegate] and counts the match's observations of a game and the moves sent. */
private class CountingService(private val delegate: OnlineGameService) : OnlineGameService by delegate {
    var observeGameCalls = 0
        private set
    var playMoveCalls = 0
        private set

    override fun observeGame(gameId: String): Flow<OnlineGame?> {
        observeGameCalls++
        return delegate.observeGame(gameId)
    }

    override suspend fun playMove(gameId: String, location: Int): MoveOutcome {
        playMoveCalls++
        return delegate.playMove(gameId, location)
    }
}
