package com.edu.unal.tictactoe

import android.app.Application
import android.os.Bundle
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
import com.edu.unal.tictactoe.ui.navigation.NavigationViewModel
import com.edu.unal.tictactoe.ui.navigation.navigationViewModelFactory

/**
 * Stand-in for a new Activity instance after process death, for [NavigationViewModel]: a fresh
 * lifecycle, a fresh [ViewModelStore] and a fresh [SavedStateRegistryController] restored from
 * [savedState]. It exposes the same extras `ComponentActivity` provides, so the production
 * [navigationViewModelFactory] builds the ViewModel and its SavedStateHandle through the
 * framework. [saveState] writes this host's registry the way an Activity does before it dies.
 * Must be created, used and disposed on the main thread.
 */
internal class RestoredNavigationHost(savedState: Bundle?) :
    SavedStateRegistryOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val factory: ViewModelProvider.Factory = navigationViewModelFactory()
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
            set(SAVED_STATE_REGISTRY_OWNER_KEY, this@RestoredNavigationHost)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@RestoredNavigationHost)
        }
        controller.performAttach()
        enableSavedStateHandles()
        controller.performRestore(savedState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /** Same call and default key as `MainActivity`. */
    fun createViewModel(): NavigationViewModel =
        ViewModelProvider(viewModelStore, defaultViewModelProviderFactory, defaultViewModelCreationExtras)[
            NavigationViewModel::class.java,
        ]

    /** Writes this host's saved state into a new Bundle, then sends it through a real Parcel. */
    fun saveState(): Bundle {
        val out = Bundle()
        controller.performSave(out)
        return parcelRoundTrip(out, NavigationViewModel::class.java.classLoader)
    }

    fun dispose() {
        viewModelStore.clear()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
