package com.recoverycoach.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recoverycoach.app.data.ActivityLog
import com.recoverycoach.app.ui.components.OutlinedPillButton
import com.recoverycoach.app.ui.components.PrimaryButton
import com.recoverycoach.app.ui.theme.InstrumentSans
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryType

/**
 * Content of the "Log activity" modal bottom sheet.
 *
 * The fields scroll; the action bar does not. That split matters — the form is
 * taller than a phone screen, and as one plain Column the Save button sat below
 * the fold with no way to reach it. The sheet looked like it had no save button
 * at all, and dismissing it discarded everything the user had typed.
 */
@Composable
fun LogActivitySheetContent(
    current: ActivityLog,
    onCancel: () -> Unit,
    onSave: (ActivityLog) -> Unit,
    modifier: Modifier = Modifier,
) {
    var morningWalkKm by remember { mutableStateOf(current.morningWalkKm.toString()) }
    var morningWalkMin by remember { mutableStateOf(current.morningWalkMin.toString()) }
    var totalWalkKm by remember { mutableStateOf(current.totalWalkKm.toString()) }
    var steps by remember { mutableStateOf(current.steps.toString()) }
    var swimM by remember { mutableStateOf(current.swimM.toString()) }
    var swimMin by remember { mutableStateOf(current.swimMin.toString()) }
    var heartPoints by remember { mutableStateOf(current.heartPoints.toString()) }
    var strengthMin by remember { mutableStateOf(current.strengthMin.toString()) }

    Column(modifier = modifier.fillMaxWidth().imePadding()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text(
                    "Today's activity",
                    style = RecoveryType.screenTitle.copy(fontSize = 25.sp),
                    color = RecoveryColors.TextPrimary,
                )
                Text(
                    "Leave anything you did not do blank.",
                    style = RecoveryType.rowCaption,
                    color = RecoveryColors.TextSecondary,
                )
            }

            SectionLabel("Deliberate morning walk")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricField("Distance (km)", morningWalkKm, { morningWalkKm = it }, Modifier.weight(1f))
                MetricField("Duration (minutes)", morningWalkMin, { morningWalkMin = it }, Modifier.weight(1f))
            }

            SectionLabel("Total day")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricField("Walking distance (km)", totalWalkKm, { totalWalkKm = it }, Modifier.weight(1f))
                MetricField("Steps", steps, { steps = it }, Modifier.weight(1f), KeyboardType.Number)
            }

            SectionLabel("Swimming")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricField("Distance (metres)", swimM, { swimM = it }, Modifier.weight(1f), KeyboardType.Number)
                MetricField("Duration (minutes)", swimMin, { swimMin = it }, Modifier.weight(1f), KeyboardType.Number)
            }

            SectionLabel("Strength")
            MetricField("Duration (minutes)", strengthMin, { strengthMin = it }, keyboardType = KeyboardType.Number)
            Text(
                "Any resistance work counts as one session for the week, however long it ran.",
                style = RecoveryType.rowCaption,
                color = RecoveryColors.TextSecondary,
            )

            SectionLabel("Other metrics")
            MetricField("Heart Points (optional)", heartPoints, { heartPoints = it }, keyboardType = KeyboardType.Number)
            Text(
                "Heart Points are stored only as secondary information.",
                style = RecoveryType.rowCaption,
                color = RecoveryColors.TextSecondary,
            )
        }

        ActionBar(
            onCancel = onCancel,
            onSave = {
                onSave(
                    ActivityLog(
                        morningWalkKm = morningWalkKm.toDoubleOrNull() ?: current.morningWalkKm,
                        morningWalkMin = morningWalkMin.toIntOrNull() ?: current.morningWalkMin,
                        totalWalkKm = totalWalkKm.toDoubleOrNull() ?: current.totalWalkKm,
                        steps = steps.toIntOrNull() ?: current.steps,
                        swimM = swimM.toIntOrNull() ?: current.swimM,
                        swimMin = swimMin.toIntOrNull() ?: current.swimMin,
                        heartPoints = heartPoints.toIntOrNull() ?: current.heartPoints,
                        strengthMin = strengthMin.toIntOrNull() ?: current.strengthMin,
                    ),
                )
            },
        )
    }
}

/** Pinned to the bottom of the sheet, so Save is reachable at any content height. */
@Composable
private fun ActionBar(onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RecoveryColors.Surface)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedPillButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
        PrimaryButton(text = "Save", onClick = onSave, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = RecoveryType.eyebrow, color = RecoveryColors.Primary)
}

@Composable
private fun MetricField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    Column(
        modifier = modifier
            .border(1.dp, RecoveryColors.BorderStrong, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                fontFamily = InstrumentSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = RecoveryColors.TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}
