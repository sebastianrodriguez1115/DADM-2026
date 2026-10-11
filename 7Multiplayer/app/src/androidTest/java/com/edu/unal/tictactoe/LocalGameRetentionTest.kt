package com.edu.unal.tictactoe

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.preferences.GamePreferenceSchema
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"
private const val CELL_TAG_PREFIX = "tic_tac_toe_cell_"
private const val WAIT_MILLIS = 10_000L

/**
 * Leaving the local game with Back and choosing it again returns the same game: same board, turn,
 * result and score, with no new game and no second point for a result already scored.
 */
@RunWith(AndroidJUnit4::class)
class LocalGameRetentionTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = GamePreferencesTestFixture()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var gameViewModel: GameViewModel

    @Before
    fun launch() {
        fixture.prepare(context)
        seedStoredNickname(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { gameViewModel = ViewModelProvider(it)[GameViewModel::class.java] }
        composeTestRule.waitForIdle()
        composeTestRule.startLocalGame()
    }

    @After
    fun close() {
        try {
            if (::scenario.isInitialized) scenario.close()
        } finally {
            try {
                clearStoredNickname(context)
            } finally {
                fixture.cleanup()
            }
        }
    }

    @Test
    fun gameInProgressIsTheSameAfterBackToStartAndBackIn() {
        clickCell(0)
        val before = snapshot()
        assertEquals(GameResult.InProgress, before.result)

        backToStart()
        assertSame(gameViewModel, currentGameViewModel())
        assertSameGame(before, snapshot())

        composeTestRule.startLocalGame()

        assertSameGame(before, snapshot())
        assertTrue("a new game would have an empty board", before.board.any { it != Mark.Empty })
        assertScoreStored(before)
    }

    @Test
    fun finishedAndScoredGameKeepsItsResultAndScoreWithoutScoringAgain() {
        val terminal = playUntilTerminal()
        assertTrue(terminal.result != GameResult.InProgress)
        assertEquals(1, scoreTotal(terminal))
        assertScoreStored(terminal)

        backToStart()
        assertSameGame(terminal, snapshot())

        composeTestRule.startLocalGame()

        assertSameGame(terminal, snapshot())
        assertEquals(1, scoreTotal(snapshot()))
        assertScoreStored(terminal)
        composeTestRule.waitForIdle()
        assertEquals(1, scoreTotal(snapshot()))
    }

    @Test
    fun visitingTheLobbyBetweenDoesNotChangeTheLocalGame() {
        clickCell(0)
        val before = snapshot()

        backToStart()
        composeTestRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        assertLobbyShown()
        pressBackAsUser()
        awaitStartShown()
        assertSameGame(before, snapshot())

        composeTestRule.startLocalGame()

        assertSameGame(before, snapshot())
        assertSame(gameViewModel, currentGameViewModel())
    }

    @Test
    fun visitingTheLobbyAfterAScoredGameKeepsItsScore() {
        val terminal = playUntilTerminal()
        assertEquals(1, scoreTotal(terminal))

        backToStart()
        composeTestRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        assertLobbyShown()
        pressBackAsUser()
        awaitStartShown()
        composeTestRule.startLocalGame()

        assertSameGame(terminal, snapshot())
        assertScoreStored(terminal)
    }

    // --- Lobby marker: the only place that knows how the lobby is recognised. With a stored
    // nickname the real lobby shows one of its connection states (see lobbyMarkerCount). ---

    private fun assertLobbyShown() {
        composeTestRule.waitUntil(WAIT_MILLIS) { composeTestRule.lobbyMarkerCount() > 0 }
    }

    // ------------------------------------------------------------------------------------------

    private fun awaitStartShown() {
        val entry = context.getString(R.string.start_play_local)
        composeTestRule.waitUntil(WAIT_MILLIS) {
            composeTestRule.onAllNodesWithText(entry).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(entry).assertIsDisplayed()
    }

    private fun backToStart() {
        pressBackAsUser()
        awaitStartShown()
        assertEquals(0, composeTestRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
        assertNotEquals(Lifecycle.State.DESTROYED, scenario.state)
    }

    private fun snapshot(): GameUiState = gameViewModel.uiState.value

    private fun currentGameViewModel(): GameViewModel {
        var viewModel: GameViewModel? = null
        scenario.onActivity { viewModel = ViewModelProvider(it)[GameViewModel::class.java] }
        return checkNotNull(viewModel)
    }

    private fun clickCell(location: Int) {
        composeTestRule.onAllNodesWithTag(CELL_TAG_PREFIX + location)[0].performClick()
        composeTestRule.waitUntil(WAIT_MILLIS) {
            val state = snapshot()
            state.turn == Turn.Human || state.result != GameResult.InProgress
        }
        composeTestRule.waitForIdle()
    }

    /** Human takes the first free cell each turn; the default seed (Expert) ends it in <= 5 moves. */
    private fun playUntilTerminal(): GameUiState {
        var humanMoves = 0
        while (snapshot().result == GameResult.InProgress) {
            assertTrue("game did not finish within five human moves", humanMoves < 5)
            val location = snapshot().board.indexOfFirst { it == Mark.Empty }
            assertTrue("active game has no free cell", location >= 0)
            clickCell(location)
            humanMoves++
        }
        return snapshot()
    }

    private fun scoreTotal(state: GameUiState) =
        state.score.xWins + state.score.oWins + state.score.ties

    private fun assertScoreStored(state: GameUiState) {
        val stored = context.getSharedPreferences(GamePreferenceSchema.FILE_NAME, Context.MODE_PRIVATE)
        assertEquals(state.score.xWins, stored.getInt(GamePreferenceSchema.SCORE_X_WINS, -1))
        assertEquals(state.score.oWins, stored.getInt(GamePreferenceSchema.SCORE_O_WINS, -1))
        assertEquals(state.score.ties, stored.getInt(GamePreferenceSchema.SCORE_TIES, -1))
    }

    private fun assertSameGame(expected: GameUiState, actual: GameUiState) {
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.starter, actual.starter)
        assertEquals(expected.nextStarter, actual.nextStarter)
        assertEquals(expected.score, actual.score)
        assertEquals(expected.difficulty, actual.difficulty)
        assertEquals(expected.soundEnabled, actual.soundEnabled)
    }
}
