package com.hfuu.schedule.ui.agenda

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hfuu.schedule.data.model.AgendaEventEntity
import com.hfuu.schedule.data.model.AgendaRepeatRule
import com.hfuu.schedule.data.model.AgendaTag
import com.hfuu.schedule.reminder.AgendaReminderReceiver
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesCrimson
import com.hfuu.schedule.ui.theme.ThemedCard
import com.hfuu.schedule.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val EditorRose = Color(0xFFE13C5E)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgendaEditorScreen(
    viewModel: MainViewModel,
    eventId: Long?,
    presetTagName: String?,
    presetDateIso: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allEvents by viewModel.agendaEvents.collectAsState()
    val existing = remember(allEvents, eventId) {
        if (eventId != null && eventId > 0L) allEvents.firstOrNull { it.id == eventId } else null
    }

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses
    val accentRose = if (isMasses) MassesCrimson else EditorRose

    val zone = remember { ZoneId.systemDefault() }
    val initialDate = remember(existing, presetDateIso) {
        existing?.startDateTime(zone)?.toLocalDate()
            ?: presetDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()
    }
    val nowTime = remember { LocalTime.now().withSecond(0).withNano(0) }
    val initialStartTime = remember(existing) {
        existing?.startDateTime(zone)?.toLocalTime() ?: nowTime
    }
    val initialEndDate = remember(existing, initialDate) {
        existing?.endDateTime(zone)?.toLocalDate() ?: initialDate
    }
    val initialEndTime = remember(existing, initialStartTime) {
        existing?.endDateTime(zone)?.toLocalTime() ?: initialStartTime.plusHours(1)
    }

    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var location by remember(existing?.id) { mutableStateOf(existing?.location.orEmpty()) }
    var isAllDay by remember(existing?.id) { mutableStateOf(existing?.isAllDay ?: false) }
    var startDate by remember(existing?.id) { mutableStateOf(initialDate) }
    var startTime by remember(existing?.id) { mutableStateOf(initialStartTime) }
    var endDate by remember(existing?.id) { mutableStateOf(initialEndDate) }
    var endTime by remember(existing?.id) { mutableStateOf(initialEndTime) }
    var repeatRule by remember(existing?.id) {
        mutableStateOf(existing?.repeatEnum() ?: AgendaRepeatRule.NEVER)
    }
    var selectedTag by remember(existing?.id, presetTagName) {
        mutableStateOf(
            existing?.tagEnum()
                ?: presetTagName?.let { AgendaTag.fromName(it) }
                ?: AgendaTag.OTHER
        )
    }
    var countdownEnabled by remember(existing?.id) { mutableStateOf(existing?.countdownEnabled ?: false) }
    var reminderEnabled by remember(existing?.id) { mutableStateOf(existing?.reminderEnabled ?: false) }
    var reminderMinutesBefore by remember(existing?.id) { mutableIntStateOf(existing?.reminderMinutesBefore ?: 15) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }

    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }
    var pickingStartTime by remember { mutableStateOf(false) }
    var pickingEndTime by remember { mutableStateOf(false) }
    var showRepeatPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.postToast("未授予通知权限，可在系统设置中开启以接收日程提醒")
        }
    }

    fun requestNotificationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun handleSave() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            viewModel.postToast("请先输入日程内容")
            return
        }
        val effectiveStart = if (isAllDay) {
            LocalDateTime.of(startDate, LocalTime.of(9, 0))
        } else {
            LocalDateTime.of(startDate, startTime)
        }
        val rawEnd = if (isAllDay) {
            LocalDateTime.of(if (endDate.isBefore(startDate)) startDate else endDate, LocalTime.of(23, 59))
        } else {
            LocalDateTime.of(endDate, endTime)
        }
        val effectiveEnd = if (rawEnd.isBefore(effectiveStart)) effectiveStart.plusHours(1) else rawEnd

        val entity = AgendaEventEntity(
            id = existing?.id ?: 0L,
            title = trimmedTitle,
            location = location.trim(),
            isAllDay = isAllDay,
            startMillis = effectiveStart.atZone(zone).toInstant().toEpochMilli(),
            endMillis = effectiveEnd.atZone(zone).toInstant().toEpochMilli(),
            repeatRule = repeatRule.name,
            tag = selectedTag.name,
            countdownEnabled = countdownEnabled,
            reminderEnabled = reminderEnabled,
            reminderMinutesBefore = reminderMinutesBefore,
            notes = notes.trim(),
            isCompleted = existing?.isCompleted ?: false
        )
        viewModel.saveAgendaEvent(entity)
        onBack()
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isMasses) colors.surface else colors.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭",
                            tint = colors.textPrimary
                        )
                    }

                    Text(
                        text = if (existing == null) "添加日程" else "编辑日程",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(if (isMasses) 2.dp else 12.dp))
                            .background(if (isMasses) MassesCobalt else Color.White)
                            .then(
                                if (isMasses) Modifier else Modifier.border(0.6.dp, colors.divider, RoundedCornerShape(12.dp))
                            )
                            .clickable { handleSave() }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "保存",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMasses) Color.White else accentRose
                        )
                    }
                }
                if (isMasses) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MassesCobalt.copy(alpha = 0.55f)))
                }
            }
        }
    ) { padding ->
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
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 标题与位置卡片（还原图2顶部白卡）
                ThemedCard(indexTag = if (isMasses) "EVENT // 01" else null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            BasicTextField(
                                value = title,
                                onValueChange = { title = it },
                                textStyle = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                ),
                                cursorBrush = SolidColor(colors.primary),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (title.isEmpty()) {
                                        Text(
                                            text = "添加考试、活动会议、自习",
                                            fontSize = 18.sp,
                                            color = colors.textTertiary
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "位置",
                                fontSize = 15.5.sp,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = location,
                                onValueChange = { location = it },
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.End
                                ),
                                cursorBrush = SolidColor(colors.primary),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                        if (location.isEmpty()) {
                                            Text("选填", fontSize = 15.sp, color = colors.textTertiary)
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                // 2. 全天 + 开始/结束时间卡片
                ThemedCard(indexTag = if (isMasses) "TIME // 02" else null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAllDay = !isAllDay }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("全天", fontSize = 15.5.sp, color = colors.textPrimary)
                            }
                            Switch(
                                checked = isAllDay,
                                onCheckedChange = { isAllDay = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = if (isMasses) MassesCobalt else accentRose
                                )
                            )
                        }

                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))

                        // 开始
                        DateTimePickerRow(
                            label = "开始",
                            dateText = startDate.format(dateFormatter),
                            timeText = startTime.format(timeFormatter),
                            showTime = !isAllDay,
                            onDateClick = { pickingStartDate = true },
                            onTimeClick = { pickingStartTime = true }
                        )

                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))

                        // 结束
                        DateTimePickerRow(
                            label = "结束",
                            dateText = endDate.format(dateFormatter),
                            timeText = endTime.format(timeFormatter),
                            showTime = !isAllDay,
                            onDateClick = { pickingEndDate = true },
                            onTimeClick = { pickingEndTime = true }
                        )
                    }
                }

                // 3. 重复卡片
                ThemedCard(onClick = { showRepeatPicker = true }) {
                    EditorOptionRow(
                        icon = Icons.Rounded.Repeat,
                        label = "重复",
                        trailingContent = {
                            Text(repeatRule.label, fontSize = 15.sp, color = colors.textSecondary)
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Rounded.UnfoldMore,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                // 4. 标签卡片
                ThemedCard(onClick = { showTagPicker = true }) {
                    EditorOptionRow(
                        icon = Icons.AutoMirrored.Outlined.Label,
                        label = "标签",
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(if (isMasses) RoundedCornerShape(1.dp) else CircleShape)
                                    .background(Color(selectedTag.dotColorHex))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(selectedTag.label, fontSize = 15.sp, color = colors.textSecondary)
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Rounded.UnfoldMore,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                // 5. 倒计时提醒卡片
                ThemedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = !countdownEnabled
                                countdownEnabled = next
                                if (next) requestNotificationIfNeeded()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.HourglassBottom,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("倒计时提醒", fontSize = 15.5.sp, color = colors.textPrimary)
                                if (countdownEnabled) {
                                    Text(
                                        text = "将在日程页实时展示倒计时并到点提醒",
                                        fontSize = 11.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                        Switch(
                            checked = countdownEnabled,
                            onCheckedChange = { next ->
                                countdownEnabled = next
                                if (next) requestNotificationIfNeeded()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isMasses) MassesCrimson else accentRose
                            )
                        )
                    }
                }

                // 6. 普通提醒卡片
                ThemedCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = !reminderEnabled
                                    reminderEnabled = next
                                    if (next) requestNotificationIfNeeded()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("普通提醒", fontSize = 15.5.sp, color = colors.textPrimary)
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { next ->
                                    reminderEnabled = next
                                    if (next) requestNotificationIfNeeded()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = if (isMasses) MassesCobalt else accentRose
                                )
                            )
                        }

                        if (reminderEnabled) {
                            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                            val leadOptions = listOf(
                                0 to "准时",
                                5 to "提前5分",
                                15 to "提前15分",
                                30 to "提前30分",
                                60 to "提前1小时"
                            )
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                leadOptions.forEach { (mins, label) ->
                                    val active = reminderMinutesBefore == mins
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                                            .background(
                                                if (active) colors.primary else colors.chipBg
                                            )
                                            .clickable { reminderMinutesBefore = mins }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (active) Color.White else colors.textSecondary
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "测试提醒音",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.primary,
                                    modifier = Modifier
                                        .clickable {
                                            requestNotificationIfNeeded()
                                            AgendaReminderReceiver.sendTestNotification(context)
                                            viewModel.postToast("已发送测试提醒（请确认手机未开启勿扰/静音模式）")
                                        }
                                        .padding(vertical = 2.dp)
                                )
                                Text(
                                    text = "系统铃声与横幅设置 >",
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier
                                        .clickable { AgendaReminderReceiver.openChannelSettings(context) }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // 7. 备注卡片
                ThemedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("备注", fontSize = 15.5.sp, color = colors.textPrimary)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = colors.textPrimary,
                                textAlign = TextAlign.End
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            maxLines = 4,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    if (notes.isEmpty()) {
                                        Text("选填", fontSize = 15.sp, color = colors.textTertiary)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                // 编辑模式下的删除按钮
                if (existing != null) {
                    ThemedCard(
                        accentColor = if (isMasses) MassesCrimson else null,
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Text(
                            text = "删除日程",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.danger,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // 开始日期选择器
    if (pickingStartDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        startDate = picked
                        if (endDate.isBefore(picked)) endDate = picked
                    }
                    pickingStartDate = false
                }) { Text("确定", color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) { Text("取消", color = colors.textSecondary) }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // 结束日期选择器
    if (pickingEndDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        endDate = if (picked.isBefore(startDate)) startDate else picked
                    }
                    pickingEndDate = false
                }) { Text("确定", color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) { Text("取消", color = colors.textSecondary) }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // 开始时间选择器
    if (pickingStartTime) {
        val state = rememberTimePickerState(
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { pickingStartTime = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("选择开始时间", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val picked = LocalTime.of(state.hour, state.minute)
                    startTime = picked
                    if (endDate == startDate && !endTime.isAfter(picked)) {
                        endTime = picked.plusHours(1)
                    }
                    pickingStartTime = false
                }) { Text("确定", color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartTime = false }) { Text("取消", color = colors.textSecondary) }
            }
        )
    }

    // 结束时间选择器
    if (pickingEndTime) {
        val state = rememberTimePickerState(
            initialHour = endTime.hour,
            initialMinute = endTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { pickingEndTime = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("选择结束时间", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    endTime = LocalTime.of(state.hour, state.minute)
                    pickingEndTime = false
                }) { Text("确定", color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndTime = false }) { Text("取消", color = colors.textSecondary) }
            }
        )
    }

    // 重复规则选择弹窗
    if (showRepeatPicker) {
        AlertDialog(
            onDismissRequest = { showRepeatPicker = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("重复频率", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AgendaRepeatRule.entries.forEach { rule ->
                        val active = repeatRule == rule
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                                .background(if (active) colors.primaryLight else Color.Transparent)
                                .clickable {
                                    repeatRule = rule
                                    showRepeatPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rule.label,
                                fontSize = 15.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) colors.primary else colors.textPrimary
                            )
                            if (active) {
                                Text("✓", color = colors.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRepeatPicker = false }) { Text("取消", color = colors.textSecondary) }
            }
        )
    }

    // 标签选择弹窗
    if (showTagPicker) {
        AlertDialog(
            onDismissRequest = { showTagPicker = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("选择标签", fontWeight = FontWeight.Bold) },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AgendaTag.entries.forEach { tag ->
                        val active = selectedTag == tag
                        val shape = RoundedCornerShape(if (isMasses) 2.dp else 12.dp)
                        Row(
                            modifier = Modifier
                                .clip(shape)
                                .background(if (active) Color(tag.chipBgHex) else colors.background)
                                .border(
                                    width = if (active) 1.3.dp else 0.6.dp,
                                    color = if (active) Color(tag.dotColorHex) else colors.divider,
                                    shape = shape
                                )
                                .clickable {
                                    selectedTag = tag
                                    showTagPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(if (isMasses) RoundedCornerShape(1.dp) else CircleShape)
                                    .background(Color(tag.dotColorHex))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tag.label,
                                fontSize = 13.5.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) Color(tag.chipTextHex) else colors.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTagPicker = false }) { Text("关闭", color = colors.textSecondary) }
            }
        )
    }

    // 删除确认弹窗
    if (showDeleteConfirm && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("删除日程", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除「${existing.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAgendaEvent(existing)
                    showDeleteConfirm = false
                    onBack()
                }) { Text("删除", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = colors.textSecondary) }
            }
        )
    }
}

@Composable
private fun DateTimePickerRow(
    label: String,
    dateText: String,
    timeText: String,
    showTime: Boolean,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    val chipShape = RoundedCornerShape(if (isMasses) 2.dp else 10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 15.5.sp, color = colors.textPrimary)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(chipShape)
                    .background(colors.surfaceSubtle)
                    .then(
                        if (isMasses) Modifier.border(0.8.dp, colors.border, chipShape) else Modifier
                    )
                    .clickable(onClick = onDateClick)
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Text(
                    text = dateText,
                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                    fontSize = 13.5.sp,
                    color = colors.textPrimary
                )
            }
            if (showTime) {
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(colors.surfaceSubtle)
                        .then(
                            if (isMasses) Modifier.border(0.8.dp, colors.border, chipShape) else Modifier
                        )
                        .clickable(onClick = onTimeClick)
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = timeText,
                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 13.5.sp,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorOptionRow(
    icon: ImageVector,
    label: String,
    trailingContent: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 15.5.sp, color = colors.textPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailingContent()
        }
    }
}
