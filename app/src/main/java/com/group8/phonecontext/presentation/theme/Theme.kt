package com.group8.phonecontext.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ContextSenseDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = CrispOffWhite,
    primaryContainer = DeepPurpleGlow,
    onPrimaryContainer = CrispOffWhite,
    secondary = SlateGraphite,
    onSecondary = CrispOffWhite,
    tertiary = EmeraldPulse,
    onTertiary = DeepObsidian,
    background = DeepObsidian,
    onBackground = CrispOffWhite,
    surface = MatteCharcoal,
    onSurface = CrispOffWhite,
    surfaceVariant = SlateGraphite,
    onSurfaceVariant = MutedAsh,
    outline = SubtleRimBorder,
    error = AmberAlert,
    onError = DeepObsidian
)

@Composable
fun ContextSenseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ContextSenseDarkColorScheme,
        typography = ContextSenseTypography,
        content = content
    )
}
