package com.recoverycoach.app.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.ui.theme.RecoveryMotion
import com.recoverycoach.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * A burst of short rays from wherever the finger landed. Replaces the tap ripple
 * with something that reads as an event rather than a state change.
 *
 * Handles the click itself, so callers should not also attach `clickable` —
 * doing both would fire twice.
 */
@Composable
fun Modifier.clickSpark(
    color: Color,
    rayCount: Int = 8,
    radius: Dp = 30.dp,
    onClick: () -> Unit,
): Modifier {
    val reduce = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var origin by remember { mutableStateOf<Offset?>(null) }

    return this
        .pointerInput(reduce) {
            detectTapGestures { position ->
                onClick()
                if (reduce) return@detectTapGestures
                origin = position
                scope.launch {
                    progress.snapTo(0f)
                    progress.animateTo(1f, tween(430, easing = RecoveryMotion.Decelerate))
                }
            }
        }
        .drawWithContent {
            drawContent()
            val start = origin ?: return@drawWithContent
            val p = progress.value
            if (p <= 0f || p >= 1f) return@drawWithContent

            val maxRadius = radius.toPx()
            val fade = (1f - p).coerceIn(0f, 1f)
            repeat(rayCount) { index ->
                val angle = (2.0 * Math.PI * index / rayCount).toFloat()
                val direction = Offset(cos(angle), sin(angle))
                drawLine(
                    color = color.copy(alpha = fade * 0.9f),
                    start = start + direction * (maxRadius * p * 0.5f),
                    end = start + direction * (maxRadius * p),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
}

/**
 * Leans toward the finger on press and settles back on release, with a small
 * scale dip. Gives a card weight without changing its layout.
 */
@Composable
fun Modifier.magneticPress(
    pull: Dp = 5.dp,
    pressedScale: Float = 0.985f,
): Modifier {
    val reduce = rememberReduceMotion()
    if (reduce) return this

    var pressPoint by remember { mutableStateOf<Offset?>(null) }
    var boxSize by remember { mutableStateOf(Offset.Zero) }

    // Normalised -1..1 offset of the press from centre, so the lean follows where
    // the finger actually is rather than always tilting the same way.
    val fraction = pressPoint?.let { point ->
        if (boxSize.x <= 0f || boxSize.y <= 0f) Offset.Zero
        else Offset(
            ((point.x / boxSize.x) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f,
            ((point.y / boxSize.y) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f,
        )
    } ?: Offset.Zero

    val pressed = pressPoint != null
    val spec = tween<Float>(RecoveryMotion.FAST_MS, easing = RecoveryMotion.Standard)
    val dx by animateFloatAsState(fraction.x, spec, label = "magnetX")
    val dy by animateFloatAsState(fraction.y, spec, label = "magnetY")
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, spec, label = "magnetScale")

    return this
        .pointerInput(Unit) {
            boxSize = Offset(size.width.toFloat(), size.height.toFloat())
            detectTapGestures(
                onPress = { offset ->
                    pressPoint = offset
                    tryAwaitRelease()
                    pressPoint = null
                },
            )
        }
        .graphicsLayer {
            val pullPx = pull.toPx()
            translationX = dx * pullPx
            translationY = dy * pullPx
            scaleX = scale
            scaleY = scale
        }
}
