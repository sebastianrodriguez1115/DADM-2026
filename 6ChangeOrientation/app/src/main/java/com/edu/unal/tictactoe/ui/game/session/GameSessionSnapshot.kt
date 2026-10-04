package com.edu.unal.tictactoe.ui.game.session

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import java.util.Collections

/**
 * Immutable, data-only picture of a live game session, used only to survive
 * framework-driven recreation (rotation and recreation after process death). Its encoding and
 * versioning live in [GameSessionSnapshotCodec]; when it is captured and restored is described
 * on `GameViewModel`.
 *
 * Boundary between temporary and permanent data: this snapshot is temporary framework state
 * (it lives in the `SavedStateHandle`, i.e. the Activity's saved instance state). The score and
 * the difficulty are permanent data owned by the preferences in `SharedPreferences`, and sound
 * lives only here. Fields deliberately excluded: score, difficulty, jobs, `Context` or any
 * Android object, audio sinks and absolute instants (clock readings or deadlines).
 *
 * [pendingComputerRemainingNanos] is a remaining duration (not a deadline); it is `null`
 * when there is no pending turn of O.
 *
 * Restoration limits: only what Android delivers as instance state is restored. After the task
 * is closed, a Back that finishes the Activity, cleared app data or a force-stop there is no
 * saved state, a new session starts, and nothing is guaranteed in those cases.
 */
class GameSessionSnapshot(
    board: List<Mark>,
    val turn: Turn,
    val result: GameResult,
    val starter: Player,
    val nextStarter: Player,
    val soundEnabled: Boolean,
    val pendingComputerRemainingNanos: Long?,
) {
    /** Unmodifiable defensive copy of the nine marks. */
    val board: List<Mark> = Collections.unmodifiableList(ArrayList(board))

    init {
        require(this.board.size == BOARD_SIZE) { "A snapshot board has exactly $BOARD_SIZE marks" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameSessionSnapshot) return false
        return board == other.board && turn == other.turn && result == other.result &&
            starter == other.starter && nextStarter == other.nextStarter &&
            soundEnabled == other.soundEnabled &&
            pendingComputerRemainingNanos == other.pendingComputerRemainingNanos
    }

    override fun hashCode(): Int {
        var hash = board.hashCode()
        hash = 31 * hash + turn.hashCode()
        hash = 31 * hash + result.hashCode()
        hash = 31 * hash + starter.hashCode()
        hash = 31 * hash + nextStarter.hashCode()
        hash = 31 * hash + soundEnabled.hashCode()
        hash = 31 * hash + (pendingComputerRemainingNanos?.hashCode() ?: 0)
        return hash
    }

    override fun toString(): String =
        "GameSessionSnapshot(board=$board, turn=$turn, result=$result, starter=$starter, " +
            "nextStarter=$nextStarter, soundEnabled=$soundEnabled, " +
            "pendingComputerRemainingNanos=$pendingComputerRemainingNanos)"

    companion object {
        const val BOARD_SIZE = 9

        /** Single SavedStateHandle key holding the codec's `LongArray`. */
        const val SAVED_STATE_KEY = "game_session_snapshot"
    }
}
