package com.edu.unal.directorio.data

import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.Classification.Consulting
import com.edu.unal.directorio.domain.Classification.CustomDevelopment
import com.edu.unal.directorio.domain.Classification.SoftwareFactory
import com.edu.unal.directorio.domain.CompanyDraft
import com.edu.unal.directorio.domain.CompanyFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class FakeCompanyRepositoryTest {
    private fun draft(
        name: String,
        email: String = "${name.filter { it.isLetter() }.lowercase()}@example.com",
        classification: Classification = Consulting,
    ) = CompanyDraft(name = name, email = email, classification = classification)

    private fun names(repo: FakeCompanyRepository, filter: CompanyFilter = CompanyFilter()) =
        runBlocking { repo.observeCompanies(filter).first().map { it.name } }

    @Test
    fun saveTrimsAndOrdersByNormalizedName() = runBlocking {
        val repo = FakeCompanyRepository()
        repo.save(draft("  Zeta  "), null)
        repo.save(draft("Ñandú"), null)
        repo.save(draft("acme"), null)

        assertEquals(listOf("acme", "Ñandú", "Zeta"), names(repo))
    }

    @Test
    fun duplicatesAreDetectedIgnoringCaseAccentsAndSpaces() = runBlocking {
        val repo = FakeCompanyRepository(listOf(draft("Tecnología", "a@example.com")))

        assertEquals(SaveResult.NameTaken, repo.save(draft("  TECNOLOGIA ", "b@example.com"), null))
        assertEquals(SaveResult.EmailTaken, repo.save(draft("Otra", " A@EXAMPLE.com"), null))
        assertEquals(SaveResult.NameAndEmailTaken, repo.save(draft("tecnologia", "a@example.com"), null))
        assertEquals(1, repo.current.size)
    }

    @Test
    fun editingExcludesTheCompanyItself() = runBlocking {
        val repo = FakeCompanyRepository(listOf(draft("Acme software"), draft("Beta")))
        val acme = repo.current.first { it.name == "Acme software" }.id

        assertEquals(SaveResult.Saved(acme), repo.save(draft("Acme Software", "acmesoftware@example.com"), acme))
        assertEquals(SaveResult.NameTaken, repo.save(draft("beta", "x@example.com"), acme))
        assertEquals(2, repo.current.size)
    }

    @Test
    fun editingAMissingIdThrowsAndADraftWithoutClassificationIsRejected() = runBlocking {
        val repo = FakeCompanyRepository()
        try {
            repo.save(draft("Acme"), 99L)
            fail()
        } catch (_: NoSuchElementException) {
        }
        try {
            repo.save(CompanyDraft(name = "Acme", email = "a@example.com"), null)
            fail()
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun filterIsSubstringOfNormalizedNameAndUnionOfChecked() = runBlocking {
        val repo = FakeCompanyRepository(
            listOf(
                draft("Ñandú Ágil", "n@example.com", Consulting),
                draft("Acme", "a@example.com", SoftwareFactory),
                draft("Zeta Tecnología", "z@example.com", CustomDevelopment),
            ),
        )

        assertEquals(listOf("Zeta Tecnología"), names(repo, CompanyFilter(query = "TECNOLOGIA")))
        assertEquals(
            listOf("Ñandú Ágil", "Zeta Tecnología"),
            names(repo, CompanyFilter(classifications = setOf(Consulting, CustomDevelopment))),
        )
        assertEquals(
            listOf("Ñandú Ágil"),
            names(repo, CompanyFilter("nandu", setOf(Consulting))),
        )
    }

    @Test
    fun companyHasTheSingleClassificationItWasSavedWith() = runBlocking {
        val repo = FakeCompanyRepository()
        val id = (repo.save(draft("Acme", classification = SoftwareFactory), null) as SaveResult.Saved).id

        assertEquals(SoftwareFactory, repo.observeCompany(id).first()?.classification)
        assertEquals(SaveResult.Saved(id), repo.save(draft("Acme", classification = CustomDevelopment), id))
        assertEquals(CustomDevelopment, repo.observeCompany(id).first()?.classification)
        assertEquals(1, repo.current.size)
    }

    @Test
    fun noCheckedClassificationDoesNotFilter() = runBlocking {
        val repo = FakeCompanyRepository(
            listOf(draft("A", "a@example.com", Consulting), draft("B", "b@example.com", SoftwareFactory)),
        )
        assertEquals(listOf("A", "B"), names(repo, CompanyFilter()))
    }

    @Test
    fun failureSwitchesFailOnceAndChangeNothing() = runBlocking {
        val repo = FakeCompanyRepository(listOf(draft("Acme")))
        val id = repo.current.single().id

        repo.failNextSave = true
        try {
            repo.save(draft("Beta"), null)
            fail()
        } catch (_: IllegalStateException) {
        }
        repo.failNextDelete = true
        try {
            repo.delete(id)
            fail()
        } catch (_: IllegalStateException) {
        }
        assertEquals(listOf("Acme"), names(repo))

        repo.save(draft("Beta"), null)
        repo.delete(id)
        assertEquals(listOf("Beta"), names(repo))
        assertNull(repo.observeCompany(id).first())
    }
}
