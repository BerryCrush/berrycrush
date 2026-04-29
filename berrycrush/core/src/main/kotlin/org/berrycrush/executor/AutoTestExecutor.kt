package org.berrycrush.executor

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.AutoTestGenerator
import org.berrycrush.autotest.MultiMode
import org.berrycrush.autotest.MultiTestParameters
import org.berrycrush.autotest.MultiTestResult
import org.berrycrush.autotest.RequestResult
import org.berrycrush.autotest.provider.AutoTestProviderRegistry
import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.context.ExecutionContext
import org.berrycrush.model.Assertion
import org.berrycrush.model.AssertionResult
import org.berrycrush.model.AutoTestResult
import org.berrycrush.model.BodyProperty
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.scenario.AutoTestType
import tools.jackson.databind.ObjectMapper
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

private const val RESPONSE_BODY_PREVIEW_LENGTH = 500

/**
 * Executes auto-generated invalid and security tests for API endpoints.
 *
 * This class is responsible for:
 * - Generating test cases based on OpenAPI schema constraints
 * - Executing each test case with modified parameters
 * - Setting context variables for conditional assertions
 * - Collecting and reporting test results
 *
 * Auto-tests are generated for:
 * - **Invalid tests**: Violate schema constraints (minLength, maxLength, pattern, required, enum, type)
 * - **Security tests**: Inject common attack payloads (SQL injection, XSS, path traversal, etc.)
 *
 * @property specRegistry Registry for OpenAPI specifications
 * @property configuration Execution configuration
 * @property httpBuilder HTTP request builder for executing requests
 * @property assertionRunner Function to run assertions against responses
 */
class AutoTestExecutor(
    private val specRegistry: SpecRegistry,
    private val configuration: BerryCrushConfiguration,
    private val httpBuilder: HttpRequestBuilder,
    private val assertionRunner: (HttpResponse<String>, List<Assertion>, ExecutionContext) -> List<AssertionResult>,
    private val paramResolver: (Map<String, Any>, ExecutionContext) -> Map<String, Any>,
    private val requestLogger: (String, String, Map<String, String>, String?) -> Unit,
    private val responseLogger: (String, String, HttpResponse<String>, Long) -> Unit,
) {
    private val objectMapper = ObjectMapper()

    /**
     * Execute auto-generated tests for a step with autoTestConfig.
     *
     * This generates invalid and/or security test cases based on the OpenAPI schema
     * and executes each one, setting context variables for conditional assertions.
     *
     * @param step The step with auto-test configuration
     * @param context The execution context for variable interpolation
     * @param stepStartTime When the step started (for duration calculation)
     * @param listener Listener for execution events
     * @return StepResult containing all auto-test results
     */
    fun executeAutoTests(
        step: Step,
        context: ExecutionContext,
        stepStartTime: Instant,
        listener: BerryCrushExecutionListener = BerryCrushExecutionListener.NOOP,
    ): StepResult {
        val autoTestConfig = step.autoTestConfig!!
        val operationId = step.operationId!!

        // Resolve the operation to get the OpenAPI spec
        val (spec, _) = specRegistry.resolve(operationId, step.specName)

        // Create the auto-test generator
        val generator = AutoTestGenerator.fromSpec(spec)

        // Extract base body from step if present
        val baseBody = extractBaseBody(step, context)

        // Extract base path params from step
        val basePathParams =
            step.pathParams.mapValues { (_, v) ->
                when (v) {
                    is String -> context.interpolate(v)
                    else -> v
                }
            }

        // Extract base headers from step
        val baseHeaders = step.headers.mapValues { (_, v) -> context.interpolate(v) }

        // Generate test cases
        val allTestCases =
            generator.generateTestCases(
                operationId = operationId,
                testTypes = autoTestConfig.types,
                baseBody = baseBody,
                basePathParams = basePathParams,
                baseHeaders = baseHeaders,
            )

        // Filter out excluded tests
        val testCases = filterExcludedTests(allTestCases, autoTestConfig.excludes)

        if (testCases.isEmpty()) {
            // No test cases generated - just pass
            return StepResult(
                step = step,
                status = ResultStatus.PASSED,
                duration = Duration.between(stepStartTime, Instant.now()),
                message = "No auto-test cases generated (operation may not have parameters or constraints)",
            )
        }

        // Execute each test case and collect results
        val allResults = mutableListOf<AutoTestResult>()

        for (testCase in testCases) {
            // Notify listener that test is starting
            listener.onAutoTestStarting(testCase)

            val testResult = executeAutoTestCase(step, testCase, context)
            allResults.add(testResult)

            // Notify listener that test finished
            listener.onAutoTestCompleted(testCase, testResult)

            // Log the test case execution
            logAutoTestCase(testCase, testResult)
        }

        // Aggregate results
        val failedCount = allResults.count { !it.passed }
        val totalCount = allResults.size

        return StepResult(
            step = step,
            status = if (failedCount == 0) ResultStatus.PASSED else ResultStatus.FAILED,
            duration = Duration.between(stepStartTime, Instant.now()),
            message = "Auto-tests: $totalCount executed, $failedCount failed",
            autoTestResults = allResults,
        )
    }

    /**
     * Extract base body map from step for auto-test generation.
     *
     * @param step The step containing the body
     * @param context Execution context for variable interpolation
     * @return Map representation of the body, or null if no body specified
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractBaseBody(
        step: Step,
        context: ExecutionContext,
    ): Map<String, Any>? {
        // From inline body JSON
        step.body?.let { bodyStr ->
            val interpolated = context.interpolate(bodyStr)
            return runCatching {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(interpolated, Map::class.java) as Map<String, Any>
            }.getOrNull()
        }

        // From structured body properties
        step.bodyProperties?.let { props ->
            return flattenBodyProperties(props, context)
        }

        return null
    }

    /**
     * Flatten body properties to a simple map for auto-test base body.
     *
     * @param props The body properties to flatten
     * @param context Execution context for variable interpolation
     * @return Flattened map representation
     */
    private fun flattenBodyProperties(
        props: Map<String, BodyProperty>,
        context: ExecutionContext,
    ): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((key, value) in props) {
            when (value) {
                is BodyProperty.Simple -> {
                    val resolved =
                        when (val v = value.value) {
                            is String -> context.interpolate(v)
                            else -> v
                        }
                    result[key] = resolved
                }
                is BodyProperty.Nested -> {
                    result[key] = flattenBodyProperties(value.properties, context)
                }
            }
        }
        return result
    }

    /**
     * Parameters prepared for a test case execution.
     */
    private data class TestCaseParams(
        val body: String?,
        val pathParams: Map<String, Any>,
        val headers: Map<String, String>,
    )

    private fun buildTestCaseParams(
        step: Step,
        testCase: AutoTestCase,
    ): TestCaseParams {
        val testBody =
            if (testCase.body.isNotEmpty()) {
                objectMapper.writeValueAsString(testCase.body)
            } else {
                step.body
            }

        val testPathParams = mutableMapOf<String, Any>()
        testPathParams.putAll(step.pathParams)
        testCase.pathParams.forEach { (k, v) -> testPathParams[k] = v ?: "" }

        val testHeaders =
            step.headers.toMutableMap().apply {
                putAll(testCase.headers)
            }

        return TestCaseParams(testBody, testPathParams, testHeaders)
    }

    private fun setupTestCaseContext(
        testCase: AutoTestCase,
        context: ExecutionContext,
    ) {
        context["test.type"] = testCase.type.name.lowercase()
        context["test.field"] = testCase.fieldName
        context["test.description"] = testCase.description
        context["test.value"] = testCase.invalidValue?.toString() ?: "null"
        context["test.location"] = testCase.location.name.lowercase()
    }

    private fun isSecurityTestBlocked(
        testCase: AutoTestCase,
        error: Exception,
    ): Boolean {
        val isSecurityTest = testCase.type == AutoTestType.SECURITY
        val isUrlError =
            error.message?.contains("Illegal character") == true ||
                error.message?.contains("Invalid URL") == true
        return isSecurityTest && isUrlError
    }

    /**
     * Execute a single auto-test case.
     *
     * Sets up context variables for conditional assertions, builds the request with
     * modified parameters, executes the request, and runs assertions.
     *
     * @param step The original step
     * @param testCase The test case to execute
     * @param context Execution context
     * @return AutoTestResult with pass/fail status and details
     */
    private fun executeAutoTestCase(
        step: Step,
        testCase: AutoTestCase,
        context: ExecutionContext,
    ): AutoTestResult {
        setupTestCaseContext(testCase, context)
        val testStartTime = Instant.now()
        val params = buildTestCaseParams(step, testCase)

        return runCatching {
            executeAndAssert(step, testCase, params, context, testStartTime)
        }.getOrElse { e ->
            AutoTestResult(
                testCase = testCase,
                passed = isSecurityTestBlocked(testCase, e as Exception),
                error = e.message ?: e.javaClass.simpleName,
                duration = Duration.between(testStartTime, Instant.now()),
            )
        }
    }

    private fun executeAndAssert(
        step: Step,
        testCase: AutoTestCase,
        params: TestCaseParams,
        context: ExecutionContext,
        testStartTime: Instant,
    ): AutoTestResult {
        val (spec, resolvedOp) = specRegistry.resolve(step.operationId!!, step.specName)
        val baseUrl = configuration.baseUrl ?: spec.baseUrl
        val url =
            httpBuilder.buildUrl(
                baseUrl = baseUrl,
                path = resolvedOp.path,
                pathParams = paramResolver(params.pathParams, context),
                queryParams = paramResolver(step.queryParams, context),
            )

        val headers = configuration.defaultHeaders + spec.defaultHeaders + params.headers
        requestLogger(resolvedOp.method.name, url, headers, params.body)

        val requestStartTime = System.currentTimeMillis()
        val response =
            httpBuilder.execute(
                method = resolvedOp.method,
                url = url,
                headers = headers,
                body = params.body,
            )
        responseLogger(resolvedOp.method.name, url, response, requestStartTime)

        context.updateLastResponse(response)
        val assertionResults = assertionRunner(response, step.assertions, context)

        return AutoTestResult(
            testCase = testCase,
            passed = assertionResults.all { it.passed },
            statusCode = response.statusCode(),
            responseBody = response.body()?.take(RESPONSE_BODY_PREVIEW_LENGTH),
            assertionResults = assertionResults,
            duration = Duration.between(testStartTime, Instant.now()),
        )
    }

    /**
     * Log auto-test case execution details.
     *
     * @param testCase The test case that was executed
     * @param result The result of the test execution
     */
    private fun logAutoTestCase(
        testCase: AutoTestCase,
        result: AutoTestResult,
    ) {
        if (configuration.logRequests) {
            val status = if (result.passed) "PASS" else "FAIL"
            val message =
                buildString {
                    append("  [AUTO-TEST] [$status] ")
                    append("[${testCase.tag}] ")
                    append("${testCase.description} ")
                    append("(field=${testCase.fieldName}, status=${result.statusCode ?: "N/A"})")
                }
            println(message)
        }
    }

    /**
     * Filter out test cases that match any of the exclude patterns.
     *
     * Excludes can match:
     * - Security test categories (e.g., "SQLInjection", "XSS", "PathTraversal")
     * - Invalid test types (e.g., "minLength", "maxLength", "required", "pattern", "enum", "type")
     * - Test description keywords (case-insensitive partial match)
     *
     * @param testCases The generated test cases
     * @param excludes Set of exclude patterns
     * @return Filtered list of test cases
     */
    private fun filterExcludedTests(
        testCases: List<AutoTestCase>,
        excludes: Set<String>,
    ): List<AutoTestCase> {
        if (excludes.isEmpty()) {
            return testCases
        }

        // Normalize exclude patterns for case-insensitive matching
        val normalizedExcludes = excludes.map { it.lowercase().replace(" ", "") }

        return testCases.filter { testCase ->
            val description = testCase.description.lowercase().replace(" ", "")
            val tag = testCase.tag.lowercase().replace(" ", "")

            // Check if any exclude pattern matches
            !normalizedExcludes.any { exclude ->
                description.contains(exclude) ||
                    tag.contains(exclude) ||
                    // Also match common category name formats
                    matchesCategoryPattern(description, exclude)
            }
        }
    }

    /**
     * Check if a description matches a category pattern.
     *
     * Supports various naming conventions:
     * - "SQLInjection" matches "sql injection", "SQL Injection", "SQL_Injection"
     * - "maxLength" matches "maxlength", "max_length", "max length", "Maximum length"
     */
    private fun matchesCategoryPattern(
        description: String,
        pattern: String,
    ): Boolean {
        // Map common pattern aliases
        val patternAliases =
            mapOf(
                "sqlinjection" to listOf("sql", "injection", "union", "select", "drop"),
                "xss" to listOf("script", "alert", "onerror", "javascript"),
                "pathtraversal" to listOf("path", "traversal", "../", "..\\"),
                "commandinjection" to listOf("command", "injection", "exec", "system"),
                "ldapinjection" to listOf("ldap", "filter"),
                "xxe" to listOf("xxe", "entity", "doctype"),
                "xmlinjection" to listOf("xml", "cdata"),
                "headerinjection" to listOf("header", "crlf", "injection"),
                "minlength" to listOf("minimum", "minlength", "too short", "below minimum"),
                "maxlength" to listOf("maximum", "maxlength", "too long", "exceeds maximum"),
                "required" to listOf("required", "missing"),
                "pattern" to listOf("pattern", "format", "invalid format"),
                "enum" to listOf("enum", "invalid value", "not in allowed"),
                "type" to listOf("type", "invalid type", "wrong type"),
            )

        val aliases = patternAliases[pattern] ?: return false
        return aliases.any { alias -> description.contains(alias) }
    }

    /**
     * Execute multi-request idempotency tests for a step.
     *
     * This sends multiple identical requests either sequentially or concurrently
     * to verify that the API operation is idempotent.
     *
     * @param step The step with auto-test configuration including MULTI type
     * @param context The execution context for variable interpolation
     * @param stepStartTime When the step started (for duration calculation)
     * @param parameters Parameters map containing multi-test configuration
     * @param listener Listener for execution events
     * @return StepResult containing multi-test results
     */
    fun executeMultiTests(
        step: Step,
        context: ExecutionContext,
        stepStartTime: Instant,
        parameters: Map<String, Any?>,
        listener: BerryCrushExecutionListener = BerryCrushExecutionListener.NOOP,
    ): StepResult {
        val autoTestConfig = step.autoTestConfig!!
        val operationId = step.operationId!!

        // Get the provider registry
        val registry = AutoTestProviderRegistry.default

        // Determine which modes to run based on excludes
        val excludes = autoTestConfig.excludes.map { it.lowercase() }.toSet()
        val runSequential = "sequential" !in excludes
        val runConcurrent = "concurrent" !in excludes

        // Get counts from parameters
        val sequentialCount = MultiTestParameters.getSequentialCount(parameters)
        val concurrentCount = MultiTestParameters.getConcurrentCount(parameters)

        val multiTestResults = mutableListOf<MultiTestResult>()

        // Execute sequential tests if not excluded
        if (runSequential) {
            executeIfProviderExists(
                registry,
                "sequential",
                MultiMode.SEQUENTIAL,
                sequentialCount,
                step,
                context,
                listener,
                multiTestResults,
            )
        }

        // Execute concurrent tests if not excluded
        if (runConcurrent) {
            executeIfProviderExists(
                registry,
                "concurrent",
                MultiMode.CONCURRENT,
                concurrentCount,
                step,
                context,
                listener,
                multiTestResults,
            )
        }

        // Aggregate and return results
        return buildMultiTestResult(step, stepStartTime, multiTestResults)
    }

    /**
     * Execute a multi-test mode if the provider exists, adding results to the list.
     */
    private fun executeIfProviderExists(
        registry: AutoTestProviderRegistry,
        providerName: String,
        mode: MultiMode,
        count: Int,
        step: Step,
        context: ExecutionContext,
        listener: BerryCrushExecutionListener,
        results: MutableList<MultiTestResult>,
    ) {
        val provider = registry.getMultiTestProvider(providerName) ?: return
        listener.onMultiTestStarting(mode, count)
        val result = executeMultiTestMode(step, context, provider, count)
        results.add(result)
        listener.onMultiTestCompleted(result)
        logMultiTest(result)
    }

    /**
     * Build the final StepResult from multi-test results.
     */
    private fun buildMultiTestResult(
        step: Step,
        stepStartTime: Instant,
        multiTestResults: List<MultiTestResult>,
    ): StepResult {
        val allPassed = multiTestResults.all { it.passed }
        val failedCount = multiTestResults.count { !it.passed }
        val totalModes = multiTestResults.size

        // Extract reference response from first successful result for assertions
        val firstResult = multiTestResults.firstOrNull()
        val firstResponse = firstResult?.results?.firstOrNull()
        val referenceStatusCode = firstResponse?.statusCode
        val referenceBody = firstResponse?.body?.toString()
        val referenceHeaders =
            firstResponse?.headers?.mapValues { listOf(it.value) } ?: emptyMap()

        return StepResult(
            step = step,
            status = if (allPassed) ResultStatus.PASSED else ResultStatus.FAILED,
            statusCode = referenceStatusCode,
            responseBody = referenceBody,
            responseHeaders = referenceHeaders,
            duration = Duration.between(stepStartTime, Instant.now()),
            message = "Multi-tests: $totalModes modes executed, $failedCount failed",
            multiTestResults = multiTestResults,
        )
    }

    /**
     * Execute a single multi-test mode using the given provider.
     */
    private fun executeMultiTestMode(
        step: Step,
        context: ExecutionContext,
        provider: org.berrycrush.autotest.provider.MultiTestProvider,
        count: Int,
    ): MultiTestResult =
        provider.executeMultiTest(count) { requestIndex ->
            executeRequestForMultiTest(step, context, requestIndex)
        }

    /**
     * Execute a single HTTP request for multi-test.
     */
    private fun executeRequestForMultiTest(
        step: Step,
        context: ExecutionContext,
        requestIndex: Int,
    ): RequestResult {
        val requestStartTime = System.currentTimeMillis()

        return runCatching {
            // Resolve the operation
            val (spec, resolvedOp) = specRegistry.resolve(step.operationId!!, step.specName)

            // Build URL
            val baseUrl = configuration.baseUrl ?: spec.baseUrl
            val url =
                httpBuilder.buildUrl(
                    baseUrl = baseUrl,
                    path = resolvedOp.path,
                    pathParams = paramResolver(step.pathParams, context),
                    queryParams = paramResolver(step.queryParams, context),
                )

            // Merge headers
            val headers = configuration.defaultHeaders + spec.defaultHeaders + step.headers

            // Resolve body
            val body =
                step.body?.let { context.interpolate(it) }
                    ?: step.bodyProperties?.let { buildBodyFromProperties(it, context) }

            // Log request if enabled
            requestLogger(resolvedOp.method.name, url, headers, body)

            // Execute the request
            val response =
                httpBuilder.execute(
                    method = resolvedOp.method,
                    url = url,
                    headers = headers,
                    body = body,
                )

            // Log response if enabled
            responseLogger(resolvedOp.method.name, url, response, requestStartTime)

            // Update context with response for subsequent assertions
            context.updateLastResponse(response)

            val durationMs = System.currentTimeMillis() - requestStartTime

            RequestResult.create(
                requestIndex = requestIndex,
                statusCode = response.statusCode(),
                body = response.body(),
                headers = response.headers().map().mapValues { it.value.firstOrNull() ?: "" },
                durationMs = durationMs,
            )
        }.getOrElse { e ->
            val durationMs = System.currentTimeMillis() - requestStartTime
            RequestResult.create(
                requestIndex = requestIndex,
                statusCode = -1,
                body = e.message,
                headers = emptyMap(),
                durationMs = durationMs,
            )
        }
    }

    /**
     * Build JSON body from structured body properties.
     */
    private fun buildBodyFromProperties(
        props: Map<String, BodyProperty>,
        context: ExecutionContext,
    ): String {
        val map = flattenBodyProperties(props, context)
        return objectMapper.writeValueAsString(map)
    }

    /**
     * Log multi-test execution details.
     */
    private fun logMultiTest(result: MultiTestResult) {
        if (configuration.logRequests) {
            val status = if (result.passed) "PASS" else "FAIL"
            val message =
                buildString {
                    append("  [MULTI-TEST] [$status] ")
                    append("[${result.mode.name.lowercase()}] ")
                    append("${result.requestCount} requests ")
                    append("(${result.totalDurationMs}ms total)")
                    if (!result.passed) {
                        append(" - ${result.failureReason}")
                    }
                }
            println(message)
        }
    }
}
