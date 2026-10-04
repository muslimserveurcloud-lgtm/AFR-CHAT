package com.afrchat.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = Color(0xFFE6E3FF),
    onPrimaryContainer = Color(0xFF1B1550),
    secondary = SecondaryLight,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = Color(0xFFF0EFFB),
    onSurfaceVariant = Color(0xFF5E5A73),
    outline = Color(0xFFB9B6CF),
    outlineVariant = Color(0xFFE6E4F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F8FD),
    surfaceContainer = Color(0xFFF4F3FC),
    surfaceContainerHigh = Color(0xFFEFEEF9),
    surfaceContainerHighest = Color(0xFFEAE9F5),
    error = ErrorColor,
    errorContainer = Color(0xFFFDE8EA),
    onErrorContainer = Color(0xFF7A1620)
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF3B3380),
    onPrimaryContainer = Color(0xFFE6E3FF),
    secondary = SecondaryDark,
    onSecondary = Color(0xFF2B1209),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = Color(0xFF2A2740),
    onSurfaceVariant = Color(0xFFB8B4D0),
    outline = Color(0xFF5A5578),
    outlineVariant = Color(0xFF2F2B45),
    surfaceContainerLowest = Color(0xFF0B0A12),
    surfaceContainerLow = Color(0xFF171522),
    surfaceContainer = Color(0xFF1D1A2C),
    surfaceContainerHigh = Color(0xFF232039),
    surfaceContainerHighest = Color(0xFF2B2842),
    error = Color(0xFFFF6B6B),
    errorContainer = Color(0xFF4A1D22),
    onErrorContainer = Color(0xFFFFD9DC)
)

private val AfrChatShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Couleurs qui n'existent pas dans le ColorScheme Material3 par défaut (bulles, statut en ligne). */
data class AfrChatExtraColors(
    val bubbleOut: Color,
    val bubbleIn: Color,
    val onlineDot: Color
)

val LocalAfrChatExtraColors = androidx.compose.runtime.staticCompositionLocalOf {
    AfrChatExtraColors(BubbleOutLight, BubbleInLight, OnlineDot)
}

@Composable
fun AfrChatTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val extra = if (darkTheme) {
        AfrChatExtraColors(BubbleOutDark, BubbleInDark, OnlineDot)
    } else {
        AfrChatExtraColors(BubbleOutLight, BubbleInLight, OnlineDot)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAfrChatExtraColors provides extra) {
        MaterialTheme(
            colorScheme = colors,
            typography = AfrChatTypography,
            shapes = AfrChatShapes,
            content = content
        )
    }
}
