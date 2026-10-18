package com.edu.unal.directorio.ui.detail

import com.edu.unal.directorio.data.FakeCompanyRepository
import com.edu.unal.directorio.domain.Classification.Consulting
import com.edu.unal.directorio.domain.Classification.SoftwareFactory
import com.edu.unal.directorio.domain.CompanyDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanyDetailViewModelTest {
    private lateinit var repo: FakeCompanyRepository
    private lateinit var vm: CompanyDetailViewModel

    // The ViewModel is built after setMain: viewModelScope captures Dispatchers.Main when created.
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        repo = FakeCompanyRepository(listOf(acme, beta))
        vm = CompanyDetailViewModel(repo)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val acme = CompanyDraft(
        name = "Acme Software",
        email = "ventas@acme.example.com",
        phone = "601 555 0123",
        products = "Uno\nDos\nTres",
        classifications = setOf(Consulting, SoftwareFactory),
    )
    private val beta = acme.copy(name = "Beta SAS", email = "beta@example.com")

    /** Keeps [state] subscribed (as a visible screen would) and collects the events. */
    private fun TestScope.subscribe(): List<CompanyDetailEvent> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        val events = mutableListOf<CompanyDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }
        return events
    }

    private fun loaded() = (vm.state.value.content as CompanyDetailContent.Loaded).company

    @Test
    fun startsLoadingNotNotFound() = runTest {
        val events = subscribe()
        advanceUntilIdle()
        assertEquals(CompanyDetailContent.Loading, vm.state.value.content)
        assertTrue(events.isEmpty())
    }

    @Test
    fun showsTheCompanyData() = runTest {
        subscribe()
        vm.open(1)
        advanceUntilIdle()
        assertEquals("Acme Software", loaded().name)
        assertEquals(setOf(Consulting, SoftwareFactory), loaded().classifications)
        assertFalse(vm.state.value.deleteDialogVisible)
        assertFalse(vm.state.value.deleteFailed)
    }

    @Test
    fun requestDeleteOpensTheDialogWithoutDeleting() = runTest {
        subscribe()
        vm.open(1)
        advanceUntilIdle()
        vm.requestDelete()
        advanceUntilIdle()
        assertTrue(vm.state.value.deleteDialogVisible)
        assertEquals(2, repo.current.size)
    }

    @Test
    fun dismissClosesTheDialogAndKeepsTheCompany() = runTest {
        val events = subscribe()
        vm.open(1)
        vm.requestDelete()
        vm.dismissDelete()
        advanceUntilIdle()
        assertFalse(vm.state.value.deleteDialogVisible)
        assertEquals(2, repo.current.size)
        assertTrue(events.isEmpty())
        assertEquals("Acme Software", loaded().name)
    }

    @Test
    fun confirmDeletesAndSignalsBackToListExactlyOnce() = runTest {
        val events = subscribe()
        vm.open(1)
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf("Beta SAS"), repo.current.map { it.name })
        // The repository also emitted null after the delete: still a single signal.
        assertEquals(listOf<CompanyDetailEvent>(CompanyDetailEvent.BackToList), events)
        assertFalse(vm.state.value.deleteDialogVisible)
    }

    @Test
    fun secondConfirmIsIgnored() = runTest {
        val events = subscribe()
        vm.open(1)
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(1, events.size)
    }

    @Test
    fun failedDeleteShowsTheFailureAndKeepsTheCompany() = runTest {
        val events = subscribe()
        vm.open(1)
        advanceUntilIdle()
        repo.failNextDelete = true
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(vm.state.value.deleteFailed)
        assertFalse(vm.state.value.deleteDialogVisible)
        assertEquals(2, repo.current.size)
        assertEquals("Acme Software", loaded().name)
        assertTrue(events.isEmpty())
        // Retrying works and clears the failure.
        vm.requestDelete()
        advanceUntilIdle()
        assertFalse(vm.state.value.deleteFailed)
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(1, events.size)
        assertEquals(1, repo.current.size)
    }

    @Test
    fun cancellationIsNotReportedAsAFailure() = runTest {
        val events = subscribe()
        vm.open(1)
        advanceUntilIdle()
        repo.failure = CancellationException("cancelled")
        repo.failNextDelete = true
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertFalse(vm.state.value.deleteFailed)
        assertTrue(events.isEmpty())
    }

    @Test
    fun companyDeletedWhileViewingSignalsBackToList() = runTest {
        val events = subscribe()
        vm.open(1)
        advanceUntilIdle()
        repo.delete(1)
        advanceUntilIdle()
        assertEquals(CompanyDetailContent.NotFound, vm.state.value.content)
        assertEquals(listOf<CompanyDetailEvent>(CompanyDetailEvent.BackToList), events)
    }

    @Test
    fun unknownIdSignalsBackToListWithoutFailing() = runTest {
        val events = subscribe()
        vm.open(999)
        advanceUntilIdle()
        assertEquals(CompanyDetailContent.NotFound, vm.state.value.content)
        assertEquals(listOf<CompanyDetailEvent>(CompanyDetailEvent.BackToList), events)
    }

    @Test
    fun openingAnotherCompanyResetsTheDialogAndTheFailure() = runTest {
        subscribe()
        vm.open(1)
        advanceUntilIdle()
        repo.failNextDelete = true
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        vm.requestDelete()
        advanceUntilIdle()
        assertTrue(vm.state.value.deleteDialogVisible)

        vm.open(2)
        advanceUntilIdle()
        assertFalse(vm.state.value.deleteDialogVisible)
        assertFalse(vm.state.value.deleteFailed)
        assertEquals("Beta SAS", loaded().name)

        // Failure is reset too.
        repo.failNextDelete = true
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(vm.state.value.deleteFailed)
        vm.open(1)
        advanceUntilIdle()
        assertFalse(vm.state.value.deleteFailed)
    }

    @Test
    fun reopeningAfterANotFoundSignalsAgainForTheNewSession() = runTest {
        val events = subscribe()
        vm.open(999)
        advanceUntilIdle()
        vm.open(1)
        advanceUntilIdle()
        assertEquals(1, events.size)
        assertEquals("Acme Software", loaded().name)
        vm.open(998)
        advanceUntilIdle()
        assertEquals(2, events.size)
    }
}
