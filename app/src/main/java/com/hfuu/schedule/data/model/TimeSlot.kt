package com.hfuu.schedule.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TimeSlot(
    val section: Int,
    val startTime: String, // "HH:mm"
    val endTime: String    // "HH:mm"
) {
    fun durationMinutes(): Int {
        val s = parseMinutes(startTime)
        val e = parseMinutes(endTime)
        return (e - s).coerceAtLeast(0)
    }

    companion object {
        fun parseMinutes(hhmm: String): Int {
            val parts = hhmm.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }

        fun formatMinutes(totalMinutes: Int): String {
            val clamped = totalMinutes.coerceIn(0, 23 * 60 + 59)
            val h = clamped / 60
            val m = clamped % 60
            return "%02d:%02d".format(h, m)
        }
    }
}
