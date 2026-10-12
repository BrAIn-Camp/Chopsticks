package com.example.chopsticks.domain.model

import com.example.chopsticks.domain.ai.Move

enum class DrawReason {
    /** The same position came up for the third time. */
    REPETITION,
    /** The player to move has no legal moves. */
    NO_LEGAL_MOVES
}

/**
 * Everything that decides what can happen next: every hand, and whose turn it is.
 */
data class Position(
    val p1Left: Int,
    val p1Right: Int,
    val p2Left: Int,
    val p2Right: Int,
    val currentPlayerId: String
)

/**
 * Represents the complete state of a game.
 */
data class GameState(
    val player1: Player = Player("Player 1"),
    val player2: Player = Player("Player 2"),
    val currentPlayerId: String = "Player 1",
    val ruleset: RulesetConfig = RulesetConfig(),
    val winnerId: String? = null,
    val drawReason: DrawReason? = null,
    val errorMessage: String? = null,
    val lastMove: Move? = null,
    val lastMoveTimestamp: Long = 0L,
    val isMovePending: Boolean = false
) {
    fun getCurrentPlayer(): Player = if (currentPlayerId == player1.id) player1 else player2
    fun getOpponentPlayer(): Player = if (currentPlayerId == player1.id) player2 else player1

    val winner: String? get() = winnerId

    val isGameOver: Boolean get() = winnerId != null || drawReason != null

    val position: Position
        get() = Position(
            player1.leftHand.fingers, player1.rightHand.fingers,
            player2.leftHand.fingers, player2.rightHand.fingers,
            currentPlayerId
        )

    fun updatePlayer(player: Player): GameState {
        return if (player.id == player1.id) {
            copy(player1 = player)
        } else {
            copy(player2 = player)
        }
    }
}
