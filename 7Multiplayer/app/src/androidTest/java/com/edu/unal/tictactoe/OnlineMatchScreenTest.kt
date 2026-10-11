package com.edu.unal.tictactoe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.online.match.MatchPhase
import com.edu.unal.tictactoe.ui.online.match.MatchResult
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchScreen
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchTags
import com.edu.unal.tictactoe.ui.online.match.OnlineMatchUiState
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val MATCH_BOARD_TAG = "tic_tac_toe_board"

@RunWith(AndroidJUnit4::class)
class OnlineMatchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var current by mutableStateOf(OnlineMatchUiState())
    private var leaveDialogOpen by mutableStateOf(false)
    private var hosted = false
    private val clicked = mutableListOf<Int>()
    private var backToLobby = 0
    private var leaves = 0

    private fun playing(
        myMark: Player = Player.X,
        board: List<Mark> = emptyBoard(),
        isMyTurn: Boolean = true,
        isConnected: Boolean = true,
        isMovePending: Boolean = false,
    ) = OnlineMatchUiState(
        myMark = myMark,
        opponentName = "Luis",
        board = board,
        phase = MatchPhase.Playing,
        isMyTurn = isMyTurn,
        isConnected = isConnected,
        isMovePending = isMovePending,
    )

    private fun finished(result: MatchResult) = OnlineMatchUiState(
        myMark = Player.X,
        opponentName = "Luis",
        board = FINAL_BOARD,
        phase = MatchPhase.Finished,
        result = result,
    )

    private val abandoned = OnlineMatchUiState(
        myMark = Player.X,
        opponentName = "Luis",
        board = FINAL_BOARD,
        phase = MatchPhase.Abandoned,
    )

    private fun show(
        state: OnlineMatchUiState,
        width: Int = 360,
        height: Int = 640,
        fontScale: Float = 1f,
    ) {
        current = state
        if (hosted) {
            viewport.show(width, height, fontScale)
            return
        }
        hosted = true
        viewport.setContent(width, height, fontScale) {
            TicTacToeTheme(dynamicColor = false) {
                OnlineMatchScreen(
                    uiState = current,
                    isLeaveDialogOpen = leaveDialogOpen,
                    onLeaveDialogOpenChange = { leaveDialogOpen = it },
                    onLeaveConfirmed = { leaves++ },
                    onCellClick = { clicked += it },
                    onBackToLobby = { backToLobby++ },
                )
            }
        }
    }

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun bounds(tag: String) = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun cellTag(location: Int) = "tic_tac_toe_cell_$location"

    private fun assertCount(tag: String, count: Int) {
        composeTestRule.onAllNodesWithTag(tag).assertCountEquals(count)
    }

    private fun assertAllCellsDisabled() {
        (0 until 9).forEach { composeTestRule.onNodeWithTag(cellTag(it)).assertIsNotEnabled() }
    }

    @Test
    fun creatorSeesXMarkAndTheRival() {
        show(playing(myMark = Player.X))

        assertEquals("Juegas con X", text(R.string.match_mark_x))
        assertEquals("Rival: Luis", text(R.string.match_opponent, "Luis"))
        composeTestRule.onNodeWithText("Juegas con X").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rival: Luis").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Juegas con O").assertCountEquals(0)
    }

    @Test
    fun joinerSeesOMarkAndTheRival() {
        show(playing(myMark = Player.O, isMyTurn = false).copy(opponentName = "Ana"))

        assertEquals("Juegas con O", text(R.string.match_mark_o))
        composeTestRule.onNodeWithText("Juegas con O").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rival: Ana").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Juegas con X").assertCountEquals(0)
    }

    @Test
    fun turnTextNamesWhoPlays() {
        show(playing(isMyTurn = true))
        assertEquals("Tu turno", text(R.string.turn_human))
        composeTestRule.onNodeWithTag(OnlineMatchTags.STATUS).assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu turno").assertIsDisplayed()

        show(playing(isMyTurn = false))
        assertEquals("Turno de Luis", text(R.string.match_turn_opponent, "Luis"))
        composeTestRule.onNodeWithText("Turno de Luis").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Tu turno").assertCountEquals(0)
    }

    @Test
    fun tappingAFreeCellWithInputEnabledDeliversItsLocation() {
        show(playing(board = boardWith(4 to Mark.O)))

        composeTestRule.onNodeWithTag(cellTag(0)).assertIsEnabled().performClick()
        composeTestRule.onNodeWithTag(cellTag(8)).performClick()

        assertEquals(listOf(0, 8), clicked)
    }

    @Test
    fun anOccupiedCellDoesNotRespondEvenWithInputEnabled() {
        show(playing(board = boardWith(4 to Mark.O)))

        composeTestRule.onNodeWithTag(cellTag(4)).assertIsNotEnabled().performClick()

        assertTrue(clicked.isEmpty())
    }

    @Test
    fun withInputDisabledNoCellRespondsAndAllAreExposedAsDisabled() {
        listOf(
            playing(isMyTurn = false),
            playing(isMovePending = true),
            playing(isConnected = false),
        ).forEach { state ->
            show(state)

            assertAllCellsDisabled()
            (0 until 9).forEach { composeTestRule.onNodeWithTag(cellTag(it)).performClick() }
        }

        assertTrue("no cell may respond: $clicked", clicked.isEmpty())
    }

    @Test
    fun victoryShowsTheResultTheLockedFinalBoardAndBackToLobby() {
        show(finished(MatchResult.Won))

        assertEquals("Ganaste", text(R.string.match_result_win))
        composeTestRule.onNodeWithText("Ganaste").assertIsDisplayed()
        assertFinishedBoardAndAction()
    }

    @Test
    fun defeatNamesTheWinner() {
        show(finished(MatchResult.Lost))

        assertEquals("Ganó Luis", text(R.string.match_result_opponent_wins, "Luis"))
        composeTestRule.onNodeWithText("Ganó Luis").assertIsDisplayed()
        assertFinishedBoardAndAction()
    }

    @Test
    fun tieShowsTheResultAndLocksTheBoard() {
        show(finished(MatchResult.Tie))

        assertEquals("Empate", text(R.string.result_tie))
        composeTestRule.onNodeWithText("Empate").assertIsDisplayed()
        assertFinishedBoardAndAction()
    }

    @Test
    fun abandonmentShowsItsTextTheLockedBoardAndBackToLobby() {
        show(abandoned)

        assertEquals("Tu rival salió de la partida", text(R.string.match_opponent_left))
        composeTestRule.onNodeWithText("Tu rival salió de la partida").assertIsDisplayed()
        assertCount(OnlineMatchTags.LEAVE, 0)
        composeTestRule.onAllNodesWithText("Tu turno").assertCountEquals(0)
        assertAllCellsDisabled()
        composeTestRule.onNodeWithContentDescription(text(R.string.cell_x, 1, 1)).assertExists()
        composeTestRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        assertEquals(1, backToLobby)
    }

    @Test
    fun offlineNoticeShowsWithDisabledCellsAndDisappearsOnReconnection() {
        show(playing(isConnected = false))

        assertEquals("Sin conexión", text(R.string.lobby_offline))
        composeTestRule.onNodeWithText("Sin conexión").assertIsDisplayed()
        assertAllCellsDisabled()

        show(playing(isConnected = true))

        assertCount(OnlineMatchTags.OFFLINE, 0)
        composeTestRule.onNodeWithTag(cellTag(0)).assertIsEnabled()
    }

    @Test
    fun noOfflineNoticeOnceTheMatchIsOver() {
        show(finished(MatchResult.Won).copy(isConnected = false))

        assertCount(OnlineMatchTags.OFFLINE, 0)
    }

    @Test
    fun loadingShowsNoActionableBoardNorTurnNorResultText() {
        show(OnlineMatchUiState())

        assertCount(MATCH_BOARD_TAG, 0)
        assertCount(cellTag(0), 0)
        assertCount(OnlineMatchTags.STATUS, 0)
        assertCount(OnlineMatchTags.LEAVE, 0)
        assertCount(OnlineMatchTags.BACK_TO_LOBBY, 0)
        listOf(R.string.turn_human, R.string.result_tie, R.string.match_result_win, R.string.match_opponent_left)
            .forEach { composeTestRule.onAllNodesWithText(text(it)).assertCountEquals(0) }
        assertTrue(clicked.isEmpty())
    }

    @Test
    fun noScoreboardOptionsMenuOrRematchInAnyPhase() {
        val forbidden = listOf(
            R.string.score_label,
            R.string.score_human_wins,
            R.string.score_computer_wins,
            R.string.score_ties,
            R.string.menu_difficulty,
            R.string.menu_reset_scores,
            R.string.menu_sound,
            R.string.new_game,
        )
        listOf(
            OnlineMatchUiState(),
            playing(),
            finished(MatchResult.Won),
            finished(MatchResult.Tie),
            abandoned,
        ).forEach { state ->
            show(state)
            forbidden.forEach { id ->
                val label = context.getString(id).replace(Regex("%\\d[$]d"), "")
                composeTestRule.onAllNodesWithText(label, substring = true).assertCountEquals(0)
            }
            composeTestRule.onAllNodesWithText("Revancha", substring = true).assertCountEquals(0)
            composeTestRule.onAllNodesWithContentDescription(text(R.string.menu_open)).assertCountEquals(0)
        }
    }

    @Test
    fun leaveIsOfferedOnlyWhilePlaying() {
        show(playing())
        composeTestRule.onNodeWithTag(OnlineMatchTags.LEAVE).assertIsDisplayed()
        assertEquals("Salir", text(R.string.match_leave))
        composeTestRule.onNodeWithText("Salir").assertIsDisplayed()
        assertCount(OnlineMatchTags.BACK_TO_LOBBY, 0)

        listOf(OnlineMatchUiState(), finished(MatchResult.Lost), abandoned).forEach { state ->
            show(state)
            assertCount(OnlineMatchTags.LEAVE, 0)
            composeTestRule.onAllNodesWithText("Salir").assertCountEquals(0)
        }
    }

    @Test
    fun backToLobbyIsOfferedOnlyWhenFinishedOrAbandoned() {
        assertEquals("Volver al lobby", text(R.string.match_back_to_lobby))
        listOf(OnlineMatchUiState(), playing()).forEach { state ->
            show(state)
            assertCount(OnlineMatchTags.BACK_TO_LOBBY, 0)
        }
        show(finished(MatchResult.Won))
        composeTestRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        assertEquals(1, backToLobby)
    }

    @Test
    fun wideLandscapePlacesTheBoardOnTheLeftAndTheStateAndActionsOnTheRight() {
        show(playing(), width = 640, height = 360)

        val board = bounds(MATCH_BOARD_TAG)
        val tolerance = 1f
        listOf(
            OnlineMatchTags.MARK,
            OnlineMatchTags.OPPONENT,
            OnlineMatchTags.STATUS,
            OnlineMatchTags.LEAVE,
        ).forEach { tag ->
            val box = bounds(tag)
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
            assertTrue("$tag must be right of the board", board.right.value <= box.left.value + tolerance)
            assertTrue("$tag must not be clipped", box.right.value <= 640f + tolerance)
            assertTrue("$tag must not be clipped", box.bottom.value <= 360f + tolerance)
        }
        assertTrue("board must not be clipped", board.left.value >= -tolerance)
        assertTrue("board must fit the height", board.bottom.value <= 360f + tolerance)
        assertEquals("board must be square", board.right.value - board.left.value, board.bottom.value - board.top.value, tolerance)
        assertCellsAtLeast48DpInside(board.left.value, board.right.value)
    }

    @Test
    fun portraitStacksTheBoardAboveTheStateAndTheAction() {
        show(playing(), width = 360, height = 640)

        val board = bounds(MATCH_BOARD_TAG)
        val mark = bounds(OnlineMatchTags.MARK)
        val status = bounds(OnlineMatchTags.STATUS)
        val leave = bounds(OnlineMatchTags.LEAVE)
        assertTrue(board.bottom <= mark.top)
        assertTrue(mark.bottom <= status.top)
        assertTrue(status.bottom <= leave.top)
        assertTrue("nothing may leave the viewport", leave.bottom.value <= 640f + 1f)
        assertEquals(360f, board.right.value - board.left.value, 32f)
    }

    @Test
    fun largeFontInPortraitKeepsEverythingReachable() {
        listOf(playing(isConnected = false), finished(MatchResult.Lost)).forEach { state ->
            show(state, width = 320, height = 480, fontScale = 2f)
            assertEverythingReachable(state, width = 320)
        }
    }

    @Test
    fun largeFontInLandscapeKeepsEverythingReachable() {
        listOf(
            Triple(playing(isConnected = false), 640, 360),
            Triple(finished(MatchResult.Lost), 640, 360),
            Triple(playing(isConnected = false), 560, 320),
            Triple(abandoned, 320, 192),
        ).forEach { (state, width, height) ->
            show(state, width = width, height = height, fontScale = 2f)
            assertEverythingReachable(state, width = width)
        }
    }

    @Test
    fun wideLandscapeWithLargeFontKeepsTheBoardOnTheLeft() {
        show(playing(), width = 640, height = 360, fontScale = 2f)

        val board = bounds(MATCH_BOARD_TAG)
        val leave = bounds(OnlineMatchTags.LEAVE)
        assertTrue(board.right.value <= leave.left.value + 1f)
        assertEverythingReachable(playing(), width = 640)
    }

    @Test
    fun nineCellsMeasureAtLeast48DpAndExposeLocationContentAndAvailability() {
        val board = boardWith(0 to Mark.X, 4 to Mark.O)
        show(playing(board = board), width = 320, height = 480)

        val boardBounds = bounds(MATCH_BOARD_TAG)
        assertCellsAtLeast48DpInside(boardBounds.left.value, boardBounds.right.value)
        composeTestRule.onNodeWithContentDescription(text(R.string.board_label)).assertExists()
        (0 until 9).forEach { location ->
            val row = location / 3 + 1
            val column = location % 3 + 1
            val description = when (board[location]) {
                Mark.X -> text(R.string.cell_x, row, column)
                Mark.O -> text(R.string.cell_o, row, column)
                Mark.Empty -> text(R.string.cell_empty, row, column)
            }
            val node = composeTestRule.onNodeWithContentDescription(description)
            node.assertExists()
            if (board[location] == Mark.Empty) node.assertIsEnabled() else node.assertIsNotEnabled()
        }
    }

    @Test
    fun finishedStateKeepsTheResultAndTheActionReachableWithDisabledCells() {
        show(finished(MatchResult.Won), width = 320, height = 480, fontScale = 2f)

        composeTestRule.onNodeWithTag(OnlineMatchTags.STATUS).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performScrollTo().assertIsDisplayed()
        assertAllCellsDisabled()
    }

    @Test
    fun actionsMeasureAtLeast48DpAndCarryTheirResourceLabel() {
        listOf(
            playing() to (OnlineMatchTags.LEAVE to R.string.match_leave),
            finished(MatchResult.Won) to (OnlineMatchTags.BACK_TO_LOBBY to R.string.match_back_to_lobby),
            abandoned to (OnlineMatchTags.BACK_TO_LOBBY to R.string.match_back_to_lobby),
        ).forEach { (state, action) ->
            show(state)
            val (tag, label) = action
            composeTestRule.onNodeWithTag(tag).assertIsEnabled()
            composeTestRule.onNodeWithText(text(label)).assertIsDisplayed()
            val box = bounds(tag)
            assertTrue("$tag is shorter than 48 dp", box.bottom - box.top >= 48.dp - 1.dp)
            assertTrue("$tag is narrower than 48 dp", box.right - box.left >= 48.dp - 1.dp)
        }
    }

    private fun assertFinishedBoardAndAction() {
        assertCount(OnlineMatchTags.LEAVE, 0)
        assertAllCellsDisabled()
        // The final position stays on the board: the X at row 1, column 1 and the O at row 2, column 2.
        composeTestRule.onNodeWithContentDescription(text(R.string.cell_x, 1, 1)).assertExists()
        composeTestRule.onNodeWithContentDescription(text(R.string.cell_o, 2, 2)).assertExists()
        (0 until 9).forEach { composeTestRule.onNodeWithTag(cellTag(it)).performClick() }
        assertTrue(clicked.isEmpty())
        composeTestRule.onNodeWithTag(OnlineMatchTags.BACK_TO_LOBBY).performClick()
        assertEquals(1, backToLobby)
        assertEquals(0, leaves)
    }

    private fun assertCellsAtLeast48DpInside(left: Float, right: Float) {
        val tolerance = 1f
        (0 until 9).forEach { location ->
            val cell = bounds(cellTag(location))
            val width = cell.right.value - cell.left.value
            val height = cell.bottom.value - cell.top.value
            assertTrue("cell $location is narrower than 48 dp ($width)", width >= 48f - tolerance)
            assertTrue("cell $location is shorter than 48 dp ($height)", height >= 48f - tolerance)
            assertTrue(cell.left.value >= left - tolerance)
            assertTrue(cell.right.value <= right + tolerance)
        }
    }

    private fun assertEverythingReachable(state: OnlineMatchUiState, width: Int) {
        val tags = buildList {
            add(MATCH_BOARD_TAG)
            add(OnlineMatchTags.MARK)
            add(OnlineMatchTags.OPPONENT)
            add(OnlineMatchTags.STATUS)
            if (state.isOfflineNoticeVisible) add(OnlineMatchTags.OFFLINE)
            add(if (state.phase == MatchPhase.Playing) OnlineMatchTags.LEAVE else OnlineMatchTags.BACK_TO_LOBBY)
        }
        tags.forEach { tag ->
            composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            val box = bounds(tag)
            assertTrue("$tag clipped on the left", box.left.value >= -1f)
            assertTrue("$tag clipped on the right", box.right.value <= width + 1f)
        }
        (0 until 9).forEach { composeTestRule.onNodeWithTag(cellTag(it)).performScrollTo().assertIsDisplayed() }
        val boardBounds = bounds(MATCH_BOARD_TAG)
        assertCellsAtLeast48DpInside(boardBounds.left.value, boardBounds.right.value)
    }

    private companion object {
        fun emptyBoard(): List<Mark> = List(9) { Mark.Empty }

        fun boardWith(vararg marks: Pair<Int, Mark>): List<Mark> =
            emptyBoard().toMutableList().also { board -> marks.forEach { (index, mark) -> board[index] = mark } }

        // A full board: the final position that stays visible once the match is over.
        val FINAL_BOARD: List<Mark> = listOf(
            Mark.X, Mark.X, Mark.O,
            Mark.O, Mark.O, Mark.X,
            Mark.X, Mark.O, Mark.X,
        )
    }
}
