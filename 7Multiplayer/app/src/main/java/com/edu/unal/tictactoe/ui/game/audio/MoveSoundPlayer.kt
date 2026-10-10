package com.edu.unal.tictactoe.ui.game.audio

/** Small, replaceable boundary around SoundPool for deterministic controller tests. */
interface MoveSoundPlayer {
    fun load(resourceId: Int, onLoaded: (sampleId: Int, status: Int) -> Unit): Int
    fun play(sampleId: Int): Int
    fun stop(streamId: Int)
    fun release()
}

fun interface MoveSoundPlayerFactory {
    fun create(): MoveSoundPlayer
}
