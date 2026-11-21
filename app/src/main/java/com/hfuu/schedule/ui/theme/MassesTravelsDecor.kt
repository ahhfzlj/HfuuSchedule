package com.hfuu.schedule.ui.theme

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfuu.schedule.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class MassesBackdropVariant {
    SCHEDULE,
    STUDY,
    AGENDA,
    PROFILE,
    SHOWCASE
}

private val MASCOT_QUOTES = listOf(
    "苹果派！今天也要按时上课哦 ✦",
    "能天使提醒：别忘了检查今天的日程与待办~",
    "拉特兰通行证已核验 · 祝你今日顺利 ↵/",
    "Apple Pie! 需要休息一下吃口苹果派吗？"
)

/**
 * 顶栏小苹果能天使角色挂件：
 * 占据独立非重叠布局槽位（绝不遮挡文字与按钮），带轻盈呼吸悬浮与点击弹跳彩蛋。
 */
@Composable
fun TopBarMascot(
    size: Dp = 34.dp,
    modifier: Modifier = Modifier,
    onQuote: ((String) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float")
    val bobDp by infiniteTransition.animateFloat(
        initialValue = -1.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_bob"
    )

    var popped by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = if (popped) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
        label = "mascot_scale"
    )

    LaunchedEffect(popped) {
        if (popped) {
            delay(180L)
            popped = false
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                popped = true
                val msg = MASCOT_QUOTES[tapCount % MASCOT_QUOTES.size]
                tapCount++
                onQuote?.invoke(msg)
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.mascot_apple_exusiai),
            contentDescription = "能天使小苹果",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = bobDp.dp)
                .scale(scale)
        )
    }
}

/**
 * 众生行迹（The Masses' Travels）竖屏专属海报蓝图背景板：
 * 针对手机竖屏（9:19.5 / 9:20）重新构图，彻底避免横版海报被左右裁切或拉伸变形：
 * 1. 底板铺设温润纸面（#EFF1EF）+ 竖向蓝图坐标系网格 + 上下留白排印（The Masses' Travels / LATERANO 1099）；
 * 2. 画面中段以 ContentScale.FillWidth 完整呈现 1:1 无裁切的圆形图版海报（钴蓝圆盘、光环、羽兽、花枝、拼贴界面框全部完整入画）；
 * 3. 四周施加柔和纸面羽化遮罩（.world-veil），让海报自然融于竖版纸面无硬边，同时保持课表与文字极高可读性；
 * 4. 叠绘 sketch.ts 手绘呼吸过冲圆环、套印错位双线、垂直刻度尺与克莱因蓝十字准星。
 */
@Composable
fun MassesBlueprintBackdrop(
    variant: MassesBackdropVariant,
    modifier: Modifier = Modifier
) {
    val posterRes = when (variant) {
        MassesBackdropVariant.SCHEDULE -> R.drawable.poster_mt_blueprint
        MassesBackdropVariant.AGENDA -> R.drawable.poster_mt_home
        MassesBackdropVariant.STUDY -> R.drawable.poster_mt_cobalt
        MassesBackdropVariant.PROFILE -> R.drawable.poster_mt_lemuen
        MassesBackdropVariant.SHOWCASE -> R.drawable.poster_mt_home
    }

    // 完整竖版无裁切海报的柔和透明度：课表偏轻盈，日程/学习/我的更具画报层次
    val posterAlpha = when (variant) {
        MassesBackdropVariant.SCHEDULE -> 0.28f
        MassesBackdropVariant.AGENDA -> 0.36f
        MassesBackdropVariant.STUDY -> 0.32f
        MassesBackdropVariant.PROFILE -> 0.32f
        MassesBackdropVariant.SHOWCASE -> 0.30f
    }

    val posterAlignment = when (variant) {
        MassesBackdropVariant.SCHEDULE -> Alignment.Center
        MassesBackdropVariant.AGENDA -> Alignment.Center
        MassesBackdropVariant.STUDY -> Alignment.BottomCenter
        MassesBackdropVariant.PROFILE -> Alignment.Center
        MassesBackdropVariant.SHOWCASE -> Alignment.Center
    }

    val verticalTag = when (variant) {
        MassesBackdropVariant.SCHEDULE -> "CHRONICLE // 01"
        MassesBackdropVariant.STUDY -> "COVENANT // 02"
        MassesBackdropVariant.AGENDA -> "THE MASSES' TRAVELS // 03"
        MassesBackdropVariant.PROFILE -> "LATERANO ARCHIVE // 04"
        MassesBackdropVariant.SHOWCASE -> "SIDESTORY 06 // ATLAS"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MassesCanvas)
    ) {
        // 第 1 层：竖屏上下延展的淡雅衬线体排印（填补竖屏上下空间，形成完整竖版海报节奏）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "THE MASSES'",
                    fontFamily = FontFamily.Serif,
                    fontSize = 34.sp,
                    letterSpacing = 3.sp,
                    color = MassesInk.copy(alpha = 0.045f)
                )
                Text(
                    text = "    TRAVELS",
                    fontFamily = FontFamily.Serif,
                    fontSize = 32.sp,
                    letterSpacing = 4.sp,
                    color = MassesCobalt.copy(alpha = 0.055f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "06",
                    fontFamily = FontFamily.Serif,
                    fontSize = 54.sp,
                    color = MassesCobalt.copy(alpha = 0.07f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = verticalTag,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        color = MassesSlate.copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "LATERANO · 1099 · MEMORY ARCHIVE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        letterSpacing = 1.6.sp,
                        color = MassesCobalt.copy(alpha = 0.30f)
                    )
                }
            }
        }

        // 第 2 层：按手机宽度完整适配（FillWidth，绝不左右切掉圆盘/羽兽/蓝手），上下自然羽化融入纸面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(posterAlignment)
                .padding(
                    top = if (variant == MassesBackdropVariant.STUDY) 80.dp else 0.dp,
                    bottom = if (variant == MassesBackdropVariant.STUDY) 36.dp else 0.dp
                )
        ) {
            Image(
                painter = painterResource(id = posterRes),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(posterAlpha)
            )
            // 上下边缘柔和羽化，消除横纵交界硬边
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to MassesCanvas,
                            0.14f to MassesCanvas.copy(alpha = 0.22f),
                            0.50f to Color.Transparent,
                            0.86f to MassesCanvas.copy(alpha = 0.22f),
                            1.0f to MassesCanvas
                        )
                    )
            )
            // 左右极细柔边羽化，让两侧图版自然融于纸面
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to MassesCanvas.copy(alpha = 0.85f),
                            0.09f to Color.Transparent,
                            0.91f to Color.Transparent,
                            1.0f to MassesCanvas.copy(alpha = 0.85f)
                        )
                    )
            )
        }

        // 第 3 层：sketch.ts 手绘蓝图几何、套印双线、过冲圆环、准星与垂直标尺刻度
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 32.dp.toPx()
            val gridColor = MassesGridBlue.copy(alpha = 0.042f)

            var x = 0f
            while (x <= size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.7f)
                x += gridStep
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.7f)
                y += gridStep
            }

            // 贯穿全屏的细蓝图基准纵线（强化竖版海报延展感）
            val axisX = size.width * 0.50f
            drawLine(
                color = MassesCobalt.copy(alpha = 0.08f),
                start = Offset(axisX, 0f),
                end = Offset(axisX, size.height),
                strokeWidth = 0.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f))
            )

            // 右侧/左侧蓝图垂直标尺刻度线（套印错位双描）
            val rulerX = if (variant == MassesBackdropVariant.STUDY) 10.dp.toPx() else size.width - 10.dp.toPx()
            val tickDir = if (variant == MassesBackdropVariant.STUDY) 1f else -1f
            val rulerTop = size.height * 0.12f
            val rulerBottom = size.height * 0.84f
            drawLine(MassesCobalt.copy(alpha = 0.24f), Offset(rulerX, rulerTop), Offset(rulerX, rulerBottom), 1.dp.toPx())
            drawLine(
                MassesCobalt.copy(alpha = 0.09f),
                Offset(rulerX + 1.5.dp.toPx(), rulerTop + 1.dp.toPx()),
                Offset(rulerX + 1.5.dp.toPx(), rulerBottom + 1.dp.toPx()),
                0.8.dp.toPx()
            )
            val tickCount = 14
            for (i in 0..tickCount) {
                val ty = rulerTop + (rulerBottom - rulerTop) * (i / tickCount.toFloat())
                val len = if (i % 4 == 0) 8.dp.toPx() else 4.dp.toPx()
                drawLine(
                    color = MassesCobalt.copy(alpha = if (i % 4 == 0) 0.36f else 0.18f),
                    start = Offset(rulerX, ty),
                    end = Offset(rulerX + tickDir * len, ty),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // sketch.ts 手绘呼吸过冲圆环（带 1.6dp 套印偏移重影，居于上部留白区与中部海报呼应）
            val haloCenter = when (variant) {
                MassesBackdropVariant.SCHEDULE -> Offset(size.width * 0.84f, size.height * 0.18f)
                MassesBackdropVariant.AGENDA -> Offset(size.width * 0.82f, size.height * 0.16f)
                MassesBackdropVariant.STUDY -> Offset(size.width * 0.84f, size.height * 0.15f)
                MassesBackdropVariant.PROFILE -> Offset(size.width * 0.84f, size.height * 0.14f)
                MassesBackdropVariant.SHOWCASE -> Offset(size.width * 0.82f, size.height * 0.13f)
            }
            drawBreathingOvershootRing(
                center = haloCenter,
                radius = 48.dp.toPx(),
                color = MassesCobalt.copy(alpha = 0.17f),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = MassesCobalt.copy(alpha = 0.10f),
                radius = 78.dp.toPx(),
                center = haloCenter,
                style = Stroke(
                    width = 0.9.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
                )
            )

            // 两个克莱因蓝十字准星 +（上下呼应）
            drawCrosshairMark(
                center = Offset(size.width * 0.50f, size.height * 0.15f),
                arm = 6.dp.toPx(),
                color = MassesKlein.copy(alpha = 0.28f)
            )
            drawCrosshairMark(
                center = Offset(size.width * 0.12f, size.height * 0.82f),
                arm = 6.dp.toPx(),
                color = MassesKlein.copy(alpha = 0.26f)
            )

            // 拉伸星芒裂隙线（上下对角呼应，契合竖屏比例）
            drawStretchedStarFilament(
                center = Offset(size.width * 0.82f, size.height * 0.10f),
                armLong = 78.dp.toPx(),
                armShort = 22.dp.toPx(),
                angleDeg = -22f,
                strokeColor = MassesInk.copy(alpha = 0.14f),
                fillColor = Color.White.copy(alpha = 0.65f)
            )
            drawStretchedStarFilament(
                center = Offset(size.width * 0.18f, size.height * 0.87f),
                armLong = 70.dp.toPx(),
                armShort = 20.dp.toPx(),
                angleDeg = 18f,
                strokeColor = MassesCobalt.copy(alpha = 0.18f),
                fillColor = Color.White.copy(alpha = 0.62f)
            )
        }
    }
}

/**
 * 致敬 Funkierry/the-masses-travels/src/sketch.ts 的 `ring` + `inked`：
 * 半径叠两层低频正弦起伏，收笔越过起点一小段，再偏移 1.5dp 描出半透明套印错位感。
 */
private fun DrawScope.drawBreathingOvershootRing(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    fun buildPath(dx: Float, dy: Float): Path {
        val steps = 64
        val startAngle = -0.6f
        val endAngle = startAngle + (2f * PI.toFloat()) + 0.24f
        val amp = 1.8.dp.toPx()
        return Path().apply {
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val a = startAngle + (endAngle - startAngle) * t
                val wobble = sin(a * 2f + 0.8f) * 0.65f + sin(a * 4f + 2.1f) * 0.35f
                val r = radius + wobble * amp
                val px = center.x + dx + cos(a) * r
                val py = center.y + dy + sin(a) * r
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
        }
    }
    drawPath(buildPath(0f, 0f), color = color, style = Stroke(width = strokeWidth))
    drawPath(
        buildPath(1.6.dp.toPx(), 1.1.dp.toPx()),
        color = color.copy(alpha = color.alpha * 0.32f),
        style = Stroke(width = strokeWidth * 0.85f)
    )
}

private fun DrawScope.drawCrosshairMark(center: Offset, arm: Float, color: Color) {
    val sw = 1.dp.toPx()
    drawLine(color, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), sw)
    drawLine(color, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), sw)
}

/**
 * 绘制《众生行迹》标志性的拉伸四角星芒 / 异化几何光丝
 */
fun DrawScope.drawStretchedStarFilament(
    center: Offset,
    armLong: Float,
    armShort: Float,
    angleDeg: Float,
    strokeColor: Color,
    fillColor: Color
) {
    val rad = angleDeg * PI.toFloat() / 180f
    val cosA = cos(rad)
    val sinA = sin(rad)
    fun pt(lx: Float, ly: Float): Offset = Offset(
        center.x + lx * cosA - ly * sinA,
        center.y + lx * sinA + ly * cosA
    )

    val waist = (armShort * 0.13f).coerceAtLeast(2.2f)
    val path = Path().apply {
        val pRight = pt(armLong, 0f)
        val pTop = pt(0f, -armShort)
        val pLeft = pt(-armLong * 0.75f, 0f)
        val pBottom = pt(0f, armShort * 0.85f)

        moveTo(pRight.x, pRight.y)
        quadraticTo(pt(waist, -waist).x, pt(waist, -waist).y, pTop.x, pTop.y)
        quadraticTo(pt(-waist, -waist).x, pt(-waist, -waist).y, pLeft.x, pLeft.y)
        quadraticTo(pt(-waist, waist).x, pt(-waist, waist).y, pBottom.x, pBottom.y)
        quadraticTo(pt(waist, waist).x, pt(waist, waist).y, pRight.x, pRight.y)
        close()
    }
    drawPath(path = path, color = fillColor)
    drawPath(path = path, color = strokeColor, style = Stroke(width = 1.dp.toPx()))
}

/**
 * 统一自适应卡片容器：
 * - DEFAULT 模式：保持原有 16.dp 柔和白底卡片
 * - MASSES_TRAVELS 模式：温润磨砂纸面（0.89 不透明度，既透出海报轮廓又确保文字锐利易读）+ 1.dp 细线 + 套印双线角标
 */
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    indexTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AppTheme.colors
    val metrics = AppTheme.metrics
    val isMasses = metrics.isMasses

    if (!isMasses) {
        Surface(
            shape = metrics.cardShape,
            color = colors.surface,
            modifier = modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            Box(content = content)
        }
    } else {
        val bracketColor = accentColor ?: MassesCobalt
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(metrics.cardShape)
                .background(colors.surface.copy(alpha = 0.89f))
                .border(1.dp, colors.border.copy(alpha = 0.85f), metrics.cardShape)
                .drawWithContent {
                    drawContent()
                    if (accentColor != null) {
                        drawRect(
                            color = accentColor,
                            topLeft = Offset.Zero,
                            size = Size(3.dp.toPx(), size.height)
                        )
                    }
                    // 左上 & 右下建筑定位角标（含微弱套印第二描）
                    val arm = 6.dp.toPx()
                    val stroke = 1.4.dp.toPx()
                    drawLine(bracketColor, Offset(0f, 0f), Offset(arm, 0f), stroke)
                    drawLine(bracketColor, Offset(0f, 0f), Offset(0f, arm), stroke)
                    val nudge = 1.5.dp.toPx()
                    drawLine(
                        bracketColor.copy(alpha = 0.25f),
                        Offset(nudge, nudge),
                        Offset(arm + nudge, nudge),
                        0.8.dp.toPx()
                    )
                    drawLine(
                        MassesInk.copy(alpha = 0.55f),
                        Offset(size.width, size.height),
                        Offset(size.width - arm, size.height),
                        stroke
                    )
                    drawLine(
                        MassesInk.copy(alpha = 0.55f),
                        Offset(size.width, size.height),
                        Offset(size.width, size.height - arm),
                        stroke
                    )
                }
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            content()
            if (indexTag != null) {
                Text(
                    text = indexTag,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.5.sp,
                    color = MassesCobalt.copy(alpha = 0.62f),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 6.dp)
                )
            }
        }
    }
}

/**
 * 《众生行迹》标志性 "> ∞ /" 符号徽章
 */
@Composable
fun MassesInfinitySlashBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
    ) {
        Text(
            text = ">",
            fontFamily = FontFamily.Monospace,
            fontSize = if (compact) 11.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            color = MassesCobalt
        )
        Text(
            text = "∞",
            fontFamily = FontFamily.Serif,
            fontSize = if (compact) 13.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = MassesInk
        )
        Canvas(
            modifier = Modifier
                .width(if (compact) 10.dp else 13.dp)
                .height(if (compact) 11.dp else 14.dp)
        ) {
            val slash = Path().apply {
                moveTo(size.width * 0.58f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width * 0.42f, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(slash, color = MassesInk)
        }
    }
}

/**
 * 《众生行迹》页面大标题横幅：
 * 支持左侧可选角色槽位（leading）与右侧操作槽位（trailing），绝不遮挡文字与信息。
 */
@Composable
fun MassesPageHeader(
    title: String,
    watermark: String,
    codeLabel: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 衬线体低透明大水印
            Text(
                text = watermark,
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 5.sp,
                color = MassesCobalt.copy(alpha = 0.085f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .background(MassesCobalt)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = title,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = MassesInk,
                                letterSpacing = 1.sp,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(6.dp))
                            MassesInfinitySlashBadge(compact = true)
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(MassesCrimson)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = codeLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                letterSpacing = 1.sp,
                                color = MassesSlate,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 水平基准轴线（钴蓝主线 + 炭墨/绯红微刻度）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.2.dp)
                    .background(MassesCobalt.copy(alpha = 0.65f))
            )
            Spacer(Modifier.width(4.dp))
            Box(modifier = Modifier.size(4.dp).background(MassesInk))
            Spacer(Modifier.width(2.dp))
            Box(modifier = Modifier.size(4.dp).background(MassesCobalt))
            Spacer(Modifier.width(2.dp))
            Box(modifier = Modifier.size(4.dp).background(MassesCrimson))
        }
    }
}

/**
 * 众生行迹 · 真海报预览画板（用于「UI 切换」页面直接展示《众生行记》主视觉海报）
 */
@Composable
fun MassesTravelsPreviewArtwork(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MassesCanvas)
            .border(1.dp, MassesCobalt.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.poster_mt_preview),
            contentDescription = "众生行迹主视觉海报预览",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // 右下角细微图录编号角标
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MassesCanvas.copy(alpha = 0.85f))
                .border(0.7.dp, MassesCobalt.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "SIDESTORY 06",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                color = MassesInk
            )
            MassesInfinitySlashBadge(compact = true)
        }
    }
}

/**
 * 拾光极简（默认风格）迷你预览画板（用于「UI 切换」页面对比展示）
 */
@Composable
fun DefaultMinimalPreviewArtwork(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppBackground)
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardSurface)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("第 8 周", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "本周",
                        fontSize = 9.sp,
                        color = BrandBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandBlueLight)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val samples = listOf(
                    Triple("数据结构", "@36-201", CoursePalettes[0]),
                    Triple("高等数学", "@38-104", CoursePalettes[1]),
                    Triple("大学英语", "@35-302", CoursePalettes[2])
                )
                samples.forEach { (name, loc, pal) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pal.container)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(78.dp)
                                .background(pal.accent)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 8.dp, top = 7.dp, end = 6.dp, bottom = 7.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pal.onContainer
                            )
                            Text(
                                text = loc,
                                fontSize = 9.sp,
                                color = pal.onContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
