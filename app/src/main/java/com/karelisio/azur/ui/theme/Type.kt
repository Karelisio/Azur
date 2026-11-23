package com.karelisio.azur.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val base = Typography()

val AzurTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-0.04).em),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.03).em),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Medium),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** Gros chiffres de température (typographie expressive). */
object AzurText {
    val Hero = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Thin,
        fontSize = 128.sp,
        lineHeight = 128.sp,
        letterSpacing = (-0.06).em,
    )
    val Big = TextStyle(fontWeight = FontWeight.Light, fontSize = 40.sp, letterSpacing = (-0.03).em)
    val CardValue = TextStyle(fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.02).em)
}
