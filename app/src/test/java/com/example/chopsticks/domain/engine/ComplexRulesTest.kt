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

class ComplexRulesTest {

    @Test
    fun `Exact Five and Overflow interaction - hitting 5 kills`() {
        val rules = RulesetConfig(exactFive = true, overflow = true)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2)),
            player2 = Player("Player 2", leftHand = Hand(3)),
            ruleset = rules,
            currentPlayerId = "Player 1"
        )
        // 2 + 3 = 5. In Rollover, 5 % 5 = 0. In Exact Five, 5 is dead.
        val nextState = RulesEngine.attack(state, 0, 0)
        
        assertEquals(0, nextState.player2.leftHand.fingers)
        assertTrue(nextState.player2.leftHand.isDead(5, true))
    }

    @Test
    fun `Exact Five and Overflow interaction - hitting 6 rolls over`() {
        val rules = RulesetConfig(exactFive = true, overflow = true)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3)),
            player2 = Player("Player 2", leftHand = Hand(3)),
            ruleset = rules,
            currentPlayerId = "Player 1"
        )
        // 3 + 3 = 6. In Rollover, 6 % 5 = 1.
        val nextState = RulesEngine.attack(state, 0, 0)
        
        assertEquals(1, nextState.player2.leftHand.fingers)
        assertFalse(nextState.player2.leftHand.isDead(5, true))
    }

    @Test
    fun `Suicide and Exact Five - split to hit exactly 5 on self`() {
        val rules = RulesetConfig(exactFive = true, allowSuicide = true, allowDivisions = true, allowTransfers = true, onlyEvenSplits = false, overflow = false)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(2)),
            ruleset = rules,
            currentPlayerId = "Player 1"
        )
        // Transfer 2 from left to right: (1, 4).
        val nextState = RulesEngine.transfer(state, 0, 1, 2)
        assertEquals(1, nextState.player1.leftHand.fingers)
        assertEquals(4, nextState.player1.rightHand.fingers)

        // Transfer 3 from left to right: (0, 5).
        val suicideState = RulesEngine.transfer(state, 0, 1, 3)
        assertEquals(0, suicideState.player1.leftHand.fingers)
        assertEquals(5, suicideState.player1.rightHand.fingers)
        assertTrue(suicideState.player1.leftHand.isDead(5, true))
        assertTrue(suicideState.player1.rightHand.isDead(5, true))
        assertEquals("Player 2", suicideState.winnerId)
    }

    @Test
    fun `Divisions and Overflow - revive a hand hit by overflow`() {
        val rules = RulesetConfig(overflow = true, allowDivisions = true)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2), rightHand = Hand(0)),
            player2 = Player("Player 2"),
            ruleset = rules,
            currentPlayerId = "Player 1"
        )
        // Divide 1 to get (1, 1)
        val nextState = RulesEngine.transfer(state, 0, 1, 1)
        assertEquals(1, nextState.player1.leftHand.fingers)
        assertEquals(1, nextState.player1.rightHand.fingers)
        assertFalse(nextState.player1.rightHand.isDead(maxFingers = 5))
    }

    @Test
    fun `All rules ON interaction`() {
        val rules = RulesetConfig(
            overflow = true,
            exactFive = true,
            allowSuicide = true,
            allowDivisions = true,
            allowTransfers = true,
            onlyEvenSplits = false
        )
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(4)),
            player2 = Player("Player 2", leftHand = Hand(4), rightHand = Hand(4)),
            ruleset = rules,
            currentPlayerId = "Player 1"
        )
        
        // Attack 4+4 = 8 -> 3.
        val s1 = RulesEngine.attack(state, 0, 0)
        assertEquals(3, s1.player2.leftHand.fingers)
        
        // Transfer 4 from 4 to 3 -> (0, 7) -> (0, 2)
        // S1 turn is P2. P2 hands are (3, 4). Transfer 4 from right to left.
        val s2 = RulesEngine.transfer(s1, 1, 0, 4)
        assertEquals(2, s2.player2.leftHand.fingers) // 3 + 4 = 7 -> 2
        assertEquals(0, s2.player2.rightHand.fingers)
        assertTrue(s2.player2.rightHand.isDead(5, true))
    }

    @Test
    fun `All rules OFF interaction`() {
        val rules = RulesetConfig(
            overflow = false,
            exactFive = false,
            allowSuicide = false,
            allowDivisions = false,
            allowTransfers = false
        )
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(2)),
            player2 = Player("Player 2", leftHand = Hand(3), rightHand = Hand(0)),
            ruleset = rules,
            currentPlayerId = "Player 1"
        )
        
        // Attack 3+3 = 6. Rollover OFF, Exact Five OFF -> Dies (0)
        val s1 = RulesEngine.attack(state, 0, 0)
        assertEquals(0, s1.player2.leftHand.fingers)
        assertTrue(s1.player2.leftHand.isDead(maxFingers = 5))
        
        // Transfer 1 from 3 to 2 -> (2, 3). Live to Live. Transfers OFF -> Illegal.
        val s2 = RulesEngine.transfer(state, 0, 1, 1)
        assertNotNull(s2.errorMessage)
        
        // Division to revive P2 right hand -> Illegal (Divisions OFF)
        val s3 = RulesEngine.transfer(state.copy(currentPlayerId = "Player 2"), 0, 1, 1)
        assertNotNull(s3.errorMessage)
    }

    @Test
    fun `Misere Mode - player whose hands are killed wins`() {
        val rules = RulesetConfig(isMisere = true)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4)),
            player2 = Player("Player 2", leftHand = Hand(1), rightHand = Hand(0)),
            ruleset = rules
        )
        // P1 attacks P2: 1 + 4 = 5 (Dead). P2 hands both killed. In Misere, P2 wins.
        val nextState = RulesEngine.attack(state, 0, 0)
        
        assertEquals("Player 2", nextState.winnerId)
    }

    @Test
    fun `Misere Mode - self suicide leads to victory`() {
        val rules = RulesetConfig(isMisere = true, allowSuicide = true, allowDivisions = true, allowTransfers = true, onlyEvenSplits = false, overflow = false)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(1), rightHand = Hand(4)),
            ruleset = rules
        )
        // Transfer 1 from left to right: (0, 5) -> Both dead. In Misere, P1 wins.
        val nextState = RulesEngine.transfer(state, 0, 1, 1)
        
        assertEquals("Player 1", nextState.winnerId)
    }

    @Test
    fun `Identity Change - swapping values is illegal when allowSwaps is false`() {
        val rules = RulesetConfig(allowSwaps = false, allowTransfers = true, onlyEvenSplits = false)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(3), rightHand = Hand(1)),
            ruleset = rules
        )
        // Transfer 2 from left to right: (1, 3). This is a swap.
        val nextState = RulesEngine.transfer(state, 0, 1, 2)
        assertNotNull(nextState.errorMessage)
        
        // If allowSwaps is true, it should be legal
        val rulesAllowed = rules.copy(allowSwaps = true)
        val stateAllowed = state.copy(ruleset = rulesAllowed)
        val nextStateAllowed = RulesEngine.transfer(stateAllowed, 0, 1, 2)
        assertNull(nextStateAllowed.errorMessage)
        assertEquals(1, nextStateAllowed.player1.leftHand.fingers)
        assertEquals(3, nextStateAllowed.player1.rightHand.fingers)
    }

    @Test
    fun `Variable Hand Capacity - logic uses maxFingers`() {
        val rules = RulesetConfig(maxFingers = 3, overflow = false)
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(2)),
            player2 = Player("Player 2", leftHand = Hand(1)),
            ruleset = rules
        )
        // 2 + 1 = 3. Since maxFingers is 3, it should die.
        val nextState = RulesEngine.attack(state, 0, 0)
        assertEquals(0, nextState.player2.leftHand.fingers)
        assertTrue(nextState.player2.leftHand.isDead(3, false))
    }
}
