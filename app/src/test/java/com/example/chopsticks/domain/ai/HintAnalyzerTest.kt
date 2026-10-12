package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Player
import com.example.chopsticks.domain.model.RulesetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HintAnalyzerTest {

    // Player 1 to move: You 2|3 · Them 3|1, cutoff rules
    private val state = GameState(
        player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(3)),
        player2 = Player("Player 2", leftHand = Hand(3), rightHand = Hand(1)),
        ruleset = RulesetConfig(overflow = false)
    )
    private val analysis = HintAnalyzer.analyze(state, GameSolver(state))

    @Test
    fun `lists every legal move exactly once`() {
        assertEquals(RulesEngine.legalMoves(state).toSet(), analysis.moves.map { it.move }.toSet())
        assertEquals(analysis.moves.size, analysis.moves.map { it.move }.distinct().size)
    }

    @Test
    fun `moves are sorted best first and the best ones match the solver`() {
        val solver = GameSolver(state)
        assertEquals(solver.bestMoves(state).toSet(), analysis.moves.filter { it.isBest }.map { it.move }.toSet())
        assertTrue(analysis.moves.first().isBest)
    }

    @Test
    fun `describes the board from the asker's point of view`() {
        assertEquals("You 2|3 · Them 3|1", analysis.hands)
        // Their 3 (left) + your 2 (left) = 5 knocks out your left hand, and so does their 3 onto your 2... etc.
        assertTrue(analysis.dangers.toString(), "Their 3 (left) → your 2 (left): 2 + 3 = 5" in analysis.dangers)
        assertTrue(analysis.chances.toString(), "Your 2 (left) → their 3 (left): 3 + 2 = 5" in analysis.chances)
    }

    @Test
    fun `explains the arithmetic of an attack`() {
        val knockout = analysis.moves.first { it.move == Move.Attack("Player 1", 0, 0) }
        assertEquals("Attack their 3 (left) with your 2 (left)", knockout.action)
        assertEquals("3 + 2 = 5: their left hand is knocked out", knockout.effect)
    }

    @Test
    fun `every line starts with its move and ends the way the verdict says`() {
        for (m in analysis.moves) {
            assertTrue(m.line.first().byYou)
            when (m.evaluation.outcome) {
                Outcome.WIN -> assertEquals("You win.", m.lineEnd)
                Outcome.LOSS -> assertEquals("They win.", m.lineEnd)
                Outcome.DRAW -> assertTrue(m.lineEnd.isNotEmpty())
            }
        }
    }

    @Test
    fun `player two gets the same view of their own position`() {
        val mirrored = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(1)),
            player2 = Player("Player 2", leftHand = Hand(2), rightHand = Hand(3)),
            currentPlayerId = "Player 2",
            ruleset = RulesetConfig(overflow = false)
        )
        val p2 = HintAnalyzer.analyze(mirrored, GameSolver(mirrored))
        assertEquals(analysis.hands, p2.hands)
        assertEquals(analysis.verdict, p2.verdict)
        assertEquals(analysis.moves.map { it.action }, p2.moves.map { it.action })
    }
}
