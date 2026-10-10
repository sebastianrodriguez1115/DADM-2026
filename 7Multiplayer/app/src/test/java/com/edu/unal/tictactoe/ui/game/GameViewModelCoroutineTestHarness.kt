package com.edu.unal.tictactoe.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.edu.unal.tictactoe.data.preferences.FakeGamePreferences
import com.edu.unal.tictactoe.data.preferences.GamePreferences
import com.edu.unal.tictactoe.game.TicTacToeGame
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Keeps coroutine delays and the injected monotonic clock on one virtual timeline. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelCoroutineTestHarness : TestWatcher() {
    val scheduler = TestCoroutineScheduler()
    val dispatcher = StandardTestDispatcher(scheduler)
    val clock = MonotonicClock { scheduler.currentTime * NANOS_PER_MILLISECOND }
    private val viewModelStores = mutableListOf<ViewModelStore>()

    fun createViewModel(
        game: TicTacToeGame = TicTacToeGame(),
        mainDispatcher: CoroutineDispatcher = dispatcher,
        preferences: GamePreferences = FakeGamePreferences(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): GameViewModel {
        val viewModel = GameViewModel(
            preferences = preferences,
            savedStateHandle = savedStateHandle,
            game = game,
            monotonicClock = clock,
            mainDispatcher = mainDispatcher,
        )
        viewModelStores += ViewModelStore().apply { put("game", viewModel) }
        return viewModel
    }

    fun createViewModelResumed(
        game: TicTacToeGame = TicTacToeGame(),
        preferences: GamePreferences = FakeGamePreferences(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = createViewModel(game, preferences = preferences, savedStateHandle = savedStateHandle).also { it.onHostResumed() }

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        try {
            viewModelStores.forEach(ViewModelStore::clear)
        } finally {
            viewModelStores.clear()
            Dispatchers.resetMain()
        }
    }

    companion object {
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
