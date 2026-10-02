package com.edu.unal.tictactoe.game

import kotlin.random.Random

fun interface MoveRandomizer {
    fun nextInt(bound: Int): Int

    companion object {
        val Default: MoveRandomizer = MoveRandomizer { bound ->
            Random.Default.nextInt(bound)
        }
    }
}
