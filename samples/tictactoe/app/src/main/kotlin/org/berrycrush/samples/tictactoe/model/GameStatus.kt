package org.berrycrush.samples.tictactoe.model

/**
 * Represents the current game status including the board state and winner.
 */
data class GameStatus(
    val winner: String,
    val board: List<List<String>>
)
