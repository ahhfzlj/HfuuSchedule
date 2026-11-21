package com.hfuu.schedule.data.model

object HfuuDefaults {
    const val HFUU_JWC_URL = "https://vpn.hfuu.edu.cn/login"
    const val HFUU_HOME_URL = "https://www.hfuu.edu.cn/"
    const val CHAOXING_LOGIN_URL = "https://passport2.chaoxing.com/login?fid=&newversion=true&refer=https%3A%2F%2Fi.chaoxing.com"
    const val CHAOXING_HOME_URL = "https://i.chaoxing.com"

    /**
     * 合肥大学常规课堂作息时间表 (1-10节，并预留11-12节)
     */
    val defaultTimeSlots: List<TimeSlot> = listOf(
        TimeSlot(1, "08:20", "09:05"),
        TimeSlot(2, "09:15", "10:00"),
        TimeSlot(3, "10:30", "11:15"),
        TimeSlot(4, "11:25", "12:10"),
        TimeSlot(5, "14:00", "14:45"),
        TimeSlot(6, "14:55", "15:40"),
        TimeSlot(7, "16:10", "16:55"),
        TimeSlot(8, "17:05", "17:50"),
        TimeSlot(9, "18:30", "19:15"),
        TimeSlot(10, "19:25", "20:10"),
        TimeSlot(11, "20:20", "21:05"),
        TimeSlot(12, "21:15", "22:00")
    )

    val demoCourses: List<CourseEntity> = listOf(
        CourseEntity(
            name = "数据结构",
            teacher = "王建国",
            location = "36栋-201",
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
            weeksCsv = (1..16).joinToString(","),
            colorIndex = 0,
            credit = 4.0f
        ),
        CourseEntity(
            name = "线性代数",
            teacher = "李明华",
            location = "37栋-305",
            dayOfWeek = 1,
            startSection = 5,
            endSection = 6,
            weeksCsv = (1..16).joinToString(","),
            colorIndex = 1,
            credit = 3.0f
        ),
        CourseEntity(
            name = "大学英语I（三）",
            teacher = "张婷婷",
            location = "38栋-104",
            dayOfWeek = 2,
            startSection = 3,
            endSection = 4,
            weeksCsv = (1..16).joinToString(","),
            colorIndex = 2,
            credit = 3.0f
        ),
        CourseEntity(
            name = "电工电子技术基础",
            teacher = "陈志强",
            location = "35栋-402",
            dayOfWeek = 2,
            startSection = 7,
            endSection = 8,
            weeksCsv = (1..16).joinToString(","),
            colorIndex = 3,
            credit = 3.5f
        ),
        CourseEntity(
            name = "计算机组成原理",
            teacher = "周海峰",
            location = "36栋-308",
            dayOfWeek = 3,
            startSection = 1,
            endSection = 2,
            weeksCsv = (1..16).joinToString(","),
            colorIndex = 4,
            credit = 4.0f
        ),
        CourseEntity(
            name = "数据结构实验",
            teacher = "王建国",
            location = "36栋-机房B",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
            weeksCsv = (3..14).joinToString(","),
            colorIndex = 0,
            credit = 1.0f
        ),
        CourseEntity(
            name = "概率论与数理统计",
            teacher = "赵敏",
            location = "37栋-202",
            dayOfWeek = 4,
            startSection = 3,
            endSection = 4,
            weeksCsv = (1..16).joinToString(","),
            colorIndex = 5,
            credit = 3.0f
        ),
        CourseEntity(
            name = "Android 移动应用开发",
            teacher = "刘洋",
            location = "36栋-503",
            dayOfWeek = 4,
            startSection = 9,
            endSection = 10,
            weeksCsv = (1..14).joinToString(","),
            colorIndex = 6,
            credit = 2.0f
        ),
        CourseEntity(
            name = "马克思主义基本原理",
            teacher = "孙芳",
            location = "38栋-阶梯2",
            dayOfWeek = 5,
            startSection = 1,
            endSection = 2,
            weeksCsv = (1..16).joinToString(","),
            colorIndex = 7,
            credit = 3.0f
        ),
        CourseEntity(
            name = "体育（羽毛球）",
            teacher = "吴磊",
            location = "体育馆A区",
            dayOfWeek = 5,
            startSection = 5,
            endSection = 6,
            weeksCsv = (1..16).joinToString(","),
            colorIndex = 2,
            credit = 1.0f
        )
    )

    fun demoStudyTasks(nowMillis: Long = System.currentTimeMillis()): List<StudyTaskEntity> {
        val hour = 60L * 60 * 1000
        val min = 60L * 1000
        val day = 24L * hour
        return listOf(
            StudyTaskEntity(
                id = "demo_sign_1",
                type = StudyTaskType.SIGN_IN.name,
                title = "课堂位置签到（第10周第3-4节）",
                courseName = "数据结构",
                deadlineMillis = nowMillis + 25 * min,
                extraInfo = "正在进行 · 需位置签到"
            ),
            StudyTaskEntity(
                id = "demo_hw_1",
                type = StudyTaskType.HOMEWORK.name,
                title = "第 5 章 特征值与特征向量 课后习题",
                courseName = "线性代数",
                deadlineMillis = nowMillis + 3 * hour + 20 * min
            ),
            StudyTaskEntity(
                id = "demo_hw_2",
                type = StudyTaskType.HOMEWORK.name,
                title = "Unit 4 Writing: A Letter of Application",
                courseName = "大学英语I（三）",
                deadlineMillis = nowMillis + 1 * day + 3 * hour
            ),
            StudyTaskEntity(
                id = "demo_hw_3",
                type = StudyTaskType.HOMEWORK.name,
                title = "实验二 链表的实现与测试 实验报告",
                courseName = "数据结构",
                deadlineMillis = nowMillis + 2 * day + 4 * hour
            ),
            StudyTaskEntity(
                id = "demo_exam_1",
                type = StudyTaskType.EXAM.name,
                title = "期中测验（闭卷，90 分钟）",
                courseName = "电工电子技术基础",
                deadlineMillis = nowMillis + 3 * day + 1 * hour
            ),
            StudyTaskEntity(
                id = "demo_hw_4",
                type = StudyTaskType.HOMEWORK.name,
                title = "第 4 章 存储器层次结构 作业",
                courseName = "计算机组成原理",
                deadlineMillis = nowMillis + 4 * day + 5 * hour
            ),
            StudyTaskEntity(
                id = "demo_hw_5",
                type = StudyTaskType.HOMEWORK.name,
                title = "随机变量分布函数练习题",
                courseName = "概率论与数理统计",
                deadlineMillis = nowMillis + 5 * day + 2 * hour
            ),
            StudyTaskEntity(
                id = "demo_exam_2",
                type = StudyTaskType.EXAM.name,
                title = "第 1-4 章阶段随堂测验",
                courseName = "马克思主义基本原理",
                deadlineMillis = nowMillis + 6 * day + 6 * hour
            )
        )
    }

    val demoExams: List<ExamScheduleItem> = listOf(
        ExamScheduleItem("ex_1", "数据结构", "2026-12-28 08:30-10:30", "36栋-201", "18号", "期末闭卷"),
        ExamScheduleItem("ex_2", "线性代数", "2026-12-30 14:00-16:00", "37栋-305", "09号", "期末闭卷"),
        ExamScheduleItem("ex_3", "大学英语I（三）", "2027-01-04 08:30-10:30", "38栋-104", "24号", "统考闭卷"),
        ExamScheduleItem("ex_4", "电工电子技术基础", "2027-01-06 14:00-16:00", "35栋-402", "11号", "期末闭卷")
    )

    val demoGrades: List<GradeItem> = listOf(
        GradeItem("g_1", "高级语言程序设计(C)", "2025-2026 第二学期", 4.0f, "92", 4.2f, "专业必修"),
        GradeItem("g_2", "高等数学(下)", "2025-2026 第二学期", 5.0f, "88", 3.8f, "通识必修"),
        GradeItem("g_3", "离散数学", "2025-2026 第二学期", 3.5f, "90", 4.0f, "专业必修"),
        GradeItem("g_4", "大学英语I（二）", "2025-2026 第二学期", 3.0f, "85", 3.5f, "通识必修"),
        GradeItem("g_5", "大学物理(上)", "2025-2026 第二学期", 3.5f, "83", 3.3f, "通识必修")
    )

    val demoCreditSummary = CreditSummary(
        earnedCredits = 68.5f,
        requiredCredits = 160.0f,
        averageGpa = 3.79f,
        categoryBreakdown = listOf(
            CreditCategory("通识必修课", 32.0f, 52.0f),
            CreditCategory("专业核心课", 24.5f, 68.0f),
            CreditCategory("实践与实验环节", 8.0f, 28.0f),
            CreditCategory("通识选修与创新学分", 4.0f, 12.0f)
        )
    )
}
