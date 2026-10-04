package com.edu.unal.tictactoe.ui.game.session

import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.session.GameSessionTestEncodings as Fx
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GameSessionSnapshotCodecTest {
    private val codec = GameSessionSnapshotCodec

    // Criterion 1: round-trip.

    @Test
    fun roundTripPreservesEveryRepresentativeValidState() {
        for (snapshot in Fx.validSnapshots) {
            assertEquals(snapshot.toString(), snapshot, codec.decode(codec.encode(snapshot)))
        }
    }

    @Test
    fun representativeStatesCoverBothSoundValuesBothStartersAndAllResults() {
        val all = Fx.validSnapshots
        assertTrue(all.any { it.soundEnabled } && all.any { !it.soundEnabled })
        assertTrue(all.any { it.starter == Player.X } && all.any { it.starter == Player.O })
        assertTrue(all.any { it.result == GameResult.InProgress })
        assertTrue(all.any { it.result == GameResult.Won(Player.X) })
        assertTrue(all.any { it.result == GameResult.Won(Player.O) })
        assertTrue(all.any { it.result == GameResult.Tie })
        val pendings = all.mapNotNull { it.pendingComputerRemainingNanos }.toSet()
        assertTrue(pendings.containsAll(listOf(Fx.ONE_SECOND, 400_000_000L, 0L)))
    }

    // Criterion 2: defensive copies.

    @Test
    fun mutatingTheListGivenToTheConstructorDoesNotChangeTheSnapshot() {
        val source = Fx.board("X........").toMutableList()
        val snapshot = GameSessionSnapshot(
            source, Turn.Computer, GameResult.InProgress, Player.X, Player.O, true, 1L,
        )
        source[8] = Mark.O
        assertEquals(Fx.board("X........"), snapshot.board)
    }

    @Test
    fun snapshotBoardIsNotMutable() {
        @Suppress("UNCHECKED_CAST")
        val exposed = Fx.emptyStarterX.board as MutableList<Mark>
        try {
            exposed[0] = Mark.X
            fail("board must be unmodifiable")
        } catch (_: UnsupportedOperationException) {
        }
        assertEquals(Fx.board("........."), Fx.emptyStarterX.board)
    }

    @Test
    fun mutatingTheEncodedArrayDoesNotAffectLaterEncodesOrTheSnapshot() {
        val snapshot = Fx.humanTurn
        val first = codec.encode(snapshot)
        val pristine = first.copyOf()
        first.fill(99L)
        assertArrayEquals(pristine, codec.encode(snapshot))
        assertEquals(Fx.board("XX.OO...."), snapshot.board)
    }

    @Test
    fun encodeReturnsANewArrayOnEveryCall() {
        assertTrue(codec.encode(Fx.humanTurn) !== codec.encode(Fx.humanTurn))
    }

    @Test
    fun mutatingTheInputArrayAfterDecodeDoesNotChangeTheDecodedSnapshot() {
        val encoded = codec.encode(Fx.humanTurn)
        val decoded = codec.decode(encoded)
        assertNotNull(decoded)
        encoded.fill(7L)
        assertEquals(Fx.humanTurn, decoded)
    }

    @Test
    fun decodeDoesNotModifyItsInput() {
        for (snapshot in Fx.validSnapshots) {
            val encoded = codec.encode(snapshot)
            val copy = encoded.copyOf()
            codec.decode(encoded)
            assertArrayEquals(copy, encoded)
        }
    }

    // Criterion 3: bounded content.

    @Test
    fun layoutIsFixedAndDocumented() {
        assertEquals(17, codec.LENGTH)
        val encoded = codec.encode(Fx.computerPending)
        assertEquals(codec.LENGTH, encoded.size)
        // version, 9 marks, turn, result, starter, nextStarter, sound, pending flag, remaining
        assertArrayEquals(
            longArrayOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 400_000_000L),
            encoded,
        )
        for (snapshot in Fx.validSnapshots) assertEquals(codec.LENGTH, codec.encode(snapshot).size)
    }

    @Test
    fun snapshotHasNoScoreDifficultyOrInstantProperties() {
        val declared = GameSessionSnapshot::class.java.declaredFields
            .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }.toSet()
        assertEquals(
            setOf("board", "turn", "result", "starter", "nextStarter", "soundEnabled", "pendingComputerRemainingNanos"),
            declared,
        )
    }

    @Test
    fun pendingRemainingAboveOneSecondIsRejectedSoAnAbsoluteTimestampCannotBeStored() {
        val encoded = codec.encode(Fx.computerPending)
        encoded[Fx.PENDING_REMAINING] = 123_456_789_012_345L
        assertNull(codec.decode(encoded))
        encoded[Fx.PENDING_REMAINING] = Fx.ONE_SECOND + 1
        assertNull(codec.decode(encoded))
        encoded[Fx.PENDING_REMAINING] = Fx.ONE_SECOND
        assertNotNull(codec.decode(encoded))
    }

    // Criterion 4: stable codes.

    @Test
    fun versionAndCodesAreStable() {
        assertEquals(1, codec.VERSION)
        // Marks: Empty 0, X 1, O 2.
        val marks = codec.encode(
            GameSessionSnapshot(
                Fx.board("XO......."), Turn.Computer, GameResult.InProgress, Player.X, Player.O, true, 0L,
            ),
        )
        assertEquals(listOf(1L, 2L, 0L), listOf(marks[Fx.BOARD], marks[Fx.BOARD + 1], marks[Fx.BOARD + 2]))
        // Turn: Human 0, Computer 1.
        assertEquals(0L, codec.encode(Fx.humanTurn)[Fx.TURN])
        assertEquals(1L, codec.encode(Fx.computerPending)[Fx.TURN])
        // Result: InProgress 0, Won(X) 1, Won(O) 2, Tie 3.
        assertEquals(0L, codec.encode(Fx.humanTurn)[Fx.RESULT])
        assertEquals(1L, codec.encode(Fx.xWin)[Fx.RESULT])
        assertEquals(2L, codec.encode(Fx.oWin)[Fx.RESULT])
        assertEquals(3L, codec.encode(Fx.tie)[Fx.RESULT])
        // Player: X 0, O 1 (starter and nextStarter).
        val starterO = codec.encode(Fx.emptyStarterOPending)
        assertEquals(1L, starterO[Fx.STARTER])
        assertEquals(0L, starterO[Fx.NEXT_STARTER])
        val starterX = codec.encode(Fx.emptyStarterX)
        assertEquals(0L, starterX[Fx.STARTER])
        assertEquals(1L, starterX[Fx.NEXT_STARTER])
        // Booleans: false 0, true 1; pending flag absent 0, present 1.
        assertEquals(1L, codec.encode(Fx.emptyStarterX)[Fx.SOUND])
        assertEquals(0L, codec.encode(Fx.humanTurn)[Fx.SOUND])
        assertEquals(0L, codec.encode(Fx.humanTurn)[Fx.PENDING_FLAG])
        assertEquals(1L, codec.encode(Fx.computerPending)[Fx.PENDING_FLAG])
        assertEquals(1L, codec.encode(Fx.humanTurn)[Fx.VERSION])
    }

    // Task 2.3 (codec half): atomic rejection of every class of corruption.

    @Test
    fun everyCorruptionDecodesToNullWithoutThrowing() {
        assertTrue(Fx.corruptions.size > 40)
        for ((name, input) in Fx.corruptions) {
            val decoded = try {
                codec.decode(input)
            } catch (e: Throwable) {
                fail("$name threw $e")
                null
            }
            assertNull(name, decoded)
        }
    }

    @Test
    fun aRejectedArrayIsLeftIdentical() {
        for ((name, input) in Fx.corruptions) {
            val array = input as? LongArray ?: continue
            val copy = array.copyOf()
            assertNull(name, codec.decode(array))
            assertArrayEquals(name, copy, array)
        }
    }

    @Test
    fun everyCorruptionIsOneEditAwayFromAValidEncodingOrNotAnArray() {
        // Sanity: the unaltered bases decode, so rejections come from the alteration.
        for (snapshot in Fx.validSnapshots) assertNotNull(codec.decode(codec.encode(snapshot)))
        assertFalse(Fx.corruptions.any { it.second is LongArray && (it.second as LongArray).size == codec.LENGTH && codec.decode(it.second) != null })
    }
}
