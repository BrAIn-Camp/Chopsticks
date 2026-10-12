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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
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
    val terraTime = if (theme is HandTheme.TerraFirma) rememberTerraTime() else null
    val terraBuffer = remember { PixelBuffer() }
    val neonTime = if (theme is HandTheme.NeonRain) rememberNeonTime() else null
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize()) {
        when (theme) {
            is HandTheme.NeonRain -> {
                val time = neonTime?.value ?: 0f
                drawNeonCity(time, textMeasurer)
                // Targeting brackets lock onto whoever's turn it is
                val p2Label = if (gameMode == GameMode.PvE) "> AI_LINK // ACTIVE" else "> P2_LINK // ACTIVE"
                p1Area?.let { drawNeonTarget(it, p1Progress, time, textMeasurer, "> P1_LINK // ACTIVE") }
                p2Area?.let { drawNeonTarget(it, p2Progress, time, textMeasurer, p2Label) }
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
            is HandTheme.TerraFirma -> {
                // Day on Player 1's turn, night on Player 2's: the sky turns as the turn changes
                val day = p1Progress / (p1Progress + p2Progress).coerceAtLeast(0.001f)
                drawPixelated(terraBuffer, terraPixel()) {
                    drawTerraWorld(day = day, time = terraTime?.value ?: 0f, horizon = 0.5f)
                }
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
        if (theme is HandTheme.TerraFirma) {
            TerraTurnMessage(currentPlayerId, gameMode)
        }
        if (gameMode == GameMode.PvE && currentPlayerId == "Player 2") {
            ComputerThinkingStatus(p2Area, theme)
        }
    }
}

@Composable
private fun ComputerThinkingStatus(p2Area: Rect?, theme: HandTheme) {
    if (p2Area == null) return

    if (theme is HandTheme.TerraFirma) {
        TerraText(
            "The Computer is plotting…",
            TerraInk.BossPurple,
            fontSize = 13.sp,
            modifier = Modifier.graphicsLayer {
                translationX = p2Area.center.x - 110.dp.toPx()
                translationY = p2Area.bottom + 2.dp.toPx()
            }
        )
        return
    }

    if (theme is HandTheme.NeonRain) {
        // A terminal line with a blinking cursor, just below the computer's hands
        val cursor by rememberInfiniteTransition(label = "cursor").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
            label = "blink"
        )
        Text(
            "> AI JACKING IN" + if (cursor < 0.5f) "_" else " ",
            color = NeonInk.Cyan,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.graphicsLayer {
                translationX = p2Area.center.x - 80.dp.toPx()
                translationY = p2Area.bottom + 4.dp.toPx()
            }
        )
        return
    }

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
            .background(theme.secondaryColor, RoundedCornerShape(2.dp))
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

/**
 * Announces each turn the way the game announces events, fading in and out: nightfall brings
 * "The Computer has awoken!" in boss purple, daybreak hands the move back in event green.
 */
@Composable
private fun TerraTurnMessage(currentPlayerId: String, gameMode: GameMode) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(currentPlayerId) {
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(250))
        delay(1800)
        alpha.animateTo(0f, tween(700))
    }
    val (text, color) = when {
        currentPlayerId == "Player 2" && gameMode == GameMode.PvE -> "The Computer has awoken!" to TerraInk.BossPurple
        currentPlayerId == "Player 2" -> "Player 2 has awoken!" to TerraInk.BossPurple
        gameMode == GameMode.PvE -> "The sun rises. Your move." to TerraInk.EventGreen
        else -> "Player 1 has awoken!" to TerraInk.EventGreen
    }
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value }, contentAlignment = Alignment.Center) {
        TerraText(text, color, fontSize = 18.sp)
    }
}
