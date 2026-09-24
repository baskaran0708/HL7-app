# 04 — Design System

> Every colour, size and shared component in LmiHL7, where it lives, and how to change it without touching a screen.

Sibling docs: [`05-screens.md`](05-screens.md) (what each screen does) · [`06-data-layer.md`](06-data-layer.md) (repositories, mock store).

---

## Contents

1. [The five rules](#1-the-five-rules)
2. [Where everything lives](#2-where-everything-lives)
3. [Colour](#3-colour)
4. [Typography](#4-typography)
5. [Spacing, radius, elevation, motion, dimens](#5-spacing-radius-elevation-motion-dimens)
6. [How to theme a composable correctly](#6-how-to-theme-a-composable-correctly)
7. [Component catalogue](#7-component-catalogue)
8. [The four most-used components, in anger](#8-the-four-most-used-components-in-anger)
9. [State components (loading / empty / error / offline)](#9-state-components)
10. [How to restyle the whole app](#10-how-to-restyle-the-whole-app)
11. [How to add a new shared component](#11-how-to-add-a-new-shared-component)

---

## 1. The five rules

These aren't aspirational — they're literally encoded in the token files and components. Break one and the app stops looking like itself.

### Rule 1 — Red is a signal, not a surface

`critical` is only ever used as **text, a dot, or a 3dp accent rule**. Never a card fill. From `HelixColors.kt`:

> The design's governing rule is encoded here: *red is a signal, not a surface*. `critical` is only ever used for text, a dot, or a 3dp accent rule — never as a card background — so a STAT item reads instantly without the screen turning into an alarm wash.

You can see it in `CriticalResultCard` (`feature/today/components/TodayComponents.kt`) — the card background is `colors.surface`, and red appears exactly three times: the left rule, the status dot, the "Review report" CTA.

```kotlin
// The signal rule.
Box(
    Modifier
        .width(Dimens.criticalAccentWidth)   // 3.dp
        .fillMaxHeight()
        .background(colors.critical),
)
```

The only exceptions are *tinted* panels at very low alpha — `CriticalProtocolCallout` uses `critical.copy(alpha = 0.06f)` with a `0.22f` stroke. That's a wash, not a fill.

### Rule 2 — Dark mode uses hairlines where light mode uses shadows

`HelixCard` branches on `colors.isDark`:

```kotlin
val base = modifier
    .fillMaxWidth()
    .then(if (colors.isDark) Modifier else Modifier.shadow(elevation, shape, clip = false))
    .clip(shape)
    .background(colors.surface)
    .then(if (colors.isDark) Modifier.border(Dimens.hairline, colors.hairline, shape) else Modifier)
```

`Elevation` in `token/Tokens.kt` says it outright: *"Elevation is a light-mode device only: the dark theme carries structure with surface ramp and hairlines instead."* Same branch appears in `HelixSegmentedControl`, `HelixSearchBar`, `OrderCard` and `ReportCard`.

### Rule 3 — Every status shows TEXT next to its colour

`ClinicalMeta` is a `(color, label)` pair, never just a colour:

```kotlin
data class ClinicalMeta(val color: Color, val label: String)
```

`HelixStatusChip` renders both. `HelixTag` renders text. `HelixPriorityBadge` renders the word `STAT`. On the Week grid, STAT is spelled out (`"${day.statCount} STAT"`) rather than shown as a red pip. This is a hard accessibility requirement — colour-blind users and greyscale displays.

### Rule 4 — No dynamic colour, ever

From `HelixTheme.kt`:

> Dynamic colour is deliberately not supported. Helix is a branded clinical product — the palette must be identical on every device, and modality/priority hues carry meaning that a wallpaper-derived scheme would destroy.

### Rule 5 — Glow and movement mean interaction, never decoration

`Motion` tokens are short (140–420ms). The heaviest animation in the app is a **1.5% scale-down on press** (`pressScale` in `TodayComponents.kt`) and a slow opacity pulse on the loading skeleton. There is no shimmer sweep, no hero transition.

---

## 2. Where everything lives

| File | What it owns |
|---|---|
| `core/designsystem/theme/HelixColors.kt` | `HelixColors` token set, `HelixPalette` brand options, `darkHelixColors()` / `lightHelixColors()` |
| `core/designsystem/theme/HelixTypography.kt` | `HelixTypography`, font families, the `helixTypography` scale |
| `core/designsystem/theme/HelixTheme.kt` | `HelixTheme` accessor object, `HelixTheme()` composable, M3 bridging, `helixShapes`, `monoTag()` |
| `core/designsystem/theme/ClinicalColorMeta.kt` | Modality colour thread + all the `*Meta()` status descriptors |
| `core/designsystem/token/Tokens.kt` | `Spacing`, `Radius`, `Motion`, `Elevation`, `Dimens` |
| `core/designsystem/component/*.kt` | 12 files of shared `Helix*` composables |
| `core/ui/HelixStateHost.kt` | Loading / empty / error / offline rendering (not in `designsystem`, but part of the system) |
| `app/src/main/res/font/` | `inter_{regular,medium,semibold,bold}.ttf`, `jetbrains_mono_{regular,medium,semibold,bold}.ttf` |

Everything is a 1:1 port of the approved design source at `D:\D-Download\hl7-app-handoff\hl7-app\project\theme.jsx`. If you're ever unsure what a value "should" be, that file is the origin.

---

## 3. Colour

### 3.1 The token list

`HelixColors` is an `@Immutable data class` with 21 fields. Screens read them via `HelixTheme.colors`.

| Token | What it's for |
|---|---|
| `isDark` | Branch on this for the shadow/hairline and tint-alpha decisions |
| `primary` | Brand. Means "link" and "selected" — deliberately kept **out** of buttons |
| `primaryDim` | Darker brand, used for M3 `primaryContainer` in dark / `secondary` in light |
| `primaryLift` | Lighter brand, used for M3 `secondary` in dark / `primaryContainer` in light |
| `success` | Signed, completed, online, ACK |
| `warning` | Pending, preliminary, no-show, degraded, allergies, developer warnings |
| `critical` | STAT, critical findings, NAK, offline channels, the "now" line |
| `info` | In progress, arrived, ADT messages, study events |
| `background` | The app canvas. Light mode is a **tinted cool grey** so white cards lift off it |
| `surface` | Card / sheet / bar fill |
| `elevated` | Recessed-but-raised fill: icon tiles, count pills, dark-mode segmented track, indication panel |
| `sunken` | Deepest step: light-mode segmented track, light-mode indication panel |
| `hairline` | 1dp rules and card borders. The default structural stroke |
| `line` | Slightly stronger stroke: unselected chip borders, secondary button outline |
| `lineStrong` | Strongest stroke. Available; barely used |
| `text` | Primary text — and the **primary button fill** (ink inverted) |
| `textDim` | Secondary text, icon tints on rows |
| `textMute` | Tertiary text, timestamps, separators, routine priority |
| `press` | Press overlay |
| `fill` | Neutral low-alpha fill; used by the loading skeleton |
| `scrim` | Modal bottom sheet scrim |

### 3.2 Values

| Token | Dark | Light |
|---|---|---|
| `success` | `#34D399` | `#047857` |
| `warning` | `#FBBF24` | `#8A5406` |
| `critical` | `#F87171` | `#C11C1C` |
| `info` | `#38BDF8` | `#0369A1` |
| `background` | `#0B0A12` | `#EFEEF4` |
| `surface` | `#13121D` | `#FFFFFF` |
| `elevated` | `#191725` | `#F7F6FA` |
| `sunken` | `#08070E` | `#E6E4EE` |
| `hairline` | white @ 5.5% | `#171430` @ 5.5% |
| `line` | white @ 9% | `#171430` @ 9% |
| `lineStrong` | white @ 16% | `#171430` @ 16% |
| `text` | `#F5F4F9` | `#16142B` |
| `textDim` | `#A29EB8` | `#4E4B68` |
| `textMute` | `#6B6783` | `#63607A` |
| `press` | white @ 5% | `#171430` @ 4.5% |
| `fill` | white @ 4.5% | `#171430` @ 3.5% |
| `scrim` | `#04030A` @ 72% | `#171430` @ 38% |

Light-mode signals are **darkened on purpose** — the comment in `lightHelixColors()` is: *"Light-mode signals are darkened so small bold text clears 4.5:1 on white cards."*

### 3.3 The four brand palettes

`HelixPalette` (in `HelixColors.kt`):

| Enum | Label | `primary` | `primaryDim` | `primaryLift` |
|---|---|---|---|---|
| `VIOLET` | Violet | `#8B5CF6` | `#6D28D9` | `#A78BFA` |
| `INDIGO` | Indigo | `#6366F1` | `#4338CA` | `#93A1FC` |
| `TEAL` | Teal | `#14B8A6` | `#0F766E` | `#5EEAD4` |
| `STEEL` | Steel | `#64748B` | `#475569` | `#94A3B8` |

> **Heads up:** the palette parameter exists on `HelixTheme(themePreference, palette, content)` and defaults to `VIOLET`, but **nothing in the app ever passes it**. `MainActivity` calls `HelixTheme(themePreference = settings.themePreference) { ... }`, so the other three palettes are reachable only by editing that call site or adding a preference to `HelixSettings`. Settings currently exposes light/dark/system, not palette.

### 3.4 The modality colour thread

Each modality keeps the same hue everywhere — worklist tag, timeline accent, filter chip, patient prior. Defined in `ClinicalColorMeta.kt` as `private val modalityRamps`.

| Modality | Meaning | Dark | Light |
|---|---|---|---|
| `CT` | Computed Tomography | `#8B5CF6` | `#6D28D9` |
| `MR` | Magnetic Resonance | `#38BDF8` | `#0369A1` |
| `US` | Ultrasound | `#34D399` | `#047857` |
| `XR` | Radiography | `#FBBF24` | `#92400E` |
| `MG` | Mammography | `#F472B6` | `#BE185D` |
| `NM` | Nuclear Medicine | `#A78BFA` | `#6D28D9` |
| `PT` | PET | `#FB923C` | `#C2410C` |
| `FL` | Fluoroscopy | `#38BDF8` | `#0369A1` |

`FL` is an addition on the Kotlin side — `theme.jsx` only shipped seven. It reuses the MR ramp. Anything not in the map falls back to `CT`:

```kotlin
@Composable
@ReadOnlyComposable
fun modalityColor(modality: Modality): Color {
    val colors = HelixTheme.colors
    val ramp = modalityRamps[modality] ?: modalityRamps.getValue(Modality.CT)
    return if (colors.isDark) ramp.dark else ramp.light
}
```

### 3.5 Status descriptors

Six `@Composable @ReadOnlyComposable` functions in `ClinicalColorMeta.kt`, each returning `ClinicalMeta(color, label)`:

| Function | Input | Mapping |
|---|---|---|
| `priorityMeta` | `Priority` | STAT→`critical`/"STAT", URGENT→`warning`/"Urgent", ROUTINE→`textMute`/"Routine" |
| `orderStatusMeta` | `OrderStatus` | PENDING→`warning`/"Pending read", IN_PROGRESS→`info`/"In progress", COMPLETED→`success`/"Completed", CANCELLED→`textMute`/"Cancelled" |
| `reportStatusMeta` | `ReportStatus` | DRAFT→`textMute`/"Draft", PRELIMINARY→`warning`/"Preliminary", PENDING_SIGNATURE→`warning`/"Awaiting sign-off", SIGNED→`success`/"Signed", ADDENDUM→`info`/"Amended" |
| `appointmentStatusMeta` | `AppointmentStatus` | SCHEDULED→`textMute`/"Scheduled", CHECKED_IN→`info`/"Arrived", IN_PROGRESS→`info`/"In progress", COMPLETED→`success`/"Completed", CANCELLED→`textMute`/"Cancelled", NO_SHOW→`warning`/"No-show" |
| `ackStatusMeta` | `Hl7AckStatus` | ACK→`success`, DELIVERED→`info`, PENDING→`warning`, NAK→`critical` |
| `interfaceStateMeta` | `InterfaceState` | ONLINE→`success`, DEGRADED→`warning`, OFFLINE→`critical` |

Note these labels are **sentence case** and differ from the enum's own `label` (which is all-caps, e.g. `OrderStatus.IN_PROGRESS.label == "IN PROGRESS"`). The enum label is the badge/screen-reader form; the meta label is what you render in a `HelixStatusChip`.

There's one more colour thread that doesn't live here — HL7 message types. `hl7TypeColor()` in `feature/integration/components/IntegrationComponents.kt` maps ADT→`info`, SIU→`primary`, ORM→`warning`, ORU→`success`.

### 3.6 Bridging to Material 3

`HelixTheme` layers on top of M3 rather than replacing it, so `ModalBottomSheet`, `Switch`, `AlertDialog`, ripples and text editing all keep working. `HelixColors.toMaterialScheme()` maps the tokens onto `darkColorScheme()` / `lightColorScheme()`, and `toMaterialTypography()` maps the scale onto M3's slots. Shapes:

```kotlin
private val helixShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small      = RoundedCornerShape(Radius.sm),
    medium     = RoundedCornerShape(Radius.md),
    large      = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)
```

`HelixTheme()` also keeps the system bar icon polarity in step via `WindowCompat.getInsetsController(...).isAppearanceLightStatusBars = !darkTheme`.

---

## 4. Typography

### 4.1 The fonts

Bundled as static TTFs in `app/src/main/res/font/` — no downloadable fonts, so the app never waits on a network call to render text.

```kotlin
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val MonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)
```

The design specified Geist / Geist Mono. Inter stands in for Geist (same grotesque proportions, ships as a static TTF); JetBrains Mono covers the mono role.

### 4.2 The scale

| Style | Size | Weight | Tracking (px @ size) | Line height | Family |
|---|---|---|---|---|---|
| `display` | 34sp | Bold | −1.2 | 1.08 | Inter |
| `title` | 22sp | Bold | −0.6 | 1.15 | Inter |
| `headline` | 17sp | Bold | −0.4 | 1.25 | Inter |
| `bodyLarge` | 15sp | Medium | −0.2 | 1.45 | Inter |
| `body` | 14sp | Medium | −0.1 | 1.5 | Inter |
| `label` | 12.5sp | SemiBold | −0.05 | 1.35 | Inter |
| `caption` | 11.5sp | Medium | 0 | 1.4 | Inter |
| `overline` | 10sp | Bold | **+0.9** | 1.2 | Inter |
| `mono` | 12.5sp | Medium | 0 | 1.35 | JetBrains Mono |
| `monoSmall` | 10.5sp | Bold | +0.3 | 1.35 | JetBrains Mono |
| `monoLarge` | 22sp | Bold | −0.6 | 1.35 | JetBrains Mono |

`overline` is the only positively-tracked style — it's the design's signature all-caps section marker, rendered through `HelixOverline`.

### 4.3 Why `sp`, and what tracking does

```kotlin
private fun sans(size: Double, weight: FontWeight, letterSpacingPx: Double, lineHeightMultiplier: Double) =
    TextStyle(
        fontFamily = InterFontFamily,
        fontSize = size.sp,
        fontWeight = weight,
        // theme.jsx expresses tracking in px at the given size; em keeps that ratio under font scaling.
        letterSpacing = (letterSpacingPx / size).em,
        lineHeight = (size * lineHeightMultiplier).sp,
        fontFeatureSettings = TABULAR_NUMERALS,
    )
```

Two things to understand:

- **Sizes stay in `sp`, not `dp`.** The whole scale responds to the system font-size setting. It's a hard requirement — clinicians read dense worklists and many scale their device font up. Never convert a text size to `dp` to "stop it moving".
- **Tracking is stored as `em`, not `sp`.** `theme.jsx` gave tracking in px at a specific size; dividing by the size converts it to a ratio, so the ratio survives font scaling.

### 4.4 `tnum` — tabular figures

```kotlin
/**
 * Tabular figures. Applied to every time, count, MRN and accession number so digits sit in fixed
 * columns and a scrolling worklist does not visually jitter.
 */
const val TABULAR_NUMERALS = "tnum"
```

It's set as `fontFeatureSettings` on **every** style in the scale — sans and mono. Digits get equal advance widths, so `09:15` and `11:45` occupy identical space and a column of times reads straight down instead of wobbling.

### 4.5 `monoTag()`

A one-liner in `HelixTheme.kt` for the very common "mono style tinted to a signal colour":

```kotlin
@Composable
@ReadOnlyComposable
fun monoTag(color: Color): TextStyle = HelixTheme.typography.monoSmall.copy(color = color)
```

---

## 5. Spacing, radius, elevation, motion, dimens

All in `core/designsystem/token/Tokens.kt`. The file's own rule:

> Screens must never hard-code a dp value that exists here — that is what keeps a restyle a token edit rather than a sweep through every composable.

### `Spacing` — 4pt base scale

| | `xs` | `sm` | `md` | `lg` | `xl` | `xxl` | `xxxl` |
|---|---|---|---|---|---|---|---|
| dp | 4 | 8 | 12 | 16 | 20 | 28 | 40 |

`Spacing.xl` (20dp) is the standard screen horizontal padding. `Spacing.lg` (16dp) is the standard card content padding.

### `Radius`

| | `sm` | `md` | `lg` | `xl` | `pill` |
|---|---|---|---|---|---|
| dp | 8 | 12 | 16 | 20 | 999 |

`Radius.lg` (16) is the card radius. `Radius.md` (12) is buttons, chips, search bar, modality block. `Radius.sm` (8) is tags and timeline blocks. `pill` is effectively a stadium shape; it matches the design's `999`.

### `Elevation`

| | `none` | `sm` | `md` | `lg` |
|---|---|---|---|---|
| dp | 0 | 1 | 3 | 8 |

Light mode only — in dark mode every `shadow()` call is skipped.

### `Motion`

```kotlin
object Motion {
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val spring: Easing = CubicBezierEasing(0.22f, 1.2f, 0.36f, 1f)

    const val FAST_MS = 140
    const val BASE_MS = 220
    const val SLOW_MS = 340
    const val SPRING_MS = 420
}
```

### `Dimens`

| Token | dp | Used for |
|---|---|---|
| `touchTargetMin` | 48 | Minimum tap target — rows, buttons, chips, nav items |
| `bottomBarHeight` | 60 | `HelixBottomNavigation` |
| `avatarSm` / `avatarMd` / `avatarLg` | 28 / 36 / 56 | `HelixAvatar` sizes |
| `criticalAccentWidth` | 3 | The STAT/critical left rule |
| `timelineAccentWidth` | 2 | Modality accent on timeline/up-next/prior rows |
| `hairline` | 1 | Every rule and border width |
| `iconSm` / `iconMd` / `iconLg` | 16 / 20 / 24 | Icon sizing |

---

## 6. How to theme a composable correctly

Two accessors, both `@ReadOnlyComposable`:

```kotlin
object HelixTheme {
    val colors: HelixColors
        @Composable @ReadOnlyComposable get() = LocalHelixColors.current

    val typography: HelixTypography
        @Composable @ReadOnlyComposable get() = LocalHelixTypography.current
}
```

The pattern every component follows — grab `colors` once at the top, pull styles inline, take every dp from a token. This is `HelixDetailRow` verbatim:

```kotlin
@Composable
fun HelixDetailRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.touchTargetMin)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: colors.textDim,
            modifier = Modifier.size(Dimens.iconMd),
        )
        Column(Modifier.weight(1f)) {
            Text(text = title, style = HelixTheme.typography.label, color = tint ?: colors.text)
            // ...
        }
    }
}
```

### The rule

**A screen must never hard-code a colour, a dp, or an sp.**

| Don't | Do |
|---|---|
| `Color(0xFFF87171)` | `HelixTheme.colors.critical` |
| `MaterialTheme.colorScheme.error` | `HelixTheme.colors.critical` |
| `padding(16.dp)` | `padding(Spacing.lg)` |
| `fontSize = 14.sp` | `style = HelixTheme.typography.body` |
| `RoundedCornerShape(16.dp)` | `RoundedCornerShape(Radius.lg)` |
| `size(24.dp)` on an icon | `size(Dimens.iconLg)` |

### The legitimate exceptions

There *are* raw dp values in the codebase and they follow a consistent logic — they're **component dimensions**, not design tokens:

- `private val HOUR_HEIGHT = 88.dp` in `ScheduleTimeline.kt`, with the comment: *"A component dimension rather than a design token: it is the timeline's scale, and changing it changes how much of a day fits on screen, not the app's spacing rhythm."*
- `private val RAIL_WIDTH = 84.dp` in `HelixNavigationRail.kt`, `private val ICON_TILE_SIZE = 34.dp` in `HelixMenuRow.kt`, `REFERRER_MAX_WIDTH = 108.dp` in `WorklistComponents.kt`.

If you need one, make it a `private val` at the bottom of the file with a name and a reason. Alpha constants follow the same convention (`private const val TAG_FILL_ALPHA = 0.13f`).

Off-scale values derived from tokens are also fine and used widely: `Spacing.sm - 2.dp`, `Spacing.xs + 1.dp`, `Radius.md - 2.dp`. They keep the relationship to the token even when the design asked for an odd number.

---

## 7. Component catalogue

Everything in `core/designsystem/component/`. All are `@Composable` unless marked.

### `HelixPrimitives.kt` — the shared vocabulary

| Name | For | Key parameters |
|---|---|---|
| `HelixHairline` | 1dp rule at hairline opacity | `modifier`, `color = colors.hairline` |
| `HelixCard` | The standard card. Light-mode shadow / dark-mode hairline | `onClick`, `shape = RoundedCornerShape(Radius.lg)`, `elevation = Elevation.sm`, `contentPadding = Spacing.lg`, `content: ColumnScope.() -> Unit` |
| `HelixOverline` | Wide-tracked all-caps section marker. Uppercases for you | `text`, `color = colors.textMute` |
| `HelixSectionHeader` | Overline + optional trailing action (`UP NEXT   Schedule ›`) | `title`, `actionLabel`, `onActionClick` |
| `HelixDot` | 6dp status dot. **Always** paired with a label by the caller | `color`, `size = 6.dp` |
| `HelixTag` | Mono tag (`MR`, `CT`, `STAT`). Never wraps — `maxLines = 1, softWrap = false` | `text`, `color`, `filled = true` |
| `HelixStatusChip` | Dot + label. Colour and words always travel together | `label`, `color` |
| `HelixInlineAlert` | Icon + text, for allergy / contrast warnings | `icon`, `text`, `color` |
| `HelixMetaRow` | Row that spaces metadata fragments | `content: RowScope.() -> Unit` |
| `HelixMetaText` | A mono metadata fragment | `text`, `color = colors.textMute` |
| `HelixMetaSeparator` | The design's `·` bullet, in mono | — |

### `HelixClinicalComponents.kt` — clinical atoms

| Name | For | Key parameters |
|---|---|---|
| `ModalityTag` | Small mono modality tag. Sets `contentDescription` to the full modality label | `modality: Modality` |
| `ModalityBlock` | Filled modality square, used as a list-row leading element | `modality`, `size = 40.dp` |
| `HelixPriorityBadge` | STAT/Urgent as outlined tags; **Routine renders as plain muted text**, not a badge | `priority: Priority` |
| `HelixAvatar` | Circular initials avatar on a `primary @ 16%` fill | `initials`, `size = 32.dp`, `onClick` |
| `HelixMetric` | Big figure + label + optional tinted note ("2 STAT") | `value`, `label`, `note`, `noteColor`, `valueColor = colors.text`, `onClick` |
| `HelixMetricPair` | Two metrics side by side (the design's `StatPair`) | `start`, `end` |
| `HelixClinicalIndication` | The reason for exam. `label == null` → plain reading line; `label != null` → recessed panel with the label inside | `text`, `label` |

### `HelixActions.kt` — buttons

| Name | For | Key parameters |
|---|---|---|
| `HelixPrimaryAction` | Primary CTA. **Neutral fill** — `colors.text` background, `colors.background` foreground | `label`, `onClick`, `icon`, `enabled`, `container`, `content` |
| `HelixSecondaryAction` | Outlined secondary | `label`, `onClick`, `icon`, `enabled` |
| `HelixActionPair` | Equal-width `1fr 1fr` button row | `start`, `end` |

M3 `Button` is not used here — the design's primary action is a neutral fill, which M3 has no variant for. Keeping brand violet out of buttons is what leaves it free to mean "link" and "selected". `container`/`content` exist for destructive confirms.

### `HelixDetailComponents.kt` — page scaffolding

| Name | For | Key parameters |
|---|---|---|
| `HelixSection` | Overline + optional action + content. **Draws no box** | `title`, `actionLabel`, `onActionClick`, `contentSpacing = Spacing.md`, `content: ColumnScope.() -> Unit` |
| `HelixFactCard` | `HelixCard` with `contentPadding = 0.dp` — content supplies its own | `content: ColumnScope.() -> Unit` |
| `HelixRowDivider` | Hairline between rows in a card, inset past the leading element | `inset = Spacing.lg` |
| `HelixDetailRow` | Bare icon + title + subtitle + chevron. Chevron only when `onClick != null` | `icon`, `title`, `subtitle`, `tint`, `onClick` |

### `HelixFields.kt` — labelled facts

| Name | For | Key parameters |
|---|---|---|
| `HelixMiniField` | `REPORTED / 09:48`. Value defaults to mono | `label`, `value`, `mono = true` |
| `HelixMiniFieldPair` | Two mini-fields split by a vertical hairline (the 2-up fact grid) | `startLabel`, `startValue`, `endLabel`, `endValue`, `startMono`, `endMono` |

### `HelixFilterChip.kt`

| Name | For | Key parameters |
|---|---|---|
| `HelixFilterChip` | Filter chip. `tone` carries the modality/priority thread | `label`, `selected`, `onClick`, `count`, `tone`, `style = Solid` |
| `HelixFilterChipStyle` | *(enum)* `Solid` — accent fill when selected (Worklist, Schedule, Results). `Tinted` — wash behind an accent outline (Notifications, Integration) | — |
| `HelixFilterChipRow` | Horizontally scrolling strip. Filters never wrap | `content: RowScope.() -> Unit` |

Both styles paint at the design's height but reserve a 48dp slot — `minimumInteractiveComponentSize()` on `Solid`, a `defaultMinSize` + 4dp vertical inset on `Tinted`. Selection is spoken (`Role.Tab` + `contentDescription = "$label, $count"`), not just painted.

### `HelixMenuRow.kt` — menu and settings rows

| Name | For | Key parameters |
|---|---|---|
| `HelixMenuRow` | Icon **tile** + title + optional status-coloured subtitle + count + chevron | `icon`, `title`, `subtitle`, `statusColor`, `count`, `onClick`, `unavailableNote` |
| `HelixToggleRow` | Label + `Switch`, bound as a single `toggleable` node | `title`, `checked`, `onCheckedChange`, `subtitle`, `enabled`, `lockedNote` |
| `HelixCountPill` | Neutral count pill on `colors.elevated`. **Not** a signal, so uncoloured | `count` |

`unavailableNote` is how a not-yet-built destination stays honest: pass `onClick = null` plus a note and the row dims to 50%, loses its chevron and its click target, and says "Soon" in words. `lockedNote` does the equivalent for a switch that is shown but can't do what it claims.

### `HelixSearchBar.kt`

| Name | For | Key parameters |
|---|---|---|
| `HelixSearchBar` | Filled single-row search input | `query`, `onQueryChange`, `placeholder = "Search patient, MRN, accession…"`, `onSearch`, `onClear`, `autoFocus = false`, `textStyle = typography.body`, `elevation = Elevation.sm` |

Built on `BasicTextField`, not M3 `TextField`, so it can match the design exactly (no floating label, no indicator line) while keeping platform text editing and IME. `autoFocus` is for the dedicated Search screen only — an inline bar over a populated list must not steal focus.

### `HelixSegmentedControl.kt`

| Name | For | Key parameters |
|---|---|---|
| `HelixSegmentedControl<T>` | Sunken track, active segment lifted onto `surface` | `options: List<T>`, `selected: T`, `label: (T) -> String`, `onSelect: (T) -> Unit` |

Generic over the option type so callers pass their own enum — an unselectable state is impossible to express. Segments are `Role.RadioButton`. The lift is a light-mode shadow only.

### `HelixTopBar.kt`

| Name | For | Key parameters |
|---|---|---|
| `HelixTopBar` | Header for a pushed destination | `title`, `subtitle`, `onBack`, `actions: RowScope.() -> Unit` |

Detail destinations hide the tab bar (see `HelixApp`), so every one of them carries its own title and way back. `onBack` is nullable because a root screen like More has a title but nowhere to go back to.

### `HelixBottomNavigation.kt` / `HelixNavigationRail.kt`

| Name | For | Key parameters |
|---|---|---|
| `HelixBottomNavigation` | Phone tab bar: hairline top rule, 60dp, 20dp icons over caption labels | `destinations`, `selected`, `badgeCounts: Map<TopLevelDestination, Int>`, `onSelect` |
| `HelixCountBadge` | Small red count badge, caps at `9+` | `count` |
| `HelixNavigationRail` | Wide-screen counterpart, 84dp wide, same destinations and badge semantics | same signature as `HelixBottomNavigation` |

Each item is one `selectable` node with `Role.Tab`, and the icon + badge are folded into a single semantics node with `clearAndSetSemantics` so a screen reader says *"Results, 4 unread"* rather than reading the badge as a stray number.

---

## 8. The four most-used components, in anger

### `HelixCard` + `HelixSectionHeader`

From `TodayScreen.kt` — a card of hairline-separated rows under a section header with a trailing action:

```kotlin
Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
    HelixSectionHeader(
        title = "Awaiting sign-off",
        actionLabel = "All ${model.awaitingSignOff.size}",
        onActionClick = actions.openResults,
    )
    HelixCard(contentPadding = 0.dp) {
        model.awaitingSignOff.take(AWAITING_PREVIEW_COUNT).forEachIndexed { index, report ->
            if (index > 0) HelixHairline(Modifier.padding(start = Spacing.lg))
            PendingReportRow(report = report, onOpen = { actions.openReport(report.id) })
        }
    }
}
```

The `contentPadding = 0.dp` + `if (index > 0) HelixHairline(...)` pattern is everywhere. `HelixFactCard` is the same thing with the zero padding baked in, and `HelixRowDivider(inset = …)` is the inset-aware version of that hairline.

### `HelixSection` + `HelixFactCard` + `HelixMiniFieldPair`

From `OrderDetailScreen.kt` — the standard detail-screen block:

```kotlin
HelixSection(title = "Order") {
    HelixFactCard {
        HelixMiniFieldPair(
            startLabel = "Ordered",
            startValue = ClinicalFormat.dateTime(order.orderedAt),
            endLabel = "Facility",
            endValue = context.facilityName ?: order.facilityId,
            endMono = false,
        )
        HelixHairline()
        HelixMiniFieldPair(
            startLabel = "Ordering physician",
            startValue = order.orderingPhysician,
            endLabel = "Location",
            endValue = order.orderingLocation,
            startMono = false,
            endMono = false,
        )
    }
}
```

Rule of thumb for `mono`: leave it `true` for identifiers, times and codes; set it `false` for human names and prose.

### `HelixFilterChipRow` + `HelixFilterChip`

From `WorklistScreen.kt` — chips carrying the priority colour thread, with live counts:

```kotlin
HelixFilterChipRow(
    modifier = Modifier.padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.md),
) {
    filters.priorityFilters.forEach { option ->
        HelixFilterChip(
            label = option.label,
            count = option.count,
            selected = filters.priority == option.priority,
            tone = option.priority?.takeIf { it != Priority.ROUTINE }?.let { priorityMeta(it).color },
            onClick = { viewModel.selectPriority(option.priority) },
        )
    }
}
```

Note `tone` is deliberately `null` for Routine and for the "All" chip — only chips that carry urgency get a colour.

### `HelixPrimaryAction` + `HelixSecondaryAction` + `HelixActionPair`

From `SignReportSheet.kt`:

```kotlin
HelixActionPair(
    modifier = Modifier.padding(bottom = Spacing.lg),
    start = {
        HelixSecondaryAction(
            label = "Cancel",
            onClick = onDismiss,
            enabled = signature !is SignatureState.Submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    },
    end = {
        HelixPrimaryAction(
            label = if (signature is SignatureState.Submitting) "Signing…" else "Sign & finalise",
            icon = Icons.Outlined.Draw,
            onClick = onConfirm,
            enabled = signature !is SignatureState.Submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    },
)
```

`HelixActionPair` gives each side `weight(1f)` but doesn't force the child to fill it — hence `Modifier.fillMaxWidth()` on both buttons.

---

## 9. State components

Not under `designsystem/component/` but part of the same system: `core/ui/HelixStateHost.kt`.

| Name | For |
|---|---|
| `HelixStateHost<T>` | Renders `Loading` / `Success` / `Empty` / `Error` / `Offline` identically everywhere. A screen supplies only success content and its own empty copy |
| `HelixOfflineBanner` | `warning @ 12%` strip — "You're offline · showing your last synchronised data" |
| `HelixEmptyState` | Full-viewport centred icon + title + body |
| `HelixErrorState` | Full-viewport, `critical`-tinted icon, optional "Try again" |
| `HelixSkeletonBlock` | Pulsing block — opacity 0.35↔0.75 over 900ms, `RepeatMode.Reverse`. **Not** a shimmer sweep |
| `HelixListSkeleton` | `rows = 5` of three skeleton blocks |

Signature you'll use on every screen:

```kotlin
HelixStateHost(
    state = state,
    modifier = Modifier.weight(1f),
    onRetry = viewModel::refresh,
    emptyTitle = "No pending orders",
    emptyBody = "Every order on this worklist has been actioned.",
) { orders ->
    OrderList(orders = orders, /* … */)
}
```

`UiState.Offline` carrying `cached != null` renders `HelixOfflineBanner()` above the normal success content — the clinician keeps the worklist they already had, clearly marked stale. With nothing cached it falls back to a full `HelixErrorState` with a `CloudOff` icon.

---

## 10. How to restyle the whole app

The point of the token layer is that a design change is a **token edit**, not a sweep.

### Edit these

| Change | File | What to touch |
|---|---|---|
| Any semantic colour (success/warning/critical/info, any surface, any text tier) | `core/designsystem/theme/HelixColors.kt` | `darkHelixColors()` / `lightHelixColors()` |
| Brand hue | `core/designsystem/theme/HelixColors.kt` | The `HelixPalette` entry (and pass it into `HelixTheme(...)` from `MainActivity.kt`) |
| A modality's colour | `core/designsystem/theme/ClinicalColorMeta.kt` | `modalityRamps` |
| A status label or its colour | `core/designsystem/theme/ClinicalColorMeta.kt` | The relevant `*Meta()` function |
| Any type size, weight, tracking or line height | `core/designsystem/theme/HelixTypography.kt` | `helixTypography` |
| The fonts themselves | `app/src/main/res/font/` + `HelixTypography.kt` | Drop in TTFs, update `InterFontFamily` / `MonoFontFamily` |
| Spacing rhythm, corner radii, shadow depths, motion durations, icon/touch sizes | `core/designsystem/token/Tokens.kt` | `Spacing` / `Radius` / `Elevation` / `Motion` / `Dimens` |
| M3 shape mapping | `core/designsystem/theme/HelixTheme.kt` | `helixShapes` |
| What M3 components inherit | `core/designsystem/theme/HelixTheme.kt` | `toMaterialScheme()` / `toMaterialTypography()` |

### Then, if the *shape* of a thing changed

Edit the one component in `core/designsystem/component/`. Change `HelixCard` and every card in the app changes. Change `HelixFilterChip` and every filter strip changes.

### Leave these alone

**Do not** touch anything under `feature/`. If you find yourself editing a screen to apply a restyle, one of two things is true:

1. The value you're changing should have been a token — promote it to `Tokens.kt` and use it from the component.
2. The thing you're restyling should have been a shared component — pull it up into `core/designsystem/component/`.

The screens are consumers. Their job is layout and data binding, nothing else.

### Verifying a restyle

Both themes, both polarities, in one pass:

- `Settings → Appearance → Light / Dark / System` re-themes the whole app live (`SettingsRepository.observeSettings()` drives `MainActivity`).
- `Settings → Developer → Simulate offline / Simulate error` lets you see every screen's offline and error states without a backend.
- Bump the system font size to 200% and check the Schedule timeline — `TimelineBlock` measures its content against the height the clock gives it and sheds lines rather than clipping, and that logic reads line heights straight off the type scale.

---

## 11. How to add a new shared component

### Where it goes

`core/designsystem/component/`. Group it into an existing file by *role*, not by alphabet:

| If it's… | Put it in |
|---|---|
| A generic primitive (rule, dot, tag, card, section marker) | `HelixPrimitives.kt` |
| Clinical (modality, priority, patient, metric) | `HelixClinicalComponents.kt` |
| A button | `HelixActions.kt` |
| Page scaffolding (section, grouping surface, row, divider) | `HelixDetailComponents.kt` |
| A labelled fact / data grid | `HelixFields.kt` |
| A menu or settings row | `HelixMenuRow.kt` |

Anything genuinely new gets its own file named after the component (`HelixSearchBar.kt`, `HelixSegmentedControl.kt`, `HelixTopBar.kt`).

**Stay in `feature/<name>/components/` instead** if only one screen uses it. `OrderCard`, `ReportCard`, `ScheduleDayTimeline` and `NotificationRow` all live under their feature. Promote to `designsystem` only on the second consumer — `ReportCard` is the counter-example that arguably should have been promoted (it's used by Results, Patient, Study **and** Order detail, but still lives in `feature/results/components/`).

### Naming

- Prefix `Helix` for anything generic: `HelixCard`, `HelixDot`, `HelixToggleRow`.
- No prefix for domain-named atoms: `ModalityTag`, `ModalityBlock`.
- Suffix says what it is: `*Row`, `*Card`, `*Chip`, `*Badge`, `*Tag`, `*Action`, `*Section`.

### The signature convention

```kotlin
@Composable
fun HelixThing(
    // 1. required data, positional
    label: String,
    value: Int,
    // 2. modifier, always after required params, always defaulted
    modifier: Modifier = Modifier,
    // 3. optional config, defaulted from tokens — never from literals
    color: Color = HelixTheme.colors.textMute,
    size: Dp = Dimens.iconMd,
    // 4. callbacks last; nullable when the behaviour is optional
    onClick: (() -> Unit)? = null,
    // 5. trailing content slot, if any
    content: @Composable ColumnScope.() -> Unit,
)
```

Always accept and apply `modifier` first in the chain. Defaults come from `HelixTheme.colors` / `Dimens` / `Spacing`, never from a literal.

### Accessibility rules it must follow

These are non-negotiable — a clinical app fails review without them.

1. **48dp minimum touch target.** Use `heightIn(min = Dimens.touchTargetMin)`, `defaultMinSize(minHeight = Dimens.touchTargetMin)`, or `minimumInteractiveComponentSize()`. If the painted size must be smaller, reserve the target with padding and keep the paint inside it — see `TintedChip`.
   *The one documented exception is the Schedule timeline block:* a 15-minute block can't be 48dp tall without lying about when it happens, so it trades the minimum for temporal accuracy, and the List view is the accessible equivalent.

2. **Never communicate meaning with colour alone.** Every status gets its word. If a colour carries information, a `Text` next to it must say the same thing.

3. **Give the right semantic role.** `Role.Tab` for tabs and filter chips, `Role.RadioButton` for segmented controls and single-choice rows, `Role.Switch` for toggles. Mark unavailable rows with `semantics { disabled() }`.

4. **One semantics node per logical control.** Bind the label to its control:
   ```kotlin
   // Null handler: the whole row owns the toggle semantics, so the switch must not add its own.
   Switch(checked = checked, onCheckedChange = null, enabled = enabled)
   ```
   Use `clearAndSetSemantics { contentDescription = … }` when several visual pieces are one thing (nav item + badge, modality tag + code).

5. **Decorative icons get `contentDescription = null`.** Functional ones get a real sentence: `"Clear notification: ${notification.title}"`, `"Filter orders, $activeCount active"`.

6. **Nothing is lost when a layout sheds detail.** If your component drops a tag at narrow widths, the dropped information must still be in the `contentDescription`.

7. **A control that can't act must say so.** `unavailableNote` / `lockedNote` / `enabled = false` with a visible reason. Never a tappable-looking row that does nothing.

### Before you commit

- Does it read correctly in **both** themes? Check the `isDark` branch for shadow vs hairline and for tint alphas (dark tints run slightly higher — `0.12f` vs `0.09f`, `0.14f` vs `0.10f`).
- Does it survive 200% font scale?
- Is every dp from `Spacing` / `Radius` / `Dimens`, every colour from `HelixTheme.colors`, every text style from `HelixTheme.typography`?
- Does the KDoc say *why* it exists and what decision it encodes? Every existing component does — that's the house style, and it's what makes the design reviewable years later.
