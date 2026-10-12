package com.example.chopsticks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesetConfigTest {

    @Test
    fun `verify default ruleset values`() {
        val config = RulesetConfig()
        
        // Enabled by default
        assertTrue("Overflow should be enabled by default", config.overflow)
        assertTrue("Revive Dead Hands (allowDivisions) should be enabled by default", config.allowDivisions)
        assertTrue("Even Splits Only should be enabled by default", config.onlyEvenSplits)
        
        // Disabled by default
        assertFalse("Exact Five should be disabled by default", config.exactFive)
        assertFalse("Allow Transfers should be disabled by default", config.allowTransfers)
        assertFalse("Allow Suicide should be disabled by default", config.allowSuicide)
        assertFalse("isMisere should be disabled by default", config.isMisere)
        assertFalse("Allow Swaps should be disabled by default", config.allowSwaps)
        assertFalse("Allow Exchange Life should be disabled by default", config.allowExchangeLife)
        assertFalse("Allow Condense should be disabled by default", config.allowCondense)
        
        // Other values
        assertEquals("maxFingers should be 5 by default", 5, config.maxFingers)
    }
}
