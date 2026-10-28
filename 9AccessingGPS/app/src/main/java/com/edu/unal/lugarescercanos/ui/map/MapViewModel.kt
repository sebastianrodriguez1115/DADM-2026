package com.edu.unal.lugarescercanos.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.edu.unal.lugarescercanos.LugaresApplication
import com.edu.unal.lugarescercanos.data.places.PlacesRepository
import com.edu.unal.lugarescercanos.data.places.PlacesSearchResult
import com.edu.unal.lugarescercanos.domain.GeoPoint
import com.edu.unal.lugarescercanos.domain.NearbySearchParams
import com.edu.unal.lugarescercanos.domain.Place
import com.edu.unal.lugarescercanos.domain.PlaceCategory
import com.edu.unal.lugarescercanos.domain.PlacesError
import com.edu.unal.lugarescercanos.domain.RadiusRules
import com.edu.unal.lugarescercanos.location.AddressResolver
import com.edu.unal.lugarescercanos.location.LocationPermissionState
import com.edu.unal.lugarescercanos.location.LocationProvider
import com.edu.unal.lugarescercanos.location.LocationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la lectura de la ubicación (`current-location`). */
sealed interface LocationUiState {
    /** Todavía no se ha leído (sin permiso o antes de la primera lectura). */
    data object Idle : LocationUiState

    data object Loading : LocationUiState

    /**
     * Posición leída. [address] es null cuando no hay dirección; mientras [resolvingAddress] es true
     * la dirección todavía se busca.
     */
    data class Ready(
        val point: GeoPoint,
        val accuracyMeters: Float?,
        val address: String?,
        val resolvingAddress: Boolean,
    ) : LocationUiState

    data object Disabled : LocationUiState

    data object Unavailable : LocationUiState
}

/** Estado de la búsqueda de lugares (P11, `nearby-places-search`). */
sealed interface PlacesUiState {
    /** Todavía no hay ubicación, así que no hay búsqueda. */
    data object Inactive : PlacesUiState

    data object Loading : PlacesUiState

    data class Results(val places: List<Place>) : PlacesUiState

    /** La búsqueda terminó sin lugares con nombre dentro del radio. */
    data object Empty : PlacesUiState

    data class Error(val error: PlacesError) : PlacesUiState

    /** La llave es `DEFAULT_API_KEY`: no se busca (D11). */
    data object MissingKey : PlacesUiState
}

/** Estado de la pantalla del mapa. */
data class MapUiState(
    /** Null hasta que la pantalla consulta o pide el permiso. */
    val permission: LocationPermissionState? = null,
    val location: LocationUiState = LocationUiState.Idle,
    val category: PlaceCategory = PlaceCategory.DEFAULT,
    val radiusKm: Double = RadiusRules.DEFAULT_KM,
    val search: PlacesUiState = PlacesUiState.Inactive,
) {
    val hasLocationPermission: Boolean get() = permission?.isGranted == true

    /** True cuando solo se concedió la ubicación aproximada. */
    val isApproximate: Boolean get() = permission == LocationPermissionState.Approximate
}

/**
 * ViewModel del mapa. Lee la ubicación una vez al conceder el permiso, busca su dirección
 * y la vuelve a leer con «Actualizar ubicación» (D9, `current-location`). Con la ubicación lista
 * busca los lugares de la categoría y el radio actuales (D9, `nearby-places-search`).
 *
 * La búsqueda corre en [searchJob]. Cada búsqueda nueva cancela la anterior y lleva un
 * identificador: un resultado con un identificador viejo se descarta aunque llegue.
 */
class MapViewModel(
    private val locationProvider: LocationProvider,
    private val addressResolver: AddressResolver,
    private val placesRepository: PlacesRepository = NotConfiguredPlacesRepository,
    radiusKm: Flow<Double> = flowOf(RadiusRules.DEFAULT_KM),
    private val mapsKeyConfigured: Boolean = false,
) : ViewModel() {
    private val _state = MutableStateFlow(
        MapUiState(search = if (mapsKeyConfigured) PlacesUiState.Inactive else PlacesUiState.MissingKey),
    )
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private var locationJob: Job? = null
    private var searchJob: Job? = null
    private var latestSearchId = 0L

    /** Radio de la última búsqueda iniciada; null mientras no se ha buscado (`search-radius-settings`). */
    private var lastSearchedRadiusKm: Double? = null

    /**
     * False hasta la primera emisión del radio guardado. Así la primera búsqueda ya usa el radio
     * de Ajustes y no el de por defecto (1 km) (`search-radius-settings`).
     */
    private var radiusLoaded = false

    init {
        viewModelScope.launch {
            radiusKm.collect { km ->
                val changed = km != lastSearchedRadiusKm
                _state.update { it.copy(radiusKm = km) }
                radiusLoaded = true
                // Radio distinto del último buscado: se repite la búsqueda si hay ubicación y llave.
                if (changed && _state.value.location is LocationUiState.Ready) requestSearch()
            }
        }
    }

    /** Cambia la categoría y repite la búsqueda con la ubicación actual. */
    fun selectCategory(category: PlaceCategory) {
        _state.update { it.copy(category = category) }
        requestSearch()
    }

    /** Repite la búsqueda con la misma categoría y el mismo radio («Reintentar»). */
    fun retrySearch() {
        requestSearch()
    }

    /** La pantalla llama aquí con el resultado del permiso. Lee la ubicación la primera vez que hay permiso. */
    fun onPermissionResult(permission: LocationPermissionState) {
        _state.update { it.copy(permission = permission) }
        if (permission.isGranted && _state.value.location == LocationUiState.Idle) {
            refreshLocation()
        }
    }

    /** Lee la ubicación de nuevo y después su dirección. Una lectura nueva cancela la anterior. */
    fun refreshLocation() {
        if (!_state.value.hasLocationPermission) return
        locationJob?.cancel()
        _state.update { it.copy(location = LocationUiState.Loading) }
        locationJob = viewModelScope.launch {
            when (val result = locationProvider.currentLocation()) {
                is LocationResult.Success -> showPoint(result)
                LocationResult.LocationDisabled -> {
                    setLocation(LocationUiState.Disabled)
                    requestSearch()
                }
                LocationResult.Unavailable -> {
                    setLocation(LocationUiState.Unavailable)
                    requestSearch()
                }
            }
        }
    }

    /**
     * La pantalla llama aquí al volver a primer plano, con el permiso actual. Si el permiso cambió
     * lo registra. Si hay permiso y la ubicación estaba desactivada o no se obtuvo, la relee una
     * vez (D1). Con la ubicación ya lista no relee, para no repetir la búsqueda al volver de Ajustes.
     */
    fun onAppResumed(permission: LocationPermissionState) {
        if (permission != _state.value.permission) onPermissionResult(permission)
        if (permission.isGranted && _state.value.location.canRecoverOnResume) refreshLocation()
    }

    private suspend fun showPoint(result: LocationResult.Success) {
        setLocation(LocationUiState.Ready(result.point, result.accuracyMeters, null, resolvingAddress = true))
        // La búsqueda empieza con la posición, sin esperar a la dirección.
        requestSearch()
        val address = addressResolver.addressFor(result.point)
        setLocation(LocationUiState.Ready(result.point, result.accuracyMeters, address, resolvingAddress = false))
    }

    private fun setLocation(location: LocationUiState) {
        _state.update { it.copy(location = location) }
    }

    /**
     * Decide el estado de la búsqueda: sin llave, «llave faltante» sin buscar; sin ubicación lista,
     * inactiva; si no, busca con la ubicación, la categoría y el radio actuales.
     */
    private fun requestSearch() {
        if (!mapsKeyConfigured) {
            cancelSearch()
            setSearch(PlacesUiState.MissingKey)
            return
        }
        val ready = _state.value.location as? LocationUiState.Ready
        if (ready == null) {
            cancelSearch()
            setSearch(PlacesUiState.Inactive)
            return
        }
        // Sin el radio guardado no se busca: la primera emisión del radio lanza la búsqueda.
        if (!radiusLoaded) {
            setSearch(PlacesUiState.Loading)
            return
        }
        startSearch(ready.point)
    }

    private fun startSearch(point: GeoPoint) {
        searchJob?.cancel()
        val searchId = ++latestSearchId
        val current = _state.value
        val params = NearbySearchParams.create(point, current.radiusKm, current.category)
        lastSearchedRadiusKm = current.radiusKm
        setSearch(PlacesUiState.Loading)
        searchJob = viewModelScope.launch {
            val result = placesRepository.searchNearby(params)
            // Resultado de una búsqueda ya reemplazada o cancelada: se descarta.
            if (searchId != latestSearchId) return@launch
            setSearch(result.toUiState())
        }
    }

    /** Cancela la búsqueda en curso y descarta cualquier resultado que llegue después. */
    private fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        latestSearchId++
    }

    private fun setSearch(search: PlacesUiState) {
        _state.update { it.copy(search = search) }
    }

    private fun PlacesSearchResult.toUiState(): PlacesUiState = when (this) {
        is PlacesSearchResult.Success ->
            if (places.isEmpty()) PlacesUiState.Empty else PlacesUiState.Results(places)
        is PlacesSearchResult.Failure -> PlacesUiState.Error(error)
    }

    companion object {
        /** Fábrica manual, sin framework de inyección (como en el taller 8). */
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = containerFrom(this)
                MapViewModel(
                    locationProvider = container.locationProvider,
                    addressResolver = container.addressResolver,
                    placesRepository = container.placesRepository,
                    radiusKm = container.radiusPreferences.radiusKm,
                    mapsKeyConfigured = container.mapsKeyConfigured,
                )
            }
        }

        private fun containerFrom(extras: CreationExtras) =
            (extras[APPLICATION_KEY] as LugaresApplication).container
    }
}

/** Estados que se vuelven a leer al regresar a primer plano (D1, `current-location`). */
private val LocationUiState.canRecoverOnResume: Boolean
    get() = this == LocationUiState.Disabled || this == LocationUiState.Unavailable

/**
 * Repositorio por defecto del constructor (para las pruebas de ubicación). Falla si se usa: con
 * `mapsKeyConfigured = false` el ViewModel nunca busca, así que no debería llamarse.
 */
private object NotConfiguredPlacesRepository : PlacesRepository {
    override suspend fun searchNearby(params: NearbySearchParams): PlacesSearchResult =
        error("No hay repositorio de lugares configurado")
}
