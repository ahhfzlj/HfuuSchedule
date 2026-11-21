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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.theme.AppUiStyle
import com.hfuu.schedule.ui.theme.BrandBlue
import com.hfuu.schedule.ui.theme.DefaultMinimalPreviewArtwork
import com.hfuu.schedule.ui.theme.MassesBackdropVariant
import com.hfuu.schedule.ui.theme.MassesBlueprintBackdrop
import com.hfuu.schedule.ui.theme.MassesCobalt
import com.hfuu.schedule.ui.theme.MassesCobaltLight
import com.hfuu.schedule.ui.theme.MassesCrimson
import com.hfuu.schedule.ui.theme.MassesInfinitySlashBadge
import com.hfuu.schedule.ui.theme.MassesInk
import com.hfuu.schedule.ui.theme.MassesSlate
import com.hfuu.schedule.ui.theme.MassesTravelsPreviewArtwork
import com.hfuu.schedule.ui.theme.ThemedCard
import com.hfuu.schedule.ui.viewmodel.MainViewModel

/**
 * UI 切换页面：
 * 支持在「拾光极简（默认）」与「众生行迹（明日方舟风格）」之间即时预览与一键切换。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiStyleScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val activeStyle by viewModel.uiStyle.collectAsState()
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "返回",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "UI 切换",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                letterSpacing = if (isMasses) 1.sp else 0.sp
                            )
                            if (isMasses) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "INTERFACE // STYLE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MassesCobalt
                                )
                            }
                        }
                    },
                    actions = {
                        if (isMasses) {
                            MassesInfinitySlashBadge(
                                compact = true,
                                modifier = Modifier.padding(end = 14.dp)
                            )
                        }
                    }
                )
                if (isMasses) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MassesCobalt.copy(alpha = 0.55f))
                    )
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
                MassesBlueprintBackdrop(variant = MassesBackdropVariant.SHOWCASE)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 顶部说明
                if (isMasses) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "THE MASSES' TRAVELS // VISUAL PROTOCOL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MassesCobalt,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "点击下方任意主题卡片即可实时切换全局界面风格并自动保存。",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "选择你喜欢的界面视觉风格，切换后会立即应用到课表、学习、我的等全部页面并自动记住。",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = colors.textSecondary
                    )
                }

                // 风格卡片 1：众生行迹（主推特色风格放在上方或与默认并列，清晰醒目）
                UiStyleOptionCard(
                    style = AppUiStyle.MASSES_TRAVELS,
                    selected = activeStyle == AppUiStyle.MASSES_TRAVELS,
                    swatchColors = listOf(MassesCobalt, MassesInk, MassesCrimson, Color(0xFFFFFFFF)),
                    indexTag = "STYLE.02 // SHRINE",
                    onSelect = { viewModel.setUiStyle(AppUiStyle.MASSES_TRAVELS) },
                    preview = { MassesTravelsPreviewArtwork() }
                )

                // 风格卡片 2：拾光极简（默认风格）
                UiStyleOptionCard(
                    style = AppUiStyle.DEFAULT,
                    selected = activeStyle == AppUiStyle.DEFAULT,
                    swatchColors = listOf(BrandBlue, Color(0xFFE8F2FF), Color(0xFFFFF1E6), Color(0xFFE8F8F0)),
                    indexTag = "STYLE.01 // MINIMAL",
                    onSelect = { viewModel.setUiStyle(AppUiStyle.DEFAULT) },
                    preview = { DefaultMinimalPreviewArtwork() }
                )

                // 当前风格实时组件预览条
                ThemedCard(
                    accentColor = if (isMasses) MassesCobalt else null,
                    indexTag = if (isMasses) "PREVIEW ↵/" else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isMasses) "实时组件映射" else "当前风格效果预览",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isMasses) "ACTIVE: ${activeStyle.englishTitle}" else activeStyle.displayName,
                                fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                fontSize = 11.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val pal0 = colors.coursePalettes[0]
                            val pal2 = colors.coursePalettes[2]
                            // 课程示例卡
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(metrics.courseShape)
                                    .background(pal0.container)
                                    .then(
                                        if (isMasses) Modifier.border(1.dp, pal0.accent.copy(alpha = 0.5f), metrics.courseShape)
                                        else Modifier
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(66.dp)
                                        .background(pal0.accent)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "计算机组成原理",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal0.onContainer
                                    )
                                    Text(
                                        text = "@36栋-201",
                                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                        fontSize = 10.sp,
                                        color = pal0.onContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // 紧急待办示例卡
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(metrics.courseShape)
                                    .background(pal2.container)
                                    .then(
                                        if (isMasses) Modifier.border(1.dp, pal2.accent.copy(alpha = 0.5f), metrics.courseShape)
                                        else Modifier
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(66.dp)
                                        .background(pal2.accent)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "操作系统实验报告",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pal2.onContainer
                                    )
                                    Text(
                                        text = if (isMasses) "DDL // 剩余 14h" else "剩余 14 小时",
                                        fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                                        fontSize = 10.sp,
                                        color = colors.danger
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun UiStyleOptionCard(
    style: AppUiStyle,
    selected: Boolean,
    swatchColors: List<Color>,
    indexTag: String,
    onSelect: () -> Unit,
    preview: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses
    val isMassesCard = style == AppUiStyle.MASSES_TRAVELS

    val borderColor = when {
        selected && isMasses -> MassesCobalt
        selected -> BrandBlue
        isMasses -> colors.border
        else -> colors.divider
    }
    val borderWidth = if (selected) 1.8.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(metrics.cardShape)
            .background(colors.surface.copy(alpha = if (isMasses) 0.96f else 1f))
            .border(borderWidth, borderColor, metrics.cardShape)
            .clickable(onClick = onSelect)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 预览画板
            preview()

            // 标题与选中状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = style.displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = if (isMassesCard) 0.8.sp else 0.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = style.englishTitle,
                            fontFamily = if (isMassesCard) FontFamily.Serif else FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.sp,
                            color = if (isMassesCard) MassesCobalt else colors.textTertiary
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = style.subtitle,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (selected) {
                    Row(
                        modifier = Modifier
                            .clip(if (isMasses) RoundedCornerShape(2.dp) else RoundedCornerShape(14.dp))
                            .background(if (isMasses) MassesCobalt else BrandBlue)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isMasses) "使用中 ↵/" else "当前使用",
                            fontFamily = if (isMasses) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(if (isMasses) RoundedCornerShape(2.dp) else RoundedCornerShape(14.dp))
                            .background(if (isMasses) MassesCobaltLight else colors.chipBg)
                            .border(
                                width = if (isMasses) 1.dp else 0.dp,
                                color = if (isMasses) MassesCobalt.copy(alpha = 0.45f) else Color.Transparent,
                                shape = if (isMasses) RoundedCornerShape(2.dp) else RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "切换",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMasses) MassesCobalt else colors.textPrimary
                        )
                    }
                }
            }

            // 描述文字
            Text(
                text = style.description,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = colors.textSecondary
            )

            // 色板点阵 + 编号
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    swatchColors.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(if (isMassesCard) RoundedCornerShape(1.dp) else CircleShape)
                                .background(swatch)
                                .border(
                                    0.7.dp,
                                    MassesInk.copy(alpha = 0.18f),
                                    if (isMassesCard) RoundedCornerShape(1.dp) else CircleShape
                                )
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isMassesCard) "克莱因蓝 · 曜石黑 · 绯红 · 纯白" else "天空蓝 · 柔和低饱和色系",
                        fontSize = 11.sp,
                        color = colors.textTertiary
                    )
                }

                Text(
                    text = indexTag,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    color = if (isMassesCard) MassesSlate else colors.textTertiary
                )
            }
        }
    }
}
