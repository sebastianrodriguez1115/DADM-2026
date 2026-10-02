package com.edu.unal.tictactoe

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameViewModel
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TIMING_TAG = "Reto4Timing"
private const val SAMPLE_COUNT = 5

// Times newGame() logic for the Impossible O opening on the main thread.
// Not first-frame latency, not a cold-start guarantee, not a statistical benchmark.
@RunWith(AndroidJUnit4::class)
class ImpossibleOpeningTimingTest {
    @Test
    fun impossibleOpeningFromEmptyBoardRunsOneMoveAndRecordsSamples() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val failFast = MoveRandomizer { bound -> error("Impossible must not use RNG (bound=$bound)") }
        val durations = ArrayList<Long>(SAMPLE_COUNT)
        repeat(SAMPLE_COUNT) {
            instrumentation.runOnMainSync {
                val viewModel = GameViewModel(TicTacToeGame(failFast))
                assertTrue(viewModel.setDifficulty(DifficultyLevel.Impossible))
                val start = SystemClock.elapsedRealtimeNanos()
                viewModel.newGame()
                val elapsed = SystemClock.elapsedRealtimeNanos() - start
                val state = viewModel.uiState.value
                assertEquals(listOf(Mark.O) + List(8) { Mark.Empty }, state.board)
                assertEquals(Turn.Human, state.turn)
                assertEquals(GameResult.InProgress, state.result)
                assertEquals(DifficultyLevel.Impossible, state.difficulty)
                assertEquals(Score(), state.score)
                assertFalse(state.canChangeDifficulty)
                durations += elapsed
            }
        }
        assertEquals(SAMPLE_COUNT, durations.size)
        val line = String.format(
            Locale.US,
            "sdk=%d samples_ns=%s first_ms=%.3f median_ms=%.3f",
            Build.VERSION.SDK_INT, durations.joinToString(","),
            durations.first() / 1_000_000.0,
            durations.sorted()[SAMPLE_COUNT / 2] / 1_000_000.0,
        )
        Log.i(TIMING_TAG, line)
        println("$TIMING_TAG $line")
    }
}
