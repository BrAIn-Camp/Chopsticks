package com.example.chopsticks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chopsticks.domain.model.GameMode
import androidx.navigation3.ui.NavDisplay
import com.example.chopsticks.ui.MainMenu
import com.example.chopsticks.ui.ConfigurationScreen
import com.example.chopsticks.domain.model.Difficulty
import com.example.chopsticks.ui.game.GameScreen
import com.example.chopsticks.ui.game.GameViewModel
import com.example.chopsticks.data.SettingsRepository
import com.example.chopsticks.ui.theme.ChopsticksTheme
import kotlinx.serialization.Serializable

import com.example.chopsticks.domain.model.RulesetConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Serializable
sealed interface ChopsticksRoute : NavKey {
    @Serializable
    data object MainMenu : ChopsticksRoute

    @Serializable
    data object Configuration : ChopsticksRoute


    @Serializable
    data class Game(
        val mode: String,
        val themeName: String
    ) : ChopsticksRoute
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // viewModel() keeps the game alive across rotation and other configuration changes
            val gameViewModel: GameViewModel = viewModel { GameViewModel(SettingsRepository(applicationContext)) }
            val currentTheme by gameViewModel.theme.collectAsState()
            val rulesetConfig by gameViewModel.rulesetConfig.collectAsState()
            val difficulty by gameViewModel.difficulty.collectAsState()

            ChopsticksTheme(handTheme = currentTheme) {
                val backStack = rememberNavBackStack(ChopsticksRoute.MainMenu)

                NavDisplay(
                    backStack = backStack,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        } else {
                            finish()
                        }
                    },
                    entryProvider = { key ->
                        when (key) {
                            is ChopsticksRoute.MainMenu -> {
                                NavEntry(key) {
                                    MainMenu(
                                        currentRuleset = rulesetConfig,
                                        currentTheme = currentTheme,
                                        onThemeChange = { gameViewModel.updateTheme(it) },
                                        onStartGame = { mode ->
                                            gameViewModel.startGame(mode, rulesetConfig, difficulty)
                                            backStack.add(
                                                ChopsticksRoute.Game(
                                                    mode = mode.name,
                                                    themeName = currentTheme.name
                                                )
                                            )
                                        },
                                        onGameRules = {
                                            backStack.add(ChopsticksRoute.Configuration)
                                        }
                                    )
                                }
                            }
                            is ChopsticksRoute.Configuration -> {
                                NavEntry(key) {
                                    ConfigurationScreen(
                                        currentRuleset = rulesetConfig,
                                        onRulesetChange = { gameViewModel.updateRulesetConfig(it) },
                                        currentDifficulty = difficulty,
                                        onDifficultyChange = { gameViewModel.updateDifficulty(it) },
                                        onRestoreDefaults = { gameViewModel.restoreToDefaults() },
                                        onBack = {
                                            if (backStack.size > 1) backStack.removeLastOrNull()
                                        },
                                        theme = currentTheme
                                    )
                                }
                            }
                            is ChopsticksRoute.Game -> {
                                NavEntry(key) {
                                    // If Android restored this screen after closing the app in the background,
                                    // the game itself is gone: start a fresh one in the same mode.
                                    LaunchedEffect(key) {
                                        gameViewModel.ensureGameStarted(GameMode.valueOf(key.mode), rulesetConfig, difficulty)
                                    }
                                    GameScreen(
                                        viewModel = gameViewModel,
                                        theme = currentTheme,
                                        onExit = {
                                            if (backStack.size > 1) backStack.removeLastOrNull()
                                        }
                                    )
                                }
                            }
                            else -> error("Unknown route key: $key")
                        }
                    }
                )
            }
        }
    }
}
