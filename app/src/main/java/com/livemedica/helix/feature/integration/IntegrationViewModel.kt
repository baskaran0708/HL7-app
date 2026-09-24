package com.livemedica.helix.feature.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.core.ui.toUiState
import com.livemedica.helix.domain.model.Hl7Message
import com.livemedica.helix.domain.model.Hl7MessageType
import com.livemedica.helix.domain.model.IntegrationSnapshot
import com.livemedica.helix.domain.model.InterfaceState
import com.livemedica.helix.domain.repository.IntegrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The HL7 interface readout.
 *
 * [visibleMessages] is filtered here rather than in the composable so the chip counts and the list
 * are computed from the same snapshot and cannot disagree.
 */
data class IntegrationUiModel(
    val snapshot: IntegrationSnapshot,
    val selectedType: Hl7MessageType?,
    val visibleMessages: List<Hl7Message>,
) {
    val degradedChannels: Int get() = snapshot.channels.count { it.state.isUnhealthy() }
}

@HiltViewModel
class IntegrationViewModel @Inject constructor(
    private val integrationRepository: IntegrationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<IntegrationUiModel>>(UiState.Loading)
    val state: StateFlow<UiState<IntegrationUiModel>> = _state.asStateFlow()

    private var selectedType: Hl7MessageType? = null

    init {
        load()
    }

    fun refresh() = load()

    fun selectType(type: Hl7MessageType?) {
        selectedType = type
        // Re-filter in place: changing a filter must not throw the screen back to a skeleton.
        _state.update { current ->
            if (current is UiState.Success) {
                UiState.Success(current.data.withType(type))
            } else {
                current
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = integrationRepository.getSnapshot().toUiState(
                isEmpty = { it.recentMessages.isEmpty() && it.channels.isEmpty() },
                emptyMessage = "No interface activity has been recorded today.",
            ).map { snapshot ->
                IntegrationUiModel(
                    snapshot = snapshot,
                    selectedType = selectedType,
                    visibleMessages = snapshot.messagesOfType(selectedType),
                )
            }
        }
    }
}

private fun IntegrationUiModel.withType(type: Hl7MessageType?) = copy(
    selectedType = type,
    visibleMessages = snapshot.messagesOfType(type),
)

private fun IntegrationSnapshot.messagesOfType(type: Hl7MessageType?): List<Hl7Message> =
    if (type == null) recentMessages else recentMessages.filter { it.type == type }

private fun InterfaceState.isUnhealthy(): Boolean = this != InterfaceState.ONLINE

/**
 * Maps the payload of a [UiState] while leaving loading, empty, error and offline untouched —
 * including the cached data an [UiState.Offline] is carrying.
 */
private fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Success -> UiState.Success(transform(data))
    is UiState.Offline -> UiState.Offline(cached?.let(transform))
    is UiState.Empty -> this
    is UiState.Error -> this
    UiState.Loading -> UiState.Loading
}
