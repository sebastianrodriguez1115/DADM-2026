package com.edu.unal.tictactoe

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshot
import com.edu.unal.tictactoe.ui.game.session.GameSessionSnapshotCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Snapshot codec against the real Android [Bundle] and [android.os.Parcel] (no JVM stub). */
@RunWith(AndroidJUnit4::class)
class GameSessionSnapshotBundleTest {
    private val key = GameSessionSnapshot.SAVED_STATE_KEY
    private val loader: ClassLoader? = GameSessionSnapshotBundleTest::class.java.classLoader

    @Test
    fun stateKeyIsTheStableFrameworkKey() {
        assertEquals("game_session_snapshot", key)
    }

    @Test
    fun representativeValidSnapshotsSurviveRealBundleAndParcelRoundTrip() {
        for ((name, snapshot) in validSnapshots()) {
            val bundle = Bundle().apply { putLongArray(key, GameSessionSnapshotCodec.encode(snapshot)) }
            val read = parcelRoundTrip(bundle, loader).readAny(key)
            assertNotNull(name, read)
            assertEquals(name, snapshot, GameSessionSnapshotCodec.decode(read))
            assertEquals("$name (typed read)", snapshot, GameSessionSnapshotCodec.decode(readLongArray(bundle)))
        }
    }

    @Test
    fun roundTripReturnsADistinctArrayAndLeavesTheBundleUntouched() {
        val snapshot = validSnapshots().getValue("in progress human turn")
        val encoded = GameSessionSnapshotCodec.encode(snapshot)
        val bundle = Bundle().apply { putLongArray(key, encoded) }
        val copy = parcelRoundTrip(bundle, loader).getLongArray(key)
        assertNotSame(encoded, copy)
        assertEquals(encoded.toList(), copy?.toList())
        GameSessionSnapshotCodec.decode(copy)
        assertEquals(encoded.toList(), copy?.toList())
    }

    @Test
    fun wrongTypedOrAbsentBundleValuesDecodeToNull() {
        val validLength = GameSessionSnapshotCodec.encode(validSnapshots().getValue("empty X start")).size
        val wrongTyped = listOf<Pair<String, (Bundle) -> Unit>>(
            "string" to { it.putString(key, "not a snapshot") },
            "int array" to { it.putIntArray(key, IntArray(validLength)) },
            "long" to { it.putLong(key, 1L) },
            "int" to { it.putInt(key, 1) },
            "boolean" to { it.putBoolean(key, true) },
            "string array" to { it.putStringArray(key, Array(validLength) { "0" }) },
            "double array" to { it.putDoubleArray(key, DoubleArray(validLength)) },
            "nested bundle" to { it.putBundle(key, Bundle()) },
            "short long array" to { it.putLongArray(key, LongArray(3)) },
            "zero long array" to { it.putLongArray(key, LongArray(validLength)) },
        )
        for ((name, writer) in wrongTyped) {
            val bundle = Bundle().also(writer)
            assertNull(name, GameSessionSnapshotCodec.decode(bundle.readAny(key)))
            assertNull("$name after parcel", GameSessionSnapshotCodec.decode(parcelRoundTrip(bundle, loader).readAny(key)))
        }
        assertNull("absent key", GameSessionSnapshotCodec.decode(Bundle().readAny(key)))
        assertNull("absent key after parcel", GameSessionSnapshotCodec.decode(parcelRoundTrip(Bundle(), loader).readAny(key)))
        assertNull("other key", GameSessionSnapshotCodec.decode(
            Bundle().apply { putLongArray("other_key", LongArray(validLength)) }.readAny(key),
        ))
    }

    @Suppress("DEPRECATION")
    private fun Bundle.readAny(name: String): Any? = get(name)

    private fun readLongArray(bundle: Bundle): Any? = parcelRoundTrip(bundle, loader).getLongArray(key)

    private fun validSnapshots(): Map<String, GameSessionSnapshot> {
        val e = Mark.Empty
        val x = Mark.X
        val o = Mark.O
        fun snap(
            board: List<Mark>,
            turn: Turn,
            result: GameResult,
            starter: Player,
            sound: Boolean = true,
            pending: Long? = null,
        ) = GameSessionSnapshot(
            board = board,
            turn = turn,
            result = result,
            starter = starter,
            nextStarter = if (starter == Player.X) Player.O else Player.X,
            soundEnabled = sound,
            pendingComputerRemainingNanos = pending,
        )
        return linkedMapOf(
            "empty X start" to snap(List(9) { e }, Turn.Human, GameResult.InProgress, Player.X),
            "O opening pending, intermediate remaining" to
                snap(List(9) { e }, Turn.Computer, GameResult.InProgress, Player.O, pending = 600_000_000L),
            "O opening pending, zero remaining" to
                snap(List(9) { e }, Turn.Computer, GameResult.InProgress, Player.O, pending = 0L),
            "in progress human turn" to
                snap(listOf(x, e, e, e, o, e, e, e, e), Turn.Human, GameResult.InProgress, Player.X),
            "O started, human to move" to
                snap(listOf(e, e, e, e, o, e, e, e, e), Turn.Human, GameResult.InProgress, Player.O),
            "O response pending" to
                snap(listOf(x, e, e, e, e, e, e, e, e), Turn.Computer, GameResult.InProgress, Player.X, pending = 1_000_000_000L),
            "X wins" to
                snap(listOf(x, x, x, o, o, e, e, e, e), Turn.Human, GameResult.Won(Player.X), Player.X),
            "O wins" to
                snap(listOf(x, x, e, o, o, o, x, e, e), Turn.Computer, GameResult.Won(Player.O), Player.X),
            "tie" to
                snap(listOf(x, o, x, x, o, o, o, x, x), Turn.Human, GameResult.Tie, Player.X),
            "sound off, in progress" to
                snap(listOf(x, e, e, e, o, e, e, e, e), Turn.Human, GameResult.InProgress, Player.X, sound = false),
            "sound off, O pending" to
                snap(listOf(x, e, e, e, e, e, e, e, e), Turn.Computer, GameResult.InProgress, Player.X, sound = false, pending = 250_000_000L),
            "sound off, X wins" to
                snap(listOf(x, x, x, o, o, e, e, e, e), Turn.Human, GameResult.Won(Player.X), Player.X, sound = false),
        )
    }
}
