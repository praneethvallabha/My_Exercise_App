package com.recoverycoach.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The design only specifies a single light appearance (no dark-mode variant
 * was drawn), so this theme is light-only for now.
 */
private val RecoveryColorScheme = lightColorScheme(
    primary = RecoveryColors.Primary,
    onPrimary = RecoveryColors.Surface,
    primaryContainer = RecoveryColors.PrimarySoft,
    onPrimaryContainer = RecoveryColors.PrimaryPressed,
    background = RecoveryColors.Background,
    onBackground = RecoveryColors.TextPrimary,
    surface = RecoveryColors.Surface,
    onSurface = RecoveryColors.TextPrimary,
    surfaceVariant = RecoveryColors.BorderSubtle,
    onSurfaceVariant = RecoveryColors.TextSecondary,
    outline = RecoveryColors.BorderStrong,
    error = RecoveryColors.WarningText,
)

@Composable
fun RecoveryCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RecoveryColorScheme,
        typography = RecoveryTypography,
        content = content,
    )
}
