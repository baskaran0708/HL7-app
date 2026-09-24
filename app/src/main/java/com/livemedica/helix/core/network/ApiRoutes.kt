package com.livemedica.helix.core.network

/**
 * The shape of the future AWS backend.
 *
 * Declared as constants, in one place, so no URL is ever written inline in a repository — and so
 * the contract can be reviewed against the backend before a single call is implemented.
 *
 * The base URL is deliberately absent: it will arrive as a build-config field per build type, never
 * as a literal in source. Nothing in Phase 1 performs a network call.
 */
object ApiRoutes {
    const val VERSION = "v1"
    private const val BASE = "api/$VERSION"

    const val LOGIN = "$BASE/auth/login"
    const val REFRESH = "$BASE/auth/refresh"

    const val DOCTOR_PROFILE = "$BASE/doctor/profile"
    const val DOCTOR_APPOINTMENTS = "$BASE/doctor/appointments"
    const val DOCTOR_APPOINTMENT = "$BASE/doctor/appointments/{id}"
    const val DOCTOR_ORDERS = "$BASE/doctor/orders"
    const val DOCTOR_ORDER = "$BASE/doctor/orders/{id}"
    const val DOCTOR_RESULTS = "$BASE/doctor/results"
    const val DOCTOR_RESULT = "$BASE/doctor/results/{id}"

    const val PATIENT = "$BASE/patients/{id}"
    const val SEARCH = "$BASE/search"

    const val NOTIFICATIONS = "$BASE/notifications"
    const val NOTIFICATION_READ = "$BASE/notifications/{id}/read"

    const val SIGN_REPORT = "$BASE/reports/{id}/sign"

    const val INTEGRATION_STATUS = "$BASE/integration/status"
}
