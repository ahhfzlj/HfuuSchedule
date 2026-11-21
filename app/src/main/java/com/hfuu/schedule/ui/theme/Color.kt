package com.hfuu.schedule.ui.theme

import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFFF5F6F8)
val CardSurface = Color(0xFFFFFFFF)
val BrandBlue = Color(0xFF007AFF)
val BrandBlueLight = Color(0xFFEAF3FF)
val DangerRed = Color(0xFFC93B3B)
val DangerRedBg = Color(0xFFFDECEC)
val SuccessGreen = Color(0xFF1F9D55)
val SuccessGreenBg = Color(0xFFE8F7EE)
val TextPrimary = Color(0xFF111418)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)
val DividerColor = Color(0xFFECEEF2)
val ChipBg = Color(0xFFE9ECF1)
val SegmentedBg = Color(0xFFE5E7EC)

data class CoursePalette(
    val container: Color,
    val accent: Color,
    val onContainer: Color
)

/** 参照拾光课程表的柔和低饱和度卡片色板 */
val CoursePalettes = listOf(
    CoursePalette(Color(0xFFE8F2FF), Color(0xFF2563EB), Color(0xFF1E3A8A)),
    CoursePalette(Color(0xFFFFF1E6), Color(0xFFEA580C), Color(0xFF7C2D12)),
    CoursePalette(Color(0xFFE8F8F0), Color(0xFF16A34A), Color(0xFF14532D)),
    CoursePalette(Color(0xFFF3ECFF), Color(0xFF7C3AED), Color(0xFF4C1D95)),
    CoursePalette(Color(0xFFFFEBF0), Color(0xFFE11D48), Color(0xFF881337)),
    CoursePalette(Color(0xFFE6F7FA), Color(0xFF0891B2), Color(0xFF164E63)),
    CoursePalette(Color(0xFFFEF7E0), Color(0xFFCA8A04), Color(0xFF713F12)),
    CoursePalette(Color(0xFFEEF2F6), Color(0xFF475569), Color(0xFF1E293B))
)

// ==================== 明日方舟《众生行迹》(The Masses' Travels) 色板 ====================
// 参照 Funkierry/the-masses-travels 平面图录视觉规范：温润纸面、钴蓝圆盘、炭墨印字与蕾缪安绯红点缀
val MassesCanvas = Color(0xFFEFF1EF)
val MassesSurface = Color(0xFFF8F9F7)
val MassesMist = Color(0xFFE6EAE8)
val MassesCobalt = Color(0xFF3A5BC2)
val MassesKlein = Color(0xFF2448D8)
val MassesAzure = Color(0xFF4C92D4)
val MassesCobaltLight = Color(0xFFE7EEFA)
val MassesInk = Color(0xFF1D2326)
val MassesCharcoal = Color(0xFF22282B)
val MassesCrimson = Color(0xFFC84363)
val MassesCrimsonBg = Color(0xFFFAECEF)
val MassesSlate = Color(0xFF5F6A6E)
val MassesMuted = Color(0xFF838E92)
val MassesDivider = Color(0xFFD2D9D9)
val MassesBorder = Color(0xFFBFC8C9)
val MassesGridBlue = Color(0xFF2448D8)

/** 众生行迹 · 圣所蓝图与高对比档案色板（钴蓝 / 炭墨 / 蕾缪安绯红 / 青灰） */
val MassesCoursePalettes = listOf(
    CoursePalette(Color(0xFFF0F4FC), Color(0xFF3A5BC2), Color(0xFF142866)),
    CoursePalette(Color(0xFFF2F4F3), Color(0xFF1D2326), Color(0xFF1D2326)),
    CoursePalette(Color(0xFFFCF1F4), Color(0xFFC84363), Color(0xFF7A1C33)),
    CoursePalette(Color(0xFFEEF6FA), Color(0xFF2D76A8), Color(0xFF113F60)),
    CoursePalette(Color(0xFFF2F1FA), Color(0xFF4A42B5), Color(0xFF25206E)),
    CoursePalette(Color(0xFFEDF2FC), Color(0xFF2448D8), Color(0xFF102478)),
    CoursePalette(Color(0xFFF3F5F4), Color(0xFF4F5D63), Color(0xFF22282B)),
    CoursePalette(Color(0xFFEEF7F4), Color(0xFF1E7868), Color(0xFF11473E))
)
