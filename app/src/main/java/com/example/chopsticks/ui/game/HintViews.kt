package com.example.chopsticks.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.ai.HintAnalysis
import com.example.chopsticks.domain.ai.HintAnalyzer
import com.example.chopsticks.domain.ai.Move
import com.example.chopsticks.domain.ai.MoveAnalysis
import com.example.chopsticks.domain.ai.Outcome
import com.example.chopsticks.ui.ArtisticActionButton
import com.example.chopsticks.ui.theme.*

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

            drawMovePath(theme, start, end, 1f) // Draw full path

            drawCircle(
                color = theme.primaryColor.copy(alpha = alpha),
                radius = 12.dp.toPx(),
                center = end,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}

/** Colours for results, with text labels alongside so colour is never the only signal. */
private fun outcomeColor(outcome: Outcome, theme: HandTheme): Color = when (outcome) {
    Outcome.WIN -> if (theme.isDark) Color(0xFF4CD27E) else Color(0xFF2E7D32)
    Outcome.LOSS -> if (theme.isDark) Color(0xFFFF6B5E) else Color(0xFFC62828)
    Outcome.DRAW -> theme.textColor.copy(alpha = 0.75f)
}

/**
 * The full analysis: verdict, what's threatened right now, and every legal move with its
 * consequences and the complete perfect-play line.
 */
@Composable
fun HintPanel(
    analysis: HintAnalysis,
    theme: HandTheme,
    onShowOnBoard: (Move) -> Unit,
    onPlay: (Move) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            // Swallow taps so the board underneath can't be dragged while reading
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.BottomCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .background(theme.backgroundColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .border(1.dp, theme.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ANALYSIS",
                        color = theme.primaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    ArtisticActionButton(text = "CLOSE", onClick = onClose, theme = theme)
                }
            }

            item {
                Text(analysis.hands, color = theme.textColor, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, outcomeColor(analysis.evaluation.outcome, theme), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    ResultBadge(HintAnalyzer.resultLabel(analysis.evaluation), analysis.evaluation.outcome, theme)
                    Spacer(Modifier.height(6.dp))
                    Text(analysis.verdict, color = theme.textColor, fontSize = 15.sp, lineHeight = 21.sp)
                }
            }

            item {
                FactList("YOU CAN KNOCK OUT NOW", analysis.chances, "Nothing: no attack of yours knocks out a hand this move.", theme)
            }
            item {
                FactList("THEY THREATEN (if you leave it)", analysis.dangers, "Nothing: none of their attacks would knock out a hand of yours.", theme)
            }

            item {
                Column {
                    analysis.ruleNotes.forEach {
                        Text("• $it", color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }

            item {
                Text(
                    "ALL YOUR MOVES (${analysis.moves.size}), BEST FIRST",
                    color = theme.primaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            items(analysis.moves) { move ->
                MoveCard(move, theme, onShowOnBoard, onPlay)
            }
        }
    }
}

@Composable
private fun MoveCard(
    move: MoveAnalysis,
    theme: HandTheme,
    onShowOnBoard: (Move) -> Unit,
    onPlay: (Move) -> Unit
) {
    val color = outcomeColor(move.evaluation.outcome, theme)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.secondaryColor.copy(alpha = if (theme.isDark) 0.35f else 0.25f), RoundedCornerShape(8.dp))
            .border(if (move.isBest) 2.dp else 1.dp, color.copy(alpha = if (move.isBest) 1f else 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultBadge(HintAnalyzer.resultLabel(move.evaluation), move.evaluation.outcome, theme)
            if (move.isBest) {
                Text(
                    "BEST",
                    color = theme.backgroundColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .background(theme.primaryColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(move.action, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
        Text(move.effect, color = theme.textColor.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 19.sp)

        Spacer(Modifier.height(8.dp))
        FactList("LEAVES OPEN", move.leavesOpen, "Nothing of yours can be knocked out on their next move.", theme, compact = true)
        Spacer(Modifier.height(4.dp))
        FactList("THREATENS NEXT", move.threatens, "No knockout threat against them.", theme, compact = true)

        Spacer(Modifier.height(8.dp))
        Text("PERFECT PLAY FROM HERE", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        move.line.forEachIndexed { i, step ->
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "${i + 1}.",
                    color = if (step.byYou) theme.primaryColor else theme.textColor.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp)
                )
                Column {
                    Text(step.description, color = theme.textColor, fontSize = 13.sp, lineHeight = 17.sp)
                    Text(step.handsAfter, color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        if (move.lineEnd.isNotEmpty()) {
            Text(move.lineEnd, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtisticActionButton(text = "SHOW ON BOARD", onClick = { onShowOnBoard(move.move) }, theme = theme)
            ArtisticActionButton(text = "PLAY IT", onClick = { onPlay(move.move) }, theme = theme)
        }
    }
}

/** Shown instead of the panel while a move's arrow is displayed on the board. */
@Composable
fun HintPreviewBar(
    move: MoveAnalysis,
    theme: HandTheme,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.backgroundColor.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                .border(2.dp, outcomeColor(move.evaluation.outcome, theme), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            ResultBadge(HintAnalyzer.resultLabel(move.evaluation), move.evaluation.outcome, theme)
            Spacer(Modifier.height(6.dp))
            Text(move.action, color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(move.effect, color = theme.textColor.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtisticActionButton(text = "BACK", onClick = onBack, theme = theme)
                ArtisticActionButton(text = "PLAY IT", onClick = onPlay, theme = theme)
                ArtisticActionButton(text = "CLOSE", onClick = onClose, theme = theme)
            }
        }
    }
}

@Composable
private fun ResultBadge(label: String, outcome: Outcome, theme: HandTheme) {
    Text(
        label,
        color = outcomeColor(outcome, theme),
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier
            .border(1.5.dp, outcomeColor(outcome, theme), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun FactList(title: String, facts: List<String>, emptyText: String, theme: HandTheme, compact: Boolean = false) {
    Column {
        Text(
            title,
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        if (facts.isEmpty()) {
            Text(emptyText, color = theme.textColor.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 17.sp)
        } else {
            facts.forEach { Text("• $it", color = theme.textColor, fontSize = if (compact) 13.sp else 14.sp, lineHeight = 18.sp) }
        }
    }
}
