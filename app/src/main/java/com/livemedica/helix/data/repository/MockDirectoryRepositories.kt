package com.livemedica.helix.data.repository

import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.data.mock.MockCallSimulator
import com.livemedica.helix.data.mock.MockClinicalStore
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.model.Facility
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.Study
import com.livemedica.helix.domain.repository.AppointmentRepository
import com.livemedica.helix.domain.repository.DoctorRepository
import com.livemedica.helix.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDoctorRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : DoctorRepository {

    override fun observeCurrentDoctor(): Flow<Doctor> = store.doctor

    override suspend fun getCurrentDoctor(): AppResult<Doctor> =
        simulator.call(cachedOnOffline = { store.doctor.value }) { store.doctor.value }

    override suspend fun setOnCall(onCall: Boolean): AppResult<Doctor> =
        simulator.call { store.setOnCall(onCall) }
}

@Singleton
class MockAppointmentRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : AppointmentRepository {

    override fun observeTodayAppointments(): Flow<List<Appointment>> =
        store.appointments.map { list -> list.on(LocalDate.now()) }

    override suspend fun getTodayAppointments(): AppResult<List<Appointment>> =
        simulator.call(cachedOnOffline = { store.appointments.value.on(LocalDate.now()) }) {
            store.appointments.value.on(LocalDate.now())
        }

    override suspend fun getAppointments(date: LocalDate, facilityId: String?): AppResult<List<Appointment>> =
        simulator.call(cachedOnOffline = { store.appointments.value.on(date, facilityId) }) {
            store.appointments.value.on(date, facilityId)
        }

    override suspend fun getAppointment(id: String): AppResult<Appointment> = simulator.call {
        store.appointments.value.firstOrNull { it.id == id }
            ?: error("That appointment is no longer available.")
    }

    override suspend fun getFacilities(): AppResult<List<Facility>> = simulator.call { store.facilities }

    private fun List<Appointment>.on(date: LocalDate, facilityId: String? = null) =
        filter { it.start.toLocalDate() == date && (facilityId == null || it.facilityId == facilityId) }
            .sortedBy { it.start }
}

@Singleton
class MockPatientRepository @Inject constructor(
    private val store: MockClinicalStore,
    private val simulator: MockCallSimulator,
) : PatientRepository {

    override suspend fun getPatient(id: String): AppResult<Patient> = simulator.call {
        store.patients.value.firstOrNull { it.id == id }
            ?: error("That patient record is no longer available.")
    }

    override fun observePatient(id: String): Flow<Patient?> =
        store.patients.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getAppointmentsFor(patientId: String): AppResult<List<Appointment>> = simulator.call {
        store.appointments.value.filter { it.patientId == patientId }.sortedByDescending { it.start }
    }

    override suspend fun getOrdersFor(patientId: String): AppResult<List<Order>> = simulator.call {
        store.orders.value.filter { it.patientId == patientId }.sortedByDescending { it.orderedAt }
    }

    override suspend fun getReportsFor(patientId: String): AppResult<List<RadiologyReport>> = simulator.call {
        store.reports.value.filter { it.patientId == patientId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getStudiesFor(patientId: String): AppResult<List<Study>> = simulator.call {
        store.studies.value.filter { it.patientId == patientId }.sortedByDescending { it.performedAt }
    }
}
