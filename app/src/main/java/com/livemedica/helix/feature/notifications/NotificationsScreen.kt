package com.livemedica.helix.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.core.ui.UiState
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.NotificationCategory
import com.livemedica.helix.feature.notifications.components.NotificationRow
import com.livemedica.helix.feature.notifications.components.presentation
import kotlinx.coroutines.delay

/**
 * The notification centre.
 *
 * Unread is a section rather than a badge: a clinician's question is "what have I not seen?", and
 * answering it with a list beats answering it with a number they then have to hunt through.
 */
@Composable
fun NotificationsScreen(
    actions: HelixNavActions,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val model = (state as? UiState.Success)?.data

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        HelixTopBar(
            title = "Notifications",
            subtitle = model?.let {
                if (it.unreadTotal > 0) "${it.unreadTotal} unread" else "All caught up"
            },
            onBack = actions.navigateBack,
        ) {
            if (model != null && model.unreadTotal > 0) {
                IconButton(onClick = viewModel::markAllRead) {
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = "Mark all notifications read",
                        tint = HelixTheme.colors.primary,
                        modifier = Modifier.size(Dimens.iconMd),
                    )
                }
            }
        }

        actionMessage?.let { message -> ActionMessageBanner(message, viewModel::dismissActionMessage) }

        CategoryFilters(
            counts = model?.counts.orEmpty(),
            totalCount = model?.totalCount ?: 0,
            selected = model?.selectedCategory,
            onSelect = viewModel::selectCategory,
        )

        HelixStateHost(
            state = state,
            emptyTitle = "Nothing here",
            emptyBody = "You're all caught up.",
        ) { data ->
            NotificationsContent(
                model = data,
                onOpen = { notification ->
                    viewModel.markRead(notification.id)
                    notification.target?.let(actions::open)
                },
                onClear = viewModel::clear,
            )
        }
    }
}

@Composable
private fun NotificationsContent(
    model: NotificationsUiModel,
    onOpen: (HelixNotification) -> Unit,
    onClear: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (model.unread.isNotEmpty()) {
            NotificationGroup(
                title = "Unread · ${model.unread.size}",
                notifications = model.unread,
                onOpen = onOpen,
                onClear = onClear,
            )
        }
        if (model.earlier.isNotEmpty()) {
            NotificationGroup(
                title = "Earlier",
                notifications = model.earlier,
                onOpen = onOpen,
                onClear = onClear,
            )
        }
    }
}

@Composable
private fun NotificationGroup(
    title: String,
    notifications: List<HelixNotification>,
    onOpen: (HelixNotification) -> Unit,
    onClear: (String) -> Unit,
) {
    HelixSection(title = title) {
        HelixCard(contentPadding = 0.dp) {
            notifications.forEachIndexed { index, notification ->
                if (index > 0) HelixRowDivider(inset = 66.dp)
                NotificationRow(
                    notification = notification,
                    onOpen = { onOpen(notification) },
                    onClear = { onClear(notification.id) },
                )
            }
        }
    }
}

@Composable
private fun CategoryFilters(
    counts: Map<NotificationCategory, Int>,
    totalCount: Int,
    selected: NotificationCategory?,
    onSelect: (NotificationCategory?) -> Unit,
) {
    HelixFilterChipRow(
        modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.xs),
    ) {
        HelixFilterChip(
            label = "All",
            selected = selected == null,
            count = totalCount,
            onClick = { onSelect(null) },
            style = HelixFilterChipStyle.Tinted,
        )
        // Only categories that actually have something in them: an empty filter is a dead end.
        NotificationCategory.entries.filter { (counts[it] ?: 0) > 0 }.forEach { category ->
            val (_, tone) = category.presentation()
            HelixFilterChip(
                label = category.label,
                selected = selected == category,
                count = counts[category],
                tone = tone,
                onClick = { onSelect(category) },
                style = HelixFilterChipStyle.Tinted,
            )
        }
    }
}

/**
 * Why a write failed, shown inline rather than as a snackbar: the list stays on screen unchanged,
 * so the message has to sit next to the thing that did not change.
 */
@Composable
private fun ActionMessageBanner(message: String, onDismiss: () -> Unit) {
    val colors = HelixTheme.colors
    LaunchedEffect(message) {
        delay(MESSAGE_VISIBLE_MS)
        onDismiss()
    }
    Text(
        text = message,
        style = HelixTheme.typography.caption,
        color = colors.warning,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.warning.copy(alpha = 0.12f))
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
    )
}

private const val MESSAGE_VISIBLE_MS = 4_000L
