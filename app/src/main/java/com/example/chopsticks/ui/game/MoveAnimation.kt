package com.example.chopsticks.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chopsticks.domain.ai.Move
import com.example.chopsticks.domain.model.GameState
import androidx.compose.ui.unit.sp
import com.example.chopsticks.ui.theme.*
import kotlin.random.Random

/**
 * How many fingers the last move sends, e.g. "+2", for themes that pop up damage numbers.
 * During the move animation [state] is still the position before the move; an attack never
 * changes the attacking hand, so its count is right either way.
 */
fun moveAmountLabel(state: GameState): String? = when (val move = state.lastMove) {
    is Move.Attack -> {
        val attacker = if (state.player1.id == move.playerId) state.player1 else state.player2
        val fingers = if (move.fromHandIndex == 0) attacker.leftHand.fingers else attacker.rightHand.fingers
        "+$fingers"
    }
    is Move.Transfer -> "+${move.amount}"
    else -> null
}

@Composable
fun ActionDisplayOverlay(
    lastMove: Move?,
    lastMoveTimestamp: Long,
    handBounds: Map<String, Rect>,
    theme: HandTheme,
    label: String? = null
) {
    if (lastMove == null) return

    val animProgress = remember(lastMoveTimestamp) { Animatable(0f) }

    LaunchedEffect(lastMoveTimestamp) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = LinearOutSlowInEasing)
        )
    }

    if (animProgress.value >= 1f) return

    val moveMakerPrefix = if (lastMove.playerId == "Player 1") "P1" else "P2"
    val opponentPrefix = if (lastMove.playerId == "Player 1") "P2" else "P1"

    val (startKey, endKey) = when (lastMove) {
        is Move.Attack -> {
            val from = if (lastMove.fromHandIndex == 0) "${moveMakerPrefix}_L" else "${moveMakerPrefix}_R"
            val to = if (lastMove.toHandIndex == 0) "${opponentPrefix}_L" else "${opponentPrefix}_R"
            from to to
        }
        is Move.Transfer -> {
            val from = if (lastMove.fromHandIndex == 0) "${moveMakerPrefix}_L" else "${moveMakerPrefix}_R"
            val to = if (lastMove.toHandIndex == 0) "${moveMakerPrefix}_L" else "${moveMakerPrefix}_R"
            from to to
        }
        is Move.ExchangeLife -> {
            val from = if (lastMove.sacrificedHandIndex == 0) "${moveMakerPrefix}_L" else "${moveMakerPrefix}_R"
            val to = if (lastMove.sacrificedHandIndex == 0) "${moveMakerPrefix}_R" else "${moveMakerPrefix}_L"
            from to to
        }
        is Move.Condense -> {
            "${moveMakerPrefix}_L" to "${moveMakerPrefix}_R"
        }
    }

    val startRect = handBounds[startKey]
    val endRect = handBounds[endKey]
    
    if (startRect != null && endRect != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = startRect.center
            val end = endRect.center
            drawMovePath(theme, start, end, animProgress.value)
        }
        // Terra Firma: an orange damage number pops out of the target and floats up
        if (theme is HandTheme.TerraFirma && label != null && animProgress.value > 0.55f) {
            val k = (animProgress.value - 0.55f) / 0.45f
            TerraText(
                label,
                TerraInk.Damage,
                fontSize = 26.sp,
                modifier = Modifier.graphicsLayer {
                    translationX = endRect.center.x - 20.dp.toPx()
                    translationY = endRect.center.y - 40.dp.toPx() - k * 36.dp.toPx()
                    val pop = 1f + 0.35f * (1f - k)
                    scaleX = pop
                    scaleY = pop
                }
            )
        }
    }
}

fun DrawScope.drawMovePath(
    theme: HandTheme,
    start: Offset,
    end: Offset,
    progress: Float
) {
    val dir = end - start
    val len = dir.getDistance()
    if (len <= 0f) return
    val normDir = dir / len
    val ortho = Offset(-normDir.y, normDir.x)
    val currentEnd = start + dir * progress
    
    when (theme) {
        is HandTheme.NeonRain -> drawNetrunTrace(start, end, progress)
        is HandTheme.PaperCutout -> drawPaperPlaneMove(start, end, progress)
        is HandTheme.Ombromanie -> drawShadowMovePath(start, end, progress)
        is HandTheme.Marginalia -> drawPenFlourish(start, end, progress)
        is HandTheme.TerraFirma -> drawFallingStarMove(start, end, progress)
    }
}
