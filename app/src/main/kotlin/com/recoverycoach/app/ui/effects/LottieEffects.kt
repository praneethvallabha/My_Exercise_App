package com.recoverycoach.app.ui.effects

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.recoverycoach.app.R
import com.recoverycoach.app.ui.theme.rememberReduceMotion

/**
 * The two Lottie animations are hand-authored in this repo's own palette rather
 * than pulled from a stock library, so there is no third-party licence attached
 * to them and nothing to attribute. They ship as raw resources and never touch
 * the network.
 */

/**
 * A one-shot burst, for the moment something is actually achieved. Draws nothing
 * when [play] is false or the user has asked for reduced motion.
 */
@Composable
fun CelebrationBurst(
    play: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
) {
    if (!play || rememberReduceMotion()) return
    LottieOneShot(R.raw.lottie_celebration, modifier.size(size))
}

/** A slow breathing pulse for empty states — waiting for data, not loading. */
@Composable
fun EmptyStatePulse(modifier: Modifier = Modifier, size: Dp = 76.dp) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_empty_pulse),
    )
    // Reduced motion still gets the mark, just held on its first frame rather
    // than removed — the card would otherwise lose its anchor entirely.
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (rememberReduceMotion()) 1 else LottieConstants.IterateForever,
        isPlaying = !rememberReduceMotion(),
    )
    LottieAnimation(composition, { progress }, modifier = modifier.size(size))
}

@Composable
private fun LottieOneShot(@RawRes res: Int, modifier: Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(res))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)
    LottieAnimation(composition, { progress }, modifier = modifier)
}
