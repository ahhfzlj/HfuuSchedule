package com.hfuu.schedule.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 应用 UI 视觉风格定义：
 * - DEFAULT: 拾光极简（默认柔和圆角风格）
 * - MASSES_TRAVELS: 明日方舟《众生行迹》(The Masses' Travels) 圣所蓝图 / 克莱因蓝 / 锐利几何风格
 */
enum class AppUiStyle(
    val id: String,
    val displayName: String,
    val englishTitle: String,
    val subtitle: String,
    val description: String
) {
    DEFAULT(
        id = "default",
        displayName = "拾光极简",
        englishTitle = "SHIGUANG MINIMAL",
        subtitle = "默认风格 · 柔和圆角与低饱和色板",
        description = "简洁清爽的现代卡片排布，低饱和度柔和课表色块，日常阅读轻盈舒适。"
    ),
    MASSES_TRAVELS(
        id = "masses_travels",
        displayName = "众生行迹",
        englishTitle = "THE MASSES' TRAVELS",
        subtitle = "明日方舟风格 · 克莱因蓝与圣所蓝图",
        description = "灵感源自《众生行迹》视觉体系：高纯度克莱因蓝、极简白昼画布、锐利黑红碰撞、蓝图刻度网格、光环几何与衬线/等宽混排。"
    );

    companion object {
        fun fromId(id: String?): AppUiStyle {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}

@Immutable
data class AppThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val primary: Color,
    val primaryDeep: Color,
    val primaryLight: Color,
    val accentCrimson: Color,
    val accentCrimsonBg: Color,
    val ink: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val border: Color,
    val borderAccent: Color,
    val chipBg: Color,
    val segmentedBg: Color,
    val danger: Color,
    val dangerBg: Color,
    val success: Color,
    val successBg: Color,
    val memoBg: Color,
    val memoBorder: Color,
    val memoAccent: Color,
    val memoText: Color,
    val coursePalettes: List<CoursePalette>
)

@Immutable
data class AppThemeMetrics(
    val isMasses: Boolean,
    val cardRadius: Dp,
    val smallCardRadius: Dp,
    val buttonRadius: Dp,
    val pillRadius: Dp,
    val dialogRadius: Dp,
    val courseRadius: Dp,
    val cardBorderWidth: Dp
) {
    val cardShape: Shape get() = RoundedCornerShape(cardRadius)
    val smallCardShape: Shape get() = RoundedCornerShape(smallCardRadius)
    val buttonShape: Shape get() = RoundedCornerShape(buttonRadius)
    val pillShape: Shape get() = RoundedCornerShape(pillRadius)
    val dialogShape: Shape get() = RoundedCornerShape(dialogRadius)
    val courseShape: Shape get() = RoundedCornerShape(courseRadius)
}

val DefaultAppColors = AppThemeColors(
    background = AppBackground,
    surface = CardSurface,
    surfaceSubtle = Color(0xFFF0F2F5),
    primary = BrandBlue,
    primaryDeep = Color(0xFF005EC4),
    primaryLight = BrandBlueLight,
    accentCrimson = DangerRed,
    accentCrimsonBg = DangerRedBg,
    ink = TextPrimary,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    divider = DividerColor,
    border = DividerColor,
    borderAccent = BrandBlue.copy(alpha = 0.35f),
    chipBg = ChipBg,
    segmentedBg = SegmentedBg,
    danger = DangerRed,
    dangerBg = DangerRedBg,
    success = SuccessGreen,
    successBg = SuccessGreenBg,
    memoBg = Color(0xFFFFF8E1),
    memoBorder = Color(0xFFFCD34D),
    memoAccent = Color(0xFFD97706),
    memoText = Color(0xFF78350F),
    coursePalettes = CoursePalettes
)

val MassesAppColors = AppThemeColors(
    background = MassesCanvas,
    surface = MassesSurface,
    surfaceSubtle = MassesMist,
    primary = MassesCobalt,
    primaryDeep = MassesKlein,
    primaryLight = MassesCobaltLight,
    accentCrimson = MassesCrimson,
    accentCrimsonBg = MassesCrimsonBg,
    ink = MassesInk,
    textPrimary = MassesInk,
    textSecondary = MassesSlate,
    textTertiary = MassesMuted,
    divider = MassesDivider,
    border = MassesBorder,
    borderAccent = MassesCobalt.copy(alpha = 0.42f),
    chipBg = MassesMist,
    segmentedBg = MassesMist,
    danger = MassesCrimson,
    dangerBg = MassesCrimsonBg,
    success = Color(0xFF0E7A5F),
    successBg = Color(0xFFE7F6F1),
    memoBg = Color(0xFFFFF2F5),
    memoBorder = MassesCrimson.copy(alpha = 0.55f),
    memoAccent = MassesCrimson,
    memoText = Color(0xFF83102B),
    coursePalettes = MassesCoursePalettes
)

val DefaultAppMetrics = AppThemeMetrics(
    isMasses = false,
    cardRadius = 16.dp,
    smallCardRadius = 12.dp,
    buttonRadius = 12.dp,
    pillRadius = 20.dp,
    dialogRadius = 20.dp,
    courseRadius = 10.dp,
    cardBorderWidth = 0.dp
)

val MassesAppMetrics = AppThemeMetrics(
    isMasses = true,
    cardRadius = 2.dp,
    smallCardRadius = 2.dp,
    buttonRadius = 2.dp,
    pillRadius = 2.dp,
    dialogRadius = 3.dp,
    courseRadius = 2.dp,
    cardBorderWidth = 1.dp
)

val LocalUiStyle = staticCompositionLocalOf { AppUiStyle.DEFAULT }
val LocalAppColors = staticCompositionLocalOf { DefaultAppColors }
val LocalAppMetrics = staticCompositionLocalOf { DefaultAppMetrics }

object AppTheme {
    val style: AppUiStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalUiStyle.current

    val colors: AppThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val metrics: AppThemeMetrics
        @Composable
        @ReadOnlyComposable
        get() = LocalAppMetrics.current

    val isMasses: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalUiStyle.current == AppUiStyle.MASSES_TRAVELS
}
