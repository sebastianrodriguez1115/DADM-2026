package com.edu.unal.tictactoe.game.models

sealed interface MoveResult {
    data object Applied : MoveResult

    data object InvalidLocation : MoveResult

    data object Occupied : MoveResult

    data object GameAlreadyFinished : MoveResult
}
