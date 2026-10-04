package com.edu.unal.tictactoe

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.preferences.DifficultyCode
import com.edu.unal.tictactoe.data.preferences.GamePreferenceSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Leaves deliberate residue so the runner can contaminate prefs before each Activity class. */
@RunWith(AndroidJUnit4::class)
class GamePreferencesResidueWriterTest {
    @Test
    fun writeContaminatedGamePreferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val preferences = context.getSharedPreferences(GamePreferenceSchema.FILE_NAME, Context.MODE_PRIVATE)
        assertTrue(preferences.edit()
            .putInt(GamePreferenceSchema.SCORE_X_WINS, 71)
            .putInt(GamePreferenceSchema.SCORE_O_WINS, 73)
            .putInt(GamePreferenceSchema.SCORE_TIES, 79)
            .putInt(GamePreferenceSchema.DIFFICULTY_CODE, DifficultyCode.IMPOSSIBLE)
            .commit())
        assertEquals(71, preferences.getInt(GamePreferenceSchema.SCORE_X_WINS, -1))
        assertEquals(73, preferences.getInt(GamePreferenceSchema.SCORE_O_WINS, -1))
        assertEquals(79, preferences.getInt(GamePreferenceSchema.SCORE_TIES, -1))
        assertEquals(DifficultyCode.IMPOSSIBLE, preferences.getInt(GamePreferenceSchema.DIFFICULTY_CODE, -1))
    }
}
