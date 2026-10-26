package com.david.notebook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.david.notebook.data.courses.TagColor

/** True when the app is drawing its dark scheme (which may differ from the phone's setting). */
@Composable
@ReadOnlyComposable
fun isAppInDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

private data class TagTones(
    val lightContainer: Color,
    val lightContent: Color,
    val darkContainer: Color,
    val darkContent: Color,
    val dot: Color,
)

// Tag colours are fixed (they don't follow the accent): a red "Exam" tag should
// stay red whichever accent is picked. Pastel chip + dark text in light mode,
// deep chip + pale text in dark mode, so the label is readable on either.
private val tones = mapOf(
    TagColor.Red to TagTones(Color(0xFFFFDAD6), Color(0xFF410002), Color(0xFF93000A), Color(0xFFFFDAD6), Color(0xFFE53935)),
    TagColor.Orange to TagTones(Color(0xFFFFDCC2), Color(0xFF2E1500), Color(0xFF6E3900), Color(0xFFFFDCC2), Color(0xFFFB8C00)),
    TagColor.Yellow to TagTones(Color(0xFFFFF0A8), Color(0xFF221B00), Color(0xFF534600), Color(0xFFFFF0A8), Color(0xFFFDD835)),
    TagColor.Green to TagTones(Color(0xFFC4EFC0), Color(0xFF00210A), Color(0xFF1E5128), Color(0xFFC4EFC0), Color(0xFF43A047)),
    TagColor.Teal to TagTones(Color(0xFFB9F0EA), Color(0xFF00201D), Color(0xFF00504A), Color(0xFFB9F0EA), Color(0xFF00897B)),
    TagColor.Blue to TagTones(Color(0xFFD3E4FF), Color(0xFF001C38), Color(0xFF004A77), Color(0xFFD3E4FF), Color(0xFF1E88E5)),
    TagColor.Purple to TagTones(Color(0xFFF3DAFF), Color(0xFF2E004E), Color(0xFF6A2A8C), Color(0xFFF3DAFF), Color(0xFF8E24AA)),
    TagColor.Pink to TagTones(Color(0xFFFFD9E3), Color(0xFF3E0021), Color(0xFF8A1F4E), Color(0xFFFFD9E3), Color(0xFFD81B60)),
    TagColor.Grey to TagTones(Color(0xFFE3E2E6), Color(0xFF1B1B1F), Color(0xFF46464F), Color(0xFFE3E2E6), Color(0xFF757575)),
)

/** Background and text colour for a chip of this colour. */
@Composable
@ReadOnlyComposable
fun TagColor.chipColors(): Pair<Color, Color> {
    val t = tones.getValue(this)
    return if (isAppInDarkTheme()) t.darkContainer to t.darkContent else t.lightContainer to t.lightContent
}

/** The vivid colour, for the picker circles and the dots in filter chips. */
val TagColor.dot: Color get() = tones.getValue(this).dot
