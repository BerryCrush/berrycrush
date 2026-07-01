package org.berrycrush.config

import org.berrycrush.autotest.MultiTestParameters
import org.berrycrush.exception.ErrorContextConfig
import org.berrycrush.logging.HttpLogFormatter
import org.berrycrush.logging.HttpLogger
import java.time.Duration

private const val DEFAULT_TIMEOUT_SECONDS = 30L
private const val DEFAULT_MAX_ERROR_BODY_SIZE = 4096

/**
 * Configuration for BerryCrush test execution.
 *
 * @property baseUrl Base URL for API requests (overrides spec server URL)
 * @property timeout HTTP request timeout
 * @property defaultHeaders Headers to include in all requests
 * @property environment Environment name (e.g., "staging", "production")
 * @property autoAssertions Configuration for auto-generated assertions
 * @property strictSchemaValidation Whether to fail on schema validation warnings
 * @property followRedirects Whether to follow HTTP redirects
 * @property logRequests Whether to log HTTP requests
 * @property logResponses Whether to log HTTP responses
 * @property httpLogger Custom HTTP logger (default: JUL-based logger)
 * @property logFormatter Custom log formatter (default: multi-line human-readable format)
 * @property multiTestSequentialCount Number of sequential requests for multi-tests
 * @property multiTestConcurrentCount Number of concurrent requests for multi-tests
 * @property errorContextConfig Configuration for error context in exception messages
 */
data class BerryCrushConfiguration(
    var timeout: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS),
    val defaultHeaders: MutableMap<String, String> = mutableMapOf(),
    var environment: String? = null,
    var autoAssertions: AutoAssertionConfig = AutoAssertionConfig(),
    var strictSchemaValidation: Boolean = false,
    var followRedirects: Boolean = true,
    var logRequests: Boolean = false,
    var logResponses: Boolean = false,
    /**
     * Custom HTTP logger for request/response logging.
     * Set to null to use the default logger from HttpLoggerFactory.
     */
    var httpLogger: HttpLogger? = null,
    /**
     * Custom log formatter for formatting log messages.
     * Only used if httpLogger is null (using the default logger).
     */
    var logFormatter: HttpLogFormatter? = null,
    /**
     * Whether to share variables across scenarios.
     *
     * When enabled, variables extracted in one scenario are available in subsequent
     * scenarios. This allows for chained scenarios like:
     * - Scenario 1: Create a resource, extract its ID
     * - Scenario 2: Use the extracted ID to fetch or update the resource
     *
     * Default is false (each scenario has isolated variable scope).
     */
    var shareVariablesAcrossScenarios: Boolean = false,
    /**
     * Number of sequential requests for multi-request idempotency tests.
     * Default: 3
     */
    var multiTestSequentialCount: Int = MultiTestParameters.DEFAULTS.getValue(MultiTestParameters.SEQUENTIAL_COUNT),
    /**
     * Number of concurrent requests for multi-request idempotency tests.
     * Default: 5
     */
    var multiTestConcurrentCount: Int = MultiTestParameters.DEFAULTS.getValue(MultiTestParameters.CONCURRENT_COUNT),
    /**
     * Configuration for error context in exception messages.
     *
     * Controls what information is included in error messages, such as:
     * - Request/response body inclusion
     * - Maximum body size (truncation)
     * - Header masking for sensitive values
     */
    var errorContextConfig: ErrorContextConfig = ErrorContextConfig(),
    /**
     * Configuration for HTTP request retry behavior.
     *
     * Controls automatic retries for failed HTTP requests, including:
     * - Number of retry attempts
     * - Delay between attempts (with backoff strategies)
     * - Which status codes and exceptions trigger retries
     *
     * Default is disabled (maxAttempts = 0).
     *
     * @see RetryConfig
     */
    var retryConfig: RetryConfig = RetryConfig.DISABLED,
    var bindings: MutableMap<String, BindingConfig> = mutableMapOf(),
) {
    var baseUrl: String?
        get() = bindings[BindingConfig.DEFAULT_BINDING_NAME]?.baseUrl
        set(value) {
            bindings.compute(BindingConfig.DEFAULT_BINDING_NAME) { _, binding ->
                binding?.copy(baseUrl = value) ?: BindingConfig(BindingConfig.DEFAULT_BINDING_NAME, value)
            }
        }

    /**
     * DSL helper to set timeout in seconds.
     */
    fun timeout(seconds: Long) {
        timeout = Duration.ofSeconds(seconds)
    }

    /**
     * DSL helper to add default header.
     */
    fun header(
        name: String,
        value: String,
    ) {
        defaultHeaders[name] = value
    }

    /**
     * DSL helper to configure error context settings.
     */
    fun errorContext(block: ErrorContextConfig.() -> Unit) {
        errorContextConfig = errorContextConfig.copy().apply(block)
    }

    /**
     * DSL helper to configure retry settings.
     *
     * Example:
     * ```kotlin
     * retry {
     *     maxAttempts = 3
     *     delay = Duration.ofSeconds(1)
     *     backoff = BackoffStrategy.EXPONENTIAL
     * }
     * ```
     */
    fun retry(block: RetryConfigBuilder.() -> Unit) {
        retryConfig = RetryConfigBuilder(retryConfig).apply(block).build()
    }

    /**
     * Create a copy of this configuration with parameters applied.
     *
     * Supports the following parameter names:
     * - `baseUrl` - Override the base URL
     * - `timeout` - Request timeout in seconds (number)
     * - `environment` - Environment name
     * - `strictSchemaValidation` - true/false
     * - `followRedirects` - true/false
     * - `logRequests` - true/false
     * - `logResponses` - true/false
     * - `shareVariablesAcrossScenarios` - true/false
     * - `header.<name>` - Add/override a default header
     * - `multiTestSequentialCount` - Number of sequential requests for multi-tests
     * - `multiTestConcurrentCount` - Number of concurrent requests for multi-tests
     * - `errorContext.includeRequestBody` - Include request body in errors (true/false)
     * - `errorContext.includeResponseBody` - Include response body in errors (true/false)
     * - `errorContext.maxBodySize` - Max body size in error messages (number)
     * - `retry.maxAttempts` - Number of retry attempts (0 = disabled)
     * - `retry.delay` - Delay between retries (e.g., "1s", "500ms")
     * - `retry.maxDelay` - Maximum delay cap (e.g., "30s")
     * - `retry.backoff` - Backoff strategy (fixed, linear, exponential)
     * - `retry.jitter` - Add randomness to delays (true/false)
     *
     * @param parameters Map of parameter names to values
     * @return A new Configuration with parameters applied
     */
    fun withParameters(parameters: Map<String, Any>): BerryCrushConfiguration {
        val copy =
            this.copy(
                defaultHeaders = this.defaultHeaders.toMutableMap(),
                autoAssertions = this.autoAssertions.copy(),
                errorContextConfig = this.errorContextConfig.copy(),
                retryConfig = this.retryConfig.copy(),
                bindings = this.bindings.toMutableMap(),
            )

        for ((key, value) in parameters) {
            copy.applyParameter(key, value)
        }

        return copy
    }

    private fun applyParameter(
        key: String,
        value: Any,
    ) {
        when {
            key == "baseUrl" -> applyBindingParam("baseUrl", value)
            key == "timeout" -> timeout = parseTimeout(value)
            key == "environment" -> environment = value.toString()
            key == "strictSchemaValidation" -> strictSchemaValidation = value.toString().toBoolean()
            key == "followRedirects" -> followRedirects = value.toString().toBoolean()
            key == "logRequests" -> logRequests = value.toString().toBoolean()
            key == "logResponses" -> logResponses = value.toString().toBoolean()
            key == "shareVariablesAcrossScenarios" -> shareVariablesAcrossScenarios = value.toString().toBoolean()
            key == "multiTestSequentialCount" -> multiTestSequentialCount = parseIntOrDefault(value, multiTestSequentialCount)
            key == "multiTestConcurrentCount" -> multiTestConcurrentCount = parseIntOrDefault(value, multiTestConcurrentCount)
            else -> applyPrefixedParameter(key, value)
        }
    }

    private fun applyPrefixedParameter(
        key: String,
        value: Any,
    ) {
        when {
            key.startsWith("header.") -> defaultHeaders[key.removePrefix("header.")] = value.toString()
            key.startsWith("autoAssertions.") -> applyAutoAssertionParam(key, value)
            key.startsWith("errorContext.") -> applyErrorContextParam(key, value)
            key.startsWith("retry.") -> applyRetryParam(key, value)
            key.startsWith("binding.") -> applyBindingParam(key.removePrefix("binding."), value)
        }
    }

    private fun applyAutoAssertionParam(
        key: String,
        value: Any,
    ) {
        when (key) {
            "autoAssertions.enabled" -> autoAssertions.enabled = value.toString().toBoolean()
            "autoAssertions.statusCode" -> autoAssertions.statusCode = value.toString().toBoolean()
            "autoAssertions.contentType" -> autoAssertions.contentType = value.toString().toBoolean()
            "autoAssertions.schema" -> autoAssertions.schema = value.toString().toBoolean()
        }
    }

    private fun applyErrorContextParam(
        key: String,
        value: Any,
    ) {
        when (key) {
            "errorContext.includeRequestBody" ->
                errorContextConfig = errorContextConfig.copy(includeRequestBody = value.toString().toBoolean())
            "errorContext.includeResponseBody" ->
                errorContextConfig = errorContextConfig.copy(includeResponseBody = value.toString().toBoolean())
            "errorContext.maxBodySize" ->
                errorContextConfig = errorContextConfig.copy(maxBodySize = parseIntOrDefault(value, errorContextConfig.maxBodySize))
        }
    }

    private fun applyRetryParam(
        key: String,
        value: Any,
    ) {
        when (key) {
            "retry.maxAttempts" ->
                retryConfig = retryConfig.copy(maxAttempts = parseIntOrDefault(value, retryConfig.maxAttempts))
            "retry.delay" ->
                retryConfig = retryConfig.copy(delay = parseDuration(value, retryConfig.delay))
            "retry.maxDelay" ->
                retryConfig = retryConfig.copy(maxDelay = parseDuration(value, retryConfig.maxDelay))
            "retry.backoff" ->
                retryConfig = retryConfig.copy(backoff = parseBackoffStrategy(value))
            "retry.jitter" ->
                retryConfig = retryConfig.copy(jitter = value.toString().toBoolean())
        }
    }

    private fun applyBindingParam(
        key: String,
        value: Any,
    ) {
        val (name, param) =
            key.indexOf('.').let {
                if (it == -1) {
                    BindingConfig.DEFAULT_BINDING_NAME to key
                } else {
                    key.substring(0, it) to key.substring(it + 1)
                }
            }
        bindings.compute(name) { _, binding ->
            when (param) {
                "baseUrl" -> binding?.copy(baseUrl = value.toString()) ?: BindingConfig(name, baseUrl = value.toString())
                "location" -> binding?.copy(location = value.toString()) ?: BindingConfig(name, location = value.toString())
                else -> binding
            }
        }
    }

    private fun parseBackoffStrategy(value: Any): BackoffStrategy =
        when (value.toString().lowercase()) {
            "fixed" -> BackoffStrategy.FIXED
            "linear" -> BackoffStrategy.LINEAR
            "exponential" -> BackoffStrategy.EXPONENTIAL
            else -> retryConfig.backoff
        }

    private fun parseDuration(
        value: Any,
        default: Duration,
    ): Duration {
        val str = value.toString().trim().lowercase()
        return try {
            when {
                str.endsWith("ms") -> Duration.ofMillis(str.removeSuffix("ms").trim().toLong())
                str.endsWith("s") -> Duration.ofSeconds(str.removeSuffix("s").trim().toLong())
                str.endsWith("m") -> Duration.ofMinutes(str.removeSuffix("m").trim().toLong())
                else -> Duration.ofMillis(str.toLong())
            }
        } catch (_: NumberFormatException) {
            default
        }
    }

    private fun parseTimeout(value: Any): Duration =
        when (value) {
            is Number -> Duration.ofSeconds(value.toLong())
            is String -> Duration.ofSeconds(value.toLong())
            else -> timeout
        }

    private fun parseIntOrDefault(
        value: Any,
        default: Int,
    ): Int =
        when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
}

/**
 * Configuration for automatic assertion generation from OpenAPI spec.
 *
 * @property enabled Whether auto-assertions are enabled globally
 * @property statusCode Auto-assert correct status code
 * @property contentType Auto-assert Content-Type header
 * @property schema Auto-assert response matches schema
 */
data class AutoAssertionConfig(
    var enabled: Boolean = true,
    var statusCode: Boolean = true,
    var contentType: Boolean = true,
    var schema: Boolean = true,
)

/**
 * Configuration for a single OpenAPI specification.
 *
 * @property name Unique identifier for this spec
 * @property path Path to the OpenAPI spec file
 * @property baseUrl Base URL override for this spec
 * @property defaultHeaders Headers specific to this spec
 */
data class SpecConfiguration(
    val name: String,
    val path: String,
    var baseUrl: String? = null,
    val defaultHeaders: MutableMap<String, String> = mutableMapOf(),
) {
    /**
     * DSL helper to add default header for this spec.
     */
    fun header(
        name: String,
        value: String,
    ) {
        defaultHeaders[name] = value
    }
}
