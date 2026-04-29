package org.berrycrush.samples.tictactoe.model

/**
 * Represents a 3x3 TicTacToe board.
 *
 * Coordinates are 1-based (1-3) following the OpenAPI spec.
 */
class Board {
    private val grid: Array<Array<Mark>> = Array(3) { Array(3) { Mark.EMPTY } }

    /**
     * Gets the mark at the specified position.
     *
     * @param row Row number (1-3)
     * @param column Column number (1-3)
     * @return The mark at the position
     * @throws IllegalArgumentException if coordinates are invalid
     */
    fun get(row: Int, column: Int): Mark {
        validateCoordinates(row, column)
        return grid[row - 1][column - 1]
    }

    /**
     * Sets a mark at the specified position.
     *
     * @param row Row number (1-3)
     * @param column Column number (1-3)
     * @param mark The mark to place
     * @throws IllegalArgumentException if coordinates are invalid
     * @throws IllegalStateException if the square is already occupied
     */
    fun set(row: Int, column: Int, mark: Mark) {
        validateCoordinates(row, column)
        require(mark != Mark.EMPTY) { "Cannot place empty mark" }
        check(grid[row - 1][column - 1] == Mark.EMPTY) { "Square is not empty." }
        grid[row - 1][column - 1] = mark
    }

    /**
     * Converts the board to a nested list representation.
     *
     * @return 3x3 list of mark symbols
     */
    fun toList(): List<List<String>> =
        grid.map { row -> row.map { it.symbol } }

    /**
     * Checks for a winner.
     *
     * @return The winning mark, or EMPTY if no winner yet
     */
    fun checkWinner(): Mark {
        // Check rows
        for (row in 0..2) {
            if (grid[row][0] != Mark.EMPTY &&
                grid[row][0] == grid[row][1] &&
                grid[row][1] == grid[row][2]
            ) {
                return grid[row][0]
            }
        }

        // Check columns
        for (col in 0..2) {
            if (grid[0][col] != Mark.EMPTY &&
                grid[0][col] == grid[1][col] &&
                grid[1][col] == grid[2][col]
            ) {
                return grid[0][col]
            }
        }

        // Check diagonals
        if (grid[0][0] != Mark.EMPTY &&
            grid[0][0] == grid[1][1] &&
            grid[1][1] == grid[2][2]
        ) {
            return grid[0][0]
        }

        if (grid[0][2] != Mark.EMPTY &&
            grid[0][2] == grid[1][1] &&
            grid[1][1] == grid[2][0]
        ) {
            return grid[0][2]
        }

        return Mark.EMPTY
    }

    /**
     * Resets the board to empty state.
     */
    fun reset() {
        for (row in 0..2) {
            for (col in 0..2) {
                grid[row][col] = Mark.EMPTY
            }
        }
    }

    private fun validateCoordinates(row: Int, column: Int) {
        require(row in 1..3) { "Illegal coordinates." }
        require(column in 1..3) { "Illegal coordinates." }
    }
}
