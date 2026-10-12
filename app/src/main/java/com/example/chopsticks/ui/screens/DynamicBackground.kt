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
import com.example.chopsticks.ui.theme.ShadowTheatreBackground
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
            is HandTheme.PaperCutout -> PaperCutoutBackground(theme = theme)
            is HandTheme.Ombromanie -> ShadowTheatreBackground()
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

@Composable
fun PaperCutoutBackground(theme: HandTheme) {
    var jitterTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            jitterTick++
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "PaperCutoutTransition")
    val timeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PaperTime"
    )
    
    val quantizedProgress = remember(jitterTick) { timeProgress }
    
    val scraps = remember {
        List(15) { i ->
            val numPoints = (4..6).random()
            val radius = randomFloat(15f, 35f)
            val pointOffsets = (0 until numPoints).map { p ->
                val angle = (p * 2 * Math.PI / numPoints) + randomFloat(0.0f, 0.3f)
                val r = radius * randomFloat(0.7f, 1.3f)
                Offset(cos(angle).toFloat() * r, sin(angle).toFloat() * r)
            }
            
            val scrapColor = when (i % 4) {
                0 -> theme.primaryColor.copy(alpha = 0.12f)
                1 -> theme.secondaryColor.copy(alpha = 0.20f)
                2 -> theme.handColor.copy(alpha = 0.12f)
                else -> theme.fingerColor.copy(alpha = 0.12f)
            }
            
            PaperScrapData(
                xRel = Random.nextFloat(),
                yRel = Random.nextFloat(),
                color = scrapColor,
                speedY = randomFloat(0.04f, 0.12f),
                speedX = randomFloat(-0.04f, 0.04f),
                baseRotation = randomFloat(0f, 360f),
                rotSpeed = randomFloat(-35f, 35f),
                pointOffsets = pointOffsets
            )
        }
    }
    
    val splats = remember {
        List(4) { i ->
            InkSplatData(
                xRel = randomFloat(0.1f, 0.9f),
                yRel = randomFloat(0.1f, 0.9f),
                maxRadiusDp = randomFloat(25f, 45f),
                phaseOffset = i * 0.25f
            )
        }
    }
    
    val jitterOffsets = remember {
        listOf(
            Offset(-1.5f, 1f),
            Offset(1.5f, -1f),
            Offset(-1f, -1.5f),
            Offset(1f, 1.5f),
            Offset(-0.5f, 0.5f),
            Offset(0.5f, -0.5f)
        )
    }
    val currentGlobalJitter = jitterOffsets[jitterTick % jitterOffsets.size]
    val currentJitterRotation = remember(jitterTick) { ((jitterTick * 7) % 5 - 2).toFloat() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val paths = scraps.map { s ->
                    val path = Path()
                    if (s.pointOffsets.isNotEmpty()) {
                        path.moveTo(s.pointOffsets[0].x, s.pointOffsets[0].y)
                        for (j in 1 until s.pointOffsets.size) {
                            path.lineTo(s.pointOffsets[j].x, s.pointOffsets[j].y)
                        }
                        path.close()
                    }
                    path
                }
                
                onDrawBehind {
                    val width = size.width
                    val height = size.height
                    
                    // Textured Base
                    drawRect(color = theme.backgroundColor)
                    
                    // Subtle paper speckles texture
                    val speckColor = theme.textColor.copy(alpha = 0.025f)
                    var speckX = 10f
                    while (speckX < width) {
                        var speckY = 15f
                        while (speckY < height) {
                            val sx = speckX + (speckX.toInt() % 5) + currentGlobalJitter.x
                            val sy = speckY + (speckY.toInt() % 5) + currentGlobalJitter.y
                            drawCircle(color = speckColor, radius = 1.2.dp.toPx(), center = Offset(sx, sy))
                            speckY += 50.dp.toPx()
                        }
                        speckX += 50.dp.toPx()
                    }
                    
                    // Drifting Scraps
                    scraps.forEachIndexed { index, s ->
                        val curXRel = (s.xRel + quantizedProgress * s.speedX + 1f) % 1f
                        val curYRel = (s.yRel + quantizedProgress * s.speedY + 1f) % 1f
                        
                        val px = curXRel * width + currentGlobalJitter.x
                        val py = curYRel * height + currentGlobalJitter.y
                        val rot = s.baseRotation + quantizedProgress * s.rotSpeed + currentJitterRotation
                        
                        val path = paths[index]
                        
                        withTransform({
                            translate(px, py)
                            rotate(rot)
                        }) {
                            drawPath(path = path, color = s.color)
                        }
                    }
                    
                    // Ink Splats
                    splats.forEach { splat ->
                        val splatProg = (quantizedProgress + splat.phaseOffset) % 1.0f
                        
                        if (splatProg < 0.8f) {
                            val expansion = splatProg / 0.8f
                            val radius = splat.maxRadiusDp.dp.toPx() * expansion
                            val alpha = (1f - expansion) * 0.10f
                            
                            val sx = splat.xRel * width + currentGlobalJitter.x
                            val sy = splat.yRel * height + currentGlobalJitter.y
                            
                            val splatColor = theme.textColor.copy(alpha = alpha)
                            
                            drawCircle(color = splatColor, radius = radius, center = Offset(sx, sy))
                            
                            val satDistance = radius * 1.25f
                            drawCircle(color = splatColor, radius = radius * 0.18f, center = Offset(sx + satDistance, sy + satDistance * 0.25f))
                            drawCircle(color = splatColor, radius = radius * 0.12f, center = Offset(sx - satDistance * 0.45f, sy - satDistance * 0.75f))
                        }
                    }
                }
            }
    )
}

data class SynthParticle(val xRel: Float, val yRel: Float, val sizeDp: Float, val speed: Float)

data class PaperScrapData(
    val xRel: Float,
    val yRel: Float,
    val color: Color,
    val speedY: Float,
    val speedX: Float,
    val baseRotation: Float,
    val rotSpeed: Float,
    val pointOffsets: List<Offset>
)

data class InkSplatData(
    val xRel: Float,
    val yRel: Float,
    val maxRadiusDp: Float,
    val phaseOffset: Float
)
