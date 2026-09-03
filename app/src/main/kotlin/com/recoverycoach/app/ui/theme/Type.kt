package com.recoverycoach.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.recoverycoach.app.R

/**
 * Instrument Sans is a variable font (one file, many weights), so each
 * weight below points at the same font file with a different weight axis
 * setting rather than a separate file per weight.
 */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.instrument_sans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.instrument_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.instrument_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Instrument Serif is used only for large headlines — it ships as a single regular weight. */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal),
)

/** Reusable text styles that match the design's recurring type patterns. */
object RecoveryType {
    val eyebrow = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp)
    val screenTitle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 29.sp, lineHeight = 32.sp)
    val screenSubtitle = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp)
    val heroLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 1.1.sp)
    val heroTitle = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 38.sp)
    val heroReason = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp)
    val sectionTitle = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    val sectionHint = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp)
    val rowLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 15.sp)
    val rowValue = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val rowCaption = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 12.sp)
    val statNumber = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = 24.sp)
    val statLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 11.5.sp)
    val button = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
    val navLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 12.5.sp)
    val navLabelActive = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
}

val RecoveryTypography = Typography(
    bodyLarge = RecoveryType.rowLabel,
    bodyMedium = RecoveryType.sectionHint,
    titleLarge = RecoveryType.screenTitle,
    titleMedium = RecoveryType.sectionTitle,
    labelSmall = RecoveryType.eyebrow,
)
