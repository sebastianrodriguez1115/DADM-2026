package com.edu.unal.lugarescercanos.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.edu.unal.lugarescercanos.R
import com.edu.unal.lugarescercanos.location.CoordinateFormatter
import com.edu.unal.lugarescercanos.location.LocationPermissionState

/**
 * Tarjeta sobre el mapa con el permiso, el estado de la lectura y la tarjeta «Tu ubicación».
 * Sin estado propio: recibe el estado y los callbacks.
 */
@Composable
fun LocationPanel(
    state: MapUiState,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.permission) {
        null -> Unit
        LocationPermissionState.Denied -> PermissionCard(
            buttonText = stringResource(R.string.location_grant_permission),
            onButton = onGrantPermission,
            modifier = modifier,
        )
        LocationPermissionState.PermanentlyDenied -> PermissionCard(
            buttonText = stringResource(R.string.location_open_settings),
            onButton = onOpenSettings,
            modifier = modifier,
        )
        LocationPermissionState.Precise,
        LocationPermissionState.Approximate -> LocationCard(state, onRefresh, modifier)
    }
}

@Composable
private fun PermissionCard(buttonText: String, onButton: () -> Unit, modifier: Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.location_permission_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onButton, modifier = Modifier.padding(top = 4.dp)) {
                Text(buttonText)
            }
        }
    }
}

/**
 * Tarjeta compacta «Tu ubicación»: una línea con el título y la dirección, las coordenadas en
 * texto pequeño y, a la derecha, el botón de actualizar (`places-map`, «Actualizar la ubicación»).
 */
@Composable
private fun LocationCard(state: MapUiState, onRefresh: () -> Unit, modifier: Modifier) {
    val canRefresh = when (state.location) {
        is LocationUiState.Ready, LocationUiState.Unavailable -> true
        LocationUiState.Idle, LocationUiState.Loading, LocationUiState.Disabled -> false
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                LocationText(state)
            }
            if (canRefresh) {
                RefreshButton(onRefresh)
            }
        }
    }
}

@Composable
private fun LocationText(state: MapUiState) {
    when (val location = state.location) {
        LocationUiState.Idle -> Unit
        LocationUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                stringResource(R.string.location_loading),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        LocationUiState.Disabled -> Text(
            stringResource(R.string.location_disabled),
            style = MaterialTheme.typography.bodyMedium,
        )
        LocationUiState.Unavailable -> Text(
            stringResource(R.string.location_unavailable),
            style = MaterialTheme.typography.bodyMedium,
        )
        is LocationUiState.Ready -> {
            val title = stringResource(R.string.location_card_title)
            val address = addressText(location)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(title) }
                    append(" · ")
                    append(address)
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    R.string.location_coordinates,
                    CoordinateFormatter.formatPoint(location.point),
                ),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.isApproximate) {
                Text(
                    stringResource(R.string.location_approximate_notice),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun addressText(location: LocationUiState.Ready): String = when {
    location.resolvingAddress -> stringResource(R.string.location_address_resolving)
    location.address == null -> stringResource(R.string.location_address_unavailable)
    else -> location.address
}

/** Botón de actualizar con icono; el nombre accesible es «Actualizar ubicación». */
@Composable
private fun RefreshButton(onRefresh: () -> Unit) {
    IconButton(onClick = onRefresh) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.location_update),
        )
    }
}
