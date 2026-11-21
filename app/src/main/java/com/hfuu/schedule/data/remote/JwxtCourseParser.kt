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

    fun parseFromBridgeJson(rawJson: String): Result<JwxtImportResult> = runCatching {
        val payload = json.decodeFromString<JwxtBridgePayload>(rawJson)
        val colorMap = mutableMapOf<String, Int>()
        var nextColor = 0

        // 按 (课程名, 星期, 开始节次, 结束节次) 合并去重，避免教务页面重复节点导致周课数虚高与卡片堆叠
        val mergedGroups = payload.courses
            .filter { it.name.isNotBlank() }
            .groupBy { item ->
                val day = item.dayOfWeek.coerceIn(1, 7)
                val start = item.startSection.coerceIn(1, 12)
                val end = item.endSection.coerceIn(start, 12)
                "${item.name.trim()}_${day}_${start}_${end}"
            }

        val entities = mergedGroups.values.map { group ->
            val first = group.first()
            val name = first.name.trim()
            val colorIdx = colorMap.getOrPut(name) { (nextColor++) % 8 }
            val mergedWeeks = group
                .flatMap { it.weeks.ifEmpty { (1..16).toList() } }
                .filter { it in 1..30 }
                .distinct()
                .sorted()
                .ifEmpty { (1..16).toList() }

            val bestLocation = group
                .map { it.location.trim() }
                .firstOrNull { it.isNotBlank() && it != "待定教室" }
                ?: first.location.trim()

            val bestTeacher = group
                .map { it.teacher.trim() }
                .firstOrNull { it.isNotBlank() && it != "未知教师" }
                ?: first.teacher.trim()

            val start = first.startSection.coerceIn(1, 12)
            val end = first.endSection.coerceIn(start, 12)

            CourseEntity(
                name = name,
                teacher = bestTeacher,
                location = bestLocation,
                dayOfWeek = first.dayOfWeek.coerceIn(1, 7),
                startSection = start,
                endSection = end,
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
