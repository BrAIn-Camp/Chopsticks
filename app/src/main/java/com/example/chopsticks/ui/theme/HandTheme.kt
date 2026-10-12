package com.example.chopsticks.ui.theme

import androidx.compose.ui.graphics.Color
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
    val isDark: Boolean = true
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

    object PaperCutout : HandTheme(
        name = "Paper Cutout",
        backgroundColor = Color(0xFFE8E0D5),
        primaryColor = Color(0xFF8B4513),
        secondaryColor = Color(0xFFD2B48C),
        handColor = Color(0xFF5D4037),
        fingerColor = Color(0xFF795548),
        textColor = Color(0xFF3E2723),
        cornerRadius = 4.dp,
        organicEdges = true,
        isDark = false
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
        isDark = false
    )

    companion object {
        /**
         * Returns all available themes. We use a getter to ensure that the objects
         * (AnalogSynth, PaperCutout) are fully initialized before being collected into a list.
         */
        val allThemes: List<HandTheme>
            get() = listOfNotNull(
                AnalogSynth,
                PaperCutout,
                Ombromanie
            )
    }
}
