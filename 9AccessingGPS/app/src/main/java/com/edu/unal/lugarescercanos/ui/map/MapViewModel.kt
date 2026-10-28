package com.edu.unal.lugarescercanos.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.edu.unal.lugarescercanos.LugaresApplication
import com.edu.unal.lugarescercanos.domain.GeoPoint
import com.edu.unal.lugarescercanos.location.AddressResolver
import com.edu.unal.lugarescercanos.location.LocationPermissionState
import com.edu.unal.lugarescercanos.location.LocationProvider
import com.edu.unal.lugarescercanos.location.LocationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** Estado de la pantalla del mapa. La búsqueda de lugares se agrega en el grupo 5. */
data class MapUiState(
    /** Null hasta que la pantalla consulta o pide el permiso. */
    val permission: LocationPermissionState? = null,
    val location: LocationUiState = LocationUiState.Idle,
) {
    val hasLocationPermission: Boolean get() = permission?.isGranted == true

    /** True cuando solo se concedió la ubicación aproximada. */
    val isApproximate: Boolean get() = permission == LocationPermissionState.Approximate
}

/**
 * ViewModel del mapa. Lee la ubicación una vez al conceder el permiso, busca su dirección
 * y la vuelve a leer con «Actualizar ubicación» (D9, `current-location`).
 */
class MapViewModel(
    private val locationProvider: LocationProvider,
    private val addressResolver: AddressResolver,
) : ViewModel() {
    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private var locationJob: Job? = null

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
                LocationResult.LocationDisabled -> setLocation(LocationUiState.Disabled)
                LocationResult.Unavailable -> setLocation(LocationUiState.Unavailable)
            }
        }
    }

    private suspend fun showPoint(result: LocationResult.Success) {
        setLocation(LocationUiState.Ready(result.point, result.accuracyMeters, null, resolvingAddress = true))
        val address = addressResolver.addressFor(result.point)
        setLocation(LocationUiState.Ready(result.point, result.accuracyMeters, address, resolvingAddress = false))
    }

    private fun setLocation(location: LocationUiState) {
        _state.update { it.copy(location = location) }
    }

    companion object {
        /** Fábrica manual, sin framework de inyección (como en el taller 8). */
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = containerFrom(this)
                MapViewModel(container.locationProvider, container.addressResolver)
            }
        }

        private fun containerFrom(extras: CreationExtras) =
            (extras[APPLICATION_KEY] as LugaresApplication).container
    }
}
