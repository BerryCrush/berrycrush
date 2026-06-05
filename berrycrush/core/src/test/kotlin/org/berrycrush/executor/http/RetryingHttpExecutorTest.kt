package org.berrycrush.executor.http

import org.berrycrush.config.RetryConfig
import org.berrycrush.context.ExecutionContext
import org.berrycrush.exception.RetryExhaustedException
import org.berrycrush.model.Step
import org.berrycrush.model.StepType
import org.berrycrush.openapi.HttpMethod
import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.OpenApiVersion
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.openapi.ServerInfo
import org.berrycrush.openapi.SpecInfo
import java.net.ConnectException
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Optional
import javax.net.ssl.SSLSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [RetryingHttpExecutor].
 */
class RetryingHttpExecutorTest {
    private val context = ExecutionContext()

    private val step =
        Step(
            type = StepType.WHEN,
            description = "test step",
            operationId = "testOp",
        )

    @Test
    fun `delegates directly when retry is disabled`() {
        val delegate = CountingHttpExecutor(listOf(200))
        val config = RetryConfig.DISABLED
        val executor = RetryingHttpExecutor(delegate, config)

        val result = executor.execute(step, delegate.mockSpec, delegate.mockOperation, context)

        assertEquals(200, result.statusCode())
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun `returns immediately on success without retry`() {
        val delegate = CountingHttpExecutor(listOf(200))
        val config = RetryConfig(maxAttempts = 3)
        val executor = RetryingHttpExecutor(delegate, config)

        val result = executor.execute(step, delegate.mockSpec, delegate.mockOperation, context)

        assertEquals(200, result.statusCode())
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun `retries on retryable status code`() {
        // Return 503 twice, then 200
        val delegate = CountingHttpExecutor(listOf(503, 503, 200))
        val config =
            RetryConfig(
                maxAttempts = 3,
                delay = Duration.ofMillis(1), // Minimal delay for fast tests
                jitter = false,
            )
        val executor = RetryingHttpExecutor(delegate, config)

        val result = executor.execute(step, delegate.mockSpec, delegate.mockOperation, context)

        assertEquals(200, result.statusCode())
        assertEquals(3, delegate.callCount)
    }

    @Test
    fun `retries on retryable exception`() {
        val delegate =
            ExceptionThenSuccessExecutor(
                exceptionsToThrow = 1,
                successStatusCode = 200,
            )
        val config =
            RetryConfig(
                maxAttempts = 2,
                delay = Duration.ofMillis(1),
                jitter = false,
            )
        val executor = RetryingHttpExecutor(delegate, config)

        val result = executor.execute(step, delegate.mockSpec, delegate.mockOperation, context)

        assertEquals(200, result.statusCode())
        assertEquals(2, delegate.callCount)
    }

    @Test
    fun `throws RetryExhaustedException after max attempts with status`() {
        // Always return 503
        val delegate = CountingHttpExecutor(listOf(503, 503, 503, 503))
        val config =
            RetryConfig(
                maxAttempts = 2,
                delay = Duration.ofMillis(1),
                jitter = false,
            )
        val executor = RetryingHttpExecutor(delegate, config)

        val exception =
            assertFailsWith<RetryExhaustedException> {
                executor.execute(step, delegate.mockSpec, delegate.mockOperation, context)
            }

        assertEquals(3, exception.attempts) // 1 initial + 2 retries
        assertNotNull(exception.lastResponse)
        assertEquals(503, exception.lastResponse.statusCode())
        assertNull(exception.lastException)
    }

    @Test
    fun `throws RetryExhaustedException after max attempts with exception`() {
        val delegate =
            ExceptionThenSuccessExecutor(
                exceptionsToThrow = 10, // Always throw
                successStatusCode = 200,
            )
        val config =
            RetryConfig(
                maxAttempts = 2,
                delay = Duration.ofMillis(1),
                jitter = false,
            )
        val executor = RetryingHttpExecutor(delegate, config)

        val exception =
            assertFailsWith<RetryExhaustedException> {
                executor.execute(step, delegate.mockSpec, delegate.mockOperation, context)
            }

        assertEquals(3, exception.attempts)
        assertNull(exception.lastResponse)
        assertNotNull(exception.lastException)
        assertTrue(exception.lastException is ConnectException)
    }

    @Test
    fun `does not retry on non-retryable status code`() {
        val delegate = CountingHttpExecutor(listOf(400))
        val config = RetryConfig(maxAttempts = 3)
        val executor = RetryingHttpExecutor(delegate, config)

        val result = executor.execute(step, delegate.mockSpec, delegate.mockOperation, context)

        assertEquals(400, result.statusCode())
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun `does not retry on non-retryable exception`() {
        val delegate =
            object : TestHttpExecutor() {
                override fun execute(
                    step: Step,
                    spec: LoadedSpec,
                    operation: ResolvedOperation,
                    context: ExecutionContext,
                ): HttpResponse<String> {
                    callCount++
                    throw IllegalArgumentException("Bad argument")
                }
            }
        val config = RetryConfig(maxAttempts = 3)
        val executor = RetryingHttpExecutor(delegate, config)

        assertFailsWith<IllegalArgumentException> {
            executor.execute(step, delegate.mockSpec, delegate.mockOperation, context)
        }

        assertEquals(1, delegate.callCount)
    }

    // --- Test Helpers ---

    /**
     * Base test HTTP executor with mock spec and operation.
     */
    abstract class TestHttpExecutor : HttpExecutor {
        var callCount = 0

        val mockSpec: LoadedSpec by lazy { createMockSpec() }
        val mockOperation: ResolvedOperation by lazy { createMockOperation() }

        override fun execute(
            step: Step,
            spec: LoadedSpec,
            operation: ResolvedOperation,
            context: ExecutionContext,
        ): HttpResponse<String> {
            callCount++
            return createMockResponse(200)
        }

        override fun resolveBody(
            step: Step,
            operation: ResolvedOperation?,
            context: ExecutionContext,
        ): String? = null
    }

    /**
     * HTTP executor that returns responses from a list in order.
     */
    private class CountingHttpExecutor(
        private val statusCodes: List<Int>,
    ) : TestHttpExecutor() {
        override fun execute(
            step: Step,
            spec: LoadedSpec,
            operation: ResolvedOperation,
            context: ExecutionContext,
        ): HttpResponse<String> {
            val statusCode = statusCodes.getOrElse(callCount) { statusCodes.last() }
            callCount++
            return createMockResponse(statusCode)
        }
    }

    /**
     * HTTP executor that throws exceptions for a number of calls, then succeeds.
     */
    private class ExceptionThenSuccessExecutor(
        private val exceptionsToThrow: Int,
        private val successStatusCode: Int,
    ) : TestHttpExecutor() {
        override fun execute(
            step: Step,
            spec: LoadedSpec,
            operation: ResolvedOperation,
            context: ExecutionContext,
        ): HttpResponse<String> {
            callCount++
            if (callCount <= exceptionsToThrow) {
                throw ConnectException("Connection refused")
            }
            return createMockResponse(successStatusCode)
        }
    }

    companion object {
        /**
         * Create a minimal LoadedSpec for testing.
         */
        fun createMockSpec(): LoadedSpec {
            // Create a minimal OpenApiSpec for testing
            val emptyOpenApiSpec =
                object : org.berrycrush.openapi.OpenApiSpec {
                    override val rawModel: Any = Any()
                    override val version: OpenApiVersion = OpenApiVersion.V3_0_X
                    override val specVersion: String = "3.0.0"
                    override val info: SpecInfo =
                        SpecInfo(
                            title = "test",
                            description = null,
                            version = "1.0.0",
                            contact = null,
                            license = null,
                        )
                    override val servers: List<ServerInfo> = emptyList()
                    override val paths: Map<String, org.berrycrush.openapi.PathSpec> = emptyMap()
                    override val components: org.berrycrush.openapi.ComponentsSpec? = null
                    override val webhooks: Map<String, org.berrycrush.openapi.PathSpec> = emptyMap()

                    override fun getOperation(operationId: String): org.berrycrush.openapi.OperationSpec? = null

                    override fun getAllOperations(): List<org.berrycrush.openapi.OperationSpec> = emptyList()
                }

            return LoadedSpec(
                name = "test",
                path = "test.yaml",
                spec = emptyOpenApiSpec,
                baseUrl = "http://localhost",
                defaultHeaders = emptyMap(),
            )
        }

        /**
         * Create a minimal ResolvedOperation for testing.
         */
        fun createMockOperation(): ResolvedOperation {
            val mockOperationSpec =
                object : org.berrycrush.openapi.OperationSpec {
                    override val operationId: String = "testOp"
                    override val path: String = "/test"
                    override val method: HttpMethod = HttpMethod.GET
                    override val summary: String? = null
                    override val description: String? = null
                    override val tags: List<String> = emptyList()
                    override val parameters: List<org.berrycrush.openapi.ParameterSpec> = emptyList()
                    override val requestBody: org.berrycrush.openapi.RequestBodySpec? = null
                    override val responses: Map<String, org.berrycrush.openapi.ResponseSpec> = emptyMap()
                    override val security: List<Map<String, List<String>>>? = null
                    override val deprecated: Boolean = false
                    override val callbacks: Map<String, Map<String, org.berrycrush.openapi.PathSpec>> = emptyMap()
                }

            return ResolvedOperation(
                operationId = "testOp",
                path = "/test",
                method = HttpMethod.GET,
                operation = mockOperationSpec,
            )
        }

        fun createMockResponse(statusCode: Int): HttpResponse<String> =
            object : HttpResponse<String> {
                override fun statusCode(): Int = statusCode

                override fun body(): String = "{}"

                override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }

                override fun request(): java.net.http.HttpRequest = throw UnsupportedOperationException()

                override fun previousResponse(): Optional<HttpResponse<String>> = Optional.empty()

                override fun sslSession(): Optional<SSLSession> = Optional.empty()

                override fun uri(): java.net.URI = java.net.URI.create("http://localhost/test")

                override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
            }
    }
}
