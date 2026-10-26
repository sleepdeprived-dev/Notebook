package com.david.notebook.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.david.notebook.R

/**
 * Figtree (SIL Open Font License, see tools/fonts): one variable font file carries
 * every weight, and it covers Romanian (ș ț ă â î) for subject names.
 */
@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) = Font(
    resId = R.font.figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Figtree = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
)

private val Base = Typography()

private fun TextStyle.figtree(weight: FontWeight) = copy(fontFamily = Figtree, fontWeight = weight)

/** Material's sizes, with bolder, tighter headings: a strong title is most of what reads as "modern". */
val NotebookTypography = Typography(
    displayLarge = Base.displayLarge.figtree(FontWeight.Bold),
    displayMedium = Base.displayMedium.figtree(FontWeight.Bold),
    displaySmall = Base.displaySmall.figtree(FontWeight.Bold),
    headlineLarge = Base.headlineLarge.figtree(FontWeight.Bold).copy(letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.figtree(FontWeight.Bold).copy(letterSpacing = (-0.4).sp),
    headlineSmall = Base.headlineSmall.figtree(FontWeight.SemiBold).copy(letterSpacing = (-0.2).sp),
    titleLarge = Base.titleLarge.figtree(FontWeight.Bold),
    titleMedium = Base.titleMedium.figtree(FontWeight.SemiBold),
    titleSmall = Base.titleSmall.figtree(FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.figtree(FontWeight.Normal),
    bodyMedium = Base.bodyMedium.figtree(FontWeight.Normal),
    bodySmall = Base.bodySmall.figtree(FontWeight.Normal),
    labelLarge = Base.labelLarge.figtree(FontWeight.SemiBold),
    labelMedium = Base.labelMedium.figtree(FontWeight.SemiBold),
    labelSmall = Base.labelSmall.figtree(FontWeight.SemiBold),
)
