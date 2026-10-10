package com.edu.unal.tictactoe

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameBoard
import com.edu.unal.tictactoe.ui.game.TicTacToeScreen
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import com.edu.unal.tictactoe.ui.theme.TicTacToeODark
import com.edu.unal.tictactoe.ui.theme.TicTacToeOLight
import com.edu.unal.tictactoe.ui.theme.TicTacToeXDark
import com.edu.unal.tictactoe.ui.theme.TicTacToeXLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameBoardRenderingTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewport = FixedViewport(composeTestRule)

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersVectorMarksWithThemeColorsAndNoTextAcrossThemes() {
        var darkTheme by mutableStateOf(false)
        var xColor = 0
        var oColor = 0
        composeTestRule.setContent {
            TicTacToeTheme(darkTheme = darkTheme, dynamicColor = false) {
                xColor = if (darkTheme) TicTacToeXDark.toArgb() else TicTacToeXLight.toArgb()
                oColor = if (darkTheme) TicTacToeODark.toArgb() else TicTacToeOLight.toArgb()
                Surface {
                    GameBoard(
                        board = listOf(Mark.X, Mark.O) + List(7) { Mark.Empty },
                        isGameActive = true,
                        turn = Turn.Human,
                        onCellClick = {},
                    )
                }
            }
        }

        listOf(false, true).forEach { dark ->
            composeTestRule.runOnIdle { darkTheme = dark }
            composeTestRule.waitForIdle()
            val x = composeTestRule.onNodeWithTag("tic_tac_toe_cell_0").captureToImage().toPixelMap()
            val o = composeTestRule.onNodeWithTag("tic_tac_toe_cell_1").captureToImage().toPixelMap()
            assertTrue("X vector strokes visible", x.toPixels().count { it == xColor } > 20)
            assertTrue("O vector ring visible", o.toPixels().count { it == oColor } > 20)
            assertNotEquals("O is hollow", oColor, o[o.width / 2, o.height / 2].toArgb())
            assertEquals(0, composeTestRule.onAllNodesWithText("X").fetchSemanticsNodes().size)
            assertEquals(0, composeTestRule.onAllNodesWithText("O").fetchSemanticsNodes().size)
        }
    }

    private fun androidx.compose.ui.graphics.PixelMap.toPixels(): List<Int> =
        (0 until height).flatMap { y -> (0 until width).map { x -> this[x, y].toArgb() } }

    @Test
    fun rendersFourThirdsGridLinesWithoutBlockingNineCellActions() {
        val callbacks = mutableListOf<Int>()
        var gridColor = 0
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                gridColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                Surface {
                    GameBoard(
                        board = List(9) { Mark.Empty },
                        isGameActive = true,
                        turn = Turn.Human,
                        onCellClick = callbacks::add,
                    )
                }
            }
        }

        val image = composeTestRule.onNodeWithTag("tic_tac_toe_board").captureToImage()
        val pixels = image.toPixelMap()
        val mid = pixels.height / 2
        val verticalLines = listOf(pixels.width / 3, pixels.width * 2 / 3)
        verticalLines.forEach { x ->
            assertEquals("vertical grid line at x=$x", gridColor, pixels[x, mid].toArgb())
        }
        val horizontalLines = listOf(pixels.height / 3, pixels.height * 2 / 3)
        horizontalLines.forEach { y ->
            assertEquals(
                "horizontal grid line at y=$y",
                gridColor,
                pixels[pixels.width / 2, y].toArgb(),
            )
        }
        assertNotEquals(gridColor, pixels[pixels.width / 6, mid].toArgb())
        assertNotEquals(gridColor, pixels[pixels.width / 2, pixels.height / 6].toArgb())

        (0 until 9).forEach { index ->
            composeTestRule.onNodeWithTag("tic_tac_toe_cell_$index")
                .performTouchInput { click(center) }
        }
        assertEquals((0 until 9).toList(), callbacks)
    }

    @Test
    fun marksGridAndLabelsRenderTheSameInWideAndCompactScreenLayouts() {
        var darkTheme by mutableStateOf(false)
        var xColor = 0
        var oColor = 0
        var gridColor = 0
        viewport.setContent(640, 360) {
            TicTacToeTheme(darkTheme = darkTheme, dynamicColor = false) {
                xColor = if (darkTheme) TicTacToeXDark.toArgb() else TicTacToeXLight.toArgb()
                oColor = if (darkTheme) TicTacToeODark.toArgb() else TicTacToeOLight.toArgb()
                gridColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                Surface {
                    TicTacToeScreen(
                        uiState = GameUiState(
                            board = listOf(Mark.X, Mark.O) + List(7) { Mark.Empty },
                        ),
                        onCellClick = {},
                        onNewGame = {},
                    )
                }
            }
        }

        // Wide landscape layout first, then the compact vertical flow, each in both themes.
        listOf(640 to 360, 360 to 640).forEach { size ->
            viewport.show(size.first, size.second)
            listOf(false, true).forEach { dark ->
                composeTestRule.runOnIdle { darkTheme = dark }
                composeTestRule.waitForIdle()
                val case = "at $size, dark=$dark"
                val x = composeTestRule.onNodeWithTag("tic_tac_toe_cell_0").captureToImage().toPixelMap()
                val o = composeTestRule.onNodeWithTag("tic_tac_toe_cell_1").captureToImage().toPixelMap()
                assertTrue("X vector strokes visible $case", x.toPixels().count { it == xColor } > 20)
                assertTrue("O vector ring visible $case", o.toPixels().count { it == oColor } > 20)
                assertNotEquals("O is hollow $case", oColor, o[o.width / 2, o.height / 2].toArgb())

                val board = composeTestRule.onNodeWithTag("tic_tac_toe_board").captureToImage().toPixelMap()
                assertEquals(gridColor, board[board.width / 3, board.height / 2].toArgb())
                assertEquals(gridColor, board[board.width / 2, board.height * 2 / 3].toArgb())
                assertNotEquals(gridColor, board[board.width / 6, board.height / 2].toArgb())
            }

            composeTestRule.onNodeWithTag("tic_tac_toe_cell_0")
                .assertContentDescriptionEquals(context.getString(R.string.cell_x, 1, 1))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            composeTestRule.onNodeWithTag("tic_tac_toe_cell_2")
                .assertContentDescriptionEquals(context.getString(R.string.cell_empty, 1, 3))
                .assertHasClickAction()
            (0 until 9).forEach { index ->
                val cell = composeTestRule.onNodeWithTag("tic_tac_toe_cell_$index")
                    .getUnclippedBoundsInRoot()
                assertTrue("cell $index width at $size", cell.right - cell.left >= 48.dp - 1.dp)
                assertTrue("cell $index height at $size", cell.bottom - cell.top >= 48.dp - 1.dp)
            }
        }
    }

    @Test
    fun cellsRemainSemanticallyClickable() {
        var callback: Int? = null
        composeTestRule.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    GameBoard(
                        board = List(9) { Mark.Empty },
                        isGameActive = true,
                        turn = Turn.Human,
                        onCellClick = { callback = it },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("tic_tac_toe_cell_4").performClick()
        assertEquals(4, callback)
    }
}
