package com.edu.unal.tictactoe.ui.game.session

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn

/**
 * Reusable fixtures: valid snapshots and corrupt encodings, each corrupt case derived from a
 * valid encoding by altering only what is needed to trip one rule. Also meant for the
 * ViewModel fallback tests of the next delivery.
 */
object GameSessionTestEncodings {
    const val ONE_SECOND = 1_000_000_000L

    // Positions in the encoded array (see GameSessionSnapshotCodec KDoc).
    const val VERSION = 0
    const val BOARD = 1
    const val TURN = 10
    const val RESULT = 11
    const val STARTER = 12
    const val NEXT_STARTER = 13
    const val SOUND = 14
    const val PENDING_FLAG = 15
    const val PENDING_REMAINING = 16

    /** Board from nine characters: 'X', 'O' or '.' (empty). */
    fun board(cells: String): List<Mark> {
        require(cells.length == 9)
        return cells.map {
            when (it) {
                'X' -> Mark.X
                'O' -> Mark.O
                else -> Mark.Empty
            }
        }
    }

    private fun snapshot(
        cells: String,
        turn: Turn,
        result: GameResult,
        starter: Player,
        sound: Boolean = true,
        pending: Long? = null,
    ) = GameSessionSnapshot(
        board = board(cells),
        turn = turn,
        result = result,
        starter = starter,
        nextStarter = if (starter == Player.X) Player.O else Player.X,
        soundEnabled = sound,
        pendingComputerRemainingNanos = pending,
    )

    val emptyStarterX = snapshot(".........", Turn.Human, GameResult.InProgress, Player.X)
    val emptyStarterOPending = snapshot(".........", Turn.Computer, GameResult.InProgress, Player.O, pending = ONE_SECOND)
    val emptyStarterOPendingMid = snapshot(".........", Turn.Computer, GameResult.InProgress, Player.O, sound = false, pending = 400_000_000L)
    val emptyStarterOPendingZero = snapshot(".........", Turn.Computer, GameResult.InProgress, Player.O, pending = 0L)
    val humanTurn = snapshot("XX.OO....", Turn.Human, GameResult.InProgress, Player.X, sound = false)
    val humanTurnStarterO = snapshot("OO.XX...O", Turn.Human, GameResult.InProgress, Player.O)
    val computerPending = snapshot("X........", Turn.Computer, GameResult.InProgress, Player.X, pending = 400_000_000L)
    val xWin = snapshot("XXXOO....", Turn.Human, GameResult.Won(Player.X), Player.X)
    val oWin = snapshot("XX.OOO..X", Turn.Computer, GameResult.Won(Player.O), Player.X, sound = false)
    val oWinStarterO = snapshot("OOOXX....", Turn.Computer, GameResult.Won(Player.O), Player.O)
    val tie = snapshot("XOXXOOOXX", Turn.Human, GameResult.Tie, Player.X)
    val tieStarterO = snapshot("OXOOXXXOO", Turn.Computer, GameResult.Tie, Player.O, sound = false)

    val validSnapshots: List<GameSessionSnapshot> = listOf(
        emptyStarterX, emptyStarterOPending, emptyStarterOPendingMid, emptyStarterOPendingZero,
        humanTurn, humanTurnStarterO, computerPending, xWin, oWin, oWinStarterO, tie, tieStarterO,
    )

    private fun enc(s: GameSessionSnapshot): LongArray = GameSessionSnapshotCodec.encode(s)

    private fun setBoard(encoded: LongArray, cells: String) {
        cells.forEachIndexed { i, ch ->
            encoded[BOARD + i] = when (ch) {
                'X' -> 1L
                'O' -> 2L
                else -> 0L
            }
        }
    }

    private fun altered(base: GameSessionSnapshot, change: (LongArray) -> Unit): LongArray =
        enc(base).also(change)

    /** Corrupt inputs that must all decode to null: description to input. */
    val corruptions: List<Pair<String, Any?>> = buildList {
        // Not an encoded snapshot at all.
        add("null" to null)
        add("String" to "snapshot")
        add("IntArray" to IntArray(GameSessionSnapshotCodec.LENGTH))
        add("Int" to 1)
        add("List" to List(GameSessionSnapshotCodec.LENGTH) { 0L })
        // R1 length.
        add("length shorter" to enc(humanTurn).copyOf(GameSessionSnapshotCodec.LENGTH - 1))
        add("length longer" to enc(humanTurn).copyOf(GameSessionSnapshotCodec.LENGTH + 1))
        add("length zero" to LongArray(0))
        // R1 version.
        for (v in longArrayOf(0L, 2L, -1L, Long.MAX_VALUE)) {
            add("version $v" to altered(humanTurn) { it[VERSION] = v })
        }
        // R1 codes out of range, above and negative.
        for (v in longArrayOf(3L, -1L)) add("mark code $v" to altered(humanTurn) { it[BOARD + 8] = v })
        for (v in longArrayOf(2L, -1L)) add("turn code $v" to altered(humanTurn) { it[TURN] = v })
        for (v in longArrayOf(4L, -1L)) add("result code $v" to altered(humanTurn) { it[RESULT] = v })
        for (v in longArrayOf(2L, -1L)) add("starter code $v" to altered(humanTurn) { it[STARTER] = v })
        for (v in longArrayOf(2L, -1L)) add("nextStarter code $v" to altered(humanTurn) { it[NEXT_STARTER] = v })
        for (v in longArrayOf(2L, -1L)) add("sound code $v" to altered(humanTurn) { it[SOUND] = v })
        for (v in longArrayOf(2L, -1L)) add("pending flag $v" to altered(humanTurn) { it[PENDING_FLAG] = v })
        // R1 remaining.
        add("remaining negative" to altered(computerPending) { it[PENDING_REMAINING] = -1L })
        add("remaining over 1 s" to altered(computerPending) { it[PENDING_REMAINING] = ONE_SECOND + 1 })
        add("remaining timestamp-like" to altered(computerPending) { it[PENDING_REMAINING] = Long.MAX_VALUE })
        add("remaining nonzero with flag 0" to altered(humanTurn) { it[PENDING_REMAINING] = 5L })
        // R2 board intrinsically illegal.
        add("both players win" to altered(xWin) { setBoard(it, "XXXOOO...") })
        add("count difference 2" to altered(humanTurn) { setBoard(it, "XX.......") })
        // R3 result does not match the board.
        add("in progress over won board" to altered(xWin) { it[RESULT] = 0L })
        add("won over board without line" to altered(humanTurn) { it[RESULT] = 1L })
        add("tie over non-full board" to altered(humanTurn) { it[RESULT] = 3L })
        add("wrong winner" to altered(xWin) { it[RESULT] = 2L })
        // R4
        add("starter equals nextStarter" to altered(humanTurn) { it[NEXT_STARTER] = it[STARTER] })
        // R5
        add("counts incoherent with starter" to altered(computerPending) { it[STARTER] = 1L; it[NEXT_STARTER] = 0L })
        // R6 in progress.
        add("in progress turn should be human" to altered(humanTurn) { it[TURN] = 1L; it[PENDING_FLAG] = 1L; it[PENDING_REMAINING] = 5L })
        add("in progress turn should be computer" to altered(computerPending) { it[TURN] = 0L; it[PENDING_FLAG] = 0L; it[PENDING_REMAINING] = 0L })
        // R6 terminal.
        add("terminal x turn computer" to altered(xWin) { it[TURN] = 1L })
        add("terminal o turn human" to altered(oWin) { it[TURN] = 0L })
        add("winner did not move last" to altered(xWin) { setBoard(it, "XXXOO.O.."); it[TURN] = 1L })
        // R7
        add("pending absent on O turn" to altered(computerPending) { it[PENDING_FLAG] = 0L; it[PENDING_REMAINING] = 0L })
        add("pending present on human turn" to altered(humanTurn) { it[PENDING_FLAG] = 1L; it[PENDING_REMAINING] = 5L })
        add("pending present on terminal" to altered(xWin) { it[PENDING_FLAG] = 1L; it[PENDING_REMAINING] = 5L })
        add("pending present on terminal o" to altered(oWin) { it[PENDING_FLAG] = 1L; it[PENDING_REMAINING] = 5L })
    }
}
