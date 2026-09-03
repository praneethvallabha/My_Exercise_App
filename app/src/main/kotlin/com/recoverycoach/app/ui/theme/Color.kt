package com.recoverycoach.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The full palette from the Phase 1 design. Every screen pulls its colors
 * from here instead of hard-coding hex values, so the app stays visually
 * consistent and easy to re-theme later.
 */
object RecoveryColors {
    // Core surfaces
    val Background = Color(0xFFF7F7F1)
    val Surface = Color(0xFFFFFEF8)
    val BorderSubtle = Color(0xFFE9ECE5)
    val BorderStrong = Color(0xFFC3CBBF)

    // Brand / primary
    val Primary = Color(0xFF315C49)
    val PrimaryPressed = Color(0xFF123426)
    val PrimarySoft = Color(0xFFD5EBDD)

    // Text
    val TextPrimary = Color(0xFF1C1F1B)
    val TextSecondary = Color(0xFF4A4F48)
    val TextMuted = Color(0xFF7C8479)

    // Recommendation level tones
    val EasyBg = Color(0xFFFFE6B5)
    val EasyText = Color(0xFF5C3A00)
    val NormalBg = Color(0xFFD8ECDD)
    val NormalText = Color(0xFF173E2B)
    val RecoveryBg = Color(0xFFFFDAD6)
    val RecoveryText = Color(0xFF6A1A18)

    // Week trend load-bar colors (mirrors the level tones)
    val LoadNormal = Color(0xFF315C49)
    val LoadEasy = Color(0xFFD9A741)
    val LoadRecovery = Color(0xFFC0685F)

    // Warning / destructive
    val WarningBorder = Color(0xFFE0BDBB)
    val WarningText = Color(0xFF9C2E2E)
}
