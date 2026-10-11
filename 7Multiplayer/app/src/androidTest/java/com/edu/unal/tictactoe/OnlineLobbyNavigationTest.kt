package com.edu.unal.tictactoe

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.data.online.OnlineGame
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.NicknameDialogTags
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"
private const val WAIT_MILLIS = 5_000L

/**
 * Mounts [AppNavigation] with ViewModels built on the fakes, without the real `MainActivity`:
 * the lobby, its nickname dialog, the waiting view, the Back handlers and the hand-over to the
 * match. The match itself is not covered here; `Match` still shows the provisional marker.
 */
@RunWith(AndroidJUnit4::class)
class OnlineLobbyNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val backend = FakeOnlineBackend()
    private val service = backend.serviceFor("me")
    private val rival = backend.serviceFor("rival")
    private lateinit var store: FakeNicknameStore
    private lateinit var navigation: NavigationViewModel
    private lateinit var lobby: OnlineLobbyViewModel

    @After
    fun hideKeyboard() {
        // A keyboard left open by a dialog would take the next test's Back.
        composeRule.runOnUiThread {
            androidx.core.view.WindowCompat.getInsetsController(
                composeRule.activity.window,
                composeRule.activity.window.decorView,
            ).hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        composeRule.waitForIdle()
    }

    private fun mount(
        storedNickname: String? = "Ana",
        lobbyService: OnlineGameService = service,
    ) {
        store = FakeNicknameStore(storedNickname)
        navigation = NavigationViewModel(SavedStateHandle())
        lobby = OnlineLobbyViewModel(lobbyService, store)
        val game = GameViewModel(InMemoryGamePreferences(), SavedStateHandle())
        composeRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                AppNavigation(navigationViewModel = navigation, gameViewModel = game, lobbyViewModel = lobby)
            }
        }
    }

    @Test
    fun playOnlineWithoutNicknameShowsOnlyTheDialog() {
        mount(storedNickname = null)

        enterLobby()

        composeRule.onNodeWithTag(NicknameDialogTags.DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nickname_dialog_title)).assertIsDisplayed()
        assertLobbyContentNotShown()
        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
    }

    @Test
    fun playOnlineWithNicknameShowsTheLobbyWithoutTheDialog() {
        mount()

        enterLobby()
        awaitLobbyShown()

        assertDialogNotShown()
        assertTrue(composeRule.onAllNodesWithText(context.getString(R.string.nickname_dialog_title)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun cancelInTheFirstTimeDialogReturnsToStartWithoutSaving() {
        mount(storedNickname = null)
        enterLobby()

        composeRule.onNodeWithTag(NicknameDialogTags.CANCEL).performClick()
        awaitStartShown()

        assertNull(store.readNickname())
        assertEquals(AppScreen.Start, navigation.currentScreen.value)
        assertDialogNotShown()
    }

    @Test
    fun backInTheFirstTimeDialogReturnsToStartWithoutSaving() {
        mount(storedNickname = null)
        enterLobby()
        composeRule.onNodeWithTag(NicknameDialogTags.DIALOG).assertIsDisplayed()

        composeRule.closeSoftKeyboardWithBack(composeRule.activity.window.decorView)
        assertEquals("Back that closes the keyboard must not dismiss the dialog", AppScreen.Lobby, navigation.currentScreen.value)
        pressBackAsUser()
        awaitStartShown()

        assertNull(store.readNickname())
        assertEquals(AppScreen.Start, navigation.currentScreen.value)
        assertDialogNotShown()
    }

    @Test
    fun acceptingAValidNicknameShowsTheLobbyAndSavesItTrimmed() {
        mount(storedNickname = null)
        enterLobby()

        composeRule.onNodeWithTag(NicknameDialogTags.FIELD).performTextInput("  Luis  ")
        composeRule.onNodeWithTag(NicknameDialogTags.ACCEPT).performClick()
        awaitLobbyShown()

        assertEquals("Luis", store.readNickname())
        assertDialogNotShown()
    }

    @Test
    fun changeNicknameDialogConsumesBackAndKeepsTheLobbyAndTheNickname() {
        mount()
        enterLobby()
        awaitLobbyShown()

        composeRule.onNodeWithText(context.getString(R.string.nickname_change)).performClick()
        composeRule.onNodeWithTag(NicknameDialogTags.DIALOG).assertIsDisplayed()
        composeRule.closeSoftKeyboardWithBack(composeRule.activity.window.decorView)
        pressBackAsUser()
        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodesWithTag(NicknameDialogTags.DIALOG).fetchSemanticsNodes().isEmpty()
        }

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals("Ana", store.readNickname())
        composeRule.onNodeWithText(context.getString(R.string.lobby_games_header)).assertIsDisplayed()
    }

    @Test
    fun createShowsWaitingForRivalWithoutABoard() {
        mount()
        enterLobby()
        awaitLobbyShown()

        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitWaitingShown()

        composeRule.onNodeWithText(context.getString(R.string.lobby_cancel)).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
        assertEquals(1, backend.createCount)
        assertEquals(listOf("Ana"), availableHosts())
    }

    @Test
    fun backWhileWaitingCancelsWithoutConfirmationAndStaysInTheLobby() {
        mount()
        enterLobby()
        awaitLobbyShown()
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitWaitingShown()

        pressBackAsUser()
        awaitLobbyShown()

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals(1, backend.cancelCount)
        assertTrue(availableHosts().isEmpty())
        assertWaitingNotShown()
        assertDialogNotShown()
    }

    @Test
    fun cancelButtonWhileWaitingCancelsWithoutConfirmationAndStaysInTheLobby() {
        mount()
        enterLobby()
        awaitLobbyShown()
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitWaitingShown()

        composeRule.onNodeWithText(context.getString(R.string.lobby_cancel)).performClick()
        awaitLobbyShown()

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals(1, backend.cancelCount)
        assertTrue(availableHosts().isEmpty())
        assertWaitingNotShown()
        assertDialogNotShown()
    }

    @Test
    fun aRivalJoiningTakesTheHostToTheMatchOnlyOnce() {
        mount()
        enterLobby()
        awaitLobbyShown()
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitWaitingShown()
        val gameId = availableIds().single()

        runBlocking { rival.joinGame(gameId, "Luis") }
        awaitMatchShown()

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertTrue("the signal must be consumed", lobby.uiState.value.signals.isEmpty())
        // Coming back to the lobby must not replay the hand-over to the match.
        composeRule.runOnUiThread { navigation.navigateTo(AppScreen.Lobby) }
        awaitLobbyShown()
        composeRule.waitForIdle()
        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertTrue(lobby.uiState.value.signals.isEmpty())
        assertEquals(1, backend.createCount)
        assertEquals(0, backend.cancelCount)
    }

    @Test
    fun joiningAnAvailableGameTakesThePersonToTheMatch() {
        val gameId = runBlocking { rival.createGame("Luis") }
        mount()
        enterLobby()
        awaitLobbyShown()

        composeRule.onNodeWithText(context.getString(R.string.lobby_game_item, "Luis")).performClick()
        awaitMatchShown()

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertEquals("me", backend.gameById(gameId)?.guest?.uid)
        assertTrue(lobby.uiState.value.signals.isEmpty())
    }

    @Test
    fun aGameAlreadyTakenShowsTheUnavailableMessageAndStaysInTheLobby() {
        val gameId = runBlocking { rival.createGame("Luis") }
        val staleGame = checkNotNull(backend.gameById(gameId))
        // Another player takes the game, but the list still shows it: the race the spec describes.
        runBlocking { backend.serviceFor("other").joinGame(gameId, "Eva") }
        mount(lobbyService = StaleListService(service, listOf(staleGame)))
        enterLobby()
        awaitLobbyShown()

        composeRule.onNodeWithText(context.getString(R.string.lobby_game_item, "Luis")).performClick()
        awaitMessage(R.string.lobby_game_unavailable)

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals("other", backend.gameById(gameId)?.guest?.uid)
        assertEquals(0, composeRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
        assertTrue(lobby.uiState.value.signals.isEmpty())
    }

    @Test
    fun aFailedCreationShowsTheMessageAndStaysInTheLobby() {
        service.failNextCreate()
        mount()
        enterLobby()
        awaitLobbyShown()

        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitMessage(R.string.lobby_create_failed)

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertWaitingNotShown()
        assertTrue(availableHosts().isEmpty())
        assertEquals(0, backend.createCount)
        assertTrue(lobby.uiState.value.signals.isEmpty())
    }

    @Test
    fun backInTheLobbyReturnsToStart() {
        mount()
        enterLobby()
        awaitLobbyShown()

        pressBackAsUser()
        awaitStartShown()

        assertEquals(AppScreen.Start, navigation.currentScreen.value)
    }

    private fun availableIds(): List<String> =
        runBlocking { rival.observeAvailableGames().first().map { it.id } }

    private fun availableHosts(): List<String> =
        runBlocking { rival.observeAvailableGames().first().map { it.host.name } }

    private fun enterLobby() {
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        composeRule.waitForIdle()
    }

    private fun awaitText(resId: Int) {
        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodesWithText(context.getString(resId)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun awaitLobbyShown() = awaitText(R.string.lobby_games_header)

    private fun awaitWaitingShown() = awaitText(R.string.lobby_waiting_for_opponent)

    private fun awaitStartShown() = awaitText(R.string.start_play_local)

    private fun awaitMessage(resId: Int) {
        awaitText(resId)
        composeRule.onNodeWithText(context.getString(resId)).assertIsDisplayed()
    }

    private fun awaitMatchShown() = awaitText(R.string.online_provisional_placeholder)

    private fun assertLobbyContentNotShown() {
        assertTextCount(R.string.lobby_games_header, 0)
        assertTextCount(R.string.lobby_create_game, 0)
        assertTextCount(R.string.lobby_connecting, 0)
        assertTextCount(R.string.lobby_connection_failed, 0)
    }

    private fun assertWaitingNotShown() = assertTextCount(R.string.lobby_waiting_for_opponent, 0)

    private fun assertDialogNotShown() {
        assertEquals(0, composeRule.onAllNodesWithTag(NicknameDialogTags.DIALOG).fetchSemanticsNodes().size)
    }

    private fun assertTextCount(resId: Int, expected: Int) {
        assertEquals(
            context.getString(resId),
            expected,
            composeRule.onAllNodesWithText(context.getString(resId)).fetchSemanticsNodes().size,
        )
    }
}

/** A service whose list of available games is frozen at [games]; everything else goes to [delegate]. */
private class StaleListService(
    private val delegate: OnlineGameService,
    private val games: List<OnlineGame>,
) : OnlineGameService by delegate {
    override fun observeAvailableGames(): Flow<List<OnlineGame>> = flowOf(games)
}
