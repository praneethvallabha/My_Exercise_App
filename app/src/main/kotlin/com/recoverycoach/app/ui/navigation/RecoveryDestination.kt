package com.recoverycoach.app.ui.navigation

import androidx.annotation.DrawableRes
import com.recoverycoach.app.R

/**
 * Each tab points at an AnimatedVectorDrawable rather than a pair of static
 * icons, so selecting a tab animates the outline into its active state instead
 * of swapping one image for another.
 */
enum class RecoveryDestination(
    val route: String,
    val label: String,
    @param:DrawableRes val animatedIcon: Int,
) {
    TODAY("today", "Today", R.drawable.avd_nav_today),
    CHECK_IN("check_in", "Check-in", R.drawable.avd_nav_checkin),
    INSIGHTS("insights", "Insights", R.drawable.avd_nav_insights),
    WEEK("week", "Week", R.drawable.avd_nav_week),
    SETTINGS("settings", "Settings", R.drawable.avd_nav_settings),
}
