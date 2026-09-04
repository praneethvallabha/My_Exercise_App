package com.recoverycoach.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.recoverycoach.app.ui.effects.clickSpark
import com.recoverycoach.app.ui.effects.magneticPress
import com.recoverycoach.app.ui.theme.RecoveryColors
import com.recoverycoach.app.ui.theme.RecoveryType

/** The cream card used everywhere in the design: white-ish surface, thin border, 16dp corners. */
@Composable
fun RecoveryCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RecoveryColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, RecoveryColors.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionEyebrow(text: String, color: Color = RecoveryColors.Primary) {
    Text(text.uppercase(), style = RecoveryType.eyebrow, color = color)
}

@Composable
fun SectionTitle(title: String, hint: String? = null) {
    Column {
        Text(title, style = RecoveryType.sectionTitle, color = RecoveryColors.TextPrimary)
        if (hint != null) {
            Text(hint, style = RecoveryType.sectionHint, color = RecoveryColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** A "label ........ value" row, the most common pattern in every card. */
@Composable
fun StatRow(
    label: String,
    value: String,
    caption: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, style = RecoveryType.rowLabel, color = RecoveryColors.TextPrimary)
            if (caption != null) {
                Text(caption, style = RecoveryType.rowCaption, color = RecoveryColors.TextSecondary)
            }
        }
        Text(value, style = RecoveryType.rowValue, color = RecoveryColors.TextPrimary)
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(color = RecoveryColors.BorderSubtle, thickness = 1.dp)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .magneticPress()
            .clickSpark(color = RecoveryColors.Surface, onClick = onClick)
            .background(RecoveryColors.Primary, RoundedCornerShape(100.dp))
            .height(50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = RecoveryType.button, color = RecoveryColors.Surface)
    }
}

@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = RecoveryColors.BorderStrong,
    textColor: Color = RecoveryColors.Primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .magneticPress()
            .clickSpark(color = textColor, onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(100.dp))
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = RecoveryType.button, color = textColor)
    }
}
