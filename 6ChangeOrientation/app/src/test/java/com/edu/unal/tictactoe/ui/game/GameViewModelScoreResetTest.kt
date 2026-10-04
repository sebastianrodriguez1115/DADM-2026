package com.edu.unal.tictactoe.ui.game

import com.edu.unal.tictactoe.data.preferences.FakeGamePreferences
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundSink
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelScoreResetTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private val seededScore = Score(xWins = 3, oWins = 5, ties = 2)
    private val xWinMoves = listOf(0, 3, 4, 8)

    private fun viewModel(preferences: FakeGamePreferences): GameViewModel =
        harness.createViewModelResumed(TicTacToeGame(MoveRandomizer { 0 }), preferences)

    private fun advance(millis: Long) {
        harness.scheduler.advanceTimeBy(millis)
        harness.scheduler.runCurrent()
    }

    private fun playMove(viewModel: GameViewModel, location: Int) {
        viewModel.onHumanMove(location)
        advance(1_000)
    }

    private fun assertSameExceptScore(before: GameUiState, after: GameUiState) {
        assertEquals(before.board, after.board)
        assertEquals(before.turn, after.turn)
        assertEquals(before.result, after.result)
        assertEquals(before.starter, after.starter)
        assertEquals(before.nextStarter, after.nextStarter)
        assertEquals(before.difficulty, after.difficulty)
        assertEquals(before.soundEnabled, after.soundEnabled)
    }

    private fun GameViewModel.oMarks(): Int = uiState.value.board.count { it == Mark.O }

    @Test
    fun activeGameWithPendingResponseKeepsEverythingAndOMovesAtItsOriginalTime() {
        val preferences = FakeGamePreferences(score = seededScore, difficulty = DifficultyLevel.Hard)
        val viewModel = viewModel(preferences)
        viewModel.onHumanMove(4)
        advance(400)
        val before = viewModel.uiState.value

        viewModel.resetScores()

        val after = viewModel.uiState.value
        assertEquals(Score(), after.score)
        assertEquals(Score(), preferences.storedScore)
        assertEquals(1, preferences.scoreWrites)
        assertSameExceptScore(before, after)
        assertEquals(Turn.Computer, after.turn)
        advance(599)
        assertEquals(0, viewModel.oMarks())
        advance(1)
        assertEquals(1, viewModel.oMarks())
        assertEquals(Turn.Human, viewModel.uiState.value.turn)
        advance(5_000)
        assertEquals(1, viewModel.oMarks())
        assertEquals(1, preferences.scoreWrites)
    }

    @Test
    fun pendingOpeningIsNeitherAdvancedNorRestarted() {
        val preferences = FakeGamePreferences(score = seededScore)
        val viewModel = viewModel(preferences)
        viewModel.newGame()
        advance(400)
        val before = viewModel.uiState.value
        assertEquals(Player.O, before.starter)
        assertEquals(0, viewModel.oMarks())

        viewModel.resetScores()

        assertSameExceptScore(before, viewModel.uiState.value)
        assertEquals(Score(), viewModel.uiState.value.score)
        advance(599)
        assertEquals(0, viewModel.oMarks())
        advance(1)
        assertEquals(1, viewModel.oMarks())
        advance(5_000)
        assertEquals(1, viewModel.oMarks())
    }

    @Test
    fun scoredTerminalGameKeepsItsResultAndIsNeverScoredAgain() {
        val preferences = FakeGamePreferences(score = seededScore)
        val viewModel = viewModel(preferences)
        xWinMoves.forEach { playMove(viewModel, it) }
        assertEquals(1, preferences.scoreWrites)
        val before = viewModel.uiState.value
        assertEquals(GameResult.Won(Player.X), before.result)

        viewModel.resetScores()

        assertEquals(Score(), viewModel.uiState.value.score)
        assertEquals(Score(), preferences.storedScore)
        assertEquals(2, preferences.scoreWrites)
        assertSameExceptScore(before, viewModel.uiState.value)

        (0..8).forEach { playMove(viewModel, it) }
        viewModel.onHostPaused()
        viewModel.onHostResumed()
        viewModel.toggleSound()
        viewModel.toggleSound()
        viewModel.setDifficulty(DifficultyLevel.Hard)
        advance(5_000)

        assertEquals(Score(), viewModel.uiState.value.score)
        assertEquals(Score(), preferences.storedScore)
        assertEquals(2, preferences.scoreWrites)
        assertEquals(before.board, viewModel.uiState.value.board)
        assertEquals(before.result, viewModel.uiState.value.result)
    }

    @Test
    fun nextGameAfterResetScoresFromZeroWithOneMoreWrite() {
        val preferences = FakeGamePreferences(score = seededScore)
        val viewModel = viewModel(preferences)
        xWinMoves.forEach { playMove(viewModel, it) }
        viewModel.resetScores()
        assertEquals(2, preferences.scoreWrites)

        viewModel.newGame()
        advance(1_000)
        assertEquals(2, preferences.scoreWrites)
        // O opened on cell 0 and keeps taking the first free cell: O completes the top row.
        playMove(viewModel, 3)
        playMove(viewModel, 4)

        val state = viewModel.uiState.value
        assertEquals(GameResult.Won(Player.O), state.result)
        assertEquals(Score(oWins = 1), state.score)
        assertEquals(Score(oWins = 1), preferences.storedScore)
        assertEquals(3, preferences.scoreWrites)
    }

    @Test
    fun mutedSoundStaysMutedAndResetNeverTouchesTheSink() {
        val preferences = FakeGamePreferences(score = seededScore)
        val viewModel = viewModel(preferences)
        val sink = RecordingSink()
        viewModel.registerMoveSoundSink(Any(), sink)
        viewModel.onHumanMove(4)
        advance(400)
        val playsBefore = sink.plays
        val stopsBefore = sink.stops

        viewModel.resetScores()

        assertEquals(playsBefore, sink.plays)
        assertEquals(stopsBefore, sink.stops)
        assertEquals(true, viewModel.uiState.value.soundEnabled)

        viewModel.toggleSound()
        val stopsAfterMute = sink.stops
        val playsAfterMute = sink.plays
        viewModel.resetScores()

        assertEquals(false, viewModel.uiState.value.soundEnabled)
        assertEquals(playsAfterMute, sink.plays)
        assertEquals(stopsAfterMute, sink.stops)
    }

    @Test
    fun resetNeitherWritesDifficultyNorReadsTheStore() {
        val preferences = FakeGamePreferences(score = seededScore, difficulty = DifficultyLevel.Impossible)
        val viewModel = viewModel(preferences)
        viewModel.onHumanMove(4)
        advance(1_000)

        viewModel.resetScores()

        assertEquals(0, preferences.difficultyWrites)
        assertEquals(DifficultyLevel.Impossible, preferences.storedDifficulty)
        assertEquals(DifficultyLevel.Impossible, viewModel.uiState.value.difficulty)
        assertEquals(1, preferences.scoreReads)
        assertEquals(1, preferences.difficultyReads)
    }

    @Test
    fun resetWithScoreAlreadyZeroChangesNothingElse() {
        val preferences = FakeGamePreferences()
        val viewModel = viewModel(preferences)
        viewModel.onHumanMove(4)
        advance(400)
        val before = viewModel.uiState.value

        viewModel.resetScores()

        assertEquals(Score(), viewModel.uiState.value.score)
        assertEquals(Score(), preferences.storedScore)
        assertSameExceptScore(before, viewModel.uiState.value)
    }

    @Test
    fun newSessionOnTheSameStoreAfterResetStartsAtZero() {
        val preferences = FakeGamePreferences(score = seededScore)
        val first = viewModel(preferences)
        first.resetScores()

        val second = viewModel(preferences)

        assertEquals(Score(), second.uiState.value.score)
    }

    private class RecordingSink : MoveSoundSink {
        var plays = listOf<Player>()
            private set
        var stops = 0
            private set

        override fun play(player: Player) {
            plays = plays + player
        }

        override fun stop() {
            stops++
        }
    }
}
