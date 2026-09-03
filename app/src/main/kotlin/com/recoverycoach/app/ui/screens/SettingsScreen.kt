package com.recoverycoach.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.ui.components.RecoveryCard
import com.recoverycoach.app.ui.components.SectionEyebrow
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryType

/**
 * Settings wasn't part of the Phase 1 design turn (only Today, Check-in,
 * Week and the Log Activity sheet were drawn) — this is a minimal
 * placeholder so the bottom nav has somewhere to go, styled to match the
 * rest of the app. Replace with the real screen once it's designed.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            SectionEyebrow("Settings")
            Text("Coming soon", style = RecoveryType.screenTitle, color = RecoveryColors.TextPrimary)
            Text(
                "This screen hasn't been designed yet — check back after Today, Check-in and Week.",
                style = RecoveryType.screenSubtitle,
                color = RecoveryColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        RecoveryCard {
            Text(
                "Settings will cover things like units, reminder times, and how much detail Health Connect fills in automatically.",
                style = RecoveryType.rowCaption,
                color = RecoveryColors.TextSecondary,
            )
        }
    }
}
