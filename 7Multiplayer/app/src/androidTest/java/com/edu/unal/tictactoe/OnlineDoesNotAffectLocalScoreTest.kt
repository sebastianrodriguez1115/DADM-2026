package com.edu.unal.tictactoe

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Playing and finishing online matches leaves the local score, and what is stored for it, untouched. */
@RunWith(AndroidJUnit4::class)
class OnlineDoesNotAffectLocalScoreTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val seededScore = Score(xWins = 2, oWins = 1, ties = 3)
    private val preferences = InMemoryGamePreferences().apply { writeScore(seededScore) }
    private val backend = FakeOnlineBackend()
    private val me = backend.serviceFor("me")
    private val rival = backend.serviceFor("rival")
    private lateinit var game: GameViewModel

    @Before
    fun mount() {
        val navigation = NavigationViewModel(SavedStateHandle())
        val lobby = OnlineLobbyViewModel(me, FakeNicknameStore("Ana"))
        val match = OnlineMatchViewModel(me)
        game = GameViewModel(preferences, SavedStateHandle())
        composeRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                AppNavigation(
                    navigationViewModel = navigation,
                    gameViewModel = game,
                    lobbyViewModel = lobby,
                    matchViewModel = match,
                )
            }
        }
    }

    @Test
    fun winningAnOnlineMatchDoesNotChangeTheLocalScore() {
        playAndFinish(HOST_WINS_MOVES, R.string.match_result_win)

        assertScoreUnchanged()
    }

    @Test
    fun losingAnOnlineMatchDoesNotChangeTheLocalScore() {
        playAndFinish(RIVAL_WINS_MOVES, R.string.match_result_opponent_wins, "Luis")

        assertScoreUnchanged()
    }

    @Test
    fun tyingAnOnlineMatchDoesNotChangeTheLocalScore() {
        playAndFinish(TIE_MOVES, R.string.result_tie)

        assertScoreUnchanged()
    }

    private fun playAndFinish(moves: List<Int>, resultRes: Int, vararg resultArgs: Any) {
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_waiting_for_opponent))
        val gameId = singleAvailableGameId(rival)
        runBlocking { rival.joinGame(gameId, "Luis") }
        composeRule.awaitTextShown(context.getString(R.string.match_mark_x))

        composeRule.playMoves(context, moves, rival, gameId)
        composeRule.awaitTextShown(context.getString(resultRes, *resultArgs))
        composeRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
    }

    private fun assertScoreUnchanged() {
        assertEquals(seededScore, game.uiState.value.score)
        assertEquals(seededScore, preferences.readScore())
    }
}
