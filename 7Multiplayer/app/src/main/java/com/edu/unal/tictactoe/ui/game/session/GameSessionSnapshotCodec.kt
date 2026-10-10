package com.edu.unal.tictactoe.ui.game.session

import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn

/**
 * Versioned encoding of [GameSessionSnapshot] as a single `LongArray`, which is
 * Bundle-compatible, stored under one key (atomic) and testable on the JVM.
 *
 * Layout (length [LENGTH]), all values stable integer codes, never enum ordinals:
 * ```
 * [0]      version (VERSION)
 * [1..9]   board marks: Empty=0, X=1, O=2
 * [10]     turn: Human=0, Computer=1
 * [11]     result: InProgress=0, Won(X)=1, Won(O)=2, Tie=3
 * [12]     starter: X=0, O=1
 * [13]     nextStarter: X=0, O=1
 * [14]     soundEnabled: false=0, true=1
 * [15]     pending computer turn flag: absent=0, present=1
 * [16]     pending remaining nanoseconds in [0, 1_000_000_000]; 0 when the flag is 0
 * ```
 * Versioning: [VERSION] is 1 and element `[0]` must equal it. An unknown version (older or newer),
 * a different length or any out-of-range code makes [decode] return `null`; there is no partial or
 * best-effort reading, so a future layout must bump the version.
 *
 * [decode] accepts only a fully coherent snapshot and otherwise returns `null`: the caller then
 * discards the value entirely and starts a fresh session (empty board, X to start, sound on) with
 * the current preferences. See [GameSessionSnapshot] for the excluded fields and the limits of
 * restoration.
 */
object GameSessionSnapshotCodec {
    const val VERSION = 1
    const val LENGTH = 17
    const val MAX_PENDING_NANOS = 1_000_000_000L

    private const val VERSION_INDEX = 0
    private const val BOARD_START = 1
    private const val TURN_INDEX = 10
    private const val RESULT_INDEX = 11
    private const val STARTER_INDEX = 12
    private const val NEXT_STARTER_INDEX = 13
    private const val SOUND_INDEX = 14
    private const val PENDING_FLAG_INDEX = 15
    private const val PENDING_REMAINING_INDEX = 16

    fun encode(snapshot: GameSessionSnapshot): LongArray {
        val out = LongArray(LENGTH)
        out[VERSION_INDEX] = VERSION.toLong()
        snapshot.board.forEachIndexed { index, mark -> out[BOARD_START + index] = markCode(mark) }
        out[TURN_INDEX] = if (snapshot.turn == Turn.Human) 0L else 1L
        out[RESULT_INDEX] = resultCode(snapshot.result)
        out[STARTER_INDEX] = playerCode(snapshot.starter)
        out[NEXT_STARTER_INDEX] = playerCode(snapshot.nextStarter)
        out[SOUND_INDEX] = if (snapshot.soundEnabled) 1L else 0L
        val remaining = snapshot.pendingComputerRemainingNanos
        out[PENDING_FLAG_INDEX] = if (remaining != null) 1L else 0L
        out[PENDING_REMAINING_INDEX] = remaining ?: 0L
        return out
    }

    /** Returns `null` for anything that is not a fully valid snapshot; never throws or mutates [encoded]. */
    fun decode(encoded: Any?): GameSessionSnapshot? {
        val source = (encoded as? LongArray)?.takeIf { it.size == LENGTH } ?: return null
        val raw = source.copyOf()
        if (raw[VERSION_INDEX] != VERSION.toLong()) return null

        val board = ArrayList<Mark>(GameSessionSnapshot.BOARD_SIZE)
        for (index in 0 until GameSessionSnapshot.BOARD_SIZE) {
            board.add(markFromCode(raw[BOARD_START + index]) ?: return null)
        }
        val turn = when (raw[TURN_INDEX]) {
            0L -> Turn.Human
            1L -> Turn.Computer
            else -> return null
        }
        val result = resultFromCode(raw[RESULT_INDEX]) ?: return null
        val starter = playerFromCode(raw[STARTER_INDEX]) ?: return null
        val nextStarter = playerFromCode(raw[NEXT_STARTER_INDEX]) ?: return null
        val soundEnabled = when (raw[SOUND_INDEX]) {
            0L -> false
            1L -> true
            else -> return null
        }
        val remaining = raw[PENDING_REMAINING_INDEX]
        val pending: Long? = when (raw[PENDING_FLAG_INDEX]) {
            0L -> if (remaining == 0L) null else return null
            1L -> if (remaining in 0L..MAX_PENDING_NANOS) remaining else return null
            else -> return null
        }

        if (!isCoherent(board, turn, result, starter, nextStarter, pending != null)) return null
        return GameSessionSnapshot(board, turn, result, starter, nextStarter, soundEnabled, pending)
    }

    private fun isCoherent(
        board: List<Mark>,
        turn: Turn,
        result: GameResult,
        starter: Player,
        nextStarter: Player,
        hasPending: Boolean,
    ): Boolean {
        // R2 and R3: intrinsically legal board whose outcome is exactly the stored result.
        val engine = TicTacToeGame()
        if (!engine.restoreBoard(board)) return false
        if (engine.checkForWinner() != result) return false
        // R4
        if (nextStarter == starter) return false
        // R5
        val xCount = board.count { it == Mark.X }
        val oCount = board.count { it == Mark.O }
        val starterExtra = if (starter == Player.X) xCount - oCount else oCount - xCount
        if (starterExtra != 0 && starterExtra != 1) return false
        // R6
        val other = if (starter == Player.X) Player.O else Player.X
        val inProgress = result == GameResult.InProgress
        // In progress: the player to move; terminal: the player who made the last move.
        val relevantPlayer = if (inProgress == (starterExtra == 0)) starter else other
        if (result is GameResult.Won && result.player != relevantPlayer) return false
        if ((turn == Turn.Human) != (relevantPlayer == Player.X)) return false
        // R7
        return hasPending == (inProgress && turn == Turn.Computer)
    }

    private fun markCode(mark: Mark): Long = when (mark) {
        Mark.Empty -> 0L
        Mark.X -> 1L
        Mark.O -> 2L
    }

    private fun markFromCode(code: Long): Mark? = when (code) {
        0L -> Mark.Empty
        1L -> Mark.X
        2L -> Mark.O
        else -> null
    }

    private fun resultCode(result: GameResult): Long = when (result) {
        GameResult.InProgress -> 0L
        is GameResult.Won -> if (result.player == Player.X) 1L else 2L
        GameResult.Tie -> 3L
    }

    private fun resultFromCode(code: Long): GameResult? = when (code) {
        0L -> GameResult.InProgress
        1L -> GameResult.Won(Player.X)
        2L -> GameResult.Won(Player.O)
        3L -> GameResult.Tie
        else -> null
    }

    private fun playerCode(player: Player): Long = if (player == Player.X) 0L else 1L

    private fun playerFromCode(code: Long): Player? = when (code) {
        0L -> Player.X
        1L -> Player.O
        else -> null
    }
}
