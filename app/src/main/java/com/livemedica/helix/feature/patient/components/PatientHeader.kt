package com.livemedica.helix.feature.patient.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixAvatar
import com.livemedica.helix.core.designsystem.component.HelixDot
import com.livemedica.helix.core.designsystem.component.HelixOverline
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Dimens
import com.livemedica.helix.core.designsystem.token.Radius
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.Sex

/**
 * Patient identity.
 *
 * Name, age, sex and MRN sit together on one line because that quartet is how a patient is
 * verified out loud before a contrast injection — splitting them across a profile-style layout
 * would make the check slower, not prettier.
 */
@Composable
fun PatientIdentity(
    patient: Patient,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HelixAvatar(initials = patient.initials, size = Dimens.avatarLg)
        Column(Modifier.weight(1f)) {
            Text(text = patient.fullName, style = HelixTheme.typography.title, color = colors.text)
            Row(
                modifier = Modifier.padding(top = Spacing.xs + 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = "${patient.age} y/o ${patient.sex.longLabel}",
                    style = HelixTheme.typography.label,
                    color = colors.textDim,
                )
                HelixDot(color = colors.textMute, size = 3.dp)
                Text(text = patient.mrn, style = HelixTheme.typography.mono, color = colors.textDim)
            }
            Text(
                text = "DOB ${ClinicalFormat.shortDate(patient.dateOfBirth)}",
                style = HelixTheme.typography.monoSmall,
                color = colors.textMute,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/**
 * Allergy banner.
 *
 * Sits directly under identity and above everything else on the record: a contrast allergy has to
 * be seen before the clinician decides anything, so it is never collapsed behind a tab or a
 * "more" affordance. Amber, listed in full, one allergy per line.
 */
@Composable
fun PatientAllergyBanner(
    allergies: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val shape = RoundedCornerShape(Radius.lg - 3.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.warning.copy(alpha = BANNER_FILL_ALPHA))
            .border(Dimens.hairline, colors.warning.copy(alpha = BANNER_STROKE_ALPHA), shape)
            .padding(horizontal = 14.dp, vertical = Spacing.md)
            .semantics {
                contentDescription = "Allergies: ${allergies.joinToString(". ")}"
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(Dimens.iconSm),
            )
            HelixOverline("Allergies", color = colors.warning)
        }
        Column(
            modifier = Modifier.padding(top = Spacing.sm - 1.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            allergies.forEach { allergy ->
                Text(text = allergy, style = HelixTheme.typography.body, color = colors.textDim)
            }
        }
    }
}

/** Initials for the avatar, derived rather than stored — the API will not supply them. */
private val Patient.initials: String
    get() = fullName.split(' ')
        .filter { it.isNotBlank() && it.length > 1 }
        .take(2)
        .joinToString("") { it.first().uppercase() }

private val Sex.longLabel: String
    get() = when (this) {
        Sex.FEMALE -> "Female"
        Sex.MALE -> "Male"
        Sex.OTHER -> "Other"
    }

private const val BANNER_FILL_ALPHA = 0.08f
private const val BANNER_STROKE_ALPHA = 0.25f
