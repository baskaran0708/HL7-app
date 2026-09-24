# LmiHL7 — developer docs

Everything you need to understand, run, change and eventually back-end this app.

**Start here, then jump to whatever you're actually doing.**

---

## What this is

LmiHL7 is a doctor-facing radiology workflow app for Android. A radiologist opens it to see what
needs reading right now, work a worklist, and sign reports.

Right now it is **frontend only**. Every screen is real and interactive, but the data comes from an
in-memory demo dataset rather than a server. That is deliberate — the app is being shown to
stakeholders before the backend exists, and the data layer is built so that swapping the demo store
for the real API is a change to *one file of bindings*, not a rewrite.

| | |
|---|---|
| App name | **LmiHL7** |
| Package | `com.livemedica.helix` |
| Internal codename | Helix (you'll see it all over the source — same thing) |
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Min / target / compile SDK | 26 / 37 / 37 |
| Architecture | UI → ViewModel → Repository → Data source |
| DI | Hilt (+ KSP) |
| Data today | In-memory mock store |
| Data tomorrow | AWS REST API → PostgreSQL (fed by Mirth from HL7) |

---

## The docs

| # | Doc | Read it when… |
|---|---|---|
| 01 | [Overview](01-overview.md) | You're new. What the product is, who uses it, where HL7 fits. |
| 02 | [Architecture](02-architecture.md) | You want to know how the layers fit together and why. |
| 03 | [Project structure](03-project-structure.md) | You're looking for a file. |
| 04 | [Design system](04-design-system.md) | You're writing UI, or the design changed. |
| 05 | [Screens](05-screens.md) | You're changing a specific screen, or adding one. |
| 06 | [Data layer](06-data-layer.md) | You're touching models, repositories, or the **demo data**. |
| 07 | [State management](07-state-management.md) | You're writing a ViewModel or handling loading/error states. |
| 08 | [Navigation](08-navigation.md) | You're adding a route, a deep link, or fixing the back stack. |
| 09 | [Backend integration](09-backend-integration.md) | **You're wiring the real API.** The migration guide. |
| 10 | [Build and run](10-build-and-run.md) | You're setting up, building an APK, or running the emulator. |
| 11 | [Conventions](11-conventions.md) | You want your PR to look like the rest of the codebase. |
| 12 | [Roadmap and gaps](12-roadmap.md) | You want to know what's done and what's deliberately not. |

---

## The 60-second version

```
UI (Compose)                 screens under feature/, components under core/designsystem/
   ↓  collects StateFlow
ViewModel                    @HiltViewModel, exposes UiState<T>
   ↓  calls suspend fns
Repository (interface)       domain/repository/ — the ONLY data contract the UI knows
   ↓  bound in data/di/RepositoryModule.kt
MockXxxRepository            data/repository/ — today
ApiXxxRepository             data/remote/    — tomorrow, same interface
   ↓
MockClinicalStore            one shared StateFlow store, seeded by SeedClinicalData.kt
```

The whole design of this app is that arrow from `MockXxxRepository` to `ApiXxxRepository`. Nothing
above the repository line knows or cares which one is live.

---

## Three rules that keep this codebase sane

1. **Never hard-code a colour, size or font size in a screen.** Use `HelixTheme.colors`,
   `HelixTheme.typography`, `Spacing`, `Radius`, `Dimens`. See [04](04-design-system.md).
2. **No business logic in a composable.** Screens render state and emit events; ViewModels decide.
   See [07](07-state-management.md).
3. **The UI never learns where data came from.** If you find yourself importing anything from
   `data/` into a screen, stop. See [02](02-architecture.md).

---

## Fast answers

| I want to… | Go to |
|---|---|
| Change the demo patients / reports | `data/mock/SeedClinicalData.kt` — [06](06-data-layer.md) |
| Change a colour or the whole palette | `core/designsystem/theme/HelixColors.kt` — [04](04-design-system.md) |
| Add a screen | recipe in [05](05-screens.md) |
| Add an API call | [09](09-backend-integration.md) |
| Build a release APK | [10](10-build-and-run.md) |
| See the offline / error states | Settings → Developer → Simulate offline / Simulate error |
