package com.example.chopsticks.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class HandTheme(
    val name: String,
    val backgroundColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val handColor: Color,
    val fingerColor: Color,
    val textColor: Color,
    val cornerRadius: Dp = 0.dp,
    val organicEdges: Boolean = false,
    val plasticTexture: Boolean = false,
    val isDark: Boolean = true,
    /** The typeface for this theme's text. */
    val typeface: FontFamily = FontFamily.Monospace
) {
    object AnalogSynth : HandTheme(
        name = "Analog Synth",
        backgroundColor = Color(0xFF1A1A1A),
        primaryColor = Color(0xFFFFCC00),
        secondaryColor = Color(0xFF333333),
        handColor = Color(0xFF444444),
        fingerColor = Color(0xFFFF3300),
        textColor = Color(0xFFEEEEEE),
        cornerRadius = 8.dp,
        plasticTexture = true
    )

    /** Paper sprites on a cardboard stage. See PaperStage.kt. */
    object PaperCutout : HandTheme(
        name = "Paper Cutout",
        backgroundColor = Color(0xFFF7F1E3),
        primaryColor = Color(0xFFE2412F),
        secondaryColor = Color(0xFFF6C443),
        handColor = Color(0xFFFFFFFF),
        fingerColor = Color(0xFFFFFFFF),
        textColor = Color(0xFF2A2838),
        cornerRadius = 16.dp,
        isDark = false,
        typeface = FontFamily.SansSerif
    )

    /** Hand shadows on a lamp-lit screen. See ShadowTheatre.kt. */
    object Ombromanie : HandTheme(
        name = "Ombromanie",
        backgroundColor = Color(0xFFEFD9A9),
        primaryColor = Color(0xFF7E1A16),
        secondaryColor = Color(0xFFD9A25F),
        handColor = Color(0xFF15100E),
        fingerColor = Color(0xFF15100E),
        textColor = Color(0xFF22140F),
        cornerRadius = 0.dp,
        isDark = false,
        typeface = FontFamily.Serif
    )

    /** A page of a medieval manuscript. See Manuscript.kt. */
    object Marginalia : HandTheme(
        name = "Marginalia",
        backgroundColor = Color(0xFFEFE2C4),
        primaryColor = Color(0xFF22408F),
        secondaryColor = Color(0xFFC9A13B),
        handColor = Color(0xFF2B1C12),
        fingerColor = Color(0xFF2B1C12),
        textColor = Color(0xFF2B1C12),
        cornerRadius = 0.dp,
        isDark = false,
        typeface = FontFamily.Serif
    )

    companion object {
        /**
         * Returns all available themes. We use a getter to ensure that the objects
         * (AnalogSynth, PaperCutout) are fully initialized before being collected into a list.
         */
        val allThemes: List<HandTheme>
            get() = listOfNotNull(
                Ombromanie,
                Marginalia,
                AnalogSynth,
                PaperCutout
            )

        /** The theme new players start with. */
        val Default: HandTheme get() = Ombromanie
    }
}
