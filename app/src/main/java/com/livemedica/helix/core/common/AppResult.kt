package com.livemedica.helix.core.common

/**
 * Transport-agnostic result type returned by the repository layer.
 *
 * The UI never learns whether data came from the mock store, Room or the AWS API — only whether
 * the call succeeded, failed, or failed specifically because the device is offline. [Offline] is
 * separate from [Failure] because clinical screens fall back to last-synchronised data rather than
 * showing a hard error.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
    data class Offline<T>(val cached: T? = null) : AppResult<T>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
    is AppResult.Offline -> AppResult.Offline(cached?.let(transform))
}

fun <T> AppResult<T>.dataOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Offline -> cached
    is AppResult.Failure -> null
}
