package com.livemedica.helix.core.ui

import androidx.compose.runtime.Immutable

/**
 * The state contract every screen uses.
 *
 * [Offline] carries the last-known data rather than replacing the screen, because a clinician
 * losing signal should still see the worklist they already had — just clearly marked as stale.
 * [Empty] is distinct from [Success] with an empty list so each screen can say something useful
 * ("No pending orders") instead of showing a blank surface.
 */
@Immutable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String? = null) : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Offline<T>(val cached: T? = null) : UiState<T>
}

fun <T> UiState<T>.dataOrNull(): T? = when (this) {
    is UiState.Success -> data
    is UiState.Offline -> cached
    else -> null
}
