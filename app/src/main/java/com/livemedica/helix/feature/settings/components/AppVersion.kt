package com.livemedica.helix.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The installed app's version name.
 *
 * Read from the package manager rather than `BuildConfig` because this module does not currently
 * generate a `BuildConfig` class (`android.buildFeatures.buildConfig` is off, and turning it on is
 * a build-script change outside this feature's scope). The value is identical either way — it is
 * the `versionName` the APK was built with — so nothing is lost by asking the platform for it.
 */
@Composable
fun rememberAppVersionName(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: UNKNOWN_VERSION
    }
}

private const val UNKNOWN_VERSION = "unknown"
