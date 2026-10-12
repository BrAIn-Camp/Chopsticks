package com.example.chopsticks.domain.model

/**
 * Represents a hand in the Chopsticks game.
 * @property fingers The number of fingers currently active on the hand.
 */
data class Hand(val fingers: Int = 1) {
    /**
     * Checks if the hand is "dead" based on the ruleset's maximum finger limit.
     */
    fun isDead(maxFingers: Int, exactFive: Boolean = false): Boolean {
        return if (exactFive) {
            fingers == 0 || fingers == maxFingers
        } else {
            fingers == 0 || fingers >= maxFingers
        }
    }
}

