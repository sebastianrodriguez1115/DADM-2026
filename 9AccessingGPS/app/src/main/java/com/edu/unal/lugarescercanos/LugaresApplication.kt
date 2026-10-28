package com.edu.unal.lugarescercanos

import android.app.Application
import android.content.Context
import com.edu.unal.lugarescercanos.data.DataStoreRadiusPreferences
import com.edu.unal.lugarescercanos.data.RadiusPreferences
import com.edu.unal.lugarescercanos.data.places.MapsKey
import com.edu.unal.lugarescercanos.data.places.PlacesRepository
import com.edu.unal.lugarescercanos.data.places.PlacesSdkPlacesRepository
import com.edu.unal.lugarescercanos.location.AddressResolver
import com.edu.unal.lugarescercanos.location.FusedLocationProvider
import com.edu.unal.lugarescercanos.location.GeocoderAddressResolver
import com.edu.unal.lugarescercanos.location.LocationProvider
import com.google.android.libraries.places.api.Places

/**
 * Contenedor de dependencias de la app (D9). [mapsKeyConfigured] es false si la llave es
 * `DEFAULT_API_KEY`; en ese caso no se busca lugares.
 */
class AppContainer(context: Context) {
    val radiusPreferences: RadiusPreferences = DataStoreRadiusPreferences.fromContext(context)
    val locationProvider: LocationProvider = FusedLocationProvider(context)
    val addressResolver: AddressResolver = GeocoderAddressResolver(context)
    val placesRepository: PlacesRepository = PlacesSdkPlacesRepository(context)
    val mapsKeyConfigured: Boolean = MapsKey.isConfigured(BuildConfig.MAPS_API_KEY)
}

/** Clase Application: crea una sola vez el [AppContainer] y Places del proceso (D5). */
class LugaresApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (MapsKey.isConfigured(BuildConfig.MAPS_API_KEY)) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
        }
        container = AppContainer(this)
    }
}
