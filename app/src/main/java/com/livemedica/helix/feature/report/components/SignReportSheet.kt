package com.livemedica.helix.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixActionPair
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.component.HelixPrimaryAction
import com.livemedica.helix.core.designsystem.component.HelixSecondaryAction
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.feature.report.SignatureState

/**
 * Sign-off confirmation.
 *
 * Signing is irreversible and carries the attending's licence, so it is never a bare tap on the
 * report: the sheet restates exactly *what* is being signed, *who* it will be attributed to, and —
 * for a critical result — that signing triggers notification of the ordering clinician. The design
 * prototype captured a drawn signature on a canvas; that is deferred until the backend can actually
 * store and verify a signature image, and the attestation is made in words instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignReportSheet(
    report: RadiologyReport,
    doctor: Doctor,
    signature: SignatureState,
    sheetState: SheetState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = HelixTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = colors.scrim,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column {
                Text(
                    text = "Sign report",
                    style = HelixTheme.typography.headline,
                    color = colors.text,
                )
                Text(
                    text = "${report.patientName} · ${report.accessionNumber}",
                    style = HelixTheme.typography.monoSmall,
                    color = colors.textMute,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                Text(
                    text = report.procedure,
                    style = HelixTheme.typography.label,
                    color = colors.textDim,
                    modifier = Modifier.padding(top = Spacing.xs + 2.dp),
                )
            }

            if (report.isCritical) {
                CriticalSignNotice()
            }

            AttributionBlock(report = report, doctor = doctor)

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 1.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.textMute,
                    modifier = Modifier.size(Dimens.iconSm),
                )
                Text(
                    text = "Signing attests that you have reviewed this report in full. The " +
                        "signature is audit-logged against your licence and cannot be withdrawn.",
                    style = HelixTheme.typography.caption,
                    color = colors.textMute,
                )
            }

            if (signature is SignatureState.Failed) {
                SignFailureNotice(message = signature.message)
            }

            HelixHairline()

            HelixActionPair(
                modifier = Modifier.padding(bottom = Spacing.lg),
                start = {
                    HelixSecondaryAction(
                        label = "Cancel",
                        onClick = onDismiss,
                        enabled = signature !is SignatureState.Submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                end = {
                    HelixPrimaryAction(
                        // The label states the consequence, not the gesture: this is the last
                        // screen before the report leaves the radiologist's hands.
                        label = if (signature is SignatureState.Submitting) "Signing…" else "Sign & finalise",
                        icon = Icons.Outlined.Draw,
                        onClick = onConfirm,
                        enabled = signature !is SignatureState.Submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }
    }
}

@Composable
private fun AttributionBlock(report: RadiologyReport, doctor: Doctor) {
    val colors = HelixTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(colors.elevated)
            .padding(Spacing.lg),
    ) {
        HelixOverline("Signed as")
        Text(
            text = report.radiologist,
            style = HelixTheme.typography.label.copy(fontWeight = FontWeight.Bold),
            color = colors.text,
            modifier = Modifier.padding(top = Spacing.sm - 2.dp),
        )
        Text(
            text = listOf(doctor.specialty, doctor.licenceIdentifier)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            style = HelixTheme.typography.monoSmall,
            color = colors.textMute,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun CriticalSignNotice() {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.critical.copy(alpha = NOTICE_FILL_ALPHA))
            .border(Dimens.hairline, colors.critical.copy(alpha = NOTICE_STROKE_ALPHA), shape)
            .padding(Spacing.md + 1.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 1.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = colors.critical,
            modifier = Modifier.size(Dimens.iconSm),
        )
        Text(
            text = "Critical result. Signing notifies the ordering clinician and opens a " +
                "read-back record against this accession.",
            style = HelixTheme.typography.caption,
            color = colors.textDim,
        )
    }
}

@Composable
private fun SignFailureNotice(message: String) {
    val colors = HelixTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(colors.critical.copy(alpha = NOTICE_FILL_ALPHA))
            .padding(Spacing.md + 1.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 1.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = colors.critical,
            modifier = Modifier.size(Dimens.iconSm),
        )
        Text(
            text = "$message The report has not been signed.",
            style = HelixTheme.typography.caption,
            color = colors.critical,
        )
    }
}

private const val NOTICE_FILL_ALPHA = 0.07f
private const val NOTICE_STROKE_ALPHA = 0.22f
