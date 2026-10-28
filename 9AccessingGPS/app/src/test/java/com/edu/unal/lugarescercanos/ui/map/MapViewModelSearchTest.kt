package com.edu.unal.lugarescercanos.ui.map

import com.edu.unal.lugarescercanos.data.places.PlacesSearchResult
import com.edu.unal.lugarescercanos.domain.GeoPoint
import com.edu.unal.lugarescercanos.domain.Place
import com.edu.unal.lugarescercanos.domain.PlaceCategory
import com.edu.unal.lugarescercanos.domain.PlacesError
import com.edu.unal.lugarescercanos.location.LocationPermissionState
import com.edu.unal.lugarescercanos.location.LocationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelSearchTest {
    private val dispatcher = StandardTestDispatcher()
    private val bogota = GeoPoint(4.6381, -74.0840)
    private val hospital = Place("h1", "Clínica Nicolás de Federman", 4.6390, -74.0840, 100.0)
    private val pharmacy = Place("p1", "Farmacia Cafam", 4.6400, -74.0850, 200.0)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakePlacesRepository,
        mapsKeyConfigured: Boolean = true,
        radiusKm: MutableStateFlow<Double> = MutableStateFlow(1.0),
    ) = MapViewModel(
        locationProvider = FakeLocationProvider(LocationResult.Success(bogota, 5f)),
        addressResolver = FakeAddressResolver(null),
        placesRepository = repository,
        radiusKm = radiusKm,
        mapsKeyConfigured = mapsKeyConfigured,
    )

    @Test
    fun resultWithPlacesShowsResultsForDefaultCategoryAndRadius() = runTest(dispatcher) {
        val repository = FakePlacesRepository { PlacesSearchResult.Success(listOf(hospital)) }
        val vm = viewModel(repository)

        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(PlacesUiState.Results(listOf(hospital)), vm.state.value.search)
        val request = repository.requests.single()
        assertEquals(bogota, request.center)
        assertEquals(1000.0, request.radiusMeters, 0.0)
        assertEquals(PlaceCategory.HospitalsAndClinics.placeTypes, request.includedTypes)
    }

    @Test
    fun emptyResultShowsEmpty() = runTest(dispatcher) {
        val vm = viewModel(FakePlacesRepository { PlacesSearchResult.Success(emptyList()) })

        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(PlacesUiState.Empty, vm.state.value.search)
    }

    @Test
    fun failureShowsErrorWithItsCause() = runTest(dispatcher) {
        val vm = viewModel(FakePlacesRepository { PlacesSearchResult.Failure(PlacesError.NoConnection) })

        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(PlacesUiState.Error(PlacesError.NoConnection), vm.state.value.search)
    }

    @Test
    fun retryRepeatsSearchWithSameCategoryAndRadius() = runTest(dispatcher) {
        val repository = FakePlacesRepository()
        repository.responder = { PlacesSearchResult.Failure(PlacesError.ServiceUnavailable) }
        val vm = viewModel(repository)
        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()
        assertEquals(PlacesUiState.Error(PlacesError.ServiceUnavailable), vm.state.value.search)

        repository.responder = { PlacesSearchResult.Success(listOf(hospital)) }
        vm.retrySearch()
        advanceUntilIdle()

        assertEquals(2, repository.requests.size)
        assertEquals(repository.requests[0], repository.requests[1])
        assertEquals(PlacesUiState.Results(listOf(hospital)), vm.state.value.search)
    }

    @Test
    fun selectingCategoryRepeatsSearchWithNewCategory() = runTest(dispatcher) {
        val repository = FakePlacesRepository { PlacesSearchResult.Success(listOf(hospital)) }
        val vm = viewModel(repository)
        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        repository.responder = { PlacesSearchResult.Success(listOf(pharmacy)) }
        vm.selectCategory(PlaceCategory.Pharmacies)
        advanceUntilIdle()

        assertEquals(PlaceCategory.Pharmacies, vm.state.value.category)
        assertEquals(2, repository.requests.size)
        assertEquals(listOf("pharmacy"), repository.requests.last().includedTypes)
        assertEquals(PlacesUiState.Results(listOf(pharmacy)), vm.state.value.search)
    }

    @Test
    fun searchUsesTheCurrentRadius() = runTest(dispatcher) {
        val repository = FakePlacesRepository()
        val radius = MutableStateFlow(2.0)
        val vm = viewModel(repository, radiusKm = radius)
        advanceUntilIdle()

        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(2.0, vm.state.value.radiusKm, 0.0)
        assertEquals(2000.0, repository.requests.single().radiusMeters, 0.0)
    }

    @Test
    fun newSearchCancelsThePreviousOne() = runTest(dispatcher) {
        val repository = FakePlacesRepository()
        var firstCancelled = false
        var calls = 0
        repository.responder = {
            calls++
            if (calls == 1) {
                try {
                    awaitCancellation()
                } finally {
                    firstCancelled = true
                }
            } else {
                PlacesSearchResult.Success(listOf(pharmacy))
            }
        }
        val vm = viewModel(repository)
        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        vm.selectCategory(PlaceCategory.Pharmacies)
        advanceUntilIdle()

        assertTrue("la búsqueda anterior debía cancelarse", firstCancelled)
        assertEquals(PlacesUiState.Results(listOf(pharmacy)), vm.state.value.search)
    }

    @Test
    fun lateResultFromReplacedSearchIsDiscarded() = runTest(dispatcher) {
        val repository = FakePlacesRepository()
        val oldResult = CompletableDeferred<Unit>()
        var calls = 0
        repository.responder = {
            calls++
            if (calls == 1) {
                // Simula un Task que no se puede cancelar: responde igual después de la cancelación.
                withContext(NonCancellable) {
                    oldResult.await()
                    PlacesSearchResult.Success(listOf(hospital))
                }
            } else {
                PlacesSearchResult.Success(listOf(pharmacy))
            }
        }
        val vm = viewModel(repository)
        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        vm.selectCategory(PlaceCategory.Pharmacies)
        advanceUntilIdle()
        assertEquals(PlacesUiState.Results(listOf(pharmacy)), vm.state.value.search)

        oldResult.complete(Unit)
        advanceUntilIdle()

        assertEquals(PlacesUiState.Results(listOf(pharmacy)), vm.state.value.search)
    }

    @Test
    fun missingKeyNeverSearchesAndShowsMissingKey() = runTest(dispatcher) {
        val repository = FakePlacesRepository()
        val vm = viewModel(repository, mapsKeyConfigured = false)
        assertEquals(PlacesUiState.MissingKey, vm.state.value.search)

        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()
        vm.selectCategory(PlaceCategory.Parks)
        vm.retrySearch()
        advanceUntilIdle()

        assertEquals(PlacesUiState.MissingKey, vm.state.value.search)
        assertTrue("no debe haber búsquedas", repository.requests.isEmpty())
    }

    @Test
    fun noLocationMeansInactiveAndNoSearch() = runTest(dispatcher) {
        val repository = FakePlacesRepository()
        val vm = MapViewModel(
            locationProvider = FakeLocationProvider(LocationResult.Unavailable),
            addressResolver = FakeAddressResolver(null),
            placesRepository = repository,
            radiusKm = flowOf(1.0),
            mapsKeyConfigured = true,
        )

        vm.onPermissionResult(LocationPermissionState.Precise)
        advanceUntilIdle()

        assertEquals(PlacesUiState.Inactive, vm.state.value.search)
        assertTrue(repository.requests.isEmpty())
    }
}
