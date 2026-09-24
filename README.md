# LmiHL7

A doctor-facing **radiology workflow app** for Android — built for radiologists to triage their
queue, work a worklist, and sign reports from a phone.

Kotlin · Jetpack Compose · Material 3 · Hilt · Navigation Compose

---

## Status — Phase 1 (frontend)

Every screen is built and interactive. Data currently comes from an in-memory demo dataset rather
than a server — deliberately, so the workflow can be reviewed before the backend exists.

The data layer is built around a single seam: swapping the demo store for the real API is a change
to the bindings in one file, with **no change to any screen, ViewModel or navigation code**.

| | |
|---|---|
| Package | `com.livemedica.helix` |
| Min / target / compile SDK | 26 / 37 / 37 |
| Architecture | UI → ViewModel → Repository → Data source |
| Screens | 17, all functional |
| Themes | Dark + light, from one token set |
| Tests | JVM unit tests on repositories, search and the sign flow |

---

## Screens

**Today** · **Schedule** · **Worklist** · **Results** · **More**, plus report, patient, study,
order and appointment detail, notifications, global search, HL7 integration, profile and settings.

Highlights:

- **Today** — critical-result alert, workload counters, up-next exam, reports awaiting sign-off,
  recent activity timeline
- **Worklist** — orders/results tabs, priority and status filters, search across patient, MRN,
  accession and CPT
- **Report detail** — full radiology report with a confirmation-gated **sign** flow; signing
  propagates live to the results list, the Today counters and the tab badge
- **HL7 Integration** — operational readout of message volume, ACK rate, and Mirth channel health

---

## Architecture

```
Hospital HIS/RIS → HL7 v2 (ADT/SIU/ORM/ORU) → Mirth Connect → PostgreSQL → AWS REST API → this app
```

The app talks **only** to the HTTPS API. It never connects to PostgreSQL or Mirth directly, and
ships no database credentials.

```
UI (Compose)  →  ViewModel (StateFlow<UiState>)  →  Repository (interface)  →  Data source
                                                          │
                                        ┌─────────────────┴─────────────────┐
                                   MockRepository                     ApiRepository
                                   (today)                            (Phase 2)
```

---

## Docs

Full developer documentation is in [`doc/`](doc/).

| | |
|---|---|
| [Overview](doc/01-overview.md) | what the product is, and where HL7 fits |
| [Architecture](doc/02-architecture.md) | layers, data flow, why each boundary exists |
| [Project structure](doc/03-project-structure.md) | where every file lives |
| [Design system](doc/04-design-system.md) | tokens, theming, component catalogue |
| [Screens](doc/05-screens.md) | every screen + recipe for adding one |
| [Data layer](doc/06-data-layer.md) | models, repositories, and the demo data |
| [State management](doc/07-state-management.md) | `UiState`, ViewModels, loading/error/offline |
| [Navigation](doc/08-navigation.md) | routes, deep links, back stack |
| [Backend integration](doc/09-backend-integration.md) | **the Phase 2 migration guide** |
| [Build and run](doc/10-build-and-run.md) | setup, emulator, APKs |
| [Conventions](doc/11-conventions.md) | how to write code that fits |
| [Roadmap](doc/12-roadmap.md) | what's done, what's deliberately not |

---

## Build

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:lintDebug            # lint
```

Requires JDK 17+ (the Gradle daemon is pinned to JDK 25 — Android Studio's bundled JBR works) and
an Android SDK with platform 37. See [doc/10-build-and-run.md](doc/10-build-and-run.md).

---

## A note on the data

All patients, reports, orders and findings in this repository are **fictional demo data** used to
exercise the UI. They describe no real person and are not medical advice.

---

## Licence

Proprietary — Live Medica.
