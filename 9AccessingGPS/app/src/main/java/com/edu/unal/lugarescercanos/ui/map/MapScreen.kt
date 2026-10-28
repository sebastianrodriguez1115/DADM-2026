package com.edu.unal.lugarescercanos.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.unal.lugarescercanos.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

/** Plaza de Bolívar: vista inicial mientras llega la ubicación (P6). */
private val VISTA_INICIAL = LatLng(4.5981, -74.0760)
private const val ZOOM_INICIAL = 12f

/** Zoom al centrar el mapa en la ubicación del usuario. */
private const val ZOOM_UBICACION = 15f

/**
 * Pantalla del mapa: el mapa de Google, la tarjeta de ubicación sobre él y el acceso a Ajustes.
 * El marcador, el círculo, la búsqueda y el selector de categoría se agregan en los grupos 5 y 6.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel(factory = MapViewModel.factory())
    val state by viewModel.state.collectAsState()
    val permissionLauncher = rememberLocationPermissionLauncher(viewModel)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(VISTA_INICIAL, ZOOM_INICIAL)
    }
    var mapLoaded by remember { mutableStateOf(false) }
    val point = (state.location as? LocationUiState.Ready)?.point

    // La cámara solo se mueve cuando el mapa ya cargó (la ubicación puede llegar antes).
    LaunchedEffect(point, mapLoaded) {
        if (mapLoaded && point != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), ZOOM_UBICACION),
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.map_settings_description),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = innerPadding,
                onMapLoaded = { mapLoaded = true },
            )
            LocationPanel(
                state = state,
                onGrantPermission = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                onOpenSettings = { openAppSettings(context) },
                onRefresh = viewModel::refreshLocation,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
