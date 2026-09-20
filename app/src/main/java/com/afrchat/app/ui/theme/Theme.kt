package com.afrchat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryVariantLight,
    secondary = SecondaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    error = ErrorColor
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryVariantDark,
    secondary = SecondaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    error = ErrorColor
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
            content = content
        )
    }
}
