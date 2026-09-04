package com.recoverycoach.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.data.LoadBar
import com.recoverycoach.app.data.RecoveryViewModel
import com.recoverycoach.app.data.WeekDayRecord
import com.recoverycoach.app.ui.components.RecoveryCard
import com.recoverycoach.app.ui.components.RowDivider
import com.recoverycoach.app.ui.components.SectionEyebrow
import com.recoverycoach.app.ui.components.StatRow
import com.recoverycoach.app.ui.effects.BlurIn
import com.recoverycoach.app.ui.effects.EmptyStatePulse
import com.recoverycoach.app.ui.effects.ShinyText
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryMotion
import com.recoverycoach.app.ui.theme.RecoveryType
import com.recoverycoach.app.ui.theme.recoveryTween

@Composable
fun WeekScreen(viewModel: RecoveryViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header()
        LoadChartCard(viewModel.loadBars)
        SummaryCard(viewModel)
        Text("Daily detail", style = RecoveryType.sectionTitle, color = RecoveryColors.TextPrimary, modifier = Modifier.padding(top = 2.dp))
        if (viewModel.weekDays.isEmpty()) {
            EmptyHistoryCard()
        } else {
            viewModel.weekDays.forEachIndexed { index, day ->
                BlurIn(delayMs = index * 55) { DailyDetailCard(day) }
            }
        }
    }
}

@Composable
private fun Header() {
    Column {
        SectionEyebrow("Trends")
        ShinyText(
            text = "Your last seven days",
            style = RecoveryType.screenTitle,
            baseColor = RecoveryColors.TextPrimary,
            highlightColor = RecoveryColors.Primary,
        )
        Text(
            "Compare against your own routine, not a generic leaderboard.",
            style = RecoveryType.screenSubtitle,
            color = RecoveryColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun LoadChartCard(bars: List<LoadBar>) {
    RecoveryCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Daily load estimate", style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.SemiBold), color = RecoveryColors.TextPrimary)
            Text("Walk and swim minutes per day", style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
        }
        Box(modifier = Modifier.fillMaxWidth().height(132.dp).padding(top = 14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomStart)
                    .padding(bottom = 62.dp)
                    .background(RecoveryColors.TextMuted),
            )
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    val height by animateDpAsState(
                        targetValue = bar.heightDp.dp,
                        animationSpec = recoveryTween(RecoveryMotion.EMPHASIZED_MS, RecoveryMotion.Decelerate),
                        label = "loadBar-${bar.day}",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(height)
                            .background(bar.color, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            bars.forEach { bar ->
                Text(bar.day, style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary, modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem("Normal", RecoveryColors.LoadNormal)
            LegendItem("Easy", RecoveryColors.LoadEasy)
            LegendItem("Recovery", RecoveryColors.LoadRecovery)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(9.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
    }
}

@Composable
private fun SummaryCard(viewModel: RecoveryViewModel) {
    RecoveryCard(padding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)) {
        StatRow("Walking", "${viewModel.weekWalkTotalKm} km")
        RowDivider()
        StatRow("Exercise minutes", "${viewModel.weekExerciseMinutes}")
        RowDivider()
        StatRow("Swimming", "${viewModel.weekSwimMinutes} min")
        RowDivider()
        StatRow(
            "Strength sessions",
            "${viewModel.weekStrengthSessions}",
            "Guidance is 2-3 a week on nonconsecutive days (ADA, 2026).",
        )
        RowDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("28-day walking reference", style = RecoveryType.rowLabel, color = RecoveryColors.TextPrimary)
                Text(
                    if (viewModel.baselineDaysLogged == 0) {
                        "No days logged yet."
                    } else {
                        "Based on ${viewModel.baselineDaysLogged} locally logged " +
                            "${if (viewModel.baselineDaysLogged == 1) "day" else "days"}."
                    },
                    style = RecoveryType.rowCaption,
                    color = RecoveryColors.TextSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${viewModel.baselineWalkKmPerWeek} km", style = RecoveryType.rowValue, color = RecoveryColors.TextPrimary)
                Text("per week", style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun DailyDetailCard(day: WeekDayRecord) {
    RecoveryCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(day.name, style = RecoveryType.rowValue, color = RecoveryColors.TextPrimary)
                Text(day.date, style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
            }
            Text(
                day.level.title.removeSuffix(" day"),
                style = RecoveryType.eyebrow,
                color = RecoveryColors.TextPrimary,
                modifier = Modifier
                    .background(day.level.cardBg, RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            DetailLine("Deliberate walk", day.walk)
            DetailLine("Total movement", day.total)
            DetailLine("Swimming", day.swim)
            DetailLine("Strength", day.strength)
            DetailLine("Recovery feedback", day.feedback)
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    RecoveryCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmptyStatePulse(size = 58.dp)
            Column {
                Text("Nothing logged yet", style = RecoveryType.rowValue, color = RecoveryColors.TextPrimary)
                Text(
                    "Days appear here once you save a check-in or log an activity. " +
                        "Trends are built only from days you actually recorded.",
                    style = RecoveryType.rowCaption,
                    color = RecoveryColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
        Text(value, style = RecoveryType.rowCaption.copy(fontWeight = FontWeight.Medium), color = RecoveryColors.TextPrimary)
    }
}
