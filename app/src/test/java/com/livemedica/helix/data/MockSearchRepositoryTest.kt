package com.livemedica.helix.data

import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.data.mock.MockCallSimulator
import com.livemedica.helix.data.mock.MockClinicalStore
import com.livemedica.helix.data.repository.MockSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockSearchRepositoryTest {

    private val store = MockClinicalStore()
    private val repository = MockSearchRepository(store, MockCallSimulator(FakeSettingsRepository()))

    private suspend fun search(query: String) =
        (repository.search(query) as AppResult.Success).data

    @Test
    fun `searching a patient name returns that patient and their related records`() = runTest {
        val name = store.patients.value.first().fullName
        val surname = name.substringAfterLast(' ')

        val results = search(surname)

        assertTrue(results.patients.any { it.fullName == name })
        assertFalse(results.isEmpty)
    }

    @Test
    fun `searching an accession number finds the order it belongs to`() = runTest {
        val order = store.orders.value.first()

        val results = search(order.accessionNumber)

        assertTrue(results.orders.any { it.id == order.id })
    }

    @Test
    fun `searching an MRN crosses entity types`() = runTest {
        val patient = store.patients.value.first { patient ->
            store.orders.value.any { it.patientId == patient.id }
        }

        val results = search(patient.mrn)

        assertTrue(results.patients.isNotEmpty())
        assertTrue(results.orders.isNotEmpty())
        assertTrue(results.totalCount >= 2)
    }

    @Test
    fun `a one character query is treated as not yet searched`() = runTest {
        assertTrue(search("L").isEmpty)
    }

    @Test
    fun `a query matching nothing returns empty rather than failing`() = runTest {
        val results = search("zzzzzzzz")

        assertTrue(results.isEmpty)
        assertEquals(0, results.totalCount)
    }

    @Test
    fun `recent queries are recorded most recent first and de-duplicated`() = runTest {
        repository.recordQuery("Park")
        repository.recordQuery("MRI")
        repository.recordQuery("Park")

        assertEquals(listOf("Park", "MRI"), repository.observeRecentQueries().first())
    }
}
