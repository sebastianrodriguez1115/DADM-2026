package com.edu.unal.tictactoe.game.models

sealed interface ComputerMoveResult {
    data class Selected(val index: Int) : ComputerMoveResult

    data object NoMoves : ComputerMoveResult

    data object GameAlreadyFinished : ComputerMoveResult
}
