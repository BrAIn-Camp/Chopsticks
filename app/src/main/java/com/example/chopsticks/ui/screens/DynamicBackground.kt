package com.example.chopsticks.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.chopsticks.ui.theme.HandTheme
import com.example.chopsticks.ui.theme.ManuscriptBackground
import com.example.chopsticks.ui.theme.NeonCityBackground
import com.example.chopsticks.ui.theme.PaperStageBackground
import com.example.chopsticks.ui.theme.ShadowTheatreBackground
import com.example.chopsticks.ui.theme.TerraMenuBackground
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


@Composable
fun DynamicBackground(theme: HandTheme, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        when (theme) {
            is HandTheme.NeonRain -> NeonCityBackground()
            is HandTheme.PaperCutout -> PaperStageBackground()
            is HandTheme.Ombromanie -> ShadowTheatreBackground()
            is HandTheme.Marginalia -> ManuscriptBackground()
            is HandTheme.TerraFirma -> TerraMenuBackground()
        }
    }
}
