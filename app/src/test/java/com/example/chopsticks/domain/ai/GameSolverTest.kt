package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Player
import com.example.chopsticks.domain.model.RulesetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameSolverTest {

    private val rulesets = listOf(
        RulesetConfig(),
        RulesetConfig(overflow = false),
        RulesetConfig(allowTransfers = true, onlyEvenSplits = false),
        RulesetConfig(allowTransfers = true, onlyEvenSplits = false, allowSuicide = true, allowSwaps = true),
        RulesetConfig(overflow = false, exactFive = true, onlyEvenSplits = false),
        RulesetConfig(isMisere = true, allowTransfers = true, onlyEvenSplits = false),
        RulesetConfig(allowCondense = true, allowExchangeLife = true, allowTransfers = true, onlyEvenSplits = false),
        RulesetConfig(maxFingers = 3, allowTransfers = true, onlyEvenSplits = false)
    )

    private fun start(ruleset: RulesetConfig) = GameState(ruleset = ruleset)

    @Test
    fun `finds a win in one move`() {
        val state = GameState(
            player1 = Player("Player 1", leftHand = Hand(1), rightHand = Hand(0)),
            player2 = Player("Player 2", leftHand = Hand(4), rightHand = Hand(0)),
            currentPlayerId = "Player 2",
            ruleset = RulesetConfig(overflow = false)
        )
        val solver = GameSolver(state)
        assertEquals(Evaluation(Outcome.WIN, 1), solver.evaluate(state))
        assertEquals(listOf(Move.Attack("Player 2", 0, 0)), solver.bestMoves(state))
    }

    @Test
    fun `every verdict agrees with the verdicts of its moves`() {
        for (ruleset in rulesets) {
            val root = start(ruleset)
            val solver = GameSolver(root)
            for (state in reachableStates(root)) {
                val eval = solver.evaluate(state)
                val moveEvals = RulesEngine.legalMoves(state).map { solver.evaluateMove(state, it) }
                when (eval.outcome) {
                    Outcome.WIN -> {
                        val wins = moveEvals.filter { it.outcome == Outcome.WIN }
                        assertEquals("fastest win in $state", eval.plies, wins.minOf { it.plies!! })
                    }
                    Outcome.LOSS -> {
                        assertTrue("every move loses in $state", moveEvals.all { it.outcome == Outcome.LOSS })
                        assertEquals("longest defence in $state", eval.plies, moveEvals.maxOf { it.plies!! })
                    }
                    Outcome.DRAW -> {
                        assertTrue("no winning move in $state", moveEvals.none { it.outcome == Outcome.WIN })
                        assertTrue(
                            "a draw needs a drawing move or no moves at all in $state",
                            moveEvals.isEmpty() || moveEvals.any { it.outcome == Outcome.DRAW }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `short forced wins match a brute force search`() {
        val random = Random(7)
        for (ruleset in rulesets) {
            val root = start(ruleset)
            val solver = GameSolver(root)
            for (state in reachableStates(root).shuffled(random).take(150)) {
                val eval = solver.evaluate(state)
                for (depth in 1..5) {
                    val solverSaysWin = eval.outcome == Outcome.WIN && eval.plies!! <= depth
                    assertEquals("win within $depth plies in $state", forcedWin(state, depth), solverSaysWin)
                }
            }
        }
    }

    @Test
    fun `best line ends in the predicted number of moves`() {
        for (ruleset in rulesets) {
            val root = start(ruleset)
            val solver = GameSolver(root)
            for (state in reachableStates(root).take(300)) {
                for (move in RulesEngine.legalMoves(state)) {
                    val eval = solver.evaluateMove(state, move)
                    if (eval.outcome == Outcome.DRAW) continue
                    val line = solver.line(state, move)
                    assertEquals("line length for $move in $state", eval.plies, line.size)
                    val winner = line.last().second.winnerId
                    val expected = if (eval.outcome == Outcome.WIN) state.currentPlayerId else state.getOpponentPlayer().id
                    assertEquals(expected, winner)
                }
            }
        }
    }

    @Test
    fun `largest ruleset solves quickly`() {
        val root = start(
            RulesetConfig(
                maxFingers = 10, allowTransfers = true, onlyEvenSplits = false, allowSuicide = true,
                allowSwaps = true, allowCondense = true, allowExchangeLife = true
            )
        )
        val started = System.nanoTime()
        val solver = GameSolver(root)
        val millis = (System.nanoTime() - started) / 1_000_000
        println("Solved ${solver.positionCount} positions in $millis ms")
        assertTrue("took $millis ms", millis < 5_000)
    }

    /** Can the player to move force a win within [plies] moves (counting both sides)? */
    private fun forcedWin(state: GameState, plies: Int): Boolean {
        if (plies <= 0) return false
        val me = state.currentPlayerId
        return RulesEngine.legalMoves(state).any { move ->
            val next = RulesEngine.applyMove(state, move)
            when {
                next.winnerId == me -> true
                next.winnerId != null -> false
                plies < 2 -> false // the opponent's reply needs a move too (it can lose them the game, e.g. in Misère)
                else -> {
                    val replies = RulesEngine.legalMoves(next)
                    replies.isNotEmpty() && replies.all { reply ->
                        val after = RulesEngine.applyMove(next, reply)
                        after.winnerId == me || (after.winnerId == null && forcedWin(after, plies - 2))
                    }
                }
            }
        }
    }

    private fun reachableStates(root: GameState): List<GameState> {
        val seen = LinkedHashMap<Any, GameState>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()
            if (state.winnerId != null || seen.containsKey(state.position)) continue
            seen[state.position] = state
            RulesEngine.legalMoves(state).forEach { queue.addLast(RulesEngine.applyMove(state, it)) }
        }
        return seen.values.toList()
    }
}
