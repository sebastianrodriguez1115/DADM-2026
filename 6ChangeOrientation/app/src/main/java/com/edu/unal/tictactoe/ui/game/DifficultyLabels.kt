package com.edu.unal.tictactoe.ui.game

import androidx.annotation.StringRes
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.DifficultyLevel

object DifficultyLabels {
    @StringRes
    fun labelRes(level: DifficultyLevel): Int = when (level) {
        DifficultyLevel.Easy -> R.string.difficulty_easy
        DifficultyLevel.Hard -> R.string.difficulty_hard
        DifficultyLevel.Expert -> R.string.difficulty_expert
        DifficultyLevel.Impossible -> R.string.difficulty_impossible
    }
}
