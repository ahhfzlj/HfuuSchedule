package com.hfuu.schedule.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.BuildConfig
import com.hfuu.schedule.data.model.HfuuDefaults
import com.hfuu.schedule.data.model.UserProfile
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.AppUiStyle
import com.hfuu.schedule.ui.theme.BrandBlue
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesCobaltLight
import com.hfuu.schedule.ui.theme.MassesCrimson
import com.hfuu.schedule.ui.theme.MassesInk
import com.hfuu.schedule.ui.theme.MassesPageHeader
import com.hfuu.schedule.ui.theme.ThemedCard
import com.hfuu.schedule.ui.theme.TopBarMascot
import com.hfuu.schedule.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onOpenAccountManage: () -> Unit,
    onOpenJwxtImport: () -> Unit,
    onOpenSemesterSettings: () -> Unit,
    onOpenTimeSettings: () -> Unit,
    onOpenUiStyle: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val semester by viewModel.semesterConfig.collectAsState()
    val accountState by viewModel.accountState.collectAsState()
    val timeSlots by viewModel.timeSlots.collectAsState()
    val hideWeekends by viewModel.hideWeekends.collectAsState()
    val memoEnabled by viewModel.memoEnabled.collectAsState()
    val uiStyle by viewModel.uiStyle.collectAsState()
    val startInAgenda by viewModel.startInAgenda.collectAsState()

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showUnbindConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val currentWeek = remember(semester) { semester.currentWeek() }
    val isDefaultSchedule = remember(timeSlots) { timeSlots == HfuuDefaults.defaultTimeSlots }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isMasses) {
                MassesPageHeader(
                    title = "我的",
                    watermark = "ARCHIVE",
                    codeLabel = "THE SHRINE // TERMINAL & PROFILE",
                    trailing = {
                        TopBarMascot(size = 38.dp, onQuote = { viewModel.postToast(it) })
                    }
                )
            } else {
                Text(
                    text = "我的",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            // 1. 账号管理卡片
            ThemedCard(
                accentColor = if (isMasses) MassesInk else null,
                indexTag = if (isMasses) "ACC // 01" else null,
                onClick = onOpenAccountManage
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "账号管理",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMasses) "LINKED ${accountState.connectedCount()}/3" else "已连 ${accountState.connectedCount()} / 3",
                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                            fontSize = if (isMasses) 12.sp else 14.sp,
                            color = if (isMasses) MassesCobalt else colors.textSecondary,
                            fontWeight = if (isMasses) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. 个人信息卡片
            ThemedCard(
                accentColor = if (isMasses) MassesCobalt else null,
                indexTag = if (isMasses) "DOSSIER ↵/" else null,
                onClick = { showEditProfileDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(if (isMasses) 2.dp else 12.dp))
                            .background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.avatarText(),
                            fontFamily = if (isMasses) FontFamily.Serif else FontFamily.Default,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isMasses) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .background(MassesCrimson)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        val profileSub = profile.subtitleText()
                        Text(
                            text = profileSub.ifBlank { "点击完善班级与专业" },
                            fontSize = 13.sp,
                            color = if (profileSub.isBlank()) colors.textTertiary else colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = profile.maskedStudentId(),
                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                        fontSize = if (isMasses) 11.5.sp else 13.sp,
                        color = colors.textTertiary
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. UI 切换卡片（拾光极简 / 明日方舟·众生行迹）
            ThemedCard(
                accentColor = if (isMasses) MassesCrimson else null,
                indexTag = if (isMasses) "THEME // UI" else null,
                onClick = onOpenUiStyle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(if (isMasses) 2.dp else 10.dp))
                                .background(if (isMasses) MassesCobaltLight else colors.primaryLight)
                                .then(
                                    if (isMasses) Modifier.border(1.dp, MassesCobalt.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "UI 切换",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (uiStyle == AppUiStyle.MASSES_TRAVELS) {
                                    "当前：众生行迹 · THE MASSES' TRAVELS"
                                } else {
                                    "当前：拾光极简 · 可切换众生行迹风格"
                                },
                                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                fontSize = 11.5.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 迷你风格色块徽章
                        val miniSwatches = if (uiStyle == AppUiStyle.MASSES_TRAVELS) {
                            listOf(MassesCobalt, MassesInk, MassesCrimson)
                        } else {
                            listOf(BrandBlue, Color(0xFFEA580C), Color(0xFF16A34A))
                        }
                        miniSwatches.forEach { dot ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(if (isMasses) RoundedCornerShape(1.dp) else CircleShape)
                                    .background(dot)
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 4. 教务同步 / 学期设置 / 上课时间
            ThemedCard(
                indexTag = if (isMasses) "SYS // 03" else null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        title = "同步教务处数据",
                        trailingText = formatLastSyncDays(accountState.jwxtLastSyncMillis),
                        showRefreshIcon = true,
                        onClick = onOpenJwxtImport
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = "学期设置",
                        trailingText = semester.summaryText(),
                        onClick = onOpenSemesterSettings
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = "上课时间",
                        trailingText = if (isDefaultSchedule) "合大默认作息，可修改" else "已自定义作息，可修改",
                        showChevron = true,
                        onClick = onOpenTimeSettings
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                    Text(
                        text = "按这个日期算，今天是第 $currentWeek 周（共 ${semester.totalWeeks} 周）。如果跟实际周次差一周，进「学期设置」把日期前后挪 7 天。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            // 5. 课表显示、备忘与启动页开关
            ThemedCard(
                indexTag = if (isMasses) "CFG // 04" else null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SwitchSettingsRow(
                        title = "启动时直接进入日程",
                        subtitle = "开启后打开 App 将默认展示「日程」页",
                        checked = startInAgenda,
                        onCheckedChange = { viewModel.setStartInAgenda(it) }
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingsRow(
                        title = "不显示周六周日",
                        subtitle = "仅显示周一至周五，课表视图更宽敞",
                        checked = hideWeekends,
                        onCheckedChange = { viewModel.setHideWeekends(it) }
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingsRow(
                        title = "课表备忘录",
                        subtitle = "开启后可点击课程或空白时段添加备忘事项",
                        checked = memoEnabled,
                        onCheckedChange = { viewModel.setMemoEnabled(it) }
                    )
                }
            }

            // 6. 解除绑定 / 清除本机缓存
            ThemedCard(
                accentColor = if (isMasses) MassesCrimson.copy(alpha = 0.7f) else null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "解除绑定",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.danger,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showUnbindConfirm = true }
                            .padding(vertical = 15.dp)
                    )
                    HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                    Text(
                        text = "清除本机缓存",
                        fontSize = 16.sp,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearCacheConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    )
                }
            }

            // 7. 检查更新 + 隐私声明
            ThemedCard(
                indexTag = if (isMasses) "VER // 05" else null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        title = "检查更新",
                        trailingText = "v${BuildConfig.VERSION_NAME}",
                        onClick = { viewModel.postToast("当前已是最新版本 v${BuildConfig.VERSION_NAME}") }
                    )
                    Text(
                        text = "数据来自合肥大学教务系统与超星学习通，只做读取，不会提交任何申请、选课或修改。账号密码只加密保存在这台手机里，不经过任何第三方服务器。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initial = profile,
            onDismiss = { showEditProfileDialog = false },
            onSave = {
                viewModel.saveUserProfile(it)
                showEditProfileDialog = false
            }
        )
    }

    if (showUnbindConfirm) {
        AlertDialog(
            onDismissRequest = { showUnbindConfirm = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("解除绑定", fontWeight = FontWeight.Bold) },
            text = { Text("确定要解除教务处和学习通账号绑定，并清除本机加密保存的登录凭证吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unbindAllAccounts()
                    showUnbindConfirm = false
                }) { Text("确认解绑", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showUnbindConfirm = false }) { Text("取消", color = colors.textSecondary) }
            }
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("清除本机缓存", fontWeight = FontWeight.Bold) },
            text = { Text("将清除本地课表、待办与备忘缓存并恢复初始状态，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearLocalCache()
                    showClearCacheConfirm = false
                }) { Text("确认清除", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("取消", color = colors.textSecondary) }
            }
        )
    }
}

@Composable
private fun SwitchSettingsRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.primary
            )
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    trailingText: String,
    showChevron: Boolean = true,
    showRefreshIcon: Boolean = false,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val isMasses = AppTheme.isMasses
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 16.sp, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = trailingText,
                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                fontSize = if (isMasses) 12.sp else 13.sp,
                color = colors.textSecondary
            )
            Spacer(Modifier.width(4.dp))
            if (showRefreshIcon) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            } else if (showChevron) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    initial: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    var name by remember { mutableStateOf(initial.name) }
    var studentId by remember { mutableStateOf(initial.studentId) }
    var className by remember { mutableStateOf(initial.className) }
    var major by remember { mutableStateOf(initial.major) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = metrics.dialogShape,
        title = { Text("编辑个人资料", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, singleLine = true)
                OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("学号") }, singleLine = true)
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("班级") },
                    placeholder = { Text("选填") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = major,
                    onValueChange = { major = it },
                    label = { Text("专业") },
                    placeholder = { Text("选填") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    initial.copy(
                        name = name.trim().ifBlank { initial.name },
                        studentId = studentId.trim().ifBlank { initial.studentId },
                        className = className.trim(),
                        major = major.trim()
                    )
                )
            }) { Text("保存", color = colors.primary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        }
    )
}

private fun formatLastSyncDays(lastSyncMillis: Long): String {
    if (lastSyncMillis <= 0L) return "未同步"
    val diffDays = (System.currentTimeMillis() - lastSyncMillis) / (24L * 60 * 60 * 1000)
    return when {
        diffDays <= 0L -> "今天已同步"
        else -> "${diffDays} 天前同步"
    }
}
