package com.livemedica.helix.data

import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.data.mock.MockCallSimulator
import com.livemedica.helix.data.mock.MockClinicalStore
import com.livemedica.helix.data.repository.MockResultRepository
import com.livemedica.helix.domain.model.HelixSettings
import com.livemedica.helix.domain.model.ReportStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockResultRepositoryTest {

    private fun repository(settings: HelixSettings = HelixSettings()): Pair<MockResultRepository, MockClinicalStore> {
        val store = MockClinicalStore()
        val simulator = MockCallSimulator(FakeSettingsRepository(settings))
        return MockResultRepository(store, simulator) to store
    }

    @Test
    fun `signing a report moves it to signed and stamps a signature time`() = runTest {
        val (repository, _) = repository()
        val unsigned = repository.observeReports().first().first { !it.isSigned }

        val result = repository.signReport(unsigned.id)

        assertTrue(result is AppResult.Success)
        val signed = (result as AppResult.Success).data
        assertEquals(ReportStatus.SIGNED, signed.status)
        assertNotNull(signed.signedAt)
    }

    @Test
    fun `signing is idempotent so a double tap cannot sign twice`() = runTest {
        val (repository, _) = repository()
        val unsigned = repository.observeReports().first().first { !it.isSigned }

        val first = (repository.signReport(unsigned.id) as AppResult.Success).data
        val second = (repository.signReport(unsigned.id) as AppResult.Success).data

        assertEquals(first.signedAt, second.signedAt)
    }

    @Test
    fun `signing propagates to every observer of the shared store`() = runTest {
        val (repository, store) = repository()
        val unsigned = repository.observeReports().first().first { !it.isSigned }
        val unsignedBefore = store.reports.value.count { !it.isSigned }

        repository.signReport(unsigned.id)

        assertEquals(unsignedBefore - 1, store.reports.value.count { !it.isSigned })
    }

    @Test
    fun `offline simulation returns cached data rather than an error`() = runTest {
        val (repository, _) = repository(HelixSettings(simulateOffline = true))

        val result = repository.getReports()

        assertTrue(result is AppResult.Offline)
        assertTrue((result as AppResult.Offline).cached!!.isNotEmpty())
    }

    @Test
    fun `error simulation surfaces a failure`() = runTest {
        val (repository, _) = repository(HelixSettings(simulateError = true))

        assertTrue(repository.getReports() is AppResult.Failure)
    }

    @Test
    fun `reports are ordered STAT first`() = runTest {
        val (repository, _) = repository()

        val reports = repository.observeReports().first()

        val priorities = reports.map { it.priority.ordinal }
        assertEquals(priorities.sorted(), priorities)
        assertSame(reports, reports)
    }
}
