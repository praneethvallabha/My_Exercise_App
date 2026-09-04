package com.recoverycoach.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.data.RecoveryViewModel
import com.recoverycoach.app.domain.GuidanceEngine
import com.recoverycoach.app.domain.GuidanceResult
import com.recoverycoach.app.domain.Tip
import com.recoverycoach.app.domain.TipSeverity
import com.recoverycoach.app.ui.components.RecoveryCard
import com.recoverycoach.app.ui.effects.BlurIn
import com.recoverycoach.app.ui.effects.CountUpText
import com.recoverycoach.app.ui.effects.ShinyText
import com.recoverycoach.app.ui.effects.aurora
import com.recoverycoach.app.ui.effects.clickSpark
import com.recoverycoach.app.ui.effects.magneticPress
import com.recoverycoach.app.ui.effects.travellingBorder
import com.recoverycoach.app.ui.components.SectionEyebrow
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryMotion
import com.recoverycoach.app.ui.theme.RecoveryType
import com.recoverycoach.app.ui.theme.recoveryTween

private const val STAGGER_MS = 55

enum class InsightsFilter(val label: String, val icon: ImageVector) {
    INSIGHTS("Insights", Icons.Outlined.Insights),
    RECOVERY("Recovery", Icons.Outlined.SelfImprovement),
}

@Composable
fun InsightsScreen(viewModel: RecoveryViewModel, modifier: Modifier = Modifier) {
    var filter by remember { mutableStateOf(InsightsFilter.INSIGHTS) }
    val guidance = viewModel.guidance

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            SectionEyebrow("Insights")
            ShinyText(
                text = when (filter) {
                    InsightsFilter.INSIGHTS -> "What your training says"
                    InsightsFilter.RECOVERY -> "How to come back from it"
                },
                style = RecoveryType.screenTitle,
                baseColor = RecoveryColors.TextPrimary,
                highlightColor = RecoveryColors.Primary,
            )
            Text(
                when (filter) {
                    InsightsFilter.INSIGHTS ->
                        "Your logged work, measured against published guidance. Every line names its source."
                    InsightsFilter.RECOVERY ->
                        "Recovery guidance drawn from the same kind of sources, shaped by what you have logged."
                },
                style = RecoveryType.screenSubtitle,
                color = RecoveryColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        FilterRow(selected = filter, onSelect = { filter = it })

        // transitionSpec runs outside composition, so the spec is built here and
        // captured rather than called inside the lambda.
        val fadeSpec = recoveryTween<Float>(RecoveryMotion.STANDARD_MS)
        AnimatedContent(
            targetState = filter,
            transitionSpec = { fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec) },
            label = "insightsFilter",
        ) { active ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (active) {
                    InsightsFilter.INSIGHTS -> {
                        BlurIn { WeeklyScorecard(guidance) }
                        if (guidance.daysUntilFullGuidance > 0) {
                            BlurIn(delayMs = STAGGER_MS) { BaselineNotice(guidance) }
                        }
                        guidance.insights.forEachIndexed { index, tip ->
                            BlurIn(delayMs = (index + 2) * STAGGER_MS) { TipCard(tip) }
                        }
                    }
                    InsightsFilter.RECOVERY -> {
                        guidance.recovery.forEachIndexed { index, tip ->
                            BlurIn(delayMs = index * STAGGER_MS) { TipCard(tip) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: InsightsFilter, onSelect: (InsightsFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RecoveryColors.BorderSubtle, RoundedCornerShape(100.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InsightsFilter.entries.forEach { entry ->
            FilterChip(
                entry = entry,
                selected = entry == selected,
                onClick = { onSelect(entry) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FilterChip(
    entry: InsightsFilter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) RecoveryColors.Surface else Color.Transparent,
        animationSpec = recoveryTween(RecoveryMotion.FAST_MS),
        label = "filterBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) RecoveryColors.PrimaryPressed else RecoveryColors.TextSecondary,
        animationSpec = recoveryTween(RecoveryMotion.FAST_MS),
        label = "filterFg",
    )
    Row(
        modifier = modifier
            .clickSpark(color = RecoveryColors.Primary, onClick = onClick)
            .background(bg, RoundedCornerShape(100.dp))
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(entry.icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
        Text(
            entry.label,
            style = RecoveryType.button,
            color = fg,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

@Composable
private fun WeeklyScorecard(guidance: GuidanceResult) {
    RecoveryCard(
        modifier = Modifier.aurora(
            tones = listOf(RecoveryColors.PrimarySoft, RecoveryColors.NormalBg, RecoveryColors.EasyBg),
            cornerRadius = 16.dp,
        ),
        padding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Aerobic minutes", style = RecoveryType.rowLabel, color = RecoveryColors.TextPrimary)
            Row(verticalAlignment = Alignment.Bottom) {
                CountUpText(
                    value = guidance.weeklyAerobicMinutes,
                    style = RecoveryType.statNumber,
                    color = RecoveryColors.TextPrimary,
                )
                Text(
                    " / ${guidance.weeklyAerobicTargetMinutes}",
                    style = RecoveryType.rowValue,
                    color = RecoveryColors.TextMuted,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        ProgressTrack(
            fraction = guidance.weeklyAerobicMinutes.toFloat() / guidance.weeklyAerobicTargetMinutes,
            color = RecoveryColors.Primary,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Strength sessions", style = RecoveryType.rowLabel, color = RecoveryColors.TextPrimary)
            Row(verticalAlignment = Alignment.Bottom) {
                CountUpText(
                    value = guidance.strengthSessions,
                    style = RecoveryType.statNumber,
                    color = RecoveryColors.TextPrimary,
                )
                Text(
                    " / ${guidance.weeklyStrengthTarget}",
                    style = RecoveryType.rowValue,
                    color = RecoveryColors.TextMuted,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        ProgressTrack(
            fraction = guidance.strengthSessions.toFloat() / guidance.weeklyStrengthTarget,
            color = RecoveryColors.LoadEasy,
        )

        Text(
            "${guidance.activeDays} active ${if (guidance.activeDays == 1) "day" else "days"} of " +
                "${GuidanceEngine.WINDOW_DAYS} · target is at least ${GuidanceEngine.MIN_ACTIVE_DAYS_PER_WEEK}",
            style = RecoveryType.rowCaption,
            color = RecoveryColors.TextSecondary,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun ProgressTrack(fraction: Float, color: Color) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = recoveryTween(RecoveryMotion.EMPHASIZED_MS, RecoveryMotion.Decelerate),
        label = "progress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp)
            .height(8.dp)
            .background(RecoveryColors.BorderSubtle, RoundedCornerShape(100.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .background(color, RoundedCornerShape(100.dp)),
        )
    }
}

@Composable
private fun BaselineNotice(guidance: GuidanceResult) {
    val remaining = guidance.daysUntilFullGuidance
    RecoveryCard(padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                Icons.Outlined.Bolt,
                contentDescription = null,
                tint = RecoveryColors.Primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "$remaining more ${if (remaining == 1) "day" else "days"} to a full week",
                style = RecoveryType.rowValue,
                color = RecoveryColors.TextPrimary,
            )
        }
        Text(
            "Weekly targets need seven logged days before they mean anything. " +
                "${guidance.daysLogged} recorded so far.",
            style = RecoveryType.rowCaption,
            color = RecoveryColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private val TipSeverity.accent: Color
    get() = when (this) {
        TipSeverity.PRIORITY -> RecoveryColors.LoadRecovery
        TipSeverity.SUGGESTION -> RecoveryColors.LoadEasy
        TipSeverity.INFO -> RecoveryColors.Primary
    }

@Composable
private fun TipCard(tip: Tip) {
    // Only the tips that actually need attention get the travelling edge — used
    // on everything it would just be noise.
    val cardModifier = Modifier
        .magneticPress()
        .let {
            if (tip.severity == TipSeverity.PRIORITY) {
                it.travellingBorder(
                    baseColor = RecoveryColors.BorderSubtle,
                    highlightColor = tip.severity.accent,
                )
            } else {
                it
            }
        }
    RecoveryCard(modifier = cardModifier, padding = PaddingValues(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .background(tip.severity.accent, RoundedCornerShape(50)),
            )
            Column {
                Text(
                    tip.title,
                    style = RecoveryType.rowValue.copy(fontWeight = FontWeight.SemiBold),
                    color = RecoveryColors.TextPrimary,
                )
                Text(
                    tip.body,
                    style = RecoveryType.heroReason,
                    color = RecoveryColors.TextSecondary,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Text(
                    tip.source,
                    style = RecoveryType.rowCaption,
                    color = RecoveryColors.TextMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
