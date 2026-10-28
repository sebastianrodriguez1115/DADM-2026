package com.edu.unal.lugarescercanos.ui.map

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edu.unal.lugarescercanos.R
import com.edu.unal.lugarescercanos.domain.PlaceCategory

/** Nombre visible de cada categoría (`nearby-places-search`, en español). */
@StringRes
fun PlaceCategory.labelRes(): Int = when (this) {
    PlaceCategory.HospitalsAndClinics -> R.string.category_hospitals
    PlaceCategory.Pharmacies -> R.string.category_pharmacies
    PlaceCategory.Restaurants -> R.string.category_restaurants
    PlaceCategory.TouristSites -> R.string.category_tourist_sites
    PlaceCategory.Parks -> R.string.category_parks
}

/**
 * Fila desplazable de categorías con selección única (`places-map`, «Selector de categoría»).
 * Sin estado propio: recibe la categoría elegida y el callback.
 */
@Composable
fun CategoryChips(
    selected: PlaceCategory,
    onSelect: (PlaceCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
    ) {
        PlaceCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.labelRes())) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
