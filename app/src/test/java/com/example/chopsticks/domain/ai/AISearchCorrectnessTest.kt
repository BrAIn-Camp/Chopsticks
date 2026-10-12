package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Player
import com.example.chopsticks.domain.model.RulesetConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * The AI's search uses alpha-beta pruning and a memo table to go fast.
 * Those shortcuts must never change the answer, so every move score is checked
 * against a plain minimax that looks at every line.
 */
class AISearchCorrectnessTest {

    private val rulesets = listOf(
        RulesetConfig(),
        RulesetConfig(overflow = false),
        RulesetConfig(allowTransfers = true, onlyEvenSplits = false),
        RulesetConfig(allowTransfers = true, onlyEvenSplits = false, allowSuicide = true, allowSwaps = true),
        RulesetConfig(overflow = false, exactFive = true, onlyEvenSplits = false),
        RulesetConfig(isMisere = true, allowTransfers = true, onlyEvenSplits = false)
    )

    @Test
    fun `pruned search scores every move the same as a full search`() {
        val depth = 5
        val random = Random(1234)
        for (ruleset in rulesets) {
            for (position in samplePositions(ruleset, random, count = 40)) {
                val fast = ChopsticksAI.scoreMoves(position, depth)
                for ((move, score) in fast) {
                    val expected = fullMinimax(
                        ChopsticksAI.applyMove(position, move),
                        depth - 1,
                        maximizing = false,
                        originalPlayerId = position.currentPlayerId
                    )
                    assertEquals("Score for $move in $position", expected, score)
                }
            }
        }
    }

    /** Positions reached by random play from the opening, so they are all reachable in a real game. */
    private fun samplePositions(ruleset: RulesetConfig, random: Random, count: Int): List<GameState> {
        val positions = mutableListOf<GameState>()
        while (positions.size < count) {
            var state = GameState(player1 = Player("Player 1"), player2 = Player("Player 2"), ruleset = ruleset)
            repeat(random.nextInt(0, 12)) {
                if (state.winnerId != null) return@repeat
                val moves = ChopsticksAI.getPossibleMoves(state)
                if (moves.isEmpty()) return@repeat
                state = ChopsticksAI.applyMove(state, moves[random.nextInt(moves.size)])
            }
            if (state.winnerId == null && ChopsticksAI.getPossibleMoves(state).isNotEmpty()) {
                positions.add(state)
            }
        }
        return positions
    }

    /** Reference search with no pruning and no memo: slow, but obviously correct. */
    private fun fullMinimax(state: GameState, depth: Int, maximizing: Boolean, originalPlayerId: String): Int {
        state.winnerId?.let { winner ->
            return if (winner == originalPlayerId) 1_000_000 + depth else -1_000_000 - depth
        }
        if (depth == 0) return ChopsticksAI.evaluate(state, originalPlayerId)
        val moves = ChopsticksAI.getPossibleMoves(state)
        if (moves.isEmpty()) return 0
        val scores = moves.map { fullMinimax(ChopsticksAI.applyMove(state, it), depth - 1, !maximizing, originalPlayerId) }
        return if (maximizing) scores.max() else scores.min()
    }
}
