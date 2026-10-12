package com.example.chopsticks.domain.engine

import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Player
import com.example.chopsticks.domain.model.RulesetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WikipediaRulesTest {

    private val baseRules = RulesetConfig(
        overflow = false,
        exactFive = false,
        allowDivisions = true,
        allowTransfers = true,
        allowSuicide = true,
        isMisere = false,
        allowSwaps = true,
        allowExchangeLife = true,
        allowCondense = true,
        onlyEvenSplits = false,
        maxFingers = 5
    )

    // 1. Logan Clause (Exchange Life)
    @Test
    fun `Logan Clause - sacrifice live hand to revive dead one`() {
        val state = GameState(
            player1 = Player("P1", leftHand = Hand(3), rightHand = Hand(0)),
            currentPlayerId = "P1",
            ruleset = baseRules.copy(allowExchangeLife = true)
        )
        val nextState = RulesEngine.exchangeLife(state, 0)
        
        assertEquals("Left hand should be 0", 0, nextState.player1.leftHand.fingers)
        assertEquals("Right hand should be 3", 3, nextState.player1.rightHand.fingers)
        assertNull(nextState.errorMessage)
    }

    @Test
    fun `Logan Clause - blocked if allowExchangeLife is false`() {
        val state = GameState(
            player1 = Player("P1", leftHand = Hand(3), rightHand = Hand(0)),
            currentPlayerId = "P1",
            ruleset = baseRules.copy(allowExchangeLife = false)
        )
        val nextState = RulesEngine.exchangeLife(state, 0)
        
        assertNotNull(nextState.errorMessage)
        assertEquals(3, nextState.player1.leftHand.fingers)
    }

    // 2. Meta Rule (Condense)
    @Test
    fun `Meta Rule - condense works with various maxFingers`() {
        // maxFingers 5
        val state5 = GameState(
            player1 = Player("P1", leftHand = Hand(4), rightHand = Hand(4)),
            currentPlayerId = "P1",
            ruleset = baseRules.copy(allowCondense = true, maxFingers = 5)
        )
        val next5 = RulesEngine.condense(state5, 1, 2)
        assertEquals("Max 5: Left should be 1", 1, next5.player1.leftHand.fingers)
        assertEquals("Max 5: Right should be 2", 2, next5.player1.rightHand.fingers)

        // maxFingers 3
        val state3 = GameState(
            player1 = Player("P1", leftHand = Hand(2), rightHand = Hand(2)),
            currentPlayerId = "P1",
            ruleset = baseRules.copy(allowCondense = true, maxFingers = 3)
        )
        val next3 = RulesEngine.condense(state3, 0, 1)
        assertEquals("Max 3: Left should be 0", 0, next3.player1.leftHand.fingers)
        assertEquals("Max 3: Right should be 1", 1, next3.player1.rightHand.fingers)
    }

    @Test
    fun `Meta Rule - condense blocked if total fingers is less than or equal to maxFingers`() {
        val state = GameState(
            player1 = Player("P1", leftHand = Hand(2), rightHand = Hand(3)),
            currentPlayerId = "P1",
            ruleset = baseRules.copy(allowCondense = true, maxFingers = 5)
        )
        val nextState = RulesEngine.condense(state, 1, 0)
        assertNotNull(nextState.errorMessage)
    }

    // 3. Even Splits Only
    @Test
    fun `Even Splits Only - restrictions applied to transfers and divisions`() {
        val rules = baseRules.copy(onlyEvenSplits = true)
        
        // Odd source hand (3) -> blocked
        val stateOdd = GameState(
            player1 = Player("P1", leftHand = Hand(3), rightHand = Hand(1)),
            currentPlayerId = "P1",
            ruleset = rules
        )
        assertNotNull("Odd source blocked", RulesEngine.transfer(stateOdd, 0, 1, 1).errorMessage)
        
        // Non-equal split (4 -> 3, 1) -> blocked
        val stateNonEqual = GameState(
            player1 = Player("P1", leftHand = Hand(4), rightHand = Hand(0)),
            currentPlayerId = "P1",
            ruleset = rules
        )
        assertNotNull("Non-equal split blocked", RulesEngine.transfer(stateNonEqual, 0, 1, 1).errorMessage)
        
        // Equal split from even hand (4 -> 2, 2) -> allowed
        val stateValid = GameState(
            player1 = Player("P1", leftHand = Hand(4), rightHand = Hand(0)),
            currentPlayerId = "P1",
            ruleset = rules
        )
        assertNull("Equal split allowed", RulesEngine.transfer(stateValid, 0, 1, 2).errorMessage)
    }

    // 4. Misere Mode
    @Test
    fun `Misere Mode - win condition correctly reversed`() {
        val rules = baseRules.copy(isMisere = true, allowSuicide = true)
        
        // Lose hands to opponent -> Opponent wins
        val stateAttack = GameState(
            player1 = Player("P1", leftHand = Hand(4), rightHand = Hand(0)),
            player2 = Player("P2", leftHand = Hand(1), rightHand = Hand(0)),
            currentPlayerId = "P1",
            ruleset = rules
        )
        val nextAttack = RulesEngine.attack(stateAttack, 0, 0)
        assertEquals("Opponent should win in Misere if their hands die", "P2", nextAttack.winnerId)
        
        // Suicide -> Self wins
        val stateSuicide = GameState(
            player1 = Player("P1", leftHand = Hand(4), rightHand = Hand(1)),
            currentPlayerId = "P1",
            ruleset = rules
        )
        val nextSuicide = RulesEngine.transfer(stateSuicide, 0, 1, 4)
        assertEquals("Self should win in Misere if self hands die", "P1", nextSuicide.winnerId)
    }

    // 5. Identity & Swap Rules
    @Test
    fun `Identity & Swap Rules - blocked or allowed based on constraints`() {
        // Swap (3, 1) -> (1, 3) blocked when allowSwaps is false
        val stateSwapBlocked = GameState(
            player1 = Player("P1", leftHand = Hand(3), rightHand = Hand(1)),
            currentPlayerId = "P1",
            ruleset = baseRules.copy(allowSwaps = false)
        )
        assertNotNull("Swap should be blocked", RulesEngine.transfer(stateSwapBlocked, 0, 1, 2).errorMessage)
        
        // Swap allowed
        val stateSwapAllowed = GameState(
            player1 = Player("P1", leftHand = Hand(3), rightHand = Hand(1)),
            currentPlayerId = "P1",
            ruleset = baseRules.copy(allowSwaps = true)
        )
        assertNull("Swap should be allowed", RulesEngine.transfer(stateSwapAllowed, 0, 1, 2).errorMessage)
    }

    // 6. Variable Capacity
    @Test
    fun `Variable Capacity - attack and overflow work with different limits`() {
        // Max 3, Overflow ON
        val rules3 = baseRules.copy(maxFingers = 3, overflow = true)
        val state3 = GameState(
            player1 = Player("P1", leftHand = Hand(2), rightHand = Hand(0)),
            player2 = Player("P2", leftHand = Hand(2), rightHand = Hand(0)),
            currentPlayerId = "P1",
            ruleset = rules3
        )
        // 2 + 2 = 4. 4 % 3 = 1.
        val next3 = RulesEngine.attack(state3, 0, 0)
        assertEquals("2+2 mod 3 should be 1", 1, next3.player2.leftHand.fingers)
        
        // Max 10, Overflow ON
        val rules10 = baseRules.copy(maxFingers = 10, overflow = true)
        val state10 = GameState(
            player1 = Player("P1", leftHand = Hand(6), rightHand = Hand(0)),
            player2 = Player("P2", leftHand = Hand(7), rightHand = Hand(0)),
            currentPlayerId = "P1",
            ruleset = rules10
        )
        // 6 + 7 = 13. 13 % 10 = 3.
        val next10 = RulesEngine.attack(state10, 0, 0)
        assertEquals("6+7 mod 10 should be 3", 3, next10.player2.leftHand.fingers)
    }
}
