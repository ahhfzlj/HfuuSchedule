package com.hfuu.schedule.data.remote

import com.hfuu.schedule.data.model.CourseEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class JwxtParsedStudentInfo(
    val name: String = "",
    val studentId: String = "",
    val className: String = "",
    val major: String = ""
)

@Serializable
data class JwxtParsedCourse(
    val name: String = "",
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 2,
    val weeks: List<Int> = emptyList()
)

@Serializable
data class JwxtBridgePayload(
    val success: Boolean = false,
    val source: String = "",
    val url: String = "",
    val studentInfo: JwxtParsedStudentInfo = JwxtParsedStudentInfo(),
    val courses: List<JwxtParsedCourse> = emptyList()
)

data class JwxtImportResult(
    val courses: List<CourseEntity>,
    val studentInfo: JwxtParsedStudentInfo,
    val source: String
)

object JwxtCourseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private fun normalizeRoom(raw: String): String {
        val cleaned = raw.trim().replace(Regex("\\s+"), " ")
        return if (cleaned.isBlank() || cleaned == "待定教室") "" else cleaned
    }

    private fun normalizeWeeks(raw: List<Int>): List<Int> {
        return raw.filter { it in 1..30 }.distinct().sorted().ifEmpty { (1..16).toList() }
    }

    fun parseFromBridgeJson(rawJson: String): Result<JwxtImportResult> = runCatching {
        val payload = json.decodeFromString<JwxtBridgePayload>(rawJson)
        val colorMap = mutableMapOf<String, Int>()
        var nextColor = 0

        data class NormalizedItem(
            val name: String,
            val teacher: String,
            val location: String,
            val day: Int,
            val start: Int,
            val end: Int,
            val weeks: List<Int>
        )

        val normalized = payload.courses
            .filter { it.name.isNotBlank() }
            .map { item ->
                val day = item.dayOfWeek.coerceIn(1, 7)
                val start = item.startSection.coerceIn(1, 12)
                val end = item.endSection.coerceIn(start, 12)
                NormalizedItem(
                    name = item.name.trim(),
                    teacher = item.teacher.trim(),
                    location = normalizeRoom(item.location),
                    day = day,
                    start = start,
                    end = end,
                    weeks = normalizeWeeks(item.weeks)
                )
            }

        // 1. 过滤同课程、同时段、同周次的“无教室”重复节点（仅当存在带真实教室的同项时）
        val richKeys = normalized
            .filter { it.location.isNotEmpty() }
            .map { "${it.name}_${it.day}_${it.start}_${it.end}_${it.weeks.joinToString(",")}" }
            .toSet()

        val filtered = normalized.filter { item ->
            if (item.location.isNotEmpty()) true
            else "${item.name}_${item.day}_${item.start}_${item.end}_${item.weeks.joinToString(",")}" !in richKeys
        }

        // 2. 关键修复：按 (课程名, 星期, 节次, 教室) 分组合并周次！
        //    绝对不把同一门课不同周次、不同教室（如前半学期 35-208 与后半学期/实验 36-204）错误合并成一个教室！
        val mergedGroups = filtered.groupBy { item ->
            "${item.name}_${item.day}_${item.start}_${item.end}_${item.location}"
        }

        val entities = mergedGroups.values.map { group ->
            val first = group.first()
            val colorIdx = colorMap.getOrPut(first.name) { (nextColor++) % 8 }
            val mergedWeeks = group
                .flatMap { it.weeks }
                .filter { it in 1..30 }
                .distinct()
                .sorted()
                .ifEmpty { (1..16).toList() }

            val bestTeacher = group
                .map { it.teacher }
                .firstOrNull { it.isNotBlank() && it != "未知教师" }
                ?: first.teacher.ifBlank { "未知教师" }

            CourseEntity(
                name = first.name,
                teacher = bestTeacher,
                location = first.location.ifBlank { "待定教室" },
                dayOfWeek = first.day,
                startSection = first.start,
                endSection = first.end,
                weeksCsv = mergedWeeks.joinToString(","),
                colorIndex = colorIdx
            )
        }

        JwxtImportResult(
            courses = entities,
            studentInfo = payload.studentInfo,
            source = payload.source
        )
    }

    fun exportCoursesJson(courses: List<CourseEntity>): String {
        return json.encodeToString(courses)
    }

    fun importCoursesJson(rawJson: String): Result<List<CourseEntity>> = runCatching {
        json.decodeFromString<List<CourseEntity>>(rawJson).map { it.copy(id = 0) }
    }
}
