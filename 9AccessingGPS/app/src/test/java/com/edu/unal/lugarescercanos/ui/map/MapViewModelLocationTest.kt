package com.edu.unal.lugarescercanos.ui.map

import com.edu.unal.lugarescercanos.domain.GeoPoint
import com.edu.unal.lugarescercanos.location.LocationPermissionState
import com.edu.unal.lugarescercanos.location.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelLocationTest {
    private val dispatcher = StandardTestDispatcher()
    private val bogota = GeoPoint(4.6381, -74.0840)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successWithPreciseLocationShowsPointAndAddress() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Success(bogota, 5f))
        val resolver = FakeAddressResolver("Cra. 30 #45-03, Bogotá")
        val viewModel = MapViewModel(provider, resolver)

        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        val location = viewModel.state.value.location as LocationUiState.Ready
        assertEquals(bogota, location.point)
        assertEquals(5f, location.accuracyMeters!!, 0f)
        assertEquals("Cra. 30 #45-03, Bogotá", location.address)
        assertFalse(location.resolvingAddress)
        assertEquals(1, provider.readCount)
        assertEquals(listOf(bogota), resolver.requested)
    }

    @Test
    fun successWithoutAddressKeepsCoordinatesAndNullAddress() = runTest(dispatcher) {
        val viewModel = MapViewModel(
            FakeLocationProvider(LocationResult.Success(bogota, null)),
            FakeAddressResolver(null),
        )

        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        val location = viewModel.state.value.location as LocationUiState.Ready
        assertEquals(bogota, location.point)
        assertNull(location.accuracyMeters)
        assertNull(location.address)
        assertFalse(location.resolvingAddress)
    }

    @Test
    fun disabledDeviceLocationShowsDisabledState() = runTest(dispatcher) {
        val viewModel = MapViewModel(
            FakeLocationProvider(LocationResult.LocationDisabled),
            FakeAddressResolver("no se usa"),
        )

        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(LocationUiState.Disabled, viewModel.state.value.location)
    }

    @Test
    fun unavailableLocationShowsUnavailableStateAndSkipsAddress() = runTest(dispatcher) {
        val resolver = FakeAddressResolver("no se usa")
        val viewModel = MapViewModel(FakeLocationProvider(LocationResult.Unavailable), resolver)

        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(LocationUiState.Unavailable, viewModel.state.value.location)
        assertTrue(resolver.requested.isEmpty())
    }

    @Test
    fun approximatePermissionStillReadsLocationAndIsFlagged() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Success(bogota, 120f))
        val viewModel = MapViewModel(provider, FakeAddressResolver("Calle 26 #1-00"))

        viewModel.onPermissionResult(LocationPermissionState.Approximate)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isApproximate)
        assertTrue(viewModel.state.value.location is LocationUiState.Ready)
        assertEquals(1, provider.readCount)
    }

    @Test
    fun deniedPermissionDoesNotReadLocation() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Success(bogota, 5f))
        val viewModel = MapViewModel(provider, FakeAddressResolver("no se usa"))

        viewModel.onPermissionResult(LocationPermissionState.Denied)
        advanceUntilIdle()

        assertEquals(LocationUiState.Idle, viewModel.state.value.location)
        assertEquals(0, provider.readCount)
    }

    @Test
    fun grantingPermissionReadsOnlyOnce() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Success(bogota, 5f))
        val viewModel = MapViewModel(provider, FakeAddressResolver(null))

        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()
        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(1, provider.readCount)
    }

    @Test
    fun refreshReadsAgainAndUpdatesPoint() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Success(bogota, 5f))
        val viewModel = MapViewModel(provider, FakeAddressResolver(null))
        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        val moved = GeoPoint(4.6400, -74.0800)
        provider.result = LocationResult.Success(moved, 5f)
        viewModel.refreshLocation()
        advanceUntilIdle()

        assertEquals(2, provider.readCount)
        assertEquals(moved, (viewModel.state.value.location as LocationUiState.Ready).point)
    }

    @Test
    fun refreshWithoutPermissionDoesNothing() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Success(bogota, 5f))
        val viewModel = MapViewModel(provider, FakeAddressResolver(null))

        viewModel.refreshLocation()
        advanceUntilIdle()

        assertEquals(0, provider.readCount)
    }

    @Test
    fun resumeAfterDisabledLocationRereadsOnce() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.LocationDisabled)
        val viewModel = MapViewModel(provider, FakeAddressResolver(null))
        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()
        assertEquals(LocationUiState.Disabled, viewModel.state.value.location)

        provider.result = LocationResult.Success(bogota, 5f)
        viewModel.onAppResumed(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(2, provider.readCount)
        assertEquals(bogota, (viewModel.state.value.location as LocationUiState.Ready).point)
    }

    @Test
    fun resumeAfterUnavailableLocationRereads() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Unavailable)
        val viewModel = MapViewModel(provider, FakeAddressResolver(null))
        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        provider.result = LocationResult.Success(bogota, 5f)
        viewModel.onAppResumed(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(2, provider.readCount)
        assertTrue(viewModel.state.value.location is LocationUiState.Ready)
    }

    @Test
    fun resumeWithReadyLocationDoesNotReread() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Success(bogota, 5f))
        val viewModel = MapViewModel(provider, FakeAddressResolver(null))
        viewModel.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        viewModel.onAppResumed(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(1, provider.readCount)
    }

    @Test
    fun resumeWithoutPermissionDoesNotRead() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.LocationDisabled)
        val viewModel = MapViewModel(provider, FakeAddressResolver(null))
        viewModel.onPermissionResult(LocationPermissionState.Denied)
        advanceUntilIdle()

        viewModel.onAppResumed(LocationPermissionState.Denied)
        advanceUntilIdle()

        assertEquals(0, provider.readCount)
        assertEquals(LocationUiState.Idle, viewModel.state.value.location)
    }

    @Test
    fun resumeWithNewPermissionReadsOnlyOnce() = runTest(dispatcher) {
        val provider = FakeLocationProvider(LocationResult.Success(bogota, 5f))
        val viewModel = MapViewModel(provider, FakeAddressResolver(null))
        viewModel.onPermissionResult(LocationPermissionState.Denied)
        advanceUntilIdle()

        viewModel.onAppResumed(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(LocationPermissionState.Precise, viewModel.state.value.permission)
        assertEquals(1, provider.readCount)
    }
}
