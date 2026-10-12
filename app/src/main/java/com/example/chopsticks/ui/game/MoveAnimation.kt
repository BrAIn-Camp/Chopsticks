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
import com.example.chopsticks.ui.theme.*
import kotlin.random.Random

@Composable
fun ActionDisplayOverlay(

    lastMove: Move?,
    lastMoveTimestamp: Long,
    handBounds: Map<String, Rect>,
    theme: HandTheme
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
        is HandTheme.AnalogSynth -> {
            val color = theme.primaryColor
            val controlPoint = start + dir * 0.5f + ortho * 80.dp.toPx()
            
            val fullPath = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(controlPoint.x, controlPoint.y, end.x, end.y)
            }
            drawPath(fullPath, color.copy(alpha = 0.35f), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
            
            val partialPath = Path().apply {
                moveTo(start.x, start.y)
                val steps = 30
                for (i in 1..(steps * progress).toInt()) {
                    val t = i.toFloat() / steps
                    val p = start * ((1 - t) * (1 - t)) + controlPoint * (2 * (1 - t) * t) + end * (t * t)
                    lineTo(p.x, p.y)
                }
            }
            drawPath(partialPath, color, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
            
            val pulseT = progress
            val pulsePos = start * ((1 - pulseT) * (1 - pulseT)) + controlPoint * (2 * (1 - pulseT) * pulseT) + end * (pulseT * pulseT)
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = pulsePos)
            drawCircle(color = theme.secondaryColor, radius = 18.dp.toPx(), center = pulsePos, style = Stroke(4.dp.toPx()))
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 24.dp.toPx() * progress, center = pulsePos)
        }
        is HandTheme.PaperCutout -> drawPaperPlaneMove(start, end, progress)
        is HandTheme.Ombromanie -> drawShadowMovePath(start, end, progress)
        is HandTheme.Marginalia -> drawPenFlourish(start, end, progress)
    }
}
