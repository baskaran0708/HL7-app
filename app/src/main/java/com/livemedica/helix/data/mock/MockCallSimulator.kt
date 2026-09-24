package com.livemedica.helix.data.mock

import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.domain.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps every mock repository call so Phase 1 exercises the same code paths the real network will.
 *
 * Two jobs:
 *  - a short artificial delay, so loading skeletons are genuinely visible and we find out now
 *    whether a screen flickers on refresh;
 *  - honouring the Settings → Developer "simulate offline" / "simulate error" switches, which is
 *    the only practical way to review those states before a backend exists.
 */
@Singleton
class MockCallSimulator @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    suspend fun <T> call(cachedOnOffline: (() -> T)? = null, block: suspend () -> T): AppResult<T> {
        val settings = settingsRepository.observeSettings().first()
        delay(LATENCY_MS)

        return when {
            settings.simulateOffline -> AppResult.Offline(cachedOnOffline?.invoke())
            settings.simulateError -> AppResult.Failure(SIMULATED_FAILURE_MESSAGE)
            else -> runCatching { AppResult.Success(block()) }
                .getOrElse { AppResult.Failure(it.message ?: SIMULATED_FAILURE_MESSAGE, it) }
        }
    }

    private companion object {
        const val LATENCY_MS = 320L

        /**
         * Phrased as the backend failure it stands in for, and says what to do next. The screen
         * supplies the "Something went wrong" heading, so this must not repeat it.
         */
        const val SIMULATED_FAILURE_MESSAGE =
            "We couldn't reach the server. Your data is safe — try again in a moment."
    }
}
