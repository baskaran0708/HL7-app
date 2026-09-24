package com.livemedica.helix.data.repository

import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.data.mock.MockCallSimulator
import com.livemedica.helix.data.mock.MockClinicalStore
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.Hl7AckStatus
import com.livemedica.helix.domain.model.IntegrationSnapshot
import com.livemedica.helix.domain.model.SearchResults
import com.livemedica.helix.domain.repository.IntegrationRepository
import com.livemedica.helix.domain.repository.NotificationRepository
import com.livemedica.helix.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockNotificationRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : NotificationRepository {

    override fun observeNotifications(): Flow<List<HelixNotification>> =
        store.notifications.map { list -> list.sortedByDescending { it.createdAt } }

    override fun observeUnreadCount(): Flow<Int> =
        store.notifications.map { list -> list.count { !it.isRead } }

    override suspend fun markRead(id: String): AppResult<Unit> = simulator.call { store.markNotificationRead(id) }
    override suspend fun markAllRead(): AppResult<Unit> = simulator.call { store.markAllNotificationsRead() }
    override suspend fun clear(id: String): AppResult<Unit> = simulator.call { store.clearNotification(id) }
}

@Singleton
class MockIntegrationRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : IntegrationRepository {

    override fun observeSnapshot(): Flow<IntegrationSnapshot> = flowOf(snapshot())

    override suspend fun getSnapshot(): AppResult<IntegrationSnapshot> =
        simulator.call(cachedOnOffline = { snapshot() }) { snapshot() }

    private fun snapshot(): IntegrationSnapshot {
        val messages = store.hl7Messages
        val acknowledged = messages.count { it.ackStatus == Hl7AckStatus.ACK || it.ackStatus == Hl7AckStatus.DELIVERED }
        return IntegrationSnapshot(
            messagesToday = store.channels.sumOf { it.messagesToday },
            ackRatePercent = if (messages.isEmpty()) 100.0 else acknowledged * 100.0 / messages.size,
            countsByType = messages.groupingBy { it.type }.eachCount(),
            channels = store.channels,
            recentMessages = messages.sortedByDescending { it.receivedAt },
        )
    }
}

/**
 * Universal search.
 *
 * Runs a genuine query across every entity in the store rather than filtering one pre-built list,
 * so the behaviour matches what the `GET /api/v1/search` endpoint will do server-side and the UI
 * will not need reworking when it arrives.
 */
@Singleton
class MockSearchRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : SearchRepository {

    override suspend fun search(query: String): AppResult<SearchResults> = simulator.call {
        val q = query.trim()
        if (q.length < MIN_QUERY_LENGTH) {
            SearchResults(q, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        } else {
            SearchResults(
                query = q,
                patients = store.patients.value.filter {
                    it.fullName.contains(q, true) || it.mrn.contains(q, true)
                },
                studies = store.studies.value.filter {
                    it.accessionNumber.contains(q, true) || it.description.contains(q, true) ||
                        it.modality.code.equals(q, true)
                },
                orders = store.orders.value.filter {
                    it.patientName.contains(q, true) || it.mrn.contains(q, true) ||
                        it.accessionNumber.contains(q, true) || it.procedure.contains(q, true) ||
                        it.cptCode.contains(q, true) || it.orderingPhysician.contains(q, true)
                },
                reports = store.reports.value.filter {
                    it.patientName.contains(q, true) || it.mrn.contains(q, true) ||
                        it.accessionNumber.contains(q, true) || it.procedure.contains(q, true) ||
                        it.impression.contains(q, true)
                },
                appointments = store.appointments.value.filter {
                    it.patientName.contains(q, true) || it.mrn.contains(q, true) ||
                        it.procedure.contains(q, true) || it.room.contains(q, true)
                },
            )
        }
    }

    override fun observeRecentQueries(): Flow<List<String>> = store.recentQueries
    override suspend fun recordQuery(query: String) = store.recordQuery(query)
    override suspend fun clearRecentQueries() = store.clearRecentQueries()

    private companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
