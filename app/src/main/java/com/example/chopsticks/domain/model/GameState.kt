package com.example.chopsticks.domain.model

import com.example.chopsticks.domain.ai.Move

/**
 * Represents the complete state of a game.
 */
data class GameState(
    val player1: Player = Player("Player 1"),
    val player2: Player = Player("Player 2"),
    val currentPlayerId: String = "Player 1",
    val ruleset: RulesetConfig = RulesetConfig(),
    val winnerId: String? = null,
    val errorMessage: String? = null,
    val lastMove: Move? = null,
    val lastMoveTimestamp: Long = 0L,
    val isMovePending: Boolean = false
) {
    fun getCurrentPlayer(): Player = if (currentPlayerId == player1.id) player1 else player2
    fun getOpponentPlayer(): Player = if (currentPlayerId == player1.id) player2 else player1

    val winner: String? get() = winnerId

    fun updatePlayer(player: Player): GameState {
        return if (player.id == player1.id) {
            copy(player1 = player)
        } else {
            copy(player2 = player)
        }
    }
}
