package com.example.chopsticks.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chopsticks.domain.ai.ChopsticksAI
import com.example.chopsticks.domain.ai.Move
import com.example.chopsticks.domain.engine.RulesEngine
import com.example.chopsticks.domain.model.RulesetConfig
import com.example.chopsticks.domain.model.GameState
import com.example.chopsticks.domain.model.GameMode
import com.example.chopsticks.domain.model.Difficulty
import com.example.chopsticks.domain.model.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.chopsticks.data.SettingsRepository
import com.example.chopsticks.domain.ai.HintResult

class GameViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _rulesetConfig = MutableStateFlow(repository.loadRulesetConfig())
    val rulesetConfig: StateFlow<RulesetConfig> = _rulesetConfig.asStateFlow()

    private val _difficulty = MutableStateFlow(repository.loadDifficulty())
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val _invalidMoveHand = MutableStateFlow<Pair<String, Int>?>(null)
    val invalidMoveHand: StateFlow<Pair<String, Int>?> = _invalidMoveHand.asStateFlow()

    private val _hintState = MutableStateFlow<HintResult?>(null)
    val hintState: StateFlow<HintResult?> = _hintState.asStateFlow()

    private val _isHintLoading = MutableStateFlow(false)
    val isHintLoading: StateFlow<Boolean> = _isHintLoading.asStateFlow()

    var gameMode: GameMode = GameMode.PvP
        private set
    private var aiDifficulty: Difficulty = Difficulty.EASY
    private val aiPlayerId = "Player 2"
    private var isMovePending = false

    fun updateRulesetConfig(config: RulesetConfig) {
        _rulesetConfig.value = config
        repository.saveRulesetConfig(config)
    }

    fun updateDifficulty(newDifficulty: Difficulty) {
        _difficulty.value = newDifficulty
        repository.saveDifficulty(newDifficulty)
    }

    fun restoreToDefaults() {
        val defaultRuleset = RulesetConfig()
        val defaultDifficulty = Difficulty.EASY
        _rulesetConfig.value = defaultRuleset
        _difficulty.value = defaultDifficulty
        repository.saveRulesetConfig(defaultRuleset)
        repository.saveDifficulty(defaultDifficulty)
    }

    /**
     * Initializes a new game session.
     */
    fun startGame(mode: GameMode, ruleset: RulesetConfig, difficulty: Difficulty = Difficulty.EASY) {
        gameMode = mode
        aiDifficulty = difficulty
        isMovePending = false
        _hintState.value = null
        _uiState.value = GameState(
            player1 = Player("Player 1"),
            player2 = Player("Player 2"),
            currentPlayerId = "Player 1",
            ruleset = ruleset
        )
    }

    fun requestHint() {
        if (isMovePending || _uiState.value.winnerId != null) return
        val currentState = _uiState.value
        
        viewModelScope.launch {
            _isHintLoading.value = true
            val hint = ChopsticksAI.getHint(currentState)
            _hintState.value = hint
            _isHintLoading.value = false
        }
    }

    fun clearHint() {
        _hintState.value = null
    }

    /**
     * Restarts the current game with the same settings.
     */
    fun restartGame() {
        val currentState = _uiState.value
        startGame(gameMode, currentState.ruleset, aiDifficulty)
    }

    /**
     * Handles an attack move from the user.
     */
    fun performAttack(fromHandIndex: Int, toHandIndex: Int) {
        if (isMovePending) return
        clearHint()
        val currentState = _uiState.value
        if (currentState.winnerId != null) return
        if (currentState.currentPlayerId == aiPlayerId && gameMode == GameMode.PvE) return

        val move = Move.Attack(currentState.currentPlayerId, fromHandIndex, toHandIndex)
        if (!RulesEngine.validateMove(currentState, move)) {
            triggerInvalidMoveFeedback(currentState.currentPlayerId, fromHandIndex)
            _uiState.value = currentState.copy(errorMessage = "Move not allowed")
            return
        }

        isMovePending = true
        val timestamp = System.currentTimeMillis()
        _uiState.value = currentState.copy(
            lastMove = move,
            lastMoveTimestamp = timestamp,
            errorMessage = null,
            isMovePending = true
        )

        viewModelScope.launch {
            delay(850)
            val newState = RulesEngine.attack(currentState, fromHandIndex, toHandIndex)
            _uiState.value = newState.copy(
                lastMove = move,
                lastMoveTimestamp = timestamp,
                isMovePending = false
            )
            isMovePending = false
            checkAiTurn(_uiState.value)
        }
    }

    /**
     * Handles a finger transfer move from the user.
     */
    fun performTransfer(fromHandIndex: Int, toHandIndex: Int, amount: Int) {
        if (isMovePending) return
        clearHint()
        val currentState = _uiState.value
        if (currentState.winnerId != null) return
        if (currentState.currentPlayerId == aiPlayerId && gameMode == GameMode.PvE) return

        val move = Move.Transfer(currentState.currentPlayerId, fromHandIndex, toHandIndex, amount)
        if (!RulesEngine.validateMove(currentState, move)) {
            triggerInvalidMoveFeedback(currentState.currentPlayerId, fromHandIndex)
            _uiState.value = currentState.copy(errorMessage = "Move not allowed")
            return
        }

        isMovePending = true
        val timestamp = System.currentTimeMillis()
        _uiState.value = currentState.copy(
            lastMove = move,
            lastMoveTimestamp = timestamp,
            errorMessage = null,
            isMovePending = true
        )

        viewModelScope.launch {
            delay(850)
            val newState = RulesEngine.transfer(currentState, fromHandIndex, toHandIndex, amount)
            _uiState.value = newState.copy(
                lastMove = move,
                lastMoveTimestamp = timestamp,
                isMovePending = false
            )
            isMovePending = false
            checkAiTurn(_uiState.value)
        }
    }

    /**
     * Handles an Exchange Life move (Logan Clause).
     */
    fun performExchangeLife(sacrificedHandIndex: Int) {
        if (isMovePending) return
        clearHint()
        val currentState = _uiState.value
        if (currentState.winnerId != null) return
        if (currentState.currentPlayerId == aiPlayerId && gameMode == GameMode.PvE) return

        val move = Move.ExchangeLife(currentState.currentPlayerId, sacrificedHandIndex)
        if (!RulesEngine.validateMove(currentState, move)) {
            _uiState.value = currentState.copy(errorMessage = "Move not allowed")
            return
        }

        isMovePending = true
        val timestamp = System.currentTimeMillis()
        _uiState.value = currentState.copy(
            lastMove = move,
            lastMoveTimestamp = timestamp,
            errorMessage = null,
            isMovePending = true
        )

        viewModelScope.launch {
            delay(850)
            val newState = RulesEngine.exchangeLife(currentState, sacrificedHandIndex)
            _uiState.value = newState.copy(
                lastMove = move,
                lastMoveTimestamp = timestamp,
                isMovePending = false
            )
            isMovePending = false
            checkAiTurn(_uiState.value)
        }
    }

    /**
     * Handles a Condense move (Meta rule).
     */
    fun performCondense(leftFingers: Int, rightFingers: Int) {
        if (isMovePending) return
        clearHint()
        val currentState = _uiState.value
        if (currentState.winnerId != null) return
        if (currentState.currentPlayerId == aiPlayerId && gameMode == GameMode.PvE) return

        val move = Move.Condense(currentState.currentPlayerId, leftFingers, rightFingers)
        if (!RulesEngine.validateMove(currentState, move)) {
            _uiState.value = currentState.copy(errorMessage = "Move not allowed")
            return
        }

        isMovePending = true
        val timestamp = System.currentTimeMillis()
        _uiState.value = currentState.copy(
            lastMove = move,
            lastMoveTimestamp = timestamp,
            errorMessage = null,
            isMovePending = true
        )

        viewModelScope.launch {
            delay(850)
            val newState = RulesEngine.condense(currentState, leftFingers, rightFingers)
            _uiState.value = newState.copy(
                lastMove = move,
                lastMoveTimestamp = timestamp,
                isMovePending = false
            )
            isMovePending = false
            checkAiTurn(_uiState.value)
        }
    }

    private fun triggerInvalidMoveFeedback(playerId: String, handIndex: Int) {
        viewModelScope.launch {
            _invalidMoveHand.value = playerId to handIndex
            delay(500)
            _invalidMoveHand.value = null
        }
    }

    private fun checkAiTurn(state: GameState) {
        if (state.winnerId == null && gameMode == GameMode.PvE && state.currentPlayerId == aiPlayerId) {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        viewModelScope.launch {
            delay(1000) // Small delay for UX to let user see the board
            val currentState = _uiState.value
            val move = ChopsticksAI.chooseMove(currentState, aiDifficulty)
            
            move?.let {
                if (!RulesEngine.validateMove(currentState, it)) return@let
                
                isMovePending = true
                val timestamp = System.currentTimeMillis()
                _uiState.value = currentState.copy(
                    lastMove = it,
                    lastMoveTimestamp = timestamp,
                    errorMessage = null,
                    isMovePending = true
                )
                
                delay(850)
                
                val newState = when (it) {
                    is Move.Attack -> RulesEngine.attack(currentState, it.fromHandIndex, it.toHandIndex)
                    is Move.Transfer -> RulesEngine.transfer(currentState, it.fromHandIndex, it.toHandIndex, it.amount)
                    is Move.ExchangeLife -> RulesEngine.exchangeLife(currentState, it.sacrificedHandIndex)
                    is Move.Condense -> RulesEngine.condense(currentState, it.leftFingers, it.rightFingers)
                }
                _uiState.value = newState.copy(
                    lastMove = it,
                    lastMoveTimestamp = timestamp,
                    isMovePending = false
                )
                isMovePending = false
            }
        }
    }
}
