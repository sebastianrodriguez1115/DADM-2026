package com.edu.unal.tictactoe

import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.MoveRandomizer
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TIMING_TAG = "Reto4Timing"
private const val SAMPLE_COUNT = 5

// Measures only Impossible move selection on Main, excluding game setup and the 1000 ms turn delay.
// Not a startup/render benchmark, first-frame latency, cold-start guarantee, or statistical benchmark.
@RunWith(AndroidJUnit4::class)
class ImpossibleOpeningTimingTest {
    @Test
    fun impossibleOpeningFromEmptyBoardRunsOneMoveAndRecordsSamples() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val failFast = MoveRandomizer { bound -> error("Impossible must not use RNG (bound=$bound)") }
        val durations = ArrayList<Long>(SAMPLE_COUNT)
        repeat(SAMPLE_COUNT) {
            // Each sample starts from a fresh empty board; setup and move application are untimed.
            val game = TicTacToeGame(failFast)
            instrumentation.runOnMainSync {
                assertTrue(Looper.myLooper() == Looper.getMainLooper())
                assertEquals(List(9) { Mark.Empty }, game.board)
                val start = SystemClock.elapsedRealtimeNanos()
                val selection = game.getComputerMove(DifficultyLevel.Impossible)
                val elapsed = SystemClock.elapsedRealtimeNanos() - start
                assertTrue("Impossible must select one opening move", selection is ComputerMoveResult.Selected)
                val selectedIndex = (selection as ComputerMoveResult.Selected).index
                assertEquals(List(9) { Mark.Empty }, game.board) // Selection does not mutate the board.
                assertEquals(MoveResult.Applied, game.setMove(Player.O, selectedIndex))
                assertEquals(1, game.board.count { it == Mark.O })
                assertFalse(game.board.contains(Mark.X))
                assertEquals(8, game.board.count { it == Mark.Empty })
                assertEquals(GameResult.InProgress, game.checkForWinner())
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
