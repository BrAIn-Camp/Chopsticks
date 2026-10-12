package com.example.chopsticks.domain.engine

import com.example.chopsticks.domain.ai.Move
import com.example.chopsticks.domain.model.DrawReason
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Player
import com.example.chopsticks.domain.model.RulesetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawRulesTest {

    @Test
    fun `third repetition of a position is a draw`() {
        val state = GameState()
        assertNull(RulesEngine.detectDraw(state, timesPositionSeen = 1))
        assertNull(RulesEngine.detectDraw(state, timesPositionSeen = 2))
        assertEquals(DrawReason.REPETITION, RulesEngine.detectDraw(state, timesPositionSeen = 3))
    }

    @Test
    fun `player with no legal moves ends the game in a draw`() {
        // Cutoff + exact five, no splits of any kind: 4 + 4 overshoots, and the dead hand can't be revived.
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(0)),
            player2 = Player("Player 2", leftHand = Hand(4), rightHand = Hand(4)),
            ruleset = RulesetConfig(overflow = false, exactFive = true, allowDivisions = false, allowTransfers = false)
        )
        assertTrue(RulesEngine.legalMoves(state).isEmpty())
        assertEquals(DrawReason.NO_LEGAL_MOVES, RulesEngine.detectDraw(state, timesPositionSeen = 1))
    }

    @Test
    fun `finished games are never turned into draws`() {
        val won = GameState(winnerId = "Player 1")
        assertNull(RulesEngine.detectDraw(won, timesPositionSeen = 5))
    }

    @Test
    fun `no moves are allowed after a draw`() {
        val drawn = GameState(drawReason = DrawReason.REPETITION)
        assertTrue(drawn.isGameOver)
        assertFalse(RulesEngine.validateMove(drawn, Move.Attack("Player 1", 0, 0)))
        assertTrue(RulesEngine.legalMoves(drawn).isEmpty())
    }

    @Test
    fun `position ignores animation and message fields`() {
        val a = GameState()
        val b = a.copy(errorMessage = "x", lastMoveTimestamp = 99L, isMovePending = true)
        assertEquals(a.position, b.position)
        assertFalse(a.position == a.copy(currentPlayerId = "Player 2").position)
    }
}
