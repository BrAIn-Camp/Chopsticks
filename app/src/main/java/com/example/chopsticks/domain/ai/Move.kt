package com.example.chopsticks.domain.ai

sealed class Move {
    abstract val playerId: String
    data class Attack(override val playerId: String, val fromHandIndex: Int, val toHandIndex: Int) : Move()
    data class Transfer(override val playerId: String, val fromHandIndex: Int, val toHandIndex: Int, val amount: Int) : Move()
    data class ExchangeLife(override val playerId: String, val sacrificedHandIndex: Int) : Move()
    data class Condense(override val playerId: String, val leftFingers: Int, val rightFingers: Int) : Move()
}
