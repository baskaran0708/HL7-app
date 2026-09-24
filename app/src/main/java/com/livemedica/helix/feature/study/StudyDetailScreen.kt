package com.livemedica.helix.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Text
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
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.theme.HelixTheme
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
 * Study detail — the acquired imaging.
 *
 * Where the order screen leads with the request, this one leads with the acquisition: what exists
 * in the archive, how much of it there is, and the handle a viewer will need to open it.
 */
@Composable
fun StudyDetailScreen(
    actions: HelixNavActions,
    viewModel: StudyDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val model = state.dataOrNull()

    Column(Modifier.fillMaxSize()) {
        HelixTopBar(
            title = "Study",
            subtitle = model?.study?.accessionNumber,
            onBack = actions.navigateBack,
        )
        HelixStateHost(
            modifier = Modifier.weight(1f),
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Study not found",
            emptyBody = "This study is not available on this device.",
        ) { context ->
            StudyContent(context = context, actions = actions)
        }
    }
}

@Composable
private fun StudyContent(context: ExamContext, actions: HelixNavActions) {
    val study = context.study ?: return
    val order = context.order
    val statusMeta = orderStatusMeta(order?.status ?: com.livemedica.helix.domain.model.OrderStatus.COMPLETED)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl - 6.dp),
    ) {
        ExamPatientStrip(
            patient = context.patient,
            fallbackName = order?.patientName.orEmpty(),
            fallbackMrn = order?.mrn.orEmpty(),
            allergies = context.activeAllergies,
            onOpenPatient = order?.let { { actions.openPatient(it.patientId) } },
        )

        ExamHeadline(
            modality = study.modality,
            priority = order?.priority ?: com.livemedica.helix.domain.model.Priority.ROUTINE,
            statusLabel = statusMeta.label,
            statusColor = statusMeta.color,
            procedure = study.description,
            accessionNumber = study.accessionNumber,
            cptCode = order?.cptCode,
        )

        order?.let {
            HelixSection(title = "Clinical indication") {
                HelixClinicalIndication(text = it.clinicalIndication)
            }
        }

        HelixSection(title = "Acquisition") {
            HelixFactCard {
                HelixMiniFieldPair(
                    startLabel = "Performed",
                    startValue = study.performedAt?.let { ClinicalFormat.dateTime(it) } ?: "Not yet acquired",
                    endLabel = "Facility",
                    endValue = context.facilityName ?: order?.facilityId ?: "—",
                    endMono = false,
                )
                HelixHairline()
                HelixMiniFieldPair(
                    startLabel = "Series",
                    startValue = study.seriesCount.toString(),
                    endLabel = "Images",
                    endValue = study.imageCount.toString(),
                )
                order?.let {
                    HelixHairline()
                    HelixMiniFieldPair(
                        startLabel = "Ordering physician",
                        startValue = it.orderingPhysician,
                        endLabel = "Ordered",
                        endValue = ClinicalFormat.dateTime(it.orderedAt),
                        startMono = false,
                    )
                }
                HelixHairline()
                // Carried in full because it is the handle the PACS hand-off will use, and a
                // truncated UID is useless to anyone reading it out to a technologist.
                Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    HelixOverline("Study instance UID")
                    Text(
                        text = study.studyInstanceUid,
                        style = HelixTheme.typography.monoSmall,
                        color = HelixTheme.colors.textDim,
                        modifier = Modifier.padding(top = Spacing.xs + 1.dp),
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
                    title = "Not yet reported",
                    body = "The report appears here once the study has been dictated.",
                )
            }
        }

        ExamActions(
            study = study,
            onViewPatient = order?.let { { actions.openPatient(it.patientId) } },
        )
    }
}
