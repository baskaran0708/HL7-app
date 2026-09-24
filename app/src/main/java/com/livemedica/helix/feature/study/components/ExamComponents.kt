package com.livemedica.helix.feature.study.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixAvatar
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixFactCard
import com.livemedica.helix.core.designsystem.component.HelixHairline
import com.livemedica.helix.core.designsystem.component.HelixPriorityBadge
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.component.ModalityTag
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.Priority

/**
 * Pieces shared by Order, Study and Appointment detail.
 *
 * All three screens open with the same two blocks — who the patient is, and what the exam is —
 * because a clinician arriving from a notification needs to confirm both before reading anything
 * else, regardless of which entity they happened to land on.
 */

/**
 * Patient strip: identity, allergies, and a route into the full record.
 *
 * Allergies are carried here rather than left to the patient screen precisely because this is where
 * a contrast decision gets made.
 */
@Composable
fun ExamPatientStrip(
    patient: Patient?,
    fallbackName: String,
    fallbackMrn: String,
    allergies: List<String>,
    onOpenPatient: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val name = patient?.fullName ?: fallbackName

    HelixFactCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.touchTargetMin)
                .then(if (onOpenPatient != null) Modifier.clickable(onClick = onOpenPatient) else Modifier)
                .padding(horizontal = Spacing.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            HelixAvatar(initials = name.initials(), size = Dimens.avatarMd + 8.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = HelixTheme.typography.headline,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 1.dp),
                ) {
                    patient?.let {
                        Text(
                            text = ClinicalFormat.ageSex(it.age, it.sex),
                            style = HelixTheme.typography.monoSmall,
                            color = colors.textMute,
                        )
                        HelixDot(color = colors.textMute.copy(alpha = 0.6f), size = 3.dp)
                    }
                    Text(
                        text = patient?.mrn ?: fallbackMrn,
                        style = HelixTheme.typography.monoSmall,
                        color = colors.textMute,
                    )
                }
            }
            if (onOpenPatient != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textMute,
                    modifier = Modifier.size(Dimens.iconMd),
                )
            }
        }

        if (allergies.isNotEmpty()) {
            HelixHairline()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = colors.warning,
                    modifier = Modifier.size(Dimens.iconSm),
                )
                Text(
                    text = allergies.joinToString(" · "),
                    style = HelixTheme.typography.caption,
                    color = colors.warning,
                )
            }
        }
    }
}

/**
 * Exam headline: modality, priority, status, the procedure name, and the two identifiers a
 * clinician reads out on the phone — CPT and accession.
 */
@Composable
fun ExamHeadline(
    modality: Modality,
    priority: Priority,
    statusLabel: String,
    statusColor: Color,
    procedure: String,
    accessionNumber: String?,
    cptCode: String?,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.sm + 2.dp),
        ) {
            ModalityTag(modality)
            HelixPriorityBadge(priority)
            HelixStatusChip(label = statusLabel, color = statusColor)
        }
        Text(text = procedure, style = HelixTheme.typography.title, color = colors.text)
        Row(
            modifier = Modifier.padding(top = Spacing.sm - 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            cptCode?.let {
                Text(text = "CPT $it", style = HelixTheme.typography.mono, color = colors.textDim)
            }
            if (cptCode != null && accessionNumber != null) {
                HelixDot(color = colors.textMute, size = 3.dp)
            }
            accessionNumber?.let {
                Text(text = it, style = HelixTheme.typography.mono, color = colors.textDim)
            }
        }
    }
}

/**
 * An inline "nothing here yet" note.
 *
 * Deliberately not `HelixEmptyState`: that one fills the viewport, which is right for a whole
 * screen and wrong for a section inside a scrolling column.
 */
@Composable
fun ExamEmptyNote(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textMute,
            modifier = Modifier.size(Dimens.iconMd),
        )
        Column(Modifier.weight(1f)) {
            Text(text = title, style = HelixTheme.typography.label, color = colors.text)
            Text(
                text = body,
                style = HelixTheme.typography.caption,
                color = colors.textMute,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

private fun String.initials(): String = split(' ')
    .filter { it.isNotBlank() && it.length > 1 }
    .take(2)
    .joinToString("") { it.first().uppercase() }
