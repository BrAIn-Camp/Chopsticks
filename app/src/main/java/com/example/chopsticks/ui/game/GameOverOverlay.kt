package com.example.chopsticks.ui.game

import com.example.chopsticks.ui.ArtisticActionButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.DrawReason
import com.example.chopsticks.domain.model.GameMode
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.ui.theme.*
import kotlin.random.Random

/** One line describing how the game ended, e.g. "WINNER: Player 1" or "DRAW: SAME POSITION 3 TIMES". */
fun resultText(state: GameState): String = when (state.drawReason) {
    DrawReason.REPETITION -> "DRAW: SAME POSITION ${RulesEngine.REPETITION_LIMIT} TIMES"
    DrawReason.NO_LEGAL_MOVES -> "DRAW: NO LEGAL MOVES"
    null -> "WINNER: ${state.winnerId}"
}

@Composable
fun GameOverOverlay(
    state: GameState,
    theme: HandTheme,
    gameMode: GameMode,
    onRestart: () -> Unit
) {
    val winner = state.winnerId
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true, onClick = {}) // absorb clicks
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (theme is HandTheme.AnalogSynth) {
            // Digital glitch overlay
            val infiniteTransition = rememberInfiniteTransition(label = "glitch")
            val glitchOffset by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glitch_offset"
            )
            val message = when (winner) {
                null -> "SIGNAL DEADLOCK"
                "Player 1" -> "VICTORY ACHIEVED"
                else -> "SYSTEM SHUTDOWN"
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    Text(
                        text = message,
                        color = Color.Cyan.copy(alpha = 0.7f),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(x = glitchOffset.dp)
                    )
                    Text(
                        text = message,
                        color = Color.Magenta.copy(alpha = 0.7f),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(x = (-glitchOffset).dp, y = (glitchOffset / 2).dp)
                    )
                    Text(
                        text = message,
                        color = theme.primaryColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = resultText(state),
                    color = theme.textColor.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(48.dp))

                ArtisticActionButton(
                    text = "TRY AGAIN",
                    onClick = onRestart,
                    theme = theme,
                    modifier = Modifier.width(200.dp)
                )
            }
        } else if (theme is HandTheme.Ombromanie) {
            ShadowIntertitle(
                result = resultText(state),
                subtitle = when {
                    winner == null -> "The shadows were evenly matched."
                    gameMode == GameMode.PvE && winner == "Player 1" -> "The lamp is lowered on your triumph."
                    gameMode == GameMode.PvE -> "The computer's shadow takes the bow."
                    else -> "The lamp is lowered. The curtain falls."
                }
            ) {
                ArtisticActionButton(text = "Encore", onClick = onRestart, theme = theme)
            }
        } else if (theme is HandTheme.Marginalia) {
            ManuscriptColophon(
                result = resultText(state),
                subtitle = when {
                    winner == null -> "Neither hand prevailed."
                    gameMode == GameMode.PvE && winner == "Player 1" -> "Here endeth the game, in thy favour."
                    gameMode == GameMode.PvE -> "Here endeth the game. The machine prevaileth."
                    else -> "Here endeth the game."
                }
            ) {
                ArtisticActionButton(text = "Begin anew", onClick = onRestart, theme = theme)
            }
        } else {
            // Paper Cutout
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .drawBehind {
                        val rect = Rect(Offset.Zero, size)
                        val path = Path()
                        val steps = 20
                        val jitter = 5f
                        val r = java.util.Random(42L)
                        
                        path.moveTo(rect.left + r.nextFloat() * jitter, rect.top + r.nextFloat() * jitter)
                        for (i in 1..steps) {
                            val x = rect.left + (rect.width / steps) * i
                            val y = rect.top + (if (i == steps) 0f else (r.nextFloat() - 0.5f) * jitter)
                            path.lineTo(x, y)
                        }
                        for (i in 1..8) {
                            val x = rect.right + (r.nextFloat() - 0.5f) * jitter
                            val y = rect.top + (rect.height / 8) * i
                            path.lineTo(x, y)
                        }
                        for (i in 1..steps) {
                            val x = rect.right - (rect.width / steps) * i
                            val y = rect.bottom + (if (i == steps) 0f else (r.nextFloat() - 0.5f) * jitter)
                            path.lineTo(x, y)
                        }
                        for (i in 1..8) {
                            val x = rect.left + (r.nextFloat() - 0.5f) * jitter
                            val y = rect.bottom - (rect.height / 8) * i
                            path.lineTo(x, y)
                        }
                        path.close()
                        
                        drawPath(
                            path = path,
                            color = Color(0xFFF4EBE1)
                        )
                        drawPath(
                            path = path,
                            color = theme.primaryColor.copy(alpha = 0.4f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        
                        val stapleW = 12.dp.toPx()
                        val stapleH = 4.dp.toPx()
                        val stapleColor = Color(0xFF5A6065)
                        
                        drawRect(color = stapleColor, topLeft = Offset(12.dp.toPx(), 12.dp.toPx()), size = Size(stapleW, stapleH))
                        drawRect(color = stapleColor, topLeft = Offset(rect.right - 12.dp.toPx() - stapleW, 12.dp.toPx()), size = Size(stapleW, stapleH))
                        drawRect(color = stapleColor, topLeft = Offset(12.dp.toPx(), rect.bottom - 12.dp.toPx() - stapleH), size = Size(stapleW, stapleH))
                        drawRect(color = stapleColor, topLeft = Offset(rect.right - 12.dp.toPx() - stapleW, rect.bottom - 12.dp.toPx() - stapleH), size = Size(stapleW, stapleH))
                    }
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "GAME OVER",
                        color = Color(0xFF2E1C0C),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = resultText(state),
                        color = theme.textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    ArtisticActionButton(
                        text = "TRY AGAIN",
                        onClick = onRestart,
                        theme = theme,
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }
    }
}
