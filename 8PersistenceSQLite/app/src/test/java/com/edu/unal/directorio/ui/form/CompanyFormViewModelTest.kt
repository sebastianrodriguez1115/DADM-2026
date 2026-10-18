package com.edu.unal.directorio.ui.form

import com.edu.unal.directorio.data.FakeCompanyRepository
import com.edu.unal.directorio.domain.Classification.Consulting
import com.edu.unal.directorio.domain.Classification.CustomDevelopment
import com.edu.unal.directorio.domain.Classification.SoftwareFactory
import com.edu.unal.directorio.domain.ClassificationError
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.EmailError
import com.edu.unal.directorio.domain.NameError
import com.edu.unal.directorio.domain.PhoneError
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanyFormViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val acme = CompanyDraft(
        name = "Acme Software",
        email = "ventas@acme.example.com",
        phone = "601 555 0123",
        classification = SoftwareFactory,
    )

    private fun newVm(repo: FakeCompanyRepository = FakeCompanyRepository()) =
        CompanyFormViewModel(repo)

    private fun CompanyFormViewModel.fill(
        name: String = "Nueva SAS",
        email: String = "info@nueva.example.com",
        classification: com.edu.unal.directorio.domain.Classification? = CustomDevelopment,
    ) {
        onNameChange(name)
        onEmailChange(email)
        classification?.let { onClassificationChosen(it) }
    }

    /**
     * Collects the events into a list for the duration of the test. Unconfined so each event is
     * recorded the moment it is sent.
     */
    private fun TestScope.collectEvents(vm: CompanyFormViewModel): List<CompanyFormEvent> {
        val events = mutableListOf<CompanyFormEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }
        return events
    }

    @Test
    fun createFormStartsEmpty() = runTest {
        val vm = newVm()
        vm.open(null)
        advanceUntilIdle()
        assertEquals(CompanyFormUiState(), vm.state.value)
    }

    @Test
    fun emptyFormShowsThreeErrorsAtOnce() = runTest {
        val repo = FakeCompanyRepository()
        val vm = newVm(repo)
        vm.open(null)
        vm.save()
        advanceUntilIdle()
        val errors = vm.state.value.errors
        assertEquals(NameError.Empty, errors.name)
        assertEquals(EmailError.Empty, errors.email)
        assertEquals(ClassificationError.NoneSelected, errors.classification)
        assertNull(errors.url)
        assertNull(errors.phone)
        assertEquals(0, repo.saveCalls)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun invalidOptionalFieldsAreReportedTogetherWithRequiredOnes() = runTest {
        val vm = newVm()
        vm.open(null)
        vm.onPhoneChange("12345")
        vm.save()
        assertEquals(PhoneError.Invalid, vm.state.value.errors.phone)
        assertEquals(NameError.Empty, vm.state.value.errors.name)
    }

    @Test
    fun duplicateNameWithOtherCaseAndSpacesShowsNameError() = runTest {
        val repo = FakeCompanyRepository(listOf(acme))
        val vm = newVm(repo)
        vm.open(null)
        vm.fill(name = "  acme software  ", email = "otro@otra.example.com")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.errors.nameTaken)
        assertFalse(vm.state.value.errors.emailTaken)
        assertFalse(vm.state.value.isSaving)
        assertEquals(1, repo.current.size)
    }

    @Test
    fun duplicateEmailWithOtherCaseShowsEmailError() = runTest {
        val repo = FakeCompanyRepository(listOf(acme))
        val vm = newVm(repo)
        vm.open(null)
        vm.fill(name = "Otra", email = " VENTAS@Acme.example.com ")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.errors.emailTaken)
        assertFalse(vm.state.value.errors.nameTaken)
        assertEquals(1, repo.current.size)
    }

    @Test
    fun duplicateNameAndEmailShowBothErrors() = runTest {
        val vm = newVm(FakeCompanyRepository(listOf(acme)))
        vm.open(null)
        vm.fill(name = "ACME software", email = "ventas@acme.example.com")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.errors.nameTaken)
        assertTrue(vm.state.value.errors.emailTaken)
    }

    @Test
    fun editingAFieldClearsOnlyThatFieldError() = runTest {
        val vm = newVm()
        vm.open(null)
        vm.onPhoneChange("12345")
        vm.save()
        vm.onNameChange("A")
        val errors = vm.state.value.errors
        assertNull(errors.name)
        assertEquals(EmailError.Empty, errors.email)
        assertEquals(PhoneError.Invalid, errors.phone)
        assertEquals(ClassificationError.NoneSelected, errors.classification)
        vm.onEmailChange("a")
        assertNull(vm.state.value.errors.email)
        assertEquals(PhoneError.Invalid, vm.state.value.errors.phone)
    }

    @Test
    fun choosingAClassificationClearsOnlyItsErrorAndCanBeChanged() = runTest {
        val vm = newVm()
        vm.open(null)
        assertNull(vm.state.value.classification)
        vm.save()
        assertEquals(ClassificationError.NoneSelected, vm.state.value.errors.classification)
        assertEquals(EmailError.Empty, vm.state.value.errors.email)
        vm.onClassificationChosen(Consulting)
        assertNull(vm.state.value.errors.classification)
        assertEquals(EmailError.Empty, vm.state.value.errors.email)
        assertEquals(Consulting, vm.state.value.classification)
        vm.onClassificationChosen(SoftwareFactory)
        assertEquals(SoftwareFactory, vm.state.value.classification)
    }

    @Test
    fun editingNameClearsTakenError() = runTest {
        val vm = newVm(FakeCompanyRepository(listOf(acme)))
        vm.open(null)
        vm.fill(name = "Acme Software", email = "x@y.example.com")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.errors.nameTaken)
        vm.onNameChange("Acme Software 2")
        assertFalse(vm.state.value.errors.nameTaken)
    }

    @Test
    fun doubleTapCreatesOneCompany() = runTest {
        val repo = FakeCompanyRepository()
        val vm = newVm(repo)
        val events = collectEvents(vm)
        vm.open(null)
        vm.fill()
        vm.save()
        assertTrue(vm.state.value.isSaving)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.saveCalls)
        assertEquals(1, repo.current.size)
        assertEquals(1, events.size)
    }

    @Test
    fun storageFailureKeepsWhatWasTypedAndAllowsRetry() = runTest {
        val repo = FakeCompanyRepository()
        val vm = newVm(repo)
        val events = collectEvents(vm)
        vm.open(null)
        vm.fill(name = "Nueva SAS", email = "info@nueva.example.com")
        repo.failNextSave = true
        vm.save()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state.saveFailed)
        assertFalse(state.isSaving)
        assertEquals("Nueva SAS", state.name)
        assertEquals("info@nueva.example.com", state.email)
        assertEquals(CustomDevelopment, state.classification)
        assertTrue(events.isEmpty())
        assertTrue(repo.current.isEmpty())

        vm.save()
        advanceUntilIdle()
        assertFalse(vm.state.value.saveFailed)
        assertEquals(1, repo.current.size)
        assertEquals(1, events.size)
    }

    @Test
    fun validCreateSavesTrimmedDraftAndEmitsSaved() = runTest {
        val repo = FakeCompanyRepository()
        val vm = newVm(repo)
        val events = collectEvents(vm)
        vm.open(null)
        vm.fill(name = "  Nueva SAS  ", email = " info@nueva.example.com ", classification = SoftwareFactory)
        vm.onProductsChange("  a\nb\nc  ")
        vm.save()
        advanceUntilIdle()
        val saved = repo.current.single()
        assertEquals("Nueva SAS", saved.name)
        assertEquals("info@nueva.example.com", saved.email)
        assertEquals("a\nb\nc", saved.products)
        assertEquals(SoftwareFactory, saved.classification)
        assertEquals(listOf<CompanyFormEvent>(CompanyFormEvent.Saved(saved.id)), events)
    }

    @Test
    fun eventIsDeliveredOnceEvenIfCollectedLater() = runTest {
        val repo = FakeCompanyRepository()
        val vm = newVm(repo)
        vm.open(null)
        vm.fill()
        vm.save()
        advanceUntilIdle()
        val first = collectEvents(vm)
        advanceUntilIdle()
        assertEquals(1, first.size)
        val second = collectEvents(vm)
        advanceUntilIdle()
        assertTrue(second.isEmpty())
    }

    // ---- Editing (6.2) ----

    private fun repoWithAcme() = FakeCompanyRepository(listOf(acme))

    @Test
    fun openLoadsCurrentValuesAndTheCurrentClassification() = runTest {
        val repo = repoWithAcme()
        val vm = newVm(repo)
        vm.open(repo.current.single().id)
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        val s = vm.state.value
        assertTrue(s.isEditing)
        assertFalse(s.isLoading)
        assertEquals("Acme Software", s.name)
        assertEquals("ventas@acme.example.com", s.email)
        assertEquals("601 555 0123", s.phone)
        assertEquals(SoftwareFactory, s.classification)
    }

    @Test
    fun editingOnlyThePhoneSavesWithoutTakenErrorAndEmitsSaved() = runTest {
        val repo = repoWithAcme()
        val id = repo.current.single().id
        val vm = newVm(repo)
        val events = collectEvents(vm)
        vm.open(id)
        advanceUntilIdle()
        vm.onPhoneChange("3105550123")
        vm.save()
        advanceUntilIdle()
        assertFalse(vm.state.value.errors.nameTaken)
        assertFalse(vm.state.value.errors.emailTaken)
        assertEquals("3105550123", repo.current.single().phone)
        assertEquals(1, repo.current.size)
        assertEquals(listOf<CompanyFormEvent>(CompanyFormEvent.Saved(id)), events)
    }

    @Test
    fun changingTheClassificationWhileEditingSavesTheNewOne() = runTest {
        val repo = repoWithAcme()
        val vm = newVm(repo)
        vm.open(repo.current.single().id)
        advanceUntilIdle()
        vm.onClassificationChosen(Consulting)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.current.size)
        assertEquals(Consulting, repo.current.single().classification)
    }

    @Test
    fun changingOnlyTheNameCapitalizationSaves() = runTest {
        val repo = repoWithAcme()
        val vm = newVm(repo)
        vm.open(repo.current.single().id)
        advanceUntilIdle()
        vm.onNameChange("ACME SOFTWARE")
        vm.save()
        advanceUntilIdle()
        assertFalse(vm.state.value.errors.nameTaken)
        assertEquals("ACME SOFTWARE", repo.current.single().name)
    }

    @Test
    fun editingToAnotherCompanysNameShowsError() = runTest {
        val other = CompanyDraft(name = "Beta Labs", email = "b@beta.example.com", classification = Consulting)
        val repo = FakeCompanyRepository(listOf(acme, other))
        val acmeId = repo.current.first { it.name == "Acme Software" }.id
        val vm = newVm(repo)
        vm.open(acmeId)
        advanceUntilIdle()
        vm.onNameChange(" beta labs ")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.errors.nameTaken)
        assertEquals("Acme Software", repo.current.first { it.id == acmeId }.name)
    }

    @Test
    fun cancelWithoutSavingChangesNothing() = runTest {
        val repo = repoWithAcme()
        val vm = newVm(repo)
        val callsBefore = repo.saveCalls
        vm.open(repo.current.single().id)
        advanceUntilIdle()
        vm.onNameChange("Cambiado")
        // Cancel / Back only navigate away: the ViewModel never writes unless save() is called.
        assertEquals(callsBefore, repo.saveCalls)
        assertEquals("Acme Software", repo.current.single().name)
    }

    @Test
    fun missingCompanyEmitsNotFoundWithoutFailing() = runTest {
        val vm = newVm(FakeCompanyRepository())
        val events = collectEvents(vm)
        vm.open(999)
        advanceUntilIdle()
        assertEquals(listOf<CompanyFormEvent>(CompanyFormEvent.CompanyNotFound), events)
    }

    @Test
    fun createAfterEditStartsCompletelyEmpty() = runTest {
        val repo = repoWithAcme()
        val vm = newVm(repo)
        vm.open(repo.current.single().id)
        advanceUntilIdle()
        vm.onNameChange("")
        vm.save()
        advanceUntilIdle()
        assertEquals(NameError.Empty, vm.state.value.errors.name)
        vm.open(null)
        assertEquals(CompanyFormUiState(), vm.state.value)
        vm.fill()
        vm.save()
        advanceUntilIdle()
        // The create form saved as a new company, not as an edit of Acme.
        assertEquals(2, repo.current.size)
    }
}
