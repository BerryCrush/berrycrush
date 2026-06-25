package org.berrycrush.dsl

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals

class StepScopeTest {
    @Test
    fun `assertions should be created correctly`() {
        val suite =
            berrycrush {
                scenario("step test") {
                    whenever("step is executed") {
                        headerExists("X-Header")
                        headerEquals("X-Header", "value")
                        bodyContains("body")
                        bodyEquals(".message", "body")
                        bodyArrayNotEmpty(".array")
                        bodyMatches(".message", "pattern")
                        bodyArraySize(".array", 3)
                        matchesSchema()
                        responseTime(10L)
                        responseTime(Duration.ofNanos(1000))
                        assert("must pass this") {
                        }
                        conditional({ ctx -> ctx.statusCode == 200 }) {
                            assert("success case") {
                            }
                        } orElse {
                            assert("error case") {
                            }
                        }
                    }
                }
            }
        val step = suite.scenarios[0].steps[0]
        assertEquals(10, step.assertions.size)
        assertEquals(1, step.conditionals.size)
    }
}
