# 12 — Roadmap and known gaps

What's finished, what's deliberately unfinished, and what to do next. Written to be honest rather
than flattering — if you're picking this up cold, this is the page that saves you time.

- [Phase 1 — done](#phase-1--done)
- [Deliberately not built](#deliberately-not-built)
- [Known gaps and rough edges](#known-gaps-and-rough-edges)
- [Phase 2 — backend](#phase-2--backend)
- [Phase 3 — beyond](#phase-3--beyond)
- [Before any real clinical use](#before-any-real-clinical-use)

---

## Phase 1 — done

All verified running on an emulator, both themes, no crashes across a full walkthrough.

| Area | Status |
|---|---|
| Design system ported 1:1 from the approved design | ✅ tokens, type, 12 shared components |
| Dark + light themes from one token set | ✅ no duplicated UI |
| Navigation: type-safe routes, per-tab back stacks, deep links | ✅ |
| Today, Schedule, Worklist, Results, More | ✅ |
| Report / patient / study / order / appointment detail | ✅ |
| **Sign report**, with confirmation and app-wide propagation | ✅ |
| Notifications, global search | ✅ |
| HL7 integration readout | ✅ |
| Profile, settings, persisted theme | ✅ |
| Loading / empty / error / offline on every screen | ✅ |
| Adaptive layout (rail on tablet widths) | ✅ |
| Pull-to-refresh | ⚠️ Today only |
| Unit tests | ✅ repositories, search, sign flow |
| Lint | ✅ clean apart from dependency-version notices |

---

## Deliberately not built

These are **decisions, not oversights**. Each has a reason and a documented seam.

### No drawn-signature canvas
The design shows one. There is no backend that can store or verify a signature image, and a canvas
that silently discards its strokes would misrepresent a legal act. Replaced with a worded
attestation in `feature/report/components/SignReportSheet.kt`.

**To finish:** needs a backend endpoint that accepts and audit-logs signature material, plus a legal
review of what constitutes a valid electronic signature in your jurisdiction.

### No PACS deep link
`feature/study/components/PacsHandoff.kt` is the seam. `resolve(study)` returns `null`, "Open images"
renders disabled with an honest reason, and the file documents exactly what to replace. No URL was
invented.

**To finish:** get the PACS viewer's URI scheme, implement `resolve()`, add the study instance UID
(already on the `Study` model) to the URI.

### No real biometric lock
The preference is stored and labelled *"Not active yet — the preference is stored, but nothing is
locked."* No `BiometricPrompt` is wired.

**To finish:** `androidx.biometric`, gate app resume, decide the fallback policy.

### No real authentication
`TokenProvider` / `InMemoryTokenProvider` / `AuthInterceptor` exist and compile. No token is ever
issued, stored or sent. See [09-backend-integration.md](09-backend-integration.md).

### No Room database
Models are flat and Room-ready. Nothing is persisted except user preferences.

### No FCM / WebSocket
`HelixRealtimeEvent` defines the six event types as a sealed interface; `RealtimeEventSource` has no
implementation.

### Screens marked "Soon"
Audit trail, Saved items, Active sessions, Edit profile, My analytics. Rendered as visibly inert
rows rather than dead-but-tappable ones, because there's no data behind them. The design also has an
HL7 **message inspector** (raw MSH/PID/PV1 segment viewer) — `Hl7Message` carries no segments or raw
payload, so the feed rows don't navigate.

---

## Known gaps and rough edges

Worth fixing; none are blocking.

| # | Gap | Where | Notes |
|---|---|---|---|
| 1 | **Developer switches ship in release builds** | `feature/settings/` | Simulate offline/error are reachable in any build. Gate behind `BuildConfig.DEBUG` before a real release. |
| 2 | **Deep links have no manifest intent-filter** | `AndroidManifest.xml` | Routes accept deep links, but nothing external can trigger them. Add the `helix` scheme filter when FCM lands. |
| 3 | **Pull-to-refresh only on Today** | `feature/*/` | `refresh()` is exposed on most ViewModels; the gesture host isn't wired on other screens. |
| 4 | **No swipe actions on worklist rows** | `feature/worklist/` | Design shows swipe-to-review. There's no "mark reviewed" repository method to back it, so tap + long-press cover the destinations instead. |
| 5 | **No snackbar host** | `HelixApp.kt` | Nowhere to show transient confirmations. Needed before actions like "Priority updated" can exist. |
| 6 | **`buildConfig` is disabled** | `app/build.gradle.kts` | `AppVersion` reads the version from `PackageManager` instead. Enable `buildConfig = true` when you add the API base-URL field. |
| 7 | **Timeline blocks can be under 48dp** | `ScheduleTimeline.kt` | A 15-minute exam can't be 48dp tall without misrepresenting its duration. The List view is the accessible equivalent; documented in the file. |
| 8 | **Three files over 250 lines** | `TodayComponents.kt`, `ScheduleTimeline.kt`, `TodayScreen.kt` | Cosmetic. `SeedClinicalData.kt` is long on purpose — it's data. |
| 9 | **Only violet palette reachable** | `HelixColors.kt` | Four `HelixPalette` options exist; no UI exposes the switch. |
| 10 | **No instrumented UI tests** | `app/src/androidTest/` | Only the template test. Compose UI test deps are already wired. |
| 11 | **`android.disallowKotlinSourceSets=false`** | `gradle.properties` | AGP 9 / KSP workaround, required for Hilt. Remove when KSP adopts the AGP 9 source API. |

---

## Phase 2 — backend

The whole point of the architecture. Full guide in
[09-backend-integration.md](09-backend-integration.md); the shape is:

1. Stand up the AWS API against the contract in `data/remote/HelixApiService.kt`.
2. Add a `NetworkModule` (OkHttp + `AuthInterceptor` + kotlinx-serialization converter + Retrofit).
3. Put the base URL in a build-config field per build type. Never in source.
4. Write `Api*Repository` classes implementing the existing domain interfaces.
5. Flip the bindings in `data/di/RepositoryModule.kt` — **one line per repository**.
6. Delete `SeedClinicalData.kt` and `MockClinicalStore.kt` once every binding has moved.

**No UI, ViewModel or navigation code changes in any of those steps.** That's the test of whether
the architecture held.

Order I'd suggest: auth → doctor profile → worklist/orders → results/reports → sign → appointments
→ notifications → search → integration. Sign last, because it's the only write and deserves the
most scrutiny.

---

## Phase 3 — beyond

- **Room**, between the API and the repository, for genuine offline reading
- **FCM** for critical-result push, wired into `RealtimeEventSource`
- **PACS viewer** hand-off
- **Audit trail** — per-patient HL7 event traceability (the data exists server-side)
- **Analytics** — studies read, turnaround times, on-call load
- **Tablet list/detail** — the rail is in; two-pane layouts are not
- **Wear OS** companion for critical alerts, if on-call use justifies it

---

## Before any real clinical use

This app currently holds fictional data and touches no backend. Before it holds real PHI:

- [ ] Security review and a threat model
- [ ] Authentication + session timeout + re-auth on resume
- [ ] Biometric or PIN lock actually enforced
- [ ] Certificate pinning on the API
- [ ] Confirm no PHI reaches logs, crash reports or analytics
- [ ] R8/minification enabled and the minified build tested
- [ ] Real signing config, keystore stored outside the repo
- [ ] Developer simulation switches removed from release builds
- [ ] Regulatory review — depending on jurisdiction and claims, a diagnostic-adjacent app may be a
      regulated medical device
- [ ] Accessibility audit with real assistive tech, not just lint
- [ ] Penetration test of the API

---

Back to the [index](README.md).
