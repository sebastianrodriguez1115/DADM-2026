package com.edu.unal.tictactoe

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundController
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundPlayer
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundPlayerFactory
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WAIT_MILLIS = 10_000L

/**
 * The online match's sound end to end up to the audio backend: the real [AppNavigation], the real
 * match ViewModel over the shared fake backend, and the real [MoveSoundController] over a fake
 * [MoveSoundPlayer]. The real controller (and not a bare sink) is used so that the session's
 * "Sonido" switch and the foreground state are part of what is under test. The test plays the role
 * of `MainActivity`: it prepares the controller once and keeps it across recreations.
 */
@RunWith(AndroidJUnit4::class)
class OnlineMatchSoundIntegrationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val backend = FakeOnlineBackend()
    private val me = backend.serviceFor("me")
    private val rival = backend.serviceFor("rival")
    private val player = RecordingPlayer()
    private var soundEnabled = true
    private lateinit var controller: MoveSoundController
    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private lateinit var navigation: NavigationViewModel
    private lateinit var lobby: OnlineLobbyViewModel
    private lateinit var match: OnlineMatchViewModel
    private lateinit var game: GameViewModel
    private var originalOrientation: Int? = null

    @Before
    fun launch() {
        navigation = NavigationViewModel(SavedStateHandle())
        lobby = OnlineLobbyViewModel(me, FakeNicknameStore("Ana"))
        match = OnlineMatchViewModel(me)
        game = GameViewModel(InMemoryGamePreferences(), SavedStateHandle())
        controller = MoveSoundController(
            playerFactory = MoveSoundPlayerFactory { player },
            soundEnabled = { soundEnabled },
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            controller.prepare()
            player.completeLoads()
        }
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { originalOrientation = it.requestedOrientation }
        mountContent()
    }

    @After
    fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.release() }
        if (scenario.state != Lifecycle.State.DESTROYED) {
            originalOrientation?.let { value -> scenario.onActivity { it.requestedOrientation = value } }
        }
        scenario.close()
    }

    @Test
    fun anOwnMoveSoundsOnceWithMyMarkAndARivalMoveOnceWithTheirs() {
        val gameId = startMatch()
        assertEquals(emptyList<Player>(), player.playedMarks)

        composeRule.playMoves(context, listOf(0), rival, gameId)
        settle()
        assertEquals(listOf(Player.X), player.playedMarks)

        composeRule.playMoves(context, listOf(0, 4), rival, gameId)
        settle()
        assertEquals(listOf(Player.X, Player.O), player.playedMarks)
    }

    @Test
    fun openingAMatchThatAlreadyHasMarksDoesNotSound() {
        val gameId = runBlocking { rival.createGame("Luis") }
        runBlocking {
            me.joinGame(gameId, "Ana")
            rival.playMove(gameId, 0)
            me.playMove(gameId, 4)
            rival.playMove(gameId, 8)
        }

        // What AppNavigation does when the lobby hands over a ticket.
        composeRule.runOnUiThread {
            match.openMatch(gameId, Player.O)
            navigation.navigateTo(AppScreen.Match)
        }
        composeRule.awaitTextShown(context.getString(R.string.match_mark_o))
        composeRule.awaitCell(context, 8, Mark.X)
        settle()

        assertEquals(emptyList<Player>(), player.playedMarks)
    }

    @Test
    fun rotatingDoesNotSoundOrRepeat() {
        val gameId = startMatch()
        composeRule.playMoves(context, listOf(0, 4), rival, gameId)
        settle()
        assertEquals(listOf(Player.X, Player.O), player.playedMarks)

        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE)
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT)
        settle()

        assertEquals(listOf(Player.X, Player.O), player.playedMarks)
        composeRule.awaitCell(context, 4, Mark.O)
    }

    @Test
    fun aMoveWithTheSessionSoundDisabledDoesNotSound() {
        val gameId = startMatch()
        soundEnabled = false

        composeRule.playMoves(context, listOf(0, 4), rival, gameId)
        settle()

        assertEquals(emptyList<Player>(), player.playedMarks)
    }

    @Test
    fun aRivalMoveInTheBackgroundDoesNotSoundNeitherThenNorOnReturning() {
        val gameId = startMatch()
        composeRule.playMoves(context, listOf(0), rival, gameId)
        settle()
        assertEquals(listOf(Player.X), player.playedMarks)

        scenario.moveToState(Lifecycle.State.CREATED)
        runBlocking { rival.playMove(gameId, 4) }
        settle()
        assertEquals(listOf(Player.X), player.playedMarks)
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.awaitCell(context, 4, Mark.O)
        settle()

        assertEquals(listOf(Player.X), player.playedMarks)
        // Back in the foreground the next move sounds again.
        composeRule.onNodeWithTag("tic_tac_toe_cell_1").performClick()
        composeRule.awaitCell(context, 1, Mark.X)
        settle()
        assertEquals(listOf(Player.X, Player.X), player.playedMarks)
    }

    private fun startMatch(): String {
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_games_header))
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        composeRule.awaitTextShown(context.getString(R.string.lobby_waiting_for_opponent))
        val gameId = singleAvailableGameId(rival)
        runBlocking { rival.joinGame(gameId, "Luis") }
        composeRule.awaitTextShown(context.getString(R.string.match_mark_x))
        return gameId
    }

    /** Lets the main thread finish whatever the last event started, sound included. */
    private fun settle() {
        composeRule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {}
    }

    private fun mountContent() {
        scenario.onActivity { activity ->
            activity.setContent {
                TicTacToeTheme(dynamicColor = false) {
                    AppNavigation(
                        navigationViewModel = navigation,
                        gameViewModel = game,
                        lobbyViewModel = lobby,
                        matchViewModel = match,
                        onlineMoveSound = controller,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun rotate(requested: Int, expected: Int) {
        scenario.onActivity { it.requestedOrientation = requested }
        composeRule.waitUntil(WAIT_MILLIS) {
            var orientation = Configuration.ORIENTATION_UNDEFINED
            scenario.onActivity { orientation = it.resources.configuration.orientation }
            orientation == expected && scenario.state == Lifecycle.State.RESUMED
        }
        mountContent()
    }

    /** Records which mark each started stream belongs to; loads complete when the test says so. */
    private class RecordingPlayer : MoveSoundPlayer {
        private var nextSampleId = 1
        private val callbacks = mutableListOf<Pair<Int, (Int, Int) -> Unit>>()
        private val markOfSample = mutableMapOf<Int, Player>()
        val playedMarks = mutableListOf<Player>()

        override fun load(resourceId: Int, onLoaded: (sampleId: Int, status: Int) -> Unit): Int {
            val sampleId = nextSampleId++
            markOfSample[sampleId] = when (resourceId) {
                R.raw.game_move_x -> Player.X
                R.raw.game_move_o -> Player.O
                else -> error("Unexpected move sound resource $resourceId")
            }
            callbacks += sampleId to onLoaded
            return sampleId
        }

        fun completeLoads() = callbacks.forEach { (sampleId, callback) -> callback(sampleId, 0) }

        override fun play(sampleId: Int): Int {
            playedMarks += checkNotNull(markOfSample[sampleId])
            return sampleId + 100
        }

        override fun stop(streamId: Int) = Unit

        override fun release() = Unit
    }
}
