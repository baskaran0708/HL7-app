# 08 — Navigation

Type-safe routes, per-tab back stacks, and deep links.

- [How routes work](#how-routes-work)
- [The graph](#the-graph)
- [Tabs and back stacks](#tabs-and-back-stacks)
- [How screens navigate](#how-screens-navigate)
- [Reading arguments](#reading-arguments)
- [Deep links](#deep-links)
- [Adding a destination](#adding-a-destination)

---

## How routes work

Routes are `@Serializable` Kotlin objects, not strings. `core/navigation/HelixRoute.kt`:

```kotlin
sealed interface HelixRoute {

    // Tab graphs
    @Serializable data object TodayGraph : HelixRoute
    @Serializable data object ScheduleGraph : HelixRoute
    // …

    // Tab roots
    @Serializable data object Today : HelixRoute
    // …

    // Details — carry their arguments in the type
    @Serializable data class Patient(val patientId: String) : HelixRoute
    @Serializable data class Order(val orderId: String) : HelixRoute
    @Serializable data class Study(val studyId: String) : HelixRoute
    @Serializable data class Report(val reportId: String) : HelixRoute
    @Serializable data class Appointment(val appointmentId: String) : HelixRoute

    // Global
    @Serializable data object Notifications : HelixRoute
    @Serializable data object Search : HelixRoute
    @Serializable data object Integration : HelixRoute
    @Serializable data object Profile : HelixRoute
    @Serializable data object Settings : HelixRoute
}
```

**Why this and not string routes:** the compiler checks them. You cannot navigate to the report
screen without giving it a report id, you cannot typo an argument name, and renaming an argument is
a refactor instead of a runtime crash three screens deep.

This needs the kotlinx-serialization plugin, which is already applied in `app/build.gradle.kts`.

---

## The graph

`core/navigation/HelixNavHost.kt`:

```kotlin
NavHost(navController = navController, startDestination = HelixRoute.TodayGraph) {

    navigation<HelixRoute.TodayGraph>(startDestination = HelixRoute.Today) {
        composable<HelixRoute.Today> { TodayScreen(actions) }
    }
    // … one nested graph per tab

    detailDestinations(actions)   // patient / order / study / report / appointment
    globalDestinations(actions)   // notifications / search / integration / profile / settings
}
```

Two structural decisions worth knowing:

**Each tab is a nested graph.** That's what gives every tab its own back stack.

**Detail destinations are declared once, at the host level — not inside a tab.** A report opened
from Today and the same report opened from Results are the *same* destination. Duplicating them per
tab would mean two entries for one screen and a back stack that lies.

---

## Tabs and back stacks

`core/navigation/TopLevelDestination.kt` defines the five tabs (label, icon, graph, root, and
whether they carry a badge). `HelixApp.kt` renders them — as a bottom bar on phones, a rail from
medium width up — and both read from the same enum, so they can't drift apart.

Switching tabs uses:

```kotlin
// core/navigation/HelixNavActions.kt
fun NavOptionsBuilder.topLevelTabOptions(navController: NavHostController) {
    popUpTo(navController.graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

`saveState` / `restoreState` are what make each tab remember where you were. Scroll down the
Worklist, switch to Today, come back — you're where you left off.

The tab bar **hides on detail screens**. `HelixApp` only renders it when the current destination is
inside one of the five tab graphs:

```kotlin
val selectedTab = TopLevelDestination.entries.firstOrNull { destination ->
    currentDestination?.isWithin(destination) == true
}
```

That's a design decision: a report or a patient record is a focused reading surface, not tab
content.

---

## How screens navigate

Screens **never** receive a `NavHostController`. They receive `HelixNavActions` — a plain object of
named lambdas:

```kotlin
@Immutable
class HelixNavActions(
    val openPatient: (String) -> Unit,
    val openOrder: (String) -> Unit,
    val openStudy: (String) -> Unit,
    val openReport: (String) -> Unit,
    val openAppointment: (String) -> Unit,
    val openNotifications: () -> Unit,
    val openSearch: () -> Unit,
    val openIntegration: () -> Unit,
    val openProfile: () -> Unit,
    val openSettings: () -> Unit,
    val openSchedule: () -> Unit,
    val openWorklist: () -> Unit,
    val openResults: () -> Unit,
    val navigateBack: () -> Unit,
)
```

Why: a screen can only perform transitions the app explicitly supports, every possible transition is
listed in one readable place, and previews are trivial — `HelixNavActions.Noop` makes every action a
no-op.

There's also a helper for notification routing, so a notification can't be untappable by accident:

```kotlin
fun open(target: NotificationTarget) = when (target) {
    is NotificationTarget.Report          -> openReport(target.reportId)
    is NotificationTarget.OrderDetail     -> openOrder(target.orderId)
    is NotificationTarget.PatientDetail   -> openPatient(target.patientId)
    is NotificationTarget.AppointmentDetail -> openAppointment(target.appointmentId)
    NotificationTarget.Integration        -> openIntegration()
}
```

`NotificationTarget` is a sealed type, so adding a new notification kind forces you to say where it
navigates — the `when` stops compiling otherwise.

---

## Reading arguments

In the ViewModel, from `SavedStateHandle`:

```kotlin
@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultRepository: ResultRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<HelixRoute.Report>()
    private val reportId = route.reportId
    // …
}
```

`toRoute<T>()` comes from `androidx.navigation:navigation-compose`. No manual key strings.

---

## Deep links

Declared now so push notifications and a future PACS hand-off need no rework.

```kotlin
object HelixDeepLinks {
    const val SCHEME = "helix"
    const val PATIENT     = "$SCHEME://patient/{patientId}"
    const val ORDER       = "$SCHEME://order/{orderId}"
    const val STUDY       = "$SCHEME://study/{studyId}"
    const val REPORT      = "$SCHEME://report/{reportId}"
    const val APPOINTMENT = "$SCHEME://appointment/{appointmentId}"
}
```

Attached to the destination:

```kotlin
composable<HelixRoute.Report>(
    deepLinks = listOf(navDeepLink { uriPattern = HelixDeepLinks.REPORT }),
) { ReportDetailScreen(actions) }
```

Test one with adb:

```bash
adb shell am start -a android.intent.action.VIEW -d "helix://report/RPT-91204" com.livemedica.helix
```

> **Not done yet:** the deep links work at the navigation layer, but there's no `<intent-filter>` in
> `AndroidManifest.xml` for the `helix` scheme, so they can't be triggered from outside the app.
> Add that when FCM lands — see [12-roadmap.md](12-roadmap.md).

---

## Adding a destination

1. **Declare the route** in `HelixRoute.kt`:
   ```kotlin
   @Serializable data class Audit(val patientId: String) : HelixRoute
   ```
2. **Register it** in `HelixNavHost.kt` — inside a tab graph if it belongs to a tab, otherwise in
   `detailDestinations` / `globalDestinations`:
   ```kotlin
   composable<HelixRoute.Audit> { AuditScreen(actions) }
   ```
3. **Add the action** in `HelixNavActions.kt` — the constructor property, the `Noop` instance, and
   the wiring in `rememberHelixNavActions`:
   ```kotlin
   openAudit = { navController.navigate(HelixRoute.Audit(it)) },
   ```
4. **Read the argument** in the ViewModel with `savedStateHandle.toRoute<HelixRoute.Audit>()`.
5. **Add a deep link** in `HelixDeepLinks` if anything external should be able to open it.

Forgetting step 3 is the usual mistake — the screen exists but nothing can reach it.

---

Next: [09-backend-integration.md](09-backend-integration.md)
