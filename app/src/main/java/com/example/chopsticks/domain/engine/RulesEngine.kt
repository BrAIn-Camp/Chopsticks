package com.example.chopsticks.domain.engine

import com.example.chopsticks.domain.ai.Move
import com.example.chopsticks.domain.model.DrawReason
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.Hand
import com.example.chopsticks.domain.model.Player
import com.example.chopsticks.domain.model.RulesetConfig

/**
 * The core logic for the Chopsticks game.
 */
object RulesEngine {

    /**
     * Executes an attack from the current player's hand to an opponent's hand.
     */
    fun attack(
        state: GameState,
        fromHandIndex: Int, // 0 for left, 1 for right
        toHandIndex: Int
    ): GameState {
        if (!validateAttack(state, fromHandIndex, toHandIndex)) {
            return state.copy(errorMessage = "That attack isn't allowed")
        }

        val currentPlayer = state.getCurrentPlayer()
        val opponent = state.getOpponentPlayer()

        val attackerHand = if (fromHandIndex == 0) currentPlayer.leftHand else currentPlayer.rightHand
        val targetHand = if (toHandIndex == 0) opponent.leftHand else opponent.rightHand

        val exactFive = state.ruleset.exactFive
        val maxFingers = state.ruleset.maxFingers

        // Apply attack logic
        var newFingers = targetHand.fingers + attackerHand.fingers
        
        if (state.ruleset.overflow) {
            newFingers %= maxFingers
        } else {
            if (exactFive) {
                // Already validated in validateAttack
            } else {
                if (newFingers >= maxFingers) {
                    newFingers = 0 // Hand dies
                }
            }
        }

        val newTargetHand = Hand(newFingers)
        val newOpponent = if (toHandIndex == 0) {
            opponent.copy(leftHand = newTargetHand)
        } else {
            opponent.copy(rightHand = newTargetHand)
        }

        val newState = state.updatePlayer(newOpponent)
        
        // Check for victory
        if (newOpponent.isDefeated(maxFingers, exactFive)) {
            val winnerId = if (state.ruleset.isMisere) newOpponent.id else currentPlayer.id
            return newState.copy(winnerId = winnerId, errorMessage = null)
        }

        // Turn transition
        return newState.copy(
            currentPlayerId = opponent.id,
            errorMessage = null
        )
    }

    /**
     * Executes a transfer of fingers between the current player's own hands.
     */
    fun transfer(
        state: GameState,
        fromHandIndex: Int,
        toHandIndex: Int,
        amount: Int
    ): GameState {
        if (!validateTransfer(state, fromHandIndex, toHandIndex, amount)) {
            return state.copy(errorMessage = "That transfer isn't allowed")
        }

        val currentPlayer = state.getCurrentPlayer()
        val fromHand = if (fromHandIndex == 0) currentPlayer.leftHand else currentPlayer.rightHand
        val toHand = if (toHandIndex == 0) currentPlayer.leftHand else currentPlayer.rightHand

        val exactFive = state.ruleset.exactFive
        val maxFingers = state.ruleset.maxFingers

        val newFromFingers = fromHand.fingers - amount
        var newToFingers = toHand.fingers + amount

        // Overflow logic for transfers
        if (state.ruleset.overflow) {
            newToFingers %= maxFingers
        } else if (!exactFive && newToFingers >= maxFingers) {
            newToFingers = 0
        }

        val newFromHand = Hand(newFromFingers)
        val newToHand = Hand(newToFingers)

        val newPlayer = if (fromHandIndex == 0) {
            currentPlayer.copy(leftHand = newFromHand, rightHand = newToHand)
        } else {
            currentPlayer.copy(leftHand = newToHand, rightHand = newFromHand)
        }

        val newState = state.updatePlayer(newPlayer)

        // If a player defeats themselves (e.g. Suicide mode)
        if (newPlayer.isDefeated(maxFingers, exactFive)) {
            val winnerId = if (state.ruleset.isMisere) newPlayer.id else state.getOpponentPlayer().id
            return newState.copy(winnerId = winnerId, errorMessage = null)
        }

        return newState.copy(
            currentPlayerId = state.getOpponentPlayer().id,
            errorMessage = null
        )
    }

    /**
     * Executes any move. The move must already be valid.
     */
    fun applyMove(state: GameState, move: Move): GameState {
        return when (move) {
            is Move.Attack -> attack(state, move.fromHandIndex, move.toHandIndex)
            is Move.Transfer -> transfer(state, move.fromHandIndex, move.toHandIndex, move.amount)
            is Move.ExchangeLife -> exchangeLife(state, move.sacrificedHandIndex)
            is Move.Condense -> condense(state, move.leftFingers, move.rightFingers)
        }
    }

    /**
     * Every legal move for the player whose turn it is.
     */
    fun legalMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        val playerId = state.currentPlayerId
        val currentPlayer = state.getCurrentPlayer()

        for (from in 0..1) {
            for (to in 0..1) {
                moves.add(Move.Attack(playerId, from, to))
            }
        }
        for (from in 0..1) {
            val fromHand = if (from == 0) currentPlayer.leftHand else currentPlayer.rightHand
            for (amount in 1..fromHand.fingers) {
                moves.add(Move.Transfer(playerId, from, 1 - from, amount))
            }
        }
        for (sacrificed in 0..1) {
            moves.add(Move.ExchangeLife(playerId, sacrificed))
        }
        val remainder = currentPlayer.leftHand.fingers + currentPlayer.rightHand.fingers - state.ruleset.maxFingers
        for (left in 0..remainder) {
            moves.add(Move.Condense(playerId, left, remainder - left))
        }

        return moves.filter { validateMove(state, it) }
    }

    /**
     * Checks whether the game should end in a draw now that it is [GameState.currentPlayerId]'s turn.
     * @param timesPositionSeen how many times this exact position has occurred, including now.
     */
    fun detectDraw(state: GameState, timesPositionSeen: Int): DrawReason? {
        if (state.isGameOver) return null
        if (timesPositionSeen >= REPETITION_LIMIT) return DrawReason.REPETITION
        if (legalMoves(state).isEmpty()) return DrawReason.NO_LEGAL_MOVES
        return null
    }

    const val REPETITION_LIMIT = 3

    /**
     * Validates if a move is legal without executing it.
     */
    fun validateMove(state: GameState, move: Move): Boolean {
        return when (move) {
            is Move.Attack -> validateAttack(state, move.fromHandIndex, move.toHandIndex)
            is Move.Transfer -> validateTransfer(state, move.fromHandIndex, move.toHandIndex, move.amount)
            is Move.ExchangeLife -> validateExchangeLife(state, move.sacrificedHandIndex)
            is Move.Condense -> validateCondense(state, move.leftFingers, move.rightFingers)
        }
    }

    /**
     * Executes the Logan Clause: sacrifices one living hand to revive the dead one.
     */
    fun exchangeLife(
        state: GameState,
        sacrificedHandIndex: Int
    ): GameState {
        if (!validateExchangeLife(state, sacrificedHandIndex)) {
            return state.copy(errorMessage = "Exchange Life isn't allowed right now")
        }

        val currentPlayer = state.getCurrentPlayer()
        val fingers = if (sacrificedHandIndex == 0) currentPlayer.leftHand.fingers else currentPlayer.rightHand.fingers

        val newPlayer = if (sacrificedHandIndex == 0) {
            currentPlayer.copy(leftHand = Hand(0), rightHand = Hand(fingers))
        } else {
            currentPlayer.copy(leftHand = Hand(fingers), rightHand = Hand(0))
        }

        val newState = state.updatePlayer(newPlayer)

        return newState.copy(
            currentPlayerId = state.getOpponentPlayer().id,
            errorMessage = null
        )
    }

    /**
     * Executes the Condense rule: total fingers - maxFingers split between hands.
     */
    fun condense(
        state: GameState,
        leftFingers: Int,
        rightFingers: Int
    ): GameState {
        if (!validateCondense(state, leftFingers, rightFingers)) {
            return state.copy(errorMessage = "Condense isn't allowed right now")
        }

        val currentPlayer = state.getCurrentPlayer()
        val newPlayer = currentPlayer.copy(
            leftHand = Hand(leftFingers),
            rightHand = Hand(rightFingers)
        )

        val newState = state.updatePlayer(newPlayer)

        return newState.copy(
            currentPlayerId = state.getOpponentPlayer().id,
            errorMessage = null
        )
    }

    /**
     * Validates if an attack is legal without executing it.
     */
    fun validateAttack(state: GameState, fromHandIndex: Int, toHandIndex: Int): Boolean {
        if (state.isGameOver) return false
        val currentPlayer = state.getCurrentPlayer()
        val opponent = state.getOpponentPlayer()
        val attackerHand = if (fromHandIndex == 0) currentPlayer.leftHand else currentPlayer.rightHand
        val targetHand = if (toHandIndex == 0) opponent.leftHand else opponent.rightHand
        val exactFive = state.ruleset.exactFive
        val maxFingers = state.ruleset.maxFingers

        if (attackerHand.isDead(maxFingers, exactFive)) return false
        if (targetHand.isDead(maxFingers, exactFive)) return false

        if (!state.ruleset.overflow && exactFive && (targetHand.fingers + attackerHand.fingers) > maxFingers) return false

        return true
    }

    /**
     * Validates if a transfer is legal without executing it.
     */
    fun validateTransfer(state: GameState, fromHandIndex: Int, toHandIndex: Int, amount: Int): Boolean {
        if (state.isGameOver) return false
        if (fromHandIndex == toHandIndex) return false
        val currentPlayer = state.getCurrentPlayer()
        val fromHand = if (fromHandIndex == 0) currentPlayer.leftHand else currentPlayer.rightHand
        val toHand = if (toHandIndex == 0) currentPlayer.leftHand else currentPlayer.rightHand
        
        val exactFive = state.ruleset.exactFive
        val maxFingers = state.ruleset.maxFingers
        if (fromHand.isDead(maxFingers, exactFive)) return false
        if (amount <= 0 || amount > fromHand.fingers) return false

        val isToDead = toHand.isDead(maxFingers, exactFive)

        if (isToDead && !state.ruleset.allowDivisions) return false
        if (!isToDead && !state.ruleset.allowTransfers) return false

        val newFromFingers = fromHand.fingers - amount
        var newToFingers = toHand.fingers + amount
        
        if (state.ruleset.onlyEvenSplits) {
            // Even splits must start from an even source hand and result in two equal hands.
            // This blocks odd source hands (e.g. 3 -> 1) even if they could result in equality,
            // and blocks non-equal results (e.g. 4 -> 3,1).
            if (fromHand.fingers % 2 != 0 || newFromFingers != newToFingers) return false
        }

        if (state.ruleset.overflow) {
            newToFingers %= maxFingers
        } else if (!exactFive && newToFingers >= maxFingers) {
            newToFingers = 0
        }

        // Suicide: emptying the giving hand, or pushing the receiving hand to the limit, kills your own hand
        val killsOwnHand = Hand(newFromFingers).isDead(maxFingers, exactFive) || Hand(newToFingers).isDead(maxFingers, exactFive)
        if (killsOwnHand && !state.ruleset.allowSuicide) return false
        if (exactFive && newToFingers > maxFingers) return false

        // Wikipedia Identity/Swap Rule: Forbids moves that just swap hand values or keep them identical.
        val oldLeft = currentPlayer.leftHand.fingers
        val oldRight = currentPlayer.rightHand.fingers
        val newLeft = if (fromHandIndex == 0) newFromFingers else newToFingers
        val newRight = if (fromHandIndex == 1) newFromFingers else newToFingers

        val isIdentity = (newLeft == oldLeft && newRight == oldRight)
        val isSwap = (newLeft == oldRight && newRight == oldLeft)

        if (!state.ruleset.allowSwaps && (isIdentity || isSwap)) return false

        return true
    }

    fun validateExchangeLife(state: GameState, sacrificedHandIndex: Int): Boolean {
        if (!state.ruleset.allowExchangeLife) return false
        if (state.isGameOver) return false

        val player = state.getCurrentPlayer()
        val maxFingers = state.ruleset.maxFingers
        val exactFive = state.ruleset.exactFive

        val sacrificedHand = if (sacrificedHandIndex == 0) player.leftHand else player.rightHand
        val otherHand = if (sacrificedHandIndex == 0) player.rightHand else player.leftHand

        // Must sacrifice a living hand to revive a dead one
        if (sacrificedHand.isDead(maxFingers, exactFive)) return false
        if (!otherHand.isDead(maxFingers, exactFive)) return false

        return true
    }

    fun validateCondense(state: GameState, leftFingers: Int, rightFingers: Int): Boolean {
        if (!state.ruleset.allowCondense) return false
        if (state.isGameOver) return false

        val player = state.getCurrentPlayer()
        val maxFingers = state.ruleset.maxFingers
        val exactFive = state.ruleset.exactFive

        val total = player.leftHand.fingers + player.rightHand.fingers
        if (total <= maxFingers) return false

        val remainder = total - maxFingers
        if (leftFingers + rightFingers != remainder) return false
        if (leftFingers < 0 || rightFingers < 0) return false

        // Wikipedia Identity/Swap Rule (adapted for condense)
        val isIdentity = (leftFingers == player.leftHand.fingers && rightFingers == player.rightHand.fingers)
        val isSwap = (leftFingers == player.rightHand.fingers && rightFingers == player.leftHand.fingers)
        if (!state.ruleset.allowSwaps && (isIdentity || isSwap)) return false

        // Suicide check
        if (!state.ruleset.allowSuicide) {
            val leftDead = Hand(leftFingers).isDead(maxFingers, exactFive)
            val rightDead = Hand(rightFingers).isDead(maxFingers, exactFive)
            if (leftDead && rightDead) return false
        }

        return true
    }
}

