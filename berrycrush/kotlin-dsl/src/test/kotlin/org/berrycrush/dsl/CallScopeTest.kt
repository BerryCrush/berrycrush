package org.berrycrush.dsl

import org.berrycrush.scenario.AutoTestType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import tools.jackson.databind.ObjectMapper

class CallScopeTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `call scope branch test`() {
        val suite =
            berrycrush {
                scenario("call scope test") {
                    given("call") {
                        call("operationId") {
                            body("a" to "b", "b" to 1, "c" to mapOf("d" to "d"), "d" to listOf(1, 2, 3))
                            autoAssert(true)
                            autoTest()
                            autoTest(invalid = true, security = true, multi = true)
                        }
                    }
                }
                scenario("call scope test") {
                    given("call") {
                        call("operationId2") {
                            autoTest(AutoTestType.SECURITY)
                            excludes("SQLInjection")
                        }
                    }
                }
            }
        assertEquals("operationId", suite.scenarios[0].steps[0].operationId)
        assertNotNull(suite.scenarios[0].steps[0].body)
        assertTrue(checkJson(suite.scenarios[0].steps[0].body!!))
        assertEquals("operationId2", suite.scenarios[1].steps[0].operationId)
    }

    private fun checkJson(body: String): Boolean =
        try {
            objectMapper.readTree(body)
            true
        } catch (_: Exception) {
            false
        }
}
