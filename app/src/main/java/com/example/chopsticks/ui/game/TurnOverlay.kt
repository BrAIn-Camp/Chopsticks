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
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.model.GameMode
import com.example.chopsticks.ui.theme.*
import kotlin.math.PI
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

    // Read the bounds on every composition: handBounds is snapshot state that fills in after the
    // first layout. (Wrapping this in remember(handBounds) cached the empty first result forever.)
    fun areaOf(leftKey: String, rightKey: String): Rect? {
        val l = handBounds[leftKey] ?: return null
        val r = handBounds[rightKey] ?: return null
        return Rect(
            left = minOf(l.left, r.left) - padding,
            top = minOf(l.top, r.top) - padding,
            right = maxOf(l.right, r.right) + padding,
            bottom = maxOf(l.bottom, r.bottom) + padding
        )
    }
    val p1Area = areaOf("P1_L", "P1_R")
    val p2Area = areaOf("P2_L", "P2_R")

    val flicker = if (theme is HandTheme.Ombromanie) rememberCandleFlicker() else null
    val sway = if (theme is HandTheme.PaperCutout) rememberPaperSway() else null

    Canvas(modifier = Modifier.fillMaxSize()) {
        when (theme) {
            is HandTheme.AnalogSynth -> {
                drawAnalogSynthTurn(p1Area, p1Progress, p2Area, p2Progress, theme)
            }
            is HandTheme.PaperCutout -> {
                // Spotlights swing onto the active player; the audience hops as the turn changes
                val t = p1Progress / (p1Progress + p2Progress).coerceAtLeast(0.001f)
                val spot = if (p1Area != null && p2Area != null) lerp(p2Area, p1Area, t) else p1Area ?: p2Area
                drawPaperStage(spot, sway?.value ?: 0f, hop = sin(p1Progress * PI.toFloat()))
            }
            is HandTheme.Ombromanie -> {
                // The lamp swings toward whoever's turn it is
                val p1Center = p1Area?.center ?: Offset(size.width / 2, size.height * 0.8f)
                val p2Center = p2Area?.center ?: Offset(size.width / 2, size.height * 0.2f)
                val lamp = p2Center + (p1Center - p2Center) * (p1Progress / (p1Progress + p2Progress).coerceAtLeast(0.001f))
                val f = flicker?.value ?: 0f
                drawLampLitScreen(lamp, f)
                drawSideCurtains(f)
            }
            is HandTheme.Marginalia -> {
                drawManuscriptPage()
                drawIvyBorder(10.dp.toPx(), 0f, size.height)
                drawIvyBorder(size.width - 10.dp.toPx(), 0f, size.height)
                // A manicule slides and turns to point at whoever's turn it is
                val gap = 8.dp.toPx()
                val p1Tip = Offset(size.width / 2, (p1Area?.top ?: size.height * 0.6f) - gap)
                val p2Tip = Offset(size.width / 2, (p2Area?.bottom ?: size.height * 0.4f) + gap)
                val t = p1Progress / (p1Progress + p2Progress).coerceAtLeast(0.001f)
                drawManicule(p2Tip + (p1Tip - p2Tip) * t, angleDegrees = -90f + 180f * t, scale = 1.1f)
            }
        }
    }
    
    // Active Stamp / Status Overlay
    Box(modifier = Modifier.fillMaxSize()) {
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

@Composable
private fun ComputerThinkingStatus(p2Area: Rect?, theme: HandTheme) {
    if (p2Area == null) return

    if (theme is HandTheme.PaperCutout) {
        // A speech bubble just below the computer's hands, tail pointing up at them
        PaperSpeechBubble(
            modifier = Modifier.graphicsLayer {
                translationX = p2Area.center.x - 90.dp.toPx()
                translationY = p2Area.bottom - 6.dp.toPx()
            },
            tailOnTop = true,
            showStar = false
        ) {
            Text("Hmm… let me think…", color = PaperInk.Outline, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        return
    }

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
        if (theme is HandTheme.Ombromanie || theme is HandTheme.Marginalia) {
            Text(
                text = if (theme is HandTheme.Marginalia) "the scribe deliberates…" else "the shadows deliberate…",
                color = theme.textColor,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif
            )
        } else {
            Text(
                text = "SYSTEM: THINKING...",
                color = theme.textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
