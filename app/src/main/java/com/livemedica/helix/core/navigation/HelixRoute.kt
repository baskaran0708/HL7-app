package com.livemedica.helix.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes.
 *
 * Using `@Serializable` classes rather than string routes means a destination's arguments are
 * checked by the compiler — a screen can't be reached without the id it needs, and renaming an
 * argument is a refactor rather than a runtime crash.
 */
sealed interface HelixRoute {

    /** Tab graphs. Each top-level tab owns its own back stack. */
    @Serializable data object TodayGraph : HelixRoute
    @Serializable data object ScheduleGraph : HelixRoute
    @Serializable data object WorklistGraph : HelixRoute
    @Serializable data object ResultsGraph : HelixRoute
    @Serializable data object MoreGraph : HelixRoute

    /** Tab roots. */
    @Serializable data object Today : HelixRoute
    @Serializable data object Schedule : HelixRoute
    @Serializable data object Worklist : HelixRoute
    @Serializable data object Results : HelixRoute
    @Serializable data object More : HelixRoute

    /** Details — reachable from any tab, so they are declared once at the host level. */
    @Serializable data class Patient(val patientId: String) : HelixRoute
    @Serializable data class Order(val orderId: String) : HelixRoute
    @Serializable data class Study(val studyId: String) : HelixRoute
    @Serializable data class Report(val reportId: String) : HelixRoute
    @Serializable data class Appointment(val appointmentId: String) : HelixRoute

    /** Global destinations. */
    @Serializable data object Notifications : HelixRoute
    @Serializable data object Search : HelixRoute
    @Serializable data object Integration : HelixRoute
    @Serializable data object Profile : HelixRoute
    @Serializable data object Settings : HelixRoute
}

/**
 * Deep-link scheme, declared now so push notifications and future PACS hand-offs work without
 * re-architecting navigation.
 */
object HelixDeepLinks {
    const val SCHEME = "helix"
    const val PATIENT = "$SCHEME://patient/{patientId}"
    const val ORDER = "$SCHEME://order/{orderId}"
    const val STUDY = "$SCHEME://study/{studyId}"
    const val REPORT = "$SCHEME://report/{reportId}"
    const val APPOINTMENT = "$SCHEME://appointment/{appointmentId}"
}
