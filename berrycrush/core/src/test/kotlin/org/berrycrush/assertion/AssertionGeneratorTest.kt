package org.berrycrush.assertion

import org.berrycrush.model.AssertionType
import org.berrycrush.openapi.OpenApiLoader
import org.berrycrush.openapi.OperationResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssertionGeneratorTest {
    private val generator = AssertionGenerator()

    private fun loadPetstoreResolver(): OperationResolver {
        val specPath =
            javaClass.getResource("/petstore.yaml")?.path
                ?: error("petstore.yaml not found in test resources")
        val openApi = OpenApiLoader().load(specPath)
        return OperationResolver(openApi)
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
        assertEquals(AssertionType.STATUS_CODE, assertions[0].type)
        assertEquals(200, assertions[0].expected)
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

        assertTrue(assertions.any { it.type == AssertionType.HEADER_EQUALS && it.headerName == "Content-Type" })
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

        assertTrue(assertions.any { it.type == AssertionType.MATCHES_SCHEMA })
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
        assertTrue(assertions.any { it.type == AssertionType.STATUS_CODE })
    }

    @Test
    fun `should handle deletePet with 204`() {
        val resolver = loadPetstoreResolver()
        val operation = resolver.resolve("deletePet")

        val statusCode = generator.determineSuccessStatusCode(operation)

        assertEquals(204, statusCode)
    }
}
