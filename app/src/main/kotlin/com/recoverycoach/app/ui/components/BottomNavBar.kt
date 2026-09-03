package com.recoverycoach.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.ui.navigation.RecoveryDestination
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryType

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
            .padding(top = 10.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
    ) {
        RecoveryDestination.entries.forEach { destination ->
            val selected = destination == current
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(destination) },
                horizontalArrangement = Arrangement.Center,
            ) {
                if (selected) {
                    Text(
                        destination.label,
                        style = RecoveryType.navLabelActive,
                        color = RecoveryColors.PrimaryPressed,
                        modifier = Modifier
                            .background(RecoveryColors.PrimarySoft, RoundedCornerShape(100.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    Text(
                        destination.label,
                        style = RecoveryType.navLabel,
                        color = RecoveryColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
