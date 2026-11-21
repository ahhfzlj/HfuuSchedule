package com.hfuu.schedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class StudyTaskType(val label: String) {
    HOMEWORK("作业"),
    EXAM("考试"),
    SIGN_IN("签到")
}

@Serializable
@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey val id: String,
    val type: String, // HOMEWORK, EXAM, SIGN_IN
    val title: String,
    val courseName: String,
    val courseId: String = "",
    val classId: String = "",
    /** 截止时间戳（毫秒）；0 表示无明确截止时间；考试“未开始”时表示开考时间戳 */
    val deadlineMillis: Long,
    val isCompleted: Boolean = false,
    val actionUrl: String = "",
    val extraInfo: String = ""
) {
    fun taskType(): StudyTaskType = runCatching {
        StudyTaskType.valueOf(type)
    }.getOrDefault(StudyTaskType.HOMEWORK)

    fun isNotStartedExam(): Boolean =
        taskType() == StudyTaskType.EXAM && extraInfo.contains("未开始")

    fun sortKeyMillis(): Long = if (deadlineMillis > 0L) deadlineMillis else Long.MAX_VALUE

    fun isUrgent(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (isCompleted || deadlineMillis <= 0L || isNotStartedExam()) return false
        val diff = deadlineMillis - nowMillis
        return diff in 1..(12L * 60 * 60 * 1000)
    }

    fun formatRemaining(nowMillis: Long = System.currentTimeMillis()): String {
        if (isCompleted) return "已完成"
        if (deadlineMillis <= 0L) {
            return when {
                isNotStartedExam() -> "未开始"
                taskType() == StudyTaskType.EXAM -> "进行中"
                else -> "待提交"
            }
        }
        val diff = deadlineMillis - nowMillis
        if (diff <= 0) return if (isNotStartedExam()) "已开始" else "已截止"
        val totalMinutes = diff / (60 * 1000)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val mins = totalMinutes % 60
        val durationText = when {
            days > 0 && hours > 0 -> "${days} 天 ${hours} 小时"
            days > 0 -> "${days} 天"
            hours > 0 && mins > 0 -> "${hours} 小时 ${mins} 分钟"
            hours > 0 -> "${hours} 小时"
            else -> "${mins.coerceAtLeast(1)} 分钟"
        }
        return if (isNotStartedExam()) "距开考 $durationText" else "还剩 $durationText"
    }

    fun formatDeadline(): String {
        if (deadlineMillis <= 0L) {
            return if (taskType() == StudyTaskType.EXAM) "时间以学习通为准" else "无截止时间"
        }
        val dt = Instant.ofEpochMilli(deadlineMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val prefix = if (isNotStartedExam()) "开考 " else "截止 "
        return prefix + dt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }
}

@Serializable
data class ExamScheduleItem(
    val id: String,
    val courseName: String,
    val examTime: String,
    val location: String,
    val seatNumber: String,
    val examType: String = "期末闭卷"
)

@Serializable
data class GradeItem(
    val id: String,
    val courseName: String,
    val semester: String,
    val credit: Float,
    val score: String,
    val gpa: Float,
    val category: String = "必修"
)

@Serializable
data class CreditSummary(
    val earnedCredits: Float,
    val requiredCredits: Float,
    val averageGpa: Float,
    val categoryBreakdown: List<CreditCategory>
)

@Serializable
data class CreditCategory(
    val name: String,
    val earned: Float,
    val required: Float
)
