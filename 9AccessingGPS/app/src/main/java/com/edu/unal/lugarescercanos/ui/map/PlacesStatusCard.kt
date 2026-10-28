package com.edu.unal.lugarescercanos.ui.map

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * Estado de la búsqueda sobre el mapa: carga, contador, sin resultados, error con «Reintentar»
 * o llave faltante (`places-map`, P11). Sin estado propio. Con `Inactive` no muestra nada.
 */
@Composable
fun PlacesStatusCard(
    state: MapUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val search = state.search
    if (search == PlacesUiState.Inactive) return
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (search) {
                PlacesUiState.Inactive -> Unit
                PlacesUiState.Loading -> Row {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.places_loading),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                is PlacesUiState.Results -> Text(
                    stringResource(R.string.places_count, search.places.size),
                    style = MaterialTheme.typography.titleSmall,
                )
                PlacesUiState.Empty -> Text(
                    stringResource(
                        R.string.places_empty,
                        stringResource(state.category.labelRes()),
                        RadiusRules.format(state.radiusKm),
                    ),
                )
                is PlacesUiState.Error -> Column {
                    Text(stringResource(search.error.messageRes()))
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.places_retry))
                    }
                }
                PlacesUiState.MissingKey -> Text(stringResource(R.string.maps_missing_key))
            }
        }
    }
}
