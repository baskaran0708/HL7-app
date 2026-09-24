# 06 — The Data Layer

> Everything the app knows, where it lives in Phase 1, and how to change the demo data without touching a single screen.

**Siblings:** `02-architecture.md` (the big picture) · `09-backend-integration.md` (swapping the mock store for AWS) · `07-ui-layer.md` (who consumes this)

---

## Contents

1. [The shape of it](#1-the-shape-of-it)
2. [Domain models](#2-domain-models)
3. [Enums](#3-enums)
4. [`AppResult` — success, failure, offline](#4-appresult--success-failure-offline)
5. [Repository interfaces](#5-repository-interfaces)
6. [`MockClinicalStore` — the one shared store](#6-mockclinicalstore--the-one-shared-store)
7. [`MockCallSimulator` — fake latency, fake failures](#7-mockcallsimulator--fake-latency-fake-failures)
8. [The demo data — read this before you touch anything](#8-the-demo-data--read-this-before-you-touch-anything)
9. [`HelixPreferencesDataSource` — DataStore](#9-helixpreferencesdatasource--datastore)
10. [`NetworkMonitor`](#10-networkmonitor)
11. [Dispatchers](#11-dispatchers)
12. [Known rough edges](#12-known-rough-edges)

---

## 1. The shape of it

Phase 1 is frontend only. There is no backend, no database, no network call. The whole data layer is:

```
domain/model/          <- plain Kotlin value types. No Android, no serialization.
domain/repository/     <- interfaces. THE contract. Nothing above this layer knows what's below.
        │
        ▼
data/mock/             <- MockClinicalStore (StateFlow) + SeedClinicalData (the demo dataset)
data/repository/       <- Mock*Repository, implementing the domain interfaces
data/local/            <- HelixPreferencesDataSource (DataStore, preferences only)
data/di/               <- RepositoryModule — the one file that decides mock vs. real
```

The rule that makes all of this work: **ViewModels depend on `domain/repository` interfaces, never on anything in `data/`.** Swapping the mock store for a real API is a change to `RepositoryModule.kt` and nothing else. See `09-backend-integration.md`.

Files worth opening as you read:

| Path | What it is |
|---|---|
| `app/src/main/java/com/livemedica/helix/domain/model/ClinicalModels.kt` | Every domain model |
| `app/src/main/java/com/livemedica/helix/domain/model/ClinicalEnums.kt` | Every clinical enum |
| `app/src/main/java/com/livemedica/helix/domain/model/HelixSettings.kt` | User preferences model |
| `app/src/main/java/com/livemedica/helix/domain/repository/Repositories.kt` | All nine clinical repository interfaces |
| `app/src/main/java/com/livemedica/helix/domain/repository/SettingsRepository.kt` | Settings contract |
| `app/src/main/java/com/livemedica/helix/data/mock/MockClinicalStore.kt` | The shared in-memory store |
| `app/src/main/java/com/livemedica/helix/data/mock/SeedClinicalData.kt` | **The only place demo data lives** |
| `app/src/main/java/com/livemedica/helix/data/mock/MockCallSimulator.kt` | Artificial latency + dev switches |
| `app/src/main/java/com/livemedica/helix/data/repository/MockDirectoryRepositories.kt` | Doctor, Appointment, Patient |
| `app/src/main/java/com/livemedica/helix/data/repository/MockWorkflowRepositories.kt` | Order, Result, Today |
| `app/src/main/java/com/livemedica/helix/data/repository/MockSupportRepositories.kt` | Notification, Integration, Search |
| `app/src/main/java/com/livemedica/helix/data/repository/SettingsRepositoryImpl.kt` | Settings, backed by DataStore |
| `app/src/main/java/com/livemedica/helix/data/local/HelixPreferencesDataSource.kt` | DataStore wrapper |
| `app/src/main/java/com/livemedica/helix/data/di/RepositoryModule.kt` | Hilt bindings — the Phase 1 / Phase 2 seam |
| `app/src/main/java/com/livemedica/helix/core/common/AppResult.kt` | The result type |

---

## 2. Domain models

All of these live in `domain/model/ClinicalModels.kt`. Every one is a flat `@Immutable data class`.

| Model | Clinically, this is… |
|---|---|
| `Doctor` | The signed-in radiologist: display name, specialty, home facility, `licenceIdentifier` (placeholder for the state licence / NPI the backend will supply), on-call flag, initials for the avatar. |
| `Facility` | An imaging site — main hospital, an outpatient centre. Used to filter the Schedule. |
| `Patient` | Demographics (`fullName`, `mrn`, `age`, `sex`, `dateOfBirth`) plus `allergies` and a `history` list. |
| `HistoryEntry` | One line of the patient's problem list *or* one prior imaging study. Both are carried as the same flat type — a date, a `summary`, a `detail`. |
| `Appointment` | A booked exam slot: start time, duration, modality, procedure, priority, `AppointmentStatus`, facility, room. Computes `end` from `start + durationMinutes`. |
| `Order` | An imaging order as HL7 ORM delivers it: accession number, CPT code, procedure, free-text `clinicalIndication` (the "reason for exam"), ordering physician and location, priority, `OrderStatus`, and an optional `reportId` once a read exists. |
| `Study` | The acquired imaging: series and image counts, `performedAt`, and `studyInstanceUid` — the opaque handle a PACS viewer will be deep-linked with once that integration lands. |
| `RadiologyReport` | The read itself. Carries the full dictation structure — `comparison`, `technique`, `findings`, `impression` — plus lifecycle timestamps (`createdAt` / `finalizedAt` / `signedAt`) and `isCritical`, which means "this impression requires acknowledged communication to the ordering clinician". Exposes `isSigned`. |
| `HelixNotification` | One item in the notifications feed: `category`, title, body, `createdAt`, `isRead`, and a `target`. |
| `NotificationTarget` | Sealed type describing *where tapping navigates*: `Report`, `OrderDetail`, `PatientDetail`, `AppointmentDetail`, `Integration`. Sealed on purpose — a new notification kind can't be added without declaring where it goes, so nothing is silently untappable. |
| `Hl7Message` | One HL7 v2 message on the Integration screen: type, `receivedAt`, ack status, a human `reference` line, and the Mirth channel name. |
| `InterfaceChannel` | One Mirth Connect channel: name, `InterfaceState`, message count today, last message time. |
| `IntegrationSnapshot` | Aggregate for the Integration screen — total messages today, ack rate, counts by message type, channels, recent messages. |
| `TodayDashboard` | Aggregate powering the Today screen in one repository round-trip: doctor, date, the one `criticalReport` that should interrupt you, `examsToday` / `pendingReads` / `statCount` / `unsignedReports` counters, `upNext` appointments, `unreadNotifications`. |
| `SearchResults` | Universal search payload — patients, studies, orders, reports, appointments — with `isEmpty` and `totalCount` helpers. |
| `HelixSettings` | (in `HelixSettings.kt`) Theme preference, three alert toggles, `biometricUnlockEnabled`, and the two developer switches. |

### Why flat `@Immutable` data classes?

The KDoc at the top of `ClinicalModels.kt` says it plainly:

```kotlin
/**
 * All of these are flat, immutable value types holding primitives and stable ids rather than object
 * graphs. That keeps them Compose-stable (no needless recomposition), trivially mappable from the
 * future API DTOs, and directly translatable to Room entities when offline support lands.
 */
```

Three concrete payoffs:

1. **Compose stability.** `@Immutable` tells the Compose compiler it can skip a composable whose parameters are `equals`-equal. A `data class` of primitives + `String` + enums is trivially comparable, so lists don't thrash on every emission.
2. **Trivial DTO mapping.** `Order` holds `patientName` and `mrn` as strings, not a nested `Patient`. A JSON row from the API maps 1:1 with no graph-stitching. See `data/model/Dtos.kt`.
3. **Room-ready.** A flat class of primitives + ids *is* a Room entity, give or take `@Entity` and a couple of `TypeConverter`s for `LocalDate` / `LocalDateTime` / `List<String>`. No `@Relation` graph to untangle.

The denormalisation is intentional: `Order`, `Appointment` and `RadiologyReport` each carry `patientName`, `patientAge`, `patientSex` and `mrn` inline. A worklist row needs those to render, and one flat row is one flat row — no join, no second fetch, no "loading patient…" on a list item.

---

## 3. Enums

All in `domain/model/ClinicalEnums.kt`. Every one carries a `label` (or `code`) so the UI never has to `.name.lowercase().capitalize()` anything.

| Enum | Values | Notes |
|---|---|---|
| `Modality` | `CT`, `MR`, `XR`, `US`, `MG`, `NM`, `PT`, `FL` | Real DICOM two-letter codes. `code` + `label` ("CT" / "Computed Tomography"). |
| `Priority` | `STAT`, `URGENT`, `ROUTINE` | Rendered with an icon **and** the label — never colour alone, so it survives colour-blindness and greyscale. Ordinal order is also the worklist sort order. |
| `OrderStatus` | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` | |
| `ReportStatus` | `DRAFT`, `PRELIMINARY`, `PENDING_SIGNATURE`, `SIGNED`, `ADDENDUM` | `PRELIMINARY` and `PENDING_SIGNATURE` are deliberately distinct: preliminary = a read exists but isn't submitted; pending-signature = finished text awaiting the attending's signature. Today's "unsigned reports" counter is about the latter. |
| `AppointmentStatus` | `SCHEDULED`, `CHECKED_IN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW` | |
| `Sex` | `FEMALE("F")`, `MALE("M")`, `OTHER("X")` | `code` matches what HL7 PID-8 delivers. |
| `NotificationCategory` | `CRITICAL`, `RESULTS`, `APPOINTMENTS`, `ORDERS`, `SYSTEM` | |
| `Hl7MessageType` | `ADT`, `SIU`, `ORM`, `ORU` | The four v2 message types surfaced on the Integration screen. |
| `Hl7AckStatus` | `ACK`, `DELIVERED`, `PENDING`, `NAK` | |
| `InterfaceState` | `ONLINE`, `DEGRADED`, `OFFLINE` | Per Mirth channel. |
| `ThemePreference` | `SYSTEM`, `LIGHT`, `DARK` | In `HelixSettings.kt`. |

---

## 4. `AppResult` — success, failure, offline

`core/common/AppResult.kt`. The entire file:

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
    data class Offline<T>(val cached: T? = null) : AppResult<T>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
    is AppResult.Offline -> AppResult.Offline(cached?.let(transform))
}

fun <T> AppResult<T>.dataOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Offline -> cached
    is AppResult.Failure -> null
}
```

### Why `Offline` is not just another `Failure`

Because on a clinical screen they mean completely different things to the person holding the phone:

| Case | What it means | What the UI does |
|---|---|---|
| `Success(data)` | Call worked. | Render `data`. |
| `Failure(message, cause)` | The call was attempted and went wrong — a 500, a parse failure, a bad id. | Show the error state with `message`. Something is genuinely broken. |
| `Offline(cached)` | We couldn't reach the network at all. `cached` is the last-synced data, or `null` if there is none. | Render `cached` behind an "offline / last synced" banner. **Not** a hard error. |

A radiologist in a lift on the way to the reading room should still see the worklist they loaded ninety seconds ago, with an honest "you're offline" marker — not a red screen. That's the whole argument. `dataOrNull()` is the helper that collapses `Success` and `Offline` for the "just give me something to draw" case, while keeping `Failure` as `null`.

Note the variance: `Failure` is `AppResult<Nothing>`, which is why `map` can return `this` unchanged in the failure branch.

---

## 5. Repository interfaces

`domain/repository/Repositories.kt`. Nine clinical interfaces, plus `SettingsRepository` in its own file.

### The convention

Straight from the file header:

```kotlin
/**
 * Reads that the UI observes return [Flow] so a mutation anywhere (signing a report, reading a
 * notification) propagates to every screen showing that data. One-shot reads and all writes are
 * `suspend` and return [AppResult] so offline and error states are explicit rather than exceptional.
 */
```

So:

- **`fun observeX(): Flow<T>`** — a live subscription. No `AppResult` wrapper, because a `Flow` that's already emitting doesn't have a "failed" moment; it just stops emitting or emits the next state. Screens collect these for reactive content.
- **`suspend fun getX(): AppResult<T>`** — a one-shot fetch. Returns `AppResult` so the caller must decide what happens on `Failure` and `Offline`.
- **`suspend fun doX(): AppResult<T>`** — a write. Same wrapper. Returns the mutated entity where there is one, so the caller doesn't have to re-fetch.

### The full contract

| Interface | Method | Kind |
|---|---|---|
| `DoctorRepository` | `observeCurrentDoctor(): Flow<Doctor>` | observe |
| | `getCurrentDoctor(): AppResult<Doctor>` | read |
| | `setOnCall(onCall: Boolean): AppResult<Doctor>` | write |
| `AppointmentRepository` | `observeTodayAppointments(): Flow<List<Appointment>>` | observe |
| | `getTodayAppointments(): AppResult<List<Appointment>>` | read |
| | `getAppointments(date: LocalDate, facilityId: String? = null): AppResult<List<Appointment>>` | read |
| | `getAppointment(id: String): AppResult<Appointment>` | read |
| | `getFacilities(): AppResult<List<Facility>>` | read |
| `PatientRepository` | `getPatient(id: String): AppResult<Patient>` | read |
| | `observePatient(id: String): Flow<Patient?>` | observe |
| | `getAppointmentsFor(patientId: String): AppResult<List<Appointment>>` | read |
| | `getOrdersFor(patientId: String): AppResult<List<Order>>` | read |
| | `getReportsFor(patientId: String): AppResult<List<RadiologyReport>>` | read |
| | `getStudiesFor(patientId: String): AppResult<List<Study>>` | read |
| `OrderRepository` | `observeOrders(): Flow<List<Order>>` | observe |
| | `getOrders(priority: Priority? = null, status: OrderStatus? = null, query: String? = null): AppResult<List<Order>>` | read |
| | `getOrder(id: String): AppResult<Order>` | read |
| | `getStudy(studyId: String): AppResult<Study>` | read |
| | `getStudyForOrder(orderId: String): AppResult<Study>` | read |
| `ResultRepository` | `observeReports(): Flow<List<RadiologyReport>>` | observe |
| | `getReports(status: ReportStatus? = null, priority: Priority? = null): AppResult<List<RadiologyReport>>` | read |
| | `getReport(id: String): AppResult<RadiologyReport>` | read |
| | `signReport(id: String): AppResult<RadiologyReport>` | **write — must be idempotent** |
| `NotificationRepository` | `observeNotifications(): Flow<List<HelixNotification>>` | observe |
| | `observeUnreadCount(): Flow<Int>` | observe |
| | `markRead(id: String): AppResult<Unit>` | write |
| | `markAllRead(): AppResult<Unit>` | write |
| | `clear(id: String): AppResult<Unit>` | write |
| `SearchRepository` | `search(query: String): AppResult<SearchResults>` | read |
| | `observeRecentQueries(): Flow<List<String>>` | observe |
| | `recordQuery(query: String)` | write — **returns `Unit`, not `AppResult`** |
| | `clearRecentQueries()` | write — **returns `Unit`, not `AppResult`** |
| `IntegrationRepository` | `observeSnapshot(): Flow<IntegrationSnapshot>` | observe |
| | `getSnapshot(): AppResult<IntegrationSnapshot>` | read |
| `TodayRepository` | `observeDashboard(): Flow<TodayDashboard>` | observe |
| | `getDashboard(): AppResult<TodayDashboard>` | read |
| `SettingsRepository` | `observeSettings(): Flow<HelixSettings>` | observe |
| | `setThemePreference(preference: ThemePreference)` | write — `Unit` |
| | `setCriticalAlertsEnabled(enabled: Boolean)` | write — `Unit` |
| | `setResultAlertsEnabled(enabled: Boolean)` | write — `Unit` |
| | `setScheduleAlertsEnabled(enabled: Boolean)` | write — `Unit` |
| | `setBiometricUnlockEnabled(enabled: Boolean)` | write — `Unit` |
| | `setSimulateOffline(enabled: Boolean)` | write — `Unit` (dev switch) |
| | `setSimulateError(enabled: Boolean)` | write — `Unit` (dev switch) |

The two `Unit`-returning `SearchRepository` methods are the one deviation from the convention — recording a recent query is local bookkeeping, not a server round-trip. `SettingsRepository` writes are `Unit` for the same reason: DataStore, on-device, nothing to fail over the wire. Worth knowing so the deviation doesn't read as an oversight.

### Where the mocks live

| Interface | Phase 1 implementation | File |
|---|---|---|
| `DoctorRepository` | `MockDoctorRepository` | `data/repository/MockDirectoryRepositories.kt` |
| `AppointmentRepository` | `MockAppointmentRepository` | `data/repository/MockDirectoryRepositories.kt` |
| `PatientRepository` | `MockPatientRepository` | `data/repository/MockDirectoryRepositories.kt` |
| `OrderRepository` | `MockOrderRepository` | `data/repository/MockWorkflowRepositories.kt` |
| `ResultRepository` | `MockResultRepository` | `data/repository/MockWorkflowRepositories.kt` |
| `TodayRepository` | `MockTodayRepository` | `data/repository/MockWorkflowRepositories.kt` |
| `NotificationRepository` | `MockNotificationRepository` | `data/repository/MockSupportRepositories.kt` |
| `IntegrationRepository` | `MockIntegrationRepository` | `data/repository/MockSupportRepositories.kt` |
| `SearchRepository` | `MockSearchRepository` | `data/repository/MockSupportRepositories.kt` |
| `SettingsRepository` | `SettingsRepositoryImpl` | `data/repository/SettingsRepositoryImpl.kt` |
| `NetworkMonitor` | `SettingsAwareNetworkMonitor` | `core/network/SettingsAwareNetworkMonitor.kt` |

Every one is `@Singleton` and takes exactly two constructor dependencies — `MockClinicalStore` and `MockCallSimulator` — except `SettingsRepositoryImpl`, which takes `HelixPreferencesDataSource`.

A representative one, `MockOrderRepository`:

```kotlin
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
    ...
    private companion object {
        /** STAT first, then urgent, then newest — the order a radiologist actually works a list in. */
        val worklistOrdering: Comparator<Order> =
            compareBy<Order> { it.priority.ordinal }.thenByDescending { it.orderedAt }
    }
}
```

Note the pattern: **the same lambda is passed as both `cachedOnOffline` and the work block.** In Phase 1 the "cache" and the "server" are the same in-memory list, so offline returns identical data — which is exactly what you want for a demo, because it proves the offline path renders content rather than an empty state.

Sorting is a repository concern here, not a UI concern. `MockResultRepository` uses the same comparator shape (`priority.ordinal`, then `createdAt` descending).

---

## 6. `MockClinicalStore` — the one shared store

`data/mock/MockClinicalStore.kt`. A `@Singleton` holding one `MutableStateFlow` per entity collection, exposed read-only:

```kotlin
@Singleton
class MockClinicalStore @Inject constructor() {

    private val seed: ClinicalSeed = SeedClinicalData.build(LocalDate.now())

    private val _doctor = MutableStateFlow(seed.doctor)
    private val _patients = MutableStateFlow(seed.patients)
    private val _appointments = MutableStateFlow(seed.appointments)
    private val _orders = MutableStateFlow(seed.orders)
    private val _studies = MutableStateFlow(seed.studies)
    private val _reports = MutableStateFlow(seed.reports)
    private val _notifications = MutableStateFlow(seed.notifications)
    private val _recentQueries = MutableStateFlow<List<String>>(emptyList())

    val doctor: StateFlow<Doctor> = _doctor.asStateFlow()
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()
    // ... etc
    val facilities: List<Facility> = seed.facilities
    val hl7Messages: List<Hl7Message> = seed.hl7Messages
    val channels: List<InterfaceChannel> = seed.channels
}
```

`facilities`, `hl7Messages` and `channels` are plain `List`s, not `StateFlow`s — nothing mutates them in Phase 1.

### Why one shared store and not per-repository copies

The KDoc again:

```kotlin
/**
 * Deliberately one shared store rather than per-repository copies: signing a report has to make the
 * Results list, the Today workload counters and the notification badge all change at once, which is
 * what makes the mocked app behave like a real one.
 */
```

Concretely — tap **Sign** on a report and, from that single `_reports.update { }`:

- `MockResultRepository.observeReports()` re-emits → the Results list row flips to SIGNED.
- `MockTodayRepository.observeDashboard()` `combine`s over `doctor + appointments + orders + reports + notifications`, so it recomputes → `unsignedReports` drops by one and `criticalReport` clears if that was the critical read.
- `MockNotificationRepository.observeUnreadCount()` re-emits → the tab badge updates.
- `MockPatientRepository.observePatient(id)` is unaffected (different flow), but `getReportsFor(patientId)` reads `.value` and sees the new state immediately.

If each repository held its own copy of the seed, signing a report would update exactly one screen and the demo would fall apart the moment anyone navigated back.

### `signReport` — the idempotency logic

```kotlin
/**
 * Idempotent by design: signing an already-signed report returns it unchanged rather than
 * stamping a second signature, mirroring what the backend will enforce.
 */
fun signReport(id: String, signedAt: LocalDateTime = LocalDateTime.now()): RadiologyReport? {
    var signed: RadiologyReport? = null
    _reports.update { reports ->
        reports.map { report ->
            if (report.id != id) {
                report
            } else if (report.isSigned) {
                report.also { signed = it }
            } else {
                report.copy(
                    status = ReportStatus.SIGNED,
                    signedAt = signedAt,
                    finalizedAt = report.finalizedAt ?: signedAt,
                ).also { signed = it }
            }
        }
    }
    // A signed report no longer needs the clinician's attention, so its prompts clear too.
    signed?.let { report ->
        _notifications.update { list ->
            list.map { if (it.body.contains(report.accessionNumber)) it.copy(isRead = true) else it }
        }
    }
    return signed
}
```

Three things to notice:

1. **Three branches, not two.** Not-this-report → untouched. Already signed → returned as-is, `signedAt` preserved. Not yet signed → stamped. A double-tap on Sign cannot produce a second signature event.
2. **`finalizedAt = report.finalizedAt ?: signedAt`.** A `PRELIMINARY` report with no `finalizedAt` gets one on signature; a `PENDING_SIGNATURE` report that was already finalised keeps its original timestamp. Signing doesn't rewrite history.
3. **Returns `RadiologyReport?`.** `null` means "no report with that id". `MockResultRepository` turns that into a `Failure`:
   ```kotlin
   override suspend fun signReport(id: String): AppResult<RadiologyReport> = simulator.call {
       requireNotNull(store.signReport(id)) { "Report $id not found" }
   }
   ```
   The `IllegalArgumentException` is caught by the simulator's `runCatching` and becomes `AppResult.Failure`.

The real backend will enforce the same rule server-side (see `09-backend-integration.md` — signer identity comes from the bearer token, not the client), so an `ApiResultRepository` gets idempotency for free.

### The rest of the mutators

| Method | Effect |
|---|---|
| `setOnCall(onCall: Boolean): Doctor` | Flips `Doctor.isOnCall`, returns the updated doctor. |
| `markNotificationRead(id: String)` | Sets `isRead = true` on one notification. |
| `markAllNotificationsRead()` | Sets `isRead = true` on all. |
| `clearNotification(id: String)` | Removes it from the list. |
| `recordQuery(query: String)` | Trims, dedupes case-insensitively, prepends, caps at `RECENT_QUERY_LIMIT = 8`. |
| `clearRecentQueries()` | Empties the list. |

Everything is `_flow.update { }` on an immutable `List` — copy-on-write, no locks, safe from any thread.

---

## 7. `MockCallSimulator` — fake latency, fake failures

`data/mock/MockCallSimulator.kt`. Every mock repository `suspend` method goes through it.

```kotlin
@Singleton
class MockCallSimulator @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    suspend fun <T> call(cachedOnOffline: (() -> T)? = null, block: suspend () -> T): AppResult<T> {
        val settings = settingsRepository.observeSettings().first()
        delay(LATENCY_MS)

        return when {
            settings.simulateOffline -> AppResult.Offline(cachedOnOffline?.invoke())
            settings.simulateError -> AppResult.Failure(SIMULATED_FAILURE_MESSAGE)
            else -> runCatching { AppResult.Success(block()) }
                .getOrElse { AppResult.Failure(it.message ?: SIMULATED_FAILURE_MESSAGE, it) }
        }
    }

    private companion object {
        const val LATENCY_MS = 320L
        const val SIMULATED_FAILURE_MESSAGE =
            "We couldn't reach the server. Your data is safe — try again in a moment."
    }
}
```

**Two jobs, both deliberate:**

1. **320 ms of artificial latency.** Loading skeletons are genuinely visible, so you find out *now* whether a screen flickers on refresh or whether a shimmer lands in the wrong place — instead of discovering it the day the backend is plugged in.
2. **Honouring the developer switches.** `simulateOffline` and `simulateError` come from `HelixSettings` via DataStore. They're toggled at **Settings → Developer** in the app. Because `observeSettings().first()` is read on every call, flipping a switch takes effect on the very next request — no restart.

Note the `simulateOffline` branch: it returns `AppResult.Offline(cachedOnOffline?.invoke())`. A repository that passes `cachedOnOffline` shows stale-looking content behind the offline banner; one that doesn't (single-entity getters like `getOrder(id)`) returns `Offline(null)` and the screen shows the empty-offline state. Both paths are reviewable without touching aeroplane mode.

The failure message is phrased as the backend failure it stands in for and says what to do next. The screen supplies the "Something went wrong" heading, so this string must not repeat it.

---

## 8. The demo data — read this before you touch anything

### `SeedClinicalData.kt` is the only place demo data lives

**Say it once more:** `app/src/main/java/com/livemedica/helix/data/mock/SeedClinicalData.kt` is the *single* file containing demo content. No hardcoded patient name, accession number, procedure or report body exists anywhere else in the app — not in a composable, not in a preview, not in a string resource. If you want to change what the demo shows, you change that one file and nothing else.

This dataset is **intentional and it stays** for Phase 1. It is what management is being shown before the backend exists. Do not "clean it up" or replace it with lorem ipsum.

### What it is

```kotlin
/**
 * Content is written to be clinically plausible — real CPT codes, real DICOM modality codes,
 * indications phrased the way an ED physician actually writes them — because the whole point of
 * this phase is to judge the workflow, and placeholder text makes a radiology worklist impossible
 * to evaluate. It is demo data about fictional people; it is not medical advice and no record here
 * describes a real patient.
 *
 * The names, MRNs, accession numbers, procedures, indications and report prose mirror the design
 * prototype's dataset so the Android build and the design mocks can be compared side by side.
 *
 * Everything is generated relative to [today] so the app never looks stale.
 */
```

Three claims in there, each load-bearing:

- **Clinically plausible, not placeholder.** Real CPT codes (`70553`, `74177`, `77067`…), real DICOM modality codes, indications phrased the way an ED physician writes them ("RLQ pain 18h, fever 38.4, WBC 14.2, rebound tenderness. R/O appendicitis."). You cannot evaluate a radiology worklist filled with "Lorem Ipsum · Test Procedure".
- **Mirrors the approved design prototype.** Names, MRNs, accession numbers, procedures, indications and report prose match the prototype's dataset, so the Android build and the design mocks can be held side by side and compared.
- **Fictional.** No record describes a real patient. Not medical advice.

### Everything is relative to `today`

The whole thing is a function of one parameter:

```kotlin
fun build(today: LocalDate = LocalDate.now()): ClinicalSeed {
    fun at(hour: Int, minute: Int, second: Int = 0): LocalDateTime =
        LocalDateTime.of(today, LocalTime.of(hour, minute, second))

    /** Keeps the prototype's birthday (month/day) while pinning the age to [age] as of today. */
    fun dob(month: Int, day: Int, age: Int): LocalDate {
        val birthdayThisYear = LocalDate.of(today.year, month, day)
        val year = if (birthdayThisYear.isAfter(today)) today.year - age - 1 else today.year - age
        return LocalDate.of(year, month, day)
    }
    ...
}
```

`MockClinicalStore` calls `SeedClinicalData.build(LocalDate.now())` at construction. So:

- `at(9, 12)` is 09:12 **today**, whenever "today" is.
- `at(15, 0).minusDays(1)` is yesterday afternoon.
- `at(8, 30).plusDays(1)` is tomorrow morning.
- `dob(7, 19, 76)` gives Linda Park a 19 July birthday and pins her to exactly 76 years old today.
- Priors use `today.minusMonths(30)`, problem-list entries use `today.minusYears(7)`, and so on.

Open the app on any date and the schedule is populated, the reports were dictated this morning, and the HL7 feed has messages from the last ten minutes. Nothing ever reads as a stale 2024 demo.

### What's in the box

| Collection | Count | Contents |
|---|---|---|
| `doctor` | 1 | `DR1284` — Dr. Sarah Chen, Diagnostic Radiology, Mayo Regional · Main, `NPI 1245789632`, on-call, initials `SC`. |
| `facilities` | 3 | `MAIN` Mayo Regional · Main, `NSHR` Northshore Imaging, `EAST` Eastview Outpatient. |
| `patients` | 16 | See below. |
| `appointments` | 21 | 3 yesterday (the exams behind today's signed reports), 16 today, 2 tomorrow (so the Schedule date selector has somewhere to move to). |
| `orders` | 12 | 8 "live" orders plus 4 backing the earlier reads, so every report has a real order behind it. |
| `studies` | 12 | **Derived**, not hand-written — one per order via `orders.mapIndexed`. |
| `reports` | 7 | 3 `PENDING_SIGNATURE`, 1 `PRELIMINARY`, 3 `SIGNED`. |
| `notifications` | 9 | `N-001`…`N-009`; the first three are unread. |
| `hl7Messages` | 11 | `MSG-93345`…`MSG-93421`, mixed ADT/SIU/ORM/ORU, one `NAK`, one `PENDING`. |
| `channels` | 7 | 5 `ONLINE`, 1 `DEGRADED` (`RAD-OUT-ORU`), 1 `OFFLINE` (`CERNER-IN-ADT`). |

**Patients** (id · name · MRN · age/sex):

| id | Name | MRN | Age/Sex | Notable |
|---|---|---|---|---|
| `pat-park` | Linda Park | MRN8859214 | 76F | Penicillin + iodinated contrast allergies; AFib, HTN, prior TIA; 2 priors. The critical-finding patient. |
| `pat-kowalski` | Theresa Kowalski | MRN8849031 | 54F | Contrast allergy — premedicate; CKD 3, T2DM. |
| `pat-reyes` | Marcus T. Reyes | MRN8841902 | 44M | Prior knee arthroscopy. |
| `pat-hartwell` | Eleanor J. Hartwell | MRN8843261 | 68F | Annual screening mammo, prior BI-RADS 1. |
| `pat-garrison` | Allen Garrison | MRN8857702 | 71M | Anticoagulated (apixaban), recurrent falls. |
| `pat-olsen` | Daniel R. Olsen | MRN8846712 | 60M | COPD, 40 pack-year. |
| `pat-brooks` | Jamal Brooks | MRN8852399 | 36M | Chronic LBP. |
| `pat-delgado` | Carmen Delgado | MRN8854882 | 57F | Gadolinium reaction; hepatic lesion surveillance. |
| `pat-demir` | Yusuf Demir | MRN8862004 | 38M | No history entries (tests the empty-history state). |
| `pat-sato` | Naomi Sato | MRN8848117 | 70F | Cholecystectomy. |
| `pat-iyer` | Priya Iyer | MRN8851188 | 39F | No history entries. |
| `pat-mendoza` | George Mendoza | MRN8842004 | 73M | Stage IB NSCLC s/p lobectomy; oncology surveillance. |
| `pat-zhao` | Mei Lin Zhao | MRN8855401 | 63F | Migraine with aura. |
| `pat-okonkwo` | Adaeze Okonkwo | MRN8858220 | 46F | No history entries. |
| `pat-whitman` | Henry Whitman | MRN8843991 | 67M | The `NO_SHOW` appointment. |
| `pat-bradshaw` | Owen Bradshaw | MRN8849990 | 30M | Yesterday's appendicitis read. |

**Orders** (id · accession · patient · modality · CPT · priority · status):

| Order | Accession | Patient | Mod | CPT | Priority | Status | Report |
|---|---|---|---|---|---|---|---|
| `ORD-44128` | ACC-2026-19014 | Park | MR | 70553 | STAT | PENDING | `RPT-91204` |
| `ORD-44131` | ACC-2026-19017 | Kowalski | CT | 74177 | URGENT | IN_PROGRESS | — |
| `ORD-44119` | ACC-2026-19009 | Garrison | CT | 70450 | STAT | COMPLETED | `RPT-91198` |
| `ORD-44141` | ACC-2026-19023 | Demir | US | 76700 | URGENT | PENDING | — |
| `ORD-44102` | ACC-2026-19004 | Reyes | MR | 73721 | ROUTINE | COMPLETED | `RPT-91179` |
| `ORD-44095` | ACC-2026-19044 | Delgado | MR | 74183 | ROUTINE | PENDING | — |
| `ORD-44152` | ACC-2026-19022 | Zhao | CT | 70450 | ROUTINE | PENDING | — |
| `ORD-44158` | ACC-2026-19058 | Mendoza | CT | 71250 | ROUTINE | PENDING | — |
| `ORD-44087` | ACC-2026-19001 | Hartwell | MG | 77067 | ROUTINE | COMPLETED | `RPT-91187` |
| `ORD-44012` | ACC-2026-18960 | Mendoza | CT | 71250 | ROUTINE | COMPLETED | `RPT-91164` |
| `ORD-44008` | ACC-2026-18955 | Bradshaw | CT | 74177 | URGENT | COMPLETED | `RPT-91155` |
| `ORD-44001` | ACC-2026-18948 | Brooks | MR | 72148 | ROUTINE | COMPLETED | `RPT-91142` |

**Reports:**

| Report | Order | Patient | Procedure | Priority | Status | Critical |
|---|---|---|---|---|---|---|
| `RPT-91204` | ORD-44128 | Linda Park | MRI Brain w/ & w/o Contrast | STAT | `PENDING_SIGNATURE` | **yes** — acute L MCA infarct |
| `RPT-91198` | ORD-44119 | Allen Garrison | CT Head/Brain w/o Contrast | STAT | `PENDING_SIGNATURE` | no |
| `RPT-91187` | ORD-44087 | Eleanor J. Hartwell | Screening Mammography, Bilateral | ROUTINE | `PENDING_SIGNATURE` | no |
| `RPT-91179` | ORD-44102 | Marcus T. Reyes | MRI Knee w/o Contrast | ROUTINE | `PRELIMINARY` | no |
| `RPT-91164` | ORD-44012 | George Mendoza | CT Chest w/o Contrast | ROUTINE | `SIGNED` | no |
| `RPT-91155` | ORD-44008 | Owen Bradshaw | CT Abd/Pelvis w/ Contrast | URGENT | `SIGNED` | no |
| `RPT-91142` | ORD-44001 | Jamal Brooks | MRI Lumbar Spine w/o Contrast | ROUTINE | `SIGNED` | no |

Every report carries full `comparison` / `technique` / `findings` / `impression` prose. `RPT-91204` is the one the Today screen surfaces as `criticalReport` and the one `N-001` points at — it's the demo's money shot.

**The counters this produces** (what you'll see on Today, so you can sanity-check a change):

| Counter | Value | Why |
|---|---|---|
| `examsToday` | 16 | appointments whose `start.toLocalDate() == today` |
| `pendingReads` | 6 | orders not `COMPLETED` and not `CANCELLED` |
| `statCount` | 1 | `STAT` orders not `COMPLETED` (`ORD-44128`; `ORD-44119` is STAT but completed) |
| `unsignedReports` | 4 | `!isSigned && status != DRAFT` — the 3 pending-signature plus the preliminary |
| `unreadNotifications` | 3 | `N-001`, `N-002`, `N-003` |
| `criticalReport` | `RPT-91204` | first report with `isCritical && !isSigned` |
| Integration `messagesToday` | 1217 | sum of `channel.messagesToday` |

### How `studies` are derived

Don't hand-write studies. They're generated from the orders:

```kotlin
val studies = orders.mapIndexed { index, order ->
    Study(
        id = "std-${index + 1}",
        accessionNumber = order.accessionNumber,
        orderId = order.id,
        patientId = order.patientId,
        modality = order.modality,
        description = order.procedure,
        performedAt = if (order.status == OrderStatus.PENDING) null else order.orderedAt.plusMinutes(35),
        seriesCount = when (order.modality) {
            Modality.MR -> 8
            Modality.CT -> 5
            Modality.US -> 3
            else -> 1
        },
        imageCount = when (order.modality) {
            Modality.MR -> 412
            Modality.CT -> 286
            Modality.US -> 44
            Modality.MG -> 4
            else -> 2
        },
        studyInstanceUid = "1.2.840.113619.2.55.3.${604688119 + index}.${index + 1}",
    )
}
```

**Add an order and you automatically get a study.** Series/image counts follow the modality. `performedAt` is `null` for `PENDING` orders (nothing acquired yet) and `orderedAt + 35 min` otherwise.

### The private builders at the bottom of the file

Use these rather than constructing models by hand — they keep patient demographics consistent across entities.

```kotlin
/** A problem-list entry: the prototype carries these as plain strings. */
private fun problem(id: String, date: LocalDate, summary: String) =
    HistoryEntry(id, date, summary, "Active problem list entry.")

/** A prior imaging study, summarised the way it appears on the relevant-priors rail. */
private fun prior(id: String, date: LocalDate, modality: String, procedure: String, finding: String) =
    HistoryEntry(id, date, "$modality · $procedure", finding)

private fun appointment(id, patient: Patient, start, durationMinutes, modality, procedure,
                        priority, status, facilityId, room): Appointment

private fun order(id, accession, patient: Patient, modality, cpt, procedure, indication,
                  orderingPhysician, location, priority, status, orderedAt, facilityId, reportId): Order
```

`appointment()` and `order()` both take a **whole `Patient`** and copy `patientId`, `patientName`, `patientAge`, `patientSex` and `mrn` off it. That's why you never type an MRN twice. And `p("pat-xyz")` is the lookup:

```kotlin
val byId = patients.associateBy { it.id }
fun p(id: String): Patient = requireNotNull(byId[id]) { "Unknown seed patient $id" }
```

Typo a patient id and you get a loud `IllegalArgumentException` at app start, not a silently blank row.

---

### Recipe: add a demo patient with a full workflow

Say you want a new STAT chest CT for the demo. Open `SeedClinicalData.kt` and make four edits, in this order.

**Step 1 — add the patient**, inside the `val patients = listOf(...)` block:

```kotlin
Patient(
    id = "pat-nakamura",
    fullName = "Rei Nakamura",
    mrn = "MRN8863110",
    age = 52,
    sex = Sex.FEMALE,
    dateOfBirth = dob(6, 3, 52),          // 3 June birthday, exactly 52 today
    allergies = listOf("NKDA"),
    history = listOf(
        problem("h-naka-1", today.minusYears(4), "Pulmonary embolism, provoked"),
        prior("h-naka-2", today.minusMonths(8), "CT", "CT Angiogram Chest", "No residual thrombus."),
    ),
),
```

Use `dob(month, day, age)` — never a literal `LocalDate.of(1974, 6, 3)`, or she ages a year every January.

**Step 2 — add the order**, inside `val orders = listOf(...)`:

```kotlin
order("ORD-44163", "ACC-2026-19061", p("pat-nakamura"), Modality.CT, "71275", "CT Angiogram Chest w/ Contrast",
    "Pleuritic chest pain, tachycardia, D-dimer 3.4. R/O pulmonary embolism.",
    "ED · Dr. Hill", "Emergency", Priority.STAT, OrderStatus.COMPLETED, at(10, 5), FACILITY_MAIN, "RPT-91211"),
```

Pick ids that don't collide. The `at(10, 5)` puts it at 10:05 today. The trailing `"RPT-91211"` is the report you're about to write — pass `null` if there isn't one yet.

The study is now generated for you. Nothing else to do for step 2.

**Step 3 — add the report**, inside `val reports = listOf(...)`:

```kotlin
RadiologyReport(
    id = "RPT-91211",
    orderId = "ORD-44163",
    studyId = studyByOrder.getValue("ORD-44163").id,   // resolves the derived study
    accessionNumber = "ACC-2026-19061",
    patientId = "pat-nakamura",
    patientName = "Rei Nakamura",
    patientAge = 52,
    patientSex = Sex.FEMALE,
    mrn = "MRN8863110",
    modality = Modality.CT,
    procedure = "CT Angiogram Chest w/ Contrast",
    clinicalIndication = "Pleuritic chest pain, tachycardia, D-dimer 3.4. R/O pulmonary embolism.",
    comparison = "CT Angiogram Chest ${today.minusMonths(8)}.",
    technique = "Contrast-enhanced CT angiography of the pulmonary arteries, 1mm reconstructions.",
    findings = "Filling defect within the right lower lobe segmental pulmonary artery consistent " +
        "with acute pulmonary embolism. No right heart strain; RV:LV ratio 0.8. No pleural effusion.",
    impression = "1. Acute segmental pulmonary embolism, right lower lobe.\n" +
        "2. No CT evidence of right heart strain.",
    priority = Priority.STAT,
    status = ReportStatus.PENDING_SIGNATURE,
    radiologist = "Dr. S. Chen",
    createdAt = at(10, 40),
    finalizedAt = at(10, 52),
    signedAt = null,
    isCritical = true,
),
```

`studyByOrder.getValue("ORD-44163")` is defined just above the reports block (`val studyByOrder = studies.associateBy { it.orderId }`) and will throw loudly if you got the order id wrong.

**Step 4 — optional: an appointment and a notification.**

```kotlin
// in `val appointments = listOf(...)`, in the "Today" section
appointment("APT-83296", p("pat-nakamura"), at(10, 0), 30, Modality.CT,
    "CT Angiogram Chest w/ Contrast", Priority.STAT, AppointmentStatus.COMPLETED, FACILITY_MAIN, "CT-02"),

// in `val notifications = listOf(...)`
HelixNotification(
    "N-010", NotificationCategory.CRITICAL,
    "Critical finding ready to sign",
    "Rei Nakamura · CT Angiogram Chest · Acute segmental PE",
    at(10, 52), false, NotificationTarget.Report("RPT-91211"),
),
```

Rebuild. The patient appears in search, the order in the worklist (near the top — STAT sorts first), the report in Results, the notification in the feed, and `examsToday` / `statCount` / `unsignedReports` all move.

> ⚠️ **Ordering caveat.** `TodayDashboard.criticalReport` is `reports.firstOrNull { it.isCritical && !it.isSigned }` against `MockClinicalStore.reports` in **declaration order**, not sorted order. Add a second `isCritical` report *before* `RPT-91204` in the list and you've silently changed which one the Today screen hero-cards. Append new critical reports to the end unless that's what you want.

### Recipe: edit demo data

Just change the literal. Two rules:

1. **Change it in every place it appears.** A patient's name and MRN are denormalised into `Appointment`, `Order` and `RadiologyReport`. Rename Linda Park and you must update her `Patient`, her `RadiologyReport` (`patientName`, `mrn`), and the notification body that mentions her. The `appointment()` and `order()` helpers take the `Patient` object and copy from it, so those two are automatic — reports are not, because they're constructed longhand.
2. **Keep relative dates relative.** Replace `at(9, 12)` with `LocalDateTime.of(2026, 3, 14, 9, 12)` and the demo is stale the next morning.

### Recipe: remove demo data

Delete in reverse dependency order, or you'll break a `getValue` lookup:

1. Notifications referencing it (`NotificationTarget.Report("RPT-…")`, `OrderDetail("ORD-…")`, etc.).
2. The `RadiologyReport`.
3. Clear the `reportId` on the order (set it to `null`) **or** delete the order — if you delete the order, the derived study disappears with it.
4. The appointments.
5. The `Patient`.

If you delete a patient but leave an `p("pat-…")` call behind, the app throws `IllegalArgumentException: Unknown seed patient pat-…` on first construction of `MockClinicalStore`. That's by design — a hard failure at startup beats a half-rendered screen.

### Recipe: start from an empty dataset

Occasionally useful for reviewing empty states. Don't gut the file — temporarily return empty lists from `build()`:

```kotlin
return ClinicalSeed(
    doctor = doctor,
    facilities = facilities,
    patients = emptyList(),
    appointments = emptyList(),
    orders = emptyList(),
    studies = emptyList(),
    reports = emptyList(),
    notifications = emptyList(),
    hl7Messages = emptyList(),
    channels = emptyList(),
)
```

Revert before committing. This is a local-inspection trick, not a configuration.

---

## 9. `HelixPreferencesDataSource` — DataStore

`data/local/HelixPreferencesDataSource.kt`. Preferences DataStore named `helix_settings`:

```kotlin
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("helix_settings")
```

### What's stored

| Key | Type | Default | Purpose |
|---|---|---|---|
| `theme_preference` | String | `SYSTEM` | `ThemePreference` name |
| `critical_alerts` | Boolean | `true` | Alert preference |
| `result_alerts` | Boolean | `true` | Alert preference |
| `schedule_alerts` | Boolean | `true` | Alert preference |
| `biometric_unlock` | Boolean | `false` | Preference only — **no `BiometricPrompt` is wired up**, so enabling it does not gate access to anything yet |
| `dev_simulate_offline` | Boolean | `false` | Developer switch → `MockCallSimulator` + `SettingsAwareNetworkMonitor` |
| `dev_simulate_error` | Boolean | `false` | Developer switch → `MockCallSimulator` |

The theme read is defensive — an unknown stored string falls back rather than crashing:

```kotlin
themePreference = prefs[Keys.THEME]
    ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
    ?: ThemePreference.SYSTEM,
```

### What is deliberately **not** stored

```kotlin
/**
 * Persists user preferences only.
 *
 * Nothing clinical is written here: no patient data, no tokens, no identifiers. When Room lands it
 * will hold the cached clinical records; this file stays limited to display and alert preferences.
 */
```

**No PHI. No auth tokens. No patient identifiers.** Preferences DataStore is plain unencrypted storage in app-private space — fine for "user likes dark mode", categorically wrong for a patient record or a bearer token. Tokens go through `TokenProvider` (see `09-backend-integration.md`); cached clinical data will go in Room with its own encryption decision.

`SettingsRepositoryImpl` is a thin pass-through — one line per method, no logic. It exists so ViewModels depend on a `domain/` interface rather than on a DataStore class.

---

## 10. `NetworkMonitor`

`core/network/NetworkMonitor.kt` is one property:

```kotlin
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
```

`SettingsAwareNetworkMonitor` is the implementation — real `ConnectivityManager` state **AND**ed with the dev switch:

```kotlin
override val isOnline: Flow<Boolean> =
    combine(deviceConnectivity(), settingsRepository.observeSettings()) { connected, settings ->
        connected && !settings.simulateOffline
    }.distinctUntilChanged()
```

Device connectivity is a `callbackFlow` around `registerDefaultNetworkCallback`, `.conflate()`d so bursts of capability changes don't flood collectors. It handles `onAvailable`, `onLost` and `onCapabilitiesChanged` (checking `NET_CAPABILITY_VALIDATED`), emits an immediate seed value from `getNetworkCapabilities(activeNetwork)`, and unregisters on `awaitClose`. If `getSystemService<ConnectivityManager>()` returns `null` it optimistically emits `true` rather than pretending you're offline.

Why the switch matters: *"the offline UI has to be reviewable now, before there is a backend to disconnect from — flipping aeroplane mode would prove nothing about a mocked app."*

---

## 11. Dispatchers

`core/common/AppDispatchers.kt`:

```kotlin
interface AppDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
```

Bound to `DefaultAppDispatchers` in `core/common/di/DispatcherModule.kt`. Inject this rather than touching `Dispatchers` directly so unit tests can substitute a `TestDispatcher`.

Note: **no Phase 1 mock repository actually injects `AppDispatchers`** — they're all in-memory, so there's nothing to move off the main thread beyond the simulator's `delay`. It's there for the API and Room implementations, which will need `withContext(dispatchers.io)`.

---

## 12. Known rough edges

Flagged so you don't spend an afternoon rediscovering them. None of these block the Phase 1 demo.

| Thing | Where | Detail |
|---|---|---|
| Notification auto-clear on sign never fires | `MockClinicalStore.signReport` | It matches `it.body.contains(report.accessionNumber)`, but no seeded notification body contains an accession number — they read like `"Linda Park · MRI Brain · Acute L MCA infarct"`. Signing does not currently mark the related notification read. Either add the accession to the notification bodies or match on `NotificationTarget.Report(reportId)` instead. |
| Integration screen isn't reactive | `MockIntegrationRepository.observeSnapshot()` | Returns `flowOf(snapshot())` — a single emission. `hl7Messages` and `channels` are plain `List`s on the store, not `StateFlow`s, so the Integration screen can never update live. Fine for Phase 1; promote them to `MutableStateFlow` if you want a ticking feed. |
| Raw ISO dates in report prose | `SeedClinicalData` report `comparison` fields | `"MRI Brain ${today.minusMonths(30)}."` interpolates a `LocalDate`, rendering `2024-03-24` rather than a formatted date. `ClinicalFormat.shortDate()` in `core/utils/ClinicalFormat.kt` would produce `24 Mar 2024`. |
| Hardcoded `2026` in accession numbers | `SeedClinicalData` orders | `ACC-2026-19014` etc. are literals while everything else is relative to `today`. Correct through 2026, then inconsistent. Consider `"ACC-${today.year}-19014"`. |
| Ugly failure messages from `first { }` | `MockOrderRepository.getOrder`, `MockResultRepository.getReport`, etc. | `List.first { }` throws `NoSuchElementException` when nothing matches; the simulator's `runCatching` surfaces its message verbatim — *"Collection contains no element matching the predicate."* — straight to the user. Prefer `firstOrNull()` + an explicit `requireNotNull(...) { "…" }` with a human message. |
| A few timeline inconsistencies in the seed | `SeedClinicalData` | `ORD-44119` (Garrison) is `COMPLETED` at 08:51 with a finished report, but `APT-83264` has him `SCHEDULED` for 13:30 today. Similarly `RPT-91204` exists for Park while `APT-83248` is still `SCHEDULED` for 10:00. Cosmetic — nothing in the UI cross-checks them. |
| Notification `N-002` says "STAT" | `SeedClinicalData` | The order it points at (`ORD-44131`) is `Priority.URGENT`, not `STAT`. |
| Fully-qualified names inline | `MockTodayRepository.buildDashboard` | Parameters are typed `com.livemedica.helix.domain.model.Doctor` etc. rather than imported. Cosmetic. |
| `biometricUnlockEnabled` does nothing | `HelixSettings` | Stored, surfaced in Settings, **not wired to `BiometricPrompt`**. Documented in the model's KDoc as "architecture-ready only". |
