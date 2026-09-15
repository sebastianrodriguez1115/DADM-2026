package com.edu.unal.tictactoe.game.models

sealed interface GameResult {
    data object InProgress : GameResult

    data object Tie : GameResult

    data class Won(val player: Player) : GameResult
}
