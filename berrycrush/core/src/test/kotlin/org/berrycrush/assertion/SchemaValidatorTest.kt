package org.berrycrush.assertion

import kotlin.test.Test
import kotlin.test.assertTrue

class SchemaValidatorTest {
    private val validator = SchemaValidator()

    @Test
    fun `should validate valid JSON against schema`() {
        val schema =
            """
            {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "age": {"type": "integer"}
                },
                "required": ["name"]
            }
            """.trimIndent()

        val json = """{"name": "John", "age": 30}"""

        val errors = validator.validateAgainstJsonSchema(json, schema)

        assertTrue(errors.isEmpty(), "Expected no validation errors but got: $errors")
    }

    @Test
    fun `should detect missing required field`() {
        val schema =
            """
            {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "age": {"type": "integer"}
                },
                "required": ["name"]
            }
            """.trimIndent()

        val json = """{"age": 30}"""

        val errors = validator.validateAgainstJsonSchema(json, schema)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.message.contains("name") || it.keyword == "required" })
    }

    @Test
    fun `should detect type mismatch`() {
        val schema =
            """
            {
                "type": "object",
                "properties": {
                    "age": {"type": "integer"}
                }
            }
            """.trimIndent()

        val json = """{"age": "not a number"}"""

        val errors = validator.validateAgainstJsonSchema(json, schema)

        assertTrue(errors.isNotEmpty())
    }

    @Test
    fun `should validate array items`() {
        val schema =
            """
            {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "id": {"type": "integer"}
                    },
                    "required": ["id"]
                }
            }
            """.trimIndent()

        val validJson = """[{"id": 1}, {"id": 2}]"""
        val invalidJson = """[{"id": 1}, {"name": "no id"}]"""

        val validErrors = validator.validateAgainstJsonSchema(validJson, schema)
        val invalidErrors = validator.validateAgainstJsonSchema(invalidJson, schema)

        assertTrue(validErrors.isEmpty())
        assertTrue(invalidErrors.isNotEmpty())
    }

    @Test
    fun `should validate string patterns`() {
        val schema =
            """
            {
                "type": "object",
                "properties": {
                    "email": {
                        "type": "string",
                        "pattern": "^[a-z]+@[a-z]+\\.[a-z]+$"
                    }
                }
            }
            """.trimIndent()

        val validJson = """{"email": "test@example.com"}"""
        val invalidJson = """{"email": "invalid-email"}"""

        val validErrors = validator.validateAgainstJsonSchema(validJson, schema)
        val invalidErrors = validator.validateAgainstJsonSchema(invalidJson, schema)

        assertTrue(validErrors.isEmpty())
        assertTrue(invalidErrors.isNotEmpty())
    }

    @Test
    fun `should validate number ranges`() {
        val schema =
            """
            {
                "type": "object",
                "properties": {
                    "age": {
                        "type": "integer",
                        "minimum": 0,
                        "maximum": 150
                    }
                }
            }
            """.trimIndent()

        val validJson = """{"age": 30}"""
        val invalidJson = """{"age": 200}"""

        val validErrors = validator.validateAgainstJsonSchema(validJson, schema)
        val invalidErrors = validator.validateAgainstJsonSchema(invalidJson, schema)

        assertTrue(validErrors.isEmpty())
        assertTrue(invalidErrors.isNotEmpty())
    }

    @Test
    fun `should validate enum values`() {
        val schema =
            """
            {
                "type": "object",
                "properties": {
                    "status": {
                        "type": "string",
                        "enum": ["available", "pending", "sold"]
                    }
                }
            }
            """.trimIndent()

        val validJson = """{"status": "available"}"""
        val invalidJson = """{"status": "unknown"}"""

        val validErrors = validator.validateAgainstJsonSchema(validJson, schema)
        val invalidErrors = validator.validateAgainstJsonSchema(invalidJson, schema)

        assertTrue(validErrors.isEmpty())
        assertTrue(invalidErrors.isNotEmpty())
    }

    @Test
    fun `should provide error details`() {
        val schema =
            """
            {
                "type": "object",
                "properties": {
                    "name": {"type": "string"}
                },
                "required": ["name"]
            }
            """.trimIndent()

        val json = """{"age": 30}"""

        val errors = validator.validateAgainstJsonSchema(json, schema)

        assertTrue(errors.isNotEmpty())
        val error = errors.first()
        assertTrue(error.path.isNotEmpty() || error.message.isNotEmpty())
    }
}
