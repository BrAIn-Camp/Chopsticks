package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.model.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIStrictRuleTest {

    @Test
    fun `AI never performs live-to-live transfers when allowTransfers is false`() {
        // Setup state where both hands are live, so any transfer would be a live-to-live transfer.
        val ruleset = RulesetConfig(allowTransfers = false, allowDivisions = true)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(2)),
            player2 = Player("Player 2", leftHand = Hand(3), rightHand = Hand(1)),
            currentPlayerId = "Player 2",
            ruleset = ruleset
        )

        // Generate possible moves for both Easy and Hard difficulty or via getPossibleMoves directly
        val possibleMoves = ChopsticksAI.getPossibleMoves(state)

        // Assert that NO Move.Transfer is generated because live-to-live transfers are disabled
        val transfers = possibleMoves.filterIsInstance<Move.Transfer>()
        assertTrue("Should not have any transfers when allowTransfers is false", transfers.isEmpty())
    }

    @Test
    fun `AI never performs divisions when allowDivisions is false`() {
        // Setup state where one hand is dead, so any transfer to that hand would be a division/revival.
        val ruleset = RulesetConfig(allowTransfers = true, allowDivisions = false)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(2)),
            player2 = Player("Player 2", leftHand = Hand(4), rightHand = Hand(0)), // right hand is dead
            currentPlayerId = "Player 2",
            ruleset = ruleset
        )

        val possibleMoves = ChopsticksAI.getPossibleMoves(state)

        // Any transfer from left to right would be a division. Let's make sure none are allowed.
        val divisions = possibleMoves.filterIsInstance<Move.Transfer>().filter { it.toHandIndex == 1 }
        assertTrue("Should not have any divisions to dead hand when allowDivisions is false", divisions.isEmpty())
    }

    @Test
    fun `AI never performs suicide when allowSuicide is false`() {
        // Setup state where transferring 2 fingers from a hand with 2 fingers would kill it (leave it with 0).
        val ruleset = RulesetConfig(allowTransfers = true, allowSuicide = false)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(2)),
            player2 = Player("Player 2", leftHand = Hand(2), rightHand = Hand(1)),
            currentPlayerId = "Player 2",
            ruleset = ruleset
        )

        val possibleMoves = ChopsticksAI.getPossibleMoves(state)

        // Check if any transfer results in the source hand becoming dead (amount == fromHand.fingers)
        val suicideMoves = possibleMoves.filterIsInstance<Move.Transfer>().filter { it.amount == 2 && it.fromHandIndex == 0 }
        assertTrue("Should not have suicide moves when allowSuicide is false", suicideMoves.isEmpty())
    }

    @Test
    fun `AI never exceeds 5 in exactFive mode`() {
        val ruleset = RulesetConfig(exactFive = true, overflow = false)
        // Player 2 left hand has 3 fingers. Opponent has 3 and 4 fingers.
        // 3 + 3 = 6 (>5), 3 + 4 = 7 (>5). These attacks should be illegal.
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(4)),
            player2 = Player("Player 2", leftHand = Hand(3), rightHand = Hand(0)),
            currentPlayerId = "Player 2",
            ruleset = ruleset
        )

        val possibleMoves = ChopsticksAI.getPossibleMoves(state)
        val attacks = possibleMoves.filterIsInstance<Move.Attack>()
        assertTrue("Should not have any illegal attacks exceeding 5 in exactFive mode", attacks.isEmpty())
    }
}
