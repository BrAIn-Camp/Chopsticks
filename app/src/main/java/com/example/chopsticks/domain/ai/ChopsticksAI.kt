package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.Difficulty
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            // Exact play: fastest forced win, longest defence, or holding a draw
            Difficulty.PERFECT -> GameSolver(state).bestMoves(state).random()
        }
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
}
