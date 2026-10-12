package com.example.chopsticks.domain.model

/**
 * Represents a player with two hands.
 */
data class Player(
    val id: String,
    val leftHand: Hand = Hand(1),
    val rightHand: Hand = Hand(1)
) {
    /**
     * A player is defeated if both their hands are dead.
     */
    fun isDefeated(maxFingers: Int, exactFive: Boolean = false): Boolean =
        leftHand.isDead(maxFingers, exactFive) && rightHand.isDead(maxFingers, exactFive)

    /**
     * Helper to get hands as a list.
     */
    fun getHands(): List<Hand> = listOf(leftHand, rightHand)
}

