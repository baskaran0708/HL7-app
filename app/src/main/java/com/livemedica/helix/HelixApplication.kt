package com.livemedica.helix

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Deliberately empty of logic: everything the app needs is provided through Hilt so that the
 * Phase-1 mock data sources can be swapped for the AWS-backed implementations by changing the
 * bindings in `data/di`, with no change here and none in the UI.
 */
@HiltAndroidApp
class HelixApplication : Application()
