package com.livemedica.helix.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.livemedica.helix.domain.model.AppointmentStatus
import com.livemedica.helix.domain.model.Hl7AckStatus
import com.livemedica.helix.domain.model.InterfaceState
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.OrderStatus
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.ReportStatus

/**
 * The design's "modality colour thread": each modality keeps the same hue on every surface, so a
 * clinician learns to spot MR or CT by colour without reading the tag.
 *
 * Two ramps, exactly as specified: dark-mode hues are luminous, light-mode hues are darkened so the
 * small bold mono tags still clear 4.5:1 on white cards and the tinted canvas.
 */
private data class ModalityRamp(val dark: Color, val light: Color)

private val modalityRamps = mapOf(
    Modality.CT to ModalityRamp(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
    Modality.MR to ModalityRamp(Color(0xFF38BDF8), Color(0xFF0369A1)),
    Modality.US to ModalityRamp(Color(0xFF34D399), Color(0xFF047857)),
    Modality.XR to ModalityRamp(Color(0xFFFBBF24), Color(0xFF92400E)),
    Modality.MG to ModalityRamp(Color(0xFFF472B6), Color(0xFFBE185D)),
    Modality.NM to ModalityRamp(Color(0xFFA78BFA), Color(0xFF6D28D9)),
    Modality.PT to ModalityRamp(Color(0xFFFB923C), Color(0xFFC2410C)),
    Modality.FL to ModalityRamp(Color(0xFF38BDF8), Color(0xFF0369A1)),
)

@Composable
@ReadOnlyComposable
fun modalityColor(modality: Modality): Color {
    val colors = HelixTheme.colors
    val ramp = modalityRamps[modality] ?: modalityRamps.getValue(Modality.CT)
    return if (colors.isDark) ramp.dark else ramp.light
}

/**
 * Priority and status descriptors.
 *
 * Every one carries a [label] alongside its colour. Components render both, so priority and status
 * are never communicated by colour alone — required for colour-blind users and greyscale displays.
 */
data class ClinicalMeta(val color: Color, val label: String)

@Composable
@ReadOnlyComposable
fun priorityMeta(priority: Priority): ClinicalMeta {
    val c = HelixTheme.colors
    return when (priority) {
        Priority.STAT -> ClinicalMeta(c.critical, "STAT")
        Priority.URGENT -> ClinicalMeta(c.warning, "Urgent")
        Priority.ROUTINE -> ClinicalMeta(c.textMute, "Routine")
    }
}

@Composable
@ReadOnlyComposable
fun orderStatusMeta(status: OrderStatus): ClinicalMeta {
    val c = HelixTheme.colors
    return when (status) {
        OrderStatus.PENDING -> ClinicalMeta(c.warning, "Pending read")
        OrderStatus.IN_PROGRESS -> ClinicalMeta(c.info, "In progress")
        OrderStatus.COMPLETED -> ClinicalMeta(c.success, "Completed")
        OrderStatus.CANCELLED -> ClinicalMeta(c.textMute, "Cancelled")
    }
}

@Composable
@ReadOnlyComposable
fun reportStatusMeta(status: ReportStatus): ClinicalMeta {
    val c = HelixTheme.colors
    return when (status) {
        ReportStatus.DRAFT -> ClinicalMeta(c.textMute, "Draft")
        ReportStatus.PRELIMINARY -> ClinicalMeta(c.warning, "Preliminary")
        ReportStatus.PENDING_SIGNATURE -> ClinicalMeta(c.warning, "Awaiting sign-off")
        ReportStatus.SIGNED -> ClinicalMeta(c.success, "Signed")
        ReportStatus.ADDENDUM -> ClinicalMeta(c.info, "Amended")
    }
}

@Composable
@ReadOnlyComposable
fun appointmentStatusMeta(status: AppointmentStatus): ClinicalMeta {
    val c = HelixTheme.colors
    return when (status) {
        AppointmentStatus.SCHEDULED -> ClinicalMeta(c.textMute, "Scheduled")
        AppointmentStatus.CHECKED_IN -> ClinicalMeta(c.info, "Arrived")
        AppointmentStatus.IN_PROGRESS -> ClinicalMeta(c.info, "In progress")
        AppointmentStatus.COMPLETED -> ClinicalMeta(c.success, "Completed")
        AppointmentStatus.CANCELLED -> ClinicalMeta(c.textMute, "Cancelled")
        AppointmentStatus.NO_SHOW -> ClinicalMeta(c.warning, "No-show")
    }
}

@Composable
@ReadOnlyComposable
fun ackStatusMeta(status: Hl7AckStatus): ClinicalMeta {
    val c = HelixTheme.colors
    return when (status) {
        Hl7AckStatus.ACK -> ClinicalMeta(c.success, "ACK")
        Hl7AckStatus.DELIVERED -> ClinicalMeta(c.info, "DELIVERED")
        Hl7AckStatus.PENDING -> ClinicalMeta(c.warning, "PENDING")
        Hl7AckStatus.NAK -> ClinicalMeta(c.critical, "NAK")
    }
}

@Composable
@ReadOnlyComposable
fun interfaceStateMeta(state: InterfaceState): ClinicalMeta {
    val c = HelixTheme.colors
    return when (state) {
        InterfaceState.ONLINE -> ClinicalMeta(c.success, "Online")
        InterfaceState.DEGRADED -> ClinicalMeta(c.warning, "Degraded")
        InterfaceState.OFFLINE -> ClinicalMeta(c.critical, "Offline")
    }
}
