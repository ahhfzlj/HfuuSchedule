package com.hfuu.schedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    /** 1 = 周一 .. 7 = 周日 */
    val dayOfWeek: Int,
    /** 开始节次 (1-based) */
    val startSection: Int,
    /** 结束节次 (inclusive) */
    val endSection: Int,
    /** 逗号分隔的周次列表，如 "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16" */
    val weeksCsv: String = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16",
    /** 卡片配色索引 */
    val colorIndex: Int = 0,
    val credit: Float = 0f,
    val note: String = ""
) {
    fun weekList(): List<Int> = weeksCsv
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .sorted()

    fun isInWeek(week: Int): Boolean = week in weekList()

    fun formatWeeksSummary(): String {
        val list = weekList()
        if (list.isEmpty()) return "未设置周次"
        if (list.size == 1) return "第${list.first()}周"
        val isAllConsecutive = (list.first()..list.last()).toList() == list
        if (isAllConsecutive) return "${list.first()}-${list.last()}周"
        val isOdd = list.all { it % 2 == 1 } &&
            (list.first()..list.last() step 2).toList() == list
        if (isOdd) return "${list.first()}-${list.last()}周(单)"
        val isEven = list.all { it % 2 == 0 } &&
            (list.first()..list.last() step 2).toList() == list
        if (isEven) return "${list.first()}-${list.last()}周(双)"
        return "${list.first()}-${list.last()}周(共${list.size}周)"
    }
}
