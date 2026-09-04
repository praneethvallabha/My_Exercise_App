package com.recoverycoach.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recoverycoach.app.data.GeneralFeeling
import com.recoverycoach.app.data.RecoveryViewModel
import com.recoverycoach.app.ui.components.OutlinedPillButton
import com.recoverycoach.app.ui.components.PrimaryButton
import com.recoverycoach.app.ui.components.RecoveryCard
import com.recoverycoach.app.ui.components.SectionEyebrow
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryType

private val energyWords = listOf("", "Very low", "Low", "Normal", "Good", "Excellent")

@Composable
fun CheckInScreen(
    viewModel: RecoveryViewModel,
    onSave: () -> Unit,
    onReportWarningSymptom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header()
        EnergyCard(viewModel)
        FatigueCard(viewModel)
        GeneralFeelingCard(viewModel)
        OutlinedPillButton(
            text = "Add pain / soreness details (optional)",
            onClick = { viewModel.toggleSoreDetails() },
        )
        if (viewModel.soreDetailsExpanded) {
            SorenessCard(viewModel)
        }
        OutlinedPillButton(
            text = "Report a warning symptom",
            onClick = onReportWarningSymptom,
            borderColor = RecoveryColors.WarningBorder,
            textColor = RecoveryColors.WarningText,
        )
        NotesField(viewModel)
        PrimaryButton(text = "Save and see tomorrow", onClick = {
            viewModel.saveCheckIn()
            onSave()
        })
    }
}

@Composable
private fun Header() {
    Column {
        SectionEyebrow("Evening")
        Text("20-second check-in", style = RecoveryType.screenTitle, color = RecoveryColors.TextPrimary)
        Text(
            "A few honest signals are more useful than a long questionnaire.",
            style = RecoveryType.screenSubtitle,
            color = RecoveryColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EnergyCard(viewModel: RecoveryViewModel) {
    RecoveryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("How was your energy today?", style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.SemiBold), color = RecoveryColors.TextPrimary)
            Text("${viewModel.energy} / 5", style = RecoveryType.rowValue, color = RecoveryColors.Primary)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in 1..5) {
                val selected = viewModel.energy == n
                Chip(
                    text = "$n",
                    selected = selected,
                    onClick = { viewModel.energy = n },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            energyWords.getOrElse(viewModel.energy) { "" },
            style = RecoveryType.rowCaption,
            color = RecoveryColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun FatigueCard(viewModel: RecoveryViewModel) {
    RecoveryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Leg / body fatigue", style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.SemiBold), color = RecoveryColors.TextPrimary)
            Text("${viewModel.fatigue} / 10", style = RecoveryType.rowValue, color = RecoveryColors.Primary)
        }
        Slider(
            value = viewModel.fatigue.toFloat(),
            onValueChange = { viewModel.fatigue = it.toInt() },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = RecoveryColors.Primary,
                activeTrackColor = RecoveryColors.Primary,
                inactiveTrackColor = RecoveryColors.BorderSubtle,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0 none", style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
            Text("10 severe", style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
        }
    }
}

@Composable
private fun SorenessCard(viewModel: RecoveryViewModel) {
    RecoveryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Highest soreness", style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.SemiBold), color = RecoveryColors.TextPrimary)
            Text("${viewModel.soreness} / 10", style = RecoveryType.rowValue, color = RecoveryColors.Primary)
        }
        Slider(
            value = viewModel.soreness.toFloat(),
            onValueChange = { viewModel.soreness = it.toInt() },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = RecoveryColors.Primary,
                activeTrackColor = RecoveryColors.Primary,
                inactiveTrackColor = RecoveryColors.BorderSubtle,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun GeneralFeelingCard(viewModel: RecoveryViewModel) {
    RecoveryCard {
        Text("General feeling", style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.SemiBold), color = RecoveryColors.TextPrimary)
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GeneralFeeling.entries.forEach { feeling ->
                val selected = viewModel.generalFeeling == feeling
                SelectableRow(
                    text = feeling.label,
                    selected = selected,
                    onClick = { viewModel.generalFeeling = feeling },
                )
            }
        }
    }
}

@Composable
private fun NotesField(viewModel: RecoveryViewModel) {
    OutlinedTextField(
        value = viewModel.notes,
        onValueChange = { viewModel.notes = it },
        placeholder = { Text("Notes (optional)", style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary) },
        minLines = 3,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = RecoveryColors.Surface,
            unfocusedContainerColor = RecoveryColors.Surface,
            focusedIndicatorColor = RecoveryColors.Primary,
            unfocusedIndicatorColor = RecoveryColors.BorderStrong,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) RecoveryColors.PrimarySoft else RecoveryColors.Surface,
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (selected) RecoveryColors.Primary else RecoveryColors.BorderStrong,
                RoundedCornerShape(10.dp),
            )
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = if (selected) RecoveryType.rowLabel.copy(fontWeight = FontWeight.Bold) else RecoveryType.rowLabel.copy(fontWeight = FontWeight.Medium),
            color = if (selected) RecoveryColors.PrimaryPressed else RecoveryColors.TextSecondary,
        )
    }
}

@Composable
private fun SelectableRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) RecoveryColors.PrimarySoft else RecoveryColors.Surface,
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (selected) RecoveryColors.Primary else RecoveryColors.BorderStrong,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = RecoveryType.rowLabel.copy(fontSize = 14.5.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) RecoveryColors.PrimaryPressed else RecoveryColors.TextSecondary,
        )
    }
}
