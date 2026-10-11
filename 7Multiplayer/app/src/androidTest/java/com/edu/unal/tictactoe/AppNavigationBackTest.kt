package com.edu.unal.tictactoe

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.game.GameMenuTags
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"

/** Mounts [AppNavigation] with fake-backed ViewModels; no real MainActivity is involved. */
@RunWith(AndroidJUnit4::class)
class AppNavigationBackTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var navigation: NavigationViewModel
    private var backReachedSystem = 0

    @Before
    fun setUp() {
        navigation = NavigationViewModel(SavedStateHandle())
        val game = GameViewModel(InMemoryGamePreferences(), SavedStateHandle())
        val lobby = OnlineLobbyViewModel(FakeOnlineBackend().serviceFor("me"), FakeNicknameStore("Ana"))
        // Registered before the content, so any handler AppNavigation declares takes priority over
        // it. It only counts the Back presses the app did not intercept, and consumes them so the
        // test Activity is not finished.
        val match = OnlineMatchViewModel(FakeOnlineBackend().serviceFor("me"))
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.addCallback(
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        backReachedSystem++
                    }
                },
            )
        }
        composeRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                AppNavigation(navigationViewModel = navigation, gameViewModel = game, lobbyViewModel = lobby, matchViewModel = match)
            }
        }
    }

    @Test
    fun backFromLocalShowsStart() {
        goTo(AppScreen.Local)
        composeRule.onNodeWithTag(BOARD_TAG).assertIsDisplayed()

        pressBackAsUser()
        awaitStartShown()

        assertStartShown()
        assertEquals(AppScreen.Start, navigation.currentScreen.value)
        assertEquals(0, backReachedSystem)
    }

    @Test
    fun backFromLobbyShowsStart() {
        goTo(AppScreen.Lobby)
        awaitLobbyShown()
        assertEquals(1, composeRule.onAllNodesWithText(lobbyMarker).fetchSemanticsNodes().size)

        pressBackAsUser()
        awaitStartShown()

        assertStartShown()
        assertEquals(AppScreen.Start, navigation.currentScreen.value)
        assertEquals(0, backReachedSystem)
    }

    @Test
    fun backOnStartIsNotIntercepted() {
        assertStartShown()

        pressBackAsUser()
        composeRule.waitUntil(timeoutMillis = 5_000) { backReachedSystem > 0 }
        composeRule.waitForIdle()

        assertEquals("Back must reach the system on Start", 1, backReachedSystem)
        assertStartShown()
        assertEquals(AppScreen.Start, navigation.currentScreen.value)
        assertEquals(0, composeRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun backClosesOpenMenuFirstAndLocalStaysVisible() {
        goTo(AppScreen.Local)
        composeRule.onNodeWithContentDescription(context.getString(R.string.menu_open)).performClick()
        composeRule.onNodeWithTag(GameMenuTags.MENU).assertIsDisplayed()

        pressBackAsUser()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(GameMenuTags.MENU).fetchSemanticsNodes().isEmpty()
        }

        assertEquals(AppScreen.Local, navigation.currentScreen.value)
        composeRule.onNodeWithTag(BOARD_TAG).assertIsDisplayed()
        assertEquals(0, backReachedSystem)
    }

    // The lobby, with a stored nickname and a connected fake service, shows its games header.
    private val lobbyMarker get() = context.getString(R.string.lobby_games_header)

    private fun awaitLobbyShown() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(lobbyMarker).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun goTo(screen: AppScreen) {
        composeRule.runOnUiThread { navigation.navigateTo(screen) }
        composeRule.waitForIdle()
    }

    private fun awaitStartShown() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(context.getString(R.string.start_play_local))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun assertStartShown() {
        composeRule.onNodeWithText(context.getString(R.string.start_play_local)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).assertIsDisplayed()
    }
}
