package com.edu.unal.tictactoe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.tictactoe.ui.game.TicTacToeScreen
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val BOARD_TAG = "tic_tac_toe_board"
private const val STATUS_TAG = "tic_tac_toe_status"
private const val SCORE_TAG = "tic_tac_toe_score"
private const val NEW_GAME_TAG = "tic_tac_toe_new_game"
private const val MENU_STUB_TAG = "game_board_layout_test_menu"
private const val TOLERANCE = 1f

@RunWith(AndroidJUnit4::class)
class GameBoardLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val viewport = FixedViewport(composeRule)

    @Test
    fun hitTargetsStayAtLeast48DpInCompactAndResponsiveViewports() {
        var darkTheme by mutableStateOf(false)
        val density = 1.25f
        val fontScale = 2f
        viewport.setContent(320, 192, fontScale, density) {
            TicTacToeTheme(darkTheme = darkTheme, dynamicColor = false) {
                Surface {
                    TicTacToeScreen(
                        uiState = GameUiState(),
                        onCellClick = {},
                        onNewGame = {},
                    )
                }
            }
        }

        // Compact portrait, ordinary portrait, compact landscape, and landscape.
        listOf(320 to 192, 320 to 480, 480 to 192, 480 to 320).forEach { size ->
            composeRule.runOnIdle { darkTheme = !darkTheme }
            viewport.show(size.first, size.second, fontScale, density)
            assertBoardAndCells(viewport.density, size.first)
            if (size.second == 192) {
                listOf("tic_tac_toe_status", "tic_tac_toe_score", "tic_tac_toe_new_game")
                    .forEach { tag ->
                        composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                    }
            }
        }
    }

    @Test
    fun fractionalDensitiesDoNotRoundHitTargetsBelow48Dp() {
        viewport.setContent(320, 192, fontScale = 2f, density = 1.1f) {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    TicTacToeScreen(GameUiState(), {}, {})
                }
            }
        }

        listOf(1.1f, 1.25f, 1.5f).forEach { nextDensity ->
            viewport.show(320, 192, fontScale = 2f, density = nextDensity)
            assertBoardAndCells(nextDensity, viewportWidth = 320)
        }
    }

    @Test
    fun wideLandscapePlacesTheSquareBoardLeftOfStatusScoreAndControlsWithoutScrolling() {
        setScreen(640 to 360)

        listOf(640 to 360, 915 to 412).forEach { (width, height) ->
            viewport.show(width, height)

            val board = bounds(BOARD_TAG)
            val side = board.right.value - board.left.value
            assertEquals("board must be square", side, board.bottom.value - board.top.value, TOLERANCE)
            assertEquals("room allows the 270 dp reference", 270f, side, TOLERANCE)
            assertTrue("board must fit the height", side <= height - 32f + TOLERANCE)
            assertBoardAndCells(viewport.density, width)

            val rightPanel = listOf(STATUS_TAG, SCORE_TAG, NEW_GAME_TAG, MENU_STUB_TAG)
            rightPanel.forEach { tag ->
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                val box = bounds(tag)
                assertTrue("board must be left of $tag", board.right.value <= box.left.value + TOLERANCE)
                assertTrue("$tag must fit the viewport", box.right.value <= width + TOLERANCE)
                assertTrue("$tag must fit the viewport", box.bottom.value <= height + TOLERANCE)
                assertTrue("$tag must fit the viewport", box.top.value >= -TOLERANCE)
            }
            assertTrue("board must fit the viewport", board.bottom.value <= height + TOLERANCE)
        }
    }

    @Test
    fun heightLimitedLandscapeShrinksTheBoardBelowTheReferenceKeepingCellsUsable() {
        setScreen(480 to 192)

        listOf(480 to 192, 640 to 220).forEach { (width, height) ->
            viewport.show(width, height)

            val board = bounds(BOARD_TAG)
            val side = board.right.value - board.left.value
            assertEquals("board must be square", side, board.bottom.value - board.top.value, TOLERANCE)
            assertTrue("height must limit the board below 270 dp, was $side", side < 270f - TOLERANCE)
            assertBoardAndCells(viewport.density, width)
            listOf(STATUS_TAG, SCORE_TAG, NEW_GAME_TAG, MENU_STUB_TAG).forEach { tag ->
                composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                assertTrue("board must be left of $tag", board.right.value <= bounds(tag).left.value + TOLERANCE)
            }
        }
    }

    private fun setScreen(initial: Pair<Int, Int>) {
        viewport.setContent(initial.first, initial.second) {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    TicTacToeScreen(
                        uiState = GameUiState(),
                        onCellClick = {},
                        onNewGame = {},
                        menu = { Box(Modifier.size(48.dp).testTag(MENU_STUB_TAG)) },
                    )
                }
            }
        }
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertBoardAndCells(density: Float, viewportWidth: Int) {
        val board = composeRule.onNodeWithTag("tic_tac_toe_board")
            .getUnclippedBoundsInRoot()
        val boardWidthPx = (board.right - board.left).value * density
        val boardHeightPx = (board.bottom - board.top).value * density
        assertTrue("board must remain square", kotlin.math.abs(boardWidthPx - boardHeightPx) <= 1.1f)
        assertTrue("board must fit available width", boardWidthPx <= (viewportWidth - 32f) * density + 1f)
        assertTrue("board must respect horizontal screen padding", board.left.value * density >= 16f * density - 1f)
        assertTrue(
            "board must respect horizontal screen padding",
            board.right.value * density <= (viewportWidth - 16f) * density + 1f,
        )

        val minimumTargetPx = ceil(48f * density)
        val bounds = (0..8).map { index ->
            composeRule.onNodeWithTag("tic_tac_toe_cell_$index").getUnclippedBoundsInRoot()
        }
        bounds.forEachIndexed { index, cell ->
            val widthPx = (cell.right - cell.left).value * density
            val heightPx = (cell.bottom - cell.top).value * density
            assertTrue("cell $index width < 48dp at density $density", widthPx + 0.1f >= minimumTargetPx)
            assertTrue("cell $index height < 48dp at density $density", heightPx + 0.1f >= minimumTargetPx)
            assertTrue(cell.left >= board.left)
            assertTrue(cell.top >= board.top)
            assertTrue(cell.right <= board.right)
            assertTrue(cell.bottom <= board.bottom)
        }

        // Confirm the measured cells still form three aligned columns and rows.
        val widthsPx = bounds.map { ((it.right - it.left).value * density).roundToInt() }
        val heightsPx = bounds.map { ((it.bottom - it.top).value * density).roundToInt() }
        assertTrue(widthsPx.maxOrNull()!! - widthsPx.minOrNull()!! <= 1)
        assertTrue(heightsPx.maxOrNull()!! - heightsPx.minOrNull()!! <= 1)

        val leftsPx = bounds.map { (it.left.value * density).roundToInt() }
        val rightsPx = bounds.map { (it.right.value * density).roundToInt() }
        val topsPx = bounds.map { (it.top.value * density).roundToInt() }
        val bottomsPx = bounds.map { (it.bottom.value * density).roundToInt() }
        for (row in 0..2) {
            for (column in 0..2) {
                val index = row * 3 + column
                assertTrue("column $column left edges must align", (0..2).all { otherRow ->
                    kotlin.math.abs(leftsPx[index] - leftsPx[otherRow * 3 + column]) <= 1
                })
                assertTrue("column $column right edges must align", (0..2).all { otherRow ->
                    kotlin.math.abs(rightsPx[index] - rightsPx[otherRow * 3 + column]) <= 1
                })
                assertTrue("row $row top edges must align", (0..2).all { otherColumn ->
                    kotlin.math.abs(topsPx[index] - topsPx[row * 3 + otherColumn]) <= 1
                })
                assertTrue("row $row bottom edges must align", (0..2).all { otherColumn ->
                    kotlin.math.abs(bottomsPx[index] - bottomsPx[row * 3 + otherColumn]) <= 1
                })
            }
        }
        assertTrue(leftsPx[0] < leftsPx[1] && leftsPx[1] < leftsPx[2])
        assertTrue(topsPx[0] < topsPx[3] && topsPx[3] < topsPx[6])
    }
}
