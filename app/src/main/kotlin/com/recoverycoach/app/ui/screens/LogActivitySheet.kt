package com.recoverycoach.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recoverycoach.app.data.ActivityLog
import com.recoverycoach.app.ui.components.OutlinedPillButton
import com.recoverycoach.app.ui.components.PrimaryButton
import com.recoverycoach.app.ui.theme.InstrumentSans
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight

/**
 * Content of the "Log activity" modal bottom sheet (design option 1d). The
 * host screen is responsible for showing/hiding the sheet itself; this
 * composable only owns the form fields.
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("Today's activity", style = RecoveryType.screenTitle.copy(fontSize = 25.sp), color = RecoveryColors.TextPrimary)
            Text("Leave anything you did not do blank.", style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
        }

        SectionLabel("Deliberate morning walk")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricField("Distance (km)", morningWalkKm, { morningWalkKm = it }, Modifier.weight(1f))
            MetricField("Duration (minutes)", morningWalkMin, { morningWalkMin = it }, Modifier.weight(1f))
        }

        SectionLabel("Total day")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricField("Walking distance (km)", totalWalkKm, { totalWalkKm = it }, Modifier.weight(1f))
            MetricField("Steps", steps, { steps = it }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        }

        SectionLabel("Swimming")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricField("Distance (metres)", swimM, { swimM = it }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
            MetricField("Duration (minutes)", swimMin, { swimMin = it }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        }

        SectionLabel("Other metrics")
        MetricField("Heart Points (optional)", heartPoints, { heartPoints = it }, keyboardType = KeyboardType.Number)
        Text(
            "Heart Points are stored only as secondary information.",
            style = RecoveryType.rowCaption,
            color = RecoveryColors.TextSecondary,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedPillButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "Save",
                onClick = {
                    onSave(
                        ActivityLog(
                            morningWalkKm = morningWalkKm.toDoubleOrNull() ?: current.morningWalkKm,
                            morningWalkMin = morningWalkMin.toIntOrNull() ?: current.morningWalkMin,
                            totalWalkKm = totalWalkKm.toDoubleOrNull() ?: current.totalWalkKm,
                            steps = steps.toIntOrNull() ?: current.steps,
                            swimM = swimM.toIntOrNull() ?: current.swimM,
                            swimMin = swimMin.toIntOrNull() ?: current.swimMin,
                            heartPoints = heartPoints.toIntOrNull() ?: current.heartPoints,
                        )
                    )
                },
                modifier = Modifier.weight(1.4f),
            )
        }
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
