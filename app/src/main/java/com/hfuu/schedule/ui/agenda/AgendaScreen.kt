package com.hfuu.schedule.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.data.model.AgendaEventEntity
import com.hfuu.schedule.data.model.AgendaRepeatRule
import com.hfuu.schedule.data.model.AgendaTag
import com.hfuu.schedule.data.model.CourseEntity
import com.hfuu.schedule.data.model.TimeSlot
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesCobaltLight
import com.hfuu.schedule.ui.theme.MassesCrimson
import com.hfuu.schedule.ui.theme.MassesCrimsonBg
import com.hfuu.schedule.ui.theme.MassesInk
import com.hfuu.schedule.ui.theme.MassesPageHeader
import com.hfuu.schedule.ui.theme.ThemedCard
import com.hfuu.schedule.ui.theme.TopBarMascot
import com.hfuu.schedule.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val AgendaRose = Color(0xFFE13C5E)
private val DAY_SHORT_CN = listOf("一", "二", "三", "四", "五", "六", "日")
private val DAY_SHORT_EN = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

@Composable
fun AgendaScreen(
    viewModel: MainViewModel,
    onOpenAddAgenda: (presetTag: AgendaTag?, presetDateIso: String) -> Unit,
    onOpenEditAgenda: (eventId: Long) -> Unit
) {
    val allEvents by viewModel.agendaEvents.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val timeSlots by viewModel.timeSlots.collectAsState()
    val semester by viewModel.semesterConfig.collectAsState()

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    // 每 30 秒刷新一次实时倒计时显示
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val dayEvents = remember(allEvents, selectedDate) {
        allEvents.filter { it.occursOn(selectedDate) }
    }

    val dayCourses = remember(courses, semester, selectedDate) {
        val week = semester.weekForDate(selectedDate) ?: return@remember emptyList()
        val dow = selectedDate.dayOfWeek.value // 1=Mon .. 7=Sun
        courses
            .filter { it.dayOfWeek == dow && it.isInWeek(week) }
            .distinctBy { "${it.dayOfWeek}_${it.startSection}_${it.endSection}" }
            .sortedBy { it.startSection }
    }

    // 所有开启了倒计时且尚未完成的未来日程（方便跨天也能一眼看到重要倒计时）
    val upcomingCountdowns = remember(allEvents, selectedDate, nowMillis) {
        allEvents.filter {
            it.countdownEnabled && !it.isCompleted && !it.occursOn(selectedDate) &&
                it.nextOccurrenceStartMillis(nowMillis) > nowMillis
        }.sortedBy { it.nextOccurrenceStartMillis(nowMillis) }.take(3)
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd") }
    val weekdayCn = "周${DAY_SHORT_CN[selectedDate.dayOfWeek.value - 1]}"
    val weekdayEn = DAY_SHORT_EN[selectedDate.dayOfWeek.value - 1]
    val headerDateText = "${selectedDate.format(dateFormatter)} $weekdayCn"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (isMasses) {
            MassesBlueprintBackdrop(variant = MassesBackdropVariant.AGENDA)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            // 顶部日期栏 + 小苹果角色
            if (isMasses) {
                MassesPageHeader(
                    title = headerDateText,
                    watermark = "AGENDA",
                    codeLabel = "CHRONICLE // $weekdayEn · ${dayEvents.size} EVENTS · ${dayCourses.size} CLASSES",
                    leading = {
                        TopBarMascot(size = 34.dp, onQuote = { viewModel.postToast(it) })
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedDate != today) {
                                Text(
                                    text = "今日 ↵",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MassesCrimson,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MassesCrimsonBg)
                                        .border(0.8.dp, MassesCrimson.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                        .clickable { selectedDate = today }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            IconButton(
                                onClick = { selectedDate = selectedDate.minusDays(1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "前一天", tint = MassesInk)
                            }
                            IconButton(
                                onClick = { selectedDate = selectedDate.plusDays(1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "后一天", tint = MassesInk)
                            }
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = headerDateText,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedDate != today) {
                            Text(
                                text = "回今天",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = AgendaRose,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AgendaRose.copy(alpha = 0.1f))
                                    .clickable { selectedDate = today }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = { selectedDate = selectedDate.minusDays(1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "前一天", tint = colors.textSecondary)
                        }
                        IconButton(
                            onClick = { selectedDate = selectedDate.plusDays(1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "后一天", tint = colors.textSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 紧凑一周日期切换条（点击快速查看本周任意一天）
            WeekDateStrip(
                selectedDate = selectedDate,
                today = today,
                allEvents = allEvents,
                onSelectDate = { selectedDate = it }
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // 尚未创建任何日程时，完整呈现参照图1的「新增「日程」」引导卡片
                if (allEvents.isEmpty()) {
                    item(key = "intro_card") {
                        AgendaPromoCard(
                            onTagClick = { tag ->
                                onOpenAddAgenda(tag, selectedDate.toString())
                            },
                            onTryClick = {
                                onOpenAddAgenda(null, selectedDate.toString())
                            }
                        )
                    }
                }

                // 跨天重要倒计时提醒条（如有）
                if (upcomingCountdowns.isNotEmpty()) {
                    item(key = "countdown_header") {
                        AgendaSectionTitle(
                            title = "临近倒计时",
                            englishCode = "COUNTDOWN // ACTIVE"
                        )
                    }
                    items(upcomingCountdowns, key = { "cd_${it.id}" }) { event ->
                        AgendaEventCard(
                            event = event,
                            nowMillis = nowMillis,
                            showDatePrefix = true,
                            onToggleCompleted = { viewModel.toggleAgendaEventCompleted(event) },
                            onClick = { onOpenEditAgenda(event.id) }
                        )
                    }
                }

                // 当日自定义日程
                if (dayEvents.isNotEmpty()) {
                    item(key = "events_header") {
                        AgendaSectionTitle(
                            title = if (selectedDate == today) "今日日程 (${dayEvents.size})" else "当日日程 (${dayEvents.size})",
                            englishCode = "SCHEDULED // ${dayEvents.size.toString().padStart(2, '0')}"
                        )
                    }
                    items(dayEvents, key = { "ev_${it.id}" }) { event ->
                        AgendaEventCard(
                            event = event,
                            nowMillis = nowMillis,
                            showDatePrefix = false,
                            onToggleCompleted = { viewModel.toggleAgendaEventCompleted(event) },
                            onClick = { onOpenEditAgenda(event.id) }
                        )
                    }
                }

                // 当日课程联动展示
                if (dayCourses.isNotEmpty()) {
                    item(key = "courses_header") {
                        AgendaSectionTitle(
                            title = if (selectedDate == today) "今日课程 (${dayCourses.size})" else "当日课程 (${dayCourses.size})",
                            englishCode = "CLASSES // ${dayCourses.size.toString().padStart(2, '0')}"
                        )
                    }
                    items(dayCourses, key = { "course_${it.id}_${it.startSection}" }) { course ->
                        AgendaCourseCard(course = course, timeSlots = timeSlots)
                    }
                }

                // 当日无日程且无课程时的空状态（完全还原图1文案）
                if (dayEvents.isEmpty() && dayCourses.isEmpty()) {
                    item(key = "empty_day") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (allEvents.isEmpty()) 54.dp else 90.dp, bottom = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedDate == today) "今日暂无日程/课程" else "当日暂无日程/课程",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textTertiary
                                )
                                if (isMasses) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "NO ACTIVE SCHEDULE // CLICK + TO CREATE",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MassesCobalt.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 右下角悬浮 + 按钮（默认：纯白圆形 + 玫红加号还原图1；众生行迹：曜石黑锐利方块 + 克莱因蓝角标）
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .size(54.dp)
                .then(
                    if (isMasses) {
                        Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(MassesInk)
                            .border(1.dp, MassesCobalt.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                    } else {
                        Modifier
                            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.18f))
                            .clip(CircleShape)
                            .background(Color.White)
                    }
                )
                .clickable { onOpenAddAgenda(null, selectedDate.toString()) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "添加日程",
                tint = if (isMasses) Color.White else AgendaRose,
                modifier = Modifier.size(28.dp)
            )
            if (isMasses) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .background(MassesCrimson)
                )
            }
        }
    }
}

@Composable
private fun WeekDateStrip(
    selectedDate: LocalDate,
    today: LocalDate,
    allEvents: List<AgendaEventEntity>,
    onSelectDate: (LocalDate) -> Unit
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    val monday = remember(selectedDate) {
        selectedDate.with(DayOfWeek.MONDAY)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isMasses) 2.dp else 14.dp))
            .background(if (isMasses) colors.surface.copy(alpha = 0.94f) else colors.surface)
            .then(
                if (isMasses) Modifier.border(1.dp, colors.border, RoundedCornerShape(2.dp))
                else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0..6) {
            val date = monday.plusDays(i.toLong())
            val selected = date == selectedDate
            val isToday = date == today
            val hasEvent = allEvents.any { it.occursOn(date) }

            val bg = when {
                selected && isMasses -> MassesCobalt
                selected -> AgendaRose
                isToday && isMasses -> MassesCobaltLight
                isToday -> AgendaRose.copy(alpha = 0.1f)
                else -> Color.Transparent
            }
            val dayColor = when {
                selected -> Color.White.copy(alpha = 0.9f)
                else -> colors.textTertiary
            }
            val numColor = when {
                selected -> Color.White
                isToday && isMasses -> MassesCobalt
                isToday -> AgendaRose
                else -> colors.textPrimary
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                    .background(bg)
                    .clickable { onSelectDate(date) }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = DAY_SHORT_CN[i],
                    fontSize = 10.5.sp,
                    color = dayColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isMasses) date.dayOfMonth.toString().padStart(2, '0') else "${date.dayOfMonth}",
                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                    fontSize = 13.5.sp,
                    fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = numColor
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(if (isMasses) RoundedCornerShape(0.dp) else CircleShape)
                        .background(
                            when {
                                !hasEvent -> Color.Transparent
                                selected -> Color.White
                                isMasses -> MassesCrimson
                                else -> AgendaRose
                            }
                        )
                )
            }
        }
    }
}

/**
 * 参照图1的「新增「日程」」彩色标签云引导卡片（同时完美适配拾光极简与众生行迹）
 */
@Composable
private fun AgendaPromoCard(
    onTagClick: (AgendaTag) -> Unit,
    onTryClick: () -> Unit
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses

    val row1 = listOf(AgendaTag.CLASS_TIME, AgendaTag.EXAM_DATE)
    val row2 = listOf(AgendaTag.HOMEWORK, AgendaTag.STUDY_TASK, AgendaTag.CLASS_DINNER)
    val row3 = listOf(AgendaTag.MOVIE, AgendaTag.SHOW)

    ThemedCard(
        accentColor = if (isMasses) MassesCobalt else null,
        indexTag = if (isMasses) "NEW // AGENDA ↵/" else null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 上半区：彩色标签云
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (isMasses) {
                            Brush.linearGradient(
                                listOf(
                                    MassesCobaltLight.copy(alpha = 0.55f),
                                    Color.White,
                                    MassesCrimsonBg.copy(alpha = 0.35f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFFF1F4),
                                    Color(0xFFFFF9F2),
                                    Color.White
                                )
                            )
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(row1, row2, row3).forEach { tagRow ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tagRow.forEach { tag ->
                                val chipShape = RoundedCornerShape(if (isMasses) 2.dp else 10.dp)
                                Box(
                                    modifier = Modifier
                                        .clip(chipShape)
                                        .background(if (isMasses) Color.White else Color(tag.chipBgHex))
                                        .then(
                                            if (isMasses) {
                                                Modifier.border(
                                                    width = 0.9.dp,
                                                    color = Color(tag.dotColorHex).copy(alpha = 0.55f),
                                                    shape = chipShape
                                                )
                                            } else Modifier
                                        )
                                        .clickable { onTagClick(tag) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isMasses) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(Color(tag.dotColorHex))
                                            )
                                            Spacer(Modifier.width(5.dp))
                                        }
                                        Text(
                                            text = tag.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isMasses) MassesInk else Color(tag.chipTextHex)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 下半区：标题 + 副标题 + 去试试按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "新增「日程」",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "随时随地添加日程，设置提醒",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                        .background(if (isMasses) MassesCrimson else AgendaRose)
                        .clickable(onClick = onTryClick)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMasses) "去试试 ↵" else "去试试",
                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaSectionTitle(title: String, englishCode: String) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        if (isMasses) {
            Text(
                text = englishCode,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                color = MassesCobalt
            )
        }
    }
}

@Composable
private fun AgendaEventCard(
    event: AgendaEventEntity,
    nowMillis: Long,
    showDatePrefix: Boolean,
    onToggleCompleted: () -> Unit,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    val tag = event.tagEnum()
    val dotColor = Color(tag.dotColorHex)
    val repeat = event.repeatEnum()
    val dateFmt = remember { DateTimeFormatter.ofPattern("M月d日") }

    ThemedCard(
        accentColor = if (isMasses) (if (event.isCompleted) colors.success else dotColor) else null,
        indexTag = if (isMasses) "${tag.label} ↵/" else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isMasses) 14.dp else 14.dp,
                    end = 14.dp,
                    top = 13.dp,
                    bottom = 13.dp
                ),
            verticalAlignment = Alignment.Top
        ) {
            // 完成勾选按钮
            Icon(
                imageVector = if (event.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = "标记完成",
                tint = if (event.isCompleted) colors.success else dotColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
                    .clickable(onClick = onToggleCompleted)
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 标题 + 标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.title,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (event.isCompleted) colors.textTertiary else colors.textPrimary,
                        textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isMasses) {
                        Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(tag.chipBgHex))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tag.label,
                                fontSize = 11.sp,
                                color = Color(tag.chipTextHex),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 时间与地点
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val timeLabel = if (showDatePrefix) {
                        "${event.startDateTime().format(dateFmt)} ${event.formatTimeRange()}"
                    } else {
                        event.formatTimeRange()
                    }
                    Text(
                        text = timeLabel,
                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    if (event.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = event.location,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 倒计时 / 普通提醒 / 重复 徽标行
                if (event.countdownEnabled || event.reminderEnabled || repeat != AgendaRepeatRule.NEVER) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (event.countdownEnabled) {
                            val cdText = event.formatCountdown(nowMillis)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(if (isMasses) 2.dp else 8.dp))
                                    .background(if (isMasses) MassesCrimsonBg else AgendaRose.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HourglassBottom,
                                    contentDescription = null,
                                    tint = if (isMasses) MassesCrimson else AgendaRose,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = cdText,
                                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isMasses) MassesCrimson else AgendaRose
                                )
                            }
                        }
                        if (event.reminderEnabled) {
                            val remindLabel = when (event.reminderMinutesBefore) {
                                0 -> "准时提醒"
                                60 -> "提前1小时提醒"
                                else -> "提前${event.reminderMinutesBefore}分钟"
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(if (isMasses) 2.dp else 8.dp))
                                    .background(colors.primaryLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = remindLabel,
                                    fontSize = 11.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (repeat != AgendaRepeatRule.NEVER) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = repeat.label,
                                    fontSize = 11.sp,
                                    color = colors.textTertiary
                                )
                            }
                        }
                    }
                }

                if (event.notes.isNotBlank()) {
                    Text(
                        text = event.notes,
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaCourseCard(
    course: CourseEntity,
    timeSlots: List<TimeSlot>
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    val palette = colors.coursePalettes[course.colorIndex.mod(colors.coursePalettes.size)]
    val startTime = timeSlots.find { it.section == course.startSection }?.startTime ?: "第${course.startSection}节"
    val endTime = timeSlots.find { it.section == course.endSection }?.endTime ?: "第${course.endSection}节"

    ThemedCard(
        accentColor = if (isMasses) palette.accent else null,
        indexTag = if (isMasses) "SEC ${course.startSection}-${course.endSection}" else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isMasses) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.accent)
                )
                Spacer(Modifier.width(12.dp))
            } else {
                Spacer(Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val meta = buildList {
                    add("第${course.startSection}-${course.endSection}节 ($startTime - $endTime)")
                    if (course.location.isNotBlank()) add("@${course.location}")
                }.joinToString(" · ")
                Text(
                    text = meta,
                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
