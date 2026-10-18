package com.edu.unal.directorio.ui.list

import com.edu.unal.directorio.data.FakeCompanyRepository
import com.edu.unal.directorio.domain.Classification.Consulting
import com.edu.unal.directorio.domain.Classification.CustomDevelopment
import com.edu.unal.directorio.domain.Classification.SoftwareFactory
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.CompanyFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class CompanyListViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun draft(name: String, classification: com.edu.unal.directorio.domain.Classification) =
        CompanyDraft(name = name, email = "${name.filter { it.isLetter() }}@x.com", classification = classification)

    // One classification each: Acme Software {Consulting}, Acme Labs {CustomDevelopment}, Beta {SoftwareFactory}.
    private val abc = listOf(
        draft("Acme Software", Consulting),
        draft("Acme Labs", CustomDevelopment),
        draft("Beta", SoftwareFactory),
    )

    private fun names(vm: CompanyListViewModel) = vm.results.value.companies.map { it.name }


    private suspend fun kotlinx.coroutines.test.TestScope.observe(vm: CompanyListViewModel) {
        vm.results.launchIn(backgroundScope)
        advanceUntilIdle()
    }

    @Test
    fun listsAllCompaniesInAlphabeticalOrder() = runTest {
        val vm = CompanyListViewModel(
            FakeCompanyRepository(
                listOf(draft("beta Labs", Consulting), draft("Álamo Tech", Consulting), draft("Cobalto", Consulting)),
            ),
        )
        observe(vm)
        assertEquals(listOf("Álamo Tech", "beta Labs", "Cobalto"), names(vm))
    }

    @Test
    fun emptyDirectoryShowsOnlyTheDirectoryEmptyMessage() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository())
        observe(vm)
        val r = vm.results.value
        assertTrue(r.loaded)
        assertTrue(r.showDirectoryEmpty)
        assertFalse(r.showNoMatches)
    }

    @Test
    fun beforeLoadingNoEmptyMessageShows() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository())
        assertFalse(vm.results.value.showDirectoryEmpty)
        assertFalse(vm.results.value.showNoMatches)
    }

    @Test
    fun noMatchesShowsOnlyTheNoMatchesMessage() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        vm.onQueryChange("zzzz")
        advanceUntilIdle()
        val r = vm.results.value
        assertTrue(r.companies.isEmpty())
        assertTrue(r.showNoMatches)
        assertFalse(r.showDirectoryEmpty)
    }

    @Test
    fun searchMatchesSubstringWithoutCaseOrAccents() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc + draft("Tecnología Ñandú", Consulting)))
        observe(vm)
        vm.onQueryChange("me sof")
        advanceUntilIdle()
        assertEquals(listOf("Acme Software"), names(vm))
        vm.onQueryChange("TECNOLOGIA")
        advanceUntilIdle()
        assertEquals(listOf("Tecnología Ñandú"), names(vm))
        vm.onQueryChange("ñandu")
        advanceUntilIdle()
        assertEquals(listOf("Tecnología Ñandú"), names(vm))
    }

    @Test
    fun blankQueryDoesNotFilterAndIsKeptAsTyped() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        vm.onQueryChange("   ")
        advanceUntilIdle()
        assertEquals("   ", vm.filter.value.query)
        assertEquals(3, names(vm).size)
    }

    @Test
    fun singleChipFilters() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        vm.toggleClassification(Consulting)
        advanceUntilIdle()
        assertEquals(listOf("Acme Software"), names(vm))
    }

    @Test
    fun severalChipsAreTheUnionOfTheirCompanies() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        vm.toggleClassification(Consulting)
        vm.toggleClassification(SoftwareFactory)
        advanceUntilIdle()
        // A {Consulting} and C {SoftwareFactory} appear; B {CustomDevelopment} does not.
        assertEquals(listOf("Acme Software", "Beta"), names(vm))
        vm.toggleClassification(CustomDevelopment)
        advanceUntilIdle()
        assertEquals(listOf("Acme Labs", "Acme Software", "Beta"), names(vm))
    }

    @Test
    fun noChipsDoesNotFilterByClassification() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        assertTrue(vm.filter.value.classifications.isEmpty())
        assertEquals(listOf("Acme Labs", "Acme Software", "Beta"), names(vm))
    }

    @Test
    fun searchPlusChipCombine() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        vm.onQueryChange("acme")
        vm.toggleClassification(Consulting)
        advanceUntilIdle()
        assertEquals(listOf("Acme Software"), names(vm))
    }

    @Test
    fun combinationWithoutResultsShowsNoMatches() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        vm.onQueryChange("beta")
        vm.toggleClassification(CustomDevelopment)
        advanceUntilIdle()
        assertTrue(vm.results.value.showNoMatches)
    }

    @Test
    fun uncheckingAllChipsRestoresTheList() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        vm.toggleClassification(Consulting)
        advanceUntilIdle()
        vm.toggleClassification(Consulting)
        advanceUntilIdle()
        assertTrue(vm.filter.value.classifications.isEmpty())
        assertEquals(3, names(vm).size)
    }

    @Test
    fun clearQueryEmptiesTheTextAndKeepsChips() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        observe(vm)
        vm.onQueryChange("acme")
        vm.toggleClassification(Consulting)
        vm.clearQuery()
        advanceUntilIdle()
        assertEquals(CompanyFilter("", setOf(Consulting)), vm.filter.value)
        assertEquals(listOf("Acme Software"), names(vm))
    }

    @Test
    fun listUpdatesWhenTheRepositoryChanges() = runTest {
        val repo = FakeCompanyRepository(abc)
        val vm = CompanyListViewModel(repo)
        observe(vm)
        repo.save(draft("Aaa", Consulting), null)
        advanceUntilIdle()
        assertEquals("Aaa", names(vm).first())
        repo.delete(repo.current.first { it.name == "Aaa" }.id)
        advanceUntilIdle()
        assertEquals(3, names(vm).size)
    }

    @Test
    fun filterIsKeptWhenObservationStopsAndStartsAgain() = runTest {
        val vm = CompanyListViewModel(FakeCompanyRepository(abc))
        val first = vm.results.launchIn(this)
        advanceUntilIdle()
        vm.onQueryChange("acme")
        vm.toggleClassification(Consulting)
        advanceUntilIdle()
        first.cancel()
        advanceTimeBy(10_000)
        advanceUntilIdle()
        val second = vm.results.launchIn(this)
        advanceUntilIdle()
        assertEquals(CompanyFilter("acme", setOf(Consulting)), vm.filter.value)
        assertEquals(listOf("Acme Software"), names(vm))
        second.cancel()
    }
}
