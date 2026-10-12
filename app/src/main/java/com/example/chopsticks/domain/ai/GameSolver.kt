package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Position

/** What happens with perfect play, from the point of view of the player to move. */
enum class Outcome { WIN, DRAW, LOSS }

/**
 * @property plies moves by both players until the game ends with perfect play
 * (the winner finishes as fast as possible, the loser holds out as long as possible). Null for a draw.
 */
data class Evaluation(val outcome: Outcome, val plies: Int?)

/**
 * Solves Chopsticks exactly for every position reachable from [root].
 *
 * The game is small (at most a few tens of thousands of positions), so instead of searching a few
 * moves ahead it maps out every reachable position and works backwards from finished games
 * (retrograde analysis). Positions that neither side can force to a finish are draws.
 */
class GameSolver(root: GameState) {

    private val ruleset = root.ruleset
    private val playerIds = root.player1.id to root.player2.id

    private val index = HashMap<Position, Int>()
    private val states = ArrayList<GameState>()
    private val moves = ArrayList<List<Move>>()
    /** For each position and move: the resulting position's index, or [MOVER_WINS] / [MOVER_LOSES]. */
    private val children = ArrayList<IntArray>()
    private lateinit var outcomes: Array<Outcome>
    private lateinit var plies: IntArray

    val positionCount: Int get() = states.size

    init {
        explore(root)
        solve()
    }

    /** True if this solver was built for [state]'s rules and has seen its position. */
    fun covers(state: GameState): Boolean =
        state.ruleset == ruleset && (state.player1.id to state.player2.id) == playerIds && state.position in index

    fun evaluate(state: GameState): Evaluation {
        val i = indexOf(state)
        return Evaluation(outcomes[i], plies[i].takeIf { outcomes[i] != Outcome.DRAW })
    }

    /** The result of playing [move] in [state], from the point of view of the player making it. */
    fun evaluateMove(state: GameState, move: Move): Evaluation {
        val i = indexOf(state)
        val k = moves[i].indexOf(move)
        require(k >= 0) { "Not a legal move here: $move" }
        return childEvaluation(children[i][k])
    }

    /** All moves that achieve the best possible result: fastest win, longest defence, or any drawing move. */
    fun bestMoves(state: GameState): List<Move> {
        val i = indexOf(state)
        val scored = moves[i].indices.map { k -> moves[i][k] to childEvaluation(children[i][k]) }
        val best = scored.minOf { rank(it.second) }
        return scored.filter { rank(it.second) == best }.map { it.first }
    }

    /**
     * Plays [first] and then perfect moves for both sides until the game ends,
     * or for at most [drawPlies] moves when the result is a draw (draws go round in circles).
     */
    fun line(state: GameState, first: Move, drawPlies: Int = 4): List<Pair<Move, GameState>> {
        val result = mutableListOf<Pair<Move, GameState>>()
        var current = state
        var move = first
        val limit = if (evaluateMove(state, first).outcome == Outcome.DRAW) drawPlies else MAX_LINE
        while (result.size < limit) {
            val next = RulesEngine.applyMove(current, move)
            result.add(move to next)
            if (next.winnerId != null || RulesEngine.legalMoves(next).isEmpty()) break
            current = next
            move = bestMoves(current).first()
        }
        return result
    }

    /** Lower is better for the player making the move. */
    private fun rank(e: Evaluation): Int = when (e.outcome) {
        Outcome.WIN -> e.plies!!
        Outcome.DRAW -> MAX_LINE + 1
        Outcome.LOSS -> 2 * MAX_LINE + 2 - e.plies!!
    }

    private fun childEvaluation(child: Int): Evaluation = when (child) {
        MOVER_WINS -> Evaluation(Outcome.WIN, 1)
        MOVER_LOSES -> Evaluation(Outcome.LOSS, 1)
        else -> when (outcomes[child]) {
            Outcome.WIN -> Evaluation(Outcome.LOSS, plies[child] + 1)
            Outcome.LOSS -> Evaluation(Outcome.WIN, plies[child] + 1)
            Outcome.DRAW -> Evaluation(Outcome.DRAW, null)
        }
    }

    private fun indexOf(state: GameState): Int {
        require(covers(state)) { "Position not covered by this solver" }
        return index.getValue(state.position)
    }

    private fun explore(root: GameState) {
        val queue = ArrayDeque<Int>()
        fun add(state: GameState): Int = index.getOrPut(state.position) {
            states.add(state.copy(errorMessage = null, lastMove = null, lastMoveTimestamp = 0L, isMovePending = false, drawReason = null))
            moves.add(emptyList())
            children.add(IntArray(0))
            queue.addLast(states.size - 1)
            states.size - 1
        }

        add(root)
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            val state = states[i]
            val legal = RulesEngine.legalMoves(state)
            moves[i] = legal
            children[i] = IntArray(legal.size) { k ->
                val next = RulesEngine.applyMove(state, legal[k])
                when (next.winnerId) {
                    null -> add(next)
                    state.currentPlayerId -> MOVER_WINS
                    else -> MOVER_LOSES
                }
            }
        }
    }

    /**
     * Round by round: a position is won if some move leads to a lost position for the opponent,
     * and lost if every move leads to a won position for the opponent. Positions decided in round N
     * end in exactly N moves, so the first decision found is also the fastest win / longest defence.
     */
    private fun solve() {
        val n = states.size
        val decided = arrayOfNulls<Outcome>(n)
        val distance = IntArray(n)
        var changed = true
        while (changed) {
            changed = false
            val before = decided.copyOf()
            val distanceBefore = distance.copyOf()
            for (i in 0 until n) {
                if (before[i] != null || children[i].isEmpty()) continue
                var fastestWin = Int.MAX_VALUE
                var everyMoveLoses = true
                var longestLoss = 0
                for (child in children[i]) {
                    when (child) {
                        MOVER_WINS -> fastestWin = minOf(fastestWin, 1)
                        MOVER_LOSES -> longestLoss = maxOf(longestLoss, 1)
                        else -> when (before[child]) {
                            Outcome.LOSS -> fastestWin = minOf(fastestWin, distanceBefore[child] + 1)
                            Outcome.WIN -> longestLoss = maxOf(longestLoss, distanceBefore[child] + 1)
                            else -> everyMoveLoses = false
                        }
                    }
                }
                if (fastestWin != Int.MAX_VALUE) {
                    decided[i] = Outcome.WIN
                    distance[i] = fastestWin
                    changed = true
                } else if (everyMoveLoses) {
                    decided[i] = Outcome.LOSS
                    distance[i] = longestLoss
                    changed = true
                }
            }
        }
        // Undecided positions (including ones with no legal moves) are draws
        outcomes = Array(n) { decided[it] ?: Outcome.DRAW }
        plies = distance
    }

    private companion object {
        const val MOVER_WINS = -1
        const val MOVER_LOSES = -2
        const val MAX_LINE = 200
    }
}
