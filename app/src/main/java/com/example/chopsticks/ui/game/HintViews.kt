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
 * The analysis, kept compact: a one-line verdict, any immediate knockouts, then every legal move
 * as a single row (best first). Tapping a row expands its reasons: what it leaves open, what it
 * threatens, the opponent's best reply, and (behind one more tap) the full perfect-play line.
 */
@Composable
fun HintPanel(
    analysis: HintAnalysis,
    theme: HandTheme,
    onShowOnBoard: (Move) -> Unit,
    onPlay: (Move) -> Unit,
    onClose: () -> Unit
) {
    // The first best move starts open; the rest are one line each until tapped
    var expanded by remember(analysis) { mutableStateOf(analysis.moves.firstOrNull()?.move) }
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
                .heightIn(max = 620.dp)
                .background(theme.backgroundColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .border(1.dp, theme.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(analysis.hands, color = theme.textColor, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            ResultBadge(HintAnalyzer.resultLabel(analysis.evaluation), analysis.evaluation.outcome, theme)
                            Spacer(Modifier.width(8.dp))
                            Text(analysis.verdict, color = theme.textColor, fontSize = 13.sp, lineHeight = 17.sp)
                        }
                    }
                    ArtisticActionButton(text = "CLOSE", onClick = onClose, theme = theme)
                }
            }

            if (analysis.chances.isNotEmpty() || analysis.dangers.isNotEmpty() || analysis.ruleNotes.isNotEmpty()) {
                item {
                    Column {
                        analysis.chances.forEach { Fact("Can knock out", it, outcomeColor(Outcome.WIN, theme), theme) }
                        analysis.dangers.forEach { Fact("Threatened", it, outcomeColor(Outcome.LOSS, theme), theme) }
                        analysis.ruleNotes.forEach { Text(it, color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp) }
                    }
                }
            }

            items(analysis.moves) { move ->
                MoveRow(
                    move = move,
                    theme = theme,
                    expanded = expanded == move.move,
                    onToggle = { expanded = if (expanded == move.move) null else move.move },
                    onShowOnBoard = onShowOnBoard,
                    onPlay = onPlay
                )
            }
        }
    }
}

@Composable
private fun Fact(label: String, text: String, color: Color, theme: HandTheme) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text("$label: ", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text, color = theme.textColor, fontSize = 12.sp)
    }
}

/** One move: a single line until tapped, then its reasons and the line of play. */
@Composable
private fun MoveRow(
    move: MoveAnalysis,
    theme: HandTheme,
    expanded: Boolean,
    onToggle: () -> Unit,
    onShowOnBoard: (Move) -> Unit,
    onPlay: (Move) -> Unit
) {
    val color = outcomeColor(move.evaluation.outcome, theme)
    var showLine by remember(move) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.secondaryColor.copy(alpha = if (theme.isDark) 0.35f else 0.25f), RoundedCornerShape(8.dp))
            .border(if (move.isBest) 2.dp else 1.dp, color.copy(alpha = if (move.isBest) 1f else 0.4f), RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ResultBadge(if (move.isBest) "BEST · " + HintAnalyzer.resultLabel(move.evaluation) else HintAnalyzer.resultLabel(move.evaluation), move.evaluation.outcome, theme)
            Spacer(Modifier.width(8.dp))
            Text(if (expanded) "▾" else "▸", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        Text(move.action, color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
        Text(move.effect, color = theme.textColor.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 17.sp)

        if (expanded) {
            Spacer(Modifier.height(6.dp))
            move.leavesOpen.forEach { Fact("Exposes", it, outcomeColor(Outcome.LOSS, theme), theme) }
            move.threatens.forEach { Fact("Threatens", it, outcomeColor(Outcome.WIN, theme), theme) }
            // Their best reply is usually the whole reason a move is good or bad
            move.line.getOrNull(1)?.let { reply ->
                Fact("Best reply", reply.description.removePrefix("They ").replaceFirstChar(Char::uppercase), theme.primaryColor, theme)
            }
            if (move.line.size > 2) {
                Text(
                    if (showLine) "▾ Full line (${move.line.size} moves)" else "▸ Full line (${move.line.size} moves)",
                    color = theme.primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp).clickable { showLine = !showLine }
                )
                if (showLine) {
                    move.line.forEachIndexed { i, step ->
                        Text(
                            "${i + 1}. ${step.description}  →  ${step.handsAfter}",
                            color = if (step.byYou) theme.textColor else theme.textColor.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            if (move.lineEnd.isNotEmpty()) {
                Text(move.lineEnd, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtisticActionButton(text = "SHOW", onClick = { onShowOnBoard(move.move) }, theme = theme)
                ArtisticActionButton(text = "PLAY IT", onClick = { onPlay(move.move) }, theme = theme)
            }
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
