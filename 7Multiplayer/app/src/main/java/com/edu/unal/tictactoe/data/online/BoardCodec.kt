package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.models.Mark

/** Converts a board to and from its nine-character wire form (`-` empty, `X`, `O`). */
object BoardCodec {
    private const val BOARD_SIZE = 9
    private const val EMPTY = '-'

    fun encode(board: List<Mark>): String {
        require(board.size == BOARD_SIZE) { "A board has $BOARD_SIZE cells" }
        return board.joinToString("") { mark ->
            when (mark) {
                Mark.Empty -> EMPTY.toString()
                Mark.X -> "X"
                Mark.O -> "O"
            }
        }
    }

    /** Returns null when [text] is not exactly nine valid characters. */
    fun decode(text: String): List<Mark>? {
        if (text.length != BOARD_SIZE) {
            return null
        }
        return text.map { char ->
            when (char) {
                EMPTY -> Mark.Empty
                'X' -> Mark.X
                'O' -> Mark.O
                else -> return null
            }
        }
    }
}
