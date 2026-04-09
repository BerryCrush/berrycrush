package io.github.ktakashi.lemoncheck.step

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the [StepBuilder] DSL.
 */
class StepDslTest {
    @Test
    fun `define step with no parameters`() {
        val builder =
            steps {
                step("I do nothing") {
                    "result"
                }
            }

        val definitions = builder.build()
        assertEquals(1, definitions.size)
        assertEquals("I do nothing", definitions[0].pattern)
    }

    @Test
    fun `define step with one parameter`() {
        var capturedValue: Int? = null

        val builder =
            steps {
                step<Int>("I have {int} items") { count ->
                    capturedValue = count
                }
            }

        val definitions = builder.build()
        assertEquals(1, definitions.size)

        // Invoke the step
        definitions[0].method.invoke(definitions[0].instance, 5)
        assertEquals(5, capturedValue)
    }

    @Test
    fun `define step with two parameters`() {
        var result: Int? = null

        val builder =
            steps {
                step<Int, Int>("I add {int} and {int}") { a, b ->
                    result = a + b
                }
            }

        val definitions = builder.build()
        assertEquals(1, definitions.size)

        // Invoke the step
        definitions[0].method.invoke(definitions[0].instance, 3, 4)
        assertEquals(7, result)
    }

    @Test
    fun `define step with three parameters`() {
        var captured: Triple<Int, String, Int>? = null

        val builder =
            steps {
                step<Int, String, Int>("I have {int} {string} weighing {int} kg") { a, b, c ->
                    captured = Triple(a, b, c)
                }
            }

        val definitions = builder.build()
        definitions[0].method.invoke(definitions[0].instance, 2, "cats", 5)
        assertEquals(Triple(2, "cats", 5), captured)
    }

    @Test
    fun `define step with description`() {
        val builder =
            steps {
                step("I do something", description = "This step does something") {
                    // action
                }
            }

        val definitions = builder.build()
        assertEquals("This step does something", definitions[0].description)
    }

    @Test
    fun `define multiple steps`() {
        val builder =
            steps {
                step("step one") { }
                step<Int>("step with {int}") { _ -> }
                step<String, Int>("step with {string} and {int}") { _, _ -> }
            }

        val definitions = builder.build()
        assertEquals(3, definitions.size)
    }

    @Test
    fun `register to registry`() {
        val registry = DefaultStepRegistry()

        steps {
            step<Int>("I have {int} items") { _ -> }
            step<String>("the name is {string}") { _ -> }
        }.registerTo(registry)

        assertEquals(2, registry.allDefinitions().size)
        assertNotNull(registry.findMatch("I have 5 items"))
        assertNotNull(registry.findMatch("the name is \"Test\""))
    }

    @Test
    fun `step with return value`() {
        val builder =
            steps {
                step<Int, Int>("I add {int} and {int}") { a, b -> a + b }
            }

        val definitions = builder.build()
        val result = definitions[0].method.invoke(definitions[0].instance, 10, 20)
        assertEquals(30, result)
    }

    @Test
    fun `define step with four parameters`() {
        var sum: Int? = null

        val builder =
            steps {
                step<Int, Int, Int, Int>("{int} + {int} + {int} + {int}") { a, b, c, d ->
                    sum = a + b + c + d
                }
            }

        val definitions = builder.build()
        definitions[0].method.invoke(definitions[0].instance, 1, 2, 3, 4)
        assertEquals(10, sum)
    }

    @Test
    fun `define step with five parameters`() {
        var sum: Int? = null

        val builder =
            steps {
                step<Int, Int, Int, Int, Int>("{int} {int} {int} {int} {int}") { a, b, c, d, e ->
                    sum = a + b + c + d + e
                }
            }

        val definitions = builder.build()
        definitions[0].method.invoke(definitions[0].instance, 1, 2, 3, 4, 5)
        assertEquals(15, sum)
    }
}
