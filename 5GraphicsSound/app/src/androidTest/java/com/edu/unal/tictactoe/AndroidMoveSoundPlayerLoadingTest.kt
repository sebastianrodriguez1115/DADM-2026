package com.edu.unal.tictactoe

import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.tictactoe.ui.game.audio.AndroidMoveSoundPlayerFactory
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundPlayer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMoveSoundPlayerLoadingTest {
    @Test
    fun twoLoadsFromOnePoolCompleteIndependentlyWithoutBlockingMain() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val playerHolder = arrayOfNulls<MoveSoundPlayer>(1)
        val ids = IntArray(2)
        val statuses = IntArray(2) { Int.MIN_VALUE }
        val callbacks = CountDownLatch(2)

        instrumentation.runOnMainSync {
            assertEquals(Looper.getMainLooper(), Looper.myLooper())
            val player = AndroidMoveSoundPlayerFactory(
                instrumentation.targetContext,
            ).create()
            playerHolder[0] = player
            ids[0] = player.load(R.raw.game_move_x) { id, status ->
                ids[0] = id
                statuses[0] = status
                callbacks.countDown()
            }
            ids[1] = player.load(R.raw.game_move_o) { id, status ->
                ids[1] = id
                statuses[1] = status
                callbacks.countDown()
            }
        }

        try {
            assertTrue("both asynchronous loads complete", callbacks.await(10, TimeUnit.SECONDS))
            assertNotEquals("SoundPool sample IDs are distinct", ids[0], ids[1])
            assertEquals(0, statuses[0])
            assertEquals(0, statuses[1])
        } finally {
            instrumentation.runOnMainSync { playerHolder[0]?.release() }
        }
    }
}
