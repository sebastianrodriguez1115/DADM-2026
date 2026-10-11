package com.edu.unal.tictactoe

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"
private const val CELL_TAG_PREFIX = "tic_tac_toe_cell_"
private const val WAIT_MILLIS = 10_000L

/**
 * The current screen survives rotation (a real configuration change) and, through a real saved
 * state registry and Parcel, a new Activity after process death. `recreate()` keeps the
 * ViewModelStore, so it is never used as a stand-in for process death.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityNavigationStateTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val fixture = GamePreferencesTestFixture()
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originalOrientation: Int? = null
    private val hosts = mutableListOf<RestoredNavigationHost>()
    private val sessionHosts = mutableListOf<RestoredSessionHost>()

    @Before
    fun launch() {
        fixture.prepare(context)
        seedStoredNickname(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario!!.onActivity { originalOrientation = it.requestedOrientation }
        composeTestRule.waitForIdle()
    }

    @After
    fun cleanup() {
        try {
            try {
                instrumentation.runOnMainSync { hosts.forEach(RestoredNavigationHost::dispose) }
                instrumentation.runOnMainSync { sessionHosts.forEach(RestoredSessionHost::dispose) }
                hosts.clear()
                sessionHosts.clear()
            } finally {
                scenario?.let { current ->
                    if (current.state != Lifecycle.State.DESTROYED) {
                        originalOrientation?.let { value ->
                            current.onActivity { it.requestedOrientation = value }
                        }
                    }
                    current.close()
                }
            }
        } finally {
            try {
                clearStoredNickname(context)
            } finally {
                fixture.cleanup()
            }
        }
    }

    @Test
    fun freshLaunchWithoutSavedStateShowsStart() {
        assertStartShown()
        assertEquals(AppScreen.Start, navigation().currentScreen.value)
        assertLocalNotShown()
        assertLobbyNotShown()
    }

    @Test
    fun rotationOnStartKeepsStart() {
        rotateToLandscapeAndBack { assertStartShown() }
        assertStartShown()
        assertLocalNotShown()
    }

    @Test
    fun rotationOnLocalKeepsTheScreenAndTheGameState() {
        composeTestRule.startLocalGame()
        clickCell(0)
        val before = game().uiState.value
        val gameViewModel = game()

        rotateToLandscapeAndBack {
            assertLocalShown()
            assertSameGame(before, game().uiState.value)
        }

        assertLocalShown()
        assertSame(gameViewModel, game())
        assertSameGame(before, game().uiState.value)
    }

    @Test
    fun rotationOnLobbyKeepsTheLobby() {
        goToLobby()
        rotateToLandscapeAndBack { assertLobbyShown() }
        assertLobbyShown()
        assertLocalNotShown()
    }

    @Test
    fun processDeathOnLocalRestoresLocalAndTheSavedGame() {
        composeTestRule.startLocalGame()
        clickCell(0)
        val before = game().uiState.value
        val saved = captureSavedStateAndCloseScenario()

        assertEquals(AppScreen.Local, restoreNavigation(saved).currentScreen.value)
        // The same saved Bundle also feeds the game's ViewModel: the game is restored with it.
        assertSameGame(before, restoreGame(saved).uiState.value)
    }

    @Test
    fun processDeathOnLobbyRestoresLobby() {
        goToLobby()
        val saved = captureSavedStateAndCloseScenario()

        assertEquals(AppScreen.Lobby, restoreNavigation(saved).currentScreen.value)
    }

    @Test
    fun processDeathOnStartRestoresStart() {
        val saved = captureSavedStateAndCloseScenario()

        assertEquals(AppScreen.Start, restoreNavigation(saved).currentScreen.value)
    }

    @Test
    fun savedMatchIsRestoredAsLobby() {
        var saved: Bundle? = null
        instrumentation.runOnMainSync {
            val host = RestoredNavigationHost(null).also(hosts::add)
            host.createViewModel().navigateTo(AppScreen.Match)
            saved = host.saveState()
        }

        assertEquals(AppScreen.Lobby, restoreNavigation(checkNotNull(saved)).currentScreen.value)
    }

    @Test
    fun absentSavedStateCreatesStart() {
        assertEquals(AppScreen.Start, restoreNavigation(null).currentScreen.value)
    }

    // --- Lobby marker: the only place that knows how the lobby is recognised. With a stored
    // nickname the real lobby shows one of its connection states (see lobbyMarkerCount). ---

    private fun lobbyMarkerCount(): Int = composeTestRule.lobbyMarkerCount()

    private fun assertLobbyShown() {
        composeTestRule.waitUntil(WAIT_MILLIS) { lobbyMarkerCount() > 0 }
    }

    private fun assertLobbyNotShown() {
        assertEquals(0, lobbyMarkerCount())
    }

    // ------------------------------------------------------------------------------------------

    private fun startEntryCount() = composeTestRule
        .onAllNodesWithText(context.getString(R.string.start_play_local)).fetchSemanticsNodes().size

    private fun assertStartShown() {
        composeTestRule.waitUntil(WAIT_MILLIS) { startEntryCount() > 0 }
        composeTestRule.onNodeWithText(context.getString(R.string.start_play_local)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.start_play_online)).assertIsDisplayed()
    }

    private fun boardCount() = composeTestRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size

    private fun assertLocalShown() {
        composeTestRule.waitUntil(WAIT_MILLIS) { boardCount() > 0 }
    }

    private fun assertLocalNotShown() {
        assertEquals(0, boardCount())
    }

    private fun goToLobby() {
        composeTestRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        assertLobbyShown()
    }

    private fun clickCell(location: Int) {
        composeTestRule.onAllNodesWithTag(CELL_TAG_PREFIX + location)[0].performClick()
        composeTestRule.waitUntil(WAIT_MILLIS) {
            val state = game().uiState.value
            state.turn == Turn.Human || state.result != GameResult.InProgress
        }
        composeTestRule.waitForIdle()
    }

    private fun navigation(): NavigationViewModel {
        var viewModel: NavigationViewModel? = null
        scenario!!.onActivity { viewModel = ViewModelProvider(it)[NavigationViewModel::class.java] }
        return checkNotNull(viewModel)
    }

    private fun game(): GameViewModel {
        var viewModel: GameViewModel? = null
        scenario!!.onActivity { viewModel = ViewModelProvider(it)[GameViewModel::class.java] }
        return checkNotNull(viewModel)
    }

    /**
     * Forces a real configuration change twice (landscape, then portrait) without touching the
     * device's system settings; [whileLandscape] runs once the landscape Activity is resumed.
     */
    private fun rotateToLandscapeAndBack(whileLandscape: () -> Unit) {
        val current = checkNotNull(scenario)
        forceOrientation(current, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE)
        whileLandscape()
        forceOrientation(current, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT)
    }

    private fun forceOrientation(current: ActivityScenario<MainActivity>, requested: Int, expected: Int) {
        current.onActivity { it.requestedOrientation = requested }
        composeTestRule.waitUntil(WAIT_MILLIS) {
            var orientation = Configuration.ORIENTATION_UNDEFINED
            current.onActivity { orientation = it.resources.configuration.orientation }
            orientation == expected && current.state == Lifecycle.State.RESUMED
        }
        composeTestRule.waitForIdle()
    }

    /** The real Activity writes its real registry, which goes through a real Parcel. */
    private fun captureSavedStateAndCloseScenario(): Bundle {
        val current = checkNotNull(scenario)
        current.moveToState(Lifecycle.State.CREATED)
        var parceled: Bundle? = null
        current.onActivity { activity ->
            val out = Bundle()
            instrumentation.callActivityOnSaveInstanceState(activity, out)
            parceled = parcelRoundTrip(out, MainActivity::class.java.classLoader)
        }
        current.close()
        scenario = null
        return checkNotNull(parceled)
    }

    private fun restoreNavigation(bundle: Bundle?): NavigationViewModel {
        var created: NavigationViewModel? = null
        instrumentation.runOnMainSync {
            created = RestoredNavigationHost(bundle?.let(::copyOf)).also(hosts::add).createViewModel()
        }
        return checkNotNull(created)
    }

    /** Each host consumes entries from the Bundle it restores from, so each gets its own copy. */
    private fun copyOf(bundle: Bundle): Bundle = parcelRoundTrip(bundle, MainActivity::class.java.classLoader)

    private fun restoreGame(bundle: Bundle?): GameViewModel {
        var created: GameViewModel? = null
        instrumentation.runOnMainSync {
            created = RestoredSessionHost(bundle?.let(::copyOf)).also(sessionHosts::add).createViewModel()
        }
        return checkNotNull(created)
    }

    private fun assertSameGame(
        expected: com.edu.unal.tictactoe.ui.game.models.GameUiState,
        actual: com.edu.unal.tictactoe.ui.game.models.GameUiState,
    ) {
        assertEquals(expected.board, actual.board)
        assertEquals(expected.turn, actual.turn)
        assertEquals(expected.result, actual.result)
        assertEquals(expected.starter, actual.starter)
        assertEquals(expected.nextStarter, actual.nextStarter)
        assertEquals(expected.score, actual.score)
        assertEquals(expected.difficulty, actual.difficulty)
    }
}
