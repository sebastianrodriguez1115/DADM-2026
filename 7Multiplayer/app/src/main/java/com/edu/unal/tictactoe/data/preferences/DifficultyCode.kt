package com.edu.unal.tictactoe.data.preferences

import com.edu.unal.tictactoe.game.models.DifficultyLevel

/** Stable integer identifiers for persisted difficulty; enum ordering is deliberately irrelevant. */
object DifficultyCode {
    fun encode(level: DifficultyLevel): Int = when (level) {
        DifficultyLevel.Easy -> EASY
        DifficultyLevel.Hard -> HARD
        DifficultyLevel.Expert -> EXPERT
        DifficultyLevel.Impossible -> IMPOSSIBLE
    }

    /** Returns [DifficultyLevel.Expert] for absent or unrecognized persisted values. */
    fun decode(code: Int?): DifficultyLevel = when (code) {
        EASY -> DifficultyLevel.Easy
        HARD -> DifficultyLevel.Hard
        EXPERT -> DifficultyLevel.Expert
        IMPOSSIBLE -> DifficultyLevel.Impossible
        else -> DifficultyLevel.Expert
    }

    const val EASY = 0
    const val HARD = 1
    const val EXPERT = 2
    const val IMPOSSIBLE = 3
}
