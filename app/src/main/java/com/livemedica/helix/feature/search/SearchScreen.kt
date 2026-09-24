package com.livemedica.helix.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixSearchBar
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Elevation
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.domain.model.SearchResults
import com.livemedica.helix.feature.search.components.AppointmentResultRow
import com.livemedica.helix.feature.search.components.OrderResultRow
import com.livemedica.helix.feature.search.components.PatientResultRow
import com.livemedica.helix.feature.search.components.ReportResultRow
import com.livemedica.helix.feature.search.components.SearchGroup
import com.livemedica.helix.feature.search.components.SearchIntroPanel
import com.livemedica.helix.feature.search.components.SearchResultDivider
import com.livemedica.helix.feature.search.components.StudyResultRow

/**
 * Global search across every clinical entity.
 *
 * The field sits outside the state host on purpose: a failed or empty search must never take the
 * input away from the user, because correcting the query is exactly what they need to do next.
 */
@Composable
fun SearchScreen(
    actions: HelixNavActions,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recentQueries by viewModel.recentQueries.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        HelixTopBar(title = "Search", onBack = actions.navigateBack) {
            TextButton(onClick = actions.navigateBack) {
                Text(
                    text = "Cancel",
                    style = HelixTheme.typography.label,
                    color = HelixTheme.colors.primary,
                )
            }
        }

        HelixSearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearchSubmitted,
            placeholder = "Patient, MRN, accession, study…",
            // This screen exists only to be typed into, so it arrives with the keyboard already up.
            autoFocus = true,
            textStyle = HelixTheme.typography.bodyLarge,
            elevation = Elevation.none,
            modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm),
        )

        if (query.isBlank()) {
            SearchIntroPanel(
                recentQueries = recentQueries,
                onSelect = viewModel::onRecentQuerySelected,
            )
        } else {
            HelixStateHost(
                state = state,
                emptyTitle = "No results",
                emptyBody = "Nothing matched \"$query\".",
            ) { results ->
                SearchResultsContent(
                    results = results,
                    actions = actions,
                    onResultOpened = viewModel::onResultOpened,
                )
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    results: SearchResults,
    actions: HelixNavActions,
    onResultOpened: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .padding(top = Spacing.xs, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        // Group order is the design's: people first, then the work attached to them.
        if (results.patients.isNotEmpty()) {
            SearchGroup(label = "Patients", count = results.patients.size) {
                results.patients.forEachIndexed { index, patient ->
                    if (index > 0) SearchResultDivider()
                    PatientResultRow(patient = patient, onOpen = {
                        onResultOpened()
                        actions.openPatient(patient.id)
                    })
                }
            }
        }

        if (results.orders.isNotEmpty()) {
            SearchGroup(label = "Orders", count = results.orders.size) {
                results.orders.forEachIndexed { index, order ->
                    if (index > 0) SearchResultDivider()
                    OrderResultRow(order = order, onOpen = {
                        onResultOpened()
                        actions.openOrder(order.id)
                    })
                }
            }
        }

        if (results.reports.isNotEmpty()) {
            SearchGroup(label = "Reports", count = results.reports.size) {
                results.reports.forEachIndexed { index, report ->
                    if (index > 0) SearchResultDivider()
                    ReportResultRow(report = report, onOpen = {
                        onResultOpened()
                        actions.openReport(report.id)
                    })
                }
            }
        }

        if (results.studies.isNotEmpty()) {
            SearchGroup(label = "Studies", count = results.studies.size) {
                results.studies.forEachIndexed { index, study ->
                    if (index > 0) SearchResultDivider()
                    StudyResultRow(study = study, onOpen = {
                        onResultOpened()
                        actions.openStudy(study.id)
                    })
                }
            }
        }

        if (results.appointments.isNotEmpty()) {
            SearchGroup(label = "Appointments", count = results.appointments.size) {
                results.appointments.forEachIndexed { index, appointment ->
                    if (index > 0) SearchResultDivider()
                    AppointmentResultRow(appointment = appointment, onOpen = {
                        onResultOpened()
                        actions.openAppointment(appointment.id)
                    })
                }
            }
        }

        Text(
            text = "${results.totalCount} results for \"${results.query}\"",
            style = HelixTheme.typography.monoSmall,
            color = HelixTheme.colors.textMute,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}
