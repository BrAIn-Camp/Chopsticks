package com.example.chopsticks.ui.game

import com.example.chopsticks.ui.ArtisticActionButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.ai.HintCategory
import com.example.chopsticks.domain.ai.HintResult
import com.example.chopsticks.domain.ai.Move
import com.example.chopsticks.ui.theme.*
import kotlin.random.Random

@Composable
fun HintMoveOverlay(
    move: Move,
    handBounds: Map<String, Rect>,
    theme: HandTheme
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hint_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    val moveMakerPrefix = if (move.playerId == "Player 1") "P1" else "P2"
    val opponentPrefix = if (move.playerId == "Player 1") "P2" else "P1"

    val (startKey, endKey) = when (move) {
        is Move.Attack -> {
            val from = if (move.fromHandIndex == 0) "${moveMakerPrefix}_L" else "${moveMakerPrefix}_R"
            val to = if (move.toHandIndex == 0) "${opponentPrefix}_L" else "${opponentPrefix}_R"
            from to to
        }
        is Move.Transfer -> {
            val from = if (move.fromHandIndex == 0) "${moveMakerPrefix}_L" else "${moveMakerPrefix}_R"
            val to = if (move.toHandIndex == 0) "${moveMakerPrefix}_L" else "${moveMakerPrefix}_R"
            from to to
        }
        is Move.ExchangeLife -> {
            val from = if (move.sacrificedHandIndex == 0) "${moveMakerPrefix}_L" else "${moveMakerPrefix}_R"
            val to = if (move.sacrificedHandIndex == 0) "${moveMakerPrefix}_R" else "${moveMakerPrefix}_L"
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
            val color = if (theme is HandTheme.AnalogSynth) Color.Cyan else theme.primaryColor
            
            drawMovePath(theme, start, end, 1f) // Draw full path
            
            // Draw hint indicator
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = 12.dp.toPx(),
                center = end,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}

@Composable
fun HintDialog(
    hint: HintResult,
    theme: HandTheme,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f)) // More translucent container
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        if (theme is HandTheme.AnalogSynth) {
            AnalogSynthHintHUD(hint, theme, onDismiss)
        } else {
            PaperCutoutHintNote(hint, theme, onDismiss)
        }
    }
}

@Composable
fun AnalogSynthHintHUD(
    hint: HintResult,
    theme: HandTheme,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.7f) // Significantly smaller
            .border(2.dp, Color.Cyan.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.7f)) // More translucent
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
            Text(
                text = ">>> TACTICAL ANALYSIS <<<",
                color = Color.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val categoryColor = when (hint.category) {
                    HintCategory.VICTORY -> Color.Green
                    HintCategory.OFFENSIVE -> Color.Red
                    HintCategory.DEFENSIVE -> Color.Yellow
                    HintCategory.SETUP -> Color.Cyan
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(categoryColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = hint.category.name,
                    color = categoryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                
                if (hint.mateInSteps != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "MATE IN ${hint.mateInSteps}",
                        color = Color.Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, Color.Green, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = hint.explanation,
                color = theme.textColor,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 22.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ArtisticActionButton(
            text = "DISMISS",
            onClick = onDismiss,
            theme = theme,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun PaperCutoutHintNote(
    hint: HintResult,
    theme: HandTheme,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.7f) // Significantly smaller
            .drawBehind {
                val rect = Rect(Offset.Zero, size)
                val path = createOrganicRectPath(rect)
                drawPath(path, Color(0xFFFDF5E6).copy(alpha = 0.75f)) // Tracing paper effect
                drawPath(path, Color(0xFF8B4513).copy(alpha = 0.2f), style = Stroke(1.5.dp.toPx()))
                
                // Torn edge effect or scribbles
                val r = java.util.Random(123)
                repeat(3) {
                    val x = r.nextFloat() * size.width
                    val y = r.nextFloat() * size.height
                    drawCircle(Color(0xFF8B4513).copy(alpha = 0.03f), radius = 15.dp.toPx(), center = Offset(x, y))
                }
            }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
            Text(
                text = "STRATEGIC NOTE",
                color = Color(0xFF2E1C0C),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (hint.mateInSteps != null) {
                Text(
                    text = "* Win guaranteed in ${hint.mateInSteps} moves",
                    color = Color(0xFFB22222),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = hint.explanation,
                color = Color(0xFF4A3728),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ArtisticActionButton(
                text = "GOT IT",
                onClick = onDismiss,
                theme = theme,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
