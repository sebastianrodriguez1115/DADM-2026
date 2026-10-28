package com.edu.unal.lugarescercanos

import android.app.Application
import android.content.Context
import com.edu.unal.lugarescercanos.data.DataStoreRadiusPreferences
import com.edu.unal.lugarescercanos.data.RadiusPreferences

/**
 * Contenedor de dependencias de la app (D9). El repositorio de lugares se agrega en el grupo 5.
 */
class AppContainer(context: Context) {
    val radiusPreferences: RadiusPreferences = DataStoreRadiusPreferences.fromContext(context)
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
