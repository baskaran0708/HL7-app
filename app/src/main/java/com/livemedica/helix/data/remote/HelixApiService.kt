package com.livemedica.helix.data.remote

import com.livemedica.helix.core.network.ApiRoutes
import com.livemedica.helix.data.model.AppointmentDto
import com.livemedica.helix.data.model.DoctorDto
import com.livemedica.helix.data.model.NotificationDto
import com.livemedica.helix.data.model.OrderDto
import com.livemedica.helix.data.model.PatientDto
import com.livemedica.helix.data.model.ReportDto
import com.livemedica.helix.data.model.SearchResultsDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The future AWS backend contract.
 *
 * Defined now and compiled against real DTOs so the endpoint shapes can be agreed with the backend
 * team before either side builds. **No implementation is wired**: there is no Retrofit instance, no
 * base URL, and no repository calls this. Phase 2 adds a `NetworkModule` that builds this service
 * and swaps the bindings in `data/di/RepositoryModule.kt` from `Mock*` to `Api*`.
 *
 * Note what is deliberately absent from every signature: any hospital database handle. Android
 * talks only to this HTTPS API. PostgreSQL and Mirth Connect sit behind the backend and are never
 * reachable from the device.
 */
interface HelixApiService {

    @GET(ApiRoutes.DOCTOR_PROFILE)
    suspend fun getDoctorProfile(): DoctorDto

    @GET(ApiRoutes.DOCTOR_APPOINTMENTS)
    suspend fun getAppointments(
        @Query("date") isoDate: String? = null,
        @Query("facilityId") facilityId: String? = null,
    ): List<AppointmentDto>

    @GET(ApiRoutes.DOCTOR_APPOINTMENT)
    suspend fun getAppointment(@Path("id") id: String): AppointmentDto

    @GET(ApiRoutes.DOCTOR_ORDERS)
    suspend fun getOrders(
        @Query("priority") priority: String? = null,
        @Query("status") status: String? = null,
        @Query("q") query: String? = null,
    ): List<OrderDto>

    @GET(ApiRoutes.DOCTOR_ORDER)
    suspend fun getOrder(@Path("id") id: String): OrderDto

    @GET(ApiRoutes.DOCTOR_RESULTS)
    suspend fun getResults(@Query("status") status: String? = null): List<ReportDto>

    @GET(ApiRoutes.DOCTOR_RESULT)
    suspend fun getResult(@Path("id") id: String): ReportDto

    @GET(ApiRoutes.PATIENT)
    suspend fun getPatient(@Path("id") id: String): PatientDto

    @GET(ApiRoutes.SEARCH)
    suspend fun search(@Query("q") query: String): SearchResultsDto

    @GET(ApiRoutes.NOTIFICATIONS)
    suspend fun getNotifications(): List<NotificationDto>

    @POST(ApiRoutes.NOTIFICATION_READ)
    suspend fun markNotificationRead(@Path("id") id: String)

    /**
     * Signing is a POST with an empty body: the identity of the signer comes from the bearer token,
     * never from the client, so the device cannot claim to sign as someone else.
     */
    @POST(ApiRoutes.SIGN_REPORT)
    suspend fun signReport(@Path("id") id: String, @Body body: Map<String, String> = emptyMap()): ReportDto
}
