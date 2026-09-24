package com.livemedica.helix.core.utils

import com.livemedica.helix.domain.model.Sex
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Clinical string formatting.
 *
 * Centralised so `76F`, `09:48` and `SUN · 24 MAY` are produced identically everywhere — small
 * inconsistencies in these read as sloppiness on a clinical screen.
 */
object ClinicalFormat {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("EEE · dd MMM", Locale.US)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.US)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.US)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)

    fun time(value: LocalDateTime): String = value.format(timeFormatter)
    fun time(value: LocalTime): String = value.format(timeFormatter)

    /** `SUN · 24 MAY`, as shown under the Today greeting. */
    fun dayMonth(value: LocalDate): String = value.format(dayMonthFormatter).uppercase(Locale.US)

    fun fullDate(value: LocalDate): String = value.format(fullDateFormatter)
    fun shortDate(value: LocalDate): String = value.format(shortDateFormatter)
    fun dateTime(value: LocalDateTime): String = value.format(dateTimeFormatter)

    /** `76F` — the compact form radiologists read on a worklist. */
    fun ageSex(age: Int, sex: Sex): String = "$age${sex.code}"

    /**
     * Greeting by time of day. Uses the device clock rather than a fixed string so the app reads
     * correctly across a night shift.
     */
    fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
        in 0..11 -> "Morning"
        in 12..16 -> "Afternoon"
        else -> "Evening"
    }

    /**
     * Masks an identifier for any context that may be logged or shared.
     *
     * Nothing in Phase 1 logs patient data at all; this exists so that when diagnostics are added
     * there is already a correct way to refer to a record without leaking PHI.
     */
    fun maskIdentifier(value: String): String =
        if (value.length <= VISIBLE_IDENTIFIER_CHARS) "•".repeat(value.length)
        else "${"•".repeat(value.length - VISIBLE_IDENTIFIER_CHARS)}${value.takeLast(VISIBLE_IDENTIFIER_CHARS)}"

    private const val VISIBLE_IDENTIFIER_CHARS = 4
}
