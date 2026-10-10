package com.edu.unal.tictactoe

import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameMenuTags
import com.edu.unal.tictactoe.ui.game.GameScreen
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundHost
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundPlayer
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundPlayerFactory
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real screen, session ViewModel and host/controller with a deterministic backend. */
@RunWith(AndroidJUnit4::class)
class GameScreenSoundIntegrationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val viewModelStore = ViewModelStore()
    private lateinit var viewModel: GameViewModel
    private lateinit var host: MoveSoundHost
    private lateinit var player: IndefiniteFakePlayer

    @Before
    fun setUp() {
        composeRule.runOnIdle {
            viewModel = GameViewModel(
                preferences = InMemoryGamePreferences(),
                savedStateHandle = SavedStateHandle(),
                game = TicTacToeGame(MoveRandomizer { 0 }),
            )
            assertTrue(viewModel.setDifficulty(DifficultyLevel.Easy))
            viewModelStore.put(GameViewModel::class.java.name, viewModel)
            host = MoveSoundHost(viewModel, MoveSoundPlayerFactory {
                IndefiniteFakePlayer().also { player = it }
            })
            host.onResume()
            player.completeLoads()
            assertEquals(2, player.completedLoadCount)
        }
        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            TicTacToeTheme(dynamicColor = false) {
                GameScreen(
                    uiState = state,
                    onCellClick = viewModel::onHumanMove,
                    onNewGame = viewModel::newGame,
                    onToggleSound = viewModel::toggleSound,
                    onDifficultySelected = viewModel::setDifficulty,
                    onShowToast = {},
                    onResetScores = viewModel::resetScores,
                )
            }
        }
    }

    @After
    fun tearDown() {
        if (::host.isInitialized) composeRule.runOnIdle {
            host.onPause()
            host.onDestroy()
        }
        composeRule.runOnIdle { viewModelStore.clear() }
    }

    @Test
    fun menuMuteStopsInSameMainEventPreservesTurnAndSuppressesUntilUnmuted() {
        // Start a normal turn via the rendered board. O's response is delayed by the
        // production ViewModel; this test waits for the real transition, not a sleep.
        composeRule.onNodeWithTag("tic_tac_toe_cell_0").performClick()
        assertEquals(1, player.playedMarks.size)
        assertEquals(Player.X, player.playedMarks.single())
        awaitHumanTurnAfterComputerMove(
            expectedBoardXCount = 1,
            expectedBoardOCount = 1,
            expectedPlayedMarks = listOf(Player.X, Player.O),
        )
        assertEquals(2, player.playedMarks.size)
        assertEquals(Player.O, player.playedMarks.last())

        openMenu()
        val soundOption = composeRule.onNodeWithTag(GameMenuTags.SOUND)
        val soundClickAction = soundOption.fetchSemanticsNode()
            .config[SemanticsActions.OnClick]?.action ?: error("Sound menu entry has no OnClick semantics")
        val beforeMute = viewModel.uiState.value
        // With X at 0 and deterministic O at 1, X at 3 stays non-terminal so
        // this accepted move has exactly one delayed O response to preserve.
        val acceptedCell = 3
        assertEquals(Mark.Empty, beforeMute.board[acceptedCell])

        // Both the accepted X and the real menu semantics action execute in one
        // Main/Compose-idle callback. Snapshot after X isolates the menu action.
        composeRule.runOnIdle {
            viewModel.onHumanMove(acceptedCell)
            val immediatelyBeforeToggle = viewModel.uiState.value
            soundClickAction()

            val muted = viewModel.uiState.value
            assertFalse(muted.soundEnabled)
            assertEquals(3, player.stopStreams.size)
            assertTrue(player.activeStreams.isEmpty())
            assertEquals(immediatelyBeforeToggle.board, muted.board)
            assertEquals(immediatelyBeforeToggle.turn, muted.turn)
            assertEquals(immediatelyBeforeToggle.result, muted.result)
            assertEquals(immediatelyBeforeToggle.score, muted.score)
            assertEquals(immediatelyBeforeToggle.starter, muted.starter)
            assertEquals(immediatelyBeforeToggle.nextStarter, muted.nextStarter)
            assertEquals(immediatelyBeforeToggle.difficulty, muted.difficulty)
            assertEquals(immediatelyBeforeToggle.canChangeDifficulty, muted.canChangeDifficulty)
        }

        assertFalse(viewModel.uiState.value.soundEnabled)
        assertEquals(2, player.playedMarks.count { it == Player.X })
        assertEquals(1, player.playedMarks.count { it == Player.O })
        assertEquals(Player.X, player.playedMarks.last())

        // The single pending O response still completes, but cannot start sound muted.
        awaitHumanTurnAfterComputerMove(expectedBoardXCount = 2, expectedBoardOCount = 2)
        assertEquals(2, player.playedMarks.count { it == Player.X })
        assertEquals(1, player.playedMarks.count { it == Player.O })
        assertTrue(player.activeStreams.isEmpty())

        openMenu()
        composeRule.onNodeWithTag(GameMenuTags.SOUND).performClick()
        assertTrue(viewModel.uiState.value.soundEnabled)
        assertEquals(3, player.playedMarks.size) // unmute does not replay anything

        // The next accepted X after unmute produces exactly one new attempt/stream.
        val nextCell = viewModel.uiState.value.board.indexOf(Mark.Empty)
        assertTrue(nextCell >= 0)
        composeRule.onNodeWithTag("tic_tac_toe_cell_$nextCell").performClick()
        assertEquals(4, player.playedMarks.size)
        assertEquals(Player.X, player.playedMarks.last())
        assertEquals(3, player.playedMarks.count { it == Player.X })
        assertEquals(1, player.playedMarks.count { it == Player.O })
        assertEquals(1, player.activeStreams.size)
    }

    private fun openMenu() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithContentDescription(context.getString(R.string.menu_open)).performClick()
        composeRule.onNodeWithTag(GameMenuTags.MENU).assertExists()
    }

    private fun awaitHumanTurnAfterComputerMove(
        expectedBoardXCount: Int,
        expectedBoardOCount: Int,
        expectedPlayedMarks: List<Player>? = null,
    ) {
        composeRule.waitUntil(5_000) {
            val state = viewModel.uiState.value
            state.turn == Turn.Human &&
                state.board.count { it == Mark.X } == expectedBoardXCount &&
                state.board.count { it == Mark.O } == expectedBoardOCount &&
                (expectedPlayedMarks == null || player.playedMarks == expectedPlayedMarks)
        }
        // Sound is emitted after the state is published, in the same main-thread event: wait for it
        // to finish before the test thread reads the player.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {}
    }

    private class IndefiniteFakePlayer : MoveSoundPlayer {
        private var nextSampleId = 1
        private var nextStreamId = 100
        private val callbacks = mutableListOf<Pair<Int, (Int, Int) -> Unit>>()
        private val playerForSample = mutableMapOf<Int, Player>()
        val playedMarks = mutableListOf<Player>()
        val activeStreams = mutableSetOf<Int>()
        val stopStreams = mutableListOf<Int>()
        var completedLoadCount = 0
            private set

        override fun load(resourceId: Int, onLoaded: (sampleId: Int, status: Int) -> Unit): Int {
            val sampleId = nextSampleId++
            playerForSample[sampleId] = when (resourceId) {
                R.raw.game_move_x -> Player.X
                R.raw.game_move_o -> Player.O
                else -> error("Unexpected move sound resource $resourceId")
            }
            callbacks += sampleId to onLoaded
            return sampleId
        }

        fun completeLoads() = callbacks.forEach { (sampleId, callback) ->
            callback(sampleId, 0)
            completedLoadCount++
        }

        override fun play(sampleId: Int): Int {
            playedMarks += checkNotNull(playerForSample[sampleId]) { "Unknown sample ID $sampleId" }
            val streamId = nextStreamId++
            activeStreams += streamId
            return streamId
        }

        override fun stop(streamId: Int) {
            stopStreams += streamId
            activeStreams -= streamId
        }

        override fun release() {
            activeStreams.clear()
        }
    }
}
