package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.Difficulty
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Player
import com.example.chopsticks.domain.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class HintCategory {
    OFFENSIVE, DEFENSIVE, SETUP, VICTORY
}

data class HintResult(
    val move: Move,
    val mateInSteps: Int?,
    val explanation: String,
    val category: HintCategory
)

object ChopsticksAI {

    private const val WIN_SCORE = 1000000
    private const val DRAW_SCORE = 0

    suspend fun chooseMove(state: GameState, difficulty: Difficulty): Move? = withContext(Dispatchers.Default) {
        val possibleMoves = getPossibleMoves(state)
        if (possibleMoves.isEmpty()) return@withContext null

        when (difficulty) {
            Difficulty.NOVICE -> possibleMoves.random()
            Difficulty.EASY -> getBestMoveMinimax(state, depth = 1)
            Difficulty.MEDIUM -> getBestMoveMinimax(state, depth = 2)
            Difficulty.HARD -> getBestMoveMinimax(state, depth = 4)
            Difficulty.PERFECT -> getBestMoveMinimax(state, depth = 12)
        }
    }

    suspend fun getHint(state: GameState): HintResult? = withContext(Dispatchers.Default) {
        val depth = 12
        val currentPlayerId = state.currentPlayerId
        val (move, bestScore) = scoreMoves(state, depth).maxByOrNull { it.second } ?: return@withContext null

        val nextState = applyMove(state, move)
        val opponentId = state.getOpponentPlayer().id

        // Determine category and explanation
        val mateInSteps = if (bestScore >= WIN_SCORE) {
            depth - (bestScore - WIN_SCORE)
        } else null

        val category: HintCategory
        if (mateInSteps != null || nextState.winnerId == currentPlayerId) {
            category = HintCategory.VICTORY
        } else {
            // Check for immediate threats in current state
            val opponentMoves = getPossibleMoves(state.copy(currentPlayerId = opponentId))
            var isThreatened = false
            for (om in opponentMoves) {
                val threatState = applyMove(state.copy(currentPlayerId = opponentId), om)
                if (threatState.getPlayer(currentPlayerId).isDefeated(state.ruleset.maxFingers, state.ruleset.exactFive)) {
                    isThreatened = true
                    break
                }
            }

            // Check if move prevents a loss next turn
            val nextOpponentMoves = getPossibleMoves(nextState)
            var canOpponentKillNext = false
            for (om in nextOpponentMoves) {
                val s = applyMove(nextState, om)
                if (s.winnerId == opponentId) {
                    canOpponentKillNext = true
                    break
                }
            }

            // Simplified offensive check: Did we kill a hand?
            val opponent = state.getOpponentPlayer()
            val nextOpponent = nextState.getPlayer(opponentId)
            val killedHand = (opponent.leftHand.fingers > 0 && nextOpponent.leftHand.fingers == 0) ||
                             (opponent.rightHand.fingers > 0 && nextOpponent.rightHand.fingers == 0)

            if (killedHand) {
                category = HintCategory.OFFENSIVE
            } else if (isThreatened && !canOpponentKillNext) {
                category = HintCategory.DEFENSIVE
            } else {
                category = HintCategory.SETUP
            }
        }

        val explanation = generateStrategicExplanation(state, nextState, move, mateInSteps, category)
        HintResult(move, mateInSteps, explanation, category)
    }

    private fun generateStrategicExplanation(
        state: GameState,
        nextState: GameState,
        move: Move,
        mateInSteps: Int?,
        category: HintCategory
    ): String {
        val currentPlayer = state.getCurrentPlayer()
        val nextPlayer = nextState.getPlayer(state.currentPlayerId)
        val opponent = state.getOpponentPlayer()
        val nextOpponent = nextState.getPlayer(opponent.id)
        val maxFingers = state.ruleset.maxFingers
        val currentPlayerId = state.currentPlayerId

        val curL = currentPlayer.leftHand.fingers
        val curR = currentPlayer.rightHand.fingers
        val nextL = nextPlayer.leftHand.fingers
        val nextR = nextPlayer.rightHand.fingers

        val oppL = opponent.leftHand.fingers
        val oppR = opponent.rightHand.fingers

        val futureAdvantage = "This secures a stronger position for the next 3 turns by limiting the opponent's ability to reach the limit."

        return when (category) {
            HintCategory.VICTORY -> {
                if (nextState.winnerId == currentPlayerId) {
                    "This move wins the game immediately! By removing their final hand, you've secured total dominance and finished the mathematical sequence."
                } else {
                    "This move begins a ${mateInSteps}-step sequence where every counter-move by the opponent leads to their defeat. You are mathematically forcing a win by controlling the finger parity."
                }
            }
            HintCategory.OFFENSIVE -> {
                val killedHand = if (opponent.leftHand.fingers > 0 && nextOpponent.leftHand.fingers == 0) "left" else "right"
                
                if (move is Move.Attack) {
                    val attackingFingers = if (move.fromHandIndex == 0) curL else curR
                    val targetFingers = if (move.toHandIndex == 0) oppL else oppR
                    val resultFingers = if (move.toHandIndex == 0) nextOpponent.leftHand.fingers else nextOpponent.rightHand.fingers
                    
                    if (resultFingers == 0) {
                        "By removing their $killedHand hand using your $attackingFingers-finger hand, you cut their attacking power in half and force them into a more predictable strategy."
                    } else if (state.ruleset.overflow && resultFingers < targetFingers) {
                        "By attacking with $attackingFingers fingers, you force the opponent's hand to overflow back to $resultFingers, resetting their offensive pressure and delaying their victory path."
                    } else {
                        "Attacking with $attackingFingers fingers increases their $killedHand hand to $resultFingers, pushing them closer to the $maxFingers-finger limit and destabilizing their formation."
                    }
                } else {
                    "This move improves your offensive capability, putting you in a position to eliminate the opponent's $killedHand hand in the coming turns."
                }
            }
            HintCategory.DEFENSIVE -> {
                val threatenedHandIndex = findThreatenedHandIndex(state)
                val threatenedHandName = if (threatenedHandIndex == 0) "left" else "right"
                val threatSourceIndex = findThreatSourceIndex(state, threatenedHandIndex)
                val threatSourceName = if (threatSourceIndex == 0) "left" else "right"
                val threatFingers = if (threatSourceIndex == 0) oppL else oppR

                val baseDef = "Your $threatenedHandName hand was at risk of being killed by the opponent's $threatFingers-finger $threatSourceName hand. This move neutralizes that threat."
                
                if (move is Move.Transfer || move is Move.Condense) {
                    "$baseDef By shifting your state from [$curL, $curR] to [$nextL, $nextR], you create balanced protection and move your vulnerable hand out of their reach."
                } else {
                    "$baseDef $futureAdvantage"
                }
            }
            HintCategory.SETUP -> {
                val moveDetail = when (move) {
                    is Move.Transfer -> "shifting your state from [$curL, $curR] to [$nextL, $nextR]"
                    is Move.Condense -> "condensing your hands from [$curL, $curR] to [$nextL, $nextR]"
                    is Move.ExchangeLife -> {
                        val revivedHand = if (nextPlayer.leftHand.fingers > 0 && currentPlayer.leftHand.fingers == 0) "left" else "right"
                        val revivedFingers = if (revivedHand == "left") nextL else nextR
                        "this revival brings your $revivedHand hand back with $revivedFingers fingers, forcing the opponent to split their focus and giving you a numerical advantage."
                    }
                    else -> "improving your position to a state of [$nextL, $nextR]"
                }

                val tacticalLogic = if (nextL == nextR && nextL > 0) {
                    "creating balanced protection against the opponent's ${maxOf(oppL, oppR)}-finger hand."
                } else if (nextL + nextR > curL + curR) {
                    "increasing your total finger count to maintain mathematical flexibility."
                } else {
                    "limiting the opponent's counter-attack options while preparing for an offensive push."
                }

                "By $moveDetail, you are $tacticalLogic $futureAdvantage"
            }
        }
    }

    private fun findThreatenedHandIndex(state: GameState): Int {
        val currentPlayer = state.getCurrentPlayer()
        val opponent = state.getOpponentPlayer()
        val maxFingers = state.ruleset.maxFingers
        val exactFive = state.ruleset.exactFive
        
        for (ph in 0..1) {
            val playerHand = if (ph == 0) currentPlayer.leftHand else currentPlayer.rightHand
            if (playerHand.isDead(maxFingers, exactFive)) continue
            
            for (oh in 0..1) {
                val opponentHand = if (oh == 0) opponent.leftHand else opponent.rightHand
                if (opponentHand.isDead(maxFingers, exactFive)) continue
                
                val total = playerHand.fingers + opponentHand.fingers
                if (state.ruleset.overflow) {
                    if (total % maxFingers == 0) return ph
                } else {
                    if (exactFive) {
                        if (total == maxFingers) return ph
                    } else if (total >= maxFingers) {
                        return ph
                    }
                }
            }
        }
        return 0 // Fallback
    }

    private fun findThreatSourceIndex(state: GameState, threatenedHandIndex: Int): Int {
        val currentPlayer = state.getCurrentPlayer()
        val opponent = state.getOpponentPlayer()
        val maxFingers = state.ruleset.maxFingers
        val exactFive = state.ruleset.exactFive
        
        val targetHand = if (threatenedHandIndex == 0) currentPlayer.leftHand else currentPlayer.rightHand
        
        for (oh in 0..1) {
            val opponentHand = if (oh == 0) opponent.leftHand else opponent.rightHand
            if (opponentHand.isDead(maxFingers, exactFive)) continue
            
            val total = targetHand.fingers + opponentHand.fingers
            if (state.ruleset.overflow) {
                if (total % maxFingers == 0) return oh
            } else {
                if (exactFive) {
                    if (total == maxFingers) return oh
                } else if (total >= maxFingers) {
                    return oh
                }
            }
        }
        return 0 // Fallback
    }

    /**
     * Scores every legal move for the current player by searching [depth] plies ahead.
     * Higher is better for the player to move.
     */
    internal fun scoreMoves(state: GameState, depth: Int): List<Pair<Move, Int>> {
        val search = Search(state.currentPlayerId)
        return getPossibleMoves(state).map { move ->
            move to search.minimax(applyMove(state, move), depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false)
        }
    }

    private fun getBestMoveMinimax(state: GameState, depth: Int): Move {
        val scored = scoreMoves(state, depth)
        val bestScore = scored.maxOf { it.second }
        return scored.filter { it.second == bestScore }.random().first
    }

    /**
     * Whether a remembered score is exact, or only a limit because the search was cut short
     * once it knew the result was too good or too bad to matter.
     */
    private enum class Bound { EXACT, AT_LEAST, AT_MOST }

    private class TableEntry(val score: Int, val bound: Bound)

    /** One search from one player's point of view, with its own memo table. */
    private class Search(private val originalPlayerId: String) {
        private val table = HashMap<Pair<Position, Int>, TableEntry>()

        fun minimax(state: GameState, depth: Int, alpha: Int, beta: Int, maximizingPlayer: Boolean): Int {
            state.winnerId?.let { winner ->
                return if (winner == originalPlayerId) WIN_SCORE + depth else -WIN_SCORE - depth
            }
            if (depth == 0) return evaluate(state, originalPlayerId)

            val key = state.position to depth
            var currentAlpha = alpha
            var currentBeta = beta
            table[key]?.let { entry ->
                when (entry.bound) {
                    Bound.EXACT -> return entry.score
                    Bound.AT_LEAST -> currentAlpha = maxOf(currentAlpha, entry.score)
                    Bound.AT_MOST -> currentBeta = minOf(currentBeta, entry.score)
                }
                if (currentAlpha >= currentBeta) return entry.score
            }

            val moves = getPossibleMoves(state)
            if (moves.isEmpty()) return DRAW_SCORE

            val windowAlpha = currentAlpha
            val windowBeta = currentBeta
            var best = if (maximizingPlayer) Int.MIN_VALUE else Int.MAX_VALUE
            for (move in moves) {
                val eval = minimax(applyMove(state, move), depth - 1, currentAlpha, currentBeta, !maximizingPlayer)
                if (maximizingPlayer) {
                    best = maxOf(best, eval)
                    currentAlpha = maxOf(currentAlpha, eval)
                } else {
                    best = minOf(best, eval)
                    currentBeta = minOf(currentBeta, eval)
                }
                if (currentBeta <= currentAlpha) break
            }

            val bound = when {
                best <= windowAlpha -> Bound.AT_MOST
                best >= windowBeta -> Bound.AT_LEAST
                else -> Bound.EXACT
            }
            table[key] = TableEntry(best, bound)
            return best
        }
    }

    internal fun evaluate(state: GameState, originalPlayerId: String): Int {
        val currentPlayer = if (state.player1.id == originalPlayerId) state.player1 else state.player2
        val opponent = if (state.player1.id == originalPlayerId) state.player2 else state.player1
        
        var score = 0
        val maxFingers = state.ruleset.maxFingers
        val exactFive = state.ruleset.exactFive
        
        // 1. Hand status
        if (opponent.leftHand.isDead(maxFingers, exactFive)) score += 500
        if (opponent.rightHand.isDead(maxFingers, exactFive)) score += 500
        
        if (currentPlayer.leftHand.isDead(maxFingers, exactFive)) score -= 600
        if (currentPlayer.rightHand.isDead(maxFingers, exactFive)) score -= 600
        
        // 2. Finger count heuristics
        score += evaluateHand(currentPlayer.leftHand, maxFingers)
        score += evaluateHand(currentPlayer.rightHand, maxFingers)
        score -= evaluateHand(opponent.leftHand, maxFingers)
        score -= evaluateHand(opponent.rightHand, maxFingers)

        // If Misere mode, flip the scoring goal
        if (state.ruleset.isMisere) {
            score = -score
        }

        return score
    }

    private fun evaluateHand(hand: Hand, maxFingers: Int): Int {
        if (hand.fingers == 0) return 0
        return when (hand.fingers) {
            1 -> 50 // Safe
            maxFingers - 1 -> 10 // Vulnerable
            else -> 30
        }
    }

    fun getPossibleMoves(state: GameState): List<Move> = RulesEngine.legalMoves(state)

    internal fun applyMove(state: GameState, move: Move): GameState = RulesEngine.applyMove(state, move)

    private fun GameState.getPlayer(id: String): Player = if (player1.id == id) player1 else player2
}
