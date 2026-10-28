package com.edu.unal.lugarescercanos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edu.unal.lugarescercanos.domain.GeoPoint
import com.edu.unal.lugarescercanos.domain.Place
import com.edu.unal.lugarescercanos.domain.PlaceCategory
import com.edu.unal.lugarescercanos.domain.PlacesError
import com.edu.unal.lugarescercanos.location.LocationPermissionState
import com.edu.unal.lugarescercanos.ui.map.LocationPanel
import com.edu.unal.lugarescercanos.ui.map.LocationUiState
import com.edu.unal.lugarescercanos.ui.map.MapUiState
import com.edu.unal.lugarescercanos.ui.map.PlacesCountLabel
import com.edu.unal.lugarescercanos.ui.map.PlacesStatusCard
import com.edu.unal.lugarescercanos.ui.map.PlacesUiState
import com.edu.unal.lugarescercanos.ui.theme.LugaresCercanosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Estados visibles del mapa (P11) con los paneles sin estado y datos de prueba. */
@RunWith(AndroidJUnit4::class)
class MapStatesTest {
    @get:Rule
    val rule = createComposeRule()

    private val bogota = GeoPoint(4.6381, -74.0840)
    private val places = listOf(
        Place("h1", "Clínica Nicolás de Federman", 4.6390, -74.0840, 100.0),
        Place("h2", "Hospital de prueba", 4.6400, -74.0850, 200.0),
    )

    private fun showLocation(state: MapUiState, onGrant: () -> Unit = {}) {
        rule.setContent {
            LugaresCercanosTheme {
                LocationPanel(
                    state = state,
                    onGrantPermission = onGrant,
                    onOpenSettings = {},
                    onRefresh = {},
                )
            }
        }
    }

    private fun showSearch(state: MapUiState, onRetry: () -> Unit = {}) {
        rule.setContent {
            LugaresCercanosTheme {
                PlacesStatusCard(state = state, onRetry = onRetry)
            }
        }
    }

    private fun showCount(state: MapUiState) {
        rule.setContent {
            LugaresCercanosTheme {
                PlacesCountLabel(state = state)
            }
        }
    }

    @Test
    fun withoutPermissionShowsThePermissionMessageAndButton() {
        var granted = 0
        showLocation(MapUiState(permission = LocationPermissionState.Denied)) { granted++ }

        rule.onNodeWithText("Lugares cercanos necesita tu ubicación para buscar lugares cerca de ti.")
            .assertIsDisplayed()
        rule.onNodeWithText("Conceder permiso").performClick()
        assertEquals(1, granted)
    }

    @Test
    fun connectionErrorShowsMessageAndRetryButton() {
        var retries = 0
        showSearch(MapUiState(search = PlacesUiState.Error(PlacesError.NoConnection))) { retries++ }

        rule.onNodeWithText("Sin conexión a internet").assertIsDisplayed()
        rule.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun everyErrorHasItsExactMessage() {
        showSearch(MapUiState(search = PlacesUiState.Error(PlacesError.OverQueryLimit)))
        rule.onNodeWithText("El servicio de lugares alcanzó su límite. Intenta más tarde").assertIsDisplayed()
    }

    @Test
    fun emptyResultShowsCategoryAndRadius() {
        showSearch(
            MapUiState(
                category = PlaceCategory.Parks,
                radiusKm = 1.0,
                search = PlacesUiState.Empty,
            ),
        )

        rule.onNodeWithText("No se encontraron lugares de «Parques» a menos de 1 km").assertIsDisplayed()
    }

    @Test
    fun missingKeyShowsItsMessage() {
        showSearch(MapUiState(search = PlacesUiState.MissingKey))

        rule.onNodeWithText("Falta configurar la llave de Google Maps").assertIsDisplayed()
    }

    @Test
    fun resultsShowTheCounter() {
        showCount(
            MapUiState(
                location = LocationUiState.Ready(bogota, 5f, null, resolvingAddress = false),
                search = PlacesUiState.Results(places),
            ),
        )

        rule.onNodeWithText("2 lugares").assertIsDisplayed()
    }

    @Test
    fun oneResultShowsTheSingularCounter() {
        showCount(
            MapUiState(
                location = LocationUiState.Ready(bogota, 5f, null, resolvingAddress = false),
                search = PlacesUiState.Results(places.take(1)),
            ),
        )

        rule.onNodeWithText("1 lugar").assertIsDisplayed()
    }

    @Test
    fun refreshButtonIsFoundByItsAccessibleName() {
        var refreshes = 0
        rule.setContent {
            LugaresCercanosTheme {
                LocationPanel(
                    state = MapUiState(
                        permission = LocationPermissionState.Precise,
                        location = LocationUiState.Ready(bogota, 5f, null, resolvingAddress = false),
                    ),
                    onGrantPermission = {},
                    onOpenSettings = {},
                    onRefresh = { refreshes++ },
                )
            }
        }

        rule.onNodeWithContentDescription("Actualizar ubicación").performClick()
        assertEquals(1, refreshes)
    }

    @Test
    fun disabledLocationShowsMessageAndRefreshButtonThatCallsBack() {
        var refreshes = 0
        rule.setContent {
            LugaresCercanosTheme {
                LocationPanel(
                    state = MapUiState(
                        permission = LocationPermissionState.Precise,
                        location = LocationUiState.Disabled,
                    ),
                    onGrantPermission = {},
                    onOpenSettings = {},
                    onRefresh = { refreshes++ },
                )
            }
        }

        rule.onNodeWithText("La ubicación del dispositivo está desactivada.").assertIsDisplayed()
        rule.onNodeWithContentDescription("Actualizar ubicación").performClick()
        assertEquals(1, refreshes)
    }
}
