package com.hfuu.schedule.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.data.model.SemesterConfig
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesCobaltLight
import com.hfuu.schedule.ui.theme.ThemedCard
import com.hfuu.schedule.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

private val SHORT_LABEL_OPTIONS = listOf(
    "大一上", "大一下",
    "大二上", "大二下",
    "大三上", "大三下",
    "大四上", "大四下",
    "大五上", "大五下",
    "研一上", "研一下",
    "研二上", "研二下",
    "研三上", "研三下",
    "小学期"
)

private val ACADEMIC_YEAR_OPTIONS = listOf(
    "2022-2023", "2023-2024", "2024-2025", "2025-2026",
    "2026-2027", "2027-2028", "2028-2029", "2029-2030", "2030-2031"
)

private val TERM_PART_OPTIONS = listOf(
    "第一学期", "第二学期", "暑期小学期"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val current by viewModel.semesterConfig.collectAsState()
    var shortLabel by remember(current) { mutableStateOf(current.shortLabel) }
    var semesterName by remember(current) { mutableStateOf(current.semesterName) }
    var startDateIso by remember(current) { mutableStateOf(current.startDateIso) }
    var totalWeeksText by remember(current) { mutableStateOf(current.totalWeeks.toString()) }

    var showShortLabelPicker by remember { mutableStateOf(false) }
    var showFullSemesterPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    val previewConfig = remember(shortLabel, semesterName, startDateIso, totalWeeksText) {
        SemesterConfig(
            semesterName = semesterName.ifBlank { current.semesterName },
            shortLabel = shortLabel.ifBlank { current.shortLabel },
            startDateIso = startDateIso,
            totalWeeks = (totalWeeksText.toIntOrNull() ?: 18).coerceIn(1, 30)
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = colors.textPrimary)
                        }
                    },
                    title = { Text("学期设置", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary) }
                )
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
                MassesBlueprintBackdrop(variant = MassesBackdropVariant.PROFILE)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemedCard(indexTag = if (isMasses) "TERM // CFG" else null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. 学期简称（滑动选择，取代手动输入）
                        WheelSelectorField(
                            label = "学期简称",
                            value = shortLabel,
                            helper = "滑动选择",
                            onClick = { showShortLabelPicker = true }
                        )

                        // 2. 完整学期名（双列滑动选择，取代手动输入）
                        WheelSelectorField(
                            label = "完整学期名",
                            value = semesterName,
                            helper = "滑动选择",
                            onClick = { showFullSemesterPicker = true }
                        )

                        // 3. 第 1 周周一日期（支持手动编辑与日历选择 + ±7 天微调）
                        OutlinedTextField(
                            value = startDateIso,
                            onValueChange = { startDateIso = it },
                            label = { Text("第 1 周周一日期 (yyyy-MM-dd)") },
                            trailingIcon = {
                                IconButton(onClick = { showStartDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarToday,
                                        contentDescription = "选择日期",
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val d = runCatching { LocalDate.parse(startDateIso) }.getOrDefault(LocalDate.now())
                                    startDateIso = d.minusDays(7).toString()
                                },
                                modifier = Modifier.weight(1f),
                                shape = metrics.buttonShape
                            ) { Text("前挪 7 天 (周次+1)", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = {
                                    val d = runCatching { LocalDate.parse(startDateIso) }.getOrDefault(LocalDate.now())
                                    startDateIso = d.plusDays(7).toString()
                                },
                                modifier = Modifier.weight(1f),
                                shape = metrics.buttonShape
                            ) { Text("后挪 7 天 (周次-1)", fontSize = 12.sp) }
                        }

                        // 4. 本学期总周数（默认 18 周，含便捷加减）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = totalWeeksText,
                                onValueChange = { totalWeeksText = it.filter(Char::isDigit) },
                                label = { Text("本学期总周数（默认 18 周）") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            val curWeeks = (totalWeeksText.toIntOrNull() ?: 18).coerceIn(1, 30)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(metrics.buttonShape)
                                    .background(colors.surfaceSubtle)
                                    .border(0.8.dp, colors.border, metrics.buttonShape)
                                    .clickable {
                                        totalWeeksText = (curWeeks - 1).coerceAtLeast(1).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "减少一周", tint = colors.textPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(metrics.buttonShape)
                                    .background(colors.surfaceSubtle)
                                    .border(0.8.dp, colors.border, metrics.buttonShape)
                                    .clickable {
                                        totalWeeksText = (curWeeks + 1).coerceAtMost(30).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "增加一周", tint = colors.textPrimary)
                            }
                        }
                    }
                }

                ThemedCard(
                    accentColor = if (isMasses) MassesCobalt else null,
                    indexTag = if (isMasses) "PREVIEW ↵/" else null
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = "实时预览：今天是第 ${previewConfig.currentWeek()} 周（共 ${previewConfig.totalWeeks} 周）",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "摘要显示：${previewConfig.summaryText()}",
                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.saveSemesterConfig(previewConfig)
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = metrics.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("保存学期设置", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }

    // 学期简称 · 滚轮滑动选择弹窗
    if (showShortLabelPicker) {
        val options = remember(shortLabel) {
            if (shortLabel in SHORT_LABEL_OPTIONS) SHORT_LABEL_OPTIONS else listOf(shortLabel) + SHORT_LABEL_OPTIONS
        }
        var tempSelected by remember(showShortLabelPicker) {
            mutableStateOf(shortLabel.ifBlank { SHORT_LABEL_OPTIONS[2] })
        }

        AlertDialog(
            onDismissRequest = { showShortLabelPicker = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = {
                Column {
                    Text("选择学期简称", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("上下滑动滚轮选择", fontSize = 12.sp, color = colors.textSecondary)
                }
            },
            text = {
                WheelPicker(
                    items = options,
                    initialIndex = options.indexOf(tempSelected).coerceAtLeast(0),
                    onSelected = { tempSelected = it }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    shortLabel = tempSelected
                    showShortLabelPicker = false
                }) { Text("确定", color = colors.primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showShortLabelPicker = false }) {
                    Text("取消", color = colors.textSecondary)
                }
            }
        )
    }

    // 完整学期名 · 双列滚轮滑动选择弹窗（学年 + 学期）
    if (showFullSemesterPicker) {
        val parsedParts = remember(semesterName) { semesterName.trim().split(Regex("\\s+")) }
        val initYear = parsedParts.getOrNull(0)?.takeIf { it in ACADEMIC_YEAR_OPTIONS } ?: "2026-2027"
        val initTerm = parsedParts.getOrNull(1)?.takeIf { it in TERM_PART_OPTIONS } ?: "第一学期"

        var tempYear by remember(showFullSemesterPicker) { mutableStateOf(initYear) }
        var tempTerm by remember(showFullSemesterPicker) { mutableStateOf(initTerm) }

        AlertDialog(
            onDismissRequest = { showFullSemesterPicker = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = {
                Column {
                    Text("选择完整学期名", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$tempYear $tempTerm",
                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WheelPicker(
                        items = ACADEMIC_YEAR_OPTIONS,
                        initialIndex = ACADEMIC_YEAR_OPTIONS.indexOf(tempYear).coerceAtLeast(0),
                        onSelected = { tempYear = it },
                        useMonospace = true,
                        modifier = Modifier.weight(1.15f)
                    )
                    WheelPicker(
                        items = TERM_PART_OPTIONS,
                        initialIndex = TERM_PART_OPTIONS.indexOf(tempTerm).coerceAtLeast(0),
                        onSelected = { tempTerm = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    semesterName = "$tempYear $tempTerm"
                    showFullSemesterPicker = false
                }) { Text("确定", color = colors.primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showFullSemesterPicker = false }) {
                    Text("取消", color = colors.textSecondary)
                }
            }
        )
    }

    // 第 1 周周一日期选择器
    if (showStartDatePicker) {
        val currentStart = runCatching { LocalDate.parse(startDateIso) }.getOrDefault(LocalDate.of(2026, 9, 7))
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentStart.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        startDateIso = picked.toString()
                    }
                    showStartDatePicker = false
                }) { Text("确定", color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("取消", color = colors.textSecondary) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun WheelSelectorField(
    label: String,
    value: String,
    helper: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    val fieldShape = RoundedCornerShape(if (isMasses) 2.dp else 10.dp)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(fieldShape)
                .background(if (isMasses) colors.background else colors.surfaceSubtle.copy(alpha = 0.65f))
                .border(1.dp, colors.border, fieldShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = helper,
                    fontSize = 12.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.UnfoldMore,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 丝滑吸附滚轮滑动选择器（WheelPicker）：
 * 基于 LazyColumn + rememberSnapFlingBehavior，支持手指上下滑动惯性吸附与点击直选。
 */
@Composable
private fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 40.dp,
    useMonospace: Boolean = false
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    val halfVisible = visibleCount / 2
    val safeInitial = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitial)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    val centeredIndex by remember(items) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index
                ?.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                ?: safeInitial
        }
    }

    var lastReported by remember { mutableIntStateOf(-1) }
    LaunchedEffect(centeredIndex, items) {
        if (centeredIndex in items.indices && centeredIndex != lastReported) {
            lastReported = centeredIndex
            onSelected(items[centeredIndex])
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center
    ) {
        // 中央选中高亮框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                .background(if (isMasses) MassesCobaltLight.copy(alpha = 0.7f) else colors.primaryLight)
        ) {
            HorizontalDivider(
                color = colors.primary.copy(alpha = 0.35f),
                thickness = 1.dp,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            HorizontalDivider(
                color = colors.primary.copy(alpha = 0.35f),
                thickness = 1.dp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { index ->
                val dist = abs(index - centeredIndex)
                val isCenter = dist == 0
                val itemAlpha = when (dist) {
                    0 -> 1f
                    1 -> 0.55f
                    else -> 0.28f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontFamily = if (useMonospace || isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = if (isCenter) 16.sp else 14.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) colors.primary else colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(itemAlpha)
                    )
                }
            }
        }
    }
}
