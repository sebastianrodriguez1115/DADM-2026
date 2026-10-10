package com.edu.unal.tictactoe.ui.game.audio

import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveSoundControllerTest {
    @Test
    fun readySamplesPlayMatchingResourceOnceAndLoadNeverPlays() {
        val backend = FakePlayer()
        val controller = MoveSoundController(MoveSoundPlayerFactory { backend }) { true }
        controller.prepare()
        assertEquals(emptyList<Int>(), backend.played)
        backend.complete(R.raw.game_move_x, status = 0)
        backend.complete(R.raw.game_move_o, status = 0)
        controller.play(Player.X)
        controller.play(Player.O)
        assertEquals(listOf(101, 102), backend.played)
    }

    @Test
    fun noReadyFailureMuteOrForegroundMeansNoPlaybackAndNoRetry() {
        var enabled = true
        val backend = FakePlayer()
        val controller = MoveSoundController(MoveSoundPlayerFactory { backend }) { enabled }
        controller.play(Player.X)
        controller.prepare()
        controller.play(Player.X)
        backend.complete(R.raw.game_move_x, status = -1)
        backend.complete(R.raw.game_move_o, sampleId = 0, status = 0)
        controller.play(Player.X)
        controller.play(Player.O)
        backend.complete(R.raw.game_move_x, status = 0)
        enabled = false
        controller.play(Player.X)
        assertTrue(backend.played.isEmpty())
        enabled = true
        controller.play(Player.X)
        controller.pause()
        controller.play(Player.X)
        assertEquals(listOf(101), backend.played)
    }

    @Test
    fun backendPlayFailureOrZeroIsIsolatedWithoutQueuedRetry() {
        val backend = FakePlayer()
        val controller = MoveSoundController(MoveSoundPlayerFactory { backend }) { true }
        controller.prepare()
        backend.complete(R.raw.game_move_x, status = 0)
        backend.throwOnPlay = true
        controller.play(Player.X)
        backend.throwOnPlay = false
        backend.nextStream = 0
        controller.play(Player.X)
        assertEquals(listOf(101), backend.played)
        backend.nextStream = 9
        controller.play(Player.X)
        assertEquals(listOf(101, 101), backend.played)
    }

    @Test
    fun factoryAndResourceLoadFailuresAreIsolated() {
        val unavailable = MoveSoundController(
            MoveSoundPlayerFactory { throw IllegalStateException("factory failed") },
        ) { true }
        unavailable.prepare()
        unavailable.play(Player.X)

        val backends = mutableListOf<FakePlayer>()
        val controller = MoveSoundController(MoveSoundPlayerFactory {
            FakePlayer().also { backend ->
                if (backends.isEmpty()) {
                    backend.throwOnLoadResource = R.raw.game_move_x
                    backend.zeroLoadResource = R.raw.game_move_o
                }
                backends += backend
            }
        }) { true }
        controller.prepare()
        val firstBackend = backends[0]
        controller.play(Player.X)
        controller.play(Player.O)
        assertTrue(firstBackend.played.isEmpty())
        controller.pause()
        controller.prepare()
        val resumedBackend = backends[1]
        resumedBackend.complete(R.raw.game_move_o, status = 0)
        controller.play(Player.O)
        assertEquals(listOf(102), resumedBackend.played)
    }

    @Test
    fun pauseStopsAndReleasesAndReleaseIsIdempotent() {
        val backends = mutableListOf<FakePlayer>()
        val controller = MoveSoundController(MoveSoundPlayerFactory { FakePlayer().also(backends::add) }) { true }
        controller.prepare()
        val first = backends[0]
        first.complete(R.raw.game_move_x, status = 0)
        controller.play(Player.X)
        controller.pause()
        controller.pause()
        assertEquals(listOf(1), first.stopped)
        assertEquals(1, first.releaseCount)
        controller.prepare()
        assertEquals(2, backends.size)
        assertTrue(backends[1].played.isEmpty())
        backends[1].complete(R.raw.game_move_x, status = 0)
        controller.play(Player.X)
        assertEquals(listOf(101), backends[1].played)
        controller.release()
        controller.release()
        assertEquals(1, backends[1].releaseCount)
        controller.prepare()
        assertEquals(2, backends.size)
    }

    @Test
    fun prepareIsIdempotentAndLateOldPoolCallbackCannotReadySameSampleInNewPool() {
        val backends = mutableListOf<FakePlayer>()
        val controller = MoveSoundController(MoveSoundPlayerFactory { FakePlayer().also(backends::add) }) { true }
        controller.prepare()
        controller.prepare()
        val first = backends.single()
        val oldXCallback = first.callback(R.raw.game_move_x)
        controller.pause()
        controller.prepare()
        val second = backends[1]
        oldXCallback(101, 0)
        controller.play(Player.X)
        assertTrue(second.played.isEmpty())
        second.complete(R.raw.game_move_x, status = 0)
        controller.play(Player.X)
        assertEquals(listOf(101), second.played)
        assertTrue(first.played.isEmpty())
    }

    @Test
    fun staleOwnerCannotPlayOrMarkCurrentPoolReadyButPreloadMayStartBeforeRegistration() {
        var ownerCurrent = false
        val backend = FakePlayer()
        val controller = MoveSoundController(
            MoveSoundPlayerFactory { backend },
            soundEnabled = { true },
            isOwnerCurrent = { ownerCurrent },
        )
        controller.prepare()
        backend.complete(R.raw.game_move_x, status = 0)
        controller.play(Player.X)
        assertTrue(backend.played.isEmpty())
        ownerCurrent = true
        controller.play(Player.X)
        assertTrue("stale-owner load callback must not mark the sample ready", backend.played.isEmpty())
        backend.complete(R.raw.game_move_o, status = 0)
        controller.play(Player.O)
        assertEquals(listOf(102), backend.played)
    }

    @Test
    fun oldOwnerCannotPlayEvenWhenItsPoolAndSampleRemainReady() {
        var ownerCurrent = true
        val backend = FakePlayer()
        val controller = MoveSoundController(
            playerFactory = MoveSoundPlayerFactory { backend },
            isOwnerCurrent = { ownerCurrent },
            soundEnabled = { true },
        )
        controller.prepare()
        backend.complete(R.raw.game_move_o, status = 0)
        controller.play(Player.O)
        assertEquals(listOf(102), backend.played)

        ownerCurrent = false
        controller.play(Player.O)
        assertEquals(listOf(102), backend.played)
    }

    @Test
    fun stopTracksOnlyTheMostRecentMaximumStreams() {
        val backend = FakePlayer()
        val controller = MoveSoundController(MoveSoundPlayerFactory { backend }) { true }
        controller.prepare()
        backend.complete(R.raw.game_move_x, status = 0)
        repeat(MAX_MOVE_SOUND_STREAMS + 1) { controller.play(Player.X) }
        controller.stop()
        assertEquals((2..MAX_MOVE_SOUND_STREAMS + 1).toList(), backend.stopped)
    }

    @Test
    fun muteStopsImmediatelyAndDoesNotAutoplayAfterToggle() {
        var enabled = true
        val backend = FakePlayer()
        val controller = MoveSoundController(MoveSoundPlayerFactory { backend }) { enabled }
        controller.prepare()
        backend.complete(R.raw.game_move_x, status = 0)
        controller.play(Player.X)
        enabled = false
        controller.stop()
        assertEquals(listOf(1), backend.stopped)
        enabled = true
        assertEquals(listOf(101), backend.played)
    }

    @Test
    fun stopAndReleaseExceptionsAreIsolated() {
        val backend = FakePlayer().apply {
            throwOnStop = true
            throwOnRelease = true
        }
        val controller = MoveSoundController(MoveSoundPlayerFactory { backend }) { true }
        controller.prepare()
        backend.complete(R.raw.game_move_x, status = 0)
        controller.play(Player.X)
        controller.pause()
        controller.release()
        controller.play(Player.X)
        assertEquals(1, backend.releaseCount)
    }

    private class FakePlayer : MoveSoundPlayer {
        private val pending = mutableMapOf<Int, (Int, Int) -> Unit>()
        var nextStream = 1
        var throwOnLoadResource: Int? = null
        var zeroLoadResource: Int? = null
        var throwOnPlay = false
        var throwOnStop = false
        var throwOnRelease = false
        var releaseCount = 0
        val played = mutableListOf<Int>()
        val stopped = mutableListOf<Int>()

        override fun load(resourceId: Int, onLoaded: (Int, Int) -> Unit): Int {
            if (resourceId == throwOnLoadResource) error("fake load failure")
            if (resourceId == zeroLoadResource) return 0
            val sample = when (resourceId) {
                R.raw.game_move_x -> 101
                else -> 102
            }
            pending[resourceId] = onLoaded
            return sample
        }

        fun callback(resourceId: Int): (Int, Int) -> Unit = pending.getValue(resourceId)

        fun complete(resourceId: Int, sampleId: Int? = null, status: Int) {
            val sample = sampleId ?: if (resourceId == R.raw.game_move_x) 101 else 102
            pending[resourceId]?.invoke(sample, status)
        }

        override fun play(sampleId: Int): Int {
            if (throwOnPlay) error("fake playback failure")
            played.add(sampleId)
            return nextStream++
        }

        override fun stop(streamId: Int) {
            if (throwOnStop) error("fake stop failure")
            stopped.add(streamId)
        }

        override fun release() {
            releaseCount++
            if (throwOnRelease) error("fake release failure")
        }

    }
}
