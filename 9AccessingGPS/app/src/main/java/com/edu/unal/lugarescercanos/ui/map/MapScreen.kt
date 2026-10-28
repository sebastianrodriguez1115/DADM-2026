package com.edu.unal.lugarescercanos.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.edu.unal.lugarescercanos.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

/** Plaza de Bolívar: vista inicial mientras llega la ubicación (P6). */
private val VISTA_INICIAL = LatLng(4.5981, -74.0760)
private const val ZOOM_INICIAL = 12f

/**
 * Pantalla del mapa. Por ahora muestra solo el mapa de Google centrado en la Plaza de Bolívar,
 * bajo la barra superior con el acceso a Ajustes; el marcador, el círculo, la búsqueda y los
 * estados se agregan en los grupos 4 a 6.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(VISTA_INICIAL, ZOOM_INICIAL)
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
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = innerPadding,
        )
    }
}
