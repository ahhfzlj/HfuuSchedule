package com.hfuu.schedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.data.model.CourseEntity
import com.hfuu.schedule.data.model.ScheduleMemoEntity
import com.hfuu.schedule.data.model.SemesterConfig
import com.hfuu.schedule.data.model.TimeSlot
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesCobaltLight
import com.hfuu.schedule.ui.theme.MassesCrimson
import com.hfuu.schedule.ui.theme.MassesGridBlue
import com.hfuu.schedule.ui.theme.MassesInk
import com.hfuu.schedule.ui.theme.TopBarMascot
import com.hfuu.schedule.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

private val DAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")
private val ROW_HEIGHT: Dp = 64.dp

private data class NewMemoSlot(
    val week: Int,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int? = null,
    val courseNameHint: String = ""
)

@Composable
fun ScheduleScreen(
    viewModel: MainViewModel,
    onOpenJwxtImport: () -> Unit,
    onOpenTimeSettings: () -> Unit
) {
    val courses by viewModel.courses.collectAsState()
    val memos by viewModel.memos.collectAsState()
    val timeSlots by viewModel.timeSlots.collectAsState()
    val semester by viewModel.semesterConfig.collectAsState()
    val selectedWeek by viewModel.selectedWeek.collectAsState()
    val hideWeekends by viewModel.hideWeekends.collectAsState()
    val memoEnabled by viewModel.memoEnabled.collectAsState()

    val visibleDayCount = if (hideWeekends) 5 else 7
    val currentWeek = remember(semester) { semester.currentWeek() }
    val today = remember { LocalDate.now() }
    val totalWeeks = semester.totalWeeks.coerceAtLeast(1)
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = (selectedWeek - 1).coerceIn(0, totalWeeks - 1),
        pageCount = { totalWeeks }
    )

    LaunchedEffect(pagerState.currentPage) {
        val pagedWeek = pagerState.currentPage + 1
        if (pagedWeek != selectedWeek) {
            viewModel.selectWeek(pagedWeek)
        }
    }

    LaunchedEffect(selectedWeek, totalWeeks) {
        val targetPage = (selectedWeek - 1).coerceIn(0, totalWeeks - 1)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    var detailCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var editingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var showAddCourseDialog by remember { mutableStateOf(false) }

    var detailMemo by remember { mutableStateOf<ScheduleMemoEntity?>(null) }
    var editingMemo by remember { mutableStateOf<ScheduleMemoEntity?>(null) }
    var newMemoSlot by remember { mutableStateOf<NewMemoSlot?>(null) }
    var showAddChooser by remember { mutableStateOf(false) }

    val activeWeek = (pagerState.currentPage + 1).coerceIn(1, totalWeeks)
    val activeWeekCourses = remember(courses, activeWeek, visibleDayCount) {
        deduplicateWeekCourses(courses, activeWeek, visibleDayCount)
    }
    val activeWeekMemoCount = remember(memos, activeWeek, visibleDayCount, memoEnabled) {
        if (!memoEnabled) 0
        else memos.count { it.isInWeek(activeWeek) && it.dayOfWeek <= visibleDayCount }
    }

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (isMasses) {
            MassesBlueprintBackdrop(variant = MassesBackdropVariant.SCHEDULE)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏：周次切换 + 教务导入 + 添加按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
            ) {
                if (isMasses) {
                    Text(
                        text = "CHRONICLE",
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        letterSpacing = 5.sp,
                        color = MassesCobalt.copy(alpha = 0.065f),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 110.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMasses) {
                        TopBarMascot(
                            size = 32.dp,
                            onQuote = { viewModel.postToast(it) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isMasses) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(17.dp)
                                        .background(MassesCobalt)
                                )
                                Spacer(Modifier.width(3.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (activeWeek > 1) {
                                        coroutineScope.launch { pagerState.animateScrollToPage(activeWeek - 2) }
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "上一周", tint = colors.textPrimary)
                            }
                            Text(
                                text = "第 $activeWeek 周",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                letterSpacing = if (isMasses) 0.5.sp else 0.sp,
                                maxLines = 1
                            )
                            IconButton(
                                onClick = {
                                    if (activeWeek < totalWeeks) {
                                        coroutineScope.launch { pagerState.animateScrollToPage(activeWeek) }
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "下一周", tint = colors.textPrimary)
                            }
                            if (activeWeek != currentWeek) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isMasses) "本周 ↵" else "回本周",
                                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                    fontSize = 10.sp,
                                    color = if (isMasses) MassesCrimson else colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                                        .background(if (isMasses) colors.accentCrimsonBg else colors.primaryLight)
                                        .then(
                                            if (isMasses) Modifier.border(0.8.dp, MassesCrimson.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                            else Modifier
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage((currentWeek - 1).coerceIn(0, totalWeeks - 1))
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        val memoSuffix = if (memoEnabled && activeWeekMemoCount > 0) " · $activeWeekMemoCount 备忘" else ""
                        Text(
                            text = if (isMasses) {
                                "${semester.shortLabel} // W-${activeWeek.toString().padStart(2, '0')} · ${semester.formatDateRangeForWeek(activeWeek, visibleDayCount)} · ${activeWeekCourses.size}C$memoSuffix"
                            } else {
                                "${semester.shortLabel} · ${semester.formatDateRangeForWeek(activeWeek, visibleDayCount)} · ${activeWeekCourses.size} 节课$memoSuffix"
                            },
                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                            fontSize = if (isMasses) 10.sp else 11.5.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    Surface(
                        shape = RoundedCornerShape(if (isMasses) 2.dp else 18.dp),
                        color = colors.primaryLight,
                        modifier = Modifier
                            .then(
                                if (isMasses) Modifier.border(1.dp, MassesCobalt.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                                else Modifier
                            )
                            .clickable { onOpenJwxtImport() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = "教务导入",
                                tint = colors.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("教务导入", fontSize = 11.5.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(if (isMasses) RoundedCornerShape(2.dp) else CircleShape)
                            .background(if (isMasses) MassesInk else colors.primary)
                            .clickable {
                                if (memoEnabled) {
                                    showAddChooser = true
                                } else {
                                    showAddCourseDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "添加",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        if (isMasses) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(5.dp)
                                    .background(MassesCobalt)
                            )
                        }
                    }
                }
            }

            if (isMasses) {
                Row(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MassesCobalt.copy(alpha = 0.6f)))
                    Box(modifier = Modifier.width(24.dp).fillMaxHeight().background(MassesInk))
                    Box(modifier = Modifier.width(8.dp).fillMaxHeight().background(MassesCrimson))
                }
            }

            // 支持左右滑动切换周数的 HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageWeek = page + 1
                val pageCourses = remember(courses, pageWeek, visibleDayCount) {
                    deduplicateWeekCourses(courses, pageWeek, visibleDayCount)
                }
                val pageMemos = remember(memos, pageWeek, visibleDayCount, memoEnabled) {
                    if (!memoEnabled) emptyList()
                    else memos.filter { it.isInWeek(pageWeek) && it.dayOfWeek <= visibleDayCount }
                }
                WeekSchedulePage(
                    week = pageWeek,
                    semester = semester,
                    today = today,
                    visibleDayCount = visibleDayCount,
                    timeSlots = timeSlots,
                    weekCourses = pageCourses,
                    weekMemos = pageMemos,
                    memoEnabled = memoEnabled,
                    onOpenTimeSettings = onOpenTimeSettings,
                    onCourseClick = { detailCourse = it },
                    onMemoClick = { detailMemo = it },
                    onEmptySlotClick = { day, section ->
                        newMemoSlot = NewMemoSlot(week = pageWeek, dayOfWeek = day, startSection = section)
                    }
                )
            }
        }
    }

    // 备忘模式开启时，点击右上角 + 选择添加课程还是添加备忘
    if (showAddChooser) {
        AlertDialog(
            onDismissRequest = { showAddChooser = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("添加到第 $activeWeek 周", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = metrics.smallCardShape,
                        color = colors.primaryLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isMasses) Modifier.border(1.dp, MassesCobalt.copy(alpha = 0.45f), metrics.smallCardShape)
                                else Modifier
                            )
                            .clickable {
                                showAddChooser = false
                                showAddCourseDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("添加课程", fontWeight = FontWeight.SemiBold, color = colors.primary, fontSize = 15.sp)
                                Text("手动新增一门常规课程", fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                    }
                    Surface(
                        shape = metrics.smallCardShape,
                        color = colors.memoBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isMasses) Modifier.border(1.dp, colors.memoBorder, metrics.smallCardShape)
                                else Modifier
                            )
                            .clickable {
                                showAddChooser = false
                                newMemoSlot = NewMemoSlot(week = activeWeek, dayOfWeek = 1, startSection = 1)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.EventNote, contentDescription = null, tint = colors.memoAccent)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("添加时段备忘", fontWeight = FontWeight.SemiBold, color = colors.memoText, fontSize = 15.sp)
                                Text("也可直接点击课程卡片或空白格子添加", fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddChooser = false }) { Text("取消", color = colors.textSecondary) }
            }
        )
    }

    // 课程详情弹窗（备忘录开启时展示本堂课关联备忘；若该时段存在冲突课程，支持一键切换查看）
    detailCourse?.let { course ->
        val attachedMemos = remember(memos, activeWeek, course, memoEnabled) {
            if (!memoEnabled) emptyList()
            else memos.filter {
                it.isInWeek(activeWeek) &&
                    it.dayOfWeek == course.dayOfWeek &&
                    it.startSection <= course.endSection &&
                    it.endSection >= course.startSection
            }
        }
        val conflictingCourses = remember(activeWeekCourses, course) {
            activeWeekCourses.filter {
                it.dayOfWeek == course.dayOfWeek &&
                    it.startSection <= course.endSection &&
                    it.endSection >= course.startSection
            }
        }
        CourseDetailDialog(
            course = course,
            conflictingCourses = conflictingCourses,
            onSwitchCourse = { detailCourse = it },
            timeSlots = timeSlots,
            memoEnabled = memoEnabled,
            attachedMemos = attachedMemos,
            onAddCourseMemo = {
                detailCourse = null
                newMemoSlot = NewMemoSlot(
                    week = activeWeek,
                    dayOfWeek = course.dayOfWeek,
                    startSection = course.startSection,
                    endSection = course.endSection,
                    courseNameHint = course.name
                )
            },
            onOpenMemo = { memo ->
                detailCourse = null
                detailMemo = memo
            },
            onToggleMemoDone = { memo -> viewModel.toggleMemoDone(memo) },
            onDismiss = { detailCourse = null },
            onEdit = {
                detailCourse = null
                editingCourse = course
            },
            onDelete = {
                viewModel.deleteCourse(course)
                detailCourse = null
            }
        )
    }

    // 新增/编辑课程弹窗
    if (showAddCourseDialog || editingCourse != null) {
        CourseEditorDialog(
            initial = editingCourse,
            maxSection = timeSlots.size.coerceAtLeast(10),
            onDismiss = {
                showAddCourseDialog = false
                editingCourse = null
            },
            onConfirm = { saved ->
                viewModel.saveCourse(saved)
                showAddCourseDialog = false
                editingCourse = null
            }
        )
    }

    // 备忘详情弹窗
    detailMemo?.let { memo ->
        MemoDetailDialog(
            memo = memo,
            timeSlots = timeSlots,
            onDismiss = { detailMemo = null },
            onToggleDone = {
                viewModel.toggleMemoDone(memo)
                detailMemo = null
            },
            onEdit = {
                detailMemo = null
                editingMemo = memo
            },
            onDelete = {
                viewModel.deleteMemo(memo)
                detailMemo = null
            }
        )
    }

    // 新增/编辑备忘弹窗
    if (newMemoSlot != null || editingMemo != null) {
        MemoEditorDialog(
            initial = editingMemo,
            presetSlot = newMemoSlot,
            currentWeek = activeWeek,
            visibleDayCount = visibleDayCount,
            maxSection = timeSlots.size.coerceAtLeast(10),
            onDismiss = {
                newMemoSlot = null
                editingMemo = null
            },
            onConfirm = { saved ->
                viewModel.saveMemo(saved)
                newMemoSlot = null
                editingMemo = null
            }
        )
    }
}

private fun deduplicateWeekCourses(
    courses: List<CourseEntity>,
    week: Int,
    visibleDayCount: Int
): List<CourseEntity> {
    // 仅去除完全重复的同一课程同一教室记录，保留所有时间冲突的不同课程或不同教室以便并排完整展示
    return courses
        .filter { it.isInWeek(week) && it.dayOfWeek in 1..visibleDayCount }
        .sortedWith(
            compareBy<CourseEntity> { it.startSection }
                .thenByDescending { it.endSection }
                .thenBy { it.location.isBlank() || it.location == "待定教室" }
                .thenBy { it.weekList().size }
                .thenBy { it.name }
        )
        .distinctBy { "${it.name.trim()}_${it.dayOfWeek}_${it.startSection}_${it.endSection}_${it.location.trim()}" }
}

private data class CourseSlotLayout(
    val course: CourseEntity,
    val laneIndex: Int,
    val laneCount: Int
)

/**
 * 为同一天内可能存在时间重叠（冲突）的课程计算并排分列布局：
 * - 无冲突时 laneCount = 1，占满整列；
 * - 有冲突（完全重叠或部分跨节重叠）时，将同一冲突簇按横向多列并排展示，避免任何课程被遮挡隐藏。
 */
private fun computeDayCourseLayouts(dayCourses: List<CourseEntity>): List<CourseSlotLayout> {
    if (dayCourses.isEmpty()) return emptyList()
    val sorted = dayCourses.sortedWith(
        compareBy<CourseEntity> { it.startSection }
            .thenByDescending { it.endSection }
            .thenBy { it.name }
            .thenBy { it.location }
    )

    // 1. 按重叠连通分量拆分成冲突簇
    val clusters = mutableListOf<MutableList<CourseEntity>>()
    var currentCluster = mutableListOf<CourseEntity>()
    var clusterMaxEnd = -1
    for (course in sorted) {
        if (currentCluster.isEmpty() || course.startSection <= clusterMaxEnd) {
            currentCluster.add(course)
            clusterMaxEnd = maxOf(clusterMaxEnd, course.endSection)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(course)
            clusterMaxEnd = course.endSection
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    // 2. 在每个冲突簇内贪心分配互不重叠的横向轨道（lane）
    val result = mutableListOf<CourseSlotLayout>()
    for (cluster in clusters) {
        val laneEndSections = mutableListOf<Int>()
        val assigned = mutableListOf<Pair<CourseEntity, Int>>()
        for (course in cluster) {
            var targetLane = laneEndSections.indexOfFirst { lastEnd -> lastEnd < course.startSection }
            if (targetLane == -1) {
                targetLane = laneEndSections.size
                laneEndSections.add(course.endSection)
            } else {
                laneEndSections[targetLane] = course.endSection
            }
            assigned.add(course to targetLane)
        }
        val totalLanes = laneEndSections.size.coerceAtLeast(1)
        for ((course, lane) in assigned) {
            result.add(CourseSlotLayout(course = course, laneIndex = lane, laneCount = totalLanes))
        }
    }
    return result
}

@Composable
private fun WeekSchedulePage(
    week: Int,
    semester: SemesterConfig,
    today: LocalDate,
    visibleDayCount: Int,
    timeSlots: List<TimeSlot>,
    weekCourses: List<CourseEntity>,
    weekMemos: List<ScheduleMemoEntity>,
    memoEnabled: Boolean,
    onOpenTimeSettings: () -> Unit,
    onCourseClick: (CourseEntity) -> Unit,
    onMemoClick: (ScheduleMemoEntity) -> Unit,
    onEmptySlotClick: (dayOfWeek: Int, section: Int) -> Unit
) {
    val isFiveDayMode = visibleDayCount == 5
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    Column(modifier = Modifier.fillMaxSize()) {
        // 星期与日期表头（自动适配 5 天或 7 天列宽）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isMasses) colors.surface.copy(alpha = 0.96f) else colors.surface)
                .padding(bottom = 8.dp, top = if (isMasses) 6.dp else 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .clickable { onOpenTimeSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMasses) "${semester.dateFor(week, 1).monthValue}M" else "${semester.dateFor(week, 1).monthValue}月",
                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMasses) MassesCobalt else colors.textSecondary
                )
            }

            for (day in 1..visibleDayCount) {
                val date = semester.dateFor(week, day)
                val isToday = date == today
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = if (isFiveDayMode) 4.dp else 2.dp)
                        .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                        .background(if (isToday) colors.primary else Color.Transparent)
                        .then(
                            if (isMasses && !isToday) {
                                Modifier.border(0.6.dp, colors.divider.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                            } else Modifier
                        )
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMasses && isToday) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(5.dp)
                                .background(MassesCrimson)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFiveDayMode) "周${DAY_LABELS[day - 1]}" else DAY_LABELS[day - 1],
                            fontSize = if (isFiveDayMode) 13.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isToday) Color.White else colors.textPrimary
                        )
                        Text(
                            text = if (isMasses) date.dayOfMonth.toString().padStart(2, '0') else "${date.dayOfMonth}",
                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 10.5.sp,
                            color = if (isToday) Color.White.copy(alpha = 0.92f) else colors.textTertiary
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = if (isMasses) MassesCobalt.copy(alpha = 0.35f) else colors.divider,
            thickness = if (isMasses) 0.9.dp else 0.6.dp
        )

        // 课表网格主体
        val scrollState = rememberScrollState()
        val totalRows = timeSlots.size.coerceAtLeast(10)
        val totalHeight = ROW_HEIGHT * totalRows

        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            // 左侧节次与作息时间列
            Column(
                modifier = Modifier
                    .width(46.dp)
                    .height(totalHeight)
                    .background(if (isMasses) colors.surface.copy(alpha = 0.92f) else colors.surface)
                    .clickable { onOpenTimeSettings() }
            ) {
                timeSlots.forEach { slot ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT)
                            .border(
                                width = 0.35.dp,
                                color = if (isMasses) MassesGridBlue.copy(alpha = 0.16f) else colors.divider.copy(alpha = 0.5f)
                            )
                    ) {
                        if (isMasses) {
                            // 蓝图刻度尺微标
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .width(5.dp)
                                    .height(1.dp)
                                    .background(MassesCobalt.copy(alpha = 0.55f))
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isMasses) slot.section.toString().padStart(2, '0') else "${slot.section}",
                                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                fontSize = if (isMasses) 12.sp else 13.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMasses) MassesInk else colors.textPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = slot.startTime,
                                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                fontSize = 8.5.sp,
                                lineHeight = 10.5.sp,
                                color = colors.textTertiary
                            )
                            Text(
                                text = slot.endTime,
                                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                fontSize = 8.5.sp,
                                lineHeight = 10.5.sp,
                                color = colors.textTertiary
                            )
                        }
                    }
                }
            }

            // 5天或7天课程与备忘列
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(totalHeight)
            ) {
                for (day in 1..visibleDayCount) {
                    val dayCourses = weekCourses.filter { it.dayOfWeek == day }
                    val dayMemos = weekMemos.filter { it.dayOfWeek == day }
                    // 仅将未与任何课程重叠的备忘作为独立卡片绘制；与课程重叠的备忘直接内嵌在课程卡片上展示，避免遮挡课程
                    val standaloneMemos = dayMemos.filter { memo ->
                        dayCourses.none { course ->
                            memo.startSection <= course.endSection && memo.endSection >= course.startSection
                        }
                    }
                    val dayCourseLayouts = remember(dayCourses) { computeDayCourseLayouts(dayCourses) }
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val columnWidth = maxWidth
                        // 背景网格线：开启备忘录时点击空白格子可直接添加该时段备忘
                        Column(modifier = Modifier.fillMaxSize()) {
                            repeat(totalRows) { rowIdx ->
                                val section = rowIdx + 1
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ROW_HEIGHT)
                                        .border(
                                            width = 0.35.dp,
                                            color = if (isMasses) MassesGridBlue.copy(alpha = 0.12f) else colors.divider.copy(alpha = 0.6f)
                                        )
                                        .then(
                                            if (memoEnabled) {
                                                Modifier.clickable { onEmptySlotClick(day, section) }
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        }

                        // 课程色块（支持冲突课程并排完整展示，并带冲突高亮边框与角标）
                        dayCourseLayouts.forEach { slotLayout ->
                            val course = slotLayout.course
                            val isConflict = slotLayout.laneCount > 1
                            val laneWidth = columnWidth / slotLayout.laneCount
                            val leftOffset = laneWidth * slotLayout.laneIndex

                            val startIdx = (course.startSection - 1).coerceIn(0, totalRows - 1)
                            val span = (course.endSection - course.startSection + 1).coerceAtLeast(1)
                            val topOffset = ROW_HEIGHT * startIdx
                            val cardHeight = ROW_HEIGHT * span
                            val palette = colors.coursePalettes[course.colorIndex.mod(colors.coursePalettes.size)]
                            val courseMemos = dayMemos.filter {
                                it.startSection <= course.endSection && it.endSection >= course.startSection
                            }
                            val firstActiveMemo = courseMemos.firstOrNull { !it.isDone } ?: courseMemos.firstOrNull()

                            Box(
                                modifier = Modifier
                                    .width(laneWidth)
                                    .offset(x = leftOffset, y = topOffset)
                                    .height(cardHeight)
                                    .padding(
                                        horizontal = if (isConflict) 1.dp else if (isFiveDayMode) 3.dp else 2.dp,
                                        vertical = if (isFiveDayMode) 3.dp else 2.dp
                                    )
                                    .clip(metrics.courseShape)
                                    .background(palette.container.copy(alpha = if (isMasses) 0.94f else 1f))
                                    .then(
                                        when {
                                            isConflict -> Modifier.border(
                                                width = 1.dp,
                                                color = colors.danger.copy(alpha = 0.72f),
                                                shape = metrics.courseShape
                                            )
                                            isMasses -> Modifier.border(
                                                width = 0.9.dp,
                                                color = palette.accent.copy(alpha = 0.52f),
                                                shape = metrics.courseShape
                                            )
                                            else -> Modifier
                                        }
                                    )
                                    .clickable { onCourseClick(course) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(if (isConflict) 2.dp else 3.dp)
                                        .fillMaxHeight()
                                        .background(if (isConflict) colors.danger else palette.accent)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            start = when {
                                                isConflict -> 4.dp
                                                isFiveDayMode -> 8.dp
                                                else -> 6.dp
                                            },
                                            end = when {
                                                isConflict -> 2.dp
                                                isFiveDayMode -> 6.dp
                                                else -> 4.dp
                                            },
                                            top = if (isConflict) 3.dp else 5.dp,
                                            bottom = if (isConflict) 3.dp else 4.dp
                                        ),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = course.name,
                                        fontSize = when {
                                            isConflict && !isFiveDayMode -> 9.sp
                                            isConflict -> 10.sp
                                            isFiveDayMode -> 12.sp
                                            else -> 11.sp
                                        },
                                        lineHeight = when {
                                            isConflict && !isFiveDayMode -> 10.5.sp
                                            isConflict -> 12.sp
                                            isFiveDayMode -> 14.5.sp
                                            else -> 13.sp
                                        },
                                        fontWeight = if (isMasses) FontWeight.Bold else FontWeight.SemiBold,
                                        color = palette.onContainer,
                                        maxLines = when {
                                            span == 1 -> 1
                                            firstActiveMemo != null -> 2
                                            isConflict -> 3
                                            isFiveDayMode -> 4
                                            else -> 3
                                        },
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (firstActiveMemo != null && !isConflict) {
                                            val allDone = courseMemos.all { it.isDone }
                                            val memoChipBg = if (allDone) colors.successBg else colors.memoBg
                                            val memoChipFg = if (allDone) colors.success else colors.memoText
                                            val memoChipBorder = if (allDone) colors.success.copy(alpha = 0.4f) else colors.memoBorder
                                            val label = if (courseMemos.size > 1) {
                                                "${firstActiveMemo.title} +${courseMemos.size - 1}"
                                            } else {
                                                firstActiveMemo.title
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(if (isMasses) 1.dp else 5.dp))
                                                    .background(memoChipBg)
                                                    .border(0.6.dp, memoChipBorder, RoundedCornerShape(if (isMasses) 1.dp else 5.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = (if (allDone) "✓ " else "注 ") + label,
                                                    fontSize = if (isFiveDayMode) 9.5.sp else 8.5.sp,
                                                    lineHeight = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = memoChipFg,
                                                    textDecoration = if (allDone) TextDecoration.LineThrough else TextDecoration.None,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (course.location.isNotBlank()) {
                                            Text(
                                                text = "@${course.location}",
                                                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                                fontSize = when {
                                                    isConflict && !isFiveDayMode -> 7.5.sp
                                                    isConflict -> 8.5.sp
                                                    isFiveDayMode -> 9.5.sp
                                                    else -> 9.sp
                                                },
                                                lineHeight = when {
                                                    isConflict -> 9.5.sp
                                                    isFiveDayMode -> 11.5.sp
                                                    else -> 11.sp
                                                },
                                                color = palette.onContainer.copy(alpha = 0.88f),
                                                maxLines = if (firstActiveMemo != null || isConflict) 2 else 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                // 右上角标识：冲突角标 > 备忘提示点 > 众生行迹角标像素
                                if (isConflict) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(colors.danger)
                                            .padding(horizontal = 2.dp, vertical = 0.5.dp)
                                    ) {
                                        Text(
                                            text = "冲",
                                            fontSize = 7.sp,
                                            lineHeight = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                } else if (firstActiveMemo != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(6.dp)
                                            .clip(if (isMasses) RoundedCornerShape(1.dp) else CircleShape)
                                            .background(if (courseMemos.all { it.isDone }) colors.success else colors.memoAccent)
                                    )
                                } else if (isMasses) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(4.dp)
                                            .background(palette.accent.copy(alpha = 0.75f))
                                    )
                                }
                            }
                        }

                        // 独立空白时段备忘卡片（便签视觉）
                        standaloneMemos.forEach { memo ->
                            val startIdx = (memo.startSection - 1).coerceIn(0, totalRows - 1)
                            val span = (memo.endSection - memo.startSection + 1).coerceAtLeast(1)
                            val topOffset = ROW_HEIGHT * startIdx
                            val cardHeight = ROW_HEIGHT * span
                            val bgColor = if (memo.isDone) colors.successBg else colors.memoBg
                            val accentColor = if (memo.isDone) colors.success else colors.memoAccent
                            val borderColor = if (memo.isDone) colors.success.copy(alpha = 0.35f) else colors.memoBorder
                            val fgColor = if (memo.isDone) colors.success else colors.memoText

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = topOffset)
                                    .height(cardHeight)
                                    .padding(if (isFiveDayMode) 3.dp else 2.dp)
                                    .clip(metrics.courseShape)
                                    .background(bgColor)
                                    .border(1.dp, borderColor, metrics.courseShape)
                                    .clickable { onMemoClick(memo) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .fillMaxHeight()
                                        .background(accentColor)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            start = if (isFiveDayMode) 8.dp else 6.dp,
                                            end = if (isFiveDayMode) 6.dp else 4.dp,
                                            top = 4.dp,
                                            bottom = 4.dp
                                        ),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = if (isMasses) {
                                                if (memo.isDone) "DONE" else "MEMO ↵"
                                            } else {
                                                if (memo.isDone) "已完成" else "备忘"
                                            },
                                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = 8.5.sp,
                                            lineHeight = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                        Spacer(Modifier.height(1.dp))
                                        Text(
                                            text = memo.title,
                                            fontSize = if (isFiveDayMode) 11.5.sp else 10.5.sp,
                                            lineHeight = if (isFiveDayMode) 14.sp else 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = fgColor,
                                            textDecoration = if (memo.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = if (span >= 2) 3 else 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (span >= 2 && memo.content.isNotBlank()) {
                                        Text(
                                            text = memo.content,
                                            fontSize = if (isFiveDayMode) 10.sp else 9.sp,
                                            lineHeight = 11.sp,
                                            color = fgColor.copy(alpha = 0.78f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoDetailDialog(
    memo: ScheduleMemoEntity,
    timeSlots: List<TimeSlot>,
    onDismiss: () -> Unit,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val startSlot = timeSlots.find { it.section == memo.startSection }
    val endSlot = timeSlots.find { it.section == memo.endSection }
    val clockRange = if (startSlot != null && endSlot != null) {
        "${startSlot.startTime} - ${endSlot.endTime}"
    } else {
        "第${memo.startSection}-${memo.endSection}节"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = metrics.dialogShape,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.EventNote,
                    contentDescription = null,
                    tint = if (memo.isDone) colors.success else colors.memoAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = memo.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textDecoration = if (memo.isDone) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(
                    Icons.Rounded.Schedule,
                    "时段",
                    "周${DAY_LABELS[(memo.dayOfWeek - 1).coerceIn(0, 6)]} 第${memo.startSection}-${memo.endSection}节 ($clockRange)"
                )
                Text(
                    text = "生效范围：${if (memo.week == 0) "每周重复" else "仅第 ${memo.week} 周"}",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                if (memo.content.isNotBlank()) {
                    Text(
                        text = "备注：${memo.content}",
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        lineHeight = 20.sp
                    )
                }
                Surface(
                    shape = metrics.smallCardShape,
                    color = if (memo.isDone) colors.successBg else colors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDone() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (memo.isDone) colors.success else colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (memo.isDone) "已完成（点击恢复待办）" else "标记为已完成",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (memo.isDone) colors.success else colors.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.danger, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除", color = colors.danger)
                }
                Button(
                    onClick = onEdit,
                    shape = metrics.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("编辑")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = colors.textSecondary) }
        }
    )
}

@Composable
private fun MemoEditorDialog(
    initial: ScheduleMemoEntity?,
    presetSlot: NewMemoSlot?,
    currentWeek: Int,
    visibleDayCount: Int,
    maxSection: Int,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleMemoEntity) -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember { mutableStateOf(initial?.content.orEmpty()) }
    var dayOfWeek by remember {
        mutableIntStateOf((initial?.dayOfWeek ?: presetSlot?.dayOfWeek ?: 1).coerceIn(1, visibleDayCount))
    }
    var startSection by remember {
        mutableIntStateOf((initial?.startSection ?: presetSlot?.startSection ?: 1).coerceIn(1, maxSection))
    }
    var endSection by remember {
        val defaultEnd = initial?.endSection
            ?: presetSlot?.endSection
            ?: ((presetSlot?.startSection ?: 1) + 1).coerceAtMost(maxSection)
        mutableIntStateOf(defaultEnd.coerceIn(startSection, maxSection))
    }
    var repeatWeekly by remember { mutableStateOf(initial?.week == 0) }
    val targetWeek = initial?.week?.takeIf { it > 0 } ?: presetSlot?.week ?: currentWeek
    val courseHint = presetSlot?.courseNameHint.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = metrics.dialogShape,
        title = {
            Column {
                Text(
                    text = if (initial == null) {
                        if (courseHint.isNotBlank()) "添加本堂课备忘" else "添加时段备忘"
                    } else {
                        "编辑时段备忘"
                    },
                    fontWeight = FontWeight.Bold
                )
                if (courseHint.isNotBlank()) {
                    Text(
                        text = "关联课程：$courseHint",
                        fontSize = 12.sp,
                        color = colors.memoAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("要做的事情 *") },
                    placeholder = {
                        Text(
                            if (courseHint.isNotBlank()) "例如：带实验报告 / 交纸质作业 / 随堂测验"
                            else "例如：去图书馆复习 / 社团例会"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("备注（选填）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("星期", fontSize = 12.sp, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    for (d in 1..visibleDayCount) {
                        val selected = dayOfWeek == d
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(if (metrics.isMasses) 2.dp else 8.dp))
                                .background(if (selected) colors.memoAccent else colors.background)
                                .clickable { dayOfWeek = d }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = DAY_LABELS[d - 1],
                                fontSize = 12.sp,
                                color = if (selected) Color.White else colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startSection.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull() ?: return@OutlinedTextField
                            startSection = v.coerceIn(1, maxSection)
                            if (endSection < startSection) endSection = startSection
                        },
                        label = { Text("开始节次") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endSection.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull() ?: return@OutlinedTextField
                            endSection = v.coerceIn(startSection, maxSection)
                        },
                        label = { Text("结束节次") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { repeatWeekly = !repeatWeekly }
                ) {
                    Checkbox(
                        checked = repeatWeekly,
                        onCheckedChange = { repeatWeekly = it },
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                    )
                    Text(
                        text = if (repeatWeekly) "每周此时段重复提醒" else "仅在第 $targetWeek 周显示（勾选可每周重复）",
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(
                        ScheduleMemoEntity(
                            id = initial?.id ?: 0L,
                            title = title.trim(),
                            content = content.trim(),
                            week = if (repeatWeekly) 0 else targetWeek,
                            dayOfWeek = dayOfWeek,
                            startSection = startSection,
                            endSection = endSection,
                            isDone = initial?.isDone ?: false
                        )
                    )
                },
                shape = metrics.buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        }
    )
}

@Composable
private fun CourseDetailDialog(
    course: CourseEntity,
    conflictingCourses: List<CourseEntity>,
    onSwitchCourse: (CourseEntity) -> Unit,
    timeSlots: List<TimeSlot>,
    memoEnabled: Boolean,
    attachedMemos: List<ScheduleMemoEntity>,
    onAddCourseMemo: () -> Unit,
    onOpenMemo: (ScheduleMemoEntity) -> Unit,
    onToggleMemoDone: (ScheduleMemoEntity) -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val startSlot = timeSlots.find { it.section == course.startSection }
    val endSlot = timeSlots.find { it.section == course.endSection }
    val clockRange = if (startSlot != null && endSlot != null) {
        "${startSlot.startTime} - ${endSlot.endTime}"
    } else {
        "第${course.startSection}-${course.endSection}节"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = metrics.dialogShape,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(course.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (conflictingCourses.size > 1) {
                    Text(
                        text = "此时段共有 ${conflictingCourses.size} 门冲突课程，可点击下方切换",
                        fontSize = 11.5.sp,
                        color = colors.danger,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (conflictingCourses.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        conflictingCourses.forEach { item ->
                            val isCurrent = item.id == course.id &&
                                item.name == course.name &&
                                item.location == course.location &&
                                item.startSection == course.startSection &&
                                item.endSection == course.endSection
                            Surface(
                                shape = metrics.smallCardShape,
                                color = if (isCurrent) colors.primaryLight else colors.background,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isCurrent) 1.dp else 0.6.dp,
                                        color = if (isCurrent) colors.primary else colors.divider,
                                        shape = metrics.smallCardShape
                                    )
                                    .clickable { onSwitchCourse(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) colors.primary else colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "第${item.startSection}-${item.endSection}节 · ${item.location.ifBlank { "待定教室" }}",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isCurrent) {
                                        Text(
                                            text = "当前",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = colors.divider)
                }
                DetailRow(Icons.Rounded.LocationOn, "教室", course.location.ifBlank { "未填写" })
                DetailRow(Icons.Rounded.Person, "教师", course.teacher.ifBlank { "未填写" })
                DetailRow(
                    Icons.Rounded.Schedule,
                    "时间",
                    "周${DAY_LABELS[(course.dayOfWeek - 1).coerceIn(0, 6)]} 第${course.startSection}-${course.endSection}节 ($clockRange)"
                )
                Text(
                    text = "上课周次：${course.formatWeeksSummary()}",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                if (memoEnabled) {
                    HorizontalDivider(color = colors.divider)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "本堂课备忘",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Surface(
                            shape = metrics.pillShape,
                            color = colors.memoBg,
                            modifier = Modifier
                                .border(0.8.dp, colors.memoBorder, metrics.pillShape)
                                .clickable { onAddCourseMemo() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = colors.memoAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "添加备忘",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.memoText
                                )
                            }
                        }
                    }

                    if (attachedMemos.isEmpty()) {
                        Text(
                            text = "暂无备忘，可点击右上角记录这堂课要带的东西或任务",
                            fontSize = 12.sp,
                            color = colors.textTertiary
                        )
                    } else {
                        attachedMemos.forEach { memo ->
                            Surface(
                                shape = metrics.smallCardShape,
                                color = if (memo.isDone) colors.successBg else colors.memoBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        0.8.dp,
                                        if (memo.isDone) colors.success.copy(alpha = 0.35f) else colors.memoBorder,
                                        metrics.smallCardShape
                                    )
                                    .clickable { onOpenMemo(memo) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "切换完成状态",
                                        tint = if (memo.isDone) colors.success else colors.memoAccent.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { onToggleMemoDone(memo) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = memo.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (memo.isDone) colors.success else colors.memoText,
                                            textDecoration = if (memo.isDone) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        if (memo.content.isNotBlank()) {
                                            Text(
                                                text = memo.content,
                                                fontSize = 11.5.sp,
                                                color = colors.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (memo.week == 0) "每周" else "第${memo.week}周",
                                        fontSize = 10.sp,
                                        color = colors.textTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.danger, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除", color = colors.danger)
                }
                Button(
                    onClick = onEdit,
                    shape = metrics.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("编辑")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = colors.textSecondary) }
        }
    )
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label：", fontSize = 13.sp, color = colors.textSecondary)
        Text(value, fontSize = 14.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CourseEditorDialog(
    initial: CourseEntity?,
    maxSection: Int,
    onDismiss: () -> Unit,
    onConfirm: (CourseEntity) -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var teacher by remember { mutableStateOf(initial?.teacher.orEmpty()) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    var dayOfWeek by remember { mutableIntStateOf(initial?.dayOfWeek ?: 1) }
    var startSection by remember { mutableIntStateOf(initial?.startSection ?: 1) }
    var endSection by remember { mutableIntStateOf(initial?.endSection ?: 2) }
    var startWeek by remember { mutableStateOf((initial?.weekList()?.firstOrNull() ?: 1).toString()) }
    var endWeek by remember { mutableStateOf((initial?.weekList()?.lastOrNull() ?: 16).toString()) }
    var colorIndex by remember { mutableIntStateOf(initial?.colorIndex ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = metrics.dialogShape,
        title = { Text(if (initial == null) "添加课程" else "编辑课程", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("教室（如 36栋-201）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("任课教师") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("星期", fontSize = 12.sp, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    for (d in 1..7) {
                        val selected = dayOfWeek == d
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(if (metrics.isMasses) 2.dp else 8.dp))
                                .background(if (selected) colors.primary else colors.background)
                                .clickable { dayOfWeek = d }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = DAY_LABELS[d - 1],
                                fontSize = 12.sp,
                                color = if (selected) Color.White else colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startSection.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull() ?: return@OutlinedTextField
                            startSection = v.coerceIn(1, maxSection)
                            if (endSection < startSection) endSection = startSection
                        },
                        label = { Text("开始节次") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endSection.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull() ?: return@OutlinedTextField
                            endSection = v.coerceIn(startSection, maxSection)
                        },
                        label = { Text("结束节次") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startWeek,
                        onValueChange = { startWeek = it.filter(Char::isDigit) },
                        label = { Text("起始周") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endWeek,
                        onValueChange = { endWeek = it.filter(Char::isDigit) },
                        label = { Text("结束周") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("卡片配色", fontSize = 12.sp, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.coursePalettes.forEachIndexed { idx, pal ->
                        val swatchShape = if (metrics.isMasses) RoundedCornerShape(2.dp) else CircleShape
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(swatchShape)
                                .background(pal.accent)
                                .border(
                                    width = if (colorIndex == idx) 2.5.dp else 0.dp,
                                    color = if (colorIndex == idx) colors.textPrimary else Color.Transparent,
                                    shape = swatchShape
                                )
                                .clickable { colorIndex = idx }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val sw = (startWeek.toIntOrNull() ?: 1).coerceIn(1, 24)
                    val ew = (endWeek.toIntOrNull() ?: 16).coerceIn(sw, 24)
                    onConfirm(
                        CourseEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            teacher = teacher.trim(),
                            location = location.trim(),
                            dayOfWeek = dayOfWeek,
                            startSection = startSection,
                            endSection = endSection,
                            weeksCsv = (sw..ew).joinToString(","),
                            colorIndex = colorIndex
                        )
                    )
                },
                shape = metrics.buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        }
    )
}
