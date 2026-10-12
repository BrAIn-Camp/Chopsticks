package com.example.chopsticks.data

import android.content.Context
import android.content.SharedPreferences
import com.example.chopsticks.domain.model.Difficulty
import com.example.chopsticks.domain.model.RulesetConfig

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE)

    fun loadRulesetConfig(): RulesetConfig {
        return RulesetConfig(
            overflow = prefs.getBoolean("overflow", true),
            exactFive = prefs.getBoolean("exactFive", false),
            allowDivisions = prefs.getBoolean("allowDivisions", true),
            allowTransfers = prefs.getBoolean("allowTransfers", false),
            allowSuicide = prefs.getBoolean("allowSuicide", false),
            isMisere = prefs.getBoolean("isMisere", false),
            allowSwaps = prefs.getBoolean("allowSwaps", false),
            allowExchangeLife = prefs.getBoolean("allowExchangeLife", false),
            allowCondense = prefs.getBoolean("allowCondense", false),
            onlyEvenSplits = prefs.getBoolean("onlyEvenSplits", true),
            maxFingers = prefs.getInt("maxFingers", 5)
        )
    }

    fun saveRulesetConfig(config: RulesetConfig) {
        prefs.edit().apply {
            putBoolean("overflow", config.overflow)
            putBoolean("exactFive", config.exactFive)
            putBoolean("allowDivisions", config.allowDivisions)
            putBoolean("allowTransfers", config.allowTransfers)
            putBoolean("allowSuicide", config.allowSuicide)
            putBoolean("isMisere", config.isMisere)
            putBoolean("allowSwaps", config.allowSwaps)
            putBoolean("allowExchangeLife", config.allowExchangeLife)
            putBoolean("allowCondense", config.allowCondense)
            putBoolean("onlyEvenSplits", config.onlyEvenSplits)
            putInt("maxFingers", config.maxFingers)
            apply()
        }
    }

    fun loadDifficulty(): Difficulty {
        val name = prefs.getString("difficulty", Difficulty.EASY.name) ?: Difficulty.EASY.name
        return try {
            Difficulty.valueOf(name)
        } catch (e: Exception) {
            Difficulty.EASY
        }
    }

    fun saveDifficulty(difficulty: Difficulty) {
        prefs.edit().putString("difficulty", difficulty.name).apply()
    }
}
