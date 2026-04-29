package org.berrycrush.samples.tictactoe.model

import com.fasterxml.jackson.annotation.JsonValue

/**
 * Represents a mark on the TicTacToe board.
 *
 * - EMPTY (".") - Square has no mark
 * - X ("X") - Player X's mark
 * - O ("O") - Player O's mark
 */
enum class Mark(
    @get:JsonValue val symbol: String
) {
    EMPTY("."),
    X("X"),
    O("O");

    companion object {
        fun fromSymbol(symbol: String): Mark? =
            entries.find { it.symbol == symbol }
    }
}
