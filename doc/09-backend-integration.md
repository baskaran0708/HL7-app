# 09 — Backend Integration

> How to turn the Phase 1 mock app into a real one: the AWS contract that already exists in source, and the exact, line-by-line swap from `Mock*` to `Api*`.

**Siblings:** `02-architecture.md` (the big picture) · `06-data-layer.md` (the mock store and the demo data you're replacing) · `10-security.md` (threat model)

---

## Contents

1. [The architecture rule](#1-the-architecture-rule)
2. [What already exists in source](#2-what-already-exists-in-source)
3. [The endpoint contract](#3-the-endpoint-contract)
4. [The DTO layer](#4-the-dto-layer)
5. [**THE SWAP** — the migration guide](#5-the-swap--the-migration-guide)
6. [Auth](#6-auth)
7. [Room and offline](#7-room-and-offline)
8. [Realtime](#8-realtime)
9. [Gaps in the current contract](#9-gaps-in-the-current-contract)
10. [Pre-flight checklist](#10-pre-flight-checklist)

---

## 1. The architecture rule

```
Hospital HIS / RIS
      │  HL7 v2  (ADT · SIU · ORM · ORU)
      ▼
Mirth Connect
      │
      ▼
PostgreSQL
      │
      ▼
AWS REST API  ──── HTTPS ────►  LmiHL7 Android app
```

**The device talks to exactly one thing: the AWS HTTPS API.** Never PostgreSQL. Never Mirth. Never a JDBC connection, a message-queue credential, or an S3 key.

This isn't a style preference, it's the whole security posture:

- **A credential on a device is a compromised credential.** APKs get decompiled in minutes; `strings` on a binary is not a hard attack. Anything shipped in the app — a DB password, a Mirth API key, a static token — must be treated as public the day you ship it. A JDBC string in an APK is a database breach with extra steps.
- **PHI must not sit behind a JDBC string.** A direct database connection gives the device the database's permission model, not the user's. There's no per-user row filtering, no audit trail tied to a clinician, no way to revoke one phone. The API is where authorisation, audit logging and rate limiting live, and it's the only layer that can enforce "Dr. Chen may read *these* reports".
- **Signing identity comes from the token, not the client.** The app sends "sign report X"; the backend decides *who* signed it from the bearer token. The device can never claim to sign as someone else.
- **Mirth and Postgres stay in a private subnet.** No public ingress. The API is the only thing with a route to them.

The API interface in source makes this explicit:

```kotlin
/**
 * Note what is deliberately absent from every signature: any hospital database handle. Android
 * talks only to this HTTPS API. PostgreSQL and Mirth Connect sit behind the backend and are never
 * reachable from the device.
 */
interface HelixApiService { ... }
```

If someone proposes a "quick direct read from the reporting replica", the answer is no.

---

## 2. What already exists in source

Phase 1 makes **zero network calls**. But the seams are cut and compiled, so Phase 2 is filling in blanks rather than designing from scratch.

| File | Status | What it is |
|---|---|---|
| `app/src/main/java/com/livemedica/helix/core/network/ApiRoutes.kt` | ✅ complete | Every route as a constant. No base URL. |
| `app/src/main/java/com/livemedica/helix/data/remote/HelixApiService.kt` | ✅ complete, **not wired** | Retrofit interface, compiled, never instantiated. |
| `app/src/main/java/com/livemedica/helix/data/model/Dtos.kt` | ✅ complete, **unused at runtime** | `@Serializable` wire models. |
| `app/src/main/java/com/livemedica/helix/core/network/AuthInterceptor.kt` | ✅ written, **installed nowhere** | Adds `Authorization: Bearer …`. |
| `app/src/main/java/com/livemedica/helix/core/network/TokenProvider.kt` | interface ✅ / impl = stub | `InMemoryTokenProvider` holds nothing. |
| `app/src/main/java/com/livemedica/helix/core/network/HelixRealtimeEvent.kt` | sealed type ✅ / **no implementation** | Six push event types + `RealtimeEventSource`. |
| `app/src/main/java/com/livemedica/helix/data/di/RepositoryModule.kt` | ✅ the seam | Every binding points at a `Mock*`. |
| `NetworkModule` | ❌ **does not exist yet** | You write it. Step 1 below. |
| Mappers (DTO → domain) | ❌ **do not exist yet** | You write them. Step 3 below. |
| Room | ❌ **not present** — no dependency, no entities, no DAOs | Optional, step 7. |

Retrofit 3.0.0, the kotlinx-serialization converter, OkHttp 5.5.0 and the logging interceptor are **already declared** in `app/build.gradle.kts` and `gradle/libs.versions.toml`:

```kotlin
// Declared so the remote layer compiles against a real contract. Phase 1 makes no calls:
// every repository is bound to a mock implementation in data/di/RepositoryModule.kt.
implementation(libs.retrofit)
implementation(libs.retrofit.serialization)
implementation(libs.okhttp)
implementation(libs.okhttp.logging)
```

So you don't need to add a single dependency to get the network layer running.

### `ApiRoutes` — why constants

```kotlin
/**
 * Declared as constants, in one place, so no URL is ever written inline in a repository — and so
 * the contract can be reviewed against the backend before a single call is implemented.
 *
 * The base URL is deliberately absent: it will arrive as a build-config field per build type, never
 * as a literal in source. Nothing in Phase 1 performs a network call.
 */
object ApiRoutes {
    const val VERSION = "v1"
    private const val BASE = "api/$VERSION"
    ...
}
```

Note the paths are **relative** (`api/v1/…`, no leading slash). That's intentional — Retrofit resolves them against a `baseUrl` that must therefore end in `/`.

---

## 3. The endpoint contract

Every method in `HelixApiService`, its route constant, its resolved path, and what it returns.

| Method | Verb | Route constant | Resolved path | Returns |
|---|---|---|---|---|
| `getDoctorProfile()` | GET | `ApiRoutes.DOCTOR_PROFILE` | `api/v1/doctor/profile` | `DoctorDto` |
| `getAppointments(isoDate, facilityId)` | GET | `ApiRoutes.DOCTOR_APPOINTMENTS` | `api/v1/doctor/appointments` | `List<AppointmentDto>` |
| `getAppointment(id)` | GET | `ApiRoutes.DOCTOR_APPOINTMENT` | `api/v1/doctor/appointments/{id}` | `AppointmentDto` |
| `getOrders(priority, status, query)` | GET | `ApiRoutes.DOCTOR_ORDERS` | `api/v1/doctor/orders` | `List<OrderDto>` |
| `getOrder(id)` | GET | `ApiRoutes.DOCTOR_ORDER` | `api/v1/doctor/orders/{id}` | `OrderDto` |
| `getResults(status)` | GET | `ApiRoutes.DOCTOR_RESULTS` | `api/v1/doctor/results` | `List<ReportDto>` |
| `getResult(id)` | GET | `ApiRoutes.DOCTOR_RESULT` | `api/v1/doctor/results/{id}` | `ReportDto` |
| `getPatient(id)` | GET | `ApiRoutes.PATIENT` | `api/v1/patients/{id}` | `PatientDto` |
| `search(query)` | GET | `ApiRoutes.SEARCH` | `api/v1/search` | `SearchResultsDto` |
| `getNotifications()` | GET | `ApiRoutes.NOTIFICATIONS` | `api/v1/notifications` | `List<NotificationDto>` |
| `markNotificationRead(id)` | POST | `ApiRoutes.NOTIFICATION_READ` | `api/v1/notifications/{id}/read` | `Unit` |
| `signReport(id, body)` | POST | `ApiRoutes.SIGN_REPORT` | `api/v1/reports/{id}/sign` | `ReportDto` |

Query parameters:

| Method | Query params |
|---|---|
| `getAppointments` | `date` (ISO-8601 local date, nullable), `facilityId` (nullable) |
| `getOrders` | `priority`, `status`, `q` — all nullable strings |
| `getResults` | `status` (nullable) |
| `search` | `q` (required) |

Route constants declared in `ApiRoutes` but **with no corresponding service method yet**:

| Constant | Path | Note |
|---|---|---|
| `ApiRoutes.LOGIN` | `api/v1/auth/login` | Auth flow is not implemented — see §6 |
| `ApiRoutes.REFRESH` | `api/v1/auth/refresh` | Auth flow is not implemented — see §6 |
| `ApiRoutes.INTEGRATION_STATUS` | `api/v1/integration/status` | No method, and no `IntegrationSnapshotDto` — see §9 |

### The one method worth reading the KDoc for

```kotlin
/**
 * Signing is a POST with an empty body: the identity of the signer comes from the bearer token,
 * never from the client, so the device cannot claim to sign as someone else.
 */
@POST(ApiRoutes.SIGN_REPORT)
suspend fun signReport(@Path("id") id: String, @Body body: Map<String, String> = emptyMap()): ReportDto
```

The backend must also make this **idempotent** — a second POST for an already-signed report returns the existing report, it does not stamp a second signature. The mock enforces the same rule (`MockClinicalStore.signReport`, see `06-data-layer.md`), so the UI already behaves correctly against it.

---

## 4. The DTO layer

`data/model/Dtos.kt` — eight `@Serializable` classes: `DoctorDto`, `PatientDto`, `AppointmentDto`, `OrderDto`, `ReportDto`, `StudyDto`, `NotificationDto`, `SearchResultsDto`.

### Why they're separate from the domain models

```kotlin
/**
 * Kept separate from the domain models on purpose. The API will evolve on the backend's schedule
 * and will carry HL7-derived quirks (string enums, ISO-8601 timestamps, nullable fields that are
 * only nullable because a hospital feed omitted them). Absorbing that here means the domain layer
 * and every screen stay clean, and a breaking API change is a mapper edit rather than an app-wide
 * refactor.
 */
```

The specific quirks the DTOs absorb:

| Quirk | DTO | Domain |
|---|---|---|
| **String enums.** HL7 delivers `"F"` / `"M"` / `"X"`, `"CT"`, `"STAT"`, `"PENDING_SIGNATURE"` as text. Adding a value server-side must not crash the app. | `val sex: String`, `val modality: String`, `val priority: String`, `val status: String` | `Sex`, `Modality`, `Priority`, `ReportStatus` — real enums |
| **ISO-8601 strings.** No JSON date type; no `kotlinx.datetime` dependency in this project. | `val start: String`, `@SerialName("ordered_at") val orderedAt: String` | `LocalDateTime` |
| **Nullable-because-a-feed-omitted-it.** A room number or a comparison line missing from an ORM isn't an error, it's Tuesday. | `val room: String? = null`, `val comparison: String? = null`, `val technique: String? = null` | non-null `String` (mapper supplies `""` or a sensible default) |
| **snake_case wire names.** | `@SerialName("patient_name") val patientName: String` | camelCase properties |
| **Flattened notification targets.** | `targetType: String?` + `targetId: String?` | `NotificationTarget` sealed type |
| **Defaults for forward compatibility.** | `@SerialName("on_call") val onCall: Boolean = false`, `val allergies: List<String> = emptyList()` | required fields |

The payoff: when the backend renames `clinical_indication` to `reason_for_exam`, you edit one `@SerialName` and one mapper line. Zero composables change, zero ViewModels change.

The mappers are **the place where the impedance mismatch is allowed to be ugly.** Everything above them stays clean.

> **Not implemented yet:** there are no mapper functions in the codebase. `Dtos.kt` says *"Unused at runtime in Phase 1."* You write them in step 3 below.

---

## 5. THE SWAP — the migration guide

Five steps. Do them in order. After step 4 you have a real app; step 5 is cleanup.

---

### Step 1 — Add a `NetworkModule`

Create `app/src/main/java/com/livemedica/helix/data/di/NetworkModule.kt`. Nothing like this exists yet.

```kotlin
package com.livemedica.helix.data.di

import com.livemedica.helix.BuildConfig
import com.livemedica.helix.core.network.AuthInterceptor
import com.livemedica.helix.data.remote.HelixApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // The backend may add fields ahead of an app release; tolerate them.
        ignoreUnknownKeys = true
        // Absent optional fields fall back to the DTO defaults rather than throwing.
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            // NEVER BODY in release: response bodies are PHI. See the pre-flight checklist.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        // Must end in "/" — ApiRoutes paths are relative ("api/v1/...", no leading slash).
        .baseUrl(BuildConfig.HELIX_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideHelixApiService(retrofit: Retrofit): HelixApiService =
        retrofit.create(HelixApiService::class.java)
}
```

`AuthInterceptor` already has `@Inject constructor(private val tokenProvider: TokenProvider)`, and `TokenProvider` is already provided by `NetworkProvisionModule` at the bottom of `RepositoryModule.kt` — so Hilt can construct it with no extra binding.

---

### Step 2 — Where the base URL goes

**Never a literal in source. Never in git. A build-config field, per build type.**

`buildConfig` is currently **not enabled**. `app/build.gradle.kts` today reads:

```kotlin
buildFeatures {
    compose = true
}
```

You must add `buildConfig = true`, or `BuildConfig.HELIX_API_BASE_URL` (and `BuildConfig.DEBUG` in the module above) won't exist:

```kotlin
android {
    // ...

    buildFeatures {
        compose = true
        buildConfig = true          // <-- required; currently missing
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "HELIX_API_BASE_URL",
                "\"${project.findProperty("HELIX_API_BASE_URL_DEBUG") ?: ""}\"",
            )
        }
        release {
            buildConfigField(
                "String",
                "HELIX_API_BASE_URL",
                "\"${project.findProperty("HELIX_API_BASE_URL_RELEASE") ?: ""}\"",
            )
            optimization {
                enable = true       // currently `false` — see the pre-flight checklist
            }
        }
    }
}
```

The values come from **outside the repo**:

- Local dev: `~/.gradle/gradle.properties` (a user-level file, never committed), or `local.properties` — already gitignored.
- CI: an environment variable injected into the Gradle invocation, e.g. `-PHELIX_API_BASE_URL_RELEASE=$HELIX_API_BASE_URL`.

```properties
# ~/.gradle/gradle.properties  — NOT in the repository
HELIX_API_BASE_URL_DEBUG=https://<placeholder-dev-host>/
HELIX_API_BASE_URL_RELEASE=https://<placeholder-prod-host>/
```

> `<placeholder-dev-host>` and `<placeholder-prod-host>` are **placeholders**. The real hosts come from whoever owns the AWS account; they are not recorded in this document or anywhere in the repository.

Three rules:

1. **Trailing slash.** `ApiRoutes` paths are relative; Retrofit's `baseUrl` must end in `/` or path resolution silently drops segments.
2. **HTTPS only.** No `http://` value, not even for a local emulator run against a laptop — set up a tunnel or a local TLS cert instead. Android blocks cleartext by default and you should not be adding a `networkSecurityConfig` exception to get around it.
3. **Fail loudly on an empty value.** The `?: ""` fallback above means a misconfigured machine produces an immediately obvious crash at `Retrofit.Builder().baseUrl("")` rather than silently pointing at the wrong environment. If you'd rather fail at build time, replace it with `?: error("HELIX_API_BASE_URL_RELEASE not set")`.

---

### Step 3 — Write an `ApiXxxRepository`

One new class per domain interface, in `data/repository/`. It implements **the same interface** the mock implements — that's the entire point.

Here's a complete worked example: `ApiResultRepository` implementing `ResultRepository`.

First, the mappers. Put them in a new file `app/src/main/java/com/livemedica/helix/data/model/ReportMappers.kt`:

```kotlin
package com.livemedica.helix.data.model

import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.ReportStatus
import com.livemedica.helix.domain.model.Sex
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * DTO -> domain. This is where the HL7-derived mess is allowed to live: string enums that may
 * carry a value this build has never heard of, ISO-8601 text, and fields that are nullable only
 * because a hospital feed omitted them.
 */
fun ReportDto.toDomain(): RadiologyReport = RadiologyReport(
    id = id,
    orderId = orderId,
    studyId = studyId,
    accessionNumber = accessionNumber,
    patientId = patientId,
    patientName = patientName,
    patientAge = patientAge,
    patientSex = patientSex.toSex(),
    mrn = mrn,
    modality = modality.toModality(),
    procedure = procedure,
    clinicalIndication = clinicalIndication,
    // Absent because the feed didn't supply one, not because something failed.
    comparison = comparison.orEmpty(),
    technique = technique.orEmpty(),
    findings = findings,
    impression = impression,
    priority = priority.toPriority(),
    status = status.toReportStatus(),
    radiologist = radiologist,
    createdAt = createdAt.toLocalDateTime(),
    finalizedAt = finalizedAt?.toLocalDateTime(),
    signedAt = signedAt?.toLocalDateTime(),
    isCritical = isCritical,
)

/**
 * Unknown wire values degrade instead of crashing. A modality the backend added last week must
 * not take down a radiologist's worklist mid-shift.
 */
internal fun String.toModality(): Modality =
    Modality.entries.firstOrNull { it.code.equals(this, ignoreCase = true) } ?: Modality.XR

internal fun String.toPriority(): Priority =
    Priority.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: Priority.ROUTINE

internal fun String.toReportStatus(): ReportStatus =
    ReportStatus.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: ReportStatus.DRAFT

/** HL7 PID-8 delivers F / M / X. */
internal fun String.toSex(): Sex =
    Sex.entries.firstOrNull { it.code.equals(this, ignoreCase = true) } ?: Sex.OTHER

internal fun String.toLocalDateTime(): LocalDateTime =
    LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
```

Then the repository:

```kotlin
package com.livemedica.helix.data.repository

import com.livemedica.helix.core.common.AppDispatchers
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.data.model.toDomain
import com.livemedica.helix.data.remote.HelixApiService
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.ReportStatus
import com.livemedica.helix.domain.repository.ResultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiResultRepository @Inject constructor(
    private val api: HelixApiService,
    private val dispatchers: AppDispatchers,
) : ResultRepository {

    // Interim cache so observeReports() keeps its Flow contract before Room lands.
    // Replace this with a Room DAO Flow at step 7 and delete the manual emissions.
    private val cache = MutableStateFlow<List<RadiologyReport>>(emptyList())

    override fun observeReports(): Flow<List<RadiologyReport>> = cache.asStateFlow()

    override suspend fun getReports(
        status: ReportStatus?,
        priority: Priority?,
    ): AppResult<List<RadiologyReport>> = apiCall(cachedOnOffline = { cache.value }) {
        api.getResults(status = status?.name)
            .map { it.toDomain() }
            // The endpoint has no `priority` filter, so narrow client-side.
            .filter { priority == null || it.priority == priority }
            .sortedWith(resultOrdering)
            .also { cache.value = it }
    }

    override suspend fun getReport(id: String): AppResult<RadiologyReport> =
        apiCall(cachedOnOffline = { cache.value.firstOrNull { it.id == id } }) {
            api.getResult(id).toDomain()
        }

    override suspend fun signReport(id: String): AppResult<RadiologyReport> = apiCall {
        // Idempotent server-side: a second POST returns the already-signed report unchanged.
        // The signer's identity comes from the bearer token, never from this call.
        val signed = api.signReport(id).toDomain()
        cache.value = cache.value.map { if (it.id == signed.id) signed else it }
        signed
    }

    /**
     * The one place AppResult is produced. Mirrors MockCallSimulator.call so the UI sees exactly
     * the same three cases it has been rendering since Phase 1.
     */
    private suspend fun <T> apiCall(
        cachedOnOffline: (() -> T?)? = null,
        block: suspend () -> T,
    ): AppResult<T> = withContext(dispatchers.io) {
        try {
            AppResult.Success(block())
        } catch (io: IOException) {
            // No route to the host: DNS, timeout, radio off. Not a failure — an offline.
            AppResult.Offline(cachedOnOffline?.invoke())
        } catch (http: HttpException) {
            AppResult.Failure(http.userMessage(), http)
        }
    }

    private fun HttpException.userMessage(): String = when (code()) {
        401, 403 -> "Your session has expired. Sign in again to continue."
        404 -> "That report is no longer available."
        in 500..599 -> "We couldn't reach the server. Your data is safe — try again in a moment."
        else -> "Something went wrong reaching the server. Try again in a moment."
    }

    private companion object {
        val resultOrdering: Comparator<RadiologyReport> =
            compareBy<RadiologyReport> { it.priority.ordinal }.thenByDescending { it.createdAt }
    }
}
```

**Things to copy from this pattern into every other `Api*Repository`:**

| Pattern | Why |
|---|---|
| `IOException → AppResult.Offline(cached)`, `HttpException → AppResult.Failure` | This is the whole reason `Offline` is a separate case. A dropped connection shows last-synced data behind a banner; a 500 shows an error. See `06-data-layer.md` §4. |
| Never let a raw exception message reach the UI | `userMessage()` maps status codes to clinician-readable text. Never `e.message` — that leaks stack detail and reads like a crash. |
| Never put an id, MRN or name in the message | PHI. If you must reference a record, use `ClinicalFormat.maskIdentifier()`. |
| `withContext(dispatchers.io)` | Inject `AppDispatchers` (`core/common/AppDispatchers.kt`) rather than touching `Dispatchers.IO`, so tests can substitute a `TestDispatcher`. The mocks don't do this because they're in-memory. |
| Sorting stays in the repository | Same comparator as `MockResultRepository`, so the UI's ordering assumptions don't change. |
| Keep the `Flow` contract | `observeReports()` must still emit. The `MutableStateFlow` above is a stopgap — Room's DAO `Flow` is the real answer (§7). |

Do the same for `ApiDoctorRepository`, `ApiAppointmentRepository`, `ApiPatientRepository`, `ApiOrderRepository`, `ApiTodayRepository`, `ApiNotificationRepository`, `ApiSearchRepository`, `ApiIntegrationRepository`. Check §9 first — three of those have no endpoint yet.

> **Migrate one repository at a time.** The bindings are independent, so `ApiResultRepository` can go live against the real backend while everything else still reads from `MockClinicalStore`. That's the safest way to do this and it's worth taking advantage of.

---

### Step 4 — Change ONE line per repository

`app/src/main/java/com/livemedica/helix/data/di/RepositoryModule.kt` is, verbatim from its KDoc:

```kotlin
/**
 * The single seam between Phase 1 and Phase 2.
 *
 * Every binding below points at a `Mock*` implementation. When the AWS backend is ready, each line
 * changes to the corresponding `Api*` implementation and nothing else in the app moves — no
 * ViewModel, no composable, no navigation.
 */
```

The edit, for the worked example:

```kotlin
// before
@Binds @Singleton
abstract fun bindResultRepository(impl: MockResultRepository): ResultRepository

// after
@Binds @Singleton
abstract fun bindResultRepository(impl: ApiResultRepository): ResultRepository
```

That's it. Update the import at the top of the file, rebuild, done.

The full set of bindings to move, in the order they appear:

| Binding | Phase 1 | Phase 2 |
|---|---|---|
| `bindDoctorRepository` | `MockDoctorRepository` | `ApiDoctorRepository` |
| `bindAppointmentRepository` | `MockAppointmentRepository` | `ApiAppointmentRepository` |
| `bindPatientRepository` | `MockPatientRepository` | `ApiPatientRepository` |
| `bindOrderRepository` | `MockOrderRepository` | `ApiOrderRepository` |
| `bindResultRepository` | `MockResultRepository` | `ApiResultRepository` |
| `bindTodayRepository` | `MockTodayRepository` | `ApiTodayRepository` |
| `bindNotificationRepository` | `MockNotificationRepository` | `ApiNotificationRepository` |
| `bindSearchRepository` | `MockSearchRepository` | `ApiSearchRepository` |
| `bindIntegrationRepository` | `MockIntegrationRepository` | `ApiIntegrationRepository` |
| `bindSettingsRepository` | `SettingsRepositoryImpl` | **unchanged** — DataStore is the real thing already |
| `bindNetworkMonitor` | `SettingsAwareNetworkMonitor` | **unchanged**, but consider dropping the `simulateOffline` AND once there's a real backend to disconnect from |

**What does NOT change — and this is the point of the whole architecture:**

- ❌ No ViewModel. They depend on `domain/repository` interfaces only.
- ❌ No composable. They render `UiState`, which comes from ViewModels.
- ❌ No navigation code. `HelixNavHost`, `HelixRoute`, `HelixNavActions` are untouched.
- ❌ No domain model. `RadiologyReport` is the same class either way.
- ❌ No theme, design system, or `core/ui` state host.

If a swap forces you to change a ViewModel, the `Api*` implementation has diverged from the interface contract and that's the bug — fix the repository, not the ViewModel.

---

### Step 5 — Retire the mocks

Once **every** binding in `RepositoryModule.kt` points at an `Api*` implementation, delete:

| File | Note |
|---|---|
| `data/mock/SeedClinicalData.kt` | The demo dataset. ~725 lines of fictional patients. |
| `data/mock/MockClinicalStore.kt` | The shared `StateFlow` store. |
| `data/mock/MockCallSimulator.kt` | Artificial latency + dev switches. |
| `data/repository/MockDirectoryRepositories.kt` | Doctor / Appointment / Patient mocks. |
| `data/repository/MockWorkflowRepositories.kt` | Order / Result / Today mocks. |
| `data/repository/MockSupportRepositories.kt` | Notification / Integration / Search mocks. |

Then follow the thread:

1. `HelixSettings.simulateOffline` and `simulateError` lose their only consumer in `MockCallSimulator`. `simulateOffline` is *also* read by `SettingsAwareNetworkMonitor` — decide whether to keep it as a genuine offline-mode toggle or remove it and the two DataStore keys (`dev_simulate_offline`, `dev_simulate_error`) plus the Settings → Developer section.
2. `app/src/test/java/com/livemedica/helix/data/MockResultRepositoryTest.kt` and `MockSearchRepositoryTest.kt` will stop compiling. Rewrite them against the `Api*` classes with a fake `HelixApiService`.
3. `app/src/test/java/com/livemedica/helix/data/FakeSettingsRepository.kt` stays useful.

**Do not delete these before every binding has moved.** Deleting `SeedClinicalData.kt` while one repository still resolves `MockClinicalStore` breaks the build; worse, deleting it during a partial migration leaves you with no way to demo.

> **Consider keeping a trimmed seed for Compose previews and instrumentation tests.** If you do, move it to `src/debug/` or `src/test/` so it cannot ship in a release APK, and cut it down to two or three records.

---

## 6. Auth

### Where it stands

| Piece | Status |
|---|---|
| `TokenProvider` interface | ✅ defined |
| `AuthInterceptor` | ✅ written, **installed nowhere** (nothing builds an `OkHttpClient` yet) |
| `InMemoryTokenProvider` | ⚠️ stub — holds two `@Volatile String?` fields, persists nothing |
| Login screen / flow | ❌ **not implemented** |
| `login` / `refresh` service methods | ❌ **not implemented** — the routes exist in `ApiRoutes`, the methods don't |
| 401 `Authenticator` | ❌ **not implemented** |

`NetworkProvisionModule` (bottom of `RepositoryModule.kt`) is explicit about it:

```kotlin
/**
 * Phase 1 holds no token at all. This exists so the auth plumbing has a real shape to grow
 * into; it is never populated from source, from a build config, or from the network yet.
 */
@Provides
@Singleton
fun provideTokenProvider(): TokenProvider = InMemoryTokenProvider()
```

### The interface you'll implement against

```kotlin
interface TokenProvider {
    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun updateTokens(access: String, refresh: String)
    suspend fun clear()
}
```

### What `AuthInterceptor` already does right

```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()

    // Auth endpoints must not carry a stale token, or a refresh can fail on the token it is
    // trying to replace.
    if (request.url.encodedPath.contains("/auth/")) return chain.proceed(request)

    // OkHttp interceptors are blocking; the token read is a fast in-memory/keystore lookup.
    val token = runBlocking { tokenProvider.accessToken() } ?: return chain.proceed(request)

    return chain.proceed(
        request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build(),
    )
}
```

Two decisions already made for you: `/auth/` requests go out unauthenticated (so a refresh can't fail on the very token it's replacing), and a missing token doesn't throw — the request proceeds and the server returns 401, which is the correct place for that decision.

The `runBlocking` is acceptable **only** while the token read is a fast in-memory or Keystore lookup. If a production `TokenProvider` ever does disk I/O or a network refresh inside `accessToken()`, this becomes a main-thread hazard — do the refresh in an `Authenticator`, not the interceptor.

### What a production implementation needs

1. **Encrypted at rest.** Back `TokenProvider` with `EncryptedSharedPreferences` (or your own Keystore-wrapped storage — an AES key in the Android Keystore, ideally `setUserAuthenticationRequired` where the UX allows). Never plain `SharedPreferences`, never the `helix_settings` DataStore — `HelixPreferencesDataSource` explicitly excludes tokens, keep it that way.
2. **A 401 `Authenticator`, not a retry loop.** OkHttp's `Authenticator` is the right hook: it fires on a 401, performs the refresh **once**, and replays the original request. Guard against a refresh storm (bail after `responseCount(response) > 1`) and serialise concurrent refreshes with a mutex so ten parallel requests don't fire ten refreshes.
3. **Refresh rotation.** Assume the backend issues a new refresh token on every refresh and invalidates the old one. Persist the new pair atomically via `updateTokens(access, refresh)`. If a refresh returns 401/403, call `clear()` and route the user to login — a rotated-away refresh token means the session is gone, possibly because it was replayed elsewhere.
4. **Short-lived access tokens.** Minutes, not days. The refresh token is the long-lived credential and it's the one that must be encrypted.
5. **Clear on logout, and on token invalidation.** `clear()` must wipe both tokens *and* any Room-cached clinical data — see §7. A logged-out phone must not still hold a worklist.
6. **Biometric gate.** `HelixSettings.biometricUnlockEnabled` is stored today but wired to nothing (its KDoc says "Architecture-ready only… no BiometricPrompt is wired up"). Gating token *release* behind `BiometricPrompt` is the natural implementation.

### Non-negotiable

**No token, password, API key or client secret is ever hard-coded, committed, or placed in a build-config field.** Build config is for the base URL and feature flags — it ends up as a `String` constant in the APK. Tokens arrive at runtime from `POST api/v1/auth/login` and live only in `TokenProvider`.

*(Deliberately not writing sample credential-storage code here — a copy-pasteable snippet is how insecure key handling spreads. Build it against the current Jetpack Security guidance and put it through a security review; see `10-security.md`.)*

---

## 7. Room and offline

**Not present today.** No Room dependency in `gradle/libs.versions.toml`, no entities, no DAOs, no database class.

### Why the current models are already Room-ready

From `ClinicalModels.kt`:

```kotlin
/**
 * All of these are flat, immutable value types holding primitives and stable ids rather than object
 * graphs. [...] and directly translatable to Room entities when offline support lands.
 */
```

Concretely:

- **Flat.** `Order` carries `patientName`, `patientAge`, `patientSex`, `mrn` inline rather than a nested `Patient`. That's a table row. No `@Relation`, no `@Embedded`, no join to render a worklist item.
- **Stable string ids on every model.** `Order.id`, `RadiologyReport.id`, `Patient.id` — straight `@PrimaryKey`s.
- **Primitives, enums, and three time types.** Everything is `String` / `Int` / `Boolean` / enum / `LocalDate` / `LocalDateTime` / `List<String>`. Room handles the first four out of the box; the rest need four small `@TypeConverter`s (`LocalDate ↔ String`, `LocalDateTime ↔ String`, `List<String> ↔ String`, and one for `Patient.history` if you don't normalise `HistoryEntry` into its own table).
- **No computed state to persist.** `Appointment.end` and `RadiologyReport.isSigned` are `get()` properties, not stored fields — Room ignores them (mark with `@Ignore` if it complains).

The only models that need thought are `Patient` (its `history: List<HistoryEntry>` wants either a converter or its own table) and the three aggregates — `TodayDashboard`, `SearchResults`, `IntegrationSnapshot` — which are **derived views, not entities**. Don't persist those; recompute them from the cached rows exactly as `MockTodayRepository.buildDashboard` does today.

### Where Room goes

Between the API and the repository. The repository becomes the orchestrator:

```
ViewModel
   │  observeReports(): Flow<List<RadiologyReport>>
   ▼
ApiResultRepository
   ├── observeX()  → Room DAO Flow          (single source of truth for reads)
   └── getX()/doX() → HelixApiService → map → DAO upsert → DAO Flow re-emits
```

This is the standard offline-first shape and it fits the existing interfaces exactly:

1. **`observeX()` returns the DAO's `Flow`.** Room emits on every write, so the `MutableStateFlow` stopgap from step 3 disappears and the reactive contract is genuinely reactive — no manual `cache.value =` bookkeeping.
2. **`getX()` becomes fetch-then-upsert.** Call the API, map DTO → domain, upsert into Room, return `AppResult.Success`. The screen was already re-rendering from the DAO `Flow`, so the return value is mostly about surfacing errors.
3. **`AppResult.Offline` gets real.** On `IOException`, read from the DAO and return `AppResult.Offline(cached)`. *This is what `Offline` was designed for* (see `06-data-layer.md` §4) — the radiologist in the lift sees the worklist they loaded ninety seconds ago with an honest "last synced" banner, not an error screen.
4. **Writes need a queue.** `signReport` while offline can't just fail. Either refuse it with a clear message (safest for a legally significant signature) or write a pending-operation row and replay it with WorkManager on reconnect. **Signing is a medico-legal act — decide this with the clinical stakeholders, not in a code review.**

### Encryption

A Room database holding reports, MRNs and patient names is **PHI on a device**. Before it ships: SQLCipher or an equivalent, plus a documented wipe on logout and on token invalidation. Part of `10-security.md`, not an afterthought.

---

## 8. Realtime

`core/network/HelixRealtimeEvent.kt`. **Nothing implements `RealtimeEventSource` in Phase 1** — the mock store mutates in-process instead.

```kotlin
/**
 * Modelled as a sealed type now so that adding a transport later cannot quietly introduce an event
 * the app fails to handle — the `when` over this type will stop compiling instead.
 */
sealed interface HelixRealtimeEvent {
    data class NewAppointment(val appointmentId: String) : HelixRealtimeEvent
    data class AppointmentChanged(val appointmentId: String) : HelixRealtimeEvent
    data class NewOrder(val orderId: String) : HelixRealtimeEvent
    data class NewOruResult(val reportId: String) : HelixRealtimeEvent
    data class CriticalResult(val reportId: String) : HelixRealtimeEvent
    data class ReportSignatureRequired(val reportId: String) : HelixRealtimeEvent
}

interface RealtimeEventSource {
    val events: Flow<HelixRealtimeEvent>
    suspend fun connect()
    suspend fun disconnect()
}
```

### The six events and where they come from

| Event | HL7 origin | What the app should do |
|---|---|---|
| `NewAppointment(appointmentId)` | SIU^S12 | Fetch the appointment, upsert, refresh Schedule + Today |
| `AppointmentChanged(appointmentId)` | SIU^S14 / S15 / S26 | Re-fetch that appointment, upsert |
| `NewOrder(orderId)` | ORM^O01 | Fetch the order, upsert, refresh the worklist and `pendingReads` |
| `NewOruResult(reportId)` | ORU^R01 | Fetch the report, upsert, refresh Results and `unsignedReports` |
| `CriticalResult(reportId)` | ORU^R01 with an abnormal flag | Same, **plus** a high-priority notification — this is the one that must interrupt |
| `ReportSignatureRequired(reportId)` | Backend workflow, not a raw HL7 message | Fetch the report, refresh the sign-off queue |

**Critically, every event carries only an id.** No clinical payload. That's deliberate: a push notification passes through Google's infrastructure and lands in the OS notification store — putting a patient name or an impression in it is a PHI leak. The id is a pointer; the app fetches the content over the authenticated HTTPS channel.

### How a transport would feed the store

Both candidates converge on the same shape:

- **FCM** — a `FirebaseMessagingService` receives a data-only message (`{"type": "critical_result", "id": "RPT-…"}`), parses it into a `HelixRealtimeEvent`, and emits it. Survives the app being backgrounded or killed, which is the whole reason a critical-finding alert has to go this way.
- **WebSocket** — an OkHttp `WebSocketListener` wrapped in a `callbackFlow`, connected while the app is foregrounded. Lower latency, better for a live worklist; useless when the process is dead.

Realistically you want both: FCM for wake-up and critical alerts, a WebSocket for the live feed while someone is actively reading.

Wiring, once a source exists:

```kotlin
// Sketch — RealtimeEventSource has no implementation yet.
class RealtimeCoordinator @Inject constructor(
    private val source: RealtimeEventSource,
    private val resultRepository: ResultRepository,
    private val orderRepository: OrderRepository,
    private val appointmentRepository: AppointmentRepository,
    private val dispatchers: AppDispatchers,
) {
    fun start(scope: CoroutineScope) = scope.launch(dispatchers.io) {
        source.connect()
        source.events.collect { event ->
            // Exhaustive: adding a seventh event type breaks this `when` at compile time,
            // which is exactly why HelixRealtimeEvent is sealed.
            when (event) {
                is HelixRealtimeEvent.NewAppointment -> appointmentRepository.getAppointment(event.appointmentId)
                is HelixRealtimeEvent.AppointmentChanged -> appointmentRepository.getAppointment(event.appointmentId)
                is HelixRealtimeEvent.NewOrder -> orderRepository.getOrder(event.orderId)
                is HelixRealtimeEvent.NewOruResult -> resultRepository.getReport(event.reportId)
                is HelixRealtimeEvent.CriticalResult -> resultRepository.getReport(event.reportId)
                is HelixRealtimeEvent.ReportSignatureRequired -> resultRepository.getReport(event.reportId)
            }
        }
    }
}
```

Each `getX` call fetches, maps, and upserts into Room — and the DAO `Flow` pushes it to every screen observing. In Phase 1 that fan-out is exactly what `MockClinicalStore` gives you for free; Room is its replacement, not a new idea.

Alert routing should respect `HelixSettings.criticalAlertsEnabled` / `resultAlertsEnabled` / `scheduleAlertsEnabled` — those preferences already persist and are already surfaced in Settings, they just have nothing to gate yet.

---

## 9. Gaps in the current contract

Before you start, know what `HelixApiService` **cannot** do today. Each of these needs a route, a DTO, and a service method added — agree them with the backend team early.

| Domain method | Gap |
|---|---|
| `DoctorRepository.setOnCall(onCall)` | No endpoint. Needs something like `PATCH api/v1/doctor/on-call`. |
| `AppointmentRepository.getFacilities()` | No endpoint, and **no `FacilityDto`**. |
| `OrderRepository.getStudy(studyId)` / `getStudyForOrder(orderId)` | `StudyDto` exists but there is **no studies route and no service method**. `StudyDto` is currently reachable only nested inside `SearchResultsDto`. |
| `PatientRepository.getAppointmentsFor / getOrdersFor / getReportsFor / getStudiesFor` | No per-patient sub-resources. Either add `api/v1/patients/{id}/orders` etc., or filter the existing list endpoints client-side. |
| `Patient.history` | `PatientDto` has **no `history` field** and there is no `HistoryEntryDto`. The patient detail timeline has no data source. |
| `NotificationRepository.markAllRead()` / `clear(id)` | Only `markNotificationRead(id)` exists. |
| `TodayRepository.getDashboard()` | No `TodayDashboard` endpoint. Either compose it client-side from the other calls (as `MockTodayRepository` does) or add `api/v1/doctor/today`. Client-side is fine and avoids a bespoke aggregate endpoint. |
| `IntegrationRepository.getSnapshot()` | `ApiRoutes.INTEGRATION_STATUS` exists but there is **no service method and no `IntegrationSnapshotDto` / `Hl7MessageDto` / `InterfaceChannelDto`**. |
| `SearchRepository.recordQuery / clearRecentQueries / observeRecentQueries` | Recent queries are local-only by design — keep them on-device (DataStore or Room), don't send search terms containing patient names to the server unless there's an explicit audit requirement. |
| Auth | `login` and `refresh` methods don't exist. See §6. |
| `getResults(status)` has no `priority` param | `ResultRepository.getReports(status, priority)` takes both; filter `priority` client-side (as the worked example does) or add the query param. |

---

## 10. Pre-flight checklist

Run this before the first build that touches a real backend or real PHI.

### Secrets

- [ ] **No base URL literal in source.** It's a `buildConfigField` per build type, sourced from `~/.gradle/gradle.properties` or a CI environment variable. §5 step 2.
- [ ] **No token, password, API key or client secret anywhere in the repo.** Not in source, not in `gradle.properties`, not in a build-config field, not in a comment, not in a test fixture. `grep -rn` for `Bearer `, `apiKey`, `password`, `secret` before every release.
- [ ] **`local.properties` and `.gradle/` stay gitignored** (they already are — check `.gitignore` hasn't drifted).
- [ ] **Scan the git history**, not just the working tree. A secret committed once is a secret forever until the history is rewritten and the credential rotated.

### Transport

- [ ] **HTTPS only.** No cleartext, no `networkSecurityConfig` exception, no `usesCleartextTraffic="true"` — not even for a local emulator run.
- [ ] **Consider certificate pinning.** OkHttp's `CertificatePinner` against the AWS endpoint's leaf or intermediate meaningfully raises the bar against a corporate MITM proxy or a compromised device trust store. The trade-off is real: a certificate rotation with a stale pin bricks every installed app. If you pin, pin the intermediate, ship a backup pin, and have a documented rotation runbook. Decide explicitly — don't leave it unconsidered.
- [ ] **Sensible timeouts.** Connect 15s / read 30s. A radiologist should get a clear offline state, not an indefinite spinner.

### Logging

- [ ] **`HttpLoggingInterceptor.Level.NONE` in release.** `BODY` in a release build writes patient names, MRNs, findings and impressions into logcat — a PHI disclosure straight out of the box. `BASIC` in debug is the maximum.
- [ ] **No PHI in any log, crash report or analytics event.** No patient name, MRN, accession number, DOB, or report text. Ever.
- [ ] **Use `ClinicalFormat.maskIdentifier()`** (`app/src/main/java/com/livemedica/helix/core/utils/ClinicalFormat.kt`) whenever a record must be referenced in something that may be logged or shared:
  ```kotlin
  fun maskIdentifier(value: String): String =
      if (value.length <= VISIBLE_IDENTIFIER_CHARS) "•".repeat(value.length)
      else "${"•".repeat(value.length - VISIBLE_IDENTIFIER_CHARS)}${value.takeLast(VISIBLE_IDENTIFIER_CHARS)}"
  ```
  `MRN8859214` becomes `••••••9214` — enough to correlate a support ticket, not enough to identify a patient. Its KDoc notes: *"Nothing in Phase 1 logs patient data at all; this exists so that when diagnostics are added there is already a correct way to refer to a record without leaking PHI."* Keep that true.
- [ ] **Exception messages are never surfaced raw.** Map status codes to clinician-readable strings (§5 step 3). A `HttpException` message can contain a URL with an id in it.

### Build

- [ ] **Enable R8 for release.** `app/build.gradle.kts` currently has minification **off**:
  ```kotlin
  buildTypes {
      release {
          optimization {
              enable = false      // <-- must be true before shipping
          }
      }
  }
  ```
  Shipping an unminified release APK hands out readable class names, method names and your entire package structure. Turn it on, add a `proguard-rules.pro`, and keep rules for: kotlinx-serialization serializers, Retrofit interface signatures (`-keep,allowobfuscation,allowshrinking interface retrofit2.Call` and the generic-signature attributes), and anything reflected over. **Then actually test the minified build** — serialization and Retrofit failures from over-aggressive shrinking only appear at runtime.
- [ ] **A real signing config.** There is no `signingConfigs` block today; `build-output/` contains `LmiHL7-release-unsigned.apk`. Release builds need a keystore whose credentials come from the environment — never from a committed `keystore.properties`.
- [ ] **`buildConfig = true`** is added to `buildFeatures` (currently only `compose = true`), or `BuildConfig.HELIX_API_BASE_URL` and `BuildConfig.DEBUG` won't compile.
- [ ] **Developer switches are gone or debug-only.** `simulateOffline` / `simulateError` must not be flippable in a production build against real PHI. Gate the Settings → Developer section on `BuildConfig.DEBUG`, or remove the section and the two DataStore keys.
- [ ] **`android:allowBackup`** reviewed — a device backup containing a cached clinical database is a data-residency problem.

### Data

- [ ] **Room encrypted** if it holds clinical records (§7).
- [ ] **Logout wipes everything** — tokens via `TokenProvider.clear()`, plus the entire clinical cache. A logged-out phone must not still hold a worklist.
- [ ] **Demo data cannot ship.** Once the swap is complete, `SeedClinicalData.kt` and `MockClinicalStore.kt` are deleted (§5 step 5). If any seed survives for previews, it lives in `src/debug/` or `src/test/` where a release build cannot reach it.
- [ ] **No PHI in DataStore.** `HelixPreferencesDataSource` is preferences-only and must stay that way — see `06-data-layer.md` §9.

### Process

- [ ] Security review of the auth implementation before it reaches a device holding real PHI.
- [ ] Penetration test / threat model sign-off — see `10-security.md`.
- [ ] Backend enforces per-user authorisation on every endpoint. The app is not a security boundary; assume every request can be forged and replayed.
- [ ] Backend enforces signature idempotency (§3).
