package org.berrycrush.samples.petstore.provider

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.AutoTestType
import org.berrycrush.autotest.ParameterLocation
import org.berrycrush.autotest.provider.SecurityTestRequest
import org.berrycrush.autotest.provider.SecurityTestProvider

/**
 * Example custom security test provider for demonstrating extensibility in Kotlin.
 *
 * This provider tests for NoSQL injection attacks, which are common in
 * MongoDB and similar databases.
 *
 * This Kotlin implementation works alongside the Java-based EmojiTestProvider,
 * demonstrating that providers can be written in either language.
 */
class NoSqlInjectionProvider : SecurityTestProvider {
    override val testType: String = "NoSQLInjection"
    override val displayName: String = "NoSQL Injection"
    override val priority: Int = 100 // Higher than built-in providers

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.BODY, ParameterLocation.QUERY)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> {
        if (request.location !in applicableLocations()) {
            return emptyList()
        }

        return payloads().map { (name, payload) ->
            val body =
                if (request.location == ParameterLocation.BODY) {
                    request.baseBody.toMutableMap().apply { this[request.fieldName] = payload }
                } else {
                    request.baseBody
                }
            AutoTestCase(
                type = AutoTestType.SECURITY,
                testType = testType,
                fieldName = request.fieldName,
                invalidValue = payload,
                description = "$displayName: $name",
                location = request.location,
                body = body,
                pathParams = request.basePathParams,
                headers = request.baseHeaders,
                tag = "security - $displayName",
            )
        }
    }

    private fun payloads(): List<Pair<String, String>> =
        listOf(
            "MongoDB \$ne operator" to "{\"\$ne\": null}",
            "MongoDB \$gt operator" to "{\"\$gt\": \"\"}",
            "MongoDB \$where" to "{\"\$where\": \"sleep(5000)\"}",
            "MongoDB \$regex" to "{\"\$regex\": \".*\"}",
        )
}
