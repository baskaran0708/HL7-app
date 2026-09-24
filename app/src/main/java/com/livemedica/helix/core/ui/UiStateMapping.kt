package com.livemedica.helix.core.ui

import com.livemedica.helix.core.common.AppResult

/**
 * Single place where a repository result becomes screen state.
 *
 * Centralising this is what keeps "empty" meaning the same thing on every screen, and stops each
 * ViewModel from inventing its own error copy.
 */
fun <T> AppResult<T>.toUiState(
    isEmpty: (T) -> Boolean = { false },
    emptyMessage: String? = null,
): UiState<T> = when (this) {
    is AppResult.Success -> if (isEmpty(data)) UiState.Empty(emptyMessage) else UiState.Success(data)
    is AppResult.Offline -> UiState.Offline(cached)
    is AppResult.Failure -> UiState.Error(message)
}

fun <T> AppResult<List<T>>.toListUiState(emptyMessage: String): UiState<List<T>> =
    toUiState(isEmpty = { it.isEmpty() }, emptyMessage = emptyMessage)
