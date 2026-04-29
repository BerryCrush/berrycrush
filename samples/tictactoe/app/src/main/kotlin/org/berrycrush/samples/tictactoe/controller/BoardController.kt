package org.berrycrush.samples.tictactoe.controller

import org.berrycrush.samples.tictactoe.model.GameStatus
import org.berrycrush.samples.tictactoe.model.MarkRequest
import org.berrycrush.samples.tictactoe.service.GameService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for TicTacToe game operations.
 */
@RestController
class BoardController(
    private val gameService: GameService
) {
    /**
     * GET /board - Get the whole board and winner.
     */
    @GetMapping("/board")
    fun getBoard(): GameStatus = gameService.getStatus()

    /**
     * POST /board/reset - Reset the game board.
     */
    @PostMapping("/board/reset")
    fun resetBoard(): GameStatus = gameService.reset()

    /**
     * GET /board/{row}/{column} - Get a single square.
     */
    @GetMapping("/board/{row}/{column}")
    fun getSquare(
        @PathVariable row: Int,
        @PathVariable column: Int
    ): ResponseEntity<Any> = try {
        ResponseEntity.ok(gameService.getSquare(row, column).symbol)
    } catch (e: IllegalArgumentException) {
        ResponseEntity.badRequest().body(e.message)
    }

    /**
     * PUT /board/{row}/{column} - Place a mark on a square.
     */
    @PutMapping("/board/{row}/{column}")
    fun putSquare(
        @PathVariable row: Int,
        @PathVariable column: Int,
        @RequestBody request: MarkRequest
    ): ResponseEntity<Any> = try {
        ResponseEntity.ok(gameService.placeMark(row, column, request.mark))
    } catch (e: IllegalArgumentException) {
        ResponseEntity.badRequest().body(e.message)
    } catch (e: IllegalStateException) {
        ResponseEntity.badRequest().body(e.message)
    }
}
