package com.recoverycoach.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.domain.GuidanceEngine
import com.recoverycoach.app.domain.GuidanceResult
import com.recoverycoach.app.domain.Tip
import com.recoverycoach.app.domain.TipSeverity
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryMotion
import com.recoverycoach.app.ui.theme.RecoveryType
import com.recoverycoach.app.ui.theme.recoveryTween

private val TipSeverity.accent: Color
    get() = when (this) {
        TipSeverity.ATTENTION -> RecoveryColors.LoadRecovery
        TipSeverity.SUGGESTION -> RecoveryColors.LoadEasy
        TipSeverity.INFO -> RecoveryColors.Primary
    }

private val TipSeverity.icon: ImageVector
    get() = when (this) {
        TipSeverity.ATTENTION -> Icons.Outlined.PriorityHigh
        TipSeverity.SUGGESTION -> Icons.Outlined.Lightbulb
        TipSeverity.INFO -> Icons.Outlined.Info
    }

/**
 * Sourced guidance for today. Until a full week of real days has accumulated this
 * shows how many are still needed instead of a tip — a rule fired against two days
 * of data would look authoritative and mean nothing.
 */
@Composable
fun GuidanceCard(guidance: GuidanceResult, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = recoveryTween<IntSize>(RecoveryMotion.STANDARD_MS)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle("Guidance", "Based on your logged days, checked against published guidance.")

        RecoveryCard(padding = PaddingValues(16.dp)) {
            WeeklyProgress(guidance)

            if (guidance.daysUntilFullGuidance > 0) {
                BaselineNotice(guidance)
            }

            // The card's animateContentSize carries the entry; wrapping each tip in
            // an always-visible AnimatedVisibility would animate nothing.
            guidance.tips.forEachIndexed { index, tip ->
                if (index > 0 || guidance.daysUntilFullGuidance == 0) {
                    Box(Modifier.height(12.dp))
                }
                TipRow(tip)
            }

            Text(
                "General guidance, not personal medical advice. Anything that concerns you is " +
                    "worth raising with your doctor.",
                style = RecoveryType.rowCaption,
                color = RecoveryColors.TextMuted,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun WeeklyProgress(guidance: GuidanceResult) {
    val target = guidance.weeklyAerobicTargetMinutes
    val fraction = (guidance.weeklyAerobicMinutes.toFloat() / target).coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("This week", style = RecoveryType.rowLabel, color = RecoveryColors.TextPrimary)
            Text(
                "${guidance.weeklyAerobicMinutes} / $target min",
                style = RecoveryType.rowValue,
                color = RecoveryColors.TextPrimary,
            )
        }
        ProgressTrack(fraction)
        Text(
            "Walk and swim minutes, counted as moderate intensity. " +
                "${guidance.activeDays} active ${if (guidance.activeDays == 1) "day" else "days"} · " +
                "${guidance.strengthSessions} strength ${if (guidance.strengthSessions == 1) "session" else "sessions"}.",
            style = RecoveryType.rowCaption,
            color = RecoveryColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ProgressTrack(fraction: Float) {
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = recoveryTween(RecoveryMotion.EMPHASIZED_MS, RecoveryMotion.Decelerate),
        label = "weeklyProgress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(8.dp)
            .background(RecoveryColors.BorderSubtle, RoundedCornerShape(100.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .background(RecoveryColors.Primary, RoundedCornerShape(100.dp)),
        )
    }
}

@Composable
private fun BaselineNotice(guidance: GuidanceResult) {
    val remaining = guidance.daysUntilFullGuidance
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .background(RecoveryColors.PrimarySoft, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            "Building your baseline",
            style = RecoveryType.rowLabel.copy(fontWeight = FontWeight.SemiBold),
            color = RecoveryColors.PrimaryPressed,
        )
        Text(
            "$remaining more ${if (remaining == 1) "day" else "days"} of logging before weekly " +
                "guidance can say anything meaningful. ${guidance.daysLogged} of " +
                "${GuidanceEngine.WINDOW_DAYS} days recorded so far.",
            style = RecoveryType.rowCaption,
            color = RecoveryColors.PrimaryPressed,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TipRow(tip: Tip) {
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(26.dp)
                .background(tip.severity.accent.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tip.severity.icon,
                contentDescription = null,
                tint = tip.severity.accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(tip.title, style = RecoveryType.rowValue, color = RecoveryColors.TextPrimary)
            Text(
                tip.body,
                style = RecoveryType.heroReason,
                color = RecoveryColors.TextSecondary,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                tip.source,
                style = RecoveryType.rowCaption,
                color = RecoveryColors.TextMuted,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}
