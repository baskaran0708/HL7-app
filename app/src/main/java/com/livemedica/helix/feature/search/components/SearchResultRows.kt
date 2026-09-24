package com.livemedica.helix.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixAvatar
import com.livemedica.helix.core.designsystem.component.HelixCard
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.HelixRowDivider
import com.livemedica.helix.core.designsystem.component.HelixSection
import com.livemedica.helix.core.designsystem.component.ModalityBlock
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.theme.reportStatusMeta
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.Study

/**
 * Result rows, one shape per entity.
 *
 * Each keeps the leading element its own list uses elsewhere — initials for a patient, a modality
 * block for anything imaging — so a result looks like the thing it will open.
 */

/** `PATIENTS · 3` heading over a card of hairline-separated rows. */
@Composable
fun SearchGroup(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    HelixSection(title = "$label · $count", modifier = modifier) {
        HelixCard(contentPadding = 0.dp) { content() }
    }
}

@Composable
fun SearchResultDivider() = HelixRowDivider(inset = 66.dp)

@Composable
fun PatientResultRow(patient: Patient, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    ResultRow(
        modifier = modifier,
        onOpen = onOpen,
        leading = { HelixAvatar(initials = patient.initials(), size = 38.dp) },
        title = patient.fullName,
        subtitle = "${patient.mrn} · ${ClinicalFormat.ageSex(patient.age, patient.sex)}",
    )
}

@Composable
fun OrderResultRow(order: Order, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    ResultRow(
        modifier = modifier,
        onOpen = onOpen,
        leading = { ModalityBlock(order.modality, size = 38.dp) },
        title = order.procedure.substringBefore(" w/"),
        subtitle = "${order.patientName} · ${order.accessionNumber}",
        trailing = { if (order.priority != Priority.ROUTINE) HelixPriorityBadge(order.priority) },
    )
}

@Composable
fun ReportResultRow(report: RadiologyReport, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val meta = reportStatusMeta(report.status)
    ResultRow(
        modifier = modifier,
        onOpen = onOpen,
        leading = { ModalityBlock(report.modality, size = 38.dp) },
        title = report.procedure.substringBefore(" w/"),
        subtitle = "${report.patientName} · ${meta.label}",
        subtitleColor = if (report.isCritical) HelixTheme.colors.critical else null,
    )
}

@Composable
fun StudyResultRow(study: Study, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    ResultRow(
        modifier = modifier,
        onOpen = onOpen,
        leading = { ModalityBlock(study.modality, size = 38.dp) },
        title = study.description,
        subtitle = "${study.accessionNumber} · ${study.seriesCount} series · ${study.imageCount} images",
    )
}

@Composable
fun AppointmentResultRow(
    appointment: Appointment,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResultRow(
        modifier = modifier,
        onOpen = onOpen,
        leading = {
            Text(
                text = ClinicalFormat.time(appointment.start),
                style = HelixTheme.typography.mono,
                color = HelixTheme.colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(38.dp),
            )
        },
        title = appointment.patientName,
        subtitle = "${appointment.modality.code} · ${appointment.room} · ${appointment.procedure}",
    )
}

@Composable
private fun ResultRow(
    onOpen: () -> Unit,
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    subtitleColor: androidx.compose.ui.graphics.Color? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = title,
                    style = HelixTheme.typography.label,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                trailing()
            }
            Text(
                text = subtitle,
                style = HelixTheme.typography.caption,
                color = subtitleColor ?: colors.textMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconSm),
        )
    }
}

/**
 * Initials for the avatar. Derived here rather than stored on [Patient] because it is a display
 * concern, and a name that arrives from HL7 as a single field still has to render something.
 */
private fun Patient.initials(): String {
    val parts = fullName.split(' ').filter { it.isNotBlank() }
    val letters = listOfNotNull(parts.firstOrNull(), parts.getOrNull(1))
        .joinToString("") { it.first().uppercase() }
    return letters.ifEmpty { "?" }
}
