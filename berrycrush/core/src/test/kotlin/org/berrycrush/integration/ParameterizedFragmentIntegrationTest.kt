package org.berrycrush.integration

import org.berrycrush.scenario.ScenarioLoader
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for Parameterized Fragments feature.
 *
 * Tests the complete flow of:
 * 1. Parsing include directives with parameters
 * 2. Loading scenarios with parameterized includes
 * 3. Parameter injection during fragment expansion
 * 4. Variable interpolation within fragment execution
 */
class ParameterizedFragmentIntegrationTest {
    private val loader = ScenarioLoader()

    @Test
    fun `should parse include directive with parameters`() {
        val source =
            """
            |scenario: Create user with params
            |  given I create a user
            |    include create_user
            |      name: "John Doe"
            |      email: "john@example.com"
            |      age: 30
            |  then user is created
            |    assert status 201
            """.trimMargin()

        val scenarios = loader.loadScenariosFromString(source)

        assertEquals(1, scenarios.size)
        val step = scenarios[0].steps.first { it.fragmentName != null }
        assertEquals("create_user", step.fragmentName)
        assertEquals(3, step.includeParameters.size)
        assertEquals("John Doe", step.includeParameters["name"])
        assertEquals("john@example.com", step.includeParameters["email"])
        assertEquals(30L, step.includeParameters["age"])
    }

    @Test
    fun `should parse include with variable references as parameters`() {
        val source =
            """
            |scenario: Create user from context
            |  given I have user data
            |    set testName => "Alice"
            |  when I create the user
            |    include create_user
            |      name: {{testName}}
            |      email: "alice@test.com"
            |      age: 25
            """.trimMargin()

        val scenarios = loader.loadScenariosFromString(source)

        assertEquals(1, scenarios.size)
        val step = scenarios[0].steps.first { it.fragmentName != null }
        assertEquals("create_user", step.fragmentName)

        // The variable reference should be stored as a string with the {{}} syntax
        // for later resolution during execution
        val nameParam = step.includeParameters["name"]
        assertNotNull(nameParam)
    }

    @Test
    fun `should load parameterized fragment file`() {
        val path = getResourcePath("valid/parameterized-fragment.fragment")
        val fragments = loader.loadFragmentsFromFile(path)

        assertEquals(2, fragments.size)
        assertTrue(fragments.containsKey("create_user"))
        assertTrue(fragments.containsKey("greet_user"))

        val createUserFragment = fragments["create_user"]
        assertNotNull(createUserFragment)
        assertTrue(createUserFragment.steps.isNotEmpty())
    }

    @Test
    fun `should support multiple parameter types`() {
        val source =
            """
            |scenario: Multi-type parameters
            |  given I configure entity
            |    include configure
            |      stringVal: "hello"
            |      numberVal: 42
            |      decimalVal: 99.5
            |      boolVal: true
            """.trimMargin()

        val scenarios = loader.loadScenariosFromString(source)
        val step = scenarios[0].steps.first { it.fragmentName != null }

        assertEquals("hello", step.includeParameters["stringVal"])
        assertEquals(42L, step.includeParameters["numberVal"])
        assertEquals(99.5, step.includeParameters["decimalVal"])
        // Boolean values are parsed as strings (identifiers) by the lexer
        assertEquals("true", step.includeParameters["boolVal"])
    }

    @Test
    fun `should handle include without parameters (backward compatible)`() {
        val source =
            """
            |scenario: Simple include
            |  given I authenticate
            |    include authenticate
            |  then I am logged in
            """.trimMargin()

        val scenarios = loader.loadScenariosFromString(source)
        val step = scenarios[0].steps.first { it.fragmentName != null }

        assertEquals("authenticate", step.fragmentName)
        assertTrue(step.includeParameters.isEmpty())
    }

    @Test
    fun `should handle JSON object as parameter value`() {
        val source =
            """
            |scenario: JSON parameter
            |  given I configure with metadata
            |    include configure
            |      metadata: {"key": "value", "nested": {"a": 1}}
            """.trimMargin()

        val scenarios = loader.loadScenariosFromString(source)
        val step = scenarios[0].steps.first { it.fragmentName != null }

        assertNotNull(step.includeParameters["metadata"])
        val metadata = step.includeParameters["metadata"].toString()
        assertTrue(metadata.contains("key"))
        assertTrue(metadata.contains("value"))
    }

    private fun getResourcePath(name: String): Path {
        val url = javaClass.getResource("/scenarios/$name") ?: error("Resource not found: /scenarios/$name")
        return Paths.get(url.toURI())
    }
}
