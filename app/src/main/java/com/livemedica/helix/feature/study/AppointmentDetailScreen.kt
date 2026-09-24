package com.livemedica.helix.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixFactCard
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixMiniFieldPair
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.theme.appointmentStatusMeta
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.core.ui.dataOrNull
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.feature.study.components.ExamActions
import com.livemedica.helix.feature.study.components.ExamEmptyNote
import com.livemedica.helix.feature.study.components.ExamHeadline
import com.livemedica.helix.feature.study.components.ExamPatientStrip

/**
 * Appointment detail — the booking.
 *
 * An appointment precedes the order and the study, so this screen deliberately says what does *not*
 * exist yet rather than leaving blank sections: it is the one place a clinician can tell that an
 * exam is scheduled but nothing has been filled.
 */
@Composable
fun AppointmentDetailScreen(
    actions: HelixNavActions,
    viewModel: AppointmentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val model = state.dataOrNull()

    Column(Modifier.fillMaxSize()) {
        HelixTopBar(
            title = "Appointment",
            subtitle = model?.appointment?.let { ClinicalFormat.dateTime(it.start) },
            onBack = actions.navigateBack,
        )
        HelixStateHost(
            modifier = Modifier.weight(1f),
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Appointment not found",
            emptyBody = "This booking is no longer on the schedule.",
        ) { context ->
            AppointmentContent(context = context, actions = actions)
        }
    }
}

@Composable
private fun AppointmentContent(context: ExamContext, actions: HelixNavActions) {
    val appointment = context.appointment ?: return
    val statusMeta = appointmentStatusMeta(appointment.status)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl - 6.dp),
    ) {
        ExamPatientStrip(
            patient = context.patient,
            fallbackName = appointment.patientName,
            fallbackMrn = appointment.mrn,
            allergies = context.activeAllergies,
            onOpenPatient = { actions.openPatient(appointment.patientId) },
        )

        ExamHeadline(
            modality = appointment.modality,
            priority = appointment.priority,
            statusLabel = statusMeta.label,
            statusColor = statusMeta.color,
            procedure = appointment.procedure,
            accessionNumber = null,
            cptCode = null,
        )

        HelixSection(title = "Schedule") {
            HelixFactCard {
                HelixMiniFieldPair(
                    startLabel = "Scheduled",
                    startValue = ClinicalFormat.dateTime(appointment.start),
                    endLabel = "Duration",
                    endValue = "${appointment.durationMinutes} min",
                )
                HelixHairline()
                HelixMiniFieldPair(
                    startLabel = "Room",
                    startValue = appointment.room,
                    endLabel = "Facility",
                    endValue = context.facilityName ?: appointment.facilityId,
                    endMono = false,
                )
            }
        }

        HelixSection(title = "Order") {
            ExamEmptyNote(
                icon = Icons.AutoMirrored.Outlined.Assignment,
                title = "No order filled yet",
                body = "The accession, CPT code and clinical indication arrive with the order.",
            )
        }

        ExamActions(
            study = null,
            onViewPatient = { actions.openPatient(appointment.patientId) },
        )
    }
}
