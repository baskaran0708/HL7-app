package com.livemedica.helix.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixClinicalIndication
import com.livemedica.helix.core.designsystem.component.HelixFactCard
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixMiniFieldPair
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.theme.orderStatusMeta
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.core.ui.dataOrNull
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.feature.results.components.ReportCard
import com.livemedica.helix.feature.study.components.ExamActions
import com.livemedica.helix.feature.study.components.ExamEmptyNote
import com.livemedica.helix.feature.study.components.ExamHeadline
import com.livemedica.helix.feature.study.components.ExamPatientStrip

/**
 * Order detail — the requisition as the department received it.
 *
 * Reads top-down as the HL7 ORM does: who, what was asked for, why, who asked, and what came back.
 */
@Composable
fun OrderDetailScreen(
    actions: HelixNavActions,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val model = state.dataOrNull()

    Column(Modifier.fillMaxSize()) {
        HelixTopBar(
            title = "Order",
            subtitle = model?.order?.accessionNumber,
            onBack = actions.navigateBack,
        )
        HelixStateHost(
            modifier = Modifier.weight(1f),
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Order not found",
            emptyBody = "This order is no longer available.",
        ) { context ->
            OrderContent(context = context, actions = actions)
        }
    }
}

@Composable
private fun OrderContent(context: ExamContext, actions: HelixNavActions) {
    val order = context.order ?: return
    val statusMeta = orderStatusMeta(order.status)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl - 6.dp),
    ) {
        ExamPatientStrip(
            patient = context.patient,
            fallbackName = order.patientName,
            fallbackMrn = order.mrn,
            allergies = context.activeAllergies,
            onOpenPatient = { actions.openPatient(order.patientId) },
        )

        ExamHeadline(
            modality = order.modality,
            priority = order.priority,
            statusLabel = statusMeta.label,
            statusColor = statusMeta.color,
            procedure = order.procedure,
            accessionNumber = order.accessionNumber,
            cptCode = order.cptCode,
        )

        HelixSection(title = "Clinical indication") {
            HelixClinicalIndication(text = order.clinicalIndication)
        }

        HelixSection(title = "Order") {
            HelixFactCard {
                HelixMiniFieldPair(
                    startLabel = "Ordered",
                    startValue = ClinicalFormat.dateTime(order.orderedAt),
                    endLabel = "Facility",
                    endValue = context.facilityName ?: order.facilityId,
                    endMono = false,
                )
                HelixHairline()
                HelixMiniFieldPair(
                    startLabel = "Ordering physician",
                    startValue = order.orderingPhysician,
                    endLabel = "Location",
                    endValue = order.orderingLocation,
                    startMono = false,
                    endMono = false,
                )
                context.study?.let { study ->
                    HelixHairline()
                    HelixMiniFieldPair(
                        startLabel = "Acquired",
                        startValue = study.performedAt?.let { ClinicalFormat.dateTime(it) } ?: "Not yet",
                        endLabel = "Images",
                        endValue = "${study.imageCount} · ${study.seriesCount} series",
                    )
                }
            }
        }

        HelixSection(title = "Report") {
            val report = context.report
            if (report != null) {
                ReportCard(report = report, onOpen = { actions.openReport(report.id) })
            } else {
                ExamEmptyNote(
                    icon = Icons.Outlined.Schedule,
                    title = "Awaiting acquisition",
                    body = "The report appears here once images are acquired and dictated.",
                )
            }
        }

        ExamActions(
            study = context.study,
            onViewPatient = { actions.openPatient(order.patientId) },
        )
    }
}
