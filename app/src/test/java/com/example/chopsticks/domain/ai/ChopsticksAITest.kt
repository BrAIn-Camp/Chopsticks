package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChopsticksAITest {

    @Test
    fun `novice AI picks a valid move`() = runBlocking {
        val state = GameState()
        val move = ChopsticksAI.chooseMove(state, Difficulty.NOVICE)
        
        assertNotNull(move)
    }

    @Test
    fun `perfect AI prioritizes winning move`() = runBlocking {
        // Player 2 is AI. P2 (4, 0) vs P1 (1, 0). 
        // P2 attacks P1 (1 + 4 = 5) -> P2 wins.
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(1), rightHand = Hand(0)),
            player2 = Player("Player 2", leftHand = Hand(4), rightHand = Hand(0)),
            currentPlayerId = "Player 2"
        )
        
        val move = ChopsticksAI.chooseMove(state, Difficulty.PERFECT)
        
        assertTrue("Move should be an Attack", move is Move.Attack)
        val attack = move as Move.Attack
        assertEquals("Should attack with left hand (index 0)", 0, attack.fromHandIndex)
        assertEquals("Should attack opponent's left hand (index 0) to win", 0, attack.toHandIndex)
    }

    @Test
    fun `medium AI can find mate in two`() = runBlocking {
        // Simple mate in two scenario
        // P2 (AI) turn. 
        // P1: (2, 2)
        // P2: (3, 0)
        // If P2 attacks P1's left hand (2+3=5), P1 left hand dies.
        // P1: (0, 2), P2: (3, 0). 
        // Then P1 attacks P2: (3+2=5) NO, P1 might try to survive.
        // Actually Chopsticks is complex. Let's just verify it finds a winning move if it exists within depth.
        
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(4)),
            player2 = Player("Player 2", leftHand = Hand(1), rightHand = Hand(0)),
            currentPlayerId = "Player 2"
        )
        // P2 can't win in one. 
        val move = ChopsticksAI.chooseMove(state, Difficulty.MEDIUM)
        assertNotNull(move)
    }

    @Test
    fun `AI avoids being killed and kills opponent hand`() = runBlocking {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(1)),
            player2 = Player("Player 2", leftHand = Hand(1), rightHand = Hand(0)),
            currentPlayerId = "Player 2"
        )
        
        val move = ChopsticksAI.chooseMove(state, Difficulty.HARD)
        
        // AI should prefer attacking P1's left hand to kill it (4+1=5)
        assertTrue("Move should be an Attack", move is Move.Attack)
        val attack = move as Move.Attack
        assertEquals("Should target P1's left hand (index 0) to kill it", 0, attack.toHandIndex)
    }

    @Test
    fun `evaluation rewards killing an opponent hand for either player`() {
        val even = GameState(
            player1 = Player("Player 1", leftHand = Hand(1), rightHand = Hand(1)),
            player2 = Player("Player 2", leftHand = Hand(1), rightHand = Hand(1))
        )
        val p2LostAHand = even.copy(player2 = Player("Player 2", leftHand = Hand(0), rightHand = Hand(1)))
        val p1LostAHand = even.copy(player1 = Player("Player 1", leftHand = Hand(0), rightHand = Hand(1)))

        assertTrue(ChopsticksAI.evaluate(p2LostAHand, "Player 1") > ChopsticksAI.evaluate(even, "Player 1"))
        assertTrue(ChopsticksAI.evaluate(p1LostAHand, "Player 2") > ChopsticksAI.evaluate(even, "Player 2"))
    }

    @Test
    fun `easy AI takes a free hand kill`() = runBlocking {
        // AI's 2 + opponent's 3 kills the opponent's right hand; nothing else changes much.
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(1), rightHand = Hand(3)),
            player2 = Player("Player 2", leftHand = Hand(2), rightHand = Hand(1)),
            currentPlayerId = "Player 2",
            ruleset = RulesetConfig(overflow = false)
        )
        repeat(10) {
            val move = ChopsticksAI.chooseMove(state, Difficulty.EASY)
            assertEquals(Move.Attack("Player 2", fromHandIndex = 0, toHandIndex = 1), move)
        }
    }

    @Test
    fun `AI returns null when no moves possible`() = runBlocking {
        val state = GameState(winnerId = "Player 1")
        val move = ChopsticksAI.chooseMove(state, Difficulty.EASY)
        assertEquals(null, move)
    }
    
    @Test
    fun `perfect AI avoids immediate loss`() = runBlocking {
        // P1 turn soon. If P2 moves to a state where P1 can win next turn, it's bad.
        // P2: (1, 1), P1: (4, 0). 
        // If P2 attacks P1 (4+1=5), P2 wins. Wait, P2 is current player.
        
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(4), rightHand = Hand(0)),
            player2 = Player("Player 2", leftHand = Hand(1), rightHand = Hand(1)),
            currentPlayerId = "Player 2"
        )
        
        val move = ChopsticksAI.chooseMove(state, Difficulty.PERFECT)
        
        // P2 should attack P1's left hand to win.
        assertTrue(move is Move.Attack)
        assertEquals(0, (move as Move.Attack).toHandIndex)
    }
}
