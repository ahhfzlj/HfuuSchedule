package com.hfuu.schedule.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = CardSurface,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlue,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = ChipBg,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    outline = DividerColor
)

private val MassesColorScheme = lightColorScheme(
    primary = MassesCobalt,
    onPrimary = MassesSurface,
    primaryContainer = MassesCobaltLight,
    onPrimaryContainer = MassesKlein,
    secondary = MassesInk,
    onSecondary = MassesSurface,
    tertiary = MassesCrimson,
    onTertiary = MassesSurface,
    background = MassesCanvas,
    onBackground = MassesInk,
    surface = MassesSurface,
    onSurface = MassesInk,
    surfaceVariant = MassesMist,
    onSurfaceVariant = MassesSlate,
    error = MassesCrimson,
    outline = MassesBorder
)

@Composable
fun HfuuScheduleTheme(
    uiStyle: AppUiStyle = AppUiStyle.DEFAULT,
    content: @Composable () -> Unit
) {
    val isMasses = uiStyle == AppUiStyle.MASSES_TRAVELS
    val colors = if (isMasses) MassesAppColors else DefaultAppColors
    val metrics = if (isMasses) MassesAppMetrics else DefaultAppMetrics
    val colorScheme = if (isMasses) MassesColorScheme else LightColorScheme
    val typography = if (isMasses) MassesTypography else AppTypography

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = if (isMasses) MassesSurface.toArgb() else AppBackground.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = colors.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    CompositionLocalProvider(
        LocalUiStyle provides uiStyle,
        LocalAppColors provides colors,
        LocalAppMetrics provides metrics
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
