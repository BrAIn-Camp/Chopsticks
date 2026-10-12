package com.example.chopsticks.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.model.GameMode
import com.example.chopsticks.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ThemeTurnOverlay(
    currentPlayerId: String,
    theme: HandTheme,
    handBounds: Map<String, Rect>,
    gameMode: GameMode
) {
    val transition = updateTransition(targetState = currentPlayerId, label = "TurnTransition")
    val density = LocalDensity.current
    val padding = with(density) { 16.dp.toPx() }
    
    val p1Progress by transition.animateFloat(
        transitionSpec = { tween(800, easing = FastOutSlowInEasing) },
        label = "P1Progress"
    ) { state -> if (state == "Player 1") 1f else 0f }

    val p2Progress by transition.animateFloat(
        transitionSpec = { tween(800, easing = FastOutSlowInEasing) },
        label = "P2Progress"
    ) { state -> if (state == "Player 2") 1f else 0f }

    val p1Area = remember(handBounds, padding) {
        val l = handBounds["P1_L"]
        val r = handBounds["P1_R"]
        if (l != null && r != null) {
            Rect(
                left = minOf(l.left, r.left) - padding,
                top = minOf(l.top, r.top) - padding,
                right = maxOf(l.right, r.right) + padding,
                bottom = maxOf(l.bottom, r.bottom) + padding
            )
        } else null
    }

    val p2Area = remember(handBounds, padding) {
        val l = handBounds["P2_L"]
        val r = handBounds["P2_R"]
        if (l != null && r != null) {
            Rect(
                left = minOf(l.left, r.left) - padding,
                top = minOf(l.top, r.top) - padding,
                right = maxOf(l.right, r.right) + padding,
                bottom = maxOf(l.bottom, r.bottom) + padding
            )
        } else null
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        when (theme) {
            is HandTheme.AnalogSynth -> {
                drawAnalogSynthTurn(p1Area, p1Progress, p2Area, p2Progress, theme)
            }
            is HandTheme.PaperCutout -> {
                drawPaperCutoutTurn(p1Area, p1Progress, p2Area, p2Progress, theme)
            }
        }
    }
    
    // Active Stamp / Status Overlay
    Box(modifier = Modifier.fillMaxSize()) {
        if (theme is HandTheme.PaperCutout) {
            PaperActiveStamp(p1Area, p1Progress, p2Area, p2Progress, theme)
        }
        
        if (gameMode == GameMode.PvE && currentPlayerId == "Player 2") {
            ComputerThinkingStatus(p2Area, theme)
        }
    }
}

private fun DrawScope.drawAnalogSynthTurn(
    p1Area: Rect?,
    p1Progress: Float,
    p2Area: Rect?,
    p2Progress: Float,
    theme: HandTheme
) {
    val glowColor = theme.primaryColor
    val cableColor = theme.fingerColor
    
    // P1 Frame
    p1Area?.let { rect ->
        drawPowerFrame(rect, p1Progress, glowColor)
        drawEnergyCable(rect, p1Progress, cableColor, isTop = false)
    }
    
    // P2 Frame
    p2Area?.let { rect ->
        drawPowerFrame(rect, p2Progress, glowColor)
        drawEnergyCable(rect, p2Progress, cableColor, isTop = true)
    }
}

private fun DrawScope.drawPowerFrame(rect: Rect, progress: Float, color: Color) {
    if (progress <= 0f) return
    val alpha = progress
    val stroke = 2.dp.toPx()
    val cornerLen = 24.dp.toPx()
    
    // Main frame lines (pulsing)
    drawRect(
        color = color.copy(alpha = 0.2f * alpha),
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(width = stroke)
    )
    
    // Sharp Corners
    val corners = listOf(
        rect.topLeft to Offset(1f, 1f),
        rect.topRight to Offset(-1f, 1f),
        rect.bottomLeft to Offset(1f, -1f),
        rect.bottomRight to Offset(-1f, -1f)
    )
    
    corners.forEach { (pos, dir) ->
        drawLine(
            color = color.copy(alpha = alpha),
            start = pos,
            end = pos + Offset(dir.x * cornerLen, 0f),
            strokeWidth = stroke * 2
        )
        drawLine(
            color = color.copy(alpha = alpha),
            start = pos,
            end = pos + Offset(0f, dir.y * cornerLen),
            strokeWidth = stroke * 2
        )
    }
}

private fun DrawScope.drawEnergyCable(rect: Rect, progress: Float, color: Color, isTop: Boolean) {
    if (progress <= 0.1f) return
    
    val startX = if (isTop) size.width else 0f
    val startY = if (isTop) rect.top else rect.bottom
    val start = Offset(startX, startY)
    val end = rect.center
    
    val ctrl1 = Offset(startX + (if (isTop) -100.dp.toPx() else 100.dp.toPx()), startY)
    val ctrl2 = Offset(end.x, startY)
    
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, end.x, end.y)
    }
    
    // Draw thick neon cable
    drawPath(
        path = path,
        color = color.copy(alpha = 0.3f * progress),
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
    )
    
    // Pulsing core
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(path, false)
    val length = pathMeasure.length
    val partialPath = Path()
    pathMeasure.getSegment(0f, length * progress, partialPath, true)
    
    drawPath(
        path = partialPath,
        color = color.copy(alpha = progress),
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
    )
    
    // Energy pulses
    val time = System.currentTimeMillis() / 500f
    val pulsePos = (time % 1f) * length * progress
    val pos = pathMeasure.getPosition(pulsePos)
    drawCircle(Color.White, radius = 4.dp.toPx(), center = pos)
}

private fun DrawScope.drawPaperCutoutTurn(
    p1Area: Rect?,
    p1Progress: Float,
    p2Area: Rect?,
    p2Progress: Float,
    theme: HandTheme
) {
    // Tracing Paper (Dimming layer)
    if (p1Progress < 0.9f) {
        p1Area?.let { rect ->
            drawRect(
                color = theme.backgroundColor.copy(alpha = 0.5f * (1f - p1Progress)),
                topLeft = rect.topLeft,
                size = rect.size
            )
            // Texture/Grain could be added here
        }
    }
    
    if (p2Progress < 0.9f) {
        p2Area?.let { rect ->
            drawRect(
                color = theme.backgroundColor.copy(alpha = 0.5f * (1f - p2Progress)),
                topLeft = rect.topLeft,
                size = rect.size
            )
        }
    }

    // Spotlight (Paper shape behind)
    p1Area?.let { rect -> drawPaperSpotlight(rect, p1Progress, theme.secondaryColor) }
    p2Area?.let { rect -> drawPaperSpotlight(rect, p2Progress, theme.secondaryColor) }
}

private fun DrawScope.drawPaperSpotlight(rect: Rect, progress: Float, color: Color) {
    if (progress <= 0f) return
    
    val center = rect.center
    val w = rect.width * (0.8f + 0.4f * progress)
    val h = rect.height * (0.8f + 0.4f * progress)
    
    val path = Path()
    val steps = 12
    val r = java.util.Random(rect.hashCode().toLong())
    
    for (i in 0 until steps) {
        val angle = (i.toFloat() / steps) * 2 * Math.PI
        val dist = (0.9f + r.nextFloat() * 0.2f) * (if (i % 2 == 0) w else h) / 2
        val x = center.x + Math.cos(angle).toFloat() * dist
        val y = center.y + Math.sin(angle).toFloat() * dist
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    
    drawPath(path, color.copy(alpha = 0.4f * progress))
}

@Composable
private fun PaperActiveStamp(
    p1Area: Rect?,
    p1Progress: Float,
    p2Area: Rect?,
    p2Progress: Float,
    theme: HandTheme
) {
    if (p1Progress > 0.5f) {
        p1Area?.let { rect ->
            ActiveStamp(
                modifier = Modifier.graphicsLayer {
                    translationX = rect.right - 20.dp.toPx()
                    translationY = rect.top + 20.dp.toPx()
                    alpha = (p1Progress - 0.5f) * 2f
                    rotationZ = 10f
                },
                theme = theme
            )
        }
    }
    
    if (p2Progress > 0.5f) {
        p2Area?.let { rect ->
            ActiveStamp(
                modifier = Modifier.graphicsLayer {
                    translationX = rect.left + 20.dp.toPx()
                    translationY = rect.bottom - 60.dp.toPx()
                    alpha = (p2Progress - 0.5f) * 2f
                    rotationZ = -165f
                },
                theme = theme
            )
        }
    }
}

@Composable
private fun ActiveStamp(modifier: Modifier, theme: HandTheme) {
    val time = System.currentTimeMillis() / 200
    val jitterX = remember(time) { ((Math.random() - 0.5) * 4).toFloat() }
    val jitterY = remember(time) { ((Math.random() - 0.5) * 4).toFloat() }
    
    Box(
        modifier = modifier
            .offset(x = jitterX.dp, y = jitterY.dp)
            .background(theme.primaryColor, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("ACTIVE", color = theme.backgroundColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
private fun ComputerThinkingStatus(p2Area: Rect?, theme: HandTheme) {
    if (p2Area == null) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = p2Area.center.x - 60.dp.toPx()
                translationY = p2Area.top - 40.dp.toPx()
                this.alpha = alpha
            }
            .let {
                if (theme is HandTheme.AnalogSynth) {
                    it.border(1.dp, theme.primaryColor, RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                } else {
                    it.background(theme.secondaryColor, RoundedCornerShape(2.dp))
                }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "SYSTEM: THINKING...",
            color = theme.textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
