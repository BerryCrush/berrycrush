package org.berrycrush.assertion

import org.berrycrush.model.Assertion
import org.berrycrush.model.Condition
import org.berrycrush.openapi.OperationResolver
import org.berrycrush.openapi.impl.SwaggerParserAdapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssertionGeneratorTest {
    private val generator = AssertionGenerator()

    private val Assertion.builtinCondition: Condition?
        get() = (this as? Assertion.BuiltinAssertion)?.condition

    private fun loadPetstoreResolver(): OperationResolver {
        val spec = SwaggerParserAdapter().parse("/petstore.yaml")
        return OperationResolver(spec)
    }

    // Helper to check if assertion is a status assertion
    private fun Assertion.isStatusAssertion(): Boolean {
        val c = builtinCondition
        return c is Condition.Status || (c is Condition.Negated && c.condition is Condition.Status)
    }

    // Helper to check if assertion is a header assertion
    private fun Assertion.isHeaderAssertion(): Boolean {
        val c = builtinCondition ?: return false
        val unwrapped = if (c is Condition.Negated) c.condition else c
        return unwrapped is Condition.Header
    }

    // Helper to check if assertion is a schema assertion
    private fun Assertion.isSchemaAssertion(): Boolean {
        val c = builtinCondition
        return c is Condition.Schema || (c is Condition.Negated && c.condition is Condition.Schema)
    }

    // Helper to get expected status code
    private fun Assertion.getExpectedStatus(): Any? =
        when (val c = builtinCondition) {
            is Condition.Status -> c.expected
            is Condition.Negated -> (c.condition as? Condition.Status)?.expected
            else -> null
        }

    // Helper to get header name
    private fun Assertion.getHeaderName(): String? {
        val c = builtinCondition ?: return null
        val unwrapped = if (c is Condition.Negated) c.condition else c
        return (unwrapped as? Condition.Header)?.name
    }

    @Test
    fun `should generate status code assertion`() {
        val resolver = loadPetstoreResolver()
        val operation = resolver.resolve("listPets")

        val assertions =
            generator.generateAssertions(
                operation,
                expectedStatusCode = 200,
                includeContentType = false,
                includeSchema = false,
            )

        assertEquals(1, assertions.size)
        assertTrue(assertions[0].isStatusAssertion(), "Should be a status assertion")
        assertEquals(200, assertions[0].getExpectedStatus())
    }

    @Test
    fun `should generate content-type assertion`() {
        val resolver = loadPetstoreResolver()
        val operation = resolver.resolve("listPets")

        val assertions =
            generator.generateAssertions(
                operation,
                includeStatusCode = false,
                includeSchema = false,
            )

        assertTrue(assertions.any { it.isHeaderAssertion() && it.getHeaderName() == "Content-Type" })
    }

    @Test
    fun `should generate schema validation assertion`() {
        val resolver = loadPetstoreResolver()
        val operation = resolver.resolve("listPets")

        val assertions =
            generator.generateAssertions(
                operation,
                includeStatusCode = false,
                includeContentType = false,
            )

        assertTrue(assertions.any { it.isSchemaAssertion() })
    }

    @Test
    fun `should determine 201 for createPet`() {
        val resolver = loadPetstoreResolver()
        val operation = resolver.resolve("createPet")

        val statusCode = generator.determineSuccessStatusCode(operation)

        assertEquals(201, statusCode)
    }

    @Test
    fun `should determine 200 for listPets`() {
        val resolver = loadPetstoreResolver()
        val operation = resolver.resolve("listPets")

        val statusCode = generator.determineSuccessStatusCode(operation)

        assertEquals(200, statusCode)
    }

    @Test
    fun `should generate all assertions by default`() {
        val resolver = loadPetstoreResolver()
        val operation = resolver.resolve("getPetById")

        val assertions = generator.generateAssertions(operation)

        assertTrue(assertions.size >= 2) // At least status code and content-type
        assertTrue(assertions.any { it.isStatusAssertion() })
    }

    @Test
    fun `should handle deletePet with 204`() {
        val resolver = loadPetstoreResolver()
        val operation = resolver.resolve("deletePet")

        val statusCode = generator.determineSuccessStatusCode(operation)

        assertEquals(204, statusCode)
    }
}
