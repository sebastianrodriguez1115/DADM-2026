package com.edu.unal.tictactoe.ui.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.MainThread

/** Creates a SoundPool using only application context; call methods from the main thread. */
class AndroidMoveSoundPlayerFactory(context: Context) : MoveSoundPlayerFactory {
    private val applicationContext = context.applicationContext

    @MainThread
    override fun create(): MoveSoundPlayer {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(MAX_MOVE_SOUND_STREAMS)
            .setAudioAttributes(attributes)
            .build()
        return AndroidMoveSoundPlayer(applicationContext, pool)
    }
}

private class AndroidMoveSoundPlayer(private val context: Context, private val pool: SoundPool) : MoveSoundPlayer {
    private val pendingLoads = mutableMapOf<Int, (Int, Int) -> Unit>()

    init {
        // SoundPool has one listener per pool; install once before any load and route by sample ID.
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            pendingLoads.remove(sampleId)?.invoke(sampleId, status)
        }
    }

    /** Called on Main; completion is delivered asynchronously by SoundPool on its owner looper. */
    @MainThread
    override fun load(resourceId: Int, onLoaded: (Int, Int) -> Unit): Int {
        val sampleId = pool.load(context, resourceId, 1)
        if (sampleId == 0) {
            onLoaded(0, -1)
        } else {
            pendingLoads[sampleId] = onLoaded
        }
        return sampleId
    }

    @MainThread
    override fun play(sampleId: Int): Int = pool.play(sampleId, 1f, 1f, 1, 0, 1f)

    @MainThread
    override fun stop(streamId: Int) = pool.stop(streamId)

    @MainThread
    override fun release() {
        pendingLoads.clear()
        pool.release()
    }
}

internal const val MAX_MOVE_SOUND_STREAMS = 4
