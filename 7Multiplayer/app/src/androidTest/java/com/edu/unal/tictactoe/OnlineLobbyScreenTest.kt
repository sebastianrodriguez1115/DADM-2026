package com.edu.unal.tictactoe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.online.lobby.LobbyConnection
import com.edu.unal.tictactoe.ui.online.lobby.LobbyGame
import com.edu.unal.tictactoe.ui.online.lobby.LobbyMessage
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyScreen
import com.edu.unal.tictactoe.ui.online.lobby.OnlineLobbyTags
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val LOBBY_BOARD_TAG = "tic_tac_toe_board"

@RunWith(AndroidJUnit4::class)
class OnlineLobbyScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var retries = 0
    private var creations = 0
    private var nicknameChanges = 0
    private val joined = mutableListOf<String>()
    private var shown = 0
    private var currentMessage by mutableStateOf<LobbyMessage?>(null)
    private var currentConnection by mutableStateOf(LobbyConnection.Connected)
    private var currentGames by mutableStateOf<List<LobbyGame>>(emptyList())
    private var hosted = false

    private val twoGames = listOf(LobbyGame("g1", "Ana"), LobbyGame("g2", "Luis"))

    private fun show(
        connection: LobbyConnection = LobbyConnection.Connected,
        games: List<LobbyGame> = emptyList(),
        message: LobbyMessage? = null,
        width: Int = 360,
        height: Int = 640,
        fontScale: Float = 1f,
    ) {
        currentMessage = message
        currentConnection = connection
        currentGames = games
        if (hosted) {
            viewport.show(width, height, fontScale)
            return
        }
        hosted = true
        viewport.setContent(width, height, fontScale) {
            TicTacToeTheme(dynamicColor = false) {
                OnlineLobbyScreen(
                    connection = currentConnection,
                    games = currentGames,
                    message = currentMessage,
                    onMessageShown = { shown++; currentMessage = null },
                    onRetry = { retries++ },
                    onCreateGame = { creations++ },
                    onJoinGame = { joined += it },
                    onChangeNickname = { nicknameChanges++ },
                )
            }
        }
    }

    private fun text(id: Int) = context.getString(id)

    private fun assertNoneOf(vararg tags: String) {
        tags.forEach { composeTestRule.onAllNodesWithTag(it).assertCountEquals(0) }
    }

    @Test
    fun connectingShowsOnlyTheConnectingText() {
        show(connection = LobbyConnection.Connecting, games = twoGames)

        assertEquals("Conectando…", text(R.string.lobby_connecting))
        composeTestRule.onNodeWithText(text(R.string.lobby_connecting)).assertIsDisplayed()
        assertNoneOf(
            OnlineLobbyTags.LIST,
            OnlineLobbyTags.HEADER,
            OnlineLobbyTags.NO_GAMES,
            OnlineLobbyTags.OFFLINE,
            OnlineLobbyTags.CREATE,
            OnlineLobbyTags.CHANGE_NICKNAME,
            OnlineLobbyTags.RETRY,
            OnlineLobbyTags.game("g1"),
        )
        composeTestRule.onAllNodesWithText(text(R.string.lobby_no_games)).assertCountEquals(0)
        composeTestRule.onAllNodesWithText(text(R.string.lobby_games_header)).assertCountEquals(0)
    }

    @Test
    fun identityFailureShowsTheMessageAndRetryThatInvokesItsCallback() {
        show(connection = LobbyConnection.IdentityFailed)

        assertEquals("No se pudo conectar con el servicio", text(R.string.lobby_connection_failed))
        composeTestRule.onNodeWithText(text(R.string.lobby_connection_failed)).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.lobby_retry)).assertIsDisplayed()
        assertNoneOf(OnlineLobbyTags.LIST, OnlineLobbyTags.HEADER, OnlineLobbyTags.CREATE)
        composeTestRule.onAllNodesWithText(text(R.string.lobby_no_games)).assertCountEquals(0)

        composeTestRule.onNodeWithTag(OnlineLobbyTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun connectedWithGamesShowsHeaderAndOneItemPerGame() {
        show(games = twoGames)

        assertEquals("Juegos disponibles", text(R.string.lobby_games_header))
        composeTestRule.onNodeWithText(text(R.string.lobby_games_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Juego de Ana").assertIsDisplayed()
        composeTestRule.onNodeWithText("Juego de Luis").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(text(R.string.lobby_no_games)).assertCountEquals(0)
        composeTestRule.onNodeWithText(text(R.string.lobby_create_game)).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.nickname_change)).assertIsDisplayed()
        assertNoneOf(OnlineLobbyTags.OFFLINE, OnlineLobbyTags.RETRY)
    }

    @Test
    fun connectedWithoutGamesShowsTheEmptyNotice() {
        show()

        assertEquals("No hay juegos disponibles", text(R.string.lobby_no_games))
        composeTestRule.onNodeWithText(text(R.string.lobby_games_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.lobby_no_games)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Juego de", substring = true).assertCountEquals(0)
    }

    @Test
    fun tappingAGameDeliversItsId() {
        show(games = twoGames)

        composeTestRule.onNodeWithTag(OnlineLobbyTags.game("g2")).performClick()

        assertEquals(listOf("g2"), joined)
        composeTestRule.onNodeWithText("Juego de Ana").performClick()
        assertEquals(listOf("g2", "g1"), joined)
    }

    @Test
    fun createAndChangeNicknameInvokeTheirCallbacks() {
        show()

        composeTestRule.onNodeWithTag(OnlineLobbyTags.CREATE).performClick()
        assertEquals(1, creations)
        assertEquals(0, nicknameChanges)

        composeTestRule.onNodeWithTag(OnlineLobbyTags.CHANGE_NICKNAME).performClick()
        assertEquals(1, creations)
        assertEquals(1, nicknameChanges)
    }

    @Test
    fun offlineShowsTheNoticeAndDisablesCreateAndGamesButNotChangeNickname() {
        show(connection = LobbyConnection.Offline, games = twoGames)

        assertEquals("Sin conexión", text(R.string.lobby_offline))
        composeTestRule.onNodeWithText(text(R.string.lobby_offline)).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.lobby_games_header)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(OnlineLobbyTags.CREATE).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(OnlineLobbyTags.game("g1")).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(OnlineLobbyTags.game("g2")).assertIsNotEnabled()
        composeTestRule.onNodeWithText("Juego de Ana").assertIsDisplayed()
        composeTestRule.onNodeWithTag(OnlineLobbyTags.CHANGE_NICKNAME).assertIsEnabled()

        composeTestRule.onNodeWithTag(OnlineLobbyTags.CREATE).performClick()
        composeTestRule.onNodeWithTag(OnlineLobbyTags.game("g1")).performClick()

        assertEquals(0, creations)
        assertTrue(joined.isEmpty())
    }

    @Test
    fun connectedActionsAreEnabled() {
        show(games = twoGames)

        composeTestRule.onNodeWithTag(OnlineLobbyTags.CREATE).assertIsEnabled().assertHasClickAction()
        composeTestRule.onNodeWithTag(OnlineLobbyTags.game("g1")).assertIsEnabled().assertHasClickAction()
    }

    @Test
    fun gameUnavailableIsShownAndReportedOnce() {
        show(message = LobbyMessage.GameUnavailable)
        composeTestRule.waitForIdle()

        assertEquals("Este juego ya no está disponible", text(R.string.lobby_game_unavailable))
        composeTestRule.onNodeWithText(text(R.string.lobby_game_unavailable)).assertIsDisplayed()
        assertEquals(1, shown)
        composeTestRule.onNodeWithText(text(R.string.lobby_games_header)).assertIsDisplayed()
    }

    @Test
    fun createFailedIsShownOverTheLobbyAndReportedOnce() {
        show(message = LobbyMessage.CreateFailed)
        composeTestRule.waitForIdle()

        assertEquals("No se pudo crear el juego", text(R.string.lobby_create_failed))
        composeTestRule.onNodeWithText(text(R.string.lobby_create_failed)).assertIsDisplayed()
        assertEquals(1, shown)
        composeTestRule.onNodeWithTag(OnlineLobbyTags.CREATE).assertIsEnabled()
        composeTestRule.onAllNodesWithText(text(R.string.lobby_waiting_for_opponent)).assertCountEquals(0)
    }

    @Test
    fun aMessageArrivingLaterIsShownAndReported() {
        show()
        assertEquals(0, shown)

        composeTestRule.runOnIdle { currentMessage = LobbyMessage.GameUnavailable }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(text(R.string.lobby_game_unavailable)).assertIsDisplayed()
        assertEquals(1, shown)
    }

    @Test
    fun noMessageMeansNoNoticeAndNoReport() {
        show()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(text(R.string.lobby_game_unavailable)).assertCountEquals(0)
        composeTestRule.onAllNodesWithText(text(R.string.lobby_create_failed)).assertCountEquals(0)
        assertEquals(0, shown)
    }

    @Test
    fun longListInLandscapeScrollsAndEveryItemIsReachable() {
        val games = (1..30).map { LobbyGame("g$it", "Jugador$it") }
        show(games = games, width = 640, height = 360)

        composeTestRule.onNodeWithTag(OnlineLobbyTags.CREATE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(OnlineLobbyTags.CHANGE_NICKNAME).assertIsDisplayed()
        games.forEach { game ->
            composeTestRule.onNodeWithTag(OnlineLobbyTags.LIST)
                .performScrollToNode(hasTestTag(OnlineLobbyTags.game(game.id)))
            composeTestRule.onNodeWithTag(OnlineLobbyTags.game(game.id)).assertIsDisplayed()
            val bounds = composeTestRule.onNodeWithTag(OnlineLobbyTags.game(game.id)).getUnclippedBoundsInRoot()
            assertTrue("${game.id} ends outside the viewport", bounds.bottom <= 360.dp && bounds.right <= 640.dp)
        }
        composeTestRule.onNodeWithTag(OnlineLobbyTags.game("g30")).performClick()
        assertEquals(listOf("g30"), joined)
    }

    @Test
    fun largeFontKeepsActionsReachableInPortraitAndLandscape() {
        listOf(320 to 480, 640 to 360).forEach { (width, height) ->
            show(games = twoGames, width = width, height = height, fontScale = 2f)

            composeTestRule.onNodeWithTag(OnlineLobbyTags.CREATE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(OnlineLobbyTags.CHANGE_NICKNAME).assertIsDisplayed()
            composeTestRule.onNodeWithTag(OnlineLobbyTags.LIST)
                .performScrollToNode(hasTestTag(OnlineLobbyTags.game("g2")))
            composeTestRule.onNodeWithTag(OnlineLobbyTags.game("g2")).assertIsDisplayed()
        }
    }

    @Test
    fun actionsAndGameItemsMeasureAtLeast48Dp() {
        show(games = twoGames)

        listOf(
            OnlineLobbyTags.CREATE,
            OnlineLobbyTags.CHANGE_NICKNAME,
            OnlineLobbyTags.game("g1"),
            OnlineLobbyTags.game("g2"),
        ).forEach { tag ->
            val bounds = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag is shorter than 48 dp", bounds.bottom - bounds.top >= 48.dp)
            assertTrue("$tag is narrower than 48 dp", bounds.right - bounds.left >= 48.dp)
        }
        show(connection = LobbyConnection.IdentityFailed)
        val retry = composeTestRule.onNodeWithTag(OnlineLobbyTags.RETRY).getUnclippedBoundsInRoot()
        assertTrue("retry is shorter than 48 dp", retry.bottom - retry.top >= 48.dp)
    }

    @Test
    fun noStateShowsABoard() {
        listOf(
            LobbyConnection.Connecting,
            LobbyConnection.IdentityFailed,
            LobbyConnection.Offline,
            LobbyConnection.Connected,
        ).forEach { connection ->
            show(connection = connection, games = twoGames)

            composeTestRule.onAllNodesWithTag(LOBBY_BOARD_TAG).assertCountEquals(0)
            composeTestRule.onAllNodes(hasContentDescription("Casilla", substring = true)).assertCountEquals(0)
        }
    }
}
