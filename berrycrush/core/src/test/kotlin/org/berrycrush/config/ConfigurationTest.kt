package org.berrycrush.config

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigurationTest {
    @Test
    fun `should apply baseUrl parameter`() {
        val config = BerryCrushConfiguration()
        val modified = config.withParameters(mapOf("baseUrl" to "http://localhost:8080"))

        assertEquals("http://localhost:8080", modified.baseUrl)
    }

    @Test
    fun `should apply timeout parameter as number`() {
        val config = BerryCrushConfiguration()
        val modified = config.withParameters(mapOf("timeout" to 60))

        assertEquals(Duration.ofSeconds(60), modified.timeout)
    }

    @Test
    fun `should apply timeout parameter as string`() {
        val config = BerryCrushConfiguration()
        val modified = config.withParameters(mapOf("timeout" to "120"))

        assertEquals(Duration.ofSeconds(120), modified.timeout)
    }

    @Test
    fun `should apply environment parameter`() {
        val config = BerryCrushConfiguration()
        val modified = config.withParameters(mapOf("environment" to "staging"))

        assertEquals("staging", modified.environment)
    }

    @Test
    fun `should apply boolean parameters`() {
        val config = BerryCrushConfiguration()
        val modified =
            config.withParameters(
                mapOf(
                    "strictSchemaValidation" to true,
                    "followRedirects" to false,
                    "logRequests" to true,
                    "logResponses" to true,
                    "shareVariablesAcrossScenarios" to true,
                ),
            )

        assertTrue(modified.strictSchemaValidation)
        assertFalse(modified.followRedirects)
        assertTrue(modified.logRequests)
        assertTrue(modified.logResponses)
        assertTrue(modified.shareVariablesAcrossScenarios)
    }

    @Test
    fun `should apply boolean parameters from strings`() {
        val config = BerryCrushConfiguration()
        val modified =
            config.withParameters(
                mapOf(
                    "strictSchemaValidation" to "true",
                    "followRedirects" to "false",
                ),
            )

        assertTrue(modified.strictSchemaValidation)
        assertFalse(modified.followRedirects)
    }

    @Test
    fun `should apply header parameters`() {
        val config = BerryCrushConfiguration()
        val modified =
            config.withParameters(
                mapOf(
                    "header.Authorization" to "Bearer token123",
                    "header.X-Custom" to "custom-value",
                ),
            )

        assertEquals("Bearer token123", modified.defaultHeaders["Authorization"])
        assertEquals("custom-value", modified.defaultHeaders["X-Custom"])
    }

    @Test
    fun `should apply auto assertion parameters`() {
        val config = BerryCrushConfiguration()
        val modified =
            config.withParameters(
                mapOf(
                    "autoAssertions.enabled" to false,
                    "autoAssertions.statusCode" to false,
                    "autoAssertions.contentType" to false,
                    "autoAssertions.schema" to false,
                ),
            )

        assertFalse(modified.autoAssertions.enabled)
        assertFalse(modified.autoAssertions.statusCode)
        assertFalse(modified.autoAssertions.contentType)
        assertFalse(modified.autoAssertions.schema)
    }

    @Test
    fun `should not modify original configuration`() {
        val config =
            BerryCrushConfiguration(
                timeout = Duration.ofSeconds(10),
            )
        config.baseUrl = "http://original.com"
        config.defaultHeaders["Original"] = "value"

        val modified =
            config.withParameters(
                mapOf(
                    "baseUrl" to "http://modified.com",
                    "timeout" to 60,
                    "header.New" to "new-value",
                ),
            )

        // Original should be unchanged
        assertEquals("http://original.com", config.baseUrl)
        assertEquals(Duration.ofSeconds(10), config.timeout)
        assertEquals(1, config.defaultHeaders.size)
        assertEquals("value", config.defaultHeaders["Original"])

        // Modified should have new values
        assertEquals("http://modified.com", modified.baseUrl)
        assertEquals(Duration.ofSeconds(60), modified.timeout)
        assertEquals(2, modified.defaultHeaders.size)
        assertEquals("value", modified.defaultHeaders["Original"])
        assertEquals("new-value", modified.defaultHeaders["New"])
    }

    @Test
    fun `should preserve existing headers when adding new ones`() {
        val config = BerryCrushConfiguration()
        config.defaultHeaders["Existing"] = "existing-value"

        val modified =
            config.withParameters(
                mapOf("header.New" to "new-value"),
            )

        assertEquals("existing-value", modified.defaultHeaders["Existing"])
        assertEquals("new-value", modified.defaultHeaders["New"])
    }

    @Test
    fun `should ignore unknown parameters`() {
        val config =
            BerryCrushConfiguration().apply {
                baseUrl = "http://original.com"
            }
        val modified =
            config.withParameters(
                mapOf(
                    "unknownParam" to "value",
                    "baseUrl" to "http://modified.com",
                ),
            )

        assertEquals("http://modified.com", modified.baseUrl)
    }

    @Test
    fun `should handle empty parameters map`() {
        val config =
            BerryCrushConfiguration().apply {
                baseUrl = "http://original.com"
            }
        val modified = config.withParameters(emptyMap())

        assertEquals("http://original.com", modified.baseUrl)
    }

    @Test
    fun `should apply error context parameters`() {
        val config = BerryCrushConfiguration()
        val modified =
            config.withParameters(
                mapOf(
                    "errorContext.includeRequestBody" to false,
                    "errorContext.includeResponseBody" to false,
                    "errorContext.maxBodySize" to 1024,
                ),
            )

        assertFalse(modified.errorContextConfig.includeRequestBody)
        assertFalse(modified.errorContextConfig.includeResponseBody)
        assertEquals(1024, modified.errorContextConfig.maxBodySize)
    }

    @Test
    fun `should apply error context maxBodySize from string`() {
        val config = BerryCrushConfiguration()
        val modified =
            config.withParameters(
                mapOf("errorContext.maxBodySize" to "2048"),
            )

        assertEquals(2048, modified.errorContextConfig.maxBodySize)
    }

    @Test
    fun `should have sensible error context defaults`() {
        val config = BerryCrushConfiguration()

        assertTrue(config.errorContextConfig.includeRequestBody)
        assertTrue(config.errorContextConfig.includeResponseBody)
        assertEquals(4096, config.errorContextConfig.maxBodySize)
        assertTrue(config.errorContextConfig.maskedHeaders.contains("authorization"))
    }

    @Test
    fun `should not modify original error context config`() {
        val config = BerryCrushConfiguration()
        val original = config.errorContextConfig

        val modified =
            config.withParameters(
                mapOf("errorContext.maxBodySize" to 512),
            )

        // Original should be unchanged
        assertEquals(4096, config.errorContextConfig.maxBodySize)
        assertEquals(original, config.errorContextConfig)

        // Modified should have new value
        assertEquals(512, modified.errorContextConfig.maxBodySize)
    }
}
