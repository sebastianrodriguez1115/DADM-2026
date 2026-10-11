package com.edu.unal.tictactoe.ui.online.lobby

import com.edu.unal.tictactoe.data.online.FakeNicknameStore
import com.edu.unal.tictactoe.data.online.FakeOnlineBackend
import com.edu.unal.tictactoe.data.online.FakeOnlineGameService
import com.edu.unal.tictactoe.data.online.NicknameStore
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.ui.game.GameViewModelCoroutineTestHarness
import androidx.lifecycle.viewmodel.CreationExtras
import com.edu.unal.tictactoe.data.online.OnlineStatus
import com.edu.unal.tictactoe.game.models.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.junit.After
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineLobbyViewModelTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private val backend = FakeOnlineBackend()
    private val fakeService = backend.serviceFor("me")

    private class CountingService(
        private val delegate: FakeOnlineGameService,
    ) : OnlineGameService by delegate {
        var signInCalls = 0
            private set
        var listObservations = 0
            private set

        override fun observeAvailableGames(): Flow<List<com.edu.unal.tictactoe.data.online.OnlineGame>> {
            listObservations++
            return delegate.observeAvailableGames()
        }

        override suspend fun ensureSignedIn(): String {
            signInCalls++
            return delegate.ensureSignedIn()
        }
    }

    private val service = CountingService(fakeService)

    private fun viewModel(store: NicknameStore) =
        OnlineLobbyViewModel(service, store, harness.dispatcher)

    private val collectors = mutableListOf<Job>()

    @After
    fun stopCollectors() {
        collectors.forEach(Job::cancel)
    }

    /** Collects [OnlineLobbyViewModel.games] like a visible screen would; read it through the result. */
    private fun OnlineLobbyViewModel.collectGames(): Job {
        val job = CoroutineScope(harness.dispatcher).launch { games.collect {} }
        collectors += job
        harness.scheduler.runCurrent()
        return job
    }

    private fun gameTexts(vm: OnlineLobbyViewModel) = vm.games.value.map { it.hostNickname }

    /** A second device: its own service on the shared backend, nickname and ViewModel, already in the lobby. */
    private fun otherDevice(uid: String, nickname: String): OnlineLobbyViewModel {
        val vm = OnlineLobbyViewModel(backend.serviceFor(uid), FakeNicknameStore(nickname), harness.dispatcher)
        enter(vm)
        vm.collectGames()
        return vm
    }

    private fun readyLobby(nickname: String = "Ana"): OnlineLobbyViewModel {
        val vm = viewModel(FakeNicknameStore(nickname))
        enter(vm)
        vm.collectGames()
        return vm
    }

    private fun idle() = harness.scheduler.runCurrent()

    private fun enter(viewModel: OnlineLobbyViewModel) {
        viewModel.onLobbyEntered()
        harness.scheduler.runCurrent()
    }

    private fun OnlineLobbyViewModel.state() = uiState.value

    @Test
    fun firstTimeWithoutNicknameAsksForItAndHidesTheLobby() {
        val vm = viewModel(FakeNicknameStore())
        enter(vm)

        val state = vm.state()
        assertNull(state.nickname)
        assertEquals(NicknameDialogState(isFirstTime = true), state.nicknameDialog)
        assertFalse(state.actionsEnabled)
    }

    @Test
    fun storedNicknameShowsNoDialog() {
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)

        assertEquals("Ana", vm.state().nickname)
        assertNull(vm.state().nicknameDialog)
    }

    @Test
    fun cancellingTheFirstTimeDialogSignalsBackToStartOnceAndSavesNothing() {
        val store = FakeNicknameStore()
        val vm = viewModel(store)
        enter(vm)

        vm.onNicknameDialogDismissed()

        assertEquals(listOf(LobbySignal.BackToStart), vm.state().signals)
        assertNull(store.readNickname())
        vm.consumeSignal(LobbySignal.BackToStart)
        vm.consumeSignal(LobbySignal.BackToStart)
        assertTrue(vm.state().signals.isEmpty())
    }

    @Test
    fun rejectedNicknamesKeepTheDialogOpenWithErrorAndSaveNothing() {
        val store = FakeNicknameStore()
        val vm = viewModel(store)
        enter(vm)

        listOf("", "   ", "x".repeat(21)).forEach { text ->
            vm.onNicknameSubmitted(text)

            assertEquals(NicknameDialogState(isFirstTime = true, isRejected = true), vm.state().nicknameDialog)
            assertNull(vm.state().nickname)
            assertNull(store.readNickname())
        }
    }

    @Test
    fun validNicknameIsStoredTrimmedAndClosesTheDialog() {
        val store = FakeNicknameStore()
        val vm = viewModel(store)
        enter(vm)
        vm.onNicknameSubmitted("")

        vm.onNicknameSubmitted("  Ana  ")

        assertEquals("Ana", store.readNickname())
        assertEquals("Ana", vm.state().nickname)
        assertNull(vm.state().nicknameDialog)
    }

    @Test
    fun twentyCharacterNicknameIsStoredWhole() {
        val store = FakeNicknameStore()
        val vm = viewModel(store)
        enter(vm)

        vm.onNicknameSubmitted("x".repeat(20))

        assertEquals("x".repeat(20), store.readNickname())
    }

    @Test
    fun changeNicknameStoresLuis() {
        val store = FakeNicknameStore("Ana")
        val vm = viewModel(store)
        enter(vm)

        vm.onChangeNicknameRequested()
        assertEquals(NicknameDialogState(isFirstTime = false), vm.state().nicknameDialog)
        assertEquals("Ana", vm.state().nickname)
        vm.onNicknameSubmitted("Luis")

        assertEquals("Luis", store.readNickname())
        assertEquals("Luis", vm.state().nickname)
        assertNull(vm.state().nicknameDialog)
    }

    @Test
    fun cancellingTheChangeKeepsTheNicknameWithoutSignal() {
        val store = FakeNicknameStore("Ana")
        val vm = viewModel(store)
        enter(vm)
        vm.onChangeNicknameRequested()

        vm.onNicknameDialogDismissed()

        assertNull(vm.state().nicknameDialog)
        assertEquals("Ana", vm.state().nickname)
        assertEquals("Ana", store.readNickname())
        assertTrue(vm.state().signals.isEmpty())
    }

    @Test
    fun rejectedChangeKeepsTheCurrentNickname() {
        val store = FakeNicknameStore("Ana")
        val vm = viewModel(store)
        enter(vm)
        vm.onChangeNicknameRequested()

        vm.onNicknameSubmitted("   ")

        assertEquals(NicknameDialogState(isFirstTime = false, isRejected = true), vm.state().nicknameDialog)
        assertEquals("Ana", store.readNickname())
    }

    @Test
    fun slowIdentityKeepsConnectingAndNeverTimesOutBeforeItEstablishes() {
        fakeService.holdSignIn()
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)

        harness.scheduler.advanceTimeBy(10_000)
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Connecting, vm.state().connection)

        fakeService.releaseSignIn()
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Connected, vm.state().connection)
        assertTrue(vm.state().actionsEnabled)
    }

    @Test
    fun identityFailureThenRetryGoesBackToConnectingAndThenConnects() {
        fakeService.signInFails = true
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)
        assertEquals(LobbyConnection.IdentityFailed, vm.state().connection)
        assertFalse(vm.state().actionsEnabled)

        fakeService.signInFails = false
        fakeService.holdSignIn()
        vm.onRetryConnection()
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Connecting, vm.state().connection)

        fakeService.releaseSignIn()
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Connected, vm.state().connection)
        assertEquals(2, service.signInCalls)
    }

    @Test
    fun retryIsIgnoredUnlessIdentityFailed() {
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)

        vm.onRetryConnection()
        harness.scheduler.runCurrent()

        assertEquals(1, service.signInCalls)
    }

    @Test
    fun noConnectionAfterIdentityBecomesOfflineWhenTheWaitExpiresAndRecoversByItself() {
        backend.disconnect("me")
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)

        harness.scheduler.advanceTimeBy(OnlineLobbyViewModel.CONNECTION_TIMEOUT_MILLIS - 1)
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Connecting, vm.state().connection)

        harness.scheduler.advanceTimeBy(1)
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Offline, vm.state().connection)
        assertFalse(vm.state().actionsEnabled)

        backend.reconnect("me")
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Connected, vm.state().connection)
    }

    @Test
    fun connectionLossAndRecoveryAreReflectedImmediately() {
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)
        assertEquals(LobbyConnection.Connected, vm.state().connection)
        assertTrue(vm.state().actionsEnabled)

        backend.disconnect("me")
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Offline, vm.state().connection)
        assertFalse(vm.state().actionsEnabled)

        backend.reconnect("me")
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Connected, vm.state().connection)
        assertTrue(vm.state().actionsEnabled)
    }

    @Test
    fun connectionTimeoutDoesNotFireOnceConnected() {
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)

        harness.scheduler.advanceTimeBy(10_000)
        harness.scheduler.runCurrent()

        assertEquals(LobbyConnection.Connected, vm.state().connection)
    }

    @Test
    fun secondEntryDoesNotRepeatTheIdentityAndShowsTheCurrentConnection() {
        val store = FakeNicknameStore("Ana")
        val vm = viewModel(store)
        enter(vm)
        backend.disconnect("me")
        harness.scheduler.runCurrent()

        enter(vm)

        assertEquals(1, service.signInCalls)
        assertEquals(LobbyConnection.Offline, vm.state().connection)
        backend.reconnect("me")
        harness.scheduler.runCurrent()
        assertEquals(LobbyConnection.Connected, vm.state().connection)
    }

    @Test
    fun reenteringAfterFirstTimeCancellationAsksAgainAndReenteringWhileHeldDoesNotRestart() {
        fakeService.holdSignIn()
        val vm = viewModel(FakeNicknameStore())
        enter(vm)
        vm.onNicknameDialogDismissed()
        vm.consumeSignal(LobbySignal.BackToStart)

        enter(vm)

        assertEquals(NicknameDialogState(isFirstTime = true), vm.state().nicknameDialog)
        assertEquals(1, service.signInCalls)
        assertEquals(LobbyConnection.Connecting, vm.state().connection)
    }

    @Test
    fun gamesListShowsTwoGamesNewestFirst() {
        val vm = readyLobby("Zoe")
        otherDevice("ana", "Ana").onCreateGame()
        idle()
        otherDevice("luis", "Luis").onCreateGame()
        idle()

        assertEquals(listOf("Luis", "Ana"), gameTexts(vm))
    }

    @Test
    fun gamesListIsEmptyWithoutGames() {
        val vm = readyLobby()

        assertTrue(vm.games.value.isEmpty())
    }

    @Test
    fun newGameAppearsAndTakenGameLeavesWithoutReloading() {
        val vm = readyLobby("Zoe")
        val ana = otherDevice("ana", "Ana")
        ana.onCreateGame()
        idle()
        assertEquals(listOf("Ana"), gameTexts(vm))

        vm.onJoinGame(vm.games.value.single().id)
        idle()

        assertTrue(vm.games.value.isEmpty())
    }

    @Test
    fun gamesAreNotObservedBeforeIdentityNorWithoutNickname() {
        fakeService.holdSignIn()
        val store = FakeNicknameStore()
        val vm = viewModel(store)
        enter(vm)
        vm.collectGames()
        assertEquals(0, service.listObservations)

        fakeService.releaseSignIn()
        idle()
        assertEquals(0, service.listObservations)
        assertTrue(vm.games.value.isEmpty())

        vm.onNicknameSubmitted("Ana")
        idle()
        assertEquals(1, service.listObservations)
    }

    @Test
    fun gamesAreNotObservedBeforeIdentityWithStoredNickname() {
        fakeService.holdSignIn()
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)
        vm.collectGames()
        assertEquals(0, service.listObservations)

        fakeService.releaseSignIn()
        idle()
        assertEquals(1, service.listObservations)
    }

    @Test
    fun gamesSubscriptionIsNotReopenedWithinFiveSecondsOfLeavingAndComingBack() {
        val vm = viewModel(FakeNicknameStore("Ana"))
        enter(vm)
        val first = vm.collectGames()
        assertEquals(1, service.listObservations)

        first.cancel()
        harness.scheduler.advanceTimeBy(4_000)
        vm.collectGames()
        harness.scheduler.advanceTimeBy(10_000)
        idle()

        assertEquals(1, service.listObservations)
    }

    @Test
    fun createdGameUsesTheCurrentNicknameAfterAChange() {
        val vm = readyLobby("Ana")
        val other = otherDevice("zoe", "Zoe")
        vm.onChangeNicknameRequested()
        vm.onNicknameSubmitted("Luis")

        vm.onCreateGame()
        idle()

        assertEquals(listOf("Luis"), gameTexts(other))
        assertEquals("game-1", vm.uiState.value.waitingGameId)
    }

    @Test
    fun createDoesNothingWithoutConnectionOrNickname() {
        val offline = readyLobby()
        backend.disconnect("me")
        idle()
        offline.onCreateGame()
        idle()
        assertEquals(0, backend.createCount)
        assertNull(offline.uiState.value.waitingGameId)

        val noNickname = otherDeviceWithoutNickname()
        noNickname.onCreateGame()
        idle()
        assertEquals(0, backend.createCount)
    }

    private fun otherDeviceWithoutNickname(): OnlineLobbyViewModel {
        val vm = OnlineLobbyViewModel(backend.serviceFor("new"), FakeNicknameStore(), harness.dispatcher)
        enter(vm)
        return vm
    }

    @Test
    fun doublePressCreatesOneGame() {
        val vm = readyLobby()

        vm.onCreateGame()
        vm.onCreateGame()
        idle()
        vm.onCreateGame()
        idle()

        assertEquals(1, backend.createCount)
    }

    @Test
    fun failedCreationSignalsAndDoesNotWaitThenSecondAttemptSucceeds() {
        val vm = readyLobby()
        fakeService.failNextCreate()

        vm.onCreateGame()
        idle()

        assertEquals(listOf<LobbySignal>(LobbySignal.CreateFailed), vm.uiState.value.signals)
        assertNull(vm.uiState.value.waitingGameId)
        assertEquals(0, backend.createCount)
        assertTrue(vm.games.value.isEmpty())

        vm.onCreateGame()
        idle()
        assertEquals("game-1", vm.uiState.value.waitingGameId)
        assertEquals(1, backend.createCount)
    }

    @Test
    fun cancellingWaitingRemovesTheGameWithoutMessage() {
        val vm = readyLobby()
        val other = otherDevice("zoe", "Zoe")
        vm.onCreateGame()
        idle()
        assertEquals(listOf("Ana"), gameTexts(other))

        vm.onCancelWaiting()
        idle()

        assertNull(vm.uiState.value.waitingGameId)
        assertTrue(vm.uiState.value.signals.isEmpty())
        assertTrue(other.games.value.isEmpty())
        assertEquals(1, backend.cancelCount)
    }

    @Test
    fun externalDeletionOfTheWaitingGameEndsWaitingWithMessage() {
        val vm = readyLobby()
        vm.onCreateGame()
        idle()

        backend.deleteGame("game-1")
        idle()

        assertNull(vm.uiState.value.waitingGameId)
        assertEquals(listOf<LobbySignal>(LobbySignal.GameUnavailable), vm.uiState.value.signals)
    }

    @Test
    fun rivalJoiningGivesTheCreatorATicketWithMarkXOnce() {
        val host = readyLobby("Ana")
        host.onCreateGame()
        idle()
        val guest = otherDevice("zoe", "Zoe")

        guest.onJoinGame("game-1")
        idle()

        val ticket = MatchTicket("game-1", Player.X)
        assertEquals(listOf<LobbySignal>(LobbySignal.MatchReady(ticket)), host.uiState.value.signals)
        assertNull(host.uiState.value.waitingGameId)
        host.consumeSignal(LobbySignal.MatchReady(ticket))
        host.consumeSignal(LobbySignal.MatchReady(ticket))
        assertTrue(host.uiState.value.signals.isEmpty())
    }

    @Test
    fun joiningSuccessfullyGivesTicketWithMarkO() {
        otherDevice("ana", "Ana").onCreateGame()
        idle()
        val vm = readyLobby("Zoe")

        vm.onJoinGame(vm.games.value.single().id)
        idle()

        assertEquals(
            listOf<LobbySignal>(LobbySignal.MatchReady(MatchTicket("game-1", Player.O))),
            vm.uiState.value.signals,
        )
        assertEquals("Zoe", backend.gameById("game-1")?.guest?.name)
    }

    @Test
    fun twoClientsChoosingTheSameGameLetOnlyOnePass() {
        otherDevice("ana", "Ana").onCreateGame()
        idle()
        val first = otherDevice("luis", "Luis")
        val second = otherDevice("zoe", "Zoe")

        first.onJoinGame("game-1")
        second.onJoinGame("game-1")
        idle()

        assertEquals(
            listOf<LobbySignal>(LobbySignal.MatchReady(MatchTicket("game-1", Player.O))),
            first.uiState.value.signals,
        )
        assertEquals(listOf<LobbySignal>(LobbySignal.GameUnavailable), second.uiState.value.signals)
        assertEquals("Luis", backend.gameById("game-1")?.guest?.name)
    }

    @Test
    fun gameCancelledBeforeJoiningShowsMessageAndStaysInTheLobby() {
        val host = otherDevice("ana", "Ana")
        host.onCreateGame()
        idle()
        val vm = readyLobby("Zoe")
        host.onCancelWaiting()
        idle()

        vm.onJoinGame("game-1")
        idle()

        assertEquals(listOf<LobbySignal>(LobbySignal.GameUnavailable), vm.uiState.value.signals)
        assertNull(backend.gameById("game-1"))
    }

    @Test
    fun thirdPlayerGetsTheMessageAndTheMatchDoesNotChange() {
        otherDevice("ana", "Ana").onCreateGame()
        idle()
        otherDevice("luis", "Luis").onJoinGame("game-1")
        idle()
        val before = backend.gameById("game-1")
        val third = otherDevice("zoe", "Zoe")

        third.onJoinGame("game-1")
        idle()

        assertEquals(listOf<LobbySignal>(LobbySignal.GameUnavailable), third.uiState.value.signals)
        assertEquals(before, backend.gameById("game-1"))
        assertEquals(OnlineStatus.Playing, backend.gameById("game-1")?.status)
    }

    @Test
    fun joinIsIgnoredWithoutConnectionOrWhileWaiting() {
        otherDevice("ana", "Ana").onCreateGame()
        idle()
        val vm = readyLobby("Zoe")
        vm.onCreateGame()
        idle()

        vm.onJoinGame("game-1")
        idle()
        assertTrue(vm.uiState.value.signals.isEmpty())
        assertEquals("game-2", vm.uiState.value.waitingGameId)

        vm.onCancelWaiting()
        backend.disconnect("me")
        idle()
        vm.onJoinGame("game-1")
        idle()
        assertTrue(vm.uiState.value.signals.isEmpty())
        assertNull(backend.gameById("game-1")?.guest)
    }

    @Test
    fun returningToTheLobbyAfterAMatchStartsCleanAndRelisten() {
        val host = readyLobby("Ana")
        host.onCreateGame()
        idle()
        otherDevice("zoe", "Zoe").onJoinGame("game-1")
        idle()
        host.consumeSignal(host.uiState.value.signals.single())

        enter(host)

        assertNull(host.uiState.value.waitingGameId)
        assertTrue(host.uiState.value.signals.isEmpty())
        assertEquals("Ana", host.uiState.value.nickname)
    }

    @Test
    fun factoryCreatesTheViewModel() {
        val factory = onlineLobbyViewModelFactory(service, FakeNicknameStore("Ana"))

        val vm = factory.create(OnlineLobbyViewModel::class.java, CreationExtras.Empty)

        assertEquals(OnlineLobbyUiState(), vm.uiState.value)
    }

    @Test
    fun reenteringKeepsAPendingMatchSignalUntilItIsConsumedOnce() {
        val host = readyLobby("Ana")
        host.onCreateGame()
        idle()
        otherDevice("zoe", "Zoe").onJoinGame("game-1")
        idle()
        val signal = LobbySignal.MatchReady(MatchTicket("game-1", Player.X))

        enter(host)

        assertEquals(listOf<LobbySignal>(signal), host.uiState.value.signals)
        host.consumeSignal(signal)
        host.consumeSignal(signal)
        assertTrue(host.uiState.value.signals.isEmpty())
    }

    @Test
    fun reenteringKeepsTheChangeNicknameDialogOpen() {
        val vm = readyLobby("Ana")
        vm.onChangeNicknameRequested()

        enter(vm)

        assertEquals(NicknameDialogState(isFirstTime = false), vm.uiState.value.nicknameDialog)
    }

    @Test
    fun reenteringKeepsTheRejectionMarkOfTheFirstTimeDialog() {
        val vm = viewModel(FakeNicknameStore())
        enter(vm)
        vm.onNicknameSubmitted("   ")

        enter(vm)

        assertEquals(NicknameDialogState(isFirstTime = true, isRejected = true), vm.uiState.value.nicknameDialog)
    }

    @Test
    fun reenteringWhileWaitingNeitherCancelsNorDuplicatesTheGame() {
        val vm = readyLobby("Ana")
        vm.onCreateGame()
        idle()

        enter(vm)

        assertEquals("game-1", vm.uiState.value.waitingGameId)
        assertEquals(1, backend.createCount)
        assertEquals(0, backend.cancelCount)
        assertEquals(1, service.signInCalls)
        assertEquals("Ana", backend.gameById("game-1")?.host?.name)
    }

    @Test
    fun dismissingTheFirstTimeDialogClearsTheRejectionMarkForTheNextEntry() {
        val store = FakeNicknameStore()
        val vm = viewModel(store)
        enter(vm)
        vm.onNicknameSubmitted("   ")
        vm.onNicknameDialogDismissed()
        vm.consumeSignal(LobbySignal.BackToStart)

        enter(vm)

        assertEquals(NicknameDialogState(isFirstTime = true), vm.uiState.value.nicknameDialog)
        assertNull(store.readNickname())
    }
}
