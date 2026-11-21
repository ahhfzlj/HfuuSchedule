package com.hfuu.schedule.ui.study

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.data.model.HfuuDefaults
import com.hfuu.schedule.data.model.StudyTaskEntity
import com.hfuu.schedule.data.model.StudyTaskType
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesCobaltLight
import com.hfuu.schedule.ui.theme.MassesCrimson
import com.hfuu.schedule.ui.theme.MassesCrimsonBg
import com.hfuu.schedule.ui.theme.MassesInk
import com.hfuu.schedule.ui.theme.MassesMist
import com.hfuu.schedule.ui.theme.MassesPageHeader
import com.hfuu.schedule.ui.theme.ThemedCard
import com.hfuu.schedule.ui.theme.TopBarMascot
import com.hfuu.schedule.ui.viewmodel.MainViewModel

@Composable
fun StudyScreen(
    viewModel: MainViewModel,
    onOpenChaoxingLogin: () -> Unit
) {
    val context = LocalContext.current
    val tasks by viewModel.studyTasks.collectAsState()
    val accountState by viewModel.accountState.collectAsState()
    val isSyncing by viewModel.isSyncingChaoxing.collectAsState()

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    var topTabIndex by remember { mutableIntStateOf(0) }
    var filterType by remember { mutableStateOf<StudyTaskType?>(null) }
    var sortByCourse by remember { mutableStateOf(false) }
    var showAllTasks by remember { mutableStateOf(false) }

    val topTabs = listOf("学习通", "考试")
    val topTabCodes = listOf("CHAOXING", "EXAMS")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (isMasses) {
            MassesBlueprintBackdrop(variant = MassesBackdropVariant.STUDY)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            // 顶部大标题 + 小苹果角色 + 刷新
            if (isMasses) {
                MassesPageHeader(
                    title = "学习",
                    watermark = "COVENANT",
                    codeLabel = "THE SHRINE // CHAOXING & EXAM SYNC",
                    leading = {
                        TopBarMascot(size = 36.dp, onQuote = { viewModel.postToast(it) })
                    },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.surface)
                                .border(1.dp, MassesCobalt.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                                .clickable(enabled = !isSyncing) { viewModel.syncChaoxingTasks() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MassesCobalt
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "刷新",
                                    tint = MassesCobalt,
                                    modifier = Modifier.size(19.dp)
                                )
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
                        text = "学习",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    IconButton(onClick = { viewModel.syncChaoxingTasks() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "刷新",
                                tint = colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 顶部分段控制器：学习通 | 考试
            if (isMasses) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surface)
                        .border(1.dp, MassesInk.copy(alpha = 0.28f), RoundedCornerShape(2.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    topTabs.forEachIndexed { index, label ->
                        val selected = topTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (selected) MassesInk else Color.Transparent)
                                .clickable { topTabIndex = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(MassesCobalt)
                                    )
                                }
                                Text(
                                    text = label,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else colors.textSecondary
                                )
                                Text(
                                    text = topTabCodes[index],
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = if (selected) MassesCobaltLight else colors.textTertiary
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.segmentedBg)
                        .padding(3.dp)
                ) {
                    topTabs.forEachIndexed { index, label ->
                        val selected = topTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) colors.surface else Color.Transparent)
                                .clickable { topTabIndex = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) colors.textPrimary else colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (topTabIndex) {
                0 -> ChaoxingTabContent(
                    tasks = tasks,
                    courseCount = accountState.chaoxingCourseCount.takeIf { it > 0 } ?: 12,
                    lastSyncMillis = accountState.chaoxingLastSyncMillis,
                    isConnected = accountState.chaoxingConnected,
                    isSyncing = isSyncing,
                    filterType = filterType,
                    onFilterChange = { filterType = it },
                    sortByCourse = sortByCourse,
                    onSortChange = { sortByCourse = it },
                    showAllTasks = showAllTasks,
                    onToggleShowAll = { showAllTasks = !showAllTasks },
                    onTaskClick = { task -> viewModel.toggleTaskCompleted(task) },
                    onRecheck = { viewModel.syncChaoxingTasks() },
                    onOpenChaoxingApp = { openChaoxingOrLogin(context, onOpenChaoxingLogin) },
                    onDisconnectOrConnect = {
                        if (accountState.chaoxingConnected) {
                            viewModel.disconnectChaoxing()
                        } else {
                            onOpenChaoxingLogin()
                        }
                    }
                )
                1 -> ExamListContent(
                    chaoxingExams = tasks.filter { it.taskType() == StudyTaskType.EXAM },
                    isConnected = accountState.chaoxingConnected,
                    onExamClick = { viewModel.toggleTaskCompleted(it) }
                )
            }
        }
    }
}

@Composable
private fun ChaoxingTabContent(
    tasks: List<StudyTaskEntity>,
    courseCount: Int,
    lastSyncMillis: Long,
    isConnected: Boolean,
    isSyncing: Boolean,
    filterType: StudyTaskType?,
    onFilterChange: (StudyTaskType?) -> Unit,
    sortByCourse: Boolean,
    onSortChange: (Boolean) -> Unit,
    showAllTasks: Boolean,
    onToggleShowAll: () -> Unit,
    onTaskClick: (StudyTaskEntity) -> Unit,
    onRecheck: () -> Unit,
    onOpenChaoxingApp: () -> Unit,
    onDisconnectOrConnect: () -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    val activePendingCount = tasks.count { !it.isCompleted }
    val hwCount = tasks.count { it.taskType() == StudyTaskType.HOMEWORK }
    val examCount = tasks.count { it.taskType() == StudyTaskType.EXAM }
    val signCount = tasks.count { it.taskType() == StudyTaskType.SIGN_IN }

    val filtered = remember(tasks, filterType, sortByCourse) {
        val base = if (filterType == null) tasks else tasks.filter { it.taskType() == filterType }
        if (sortByCourse) {
            base.sortedWith(compareBy<StudyTaskEntity> { it.isCompleted }.thenBy { it.courseName }.thenBy { it.sortKeyMillis() })
        } else {
            base.sortedWith(compareBy<StudyTaskEntity> { it.isCompleted }.thenBy { it.sortKeyMillis() })
        }
    }

    val displayed = if (showAllTasks || filtered.size <= 4) filtered else filtered.take(4)
    val hiddenCount = (filtered.size - displayed.size).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$activePendingCount 条要交",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    if (isMasses) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "/ PENDING",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (activePendingCount > 0) MassesCrimson else MassesCobalt,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
                Text(
                    text = if (isMasses) {
                        "${formatRelativeMinutes(lastSyncMillis)} // ${courseCount}C"
                    } else {
                        "${formatRelativeMinutes(lastSyncMillis)} · 共 $courseCount 门课"
                    },
                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                    fontSize = if (isMasses) 11.sp else 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill("全部", tasks.size, filterType == null) { onFilterChange(null) }
                FilterPill("作业", hwCount, filterType == StudyTaskType.HOMEWORK) { onFilterChange(StudyTaskType.HOMEWORK) }
                FilterPill("考试", examCount, filterType == StudyTaskType.EXAM) { onFilterChange(StudyTaskType.EXAM) }
                FilterPill("签到", signCount, filterType == StudyTaskType.SIGN_IN) { onFilterChange(StudyTaskType.SIGN_IN) }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                    .background(if (isMasses) colors.surface else colors.segmentedBg)
                    .then(
                        if (isMasses) Modifier.border(1.dp, colors.border, RoundedCornerShape(2.dp))
                        else Modifier
                    )
                    .padding(3.dp)
            ) {
                SortSegment("按紧急度", !sortByCourse, Modifier.weight(1f)) { onSortChange(false) }
                SortSegment("按课程", sortByCourse, Modifier.weight(1f)) { onSortChange(true) }
            }
        }

        if (displayed.isEmpty()) {
            item {
                ThemedCard(indexTag = if (isMasses) "EMPTY ↵/" else null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isConnected) "当前没有待交作业、考试或进行中的签到" else "尚未连接学习通账号，点击右下角「连接」登录",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            items(displayed, key = { it.id }) { task ->
                StudyTaskCard(task = task, onClick = { onTaskClick(task) })
            }
        }

        if (hiddenCount > 0 || (showAllTasks && filtered.size > 4)) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hiddenCount > 0) {
                            if (isMasses) "+ 展开其余 $hiddenCount 条记录 ↵/" else "还有 $hiddenCount 条没显示"
                        } else {
                            if (isMasses) "收起列表 //" else "收起部分待办"
                        },
                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = if (isMasses) 12.sp else 13.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onToggleShowAll() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRecheck,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = metrics.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(
                        text = if (isSyncing) "检查中..." else "重新检查",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (isMasses) {
                    OutlinedButton(
                        onClick = onOpenChaoxingApp,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = metrics.buttonShape,
                        border = BorderStroke(1.dp, MassesInk.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface)
                    ) {
                        Text(
                            text = "打开学习通",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MassesInk
                        )
                    }
                } else {
                    Button(
                        onClick = onOpenChaoxingApp,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = metrics.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3E5EA))
                    ) {
                        Text(
                            text = "打开学习通",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }
                }

                TextButton(onClick = onDisconnectOrConnect) {
                    Text(
                        text = if (isConnected) "断开" else "连接",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnected) colors.danger else colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    val bg = when {
        selected && isMasses -> MassesCobalt
        selected -> Color(0xFFDDE0E6)
        else -> colors.surface
    }
    val borderColor = when {
        selected && isMasses -> MassesCobalt
        selected -> Color.Transparent
        isMasses -> colors.border
        else -> colors.divider
    }
    val labelColor = when {
        selected && isMasses -> Color.White
        selected -> colors.textPrimary
        else -> colors.textSecondary
    }
    val countColor = when {
        selected && isMasses -> Color.White.copy(alpha = 0.9f)
        selected -> colors.textPrimary
        else -> colors.textTertiary
    }

    Row(
        modifier = Modifier
            .clip(metrics.pillShape)
            .background(bg)
            .border(
                width = if (selected && !isMasses) 0.dp else 0.9.dp,
                color = borderColor,
                shape = metrics.pillShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$count",
            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
            fontSize = 11.5.sp,
            color = countColor
        )
    }
}

@Composable
private fun SortSegment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isMasses) 1.dp else 8.dp))
            .background(
                when {
                    selected && isMasses -> MassesCobaltLight
                    selected -> colors.surface
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                selected && isMasses -> MassesCobalt
                selected -> colors.textPrimary
                else -> colors.textSecondary
            }
        )
    }
}

@Composable
private fun StudyTaskCard(task: StudyTaskEntity, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    val taskType = task.taskType()
    val urgent = task.isUrgent()

    val badgeBg = if (isMasses) {
        when (taskType) {
            StudyTaskType.SIGN_IN -> MassesCobaltLight
            StudyTaskType.EXAM -> MassesCrimsonBg
            StudyTaskType.HOMEWORK -> MassesMist
        }
    } else {
        when (taskType) {
            StudyTaskType.SIGN_IN -> colors.primaryLight
            StudyTaskType.EXAM -> Color(0xFFFFF1E6)
            StudyTaskType.HOMEWORK -> colors.chipBg
        }
    }
    val badgeFg = if (isMasses) {
        when (taskType) {
            StudyTaskType.SIGN_IN -> MassesCobalt
            StudyTaskType.EXAM -> MassesCrimson
            StudyTaskType.HOMEWORK -> MassesInk
        }
    } else {
        when (taskType) {
            StudyTaskType.SIGN_IN -> colors.primary
            StudyTaskType.EXAM -> Color(0xFFD97706)
            StudyTaskType.HOMEWORK -> colors.textSecondary
        }
    }

    val leftAccent = when {
        !isMasses -> null
        task.isCompleted -> colors.success
        urgent -> MassesCrimson
        taskType == StudyTaskType.SIGN_IN -> MassesCobalt
        taskType == StudyTaskType.EXAM -> MassesCrimson
        else -> MassesInk
    }

    ThemedCard(
        accentColor = leftAccent,
        indexTag = if (isMasses) {
            when (taskType) {
                StudyTaskType.SIGN_IN -> "SIGN ↵/"
                StudyTaskType.EXAM -> "EXAM ↵/"
                StudyTaskType.HOMEWORK -> "TASK ↵/"
            }
        } else null,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isMasses) 16.dp else 14.dp,
                    end = 14.dp,
                    top = 13.dp,
                    bottom = 13.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMasses) "[${taskType.label}]" else taskType.label,
                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeFg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (isMasses) 2.dp else 6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) colors.textTertiary else colors.textPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = if (isMasses) 44.dp else 0.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.courseName,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    if (task.extraInfo.isNotBlank()) {
                        Text(
                            text = task.extraInfo,
                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 11.sp,
                            color = colors.primary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = task.formatRemaining(),
                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = if (isMasses) 12.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            task.isCompleted -> colors.success
                            urgent -> colors.danger
                            else -> colors.textSecondary
                        }
                    )
                    Text(
                        text = task.formatDeadline(),
                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 10.5.sp,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamListContent(
    chaoxingExams: List<StudyTaskEntity>,
    isConnected: Boolean,
    onExamClick: (StudyTaskEntity) -> Unit
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (isConnected) {
            if (chaoxingExams.isEmpty()) {
                item {
                    ThemedCard(indexTag = if (isMasses) "EXAM // 00" else null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "当前学习通暂无未开始或进行中的考试",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(chaoxingExams, key = { it.id }) { exam ->
                    StudyTaskCard(task = exam, onClick = { onExamClick(exam) })
                }
            }
        } else {
            items(HfuuDefaults.demoExams, key = { it.id }) { exam ->
                ThemedCard(
                    accentColor = if (isMasses) MassesCobalt else null,
                    indexTag = if (isMasses) "EXAM ↵/" else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isMasses) 16.dp else 14.dp,
                                end = 14.dp,
                                top = 14.dp,
                                bottom = 14.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exam.courseName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = exam.examType,
                                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                fontSize = 11.sp,
                                color = colors.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(if (isMasses) 2.dp else 6.dp))
                                    .background(colors.primaryLight)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "时间：${exam.examTime}",
                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "考场：${exam.location} · 座位号 ${exam.seatNumber}",
                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun formatRelativeMinutes(lastSyncMillis: Long): String {
    if (lastSyncMillis <= 0L) return "尚未同步"
    val diffMins = ((System.currentTimeMillis() - lastSyncMillis) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMins < 1 -> "刚刚查的"
        diffMins < 60 -> "$diffMins 分钟前查的"
        diffMins < 24 * 60 -> "${diffMins / 60} 小时前查的"
        else -> "${diffMins / (24 * 60)} 天前查的"
    }
}

private fun openChaoxingOrLogin(context: Context, fallbackLogin: () -> Unit) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.chaoxing.mobile")
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        runCatching {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(HfuuDefaults.CHAOXING_HOME_URL))
            context.startActivity(webIntent)
        }.onFailure {
            fallbackLogin()
        }
    }
}
