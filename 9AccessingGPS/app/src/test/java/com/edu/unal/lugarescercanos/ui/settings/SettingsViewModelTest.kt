package com.edu.unal.lugarescercanos.ui.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultRadiusIsOneKmWhenNothingWasSaved() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeRadiusPreferences(null))
        advanceUntilIdle()

        assertEquals(1.0, viewModel.state.value.radiusKm, 0.0)
        assertEquals("1 km", viewModel.state.value.radiusText)
    }

    @Test
    fun savedValueOutOfRangeIsClampedToTheLimit() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeRadiusPreferences(25.0))
        advanceUntilIdle()

        assertEquals(10.0, viewModel.state.value.radiusKm, 0.0)
        assertEquals("10 km", viewModel.state.value.radiusText)
    }

    @Test
    fun movingTheSliderOnlyChangesTheUiState() = runTest(dispatcher) {
        val preferences = FakeRadiusPreferences(null)
        val viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.onRadiusChanged(3.0)
        viewModel.onRadiusChanged(4.5)
        advanceUntilIdle()

        assertEquals(4.5, viewModel.state.value.radiusKm, 0.0)
        assertEquals("4,5 km", viewModel.state.value.radiusText)
        assertTrue("no debe guardar mientras se mueve", preferences.setCalls.isEmpty())
        assertEquals(null, preferences.saved)
    }

    @Test
    fun releasingTheSliderSavesTheClampedValue() = runTest(dispatcher) {
        val preferences = FakeRadiusPreferences(null)
        val viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.onRadiusChanged(0.2)
        assertEquals(0.5, viewModel.state.value.radiusKm, 0.0)
        viewModel.onRadiusChangeFinished()
        advanceUntilIdle()

        assertEquals(listOf(0.5), preferences.setCalls)
        assertEquals(0.5, preferences.saved!!, 0.0)
    }

    @Test
    fun releasingAtThreeKmSavesThreeKm() = runTest(dispatcher) {
        val preferences = FakeRadiusPreferences(null)
        val viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.onRadiusChanged(3.0)
        viewModel.onRadiusChangeFinished()
        advanceUntilIdle()

        assertEquals(listOf(3.0), preferences.setCalls)
        assertEquals("3 km", viewModel.state.value.radiusText)
    }
}
