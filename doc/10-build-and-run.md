# 10 — Build and run

Getting the project building, running it on a device, and producing APKs.

- [What you need](#what-you-need)
- [First run](#first-run)
- [Gradle commands](#gradle-commands)
- [Running on an emulator](#running-on-an-emulator)
- [Building APKs](#building-apks)
- [Release signing](#release-signing)
- [Toolchain notes and gotchas](#toolchain-notes-and-gotchas)

---

## What you need

| | Version | Notes |
|---|---|---|
| JDK | **17+** (25 recommended) | The Gradle daemon is pinned to JDK 25 by `gradle/gradle-daemon-jvm.properties`. Android Studio's bundled JBR is 25 and works. |
| Android SDK | platform **37** + build-tools 36.1.0 or newer | AGP will auto-download build-tools if missing |
| Gradle | 9.6.0 | via the wrapper — don't install it separately |
| Android Studio | any version bundling JBR 25+ | optional; the CLI works fine |

Dependency versions are all declared in `gradle/libs.versions.toml`. Add new dependencies there,
never inline in `app/build.gradle.kts`.

---

## First run

1. Clone the repo.
2. Create `local.properties` in the project root pointing at your SDK:
   ```properties
   sdk.dir=D\:\\path\\to\\Android\\Sdk
   ```
   (Android Studio writes this for you on first open. It's gitignored — never commit it.)
3. Build:
   ```bash
   ./gradlew :app:assembleDebug
   ```

First build pulls dependencies and takes a few minutes. After that it's seconds.

---

## Gradle commands

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # release APK (unsigned as configured)
./gradlew :app:installDebug         # build + install on the connected device
./gradlew :app:testDebugUnitTest    # JVM unit tests
./gradlew :app:lintDebug            # Android lint -> app/build/reports/lint-results-debug.html
./gradlew clean                     # nuke build outputs
```

**On Windows without JDK 25 on your PATH**, point Gradle at Android Studio's JBR for the
invocation:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleDebug
```

Otherwise Gradle tries to auto-provision JDK 25 over the network, which is slower and can fail
behind a proxy.

---

## Running on an emulator

An AVD named `Helix` is already configured in `~/.android/avd`. If you need to create one:

**Easiest:** Android Studio → Device Manager → Create Device → any Pixel → system image
`android-36.1 google_apis_playstore x86_64`.

**From the CLI**, if you have `cmdline-tools` installed:

```bash
avdmanager create avd -n Helix -k "system-images;android-36.1;google_apis_playstore;x86_64" -d pixel_7
```

Then:

```bash
$ANDROID_HOME/emulator/emulator -avd Helix -no-snapshot &
adb wait-for-device
./gradlew :app:installDebug
adb shell am start -n com.livemedica.helix/.MainActivity
```

Useful while developing:

```bash
adb exec-out screencap -p > shot.png          # screenshot
adb logcat -s AndroidRuntime:E                # crashes only
adb shell cmd uimode night yes                # force dark mode
adb shell cmd uimode night no                 # back to light
adb shell am start -a android.intent.action.VIEW \
  -d "helix://report/RPT-91204" com.livemedica.helix   # deep link (see 08-navigation.md)
```

### Reviewing the offline and error states

No backend needed. **Settings → Developer → Simulate offline / Simulate error.** Both flip every
screen in the app at once. See [07-state-management.md](07-state-management.md).

---

## Building APKs

```bash
./gradlew :app:assembleDebug :app:assembleRelease
```

Outputs:

| Build | Path | Size |
|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | ~24 MB |
| Release | `app/build/outputs/apk/release/app-release-unsigned.apk` | ~16 MB |

Human-named copies are kept in `build-output/` (gitignored — don't version binaries; attach them to
a GitHub Release instead).

The debug APK is the one to hand round for demos: it installs without signing setup and carries the
full demo dataset.

---

## Release signing

**Not configured yet**, deliberately — no keystore should exist in this repo.

When you're ready:

1. Generate a keystore and store it **outside** the repo.
2. Create `keystore.properties` (already gitignored):
   ```properties
   storeFile=/absolute/path/to/release.jks
   storePassword=…
   keyAlias=…
   keyPassword=…
   ```
3. Read it in `app/build.gradle.kts` and wire a `signingConfigs.release`. Never inline the values.
4. Turn on minification for release — it's currently off:
   ```kotlin
   buildTypes {
       release {
           optimization { enable = true }   // AGP 9 DSL, replaces isMinifyEnabled
       }
   }
   ```
   R8 keep rules live in `app/src/main/keepRules/rules.keep` (AGP 9 replaces `proguard-rules.pro`).
   Test the minified build before shipping — Hilt and kotlinx-serialization both need their rules
   to hold.

---

## Toolchain notes and gotchas

**AGP 9 is new, and its DSL differs from AGP 8.** Things you'll notice:

```kotlin
compileSdk { version = release(37) }              // not compileSdk = 37
buildTypes { release { optimization { enable = false } } }  // not isMinifyEnabled
```

Kotlin is applied by AGP itself — there is **no** `org.jetbrains.kotlin.android` plugin in this
project, and that's correct.

**`android.disallowKotlinSourceSets=false` in `gradle.properties` is load-bearing.** AGP 9 rejects
the way KSP registers its generated sources; this is the documented escape hatch and Hilt codegen
fails at configuration time without it. Remove it once KSP adopts the AGP 9 source API.

**Configuration cache is on.** If you hit a weird stale-config error after editing build scripts:

```bash
./gradlew --no-configuration-cache :app:assembleDebug
```

**"Kotlin does not yet support 25 JDK target, falling back to JVM_24"** — harmless. Java and Kotlin
both target 17 via `compileOptions`; the message is about the daemon's JDK.

**Concurrent Gradle runs** on the same project directory will block on a lock and can occasionally
produce a transient KSP `FileNotFoundException`. Retry; don't run two builds at once.

---

Next: [11-conventions.md](11-conventions.md)
