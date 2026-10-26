package com.david.notebook.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.david.notebook.data.settings.Accent

/** The four primary-role colours of one accent, for one brightness. */
private data class Tones(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
)

private data class Palette(val light: Tones, val dark: Tones, val swatch: Color)

// Tones 40/100/90/10 for light and 80/20/30/90 for dark, as Material 3 derives them
// from each seed colour. That's why light-mode yellow reads as a deep gold: a bright
// yellow on white would fail contrast for text and icons. [swatch] is the vivid seed,
// used only for the colour picker circles.
private val palettes = mapOf(
    Accent.Red to Palette(
        Tones(Color(0xFFBB1614), Color.White, Color(0xFFFFDAD5), Color(0xFF410001)),
        Tones(Color(0xFFFFB4A9), Color(0xFF690001), Color(0xFF930001), Color(0xFFFFDAD5)),
        swatch = Color(0xFFE53935),
    ),
    Accent.Orange to Palette(
        Tones(Color(0xFF9A4600), Color.White, Color(0xFFFFDBC8), Color(0xFF321300)),
        Tones(Color(0xFFFFB68C), Color(0xFF532200), Color(0xFF763300), Color(0xFFFFDBC8)),
        swatch = Color(0xFFFB8C00),
    ),
    Accent.Yellow to Palette(
        Tones(Color(0xFF6D5E00), Color.White, Color(0xFFF9E359), Color(0xFF211B00)),
        Tones(Color(0xFFDCC73F), Color(0xFF393000), Color(0xFF524600), Color(0xFFF9E359)),
        swatch = Color(0xFFFDD835),
    ),
    Accent.Green to Palette(
        Tones(Color(0xFF006E1C), Color.White, Color(0xFF94F990), Color(0xFF002204)),
        Tones(Color(0xFF78DC77), Color(0xFF00390A), Color(0xFF005313), Color(0xFF94F990)),
        swatch = Color(0xFF43A047),
    ),
    Accent.Blue to Palette(
        Tones(Color(0xFF0061A4), Color.White, Color(0xFFD1E4FF), Color(0xFF001D36)),
        Tones(Color(0xFF9ECAFF), Color(0xFF003258), Color(0xFF00497D), Color(0xFFD1E4FF)),
        swatch = Color(0xFF1E88E5),
    ),
    Accent.Purple to Palette(
        Tones(Color(0xFF6750A4), Color.White, Color(0xFFEADDFF), Color(0xFF21005D)),
        Tones(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF)),
        swatch = Color(0xFF8E24AA),
    ),
)

private val FallbackAccent = Accent.Blue

fun colorSchemeFor(accent: Accent, darkTheme: Boolean, context: Context): ColorScheme {
    if (accent == Accent.Dynamic && Accent.dynamicSupported) {
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    val palette = palettes[accent] ?: palettes.getValue(FallbackAccent)
    val t = if (darkTheme) palette.dark else palette.light
    // Secondary follows the accent too: the bottom bar's selected pill is secondaryContainer.
    return if (darkTheme) {
        darkColorScheme(
            primary = t.primary, onPrimary = t.onPrimary,
            primaryContainer = t.container, onPrimaryContainer = t.onContainer,
            secondary = t.primary, onSecondary = t.onPrimary,
            secondaryContainer = t.container, onSecondaryContainer = t.onContainer,
        )
    } else {
        lightColorScheme(
            primary = t.primary, onPrimary = t.onPrimary,
            primaryContainer = t.container, onPrimaryContainer = t.onContainer,
            secondary = t.primary, onSecondary = t.onPrimary,
            secondaryContainer = t.container, onSecondaryContainer = t.onContainer,
        )
    }
}

/** The colour to show for [accent] in the picker: the vivid seed, or the wallpaper colour. */
fun swatchFor(accent: Accent, darkTheme: Boolean, context: Context): Color =
    palettes[accent]?.swatch ?: colorSchemeFor(accent, darkTheme, context).primary

@Composable
fun NotebookTheme(
    darkTheme: Boolean,
    accent: Accent,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(accent, darkTheme, LocalContext.current),
        typography = NotebookTypography,
        content = content,
    )
}
