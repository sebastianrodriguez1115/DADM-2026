package com.edu.unal.tictactoe.ui.online.match

import com.edu.unal.tictactoe.data.online.FakeOnlineBackend
import com.edu.unal.tictactoe.data.online.FakeOnlineGameService
import com.edu.unal.tictactoe.data.online.JoinResult
import com.edu.unal.tictactoe.data.online.MoveOutcome
import com.edu.unal.tictactoe.data.online.OnlineGameService
import com.edu.unal.tictactoe.game.models.Mark
import com.edu.unal.tictactoe.game.models.Player
import com.edu.unal.tictactoe.ui.game.GameViewModelCoroutineTestHarness
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.edu.unal.tictactoe.data.online.OnlineGame
import com.edu.unal.tictactoe.data.online.OnlineServiceException
import com.edu.unal.tictactoe.data.online.OnlineStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineMatchViewModelTest {
    @get:Rule
    val harness = GameViewModelCoroutineTestHarness()

    private val backend = FakeOnlineBackend()
    private val anaService = backend.serviceFor("ana")
    private val luisService = backend.serviceFor("luis")

    private class RecordingService(private val delegate: FakeOnlineGameService) : OnlineGameService by delegate {
        val moves = mutableListOf<Int>()

        override suspend fun playMove(gameId: String, location: Int): MoveOutcome {
            moves += location
            return delegate.playMove(gameId, location)
        }
    }

    /** Observes a game the test controls, so values can be repeated, skipped or withheld. */
    private class ScriptedService(
        private val delegate: FakeOnlineGameService,
        private val failLeave: Boolean = false,
    ) : OnlineGameService by delegate {
        val games = MutableSharedFlow<OnlineGame?>()

        override fun observeGame(gameId: String): Flow<OnlineGame?> = games

        override suspend fun leaveGame(gameId: String) {
            if (failLeave) throw OnlineServiceException("Leave failed")
            delegate.leaveGame(gameId)
        }
    }

    private val anaRecorder = RecordingService(anaService)
    private val luisRecorder = RecordingService(luisService)

    private fun idle() = harness.scheduler.runCurrent()

    private fun runSuspending(block: suspend () -> Unit) {
        CoroutineScope(harness.dispatcher).launch { block() }
        idle()
    }

    /** Ana hosts a game and Luis joins it; returns the game id. */
    private fun startedGame(): String {
        var id = ""
        runSuspending {
            id = anaService.createGame("Ana")
            assertEquals(JoinResult.Joined, luisService.joinGame(id, "Luis"))
        }
        return id
    }

    private fun viewModel(service: OnlineGameService) = OnlineMatchViewModel(service, harness.dispatcher)

    private fun anaVm(gameId: String) = viewModel(anaRecorder).also { it.openMatch(gameId, Player.X); idle() }

    private fun luisVm(gameId: String) = viewModel(luisRecorder).also { it.openMatch(gameId, Player.O); idle() }

    private fun OnlineMatchViewModel.state() = uiState.value

    private fun OnlineMatchViewModel.tap(cell: Int) {
        play(cell)
        idle()
    }

    private fun board(vararg marks: Pair<Int, Mark>): List<Mark> =
        List(9) { Mark.Empty }.toMutableList().also { list -> marks.forEach { (i, m) -> list[i] = m } }

    private data class Players(val ana: OnlineMatchViewModel, val luis: OnlineMatchViewModel)

    private fun twoPlayers(): Players {
        val id = startedGame()
        return Players(anaVm(id), luisVm(id))
    }

    /** Alternates X (Ana) and O (Luis) over [cells], X first. */
    private fun Players.playAll(vararg cells: Int) {
        cells.forEachIndexed { index, cell -> (if (index % 2 == 0) ana else luis).tap(cell) }
    }

    @Test
    fun beforeTheFirstGameValueTheStateIsLoadingAndInputDisabled() {
        val vm = viewModel(ScriptedService(anaService))
        assertEquals(MatchPhase.Loading, vm.state().phase)
        assertNull(vm.state().myMark)

        vm.openMatch("any", Player.X)
        idle()

        val state = vm.state()
        assertEquals(Player.X, state.myMark)
        assertEquals(MatchPhase.Loading, state.phase)
        assertNull(state.opponentName)
        assertNull(state.result)
        assertEquals(board(), state.board)
        assertFalse(state.isInputEnabled)
    }

    @Test
    fun anaSeesXAndLuisAndLuisSeesOAndAna() {
        val (ana, luis) = twoPlayers()

        assertEquals(Player.X, ana.state().myMark)
        assertEquals("Luis", ana.state().opponentName)
        assertEquals(Player.O, luis.state().myMark)
        assertEquals("Ana", luis.state().opponentName)
        assertEquals(MatchPhase.Playing, ana.state().phase)
    }

    @Test
    fun theInitialBoardIsEmptyOnBothSides() {
        val players = twoPlayers()

        assertEquals(board(), players.ana.state().board)
        assertEquals(board(), players.luis.state().board)
    }

    @Test
    fun xHasTheFirstTurnSeenFromBothSides() {
        val players = twoPlayers()

        assertTrue(players.ana.state().isMyTurn)
        assertTrue(players.ana.state().isInputEnabled)
        assertFalse(players.luis.state().isMyTurn)
        assertFalse(players.luis.state().isInputEnabled)
    }

    @Test
    fun theRivalsMoveArrivesAndTheTurnChanges() {
        val players = twoPlayers()

        players.ana.tap(4)

        assertEquals(board(4 to Mark.X), players.luis.state().board)
        assertTrue(players.luis.state().isMyTurn)
        assertTrue(players.luis.state().isInputEnabled)
        assertFalse(players.ana.state().isMyTurn)
    }

    @Test
    fun anOwnConfirmedMoveShowsTheMarkAndPassesTheTurnToTheRival() {
        val players = twoPlayers()

        players.ana.tap(0)

        val state = players.ana.state()
        assertEquals(board(0 to Mark.X), state.board)
        assertFalse(state.isMyTurn)
        assertFalse(state.isMovePending)
        assertEquals(listOf(0), anaRecorder.moves)
    }

    @Test
    fun theMarkIsNotShownBeforeConfirmationAndInputStaysDisabledWhilePending() {
        val players = twoPlayers()
        anaService.holdNextMove()

        players.ana.tap(4)

        assertEquals(board(), players.ana.state().board)
        assertTrue(players.ana.state().isMyTurn)
        assertTrue(players.ana.state().isMovePending)
        assertFalse(players.ana.state().isInputEnabled)

        anaService.releaseHeldMove()
        idle()

        assertEquals(board(4 to Mark.X), players.ana.state().board)
        assertFalse(players.ana.state().isMovePending)
    }

    @Test
    fun aSecondTapWhileAMoveIsPendingIsNotSent() {
        val players = twoPlayers()
        anaService.holdNextMove()

        players.ana.tap(4)
        players.ana.tap(5)
        anaService.releaseHeldMove()
        idle()

        assertEquals(listOf(4), anaRecorder.moves)
        assertEquals(board(4 to Mark.X), players.ana.state().board)
    }

    @Test
    fun aRejectedMoveLeavesBoardAndTurnUnchangedAndAllowsRetry() {
        val players = twoPlayers()
        anaService.rejectNextMove()

        players.ana.tap(4)

        assertEquals(board(), players.ana.state().board)
        assertTrue(players.ana.state().isMyTurn)
        assertFalse(players.ana.state().isMovePending)
        assertTrue(players.ana.state().isInputEnabled)

        players.ana.tap(4)

        assertEquals(listOf(4, 4), anaRecorder.moves)
        assertEquals(board(4 to Mark.X), players.ana.state().board)
    }

    @Test
    fun aServiceFailureLeavesBoardAndTurnUnchangedAndAllowsRetry() {
        val players = twoPlayers()
        anaService.failNextMove()

        players.ana.tap(4)

        assertEquals(board(), players.ana.state().board)
        assertTrue(players.ana.state().isMyTurn)
        assertTrue(players.ana.state().isInputEnabled)

        players.ana.tap(4)

        assertEquals(board(4 to Mark.X), players.ana.state().board)
    }

    @Test
    fun anOccupiedCellIsNotSent() {
        val players = twoPlayers()
        players.playAll(4)

        players.luis.tap(4)

        assertTrue(luisRecorder.moves.isEmpty())
        assertEquals(board(4 to Mark.X), players.luis.state().board)
        assertTrue(players.luis.state().isMyTurn)
    }

    @Test
    fun aTapOutOfTurnIsNotSent() {
        val players = twoPlayers()

        players.luis.tap(4)

        assertTrue(luisRecorder.moves.isEmpty())
        assertEquals(board(), players.luis.state().board)
        assertEquals(board(), players.ana.state().board)
    }

    @Test
    fun aTapOutsideTheBoardIsNotSent() {
        val players = twoPlayers()

        players.ana.tap(9)
        players.ana.tap(-1)

        assertTrue(anaRecorder.moves.isEmpty())
    }

    @Test
    fun victoryAndDefeatAreSeenFromEachPlayerWithInputDisabled() {
        val players = twoPlayers()

        players.playAll(0, 3, 1, 4, 2)

        assertEquals(MatchPhase.Finished, players.ana.state().phase)
        assertEquals(MatchResult.Won, players.ana.state().result)
        assertEquals(MatchResult.Lost, players.luis.state().result)
        assertFalse(players.ana.state().isInputEnabled)
        assertFalse(players.luis.state().isInputEnabled)
        assertEquals(
            board(0 to Mark.X, 1 to Mark.X, 2 to Mark.X, 3 to Mark.O, 4 to Mark.O),
            players.luis.state().board,
        )
    }

    @Test
    fun theWinnerAsOSeesVictoryAndXSeesDefeat() {
        val players = twoPlayers()

        players.playAll(0, 3, 1, 4, 8, 5)

        assertEquals(MatchResult.Won, players.luis.state().result)
        assertEquals(MatchResult.Lost, players.ana.state().result)
        assertFalse(players.ana.state().isInputEnabled)
        assertFalse(players.luis.state().isInputEnabled)
    }

    @Test
    fun aFullBoardWithoutLineIsATieForBothWithInputDisabled() {
        val players = twoPlayers()

        players.playAll(0, 1, 2, 4, 3, 5, 7, 6, 8)

        assertEquals(MatchResult.Tie, players.ana.state().result)
        assertEquals(MatchResult.Tie, players.luis.state().result)
        assertEquals(MatchPhase.Finished, players.ana.state().phase)
        assertFalse(players.ana.state().isInputEnabled)
        assertFalse(players.luis.state().isInputEnabled)
    }

    @Test
    fun thereIsNoResultWhileTheGameIsInProgress() {
        val players = twoPlayers()

        players.playAll(0, 3)

        assertNull(players.ana.state().result)
    }

    @Test
    fun inputIsDisabledWithoutConnectionAndEnabledAgainWhenItReturns() {
        val players = twoPlayers()

        backend.disconnect("ana")
        idle()

        assertFalse(players.ana.state().isConnected)
        assertFalse(players.ana.state().isInputEnabled)
        players.ana.tap(4)
        assertTrue(anaRecorder.moves.isEmpty())

        backend.reconnect("ana")
        idle()

        assertTrue(players.ana.state().isConnected)
        assertTrue(players.ana.state().isInputEnabled)
    }

    @Test
    fun openingASecondMatchDiscardsTheStateOfTheFirst() {
        val first = startedGame()
        val vm = anaVm(first)
        vm.tap(4)
        assertEquals(board(4 to Mark.X), vm.state().board)

        var second = ""
        runSuspending {
            second = luisService.createGame("Luis")
            anaService.joinGame(second, "Ana")
        }
        vm.openMatch(second, Player.O)
        idle()

        val state = vm.state()
        assertEquals(Player.O, state.myMark)
        assertEquals("Luis", state.opponentName)
        assertEquals(board(), state.board)
        assertFalse(state.isMyTurn)
        assertFalse(state.isMovePending)

        runSuspending { luisService.playMove(first, 0) }
        assertEquals(board(), vm.state().board)
    }

    @Test
    fun openingAMatchWhileAMoveIsPendingDropsThePendingMove() {
        val first = startedGame()
        val vm = anaVm(first)
        anaService.holdNextMove()
        vm.tap(4)
        assertTrue(vm.state().isMovePending)

        val second = runSuspending2 { luisService.createGame("Luis").also { anaService.joinGame(it, "Ana") } }
        vm.openMatch(second, Player.O)
        idle()
        anaService.releaseHeldMove()
        idle()

        assertFalse(vm.state().isMovePending)
        assertEquals(board(), vm.state().board)
    }

    private fun runSuspending2(block: suspend () -> String): String {
        var result = ""
        runSuspending { result = block() }
        return result
    }

    private val collectors = mutableListOf<Job>()

    @After
    fun stopCollectors() {
        collectors.forEach(Job::cancel)
    }

    private fun OnlineMatchViewModel.collectSounds(): List<Player> {
        val sounds = mutableListOf<Player>()
        collectors += CoroutineScope(harness.dispatcher).launch { acceptedMoves.collect { sounds += it } }
        idle()
        return sounds
    }

    private fun scripted(): Triple<OnlineMatchViewModel, ScriptedService, OnlineGame> {
        val id = startedGame()
        val service = ScriptedService(anaService)
        val vm = viewModel(service)
        vm.openMatch(id, Player.X)
        idle()
        return Triple(vm, service, backend.gameById(id)!!)
    }

    private fun ScriptedService.send(game: OnlineGame?) {
        runSuspending { games.emit(game) }
    }

    @Test
    fun theRivalLeavingDuringTheGameAbandonsItWithoutWinner() {
        val players = twoPlayers()
        players.playAll(0, 3)

        players.luis.leave()
        idle()

        val state = players.ana.state()
        assertEquals(MatchPhase.Abandoned, state.phase)
        assertNull(state.result)
        assertFalse(state.isInputEnabled)
        assertFalse(state.isMovePending)
        assertEquals(board(0 to Mark.X, 3 to Mark.O), state.board)
    }

    @Test
    fun aFirstNullValueIsAnAbandonment() {
        val vm = viewModel(anaRecorder)

        vm.openMatch("missing", Player.X)
        idle()

        assertEquals(MatchPhase.Abandoned, vm.state().phase)
        assertNull(vm.state().result)
        assertFalse(vm.state().isInputEnabled)
    }

    @Test
    fun aFinishedResultIsStickyWhenTheGameVanishesLater() {
        val players = twoPlayers()
        players.playAll(0, 3, 1, 4, 2)
        val before = players.luis.state()

        players.ana.leave()
        idle()

        assertNull(backend.gameById("game-1"))
        assertEquals(before, players.luis.state())
        assertEquals(MatchResult.Lost, players.luis.state().result)
    }

    @Test
    fun anAbandonmentWithAPendingMoveLeavesNoStuckPendingState() {
        val players = twoPlayers()
        anaService.holdNextMove()
        players.ana.tap(4)
        assertTrue(players.ana.state().isMovePending)

        players.luis.leave()
        idle()

        assertEquals(MatchPhase.Abandoned, players.ana.state().phase)
        assertFalse(players.ana.state().isMovePending)

        anaService.releaseHeldMove()
        idle()

        assertEquals(MatchPhase.Abandoned, players.ana.state().phase)
        assertFalse(players.ana.state().isMovePending)
        assertEquals(board(), players.ana.state().board)
    }

    @Test
    fun leavingDuringTheGameDeletesItAndTheRivalSeesTheAbandonment() {
        val players = twoPlayers()

        players.ana.leave()
        idle()

        assertNull(backend.gameById("game-1"))
        assertEquals(MatchPhase.Abandoned, players.luis.state().phase)
        assertNull(players.luis.state().result)
    }

    @Test
    fun leavingAnAbandonedMatchCleansUpAndClearsTheState() {
        val players = twoPlayers()
        players.luis.leave()
        idle()

        players.ana.leave()
        idle()

        assertEquals(OnlineMatchUiState(), players.ana.state())
    }

    @Test
    fun leavingClearsTheStateAndANewMatchCanBeOpened() {
        val players = twoPlayers()
        players.playAll(0)

        players.ana.leave()
        idle()

        assertEquals(OnlineMatchUiState(), players.ana.state())

        var second = ""
        runSuspending {
            second = luisService.createGame("Luis")
            anaService.joinGame(second, "Ana")
        }
        players.ana.openMatch(second, Player.O)
        idle()

        assertEquals(Player.O, players.ana.state().myMark)
        assertEquals("Luis", players.ana.state().opponentName)
        assertEquals(MatchPhase.Playing, players.ana.state().phase)
    }

    @Test
    fun aFailingLeaveDoesNotPreventLeaving() {
        val id = startedGame()
        val vm = viewModel(ScriptedService(anaService, failLeave = true))
        vm.openMatch(id, Player.X)
        idle()

        vm.leave()
        idle()

        assertEquals(OnlineMatchUiState(), vm.state())
    }

    @Test
    fun theOfflineNoticeShowsOnlyWhileTheGameIsInProgress() {
        val players = twoPlayers()
        assertFalse(players.ana.state().isOfflineNoticeVisible)

        backend.disconnect("ana")
        idle()
        assertTrue(players.ana.state().isOfflineNoticeVisible)

        backend.reconnect("ana")
        idle()
        assertFalse(players.ana.state().isOfflineNoticeVisible)
        assertTrue(players.ana.state().isInputEnabled)
    }

    @Test
    fun noOfflineNoticeOnAFinishedOrAbandonedGame() {
        val players = twoPlayers()
        players.playAll(0, 3, 1, 4, 2)
        backend.disconnect("ana")
        idle()
        assertFalse(players.ana.state().isOfflineNoticeVisible)

        val other = OnlineMatchViewModel(anaRecorder, harness.dispatcher)
        other.openMatch("missing", Player.X)
        idle()
        backend.disconnect("ana")
        idle()
        assertFalse(other.state().isOfflineNoticeVisible)
    }

    @Test
    fun afterReconnectingOnTheRivalsTurnTheInputStaysDisabled() {
        val players = twoPlayers()

        backend.disconnect("luis")
        idle()
        backend.reconnect("luis")
        idle()

        assertFalse(players.luis.state().isMyTurn)
        assertFalse(players.luis.state().isInputEnabled)
    }

    @Test
    fun theFactoryReturnsTheSameInstanceOnTwoGets() {
        val provider = ViewModelProvider(ViewModelStore(), onlineMatchViewModelFactory(anaService))

        val first = provider[OnlineMatchViewModel::class.java]
        val second = provider[OnlineMatchViewModel::class.java]

        assertTrue(first === second)
    }

    @Test
    fun anOwnConfirmedMoveEmitsOneSoundWithMyMark() {
        val players = twoPlayers()
        val sounds = players.ana.collectSounds()

        players.ana.tap(0)

        assertEquals(listOf(Player.X), sounds)
    }

    @Test
    fun theRivalsMoveEmitsOneSoundWithItsMark() {
        val players = twoPlayers()
        val sounds = players.luis.collectSounds()

        players.ana.tap(0)

        assertEquals(listOf(Player.X), sounds)
    }

    @Test
    fun theWinningMoveAlsoEmitsASound() {
        val players = twoPlayers()
        players.playAll(0, 3, 1, 4)
        val sounds = players.luis.collectSounds()

        players.ana.tap(2)

        assertEquals(listOf(Player.X), sounds)
        assertEquals(MatchPhase.Finished, players.luis.state().phase)
    }

    @Test
    fun theFirstStateAfterOpeningWithMarksEmitsNothing() {
        val players = twoPlayers()
        players.playAll(0, 3)
        val vm = viewModel(anaRecorder)
        val sounds = vm.collectSounds()

        vm.openMatch("game-1", Player.X)
        idle()

        assertEquals(board(0 to Mark.X, 3 to Mark.O), vm.state().board)
        assertTrue(sounds.isEmpty())
    }

    @Test
    fun resendingTheSameStateEmitsNothing() {
        val (vm, service, game) = scripted()
        val sounds = vm.collectSounds()
        val withMark = game.copy(board = board(0 to Mark.X), turn = Player.O)

        service.send(withMark)
        service.send(withMark)
        service.send(withMark.copy(status = OnlineStatus.Playing))

        assertTrue(sounds.isEmpty())
    }

    @Test
    fun aStateWithExactlyOneMoreMarkEmitsOnceForItsOwner() {
        val (vm, service, game) = scripted()
        val sounds = vm.collectSounds()

        service.send(game.copy(board = board(0 to Mark.X), turn = Player.O))
        service.send(game.copy(board = board(0 to Mark.X, 1 to Mark.O), turn = Player.X))
        assertEquals(listOf(Player.O), sounds)
    }

    @Test
    fun aJumpOfTwoMarksEmitsNothing() {
        val (vm, service, game) = scripted()
        service.send(game)
        val sounds = vm.collectSounds()

        service.send(game.copy(board = board(0 to Mark.X, 1 to Mark.O)))

        assertTrue(sounds.isEmpty())
    }

    @Test
    fun anAbandonmentEmitsNothing() {
        val players = twoPlayers()
        players.playAll(0)
        val sounds = players.ana.collectSounds()

        players.luis.leave()
        idle()

        assertEquals(MatchPhase.Abandoned, players.ana.state().phase)
        assertTrue(sounds.isEmpty())
    }

    @Test
    fun aMoveEmittedWithoutACollectorIsNotDeliveredToALaterOne() {
        val players = twoPlayers()
        players.ana.tap(0)

        val sounds = players.ana.collectSounds()
        assertTrue(sounds.isEmpty())

        players.luis.tap(4)

        assertEquals(listOf(Player.O), sounds)
    }
}
