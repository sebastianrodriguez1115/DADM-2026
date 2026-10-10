package com.edu.unal.tictactoe.game

import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TicTacToeGameTest {
    @Test
    fun initialBoardHasNineEmptyCells() {
        val game = TicTacToeGame()

        assertEquals(9, game.board.size)
        assertEquals(List(9) { Mark.Empty }, game.board)
        assertEquals(Mark.Empty.symbol, ' ')
    }

    @Test
    fun boardSnapshotsDoNotExposeTheGameContainer() {
        val game = TicTacToeGame()
        val firstSnapshot = game.board

        assertNotSame(firstSnapshot, game.board)
        @Suppress("UNCHECKED_CAST")
        val exposed = firstSnapshot as MutableList<Mark>
        assertThrows(UnsupportedOperationException::class.java) {
            exposed[0] = Mark.X
        }
        val consumerCopy = game.board.toMutableList()
        consumerCopy[0] = Mark.O
        assertEquals(Mark.Empty, game.board[0])
        assertEquals(Mark.Empty, firstSnapshot[0])
        assertEquals(Mark.O, consumerCopy[0])
    }

    @Test
    fun clearBoardLeavesTheBoardEmptyAndIsIdempotent() {
        val game = TicTacToeGame()

        game.clearBoard()
        val firstClear = game.board
        game.clearBoard()

        assertEquals(List(9) { Mark.Empty }, firstClear)
        assertEquals(firstClear, game.board)
    }

    @Test
    fun appliedMovesPlaceTheCorrespondingMark() {
        val game = TicTacToeGame()

        assertEquals(MoveResult.Applied, game.setMove(Player.X, 0))
        assertEquals(Mark.X, game.board[0])
        assertEquals(MoveResult.Applied, game.setMove(Player.O, 1))
        assertEquals(Mark.O, game.board[1])
        assertEquals(GameResult.InProgress, game.checkForWinner())
    }

    @Test
    fun invalidLocationsDoNotChangeTheBoard() {
        val game = TicTacToeGame()
        val before = game.board

        for (location in listOf(-1, 9, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertEquals(MoveResult.InvalidLocation, game.setMove(Player.X, location))
            assertEquals(before, game.board)
        }
    }

    @Test
    fun occupiedCellsRejectBothPlayersWithoutChangingTheBoard() {
        val game = TicTacToeGame()

        assertEquals(MoveResult.Applied, game.setMove(Player.X, 0))
        val afterX = game.board
        assertEquals(MoveResult.Occupied, game.setMove(Player.O, 0))
        assertEquals(afterX, game.board)

        assertEquals(MoveResult.Applied, game.setMove(Player.O, 1))
        val afterO = game.board
        assertEquals(MoveResult.Occupied, game.setMove(Player.X, 1))
        assertEquals(afterO, game.board)
    }

    @Test
    fun everyWinningLineWinsForBothPlayersOnlyOnTheThirdMark() {
        for (player in Player.values()) {
            for (line in winningLines) {
                val game = TicTacToeGame()
                val otherPlayer = if (player == Player.X) Player.O else Player.X
                val otherLocations = (0..8).filter { it !in line }

                assertEquals(MoveResult.Applied, game.setMove(player, line[0]))
                assertEquals(MoveResult.Applied, game.setMove(player, line[1]))
                assertEquals(GameResult.InProgress, game.checkForWinner())
                assertEquals(MoveResult.Applied, game.setMove(otherPlayer, otherLocations[0]))
                assertEquals(MoveResult.Applied, game.setMove(otherPlayer, otherLocations[1]))
                assertEquals(GameResult.InProgress, game.checkForWinner())

                assertEquals(MoveResult.Applied, game.setMove(player, line[2]))
                assertEquals(GameResult.Won(player), game.checkForWinner())
            }
        }
    }

    @Test
    fun fullBoardWithoutAWinningLineIsATie() {
        val game = TicTacToeGame()
        val moves = listOf(
            Player.X, Player.O, Player.X,
            Player.X, Player.O, Player.O,
            Player.O, Player.X, Player.X,
        )

        for ((location, player) in moves.withIndex()) {
            assertEquals(MoveResult.Applied, game.setMove(player, location))
        }

        assertEquals(GameResult.Tie, game.checkForWinner())
    }

    @Test
    fun aWinningLastCellTakesPriorityOverTieForBothPlayers() {
        for (player in Player.values()) {
            val game = TicTacToeGame()
            val otherPlayer = if (player == Player.X) Player.O else Player.X
            val firstEightMoves = listOf(
                player, otherPlayer, otherPlayer,
                otherPlayer, player, player,
                player, otherPlayer,
            )

            for ((location, markOwner) in firstEightMoves.withIndex()) {
                assertEquals(MoveResult.Applied, game.setMove(markOwner, location))
            }
            assertEquals(GameResult.InProgress, game.checkForWinner())

            assertEquals(MoveResult.Applied, game.setMove(player, 8))
            assertEquals(GameResult.Won(player), game.checkForWinner())
        }
    }

    @Test
    fun terminalWinnerOrTieRejectsMovesWithoutChangingTheBoard() {
        val winningGame = TicTacToeGame()
        winningGame.setMove(Player.X, 0)
        winningGame.setMove(Player.X, 1)
        winningGame.setMove(Player.X, 2)
        assertEquals(GameResult.Won(Player.X), winningGame.checkForWinner())
        val winningBoard = winningGame.board

        assertEquals(MoveResult.GameAlreadyFinished, winningGame.setMove(Player.O, 3))
        assertEquals(winningBoard, winningGame.board)

        val tieGame = TicTacToeGame()
        val tieMoves = listOf(
            Player.X, Player.O, Player.X,
            Player.X, Player.O, Player.O,
            Player.O, Player.X, Player.X,
        )
        for ((location, player) in tieMoves.withIndex()) {
            tieGame.setMove(player, location)
        }
        assertEquals(GameResult.Tie, tieGame.checkForWinner())
        val tieBoard = tieGame.board

        assertEquals(MoveResult.GameAlreadyFinished, tieGame.setMove(Player.X, 0))
        assertEquals(tieBoard, tieGame.board)
    }

    @Test
    fun clearBoardResetsOccupiedAndFinishedGamesAndAllowsMovesAgain() {
        val game = TicTacToeGame()
        game.setMove(Player.X, 0)
        game.setMove(Player.O, 1)

        game.clearBoard()

        assertEquals(List(9) { Mark.Empty }, game.board)
        assertEquals(GameResult.InProgress, game.checkForWinner())
        assertEquals(MoveResult.Applied, game.setMove(Player.O, 8))

        game.clearBoard()
        game.setMove(Player.X, 0)
        game.setMove(Player.X, 1)
        game.setMove(Player.X, 2)
        assertEquals(GameResult.Won(Player.X), game.checkForWinner())

        game.clearBoard()

        assertEquals(List(9) { Mark.Empty }, game.board)
        assertEquals(GameResult.InProgress, game.checkForWinner())
        assertEquals(MoveResult.Applied, game.setMove(Player.X, 4))
    }

    @Test
    fun checkingTheWinnerDoesNotMutateTheBoard() {
        val game = TicTacToeGame()
        game.setMove(Player.X, 0)
        game.setMove(Player.O, 4)
        val before = game.board

        assertEquals(GameResult.InProgress, game.checkForWinner())
        assertEquals(GameResult.InProgress, game.checkForWinner())
        assertEquals(before, game.board)
    }

    @Test
    fun oldSnapshotsRemainStableAfterMovesAndClearBoard() {
        val game = TicTacToeGame()
        val initialSnapshot = game.board

        assertEquals(MoveResult.Applied, game.setMove(Player.X, 0))
        assertEquals(List(9) { Mark.Empty }, initialSnapshot)

        val afterFirstMove = game.board
        assertEquals(MoveResult.Applied, game.setMove(Player.O, 1))
        assertEquals(Mark.X, afterFirstMove[0])
        assertEquals(Mark.Empty, afterFirstMove[1])

        game.clearBoard()

        assertEquals(Mark.X, afterFirstMove[0])
        assertEquals(Mark.Empty, afterFirstMove[1])
        assertEquals(List(9) { Mark.Empty }, game.board)
    }

    @Test
    fun markSymbolsMatchTheDomainContract() {
        assertEquals('X', Mark.X.symbol)
        assertEquals('O', Mark.O.symbol)
        assertEquals(' ', Mark.Empty.symbol)
    }

    @Test
    fun computerChoosesItsWinBeforeBlockingAWinWithALowerIndex() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.X, 1, 2)
        place(game, Player.O, 3, 5)
        val before = game.board

        assertEquals(ComputerMoveResult.Selected(4), game.getComputerMove())
        assertEquals(0, randomizer.calls)
        assertEquals(before, game.board)
    }

    @Test
    fun computerChoosesTheFirstImmediateWinInBoardOrder() {
        val firstGame = TicTacToeGame(RecordingRandomizer())
        place(firstGame, Player.O, 1, 2, 4, 5)
        val firstBefore = firstGame.board

        assertEquals(ComputerMoveResult.Selected(0), firstGame.getComputerMove())
        assertEquals(firstBefore, firstGame.board)

        val laterGame = TicTacToeGame(RecordingRandomizer())
        place(laterGame, Player.O, 0, 4)
        place(laterGame, Player.X, 1, 3)
        val laterBefore = laterGame.board

        assertEquals(ComputerMoveResult.Selected(8), laterGame.getComputerMove())
        assertEquals(laterBefore, laterGame.board)
    }

    @Test
    fun computerBlocksTheFirstHumanThreatWhenItCannotWin() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.X, 1, 2)
        place(game, Player.O, 4)
        val before = game.board

        assertEquals(ComputerMoveResult.Selected(0), game.getComputerMove())
        assertEquals(0, randomizer.calls)
        assertEquals(before, game.board)
    }

    @Test
    fun computerBlocksTheFirstOfMultipleHumanThreats() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.X, 0, 1, 3, 4)
        place(game, Player.O, 6)
        val before = game.board

        assertEquals(ComputerMoveResult.Selected(2), game.getComputerMove())
        assertEquals(0, randomizer.calls)
        assertEquals(before, game.board)
    }

    @Test
    fun randomSelectionUsesEachControlledFreeListIndexWithoutMutatingTheBoard() {
        val occupiedLocations = listOf(0, 4)
        val freeLocations = (0..8).filter { it !in occupiedLocations }

        for (randomIndex in freeLocations.indices) {
            val randomizer = RecordingRandomizer(randomIndex)
            val game = TicTacToeGame(randomizer)
            place(game, Player.X, 0)
            place(game, Player.O, 4)
            val before = game.board

            val result = game.getComputerMove()

            assertEquals(ComputerMoveResult.Selected(freeLocations[randomIndex]), result)
            assertEquals(listOf(freeLocations.size), randomizer.bounds)
            assertEquals(Mark.Empty, game.board[freeLocations[randomIndex]])
            assertEquals(before, game.board)
        }
    }

    @Test
    fun terminalWinnerWithFreeCellsDoesNotProbeOrInvokeRandomizer() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        place(game, Player.X, 0, 1, 2)
        val before = game.board

        assertEquals(ComputerMoveResult.GameAlreadyFinished, game.getComputerMove())
        assertEquals(0, randomizer.calls)
        assertEquals(before, game.board)
    }

    @Test
    fun fullWinningBoardIsFinishedBeforeNoMovesAndDoesNotInvokeRandomizer() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        val firstEightMoves = listOf(
            Player.X, Player.O, Player.O,
            Player.O, Player.X, Player.X,
            Player.X, Player.O,
        )
        for ((location, player) in firstEightMoves.withIndex()) {
            assertEquals(MoveResult.Applied, game.setMove(player, location))
        }
        assertEquals(MoveResult.Applied, game.setMove(Player.X, 8))
        assertEquals(GameResult.Won(Player.X), game.checkForWinner())
        val before = game.board

        assertEquals(ComputerMoveResult.GameAlreadyFinished, game.getComputerMove())
        assertEquals(0, randomizer.calls)
        assertEquals(before, game.board)
    }

    @Test
    fun fullTieReturnsNoMovesWithoutProbingOrInvokingRandomizer() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)
        val tieMoves = listOf(
            Player.X, Player.O, Player.X,
            Player.X, Player.O, Player.O,
            Player.O, Player.X, Player.X,
        )
        for ((location, player) in tieMoves.withIndex()) {
            assertEquals(MoveResult.Applied, game.setMove(player, location))
        }
        assertEquals(GameResult.Tie, game.checkForWinner())
        val before = game.board

        assertEquals(ComputerMoveResult.NoMoves, game.getComputerMove())
        assertEquals(0, randomizer.calls)
        assertEquals(before, game.board)
    }

    @Test
    fun selectedMovesAreValidFreeLocationsForEmptyOccupiedAndLastFreeBoards() {
        val emptyRandomizer = RecordingRandomizer(8)
        val emptyGame = TicTacToeGame(emptyRandomizer)
        assertSelectedFree(emptyGame, 8)
        assertEquals(listOf(9), emptyRandomizer.bounds)

        val occupiedRandomizer = RecordingRandomizer(1)
        val occupiedGame = TicTacToeGame(occupiedRandomizer)
        place(occupiedGame, Player.X, 0)
        place(occupiedGame, Player.O, 4)
        val occupiedFreeLocations = (0..8).filter { occupiedGame.board[it] == Mark.Empty }
        assertSelectedFree(occupiedGame, occupiedFreeLocations[1])
        assertEquals(listOf(occupiedFreeLocations.size), occupiedRandomizer.bounds)

        val lastFreeRandomizer = RecordingRandomizer(0)
        val lastFreeGame = TicTacToeGame(lastFreeRandomizer)
        val firstEightMoves = listOf(
            Player.X, Player.O, Player.X,
            Player.X, Player.O, Player.O,
            Player.O, Player.X,
        )
        for ((location, player) in firstEightMoves.withIndex()) {
            assertEquals(MoveResult.Applied, lastFreeGame.setMove(player, location))
        }
        assertEquals(GameResult.InProgress, lastFreeGame.checkForWinner())
        assertSelectedFree(lastFreeGame, 8)
        assertEquals(listOf(1), lastFreeRandomizer.bounds)
    }

    @Test
    fun omittedDifficultyMatchesExpertAndRepeatedQueriesDoNotMutateTheBoard() {
        val winGame = TicTacToeGame(RecordingRandomizer())
        place(winGame, Player.X, 1, 2)
        place(winGame, Player.O, 3, 5)
        val winBefore = winGame.board

        val explicitWin = winGame.getComputerMove(DifficultyLevel.Expert)
        assertEquals(ComputerMoveResult.Selected(4), explicitWin)
        assertEquals(explicitWin, winGame.getComputerMove())
        assertEquals(explicitWin, winGame.getComputerMove())
        assertEquals(winBefore, winGame.board)

        val blockGame = TicTacToeGame(RecordingRandomizer())
        place(blockGame, Player.X, 1, 2)
        place(blockGame, Player.O, 4)
        val blockBefore = blockGame.board

        val explicitBlock = blockGame.getComputerMove(DifficultyLevel.Expert)
        assertEquals(ComputerMoveResult.Selected(0), explicitBlock)
        assertEquals(explicitBlock, blockGame.getComputerMove())
        assertEquals(blockBefore, blockGame.board)
    }

    private fun assertSelectedFree(game: TicTacToeGame, expectedLocation: Int) {
        val before = game.board
        val result = game.getComputerMove()

        assertEquals(ComputerMoveResult.Selected(expectedLocation), result)
        val selectedLocation = (result as ComputerMoveResult.Selected).index
        assertTrue(selectedLocation in 0..8)
        assertEquals(Mark.Empty, before[selectedLocation])
        assertEquals(before, game.board)
    }

    private fun place(game: TicTacToeGame, player: Player, vararg locations: Int) {
        for (location in locations) {
            assertEquals(MoveResult.Applied, game.setMove(player, location))
        }
    }

    @Test
    fun restoreBoardAcceptsLegalBoards() {
        val legal = listOf(
            "........." to GameResult.InProgress,
            "XO.X....." to GameResult.InProgress,
            "OX.O....." to GameResult.InProgress,
            "XO.XO...." to GameResult.InProgress,
            "XXXOO...." to GameResult.Won(Player.X),
            "XXXOO.O.." to GameResult.Won(Player.X),
            "OOOXX...." to GameResult.Won(Player.O),
            "OOOXX.X.." to GameResult.Won(Player.O),
            "XOXXOOOXX" to GameResult.Tie,
            "XXXXOOXOO" to GameResult.Won(Player.X),
        )
        for ((layout, expected) in legal) {
            val game = TicTacToeGame()
            val marks = boardOf(layout)

            assertTrue(layout, game.restoreBoard(marks))
            assertEquals(layout, marks, game.board)
            assertEquals(layout, expected, game.checkForWinner())
        }
    }

    @Test
    fun restoreBoardRejectsIllegalBoardsAndLeavesAnEmptyBoardUntouched() {
        val illegal = listOf(
            "XX.......", // X leads by two
            "OO.......", // O leads by two
            "XXX......", // X leads by three
            "XXXOOO...", // both players have a line
            "XXXOO..OO", // X wins with fewer marks than O
            "OOOXX..XX", // O wins with fewer marks than X
        )
        for (layout in illegal) {
            val game = TicTacToeGame()

            assertFalse(layout, game.restoreBoard(boardOf(layout)))
            assertEquals(layout, List(9) { Mark.Empty }, game.board)
        }
    }

    @Test
    fun restoreBoardRejectsWrongSizes() {
        for (size in listOf(0, 8, 10)) {
            assertFalse(size.toString(), TicTacToeGame().restoreBoard(List(size) { Mark.Empty }))
        }
    }

    @Test
    fun rejectedRestoreKeepsThePreviousNonEmptyBoard() {
        val game = TicTacToeGame()
        val previous = boardOf("XO.X.....")
        assertTrue(game.restoreBoard(previous))

        assertFalse(game.restoreBoard(boardOf("XXXOOO...")))
        assertEquals(previous, game.board)
        assertFalse(game.restoreBoard(List(8) { Mark.Empty }))
        assertEquals(previous, game.board)
        assertFalse(game.restoreBoard(boardOf("XX.......")))
        assertEquals(previous, game.board)
    }

    @Test
    fun restoreBoardNeverUsesTheRandomizer() {
        val randomizer = RecordingRandomizer()
        val game = TicTacToeGame(randomizer)

        assertTrue(game.restoreBoard(boardOf("XO.X.....")))
        assertTrue(game.restoreBoard(boardOf("XOXXOOOXX")))
        assertFalse(game.restoreBoard(boardOf("XXXOOO...")))
        assertFalse(game.restoreBoard(List(8) { Mark.Empty }))

        assertEquals(0, randomizer.calls)
    }

    @Test
    fun restoreBoardCopiesTheInputDefensively() {
        val game = TicTacToeGame()
        val input = boardOf("XO.X.....").toMutableList()
        assertTrue(game.restoreBoard(input))

        input[2] = Mark.O
        input[0] = Mark.Empty

        assertEquals(boardOf("XO.X....."), game.board)
    }

    @Test
    fun restoredActiveBoardContinuesNormally() {
        val game = TicTacToeGame()
        assertTrue(game.restoreBoard(boardOf("XO.X.....")))

        assertEquals(GameResult.InProgress, game.checkForWinner())
        assertEquals(MoveResult.Occupied, game.setMove(Player.O, 0))
        assertEquals(MoveResult.Applied, game.setMove(Player.O, 4))
        assertEquals(Mark.O, game.board[4])
    }

    @Test
    fun restoredActiveBoardLetsTheComputerChooseFromThatPosition() {
        val game = TicTacToeGame(RecordingRandomizer())
        assertTrue(game.restoreBoard(boardOf("XX.OO....")))

        assertEquals(ComputerMoveResult.Selected(5), game.getComputerMove(DifficultyLevel.Expert))
    }

    @Test
    fun restoredTerminalBoardsRejectFurtherPlay() {
        val won = TicTacToeGame()
        assertTrue(won.restoreBoard(boardOf("XXXOO....")))
        assertEquals(GameResult.Won(Player.X), won.checkForWinner())
        assertEquals(MoveResult.GameAlreadyFinished, won.setMove(Player.O, 5))
        assertEquals(ComputerMoveResult.GameAlreadyFinished, won.getComputerMove())

        val tied = TicTacToeGame()
        assertTrue(tied.restoreBoard(boardOf("XOXXOOOXX")))
        assertEquals(GameResult.Tie, tied.checkForWinner())
        assertEquals(MoveResult.GameAlreadyFinished, tied.setMove(Player.O, 0))
        assertEquals(ComputerMoveResult.NoMoves, tied.getComputerMove())
    }

    @Test
    fun clearBoardAfterRestoreLeavesAnEmptyBoard() {
        val game = TicTacToeGame()
        assertTrue(game.restoreBoard(boardOf("XXXOO....")))

        game.clearBoard()

        assertEquals(List(9) { Mark.Empty }, game.board)
    }

    private fun boardOf(layout: String): List<Mark> = layout.map {
        when (it) {
            'X' -> Mark.X
            'O' -> Mark.O
            else -> Mark.Empty
        }
    }

    private class RecordingRandomizer(
        private val selectedIndex: Int = 0,
    ) : MoveRandomizer {
        var calls: Int = 0
            private set
        val bounds = mutableListOf<Int>()

        override fun nextInt(bound: Int): Int {
            calls++
            bounds += bound
            return selectedIndex
        }
    }

    private companion object {
        val winningLines = listOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6),
        )
    }
}
