package org.berrycrush.executor.step

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.AutoTestGenerator
import org.berrycrush.autotest.MultiTestResult
import org.berrycrush.autotest.RequestResult
import org.berrycrush.autotest.provider.AutoTestProviderRegistry
import org.berrycrush.autotest.provider.MultiTestProvider
import org.berrycrush.executor.BerryCrushConfigurationProvider
import org.berrycrush.executor.BerryCrushExecutionListener
import org.berrycrush.executor.assertion.AssertionExecutor
import org.berrycrush.executor.http.HttpExecutor
import org.berrycrush.model.Assertion
import org.berrycrush.model.AssertionResult
import org.berrycrush.model.AutoTestResult
import org.berrycrush.model.ConditionalAssertion
import org.berrycrush.model.HttpRequest
import org.berrycrush.model.HttpResponse
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import org.berrycrush.openapi.ResolvedOperation
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.plugin.StepContext
import org.berrycrush.scenario.AutoTestType
import org.berrycrush.util.toNonNullMap
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import org.berrycrush.autotest.AutoTestType as TestType

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
 * @property assertionRunner Function to run assertions against responses
 */
@Suppress("TooManyFunctions")
class AutoTestExecutor(
    private val specRegistry: SpecRegistry,
    private val configuration: BerryCrushConfigurationProvider,
    private val httpExecutor: HttpExecutor,
    private val assertionRunner: (HttpResponse, List<Assertion>, StepContext) -> List<AssertionResult>,
    private val conditionalRunner: (HttpResponse, List<ConditionalAssertion>, StepContext) -> AssertionExecutor.ConditionalRunResult,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
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
        context: StepContext,
        stepStartTime: Instant,
        listener: BerryCrushExecutionListener = BerryCrushExecutionListener.NOOP,
    ): StepResult {
        val autoTestConfig = step.autoTestConfig!!
        val operationId = step.operationId!!

        // Resolve the operation to get the OpenAPI spec
        val (spec, operation) = specRegistry.resolve(operationId, step.specName)

        // Create the auto-test generator
        val generator = AutoTestGenerator.fromSpec(spec)

        // Extract base body from step if present
        val baseBody = extractBaseBody(step, operation, context)

        // Extract base path params from step
        val basePathParams = context.resolveParams(step.pathParams)
        // Extract base headers from step
        val baseHeaders = context.resolveParams(step.headers)

        // Generate test cases
        val allTestCases =
            generator.generateTestCases(
                operationId = operationId,
                testTypes = autoTestConfig.types.map { it.toTestType() }.toSet(),
                baseBody = baseBody,
                basePathParams = basePathParams,
                baseHeaders = baseHeaders.toNonNullMap(),
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
            listener.onAutoTestCompleted(testResult)

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
        operation: ResolvedOperation?,
        context: StepContext,
    ): Map<String, Any>? =
        when (val maybeStep = step.check()) {
            // Use default Schema values
            null -> httpExecutor.resolveBody(emptyMap(), operation, context)
            else ->
                httpExecutor.resolveBody(maybeStep, operation, context)?.let { body ->
                    objectMapper.readValue(body, Map::class.java) as Map<String, Any>
                }
        }

    private fun Step.check(): Step? =
        if (body != null || bodyProperties != null || bodyFile != null) {
            this
        } else {
            null
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
            step.headers
                .toMutableMap()
                .apply {
                    putAll(testCase.headers)
                }.toMap()

        return TestCaseParams(testBody, testPathParams, testHeaders)
    }

    private fun setupTestCaseContext(
        testCase: AutoTestCase,
        context: StepContext,
    ) {
        context["test.type"] = testCase.type.name.lowercase()
        context["test.testType"] = testCase.testType
        context["test.field"] = testCase.fieldName
        context["test.description"] = testCase.description
        context["test.value"] = testCase.invalidValue?.toString() ?: "null"
        context["test.location"] = testCase.location.name.lowercase()
    }

    private fun isSecurityTestBlocked(
        testCase: AutoTestCase,
        error: Exception,
    ): Boolean {
        val isSecurityTest = testCase.type == AutoTestType.SECURITY.toTestType()
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
        context: StepContext,
    ): AutoTestResult {
        setupTestCaseContext(testCase, context)
        val testStartTime = Instant.now()

        return runCatching {
            executeAndAssert(step, testCase, context, testStartTime)
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
        context: StepContext,
        testStartTime: Instant,
    ): AutoTestResult {
        val params = buildTestCaseParams(step, testCase)
        val (spec, operation) = specRegistry.resolve(step.operationId!!, step.specName)
        val request =
            HttpRequest(
                operation.method,
                httpExecutor.resolveUrl(step, spec, operation, context, params.pathParams),
                params.headers,
                params.body,
            )
        val response = httpExecutor.execute(request, context)
        val assertionResults = assertionRunner(response, step.assertions, context)
        val conditionalResult = conditionalRunner(response, step.conditionals, context)
        val allResults = assertionResults + conditionalResult.assertionResults

        return AutoTestResult(
            testCase = testCase,
            passed = allResults.all { it.passed },
            statusCode = response.statusCode,
            responseBody = response.body?.take(RESPONSE_BODY_PREVIEW_LENGTH),
            assertionResults = allResults,
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
        context: StepContext,
        stepStartTime: Instant,
        parameters: Map<String, Any?>,
        listener: BerryCrushExecutionListener = BerryCrushExecutionListener.NOOP,
    ): StepResult {
        val autoTestConfig = step.autoTestConfig!!

        // Get the provider registry
        val registry = AutoTestProviderRegistry.default
        val excludes = autoTestConfig.excludes.map { it.lowercase() }.toSet()

        val multiTestResults = mutableListOf<MultiTestResult>()
        // run multi test
        registry
            .getMultiTestProviders()
            .filter { !excludes.contains(it.mode) }
            .forEach { provider ->
                executeMultiProvider(
                    provider,
                    step,
                    context,
                    listener,
                    parameters,
                    multiTestResults,
                )
            }
        // Aggregate and return results
        return buildMultiTestResult(step, stepStartTime, multiTestResults)
    }

    /**
     * Execute a multi-test mode if the provider exists, adding results to the list.
     */
    private fun executeMultiProvider(
        provider: MultiTestProvider,
        step: Step,
        context: StepContext,
        listener: BerryCrushExecutionListener,
        parameters: Map<String, Any?>,
        results: MutableList<MultiTestResult>,
    ) {
        val count = parameters["multiTest.${provider.mode}.count"] as Int? ?: provider.defaultCount
        listener.onMultiTestStarting(provider.mode, count)
        val result = executeMultiTestMode(step, context, provider, count)
        context.setupParameters(provider.mode, count, result)
        results.add(result)
        listener.onMultiTestCompleted(result)
        logMultiTest(result)
    }

    private fun StepContext.setupParameters(
        mode: String,
        count: Int,
        result: MultiTestResult,
    ) {
        this["multiTest.mode"] = mode.uppercase()
        this["multiTest.count"] = count
        this["multiTest.result"] = result.passed
        this["multiTest.duration"] = result.totalDuration
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

        return StepResult(
            step = step,
            status = if (allPassed) ResultStatus.PASSED else ResultStatus.FAILED,
            response = firstResponse?.response,
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
        context: StepContext,
        provider: MultiTestProvider,
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
        context: StepContext,
        requestIndex: Int,
    ): RequestResult {
        val requestStartTime = Instant.now()

        return runCatching {
            val response = httpExecutor.execute(step, specRegistry, context)

            RequestResult.create(
                requestIndex = requestIndex,
                response = response,
            )
        }.getOrElse { _ ->
            RequestResult.create(
                requestIndex = requestIndex,
                duration = Duration.between(requestStartTime, Instant.now()),
            )
        }
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
                    append("[${result.mode.lowercase()}] ")
                    append("${result.requestCount} requests ")
                    append("(${result.totalDuration}ms total)")
                    if (!result.passed) {
                        append(" - ${result.failureReason}")
                    }
                }
            println(message)
        }
    }
}

private fun AutoTestType.toTestType() =
    when (this) {
        AutoTestType.INVALID -> TestType.INVALID
        AutoTestType.SECURITY -> TestType.SECURITY
        AutoTestType.MULTI -> TestType.MULTI
    }
