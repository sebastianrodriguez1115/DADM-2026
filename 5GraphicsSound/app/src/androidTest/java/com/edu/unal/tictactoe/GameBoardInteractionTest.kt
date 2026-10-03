package com.edu.unal.tictactoe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.GameBoard
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameBoardInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun exposesNineLabeledButtonHitboxesAndEachTouchRequestsOneMove() {
        val requested = mutableListOf<Int>()
        compose.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    Box(Modifier.size(360.dp, 500.dp)) {
                        GameBoard(List(9) { Mark.Empty }, true, Turn.Human, requested::add)
                    }
                }
            }
        }
        compose.waitForIdle()
        (0..8).forEach { index ->
            val cell = compose.onNodeWithTag("tic_tac_toe_cell_$index")
                .assertIsEnabled()
                .assertHasClickAction()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assert(
                    hasContentDescription(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                            .targetContext.getString(
                                R.string.cell_empty,
                                index / 3 + 1,
                                index % 3 + 1,
                            ),
                    ),
                )
            val bounds = cell.getUnclippedBoundsInRoot()
            assertTrue("cell $index width >=48dp", bounds.right - bounds.left >= 48.dp)
            assertTrue("cell $index height >=48dp", bounds.bottom - bounds.top >= 48.dp)
            cell.performTouchInput { click() }
        }
        assertEquals((0..8).toList(), requested)
        compose.onRoot().performTouchInput {
            click(Offset(center.x, 499.dp.toPx()))
        }
        assertEquals("outside the square board must not dispatch", 9, requested.size)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun availableCellAcceptsKeyboardEnterAndDisabledCellHasNoActivationAction() {
        var mark by mutableStateOf(Mark.Empty)
        var calls = 0
        compose.setContent {
            KeyboardInputMode {
                TicTacToeTheme(dynamicColor = false) {
                    Surface {
                        GameBoard(
                            board = listOf(mark) + List(8) { Mark.Empty },
                            isGameActive = true,
                            turn = Turn.Human,
                            onCellClick = { calls++; mark = Mark.X },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val cell = compose.onNodeWithTag("tic_tac_toe_cell_0")
        cell.requestFocus()
        cell.assertIsFocused()
        cell.performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, calls)
        cell.assertIsNotEnabled()
        // Disabled cells remain in the accessibility tree, but expose no executable click action.
        assertTrue(SemanticsActions.OnClick !in cell.fetchSemanticsNode().config)
        cell.performTouchInput { click() }
        assertEquals(1, calls)
    }

    @Test
    fun availableCellClickRequestsExactlyOneMove() {
        val requested = mutableListOf<Int>()
        compose.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    GameBoard(List(9) { Mark.Empty }, true, Turn.Human, requested::add)
                }
            }
        }
        compose.onNodeWithTag("tic_tac_toe_cell_4").performClick()
        assertEquals(listOf(4), requested)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun keyboardTabSkipsOccupiedCellAndEnterActivatesTheFocusedAvailableCell() {
        val requested = mutableListOf<Int>()
        compose.setContent {
            KeyboardInputMode {
                TicTacToeTheme(dynamicColor = false) {
                    Surface {
                        GameBoard(
                            board = listOf(Mark.Empty, Mark.X) + List(7) { Mark.Empty },
                            isGameActive = true,
                            turn = Turn.Human,
                            onCellClick = requested::add,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val firstAvailable = compose.onNodeWithTag("tic_tac_toe_cell_0")
        val occupied = compose.onNodeWithTag("tic_tac_toe_cell_1")
        val nextAvailable = compose.onNodeWithTag("tic_tac_toe_cell_2")
        occupied.assertIsNotEnabled()
        assertTrue(SemanticsActions.OnClick !in occupied.fetchSemanticsNode().config)

        firstAvailable.requestFocus()
        firstAvailable.assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.Tab) }
        nextAvailable.assertIsFocused()
        nextAvailable.performKeyInput { pressKey(Key.Enter) }

        assertEquals(listOf(2), requested)
    }

    @Test
    fun computerTurnAndTerminalStateExposeDisabledCellsWithoutCallbacks() {
        var calls = 0
        var gameActive by mutableStateOf(true)
        var turn by mutableStateOf(Turn.Computer)
        compose.setContent {
            TicTacToeTheme(dynamicColor = false) {
                Surface {
                    GameBoard(List(9) { Mark.Empty }, gameActive, turn) { calls++ }
                }
            }
        }
        compose.waitForIdle()
        listOf(Turn.Computer to true, Turn.Human to false).forEach { (stateTurn, active) ->
            compose.runOnIdle {
                turn = stateTurn
                gameActive = active
            }
            compose.waitForIdle()
            (0..8).forEach { index ->
                val disabled = compose.onNodeWithTag("tic_tac_toe_cell_$index")
                    .assertIsNotEnabled()
                assertTrue(SemanticsActions.OnClick !in disabled.fetchSemanticsNode().config)
                disabled.performTouchInput { click() }
            }
        }
        assertEquals(0, calls)
    }
}

@Composable
private fun KeyboardInputMode(content: @Composable () -> Unit) {
    val inputModeManager = LocalInputModeManager.current
    LaunchedEffect(inputModeManager) {
        inputModeManager.requestInputMode(InputMode.Keyboard)
    }
    content()
}
