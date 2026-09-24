package com.livemedica.helix.data.repository

import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.data.mock.MockCallSimulator
import com.livemedica.helix.data.mock.MockClinicalStore
import com.livemedica.helix.domain.model.AppointmentStatus
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.OrderStatus
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.ReportStatus
import com.livemedica.helix.domain.model.Study
import com.livemedica.helix.domain.model.TodayDashboard
import com.livemedica.helix.domain.repository.OrderRepository
import com.livemedica.helix.domain.repository.ResultRepository
import com.livemedica.helix.domain.repository.TodayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockOrderRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : OrderRepository {

    override fun observeOrders(): Flow<List<Order>> =
        store.orders.map { list -> list.sortedWith(worklistOrdering) }

    override suspend fun getOrders(
        priority: Priority?,
        status: OrderStatus?,
        query: String?,
    ): AppResult<List<Order>> {
        val result = { store.orders.value.filtered(priority, status, query) }
        return simulator.call(cachedOnOffline = result) { result() }
    }

    override suspend fun getOrder(id: String): AppResult<Order> = simulator.call {
        store.orders.value.firstOrNull { it.id == id }
            ?: error("That order is no longer available.")
    }

    override suspend fun getStudy(studyId: String): AppResult<Study> = simulator.call {
        store.studies.value.firstOrNull { it.id == studyId }
            ?: error("That study is no longer available.")
    }

    override suspend fun getStudyForOrder(orderId: String): AppResult<Study> = simulator.call {
        store.studies.value.firstOrNull { it.orderId == orderId }
            ?: error("No images have been acquired for this order yet.")
    }

    private fun List<Order>.filtered(priority: Priority?, status: OrderStatus?, query: String?) =
        asSequence()
            .filter { priority == null || it.priority == priority }
            .filter { status == null || it.status == status }
            .filter { query.isNullOrBlank() || it.matches(query) }
            .sortedWith(worklistOrdering)
            .toList()

    private fun Order.matches(query: String): Boolean {
        val q = query.trim()
        return patientName.contains(q, ignoreCase = true) ||
            mrn.contains(q, ignoreCase = true) ||
            accessionNumber.contains(q, ignoreCase = true) ||
            procedure.contains(q, ignoreCase = true) ||
            cptCode.contains(q, ignoreCase = true) ||
            orderingPhysician.contains(q, ignoreCase = true)
    }

    private companion object {
        /** STAT first, then urgent, then newest — the order a radiologist actually works a list in. */
        val worklistOrdering: Comparator<Order> =
            compareBy<Order> { it.priority.ordinal }.thenByDescending { it.orderedAt }
    }
}

@Singleton
class MockResultRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : ResultRepository {

    override fun observeReports(): Flow<List<RadiologyReport>> =
        store.reports.map { list -> list.sortedWith(resultOrdering) }

    override suspend fun getReports(status: ReportStatus?, priority: Priority?): AppResult<List<RadiologyReport>> {
        val result = {
            store.reports.value
                .filter { status == null || it.status == status }
                .filter { priority == null || it.priority == priority }
                .sortedWith(resultOrdering)
        }
        return simulator.call(cachedOnOffline = result) { result() }
    }

    override suspend fun getReport(id: String): AppResult<RadiologyReport> = simulator.call {
        store.reports.value.firstOrNull { it.id == id }
            ?: error("That report is no longer available.")
    }

    override suspend fun signReport(id: String): AppResult<RadiologyReport> = simulator.call {
        requireNotNull(store.signReport(id)) { "Report $id not found" }
    }

    private companion object {
        val resultOrdering: Comparator<RadiologyReport> =
            compareBy<RadiologyReport> { it.priority.ordinal }.thenByDescending { it.createdAt }
    }
}

@Singleton
class MockTodayRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : TodayRepository {

    override fun observeDashboard(): Flow<TodayDashboard> = combine(
        store.doctor,
        store.appointments,
        store.orders,
        store.reports,
        store.notifications,
    ) { doctor, appointments, orders, reports, notifications ->
        buildDashboard(doctor, appointments, orders, reports, notifications)
    }

    override suspend fun getDashboard(): AppResult<TodayDashboard> {
        // Captured before the simulator's delay so the offline fallback shows the last known
        // snapshot rather than nothing.
        val snapshot = observeDashboard().first()
        return simulator.call(cachedOnOffline = { snapshot }) { snapshot }
    }

    private fun buildDashboard(
        doctor: com.livemedica.helix.domain.model.Doctor,
        appointments: List<com.livemedica.helix.domain.model.Appointment>,
        orders: List<Order>,
        reports: List<RadiologyReport>,
        notifications: List<com.livemedica.helix.domain.model.HelixNotification>,
    ): TodayDashboard {
        val today = LocalDate.now()
        val now = java.time.LocalDateTime.now()
        val todays = appointments.filter { it.start.toLocalDate() == today }

        return TodayDashboard(
            doctor = doctor,
            date = today,
            // The one item that should interrupt the clinician: a critical read not yet signed.
            criticalReport = reports.firstOrNull { it.isCritical && !it.isSigned },
            // "Exams remaining", so completed and cancelled ones are excluded — counting the
            // whole day would show the same number at 18:00 as at 08:00.
            examsToday = todays.count {
                it.status != AppointmentStatus.COMPLETED &&
                    it.status != AppointmentStatus.CANCELLED &&
                    it.status != AppointmentStatus.NO_SHOW
            },
            pendingReads = orders.count { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED },
            statCount = orders.count { it.priority == Priority.STAT && it.status != OrderStatus.COMPLETED },
            unsignedReports = reports.count { !it.isSigned && it.status != ReportStatus.DRAFT },
            // What is still to come: not yet finished on the clock, and not already dealt with.
            // Rolls past today into the next scheduled day, so the section is never empty at the
            // end of a shift.
            upNext = appointments
                .filter { it.end.isAfter(now) && it.status in UP_NEXT_STATUSES }
                .sortedBy { it.start }
                .take(UP_NEXT_LIMIT),
            unreadNotifications = notifications.count { !it.isRead },
        )
    }

    private companion object {
        const val UP_NEXT_LIMIT = 6

        /** An exam only counts as "up next" while it can still be attended. */
        val UP_NEXT_STATUSES = setOf(
            AppointmentStatus.SCHEDULED,
            AppointmentStatus.CHECKED_IN,
            AppointmentStatus.IN_PROGRESS,
        )
    }
}
