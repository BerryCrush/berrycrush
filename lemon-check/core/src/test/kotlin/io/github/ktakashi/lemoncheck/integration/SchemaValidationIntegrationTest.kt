package io.github.ktakashi.lemoncheck.integration

import io.github.ktakashi.lemoncheck.assertion.SchemaValidator
import io.github.ktakashi.lemoncheck.config.AutoAssertionConfig
import io.github.ktakashi.lemoncheck.config.Configuration
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration test for Schema Validation (User Story 5).
 *
 * Tests the complete flow of:
 * 1. Validating responses against OpenAPI schemas
 * 2. Auto-generating assertions from OpenAPI spec
 * 3. Strict vs lenient validation modes
 */
class SchemaValidationIntegrationTest {
    private val schemaValidator = SchemaValidator()

    @Test
    fun `should validate Pet response against OpenAPI Pet schema`() {
        // Simulating a Pet response from the Petstore API
        val petSchema =
            """
            {
                "type": "object",
                "required": ["id", "name"],
                "properties": {
                    "id": {"type": "integer", "format": "int64"},
                    "name": {"type": "string"},
                    "tag": {"type": "string"},
                    "status": {"type": "string", "enum": ["available", "pending", "sold"]}
                }
            }
            """.trimIndent()

        val validPetResponse =
            """
            {
                "id": 123,
                "name": "Fluffy",
                "tag": "dog",
                "status": "available"
            }
            """.trimIndent()

        val errors = schemaValidator.validateAgainstJsonSchema(validPetResponse, petSchema)

        assertTrue(errors.isEmpty(), "Expected valid Pet to pass schema validation but got: $errors")
    }

    @Test
    fun `should detect missing required fields in Pet response`() {
        val petSchema =
            """
            {
                "type": "object",
                "required": ["id", "name"],
                "properties": {
                    "id": {"type": "integer"},
                    "name": {"type": "string"}
                }
            }
            """.trimIndent()

        val invalidPetResponse = """{"id": 123}""" // missing "name"

        val errors = schemaValidator.validateAgainstJsonSchema(invalidPetResponse, petSchema)

        assertTrue(errors.isNotEmpty(), "Expected validation errors for missing required field 'name'")
        assertTrue(errors.any { it.message.contains("name") || it.keyword == "required" })
    }

    @Test
    fun `should detect invalid enum value in status`() {
        val petSchema =
            """
            {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "enum": ["available", "pending", "sold"]}
                }
            }
            """.trimIndent()

        val invalidStatusResponse = """{"status": "unknown_status"}"""

        val errors = schemaValidator.validateAgainstJsonSchema(invalidStatusResponse, petSchema)

        assertTrue(errors.isNotEmpty(), "Expected validation error for invalid enum value")
    }

    @Test
    fun `should validate array of Pets response`() {
        val petsArraySchema =
            """
            {
                "type": "array",
                "items": {
                    "type": "object",
                    "required": ["id", "name"],
                    "properties": {
                        "id": {"type": "integer"},
                        "name": {"type": "string"}
                    }
                }
            }
            """.trimIndent()

        val validPetsResponse =
            """
            [
                {"id": 1, "name": "Fluffy"},
                {"id": 2, "name": "Buddy"},
                {"id": 3, "name": "Charlie"}
            ]
            """.trimIndent()

        val errors = schemaValidator.validateAgainstJsonSchema(validPetsResponse, petsArraySchema)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should validate nested objects in Order response`() {
        val orderSchema =
            """
            {
                "type": "object",
                "required": ["id", "petId", "quantity"],
                "properties": {
                    "id": {"type": "integer"},
                    "petId": {"type": "integer"},
                    "quantity": {"type": "integer", "minimum": 1},
                    "shipDate": {"type": "string", "format": "date-time"},
                    "status": {"type": "string", "enum": ["placed", "approved", "delivered"]},
                    "complete": {"type": "boolean"}
                }
            }
            """.trimIndent()

        val validOrderResponse =
            """
            {
                "id": 1001,
                "petId": 123,
                "quantity": 2,
                "status": "placed",
                "complete": false
            }
            """.trimIndent()

        val errors = schemaValidator.validateAgainstJsonSchema(validOrderResponse, orderSchema)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should handle configuration with auto assertions enabled`() {
        val config =
            Configuration(
                autoAssertions =
                    AutoAssertionConfig(
                        enabled = true,
                        statusCode = true,
                        contentType = true,
                        schema = true,
                    ),
                strictSchemaValidation = false,
            )

        assertTrue(config.autoAssertions.enabled)
        assertTrue(config.autoAssertions.schema)
        assertTrue(!config.strictSchemaValidation) // lenient mode
    }

    @Test
    fun `should support strict validation mode`() {
        val config =
            Configuration(
                strictSchemaValidation = true,
            )

        assertTrue(config.strictSchemaValidation)
    }

    @Test
    fun `should validate error response schema`() {
        val errorSchema =
            """
            {
                "type": "object",
                "required": ["code", "message"],
                "properties": {
                    "code": {"type": "integer"},
                    "message": {"type": "string"}
                }
            }
            """.trimIndent()

        val validErrorResponse =
            """
            {
                "code": 404,
                "message": "Pet not found"
            }
            """.trimIndent()

        val invalidErrorResponse =
            """
            {
                "error": "Pet not found"
            }
            """.trimIndent()

        val validErrors = schemaValidator.validateAgainstJsonSchema(validErrorResponse, errorSchema)
        val invalidErrors = schemaValidator.validateAgainstJsonSchema(invalidErrorResponse, errorSchema)

        assertTrue(validErrors.isEmpty())
        assertTrue(invalidErrors.isNotEmpty())
    }

    @Test
    fun `should provide detailed validation error information`() {
        val schema =
            """
            {
                "type": "object",
                "properties": {
                    "count": {"type": "integer", "minimum": 0, "maximum": 100}
                }
            }
            """.trimIndent()

        val invalidResponse = """{"count": 150}"""

        val errors = schemaValidator.validateAgainstJsonSchema(invalidResponse, schema)

        assertTrue(errors.isNotEmpty())
        val error = errors.first()
        assertTrue(error.message.isNotBlank() || !error.keyword.isNullOrBlank())
    }
}
