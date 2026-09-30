package com.edu.unal.tictactoe.ui.game

import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class DifficultyLabelsTest {
    @Test
    fun mapsEveryDifficultyToItsSpanishLabel() {
        assertEquals(R.string.difficulty_easy, DifficultyLabels.labelRes(DifficultyLevel.Easy))
        assertEquals(R.string.difficulty_hard, DifficultyLabels.labelRes(DifficultyLevel.Hard))
        assertEquals(R.string.difficulty_expert, DifficultyLabels.labelRes(DifficultyLevel.Expert))
        assertEquals(R.string.difficulty_impossible, DifficultyLabels.labelRes(DifficultyLevel.Impossible))
    }

    @Test
    fun keepsTheLabelsInTheDifficultyOrder() {
        val expected = listOf(
            R.string.difficulty_easy,
            R.string.difficulty_hard,
            R.string.difficulty_expert,
            R.string.difficulty_impossible,
        )

        val actual = DifficultyLevel.entries.map { DifficultyLabels.labelRes(it) }

        assertEquals(expected, actual)
    }

    @Test
    fun usesADistinctLabelResourcePerDifficulty() {
        val resources = DifficultyLevel.entries.map { DifficultyLabels.labelRes(it) }

        assertEquals(resources.size, resources.toSet().size)
    }
}
