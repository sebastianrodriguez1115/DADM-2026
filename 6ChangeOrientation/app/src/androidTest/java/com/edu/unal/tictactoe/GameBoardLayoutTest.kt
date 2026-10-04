package com.edu.unal.tictactoe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.tictactoe.ui.game.TicTacToeScreen
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class GameBoardLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hitTargetsStayAtLeast48DpInCompactAndResponsiveViewports() {
        var viewport by mutableStateOf(320 to 192)
        var darkTheme by mutableStateOf(false)
        val density = 1.25f
        val fontScale = 2f
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                TicTacToeTheme(darkTheme = darkTheme, dynamicColor = false) {
                    Surface {
                        Box(Modifier.size(viewport.first.dp, viewport.second.dp)) {
                            TicTacToeScreen(
                                uiState = GameUiState(),
                                onCellClick = {},
                                onNewGame = {},
                            )
                        }
                    }
                }
            }
        }

        // Compact portrait, ordinary portrait, compact landscape, and landscape.
        listOf(320 to 192, 320 to 480, 480 to 192, 480 to 320).forEach { size ->
            composeRule.runOnIdle {
                viewport = size
                darkTheme = !darkTheme
            }
            composeRule.waitForIdle()
            assertBoardAndCells(density, size.first)
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
        var density by mutableStateOf(1.1f)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                TicTacToeTheme(dynamicColor = false) {
                    Surface {
                        Box(Modifier.size(320.dp, 192.dp)) {
                            TicTacToeScreen(GameUiState(), {}, {})
                        }
                    }
                }
            }
        }

        listOf(1.1f, 1.25f, 1.5f).forEach { nextDensity ->
            composeRule.runOnIdle { density = nextDensity }
            composeRule.waitForIdle()
            assertBoardAndCells(nextDensity, viewportWidth = 320)
        }
    }

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
