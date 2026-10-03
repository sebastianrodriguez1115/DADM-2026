package com.edu.unal.tictactoe.ui.game.audio

import androidx.annotation.MainThread
import com.edu.unal.tictactoe.ui.game.GameViewModel

/** Coordinates one Activity's audio resources and the retained session's ephemeral sink. */
internal class MoveSoundHost(
    private val viewModel: GameViewModel,
    playerFactory: MoveSoundPlayerFactory,
) {
    private val owner = Any()
    private val controller: MoveSoundController
    private var destroyed = false

    init {
        // Capture only the retained ViewModel and opaque token, never the Activity.
        val session = viewModel
        val token = owner
        controller = MoveSoundController(
            playerFactory = playerFactory,
            isOwnerCurrent = { session.isMoveSoundOwnerCurrent(token) },
            soundEnabled = { session.uiState.value.soundEnabled },
        )
    }

    @MainThread
    fun onResume() {
        if (destroyed) return
        controller.prepare()
        viewModel.registerMoveSoundSink(owner, controller)
        viewModel.onHostResumed()
    }

    @MainThread
    fun onPause() {
        if (destroyed) return
        // A delayed lifecycle callback from a replaced Activity must not pause the
        // retained session or clear the replacement host's sink.
        if (viewModel.isMoveSoundOwnerCurrent(owner)) viewModel.onHostPaused()
        controller.pause()
    }

    @MainThread
    fun onDestroy() {
        if (destroyed) return
        destroyed = true
        viewModel.unregisterMoveSoundSink(owner)
        controller.release()
    }
}
