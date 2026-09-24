# 05 — Screens

> Every screen in LmiHL7: what it's for clinically, which files make it, what it pulls from, where its taps go, and the bits that will bite you.

Sibling docs: [`04-design-system.md`](04-design-system.md) (tokens and shared components) · [`06-data-layer.md`](06-data-layer.md) (repositories, mock store, `AppResult`).

> **Phase 1 is frontend-only.** Every repository is an in-memory mock backed by `data/mock/MockClinicalStore.kt` + `SeedClinicalData.kt`. The demo data is intentional — it's what the app is demoed with. Nothing here talks to a network.

---

## Contents

1. [The shell](#1-the-shell)
2. [Tab roots](#2-tab-roots) — [Today](#today) · [Schedule](#schedule) · [Worklist](#worklist) · [Results](#results) · [More](#more)
3. [Detail destinations](#3-detail-destinations) — [Report](#report-detail) · [Patient](#patient-detail) · [Order](#order-detail) · [Study](#study-detail) · [Appointment](#appointment-detail)
4. [Global destinations](#4-global-destinations) — [Notifications](#notifications) · [Search](#search) · [Integration](#hl7-integration) · [Profile](#profile) · [Settings](#settings)
5. [Where do I change X?](#5-where-do-i-change-x)
6. [Recipe: adding a new screen](#6-recipe-adding-a-new-screen)
7. [Known stubs and honest gaps](#7-known-stubs-and-honest-gaps)

---

## 1. The shell

Before the screens: `HelixApp.kt` decides two things and nothing else decides them.

**Where the tabs live.** Bottom bar on compact width, side rail from medium up:

```kotlin
val useRail = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass !=
    WindowWidthSizeClass.COMPACT
```

One `TopLevelDestination.entries` list drives both, so they can't drift.

**When tabs are shown at all.** `selectedTab` is `null` unless the current destination sits inside a tab graph — so detail and global destinations hide the tab affordance entirely. That's why every non-tab screen carries its own `HelixTopBar`.

```kotlin
private fun NavDestination.isWithin(destination: TopLevelDestination): Boolean =
    generateSequence(this) { it.parent }.any { it.hasRoute(destination.graph::class) }
```

`HelixAppViewModel` supplies the only app-wide state: the theme preference (drives `HelixTheme` from `MainActivity`) and `unsignedReportCount`, which is the Results tab badge (`TopLevelDestination.RESULTS` has `BadgeSource.UNSIGNED_REPORTS`).

Screens never see a `NavHostController`. They get a `HelixNavActions` — an `@Immutable` bag of named lambdas. There's a `HelixNavActions.Noop` for previews.

---

## 2. Tab roots

### Today

The doctor's home. Answers, in clinical order: what cannot wait → how much work today → what's next → what needs signing → what happened in the background.

| Role | File |
|---|---|
| Screen | `feature/today/TodayScreen.kt` |
| ViewModel | `feature/today/TodayViewModel.kt` |
| Components | `feature/today/components/TodayComponents.kt` (`CriticalResultCard`, `UpNextAppointment`, `PendingReportRow`, `pressScale`), `feature/today/components/ActivityLine.kt` |

**Data:** `TodayRepository` (the `TodayDashboard` aggregate), `ResultRepository` (sign-off list), `NotificationRepository` (recent activity). All three are combined into one `TodayUiModel` in the ViewModel so the counters and the sign-off list can never disagree.

**Interactions:**

| Tap | Goes to |
|---|---|
| Search icon | `actions.openSearch()` → Search |
| Bell icon (red dot if unread) | `actions.openNotifications()` |
| Avatar | `actions.openProfile()` |
| Critical result card | `actions.openReport(report.id)` |
| "Exams remaining" metric | `actions.openSchedule()` (switches tab) |
| "Reports awaiting sign-off" metric | `actions.openResults()` (switches tab) |
| Up-next row / "Schedule ›" | `actions.openAppointment(id)` / `actions.openSchedule()` |
| Awaiting-sign-off row / "All N ›" | `actions.openReport(id)` / `actions.openResults()` |
| Activity line | `actions.open(notification.target)` — only when the notification has a target |
| Pull to refresh | `viewModel.refresh()` |

**Gotchas:**

- `examsToday` is labelled **"Exams remaining"** on screen, but `MockTodayRepository.buildDashboard()` computes it as `appointments.filter { it.start.toLocalDate() == today }.size` — *all* of today's appointments, not the ones still ahead. The label and the number disagree.
- `UpNextAppointment` takes an `allergy: String?` and Today always passes `null`. There's a dead `private fun Patient?.primaryAllergy()` at the bottom of `TodayScreen.kt` waiting for patient context to be wired in.
- `observeLiveUpdates()` only replaces state when it's already `UiState.Success` — a background emission can't cancel a visible loading/error/offline state. Every live-updating ViewModel in the app follows this guard.
- Greeting shows surname only (`doctorName.substringAfterLast(' ')`) and facility is `facility.substringAfterLast("· ")` — the seed stores `"Mayo Regional · Main"`, so it renders as `Main`.
- Only the first `upNext` appointment is rendered; the repository returns up to 6.

---

### Schedule

The day's bookings, in three readings of the same fetched data: **Day** (a wall-clock timeline), **Week** (relative load per day), **List** (grouped by lifecycle). Switching view never refetches.

| Role | File |
|---|---|
| Screen | `feature/schedule/ScheduleScreen.kt` |
| ViewModel | `feature/schedule/ScheduleViewModel.kt` |
| State types | `feature/schedule/ScheduleUiState.kt` |
| Layout algorithm | `feature/schedule/ScheduleTimelineBuilder.kt` |
| Components | `components/ScheduleHeader.kt` (`ScheduleHeader`, `ScheduleDateStrip`), `components/ScheduleTimeline.kt` (`ScheduleDayTimeline`), `components/ScheduleWeekGrid.kt`, `components/ScheduleComponents.kt` (`ScheduleDayList`, `ScheduleAppointmentRow`), `components/ScheduleSheets.kt` (`ScheduleFacilitySheet`, `ScheduleModalitySheet`) |

**Data:** `AppointmentRepository` only — `getAppointments(date, facilityId)` for the day, `getFacilities()` for the selector, and seven parallel `getAppointments()` calls for the surrounding week.

**Interactions:**

| Action | Effect |
|---|---|
| Date strip cell | `selectDate(date)` → refetch day + week |
| ‹ / › week stepper | `shiftWeek(±1)` — keeps the same weekday selected |
| Day/Week/List segmented control | `selectViewMode(mode)` → local re-derive, no fetch |
| Building icon | Opens `ScheduleFacilitySheet` → `selectFacility(id?)` → **refetch** (facility is a query param) |
| Tune icon | Opens `ScheduleModalitySheet` → `toggleModality(m)` → **local** re-derive |
| Modality chip in the header | `toggleModality(m)` — local |
| Timeline block / list row | `actions.openAppointment(id)` |
| Week grid row | `onSelectDate(day.date)` — drops back into that day |

**Gotchas:**

- `ScheduleFilterState` lives **outside** `UiState` on purpose. The header renders unconditionally so a clinician who filtered themselves into an empty day can always change the date or drop a filter without a retry round-trip.
- Facility filter goes to the repository; modality filter is applied locally. Don't move either.
- `ScheduleTimelineBuilder.build()` is the one genuinely non-trivial algorithm: it walks appointments in start order, accumulates transitively-overlapping clusters, and column-packs each cluster into lanes. That's what makes a double-booked scanner render as two half-width blocks. The window is always at least 07:00–19:00 and stretches for anything outside it.
- `TimelineBlock` sheds detail in two directions — sideways as lanes multiply (2 lanes drops the procedure line, 3 drops the modality tag) and vertically by *measuring* available height against the type scale's line heights. Nothing dropped is lost: modality, procedure, priority, status and room all stay in the `contentDescription`.
- `MIN_SLOT_HEIGHT = Dimens.touchTargetMin - Spacing.xs` (44dp) — a 15-minute block deliberately breaks the 48dp minimum to stay temporally honest. List view is the accessible equivalent.
- The "now" line is driven by a `minuteTicker()` flow, and `nowMinutes` is `null` unless the selected day is today.
- Week view has a different emptiness rule: `publish()` checks `weekLoads.any { it.appointmentCount > 0 }` in WEEK mode, `visible.isNotEmpty()` otherwise.
- `resetFilters()` exists on the ViewModel but nothing on screen calls it — the sheets call `clearModalities()` and `selectFacility(null)` individually.

---

### Worklist

The ORM inbox: every requested exam, triaged. Two tabs — **Orders** (everything requested) and **Results** (the same list narrowed to orders that already have a report attached).

| Role | File |
|---|---|
| Screen | `feature/worklist/WorklistScreen.kt` |
| ViewModel | `feature/worklist/WorklistViewModel.kt` |
| State types | `feature/worklist/WorklistUiState.kt` (`WorklistTab`, `PriorityFilter`, `WorklistFilterState`, `WorklistStatusFilters`, `WorklistModalityFilters`) |
| Components | `components/WorklistComponents.kt` (`OrderCard`), `components/WorklistControls.kt` (`WorklistTabRow`), `components/WorklistSheets.kt` (`WorklistFilterSheet`, `OrderActionSheet`) |

**Data:** `OrderRepository` — `getOrders(priority, status, query)`, `observeOrders()` for live chip counts, `getStudyForOrder(orderId)` for the "Open study images" action.

**Interactions:**

| Action | Effect |
|---|---|
| ORDERS / RESULTS tab | `selectTab()` — local re-slice |
| Search field | `setQuery()` — **250ms debounced**, then refetch |
| Tune button (badged with `activeFilterCount`) | Opens `WorklistFilterSheet` |
| Priority chip | `selectPriority()` → refetch (a query param) |
| Filter sheet: status | `selectStatus()` → refetch |
| Filter sheet: modality | `toggleModality()` → **local** re-derive |
| Filter sheet: Reset / "Show N results" | `resetFilters()` / dismiss |
| **Tap** a card | `actions.openOrder(order.id)` |
| **Long-press** a card | Opens `OrderActionSheet` |
| Action sheet rows | Patient record / Order detail / Study images / Report (last only if `order.reportId != null`) |

**Gotchas:**

- **Search and chips sit outside `HelixStateHost`** — "the fastest route out of an empty result is the control that emptied it."
- Which filters go where is a deliberate split, stated in the ViewModel KDoc: *priority, status and free-text query* are pushed to the repository (they'll be backend query params); *tab and modality set* are applied locally (they're ways of looking at the same fetched page). **Nothing filters in a composable.**
- Chip counts come from `observeOrders()` (the whole store), not from the current page — so they show the honest total even while a narrowing filter is applied.
- "Open study images" can't navigate directly: the card carries no study id. The sheet calls `viewModel.requestStudy(orderId)`, the ViewModel resolves it, and a `LaunchedEffect(studyToOpen)` in the screen does the jump and then calls `consumeStudyRequest()` so a config change can't replay it.
- `OrderCard` leads with the modality tag when routine and with the **priority badge** when not — and only non-routine orders get a `ModalityBlock` and a coloured left rule (`critical` for STAT, `warning` for URGENT, nothing for routine).
- `loadJob` and `searchJob` are cancelled on every new load so a slow fetch can't overwrite a newer one.
- `emptyMessageFor()` produces four different messages depending on whether there's a query, an active filter, which tab you're on, or none of those.

---

### Results

The ORU inbox — the radiologist's "what do I owe?" list. Four **overlapping** categories: New, STAT, Pending sign-off, Signed. A STAT report awaiting signature legitimately appears in three of them.

| Role | File |
|---|---|
| Screen | `feature/results/ResultsScreen.kt` |
| ViewModel | `feature/results/ResultsViewModel.kt` (also defines `ResultsCategory` and `ResultsUiModel`) |
| Components | `feature/results/components/ReportCard.kt` |

**Data:** `ResultRepository` — `getReports()` once, then `observeReports()` combined with the selected category.

**Interactions:**

| Action | Effect |
|---|---|
| Category chip | `selectCategory()` — pure re-slice, **never** returns to loading |
| Report card | `actions.openReport(report.id)` |
| Retry (error state) | `viewModel.refresh()` |

**Gotchas:**

- Two different kinds of "empty". `UiState.Empty` means *the whole inbox is empty*. A filter that matches nothing is a `UiState.Success` carrying an empty list, handled inside `ResultsContent` with a per-category `HelixEmptyState` — because the chip row has to stay reachable.
- The per-category empty copy lives in two private extension properties at the bottom of `ResultsScreen.kt` (`ResultsCategory.emptyTitle` / `.emptyBody`).
- `awaitsSignature` is a private extension: `!isSigned && status != ReportStatus.DRAFT`. A draft is still being dictated, so it isn't yet the attending's to sign. This is what `awaitingSignOff` and the `PENDING_SIGN_OFF` chip both count.
- `ReportsCategory.NEW` matches on `createdAt.toLocalDate() == LocalDate.now()` — with seeded data it tracks the device clock.
- The STAT chip is the only one with a `tone`, and only when active — *"an always-red chip would desensitise the clinician to red."*
- The `ReportCard` footer **states the next action** ("Sign report →") rather than offering a button. Signing belongs on the report itself where the prose can be read first.
- The default category is `PENDING_SIGN_OFF`, not `NEW`.
- `ReportCard` is used by Results, Patient detail, Study detail and Order detail, but still lives under `feature/results/components/`.

---

### More

Everything outside the four clinical tabs, grouped by purpose: **Clinical → Workflow → Integration → System**. Group order is the product's, not alphabetical.

| Role | File |
|---|---|
| Screen | `feature/more/MoreScreen.kt` |
| ViewModel | `feature/more/MoreViewModel.kt` |
| Menu as data | `feature/more/components/MoreMenu.kt` (`moreMenuGroups()`, `MoreMenuItem`, `MoreMenuGroup`, `MoreStatusTone`) |
| Components | `feature/more/components/MoreComponents.kt` (`MoreIdentityCard`, `MoreMenuGroupCard`) |

**Data:** five repositories combined — `DoctorRepository`, `AppointmentRepository`, `ResultRepository`, `NotificationRepository`, `IntegrationRepository`. Every count on every row is resolved in the ViewModel.

**Interactions:** the menu is built by `moreMenuGroups(model, actions)`, so every destination is declared in one place:

| Row | Goes to |
|---|---|
| Patients | `openSearch()` |
| Reports | `openResults()` |
| Notifications | `openNotifications()` |
| Audit trail | — marked **"Soon"**, inert |
| Saved items | — marked **"Soon"**, inert |
| HL7 feed / Interface status / MWL sync | `openIntegration()` |
| Profile | `openProfile()` |
| Appearance / Settings | `openSettings()` |

**Gotchas:**

- Two rows are honest stubs. `unavailableNote = "Soon"` + `onClick = null` makes `HelixMenuRow` dim to 50%, drop its chevron, lose its click target and print "Soon" — deliberately better than a tappable row that does nothing.
- `MoreStatusTone` is an enum, not a `Color`, because the menu list is built *outside* composition and colours can only be resolved inside it. `MoreMenuGroupCard` converts it.
- "MWL sync" finds its channel by **name matching**: `channels.firstOrNull { it.name.contains("MWL", ignoreCase = true) }`. The domain model doesn't type channels yet. The marker constant is `WORKLIST_CHANNEL_MARKER` in `MoreViewModel.kt`.
- The `BuildStamp()` footer ("Helix · HL7 v2.5.1 · FHIR R4 · DICOM") is a hard-coded string in `MoreScreen.kt`, duplicated in `AboutSection` in `feature/settings/components/SettingsSections.kt`.
- `More` is a tab root but still renders a `HelixTopBar` — with `onBack = null`, since there's nowhere to go back to.

---

## 3. Detail destinations

All five are declared once at host level in `HelixNavHost.detailDestinations()`, each with a `helix://` deep link, and all five hide the tab bar.

### Report detail

Read the study, then sign it. Single column in clinical reading order: who → what → why → comparison → technique → findings → impression → who read it → where it stands → the signature.

| Role | File |
|---|---|
| Screen | `feature/report/ReportDetailScreen.kt` |
| ViewModel | `feature/report/ReportViewModel.kt` (also `ReportUiModel`, `SignatureState`) |
| Components | `components/ReportSections.kt` (`ReportPatientHeader`, `ReportStudyCard`, `CriticalProtocolCallout`, `ReportProseSection`), `components/ReportSignature.kt` (`ReportRadiologistCard`, `ReportStatusCard`, `ReportSignatureBlock`), `components/SignReportSheet.kt` |

**Route:** `HelixRoute.Report(reportId)` — read via `savedStateHandle.toRoute<HelixRoute.Report>().reportId`.

**Data:** `ResultRepository` (report + `signReport`), `PatientRepository`, `OrderRepository`, `DoctorRepository`.

**Interactions:**

| Action | Effect |
|---|---|
| "Patient record ›" in the header | `actions.openPatient(report.patientId)` |
| **Sign report** (only when `!report.isSigned`) | Opens `SignReportSheet` |
| Sheet → "Sign & finalise" | `viewModel.signReport()` |
| Sheet → Cancel | Hides sheet, `acknowledgeSignature()` |
| Related → Patient record / Study / Original order | `openPatient` / `openStudy` / `openOrder` |

**Gotchas:**

- **Signing has its own state machine** (`SignatureState`: `Idle` / `Submitting` / `Signed` / `Failed`) separate from the screen's `UiState`. A failed signature must not blank out the report the clinician is reading.
- Double-tap guard: `if (_signature.value == SignatureState.Submitting) return`. The repository is also idempotent, but the sheet stays on screen during submission.
- A successful signature closes the sheet itself via `LaunchedEffect(signature)` — the clinician's next act is reading the signed state, not dismissing a dialog.
- Patient/order/doctor are fetched **once** into a private `ReportContext` and cached; only the report is re-read from `observeReports()`. If `getCurrentDoctor()` fails, `fallbackDoctor(report)` synthesises one from the report's `radiologist` field so the signature block is never authorless.
- Offline is `UiState.Offline(null)` — no cached fallback on this screen.
- Signing while offline produces a specific message: *"You're offline. A report can only be signed while connected."*
- **The design prototype captured a drawn signature on a canvas. That is deferred** — `SignReportSheet`'s KDoc says so — and the attestation is made in words instead, until the backend can store and verify a signature image.
- "Original order" only appears when `model.order != null`.

---

### Patient detail

The patient record, read-only. Ordered by clinical urgency: identity → anything that changes what you may safely do (allergies) → what's happening now → history that informs the current read → the audit-style timeline.

| Role | File |
|---|---|
| Screen | `feature/patient/PatientDetailScreen.kt` |
| ViewModel | `feature/patient/PatientViewModel.kt` (also `PatientUiModel`, `PatientEvent`, `PatientEventKind`) |
| Components | `components/PatientHeader.kt` (`PatientIdentity`, `PatientAllergyBanner`), `components/PatientSections.kt` (`CurrentOrderRow`, `StudyRow`, `PatientAppointmentRow`), `components/PatientHistory.kt` (`PriorStudyEntry`, `HistoryBullet`), `components/PatientTimeline.kt` (`PatientTimelineEvent`) |

**Route:** `HelixRoute.Patient(patientId)`.

**Data:** `PatientRepository` (patient + orders + studies + reports + appointments), `ResultRepository` (`observeReports()` to rebuild when a report changes).

**Interactions:** current order → `openOrder`; current/previous study → `openStudy`; appointment → `openAppointment`; report card → `openReport`. The timeline is not tappable.

**Gotchas:**

- `"NKDA"` is a recorded **negative**, not an allergy — `activeAllergies` filters it out so it can't raise the amber banner. Same rule appears in `ExamContext.activeAllergies`.
- "Current" is a clinical judgement made in the ViewModel, not the layout: `currentOrder = orders.firstOrNull { it.status.isOpen }` where `isOpen` means not COMPLETED and not CANCELLED. `currentStudy` is the study attached to that order.
- Problems vs priors are split by a **regex on the summary string**: `Regex("^[A-Z]{2} · ")`. The seed carries both in one `history` list and priors lead with a DICOM code (`CT · CT Head w/o Contrast`). Fragile by design and documented as such.
- The timeline is assembled from four sources, sorted descending, capped at `TIMELINE_LIMIT = 12`. A signed report contributes **two** events (dictated + signed).
- `observeReportChanges()` rebuilds only when `reports.any { it.patientId == patientId }`.
- Offline is `UiState.Offline(null)`.

---

### Order detail

The requisition as the department received it. Reads top-down as the HL7 ORM does: who → what was asked for → why → who asked → what came back.

| Role | File |
|---|---|
| Screen | `feature/study/OrderDetailScreen.kt` |
| ViewModel | `feature/study/OrderDetailViewModel.kt` |
| Shared context | `feature/study/ExamContext.kt` |
| Components | `feature/study/components/ExamComponents.kt`, `ExamActions.kt` |

**Route:** `HelixRoute.Order(orderId)`. **Data:** `ExamContextLoader.forOrder(orderId)` + `ResultRepository.observeReports()`.

**Interactions:** patient strip → `openPatient`; report card → `openReport`; `ExamActions` → Open images (disabled, see [stubs](#7-known-stubs-and-honest-gaps)) / Add note / View patient record.

---

### Study detail

The acquired imaging. Where Order leads with the request, this leads with the acquisition: what exists in the archive, how much of it, and the handle a viewer needs.

| Role | File |
|---|---|
| Screen | `feature/study/StudyDetailScreen.kt` |
| ViewModel | `feature/study/StudyDetailViewModel.kt` |
| Shared context | `feature/study/ExamContext.kt` |
| Components | `feature/study/components/ExamComponents.kt`, `ExamActions.kt`, `PacsHandoff.kt` |

**Route:** `HelixRoute.Study(studyId)`. **Data:** `ExamContextLoader.forStudy(studyId)` + `ResultRepository.observeReports()`.

**Gotchas:**

- The Study Instance UID is rendered **in full**, never truncated — *"it is the handle the PACS hand-off will use, and a truncated UID is useless to anyone reading it out to a technologist."*
- If the study's order can't be resolved, the loader returns `ExamContext(null, null, study, null, null, null)` and the screen degrades: no clinical indication section, status falls back to `OrderStatus.COMPLETED`, priority to `Priority.ROUTINE`.

---

### Appointment detail

The booking — which precedes the order and the study. This screen deliberately says what does **not** exist yet rather than leaving blank sections.

| Role | File |
|---|---|
| Screen | `feature/study/AppointmentDetailScreen.kt` |
| ViewModel | `feature/study/AppointmentDetailViewModel.kt` |
| Components | `feature/study/components/ExamComponents.kt`, `ExamActions.kt` |

**Route:** `HelixRoute.Appointment(appointmentId)`. **Data:** `ExamContextLoader.forAppointment(appointmentId)`.

**Gotchas:**

- Nothing is observed live — an appointment isn't mutated by anything the app can currently do. One-shot read + manual retry.
- `forAppointment()` hard-codes `order = null, study = null, report = null`. The "Order" section always shows *"No order filled yet"*, and `ExamHeadline` gets `accessionNumber = null, cptCode = null`.

#### The three exam screens share one context loader

`ExamContext.kt` models an order, a study and an appointment as three views of the same episode of care:

```kotlin
data class ExamContext(
    val patient: Patient?,
    val order: Order?,
    val study: Study?,
    val report: RadiologyReport?,
    val appointment: Appointment?,
    val facilityName: String?,
)
```

`ExamContextLoader` is a plain `@Inject`-able collaborator (not a base ViewModel) with `forOrder()`, `forStudy()` and `forAppointment()`. Its report lookup prefers `order.reportId` and falls back to matching on `accessionNumber`, *"because an ORU can arrive before the order is updated."*

`OrderDetailViewModel` and `StudyDetailViewModel` both use `observeReports().drop(1)` — `drop(1)` skips the store's current value, which the initial load has already read.

---

## 4. Global destinations

### Notifications

The notification centre. Unread is a **section**, not a badge — the clinician's question is "what have I not seen?", and a list answers that better than a number.

| Role | File |
|---|---|
| Screen | `feature/notifications/NotificationsScreen.kt` |
| ViewModel | `feature/notifications/NotificationsViewModel.kt` (also `NotificationsUiModel`) |
| Components | `feature/notifications/components/NotificationRow.kt` (also exports `NotificationCategory.presentation()`) |

**Data:** `NotificationRepository` — `observeNotifications()`, `markRead`, `markAllRead`, `clear`.

**Interactions:**

| Action | Effect |
|---|---|
| Row tap | `markRead(id)` **then** `actions.open(notification.target)` |
| Row × button | `clear(id)` |
| Top-bar "done all" (only when unread > 0) | `markAllRead()` |
| Category chip | `selectCategory(category?)` |

**Gotchas:**

- `state` is a `stateIn`-shared `combine` of the repository flow and the selected category — no `MutableStateFlow` of `UiState`, no manual `load()`. That makes it the simplest ViewModel in the app to copy.
- `counts` is computed over the **whole** list, not the filtered one, so chips keep showing bucket sizes while a filter is applied.
- Only categories with `count > 0` get a chip — *"an empty filter is a dead end."*
- Chips use `HelixFilterChipStyle.Tinted` (the system-screen treatment), unlike the clinical screens' `Solid`.
- Failed writes surface as an inline `ActionMessageBanner` that self-dismisses after 4s — not a snackbar, because the list stays on screen unchanged and the message has to sit next to the thing that didn't change. This is how `Settings → Simulate error` shows up here.
- Read rows dim to 68% *and* lose their dot, so unread survives greyscale.
- `NotificationCategory.presentation()` is the app-wide category → icon+colour map. It's duplicated as a `private fun` in `feature/today/components/ActivityLine.kt` — if you change one, change both.

---

### Search

Global search across every clinical entity.

| Role | File |
|---|---|
| Screen | `feature/search/SearchScreen.kt` |
| ViewModel | `feature/search/SearchViewModel.kt` |
| Components | `components/RecentQueries.kt` (`SearchIntroPanel`), `components/SearchResultRows.kt` (`SearchGroup`, `SearchResultDivider`, `PatientResultRow`, `OrderResultRow`, `ReportResultRow`, `StudyResultRow`, `AppointmentResultRow`) |

**Data:** `SearchRepository` — `search(query)`, `observeRecentQueries()`, `recordQuery()`.

**Interactions:** typing debounces 250ms then searches; IME "Search" bypasses the debounce **and** records the query; a recent-query row re-runs it; opening any result calls `onResultOpened()` then navigates.

**Gotchas:**

- The field sits **outside** `HelixStateHost` — a failed or empty search must never take the input away.
- Blank query renders `SearchIntroPanel` (recents + "search across…"), not an empty state. *"'nothing here yet' is a different message from 'nothing matched', and conflating the two is the classic way a search screen feels broken on first open."*
- A query is promoted to "recent" when a **result is opened**, not merely when it's typed. `onSearchSubmitted()` also records.
- `IDLE_STATE` is a `UiState.Empty` the screen never actually renders — it's a safety net.
- `clearQuery()` exists on the ViewModel but the screen wires the search bar's clear button to `onQueryChange` via `HelixSearchBar`'s default `onClear`.
- Group order is people first, then the work attached to them: Patients → Orders → Reports → Studies → Appointments.
- `autoFocus = true` and `elevation = Elevation.none` are set here and nowhere else.

---

### HL7 Integration

Mirth / interface operations. **Deliberately not clinical** — no patient names, no priorities, no modality thread. Answers "is the interface healthy" in the order an engineer asks it: volume and ACK rate → mix by type → which channel is misbehaving → the raw feed.

| Role | File |
|---|---|
| Screen | `feature/integration/IntegrationScreen.kt` |
| ViewModel | `feature/integration/IntegrationViewModel.kt` (also `IntegrationUiModel`) |
| Components | `components/IntegrationSummary.kt` (`IntegrationHeadline`, `MessageTypeCounts`), `components/IntegrationComponents.kt` (`hl7TypeColor`, `Hl7TypeBlock`, `ChannelRow`, `Hl7MessageRow`) |

**Data:** `IntegrationRepository.getSnapshot()`.

**Interactions:** message-type chips call `selectType(type?)`, which **re-filters in place** via `_state.update` — changing a filter must not throw the screen back to a skeleton. Nothing on this screen navigates.

**Gotchas:**

- Channel **direction is inferred from the channel name** (`name.contains("-IN", ignoreCase = true)`) because the domain model doesn't carry it. Documented in `ChannelRow`'s KDoc.
- Channels render worst-first: `sortedByDescending { it.state.ordinal }` — relying on `InterfaceState` being declared ONLINE, DEGRADED, OFFLINE. Reorder that enum and you break the sort.
- `MessageTypeCounts` always shows all four types even at zero — *"a missing ADT count is itself the diagnosis."*
- The ACK rate value turns `warning` below `ACK_TARGET_PERCENT = 99.0`.
- `IntegrationViewModel` has its own private `UiState.map { }` extension — `UiState` doesn't provide one.
- `selectedType` is a plain `var`, not a `StateFlow`; it survives only as long as the ViewModel.

---

### Profile

The clinician's own record. On-call sits near the top because it's the only control here that changes clinical behaviour.

| Role | File |
|---|---|
| Screen | `feature/profile/ProfileScreen.kt` |
| ViewModel | `feature/profile/ProfileViewModel.kt` |
| Components | `feature/profile/components/ProfileComponents.kt` (`ProfileHeader`, `CredentialField`, `CredentialRow`, `SignOutDialog`, `ProfileCard`) |

**Data:** `DoctorRepository` — `getCurrentDoctor()`, `observeCurrentDoctor()`, `setOnCall()`.

**Interactions:** the On-call switch writes through `setOnCall()`; the three Preferences rows and "Edit profile"/"Sign out" are in Account.

**Gotchas:**

- On-call is a **real write**, not a local toggle — *"it changes who the paging system routes critical findings to, so the switch must reflect what the server accepted rather than what was tapped."* The switch renders from `doctor.isOnCall`, and a failure surfaces as an amber banner under the top bar.
- All three Preferences rows (Notification preferences / Appearance / Security) go to the **same** destination: `actions.openSettings()`. Settings has no deep-link-to-section support.
- "Edit profile" is `unavailableNote = "Read-only"` and inert — profile fields are managed by the facility.
- **"Sign out" opens a dialog that explicitly says nothing happens.** `SignOutDialog`: *"Authentication isn't connected in this build, so this won't end your session yet."* The only button is "Understood".
- `dismissActionMessage()` exists but nothing calls it — the banner has no dismiss affordance and no auto-timeout (unlike the Notifications one).

---

### Settings

Ordered by how often a clinician touches it: appearance and alerts → security → developer switches, visibly separated because they change how the whole app loads data.

| Role | File |
|---|---|
| Screen | `feature/settings/SettingsScreen.kt` |
| ViewModel | `feature/settings/SettingsViewModel.kt` |
| Components | `components/SettingsSections.kt` (`AppearanceSection`, `NotificationSection`, `SecuritySection`, `DeveloperSection`, `AboutSection`), `components/AppVersion.kt` (`rememberAppVersionName`) |

**Data:** `SettingsRepository.observeSettings()` → `HelixSettings`, plus seven setters.

**Sections:**

| Section | Controls |
|---|---|
| Appearance | `HelixSegmentedControl` over `ThemePreference` (System / Light / Dark) |
| Notifications | Critical findings · Results ready · Schedule changes |
| Security | Biometric unlock (stub) · Active sessions ("Soon") |
| Developer | Simulate offline · Simulate error |
| About | App name, version from the package manager, standards line |

**Gotchas:**

- **There is no local mirror of any switch.** Every write goes to the repository and the screen re-renders from `observeSettings()`. That's what makes the theme selector and the developer switches genuinely authoritative — the same flow drives this screen, `MainActivity`'s `HelixTheme`, and `MockCallSimulator`.
- **Biometric unlock stores a preference and nothing else.** No `BiometricPrompt` is wired up. The row carries `lockedNote = "Not active yet — the preference is stored, but nothing is locked."` because *"a clinician must never believe a shared device is locked when it is not."* Same note on `HelixSettings.biometricUnlockEnabled`.
- Turning **Critical findings** off produces a persistent amber `lockedNote`: *"Critical findings will not alert you while this is off."*
- The developer switches drive `data/mock/MockCallSimulator.kt`, so **every repository call in the app** returns offline or failure while they're on. That's the only way to review offline/error states before there's a backend.
- The version string is read from the `PackageManager`, not `BuildConfig` — this module has `android.buildFeatures.buildConfig` off.
- `state` is never `Empty` in practice (`observeSettings()` always emits), so the `emptyTitle`/`emptyBody` are defensive only.
- The palette selector from the design does not exist here — see [`04-design-system.md`](04-design-system.md) §3.3.

---

## 5. Where do I change X?

| I want to… | Edit |
|---|---|
| **Change the worklist card layout** | `feature/worklist/components/WorklistComponents.kt` → `OrderCard` |
| **Change what a worklist long-press offers** | `feature/worklist/components/WorklistSheets.kt` → `OrderActionSheet` |
| **Add a worklist filter** | `feature/worklist/WorklistUiState.kt` (add to `WorklistFilterState` + `activeFilterCount`), `WorklistViewModel.kt` (setter + `visibleOrders()`), `components/WorklistSheets.kt` (`WorklistFilterSheet` UI) |
| **Change which statuses/modalities the worklist filter offers** | `feature/worklist/WorklistUiState.kt` → `WorklistStatusFilters` / `WorklistModalityFilters` |
| **Change the worklist search debounce** | `feature/worklist/WorklistViewModel.kt` → `SEARCH_DEBOUNCE_MS` |
| **Change how orders are sorted** | `data/repository/MockWorkflowRepositories.kt` → `MockOrderRepository.worklistOrdering` |
| **Change what fields worklist search matches** | `data/repository/MockWorkflowRepositories.kt` → `Order.matches(query)` |
| **Change what Today counts** | `data/repository/MockWorkflowRepositories.kt` → `MockTodayRepository.buildDashboard()` |
| **Change Today's section order / labels** | `feature/today/TodayScreen.kt` → `TodayContent` |
| **Change how many "awaiting sign-off" rows Today previews** | `feature/today/TodayScreen.kt` → `AWAITING_PREVIEW_COUNT` |
| **Change how many activity lines Today shows** | `feature/today/TodayViewModel.kt` → `RECENT_ACTIVITY_LIMIT` |
| **Change the greeting wording / time bands** | `core/utils/ClinicalFormat.kt` → `greeting()` |
| **Change any date/time/age format** | `core/utils/ClinicalFormat.kt` |
| **Change the schedule timeline's hour height** | `feature/schedule/components/ScheduleTimeline.kt` → `HOUR_HEIGHT` |
| **Change the schedule's default day window** | `feature/schedule/ScheduleTimelineBuilder.kt` → `DEFAULT_START_HOUR` / `DEFAULT_END_HOUR` |
| **Change how double-bookings are laid out** | `feature/schedule/ScheduleTimelineBuilder.kt` → `build()` |
| **Change the schedule List view's groups** | `feature/schedule/ScheduleUiState.kt` → `ScheduleGroup` + `AppointmentStatus.scheduleGroup()`; colours in `components/ScheduleComponents.kt` → `ScheduleGroup.accent()` |
| **Add a Results category** | `feature/results/ResultsViewModel.kt` → `ResultsCategory` + `RadiologyReport.matches()`; empty copy in `feature/results/ResultsScreen.kt` |
| **Change the result card** | `feature/results/components/ReportCard.kt` (affects Results, Patient, Study, Order) |
| **Change the report reading order / sections** | `feature/report/ReportDetailScreen.kt` → `ReportContent` |
| **Change the sign-off confirmation copy** | `feature/report/components/SignReportSheet.kt` |
| **Change what the signature block shows** | `feature/report/components/ReportSignature.kt` → `ReportSignatureBlock` |
| **Change what counts as "current" on a patient** | `feature/patient/PatientViewModel.kt` → `OrderStatus.isOpen` |
| **Change how priors are split from problems** | `feature/patient/PatientViewModel.kt` → `HistoryEntry.isPriorStudy` |
| **Change the patient timeline length** | `feature/patient/PatientViewModel.kt` → `TIMELINE_LIMIT` |
| **Wire up the PACS viewer** | `feature/study/components/PacsHandoff.kt` → `resolve()`; caller is `components/ExamActions.kt` |
| **Add/remove/reorder a More menu row** | `feature/more/components/MoreMenu.kt` → `moreMenuGroups()` |
| **Change a notification's icon or colour** | `feature/notifications/components/NotificationRow.kt` → `NotificationCategory.presentation()` **and** `feature/today/components/ActivityLine.kt` → the private `presentation()` |
| **Add a settings toggle** | `domain/model/HelixSettings.kt`, `domain/repository/SettingsRepository.kt`, `data/repository/SettingsRepositoryImpl.kt`, `data/local/HelixPreferencesDataSource.kt`, `feature/settings/SettingsViewModel.kt`, `feature/settings/components/SettingsSections.kt` |
| **Change the tabs, their order, icons or badges** | `core/navigation/TopLevelDestination.kt` |
| **Change the bottom bar / rail look** | `core/designsystem/component/HelixBottomNavigation.kt` / `HelixNavigationRail.kt` |
| **Change the breakpoint where the rail replaces the bar** | `HelixApp.kt` → `useRail` |
| **Add a route** | `core/navigation/HelixRoute.kt` + `HelixNavHost.kt` + `HelixNavActions.kt` |
| **Change loading / empty / error / offline rendering** | `core/ui/HelixStateHost.kt` |
| **Change how a repository result becomes screen state** | `core/ui/UiStateMapping.kt` |
| **Change the demo data** | `data/mock/SeedClinicalData.kt` |
| **Change offline/error simulation behaviour** | `data/mock/MockCallSimulator.kt` |
| **Change any colour, size, spacing or shared component** | See [`04-design-system.md`](04-design-system.md) §10 |

---

## 6. Recipe: adding a new screen

Worked example: a global destination called **Handover**. Snippets are copied from `Profile`, the simplest existing screen of this shape.

### Step 1 — create the package

```
feature/handover/
├── HandoverScreen.kt
├── HandoverViewModel.kt
└── components/
    └── HandoverComponents.kt
```

One file per screen, one for its ViewModel, and a `components/` package for anything only this screen uses. Keep the UI state types in the ViewModel file if they're small (like `ProfileViewModel`, `ResultsViewModel`) or in their own `*UiState.kt` if they aren't (like `WorklistUiState.kt`, `ScheduleUiState.kt`).

### Step 2 — the ViewModel

```kotlin
package com.livemedica.helix.feature.handover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.core.ui.toUiState
import com.livemedica.helix.domain.repository.DoctorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HandoverViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HandoverUiModel>>(UiState.Loading)
    val state: StateFlow<UiState<HandoverUiModel>> = _state.asStateFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = doctorRepository.getCurrentDoctor().toUiState()
                // …map to HandoverUiModel here
        }
    }
}
```

Conventions to keep:

- `@HiltViewModel` + `@Inject constructor`. Repositories are bound in `data/di/RepositoryModule.kt`.
- Private `MutableStateFlow`, public `StateFlow` via `asStateFlow()`. Never expose the mutable one.
- `AppResult` → `UiState` goes through `core/ui/UiStateMapping.kt` (`toUiState()` / `toListUiState()`), so "empty" means the same thing everywhere.
- A `refresh()` for `HelixStateHost`'s retry.
- If you also observe a flow, guard it: `if (_state.value is UiState.Success) _state.value = UiState.Success(model)`. A background emission must never cancel a visible loading/error/offline state.
- Constants go in a `private companion object` at the bottom.

### Step 3 — the UiState model

```kotlin
/** Everything the Handover screen renders, assembled once so its parts cannot disagree. */
@Immutable
data class HandoverUiModel(
    val doctor: Doctor,
    val outstanding: List<RadiologyReport>,
)
```

One model per screen. Derive everything here or in the ViewModel — **never filter or compute in a composable**.

### Step 4 — the screen

```kotlin
package com.livemedica.helix.feature.handover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost

@Composable
fun HandoverScreen(
    actions: HelixNavActions,
    viewModel: HandoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        HelixTopBar(title = "Handover", onBack = actions.navigateBack)

        HelixStateHost(
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Nothing to hand over",
            emptyBody = "Every report assigned to you has been signed.",
        ) { model ->
            HandoverContent(model = model, actions = actions)
        }
    }
}
```

Conventions:

- Signature is always `(actions: HelixNavActions, viewModel: X = hiltViewModel())`.
- `collectAsStateWithLifecycle()`, never `collectAsState()`.
- Non-tab destinations carry their own `HelixTopBar` with `onBack = actions.navigateBack` — the tab bar is hidden for them.
- Wrap success content in `HelixStateHost` and give it screen-specific empty copy.
- Split the success body into a private `HandoverContent(...)` so the public screen stays a thin shell.
- Put persistent controls (search fields, filter chips, headers) **outside** the state host if the user needs them to escape an empty result.
- Use `windowInsetsPadding(WindowInsets.statusBars)` on tab roots (no top bar) and `navigationBarsPadding()` on pushed screens.

### Step 5 — add the route

`core/navigation/HelixRoute.kt`:

```kotlin
/** Global destinations. */
@Serializable data object Notifications : HelixRoute
@Serializable data object Search : HelixRoute
@Serializable data object Integration : HelixRoute
@Serializable data object Profile : HelixRoute
@Serializable data object Settings : HelixRoute
@Serializable data object Handover : HelixRoute      // ← new
```

If it takes an argument, use a `data class` instead — that's what makes the id compiler-checked:

```kotlin
@Serializable data class Report(val reportId: String) : HelixRoute
```

...and read it in the ViewModel with `savedStateHandle.toRoute<HelixRoute.Report>().reportId`.

Add a deep link too if push notifications will target it:

```kotlin
object HelixDeepLinks {
    const val SCHEME = "helix"
    const val REPORT = "$SCHEME://report/{reportId}"
}
```

### Step 6 — register it in the nav host

`core/navigation/HelixNavHost.kt`:

```kotlin
private fun NavGraphBuilder.globalDestinations(actions: HelixNavActions) {
    composable<HelixRoute.Notifications> { NotificationsScreen(actions) }
    composable<HelixRoute.Search> { SearchScreen(actions) }
    composable<HelixRoute.Integration> { IntegrationScreen(actions) }
    composable<HelixRoute.Profile> { ProfileScreen(actions) }
    composable<HelixRoute.Settings> { SettingsScreen(actions) }
    composable<HelixRoute.Handover> { HandoverScreen(actions) }   // ← new
}
```

Argument-carrying detail destinations go in `detailDestinations()` with their deep link:

```kotlin
composable<HelixRoute.Report>(
    deepLinks = listOf(navDeepLink { uriPattern = HelixDeepLinks.REPORT }),
) { ReportDetailScreen(actions) }
```

**Don't** nest a detail destination inside a tab graph. They're declared once at host level so "the same report opened from Today and from Results" is the same destination.

### Step 7 — add the nav action

`core/navigation/HelixNavActions.kt` — three edits, all mechanical.

```kotlin
@Immutable
class HelixNavActions(
    // …
    val openSettings: () -> Unit,
    val openHandover: () -> Unit,        // ← 1. the property
    // …
) {
    companion object {
        val Noop = HelixNavActions(
            // …
            openSettings = {},
            openHandover = {},           // ← 2. the no-op for previews
            // …
        )
    }
}

fun rememberHelixNavActions(
    navController: NavHostController,
    selectTab: (TopLevelDestination) -> Unit,
): HelixNavActions = HelixNavActions(
    // …
    openSettings = { navController.navigate(HelixRoute.Settings) },
    openHandover = { navController.navigate(HelixRoute.Handover) },   // ← 3. the real impl
    // …
)
```

Screens are given `HelixNavActions` rather than a `NavHostController` on purpose: *"every transition the app supports is named here, which keeps navigation reviewable and makes screens trivially previewable with no-op actions."*

If the new screen is reachable from a notification, also add a `NotificationTarget` case in `domain/model/ClinicalModels.kt` and handle it in `HelixNavActions.open(target)` — the `when` is exhaustive, so the compiler will tell you.

### Step 8 — put an entry point to it somewhere

Nothing reaches a new screen by itself. Usually that means a row in `feature/more/components/MoreMenu.kt`:

```kotlin
MoreMenuItem(
    icon = Icons.Outlined.SwapHoriz,
    title = "Handover",
    subtitle = "${model.unsignedReports} reports to pass on",
    count = model.unsignedReports,
    onClick = actions.openHandover,
),
```

If it isn't built yet, ship it honest — `unavailableNote = "Soon"` and no `onClick`.

### Step 9 — if it should be a *tab* instead

Add a graph + root to `HelixRoute.kt`, a `navigation<...>` block to `HelixNavHost.kt`, and an entry to `core/navigation/TopLevelDestination.kt`:

```kotlin
enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val graph: HelixRoute,
    val root: HelixRoute,
    val badgeSource: BadgeSource = BadgeSource.NONE,
) {
    TODAY("Today", Icons.Outlined.Home, HelixRoute.TodayGraph, HelixRoute.Today),
    // …
    RESULTS("Results", Icons.Outlined.Description, HelixRoute.ResultsGraph, HelixRoute.Results, BadgeSource.UNSIGNED_REPORTS),
}
```

The bottom bar and the rail both render `TopLevelDestination.entries`, so they pick it up automatically. If it needs a badge, add a `BadgeSource` case and wire the count in `HelixAppViewModel` + `HelixApp`'s `badgeCounts` map. Tab roots use `windowInsetsPadding(WindowInsets.statusBars)` instead of a `HelixTopBar` (except More, which has both).

### Checklist before you're done

- [ ] Reads its data through a `domain/repository` interface, never a mock class directly
- [ ] Every filter/derivation is in the ViewModel, not a composable
- [ ] Wrapped in `HelixStateHost` with screen-specific empty copy
- [ ] Controls that let a user escape an empty result sit outside the state host
- [ ] No hard-coded colour, dp or sp (see [`04-design-system.md`](04-design-system.md) §6)
- [ ] Every tappable target ≥ `Dimens.touchTargetMin`
- [ ] Any status shows text, not just colour
- [ ] Tested against `Settings → Developer → Simulate offline` and `Simulate error`
- [ ] Tested in light and dark

---

## 7. Known stubs and honest gaps

Documented deliberately — these are *designed* gaps, not bugs. Each one says so on screen.

| Thing | Where | State |
|---|---|---|
| **PACS / DICOM viewer hand-off** | `feature/study/components/PacsHandoff.kt` | `resolve(study)` returns `null` unconditionally. "Open images" renders **disabled** with the reason *"No imaging viewer is configured for this facility yet."* The file's KDoc explains what to replace and why it's empty: no viewer endpoint was supplied, and inventing one would bake a fictional host into the app. Only this object and its injection point should need to change. |
| **Add note** | `feature/study/components/ExamActions.kt` | Tapping it sets a local `noteRequested` flag and prints *"Notes are not yet synchronised to the record. Dictate into the RIS for now."* Phase 2 write path. |
| **Biometric unlock** | `feature/settings/components/SettingsSections.kt`, `domain/model/HelixSettings.kt` | Stores a preference only. No `BiometricPrompt`. Permanent `lockedNote` on the row. |
| **Sign out** | `feature/profile/components/ProfileComponents.kt` | `SignOutDialog` says authentication isn't connected; the only button is "Understood". |
| **Edit profile** | `feature/profile/ProfileScreen.kt` | `unavailableNote = "Read-only"`, inert. |
| **Active sessions** | `feature/settings/components/SettingsSections.kt` | `unavailableNote = "Soon"`, inert. |
| **Audit trail**, **Saved items** | `feature/more/components/MoreMenu.kt` | `unavailableNote = "Soon"`, inert. |
| **Drawn signature capture** | `feature/report/components/SignReportSheet.kt` | The design prototype had a signature canvas. Deferred until the backend can store and verify a signature image; the attestation is in words instead. |
| **Patient allergy on Today's Up Next** | `feature/today/TodayScreen.kt` | `UpNextAppointment` supports it; Today passes `null`. A dead `Patient?.primaryAllergy()` helper is left in place for when patient context is wired in. |
| **Brand palette selector** | `core/designsystem/theme/HelixColors.kt` | Four `HelixPalette` options exist; `MainActivity` never passes one, so the app is always Violet. |
| **Network layer** | `core/network/`, `data/remote/` | `HelixApiService`, `ApiRoutes`, `AuthInterceptor`, `TokenProvider`, `NetworkMonitor` exist as architecture-ready scaffolding. Nothing calls them — every repository binding in `data/di/RepositoryModule.kt` points at a mock. See [`06-data-layer.md`](06-data-layer.md). |

### And one genuine inconsistency

Today's first metric is labelled **"Exams remaining"** but `MockTodayRepository.buildDashboard()` sets `examsToday = appointments.filter { it.start.toLocalDate() == today }.size` — every appointment today, including finished ones. Either the label should read "Exams today" or the count should filter on `it.end.isAfter(now)` the way `upNext` does. Worth deciding before the demo.
