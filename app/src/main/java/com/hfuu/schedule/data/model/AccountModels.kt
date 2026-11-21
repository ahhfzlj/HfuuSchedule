package com.hfuu.schedule.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
data class UserProfile(
    val name: String = "合大同学",
    val studentId: String = "2025010130",
    val className: String = "",
    val major: String = "",
    val college: String = ""
) {
    fun maskedStudentId(): String {
        if (studentId.length <= 6) return studentId
        return studentId.take(4) + "****" + studentId.takeLast(2)
    }

    fun avatarText(): String = name.trim().firstOrNull()?.toString() ?: "合"

    fun subtitleText(): String {
        val parts = listOf(className.trim(), major.trim()).filter { it.isNotEmpty() }
        return parts.joinToString(" · ")
    }
}

@Serializable
data class SemesterConfig(
    val semesterName: String = "2026-2027 第一学期",
    val shortLabel: String = "大二上",
    /** 开学第一周周一，格式 yyyy-MM-dd */
    val startDateIso: String = "2026-09-07",
    val totalWeeks: Int = 18
) {
    fun startDate(): LocalDate = runCatching {
        LocalDate.parse(startDateIso)
    }.getOrDefault(LocalDate.of(2026, 9, 7))

    fun currentWeek(today: LocalDate = LocalDate.now()): Int {
        val days = ChronoUnit.DAYS.between(startDate(), today)
        val rawWeek = (days / 7L).toInt() + 1
        return rawWeek.coerceIn(1, totalWeeks)
    }

    /** 计算指定日期属于本学期第几周，若不在本学期范围内返回 null */
    fun weekForDate(date: LocalDate): Int? {
        val days = ChronoUnit.DAYS.between(startDate(), date)
        if (days < 0) return null
        val week = (days / 7L).toInt() + 1
        return if (week in 1..totalWeeks) week else null
    }

    fun formatStartShort(): String {
        val d = startDate()
        return "${d.monthValue}.${"%02d".format(d.dayOfMonth)}"
    }

    fun summaryText(): String = "$shortLabel · ${formatStartShort()} 开学 · ${totalWeeks} 周"

    fun dateFor(week: Int, dayOfWeek: Int): LocalDate {
        return startDate().plusWeeks((week - 1).toLong()).plusDays((dayOfWeek - 1).toLong())
    }

    fun formatDateRangeForWeek(week: Int, dayCount: Int = 7): String {
        val mon = dateFor(week, 1)
        val endDay = dateFor(week, dayCount.coerceIn(1, 7))
        val fmt = DateTimeFormatter.ofPattern("M/d")
        return "${mon.format(fmt)} - ${endDay.format(fmt)}"
    }
}

@Serializable
data class AccountConnectionState(
    val jwxtConnected: Boolean = false,
    val jwxtUsername: String = "",
    val jwxtLastSyncMillis: Long = 0L,
    val chaoxingConnected: Boolean = false,
    val chaoxingAccount: String = "",
    val chaoxingCourseCount: Int = 0,
    val chaoxingLastSyncMillis: Long = 0L,
    val localAccountSignedIn: Boolean = true
) {
    fun connectedCount(): Int {
        var count = 0
        if (jwxtConnected) count++
        if (chaoxingConnected) count++
        if (localAccountSignedIn) count++
        return count
    }
}
