package com.edu.unal.tictactoe

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.lobby.WaitingForOpponentScreen
import com.edu.unal.tictactoe.ui.online.match.MatchPhase
import com.edu.unal.tictactoe.ui.online.match.MatchResult
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchScreen
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchUiState
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The screen stays on only while the waiting screen or a loading or playing match is shown. It is
 * requested through `FLAG_KEEP_SCREEN_ON` on the Activity's window, which is what these tests
 * read; what the system itself holds after the app goes to the background is covered by
 * [KeepScreenOnBackgroundTest].
 */
@RunWith(AndroidJUnit4::class)
class KeepScreenOnTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var matchState by mutableStateOf(OnlineMatchUiState())

    private val keepsScreenOn: Boolean
        get() {
            composeRule.waitForIdle()
            return composeRule.activity.window.attributes.flags and
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        }

    private fun matchIn(phase: MatchPhase) = OnlineMatchUiState(
        myMark = Player.X,
        opponentName = "Luis",
        board = List(9) { Mark.Empty },
        phase = phase,
        result = MatchResult.Won.takeIf { phase == MatchPhase.Finished },
    )

    @Test
    fun theWaitingScreenKeepsTheScreenOnAndLeavingItReleasesIt() {
        var waiting by mutableStateOf(true)
        composeRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                if (waiting) WaitingForOpponentScreen(onCancel = {})
            }
        }
        assertEquals(true, keepsScreenOn)

        waiting = false

        assertEquals(false, keepsScreenOn)
    }

    @Test
    fun theMatchKeepsTheScreenOnOnlyWhileLoadingOrPlaying() {
        composeRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                OnlineMatchScreen(
                    uiState = matchState,
                    isLeaveDialogOpen = false,
                    onLeaveDialogOpenChange = {},
                    onLeaveConfirmed = {},
                    onCellClick = {},
                    onBackToLobby = {},
                )
            }
        }
        val expected = mapOf(
            MatchPhase.Loading to true,
            MatchPhase.Playing to true,
            MatchPhase.Finished to false,
            MatchPhase.Abandoned to false,
            MatchPhase.ConnectionLost to false,
        )
        // Every transition, in both directions, so a stale flag cannot pass.
        listOf(
            MatchPhase.Loading, MatchPhase.Playing, MatchPhase.Finished, MatchPhase.Loading,
            MatchPhase.Abandoned, MatchPhase.Playing, MatchPhase.ConnectionLost, MatchPhase.Playing,
        ).forEach { phase ->
            matchState = matchIn(phase)
            assertEquals("keepScreenOn in $phase", expected.getValue(phase), keepsScreenOn)
        }
        assertEquals(
            setOf(
                MatchPhase.Loading, MatchPhase.Playing, MatchPhase.Finished,
                MatchPhase.Abandoned, MatchPhase.ConnectionLost,
            ),
            expected.keys,
        )
    }

    @Test
    fun theWholeFlowKeepsTheScreenOnOnlyWhileWaitingAndPlaying() {
        val backend = FakeOnlineBackend()
        val me = backend.serviceFor("me")
        val rival = backend.serviceFor("rival")
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
        assertEquals("start", false, keepsScreenOn)

        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        assertEquals("lobby", false, keepsScreenOn)

        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_waiting_for_opponent))
        assertEquals("waiting", true, keepsScreenOn)

        val gameId = singleAvailableGameId(rival)
        runBlocking { rival.joinGame(gameId, "Luis") }
        composeRule.awaitTextShown(context.getString(R.string.match_mark_x))
        assertEquals("playing", true, keepsScreenOn)

        composeRule.playMoves(context, HOST_WINS_MOVES, rival, gameId)
        composeRule.awaitTextShown(context.getString(R.string.match_result_win))
        assertEquals("finished", false, keepsScreenOn)

        composeRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        assertEquals("lobby again", false, keepsScreenOn)

        val secondId = runBlocking { rival.createGame("Luis") }
        composeRule.onNodeWithText(context.getString(R.string.lobby_game_item, "Luis")).performClick()
        composeRule.awaitTextShown(context.getString(R.string.match_mark_o))
        assertEquals("second match", true, keepsScreenOn)

        backend.disconnect("me")
        backend.deleteGame(secondId)
        composeRule.awaitTextShown(context.getString(R.string.match_connection_lost))
        assertEquals("connection lost", false, keepsScreenOn)

        composeRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        assertEquals("lobby after connection lost", false, keepsScreenOn)
    }

    @Test
    fun theAbandonedMatchAndTheLocalModeReleaseTheScreen() {
        val backend = FakeOnlineBackend()
        val me = backend.serviceFor("me")
        val rival = backend.serviceFor("rival")
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
        val gameId = runBlocking { rival.createGame("Luis") }
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        composeRule.onNodeWithText(context.getString(R.string.lobby_game_item, "Luis")).performClick()
        composeRule.awaitTextShown(context.getString(R.string.match_mark_o))
        assertEquals("playing", true, keepsScreenOn)

        runBlocking { rival.leaveGame(gameId) }
        composeRule.awaitTextShown(context.getString(R.string.match_opponent_left))
        assertEquals("abandoned", false, keepsScreenOn)

        navigation.navigateTo(AppScreen.Local)
        assertEquals("local", false, keepsScreenOn)
    }
}
