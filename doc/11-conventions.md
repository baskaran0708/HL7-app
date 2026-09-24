# 11 — Conventions

How to write code that looks like the rest of this codebase.

- [The short version](#the-short-version)
- [Composables](#composables)
- [ViewModels](#viewmodels)
- [Naming](#naming)
- [Comments](#comments)
- [Accessibility](#accessibility)
- [Performance](#performance)
- [Security](#security)
- [Tests](#tests)
- [Before you open a PR](#before-you-open-a-pr)

---

## The short version

1. No hard-coded colours, dp or sp in a screen — use the tokens.
2. No business logic in a composable.
3. No `data/` imports in `feature/`.
4. Every screen handles all five `UiState` cases.
5. Status and priority always show **text** next to colour.
6. Comments explain **why**, not what.
7. Files under ~250 lines.

---

## Composables

**Signature order.** Required params, then `modifier: Modifier = Modifier`, then optional params,
then trailing lambdas.

```kotlin
@Composable
fun HelixFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    tone: Color? = null,
)
```

`modifier` is always the first optional parameter and is always applied to the outermost layout.

**Screens take `HelixNavActions`, never a `NavController`:**

```kotlin
@Composable
fun WorklistScreen(
    actions: HelixNavActions,
    viewModel: WorklistViewModel = hiltViewModel(),
)
```

**Stateless where possible.** A component should take state and emit events. Hoist anything a
parent might need to control.

**Theme access:**

```kotlin
val colors = HelixTheme.colors
Text(text = label, style = HelixTheme.typography.label, color = colors.textDim)
```

Never `Color(0xFF…)`, never `14.sp`, never `16.dp` inside a screen. If a value you need isn't in
the tokens, add it to the tokens — see [04-design-system.md](04-design-system.md).

---

## ViewModels

```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val someRepository: SomeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<XxxModel>>(UiState.Loading)
    val state: StateFlow<UiState<XxxModel>> = _state.asStateFlow()

    init { load() }

    fun refresh() { … }
    private fun load() { viewModelScope.launch { … } }
}
```

- Constructor-inject repository **interfaces**, never implementations.
- Expose `StateFlow`, never `MutableStateFlow`. Always `.asStateFlow()`.
- Public functions are the screen's event surface; keep them verbs: `refresh()`,
  `selectPriority(p)`, `signReport()`.
- Filtering, sorting, debouncing, combining — all here.
- Inject `AppDispatchers` rather than touching `Dispatchers` directly.

Full patterns in [07-state-management.md](07-state-management.md).

---

## Naming

| Thing | Convention |
|---|---|
| Shared component | `Helix*` — the prefix means "safe to use anywhere" |
| Feature-local component | no prefix, lives in `feature/x/components/` |
| Repository interface | `XxxRepository` in `domain/repository/` |
| Mock implementation | `MockXxxRepository` |
| Future API implementation | `ApiXxxRepository` |
| Screen | `XxxScreen` |
| ViewModel | `XxxViewModel` |
| Private constants | `SCREAMING_SNAKE` in a `private companion object` or file-private `val` |

Avoid abbreviations except established clinical ones (`MRN`, `CPT`, `STAT`, `ORU`, `PACS`). Those
are the domain's vocabulary — see the glossary in [01-overview.md](01-overview.md).

---

## Comments

**Explain why. The code already says what.**

```kotlin
// Bad — restates the code
// Set the state to loading
_state.value = UiState.Loading

// Good — explains a decision the reader can't infer
// Only replace a state that is already showing data, so a live update can't
// silently cancel a visible loading, error or offline state.
if (_state.value is UiState.Success) _state.value = UiState.Success(model)
```

KDoc every public component and ViewModel with a sentence on what it's for, and a paragraph on any
non-obvious decision. The existing files set the tone — match it.

Worth commenting: clinical reasoning (why STAT sorts first), design rules (why red is never a
surface), deliberate omissions (why there's no signature canvas), and anything that looks like a
bug but isn't.

---

## Accessibility

Not optional in a clinical app.

- **Never colour alone.** Every priority and status renders its label next to its colour.
  `HelixPriorityBadge` always prints "STAT". If you hide a badge for space, its meaning must survive
  in the parent's `contentDescription` — see `ScheduleTimeline.kt` for the pattern.
- **Touch targets ≥ 48dp** (`Dimens.touchTargetMin`).
- **`contentDescription` on every icon-only control.** Decorative icons inside a labelled row take
  `contentDescription = null` so they aren't announced twice.
- **Fold related content into one semantics node** when it reads better as a sentence:
  ```kotlin
  Modifier.clearAndSetSemantics { contentDescription = "Results, 4 unread" }
  ```
- **Type in `sp`**, so the system font-size setting scales the whole app.
- Don't assume a fixed width — test at 200% font scale.

---

## Performance

- `LazyColumn` / `LazyRow` for any list that can grow, always with a stable `key`:
  ```kotlin
  items(orders, key = { it.id }) { order -> OrderCard(order) }
  ```
- Domain models are `@Immutable` data classes — keep them that way so Compose can skip.
- Read `HelixTheme.colors` once at the top of a composable rather than repeatedly inside loops.
- Use `derivedStateOf` for values computed from scroll state.
- Do work in the ViewModel, not in recomposition.

---

## Security

Non-negotiable, and easy to get wrong:

- **No credentials, tokens, API keys or URLs in source.** The base URL will come from a build-config
  field; tokens come from `TokenProvider`.
- **No PHI in logs.** Don't log patient names, MRNs or report text. `ClinicalFormat.maskIdentifier`
  exists in `core/utils/` for when diagnostics need to refer to a record.
- **Never connect to PostgreSQL or Mirth from the app.** Everything goes through the HTTPS API. See
  [09-backend-integration.md](09-backend-integration.md).
- `local.properties`, keystores and `*.properties` secrets are gitignored — keep it that way.

---

## Tests

JVM unit tests live in `app/src/test/`. Current coverage: repository behaviour, search, the sign
flow, and `AppResult` mapping.

```kotlin
@Test
fun `signing is idempotent so a double tap cannot sign twice`() = runTest {
    val unsigned = repository.observeReports().first().first { !it.isSigned }

    val first  = (repository.signReport(unsigned.id) as AppResult.Success).data
    val second = (repository.signReport(unsigned.id) as AppResult.Success).data

    assertEquals(first.signedAt, second.signedAt)
}
```

- Backtick test names that state the behaviour, not the method.
- `runTest` for coroutines.
- `FakeSettingsRepository` (in `app/src/test/…/data/`) lets you drive the offline/error simulation.
- Test behaviour through the repository interface, not the mock internals.

Run: `./gradlew :app:testDebugUnitTest`

---

## Before you open a PR

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

All three green. Then check:

- [ ] No hard-coded colours, dp or sp in screens
- [ ] All five `UiState` cases handled, with specific empty copy
- [ ] Status/priority carry text, not just colour
- [ ] Icon-only buttons have `contentDescription`
- [ ] Lists have stable keys
- [ ] No `data/` imports under `feature/`
- [ ] Files under ~250 lines
- [ ] No secrets, no PHI in logs
- [ ] Tried it in **both** themes
- [ ] Tried it with Simulate offline and Simulate error on

---

Next: [12-roadmap.md](12-roadmap.md)
