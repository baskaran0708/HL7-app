# 03 — Project structure

Where everything lives. Use this when you're hunting for a file.

- [Repo root](#repo-root)
- [Source tree](#source-tree)
- [Naming conventions](#naming-conventions)
- [Where do I put a new file?](#where-do-i-put-a-new-file)

---

## Repo root

```
D:\Live_Medica\HL7app\
├── app/
│   ├── build.gradle.kts              module build config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/livemedica/helix/    ← all source (see below)
│       │   ├── keepRules/rules.keep          R8 keep rules (AGP 9 replaces proguard-rules.pro)
│       │   └── res/
│       │       ├── font/                     Inter + JetBrains Mono, 8 TTFs
│       │       ├── drawable/  mipmap-*/       launcher icon
│       │       └── values/                   strings, colors, themes
│       ├── test/                             JVM unit tests
│       └── androidTest/                      instrumented tests
├── build.gradle.kts                  root build config
├── settings.gradle.kts               rootProject.name = "LmiHL7"
├── gradle/
│   ├── libs.versions.toml            THE version catalog — all deps declared here
│   ├── gradle-daemon-jvm.properties  pins the Gradle daemon to JDK 25
│   └── wrapper/                      Gradle 9.6.0
├── gradle.properties
├── local.properties                  sdk.dir — machine-specific, not committed
├── build-output/                     copied, human-named APKs
└── doc/                              ← you are here
```

---

## Source tree

`app/src/main/java/com/livemedica/helix/`

```
├── HelixApplication.kt        @HiltAndroidApp — the DI root
├── MainActivity.kt            single activity, edge-to-edge, attaches the theme
├── HelixApp.kt                app shell: bottom bar / rail + nav host
├── HelixAppViewModel.kt       shell state: theme preference, Results tab badge
│
├── core/
│   ├── common/
│   │   ├── AppResult.kt               Success / Failure / Offline
│   │   ├── AppDispatchers.kt          injectable dispatchers
│   │   └── di/DispatcherModule.kt
│   ├── designsystem/
│   │   ├── token/Tokens.kt            Spacing, Radius, Motion, Elevation, Dimens
│   │   ├── theme/
│   │   │   ├── HelixTheme.kt          the theme entry point + CompositionLocals
│   │   │   ├── HelixColors.kt         semantic colour tokens + HelixPalette
│   │   │   ├── HelixTypography.kt     the type scale + font families
│   │   │   └── ClinicalColorMeta.kt   modality / priority / status → colour + label
│   │   └── component/                 the shared Helix* library (12 files)
│   │       ├── HelixPrimitives.kt         card, hairline, overline, section header, dot, tag, status chip
│   │       ├── HelixClinicalComponents.kt modality tag/block, priority badge, avatar, metric
│   │       ├── HelixTopBar.kt             detail-screen top bar
│   │       ├── HelixFilterChip.kt         the one filter chip, used by 4 screens
│   │       ├── HelixSearchBar.kt          the one search field
│   │       ├── HelixSegmentedControl.kt   Day/Week/List style switcher
│   │       ├── HelixMenuRow.kt            More / Settings list rows
│   │       ├── HelixDetailComponents.kt   detail sections and rows
│   │       ├── HelixFields.kt             fact grids and mini fields
│   │       ├── HelixActions.kt            primary / secondary action buttons
│   │       ├── HelixBottomNavigation.kt   phone tab bar
│   │       └── HelixNavigationRail.kt     tablet rail
│   ├── navigation/
│   │   ├── HelixRoute.kt              @Serializable routes + deep-link patterns
│   │   ├── HelixNavHost.kt            the graph
│   │   ├── HelixNavActions.kt         what screens are allowed to navigate to
│   │   └── TopLevelDestination.kt     the five tabs
│   ├── network/                       Phase 2 plumbing — interfaces and stubs only
│   │   ├── ApiRoutes.kt               every endpoint path, in one place
│   │   ├── HelixApiService.kt → see data/remote/
│   │   ├── AuthInterceptor.kt         Bearer token attachment (not installed)
│   │   ├── TokenProvider.kt           + InMemoryTokenProvider stub
│   │   ├── NetworkMonitor.kt          + SettingsAwareNetworkMonitor
│   │   └── HelixRealtimeEvent.kt      WebSocket/FCM event types
│   ├── ui/
│   │   ├── UiState.kt                 Loading / Success / Empty / Error / Offline
│   │   ├── UiStateMapping.kt          AppResult → UiState
│   │   └── HelixStateHost.kt          renders every non-success state + skeletons
│   └── utils/ClinicalFormat.kt        times, dates, "76F", identifier masking
│
├── domain/                            pure Kotlin — no Android imports
│   ├── model/
│   │   ├── ClinicalModels.kt          Patient, Order, Report, Appointment, Study, …
│   │   ├── ClinicalEnums.kt           Modality, Priority, statuses, HL7 types
│   │   └── HelixSettings.kt           ThemePreference + settings model
│   └── repository/
│       ├── Repositories.kt            the nine data contracts
│       └── SettingsRepository.kt
│
├── data/
│   ├── mock/
│   │   ├── SeedClinicalData.kt        ★ ALL demo data lives here, and only here
│   │   ├── MockClinicalStore.kt       the shared StateFlow store
│   │   └── MockCallSimulator.kt       latency + offline/error injection
│   ├── repository/
│   │   ├── MockDirectoryRepositories.kt   doctor, appointments, patients
│   │   ├── MockWorkflowRepositories.kt    orders, results, today dashboard
│   │   ├── MockSupportRepositories.kt     notifications, integration, search
│   │   └── SettingsRepositoryImpl.kt
│   ├── local/HelixPreferencesDataSource.kt   DataStore — preferences only
│   ├── remote/HelixApiService.kt      ★ the future API contract (not wired)
│   ├── model/Dtos.kt                  @Serializable wire models
│   └── di/RepositoryModule.kt         ★ THE swap point — mock ⇄ api
│
└── feature/                           one package per screen area
    ├── today/          schedule/     worklist/    results/
    ├── report/         patient/      study/       notifications/
    ├── search/         more/         integration/ profile/       settings/
```

★ = the three files that matter most when you're doing the backend migration.

---

## Naming conventions

| Thing | Pattern | Example |
|---|---|---|
| Screen | `XxxScreen.kt` → `fun XxxScreen(actions, viewModel)` | `WorklistScreen.kt` |
| ViewModel | `XxxViewModel.kt` → `@HiltViewModel class XxxViewModel` | `WorklistViewModel.kt` |
| State model | `XxxUiState.kt` (only when it's non-trivial) | `ScheduleUiState.kt` |
| Screen-local components | `feature/xxx/components/` | `WorklistComponents.kt` |
| Shared components | `core/designsystem/component/Helix*.kt` | `HelixFilterChip.kt` |
| Repository interface | `domain/repository/` | `OrderRepository` |
| Mock implementation | `data/repository/Mock*` | `MockOrderRepository` |
| Future API impl | `data/remote/Api*` | `ApiOrderRepository` |

Everything shared is prefixed `Helix`. If it's not prefixed, it belongs to one feature.

---

## Where do I put a new file?

Ask in this order:

1. **Is it a data model the whole app uses?** → `domain/model/`
2. **Is it a data contract?** → `domain/repository/` (interface) + `data/repository/` (impl)
3. **Is it a UI component more than one screen uses?** → `core/designsystem/component/`, named `Helix*`
4. **Is it a UI component one screen uses?** → `feature/<screen>/components/`
5. **Is it a whole new screen?** → new `feature/<name>/` package. Follow the recipe in
   [05-screens.md](05-screens.md).
6. **Is it a formatting or date helper?** → `core/utils/ClinicalFormat.kt`

**File size rule:** aim to keep files under ~250 lines. When a screen grows past that, pull the
sections into `components/`. The exception is `SeedClinicalData.kt`, which is a data file and is
long on purpose.

---

Next: [04-design-system.md](04-design-system.md)
