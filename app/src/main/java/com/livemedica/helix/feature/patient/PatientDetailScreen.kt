package com.livemedica.helix.feature.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.livemedica.helix.core.designsystem.component.HelixFactCard
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.core.ui.dataOrNull
import com.livemedica.helix.feature.patient.components.CurrentOrderRow
import com.livemedica.helix.feature.patient.components.HistoryBullet
import com.livemedica.helix.feature.patient.components.PatientAllergyBanner
import com.livemedica.helix.feature.patient.components.PatientAppointmentRow
import com.livemedica.helix.feature.patient.components.PatientIdentity
import com.livemedica.helix.feature.patient.components.PatientTimelineEvent
import com.livemedica.helix.feature.patient.components.PriorStudyEntry
import com.livemedica.helix.feature.patient.components.StudyRow
import com.livemedica.helix.feature.results.components.ReportCard

/**
 * The patient record.
 *
 * Ordered by clinical urgency rather than by entity type: identity, then anything that changes what
 * you may safely do (allergies), then what is happening now, then the history that informs the
 * current read, then the audit-style timeline. Everything on this screen is read-only — the record
 * is context for a decision made elsewhere.
 */
@Composable
fun PatientDetailScreen(
    actions: HelixNavActions,
    viewModel: PatientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val model = state.dataOrNull()

    Column(Modifier.fillMaxSize()) {
        HelixTopBar(
            title = "Patient",
            subtitle = model?.patient?.mrn,
            onBack = actions.navigateBack,
        )
        HelixStateHost(
            modifier = Modifier.weight(1f),
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Patient not found",
            emptyBody = "This record is not available on this device.",
        ) { data ->
            PatientContent(model = data, actions = actions)
        }
    }
}

@Composable
private fun PatientContent(model: PatientUiModel, actions: HelixNavActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl - 6.dp),
    ) {
        PatientIdentity(patient = model.patient)

        if (model.activeAllergies.isNotEmpty()) {
            PatientAllergyBanner(allergies = model.activeAllergies)
        }

        if (model.currentStudy != null || model.currentOrder != null) {
            HelixSection(title = "Current activity") {
                HelixFactCard {
                    model.currentOrder?.let { order ->
                        CurrentOrderRow(order = order, onOpen = { actions.openOrder(order.id) })
                    }
                    model.currentStudy?.let { study ->
                        if (model.currentOrder != null) HelixHairline(Modifier.padding(start = Spacing.lg))
                        StudyRow(study = study, onOpen = { actions.openStudy(study.id) })
                    }
                }
            }
        }

        if (model.appointments.isNotEmpty()) {
            HelixSection(title = "Appointments") {
                HelixFactCard {
                    model.appointments.forEachIndexed { index, appointment ->
                        if (index > 0) HelixHairline(Modifier.padding(start = Spacing.lg))
                        PatientAppointmentRow(
                            appointment = appointment,
                            onOpen = { actions.openAppointment(appointment.id) },
                        )
                    }
                }
            }
        }

        if (model.previousStudies.isNotEmpty()) {
            HelixSection(title = "Previous studies") {
                HelixFactCard {
                    model.previousStudies.forEachIndexed { index, study ->
                        if (index > 0) HelixHairline(Modifier.padding(start = Spacing.lg))
                        StudyRow(study = study, onOpen = { actions.openStudy(study.id) })
                    }
                }
            }
        }

        if (model.reports.isNotEmpty()) {
            HelixSection(title = "Previous reports") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md - 2.dp)) {
                    model.reports.forEach { report ->
                        ReportCard(report = report, onOpen = { actions.openReport(report.id) })
                    }
                }
            }
        }

        RelevantHistory(model = model)

        if (model.timeline.isNotEmpty()) {
            HelixSection(title = "Timeline") {
                Column(Modifier.fillMaxWidth()) {
                    model.timeline.forEachIndexed { index, event ->
                        PatientTimelineEvent(
                            event = event,
                            isLast = index == model.timeline.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Problem list and prior imaging under one heading — they answer the same question ("what do I need
 * to know before reading this study?"), and separating them would make the reader hunt twice.
 */
@Composable
private fun RelevantHistory(model: PatientUiModel) {
    if (model.problems.isEmpty() && model.priors.isEmpty()) return

    HelixSection(title = "Relevant history") {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md - 2.dp)) {
            model.problems.forEach { entry -> HistoryBullet(entry = entry) }

            if (model.priors.isNotEmpty()) {
                Text(
                    text = "Prior imaging",
                    style = HelixTheme.typography.label,
                    color = HelixTheme.colors.textMute,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    model.priors.forEach { entry -> PriorStudyEntry(entry = entry) }
                }
            }
        }
    }
}
