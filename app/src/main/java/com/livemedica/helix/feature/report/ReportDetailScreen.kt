package com.livemedica.helix.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemedica.helix.core.designsystem.component.HelixDetailRow
import com.livemedica.helix.core.designsystem.component.HelixFactCard
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixPrimaryAction
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.HelixTopBar
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.navigation.HelixNavActions
import com.livemedica.helix.core.ui.HelixStateHost
import com.livemedica.helix.core.ui.dataOrNull
import com.livemedica.helix.feature.report.components.CriticalProtocolCallout
import com.livemedica.helix.feature.report.components.ReportPatientHeader
import com.livemedica.helix.feature.report.components.ReportProseSection
import com.livemedica.helix.feature.report.components.ReportRadiologistCard
import com.livemedica.helix.feature.report.components.ReportSignatureBlock
import com.livemedica.helix.feature.report.components.ReportStatusCard
import com.livemedica.helix.feature.report.components.ReportStudyCard
import com.livemedica.helix.feature.report.components.SignReportSheet
import kotlinx.coroutines.launch

/**
 * Report detail — read the study, then sign it.
 *
 * The screen is a single column in clinical reading order: who, what, why, what was compared, how
 * it was acquired, what was seen, what it means, who read it, where it stands, and the signature.
 * Sign Report is the only filled action on the page and exists only while the report is unsigned;
 * once signed, the page becomes a record rather than a task.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    actions: HelixNavActions,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signature by viewModel.signature.collectAsStateWithLifecycle()

    var sheetVisible by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val dismissSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            sheetVisible = false
            viewModel.acknowledgeSignature()
        }
    }

    // A successful signature closes the sheet itself: the clinician's next act is reading the
    // signed state on the report, not dismissing a dialog.
    LaunchedEffect(signature) {
        if (signature is SignatureState.Signed && sheetVisible) {
            sheetState.hide()
            sheetVisible = false
            viewModel.acknowledgeSignature()
        }
    }

    // Held here so the top bar can name the patient and the sheet can read the current report
    // without either of them re-deriving it from the state wrapper.
    val model = state.dataOrNull()

    Column(Modifier.fillMaxSize()) {
        HelixTopBar(
            title = "Report",
            subtitle = model?.report?.patientName,
            onBack = actions.navigateBack,
        )
        HelixStateHost(
            modifier = Modifier.weight(1f),
            state = state,
            onRetry = viewModel::refresh,
            emptyTitle = "Report unavailable",
            emptyBody = "This report could not be loaded.",
        ) { model ->
            ReportContent(
                model = model,
                onSign = { sheetVisible = true },
                onOpenPatient = { actions.openPatient(model.report.patientId) },
                onOpenStudy = { actions.openStudy(model.report.studyId) },
                onOpenOrder = { model.order?.let { actions.openOrder(it.id) } },
            )
        }
    }

    if (sheetVisible && model != null) {
        SignReportSheet(
            report = model.report,
            doctor = model.doctor,
            signature = signature,
            sheetState = sheetState,
            onConfirm = viewModel::signReport,
            onDismiss = dismissSheet,
        )
    }
}

@Composable
private fun ReportContent(
    model: ReportUiModel,
    onSign: () -> Unit,
    onOpenPatient: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenOrder: () -> Unit,
) {
    val report = model.report

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl - 4.dp),
    ) {
        ReportPatientHeader(report = report, onOpenPatient = onOpenPatient)

        HelixSection(title = "Study information") {
            ReportStudyCard(report = report, order = model.order)
        }

        if (report.isCritical) {
            CriticalProtocolCallout()
        }

        ReportProseSection(label = "Clinical indication", body = report.clinicalIndication)
        ReportProseSection(label = "Comparison", body = report.comparison)
        ReportProseSection(label = "Technique", body = report.technique)
        ReportProseSection(label = "Findings", body = report.findings)
        ReportProseSection(label = "Impression", body = report.impression, emphasis = true)

        HelixHairline()

        HelixSection(title = "Radiologist") {
            ReportRadiologistCard(report = report, doctor = model.doctor)
        }

        HelixSection(title = "Report status") {
            ReportStatusCard(report = report)
        }

        HelixSection(title = "Signature") {
            ReportSignatureBlock(report = report, doctor = model.doctor)
            if (!report.isSigned) {
                HelixPrimaryAction(
                    label = "Sign report",
                    icon = Icons.Outlined.Draw,
                    onClick = onSign,
                    // The section already spaces its children; the button only needs the width.
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        HelixSection(title = "Related") {
            HelixFactCard {
                HelixDetailRow(
                    icon = Icons.Outlined.Person,
                    title = "Patient record",
                    subtitle = "${report.patientName} · ${report.mrn}",
                    onClick = onOpenPatient,
                )
                HelixHairline(Modifier.padding(start = Spacing.lg))
                HelixDetailRow(
                    icon = Icons.Outlined.Image,
                    title = "Study",
                    subtitle = "${report.procedure} · ${report.accessionNumber}",
                    onClick = onOpenStudy,
                )
                if (model.order != null) {
                    HelixHairline(Modifier.padding(start = Spacing.lg))
                    HelixDetailRow(
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        title = "Original order",
                        subtitle = "${model.order.id} · ${model.order.orderingPhysician}",
                        onClick = onOpenOrder,
                    )
                }
            }
        }
    }
}
