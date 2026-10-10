package com.edu.unal.tictactoe.ui.game

/** Supplies monotonic nanoseconds for measuring in-process elapsed time. */
fun interface MonotonicClock {
    fun nowNanos(): Long
}
