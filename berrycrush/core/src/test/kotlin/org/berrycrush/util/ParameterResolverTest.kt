package org.berrycrush.util

import org.berrycrush.context.ExecutionContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParameterResolverTest {
    @Test
    fun `should resolve environment variable`() {
        val resolver =
            ParameterResolver(
                environmentProvider = { name ->
                    when (name) {
                        "API_KEY" -> "secret123"
                        else -> null
                    }
                },
            )

        val result = resolver.resolve("\${env.API_KEY}")
        assertEquals("secret123", result)
    }

    @Test
    fun `should resolve context variable`() {
        val context = ExecutionContext()
        context["userId"] = "user123"

        val resolver = ParameterResolver(context = context)

        assertEquals("user123", resolver.resolve("\${context.userId}"))
        assertEquals("user123", resolver.resolve("\${userId}"))
    }

    @Test
    fun `should resolve parameter reference`() {
        val params = mapOf("baseUrl" to "https://api.example.com")
        val resolver = ParameterResolver(parameters = params)

        val result = resolver.resolve("\${param.baseUrl}")
        assertEquals("https://api.example.com", result)
    }

    @Test
    fun `should resolve multiple variables in string`() {
        val resolver =
            ParameterResolver(
                environmentProvider = { name ->
                    when (name) {
                        "HOST" -> "api.example.com"
                        "VERSION" -> "v2"
                        else -> null
                    }
                },
            )

        val result = resolver.resolve("https://\${env.HOST}/\${env.VERSION}/users")
        assertEquals("https://api.example.com/v2/users", result)
    }

    @Test
    fun `should leave unresolved variables as-is`() {
        val resolver = ParameterResolver()

        val result = resolver.resolve("Value: \${unknown.variable}")
        assertEquals("Value: \${unknown.variable}", result)
    }

    @Test
    fun `should resolve nested parameter references`() {
        val params =
            mapOf(
                "host" to "\${env.HOST}",
                "url" to "https://\${param.host}/api",
            )

        val resolver =
            ParameterResolver(
                parameters = params,
                environmentProvider = { name ->
                    when (name) {
                        "HOST" -> "api.example.com"
                        else -> null
                    }
                },
            )

        assertEquals("api.example.com", resolver.resolve("\${param.host}"))
        assertEquals("https://api.example.com/api", resolver.resolve("\${param.url}"))
    }

    @Test
    fun `should prevent infinite recursion`() {
        val params =
            mapOf(
                "a" to "\${param.b}",
                "b" to "\${param.a}",
            )

        val resolver = ParameterResolver(parameters = params)

        // Should not hang - returns an unresolved reference after max depth
        val result = resolver.resolve("\${param.a}")
        // After MAX_RECURSION_DEPTH, it returns whatever is unresolved at that point
        // It will be one of the two references, depending on the depth
        assertTrue(
            result == "\${param.a}" || result == "\${param.b}",
            "Expected an unresolved param reference, got: $result",
        )
    }

    @Test
    fun `should resolve all values in map`() {
        val resolver =
            ParameterResolver(
                environmentProvider = { name ->
                    when (name) {
                        "API_KEY" -> "key123"
                        else -> null
                    }
                },
            )

        val params =
            mapOf(
                "apiKey" to "\${env.API_KEY}",
                "timeout" to 60,
                "description" to "Key is \${env.API_KEY}",
            )

        val resolved = resolver.resolveAll(params)

        assertEquals("key123", resolved["apiKey"])
        assertEquals(60, resolved["timeout"])
        assertEquals("Key is key123", resolved["description"])
    }

    @Test
    fun `should resolve values in nested maps`() {
        val resolver =
            ParameterResolver(
                environmentProvider = { name ->
                    when (name) {
                        "HOST" -> "localhost"
                        else -> null
                    }
                },
            )

        val params =
            mapOf(
                "server" to
                    mapOf(
                        "host" to "\${env.HOST}",
                        "port" to 8080,
                    ),
            )

        val resolved = resolver.resolveAll(params)

        @Suppress("UNCHECKED_CAST")
        val server = resolved["server"] as Map<String, Any>
        assertEquals("localhost", server["host"])
        assertEquals(8080, server["port"])
    }

    @Test
    fun `should resolve values in lists`() {
        val resolver =
            ParameterResolver(
                environmentProvider = { name ->
                    when (name) {
                        "ENV" -> "prod"
                        else -> null
                    }
                },
            )

        val params =
            mapOf(
                "tags" to listOf("app", "\${env.ENV}", "api"),
            )

        val resolved = resolver.resolveAll(params)

        @Suppress("UNCHECKED_CAST")
        val tags = resolved["tags"] as List<String>
        assertEquals(listOf("app", "prod", "api"), tags)
    }

    @Test
    fun `should handle empty parameters`() {
        val resolver = ParameterResolver()

        val resolved = resolver.resolveAll(emptyMap())
        assertEquals(emptyMap(), resolved)
    }

    @Test
    fun `should handle string with no variables`() {
        val resolver = ParameterResolver()

        val result = resolver.resolve("Just a plain string")
        assertEquals("Just a plain string", result)
    }
}
