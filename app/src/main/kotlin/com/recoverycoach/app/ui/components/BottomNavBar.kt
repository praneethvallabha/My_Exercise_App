package com.recoverycoach.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.ui.navigation.RecoveryDestination
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryMotion
import com.recoverycoach.app.ui.theme.RecoveryType
import com.recoverycoach.app.ui.theme.recoveryTween

@Composable
fun BottomNavBar(
    current: RecoveryDestination,
    onSelect: (RecoveryDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RecoveryColors.Surface)
            .padding(top = 8.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
    ) {
        RecoveryDestination.entries.forEach { destination ->
            NavItem(
                destination = destination,
                selected = destination == current,
                onClick = { onSelect(destination) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(
    destination: RecoveryDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Animating one row's colours beats swapping between two different Texts:
    // the pill grows into place instead of popping, and the layout never jumps.
    val contentColor by animateColorAsState(
        targetValue = if (selected) RecoveryColors.PrimaryPressed else RecoveryColors.TextSecondary,
        animationSpec = recoveryTween(RecoveryMotion.FAST_MS),
        label = "navContentColor",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) RecoveryColors.PrimarySoft else Color.Transparent,
        animationSpec = recoveryTween(RecoveryMotion.FAST_MS),
        label = "navPillColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = recoveryTween(RecoveryMotion.FAST_MS),
        label = "navIconScale",
    )

    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier
                .background(pillColor, RoundedCornerShape(100.dp))
                .padding(horizontal = 18.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale),
            )
        }
        Text(
            destination.label,
            style = if (selected) RecoveryType.navLabelActive else RecoveryType.navLabel,
            color = contentColor,
        )
    }
}
