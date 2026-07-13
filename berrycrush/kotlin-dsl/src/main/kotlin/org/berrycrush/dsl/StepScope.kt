package org.berrycrush.dsl

import org.berrycrush.context.ExecutionContext
import org.berrycrush.context.TestExecutionContext
import org.berrycrush.junit.BerryCrushSuite
import org.berrycrush.model.Assertion
import org.berrycrush.model.AutoTestConfig
import org.berrycrush.model.Condition
import org.berrycrush.model.ConditionOperator
import org.berrycrush.model.Extraction
import org.berrycrush.model.Step
import org.berrycrush.model.StepType
import java.time.Duration

/**
 * DSL scope for defining a single step.
 */
@BerryCrushDsl
@Suppress("TooManyFunctions") // DSL class designed to provide many step configuration functions
class StepScope internal constructor(
    private val type: StepType,
    private val description: String,
    @Suppress("UnusedPrivateProperty") // Reserved for future DSL enhancements
    private val suite: BerryCrushSuite,
) {
    private var operationId: String? = null
    private var specName: String? = null
    private val pathParams = mutableMapOf<String, Any>()
    private val queryParams = mutableMapOf<String, Any>()
    private val headers = mutableMapOf<String, String>()
    private var body: String? = null
    private val extractions = mutableListOf<Extraction>()
    private val assertions = mutableListOf<Assertion>()
    private var autoAssert: Boolean = true
    private var autoTestConfig: AutoTestConfig? = null

    /**
     * Access to the execution context for variable substitution.
     */
    val context: ExecutionContext
        get() = ExecutionContext() // Placeholder - real context provided at runtime

    /**
     * Switch to a specific OpenAPI spec (for multi-spec scenarios).
     */
    fun using(specName: String) {
        this.specName = specName
    }

    /**
     * Call an API operation.
     */
    fun call(
        operationId: String,
        block: CallScope.() -> Unit = {},
    ) {
        this.operationId = operationId
        val callScope = CallScope()
        block(callScope)
        pathParams.putAll(callScope.pathParams)
        queryParams.putAll(callScope.queryParams)
        headers.putAll(callScope.headers)
        body = callScope.body
        if (!callScope.autoAssert) {
            autoAssert = false
        }
        autoTestConfig = callScope.autoTestConfig
    }

    /**
     * Extract a value from the response.
     */
    fun extractTo(
        variableName: String,
        jsonPath: String,
    ) {
        extractions.add(Extraction(variableName, jsonPath))
    }

    // ========== Assertions ==========

    /**
     * Assert exact status code.
     */
    fun statusCode(expected: Int) {
        assertions.add(Condition.Status(expected).toAssertion("status $expected"))
    }

    /**
     * Assert status code in range.
     */
    fun statusCode(range: IntRange) {
        assertions.add(Condition.Status(range).toAssertion("status ${range.first}-${range.last}"))
    }

    /**
     * Assert response body contains a string.
     */
    fun bodyContains(substring: String) {
        assertions.add(Condition.BodyContains(substring).toAssertion("body contains \"$substring\""))
    }

    /**
     * Assert JSONPath value equals expected.
     */
    fun bodyEquals(
        jsonPath: String,
        expected: Any,
    ) {
        val condition =
            Condition.JsonPath(
                path = jsonPath,
                operator = ConditionOperator.EQUALS,
                expected = expected,
            )
        assertions.add(condition.toAssertion("$jsonPath equals $expected"))
    }

    /**
     * Assert JSONPath value matches regex.
     */
    fun bodyMatches(
        jsonPath: String,
        pattern: String,
    ) {
        val condition =
            Condition.JsonPath(
                path = jsonPath,
                operator = ConditionOperator.MATCHES,
                expected = pattern,
            )
        assertions.add(condition.toAssertion("$jsonPath matches $pattern"))
    }

    /**
     * Assert body array has expected size.
     */
    fun bodyArraySize(
        jsonPath: String,
        expected: Int,
    ) {
        val condition =
            Condition.JsonPath(
                path = jsonPath,
                operator = ConditionOperator.HAS_SIZE,
                expected = expected,
            )
        assertions.add(condition.toAssertion("$jsonPath hasSize $expected"))
    }

    /**
     * Assert body array is not empty.
     */
    fun bodyArrayNotEmpty(jsonPath: String) {
        val condition =
            Condition.JsonPath(
                path = jsonPath,
                operator = ConditionOperator.NOT_EMPTY,
                expected = null,
            )
        assertions.add(condition.toAssertion("$jsonPath notEmpty"))
    }

    /**
     * Assert header exists.
     */
    fun headerExists(name: String) {
        val condition =
            Condition.Header(
                name = name,
                operator = ConditionOperator.EXISTS,
                expected = null,
            )
        assertions.add(condition.toAssertion("header $name exists"))
    }

    /**
     * Assert header equals expected value.
     */
    fun headerEquals(
        name: String,
        expected: String,
    ) {
        val condition =
            Condition.Header(
                name = name,
                operator = ConditionOperator.EQUALS,
                expected = expected,
            )
        assertions.add(condition.toAssertion("header $name equals \"$expected\""))
    }

    /**
     * Assert response matches OpenAPI schema.
     */
    fun matchesSchema() {
        assertions.add(Condition.Schema.toAssertion("matches schema"))
    }

    /**
     * Assert response time is under threshold (milliseconds).
     */
    fun responseTime(maxMillis: Long) {
        assertions.add(Condition.ResponseTime(maxMillis).toAssertion("responseTime < $maxMillis ms"))
    }

    /**
     * Assert response time in under threshold
     */
    fun responseTime(duration: Duration) {
        assertions.add(Condition.ResponseTime(duration).toAssertion("responseTime < $duration"))
    }

    // ========== Custom Assertions ==========

    /**
     * Define a custom assertion with programmatic logic.
     *
     * The assertion callback receives the [TestExecutionContext] which provides
     * access to the current response, request history, variables, and the ability
     * to store extracted values for subsequent steps.
     *
     * Any exception thrown within the callback is treated as an assertion failure,
     * including exceptions from Kotlin's `require()`, `check()`, and `assert()`.
     *
     * Example:
     * ```kotlin
     * afterwards("response is valid") {
     *     assert("user ID is positive") { ctx ->
     *         val userId = ctx.responseBody?.let { /* parse userId */ }
     *         require(userId != null && userId > 0) { "Invalid user ID" }
     *         ctx.extract("userId", userId)
     *     }
     * }
     * ```
     *
     * @param description Human-readable description of what this assertion checks
     * @param assertion Callback that performs the assertion logic
     */
    fun assert(
        description: String,
        assertion: (TestExecutionContext) -> Unit,
    ) {
        assertions.add(Assertion.CustomAssertion(description, assertion))
    }

    // ========== Conditional Logic ==========

    /**
     * Define conditional logic that executes different assertions based on runtime state.
     *
     * The predicate receives the [TestExecutionContext] and returns true/false to determine
     * which branch to execute.
     *
     * Use the `orElse` infix function to define the else branch:
     * ```kotlin
     * conditional({ ctx -> ctx.statusCode == 200 }) {
     *     bodyEquals("$.status", "success")
     * } orElse {
     *     bodyContains("error")
     * }
     * ```
     *
     * @param predicate Function that determines which branch to execute
     * @param block Assertions to run when predicate returns true
     * @return ConditionalBuilder to chain with orElse
     */
    fun conditional(
        predicate: (TestExecutionContext) -> Boolean,
        block: StepScope.() -> Unit,
    ): ConditionalBuilder {
        val builder = ConditionalBuilder(predicate, block, this)
        conditionalBuilders.add(builder)
        return builder
    }

    private val conditionalBuilders = mutableListOf<ConditionalBuilder>()

    internal fun build(): Step =
        Step(
            type = type,
            description = description,
            operationId = operationId,
            specName = specName,
            pathParams = pathParams.toMap(),
            queryParams = queryParams.toMap(),
            headers = headers.toMap(),
            body = body,
            extractions = extractions.toList(),
            assertions = assertions + conditionalBuilders.map { it.build() },
            autoAssert = autoAssert,
            autoTestConfig = autoTestConfig,
        )

    // Internal constructor for conditional scopes
    internal constructor(
        type: StepType,
        description: String,
        scope: StepScope,
        internal: Boolean,
    ) : this(type, description, scope.suite)
}
