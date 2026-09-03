package com.recoverycoach.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recoverycoach.app.data.PlanItem
import com.recoverycoach.app.data.RecoveryLevel
import com.recoverycoach.app.data.RecoveryViewModel
import com.recoverycoach.app.ui.components.OutlinedPillButton
import com.recoverycoach.app.ui.components.RecoveryCard
import com.recoverycoach.app.ui.components.RowDivider
import com.recoverycoach.app.ui.components.SectionEyebrow
import com.recoverycoach.app.ui.components.SectionTitle
import com.recoverycoach.app.ui.components.StatRow
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    viewModel: RecoveryViewModel,
    onEditActivity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header()
        RecommendationCard(viewModel)
        TodaysPlanSection(viewModel)
        ActualActivitySection(viewModel, onEditActivity)
        RecoverySection(viewModel)
        SevenDayTrendSection(viewModel)
    }
}

@Composable
private fun Header() {
    val today = remember { LocalDate.now() }
    val formatted = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
    }
    Column {
        SectionEyebrow("Today")
        Text(formatted, style = RecoveryType.screenTitle, color = RecoveryColors.TextPrimary)
        Text(
            "Consistency includes taking recovery seriously.",
            style = RecoveryType.screenSubtitle,
            color = RecoveryColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun RecommendationCard(viewModel: RecoveryViewModel) {
    val level = viewModel.recommendedLevel
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(level.cardBg, RoundedCornerShape(22.dp))
            .padding(20.dp),
    ) {
        Text("TODAY'S RECOMMENDATION", style = RecoveryType.heroLabel, color = level.cardText)
        Text(level.title, style = RecoveryType.heroTitle, color = level.cardText, modifier = Modifier.padding(top = 6.dp))
        Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            viewModel.recommendationReasons.forEach { reason ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .background(level.cardText, RoundedCornerShape(50)),
                    )
                    Text(reason, style = RecoveryType.heroReason, color = level.cardText)
                }
            }
            viewModel.recoveryWarning?.let { warning ->
                Text(
                    warning,
                    style = RecoveryType.heroReason.copy(fontWeight = FontWeight.SemiBold),
                    color = level.cardText,
                )
            }
        }
    }
}

@Composable
private fun TodaysPlanSection(viewModel: RecoveryViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Today's plan", "Health data can fill this automatically in Phase 2; for now, confirm manually.")
        RecoveryCard(padding = PaddingValues(vertical = 6.dp, horizontal = 10.dp)) {
            viewModel.planItems.forEach { item ->
                PlanRow(item, onToggle = { viewModel.togglePlanItem(item.id) })
            }
        }
    }
}

@Composable
private fun PlanRow(item: PlanItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.done) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(RecoveryColors.Primary, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = RecoveryColors.Surface, style = RecoveryType.rowValue, textAlign = TextAlign.Center)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, RecoveryColors.TextMuted, RoundedCornerShape(6.dp)),
            )
        }
        Column {
            Text(item.whenLabel.uppercase(), style = RecoveryType.eyebrow, color = RecoveryColors.Primary)
            Text(item.title, style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.Medium), color = RecoveryColors.TextPrimary)
        }
    }
}

private fun formatPace(distanceKm: Double, durationMin: Int): String {
    if (distanceKm <= 0.0) return "$durationMin min"
    val paceMinPerKm = durationMin / distanceKm
    val minutes = paceMinPerKm.toInt()
    val seconds = ((paceMinPerKm - minutes) * 60).roundToInt()
    return "$durationMin min · $minutes:${seconds.toString().padStart(2, '0')} / km"
}

@Composable
private fun ActualActivitySection(viewModel: RecoveryViewModel, onEditActivity: () -> Unit) {
    val activity = viewModel.activity
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("Actual activity")
            OutlinedPillButton(
                text = "Edit",
                onClick = onEditActivity,
                modifier = Modifier.width(80.dp),
            )
        }
        RecoveryCard(padding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)) {
            StatRow("Morning walk", "${activity.morningWalkKm} km", formatPace(activity.morningWalkKm, activity.morningWalkMin))
            RowDivider()
            StatRow("Total walking", "${activity.totalWalkKm} km")
            RowDivider()
            StatRow("Steps", String.format(Locale.getDefault(), "%,d", activity.steps))
            RowDivider()
            StatRow("Swimming", "${activity.swimM} m", "${activity.swimMin} min")
            RowDivider()
            StatRow("Heart Points", "${activity.heartPoints}", "Secondary information only; never used for recovery decisions.")
        }
    }
}

@Composable
private fun RecoverySection(viewModel: RecoveryViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Recovery")
        RecoveryCard(padding = PaddingValues(top = 6.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)) {
            StatRow("Energy", "${viewModel.energy}/5")
            StatRow("Fatigue", "${viewModel.fatigue}/10")
            StatRow("Highest soreness", "${viewModel.soreness}/10")
            TomorrowPreview(viewModel.recommendedLevel)
        }
    }
}

@Composable
private fun TomorrowPreview(todayLevel: RecoveryLevel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(RecoveryLevel.NORMAL.cardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("TOMORROW", style = RecoveryType.heroLabel, color = RecoveryLevel.NORMAL.cardText)
        Text(RecoveryLevel.NORMAL.title, style = RecoveryType.heroTitle.copy(fontSize = 26.sp), color = RecoveryLevel.NORMAL.cardText, modifier = Modifier.padding(top = 4.dp))
        Text(
            when (todayLevel) {
                RecoveryLevel.EASY -> "Today is scheduled easy, and nothing in tonight's check-in extends it."
                RecoveryLevel.RECOVERY -> "A recovery day today doesn't automatically carry over — tonight's check-in decides."
                RecoveryLevel.NORMAL -> "Consistent with your recent routine."
            },
            style = RecoveryType.heroReason,
            color = RecoveryLevel.NORMAL.cardText,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SevenDayTrendSection(viewModel: RecoveryViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Seven-day trend")
        RecoveryCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrendStat("${viewModel.weekWalkTotalKm}", "km walking", Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(RecoveryColors.BorderSubtle))
                TrendStat("${viewModel.weekExerciseMinutes}", "exercise min", Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(RecoveryColors.BorderSubtle))
                TrendStat("${viewModel.weekRecoveryDays}", "recovery day", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrendStat(number: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(number, style = RecoveryType.statNumber, color = RecoveryColors.TextPrimary)
        Text(label, style = RecoveryType.statLabel, color = RecoveryColors.TextSecondary)
    }
}
