package org.berrycrush.samples.tictactoe

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * TicTacToe Sample Application.
 *
 * Demonstrates BerryCrush testing with OpenAPI 3.1.x features.
 */
@SpringBootApplication
class TicTacToeApplication

fun main(args: Array<String>) {
    runApplication<TicTacToeApplication>(*args)
}
