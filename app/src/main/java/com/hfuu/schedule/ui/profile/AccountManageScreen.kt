package com.hfuu.schedule.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesInk
import com.hfuu.schedule.ui.theme.ThemedCard
import com.hfuu.schedule.ui.viewmodel.MainViewModel

/**
 * 账号管理页：集中管理合肥大学教务账号、超星学习通账号与本地个人档案。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManageScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenJwxtImport: () -> Unit,
    onOpenChaoxingLogin: () -> Unit
) {
    val accountState by viewModel.accountState.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    var showJwxtCredDialog by remember { mutableStateOf(false) }

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

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
                    title = { Text("账号管理", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary) }
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
                Text(
                    text = "已连接 ${accountState.connectedCount()} / 3 个服务，所有密码与 Cookie 均通过 Android Keystore 加密保存于本机。",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )

                // 1. 合肥大学教务处账号
                AccountCard(
                    title = "合肥大学教务处",
                    subtitle = if (accountState.jwxtConnected) {
                        "已绑定学号：${accountState.jwxtUsername.ifBlank { profile.maskedStudentId() }}"
                    } else {
                        "导入课表、考试安排与成绩查询"
                    },
                    connected = accountState.jwxtConnected,
                    indexTag = "JWXT // 01",
                    primaryLabel = if (accountState.jwxtConnected) "重新导入课表" else "登录并导入",
                    onPrimaryClick = onOpenJwxtImport,
                    secondaryLabel = if (accountState.jwxtConnected) "解绑" else "保存学号密码",
                    onSecondaryClick = {
                        if (accountState.jwxtConnected) {
                            viewModel.disconnectJwxt()
                        } else {
                            showJwxtCredDialog = true
                        }
                    }
                )

                // 2. 超星学习通账号
                AccountCard(
                    title = "超星学习通",
                    subtitle = if (accountState.chaoxingConnected) {
                        "已接入 · ${accountState.chaoxingCourseCount} 门课程 · 支持作业/考试/签到查询"
                    } else {
                        "接入后自动汇总未交作业、考试与进行中的签到"
                    },
                    connected = accountState.chaoxingConnected,
                    indexTag = "MOOC // 02",
                    primaryLabel = if (accountState.chaoxingConnected) "立即同步" else "去登录接入",
                    onPrimaryClick = {
                        if (accountState.chaoxingConnected) {
                            viewModel.syncChaoxingTasks()
                        } else {
                            onOpenChaoxingLogin()
                        }
                    },
                    secondaryLabel = if (accountState.chaoxingConnected) "断开连接" else null,
                    onSecondaryClick = { viewModel.disconnectChaoxing() }
                )

                // 3. 本地校园身份档案
                val localSubtitle = listOf(profile.name, profile.className, profile.maskedStudentId())
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                AccountCard(
                    title = "校园身份档案（本机）",
                    subtitle = localSubtitle,
                    connected = accountState.localAccountSignedIn,
                    indexTag = "LOCAL // 03",
                    primaryLabel = "已启用本机存储",
                    onPrimaryClick = { viewModel.postToast("个人资料可在「我的」页面点击头像卡片修改") },
                    secondaryLabel = null,
                    onSecondaryClick = {}
                )
            }
        }
    }

    if (showJwxtCredDialog) {
        var username by remember { mutableStateOf(viewModel.getSavedJwxtUsername()) }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJwxtCredDialog = false },
            containerColor = colors.surface,
            shape = metrics.dialogShape,
            title = { Text("保存合肥大学教务账号", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("仅加密保存在本机，不会上传到任何第三方服务器。", fontSize = 12.sp, color = colors.textSecondary)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("学号 / 统一认证账号") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = username.isNotBlank(),
                    onClick = {
                        viewModel.saveJwxtAccount(username.trim(), password)
                        showJwxtCredDialog = false
                    }
                ) { Text("保存", color = colors.primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showJwxtCredDialog = false }) { Text("取消", color = colors.textSecondary) }
            }
        )
    }
}

@Composable
private fun AccountCard(
    title: String,
    subtitle: String,
    connected: Boolean,
    indexTag: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String?,
    onSecondaryClick: () -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    ThemedCard(
        accentColor = if (isMasses) (if (connected) MassesCobalt else MassesInk) else null,
        indexTag = if (isMasses) indexTag else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (isMasses) 18.dp else 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    text = if (isMasses) {
                        if (connected) "ONLINE" else "OFFLINE"
                    } else {
                        if (connected) "已连接" else "未连接"
                    },
                    fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (connected) colors.success else colors.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (isMasses) 2.dp else 8.dp))
                        .background(if (connected) colors.successBg else colors.chipBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(subtitle, fontSize = 13.sp, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPrimaryClick,
                    shape = metrics.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(primaryLabel, fontSize = 13.sp, color = Color.White)
                }
                if (secondaryLabel != null) {
                    OutlinedButton(
                        onClick = onSecondaryClick,
                        shape = metrics.buttonShape
                    ) {
                        Text(
                            text = secondaryLabel,
                            fontSize = 13.sp,
                            color = if (connected) colors.danger else colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}
