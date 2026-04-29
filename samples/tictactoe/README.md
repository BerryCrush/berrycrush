# TicTacToe Sample

A sample TicTacToe application demonstrating BerryCrush testing with OpenAPI 3.1.x features.

## Overview

This sample shows how to:
- Test a REST API using BerryCrush scenarios
- Work with OpenAPI 3.1.x features (Links, Callbacks, Webhooks)
- Use Spring Boot test integration

## API Endpoints

| Method | Path | Operation ID | Description |
|--------|------|--------------|-------------|
| GET | `/board` | `get-board` | Get the whole board and winner |
| POST | `/board/reset` | `reset-board` | Reset the game board |
| GET | `/board/{row}/{column}` | `get-square` | Get a single square |
| PUT | `/board/{row}/{column}` | `put-square` | Place a mark (X or O) |

## Running the Tests

```bash
./gradlew :samples:tictactoe:scenario:test
```

## Test Scenarios

### new-game.scenario
- Initial board is empty
- Get empty square returns dot
- Reset clears the board

### player-wins.scenario
- Player X wins with horizontal row 1
- Player O wins with diagonal

### validation.scenario
- Invalid row coordinate returns 400
- Invalid column coordinate returns 400
- Placing mark on occupied square returns 400
- Invalid mark returns 400

## OpenAPI 3.1.x Features

The OpenAPI spec demonstrates these 3.1.x features:

- **Links**: `markSquare` link from `get-square` to `put-square`
- **Callbacks**: `statusCallback` for async progress notification
- **Webhooks**: `markStatus` webhook for game completion

## Project Structure

```
samples/tictactoe/
├── README.md
├── app/
│   └── src/main/kotlin/
│       └── org/berrycrush/samples/tictactoe/
│           ├── TicTacToeApplication.kt
│           ├── controller/BoardController.kt
│           ├── model/{Mark,Board,GameStatus}.kt
│           └── service/GameService.kt
└── scenario/
    └── src/test/
        ├── kotlin/
        │   └── org/berrycrush/samples/tictactoe/
        │       ├── TicTacToeTest.kt
        │       └── TicTacToeBindings.kt
        └── resources/
            ├── openapi/tictactoe.yaml
            └── scenarios/
                ├── new-game.scenario
                ├── player-wins.scenario
                └── validation.scenario
```
