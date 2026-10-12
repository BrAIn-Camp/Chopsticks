package com.example.chopsticks.ui.game

import com.example.chopsticks.ui.ArtisticActionButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chopsticks.domain.ai.Move
import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.GameMode
import com.example.chopsticks.ui.theme.*

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

    // Which analysed move is shown as an arrow on the board; reset whenever a new analysis arrives
    var previewMove by remember(hintResult) { mutableStateOf<Move?>(null) }

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

        // Arrow for the move being previewed from the hint analysis
        if (hintResult != null) {
            previewMove?.let { move ->
                HintMoveOverlay(
                    move = move,
                    handBounds = handBounds,
                    theme = theme
                )
            }
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

        // Full analysis of every move, or a compact bar while one is previewed on the board
        hintResult?.let { analysis ->
            val preview = previewMove?.let { move -> analysis.moves.find { it.move == move } }
            if (preview != null) {
                HintPreviewBar(
                    move = preview,
                    theme = theme,
                    onBack = { previewMove = null },
                    onPlay = { viewModel.playHintMove(preview.move) },
                    onClose = { viewModel.clearHint() }
                )
            } else {
                HintPanel(
                    analysis = analysis,
                    theme = theme,
                    onShowOnBoard = { previewMove = it },
                    onPlay = { viewModel.playHintMove(it) },
                    onClose = { viewModel.clearHint() }
                )
            }
        }
    }
}
