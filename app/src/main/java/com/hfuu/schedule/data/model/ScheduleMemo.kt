package com.hfuu.schedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "schedule_memos")
data class ScheduleMemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    /** 所属周次；0 表示每周重复 */
    val week: Int,
    /** 1 = 周一 .. 7 = 周日 */
    val dayOfWeek: Int,
    /** 开始节次 (1-based) */
    val startSection: Int,
    /** 结束节次 (inclusive) */
    val endSection: Int,
    val isDone: Boolean = false
) {
    fun isInWeek(targetWeek: Int): Boolean = week == 0 || week == targetWeek
}
