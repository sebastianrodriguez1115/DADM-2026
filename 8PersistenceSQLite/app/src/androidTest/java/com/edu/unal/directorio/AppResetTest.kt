package com.edu.unal.directorio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edu.unal.directorio.domain.CompanyFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Tests of the app database reset helpers (task 3.7). */
@RunWith(AndroidJUnit4::class)
class AppResetTest {
    private fun repository() =
        ((InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)
            as DirectorioApplication).container.companyRepository

    private fun count(): Int = runBlocking { repository().observeCompanies(CompanyFilter()).first().size }

    @Test
    fun resetAppDatabase_startsWithEightSamples() {
        resetAppDatabase()
        assertEquals(8, count())
    }

    @Test
    fun resetAppDatabase_afterDeletingOne_bringsBackEight() {
        resetAppDatabase()
        runBlocking {
            val first = repository().observeCompanies(CompanyFilter()).first().first()
            repository().delete(first.id)
        }
        assertEquals(7, count())
        resetAppDatabase()
        assertEquals(8, count())
    }

    @Test
    fun resetAppDatabaseEmpty_leavesNoCompanies() {
        resetAppDatabaseEmpty()
        assertEquals(0, count())
    }
}
