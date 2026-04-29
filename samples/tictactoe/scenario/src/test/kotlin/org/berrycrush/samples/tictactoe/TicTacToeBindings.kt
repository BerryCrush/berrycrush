package org.berrycrush.samples.tictactoe

import org.berrycrush.junit.BerryCrushBindings
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/**
 * Bindings class that provides configuration to BerryCrush scenarios.
 *
 * This class is a Spring component so it can use @LocalServerPort
 * to get the dynamic port assigned to the test server.
 *
 * Note: @Lazy is required because @LocalServerPort is only available
 * after the web server has started, which happens after initial bean creation.
 */
@Component
@Lazy
class TicTacToeBindings : BerryCrushBindings {
    @LocalServerPort
    var port: Int = 0

    override fun getBindings(): Map<String, Any> =
        mapOf(
            "baseUrl" to "http://localhost:$port",
        )

    override fun getOpenApiSpec(): String = "openapi/tictactoe.yaml"
}
