package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.unal.tictactoe.game.TicTacToeGame
import com.edu.unal.tictactoe.game.models.ComputerMoveResult
import com.edu.unal.tictactoe.game.models.DifficultyLevel
import com.edu.unal.tictactoe.game.models.GameResult
import com.edu.unal.tictactoe.game.models.MoveResult
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.game.models.Score
import com.edu.unal.tictactoe.game.models.Turn
import com.edu.unal.tictactoe.ui.game.audio.MoveSoundSink
import com.edu.unal.tictactoe.ui.game.models.GameUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class GameViewModel(
    private val game: TicTacToeGame = TicTacToeGame(),
    private val monotonicClock: MonotonicClock = MonotonicClock { System.nanoTime() },
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private var isForeground = false
    private var generation = 0L
    private var pendingComputerTurn: PendingComputerTurn? = null
    private var soundOwner: Any? = null
    private var soundSink = WeakReference<MoveSoundSink>(null)

    private val _uiState: MutableStateFlow<GameUiState>

    val uiState: StateFlow<GameUiState>

    init {
        game.clearBoard()
        _uiState = MutableStateFlow(
            GameUiState(
                board = game.board,
                turn = Turn.Human,
                result = GameResult.InProgress,
                starter = Player.X,
                nextStarter = Player.O,
                score = Score(),
            ),
        )
        uiState = _uiState.asStateFlow()
    }

    fun onHumanMove(location: Int) {
        val currentState = _uiState.value
        if (currentState.result != GameResult.InProgress || currentState.turn != Turn.Human) {
            return
        }

        if (game.setMove(Player.X, location) != MoveResult.Applied) {
            return
        }

        val resultAfterHuman = game.checkForWinner()
        if (resultAfterHuman != GameResult.InProgress) {
            publishState(turn = Turn.Human, result = resultAfterHuman)
            emitMoveSound(Player.X)
            return
        }

        publishState(turn = Turn.Computer, result = GameResult.InProgress)
        startPendingComputerTurn()
        emitMoveSound(Player.X)
    }

    /** Registers an ephemeral sink; the owner token must not itself retain its host. */
    fun registerMoveSoundSink(owner: Any, sink: MoveSoundSink) {
        val previousOwner = soundOwner
        val previousSink = soundSink.get()
        soundOwner = owner
        soundSink = WeakReference(sink)
        if (previousOwner !== owner || previousSink !== sink) stopSinkSafely(previousSink)
    }

    /** An obsolete host cannot unregister a newer host's sink. */
    fun unregisterMoveSoundSink(owner: Any) {
        if (soundOwner === owner) clearMoveSoundSink()
    }

    /** Synchronous identity query for a host-owned controller; does not retain host state. */
    fun isMoveSoundOwnerCurrent(owner: Any): Boolean = soundOwner === owner

    private fun startPendingComputerTurn() {
        val pending = PendingComputerTurn(
            generation = generation,
            remainingNanos = COMPUTER_TURN_DELAY_NANOS,
        )
        pendingComputerTurn = pending
        if (isForeground) startPendingClock(pending)
    }

    /** Marks the host's RESUMED boundary. Repeated callbacks do not restart the clock. */
    fun onHostResumed() {
        if (isForeground) return
        isForeground = true
        pendingComputerTurn?.let(::startPendingClock)
    }

    /** Suspends only the executable job, retaining the logical turn and its remaining time. */
    fun onHostPaused() {
        if (!isForeground) return
        isForeground = false
        pendingComputerTurn?.let { pending ->
            pending.startedAtNanos?.let { startedAt ->
                val elapsed = (monotonicClock.nowNanos() - startedAt).coerceAtLeast(0L)
                pending.remainingNanos = (pending.remainingNanos - elapsed).coerceAtLeast(0L)
            }
            pending.startedAtNanos = null
            pending.job?.cancel()
            pending.job = null
        }
        clearMoveSoundSink()
    }

    private fun startPendingClock(pending: PendingComputerTurn) {
        if (!isForeground || pendingComputerTurn !== pending || pending.startedAtNanos != null) return
        pending.startedAtNanos = monotonicClock.nowNanos()
        scheduleComputerTurn(pending)
    }

    fun newGame() {
        generation++
        cancelPendingComputerTurn()
        val currentState = _uiState.value
        val starter = currentState.nextStarter
        val nextStarter = otherPlayer(starter)
        game.clearBoard()

        if (starter == Player.O) {
            publishState(
                turn = Turn.Computer,
                result = GameResult.InProgress,
                starter = starter,
                nextStarter = nextStarter,
            )
            startPendingComputerTurn()
        } else {
            publishState(
                turn = Turn.Human,
                result = GameResult.InProgress,
                starter = starter,
                nextStarter = nextStarter,
            )
        }
    }

    fun setDifficulty(level: DifficultyLevel): Boolean {
        val currentState = _uiState.value
        if (!currentState.canChangeDifficulty) {
            return false
        }

        _uiState.value = GameUiState(
            board = currentState.board,
            turn = currentState.turn,
            result = currentState.result,
            starter = currentState.starter,
            nextStarter = currentState.nextStarter,
            score = currentState.score,
            difficulty = level,
            soundEnabled = currentState.soundEnabled,
        )
        return true
    }

    fun toggleSound() {
        val currentState = _uiState.value
        _uiState.value = GameUiState(
            board = currentState.board,
            turn = currentState.turn,
            result = currentState.result,
            starter = currentState.starter,
            nextStarter = currentState.nextStarter,
            score = currentState.score,
            difficulty = currentState.difficulty,
            soundEnabled = !currentState.soundEnabled,
        )
        if (currentState.soundEnabled) stopSoundSink()
    }

    private fun playComputerTurn(pending: PendingComputerTurn) {
        while (isCurrentComputerTurn(pending)) {
            val difficulty = _uiState.value.difficulty
            val computerMove = game.getComputerMove(difficulty)

            // Selection may call injected/randomizer code. Revalidate its authority
            // before using the result, and retry if difficulty changed meanwhile.
            if (!isCurrentComputerTurn(pending)) return
            if (_uiState.value.difficulty != difficulty) continue

            when (computerMove) {
                is ComputerMoveResult.Selected -> {
                    if (!isCurrentComputerTurn(pending)) return
                    if (game.setMove(Player.O, computerMove.index) != MoveResult.Applied) return
                    pendingComputerTurn = null
                }

                ComputerMoveResult.NoMoves,
                ComputerMoveResult.GameAlreadyFinished,
                -> {
                    pendingComputerTurn = null
                }
            }

            val resultAfterComputer = game.checkForWinner()
            val nextTurn = if (resultAfterComputer == GameResult.InProgress) Turn.Human else Turn.Computer
            publishState(turn = nextTurn, result = resultAfterComputer)
            if (computerMove is ComputerMoveResult.Selected) emitMoveSound(Player.O)
            return
        }
    }

    private fun isCurrentComputerTurn(pending: PendingComputerTurn): Boolean {
        val state = _uiState.value
        return pendingComputerTurn === pending && pending.generation == generation &&
            isForeground && state.turn == Turn.Computer && state.result == GameResult.InProgress
    }

    private fun scheduleComputerTurn(pending: PendingComputerTurn) {
        pending.job = viewModelScope.launch(mainDispatcher) {
            try {
                while (true) {
                    if (!isCurrentComputerTurn(pending)) return@launch
                    val startedAt = pending.startedAtNanos ?: return@launch
                    val elapsed = (monotonicClock.nowNanos() - startedAt).coerceAtLeast(0L)
                    val remaining = (pending.remainingNanos - elapsed).coerceAtLeast(0L)
                    if (remaining > 0L) delay(ceilMillis(remaining))
                    if (!isCurrentComputerTurn(pending)) return@launch

                    val elapsedAtWake =
                        (monotonicClock.nowNanos() - (pending.startedAtNanos ?: return@launch)).coerceAtLeast(0L)
                    if (elapsedAtWake < pending.remainingNanos) continue

                    playComputerTurn(pending)
                    return@launch
                }
            } finally {
                if (pendingComputerTurn === pending && pending.job === currentCoroutineContext()[Job]) {
                    pending.job = null
                }
            }
        }
    }

    private fun cancelPendingComputerTurn() {
        pendingComputerTurn?.job?.cancel()
        pendingComputerTurn = null
    }

    private fun emitMoveSound(player: Player) {
        if (!isForeground || !_uiState.value.soundEnabled || soundOwner == null) return
        val sink = soundSink.get() ?: return
        try {
            sink.play(player)
        } catch (_: Exception) {
            // Audio is best-effort and must not interfere with game state/turn scheduling.
        }
    }

    private fun stopSoundSink() {
        stopSinkSafely(soundSink.get())
    }

    private fun stopSinkSafely(sink: MoveSoundSink?) {
        sink ?: return
        try {
            sink.stop()
        } catch (_: Exception) {
            // Audio failures are isolated from game state.
        }
    }

    private fun clearMoveSoundSink() {
        val oldSink = soundSink.get()
        soundSink.clear()
        soundOwner = null
        stopSinkSafely(oldSink)
    }

    private fun ceilMillis(nanos: Long): Long =
        ((nanos - 1L) / NANOS_PER_MILLISECOND + 1L).coerceAtLeast(1L)

    private class PendingComputerTurn(
        val generation: Long,
        var remainingNanos: Long,
        var startedAtNanos: Long? = null,
        var job: Job? = null,
    )

    private fun otherPlayer(player: Player): Player = when (player) {
        Player.X -> Player.O
        Player.O -> Player.X
    }

    private fun publishState(turn: Turn, result: GameResult) {
        val currentState = _uiState.value
        publishState(
            turn = turn,
            result = result,
            starter = currentState.starter,
            nextStarter = currentState.nextStarter,
        )
    }

    private fun publishState(
        turn: Turn,
        result: GameResult,
        starter: Player,
        nextStarter: Player,
    ) {
        val currentState = _uiState.value
        _uiState.value = GameUiState(
            board = game.board,
            turn = turn,
            result = result,
            starter = starter,
            nextStarter = nextStarter,
            score = scoreAfterTransition(currentState, result),
            difficulty = currentState.difficulty,
            soundEnabled = currentState.soundEnabled,
        )
    }

    private fun scoreAfterTransition(currentState: GameUiState, result: GameResult): Score {
        if (currentState.result != GameResult.InProgress || result == GameResult.InProgress) {
            return currentState.score
        }

        return when (result) {
            is GameResult.Won -> when (result.player) {
                Player.X -> currentState.score.copy(xWins = currentState.score.xWins + 1)
                Player.O -> currentState.score.copy(oWins = currentState.score.oWins + 1)
            }

            GameResult.Tie -> currentState.score.copy(ties = currentState.score.ties + 1)
            GameResult.InProgress -> currentState.score
        }
    }

    override fun onCleared() {
        generation++
        cancelPendingComputerTurn()
        isForeground = false
        clearMoveSoundSink()
    }

    private companion object {
        const val COMPUTER_TURN_DELAY_NANOS = 1_000_000_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
