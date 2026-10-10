package com.edu.unal.tictactoe

import android.app.Application
import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.tictactoe.ui.game.GameViewModel
import com.edu.unal.tictactoe.ui.game.gameViewModelFactory

/**
 * Stand-in for a new Activity instance after process death: a fresh lifecycle, a fresh
 * [ViewModelStore] and a fresh [SavedStateRegistryController] restored from [savedState].
 * It exposes the same extras `ComponentActivity` provides, so the production
 * [gameViewModelFactory] builds the ViewModel and its SavedStateHandle through the framework.
 * Must be created, used and disposed on the main thread.
 */
internal class RestoredSessionHost(savedState: Bundle?) :
    SavedStateRegistryOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val factory: ViewModelProvider.Factory = gameViewModelFactory()
    private val extras: CreationExtras

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory get() = factory
    override val defaultViewModelCreationExtras: CreationExtras get() = extras

    init {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        extras = MutableCreationExtras().apply {
            set(APPLICATION_KEY, application)
            set(SAVED_STATE_REGISTRY_OWNER_KEY, this@RestoredSessionHost)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@RestoredSessionHost)
        }
        controller.performAttach()
        enableSavedStateHandles()
        controller.performRestore(savedState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /** Same call and default key as `MainActivity`. */
    fun createViewModel(): GameViewModel =
        ViewModelProvider(viewModelStore, defaultViewModelProviderFactory, defaultViewModelCreationExtras)[
            GameViewModel::class.java,
        ]

    fun dispose() {
        viewModelStore.clear()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

/** Writes [bundle] to a real [Parcel] and reads it back, as the system does across process death. */
internal fun parcelRoundTrip(bundle: Bundle, classLoader: ClassLoader?): Bundle {
    val parcel = Parcel.obtain()
    try {
        parcel.writeBundle(bundle)
        parcel.setDataPosition(0)
        return checkNotNull(parcel.readBundle(classLoader)) { "Parcel returned a null Bundle" }
    } finally {
        parcel.recycle()
    }
}
