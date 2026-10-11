package com.edu.unal.tictactoe

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.navigation.AppNavigation
import com.edu.unal.tictactoe.ui.navigation.AppScreen
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyViewModel
import com.edu.unal.tictactoe.ui.online.match.LeaveMatchDialogTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchViewModel
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BOARD_TAG = "tic_tac_toe_board"

/**
 * The whole online match through [AppNavigation] over the shared fake backend. The person on
 * screen is "Ana" (uid "me"); the rival, "Luis" (uid "rival"), is driven from the test through the
 * same backend. Whoever creates the game is X and moves first.
 */
@RunWith(AndroidJUnit4::class)
class OnlineMatchFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val backend = FakeOnlineBackend()
    private val me = backend.serviceFor("me")
    private val rival = backend.serviceFor("rival")
    private lateinit var navigation: NavigationViewModel

    @Before
    fun mount() {
        navigation = NavigationViewModel(SavedStateHandle())
        val lobby = OnlineLobbyViewModel(me, FakeNicknameStore("Ana"))
        val match = OnlineMatchViewModel(me)
        val game = GameViewModel(InMemoryGamePreferences(), SavedStateHandle())
        composeRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                AppNavigation(
                    navigationViewModel = navigation,
                    gameViewModel = game,
                    lobbyViewModel = lobby,
                    matchViewModel = match,
                    onlineMoveSound = NoMoveSound,
                )
            }
        }
    }

    @Test
    fun creatingAGameAndARivalJoiningShowsTheMatchAsX() {
        val gameId = startAsHost()

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertShown(context.getString(R.string.match_mark_x))
        assertShown(context.getString(R.string.match_opponent, "Luis"))
        assertShown(context.getString(R.string.turn_human))
        assertEquals(1, composeRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
        assertEquals(9, countCells(Mark.Empty))
        assertEquals("Ana", backend.gameById(gameId)?.host?.name)
        assertEquals("Luis", backend.gameById(gameId)?.guest?.name)
    }

    @Test
    fun joiningTheRivalsGameShowsTheMatchAsO() {
        val gameId = startAsGuest()

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertShown(context.getString(R.string.match_mark_o))
        assertShown(context.getString(R.string.match_opponent, "Luis"))
        assertShown(context.getString(R.string.match_turn_opponent, "Luis"))
        assertEquals(9, countCells(Mark.Empty))
        assertEquals("me", backend.gameById(gameId)?.guest?.uid)
    }

    @Test
    fun ownMoveAndRivalMoveAppearOnTheBoardAndPassTheTurn() {
        val gameId = startAsHost()

        composeRule.playMoves(context, listOf(4), rival, gameId)
        assertShown(context.getString(R.string.match_turn_opponent, "Luis"))

        composeRule.playMoves(context, listOf(4, 0), rival, gameId)
        assertShown(context.getString(R.string.turn_human))
        assertEquals(1, countCells(Mark.X))
        assertEquals(1, countCells(Mark.O))
    }

    @Test
    fun winningShowsGanasteAndLocksTheBoard() {
        val gameId = startAsHost()

        composeRule.playMoves(context, HOST_WINS_MOVES, rival, gameId)

        awaitShown(context.getString(R.string.match_result_win))
        assertShown(context.getString(R.string.match_back_to_lobby))
        assertEquals(0, composeRule.onAllNodesWithTag(OnlineMatchTags.LEAVE).fetchSemanticsNodes().size)
    }

    @Test
    fun losingShowsTheRivalsNickname() {
        val gameId = startAsHost()

        composeRule.playMoves(context, RIVAL_WINS_MOVES, rival, gameId)

        awaitShown(context.getString(R.string.match_result_opponent_wins, "Luis"))
        assertShown(context.getString(R.string.match_back_to_lobby))
    }

    @Test
    fun aFullBoardWithoutWinnerShowsTie() {
        val gameId = startAsHost()

        composeRule.playMoves(context, TIE_MOVES, rival, gameId)

        awaitShown(context.getString(R.string.result_tie))
        assertShown(context.getString(R.string.match_back_to_lobby))
    }

    @Test
    fun theRivalLeavingShowsTheNoticeAndBackToLobby() {
        val gameId = startAsHost()
        composeRule.playMoves(context, listOf(0, 4), rival, gameId)

        runBlocking { rival.leaveGame(gameId) }

        awaitShown(context.getString(R.string.match_opponent_left))
        assertShown(context.getString(R.string.match_back_to_lobby))
        assertEquals(AppScreen.Match, navigation.currentScreen.value)
    }

    @Test
    fun losingTheConnectionShowsItsNoticeAndBackToLobbyLeadsToTheLobby() {
        val gameId = startAsHost()
        composeRule.playMoves(context, listOf(0, 4), rival, gameId)

        dropConnection(gameId)

        awaitShown(context.getString(R.string.match_connection_lost))
        assertShown(context.getString(R.string.match_back_to_lobby))
        assertNever(context.getString(R.string.match_opponent_left))
        assertNever(context.getString(R.string.lobby_offline))
        assertEquals(0, composeRule.onAllNodesWithTag(OnlineMatchTags.OFFLINE).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag(OnlineMatchTags.LEAVE).fetchSemanticsNodes().size)
        assertEquals(AppScreen.Match, navigation.currentScreen.value)

        backend.reconnect("me")
        composeRule.waitForIdle()
        assertShown(context.getString(R.string.match_connection_lost))

        composeRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        awaitShown(context.getString(R.string.lobby_games_header))
        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
    }

    @Test
    fun backAfterLosingTheConnectionReturnsToTheLobbyWithoutADialog() {
        val gameId = startAsHost()
        dropConnection(gameId)
        awaitShown(context.getString(R.string.match_connection_lost))

        pressBackAsUser()
        awaitShown(context.getString(R.string.lobby_games_header))

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals(0, composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().size)
    }

    @Test
    fun theLeaveDialogDisappearsWhenTheConnectionIsLost() {
        val gameId = startAsHost()
        composeRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()
        composeRule.onNodeWithTag(LeaveMatchDialogTags.DIALOG).assertIsDisplayed()

        dropConnection(gameId)

        awaitShown(context.getString(R.string.match_connection_lost))
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().isEmpty()
        }
        assertEquals(AppScreen.Match, navigation.currentScreen.value)
    }

    @Test
    fun leavingWithConfirmationReturnsToTheLobbyAndDeletesTheGame() {
        val gameId = startAsHost()

        composeRule.onNodeWithTag(OnlineMatchTags.LEAVE).performClick()
        composeRule.onNodeWithTag(LeaveMatchDialogTags.DIALOG).assertIsDisplayed()
        assertShown(context.getString(R.string.leave_match_title))
        composeRule.onNodeWithTag(LeaveMatchDialogTags.CONFIRM).performClick()
        awaitShown(context.getString(R.string.lobby_games_header))

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertNull("the rival sees the game vanish", backend.gameById(gameId))
        assertEquals(0, composeRule.onAllNodesWithTag(BOARD_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun backWhilePlayingAsksAndCancelContinuesTheMatch() {
        val gameId = startAsHost()
        composeRule.playMoves(context, listOf(0), rival, gameId)

        pressBackAsUser()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        composeRule.onNodeWithTag(LeaveMatchDialogTags.CANCEL).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().isEmpty()
        }

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertNotNull(backend.gameById(gameId))
        assertEquals(1, countCells(Mark.X))
        composeRule.playMoves(context, listOf(0, 4), rival, gameId)
        assertEquals(1, countCells(Mark.O))
    }

    @Test
    fun backOnAFinishedMatchReturnsToTheLobbyWithoutADialog() {
        val gameId = startAsHost()
        composeRule.playMoves(context, HOST_WINS_MOVES, rival, gameId)
        awaitShown(context.getString(R.string.match_result_win))

        pressBackAsUser()
        awaitShown(context.getString(R.string.lobby_games_header))

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals(0, composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().size)
        assertNull(backend.gameById(gameId))
    }

    @Test
    fun backOnAnAbandonedMatchReturnsToTheLobbyWithoutADialog() {
        val gameId = startAsHost()
        runBlocking { rival.leaveGame(gameId) }
        awaitShown(context.getString(R.string.match_opponent_left))

        pressBackAsUser()
        awaitShown(context.getString(R.string.lobby_games_header))

        assertEquals(AppScreen.Lobby, navigation.currentScreen.value)
        assertEquals(0, composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().size)
    }

    @Test
    fun aFinishedMatchOffersNoRematch() {
        val gameId = startAsHost()
        composeRule.playMoves(context, HOST_WINS_MOVES, rival, gameId)
        awaitShown(context.getString(R.string.match_result_win))

        assertEquals(0, composeRule.onAllNodesWithText(context.getString(R.string.new_game)).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag(OnlineMatchTags.LEAVE).fetchSemanticsNodes().size)
        assertEquals(1, composeRule.onAllNodesWithTag(OnlineMatchTags.BACK_TO_LOBBY).fetchSemanticsNodes().size)
        // The board is locked: a tap on a free cell changes nothing.
        composeRule.onNodeWithTag("tic_tac_toe_cell_8").performClick()
        composeRule.waitForIdle()
        assertEquals(Mark.Empty, backend.gameById(gameId)?.board?.get(8))
    }

    @Test
    fun afterReturningToTheLobbyANewCreatedGameStartsClean() {
        val firstId = startAsHost()
        composeRule.playMoves(context, HOST_WINS_MOVES, rival, firstId)
        awaitShown(context.getString(R.string.match_result_win))
        composeRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        awaitShown(context.getString(R.string.lobby_games_header))

        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitShown(context.getString(R.string.lobby_waiting_for_opponent))
        val secondId = singleAvailableGameId(rival)
        runBlocking { rival.joinGame(secondId, "Luis") }
        awaitShown(context.getString(R.string.match_mark_x))

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertShown(context.getString(R.string.turn_human))
        assertEquals(9, countCells(Mark.Empty))
        assertEquals(0, composeRule.onAllNodesWithText(context.getString(R.string.match_result_win)).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag(LeaveMatchDialogTags.DIALOG).fetchSemanticsNodes().size)
        composeRule.playMoves(context, listOf(4), rival, secondId)
    }

    @Test
    fun afterReturningToTheLobbyJoiningAnotherGameStartsClean() {
        val firstId = startAsHost()
        composeRule.playMoves(context, RIVAL_WINS_MOVES, rival, firstId)
        awaitShown(context.getString(R.string.match_result_opponent_wins, "Luis"))
        composeRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        awaitShown(context.getString(R.string.lobby_games_header))

        val secondId = runBlocking { rival.createGame("Luis") }
        composeRule.onNodeWithText(context.getString(R.string.lobby_game_item, "Luis")).performClick()
        awaitShown(context.getString(R.string.match_mark_o))

        assertEquals(AppScreen.Match, navigation.currentScreen.value)
        assertShown(context.getString(R.string.match_turn_opponent, "Luis"))
        assertEquals(9, countCells(Mark.Empty))
        assertEquals(0, composeRule.onAllNodesWithText(context.getString(R.string.match_result_opponent_wins, "Luis")).fetchSemanticsNodes().size)
        assertEquals("me", backend.gameById(secondId)?.guest?.uid)
    }

    /** Ana creates a game and Luis joins it; returns once the match is on screen. */
    private fun startAsHost(): String {
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        awaitShown(context.getString(R.string.lobby_games_header))
        composeRule.onNodeWithText(context.getString(R.string.lobby_create_game)).performClick()
        awaitShown(context.getString(R.string.lobby_waiting_for_opponent))
        val gameId = singleAvailableGameId(rival)
        runBlocking { rival.joinGame(gameId, "Luis") }
        awaitShown(context.getString(R.string.match_mark_x))
        return gameId
    }

    /** Luis creates a game and Ana joins it; returns once the match is on screen. */
    private fun startAsGuest(): String {
        val gameId = runBlocking { rival.createGame("Luis") }
        composeRule.onNodeWithText(context.getString(R.string.start_play_online)).performClick()
        awaitShown(context.getString(R.string.lobby_games_header))
        composeRule.onNodeWithText(context.getString(R.string.lobby_game_item, "Luis")).performClick()
        awaitShown(context.getString(R.string.match_mark_o))
        return gameId
    }

    /** The device notices the lost connection first, and then its local copy of the game vanishes. */
    private fun dropConnection(gameId: String) {
        backend.disconnect("me")
        backend.deleteGame(gameId)
        composeRule.waitForIdle()
    }

    private fun assertNever(text: String) =
        assertEquals(0, composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size)

    private fun awaitShown(text: String) = composeRule.awaitTextShown(text)

    private fun assertShown(text: String) = composeRule.onNodeWithText(text).assertIsDisplayed()

    private fun countCells(mark: Mark): Int = (0 until 9).count { location ->
        composeRule.onAllNodesWithContentDescription(cellDescription(context, location, mark))
            .fetchSemanticsNodes().isNotEmpty()
    }
}
