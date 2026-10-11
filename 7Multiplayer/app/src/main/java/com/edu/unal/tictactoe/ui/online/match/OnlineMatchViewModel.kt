package com.edu.unal.tictactoe.ui.online.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.unal.tictactoe.data.online.MoveOutcome
import com.edu.unal.tictactoe.data.online.OnlineGame
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.data.online.OnlineServiceException
import com.edu.unal.tictactoe.data.online.OnlineStatus
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single Activity-scoped instance behind the online match, reused game after game. It contains no
 * UI text, no score and no preferences: [uiState] only describes the observed game from the point
 * of view of the person holding the device.
 *
 * [openMatch] discards the whole state of any previous match, cancels its observations (and any
 * move still awaiting an answer) and observes the new game and the connection.
 *
 * Marks are never painted optimistically: the board shows exactly what the service last
 * published. [play] sends a move only while [OnlineMatchUiState.isInputEnabled] and the cell is
 * free, and the move stays pending until the service answered and, if it accepted, the mark has
 * arrived in the observed game; the answer and the observed game may arrive in either order. A
 * [MoveOutcome.Rejected] answer or an [OnlineServiceException] leaves board and turn untouched and
 * lets the person try again.
 *
 * A null game value means the game no longer exists: unless it was already seen finished (a final
 * result is sticky) the match ends, including when the very first value is null. It becomes
 * [MatchPhase.ConnectionLost] when this device has no connection at that moment (the service
 * reports the lost connection before the game disappears from the local copy) and
 * [MatchPhase.Abandoned] otherwise. Both endings are sticky: no later game value or connection
 * change alters them, and no move is emitted afterwards. [leave] works in any phase and clears
 * everything; the confirmation dialog belongs to the screen. While the game goes on, the
 * connection only gates the input and the "Sin conexión" notice, and recovers by itself.
 *
 * [acceptedMoves] emits the owner of a mark each time the observed board gains exactly one mark
 * over the previously known one, never for the first game value after [openMatch]. It is built so
 * that emitting never suspends and nothing is kept when nobody collects.
 */
class OnlineMatchViewModel(
    private val service: OnlineGameService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnlineMatchUiState())

    val uiState: StateFlow<OnlineMatchUiState> = _uiState.asStateFlow()

    private val _acceptedMoves = MutableSharedFlow<Player>(
        replay = 0,
        extraBufferCapacity = MOVES_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Owner of each newly confirmed mark, for the move sound; values emitted without a collector are dropped. */
    val acceptedMoves: SharedFlow<Player> = _acceptedMoves.asSharedFlow()

    private var gameId: String? = null

    /** Board of the last game value seen in this match, or null before the first one. */
    private var knownBoard: List<Mark>? = null
    private var observationJob: Job? = null
    private var moveJob: Job? = null

    /** Cell of the move sent in the current match whose answer is still awaited, if any. */
    private var requestedCell: Int? = null

    /** Cell of an accepted move whose mark has not yet arrived in the observed game, if any. */
    private var unconfirmedCell: Int? = null

    /** Starts a new match: [myMark] is X for the person who created [gameId] and O for the one who joined. */
    fun openMatch(gameId: String, myMark: Player) {
        observationJob?.cancel()
        moveJob?.cancel()
        this.gameId = gameId
        requestedCell = null
        unconfirmedCell = null
        knownBoard = null
        _uiState.value = OnlineMatchUiState(myMark = myMark)
        observationJob = viewModelScope.launch(dispatcher) {
            launch { service.observeConnection().collect { online -> _uiState.update { it.copy(isConnected = online) } } }
            launch { service.observeGame(gameId).collect { game -> if (game == null) onGameVanished() else onGameObserved(game, myMark) } }
        }
    }

    /** Leaves the match from any phase: asks the service to delete the game, stops observing and clears the state. */
    fun leave() {
        val id = gameId ?: return
        observationJob?.cancel()
        moveJob?.cancel()
        gameId = null
        requestedCell = null
        unconfirmedCell = null
        knownBoard = null
        _uiState.value = OnlineMatchUiState()
        viewModelScope.launch(dispatcher) {
            try {
                service.leaveGame(id)
            } catch (_: OnlineServiceException) {
                // The person is already leaving; the service removes the game when this client drops.
            }
        }
    }

    /** Sends a move to [cell] when the input is enabled and the cell is free; ignored otherwise. */
    fun play(cell: Int) {
        val state = _uiState.value
        val id = gameId ?: return
        if (!state.isInputEnabled || state.board.getOrNull(cell) != Mark.Empty) return
        requestedCell = cell
        publishPending()
        moveJob = viewModelScope.launch(dispatcher) {
            val outcome = try {
                service.playMove(id, cell)
            } catch (_: OnlineServiceException) {
                MoveOutcome.Rejected
            }
            requestedCell = null
            val state = _uiState.value
            unconfirmedCell = cell.takeIf {
                outcome == MoveOutcome.Accepted && state.phase == MatchPhase.Playing && state.board[it] == Mark.Empty
            }
            publishPending()
        }
    }

    private fun onGameVanished() {
        val current = _uiState.value
        if (current.phase == MatchPhase.Finished || current.phase.isEnding()) return
        requestedCell = null
        unconfirmedCell = null
        val ending = if (current.isConnected) MatchPhase.Abandoned else MatchPhase.ConnectionLost
        _uiState.update { it.copy(phase = ending, isMyTurn = false, result = null, isMovePending = false) }
    }

    private fun MatchPhase.isEnding() = this == MatchPhase.Abandoned || this == MatchPhase.ConnectionLost

    private fun onGameObserved(game: OnlineGame, myMark: Player) {
        if (_uiState.value.phase.isEnding()) return
        emitAcceptedMove(knownBoard, game.board)
        knownBoard = game.board
        val opponent = if (myMark == Player.X) game.guest else game.host
        val phase = when (game.status) {
            OnlineStatus.Waiting -> MatchPhase.Loading
            OnlineStatus.Playing -> MatchPhase.Playing
            OnlineStatus.Finished -> MatchPhase.Finished
        }
        unconfirmedCell?.let { cell ->
            if (game.board[cell] != Mark.Empty || phase != MatchPhase.Playing) unconfirmedCell = null
        }
        _uiState.update {
            it.copy(
                opponentName = opponent?.name,
                board = game.board,
                phase = phase,
                isMyTurn = game.turn == myMark,
                result = if (phase == MatchPhase.Finished) game.result.fromPointOfView(myMark) else null,
                isMovePending = requestedCell != null || unconfirmedCell != null,
            )
        }
    }

    private fun emitAcceptedMove(previous: List<Mark>?, current: List<Mark>) {
        if (previous == null || current.count { it != Mark.Empty } != previous.count { it != Mark.Empty } + 1) return
        val index = current.indices.firstOrNull { previous[it] == Mark.Empty && current[it] != Mark.Empty } ?: return
        _acceptedMoves.tryEmit(if (current[index] == Mark.X) Player.X else Player.O)
    }

    private fun publishPending() {
        _uiState.update { it.copy(isMovePending = requestedCell != null || unconfirmedCell != null) }
    }

    private fun GameResult.fromPointOfView(myMark: Player): MatchResult? = when (this) {
        GameResult.InProgress -> null
        GameResult.Tie -> MatchResult.Tie
        is GameResult.Won -> if (player == myMark) MatchResult.Won else MatchResult.Lost
    }

    private companion object {
        const val MOVES_BUFFER = 8
    }
}
