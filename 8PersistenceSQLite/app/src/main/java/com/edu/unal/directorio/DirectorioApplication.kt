package com.edu.unal.directorio

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.edu.unal.directorio.data.CompanyRepository
import com.edu.unal.directorio.data.RoomCompanyRepository
import com.edu.unal.directorio.data.db.DirectorioDatabase

/**
 * Creates the file database and the repository once per process and exposes them (design D6).
 * Use [DirectorioApplication.container] from the UI layer.
 */
class AppContainer(private val context: Context) {
    private class State(val db: DirectorioDatabase, val repository: CompanyRepository)

    @Volatile
    private var state: State = newState()

    /** The repository every ViewModel reads when it is created. */
    val companyRepository: CompanyRepository
        get() = state.repository

    private fun newState(): State {
        val db = DirectorioDatabase.create(context)
        return State(db, RoomCompanyRepository(db))
    }

    /**
     * Tests only: closes the database, deletes its file and rebuilds the database and the
     * repository, so the creation callback loads the 8 sample companies again on first use.
     * Anything holding the previous repository must be discarded (new ViewModels read the new one).
     */
    @VisibleForTesting
    @Synchronized
    fun resetForTests() {
        state.db.close()
        context.deleteDatabase(DirectorioDatabase.FILE_NAME)
        state = newState()
    }
}

/** Application class: owns the single [AppContainer] of the process. */
class DirectorioApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
