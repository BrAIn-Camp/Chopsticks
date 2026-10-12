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
import com.example.chopsticks.ui.theme.PaperStageBackground
import com.example.chopsticks.ui.theme.ShadowTheatreBackground
import com.example.chopsticks.ui.theme.TerraMenuBackground
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private fun randomFloat(min: Float, max: Float): Float = (Random.nextFloat() * (max - min)) + min

@Composable
fun DynamicBackground(theme: HandTheme, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        when (theme) {
            is HandTheme.AnalogSynth -> AnalogSynthBackground(theme = theme)
            is HandTheme.PaperCutout -> PaperStageBackground()
            is HandTheme.Ombromanie -> ShadowTheatreBackground()
            is HandTheme.Marginalia -> ManuscriptBackground()
            is HandTheme.TerraFirma -> TerraMenuBackground()
        }
    }
}

@Composable
fun AnalogSynthBackground(theme: HandTheme) {
    val infiniteTransition = rememberInfiniteTransition(label = "AnalogSynthTransition")
    
    val gridAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GridAlpha"
    )
    
    val scanlineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanlineProgress"
    )
    
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ParticleProgress"
    )
    
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(60, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlickerAlpha"
    )
    
    val particles = remember {
        List(30) {
            SynthParticle(
                xRel = Random.nextFloat(),
                yRel = Random.nextFloat(),
                sizeDp = randomFloat(3f, 8f),
                speed = randomFloat(0.08f, 0.25f)
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    val width = size.width
                    val height = size.height
                    val baseAlpha = flickerAlpha
                    
                    // Background base
                    drawRect(color = theme.backgroundColor)
                    
                    // 1. Grid
                    val gridSize = 45.dp.toPx()
                    val gridColor = theme.primaryColor
                    
                    var x = 0f
                    while (x < width) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1.dp.toPx(),
                            alpha = gridAlpha * baseAlpha
                        )
                        x += gridSize
                    }
                    
                    var y = 0f
                    while (y < height) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            alpha = gridAlpha * baseAlpha
                        )
                        y += gridSize
                    }
                    
                    // 2. Digital Particles (drift upward)
                    particles.forEach { p ->
                        val currentYRel = (p.yRel - particleProgress * p.speed + 1f) % 1f
                        val px = p.xRel * width
                        val py = currentYRel * height
                        
                        val alphaFactor = currentYRel.coerceIn(0f, 1f)
                        val sizePx = p.sizeDp.dp.toPx()
                        
                        drawRect(
                            color = Color(0xFF00FFCC),
                            topLeft = Offset(px, py),
                            size = Size(sizePx, sizePx),
                            alpha = alphaFactor * 0.4f * baseAlpha
                        )
                    }
                    
                    // 3. Scanning Lines
                    val scanY1 = scanlineProgress * height
                    val scanY2 = ((scanlineProgress + 0.5f) * height) % height
                    
                    drawLine(
                        color = Color(0xFF00FFCC),
                        start = Offset(0f, scanY1),
                        end = Offset(width, scanY1),
                        strokeWidth = 2.dp.toPx(),
                        alpha = 0.18f * baseAlpha
                    )
                    
                    drawLine(
                        color = Color(0xFF00FF00),
                        start = Offset(0f, scanY2),
                        end = Offset(width, scanY2),
                        strokeWidth = 1.5.dp.toPx(),
                        alpha = 0.12f * baseAlpha
                    )
                }
            }
    )
}

data class SynthParticle(val xRel: Float, val yRel: Float, val sizeDp: Float, val speed: Float)
