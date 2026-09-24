package com.livemedica.helix.feature.integration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixCard
import com.livemedica.helix.core.designsystem.component.HelixFilterChip
import com.livemedica.helix.core.designsystem.component.HelixFilterChipRow
import com.livemedica.helix.core.designsystem.component.HelixFilterChipStyle
import com.livemedica.helix.core.designsystem.component.HelixRowDivider
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.domain.model.Hl7MessageType
import com.livemedica.helix.domain.model.IntegrationSnapshot
import com.livemedica.helix.feature.integration.components.ChannelRow
import com.livemedica.helix.feature.integration.components.Hl7MessageRow
import com.livemedica.helix.feature.integration.components.IntegrationHeadline
import com.livemedica.helix.feature.integration.components.MessageTypeCounts
import com.livemedica.helix.feature.integration.components.hl7TypeColor

/**
 * HL7 / Mirth operations.
 *
 * This screen is deliberately *not* clinical: no patient names, no priorities, no modality thread.
 * It answers one question — is the interface healthy — in the order an engineer asks it: volume and
 * ACK rate, then the mix by message type, then which channel is misbehaving, then the raw feed.
 */
@Composable
fun IntegrationScreen(
    actions: HelixNavActions,
    viewModel: IntegrationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        HelixTopBar(
            title = "HL7 Integration",
            subtitle = "Mirth interface engine",
            onBack = actions.navigateBack,
        )

        HelixStateHost(
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "No interface activity",
            emptyBody = "No HL7 messages or channels have reported in today.",
        ) { model ->
            IntegrationContent(model = model, onSelectType = viewModel::selectType)
        }
    }
}

@Composable
private fun IntegrationContent(
    model: IntegrationUiModel,
    onSelectType: (Hl7MessageType?) -> Unit,
) {
    val snapshot = model.snapshot

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        IntegrationHeadline(snapshot)

        HelixSection(title = "Message volume by type") {
            MessageTypeCounts(counts = snapshot.countsByType)
        }

        ChannelSection(snapshot = snapshot, degradedChannels = model.degradedChannels)

        MessageFeedSection(model = model, onSelectType = onSelectType)
    }
}

@Composable
private fun ChannelSection(snapshot: IntegrationSnapshot, degradedChannels: Int) {
    val colors = HelixTheme.colors
    HelixSection(title = "Mirth channels") {
        if (degradedChannels > 0) {
            Text(
                text = "$degradedChannels of ${snapshot.channels.size} channels are not fully online.",
                style = HelixTheme.typography.caption,
                color = colors.warning,
            )
        }
        HelixCard(contentPadding = 0.dp) {
            // Worst-first: a degraded or offline channel is the reason anyone opened this screen.
            snapshot.channels
                .sortedByDescending { it.state.ordinal }
                .forEachIndexed { index, channel ->
                    if (index > 0) HelixRowDivider(inset = 44.dp)
                    ChannelRow(channel)
                }
        }
    }
}

@Composable
private fun MessageFeedSection(
    model: IntegrationUiModel,
    onSelectType: (Hl7MessageType?) -> Unit,
) {
    val colors = HelixTheme.colors
    val counts = model.snapshot.countsByType

    HelixSection(title = "Recent messages") {
        HelixFilterChipRow {
            HelixFilterChip(
                label = "All",
                selected = model.selectedType == null,
                count = model.snapshot.recentMessages.size,
                onClick = { onSelectType(null) },
                style = HelixFilterChipStyle.Tinted,
            )
            Hl7MessageType.entries.forEach { type ->
                HelixFilterChip(
                    label = type.label,
                    selected = model.selectedType == type,
                    count = counts[type] ?: 0,
                    tone = hl7TypeColor(type),
                    onClick = { onSelectType(type) },
                    style = HelixFilterChipStyle.Tinted,
                )
            }
        }

        if (model.visibleMessages.isEmpty()) {
            Text(
                text = "No ${model.selectedType?.label ?: "HL7"} messages in the current window.",
                style = HelixTheme.typography.caption,
                color = colors.textMute,
                modifier = Modifier.padding(vertical = Spacing.md),
            )
        } else {
            HelixCard(contentPadding = 0.dp) {
                model.visibleMessages.forEachIndexed { index, message ->
                    if (index > 0) HelixRowDivider(inset = 68.dp)
                    Hl7MessageRow(message)
                }
            }
        }
    }
}
