package com.recoverycoach.app.ui.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.ui.theme.rememberReduceMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * Three soft colour blobs drifting behind a card. Slow enough to read as light
 * moving rather than animation, and drawn from the passed-in palette tones so it
 * tints with the recovery level instead of introducing new colour.
 */
@Composable
fun Modifier.aurora(
    tones: List<Color>,
    cornerRadius: Dp = 22.dp,
    periodMs: Int = 14_000,
): Modifier {
    if (rememberReduceMotion() || tones.isEmpty()) return this

    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auroraPhase",
    )

    return this.drawBehind {
        val radius = size.minDimension * 0.85f
        tones.forEachIndexed { index, tone ->
            // Each blob runs on its own phase offset and a different ellipse, so
            // they never line up into an obvious repeating loop.
            val offsetPhase = phase + index * 2.1f
            val center = Offset(
                x = size.width * (0.5f + 0.34f * cos(offsetPhase + index * 0.7f)),
                y = size.height * (0.5f + 0.42f * sin(offsetPhase * 0.8f)),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tone.copy(alpha = 0.55f), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}

/**
 * A highlight that travels around a card's outline. Used sparingly — only on the
 * tips that actually need attention, so it stays meaningful.
 */
@Composable
fun Modifier.travellingBorder(
    baseColor: Color,
    highlightColor: Color,
    cornerRadius: Dp = 16.dp,
    width: Dp = 1.5.dp,
    periodMs: Int = 2600,
): Modifier {
    if (rememberReduceMotion()) {
        return this.drawWithContent {
            drawContent()
            drawRoundRect(
                color = highlightColor,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = width.toPx()),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "border")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "borderSweep",
    )

    return this.drawWithContent {
        drawContent()
        val travel = size.width + size.height
        val head = progress * travel * 2f - travel * 0.5f
        val band = travel * 0.35f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(head, 0f),
                end = Offset(head + band, size.height),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = width.toPx()),
            size = Size(size.width, size.height),
        )
    }
}
