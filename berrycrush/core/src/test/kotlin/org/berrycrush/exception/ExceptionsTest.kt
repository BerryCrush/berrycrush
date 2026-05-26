package org.berrycrush.exception

import org.berrycrush.plugin.HttpRequest
import org.berrycrush.plugin.HttpResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ExceptionsTest {
    @Nested
    inner class ScenarioErrorContextTest {
        @Test
        fun `should create context with all fields`() {
            val context =
                ScenarioErrorContext(
                    scenarioName = "Create Pet",
                    scenarioFile = "src/test/resources/petstore.scenario",
                    stepDescription = "POST /pets",
                    stepIndex = 2,
                    stepLine = 15,
                    operationId = "createPet",
                )

            assertEquals("Create Pet", context.scenarioName)
            assertEquals("src/test/resources/petstore.scenario", context.scenarioFile)
            assertEquals("POST /pets", context.stepDescription)
            assertEquals(2, context.stepIndex)
            assertEquals(15, context.stepLine)
            assertEquals("createPet", context.operationId)
        }

        @Test
        fun `should create context with minimal fields`() {
            val context = ScenarioErrorContext(scenarioName = "Simple Test")

            assertEquals("Simple Test", context.scenarioName)
            assertEquals(null, context.scenarioFile)
            assertEquals(null, context.stepDescription)
            assertEquals(null, context.operationId)
        }
    }

    @Nested
    inner class ErrorContextConfigTest {
        @Test
        fun `should have sensible defaults`() {
            val config = ErrorContextConfig()

            assertTrue(config.includeRequestBody)
            assertTrue(config.includeResponseBody)
            assertEquals(4096, config.maxBodySize)
            assertTrue(config.maskedHeaders.isNotEmpty())
            assertTrue(config.maskedHeaders.contains("authorization"))
            assertTrue(config.maskedHeaders.contains("cookie"))
        }

        @Test
        fun `should allow customization`() {
            val config =
                ErrorContextConfig(
                    includeRequestBody = false,
                    includeResponseBody = false,
                    maxBodySize = 1024,
                    maskedHeaders = setOf("x-custom-token"),
                )

            assertFalse(config.includeRequestBody)
            assertFalse(config.includeResponseBody)
            assertEquals(1024, config.maxBodySize)
            assertEquals(1, config.maskedHeaders.size)
        }
    }

    @Nested
    inner class HttpExecutionExceptionTest {
        private val sampleRequest =
            HttpRequest(
                method = "POST",
                url = "https://api.example.com/pets",
                headers =
                    mapOf(
                        "Content-Type" to listOf("application/json"),
                        "Authorization" to listOf("Bearer secret-token"),
                    ),
                body = """{"name": "Fluffy", "type": "cat"}""",
                timestamp = Instant.now(),
            )

        private val sampleResponse =
            HttpResponse(
                statusCode = 500,
                statusMessage = "Internal Server Error",
                headers = mapOf("Content-Type" to listOf("application/json")),
                body = """{"error": "Database connection failed"}""",
                duration = Duration.ofMillis(150),
                timestamp = Instant.now(),
            )

        @Test
        fun `should include basic error info`() {
            val exception =
                HttpExecutionException(
                    url = "https://api.example.com/pets",
                    method = "POST",
                    cause = RuntimeException("Connection timeout"),
                )

            val message = exception.message!!
            assertTrue(message.contains("POST"))
            assertTrue(message.contains("https://api.example.com/pets"))
            assertTrue(message.contains("Connection timeout"))
        }

        @Test
        fun `should include request and response details`() {
            val exception =
                HttpExecutionException(
                    url = sampleRequest.url,
                    method = sampleRequest.method,
                    cause = RuntimeException("Server error"),
                    request = sampleRequest,
                    response = sampleResponse,
                )

            val message = exception.message!!
            assertTrue(message.contains("--- Request ---"))
            assertTrue(message.contains("--- Response ---"))
            assertTrue(message.contains("500 Internal Server Error"))
        }

        @Test
        fun `should mask sensitive headers`() {
            val exception =
                HttpExecutionException(
                    url = sampleRequest.url,
                    method = sampleRequest.method,
                    cause = RuntimeException("Error"),
                    request = sampleRequest,
                )

            val message = exception.message!!
            assertTrue(message.contains("[MASKED]"))
            assertFalse(message.contains("Bearer secret-token"))
        }

        @Test
        fun `should include scenario context`() {
            val context =
                ScenarioErrorContext(
                    scenarioName = "Create Pet Scenario",
                    stepDescription = "Create a new pet",
                    stepLine = 10,
                    operationId = "createPet",
                )

            val exception =
                HttpExecutionException(
                    url = sampleRequest.url,
                    method = sampleRequest.method,
                    cause = RuntimeException("Error"),
                    scenarioContext = context,
                )

            val message = exception.message!!
            assertTrue(message.contains("--- Scenario Context ---"))
            assertTrue(message.contains("Create Pet Scenario"))
            assertTrue(message.contains("line 10"))
            assertTrue(message.contains("createPet"))
        }

        @Test
        fun `should truncate large response bodies`() {
            val largeBody = "x".repeat(5000)
            val response = sampleResponse.copy(body = largeBody)

            val exception =
                HttpExecutionException(
                    url = sampleRequest.url,
                    method = sampleRequest.method,
                    cause = RuntimeException("Error"),
                    response = response,
                    config = ErrorContextConfig(maxBodySize = 100),
                )

            val message = exception.message!!
            assertTrue(message.contains("truncated from 5000 to 100 bytes"))
            assertFalse(message.contains(largeBody))
        }
    }

    @Nested
    inner class AssertionExceptionTest {
        @Test
        fun `should show expected vs actual values`() {
            val exception =
                AssertionException(
                    expected = 200,
                    actual = 404,
                    assertionType = "status",
                )

            val message = exception.message!!
            assertTrue(message.contains("Expected: 200"))
            assertTrue(message.contains("Actual:   404"))
            assertTrue(message.contains("[status]"))
        }

        @Test
        fun `should include expression path`() {
            val exception =
                AssertionException(
                    expected = "Fluffy",
                    actual = "Buddy",
                    assertionType = "jsonpath",
                    expression = "$.name",
                )

            val message = exception.message!!
            assertTrue(message.contains("$.name"))
            assertTrue(message.contains("\"Fluffy\""))
            assertTrue(message.contains("\"Buddy\""))
        }

        @Test
        fun `should include scenario context`() {
            val context =
                ScenarioErrorContext(
                    scenarioName = "Pet Validation",
                    stepDescription = "Verify pet name",
                    stepLine = 25,
                )

            val exception =
                AssertionException(
                    expected = "Fluffy",
                    actual = "Buddy",
                    assertionType = "jsonpath",
                    scenarioContext = context,
                )

            val message = exception.message!!
            assertTrue(message.contains("Pet Validation"))
            assertTrue(message.contains("line 25"))
        }

        @Test
        fun `should compute string length diff`() {
            val exception =
                AssertionException(
                    expected = "short",
                    actual = "much longer string",
                    assertionType = "string",
                )

            val message = exception.message!!
            assertTrue(message.contains("length differs"))
        }
    }

    @Nested
    inner class SchemaValidationExceptionTest {
        @Test
        fun `should list all validation errors`() {
            val exception =
                SchemaValidationException(
                    errors =
                        listOf(
                            "required property 'name' is missing",
                            "property 'age' must be integer",
                        ),
                )

            val message = exception.message!!
            assertTrue(message.contains("required property 'name' is missing"))
            assertTrue(message.contains("property 'age' must be integer"))
        }

        @Test
        fun `should include schema path`() {
            val exception =
                SchemaValidationException(
                    errors = listOf("invalid type"),
                    schemaPath = "#/components/schemas/Pet",
                )

            val message = exception.message!!
            assertTrue(message.contains("#/components/schemas/Pet"))
        }

        @Test
        fun `should include scenario context and response preview`() {
            val response =
                HttpResponse(
                    statusCode = 200,
                    statusMessage = "OK",
                    headers = emptyMap(),
                    body = """{"invalid": "data"}""",
                    duration = Duration.ofMillis(50),
                    timestamp = Instant.now(),
                )

            val exception =
                SchemaValidationException(
                    errors = listOf("invalid type"),
                    scenarioContext =
                        ScenarioErrorContext(
                            scenarioName = "Schema Test",
                            operationId = "getPet",
                        ),
                    response = response,
                )

            val message = exception.message!!
            assertTrue(message.contains("Schema Test"))
            assertTrue(message.contains("getPet"))
            assertTrue(message.contains("""{"invalid": "data"}"""))
        }
    }

    @Nested
    inner class ScenarioParseExceptionTest {
        @Test
        fun `should show line and column`() {
            val exception =
                ScenarioParseException(
                    message = "Unexpected token",
                    line = 10,
                    column = 5,
                )

            val message = exception.message!!
            assertTrue(message.contains("line 10"))
            assertTrue(message.contains("column 5"))
            assertTrue(message.contains("Unexpected token"))
        }

        @Test
        fun `should include source file path`() {
            val exception =
                ScenarioParseException(
                    message = "Invalid syntax",
                    line = 3,
                    sourceFile = "test.scenario",
                )

            val message = exception.message!!
            assertTrue(message.contains("test.scenario"))
        }

        @Test
        fun `should create with source context window`() {
            val sourceLines =
                listOf(
                    "Scenario: Test",
                    "  Given a user",
                    "  When invlid step", // Typo on line 3
                    "  Then success",
                    "End Scenario",
                )

            val exception =
                ScenarioParseException.withSourceContext(
                    message = "Unknown step type 'invlid'",
                    line = 3,
                    column = 8,
                    sourceFile = "test.scenario",
                    allLines = sourceLines,
                )

            val message = exception.message!!
            assertTrue(message.contains("test.scenario"))
            assertTrue(message.contains("line 3"))
            // Should show context lines
            assertTrue(message.contains("Given a user"))
            assertTrue(message.contains("invlid step"))
            assertTrue(message.contains("Then success"))
            // Should have line marker
            assertTrue(message.contains(">"))
        }

        @Test
        fun `should handle edge case at beginning of file`() {
            val sourceLines =
                listOf(
                    "Invalid line",
                    "Second line",
                    "Third line",
                )

            val exception =
                ScenarioParseException.withSourceContext(
                    message = "Parse error",
                    line = 1,
                    allLines = sourceLines,
                )

            assertNotNull(exception.message)
            assertTrue(exception.sourceContent!!.isNotEmpty())
        }

        @Test
        fun `should handle edge case at end of file`() {
            val sourceLines =
                listOf(
                    "First line",
                    "Second line",
                    "Error line",
                )

            val exception =
                ScenarioParseException.withSourceContext(
                    message = "Parse error",
                    line = 3,
                    allLines = sourceLines,
                )

            assertNotNull(exception.message)
            assertTrue(exception.sourceContent!!.isNotEmpty())
        }
    }

    @Nested
    inner class OperationNotFoundExceptionTest {
        @Test
        fun `should suggest similar operations`() {
            val exception =
                OperationNotFoundException(
                    operationId = "getPet",
                    availableOperations = listOf("getPets", "createPet", "deletePet"),
                )

            val message = exception.message!!
            assertTrue(message.contains("getPet"))
            assertTrue(message.contains("Did you mean"))
            assertTrue(message.contains("getPets"))
        }

        @Test
        fun `should handle no suggestions`() {
            val exception =
                OperationNotFoundException(
                    operationId = "unknownOp",
                    availableOperations = listOf("createUser", "deleteUser"),
                )

            val message = exception.message!!
            assertTrue(message.contains("unknownOp"))
            assertTrue(message.contains("not found"))
            assertFalse(message.contains("Did you mean"))
        }
    }

    @Nested
    inner class ExtractionExceptionTest {
        @Test
        fun `should include variable and path info`() {
            val exception =
                ExtractionException(
                    variableName = "petId",
                    jsonPath = "$.id",
                    responseBody = """{"name": "Fluffy"}""",
                )

            val message = exception.message!!
            assertTrue(message.contains("petId"))
            assertTrue(message.contains("$.id"))
        }

        @Test
        fun `should truncate long response body`() {
            val longBody = "x".repeat(500)
            val exception =
                ExtractionException(
                    variableName = "data",
                    jsonPath = "$.field",
                    responseBody = longBody,
                )

            val message = exception.message!!
            assertFalse(message.contains(longBody))
            assertTrue(message.length < longBody.length)
        }

        @Test
        fun `should handle null response body`() {
            val exception =
                ExtractionException(
                    variableName = "data",
                    jsonPath = "$.field",
                    responseBody = null,
                )

            val message = exception.message!!
            assertTrue(message.contains("<empty>"))
        }
    }
}
