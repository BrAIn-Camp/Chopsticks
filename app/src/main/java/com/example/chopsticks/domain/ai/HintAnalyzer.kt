package com.example.chopsticks.domain.ai

import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Player

/** One move in a line of play, written from the hint-asker's point of view. */
data class LineStep(
    val byYou: Boolean,
    /** e.g. "They attack your 3 (right) with their 2 (left): 3 + 2 = 5, knocked out" */
    val description: String,
    /** e.g. "You 1|0 · Them 2|2" */
    val handsAfter: String
)

data class MoveAnalysis(
    val move: Move,
    /** e.g. "Attack their 3 (right) with your 2 (left)" */
    val action: String,
    /** e.g. "3 + 2 = 5: their right hand is knocked out" */
    val effect: String,
    /** Result for you with perfect play from both sides after this move. */
    val evaluation: Evaluation,
    /** True for every move that gets the best possible result. */
    val isBest: Boolean,
    /** Your hands they could knock out on their very next move. */
    val leavesOpen: List<String>,
    /** Their hands you could knock out on your following move, if they leave them. */
    val threatens: List<String>,
    /** This move, then perfect play from both sides. */
    val line: List<LineStep>,
    /** How the line ends: "You win.", "They win." or why it's a draw. */
    val lineEnd: String
)

data class HintAnalysis(
    val evaluation: Evaluation,
    val verdict: String,
    val hands: String,
    /** Hands of yours they could knock out if it were their move now. */
    val dangers: List<String>,
    /** Hands of theirs you can knock out this move. */
    val chances: List<String>,
    /** Short reminders of the rules that change how to read the position. */
    val ruleNotes: List<String>,
    /** Every legal move, best first. */
    val moves: List<MoveAnalysis>
)

/**
 * Turns solver results into plain facts the player can check: what each move does, what it leaves
 * open, what it threatens, and exactly how the game goes from there with perfect play.
 */
object HintAnalyzer {

    fun analyze(state: GameState, solver: GameSolver): HintAnalysis {
        val me = state.currentPlayerId
        val evaluation = solver.evaluate(state)

        val moves = RulesEngine.legalMoves(state).map { move ->
            val after = RulesEngine.applyMove(state, move)
            val moveEval = solver.evaluateMove(state, move)
            val line = solver.line(state, move)
            MoveAnalysis(
                move = move,
                // The card is always about your own move, so drop the subject: "Attack their 3 …"
                action = action(state, move, me).removePrefix("You ").replaceFirstChar(Char::uppercase),
                effect = effect(state, after, move, me),
                evaluation = moveEval,
                isBest = false,
                leavesOpen = if (after.isGameOver) emptyList() else knockouts(after, me),
                threatens = if (after.isGameOver) emptyList() else knockouts(after.copy(currentPlayerId = me), me),
                line = line.mapIndexed { i, (lineMove, lineAfter) ->
                    val before = if (i == 0) state else line[i - 1].second
                    LineStep(
                        byYou = lineMove.playerId == me,
                        description = sentence(action(before, lineMove, me), effect(before, lineAfter, lineMove, me)),
                        handsAfter = hands(lineAfter, me)
                    )
                },
                lineEnd = lineEnd(line.lastOrNull()?.second, moveEval, me)
            )
        }.sortedBy { rank(it.evaluation) }

        val bestRank = moves.minOfOrNull { rank(it.evaluation) }
        return HintAnalysis(
            evaluation = evaluation,
            verdict = verdict(evaluation),
            hands = hands(state, me),
            dangers = knockouts(state.copy(currentPlayerId = state.getOpponentPlayer().id), me),
            chances = knockouts(state, me),
            ruleNotes = ruleNotes(state),
            moves = moves.map { it.copy(isBest = rank(it.evaluation) == bestRank) }
        )
    }

    /** Short label for a move's result, e.g. "WIN IN 3", "DRAW", "LOSS IN 4". */
    fun resultLabel(e: Evaluation): String = when (e.outcome) {
        Outcome.WIN -> "WIN IN ${e.plies}"
        Outcome.DRAW -> "DRAW"
        Outcome.LOSS -> "LOSS IN ${e.plies}"
    }

    private fun verdict(e: Evaluation): String = when (e.outcome) {
        Outcome.WIN -> if (e.plies == 1) "You can win right now." else "Forced win in ${e.plies} moves."
        Outcome.DRAW -> "Draw with best play. Avoid any move marked LOSS."
        Outcome.LOSS -> "Forced loss in ${e.plies} moves at most. BEST holds out longest."
    }
    private fun rank(e: Evaluation): Int = when (e.outcome) {
        Outcome.WIN -> e.plies!!
        Outcome.DRAW -> 10_000
        Outcome.LOSS -> 20_000 - e.plies!!
    }

    /**
     * Every way the player to move in [state] can knock out one of their opponent's live hands with an attack,
     * written from [me]'s point of view.
     */
    private fun knockouts(state: GameState, me: String): List<String> {
        val attacker = state.getCurrentPlayer()
        val defender = state.getOpponentPlayer()
        val max = state.ruleset.maxFingers
        val exact = state.ruleset.exactFive
        return RulesEngine.legalMoves(state).filterIsInstance<Move.Attack>().mapNotNull { attack ->
            val target = hand(defender, attack.toHandIndex)
            val after = RulesEngine.applyMove(state, attack)
            val defenderAfter = if (after.player1.id == defender.id) after.player1 else after.player2
            if (!target.isDead(max, exact) && hand(defenderAfter, attack.toHandIndex).isDead(max, exact)) {
                val a = hand(attacker, attack.fromHandIndex).fingers
                "${handRef(attacker.id, attack.fromHandIndex, a, me, capital = true)} → " +
                    "${handRef(defender.id, attack.toHandIndex, target.fingers, me)}: ${target.fingers} + $a = ${target.fingers + a}"
            } else null
        }.distinct()
    }

    private fun action(state: GameState, move: Move, me: String): String {
        val actor = state.getCurrentPlayer()
        val opponent = state.getOpponentPlayer()
        val subject = if (actor.id == me) "You" else "They"
        return when (move) {
            is Move.Attack ->
                "$subject attack ${handRef(opponent.id, move.toHandIndex, hand(opponent, move.toHandIndex).fingers, me)} " +
                    "with ${handRef(actor.id, move.fromHandIndex, hand(actor, move.fromHandIndex).fingers, me)}"
            is Move.Transfer ->
                "$subject move ${move.amount} from ${possessive(actor.id, me)} ${side(move.fromHandIndex)} hand " +
                    "to ${possessive(actor.id, me)} ${side(move.toHandIndex)} hand"
            is Move.ExchangeLife ->
                "$subject sacrifice ${possessive(actor.id, me)} ${side(move.sacrificedHandIndex)} hand " +
                    "to revive ${possessive(actor.id, me)} ${side(1 - move.sacrificedHandIndex)} hand"
            is Move.Condense -> "$subject condense ${possessive(actor.id, me)} hands"
        }
    }

    private fun effect(before: GameState, after: GameState, move: Move, me: String): String {
        val max = before.ruleset.maxFingers
        val exact = before.ruleset.exactFive
        val actorBefore = before.getCurrentPlayer()
        val actorAfter = player(after, actorBefore.id)
        return when (move) {
            is Move.Attack -> {
                val target = hand(before.getOpponentPlayer(), move.toHandIndex).fingers
                val attacker = hand(actorBefore, move.fromHandIndex).fingers
                val sum = target + attacker
                val result = hand(player(after, before.getOpponentPlayer().id), move.toHandIndex)
                val whose = possessive(before.getOpponentPlayer().id, me)
                when {
                    result.isDead(max, exact) -> "$target + $attacker = $sum: $whose ${side(move.toHandIndex)} hand is knocked out"
                    result.fingers != sum -> "$target + $attacker = $sum, which rolls over to ${result.fingers}"
                    else -> "$target + $attacker = $sum"
                }
            }
            is Move.Transfer, is Move.ExchangeLife, is Move.Condense -> {
                val notes = mutableListOf<String>()
                for (i in 0..1) {
                    val was = hand(actorBefore, i)
                    val now = hand(actorAfter, i)
                    val whose = possessive(actorBefore.id, me)
                    if (was.isDead(max, exact) && !now.isDead(max, exact)) notes += "revives $whose ${side(i)} hand"
                    if (!was.isDead(max, exact) && now.isDead(max, exact)) notes += "knocks out $whose own ${side(i)} hand"
                }
                val change = "${pair(actorBefore)} → ${pair(actorAfter)}"
                if (notes.isEmpty()) change else "$change (${notes.joinToString(", ")})"
            }
        }
    }

    private fun lineEnd(last: GameState?, e: Evaluation, me: String): String = when {
        last?.winnerId == me -> "You win."
        last?.winnerId != null -> "They win."
        last != null && RulesEngine.legalMoves(last).isEmpty() -> "No legal moves left: the game is a draw."
        e.outcome == Outcome.DRAW -> "…neither side can force a win: draw."
        else -> ""
    }

    /** Only rules that flip how a position reads; the rest the player already knows from the board. */
    private fun ruleNotes(state: GameState): List<String> =
        if (state.ruleset.isMisere) listOf("Misère: losing both hands wins.") else emptyList()
    private fun sentence(action: String, effect: String) = "$action: $effect"

    private fun hands(state: GameState, me: String): String {
        val you = player(state, me)
        val them = if (state.player1.id == me) state.player2 else state.player1
        return "You ${pair(you, state)} · Them ${pair(them, state)}"
    }

    private fun pair(p: Player, state: GameState? = null): String {
        fun show(h: Hand) = if (state != null && h.isDead(state.ruleset.maxFingers, state.ruleset.exactFive)) "–" else h.fingers.toString()
        return "${show(p.leftHand)}|${show(p.rightHand)}"
    }

    private fun handRef(ownerId: String, index: Int, fingers: Int, me: String, capital: Boolean = false): String {
        val whose = possessive(ownerId, me).let { if (capital) it.replaceFirstChar(Char::uppercase) else it }
        return "$whose $fingers (${side(index)})"
    }

    private fun possessive(ownerId: String, me: String) = if (ownerId == me) "your" else "their"
    private fun side(index: Int) = if (index == 0) "left" else "right"
    private fun hand(p: Player, index: Int) = if (index == 0) p.leftHand else p.rightHand
    private fun player(state: GameState, id: String) = if (state.player1.id == id) state.player1 else state.player2
}
