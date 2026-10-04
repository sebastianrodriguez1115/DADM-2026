package com.edu.unal.tictactoe.ui.game.audio

import com.edu.unal.tictactoe.game.models.Player

/** Host-owned, best-effort destination for an already accepted move sound. */
interface MoveSoundSink {
    fun play(player: Player)

    fun stop()
}
