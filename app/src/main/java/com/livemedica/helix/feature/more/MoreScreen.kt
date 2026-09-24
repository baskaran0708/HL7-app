package com.livemedica.helix.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.feature.more.components.MoreIdentityCard
import com.livemedica.helix.feature.more.components.MoreMenuGroupCard
import com.livemedica.helix.feature.more.components.moreMenuGroups

/**
 * The More tab: everything outside the four clinical tabs, grouped by what it is for.
 *
 * Group order is the product's, not alphabetical — Clinical first because it is what a radiologist
 * reaches for mid-shift, System last because it is visited once a month.
 */
@Composable
fun MoreScreen(
    actions: HelixNavActions,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth()) {
        HelixTopBar(title = "More") {
            IconButton(onClick = actions.openSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search patients, orders and reports",
                    tint = HelixTheme.colors.textDim,
                    modifier = Modifier.size(Dimens.iconLg),
                )
            }
        }

        HelixStateHost(
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Menu unavailable",
            emptyBody = "We couldn't load your profile, so the menu has nothing to show.",
        ) { model ->
            MoreContent(model = model, actions = actions)
        }
    }
}

@Composable
private fun MoreContent(model: MoreUiModel, actions: HelixNavActions) {
    val groups = remember(model, actions) { moreMenuGroups(model, actions) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        MoreIdentityCard(doctor = model.doctor, onOpenProfile = actions.openProfile)

        groups.forEach { group -> MoreMenuGroupCard(group) }

        BuildStamp()
    }
}

/**
 * Standards footer. It is here because interface engineers genuinely ask "which HL7 version is this
 * build speaking?" — and on a clinical device the answer should not require opening a ticket.
 */
@Composable
private fun BuildStamp() {
    Text(
        text = "Helix · HL7 v2.5.1 · FHIR R4 · DICOM",
        style = HelixTheme.typography.monoSmall,
        color = HelixTheme.colors.textMute,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs),
    )
}
