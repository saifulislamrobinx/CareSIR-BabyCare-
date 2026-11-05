package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = PureWhite,
    primaryContainer = SageGreenContainer,
    onPrimaryContainer = DarkCharcoal,
    secondary = SoftPeachDark,
    onSecondary = PureWhite,
    secondaryContainer = SoftPeachLight,
    onSecondaryContainer = DarkCharcoal,
    tertiary = CategoryDiaperColor,
    onTertiary = PureWhite,
    background = WarmCream,
    onBackground = DarkCharcoal,
    surface = PureWhite,
    onSurface = DarkCharcoal,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = SoftGray,
    outline = OutlineWarm,
    error = ErrorMutedRed,
    onError = PureWhite,
    errorContainer = ErrorMutedRedBg,
    onErrorContainer = ErrorMutedRed
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreenBright,
    onPrimary = Color(0xFF102218),
    primaryContainer = Color(0xFF2B4435),
    onPrimaryContainer = Color(0xFFDCE8E0),
    secondary = SoftPeach,
    onSecondary = Color(0xFF3A2116),
    secondaryContainer = Color(0xFF4F3326),
    onSecondaryContainer = SoftPeachLight,
    tertiary = Color(0xFF8AB6DE),
    onTertiary = Color(0xFF10263B),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF3E4A43),
    error = Color(0xFFE58585),
    onError = Color(0xFF3B1212),
    errorContainer = Color(0xFF542020),
    onErrorContainer = Color(0xFFFAD2D2)
)

val CareSirShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CareSIRTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CareSirShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CareSIRTheme(
        themeMode = if (darkTheme) "DARK" else "LIGHT",
        content = content
    )
}
