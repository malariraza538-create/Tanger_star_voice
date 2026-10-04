package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TangerLightColorScheme = lightColorScheme(
    primary = TangerEmeraldLight,
    onPrimary = Color.White,
    primaryContainer = TangerMintSoft,
    onPrimaryContainer = TangerEmeraldDark,
    secondary = TangerTurquoise,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = TangerGold,
    onTertiary = Color.White,
    tertiaryContainer = TangerGoldLight,
    onTertiaryContainer = Color(0xFF78350F),
    background = SurfaceBackgroundMint,
    onBackground = TextDarkSlate,
    surface = SurfaceCardWhite,
    onSurface = TextDarkSlate,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextMutedSlate,
    error = TangerDangerRed,
    onError = Color.White
)

private val TangerDarkColorScheme = darkColorScheme(
    primary = TangerTurquoiseBright,
    onPrimary = TangerEmeraldDark,
    primaryContainer = TangerEmerald,
    onPrimaryContainer = TangerMintSoft,
    secondary = TangerMint,
    onSecondary = TangerEmeraldDark,
    tertiary = TangerGold,
    onTertiary = Color.Black,
    background = TangerEmeraldDark,
    onBackground = Color.White,
    surface = Color(0xFF0F3D3A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF134E4A),
    onSurfaceVariant = TangerMintSoft,
    error = TangerDangerRed,
    onError = Color.White
)

val TangerShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

val TangerPrimaryGradient = Brush.linearGradient(
    colors = listOf(
        TangerEmeraldDark,
        TangerEmerald,
        TangerTurquoise
    )
)

val TangerBrightGradient = Brush.horizontalGradient(
    colors = listOf(
        TangerEmeraldLight,
        TangerTurquoiseBright
    )
)

val TangerGoldGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFD97706),
        Color(0xFFFBBF24)
    )
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) TangerDarkColorScheme else TangerLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = TangerShapes,
        content = content
    )
}
