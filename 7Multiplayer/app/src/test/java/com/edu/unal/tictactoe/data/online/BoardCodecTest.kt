package com.edu.unal.tictactoe.data.online

import com.edu.unal.tictactoe.game.models.Mark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardCodecTest {
    private val mixed = listOf(
        Mark.X, Mark.Empty, Mark.O,
        Mark.Empty, Mark.X, Mark.Empty,
        Mark.O, Mark.Empty, Mark.X,
    )

    @Test
    fun emptyBoardEncodesAndDecodes() {
        assertEquals("---------", BoardCodec.encode(List(9) { Mark.Empty }))
        assertEquals(List(9) { Mark.Empty }, BoardCodec.decode("---------"))
    }

    @Test
    fun mixedMarksEncodeAndDecode() {
        assertEquals("X-O-X-O-X", BoardCodec.encode(mixed))
        assertEquals(mixed, BoardCodec.decode("X-O-X-O-X"))
    }

    @Test
    fun roundTripPreservesTheBoard() {
        assertEquals(mixed, BoardCodec.decode(BoardCodec.encode(mixed)))
        assertEquals("XOXOXOXOX", BoardCodec.encode(BoardCodec.decode("XOXOXOXOX")!!))
    }

    @Test
    fun lengthOtherThanNineIsRejected() {
        assertNull(BoardCodec.decode(""))
        assertNull(BoardCodec.decode("--------"))
        assertNull(BoardCodec.decode("----------"))
    }

    @Test
    fun invalidCharacterIsRejected() {
        assertNull(BoardCodec.decode("--------Z"))
        assertNull(BoardCodec.decode("x--------"))
        assertNull(BoardCodec.decode("        -"))
    }
}
