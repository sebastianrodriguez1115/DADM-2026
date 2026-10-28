package com.edu.unal.lugarescercanos.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.location_permission_message))
            Button(onClick = onButton, modifier = Modifier.padding(top = 8.dp)) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun LocationCard(state: MapUiState, onRefresh: () -> Unit, modifier: Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (val location = state.location) {
                LocationUiState.Idle -> Unit
                LocationUiState.Loading -> Row {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        stringResource(R.string.location_loading),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                LocationUiState.Disabled -> Text(stringResource(R.string.location_disabled))
                LocationUiState.Unavailable -> {
                    Text(stringResource(R.string.location_unavailable))
                    RefreshButton(onRefresh)
                }
                is LocationUiState.Ready -> {
                    Text(
                        stringResource(R.string.location_card_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(addressText(location), modifier = Modifier.padding(top = 4.dp))
                    Text(
                        stringResource(
                            R.string.location_coordinates,
                            CoordinateFormatter.formatPoint(location.point),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (state.isApproximate) {
                        Text(
                            stringResource(R.string.location_approximate_notice),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    RefreshButton(onRefresh)
                }
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

@Composable
private fun RefreshButton(onRefresh: () -> Unit) {
    TextButton(onClick = onRefresh) {
        Text(stringResource(R.string.location_update))
    }
}
