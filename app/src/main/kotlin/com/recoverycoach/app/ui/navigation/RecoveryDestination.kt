package com.recoverycoach.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

enum class RecoveryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    TODAY("today", "Today", Icons.Outlined.Today, Icons.Filled.Today),
    CHECK_IN("check_in", "Check-in", Icons.Outlined.FactCheck, Icons.Filled.FactCheck),
    INSIGHTS("insights", "Insights", Icons.Outlined.Insights, Icons.Filled.Insights),
    WEEK("week", "Week", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}
