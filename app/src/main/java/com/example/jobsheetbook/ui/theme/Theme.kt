package com.example.jobsheetbook.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SoftTeal,
    onPrimary = DeepNavy,
    primaryContainer = DeepTeal,
    onPrimaryContainer = Color(0xFFE0E7FF),

    secondary = SoftAmber,
    onSecondary = DeepNavy,
    secondaryContainer = WarmAmber,
    onSecondaryContainer = Color(0xFFFFFBEB),

    tertiary = StatusReceived,
    onTertiary = DeepNavy,

    background = DeepNavy,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,

    error = VibrantRed,
    onError = DeepNavy,
    errorContainer = ErrorRedContainer,
    onErrorContainer = Color(0xFFFECACA),

    outline = SlateBlue,
    outlineVariant = SurfaceCardElevated,

    inverseSurface = SurfaceLight,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = DeepTeal,

    surfaceTint = SoftTeal
)

private val LightColorScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),

    secondary = WarmAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),

    tertiary = StatusReceived,
    onTertiary = Color.White,

    background = SurfaceLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLightCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,

    error = VibrantRed,
    onError = Color.White,
    errorContainer = ErrorRedContainerLight,
    onErrorContainer = Color(0xFF991B1B),

    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),

    inverseSurface = DeepNavy,
    inverseOnSurface = TextPrimary,
    inversePrimary = SoftTeal,

    surfaceTint = DeepTeal
)

data class SemanticTypographyColors(
    val jobId: Color,
    val techMechId: Color,
    val customerName: Color,
    val brand: Color,
    val phone: Color
)

val LocalSemanticTypography = staticCompositionLocalOf {
    SemanticTypographyColors(
        jobId = Color.Unspecified,
        techMechId = Color.Unspecified,
        customerName = Color.Unspecified,
        brand = Color.Unspecified,
        phone = Color.Unspecified
    )
}

val LightSemanticTypography = SemanticTypographyColors(
    jobId = JobIdSemanticLight,
    techMechId = TechMechIdSemanticLight,
    customerName = CustomerNameSemanticLight,
    brand = BrandSemanticLight,
    phone = PhoneSemanticLight
)

val DarkSemanticTypography = SemanticTypographyColors(
    jobId = JobIdSemanticDark,
    techMechId = TechMechIdSemanticDark,
    customerName = CustomerNameSemanticDark,
    brand = BrandSemanticDark,
    phone = PhoneSemanticDark
)

@Composable
fun JobSheetBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticTypography else LightSemanticTypography

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSemanticTypography provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
