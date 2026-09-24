package com.livemedica.helix.feature.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.livemedica.helix.core.designsystem.component.HelixAvatar
import com.livemedica.helix.core.designsystem.component.HelixCard
import com.livemedica.helix.core.designsystem.component.HelixStatusChip
import com.livemedica.helix.core.designsystem.theme.HelixTheme
import com.livemedica.helix.core.designsystem.token.Spacing
import com.livemedica.helix.domain.model.Doctor

/** Avatar, name, specialty and on-call state, centred as the design's profile header. */
@Composable
fun ProfileHeader(doctor: Doctor, modifier: Modifier = Modifier) {
    val colors = HelixTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HelixAvatar(initials = doctor.initials, size = 76.dp)
        Text(
            text = doctor.displayName,
            style = HelixTheme.typography.title,
            color = colors.text,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = doctor.specialty,
            style = HelixTheme.typography.body,
            color = colors.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        HelixStatusChip(
            label = if (doctor.isOnCall) "On call" else "Off call",
            color = if (doctor.isOnCall) colors.success else colors.textMute,
            modifier = Modifier.padding(top = Spacing.sm + 2.dp),
        )
    }
}

/**
 * A credential field.
 *
 * Identifiers are mono so an NPI or staff ID can be read back digit by digit over the phone —
 * which is exactly what happens when a result has to be communicated urgently.
 */
@Composable
fun CredentialField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isIdentifier: Boolean = false,
) {
    val colors = HelixTheme.colors
    Column(modifier = modifier.padding(Spacing.lg)) {
        Text(text = label.uppercase(), style = HelixTheme.typography.overline, color = colors.textMute)
        Text(
            text = value,
            style = if (isIdentifier) HelixTheme.typography.mono else HelixTheme.typography.label,
            color = colors.text,
            modifier = Modifier.padding(top = Spacing.xs + 2.dp),
        )
    }
}

/** Two credential fields side by side, matching the design's 2-column credentials grid. */
@Composable
fun CredentialRow(
    modifier: Modifier = Modifier,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(Modifier.weight(1f)) { start() }
        Column(Modifier.weight(1f)) { end() }
    }
}

/**
 * Sign-out confirmation.
 *
 * The body says plainly that nothing is wired up yet. A dialog that silently does nothing is worse
 * than no dialog — a clinician handing over a shared device has to know whether they are signed out.
 */
@Composable
fun SignOutDialog(onDismiss: () -> Unit) {
    val colors = HelixTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign out", style = HelixTheme.typography.headline, color = colors.text) },
        text = {
            Text(
                text = "Authentication isn't connected in this build, so this won't end your " +
                    "session yet. Sign-out arrives with the identity provider.",
                style = HelixTheme.typography.body,
                color = colors.textDim,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood", style = HelixTheme.typography.label, color = colors.primary)
            }
        },
        containerColor = colors.surface,
    )
}

/** Card wrapper used by every profile section, kept here so the padding rule is stated once. */
@Composable
fun ProfileCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    HelixCard(modifier = modifier, contentPadding = 0.dp, content = content)
}
