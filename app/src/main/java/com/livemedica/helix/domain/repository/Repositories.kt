package com.livemedica.helix.domain.repository

import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.model.Facility
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.IntegrationSnapshot
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.OrderStatus
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.ReportStatus
import com.livemedica.helix.domain.model.SearchResults
import com.livemedica.helix.domain.model.Study
import com.livemedica.helix.domain.model.TodayDashboard
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * The repository layer is the app's only data contract.
 *
 * Phase 1 binds these to in-memory mock implementations; Phase 2 rebinds them to AWS-backed
 * implementations in `data/di`. Nothing above this layer — no ViewModel, no composable — knows or
 * cares which is in use.
 *
 * Reads that the UI observes return [Flow] so a mutation anywhere (signing a report, reading a
 * notification) propagates to every screen showing that data. One-shot reads and all writes are
 * `suspend` and return [AppResult] so offline and error states are explicit rather than exceptional.
 */

interface DoctorRepository {
    fun observeCurrentDoctor(): Flow<Doctor>
    suspend fun getCurrentDoctor(): AppResult<Doctor>
    suspend fun setOnCall(onCall: Boolean): AppResult<Doctor>
}

interface AppointmentRepository {
    fun observeTodayAppointments(): Flow<List<Appointment>>
    suspend fun getTodayAppointments(): AppResult<List<Appointment>>
    suspend fun getAppointments(date: LocalDate, facilityId: String? = null): AppResult<List<Appointment>>
    suspend fun getAppointment(id: String): AppResult<Appointment>
    suspend fun getFacilities(): AppResult<List<Facility>>
}

interface PatientRepository {
    suspend fun getPatient(id: String): AppResult<Patient>
    fun observePatient(id: String): Flow<Patient?>
    suspend fun getAppointmentsFor(patientId: String): AppResult<List<Appointment>>
    suspend fun getOrdersFor(patientId: String): AppResult<List<Order>>
    suspend fun getReportsFor(patientId: String): AppResult<List<RadiologyReport>>
    suspend fun getStudiesFor(patientId: String): AppResult<List<Study>>
}

interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>
    suspend fun getOrders(
        priority: Priority? = null,
        status: OrderStatus? = null,
        query: String? = null,
    ): AppResult<List<Order>>

    suspend fun getOrder(id: String): AppResult<Order>
    suspend fun getStudy(studyId: String): AppResult<Study>
    suspend fun getStudyForOrder(orderId: String): AppResult<Study>
}

interface ResultRepository {
    fun observeReports(): Flow<List<RadiologyReport>>
    suspend fun getReports(status: ReportStatus? = null, priority: Priority? = null): AppResult<List<RadiologyReport>>
    suspend fun getReport(id: String): AppResult<RadiologyReport>

    /**
     * Applies the attending's signature. Mock and remote implementations alike must make this
     * idempotent — a double tap on Sign must not produce a second signature event.
     */
    suspend fun signReport(id: String): AppResult<RadiologyReport>
}

interface NotificationRepository {
    fun observeNotifications(): Flow<List<HelixNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markRead(id: String): AppResult<Unit>
    suspend fun markAllRead(): AppResult<Unit>
    suspend fun clear(id: String): AppResult<Unit>
}

interface SearchRepository {
    /** Single entry point for universal search across every clinical entity. */
    suspend fun search(query: String): AppResult<SearchResults>
    fun observeRecentQueries(): Flow<List<String>>
    suspend fun recordQuery(query: String)
    suspend fun clearRecentQueries()
}

interface IntegrationRepository {
    fun observeSnapshot(): Flow<IntegrationSnapshot>
    suspend fun getSnapshot(): AppResult<IntegrationSnapshot>
}

interface TodayRepository {
    fun observeDashboard(): Flow<TodayDashboard>
    suspend fun getDashboard(): AppResult<TodayDashboard>
}
