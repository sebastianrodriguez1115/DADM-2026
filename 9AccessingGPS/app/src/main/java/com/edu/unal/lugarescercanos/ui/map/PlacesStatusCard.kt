package com.edu.unal.lugarescercanos.ui.map

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edu.unal.lugarescercanos.R
import com.edu.unal.lugarescercanos.domain.PlacesError
import com.edu.unal.lugarescercanos.domain.RadiusRules

/** Mensaje exacto de cada error de búsqueda (`nearby-places-search`, «Errores de la búsqueda»). */
@StringRes
fun PlacesError.messageRes(): Int = when (this) {
    PlacesError.NoConnection -> R.string.places_error_no_connection
    PlacesError.OverQueryLimit -> R.string.places_error_over_limit
    PlacesError.RequestDenied -> R.string.places_error_request_denied
    PlacesError.ServiceUnavailable -> R.string.places_error_service
}

/**
 * Línea compacta bajo la fila de categorías, sin tarjeta: carga de lugares y contador
 * «1 lugar» o «<n> lugares» (`places-map`, P11 y U14). Con cualquier otro estado no muestra nada.
 */
@Composable
fun PlacesCountLabel(state: MapUiState, modifier: Modifier = Modifier) {
    when (val search = state.search) {
        PlacesUiState.Loading -> InlineLabel(modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(R.string.places_loading),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        is PlacesUiState.Results -> InlineLabel(modifier) {
            val count = search.places.size
            Text(
                pluralStringResource(R.plurals.places_count, count, count),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        PlacesUiState.Inactive,
        PlacesUiState.Empty,
        is PlacesUiState.Error,
        PlacesUiState.MissingKey -> Unit
    }
}

/**
 * Estados de excepción de la búsqueda sobre el mapa, en tarjeta: sin resultados, error con
 * «Reintentar» o llave faltante (`places-map`, P11). Sin estado propio. Los demás estados no muestran nada.
 */
@Composable
fun PlacesStatusCard(
    state: MapUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val search = state.search) {
        PlacesUiState.Inactive,
        PlacesUiState.Loading,
        is PlacesUiState.Results -> Unit
        PlacesUiState.Empty -> StatusCard(modifier) {
            Text(
                stringResource(
                    R.string.places_empty,
                    stringResource(state.category.labelRes()),
                    RadiusRules.format(state.radiusKm),
                ),
            )
        }
        is PlacesUiState.Error -> StatusCard(modifier) {
            Text(stringResource(search.error.messageRes()))
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.places_retry))
            }
        }
        PlacesUiState.MissingKey -> StatusCard(modifier) {
            Text(stringResource(R.string.maps_missing_key))
        }
    }
}

@Composable
private fun StatusCard(modifier: Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

/** Fondo translúcido y redondeado para que el texto se lea sobre el mapa, sin ser una tarjeta. */
@Composable
private fun InlineLabel(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        content()
    }
}
