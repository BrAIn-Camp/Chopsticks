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
import com.example.chopsticks.domain.model.Position
import com.example.chopsticks.ui.theme.HandTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.chopsticks.data.SettingsRepository
import com.example.chopsticks.domain.ai.GameSolver
import com.example.chopsticks.domain.ai.HintAnalysis
import com.example.chopsticks.domain.ai.HintAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _rulesetConfig = MutableStateFlow(repository.loadRulesetConfig())
    val rulesetConfig: StateFlow<RulesetConfig> = _rulesetConfig.asStateFlow()

    private val _difficulty = MutableStateFlow(repository.loadDifficulty())
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    private val _theme = MutableStateFlow(
        HandTheme.allThemes.find { it.name == repository.loadThemeName() } ?: HandTheme.AnalogSynth
    )
    val theme: StateFlow<HandTheme> = _theme.asStateFlow()

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val _invalidMoveHand = MutableStateFlow<Pair<String, Int>?>(null)
    val invalidMoveHand: StateFlow<Pair<String, Int>?> = _invalidMoveHand.asStateFlow()

    private val _hintState = MutableStateFlow<HintAnalysis?>(null)
    val hintState: StateFlow<HintAnalysis?> = _hintState.asStateFlow()

    private val _isHintLoading = MutableStateFlow(false)
    val isHintLoading: StateFlow<Boolean> = _isHintLoading.asStateFlow()

    var gameMode: GameMode = GameMode.PvP
        private set
    private var aiDifficulty: Difficulty = Difficulty.EASY
    private val aiPlayerId = "Player 2"
    private var isMovePending = false
    private var hasStartedGame = false

    /** The move animation and any AI reply that follows it. Cancelled when a new game starts. */
    private var moveJob: Job? = null
    private var hintJob: Job? = null

    /** Reused between hints in the same game: it already knows every position reachable from the start. */
    private var solver: GameSolver? = null

    /** How many times each position has occurred this game, for draw by repetition. */
    private val positionCounts = mutableMapOf<Position, Int>()

    fun updateRulesetConfig(config: RulesetConfig) {
        _rulesetConfig.value = config
        repository.saveRulesetConfig(config)
    }

    fun updateDifficulty(newDifficulty: Difficulty) {
        _difficulty.value = newDifficulty
        repository.saveDifficulty(newDifficulty)
    }

    fun updateTheme(newTheme: HandTheme) {
        _theme.value = newTheme
        repository.saveThemeName(newTheme.name)
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
        moveJob?.cancel()
        clearHint()
        hasStartedGame = true
        gameMode = mode
        aiDifficulty = difficulty
        isMovePending = false
        val state = GameState(
            player1 = Player("Player 1"),
            player2 = Player("Player 2"),
            currentPlayerId = "Player 1",
            ruleset = ruleset
        )
        positionCounts.clear()
        _uiState.value = recordPosition(state)
    }

    /** Starts a game unless one has already been started in this ViewModel. */
    fun ensureGameStarted(mode: GameMode, ruleset: RulesetConfig, difficulty: Difficulty) {
        if (!hasStartedGame) startGame(mode, ruleset, difficulty)
    }

    fun requestHint() {
        val currentState = _uiState.value
        if (isMovePending || currentState.isGameOver || isAiTurn(currentState)) return
        if (hintJob?.isActive == true) return

        hintJob = viewModelScope.launch {
            _isHintLoading.value = true
            try {
                val cached = solver
                val hint = withContext(Dispatchers.Default) {
                    val s = cached?.takeIf { it.covers(currentState) } ?: GameSolver(currentState)
                    s to HintAnalyzer.analyze(currentState, s)
                }.let { (s, analysis) -> solver = s; analysis }
                // Drop the hint if the board moved on while it was being worked out
                if (_uiState.value.position == currentState.position && !_uiState.value.isGameOver) {
                    _hintState.value = hint
                }
            } finally {
                _isHintLoading.value = false
            }
        }
    }

    /** Plays a move chosen from the hint analysis. */
    fun playHintMove(move: Move) = submitPlayerMove(move)

    fun clearHint() {
        hintJob?.cancel()
        _hintState.value = null
    }

    /**
     * Restarts the current game with the same settings.
     */
    fun restartGame() {
        val currentState = _uiState.value
        startGame(gameMode, currentState.ruleset, aiDifficulty)
    }

    fun performAttack(fromHandIndex: Int, toHandIndex: Int) =
        submitPlayerMove(Move.Attack(_uiState.value.currentPlayerId, fromHandIndex, toHandIndex))

    fun performTransfer(fromHandIndex: Int, toHandIndex: Int, amount: Int) =
        submitPlayerMove(Move.Transfer(_uiState.value.currentPlayerId, fromHandIndex, toHandIndex, amount))

    /** Exchange Life (Logan Clause). */
    fun performExchangeLife(sacrificedHandIndex: Int) =
        submitPlayerMove(Move.ExchangeLife(_uiState.value.currentPlayerId, sacrificedHandIndex))

    /** Condense (Meta rule). */
    fun performCondense(leftFingers: Int, rightFingers: Int) =
        submitPlayerMove(Move.Condense(_uiState.value.currentPlayerId, leftFingers, rightFingers))

    private fun submitPlayerMove(move: Move) {
        val currentState = _uiState.value
        if (isMovePending || currentState.isGameOver || isAiTurn(currentState)) return
        clearHint()

        if (!RulesEngine.validateMove(currentState, move)) {
            when (move) {
                is Move.Attack -> triggerInvalidMoveFeedback(move.playerId, move.fromHandIndex)
                is Move.Transfer -> triggerInvalidMoveFeedback(move.playerId, move.fromHandIndex)
                else -> {}
            }
            _uiState.value = currentState.copy(errorMessage = "Move not allowed")
            return
        }

        moveJob = viewModelScope.launch { playMove(currentState, move) }
    }

    /**
     * Shows the move animation, applies the move, checks for a draw, then lets the AI reply if it's its turn.
     */
    private suspend fun playMove(state: GameState, move: Move) {
        isMovePending = true
        val timestamp = System.currentTimeMillis()
        _uiState.value = state.copy(
            lastMove = move,
            lastMoveTimestamp = timestamp,
            errorMessage = null,
            isMovePending = true
        )

        delay(850)

        val newState = recordPosition(RulesEngine.applyMove(state, move))
        _uiState.value = newState.copy(
            lastMove = move,
            lastMoveTimestamp = timestamp,
            isMovePending = false
        )
        isMovePending = false

        if (!newState.isGameOver && isAiTurn(newState)) {
            delay(1000) // Small delay for UX to let user see the board
            val aiState = _uiState.value
            val aiMove = ChopsticksAI.chooseMove(aiState, aiDifficulty) ?: return
            playMove(aiState, aiMove)
        }
    }

    /** Counts the new position and ends the game in a draw if it has repeated too often or is stuck. */
    private fun recordPosition(state: GameState): GameState {
        if (state.isGameOver) return state
        val timesSeen = (positionCounts[state.position] ?: 0) + 1
        positionCounts[state.position] = timesSeen
        val drawReason = RulesEngine.detectDraw(state, timesSeen) ?: return state
        return state.copy(drawReason = drawReason)
    }

    private fun isAiTurn(state: GameState) = gameMode == GameMode.PvE && state.currentPlayerId == aiPlayerId

    private fun triggerInvalidMoveFeedback(playerId: String, handIndex: Int) {
        viewModelScope.launch {
            _invalidMoveHand.value = playerId to handIndex
            delay(500)
            _invalidMoveHand.value = null
        }
    }
}
