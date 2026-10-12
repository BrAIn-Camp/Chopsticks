package com.example.chopsticks.domain.model

import kotlinx.serialization.Serializable

/**
 * Configuration for the Chopsticks game rules.
 */
@Serializable
data class RulesetConfig(
    val overflow: Boolean = true,
    val exactFive: Boolean = false,
    val allowDivisions: Boolean = true, // Revival: Can split to a dead hand
    val allowTransfers: Boolean = false, // Live-to-live transfers
    val allowSuicide: Boolean = false,  // Allow killing own hand
    val isMisere: Boolean = false,
    val allowSwaps: Boolean = false,
    val allowExchangeLife: Boolean = false,
    val allowCondense: Boolean = false,
    val onlyEvenSplits: Boolean = true,
    val maxFingers: Int = 5
)
