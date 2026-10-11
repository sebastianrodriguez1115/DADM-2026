package com.edu.unal.tictactoe.ui.online.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.unal.tictactoe.data.online.JoinResult
import com.edu.unal.tictactoe.data.online.NicknameStore
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.data.online.OnlineServiceException
import com.edu.unal.tictactoe.data.online.OnlineStatus
import com.edu.unal.tictactoe.game.models.Player
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single Activity-scoped instance behind the online lobby, reused every time the person enters
 * it. It holds the nickname, the anonymous identity and the connection; it contains no UI text.
 *
 * Entering ([onLobbyEntered], idempotent) reads the stored nickname and, only if the identity was
 * never established, tries it. Once established the identity is kept and the connection observation
 * keeps running, so a later entry shows the current connection without repeating anything.
 * The state stays [LobbyConnection.Connecting] after the identity until the first connection is
 * seen, up to [CONNECTION_TIMEOUT_MILLIS]; from the first connection on, every change of the
 * connection is reflected immediately.
 *
 * The available games are a separate [games] flow, observed only while someone collects it and,
 * even then, only once the identity is established and a nickname is stored (the real service
 * rejects listening before signing in). Creating or joining runs one operation at a time. The own
 * waiting game is observed until it starts, vanishes or is cancelled; cancelling stops that
 * observation first, so its own removal is never mistaken for an external deletion.
 *
 * One-shot signals live in [OnlineLobbyUiState.signals] rather than in an event stream: they
 * survive rotation and are removed atomically by [consumeSignal], so each is handled once.
 */
class OnlineLobbyViewModel(
    private val service: OnlineGameService,
    private val nicknameStore: NicknameStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnlineLobbyUiState())

    val uiState: StateFlow<OnlineLobbyUiState> = _uiState.asStateFlow()

    private val identityReady = MutableStateFlow(false)
    private var identityEstablished
        get() = identityReady.value
        set(value) {
            identityReady.value = value
        }
    private var connectionJob: Job? = null
    private var waitingJob: Job? = null
    private var operationInProgress = false

    /** Games waiting for a rival, newest first; empty until identity and nickname exist. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val games: StateFlow<List<LobbyGame>> = combine(
        identityReady,
        _uiState.map { it.nickname != null }.distinctUntilChanged(),
    ) { identified, hasNickname -> identified && hasNickname }
        .distinctUntilChanged()
        .flatMapLatest { listening ->
            if (listening) {
                service.observeAvailableGames().map { available ->
                    available.sortedByDescending { it.createdAt }.map { LobbyGame(it.id, it.host.name) }
                }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(GAMES_STOP_TIMEOUT_MILLIS), emptyList())

    /**
     * Called by the navigation layer each time the lobby enters composition, rotation included,
     * so it is idempotent: it refreshes the stored nickname, keeps any open dialog (and its
     * rejection mark) and any pending signal, opens the first-time dialog only when there is no
     * nickname and no dialog, and never repeats an established identity or an attempt in progress.
     */
    fun onLobbyEntered() {
        val stored = nicknameStore.readNickname()
        _uiState.update {
            it.copy(
                nickname = stored,
                nicknameDialog = it.nicknameDialog
                    ?: if (stored == null) NicknameDialogState(isFirstTime = true) else null,
            )
        }
        if (!identityEstablished && connectionJob?.isActive != true) {
            establishIdentity()
        }
    }

    /** Repeats the identity attempt after it failed; ignored in any other state. */
    fun onRetryConnection() {
        if (_uiState.value.connection == LobbyConnection.IdentityFailed) {
            establishIdentity()
        }
    }

    /** Validates [text]; a valid nickname is stored trimmed and closes the dialog. */
    fun onNicknameSubmitted(text: String) {
        val dialog = _uiState.value.nicknameDialog ?: return
        when (val validation = NicknameValidator.validate(text)) {
            is NicknameValidation.Valid -> {
                nicknameStore.writeNickname(validation.nickname)
                _uiState.update { it.copy(nickname = validation.nickname, nicknameDialog = null) }
            }
            NicknameValidation.Rejected ->
                _uiState.update { it.copy(nicknameDialog = dialog.copy(isRejected = true)) }
        }
    }

    /** Dismisses the dialog; the first-time one also signals [LobbySignal.BackToStart] and clears its rejection mark. */
    fun onNicknameDialogDismissed() {
        val dialog = _uiState.value.nicknameDialog ?: return
        _uiState.update {
            if (dialog.isFirstTime) {
                it.copy(
                    nicknameDialog = dialog.copy(isRejected = false),
                    signals = it.signals + LobbySignal.BackToStart,
                )
            } else {
                it.copy(nicknameDialog = null)
            }
        }
    }

    /** Opens the nickname dialog for the current nickname; ignored while none is stored. */
    fun onChangeNicknameRequested() {
        _uiState.update {
            if (it.nickname == null || it.nicknameDialog != null) {
                it
            } else {
                it.copy(nicknameDialog = NicknameDialogState(isFirstTime = false))
            }
        }
    }

    /** Creates a game for the current nickname and starts waiting for a rival. */
    fun onCreateGame() {
        val nickname = _uiState.value.nickname
        if (!canStartOperation() || nickname == null) return
        operationInProgress = true
        viewModelScope.launch(dispatcher) {
            val gameId = try {
                service.createGame(nickname)
            } catch (_: OnlineServiceException) {
                operationInProgress = false
                emit(LobbySignal.CreateFailed)
                return@launch
            }
            operationInProgress = false
            _uiState.update { it.copy(waitingGameId = gameId) }
            waitingJob = launch { awaitOpponent(gameId) }
        }
    }

    /** Withdraws the own waiting game and returns to the lobby, with no message. */
    fun onCancelWaiting() {
        val gameId = _uiState.value.waitingGameId ?: return
        waitingJob?.cancel()
        waitingJob = null
        _uiState.update { it.copy(waitingGameId = null) }
        viewModelScope.launch(dispatcher) {
            try {
                service.cancelGame(gameId)
            } catch (_: OnlineServiceException) {
                // The person already left the waiting view; a game the service could not remove
                // is cleaned up by the service itself when this client is seen as disconnected.
            }
        }
    }

    /** Joins the listed game [gameId] as the second player. */
    fun onJoinGame(gameId: String) {
        val nickname = _uiState.value.nickname
        if (!canStartOperation() || nickname == null) return
        operationInProgress = true
        viewModelScope.launch(dispatcher) {
            val result = try {
                service.joinGame(gameId, nickname)
            } catch (_: OnlineServiceException) {
                JoinResult.NoLongerAvailable
            }
            operationInProgress = false
            emit(
                when (result) {
                    JoinResult.Joined -> LobbySignal.MatchReady(MatchTicket(gameId, Player.O))
                    JoinResult.NoLongerAvailable -> LobbySignal.GameUnavailable
                },
            )
        }
    }

    private fun canStartOperation(): Boolean =
        _uiState.value.let { it.actionsEnabled && it.waitingGameId == null } && !operationInProgress

    private suspend fun awaitOpponent(gameId: String) {
        val game = service.observeGame(gameId).first { it == null || it.status != OnlineStatus.Waiting }
        _uiState.update { it.copy(waitingGameId = null) }
        emit(
            if (game == null) {
                LobbySignal.GameUnavailable
            } else {
                LobbySignal.MatchReady(MatchTicket(gameId, Player.X))
            },
        )
    }

    private fun emit(signal: LobbySignal) {
        _uiState.update { it.copy(signals = it.signals + signal) }
    }

    /** Removes one pending occurrence of [signal]; later calls for the same signal do nothing. */
    fun consumeSignal(signal: LobbySignal) {
        _uiState.update { state ->
            val index = state.signals.indexOf(signal)
            if (index < 0) state else state.copy(signals = state.signals.filterIndexed { i, _ -> i != index })
        }
    }

    private fun establishIdentity() {
        _uiState.update { it.copy(connection = LobbyConnection.Connecting) }
        connectionJob = viewModelScope.launch(dispatcher) {
            try {
                service.ensureSignedIn()
            } catch (_: OnlineServiceException) {
                _uiState.update { it.copy(connection = LobbyConnection.IdentityFailed) }
                return@launch
            }
            identityEstablished = true
            val timeout = launch {
                delay(CONNECTION_TIMEOUT_MILLIS)
                _uiState.update {
                    if (it.connection == LobbyConnection.Connecting) {
                        it.copy(connection = LobbyConnection.Offline)
                    } else {
                        it
                    }
                }
            }
            var hasConnected = false
            service.observeConnection().collect { online ->
                if (online) {
                    hasConnected = true
                    timeout.cancel()
                    _uiState.update { it.copy(connection = LobbyConnection.Connected) }
                } else if (hasConnected) {
                    _uiState.update { it.copy(connection = LobbyConnection.Offline) }
                }
            }
        }
    }

    companion object {
        /** How long to wait for the first connection after the identity before showing offline. */
        const val CONNECTION_TIMEOUT_MILLIS = 5_000L

        private const val GAMES_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
