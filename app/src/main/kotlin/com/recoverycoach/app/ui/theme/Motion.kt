package com.recoverycoach.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Every duration and easing in the app resolves here, so motion stays consistent
 * and can be tuned in one place rather than hunted through screens.
 */
object RecoveryMotion {
    const val FAST_MS = 150
    const val STANDARD_MS = 250

    /** Ceiling for anything on a navigation path — longer than this feels laggy. */
    const val EMPHASIZED_MS = 300

    /** Material's standard easing: quick to leave, gentle to settle. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
}

/**
 * True when the user has turned animations off system-wide (Developer options, or
 * the accessibility "remove animations" setting, both of which zero the animator
 * duration scale). Compose has no first-class signal for this, so read the setting.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val inspecting = LocalInspectionMode.current
    return remember(context, inspecting) {
        if (inspecting) return@remember false
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * A spec that collapses to an instant change when the user has asked for reduced
 * motion. Every animation in the app goes through this rather than calling
 * [tween] directly, so "no animations" genuinely means none.
 *
 * Returns [FiniteAnimationSpec] rather than the wider `AnimationSpec` because the
 * enter/exit transition APIs require a finite one.
 */
@Composable
fun <T> recoveryTween(
    durationMs: Int = RecoveryMotion.STANDARD_MS,
    easing: Easing = RecoveryMotion.Standard,
): FiniteAnimationSpec<T> {
    val reduce = rememberReduceMotion()
    return if (reduce) snap() else tween(durationMillis = durationMs, easing = easing)
}
