package com.hfuu.schedule.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.data.model.HfuuDefaults
import com.hfuu.schedule.data.model.TimeSlot
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.ThemedCard
import com.hfuu.schedule.ui.viewmodel.MainViewModel

/**
 * 上课时间调整页：
 * 支持逐节自定义开始/结束时间、一键批量调整单节时长、增删节次，以及一键恢复合肥大学默认作息。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTableSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val savedSlots by viewModel.timeSlots.collectAsState()
    val editableSlots = remember { mutableStateListOf<TimeSlot>() }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    LaunchedEffect(savedSlots) {
        editableSlots.clear()
        editableSlots.addAll(savedSlots)
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
                    title = { Text("上课时间设置", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary) },
                    actions = {
                        IconButton(
                            onClick = {
                                editableSlots.clear()
                                editableSlots.addAll(HfuuDefaults.defaultTimeSlots)
                                viewModel.resetDefaultTimeSlots()
                            }
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = "恢复合大默认作息", tint = colors.primary)
                        }
                    }
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
                MassesBlueprintBackdrop(variant = MassesBackdropVariant.SCHEDULE)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 顶部快捷操作栏
                Surface(
                    color = colors.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "点击任意节次可修改上下课时间。当前共 ${editableSlots.size} 节课，默认采用合肥大学常规作息。",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val nextSection = editableSlots.size + 1
                                    if (nextSection <= 14) {
                                        val prevEnd = editableSlots.lastOrNull()?.endTime ?: "20:10"
                                        val startMin = TimeSlot.parseMinutes(prevEnd) + 10
                                        val endMin = startMin + 45
                                        editableSlots.add(
                                            TimeSlot(
                                                section = nextSection,
                                                startTime = TimeSlot.formatMinutes(startMin),
                                                endTime = TimeSlot.formatMinutes(endMin)
                                            )
                                        )
                                    }
                                },
                                shape = metrics.buttonShape
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.height(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("增加一节", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    // 统一将每节课时长设为 45 分钟（保持开始时间不变）
                                    val updated = editableSlots.map { slot ->
                                        val startM = TimeSlot.parseMinutes(slot.startTime)
                                        slot.copy(endTime = TimeSlot.formatMinutes(startM + 45))
                                    }
                                    editableSlots.clear()
                                    editableSlots.addAll(updated)
                                },
                                shape = metrics.buttonShape
                            ) {
                                Text("统一45分钟/节", fontSize = 12.sp)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(editableSlots, key = { _, item -> item.section }) { index, slot ->
                        ThemedCard(
                            accentColor = if (isMasses) MassesCobalt else null,
                            onClick = { editingIndex = index }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(if (isMasses) 2.dp else 8.dp),
                                        color = colors.primaryLight
                                    ) {
                                        Text(
                                            text = if (isMasses) "SEC.${slot.section.toString().padStart(2, '0')}" else "第 ${slot.section} 节",
                                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "${slot.startTime}  —  ${slot.endTime}",
                                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${slot.durationMinutes()}分钟",
                                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                    if (editableSlots.size > 6 && index == editableSlots.lastIndex) {
                                        IconButton(onClick = { editableSlots.removeAt(index) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除", tint = colors.danger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(color = colors.surface, shadowElevation = if (isMasses) 0.dp else 6.dp, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        if (isMasses) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MassesCobalt.copy(alpha = 0.45f)))
                        }
                        Button(
                            onClick = {
                                viewModel.saveTimeSlots(editableSlots.toList())
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(48.dp),
                            shape = metrics.buttonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("保存上课时间", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    editingIndex?.let { idx ->
        val target = editableSlots.getOrNull(idx)
        if (target != null) {
            TimeSlotEditDialog(
                slot = target,
                onDismiss = { editingIndex = null },
                onConfirm = { newStart, newEnd ->
                    editableSlots[idx] = target.copy(startTime = newStart, endTime = newEnd)
                    editingIndex = null
                }
            )
        }
    }
}

@Composable
private fun TimeSlotEditDialog(
    slot: TimeSlot,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    var start by remember { mutableStateOf(slot.startTime) }
    var end by remember { mutableStateOf(slot.endTime) }
    val validRegex = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")
    val isValid = validRegex.matches(start.trim()) && validRegex.matches(end.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = metrics.dialogShape,
        title = { Text("修改第 ${slot.section} 节时间", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("请输入 24 小时制时间，例如 08:20", fontSize = 12.sp, color = colors.textSecondary)
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("上课时间 (HH:mm)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("下课时间 (HH:mm)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { onConfirm(start.trim(), end.trim()) }
            ) { Text("确定", color = colors.primary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        }
    )
}
