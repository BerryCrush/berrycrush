package org.berrycrush.step

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [DefaultStepRegistry].
 */
class StepRegistryTest {
    private lateinit var registry: DefaultStepRegistry

    @BeforeEach
    fun setup() {
        registry = DefaultStepRegistry()
    }

    @Test
    fun `register and find step definition`() {
        val method = TestSteps::class.java.getMethod("updateCount", Int::class.java)
        val instance = TestSteps()
        val definition =
            StepDefinition(
                pattern = "I have {int} items",
                method = method,
                instance = instance,
                description = "Sets the item count",
            )

        registry.register(definition)

        val match = registry.findMatch("I have 5 items")
        assertNotNull(match)
        assertEquals(definition, match.definition)
        assertEquals(listOf(5), match.parameters)
    }

    @Test
    fun `find returns null for no match`() {
        val method = TestSteps::class.java.getMethod("updateCount", Int::class.java)
        val definition =
            StepDefinition(
                pattern = "I have {int} items",
                method = method,
                instance = TestSteps(),
            )

        registry.register(definition)

        assertNull(registry.findMatch("I own 5 items"))
    }

    @Test
    fun `register all definitions at once`() {
        val instance = TestSteps()
        val definitions =
            listOf(
                StepDefinition(
                    pattern = "I have {int} items",
                    method = TestSteps::class.java.getMethod("updateCount", Int::class.java),
                    instance = instance,
                ),
                StepDefinition(
                    pattern = "the name is {string}",
                    method = TestSteps::class.java.getMethod("updateName", String::class.java),
                    instance = instance,
                ),
            )

        registry.registerAll(definitions)

        assertEquals(2, registry.allDefinitions().size)
        assertNotNull(registry.findMatch("I have 10 items"))
        assertNotNull(registry.findMatch("the name is \"Test\""))
    }

    @Test
    fun `clear removes all definitions`() {
        val definition =
            StepDefinition(
                pattern = "I have {int} items",
                method = TestSteps::class.java.getMethod("updateCount", Int::class.java),
                instance = TestSteps(),
            )

        registry.register(definition)
        assertEquals(1, registry.allDefinitions().size)

        registry.clear()
        assertTrue(registry.allDefinitions().isEmpty())
    }

    @Test
    fun `first matching definition wins`() {
        val instance1 = TestSteps()
        val instance2 = TestSteps()

        registry.register(
            StepDefinition(
                pattern = "I have {int} items",
                method = TestSteps::class.java.getMethod("updateCount", Int::class.java),
                instance = instance1,
                description = "First",
            ),
        )
        registry.register(
            StepDefinition(
                pattern = "I have {int} items",
                method = TestSteps::class.java.getMethod("updateCount", Int::class.java),
                instance = instance2,
                description = "Second",
            ),
        )

        val match = registry.findMatch("I have 5 items")
        assertNotNull(match)
        assertEquals("First", match.definition.description)
    }

    // Test helper class
    class TestSteps {
        var count: Int = 0
            private set
        var name: String = ""
            private set

        fun updateCount(value: Int) {
            count = value
        }

        fun updateName(value: String) {
            name = value
        }
    }
}
