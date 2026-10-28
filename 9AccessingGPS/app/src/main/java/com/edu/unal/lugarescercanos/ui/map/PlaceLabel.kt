package com.edu.unal.lugarescercanos.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.edu.unal.lugarescercanos.domain.Place
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberUpdatedMarkerState

/** Longitud máxima del nombre en la etiqueta, en caracteres (`places-map`, P7). */
const val LABEL_MAX_CHARS = 28

/**
 * Nombre para la etiqueta: hasta [LABEL_MAX_CHARS] caracteres; si es más largo, se corta y se
 * agrega «…». Cuenta puntos de código, para no partir un carácter compuesto.
 */
fun labelText(name: String): String {
    if (name.codePointCount(0, name.length) <= LABEL_MAX_CHARS) return name
    val end = name.offsetByCodePoints(0, LABEL_MAX_CHARS)
    return name.substring(0, end) + "…"
}

/**
 * Marcador de un lugar con su nombre siempre visible (D7). La etiqueta va encima de un pin y
 * el ancla (0,5; 1,0) cae en la punta del pin, que es la posición del lugar. `title` y
 * `snippet` muestran el nombre completo y la distancia al tocarlo.
 */
@Composable
fun PlaceLabel(place: Place) {
    val markerState = rememberUpdatedMarkerState(position = LatLng(place.latitude, place.longitude))
    MarkerComposable(
        keys = arrayOf(place.id, place.name),
        state = markerState,
        title = place.name,
        snippet = formatDistance(place.distanceMeters),
        anchor = Offset(0.5f, 1f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = labelText(place.name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
