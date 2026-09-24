package com.livemedica.helix.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.common.AppResult
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.NotificationCategory
import com.livemedica.helix.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The notification centre's state.
 *
 * [counts] is deliberately computed over the *whole* list rather than the filtered one, so the
 * category chips keep showing how much is in each bucket while a filter is applied.
 */
data class NotificationsUiModel(
    val selectedCategory: NotificationCategory?,
    val counts: Map<NotificationCategory, Int>,
    val totalCount: Int,
    val unreadTotal: Int,
    val unread: List<HelixNotification>,
    val earlier: List<HelixNotification>,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<NotificationCategory?>(null)

    /**
     * Surfaced when a mutation fails — the developer "simulate error" switch makes mark-read fail,
     * and silently doing nothing would look like a broken button rather than a failed write.
     */
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val state: StateFlow<UiState<NotificationsUiModel>> =
        combine(notificationRepository.observeNotifications(), selectedCategory) { all, category ->
            val visible = all.filter { category == null || it.category == category }
            when {
                all.isEmpty() -> UiState.Empty("No notifications have arrived today.")
                visible.isEmpty() -> UiState.Empty("Nothing in ${category?.label?.lowercase()}.")
                else -> UiState.Success(
                    NotificationsUiModel(
                        selectedCategory = category,
                        counts = all.groupingBy { it.category }.eachCount(),
                        totalCount = all.size,
                        unreadTotal = all.count { !it.isRead },
                        unread = visible.filterNot { it.isRead },
                        earlier = visible.filter { it.isRead },
                    )
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    fun selectCategory(category: NotificationCategory?) {
        selectedCategory.value = category
    }

    fun markRead(id: String) = mutate { notificationRepository.markRead(id) }

    fun markAllRead() = mutate { notificationRepository.markAllRead() }

    fun clear(id: String) = mutate { notificationRepository.clear(id) }

    fun dismissActionMessage() {
        _actionMessage.value = null
    }

    private fun mutate(block: suspend () -> AppResult<Unit>) {
        viewModelScope.launch {
            _actionMessage.value = when (val result = block()) {
                is AppResult.Success -> null
                is AppResult.Offline -> "You're offline — this will sync once you reconnect."
                is AppResult.Failure -> result.message
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
