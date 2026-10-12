package com.example.chopsticks.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.ai.HintCategory
import com.example.chopsticks.domain.ai.HintResult
import com.example.chopsticks.domain.ai.Move
import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.DrawReason
import com.example.chopsticks.domain.model.GameMode
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.RulesetConfig
import com.example.chopsticks.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

data class TransferChoice(
    val fromHandIndex: Int,
    val toHandIndex: Int,
    val validAmounts: List<Int>
)

data class CondenseChoice(
    val remainder: Int,
    val validSplits: List<Pair<Int, Int>>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    theme: HandTheme,
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val invalidHand by viewModel.invalidMoveHand.collectAsState()
    val hintResult by viewModel.hintState.collectAsState()
    val isHintLoading by viewModel.isHintLoading.collectAsState()
    
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val handBounds = remember { mutableStateMapOf<String, Rect>() }
    
    var draggingHandIndex by remember { mutableStateOf<Int?>(null) }
    var dragSourcePlayerId by remember { mutableStateOf<String?>(null) }
    var currentDragPosition by remember { mutableStateOf(Offset.Zero) }
    
    var transferChoice by remember { mutableStateOf<TransferChoice?>(null) }
    var condenseChoice by remember { mutableStateOf<CondenseChoice?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .onGloballyPositioned { rootCoordinates = it }
            .pointerInput(uiState.currentPlayerId, uiState.isGameOver, uiState.isMovePending) {
                if (uiState.isGameOver || uiState.isMovePending) return@pointerInput
                
                val currentPlayerId = uiState.currentPlayerId
                val isAiTurn = (currentPlayerId == "Player 2") && (viewModel.gameMode.name == "PvE")
                if (isAiTurn) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        val prefix = if (currentPlayerId == "Player 1") "P1" else "P2"
                        val player = if (currentPlayerId == "Player 1") uiState.player1 else uiState.player2
                        val exactFive = uiState.ruleset.exactFive

                        val leftRect = handBounds["${prefix}_L"]
                        val rightRect = handBounds["${prefix}_R"]

                        if (leftRect != null && leftRect.contains(offset) && !player.leftHand.isDead(uiState.ruleset.maxFingers, exactFive)) {
                            draggingHandIndex = 0
                            dragSourcePlayerId = currentPlayerId
                            currentDragPosition = offset
                        } else if (rightRect != null && rightRect.contains(offset) && !player.rightHand.isDead(uiState.ruleset.maxFingers, exactFive)) {
                            draggingHandIndex = 1
                            dragSourcePlayerId = currentPlayerId
                            currentDragPosition = offset
                        }
                    },
                    onDrag = { change, _ ->
                        currentDragPosition = change.position
                    },
                    onDragEnd = {
                        val fromIndex = draggingHandIndex
                        val sourceId = dragSourcePlayerId
                        if (fromIndex != null && sourceId != null) {
                            val opponentPrefix = if (sourceId == "Player 1") "P2" else "P1"
                            val ownPrefix = if (sourceId == "Player 1") "P1" else "P2"

                            val oppLeftRect = handBounds["${opponentPrefix}_L"]
                            val oppRightRect = handBounds["${opponentPrefix}_R"]

                            if (oppLeftRect != null && oppLeftRect.contains(currentDragPosition)) {
                                viewModel.performAttack(fromIndex, 0)
                            } else if (oppRightRect != null && oppRightRect.contains(currentDragPosition)) {
                                viewModel.performAttack(fromIndex, 1)
                            } else {
                                // Check for transfer to own other hand
                                val otherOwnIndex = if (fromIndex == 0) 1 else 0
                                val otherOwnKey = if (otherOwnIndex == 0) "${ownPrefix}_L" else "${ownPrefix}_R"
                                val otherOwnRect = handBounds[otherOwnKey]

                                if (otherOwnRect != null && otherOwnRect.contains(currentDragPosition)) {
                                    val player = if (sourceId == "Player 1") uiState.player1 else uiState.player2
                                    val fromHand = if (fromIndex == 0) player.leftHand else player.rightHand

                                    val validAmounts = mutableListOf<Int>()
                                    for (amt in 1..fromHand.fingers) {
                                        if (RulesEngine.validateTransfer(uiState, fromIndex, otherOwnIndex, amt)) {
                                            validAmounts.add(amt)
                                        }
                                    }

                                    if (validAmounts.size == 1 || (uiState.ruleset.onlyEvenSplits && validAmounts.isNotEmpty())) {
                                        viewModel.performTransfer(fromIndex, otherOwnIndex, validAmounts[0])
                                    } else if (validAmounts.size > 1) {
                                        transferChoice = TransferChoice(fromIndex, otherOwnIndex, validAmounts)
                                    } else {
                                        // Invalid move feedback
                                        viewModel.performTransfer(fromIndex, otherOwnIndex, 0) // This will trigger feedback
                                    }
                                } else {
                                    // If dropped outside but it was a clear drag, maybe do nothing
                                }
                            }
                        }
                        draggingHandIndex = null
                        dragSourcePlayerId = null
                    },
                    onDragCancel = {
                        draggingHandIndex = null
                        dragSourcePlayerId = null
                    }
                )
            }
    ) {
        // TURN OVERLAY (Drawn behind hands or over them)
        ThemeTurnOverlay(
            currentPlayerId = uiState.currentPlayerId,
            theme = theme,
            handBounds = handBounds,
            gameMode = viewModel.gameMode
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: Opponent
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (viewModel.gameMode.name == "PvE") "COMPUTER" else "PLAYER 2",
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ThemedHand(
                            fingers = uiState.player2.rightHand.fingers,
                            maxFingers = uiState.ruleset.maxFingers,
                            isDead = uiState.player2.rightHand.isDead(uiState.ruleset.maxFingers, uiState.ruleset.exactFive),
                            isInvalid = invalidHand?.first == "Player 2" && invalidHand?.second == 1,
                            theme = theme,
                            isLeftHand = false,
                            isActive = uiState.currentPlayerId == "Player 2",
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { rotationZ = 180f }
                                .onGloballyPositioned { coords ->
                                    rootCoordinates?.let { root -> handBounds["P2_R"] = root.localBoundingBoxOf(coords) }
                                }
                        )
                        ThemedHand(
                            fingers = uiState.player2.leftHand.fingers,
                            maxFingers = uiState.ruleset.maxFingers,
                            isDead = uiState.player2.leftHand.isDead(uiState.ruleset.maxFingers, uiState.ruleset.exactFive),
                            isInvalid = invalidHand?.first == "Player 2" && invalidHand?.second == 0,
                            theme = theme,
                            isLeftHand = true,
                            isActive = uiState.currentPlayerId == "Player 2",
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { rotationZ = 180f }
                                .onGloballyPositioned { coords ->
                                    rootCoordinates?.let { root -> handBounds["P2_L"] = root.localBoundingBoxOf(coords) }
                                }
                        )
                    }
                }
            }


            // MIDDLE: Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .let {
                        if (uiState.isGameOver) {
                            it.irregularBorder(
                                color = theme.primaryColor.copy(alpha = 0.3f),
                                strokeWidth = 2.dp,
                                cornerRadius = theme.cornerRadius,
                                isIrregular = theme.organicEdges
                            ).padding(16.dp)
                        } else it
                    }
            ) {
                if (uiState.isGameOver) {
                    Text(
                        text = resultText(uiState),
                        color = theme.textColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    ArtisticActionButton(
                        text = "TRY AGAIN",
                        onClick = { viewModel.restartGame() },
                        theme = theme
                    )
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // PLAYER ACTIONS (Condense, Exchange)
            val isAiTurn = (uiState.currentPlayerId == "Player 2") && (viewModel.gameMode == GameMode.PvE)
            // Show action buttons whenever game is active, but disabled during AI turn or move pending
            if (!uiState.isGameOver) {
                val currentPlayer = uiState.getCurrentPlayer()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Condense Button
                    if (uiState.ruleset.allowCondense) {
                        val total = currentPlayer.leftHand.fingers + currentPlayer.rightHand.fingers
                        if (total > uiState.ruleset.maxFingers) {
                            ArtisticActionButton(
                                text = "Condense Hands (Meta)",
                                onClick = {
                                    val remainder = total - uiState.ruleset.maxFingers
                                    val validSplits = mutableListOf<Pair<Int, Int>>()
                                    for (left in 0..remainder) {
                                        val right = remainder - left
                                        if (RulesEngine.validateCondense(uiState, left, right)) {
                                            validSplits.add(left to right)
                                        }
                                    }
                                    condenseChoice = CondenseChoice(remainder, validSplits)
                                },
                                enabled = !isAiTurn && !uiState.isMovePending,
                                theme = theme
                            )
                        }
                    }

                    if (uiState.ruleset.allowCondense && uiState.ruleset.allowExchangeLife) {
                        val total = currentPlayer.leftHand.fingers + currentPlayer.rightHand.fingers
                        if (total > uiState.ruleset.maxFingers) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }

                    // Exchange Life Button
                    if (uiState.ruleset.allowExchangeLife) {
                        val leftDead = currentPlayer.leftHand.isDead(uiState.ruleset.maxFingers, uiState.ruleset.exactFive)
                        val rightDead = currentPlayer.rightHand.isDead(uiState.ruleset.maxFingers, uiState.ruleset.exactFive)

                        if (leftDead != rightDead) { // One dead, one alive
                            ArtisticActionButton(
                                text = "Exchange Life (Logan Clause)",
                                onClick = {
                                    val sacrificedIndex = if (leftDead) 1 else 0
                                    viewModel.performExchangeLife(sacrificedIndex)
                                },
                                enabled = !isAiTurn && !uiState.isMovePending,
                                theme = theme
                            )
                        }
                    }

                    if (uiState.ruleset.allowExchangeLife || uiState.ruleset.allowCondense) {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
            
            // BOTTOM: Player
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ThemedHand(
                            fingers = uiState.player1.leftHand.fingers,
                            maxFingers = uiState.ruleset.maxFingers,
                            isDead = uiState.player1.leftHand.isDead(uiState.ruleset.maxFingers, uiState.ruleset.exactFive),
                            isInvalid = invalidHand?.first == "Player 1" && invalidHand?.second == 0,
                            theme = theme,
                            isLeftHand = true,
                            isActive = uiState.currentPlayerId == "Player 1",
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    rootCoordinates?.let { root -> handBounds["P1_L"] = root.localBoundingBoxOf(coords) }
                                }
                        )
                        ThemedHand(
                            fingers = uiState.player1.rightHand.fingers,
                            maxFingers = uiState.ruleset.maxFingers,
                            isDead = uiState.player1.rightHand.isDead(uiState.ruleset.maxFingers, uiState.ruleset.exactFive),
                            isInvalid = invalidHand?.first == "Player 1" && invalidHand?.second == 1,
                            theme = theme,
                            isLeftHand = false,
                            isActive = uiState.currentPlayerId == "Player 1",
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    rootCoordinates?.let { root -> handBounds["P1_R"] = root.localBoundingBoxOf(coords) }
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PLAYER 1",
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val isAiTurn = (uiState.currentPlayerId == "Player 2") && (viewModel.gameMode == GameMode.PvE)
                    // HINT button remains visible throughout the game to ensure layout stability
                    if (!uiState.isGameOver) {
                        Spacer(modifier = Modifier.height(24.dp))
                        ArtisticActionButton(
                            text = if (isHintLoading) "ANALYZING..." else "HINT",
                            onClick = { viewModel.requestHint() },
                            enabled = !isAiTurn && !uiState.isMovePending && !isHintLoading,
                            theme = theme,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }

        }

        // Drag line
        if (draggingHandIndex != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sourceKey = if (dragSourcePlayerId == "Player 1") {
                    if (draggingHandIndex == 0) "P1_L" else "P1_R"
                } else {
                    if (draggingHandIndex == 0) "P2_L" else "P2_R"
                }
                handBounds[sourceKey]?.center?.let { start ->
                    drawLine(
                        color = theme.primaryColor,
                        start = start,
                        end = currentDragPosition,
                        strokeWidth = 4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    drawCircle(color = theme.primaryColor, radius = 6.dp.toPx(), center = currentDragPosition)
                }
            }
        }

        // Move display
        ActionDisplayOverlay(
            lastMove = uiState.lastMove,
            lastMoveTimestamp = uiState.lastMoveTimestamp,
            handBounds = handBounds,
            theme = theme
        )

        // Hint Move Suggestion
        hintResult?.let { hint ->
            HintMoveOverlay(
                move = hint.move,
                handBounds = handBounds,
                theme = theme
            )
        }

        // Transfer Choice
        transferChoice?.let { choice ->
            AlertDialog(
                onDismissRequest = { transferChoice = null },
                title = { Text("Transfer Amount", color = theme.textColor, fontWeight = FontWeight.Bold) },
                text = {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        choice.validAmounts.forEach { amt ->
                            Button(
                                onClick = {
                                    viewModel.performTransfer(choice.fromHandIndex, choice.toHandIndex, amt)
                                    transferChoice = null
                                },
                                enabled = !uiState.isMovePending,
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(amt.toString(), color = if (theme.isDark) Color.Black else Color.White)
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = theme.backgroundColor,
                shape = RoundedCornerShape(theme.cornerRadius)
            )
        }

        // Condense Choice
        condenseChoice?.let { choice ->
            AlertDialog(
                onDismissRequest = { condenseChoice = null },
                title = { Text("Condense: Split Remainder (${choice.remainder})", color = theme.textColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                text = {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        choice.validSplits.forEach { (l, r) ->
                            ArtisticActionButton(
                                text = "$l ⦀ $r",
                                onClick = {
                                    viewModel.performCondense(l, r)
                                    condenseChoice = null
                                },
                                enabled = !uiState.isMovePending,
                                theme = theme
                            )
                        }
                    }
                },
                confirmButton = {},
                containerColor = theme.backgroundColor,
                shape = RoundedCornerShape(theme.cornerRadius)
            )
        }

        // Back Button
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Text("←", color = theme.textColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }

        // Themed Game Over Overlay
        if (uiState.isGameOver) {
            GameOverOverlay(
                state = uiState,
                theme = theme,
                gameMode = viewModel.gameMode,
                onRestart = { viewModel.restartGame() }
            )
        }

        // HINT OVERLAY (Educational Explanation)
        hintResult?.let { hint ->
            HintDialog(
                hint = hint,
                theme = theme,
                onDismiss = { viewModel.clearHint() }
            )
        }
    }
}

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
        is HandTheme.PaperCutout -> {
            val steps = 20
            val path = Path()
            path.moveTo(start.x, start.y)
            
            for (i in 1..(steps * progress).toInt()) {
                val p = i.toFloat() / steps
                val point = start + dir * p
                val seedSeed = i * 12345L
                val stepRandom = java.util.Random(seedSeed)
                val jitterX = (stepRandom.nextFloat() - 0.5f) * 24f
                val jitterY = (stepRandom.nextFloat() - 0.5f) * 24f
                path.lineTo(point.x + jitterX, point.y + jitterY)
            }
            drawPath(path, theme.primaryColor, style = Stroke(width = 12.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
            
            if (progress > 0.7f) {
                val arrowTip = currentEnd
                val arrowLeft = arrowTip - normDir * 40f + ortho * 24f
                val arrowRight = arrowTip - normDir * 40f - ortho * 24f
                
                val arrowPath = Path().apply {
                    moveTo(arrowTip.x, arrowTip.y)
                    lineTo(arrowLeft.x, arrowLeft.y)
                    lineTo(arrowRight.x, arrowRight.y)
                    close()
                }
                drawPath(arrowPath, theme.primaryColor)
            }
        }
    }
}

fun Modifier.irregularBorder(
    color: Color,
    strokeWidth: Dp,
    cornerRadius: Dp,
    isIrregular: Boolean
) = this.drawBehind {
    if (!isIrregular) {
        drawRoundRect(
            color = color,
            style = Stroke(strokeWidth.toPx()),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    } else {
        val path = createOrganicRectPath(Rect(Offset.Zero, size))
        drawPath(path, color, style = Stroke(strokeWidth.toPx()))
    }
}


@Composable
fun ThemedHand(
    fingers: Int,
    maxFingers: Int,
    isDead: Boolean,
    theme: HandTheme,
    isLeftHand: Boolean,
    modifier: Modifier = Modifier,
    isInvalid: Boolean = false,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hand_animations")
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val activeAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        animationSpec = tween(500),
        label = "active_alpha"
    )

    val invalidPulse by animateFloatAsState(
        targetValue = if (isInvalid) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "invalid_pulse"
    )

    val shakeX by animateFloatAsState(
        targetValue = if (isInvalid) (sin(System.currentTimeMillis() / 20f) * 6f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "shake"
    )

    Box(
        modifier = modifier
            .aspectRatio(0.75f)
            .graphicsLayer {
                translationX = shakeX
                alpha = activeAlpha
            }
            .drawWithCache {
                onDrawWithContent {
                    val w = size.width
                    val h = size.height
                    val jitter = if (theme.organicEdges) 4f else 0f

                    if (isDead) {
                        drawRect(Color.Gray.copy(alpha = 0.1f))
                        drawLine(
                            theme.textColor.copy(alpha = 0.4f),
                            Offset(0f, 0f),
                            Offset(w, h),
                            4.dp.toPx()
                        )
                        drawLine(
                            theme.textColor.copy(alpha = 0.4f),
                            Offset(w, 0f),
                            Offset(0f, h),
                            4.dp.toPx()
                        )
                        return@onDrawWithContent
                    }

                    // --- ANATOMICAL DIMENSIONS ---
                    val palmWidth = w * 0.72f
                    val palmHeight = h * 0.48f
                    val palmTop = h * 0.48f
                    val palmRect = Rect(
                        offset = Offset((w - palmWidth) / 2, palmTop),
                        size = Size(palmWidth, palmHeight)
                    )

                    // --- DRAW PALM ---
                    val palmPath = createAnatomicalPalmPath(palmRect, isLeftHand, jitter)
                    
                    if (invalidPulse > 0.01f) {
                        drawPath(
                            palmPath, 
                            Color.Red.copy(alpha = 0.4f * invalidPulse),
                            style = Stroke(6.dp.toPx())
                        )
                    }
                    
                    when (theme) {
                        is HandTheme.AnalogSynth -> {
                            val plateBrush = Brush.linearGradient(
                                colors = listOf(theme.handColor, theme.handColor.copy(alpha = 0.85f)),
                                start = Offset(palmRect.left, palmRect.top),
                                end = Offset(palmRect.right, palmRect.bottom)
                            )
                            drawPath(palmPath, plateBrush)
                            drawPath(palmPath, theme.secondaryColor.copy(alpha = 0.3f), style = Stroke(2.dp.toPx()))
                            
                            val gridPath = Path()
                            for (gx in 0..palmRect.width.toInt() step 40) {
                                gridPath.moveTo(palmRect.left + gx, palmRect.top + 20.dp.toPx())
                                gridPath.lineTo(palmRect.left + gx, palmRect.bottom - 10.dp.toPx())
                            }
                            for (gy in 0..palmRect.height.toInt() step 40) {
                                gridPath.moveTo(palmRect.left + 10.dp.toPx(), palmRect.top + gy)
                                gridPath.lineTo(palmRect.right - 10.dp.toPx(), palmRect.top + gy)
                            }
                            drawPath(gridPath, theme.secondaryColor.copy(alpha = 0.4f), style = Stroke(1.dp.toPx()))

                            // OSCILLOSCOPE: Flat if not active
                            val oscPath = Path()
                            val amplitude = if (isActive) 25.dp.toPx() else 0f
                            for (x in 0..palmRect.width.toInt() step 5) {
                                val y = sin((x + pulse * 600) * 0.08f) * amplitude
                                if (x == 0) oscPath.moveTo(palmRect.left + x, palmRect.center.y + y)
                                else oscPath.lineTo(palmRect.left + x, palmRect.center.y + y)
                            }
                            drawPath(oscPath, theme.primaryColor, style = Stroke(4.dp.toPx()))
                        }
                        is HandTheme.PaperCutout -> {
                            drawPath(palmPath, theme.handColor)
                            
                            val linePath = Path().apply {
                                moveTo(palmRect.center.x, palmRect.top + 15.dp.toPx())
                                quadraticTo(
                                    palmRect.left + (if (isLeftHand) palmWidth * 0.8f else palmWidth * 0.2f),
                                    palmRect.center.y,
                                    palmRect.center.x,
                                    palmRect.bottom - 10.dp.toPx()
                                )
                                val startX = if (isLeftHand) 15.dp.toPx() else palmWidth - 15.dp.toPx()
                                val endX = if (isLeftHand) palmWidth - 15.dp.toPx() else 15.dp.toPx()
                                moveTo(palmRect.left + startX, palmRect.top + 35.dp.toPx())
                                quadraticTo(
                                    palmRect.center.x, 
                                    palmRect.top + 25.dp.toPx(), 
                                    palmRect.left + endX, 
                                    palmRect.top + 45.dp.toPx()
                                )
                                moveTo(palmRect.left + startX, palmRect.top + 60.dp.toPx())
                                quadraticTo(
                                    palmRect.center.x,
                                    palmRect.top + 55.dp.toPx(),
                                    palmRect.left + (if (isLeftHand) palmWidth * 0.7f else palmWidth * 0.3f),
                                    palmRect.top + 75.dp.toPx()
                                )
                            }
                            drawPath(linePath, theme.textColor.copy(alpha = 0.3f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))

                            drawCircle(theme.primaryColor.copy(alpha = 0.6f), 20.dp.toPx(), Offset(palmRect.left + 20.dp.toPx(), palmRect.top + 25.dp.toPx()))
                            drawCircle(theme.primaryColor.copy(alpha = 0.4f), 12.dp.toPx(), Offset(palmRect.right - 25.dp.toPx(), palmRect.bottom - 15.dp.toPx()))
                        }
                    }

                    // --- DRAW FINGERS ---
                    val thumbSide = if (isLeftHand) 1f else 0f
                    val pinkySide = if (isLeftHand) 0f else 1f
                    
                    val fingerBases = mutableListOf<Offset>()
                    val fingerAngles = mutableListOf<Float>()
                    val fingerLengths = mutableListOf<Float>()
                    val fingerWidths = mutableListOf<Float>()

                    if (maxFingers <= 5) {
                        // Maintain original anatomical proportions for standard play
                        fingerBases.addAll(listOf(
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.9f + thumbSide * 0.1f), palmRect.top + 14.dp.toPx()), // Pinky
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.7f + thumbSide * 0.3f), palmRect.top + 4.dp.toPx()),  // Ring
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.5f + thumbSide * 0.5f), palmRect.top + 0.dp.toPx()),  // Middle
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.3f + thumbSide * 0.7f), palmRect.top + 6.dp.toPx()),  // Index
                            Offset(palmRect.left + palmWidth * (pinkySide * 0.05f + thumbSide * 0.95f), palmRect.top + palmHeight * 0.45f) // Thumb
                        ))
                        fingerAngles.addAll(listOf(
                            if (isLeftHand) -15f else 15f,
                            if (isLeftHand) -6f else 6f,
                            0f,
                            if (isLeftHand) 8f else -8f,
                            if (isLeftHand) 42f else -42f
                        ))
                        fingerLengths.addAll(listOf(h * 0.36f, h * 0.44f, h * 0.48f, h * 0.42f, h * 0.30f))
                        fingerWidths.addAll(listOf(w * 0.11f, w * 0.13f, w * 0.14f, w * 0.13f, w * 0.18f))
                    } else {
                        // Dynamic scaling for up to 10 fingers
                        val numKnuckleFingers = maxFingers - 1
                        val knuckleStart = 0.1f
                        val knuckleEnd = 0.8f

                        for (i in 0 until numKnuckleFingers) {
                            val t = i.toFloat() / (numKnuckleFingers - 1).coerceAtLeast(1)
                            val xRel = knuckleStart + t * (knuckleEnd - knuckleStart)
                            val xPos = if (isLeftHand) xRel else (1f - xRel)
                            
                            val arcT = t
                            val yOffset = (1f - sin(arcT * Math.PI.toFloat())) * 14.dp.toPx()
                            
                            fingerBases.add(Offset(palmRect.left + palmWidth * xPos, palmRect.top + yOffset))
                            
                            // Anatomical angle arc
                            val baseAngle = -15f + t * 23f
                            fingerAngles.add(if (isLeftHand) baseAngle else -baseAngle)
                            
                            // Anatomical length curve (Middle longest at t~0.6)
                            val length = h * (0.48f - 0.3f * (t - 0.6f) * (t - 0.6f))
                            fingerLengths.add(length)
                            
                            val width = (knuckleEnd - knuckleStart) * palmWidth / numKnuckleFingers * 0.85f
                            fingerWidths.add(width)
                        }
                        
                        // Thumb always at the side
                        val thumbX = if (isLeftHand) 0.95f else 0.05f
                        fingerBases.add(Offset(palmRect.left + palmWidth * thumbX, palmRect.top + palmHeight * 0.45f))
                        fingerAngles.add(if (isLeftHand) 42f else -42f)
                        fingerLengths.add(h * 0.30f)
                        fingerWidths.add(w * 0.18f * (5f / maxFingers).coerceAtLeast(0.6f))
                    }

                    // Map finger count to active indices
                    val activeIndices = if (maxFingers <= 5) {
                        when (fingers) {
                            1 -> listOf(3)
                            2 -> listOf(3, 2)
                            3 -> listOf(3, 2, 1)
                            4 -> listOf(3, 2, 1, 0)
                            else -> if (fingers >= 5) listOf(0, 1, 2, 3, 4) else emptyList()
                        }
                    } else {
                        if (fingers >= maxFingers) {
                            (0 until maxFingers).toList()
                        } else {
                            val numKnuckleFingers = maxFingers - 1
                            val indexPosition = numKnuckleFingers - 1
                            (0 until fingers).map { i -> 
                                (indexPosition - i).coerceAtLeast(0)
                            }
                        }
                    }

                    for (i in 0 until maxFingers) {
                        val isActive = i in activeIndices
                        if (!isActive) continue

                        drawAnatomicalFinger(
                            base = fingerBases[i],
                            angle = fingerAngles[i],
                            length = fingerLengths[i],
                            width = fingerWidths[i],
                            theme = theme,
                            jitter = jitter
                        )

                        // --- THEME ADAPTATION OVERLAYS ---
                        if (theme is HandTheme.AnalogSynth) {
                            val fx = fingerBases[i].x
                            val fy = fingerBases[i].y - fingerLengths[i] * 0.6f
                            val cable = Path().apply {
                                moveTo(palmRect.center.x, palmRect.center.y)
                                cubicTo(
                                    palmRect.center.x - 30.dp.toPx(), palmRect.top - 20.dp.toPx(),
                                    fx + 20.dp.toPx(), palmRect.top - 40.dp.toPx(),
                                    fx, fy
                                )
                            }
                            drawPath(cable, theme.primaryColor, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                            
                            val lightColor = if (pulse > 0.5f) theme.primaryColor else theme.secondaryColor
                            val tipPos = fingerBases[i] + Offset(
                                (sin(Math.toRadians(fingerAngles[i].toDouble())) * -fingerLengths[i]).toFloat(),
                                (-Math.cos(Math.toRadians(fingerAngles[i].toDouble())) * fingerLengths[i]).toFloat()
                            )
                            drawCircle(lightColor, 6.dp.toPx(), tipPos + Offset(0f, 10.dp.toPx()))
                        }
                    }
                }
            }
    )
}

private fun createAnatomicalPalmPath(rect: Rect, isLeft: Boolean, jitter: Float): Path {
    val path = Path()
    val w = rect.width
    val h = rect.height
    val l = rect.left
    val t = rect.top

    val tX = if (isLeft) 1f else 0f // Thumb side
    val pX = if (isLeft) 0f else 1f // Pinky side

    // Start at bottom center (wrist)
    path.moveTo(l + w * 0.5f, t + h * 0.95f)

    // Wrist area - slight inward curve for anatomical realism
    path.quadraticTo(l + w * (0.5f + (pX - 0.5f) * 0.7f), t + h * 0.98f, l + w * (0.5f + (pX - 0.5f) * 0.85f), t + h * 0.9f)

    // Heel of the palm (pinky side) - fleshy bulge (Hypothenar eminence)
    path.quadraticTo(l + w * (pX * 1.08f + tX * -0.08f), t + h * 0.75f, l + w * pX, t + h * 0.55f)
    
    // Pinky side up to knuckle
    path.lineTo(l + w * pX, t + h * 0.3f)
    
    // Knuckle arc - fleshy and pronounced
    path.cubicTo(
        l + w * (pX * 0.7f + tX * 0.3f), t + h * 0.02f,
        l + w * (pX * 0.3f + tX * 0.7f), t - h * 0.05f, // Middle finger knuckle is highest
        l + w * (pX * 0.1f + tX * 0.9f), t + h * 0.2f
    )
    
    // Thumb web space - inward curve
    path.quadraticTo(
        l + w * (tX * 0.8f + pX * 0.2f), t + h * 0.38f,
        l + w * (tX * 0.92f + pX * 0.08f), t + h * 0.55f
    )

    // Thenar eminence (thumb mound) - large fleshy bulge
    path.quadraticTo(
        l + w * (tX * 1.25f + pX * -0.25f), t + h * 0.85f,
        l + w * 0.5f, t + h * 0.95f
    )
    
    path.close()
    
    if (jitter > 0) {
        return applyJitterToPath(path, jitter)
    }
    return path
}

private fun DrawScope.drawAnatomicalFinger(
    base: Offset,
    angle: Float,
    length: Float,
    width: Float,
    theme: HandTheme,
    jitter: Float
) {
    val segments = 3
    val segLength = length / segments
    
    rotate(angle, base) {
        var currentBase = base
        for (seg in 0 until segments) {
            val isTip = seg == segments - 1
            val segW = width * (1f - seg * 0.1f) // Fleshy tapering
            val nextW = width * (1f - (seg + 1) * 0.1f)
            
            val segPath = Path().apply {
                val leftBase = currentBase.x - segW / 2
                val rightBase = currentBase.x + segW / 2
                val leftTop = currentBase.x - nextW / 2
                val rightTop = currentBase.x + nextW / 2
                val topY = currentBase.y - segLength
                
                moveTo(leftBase, currentBase.y)
                // Left fleshy bulge
                quadraticTo(leftBase - 3.dp.toPx(), currentBase.y - segLength * 0.5f, leftTop, topY)
                
                if (isTip) {
                    // Soft rounded tip
                    quadraticTo(currentBase.x, topY - 10.dp.toPx(), rightTop, topY)
                } else {
                    lineTo(rightTop, topY)
                }
                
                // Right fleshy bulge
                quadraticTo(rightBase + 3.dp.toPx(), currentBase.y - segLength * 0.5f, rightBase, currentBase.y)
                close()
            }
            
            val finalPath = if (jitter > 0) applyJitterToPath(segPath, jitter) else segPath
            
            val segRect = Rect(Offset(currentBase.x - segW/2, currentBase.y - segLength), Size(segW, segLength))
            
            when (theme) {
                is HandTheme.AnalogSynth -> {
                    val plateBrush = Brush.verticalGradient(
                        colors = listOf(theme.fingerColor.copy(alpha=0.9f), theme.fingerColor),
                        startY = segRect.bottom,
                        endY = segRect.top
                    )
                    drawPath(finalPath, plateBrush)
                    // Ball-and-socket joint
                    drawCircle(theme.secondaryColor, radius = segW * 0.45f, center = currentBase)
                    drawCircle(Color.White.copy(alpha=0.25f), radius = segW * 0.2f, center = currentBase - Offset(2.dp.toPx(), 2.dp.toPx()))
                }
                is HandTheme.PaperCutout -> {
                    drawPath(finalPath, theme.fingerColor)
                    // Staple at joint
                    val stapleW = segW * 1.3f
                    drawRect(
                        color = Color(0xFF9E9E9E),
                        topLeft = Offset(currentBase.x - stapleW / 2, currentBase.y - 2.dp.toPx()),
                        size = Size(stapleW, 5.dp.toPx())
                    )
                    // Knuckle ridge - anatomical detail
                    val ridgePath = Path().apply {
                        moveTo(currentBase.x - segW * 0.4f, currentBase.y - segLength * 0.55f)
                        quadraticTo(currentBase.x, currentBase.y - segLength * 0.7f, currentBase.x + segW * 0.4f, currentBase.y - segLength * 0.55f)
                    }
                    drawPath(ridgePath, theme.textColor.copy(alpha = 0.25f), style = Stroke(1.5.dp.toPx()))
                }
            }
            
            currentBase = Offset(currentBase.x, currentBase.y - segLength)
        }
    }
}

@Composable
fun ArtisticActionButton(
    text: String,
    onClick: () -> Unit,
    theme: HandTheme,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val buttonAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        label = "button_alpha"
    )

    if (theme is HandTheme.AnalogSynth) {
        val infiniteTransition = rememberInfiniteTransition(label = "led_glow")
        val alphaGlow by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "led_glow_alpha"
        )
        
        Box(
            modifier = modifier
                .height(48.dp)
                .graphicsLayer { alpha = buttonAlpha }
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF252525))
                .border(
                    width = 2.dp,
                    color = theme.primaryColor.copy(alpha = if (enabled) 0.8f else 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val ledColor = if (enabled) theme.fingerColor else Color.Gray
                Box(contentAlignment = Alignment.Center, modifier = Modifier.width(16.dp).height(16.dp)) {
                    if (enabled) {
                        Box(
                            modifier = Modifier.width(16.dp).height(16.dp)
                                .background(ledColor.copy(alpha = 0.3f * alphaGlow), shape = CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier.width(8.dp).height(8.dp)
                            .background(ledColor, shape = CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text.uppercase(),
                    color = if (enabled) theme.textColor else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .height(48.dp)
                .graphicsLayer { alpha = buttonAlpha }
                .drawBehind {
                    val rect = Rect(Offset.Zero, size)
                    val path = Path()
                    val steps = 12
                    val jitter = 3f
                    val r = java.util.Random(text.hashCode().toLong())
                    
                    path.moveTo(rect.left + r.nextFloat() * jitter, rect.top + r.nextFloat() * jitter)
                    for (i in 1..steps) {
                        val x = rect.left + (rect.width / steps) * i
                        val y = rect.top + (if (i == steps) 0f else (r.nextFloat() - 0.5f) * jitter)
                        path.lineTo(x, y)
                    }
                    for (i in 1..4) {
                        val x = rect.right + (r.nextFloat() - 0.5f) * jitter
                        val y = rect.top + (rect.height / 4) * i
                        path.lineTo(x, y)
                    }
                    for (i in 1..steps) {
                        val x = rect.right - (rect.width / steps) * i
                        val y = rect.bottom + (if (i == steps) 0f else (r.nextFloat() - 0.5f) * jitter)
                        path.lineTo(x, y)
                    }
                    for (i in 1..4) {
                        val x = rect.left + (r.nextFloat() - 0.5f) * jitter
                        val y = rect.bottom - (rect.height / 4) * i
                        path.lineTo(x, y)
                    }
                    path.close()
                    
                    drawPath(
                        path = path,
                        color = if (enabled) theme.secondaryColor else theme.secondaryColor.copy(alpha = 0.5f)
                    )
                    
                    drawPath(
                        path = path,
                        color = theme.textColor.copy(alpha = if (enabled) 0.6f else 0.3f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    
                    val stapleW = 8.dp.toPx()
                    val stapleH = 3.dp.toPx()
                    val stapleColor = Color(0xFF7E848C)
                    
                    drawRect(
                        color = stapleColor.copy(alpha = if (enabled) 1f else 0.5f),
                        topLeft = Offset(rect.left + 4.dp.toPx(), rect.center.y - stapleH / 2),
                        size = Size(stapleW, stapleH)
                    )
                    drawRect(
                        color = stapleColor.copy(alpha = if (enabled) 1f else 0.5f),
                        topLeft = Offset(rect.right - 4.dp.toPx() - stapleW, rect.center.y - stapleH / 2),
                        size = Size(stapleW, stapleH)
                    )
                }
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) theme.textColor else theme.textColor.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun applyJitterToPath(path: Path, jitter: Float): Path {
    val newPath = Path()
    val measures = PathMeasure()
    measures.setPath(path, false)
    
    val length = measures.length
    val step = 4f
    var distance = 0f
    
    var first = true
    while (distance < length) {
        val pos = measures.getPosition(distance)
        val jX = (Random.nextFloat() - 0.5f) * jitter * 2
        val jY = (Random.nextFloat() - 0.5f) * jitter * 2
        if (first) {
            newPath.moveTo(pos.x + jX, pos.y + jY)
            first = false
        } else {
            newPath.lineTo(pos.x + jX, pos.y + jY)
        }
        distance += step
    }
    newPath.close()
    return newPath
}

private fun createOrganicRectPath(rect: Rect): Path {
    val path = Path()
    val steps = 8
    val jitter = 4f
    
    path.moveTo(rect.left + Random.nextFloat() * jitter, rect.top + Random.nextFloat() * jitter)
    
    // Top
    for (i in 1..steps) {
        val x = rect.left + (rect.width / steps) * i
        path.lineTo(x, rect.top + Random.nextFloat() * jitter)
    }
    // Right
    for (i in 1..steps) {
        val y = rect.top + (rect.height / steps) * i
        path.lineTo(rect.right + Random.nextFloat() * jitter, y)
    }
    // Bottom
    for (i in 1..steps) {
        val x = rect.right - (rect.width / steps) * i
        path.lineTo(x, rect.bottom + Random.nextFloat() * jitter)
    }
    // Left
    for (i in 1..steps) {
        val y = rect.bottom - (rect.height / steps) * i
        path.lineTo(rect.left + Random.nextFloat() * jitter, y)
    }
    path.close()
    return path
}

@Preview(device = "id:pixel_7", showBackground = true)
@Composable
fun GameScreenThemesPreview() {
    var themeIndex by remember { mutableStateOf(0) }
    val themes = HandTheme.allThemes
    val currentTheme = themes[themeIndex]
    
    Column(modifier = Modifier.fillMaxSize().background(currentTheme.backgroundColor)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
            themes.forEachIndexed { index, theme ->
                Button(
                    onClick = { themeIndex = index },
                    modifier = Modifier.padding(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                ) {
                    Text(theme.name.take(1), color = if (theme.isDark) Color.Black else Color.White)
                }
            }
        }
        
        Text("Left vs Right Hand (Anatomy Check)", color = currentTheme.textColor, modifier = Modifier.padding(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            ThemedHand(
                fingers = 5,
                maxFingers = 5,
                isDead = false,
                isInvalid = false,
                theme = currentTheme,
                isLeftHand = true,
                modifier = Modifier.weight(1f)
            )
            ThemedHand(
                fingers = 5,
                maxFingers = 5,
                isDead = false,
                isInvalid = false,
                theme = currentTheme,
                isLeftHand = false,
                modifier = Modifier.weight(1f)
            )
        }
        
        Text("Finger Counts (Index -> All)", color = currentTheme.textColor, modifier = Modifier.padding(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { i ->
                ThemedHand(
                    fingers = i + 1,
                    maxFingers = 5,
                    isDead = false,
                    isInvalid = false,
                    theme = currentTheme,
                    isLeftHand = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("10-Finger Scaling Check", color = currentTheme.textColor, modifier = Modifier.padding(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { i ->
                val fingerCount = (i + 1) * 2
                ThemedHand(
                    fingers = fingerCount,
                    maxFingers = 10,
                    isDead = false,
                    isInvalid = false,
                    theme = currentTheme,
                    isLeftHand = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

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
