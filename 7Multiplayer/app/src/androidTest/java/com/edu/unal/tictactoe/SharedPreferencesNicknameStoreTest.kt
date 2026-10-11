package com.edu.unal.tictactoe

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.online.SharedPreferencesNicknameStore
import com.edu.unal.tictactoe.data.preferences.GamePreferenceSchema
import com.edu.unal.tictactoe.data.preferences.SharedPreferencesGamePreferences
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.Score
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesNicknameStoreTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val gameFixture = GamePreferencesTestFixture()

    @Before
    fun clearPreferences() {
        clearNicknameFile()
        gameFixture.prepare(context)
    }

    @After
    fun cleanupPreferences() {
        clearNicknameFile()
        gameFixture.cleanup()
    }

    @Test
    fun initialReadIsNull() {
        assertNull(SharedPreferencesNicknameStore(context).readNickname())
    }

    @Test
    fun writtenNicknameIsReadByFreshInstance() {
        SharedPreferencesNicknameStore(context).writeNickname("Ana")

        assertEquals("Ana", SharedPreferencesNicknameStore(context).readNickname())
    }

    @Test
    fun writingAgainOverwritesThePreviousNickname() {
        val store = SharedPreferencesNicknameStore(context)
        store.writeNickname("Ana")
        store.writeNickname("Luis")

        assertEquals("Luis", store.readNickname())
        assertEquals("Luis", SharedPreferencesNicknameStore(context).readNickname())
    }

    @Test
    fun blankStoredValueReadsAsNull() {
        SharedPreferencesNicknameStore(context).writeNickname("   ")

        assertNull(SharedPreferencesNicknameStore(context).readNickname())
    }

    @Test
    fun nicknameFileIsDistinctFromGamePreferencesFile() {
        assertNotEquals(
            GamePreferenceSchema.FILE_NAME,
            SharedPreferencesNicknameStore.FILE_NAME,
        )
    }

    @Test
    fun writingNicknameDoesNotAlterScoresOrDifficulty() {
        val game = SharedPreferencesGamePreferences(context)
        game.writeScore(Score(3, 4, 5))
        game.writeDifficulty(DifficultyLevel.Hard)

        SharedPreferencesNicknameStore(context).writeNickname("Ana")

        val reloaded = SharedPreferencesGamePreferences(context)
        assertEquals(Score(3, 4, 5), reloaded.readScore())
        assertEquals(DifficultyLevel.Hard, reloaded.readDifficulty())
    }

    @Test
    fun writingScoresAndDifficultyDoesNotAlterNickname() {
        val nicknames = SharedPreferencesNicknameStore(context)
        nicknames.writeNickname("Ana")

        val game = SharedPreferencesGamePreferences(context)
        game.writeScore(Score(7, 8, 9))
        game.writeDifficulty(DifficultyLevel.Impossible)

        assertEquals("Ana", SharedPreferencesNicknameStore(context).readNickname())
    }

    private fun clearNicknameFile() {
        val cleared = context.getSharedPreferences(
            SharedPreferencesNicknameStore.FILE_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        check(cleared) { "Could not clear nickname SharedPreferences" }
    }
}
