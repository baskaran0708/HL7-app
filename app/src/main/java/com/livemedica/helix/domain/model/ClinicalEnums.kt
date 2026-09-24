package com.livemedica.helix.domain.model

/**
 * Imaging modality, using the DICOM two-letter codes radiologists actually read on a worklist.
 */
enum class Modality(val code: String, val label: String) {
    CT("CT", "Computed Tomography"),
    MR("MR", "Magnetic Resonance"),
    XR("XR", "Radiography"),
    US("US", "Ultrasound"),
    MG("MG", "Mammography"),
    NM("NM", "Nuclear Medicine"),
    PT("PT", "PET"),
    FL("FL", "Fluoroscopy"),
}

/**
 * Order urgency. Rendered with both an icon and its [label] — never colour alone — so the
 * distinction survives colour-blindness and greyscale.
 */
enum class Priority(val label: String) {
    STAT("STAT"),
    URGENT("URGENT"),
    ROUTINE("ROUTINE"),
}

enum class OrderStatus(val label: String) {
    PENDING("PENDING"),
    IN_PROGRESS("IN PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
}

/**
 * Report lifecycle. [PRELIMINARY] and [PENDING_SIGNATURE] are distinct: a preliminary read exists
 * but is not yet submitted, whereas pending-signature is finished text awaiting the attending's
 * signature — the state the Today screen counts as "unsigned reports".
 */
enum class ReportStatus(val label: String) {
    DRAFT("DRAFT"),
    PRELIMINARY("PRELIMINARY"),
    PENDING_SIGNATURE("PENDING SIGN-OFF"),
    SIGNED("SIGNED"),
    ADDENDUM("ADDENDUM"),
}

enum class AppointmentStatus(val label: String) {
    SCHEDULED("SCHEDULED"),
    CHECKED_IN("CHECKED IN"),
    IN_PROGRESS("IN PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    NO_SHOW("NO SHOW"),
}

enum class Sex(val code: String) {
    FEMALE("F"),
    MALE("M"),
    OTHER("X"),
}

enum class NotificationCategory(val label: String) {
    CRITICAL("CRITICAL"),
    RESULTS("RESULTS"),
    APPOINTMENTS("APPOINTMENTS"),
    ORDERS("ORDERS"),
    SYSTEM("SYSTEM"),
}

/** HL7 v2 message types surfaced on the operational Integration screen. */
enum class Hl7MessageType(val label: String) {
    ADT("ADT"),
    SIU("SIU"),
    ORM("ORM"),
    ORU("ORU"),
}

enum class Hl7AckStatus(val label: String) {
    ACK("ACK"),
    DELIVERED("DELIVERED"),
    PENDING("PENDING"),
    NAK("NAK"),
}

enum class InterfaceState(val label: String) {
    ONLINE("ONLINE"),
    DEGRADED("DEGRADED"),
    OFFLINE("OFFLINE"),
}
