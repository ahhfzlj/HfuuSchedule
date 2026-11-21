package com.hfuu.schedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

enum class AgendaRepeatRule(val label: String) {
    NEVER("永不"),
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月"),
    YEARLY("每年");

    companion object {
        fun fromName(raw: String?): AgendaRepeatRule =
            entries.firstOrNull { it.name == raw } ?: NEVER
    }
}

enum class AgendaTag(
    val label: String,
    val dotColorHex: Long,
    val chipBgHex: Long,
    val chipTextHex: Long
) {
    CLASS_TIME("上课时间", 0xFFEA7B3C, 0xFFFFEFE6, 0xFFC25E24),
    EXAM_DATE("考试日期", 0xFF2589F5, 0xFFE8F3FF, 0xFF1A6FD4),
    HOMEWORK("作业提醒", 0xFF6B5CE7, 0xFFEEEBFF, 0xFF5243C2),
    STUDY_TASK("学习任务", 0xFFD99A1E, 0xFFFFF6DE, 0xFF9F6D0A),
    CLASS_DINNER("班级聚餐", 0xFF20A565, 0xFFE6F7EE, 0xFF157B4A),
    MOVIE("看电影", 0xFFE04868, 0xFFFFEBEE, 0xFFBF2E4D),
    SHOW("看演出", 0xFF6B7280, 0xFFF1F3F5, 0xFF4B5563),
    OTHER("其他", 0xFF1D63ED, 0xFFEAF1FF, 0xFF1749B3);

    companion object {
        fun fromName(raw: String?): AgendaTag =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

@Entity(tableName = "agenda_events")
data class AgendaEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val location: String = "",
    val isAllDay: Boolean = false,
    val startMillis: Long,
    val endMillis: Long,
    val repeatRule: String = AgendaRepeatRule.NEVER.name,
    val tag: String = AgendaTag.OTHER.name,
    val countdownEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 15,
    val notes: String = "",
    val isCompleted: Boolean = false
) {
    fun repeatEnum(): AgendaRepeatRule = AgendaRepeatRule.fromName(repeatRule)
    fun tagEnum(): AgendaTag = AgendaTag.fromName(tag)

    fun startDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDateTime()

    fun endDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDateTime()

    fun occursOn(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val startDate = startDateTime(zone).toLocalDate()
        val endDate = endDateTime(zone).toLocalDate()
        if (date.isBefore(startDate)) return false

        return when (repeatEnum()) {
            AgendaRepeatRule.NEVER -> !date.isBefore(startDate) && !date.isAfter(endDate)
            AgendaRepeatRule.DAILY -> true
            AgendaRepeatRule.WEEKLY -> date.dayOfWeek == startDate.dayOfWeek
            AgendaRepeatRule.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth
            AgendaRepeatRule.YEARLY -> date.month == startDate.month && date.dayOfMonth == startDate.dayOfMonth
        }
    }

    fun formatTimeRange(zone: ZoneId = ZoneId.systemDefault()): String {
        if (isAllDay) return "全天"
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        return "${startDateTime(zone).format(fmt)} - ${endDateTime(zone).format(fmt)}"
    }

    /** 计算相对于指定基准时刻的倒计时文本 */
    fun formatCountdown(nowMillis: Long = System.currentTimeMillis()): String {
        if (isCompleted) return "已完成"
        val targetStart = nextOccurrenceStartMillis(nowMillis)
        val duration = (endMillis - startMillis).coerceAtLeast(0L)
        val targetEnd = targetStart + duration

        if (nowMillis in targetStart..targetEnd && duration > 0L) {
            return "进行中"
        }
        val diff = targetStart - nowMillis
        if (diff <= 0L) {
            return "已过时"
        }
        val totalMinutes = (diff / 60_000L).coerceAtLeast(1L)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val mins = totalMinutes % 60
        return when {
            days > 0 -> if (hours > 0) "还有 ${days}天${hours}小时" else "还有 ${days}天"
            hours > 0 -> if (mins > 0) "还有 ${hours}小时${mins}分" else "还有 ${hours}小时"
            else -> "还有 ${mins}分钟"
        }
    }

    /** 计算下一次发生时刻（支持重复规则），用于闹钟提醒与倒计时 */
    fun nextOccurrenceStartMillis(nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long {
        if (repeatEnum() == AgendaRepeatRule.NEVER || startMillis >= nowMillis) {
            return startMillis
        }
        var dt = startDateTime(zone)
        val nowDt = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()
        var guard = 0
        while (dt.isBefore(nowDt) && guard < 1000) {
            dt = when (repeatEnum()) {
                AgendaRepeatRule.DAILY -> dt.plusDays(1)
                AgendaRepeatRule.WEEKLY -> dt.plusWeeks(1)
                AgendaRepeatRule.MONTHLY -> dt.plusMonths(1)
                AgendaRepeatRule.YEARLY -> dt.plusYears(1)
                AgendaRepeatRule.NEVER -> break
            }
            guard++
        }
        return dt.atZone(zone).toInstant().toEpochMilli()
    }

    fun reminderTriggerMillis(nowMillis: Long = System.currentTimeMillis()): Long? {
        if (!reminderEnabled && !countdownEnabled) return null
        if (isCompleted) return null
        val nextStart = nextOccurrenceStartMillis(nowMillis)
        val leadMs = if (reminderEnabled) abs(reminderMinutesBefore) * 60_000L else 0L
        val trigger = nextStart - leadMs
        return if (trigger > nowMillis) trigger else if (nextStart > nowMillis) nextStart else null
    }
}
