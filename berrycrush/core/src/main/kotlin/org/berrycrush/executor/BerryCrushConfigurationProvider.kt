package org.berrycrush.executor

import org.berrycrush.config.AutoAssertionConfig
import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.config.BindingConfig
import org.berrycrush.config.RetryConfig
import org.berrycrush.exception.ErrorContextConfig
import org.berrycrush.logging.HttpLogFormatter
import org.berrycrush.logging.HttpLogger
import org.berrycrush.logging.HttpLoggerFactory
import java.time.Duration
import org.berrycrush.configuration.Configuration as ApiConfiguration

/**
 * Configuration provider
 */
interface BerryCrushConfigurationProvider : ApiConfiguration {
    val baseUrl: String?
    val environment: String?
    val autoAssertions: AutoAssertionConfig
    val strictSchemaValidation: Boolean
    val followRedirects: Boolean
    val logRequests: Boolean
    val logResponses: Boolean
    val httpLogger: HttpLogger?
    val logFormatter: HttpLogFormatter?
    val shareVariablesAcrossScenarios: Boolean
    val multiTestConfig: Map<String, Any>
    val errorContextConfig: ErrorContextConfig
    val retryConfig: RetryConfig

    override val bindings: Map<String, BindingConfig>

    /**
     * Get the effective HTTP logger.
     * Returns the custom logger if set, otherwise creates one from the factory.
     */
    fun getEffectiveHttpLogger(): HttpLogger = httpLogger ?: HttpLoggerFactory.create()

    /**
     * Temporarily overwrite the configuration
     */
    fun <R> withParameters(
        parameters: Map<String, Any>,
        block: () -> R,
    ): R

    fun toParameterMap(): Map<String, Any>

    fun resolveReference(resolver: (Any) -> Any)

    companion object {
        fun from(
            configuration: BerryCrushConfiguration,
            runtimeParameters: Map<String, Any> = emptyMap(),
        ): BerryCrushConfigurationProvider = BerryCrushConfigurationWrapper(configuration, runtimeParameters)
    }
}

private class BerryCrushConfigurationWrapper(
    private var configuration: BerryCrushConfiguration,
    private val runtimeParameters: Map<String, Any>,
) : BerryCrushConfigurationProvider {
    override val baseUrl: String?
        get() = configuration.baseUrl
    override val timeout: Duration
        get() = configuration.timeout
    override val defaultHeaders: Map<String, String>
        get() = configuration.defaultHeaders
    override val environment: String?
        get() = configuration.environment
    override val autoAssertions: AutoAssertionConfig
        get() = configuration.autoAssertions
    override val strictSchemaValidation: Boolean
        get() = configuration.strictSchemaValidation
    override val followRedirects: Boolean
        get() = configuration.followRedirects
    override val logRequests: Boolean
        get() = configuration.logRequests
    override val logResponses: Boolean
        get() = configuration.logResponses
    override val httpLogger: HttpLogger?
        get() = configuration.httpLogger
    override val logFormatter: HttpLogFormatter?
        get() = configuration.logFormatter
    override val shareVariablesAcrossScenarios: Boolean
        get() = configuration.shareVariablesAcrossScenarios
    override val multiTestConfig: Map<String, Any>
        get() = configuration.multiTestConfig
    override val errorContextConfig: ErrorContextConfig
        get() = configuration.errorContextConfig
    override val retryConfig: RetryConfig
        get() = configuration.retryConfig
    override val bindings: Map<String, BindingConfig>
        get() = configuration.bindings

    override fun <R> withParameters(
        parameters: Map<String, Any>,
        block: () -> R,
    ): R {
        val savedConfiguration = configuration
        val effectiveParameters = runtimeParameters + parameters
        return try {
            configuration = configuration.withParameters(effectiveParameters)
            block()
        } finally {
            configuration = savedConfiguration
        }
    }

    override fun toParameterMap(): Map<String, Any> = configuration.toParameterMap() + runtimeParameters

    override fun resolveReference(resolver: (Any) -> Any) = configuration.resolveReference(resolver)
}
