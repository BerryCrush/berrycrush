package org.berrycrush.executor

import com.jayway.jsonpath.JsonPath
import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.context.ExecutionContext
import org.berrycrush.context.MutableTestExecutionContext
import org.berrycrush.exception.HttpExecutionException
import org.berrycrush.exception.ScenarioErrorContext
import org.berrycrush.executor.assertion.AssertionEngine
import org.berrycrush.executor.assertion.DefaultAssertionEngine
import org.berrycrush.executor.fragment.DefaultFragmentExecutor
import org.berrycrush.executor.fragment.FragmentExecutor
import org.berrycrush.executor.http.DefaultHttpExecutor
import org.berrycrush.executor.http.HttpExecutor
import org.berrycrush.model.Assertion
import org.berrycrush.model.AssertionResult
import org.berrycrush.model.AutoTestConfig
import org.berrycrush.model.Condition
import org.berrycrush.model.ConditionalActions
import org.berrycrush.model.ConditionalAssertion
import org.berrycrush.model.CustomAssertionDefinition
import org.berrycrush.model.FragmentRegistry
import org.berrycrush.model.ResultStatus
import org.berrycrush.model.Scenario
import org.berrycrush.model.ScenarioResult
import org.berrycrush.model.Step
import org.berrycrush.model.StepResult
import org.berrycrush.openapi.HttpMethod
import org.berrycrush.openapi.SpecRegistry
import org.berrycrush.plugin.PluginRegistry
import org.berrycrush.plugin.adapter.ScenarioContextAdapter
import org.berrycrush.plugin.adapter.ScenarioResultAdapter
import org.berrycrush.plugin.adapter.StepContextAdapter
import org.berrycrush.plugin.adapter.StepResultAdapter
import org.berrycrush.scenario.AutoTestType
import org.berrycrush.step.StepContext
import org.berrycrush.step.StepContextImpl
import org.berrycrush.step.StepMatch
import org.berrycrush.step.StepRegistry
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import org.berrycrush.executor.assertion.AssertionContext as ExecutorAssertionContext

/**
 * Executes BDD scenarios against API endpoints.
 *
 * @property specRegistry Registry for OpenAPI specifications
 * @property configuration Execution configuration
 * @property pluginRegistry Optional plugin registry for lifecycle hooks
 * @property fragmentRegistry Optional registry for reusable fragments
 * @property stepRegistry Optional registry for custom step definitions
 * @property assertionRegistry Optional registry for custom assertion definitions
 * @property assertionEngine Engine for evaluating assertions and conditions
 * @property httpExecutor Executor for HTTP requests
 * @property fragmentExecutor Executor for fragment expansion
 */
class BerryCrushScenarioExecutor(
    private val specRegistry: SpecRegistry,
    private val configuration: BerryCrushConfiguration,
    private val pluginRegistry: PluginRegistry? = null,
    private val fragmentRegistry: FragmentRegistry? = null,
    private val stepRegistry: StepRegistry? = null,
    private val assertionRegistry: org.berrycrush.assertion.AssertionRegistry? = null,
    private val assertionEngine: AssertionEngine = DefaultAssertionEngine(configuration, assertionRegistry),
    private val httpExecutor: HttpExecutor = DefaultHttpExecutor(configuration),
    private val fragmentExecutor: FragmentExecutor = DefaultFragmentExecutor(fragmentRegistry),
) {
    private val httpBuilder = HttpRequestBuilder(configuration)

    // Current execution listener for the executing scenario
    // Thread-local to support concurrent execution
    private val currentExecutionListener = ThreadLocal<BerryCrushExecutionListener>()

    // Lazy-initialized auto-test executor - created on first use to avoid circular dependencies
    private val autoTestExecutor: AutoTestExecutor by lazy {
        AutoTestExecutor(
            specRegistry = specRegistry,
            configuration = configuration,
            httpBuilder = httpBuilder,
            assertionRunner = ::runAssertions,
            paramResolver = ::resolveParams,
            requestLogger = { method, url, headers, body ->
                logRequest(HttpMethod.valueOf(method), url, headers, body)
            },
            responseLogger = { method, url, response, startTime ->
                logResponse(HttpMethod.valueOf(method), url, response, startTime)
            },
        )
    }

    /**
     * Execute a single scenario.
     *
     * @param scenario The scenario to execute
     * @param sharedContext Optional shared context for cross-scenario variable sharing.
     *                      If provided, variables from previous scenarios are available.
     * @param sourceFile Optional source file for the scenario (used in reports for grouping).
     * @param executionListener Optional listener for execution events (scenario, step, auto-test).
     *                          Used by frameworks (like JUnit) to receive real-time notifications.
     */
    fun execute(
        scenario: Scenario,
        sharedContext: ExecutionContext? = null,
        sourceFile: java.io.File? = null,
        executionListener: BerryCrushExecutionListener? = null,
    ): ScenarioResult {
        // If scenario has configuration-affecting parameters, delegate to a modified executor
        if (scenario.parameters.isNotEmpty()) {
            val modifiedConfig = configuration.withParameters(scenario.parameters)
            // Only create new executor if configuration actually changed
            if (modifiedConfig != configuration) {
                val modifiedExecutor =
                    BerryCrushScenarioExecutor(
                        specRegistry,
                        modifiedConfig,
                        pluginRegistry,
                        fragmentRegistry,
                        stepRegistry,
                        assertionRegistry,
                        DefaultAssertionEngine(modifiedConfig, assertionRegistry),
                        DefaultHttpExecutor(modifiedConfig),
                    )
                // Execute with modified executor but clear scenario parameters to avoid recursion
                return modifiedExecutor.execute(
                    scenario.copy(parameters = emptyMap()),
                    sharedContext,
                    sourceFile,
                    executionListener,
                )
            }
        }

        // Set the listener for this execution (thread-local for concurrent safety)
        val listener = executionListener ?: BerryCrushExecutionListener.NOOP
        currentExecutionListener.set(listener)

        try {
            // Notify listener that scenario is starting
            listener.onScenarioStarting(scenario)

            val startTime = Instant.now()
            val context = sharedContext?.createChild() ?: ExecutionContext()

            // Store scenario parameters in context for variable resolution
            for ((key, value) in scenario.parameters) {
                context["param.$key"] = value
            }

            val scenarioContext = ScenarioContextAdapter(scenario, context, startTime, sourceFile)

            pluginRegistry?.dispatchScenarioStart(scenarioContext)

            val stepResults = executeAllSteps(scenario, context, scenarioContext)
            val overallStatus = determineOverallStatus(stepResults)
            val duration = Duration.between(startTime, Instant.now())

            val scenarioResult =
                ScenarioResult(
                    scenario = scenario,
                    status = overallStatus,
                    stepResults = stepResults,
                    startTime = startTime,
                    duration = duration,
                )

            pluginRegistry?.dispatchScenarioEnd(scenarioContext, ScenarioResultAdapter(scenarioResult))

            // Copy extracted variables back to shared context for cross-scenario sharing
            if (sharedContext != null && scenarioResult.status == ResultStatus.PASSED) {
                context.allVariables().forEach { (name, value) ->
                    value?.let { sharedContext[name] = it }
                }
            }

            // Notify listener that scenario completed
            listener.onScenarioCompleted(scenario, scenarioResult)

            return scenarioResult
        } finally {
            // Clean up thread-local
            currentExecutionListener.remove()
        }
    }

    /**
     * Execute all steps (background + scenario) and return results.
     */
    private fun executeAllSteps(
        scenario: Scenario,
        context: ExecutionContext,
        scenarioContext: ScenarioContextAdapter,
    ): List<StepResult> {
        val stepResults = mutableListOf<StepResult>()
        var stepIndex = 0
        // Execute background steps
        var continueExecution =
            executeStepsWithContinuation(
                scenario.background,
                context,
                scenarioContext,
                stepResults,
                stepIndex,
                true,
            ) { stepIndex++ }

        // Execute scenario steps
        for (step in scenario.steps) {
            if (!continueExecution) {
                // Skip remaining steps
                stepResults.add(StepResult(step = step, status = ResultStatus.SKIPPED))
                continue
            }

            // Inject include parameters into context before expanding
            fragmentExecutor.injectParameters(step, context)

            val expandedSteps = fragmentExecutor.expand(step)
            for (expandedStep in expandedSteps) {
                if (!continueExecution) {
                    stepResults.add(StepResult(step = expandedStep, status = ResultStatus.SKIPPED))
                    continue
                }

                val result = executeStepWithPlugins(expandedStep, context, scenarioContext, stepIndex++)
                stepResults.add(result)

                if (result.status != ResultStatus.PASSED) {
                    continueExecution = false
                }
            }
        }

        return stepResults
    }

    /**
     * Execute a list of steps with continuation control.
     */
    private fun executeStepsWithContinuation(
        steps: List<Step>,
        context: ExecutionContext,
        scenarioContext: ScenarioContextAdapter,
        results: MutableList<StepResult>,
        startIndex: Int,
        initialContinue: Boolean,
        onStepExecuted: () -> Unit,
    ): Boolean {
        var continueExecution = initialContinue

        for (step in steps) {
            if (!continueExecution) break

            // Inject include parameters into context before expanding
            fragmentExecutor.injectParameters(step, context)

            val expandedSteps = fragmentExecutor.expand(step)
            for (expandedStep in expandedSteps) {
                if (!continueExecution) break

                val result = executeStepWithPlugins(expandedStep, context, scenarioContext, startIndex)
                results.add(result)
                onStepExecuted()

                if (result.status != ResultStatus.PASSED) {
                    continueExecution = false
                }
            }
        }

        return continueExecution
    }

    /**
     * Determine overall status from step results.
     */
    private fun determineOverallStatus(stepResults: List<StepResult>): ResultStatus =
        when {
            stepResults.isEmpty() -> ResultStatus.PASSED
            stepResults.any { it.status == ResultStatus.ERROR } -> ResultStatus.ERROR
            stepResults.any { it.status == ResultStatus.FAILED } -> ResultStatus.FAILED
            stepResults.all { it.status == ResultStatus.SKIPPED } -> ResultStatus.SKIPPED
            else -> ResultStatus.PASSED
        }

    private fun executeStepWithPlugins(
        step: Step,
        context: ExecutionContext,
        scenarioContext: ScenarioContextAdapter,
        stepIndex: Int,
    ): StepResult {
        // Get current execution listener
        val listener = currentExecutionListener.get() ?: BerryCrushExecutionListener.NOOP

        // Create step context
        val stepContext = StepContextAdapter(step, stepIndex, scenarioContext)

        // Notify listener that step is starting
        listener.onStepStarting(step)

        // Dispatch plugin: onStepStart
        pluginRegistry?.dispatchStepStart(stepContext)

        // Execute the actual step with scenario context for error enrichment
        val result = executeStep(step, context, scenarioContext, stepIndex)

        // Dispatch plugin: onStepEnd
        pluginRegistry?.dispatchStepEnd(stepContext, StepResultAdapter(result))

        // Notify listener that step completed
        listener.onStepCompleted(step, result)

        return result
    }

    private fun executeStep(
        step: Step,
        context: ExecutionContext,
        scenarioContext: ScenarioContextAdapter,
        stepIndex: Int,
    ): StepResult {
        val stepStartTime = Instant.now()

        // If no operation to call, check for custom step or assertions
        return step.operationId?.let {
            executeOperationStep(step, context, stepStartTime, scenarioContext, stepIndex)
        } ?: executeNonOperationStep(step, context, stepStartTime)
    }

    /**
     * Execute a step that has no operationId.
     *
     * Checks in order:
     * 1. Custom step definition match
     * 2. Assertions/extractions against last response
     * 3. No-op (just pass)
     */
    private fun executeNonOperationStep(
        step: Step,
        context: ExecutionContext,
        stepStartTime: Instant,
    ): StepResult {
        // First, check if this is a custom step
        stepRegistry?.let { registry ->
            val resolvedDescription = resolveVariables(step.description, context)
            val match = registry.findMatch(resolvedDescription)
            if (match != null) {
                return executeCustomStep(step, match, context, stepStartTime)
            }
        }

        // Not a custom step - check for assertions/extractions
        return if (step.assertions.isEmpty() && step.extractions.isEmpty()) {
            // No operation and no assertions - just pass
            StepResult(
                step = step,
                status = ResultStatus.PASSED,
                duration = Duration.between(stepStartTime, Instant.now()),
            )
        } else {
            // Run assertions/extractions against last response
            context.lastResponse?.let { response ->
                buildResultFromResponse(step, response, stepStartTime, context)
            } ?: StepResult(
                step = step,
                status = ResultStatus.ERROR,
                duration = Duration.between(stepStartTime, Instant.now()),
                error = IllegalStateException("No previous response to run assertions/extractions against"),
            )
        }
    }

    /**
     * Execute a custom step definition.
     */
    private fun executeCustomStep(
        step: Step,
        match: StepMatch,
        context: ExecutionContext,
        stepStartTime: Instant,
    ): StepResult =
        runCatching {
            val stepContext =
                StepContextImpl(
                    executionContext = context,
                    configuration = configuration,
                    sharedVariables = context.allVariables().toMutableMap(),
                    sharingEnabled = configuration.shareVariablesAcrossScenarios,
                )

            // Invoke the custom step method with extracted parameters and context
            val method = match.definition.method
            val parameters = match.parameters.toTypedArray()

            // Check if method accepts StepContext as last parameter
            val methodParams = method.parameters
            val args =
                if (methodParams.isNotEmpty() &&
                    methodParams.last().type.isAssignableFrom(StepContext::class.java)
                ) {
                    // Append StepContext to parameters
                    arrayOf(*parameters, stepContext)
                } else {
                    parameters
                }

            // Invoke the method
            val result = method.invoke(match.definition.instance, *args)

            // Check if the method returned a StepResult
            if (result is StepResult) {
                // Ensure custom step flag is set
                result.copy(isCustomStep = true)
            } else {
                StepResult(
                    step = step,
                    status = ResultStatus.PASSED,
                    duration = Duration.between(stepStartTime, Instant.now()),
                    isCustomStep = true,
                )
            }
        }.getOrElse { e ->
            // Unwrap InvocationTargetException to get the actual exception
            val actualException =
                when (e) {
                    is java.lang.reflect.InvocationTargetException -> e.cause ?: e
                    else -> e
                }

            // Determine status based on exception type
            val status =
                when (actualException) {
                    is AssertionError -> ResultStatus.FAILED
                    else -> ResultStatus.ERROR
                }

            StepResult(
                step = step,
                status = status,
                duration = Duration.between(stepStartTime, Instant.now()),
                error = actualException as? Exception ?: RuntimeException(actualException),
                isCustomStep = true,
            )
        }

    /**
     * Resolve variables in a string.
     */
    private fun resolveVariables(
        text: String,
        context: ExecutionContext,
    ): String {
        val regex = """\{\{(\w+)}}""".toRegex()
        return regex.replace(text) { matchResult ->
            val varName = matchResult.groupValues[1]
            context.get<Any>(varName)?.toString() ?: matchResult.value
        }
    }

    /**
     * Execute a step with an operationId (HTTP request).
     */
    private fun executeOperationStep(
        step: Step,
        context: ExecutionContext,
        stepStartTime: Instant,
        scenarioContext: ScenarioContextAdapter,
        stepIndex: Int,
    ): StepResult =
        runCatching {
            // Check if this step has auto-test configuration
            val autoTestConfig = step.autoTestConfig
            if (autoTestConfig != null) {
                executeAutoTestStep(step, context, stepStartTime, autoTestConfig)
            } else {
                executeHttpRequest(step, context, stepStartTime)
            }
        }.getOrElse { e ->
            // Create enriched error context for debugging
            val errorContext = buildScenarioErrorContext(scenarioContext, step, stepIndex)
            val enrichedError = enrichException(e, errorContext, context)

            StepResult(
                step = step,
                status = ResultStatus.ERROR,
                duration = Duration.between(stepStartTime, Instant.now()),
                error = enrichedError,
            )
        }

    /**
     * Build scenario error context from current execution state.
     */
    private fun buildScenarioErrorContext(
        scenarioContext: ScenarioContextAdapter,
        step: Step,
        stepIndex: Int,
    ): ScenarioErrorContext =
        ScenarioErrorContext(
            scenarioName = scenarioContext.scenarioName,
            scenarioFile = scenarioContext.scenarioFile.toString(),
            stepDescription = step.description,
            stepIndex = stepIndex,
            stepLine = step.sourceLocation?.line,
            operationId = step.operationId,
        )

    /**
     * Enrich an exception with scenario/step context for better debugging.
     */
    private fun enrichException(
        original: Throwable,
        errorContext: ScenarioErrorContext,
        executionContext: ExecutionContext,
    ): Exception {
        // For HTTP-related exceptions, wrap with full context
        val exception = original as? Exception ?: RuntimeException(original)

        // If this is already an HttpExecutionException with context, return as-is
        if (exception is HttpExecutionException && exception.scenarioContext != null) {
            return exception
        }

        // Create response snapshot from context
        val lastResponse = executionContext.lastResponse
        val responseSnapshot =
            lastResponse?.let { resp ->
                org.berrycrush.plugin.HttpResponse(
                    statusCode = resp.statusCode(),
                    statusMessage = "",
                    headers = resp.headers().map(),
                    body = resp.body(),
                    duration = Duration.ofMillis(executionContext.lastResponseTimeMs ?: 0L),
                    timestamp = Instant.now(),
                )
            }

        // Return enhanced exception with context
        return HttpExecutionException(
            url = lastResponse?.uri()?.toString() ?: "unknown",
            method = lastResponse?.request()?.method() ?: "UNKNOWN",
            cause = exception,
            response = responseSnapshot,
            scenarioContext = errorContext,
            config = configuration.errorContextConfig,
        )
    }

    /**
     * Execute auto-test step with configured test types.
     */
    private fun executeAutoTestStep(
        step: Step,
        context: ExecutionContext,
        stepStartTime: Instant,
        autoTestConfig: AutoTestConfig,
    ): StepResult {
        val listener = currentExecutionListener.get() ?: BerryCrushExecutionListener.NOOP
        val hasMulti = AutoTestType.MULTI in autoTestConfig.types
        val hasInvalidOrSecurity =
            autoTestConfig.types.any {
                it == AutoTestType.INVALID || it == AutoTestType.SECURITY
            }

        // Extract step-level multi-test parameters from pathParams
        val stepMultiTestParams = step.pathParams.filterKeys { it.startsWith("multiTest") }

        // Merge configuration defaults -> context params -> step params (step wins)
        val multiTestParams =
            configuration.getMultiTestParameters() +
                context.allVariables() +
                stepMultiTestParams

        return when {
            // Both MULTI and INVALID/SECURITY - run both
            hasMulti && hasInvalidOrSecurity -> {
                val multiResult =
                    autoTestExecutor.executeMultiTests(
                        step,
                        context,
                        stepStartTime,
                        multiTestParams,
                        listener,
                    )
                val autoResult =
                    autoTestExecutor.executeAutoTests(
                        step,
                        context,
                        stepStartTime,
                        listener,
                    )
                combineAutoTestResults(step, stepStartTime, multiResult, autoResult)
            }
            // Only MULTI
            hasMulti ->
                autoTestExecutor.executeMultiTests(
                    step,
                    context,
                    stepStartTime,
                    multiTestParams,
                    listener,
                )
            // Only INVALID/SECURITY
            else -> autoTestExecutor.executeAutoTests(step, context, stepStartTime, listener)
        }
    }

    /**
     * Combine multi-test and auto-test results into a single StepResult.
     */
    private fun combineAutoTestResults(
        step: Step,
        stepStartTime: Instant,
        multiResult: StepResult,
        autoResult: StepResult,
    ): StepResult {
        val passed =
            multiResult.status == ResultStatus.PASSED &&
                autoResult.status == ResultStatus.PASSED
        return StepResult(
            step = step,
            status = if (passed) ResultStatus.PASSED else ResultStatus.FAILED,
            duration = Duration.between(stepStartTime, Instant.now()),
            message = "${multiResult.message}; ${autoResult.message}",
            multiTestResults = multiResult.multiTestResults,
            autoTestResults = autoResult.autoTestResults,
        )
    }

    /**
     * Build request context and execute HTTP request.
     */
    private fun executeHttpRequest(
        step: Step,
        context: ExecutionContext,
        stepStartTime: Instant,
    ): StepResult {
        // Resolve the operation
        val (spec, resolvedOp) = specRegistry.resolve(step.operationId!!, step.specName)

        // Store the resolved operation for schema validation
        context.updateCurrentOperation(resolvedOp)

        // Record request start time
        val requestStartTime = System.currentTimeMillis()

        // Execute the HTTP request using the HttpExecutor
        val response = httpExecutor.execute(step, spec, resolvedOp, context)

        // Calculate and store response time
        val responseTimeMs = System.currentTimeMillis() - requestStartTime
        context.updateLastResponseTime(responseTimeMs)

        // Update context with response
        context.updateLastResponse(response)

        return buildResultFromResponse(step, response, stepStartTime, context)
    }

    /**
     * Check if a step contains any custom assertions.
     */
    private fun hasCustomAssertion(step: Step): Boolean {
        // Check direct assertions
        if (step.assertions.any { it.condition is Condition.CustomAssertion }) {
            return true
        }
        // Check conditional assertions
        return step.conditionals.any { conditional ->
            hasCustomAssertionInConditional(conditional)
        }
    }

    /**
     * Check if a conditional contains any custom assertions.
     */
    private fun hasCustomAssertionInConditional(conditional: ConditionalAssertion): Boolean {
        // Check if branch
        if (conditional.ifBranch.actions.assertions
                .any { it.condition is Condition.CustomAssertion }
        ) {
            return true
        }
        // Check else if branches
        if (conditional.elseIfBranches.any { branch ->
                branch.actions.assertions.any { it.condition is Condition.CustomAssertion }
            }
        ) {
            return true
        }
        // Check else actions
        if (conditional.elseActions?.assertions?.any { it.condition is Condition.CustomAssertion } == true) {
            return true
        }
        // Check nested conditionals
        val hasNestedCustom =
            conditional.ifBranch.actions.nestedConditionals
                .any { hasCustomAssertionInConditional(it) } ||
                conditional.elseIfBranches.any { branch ->
                    branch.actions.nestedConditionals.any { hasCustomAssertionInConditional(it) }
                } ||
                (conditional.elseActions?.nestedConditionals?.any { hasCustomAssertionInConditional(it) } == true)
        return hasNestedCustom
    }

    /**
     * Build a StepResult from an HTTP response.
     */
    private fun buildResultFromResponse(
        step: Step,
        response: HttpResponse<String>,
        stepStartTime: Instant,
        context: ExecutionContext,
    ): StepResult {
        val isCustom = hasCustomAssertion(step)

        // Check for unconditional fail
        if (step.failMessage != null) {
            return StepResult(
                step = step,
                status = ResultStatus.FAILED,
                statusCode = response.statusCode(),
                responseBody = response.body(),
                responseHeaders = response.headers().map(),
                duration = Duration.between(stepStartTime, Instant.now()),
                error = AssertionError(step.failMessage),
                isCustomStep = isCustom,
            )
        }

        val extractedValues = extractValues(response, step, context)
        val assertionResults = runAssertions(response, step.assertions, context).toMutableList()

        // Run conditional assertions
        val conditionalResults = runConditionals(response, step.conditionals, context)
        assertionResults.addAll(conditionalResults.assertionResults)

        // Run custom assertions (DSL assert blocks)
        val customAssertionResults = runCustomAssertions(response, step.customAssertions, context)
        assertionResults.addAll(customAssertionResults)

        // Check for conditional fail
        if (conditionalResults.failMessage != null) {
            return StepResult(
                step = step,
                status = ResultStatus.FAILED,
                statusCode = response.statusCode(),
                responseBody = response.body(),
                responseHeaders = response.headers().map(),
                duration = Duration.between(stepStartTime, Instant.now()),
                extractedValues = extractedValues + conditionalResults.extractedValues,
                assertionResults = assertionResults,
                error = AssertionError(conditionalResults.failMessage),
                isCustomStep = isCustom,
            )
        }

        val allPassed = assertionResults.all { it.passed }

        return StepResult(
            step = step,
            status = if (allPassed) ResultStatus.PASSED else ResultStatus.FAILED,
            statusCode = response.statusCode(),
            responseBody = response.body(),
            responseHeaders = response.headers().map(),
            duration = Duration.between(stepStartTime, Instant.now()),
            extractedValues = extractedValues + conditionalResults.extractedValues,
            assertionResults = assertionResults,
            isCustomStep = isCustom,
        )
    }

    /**
     * Log HTTP request if enabled.
     */
    private fun logRequest(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ) {
        if (configuration.logRequests) {
            configuration.getEffectiveHttpLogger().logRequest(method, url, headers, body)
        }
    }

    /**
     * Log HTTP response if enabled.
     */
    private fun logResponse(
        method: HttpMethod,
        url: String,
        response: HttpResponse<String>,
        requestStartTime: Long,
    ) {
        if (configuration.logResponses) {
            val durationMs = System.currentTimeMillis() - requestStartTime
            configuration.getEffectiveHttpLogger().logResponse(method, url, response, durationMs)
        }
    }

    private fun resolveParams(
        params: Map<String, Any>,
        context: ExecutionContext,
    ): Map<String, Any> =
        params.mapValues { (_, value) ->
            when (value) {
                is String -> context.interpolate(value)
                else -> value
            }
        }

    private fun extractValues(
        response: HttpResponse<String>,
        step: Step,
        context: ExecutionContext,
    ): Map<String, Any?> =
        step.extractions.associate { extraction ->
            val value =
                runCatching {
                    val body = response.body() ?: ""
                    JsonPath.read<Any>(body, extraction.jsonPath).also {
                        context[extraction.variableName] = it
                    }
                }.getOrNull()
            extraction.variableName to value
        }

    private fun runAssertions(
        response: HttpResponse<String>,
        assertions: List<Assertion>,
        context: ExecutionContext,
    ): List<AssertionResult> = assertions.map { assertion -> runAssertion(response, assertion, context) }

    /**
     * Run a single assertion using the AssertionEngine.
     *
     * Delegates condition evaluation and message generation to the assertion engine,
     * ensuring consistent behavior between `assert` and `if` conditions.
     */
    private fun runAssertion(
        response: HttpResponse<String>,
        assertion: Assertion,
        context: ExecutionContext,
    ): AssertionResult {
        val assertionContext = buildAssertionContext(response, context)
        val result = assertionEngine.evaluate(assertion.condition, assertionContext)

        return AssertionResult(
            assertion = assertion,
            passed = result.passed,
            message = result.message,
            actual = result.actual,
        )
    }

    /**
     * Build an AssertionContext from the current execution state.
     */
    private fun buildAssertionContext(
        response: HttpResponse<String>,
        context: ExecutionContext,
    ): ExecutorAssertionContext {
        val headers = response.headers().map().mapValues { it.value.toList() }
        return ExecutorAssertionContext(
            response = response,
            responseBody = response.body(),
            responseHeaders = headers,
            statusCode = response.statusCode(),
            responseTimeMs = context.lastResponseTimeMs,
            variables = context.allVariables(),
            executionContext = context,
            currentOperation = context.currentOperation,
        )
    }

    /**
     * Result of running conditional assertions.
     */
    private data class ConditionalRunResult(
        val assertionResults: List<AssertionResult> = emptyList(),
        val extractedValues: Map<String, Any?> = emptyMap(),
        val failMessage: String? = null,
    )

    /**
     * Run conditional assertions against the response.
     *
     * Evaluates each conditional's conditions in order and runs the actions
     * for the first matching branch.
     */
    private fun runConditionals(
        response: HttpResponse<String>,
        conditionals: List<ConditionalAssertion>,
        context: ExecutionContext,
    ): ConditionalRunResult {
        val allResults = mutableListOf<AssertionResult>()
        val allExtracted = mutableMapOf<String, Any?>()
        var failMessage: String? = null

        for (conditional in conditionals) {
            val result = runConditional(response, conditional, context)
            allResults.addAll(result.assertionResults)
            allExtracted.putAll(result.extractedValues)
            if (result.failMessage != null) {
                failMessage = result.failMessage
                break // Stop on first fail
            }
        }

        return ConditionalRunResult(
            assertionResults = allResults,
            extractedValues = allExtracted,
            failMessage = failMessage,
        )
    }

    /**
     * Run a single conditional assertion.
     */
    private fun runConditional(
        response: HttpResponse<String>,
        conditional: ConditionalAssertion,
        context: ExecutionContext,
    ): ConditionalRunResult {
        val assertionContext = buildAssertionContext(response, context)

        // Try if branch
        if (assertionEngine.evaluate(conditional.ifBranch.condition, assertionContext).passed) {
            return runConditionalActions(response, conditional.ifBranch.actions, context)
        }

        // Try else-if branches
        for (elseIfBranch in conditional.elseIfBranches) {
            if (assertionEngine.evaluate(elseIfBranch.condition, assertionContext).passed) {
                return runConditionalActions(response, elseIfBranch.actions, context)
            }
        }

        // Run else branch if present
        if (conditional.elseActions != null) {
            return runConditionalActions(response, conditional.elseActions, context)
        }

        // No branch matched - that's OK, no assertions to run
        return ConditionalRunResult()
    }

    /**
     * Run actions within a conditional branch.
     */
    private fun runConditionalActions(
        response: HttpResponse<String>,
        actions: ConditionalActions,
        context: ExecutionContext,
    ): ConditionalRunResult {
        // Check for fail first
        if (actions.failMessage != null) {
            return ConditionalRunResult(failMessage = actions.failMessage)
        }

        val assertionResults = mutableListOf<AssertionResult>()
        val extractedValues = mutableMapOf<String, Any?>()

        // Run extractions
        for (extraction in actions.extractions) {
            val value =
                runCatching {
                    val body = response.body() ?: ""
                    JsonPath.read<Any>(body, extraction.jsonPath).also {
                        context[extraction.variableName] = it
                    }
                }.getOrNull()
            extractedValues[extraction.variableName] = value
        }

        // Run assertions
        assertionResults.addAll(runAssertions(response, actions.assertions, context))

        // Run nested conditionals
        for (nested in actions.nestedConditionals) {
            val nestedResult = runConditional(response, nested, context)
            assertionResults.addAll(nestedResult.assertionResults)
            extractedValues.putAll(nestedResult.extractedValues)
            if (nestedResult.failMessage != null) {
                return ConditionalRunResult(
                    assertionResults = assertionResults,
                    extractedValues = extractedValues,
                    failMessage = nestedResult.failMessage,
                )
            }
        }

        return ConditionalRunResult(
            assertionResults = assertionResults,
            extractedValues = extractedValues,
        )
    }

    /**
     * Run custom assertions defined via DSL assert blocks.
     *
     * Custom assertions receive a TestExecutionContext and can throw any exception
     * (including AssertionError from require/check/assert) to indicate failure.
     */
    private fun runCustomAssertions(
        response: HttpResponse<String>,
        customAssertions: List<CustomAssertionDefinition>,
        context: ExecutionContext,
    ): List<AssertionResult> =
        customAssertions.map { customAssertion ->
            runCustomAssertion(response, customAssertion, context)
        }

    /**
     * Run a single custom assertion.
     */
    private fun runCustomAssertion(
        response: HttpResponse<String>,
        customAssertion: CustomAssertionDefinition,
        context: ExecutionContext,
    ): AssertionResult {
        val testContext = MutableTestExecutionContext(context)
        val assertion =
            Assertion(
                condition = Condition.CustomAssertion(customAssertion.description),
                description = customAssertion.description,
            )
        return runCatching {
            customAssertion.assertion(testContext)
            AssertionResult(
                assertion = assertion,
                passed = true,
                message = "Custom assertion passed: ${customAssertion.description}",
            )
        }.getOrElse { e ->
            // Unwrap AssertionFailureException if present
            val actualException =
                when (e) {
                    is org.berrycrush.exception.AssertionFailureException -> e.cause ?: e
                    else -> e
                }
            AssertionResult(
                assertion = assertion,
                passed = false,
                message = actualException.message ?: "Custom assertion failed: ${customAssertion.description}",
                actual = actualException.message,
            )
        }
    }
}
