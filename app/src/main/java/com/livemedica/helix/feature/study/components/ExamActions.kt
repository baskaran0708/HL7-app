package com.livemedica.helix.feature.study.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.livemedica.helix.core.designsystem.component.HelixActionPair
import com.livemedica.helix.core.designsystem.component.HelixPrimaryAction
import com.livemedica.helix.core.designsystem.component.HelixSecondaryAction
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.Study

/**
 * The actions available on an exam.
 *
 * "Open images" is the primary action and the only one that leaves the app — see [PacsHandoff] for
 * the deliberately empty viewer seam. When no viewer is configured the action is disabled and the
 * reason is stated, rather than the button silently doing nothing.
 */
@Composable
fun ExamActions(
    study: Study?,
    onViewPatient: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = HelixTheme.colors
    val pacsTarget = PacsHandoff.resolve(study)
    // Notes are a Phase 2 write path; the control exists now so the layout is final, and it says
    // so plainly instead of pretending to save anything.
    var noteRequested by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HelixActionPair(
            start = {
                HelixPrimaryAction(
                    label = pacsTarget?.label ?: "Open images",
                    icon = Icons.Outlined.Image,
                    onClick = { /* Wired when PacsHandoff.resolve returns a target. */ },
                    enabled = pacsTarget != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            end = {
                HelixSecondaryAction(
                    label = "Add note",
                    icon = Icons.AutoMirrored.Outlined.NoteAdd,
                    onClick = { noteRequested = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )

        if (onViewPatient != null) {
            HelixSecondaryAction(
                label = "View patient record",
                icon = Icons.Outlined.Person,
                onClick = onViewPatient,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (pacsTarget == null) {
            Text(
                text = PacsHandoff.UNAVAILABLE_REASON,
                style = HelixTheme.typography.caption,
                color = colors.textMute,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }

        if (noteRequested) {
            Text(
                text = "Notes are not yet synchronised to the record. Dictate into the RIS for now.",
                style = HelixTheme.typography.caption,
                color = colors.warning,
            )
        }
    }
}
