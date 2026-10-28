package com.edu.unal.lugarescercanos.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.unal.lugarescercanos.R
import com.edu.unal.lugarescercanos.domain.Place
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/** Plaza de Bolívar: vista inicial mientras llega la ubicación (P6). */
private val VISTA_INICIAL = LatLng(4.5981, -74.0760)
private const val ZOOM_INICIAL = 12f

/** Margen del encuadre alrededor del círculo, en dp. */
private val MARGEN_ENCUADRE = 32.dp

/** Separación vertical de los paneles superpuestos y de su margen con el borde. */
private val MARGEN_PANELES = 8.dp

/**
 * Pantalla del mapa: el mapa de Google con la ubicación, el círculo del radio y los lugares;
 * sobre él, la tarjeta de ubicación, el selector de categoría y el estado de la búsqueda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewModel: MapViewModel = viewModel(factory = MapViewModel.factory())
    val state by viewModel.state.collectAsState()
    val permissionLauncher = rememberLocationPermissionLauncher(viewModel)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(VISTA_INICIAL, ZOOM_INICIAL)
    }
    val mapProperties = remember(context) {
        MapProperties(mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style))
    }
    var mapLoaded by remember { mutableStateOf(false) }
    // Alto de los paneles superpuestos, medido en píxeles para el relleno del mapa.
    var overlayHeightPx by remember { mutableIntStateOf(0) }

    val point = (state.location as? LocationUiState.Ready)?.point
    val radiusMeters = state.radiusKm * METERS_PER_KM
    val results = (state.search as? PlacesUiState.Results)?.places
    val paddingPx = with(density) { MARGEN_ENCUADRE.roundToPx() }

    // El encuadre solo se aplica con el mapa cargado (la ubicación puede llegar antes) y
    // cada vez que cambie el punto, el radio o los resultados de la búsqueda.
    LaunchedEffect(point, radiusMeters, results, mapLoaded) {
        if (mapLoaded && point != null) {
            val b = cameraBoundsAround(point, radiusMeters)
            val bounds = LatLngBounds(LatLng(b.south, b.west), LatLng(b.north, b.east))
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
        }
    }

    val overlayDp = with(density) { overlayHeightPx.toDp() }

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
        // El mapa deja libre el área de los paneles para que el círculo se encuadre debajo de ellos.
        // El relleno inferior y lateral lo usa Google para mover su logo (esquina inferior izquierda).
        val mapPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
            top = innerPadding.calculateTopPadding() + MARGEN_PANELES + overlayDp + MARGEN_PANELES,
            bottom = innerPadding.calculateBottomPadding(),
        )
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                contentPadding = mapPadding,
                onMapLoaded = { mapLoaded = true },
            ) {
                if (point != null) {
                    val position = LatLng(point.latitude, point.longitude)
                    Marker(
                        state = rememberUpdatedMarkerState(position = position),
                        title = stringResource(R.string.map_user_marker_title),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    )
                    Circle(
                        center = position,
                        radius = radiusMeters,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = CIRCLE_FILL_ALPHA),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = CIRCLE_STROKE_PX,
                    )
                }
                results?.forEach { place: Place -> PlaceLabel(place) }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = MARGEN_PANELES),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { overlayHeightPx = it.height },
                    verticalArrangement = Arrangement.spacedBy(MARGEN_PANELES),
                ) {
                    LocationPanel(
                        state = state,
                        onGrantPermission = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                        onOpenSettings = { openAppSettings(context) },
                        onRefresh = viewModel::refreshLocation,
                    )
                    CategoryChips(
                        selected = state.category,
                        onSelect = viewModel::selectCategory,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PlacesStatusCard(state = state, onRetry = viewModel::retrySearch)
                }
            }
        }
    }
}

private const val METERS_PER_KM = 1000.0
private const val CIRCLE_FILL_ALPHA = 0.15f
private const val CIRCLE_STROKE_PX = 3f
