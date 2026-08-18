package org.berrycrush.config

import org.berrycrush.exception.ErrorContextConfig
import org.berrycrush.logging.HttpLogFormatter
import org.berrycrush.logging.HttpLogger
import java.time.Duration
import org.berrycrush.configuration.Configuration as ApiConfiguration

private const val DEFAULT_TIMEOUT_SECONDS = 30L
private const val DEFAULT_MAX_ERROR_BODY_SIZE = 4096
private const val MULTI_TEST_DEFAULT_SEQUENTIAL_COUNT = 3
private const val MULTI_TEST_DEFAULT_CONCURRENT_COUNT = 5

private const val ALIAS_MARKER = "alias."

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
 * @property multiTestConfig Configuration for multi-test execution (e.g., sequential/concurrent counts)
 * @property errorContextConfig Configuration for error context in exception messages
 */
data class BerryCrushConfiguration(
    override var timeout: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS),
    override val defaultHeaders: MutableMap<String, String> = mutableMapOf(),
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
    var multiTestConfig: MutableMap<String, Any> = mutableMapOf(),
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
    override var bindings: MutableMap<String, BindingConfig> = mutableMapOf(),
) : ApiConfiguration {
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

    fun binding(
        name: String,
        block: BindingConfig.Builder.() -> Unit,
    ) {
        val current = bindings[name]?.toBuilder() ?: BindingConfig.builder(name)
        bindings[name] = current.apply(block).build()
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
                bindings = this.bindings.mapValues { (_, binding) -> binding.copy() }.toMutableMap(),
            )

        for ((key, value) in parameters) {
            copy.applyParameter(key, value)
        }

        return copy
    }

    fun resolveReference(resolver: (Any) -> Any) {
        toParameterMap().forEach { (name, value) ->
            applyParameter(name, resolver(value))
        }
    }

    fun toParameterMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        baseUrl?.let { map["baseUrl"] = it }
        map["timeout"] = timeout
        environment?.let { map["environment"] = it }
        map["strictSchemaValidation"] = strictSchemaValidation
        map["followRedirects"] = followRedirects
        map["logRequests"] = logRequests
        map["logResponses"] = logResponses
        map["shareVariablesAcrossScenarios"] = shareVariablesAcrossScenarios
        map["multiTestSequentialCount"] = multiTestConfig["sequential.count"] ?: MULTI_TEST_DEFAULT_SEQUENTIAL_COUNT
        map["multiTestConcurrentCount"] = multiTestConfig["concurrent.count"] ?: MULTI_TEST_DEFAULT_CONCURRENT_COUNT
        map["errorContext.includeRequestBody"] = errorContextConfig.includeRequestBody
        map["errorContext.includeResponseBody"] = errorContextConfig.includeResponseBody
        map["errorContext.maxBodySize"] = errorContextConfig.maxBodySize
        map["retry.maxAttempts"] = retryConfig.maxAttempts
        map["retry.delay"] = retryConfig.delay
        map["retry.maxDelay"] = retryConfig.maxDelay
        map["retry.backoff"] = retryConfig.backoff
        map["retry.jitter"] = retryConfig.jitter

        defaultHeaders.forEach { (name, value) ->
            map["header.$name"] = value
        }
        bindings.forEach { (name, binding) ->
            binding.baseUrl?.let { map["binding.$name.baseUrl"] = it }
            binding.location?.let { map["binding.$name.location"] = it }
            binding.operationAliases.forEach { (alias, operationId) ->
                map["binding.$name.alias.$alias"] = operationId
            }
        }
        return map
    }

    private fun applyParameter(
        key: String,
        value: Any,
    ) {
        when (key) {
            "baseUrl" -> {
                applyBindingParam("baseUrl", value)
            }

            "timeout" -> {
                timeout = parseTimeout(value, timeout)
            }

            "environment" -> {
                environment = value.toString()
            }

            "strictSchemaValidation" -> {
                strictSchemaValidation = value.toString().toBoolean()
            }

            "followRedirects" -> {
                followRedirects = value.toString().toBoolean()
            }

            "logRequests" -> {
                logRequests = value.toString().toBoolean()
            }

            "logResponses" -> {
                logResponses = value.toString().toBoolean()
            }

            "shareVariablesAcrossScenarios" -> {
                shareVariablesAcrossScenarios = value.toString().toBoolean()
            }

            "multiTestSequentialCount" -> {
                multiTestConfig["sequential.count"] =
                    parseIntOrDefault(value, MULTI_TEST_DEFAULT_SEQUENTIAL_COUNT)
            }

            "multiTestConcurrentCount" -> {
                multiTestConfig["concurrent.count"] =
                    parseIntOrDefault(value, MULTI_TEST_DEFAULT_CONCURRENT_COUNT)
            }

            else -> {
                applyPrefixedParameter(key, value)
            }
        }
    }
}

/**
 * Configuration for automatic assertion generation from OpenAPI spec.
 *
 * @property enabled Whether auto-assertions are enabled (default false)
 * @property statusCode Auto-assert correct status code
 * @property contentType Auto-assert Content-Type header
 * @property schema Auto-assert response matches schema
 */
data class AutoAssertionConfig(
    var enabled: Boolean = false,
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

private fun BerryCrushConfiguration.applyPrefixedParameter(
    key: String,
    value: Any,
) {
    when {
        key.startsWith("header.") -> defaultHeaders[key.removePrefix("header.")] = value.toString()
        key.startsWith("autoAssertions.") -> applyAutoAssertionParam(key, value)
        key.startsWith("errorContext.") -> applyErrorContextParam(key, value)
        key.startsWith("retry.") -> applyRetryParam(key, value)
        key.startsWith("binding.") -> applyBindingParam(key.removePrefix("binding."), value)
        key.startsWith("multiTest.") -> multiTestConfig[key.removePrefix("multiTest.")] = value
    }
}

private fun BerryCrushConfiguration.applyAutoAssertionParam(
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

private fun BerryCrushConfiguration.applyErrorContextParam(
    key: String,
    value: Any,
) {
    when (key) {
        "errorContext.includeRequestBody" -> {
            errorContextConfig = errorContextConfig.copy(includeRequestBody = value.toString().toBoolean())
        }

        "errorContext.includeResponseBody" -> {
            errorContextConfig = errorContextConfig.copy(includeResponseBody = value.toString().toBoolean())
        }

        "errorContext.maxBodySize" -> {
            errorContextConfig = errorContextConfig.copy(maxBodySize = parseIntOrDefault(value, errorContextConfig.maxBodySize))
        }
    }
}

private fun BerryCrushConfiguration.applyRetryParam(
    key: String,
    value: Any,
) {
    when (key) {
        "retry.maxAttempts" -> {
            retryConfig = retryConfig.copy(maxAttempts = parseIntOrDefault(value, retryConfig.maxAttempts))
        }

        "retry.delay" -> {
            retryConfig = retryConfig.copy(delay = parseDuration(value, retryConfig.delay))
        }

        "retry.maxDelay" -> {
            retryConfig = retryConfig.copy(maxDelay = parseDuration(value, retryConfig.maxDelay))
        }

        "retry.backoff" -> {
            retryConfig = retryConfig.copy(backoff = parseBackoffStrategy(value))
        }

        "retry.jitter" -> {
            retryConfig = retryConfig.copy(jitter = value.toString().toBoolean())
        }
    }
}

private fun BerryCrushConfiguration.applyBindingParam(
    key: String,
    value: Any,
) {
    val (name, param) = parseBindingParam(key)
    bindings.compute(name) { _, binding ->
        when {
            param == "baseUrl" -> binding?.copy(baseUrl = value.toString()) ?: BindingConfig(name, baseUrl = value.toString())
            param == "location" -> binding?.copy(location = value.toString()) ?: BindingConfig(name, location = value.toString())
            param.startsWith(ALIAS_MARKER) -> applyBindingAlias(binding, name, param, value)
            else -> binding
        }
    }
}

private fun applyBindingAlias(
    binding: BindingConfig?,
    name: String,
    param: String,
    value: Any,
): BindingConfig? {
    if (!param.startsWith(ALIAS_MARKER)) {
        return binding
    }

    val alias = param.removePrefix(ALIAS_MARKER).trim()
    if (alias.isBlank()) {
        return binding
    }

    val current = binding ?: BindingConfig(name)
    return current.copy(operationAliases = current.operationAliases + (alias to value.toString()))
}

private fun BerryCrushConfiguration.parseBackoffStrategy(value: Any): BackoffStrategy =
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

private fun parseTimeout(
    value: Any,
    defaultTimeout: Duration,
): Duration =
    when (value) {
        is Number -> Duration.ofSeconds(value.toLong())
        is String -> Duration.ofSeconds(value.toLong())
        else -> defaultTimeout
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

private fun parseBindingParam(key: String): Pair<String, String> {
    if (key.startsWith(ALIAS_MARKER)) {
        return BindingConfig.DEFAULT_BINDING_NAME to key
    }

    val firstDot = key.indexOf('.')
    if (firstDot == -1) {
        return BindingConfig.DEFAULT_BINDING_NAME to key
    }

    val bindingName = key.substring(0, firstDot)
    val param = key.substring(firstDot + 1)
    return bindingName to param
}
