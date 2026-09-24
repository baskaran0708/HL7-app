# 07 — State management

How every screen handles loading, data, emptiness, errors and being offline — the same way.

- [UiState](#uistate)
- [The five states, and what each means](#the-five-states-and-what-each-means)
- [HelixStateHost](#helixstatehost)
- [Writing a ViewModel](#writing-a-viewmodel)
- [Keeping a screen live](#keeping-a-screen-live)
- [Seeing the states yourself](#seeing-the-states-yourself)
- [Rules](#rules)

---

## UiState

`core/ui/UiState.kt`

```kotlin
@Immutable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String? = null) : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Offline<T>(val cached: T? = null) : UiState<T>
}
```

Every screen's ViewModel exposes exactly this. No screen invents its own
`isLoading` / `errorMessage` / `items` triple.

It maps from `AppResult` (the repository's return type — see
[06-data-layer.md](06-data-layer.md)) through one shared helper:

```kotlin
// core/ui/UiStateMapping.kt
fun <T> AppResult<List<T>>.toListUiState(emptyMessage: String): UiState<List<T>>
```

Centralising this is what stops "empty" meaning something slightly different on every screen.

---

## The five states, and what each means

| State | When | What the user sees |
|---|---|---|
| `Loading` | first load, before data arrives | a skeleton shaped like the content |
| `Success(data)` | data arrived, non-empty | the screen |
| `Empty(msg)` | the call succeeded, there is genuinely nothing | a specific message: *"No pending orders"* |
| `Error(msg)` | the call failed | heading + reason + **Try again** |
| `Offline(cached)` | no connectivity | amber banner + **the cached data, still readable** |

Two of these are worth dwelling on, because they're where most apps get it wrong.

**`Empty` is not `Success(emptyList())`.** They look the same to a `LazyColumn` and completely
different to a person. An empty worklist should say "No pending orders", not render a blank
surface. So the distinction lives in the type.

**`Offline` is not `Error`.** A radiologist who loses signal in a lift should still see the worklist
they already had — clearly marked as stale, not replaced by an error screen. That's why `Offline`
carries `cached: T?` and `HelixStateHost` renders a banner *above* the content rather than instead
of it. Only when there's no cache at all does it fall back to a full-screen message.

---

## HelixStateHost

`core/ui/HelixStateHost.kt`. Every screen wraps its content in this:

```kotlin
@Composable
fun TodayScreen(actions: HelixNavActions, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
        HelixStateHost(
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Nothing scheduled",
            emptyBody = "You have no exams or reports today.",
        ) { model ->
            TodayContent(model = model, actions = actions)
        }
    }
}
```

You supply the success content and the empty copy. Everything else — skeleton, error layout, retry
button, offline banner — comes for free and looks identical app-wide.

**Write real empty copy.** `emptyTitle = "Nothing here"` is the default and is almost always the
wrong answer. Say what's missing and, if useful, what to do about it.

### Skeletons

`HelixListSkeleton` is the default loading state — a slow opacity pulse, not a shimmer sweep. That's
a deliberate design decision: motion in this app signals interaction, and a sweeping shimmer across
a dense clinical list is tiring to read past. Pass your own `loading = { … }` if a screen needs a
differently-shaped skeleton.

---

## Writing a ViewModel

The standard shape:

```kotlin
@HiltViewModel
class WorklistViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Order>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Order>>> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init { load() }

    fun refresh() {
        _isRefreshing.value = true
        load(showLoading = false)
    }

    private fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _state.value = UiState.Loading
            _state.value = orderRepository
                .getOrders(priority, status, query)
                .toListUiState("No pending orders")
            _isRefreshing.value = false
        }
    }
}
```

Notes:

- `refresh()` passes `showLoading = false` so pull-to-refresh doesn't blank the list out from under
  the user's thumb.
- Filtering and sorting happen **here**, not in the composable.
- Debounce user input in the ViewModel (search fields use ~250 ms).

---

## Keeping a screen live

One-shot loads aren't enough when another screen can change your data. Today, for example, has to
update its counters when a report is signed elsewhere. So it also observes:

```kotlin
private fun observeLiveUpdates() {
    viewModelScope.launch {
        combine(
            todayRepository.observeDashboard(),
            resultRepository.observeReports(),
            notificationRepository.observeNotifications(),
        ) { dashboard, reports, notifications ->
            TodayUiModel(dashboard, reports.filterNot { it.isSigned }, notifications.take(4))
        }.collect { model ->
            // Only replace a state that is already showing data, so a live update
            // can't silently cancel a visible loading, error or offline state.
            if (_state.value is UiState.Success) _state.value = UiState.Success(model)
        }
    }
}
```

That guard matters. Without it, a background emission would punch through an error screen the user
is currently looking at and replace it with stale content.

For state derived purely from a flow, prefer `stateIn`:

```kotlin
val unsignedReportCount: StateFlow<Int> = resultRepository.observeReports()
    .map { reports -> reports.count { !it.isSigned } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
```

`WhileSubscribed(5_000)` keeps the flow alive across a config change without leaking it when the
screen genuinely goes away.

---

## Seeing the states yourself

You don't need a backend to review error and offline handling:

**Settings → Developer → Simulate offline / Simulate error.**

Those switches are read by `MockCallSimulator` on every repository call, so every screen in the app
flips at once. `SettingsAwareNetworkMonitor` also ANDs the offline switch with real connectivity.

The section is labelled *"Not for clinical use"* in the UI. It should be gated to debug builds
before any real release — see [12-roadmap.md](12-roadmap.md).

---

## Rules

1. **Never mutate state from a composable.** Screens call `viewModel.something()`; the ViewModel
   owns `_state`.
2. **One `UiState` per screen.** If you need several independent loading regions, model that
   inside your success type, not with multiple state flows.
3. **Always collect with `collectAsStateWithLifecycle()`**, never `collectAsState()` — the latter
   keeps collecting while the screen is in the background.
4. **Specific empty copy, always.**
5. **Errors need a way out.** Pass `onRetry`.
6. **Don't reach past the repository.** If a ViewModel imports from `data/`, it's wrong.

---

Next: [08-navigation.md](08-navigation.md)
