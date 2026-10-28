package com.edu.unal.lugarescercanos

import android.app.Application
import android.content.Context
import com.edu.unal.lugarescercanos.data.DataStoreRadiusPreferences
import com.edu.unal.lugarescercanos.data.RadiusPreferences
import com.edu.unal.lugarescercanos.location.AddressResolver
import com.edu.unal.lugarescercanos.location.FusedLocationProvider
import com.edu.unal.lugarescercanos.location.GeocoderAddressResolver
import com.edu.unal.lugarescercanos.location.LocationProvider

/**
 * Contenedor de dependencias de la app (D9). El repositorio de lugares se agrega en el grupo 5.
 */
class AppContainer(context: Context) {
    val radiusPreferences: RadiusPreferences = DataStoreRadiusPreferences.fromContext(context)
    val locationProvider: LocationProvider = FusedLocationProvider(context)
    val addressResolver: AddressResolver = GeocoderAddressResolver(context)
}

/** Clase Application: crea una sola vez el [AppContainer] del proceso. */
class LugaresApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
