# 02 — Architecture

How the layers fit together, and why each boundary exists.

- [The shape](#the-shape)
- [Layer by layer](#layer-by-layer)
- [The one boundary that matters](#the-one-boundary-that-matters)
- [How a screen actually loads](#how-a-screen-actually-loads)
- [How a write propagates](#how-a-write-propagates)
- [Dependency injection](#dependency-injection)
- [Threading](#threading)
- [Why single-module](#why-single-module)

---

## The shape

```
┌──────────────────────────────────────────────────────────┐
│  UI            feature/*/XxxScreen.kt                     │
│                core/designsystem/component/*              │
│                @Composable, no logic, no data imports     │
└───────────────┬──────────────────────────────────────────┘
                │ collectAsStateWithLifecycle()
┌───────────────▼──────────────────────────────────────────┐
│  ViewModel     feature/*/XxxViewModel.kt                  │
│                @HiltViewModel, StateFlow<UiState<T>>      │
│                filtering, sorting, combining, debounce    │
└───────────────┬──────────────────────────────────────────┘
                │ suspend fun / Flow
┌───────────────▼──────────────────────────────────────────┐
│  Repository    domain/repository/Repositories.kt          │
│  (interface)   THE contract. Pure Kotlin. No Android.     │
└───────────────┬──────────────────────────────────────────┘
                │ bound in data/di/RepositoryModule.kt
     ┌──────────┴───────────┐
     ▼                      ▼
┌──────────────┐   ┌─────────────────────┐
│ Mock*Repo    │   │ Api*Repo (Phase 2)  │
│ data/repo/   │   │ data/remote/        │
└──────┬───────┘   └──────────┬──────────┘
       ▼                      ▼
┌──────────────┐   ┌─────────────────────┐
│MockClinical  │   │ Retrofit → AWS API  │
│Store         │   │ (+ Room cache)      │
└──────────────┘   └─────────────────────┘
```

---

## Layer by layer

### `core/` — things every feature uses

| Package | Holds |
|---|---|
| `core/designsystem/` | tokens, theme, the `Helix*` component library |
| `core/navigation/` | routes, nav host, nav actions, deep links |
| `core/ui/` | `UiState`, `HelixStateHost`, skeletons, empty/error states |
| `core/network/` | API route constants, auth interceptor, token provider, network monitor, realtime event types — **interfaces and stubs only in Phase 1** |
| `core/common/` | `AppResult`, `AppDispatchers` |
| `core/utils/` | `ClinicalFormat` — dates, `76F`, identifier masking |

### `domain/` — the business vocabulary

Pure Kotlin. No Android imports, no Compose, no Retrofit. This is deliberate: it means the models
and contracts survive any change of framework underneath them.

| Package | Holds |
|---|---|
| `domain/model/` | `Patient`, `Order`, `RadiologyReport`, `Appointment`, `Study`, `HelixNotification`, enums |
| `domain/repository/` | the nine repository interfaces |
| `domain/usecase/` | reserved; most logic currently lives in ViewModels and is simple enough to stay there |

### `data/` — where data actually comes from

| Package | Holds |
|---|---|
| `data/mock/` | `MockClinicalStore` (the shared store), `SeedClinicalData` (the demo dataset), `MockCallSimulator` (latency + offline/error injection) |
| `data/repository/` | the `Mock*Repository` implementations |
| `data/local/` | `HelixPreferencesDataSource` — DataStore for user preferences only |
| `data/remote/` | `HelixApiService` — the Retrofit contract, **declared not wired** |
| `data/model/` | `@Serializable` DTOs for the future API |
| `data/di/` | `RepositoryModule` — the swap point |

### `feature/` — one package per screen area

`today`, `schedule`, `worklist`, `results`, `report`, `patient`, `study`, `notifications`,
`search`, `more`, `integration`, `profile`, `settings`.

Each contains `XxxScreen.kt`, `XxxViewModel.kt`, sometimes `XxxUiState.kt`, and a `components/`
subpackage for anything screen-specific.

---

## The one boundary that matters

**The UI must not know where data comes from.**

Concretely: no file under `feature/` should import anything from `data/`. Screens and ViewModels
depend only on `domain/repository/` interfaces and `domain/model/` types.

This is what makes the Phase 2 migration a one-file change. When the AWS API is ready you write
`ApiResultRepository`, flip one `@Binds` in `data/di/RepositoryModule.kt`, and every screen keeps
working without being touched.

```kotlin
// data/di/RepositoryModule.kt — today
@Binds @Singleton
abstract fun bindResultRepository(impl: MockResultRepository): ResultRepository

// tomorrow, and this is the entire change
@Binds @Singleton
abstract fun bindResultRepository(impl: ApiResultRepository): ResultRepository
```

If you ever find yourself wanting to "just quickly" reach into `MockClinicalStore` from a screen,
that's the smell this boundary exists to catch.

---

## How a screen actually loads

Take Today. Trace it end to end:

1. `MainActivity` sets up the theme and `HelixApp`.
2. `HelixNavHost` routes to `TodayScreen(actions)`.
3. `TodayScreen` gets `TodayViewModel` via `hiltViewModel()`.
4. The ViewModel's `init` calls `load()`, which sets `UiState.Loading` then calls
   `todayRepository.getDashboard()`.
5. `MockTodayRepository` asks `MockCallSimulator` to run the call. The simulator waits ~320 ms
   (so the skeleton is actually visible), checks the developer offline/error switches, then reads
   from `MockClinicalStore`.
6. Result comes back as `AppResult.Success` / `Offline` / `Failure`.
7. The ViewModel maps it to `UiState` and emits on its `StateFlow`.
8. `TodayScreen` collects with `collectAsStateWithLifecycle()` and hands the state to
   `HelixStateHost`, which renders a skeleton, the content, an empty state, an error, or an
   offline banner + cached content.

Every screen follows this shape. If one doesn't, it's wrong.

---

## How a write propagates

This is the interesting part, and the reason there's a *single* shared store.

Signing a report has to update four separate screens at once. It does, because they're all
observing the same `StateFlow`:

```
ReportViewModel.signReport()
    → ResultRepository.signReport(id)
        → MockClinicalStore.signReport(id)        ← mutates the one _reports StateFlow
                    │
    ┌───────────────┼────────────────┬──────────────────┐
    ▼               ▼                ▼                  ▼
ResultsViewModel  TodayViewModel  HelixAppViewModel  PatientViewModel
(list + counts)   (counters,      (Results tab       (patient's
                   critical card)  badge)             reports)
```

No event bus, no manual refresh, no `notifyDataSetChanged`. One mutation, four screens.

`signReport` is also **idempotent** — signing an already-signed report returns it unchanged rather
than stamping a second signature. That mirrors what the backend will enforce, and it means a double
tap can't create a second signature event.

---

## Dependency injection

Hilt, with KSP.

| Where | What |
|---|---|
| `HelixApplication` | `@HiltAndroidApp` |
| `MainActivity` | `@AndroidEntryPoint` |
| ViewModels | `@HiltViewModel` + `@Inject constructor` |
| Screens | `hiltViewModel()` |
| Bindings | `data/di/RepositoryModule.kt`, `core/common/di/DispatcherModule.kt` |

All repositories and the store are `@Singleton` — that's load-bearing. If `MockClinicalStore` were
not a singleton, each screen would get its own copy and the propagation above would silently stop
working.

> **Build note:** `gradle.properties` sets `android.disallowKotlinSourceSets=false`. AGP 9 rejects
> the way KSP registers its generated sources, and this is the documented escape hatch. Without it
> Hilt codegen fails at configuration time. Remove it once KSP adopts the AGP 9 source API.

---

## Threading

- Repositories are `suspend`; they don't block.
- ViewModels launch in `viewModelScope`.
- `AppDispatchers` is injected rather than referencing `Dispatchers` directly, so tests can
  substitute a test dispatcher.
- Compose collects with `collectAsStateWithLifecycle()`, so collection stops when the screen isn't
  visible.

---

## Why single-module

One `:app` module, structured by package. Considered and rejected: a multi-module Gradle setup
(`:core:designsystem`, `:feature:today`, …).

**Why:** ~20 extra build files and a lot of setup before a single screen exists, in exchange for
build parallelism this project doesn't yet need. The package structure already mirrors what the
module structure would be, so splitting later is moving files, not rearchitecting.

**When to revisit:** if build times become painful, or if a second app needs to share the design
system.

---

Next: [03-project-structure.md](03-project-structure.md)
