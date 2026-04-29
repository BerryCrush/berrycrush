package org.berrycrush.samples.tictactoe.service

import org.berrycrush.samples.tictactoe.model.Board
import org.berrycrush.samples.tictactoe.model.GameStatus
import org.berrycrush.samples.tictactoe.model.Mark
import org.springframework.stereotype.Service

/**
 * Service that manages the TicTacToe game state.
 */
@Service
class GameService {
    private val board = Board()

    /**
     * Gets the current game status.
     */
    fun getStatus(): GameStatus = GameStatus(
        winner = board.checkWinner().symbol,
        board = board.toList()
    )

    /**
     * Gets the mark at the specified square.
     *
     * @param row Row number (1-3)
     * @param column Column number (1-3)
     * @return The mark at the position
     */
    fun getSquare(row: Int, column: Int): Mark =
        board.get(row, column)

    /**
     * Places a mark on the board.
     *
     * @param row Row number (1-3)
     * @param column Column number (1-3)
     * @param mark The mark to place ("X" or "O")
     * @return The updated game status
     * @throws IllegalArgumentException for invalid mark or coordinates
     * @throws IllegalStateException if square is not empty
     */
    fun placeMark(row: Int, column: Int, markSymbol: String): GameStatus {
        val mark = Mark.fromSymbol(markSymbol)
            ?: throw IllegalArgumentException("Invalid Mark (X or O).")

        require(mark != Mark.EMPTY) { "Invalid Mark (X or O)." }

        board.set(row, column, mark)
        return getStatus()
    }

    /**
     * Resets the board to start a new game.
     */
    fun reset(): GameStatus {
        board.reset()
        return getStatus()
    }
}
