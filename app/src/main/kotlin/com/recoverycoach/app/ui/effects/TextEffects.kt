package com.recoverycoach.app.ui.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.recoverycoach.app.ui.theme.RecoveryMotion
import com.recoverycoach.app.ui.theme.rememberReduceMotion
import kotlin.math.roundToInt

/**
 * A number that counts up to its value when it first appears, and animates
 * between values afterwards. Makes a scorecard feel like it was measured rather
 * than printed.
 */
@Composable
fun CountUpText(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMs: Int = 900,
    format: (Int) -> String = { it.toString() },
) {
    val reduce = rememberReduceMotion()
    var target by remember { mutableStateOf(if (reduce) value else 0) }
    LaunchedEffect(value, reduce) { target = value }

    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = if (reduce) tween(0) else tween(durationMs, easing = RecoveryMotion.Decelerate),
        label = "countUp",
    )
    Text(format(animated), style = style, color = color, modifier = modifier)
}

/** Same, for values with one decimal place. */
@Composable
fun CountUpDecimal(
    value: Double,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMs: Int = 900,
) {
    CountUpText(
        value = (value * 10).roundToInt(),
        style = style,
        color = color,
        modifier = modifier,
        durationMs = durationMs,
        format = { tenths -> ((tenths / 10.0 * 10).roundToInt() / 10.0).toString() },
    )
}

/**
 * Text with a highlight that sweeps across it on a slow loop. The sweep colour
 * sits inside the existing palette — it lifts the text rather than recolouring it.
 */
@Composable
fun ShinyText(
    text: String,
    style: TextStyle,
    baseColor: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    periodMs: Int = 4200,
) {
    if (rememberReduceMotion()) {
        Text(text, style = style, color = baseColor, modifier = modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "shine")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shineSweep",
    )

    // The band is wider than the travel step so the highlight glides rather than
    // blinking, and it starts fully off-screen on both ends.
    val bandWidth = 260f
    val head = progress * (1400f + bandWidth * 2) - bandWidth
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(head, 0f),
        end = Offset(head + bandWidth, bandWidth * 0.4f),
    )
    Text(text, style = style.copy(brush = brush), modifier = modifier)
}

/**
 * Content that resolves from blurred and slightly transparent when it enters.
 *
 * `Modifier.blur` needs API 31; below that it is a no-op, so the fade alone
 * carries the entrance on older devices rather than the content not appearing.
 */
@Composable
fun BlurIn(
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    durationMs: Int = RecoveryMotion.EMPHASIZED_MS,
    startRadius: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    val reduce = rememberReduceMotion()
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(reduce) { shown = true }

    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (reduce) tween(0) else tween(durationMs, delayMs, RecoveryMotion.Decelerate),
        label = "blurIn",
    )

    Box(
        modifier = modifier
            .alpha(progress)
            .blur(startRadius * (1f - progress)),
    ) {
        content()
    }
}
