package com.livemedica.helix.feature.schedule

import com.livemedica.helix.domain.model.Appointment
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

/**
 * Turns a day's appointments into positioned timeline slots.
 *
 * Kept out of the ViewModel because it is pure geometry with no state, and out of the composable
 * because it is the screen's one genuinely non-trivial algorithm — it belongs somewhere it can be
 * reasoned about and unit-tested on its own.
 */
internal object ScheduleTimelineBuilder {

    /**
     * Lays the day out, resolving double-bookings into side-by-side lanes.
     *
     * Appointments are walked in start order and accumulated into a cluster of items that overlap
     * transitively; each cluster is then column-packed against the earliest free lane. That is what
     * makes a scanner booked twice at 09:15 render as two half-width blocks instead of one hiding
     * the other.
     */
    fun build(appointments: List<Appointment>): ScheduleTimeline {
        if (appointments.isEmpty()) return ScheduleTimeline(DEFAULT_START_HOUR, DEFAULT_END_HOUR, emptyList())

        // The window always covers the working day, and stretches for anything booked outside it.
        val startHour = min(appointments.minOf { it.start.hour }, DEFAULT_START_HOUR)
        val endHour = max(
            appointments.maxOf { it.end.hour + if (it.end.minute > 0) 1 else 0 },
            DEFAULT_END_HOUR,
        ).coerceAtMost(HOURS_IN_DAY)

        val slots = mutableListOf<TimelineSlot>()
        val cluster = mutableListOf<Appointment>()
        var clusterEnd: LocalDateTime? = null

        fun flush() {
            if (cluster.isEmpty()) return
            val laneEnds = mutableListOf<LocalDateTime>()
            val lanes = cluster.map { appointment ->
                val free = laneEnds.indexOfFirst { !it.isAfter(appointment.start) }
                val lane = if (free >= 0) free else laneEnds.size.also { laneEnds.add(appointment.end) }
                laneEnds[lane] = appointment.end
                appointment to lane
            }
            lanes.forEach { (appointment, lane) ->
                slots += TimelineSlot(
                    appointment = appointment,
                    minuteOffset = appointment.start.hour * MINUTES_PER_HOUR +
                        appointment.start.minute - startHour * MINUTES_PER_HOUR,
                    lane = lane,
                    laneCount = laneEnds.size,
                )
            }
            cluster.clear()
            clusterEnd = null
        }

        appointments
            .sortedWith(compareBy<Appointment> { it.start }.thenByDescending { it.durationMinutes })
            .forEach { appointment ->
                val end = clusterEnd
                if (end != null && !appointment.start.isBefore(end)) flush()
                cluster += appointment
                clusterEnd = maxOf(clusterEnd ?: appointment.end, appointment.end)
            }
        flush()

        return ScheduleTimeline(startHour, endHour, slots)
    }

    private const val DEFAULT_START_HOUR = 7
    private const val DEFAULT_END_HOUR = 19
    private const val HOURS_IN_DAY = 24
    private const val MINUTES_PER_HOUR = 60
}
