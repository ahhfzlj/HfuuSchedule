package com.hfuu.schedule.data.remote

import com.hfuu.schedule.data.model.StudyTaskEntity
import com.hfuu.schedule.data.model.StudyTaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class ChaoxingCourseInfo(
    val courseId: String,
    val classId: String,
    val name: String,
    val cpi: String = ""
)

private data class CourseTokens(
    val workEnc: String = "",
    val examEnc: String = "",
    val stuEnc: String = "",
    val openc: String = ""
)

data class ChaoxingSyncResult(
    val courseCount: Int,
    val tasks: List<StudyTaskEntity>,
    val message: String
)

/**
 * 超星学习通直连同步客户端：
 * 1. 拉取课程列表 (backclazzdata)
 * 2. 请求课程中间页 (stucoursemiddle) 提取 workEnc / examEnc / stuenc / openc
 * 3. 拉取待办作业 (mooc2/work/list)、待考安排 (mooc2/exam/list & examlist)、进行中的有效签到 (activelist)
 */
class ChaoxingApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val webUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private val mobileUserAgent =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 com.chaoxing.mobile/ChaoXingStudy_3_6.3.1_android_phone"

    suspend fun syncAll(cookie: String): Result<ChaoxingSyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(cookie.isNotBlank()) { "学习通未登录，请先连接学习通账号" }

            val courses = fetchCourses(cookie)
            val semaphore = Semaphore(4)

            val allTasks = coroutineScope {
                courses.take(40).mapIndexed { index, course ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            if (index > 0) delay(80L)
                            val list = mutableListOf<StudyTaskEntity>()
                            val tokens = runCatching { fetchCourseTokens(cookie, course) }
                                .getOrDefault(CourseTokens())
                            runCatching { list += fetchActiveSignIns(cookie, course) }
                            runCatching { list += fetchPendingWorks(cookie, course, tokens) }
                            runCatching { list += fetchPendingExams(cookie, course, tokens) }
                            list
                        }
                    }
                }.awaitAll().flatten()
            }

            val deduped = allTasks
                .distinctBy { it.id }
                .sortedBy { it.sortKeyMillis() }

            ChaoxingSyncResult(
                courseCount = courses.size,
                tasks = deduped,
                message = "已检查 ${courses.size} 门课程，发现 ${deduped.size} 条待办"
            )
        }
    }

    private fun fetchCourses(cookie: String): List<ChaoxingCourseInfo> {
        val req = Request.Builder()
            .url("https://mooc1-api.chaoxing.com/mycourse/backclazzdata?view=json&rss=1")
            .header("Cookie", cookie)
            .header("User-Agent", mobileUserAgent)
            .build()

        val body = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("获取学习通课程失败: HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }

        val root = json.parseToJsonElement(body).jsonObject
        val channels = root["channelList"]?.jsonArray ?: return emptyList()
        val result = mutableListOf<ChaoxingCourseInfo>()

        for (element in channels) {
            val item = element.jsonObject
            val cpi = item["cpi"]?.jsonPrimitive?.content.orEmpty()
            val content = item["content"]?.jsonObject ?: continue
            // state == 1 表示已结课/归档课程，跳过以免拉取往期已截止作业
            val courseState = content["state"]?.jsonPrimitive?.intOrNull ?: 0
            if (courseState == 1) continue
            val classId = content["id"]?.jsonPrimitive?.content.orEmpty()
            val courseObj = content["course"]?.jsonObject ?: continue
            val dataArr = courseObj["data"]?.jsonArray ?: continue
            val first = dataArr.firstOrNull()?.jsonObject ?: continue
            val courseId = first["id"]?.jsonPrimitive?.content.orEmpty()
            val name = first["name"]?.jsonPrimitive?.content.orEmpty()
            if (courseId.isNotBlank() && classId.isNotBlank() && name.isNotBlank()) {
                result += ChaoxingCourseInfo(courseId, classId, name, cpi)
            }
        }
        return result
    }

    /**
     * 访问课程中间页，提取 mooc2 作业/考试接口所需的 workEnc、examEnc、stuenc、openc。
     */
    private fun fetchCourseTokens(cookie: String, course: ChaoxingCourseInfo): CourseTokens {
        val url = "https://mooc1.chaoxing.com/visit/stucoursemiddle" +
            "?courseid=${course.courseId}&clazzid=${course.classId}&cpi=${course.cpi}&ismooc2=1&v=2"
        val req = Request.Builder()
            .url(url)
            .header("Cookie", cookie)
            .header("User-Agent", webUserAgent)
            .header("Referer", "https://i.chaoxing.com/")
            .build()

        val html = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return CourseTokens()
            resp.body?.string().orEmpty()
        }

        val doc = Jsoup.parse(html)
        fun findInput(idOrName: String): String {
            return doc.selectFirst("#$idOrName")?.attr("value")
                ?.takeIf { it.isNotBlank() }
                ?: doc.selectFirst("input[name=$idOrName]")?.attr("value").orEmpty()
        }

        return CourseTokens(
            workEnc = findInput("workEnc"),
            examEnc = findInput("examEnc"),
            stuEnc = findInput("enc").ifBlank { findInput("stuenc") },
            openc = findInput("openc")
        )
    }

    /**
     * 拉取进行中的有效签到：
     * 严格过滤掉已过期、已签到、或老师忘记关闭的陈旧历史签到。（保留已验证逻辑）
     */
    private fun fetchActiveSignIns(cookie: String, course: ChaoxingCourseInfo): List<StudyTaskEntity> {
        val now = System.currentTimeMillis()
        val url = "https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist" +
            "?fid=0&courseId=${course.courseId}&classId=${course.classId}&showNotStartedActive=0&_=$now"
        val req = Request.Builder()
            .url(url)
            .header("Cookie", cookie)
            .header("User-Agent", mobileUserAgent)
            .build()

        val body = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            resp.body?.string().orEmpty()
        }

        val root = json.parseToJsonElement(body).jsonObject
        val activeList = root["data"]?.jsonObject?.get("activeList")?.jsonArray
            ?: root["activeList"]?.jsonArray
            ?: return emptyList()

        val items = mutableListOf<StudyTaskEntity>()
        val maxSignAgeMillis = 6L * 60 * 60 * 1000 // 超过 6 小时未结束的通常为老师遗忘关闭的历史签到

        for (el in activeList) {
            val obj = el.jsonObject
            val status = obj["status"]?.jsonPrimitive?.intOrNull ?: 0
            val activeType = obj["activeType"]?.jsonPrimitive?.intOrNull ?: 0
            if (activeType != 2 || status != 1) continue

            // 若学生已参与/已签到，跳过
            val userStatus = obj["userStatus"]?.jsonPrimitive?.intOrNull ?: 0
            if (userStatus != 0) continue

            val nameFour = obj["nameFour"]?.jsonPrimitive?.content.orEmpty()
            if (nameFour.contains("已结束") || nameFour.contains("已签") ||
                nameFour.contains("缺勤") || nameFour.contains("请假") ||
                nameFour.contains("代签")
            ) {
                continue
            }

            val rawEndTime = obj["endTime"]?.jsonPrimitive?.longOrNull ?: 0L
            val endTime = normalizeEpochMillis(rawEndTime)
            val rawStartTime = obj["startTime"]?.jsonPrimitive?.longOrNull ?: 0L
            val startTime = normalizeEpochMillis(rawStartTime)

            // 1. 明确有截止时间且已过期 -> 跳过
            if (endTime > 0L && endTime <= now) continue

            // 2. 没有截止时间（永久/手动结束型签到），但开始时间已超过 6 小时且无“剩余”倒计时提示 -> 视为已结束的陈旧签到
            if (endTime <= 0L) {
                val startedLongAgo = startTime > 0L && (now - startTime) > maxSignAgeMillis
                val hasRemainingHint = nameFour.contains("剩余")
                if (startedLongAgo || !hasRemainingHint) continue
            }

            // 3. 即使有截止时间，若开始时间已超过 24 小时且截止时间异常遥远，也过滤
            if (startTime > 0L && (now - startTime) > 24L * 60 * 60 * 1000 && endTime <= 0L) continue

            val activeId = obj["id"]?.jsonPrimitive?.content.orEmpty()
            val title = obj["nameOne"]?.jsonPrimitive?.content?.ifBlank { "课堂签到" } ?: "课堂签到"
            val effectiveDeadline = when {
                endTime > now -> endTime
                else -> parseRemainingFromText(nameFour, now) ?: (now + 30 * 60 * 1000L)
            }

            items += StudyTaskEntity(
                id = "sign_${course.courseId}_$activeId",
                type = StudyTaskType.SIGN_IN.name,
                title = title,
                courseName = course.name,
                courseId = course.courseId,
                classId = course.classId,
                deadlineMillis = effectiveDeadline,
                actionUrl = "https://mobilelearn.chaoxing.com/widget/sign/e?id=$activeId&c=${course.courseId}",
                extraInfo = nameFour.ifBlank { "进行中 · 点击前往签到" }
            )
        }
        return items
    }

    /**
     * 拉取待办作业：携带 workEnc / stuenc / openc 请求 mooc2/work/list
     */
    private fun fetchPendingWorks(
        cookie: String,
        course: ChaoxingCourseInfo,
        tokens: CourseTokens
    ): List<StudyTaskEntity> {
        val now = System.currentTimeMillis()
        val url = buildString {
            append("https://mooc1.chaoxing.com/mooc2/work/list")
            append("?courseId=${course.courseId}")
            append("&classId=${course.classId}")
            append("&cpi=${course.cpi}")
            append("&ut=s")
            append("&isdisplaytable=2")
            append("&t=$now")
            if (tokens.openc.isNotBlank()) append("&openc=${tokens.openc}")
            if (tokens.workEnc.isNotBlank()) append("&enc=${tokens.workEnc}")
            if (tokens.stuEnc.isNotBlank()) append("&stuenc=${tokens.stuEnc}")
        }

        val req = Request.Builder()
            .url(url)
            .header("Cookie", cookie)
            .header("User-Agent", webUserAgent)
            .header("Referer", "https://mooc1.chaoxing.com/")
            .build()

        val html = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            resp.body?.string().orEmpty()
        }

        val candidates = parseWorkListHtml(html, course, url, now)
        // 对列表页没有“剩余时间”的未交作业（parsedDeadline == 0L）做二次详情核验，
        // 精准剔除“未提交但已截止/已过期”的历史作业，同时保留真正开放的无截止时间作业。
        return candidates.mapNotNull { task ->
            if (task.deadlineMillis > 0L) {
                task
            } else {
                verifyOpenWorkDetail(cookie, task, now)
            }
        }
    }

    /**
     * 二次核验无倒计时的“未交”作业详情页：
     * - 若服务端自动重定向到 /work/view（查看页）或页面提示已截止/已过期/作答时间已过，视为已截止并过滤。
     * - 若详情页包含明确的截止时间且 <= now，视为已截止并过滤；若 > now，则补全真实截止时间。
     * - 若确认仍处于可作答状态（dowork 表单且无过期提示），则保留。
     */
    private fun verifyOpenWorkDetail(
        cookie: String,
        task: StudyTaskEntity,
        now: Long
    ): StudyTaskEntity? {
        val targetUrl = task.actionUrl
        if (!targetUrl.startsWith("http")) return null

        val req = Request.Builder()
            .url(targetUrl)
            .header("Cookie", cookie)
            .header("User-Agent", webUserAgent)
            .header("Referer", "https://mooc1.chaoxing.com/")
            .build()

        return runCatching {
            client.newCall(req).execute().use { resp ->
                val finalUrl = resp.request.url.toString().lowercase()
                // 超星对“未交但已截止”的作业，访问 dowork 会自动重定向到 /work/view 或 selectWorkQuestionYiPiYue
                if ((finalUrl.contains("/work/view") || finalUrl.contains("selectworkquestionyipiyue")) &&
                    !finalUrl.contains("dowork") && !finalUrl.contains("dohomework")
                ) {
                    return@use null
                }

                val bodyHtml = if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
                if (bodyHtml.isBlank()) return@use null

                val doc = Jsoup.parse(bodyHtml)
                val pageText = doc.text()

                val expiredDetailHints = listOf(
                    "已截止", "已过期", "已超期", "作答时间已过", "提交时间已过", "超过截止时间",
                    "作业已结束", "已关闭", "不能作答", "无法作答", "不可提交", "无法提交",
                    "补交已截止", "不支持补交", "您的成绩", "最终成绩", "待批阅", "批阅中", "已批阅"
                )
                if (expiredDetailHints.any { pageText.contains(it) }) {
                    return@use null
                }

                // 尝试从详情页提取“截止时间：yyyy-MM-dd HH:mm”
                val ddlSection = Regex("""(?:截止时间|结束时间|作答截止)[：:\s]*([^\n。；;]{6,32})""")
                    .find(pageText)?.groupValues?.getOrNull(1).orEmpty()
                val detailDeadline = parseSingleDeadlineMillis(ddlSection, now)
                    ?: parseSingleDeadlineMillis(
                        doc.select(".time, .notOver, .deadline, .endTime, [class*=time]").text(),
                        now
                    )

                if (detailDeadline != null) {
                    if (detailDeadline <= now) return@use null
                    return@use task.copy(deadlineMillis = detailDeadline)
                }

                // 确认页面确实包含可作答/提交入口（防止误放过无权限页或过期空白提示页）
                val canSubmit = pageText.contains("提交") || pageText.contains("暂时保存") ||
                    bodyHtml.contains("addWork") || bodyHtml.contains("saveWork") ||
                    bodyHtml.contains("btnSubmit") || finalUrl.contains("dowork")
                if (!canSubmit) return@use null

                task
            }
        }.getOrNull()
    }

    /**
     * 拉取待办考试：优先 mooc2/exam/list (带 examEnc)，同时兼容 exam-ans/exam/test/examcode/examlist
     */
    private fun fetchPendingExams(
        cookie: String,
        course: ChaoxingCourseInfo,
        tokens: CourseTokens
    ): List<StudyTaskEntity> {
        val now = System.currentTimeMillis()
        val urls = listOf(
            buildString {
                append("https://mooc1.chaoxing.com/mooc2/exam/list")
                append("?courseId=${course.courseId}")
                append("&classId=${course.classId}")
                append("&cpi=${course.cpi}")
                append("&ut=s")
                append("&t=$now")
                if (tokens.openc.isNotBlank()) append("&openc=${tokens.openc}")
                if (tokens.examEnc.isNotBlank()) append("&enc=${tokens.examEnc}")
                if (tokens.stuEnc.isNotBlank()) append("&stuenc=${tokens.stuEnc}")
            },
            buildString {
                append("https://mooc1-api.chaoxing.com/exam-ans/exam/test/examcode/examlist")
                append("?courseId=${course.courseId}")
                append("&classId=${course.classId}")
                append("&cpi=${course.cpi}")
                append("&ut=s")
                if (tokens.examEnc.isNotBlank()) append("&enc=${tokens.examEnc}")
            }
        )

        for (url in urls) {
            val req = Request.Builder()
                .url(url)
                .header("Cookie", cookie)
                .header("User-Agent", webUserAgent)
                .header("Referer", "https://mooc1.chaoxing.com/")
                .build()

            val html = runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) "" else resp.body?.string().orEmpty()
                }
            }.getOrDefault("")
            if (html.isBlank()) continue

            val parsed = parseExamListHtml(html, course, url, now)
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }

    private fun normalizeEpochMillis(raw: Long): Long {
        if (raw <= 0L) return 0L
        return if (raw < 10_000_000_000L) raw * 1000L else raw
    }

    companion object {
        // 匹配已评分的得分文本（如 "95分"、"100分"、"88.5分"），明确排除 "分钟"
        private val SCORE_REGEX = Regex("""(?<!\d)(100|[1-9]?\d)(\.\d+)?\s*分(?!钟)""")

        private val WORK_FINISHED_KEYWORDS = listOf(
            "已完成", "已提交", "已交", "批阅中", "待批阅", "已批阅",
            "已批改", "待批改", "已评分", "已过期", "已截止", "已结束",
            "已关闭", "已超时", "已撤回", "查看", "成绩"
        )

        private val WORK_PENDING_KEYWORDS = listOf(
            "未交", "未提交", "待做", "待完成", "待提交", "待重做", "未完成", "去完成", "做作业"
        )

        private val EXAM_FINISHED_KEYWORDS = listOf(
            "已完成", "已提交", "已交", "已交卷", "待批阅", "批阅中", "已批阅",
            "已批改", "待批改", "已评分", "已结束", "已过期", "已截止", "已关闭",
            "缺考", "未参加", "已错过", "已作废", "查看", "成绩"
        )

        private val EXAM_ONGOING_KEYWORDS = listOf(
            "进行中", "待做", "待考", "待考试", "未考", "未交", "开始考试", "继续考试"
        )

        private val EXAM_NOT_STARTED_KEYWORDS = listOf(
            "未开始", "即将开始"
        )

        /**
         * 解析超星作业列表 HTML：
         * 1. 按单层列表项提取，避免嵌套选择器导致字段串位。
         * 2. 通过 data 链接 (/work/view vs /work/dowork) + 全量状态文本 + 分数正则严格剔除已完成/已提交/已过期作业。
         * 3. 仅解析真实截止时间，不再伪造“当前时间+24小时”。
         */
        internal fun parseWorkListHtml(
            html: String,
            course: ChaoxingCourseInfo,
            fallbackUrl: String,
            now: Long
        ): List<StudyTaskEntity> {
            val doc: Document = Jsoup.parse(html)
            val rows = selectListRows(doc)
            val items = mutableListOf<StudyTaskEntity>()

            for (row in rows) {
                val title = row.selectFirst("p.overHidden2, .overHidden2, .tit, .workName")
                    ?.text()?.trim()
                    ?.ifBlank { null }
                    ?: row.selectFirst("p, a")?.text()?.trim().orEmpty()
                if (title.isBlank()) continue

                val dataLink = row.attr("data").ifBlank { row.attr("onclick") }
                val lowerLink = dataLink.lowercase()

                // 已提交/已批阅/已过期的作业链接为 /work/view 或 selectWorkQuestionYiPiYue（而非 dowork）
                val isViewOnlyLink = (
                    lowerLink.contains("/work/view") ||
                        lowerLink.contains("selectworkquestionyipiyue") ||
                        lowerLink.contains("/work/detail")
                    ) && !lowerLink.contains("dowork") && !lowerLink.contains("dohomework")
                if (isViewOnlyLink) continue

                val statusText = row.select(".status, .status_text, .fl.status, .status_fl, [class*=status]")
                    .joinToString(" ") { it.text().trim() }
                    .trim()
                val ariaText = buildString {
                    append(row.attr("aria-label"))
                    row.select("[aria-label]").forEach {
                        append(' ')
                        append(it.attr("aria-label"))
                    }
                }.trim()
                val timeText = row.select(".time.notOver, .notOver, .time, .overTime, [class*=time]")
                    .joinToString(" ") { it.text().trim() }
                    .trim()

                // 从整行文本中移除标题，避免标题文字干扰状态判断
                val bodyWithoutTitle = row.text().replaceFirst(title, "").trim()
                val metaText = "$statusText $ariaText $bodyWithoutTitle".trim()
                val metaWithoutTime = if (timeText.isNotBlank()) metaText.replace(timeText, " ") else metaText

                // 严格过滤：只要状态/属性/行内元信息命中已完成、已提交、批阅中、已过期或具体分数，直接跳过
                if (WORK_FINISHED_KEYWORDS.any { metaText.contains(it) }) continue
                if (SCORE_REGEX.containsMatchIn(stripDurationForScoreCheck(metaWithoutTime))) continue

                // 明确命中待提交/未完成状态（不再把单独的“进行中”误判为未交）
                val hasExplicitPending = WORK_PENDING_KEYWORDS.any { statusText.contains(it) } ||
                    (statusText.isBlank() && WORK_PENDING_KEYWORDS.any { metaText.contains(it) }) ||
                    (lowerLink.contains("dowork") && statusText.isBlank() && timeText.contains("剩余"))
                if (!hasExplicitPending) continue

                // 解析真实截止时间；解析不到则置为 0L（显示“无截止时间”），不伪造 +24h
                val parsedDeadline = parseSingleDeadlineMillis(timeText, now)
                    ?: parseSingleDeadlineMillis(ariaText, now)
                    ?: parseSingleDeadlineMillis(bodyWithoutTitle, now)
                    ?: 0L

                // 如果有明确截止时间且已过截止，跳过
                if (parsedDeadline > 0L && parsedDeadline <= now) continue

                // 如果标记了 .overTime / .isOver / .expired 且无剩余时间，视为已过期跳过
                val rowClasses = row.className().lowercase()
                val hasOverTimeClass = (
                    row.selectFirst(".overTime, .isOver, .expired") != null ||
                        rowClasses.contains("overtime") ||
                        rowClasses.contains("isover") ||
                        rowClasses.contains("expired")
                    ) &&
                    row.selectFirst(".notOver") == null &&
                    parsedDeadline <= 0L
                if (hasOverTimeClass) continue

                val workIdKey = Regex("""workId=(\d+)""").find(dataLink)?.groupValues?.getOrNull(1)
                    ?: title.hashCode().toString()

                items += StudyTaskEntity(
                    id = "hw_${course.courseId}_${course.classId}_$workIdKey",
                    type = StudyTaskType.HOMEWORK.name,
                    title = title,
                    courseName = course.name,
                    courseId = course.courseId,
                    classId = course.classId,
                    deadlineMillis = parsedDeadline,
                    actionUrl = dataLink.ifBlank { fallbackUrl },
                    extraInfo = when {
                        metaText.contains("待重做") -> "待重做"
                        else -> ""
                    }
                )
            }
            return items
        }

        /**
         * 解析超星考试列表 HTML：
         * 1. 严格剔除已完成/已交卷/批阅中/已结束/已过期/缺考/未参加/已有分数的考试。
         * 2. 准确区分「进行中」与「未开始」，并从时间区间中提取对应截止/开考时间。
         */
        internal fun parseExamListHtml(
            html: String,
            course: ChaoxingCourseInfo,
            fallbackUrl: String,
            now: Long
        ): List<StudyTaskEntity> {
            val doc: Document = Jsoup.parse(html)
            val rows = selectListRows(doc)
            val items = mutableListOf<StudyTaskEntity>()

            for (row in rows) {
                val title = row.selectFirst("p.overHidden2, .overHidden2, .tit, .examName, td:first-child")
                    ?.text()?.trim()
                    ?.ifBlank { null }
                    ?: row.selectFirst("p, a")?.text()?.trim().orEmpty()
                if (title.isBlank()) continue

                val dataLink = row.attr("data").ifBlank { row.attr("onclick") }
                val lowerLink = dataLink.lowercase()
                val isViewOnlyExam = (
                    lowerLink.contains("/exam/view") ||
                        lowerLink.contains("exam/look") ||
                        lowerLink.contains("exam/score") ||
                        lowerLink.contains("markpaper")
                    ) && !lowerLink.contains("doexam") && !lowerLink.contains("reversionteststart")
                if (isViewOnlyExam) continue

                val statusText = row.select(".examStatus, .status, .status_text, .fl.status, [class*=status]")
                    .joinToString(" ") { it.text().trim() }
                    .trim()
                val ariaText = buildString {
                    append(row.attr("aria-label"))
                    row.select("[aria-label]").forEach {
                        append(' ')
                        append(it.attr("aria-label"))
                    }
                }.trim()
                val timeText = row.select(".time.notOver, .notOver, .time, .overTime, [class*=time]")
                    .joinToString(" ") { it.text().trim() }
                    .trim()

                val bodyWithoutTitle = row.text().replaceFirst(title, "").trim()
                val metaText = "$statusText $ariaText $bodyWithoutTitle".trim()
                val metaWithoutTime = if (timeText.isNotBlank()) metaText.replace(timeText, " ") else metaText

                // 严格排除已考完、已结束、缺考、未参加、已评分等无关状态
                if (EXAM_FINISHED_KEYWORDS.any { metaText.contains(it) }) continue
                if (SCORE_REGEX.containsMatchIn(stripDurationForScoreCheck(metaWithoutTime))) continue

                val isOngoing = EXAM_ONGOING_KEYWORDS.any { metaText.contains(it) }
                val isNotStarted = EXAM_NOT_STARTED_KEYWORDS.any { metaText.contains(it) }
                if (!isOngoing && !isNotStarted) continue

                val timeSource = listOf(timeText, ariaText, bodyWithoutTitle)
                    .firstOrNull { it.isNotBlank() && (extractAllDateTimesMillis(it).isNotEmpty() || parseRemainingFromText(it, now) != null) }
                    .orEmpty()

                val timestamps = extractAllDateTimesMillis(timeSource)
                val (effectiveTime, stateLabel) = when {
                    timestamps.size >= 2 -> {
                        val startTs = timestamps[0]
                        val endTs = timestamps[1]
                        if (endTs <= now) continue // 考试区间已结束
                        if (isNotStarted || startTs > now) {
                            startTs to "未开始"
                        } else {
                            endTs to "进行中"
                        }
                    }
                    timestamps.size == 1 -> {
                        val ts = timestamps[0]
                        if (ts <= now) continue
                        ts to if (isNotStarted) "未开始" else "进行中"
                    }
                    else -> {
                        val rem = parseRemainingFromText(timeSource, now) ?: 0L
                        rem to if (isNotStarted) "未开始" else "进行中"
                    }
                }

                val examIdKey = Regex("""(?:examId|testPaperId|id)=(\d+)""").find(dataLink)?.groupValues?.getOrNull(1)
                    ?: title.hashCode().toString()

                items += StudyTaskEntity(
                    id = "exam_${course.courseId}_${course.classId}_$examIdKey",
                    type = StudyTaskType.EXAM.name,
                    title = title,
                    courseName = course.name,
                    courseId = course.courseId,
                    classId = course.classId,
                    deadlineMillis = effectiveTime,
                    actionUrl = dataLink.ifBlank { fallbackUrl },
                    extraInfo = stateLabel
                )
            }
            return items
        }

        private fun stripDurationForScoreCheck(text: String): String {
            return text
                .replace(Regex("""(?:剩余|还剩|距截止|距开考)[^，,。；;]*"""), " ")
                .replace(Regex("""\d+\s*小时\s*\d+\s*分(?!数)"""), " ")
        }

        private fun selectListRows(doc: Document): List<org.jsoup.nodes.Element> {
            val bottomItems = doc.select("div.bottomList ul li")
            if (bottomItems.isNotEmpty()) return bottomItems
            val goTaskItems = doc.select("li[onclick*=goTask], li[data]")
            if (goTaskItems.isNotEmpty()) return goTaskItems
            return doc.select("tr.dataTr")
        }

        internal fun parseSingleDeadlineMillis(text: String, now: Long): Long? {
            if (text.isBlank()) return null
            // 1. 优先匹配 "剩余 / 还剩 / 距截止还有 X天X小时X分钟"
            parseRemainingFromText(text, now)?.let { return it }

            // 2. 提取所有具体日期时间；如果是时间区间（开始 ~ 截止），取最后一个作为截止时间
            val allTs = extractAllDateTimesMillis(text)
            return allTs.lastOrNull()
        }

        internal fun extractAllDateTimesMillis(text: String): List<Long> {
            if (text.isBlank()) return emptyList()
            val results = mutableListOf<Long>()
            val zone = ZoneId.systemDefault()
            val currentYear = LocalDate.now().year

            // 匹配 yyyy-MM-dd HH:mm 或 yyyy/MM/dd HH:mm 或 yyyy年MM月dd日 HH:mm
            val fullDateRegex = Regex("""(\d{4})[-/年](\d{1,2})[-/月](\d{1,2})日?\s+(\d{1,2}):(\d{2})""")
            fullDateRegex.findAll(text).forEach { m ->
                val y = m.groupValues[1].toIntOrNull() ?: return@forEach
                val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
                val d = m.groupValues[3].toIntOrNull() ?: return@forEach
                val h = m.groupValues[4].toIntOrNull() ?: return@forEach
                val mi = m.groupValues[5].toIntOrNull() ?: return@forEach
                runCatching {
                    results += LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()
                }
            }
            if (results.isNotEmpty()) return results

            // 匹配不带年份的 MM-dd HH:mm 或 MM/dd HH:mm 或 MM月dd日 HH:mm
            val shortDateRegex = Regex("""(?<!\d[-/年])(\d{1,2})[-/月](\d{1,2})日?\s+(\d{1,2}):(\d{2})""")
            shortDateRegex.findAll(text).forEach { m ->
                val mo = m.groupValues[1].toIntOrNull() ?: return@forEach
                val d = m.groupValues[2].toIntOrNull() ?: return@forEach
                val h = m.groupValues[3].toIntOrNull() ?: return@forEach
                val mi = m.groupValues[4].toIntOrNull() ?: return@forEach
                runCatching {
                    results += LocalDateTime.of(currentYear, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()
                }
            }
            return results
        }

        internal fun parseRemainingFromText(text: String, now: Long): Long? {
            if (!text.contains("剩余") && !text.contains("还剩") && !text.contains("距截止") && !text.contains("距开考")) {
                return null
            }
            val days = Regex("""(\d+)\s*天""").find(text)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            val hours = Regex("""(\d+)\s*小时""").find(text)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            val mins = Regex("""(\d+)\s*分""").find(text)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            val total = days * 86_400_000L + hours * 3_600_000L + mins * 60_000L
            return if (total > 0L) now + total else null
        }
    }
}
