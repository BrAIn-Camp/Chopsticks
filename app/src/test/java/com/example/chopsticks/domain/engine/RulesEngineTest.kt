package com.example.chopsticks.domain.engine

import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Player
import com.example.chopsticks.domain.model.RulesetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesEngineTest {

    @Test
    fun `standard attack increases finger count`() {
        val state = GameState(ruleset = RulesetConfig(overflow = false))
        // Player 1 (1, 1) attacks Player 2 (1, 1) left hand
        val nextState = RulesEngine.attack(state, 0, 0)
        
        assertEquals(2, nextState.player2.leftHand.fingers)
        assertEquals("Player 2", nextState.currentPlayerId)
    }

    @Test
    fun `hand dies at 5 fingers in standard rules`() {
        var state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4)),
            player2 = Player("Player 2", leftHand = Hand(1)),
            ruleset = RulesetConfig(overflow = false)
        )
        // P1 attacks P2's left hand: 1 + 4 = 5 (Dead)
        state = RulesEngine.attack(state, 0, 0)
        
        assertEquals(0, state.player2.leftHand.fingers)
        assertTrue(state.player2.leftHand.isDead(maxFingers = 5))
    }


    @Test
    fun `overflow logic works`() {
        var state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3)),
            player2 = Player("Player 2", leftHand = Hand(3)),
            ruleset = RulesetConfig(overflow = true)
        )
        // 3 + 3 = 6. Rollover 5 = 1
        state = RulesEngine.attack(state, 0, 0)
        
        assertEquals(1, state.player2.leftHand.fingers)
    }

    @Test
    fun `exact five prevents attack exceeding five`() {
        var state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3)),
            player2 = Player("Player 2", leftHand = Hand(3)),
            ruleset = RulesetConfig(exactFive = true, overflow = false)
        )
        // 3 + 3 = 6. Illegal in Exact Five.
        state = RulesEngine.attack(state, 0, 0)
        
        assertNotNull(state.errorMessage)
        assertEquals(3, state.player2.leftHand.fingers)
    }

    @Test
    fun `transfer logic works`() {
        var state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(1)),
            ruleset = RulesetConfig(allowTransfers = true, onlyEvenSplits = false)
        )
        // Transfer 1 from left to right: (2, 2)
        state = RulesEngine.transfer(state, 0, 1, 1)
        
        assertEquals(2, state.player1.leftHand.fingers)
        assertEquals(2, state.player1.rightHand.fingers)
        assertEquals("Player 2", state.currentPlayerId)
    }

    @Test
    fun `suicide transfer allowed only in suicide rules`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(1)),
            ruleset = RulesetConfig(allowTransfers = true, onlyEvenSplits = false, allowSuicide = false)
        )
        // Transfer 4 from left to right: (0, 5) - Kills right hand. Illegal in Standard.
        val nextState = RulesEngine.transfer(state, 0, 1, 4)
        assertNotNull(nextState.errorMessage)

        val suicideState = state.copy(ruleset = state.ruleset.copy(allowSuicide = true))
        val suicideNext = RulesEngine.transfer(suicideState, 0, 1, 4)
        assertNull(suicideNext.errorMessage)
        assertTrue(suicideNext.player1.rightHand.isDead(maxFingers = 5))
    }

    @Test
    fun `transfer that kills the receiving hand counts as suicide`() {
        for (overflow in listOf(true, false)) {
            val state = GameState(
                player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(4)),
                ruleset = RulesetConfig(overflow = overflow, allowTransfers = true, onlyEvenSplits = false, allowSuicide = false)
            )
            // Moving 1 from left to right makes the right hand reach 5 and die: (2, 0)
            assertFalse("overflow=$overflow", RulesEngine.validateTransfer(state, 0, 1, 1))

            val suicideState = state.copy(ruleset = state.ruleset.copy(allowSuicide = true))
            val next = RulesEngine.transfer(suicideState, 0, 1, 1)
            assertNull(next.errorMessage)
            assertEquals(2, next.player1.leftHand.fingers)
            assertTrue(next.player1.rightHand.isDead(maxFingers = 5))
        }
    }

    @Test
    fun `winning the game`() {
        var state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4)),
            player2 = Player("Player 2", leftHand = Hand(1), rightHand = Hand(0)), // Right hand dead
            ruleset = RulesetConfig(overflow = false)
        )
        // P1 attacks P2's left hand: 1 + 4 = 5 (Dead). P2 has no hands left.
        state = RulesEngine.attack(state, 0, 0)
        
        assertEquals("Player 1", state.winnerId)
    }

    @Test
    fun `transfer fails when allowTransfers is false`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(2)),
            ruleset = RulesetConfig(allowTransfers = false)
        )
        // Transfer 1 from left to right: (1, 3). Target hand is live. Should fail.
        val nextState = RulesEngine.transfer(state, 0, 1, 1)
        assertNotNull(nextState.errorMessage)
        
        // Validation should also return false
        val isValid = RulesEngine.validateTransfer(state, 0, 1, 1)
        assertEquals(false, isValid)
    }

    @Test
    fun `division fails when allowDivisions is false`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(0)),
            ruleset = RulesetConfig(allowDivisions = false)
        )
        // Transfer 1 from left to right: (1, 1). Target hand is dead. Should fail.
        val nextState = RulesEngine.transfer(state, 0, 1, 1)
        assertNotNull(nextState.errorMessage)
        
        // Validation should also return false
        val isValid = RulesEngine.validateTransfer(state, 0, 1, 1)
        assertEquals(false, isValid)
    }

    @Test
    fun `transfer from dead hand is illegal`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(5), rightHand = Hand(1)),
            ruleset = RulesetConfig(exactFive = true, maxFingers = 5)
        )
        // Left hand is dead (5). Transfer 1 from Left to Right.
        val isValid = RulesEngine.validateTransfer(state, 0, 1, 1)
        assertEquals("Transfer from dead hand should be illegal", false, isValid)
    }

    @Test
    fun `misere mode victory condition`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4)),
            player2 = Player("Player 2", leftHand = Hand(1), rightHand = Hand(0)),
            ruleset = RulesetConfig(isMisere = true)
        )
        // P1 attacks P2's left hand: 1 + 4 = 5 (Dead). 
        // P2 has no hands left. In Misere, the one who loses all hands WINS.
        val nextState = RulesEngine.attack(state, 0, 0)
        
        assertEquals("Player 2", nextState.winnerId)
    }

    @Test
    fun `max fingers is respected`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(1)),
            player2 = Player("Player 2", leftHand = Hand(2)),
            ruleset = RulesetConfig(maxFingers = 3, overflow = false)
        )
        // P1 attacks P2: 1 + 2 = 3. Hand dies because maxFingers is 3.
        val nextState = RulesEngine.attack(state, 0, 0)
        
        assertEquals(0, nextState.player2.leftHand.fingers)
        assertTrue(nextState.player2.leftHand.isDead(maxFingers = 3))
    }

    @Test
    fun `identity and swap rules`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(3)),
            ruleset = RulesetConfig(allowSwaps = false, allowTransfers = true, onlyEvenSplits = false)
        )
        
        // Identity: (2, 3) -> (2, 3) is illegal
        val identityValid = RulesEngine.validateTransfer(state, 0, 1, 0) // amount 0 is illegal anyway
        assertEquals(false, identityValid)
        
        // Identity: Transfer 0 (amount validated)
        
        // Swap: (2, 3) -> (3, 2) is illegal when allowSwaps is false
        // To get (3, 2) from (2, 3), we'd need to transfer 1 from left to right? No.
        // Wait, how to swap? 
        // (2, 3) -> transfer 1 from right to left -> (3, 2).
        val swapValid = RulesEngine.validateTransfer(state, 1, 0, 1)
        assertEquals(false, swapValid)
        
        val stateWithSwaps = state.copy(ruleset = state.ruleset.copy(allowSwaps = true))
        val swapValidWithVariant = RulesEngine.validateTransfer(stateWithSwaps, 1, 0, 1)
        assertEquals(true, swapValidWithVariant)
    }

    @Test
    fun `exchange life (logan clause) logic works`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(0)),
            ruleset = RulesetConfig(allowExchangeLife = true)
        )
        // Sacrifice Left (3) to revive Right (0)
        val nextState = RulesEngine.exchangeLife(state, 0)

        assertEquals(0, nextState.player1.leftHand.fingers)
        assertEquals(3, nextState.player1.rightHand.fingers)
        assertEquals("Player 2", nextState.currentPlayerId)
    }

    @Test
    fun `condense hands (meta) logic works`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(4)),
            ruleset = RulesetConfig(allowCondense = true, maxFingers = 5)
        )
        // Total = 8. Limit = 5. Remainder = 3.
        // Condense to (1, 2)
        val nextState = RulesEngine.condense(state, 1, 2)

        assertEquals(1, nextState.player1.leftHand.fingers)
        assertEquals(2, nextState.player1.rightHand.fingers)
        assertEquals("Player 2", nextState.currentPlayerId)
        assertNull(nextState.errorMessage)
    }

    @Test
    fun `condense hands fails with invalid total`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(2)),
            ruleset = RulesetConfig(allowCondense = true, maxFingers = 5)
        )
        // Total = 4. Limit = 5. Total <= Limit. Condense should fail.
        val nextState = RulesEngine.condense(state, 1, 0)
        assertNotNull(nextState.errorMessage)
    }

    @Test
    fun `even splits only blocks odd splits and non-equal splits`() {
        val ruleset = RulesetConfig(onlyEvenSplits = true, allowTransfers = true)
        
        // 1. Odd source hand fingers (3 fingers) -> should fail
        val oddSourceState = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(1)),
            ruleset = ruleset
        )
        val oddIsValid = RulesEngine.validateTransfer(oddSourceState, 0, 1, 1)
        assertEquals(false, oddIsValid)
        
        // 2. Non-equal split from even hand -> should fail
        val nonEqualState = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(0)),
            ruleset = ruleset
        )
        val nonEqualIsValid = RulesEngine.validateTransfer(nonEqualState, 0, 1, 1)
        assertEquals(false, nonEqualIsValid)
        
        // 3. Equal split from even hand to dead hand -> should be valid
        val validSplitState = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(0)),
            ruleset = ruleset
        )
        val validIsValid = RulesEngine.validateTransfer(validSplitState, 0, 1, 2)
        assertEquals(true, validIsValid)
        
        // 4. Live-to-live split from even hand -> should be valid
        val liveToLiveState = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(2)),
            ruleset = ruleset
        )
        val liveToLiveIsValid = RulesEngine.validateTransfer(liveToLiveState, 0, 1, 1)
        assertEquals(true, liveToLiveIsValid)
    }

    @Test
    fun `logan clause sacrifice alive to revive dead`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(0)),
            ruleset = RulesetConfig(allowExchangeLife = true)
        )
        // Sacrifice Left (2) to revive Right (0)
        val nextState = RulesEngine.exchangeLife(state, 0)
        assertEquals(0, nextState.player1.leftHand.fingers)
        assertEquals(2, nextState.player1.rightHand.fingers)
    }

    @Test
    fun `meta rule condense when total exceeds capacity`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(6), rightHand = Hand(6)),
            ruleset = RulesetConfig(allowCondense = true, maxFingers = 10)
        )
        // Total = 12. Max = 10. Remainder = 2.
        // Valid splits: (0,2), (2,0), (1,1)
        assertTrue(RulesEngine.validateCondense(state, 1, 1))
        val nextState = RulesEngine.condense(state, 1, 1)
        assertEquals(1, nextState.player1.leftHand.fingers)
        assertEquals(1, nextState.player1.rightHand.fingers)
    }

    @Test
    fun `misere mode player who loses both hands wins`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(1), rightHand = Hand(0)),
            player2 = Player("Player 2", leftHand = Hand(4), rightHand = Hand(0)),
            ruleset = RulesetConfig(isMisere = true, maxFingers = 5)
        )
        // P2 attacks P1: 4 + 1 = 5 (P1 loses last hand). P1 should win.
        val nextState = RulesEngine.attack(state.copy(currentPlayerId = "Player 2"), 0, 0)
        assertEquals("Player 1", nextState.winnerId)
    }

    @Test
    fun `swap rule 3-1 to 1-3 is blocked when swaps not allowed`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(1)),
            ruleset = RulesetConfig(allowSwaps = false, allowTransfers = true, onlyEvenSplits = false)
        )
        // Transfer 2 from left to right: (3-2, 1+2) = (1, 3). This is a swap.
        val isValid = RulesEngine.validateTransfer(state, 0, 1, 2)
        assertEquals(false, isValid)
        
        val allowedState = state.copy(ruleset = state.ruleset.copy(allowSwaps = true))
        val isAllowedValid = RulesEngine.validateTransfer(allowedState, 0, 1, 2)
        assertEquals(true, isAllowedValid)
    }

    @Test
    fun `variable capacity max fingers 10`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(5)),
            player2 = Player("Player 2", leftHand = Hand(4)),
            ruleset = RulesetConfig(maxFingers = 10, overflow = false)
        )
        // Attack: 4 + 5 = 9. Hand is still alive because max is 10.
        var nextState = RulesEngine.attack(state, 0, 0)
        assertEquals(9, nextState.player2.leftHand.fingers)
        assertNull(nextState.winnerId)

        // Attack again: 9 + 5 = 14. Hand dies.
        nextState = RulesEngine.attack(nextState.copy(currentPlayerId = "Player 1"), 0, 0)
        assertEquals(0, nextState.player2.leftHand.fingers)
        assertTrue(nextState.player2.leftHand.isDead(maxFingers = 10))
    }

    @Test
    fun `variable capacity max fingers 3 with overflow`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2)),
            player2 = Player("Player 2", leftHand = Hand(2)),
            ruleset = RulesetConfig(maxFingers = 3, overflow = true)
        )
        // Attack: 2 + 2 = 4. 4 % 3 = 1.
        val nextState = RulesEngine.attack(state, 0, 0)
        assertEquals(1, nextState.player2.leftHand.fingers)
    }
}

