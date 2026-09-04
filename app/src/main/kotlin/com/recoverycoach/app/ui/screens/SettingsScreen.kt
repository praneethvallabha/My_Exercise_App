package com.recoverycoach.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.BuildConfig
import com.recoverycoach.app.data.DebugSeed
import com.recoverycoach.app.data.RecoveryViewModel
import com.recoverycoach.app.ui.components.OutlinedPillButton
import com.recoverycoach.app.ui.components.RecoveryCard
import com.recoverycoach.app.ui.components.SectionEyebrow
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryType

@Composable
fun SettingsScreen(viewModel: RecoveryViewModel, modifier: Modifier = Modifier) {
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            SectionEyebrow("Settings")
            Text("Recovery Coach", style = RecoveryType.screenTitle, color = RecoveryColors.TextPrimary)
            Text(
                "A personal tool, not a diagnostic one. Data stays on this device.",
                style = RecoveryType.screenSubtitle,
                color = RecoveryColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        RecoveryCard {
            Text("About", style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.SemiBold), color = RecoveryColors.TextPrimary)
            Text(
                "Version 1.0 · Phase 1\nToday, Check-in, Week and Log Activity are built. Units, reminder times and Health Connect sync are planned for later.",
                style = RecoveryType.rowCaption,
                color = RecoveryColors.TextSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        RecoveryCard {
            Text(
                "About the guidance",
                style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.SemiBold),
                color = RecoveryColors.TextPrimary,
            )
            Text(
                "Tips are general educational guidance drawn from published sources — the American " +
                    "Diabetes Association Standards of Care (2026), WHO physical activity guidelines " +
                    "(2020), and peer-reviewed studies — and each tip names its source.\n\n" +
                    "This is not personal medical advice, a diagnosis, or a treatment plan, and it " +
                    "never covers medication. Activity intensity is not measured here, so weekly " +
                    "minutes are an estimate. Anything that concerns you is worth raising with your " +
                    "doctor.",
                style = RecoveryType.rowCaption,
                color = RecoveryColors.TextSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (BuildConfig.DEBUG) {
            Column {
                Text(
                    "Developer",
                    style = RecoveryType.sectionTitle,
                    color = RecoveryColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    "Fills the archive with ${DebugSeed.DAYS} generated days so the weekly rules " +
                        "and the 28-day baseline have something to work with. It stops at " +
                        "yesterday, so save one check-in to complete the week and see every tip. " +
                        "Fake data — debug builds only. \"Clear all data\" removes it.",
                    style = RecoveryType.rowCaption,
                    color = RecoveryColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedPillButton(
                    text = "Seed ${DebugSeed.DAYS} days of sample history",
                    onClick = { viewModel.seedSampleHistory() },
                )
            }
        }

        Column {
            Text("Data", style = RecoveryType.sectionTitle, color = RecoveryColors.TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedPillButton(
                text = "Clear all data",
                onClick = { showResetConfirm = true },
                borderColor = RecoveryColors.WarningBorder,
                textColor = RecoveryColors.WarningText,
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Clear all data?") },
            text = { Text("This resets today's plan, check-in answers and logged activity, and erases your recorded day history. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllData()
                    showResetConfirm = false
                }) {
                    Text("Clear", color = RecoveryColors.WarningText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
