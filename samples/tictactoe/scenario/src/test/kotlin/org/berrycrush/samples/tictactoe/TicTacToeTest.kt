package org.berrycrush.samples.tictactoe

import org.berrycrush.junit.BerryCrushConfiguration
import org.berrycrush.junit.BerryCrushScenarios
import org.berrycrush.junit.BerryCrushSpec
import org.berrycrush.spring.BerryCrushContextConfiguration
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.Suite
import org.springframework.boot.test.context.SpringBootTest

/**
 * Integration test for TicTacToe API using BerryCrush scenarios.
 *
 * This test demonstrates BerryCrush with OpenAPI 3.1.x features:
 * - Links
 * - Callbacks
 * - Webhooks
 */
@Suite
@IncludeEngines("berrycrush")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TicTacToeApplication::class],
)
@BerryCrushContextConfiguration
@BerryCrushScenarios(locations = ["scenarios/*.scenario"])
@BerryCrushConfiguration(
    bindings = TicTacToeBindings::class,
    openApiSpec = "openapi/tictactoe.yaml",
    plugins = ["report:console"],
)
@BerryCrushSpec(paths = ["openapi/tictactoe.yaml"])
class TicTacToeTest
