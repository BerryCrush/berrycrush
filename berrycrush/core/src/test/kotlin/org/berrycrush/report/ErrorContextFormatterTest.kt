package org.berrycrush.report

import org.berrycrush.exception.AssertionException
import org.berrycrush.exception.ErrorContextConfig
import org.berrycrush.exception.HttpExecutionException
import org.berrycrush.exception.ScenarioErrorContext
import org.berrycrush.exception.SchemaValidationException
import org.berrycrush.plugin.HttpRequest
import org.berrycrush.plugin.HttpResponse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorContextFormatterTest {
    private val formatter = ErrorContextFormatter.plain()

    @Nested
    inner class ScenarioContextFormatting {
        @Test
        fun `formats scenario context with all fields`() {
            val context =
                ScenarioErrorContext(
                    scenarioName = "Test Scenario",
                    scenarioFile = "test/scenario.feature",
                    stepDescription = "when I call the API",
                    stepIndex = 2,
                    stepLine = 15,
                    operationId = "getPetById",
                )

            val result = formatter.formatScenarioContext(context)

            assertContains(result, "Scenario: Test Scenario")
            assertContains(result, "File: test/scenario.feature")
            assertContains(result, "Step: when I call the API")
            assertContains(result, "line 15")
            assertContains(result, "Step Index: 2")
            assertContains(result, "Operation: getPetById")
        }

        @Test
        fun `formats scenario context with minimal fields`() {
            val context = ScenarioErrorContext(scenarioName = "Minimal Scenario")

            val result = formatter.formatScenarioContext(context)

            assertContains(result, "Scenario: Minimal Scenario")
            assertFalse(result.contains("File:"))
            assertFalse(result.contains("Step:"))
        }
    }

    @Nested
    inner class HttpExceptionFormatting {
        @Test
        fun `formats HTTP exception with full context`() {
            val request = createTestRequest()
            val response = createTestResponse()
            val scenarioContext = createTestScenarioContext()

            val exception =
                HttpExecutionException(
                    url = "https://api.example.com/pets/1",
                    method = "GET",
                    cause = RuntimeException("Connection timeout"),
                    request = request,
                    response = response,
                    scenarioContext = scenarioContext,
                )

            val result = formatter.formatHttpException(exception)

            assertContains(result, "HTTP Execution Error")
            assertContains(result, "GET https://api.example.com/pets/1")
            assertContains(result, "Connection timeout")
            assertContains(result, "Scenario: Test Scenario")
            assertContains(result, "Request")
            assertContains(result, "Response")
        }

        @Test
        fun `masks sensitive headers in request`() {
            val request =
                HttpRequest(
                    method = "POST",
                    url = "https://api.example.com/login",
                    headers = mapOf("Authorization" to listOf("Bearer secret-token")),
                    body = """{"user":"test"}""",
                    timestamp = Instant.now(),
                )

            val result = formatter.formatRequest(request)

            assertContains(result, "[MASKED]")
            assertFalse(result.contains("secret-token"))
        }

        @Test
        fun `truncates large body content`() {
            val largeBody = "x".repeat(5000)
            val response =
                HttpResponse(
                    statusCode = 200,
                    statusMessage = "OK",
                    headers = emptyMap(),
                    body = largeBody,
                    duration = Duration.ofMillis(100),
                    timestamp = Instant.now(),
                )

            val config = ErrorContextConfig(maxBodySize = 1024)
            val result = formatter.formatResponse(response, config)

            assertContains(result, "truncated from 5000 to 1024 bytes")
            assertTrue(result.length < largeBody.length)
        }
    }

    @Nested
    inner class AssertionExceptionFormatting {
        @Test
        fun `formats assertion with expected and actual`() {
            val exception =
                AssertionException(
                    expected = "Max",
                    actual = "Buddy",
                    assertionType = "jsonpath",
                    expression = "$.name",
                )

            val result = formatter.formatAssertionException(exception)

            assertContains(result, "Assertion Failed")
            assertContains(result, "Expression: $.name")
            assertContains(result, "Type: jsonpath")
            assertContains(result, "Expected:")
            assertContains(result, "\"Max\"")
            assertContains(result, "Actual:")
            assertContains(result, "\"Buddy\"")
        }

        @Test
        fun `shows diff for string values`() {
            val exception =
                AssertionException(
                    expected = "line1\nline2",
                    actual = "line1\nline3",
                    assertionType = "body",
                )

            val result = formatter.formatAssertionException(exception)

            assertContains(result, "Diff:")
            assertContains(result, "line2")
            assertContains(result, "line3")
        }

        @Test
        fun `formats null values`() {
            val exception =
                AssertionException(
                    expected = "value",
                    actual = null,
                    assertionType = "exists",
                )

            val result = formatter.formatAssertionException(exception)

            assertContains(result, "null")
        }
    }

    @Nested
    inner class SchemaExceptionFormatting {
        @Test
        fun `formats schema validation errors`() {
            val exception =
                SchemaValidationException(
                    errors =
                        listOf(
                            "$.id: expected integer but got string",
                            "$.name: is required but missing",
                        ),
                    schemaPath = "#/components/schemas/Pet",
                )

            val result = formatter.formatSchemaException(exception)

            assertContains(result, "Schema Validation Failed")
            assertContains(result, "#/components/schemas/Pet")
            assertContains(result, "expected integer but got string")
            assertContains(result, "is required but missing")
        }

        @Test
        fun `includes scenario context when available`() {
            val exception =
                SchemaValidationException(
                    errors = listOf("validation error"),
                    scenarioContext = createTestScenarioContext(),
                )

            val result = formatter.formatSchemaException(exception)

            assertContains(result, "Scenario: Test Scenario")
        }
    }

    @Nested
    inner class ColoredOutput {
        private val coloredFormatter = ErrorContextFormatter.colored()

        @Test
        fun `applies colors to error headers`() {
            val exception =
                AssertionException(
                    expected = "value",
                    actual = "other",
                    assertionType = "equals",
                )

            val result = coloredFormatter.formatAssertionException(exception)

            // Should contain ANSI escape codes
            assertContains(result, "\u001B[")
        }
    }

    @Nested
    inner class GenericExceptionFormatting {
        @Test
        fun `formats generic exception`() {
            val exception = RuntimeException("Something went wrong")

            val result = formatter.format(exception)

            assertContains(result, "Error: RuntimeException")
            assertContains(result, "Something went wrong")
        }

        @Test
        fun `dispatches to correct formatter for BerryCrush exceptions`() {
            val httpException =
                HttpExecutionException(
                    url = "https://api.example.com",
                    method = "GET",
                    cause = RuntimeException("error"),
                )

            val result = formatter.format(httpException)

            assertContains(result, "HTTP Execution Error")
        }
    }

    // --- Helper methods ---

    private fun createTestRequest(): HttpRequest =
        HttpRequest(
            method = "GET",
            url = "https://api.example.com/pets/1",
            headers = mapOf("Accept" to listOf("application/json")),
            body = null,
            timestamp = Instant.now(),
        )

    private fun createTestResponse(): HttpResponse =
        HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            headers = mapOf("Content-Type" to listOf("application/json")),
            body = """{"id":1,"name":"Max"}""",
            duration = Duration.ofMillis(150),
            timestamp = Instant.now(),
        )

    private fun createTestScenarioContext(): ScenarioErrorContext =
        ScenarioErrorContext(
            scenarioName = "Test Scenario",
            scenarioFile = "test/scenario.feature",
            stepDescription = "when I call GET /pets/1",
            stepIndex = 1,
            stepLine = 10,
            operationId = "getPetById",
        )
}
