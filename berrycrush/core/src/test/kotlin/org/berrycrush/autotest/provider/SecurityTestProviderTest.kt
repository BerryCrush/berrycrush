package org.berrycrush.autotest.provider

import org.berrycrush.autotest.ParameterLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecurityTestProviderTest {
    @Test
    fun `default registry should have all built-in security providers including new ones`() {
        val registry = AutoTestProviderRegistry.default

        val expectedTypes =
            setOf(
                "SQLInjection",
                "XSS",
                "PathTraversal",
                "CommandInjection",
                "LDAPInjection",
                "XXE",
                "HeaderInjection",
                "NoSQLInjection",
                "SSTI",
                "JWT",
                "AuthorizationBypass",
            )

        expectedTypes.forEach { type ->
            assertTrue(
                registry.hasSecurityTestType(type),
                "Registry should have security test type: $type",
            )
        }
    }

    @Test
    fun `NoSqlInjectionProvider should generate MongoDB payloads`() {
        val provider = NoSqlInjectionProvider()

        assertEquals("NoSQLInjection", provider.testType)
        assertEquals("NoSQL Injection", provider.displayName)
        assertEquals(setOf(ParameterLocation.BODY, ParameterLocation.QUERY), provider.applicableLocations())

        val payloads = provider.generatePayloads()
        assertTrue(payloads.isNotEmpty(), "Should generate payloads")
        assertTrue(
            payloads.any { it.payload.contains("\$ne") },
            "Should include MongoDB \$ne payload",
        )
        assertTrue(
            payloads.any { it.payload.contains("\$where") },
            "Should include MongoDB \$where payload",
        )
    }

    @Test
    fun `SstiProvider should generate template injection payloads`() {
        val provider = SstiProvider()

        assertEquals("SSTI", provider.testType)
        assertEquals("Template Injection", provider.displayName)
        assertEquals(setOf(ParameterLocation.BODY, ParameterLocation.QUERY), provider.applicableLocations())

        val payloads = provider.generatePayloads()
        assertTrue(payloads.isNotEmpty(), "Should generate payloads")
        assertTrue(
            payloads.any { it.payload.contains("{{7*7}}") },
            "Should include Jinja2/Twig payload",
        )
        assertTrue(
            payloads.any { it.payload.contains("<%= 7*7 %>") },
            "Should include ERB payload",
        )
    }

    @Test
    fun `JwtAttackProvider should generate JWT attack payloads`() {
        val provider = JwtAttackProvider()

        assertEquals("JWT", provider.testType)
        assertEquals("JWT Attacks", provider.displayName)
        assertEquals(setOf(ParameterLocation.HEADER), provider.applicableLocations())

        val payloads = provider.generatePayloads()
        assertTrue(payloads.isNotEmpty(), "Should generate payloads")
        assertTrue(
            payloads.any { it.name.contains("Algorithm none") },
            "Should include algorithm none attack",
        )
        assertTrue(
            payloads.any { it.name.contains("Malformed") },
            "Should include malformed JWT payload",
        )
    }

    @Test
    fun `AuthorizationBypassProvider should generate auth bypass payloads`() {
        val provider = AuthorizationBypassProvider()

        assertEquals("AuthorizationBypass", provider.testType)
        assertEquals("Authorization Bypass", provider.displayName)
        assertEquals(setOf(ParameterLocation.HEADER), provider.applicableLocations())

        val payloads = provider.generatePayloads()
        assertTrue(payloads.isNotEmpty(), "Should generate payloads")
        assertTrue(
            payloads.any { it.payload.isEmpty() },
            "Should include empty auth payload",
        )
        assertTrue(
            payloads.any { it.payload == "Bearer " },
            "Should include empty bearer payload",
        )
    }

    @Test
    fun `all providers in DefaultSecurityTestProviders should be unique`() {
        val providers = DefaultSecurityTestProviders.all
        val testTypes = providers.map { it.testType }

        assertEquals(testTypes.distinct().size, testTypes.size, "All provider testTypes should be unique")
    }

    @Test
    fun `all providers should have non-empty payloads`() {
        DefaultSecurityTestProviders.all.forEach { provider ->
            val payloads = provider.generatePayloads()
            assertTrue(
                payloads.isNotEmpty(),
                "Provider ${provider.testType} should generate at least one payload",
            )
        }
    }

    @Test
    fun `all providers should have non-empty applicable locations`() {
        DefaultSecurityTestProviders.all.forEach { provider ->
            val locations = provider.applicableLocations()
            assertTrue(
                locations.isNotEmpty(),
                "Provider ${provider.testType} should have at least one applicable location",
            )
        }
    }
}
