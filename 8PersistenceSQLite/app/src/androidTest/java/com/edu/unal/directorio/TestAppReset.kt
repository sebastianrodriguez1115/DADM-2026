package com.edu.unal.directorio

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.edu.unal.directorio.domain.CompanyFilter

private fun container(): AppContainer {
    val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    return (app as DirectorioApplication).container
}

/** Resets the app database; the app then starts with the 8 sample companies (design D10). */
fun resetAppDatabase() {
    container().resetForTests()
}

/** Resets the app database and then deletes every company through the repository. */
fun resetAppDatabaseEmpty() {
    val container = container()
    container.resetForTests()
    val repository = container.companyRepository
    runBlocking {
        repository.observeCompanies(CompanyFilter()).first().forEach { repository.delete(it.id) }
    }
}
