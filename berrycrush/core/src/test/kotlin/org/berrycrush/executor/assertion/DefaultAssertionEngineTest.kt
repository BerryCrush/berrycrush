package org.berrycrush.executor.assertion

import org.berrycrush.config.BerryCrushConfiguration
import org.berrycrush.model.Condition
import org.berrycrush.model.ConditionOperator
import org.berrycrush.plugin.HttpResponse
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for DefaultAssertionEngine.
 *
 * Note: Status and Header conditions require a real HttpResponse object,
 * so we focus on testing JsonPath, Variable, and ResponseTime conditions
 * which work with just the context data.
 */
class DefaultAssertionEngineTest {
    private val configuration = BerryCrushConfiguration()
    private val engine = DefaultAssertionEngine()

    // --- JsonPath Condition Tests ---

    @Test
    fun `evaluate jsonpath condition - string value matches`() {
        val responseBody = """{"name": "Fluffy"}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.name",
                operator = ConditionOperator.EQUALS,
                expected = "Fluffy",
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "JsonPath $.name should equal 'Fluffy'")
    }

    @Test
    fun `evaluate jsonpath condition - number value matches`() {
        val responseBody = """{"age": 5}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.age",
                operator = ConditionOperator.EQUALS,
                expected = 5,
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "JsonPath $.age should equal 5")
    }

    @Test
    fun `evaluate jsonpath condition - greater than operator`() {
        val responseBody = """{"count": 10}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.count",
                operator = ConditionOperator.GREATER_THAN,
                expected = 5,
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "$.count (10) should be greater than 5")
    }

    @Test
    fun `evaluate jsonpath condition - less than operator fails`() {
        val responseBody = """{"count": 10}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.count",
                operator = ConditionOperator.LESS_THAN,
                expected = 5,
            )

        val result = engine.evaluate(condition, context)

        assertFalse(result.passed, "$.count (10) should not be less than 5")
    }

    @Test
    fun `evaluate jsonpath condition - contains operator`() {
        val responseBody = """{"tags": ["cat", "pet", "fluffy"]}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.tags",
                operator = ConditionOperator.CONTAINS,
                expected = "pet",
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "$.tags should contain 'pet'")
    }

    @Test
    fun `evaluate jsonpath condition - exists operator`() {
        val responseBody = """{"name": "Fluffy", "age": 5}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.name",
                operator = ConditionOperator.EXISTS,
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "$.name should exist")
    }

    @Test
    fun `evaluate jsonpath condition - not_exists operator`() {
        val responseBody = """{"name": "Fluffy"}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.missing",
                operator = ConditionOperator.NOT_EXISTS,
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "$.missing should not exist")
    }

    @Test
    fun `evaluate jsonpath condition - not equals operator`() {
        val responseBody = """{"name": "Max"}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.name",
                operator = ConditionOperator.NOT_EQUALS,
                expected = "Fluffy",
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "$.name 'Max' should not equal 'Fluffy'")
    }

    // --- Variable Condition Tests ---

    @Test
    fun `evaluate variable condition - equals operator passes`() {
        val variables = mapOf("userId" to "user123")
        val context = createContext(variables = variables)
        val condition =
            Condition.Variable(
                name = "userId",
                operator = ConditionOperator.EQUALS,
                expected = "user123",
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "Variable userId should equal 'user123'")
    }

    @Test
    fun `evaluate variable condition - missing variable fails`() {
        val context = createContext(variables = emptyMap())
        val condition =
            Condition.Variable(
                name = "missing",
                operator = ConditionOperator.EQUALS,
                expected = "value",
            )

        val result = engine.evaluate(condition, context)

        assertFalse(result.passed, "Missing variable should fail")
    }

    @Test
    fun `evaluate variable condition - numeric comparison`() {
        val variables = mapOf("count" to 10)
        val context = createContext(variables = variables)
        val condition =
            Condition.Variable(
                name = "count",
                operator = ConditionOperator.GREATER_THAN,
                expected = 5,
            )

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "Variable count (10) should be > 5")
    }

    // --- Response Time Condition Tests ---

    @Test
    fun `evaluate response time condition - within limit passes`() {
        val context = createContext(responseTimeMs = 100L)
        val condition = Condition.ResponseTime(maxMs = 500)

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "Response time 100ms should be less than 500ms")
    }

    @Test
    fun `evaluate response time condition - exceeds limit fails`() {
        val context = createContext(responseTimeMs = 1000L)
        val condition = Condition.ResponseTime(maxMs = 500)

        val result = engine.evaluate(condition, context)

        assertFalse(result.passed, "Response time 1000ms should not be less than 500ms")
    }

    // --- Body Contains Condition Tests ---

    @Test
    fun `evaluate body contains condition - text found passes`() {
        val responseBody = """{"message": "Hello, World!"}"""
        val context = createContext(responseBody = responseBody)
        val condition = Condition.BodyContains(text = "World")

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "Body should contain 'World'")
    }

    @Test
    fun `evaluate body contains condition - text not found fails`() {
        val responseBody = """{"message": "Hello"}"""
        val context = createContext(responseBody = responseBody)
        val condition = Condition.BodyContains(text = "World")

        val result = engine.evaluate(condition, context)

        assertFalse(result.passed, "Body should not contain 'World'")
    }

    // --- Negated Condition Tests ---

    @Test
    fun `evaluate negated condition - negates passing condition`() {
        val responseBody = """{"name": "Max"}"""
        val context = createContext(responseBody = responseBody)
        val innerCondition =
            Condition.JsonPath(
                path = "$.name",
                operator = ConditionOperator.EQUALS,
                expected = "Fluffy",
            )
        val condition = Condition.Negated(innerCondition)

        val result = engine.evaluate(condition, context)

        assertTrue(result.passed, "NOT ($.name == Fluffy) should pass when name is Max")
    }

    // --- Message Generation Tests ---

    @Test
    fun `generateMessage - jsonpath condition failed`() {
        val responseBody = """{"name": "Max"}"""
        val context = createContext(responseBody = responseBody)
        val condition =
            Condition.JsonPath(
                path = "$.name",
                operator = ConditionOperator.EQUALS,
                expected = "Fluffy",
            )

        val message = engine.generateMessage(condition, passed = false, context)

        assertTrue(message.contains("$.name"), "Message should mention the path")
    }

    @Test
    fun `generateMessage - variable condition`() {
        val variables = mapOf("userId" to "user123")
        val context = createContext(variables = variables)
        val condition =
            Condition.Variable(
                name = "userId",
                operator = ConditionOperator.EQUALS,
                expected = "user123",
            )

        val message = engine.generateMessage(condition, passed = true, context)

        assertTrue(message.contains("userId"), "Message should mention the variable")
    }

    // --- Helper Functions ---

    private fun createContext(
        statusCode: Int = 200,
        responseBody: String = "",
        responseHeaders: Map<String, List<String>> = emptyMap(),
        responseTimeMs: Long = 0L,
        variables: Map<String, Any> = emptyMap(),
    ): AssertionContext {
        val executionContext = org.berrycrush.context.ExecutionContext()
        variables.forEach { (k, v) -> executionContext[k] = v }
        val response =
            mock<HttpResponse> {
                on { statusCode } doReturn statusCode
                on { body } doReturn responseBody
                on { headers } doReturn responseHeaders
            }
        return AssertionContext(
            response = response,
            responseTime = Duration.ofMillis(responseTimeMs),
            variables = variables,
            executionContext = executionContext,
        )
    }
}
