package com.edu.unal.tictactoe.ui.game.audio

import androidx.annotation.MainThread
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.Player

/** Host-owned, best-effort playback. All entry points are called on the main thread. */
class MoveSoundController(
    private val playerFactory: MoveSoundPlayerFactory,
    private val isOwnerCurrent: () -> Boolean = { true },
    private val soundEnabled: () -> Boolean,
) : MoveSoundSink {
    private data class ResourceState(var sampleId: Int = 0, var ready: Boolean = false)

    private var player: MoveSoundPlayer? = null
    private var foreground = false
    private var released = false
    private var epoch = 0L
    private val resources = mapOf(Player.X to ResourceState(), Player.O to ResourceState())
    private val resourceIds = mapOf(Player.X to R.raw.game_move_x, Player.O to R.raw.game_move_o)
    private val activeStreams = ArrayDeque<Int>()

    /** Create and asynchronously preload a fresh player for a foreground host. */
    @MainThread
    fun prepare() {
        if (released || foreground) return
        stopStreams()
        player?.let { ignoreAudioException { it.release() } }
        val current = try {
            playerFactory.create()
        } catch (_: Exception) {
            return
        }
        player = current
        foreground = true
        val loadEpoch = ++epoch
        resources.values.forEach {
            it.sampleId = 0
            it.ready = false
        }
        resourceIds.forEach { (mark, resourceId) ->
            try {
                var expectedSampleId = 0
                val returnedSampleId = current.load(resourceId) { sampleId, status ->
                    // A completion only records readiness; never starts playback.
                    if (sampleId == expectedSampleId && sampleId != 0 &&
                        player === current && foreground && epoch == loadEpoch && ownerCurrentSafely()
                    ) {
                        resources.getValue(mark).apply {
                            this.sampleId = sampleId
                            ready = status == 0 && sampleId != 0
                        }
                    }
                }
                expectedSampleId = returnedSampleId
                if (returnedSampleId == 0) resources.getValue(mark).ready = false
            } catch (_: Exception) {
                resources.getValue(mark).ready = false
            }
        }
    }

    @MainThread
    override fun play(player: Player) {
        if (released || !foreground || !ownerCurrentSafely() || !soundEnabledSafely()) return
        val backend = this.player ?: return
        val resource = resources.getValue(player)
        if (!resource.ready || resource.sampleId == 0) return
        try {
            val stream = backend.play(resource.sampleId)
            if (stream > 0) {
                if (activeStreams.size == MAX_MOVE_SOUND_STREAMS) activeStreams.removeFirst()
                activeStreams.addLast(stream)
            }
        } catch (_: Exception) {
            // Audio is optional and must never affect accepted gameplay.
        }
    }

    @MainThread
    override fun stop() = stopStreams()

    /** Stop and release synchronously; a later prepare creates a new player and preloads anew. */
    @MainThread
    fun pause() {
        if (!foreground) return
        foreground = false
        epoch++
        val old = player
        player = null
        resources.values.forEach {
            it.sampleId = 0
            it.ready = false
        }
        stopStreams(old)
        if (old != null) ignoreAudioException { old.release() }
    }

    @MainThread
    fun release() {
        if (released) return
        pause()
        released = true
    }

    private fun stopStreams() {
        stopStreams(player)
    }

    private fun stopStreams(backend: MoveSoundPlayer?) {
        while (activeStreams.isNotEmpty()) {
            val stream = activeStreams.removeFirst()
            if (backend != null) ignoreAudioException { backend.stop(stream) }
        }
    }

    private fun soundEnabledSafely(): Boolean = try {
        soundEnabled()
    } catch (_: Exception) {
        false
    }

    private fun ownerCurrentSafely(): Boolean = try {
        isOwnerCurrent()
    } catch (_: Exception) {
        false
    }

    private inline fun ignoreAudioException(action: () -> Unit) {
        try {
            action()
        } catch (_: Exception) {
            // Best-effort audio failures are isolated; fatal JVM errors are not swallowed.
        }
    }
}
