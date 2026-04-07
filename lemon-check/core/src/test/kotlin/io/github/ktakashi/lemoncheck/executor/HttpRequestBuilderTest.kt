package io.github.ktakashi.lemoncheck.executor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpRequestBuilderTest {
    private val builder = HttpRequestBuilder()

    @Test
    fun `should build simple URL without parameters`() {
        val url =
            builder.buildUrl(
                baseUrl = "https://api.example.com",
                path = "/pets",
            )

        assertEquals("https://api.example.com/pets", url)
    }

    @Test
    fun `should substitute path parameters`() {
        val url =
            builder.buildUrl(
                baseUrl = "https://api.example.com",
                path = "/pets/{petId}",
                pathParams = mapOf("petId" to 123),
            )

        assertEquals("https://api.example.com/pets/123", url)
    }

    @Test
    fun `should add query parameters`() {
        val url =
            builder.buildUrl(
                baseUrl = "https://api.example.com",
                path = "/pets",
                queryParams = mapOf("limit" to 10, "status" to "available"),
            )

        assertTrue(url.startsWith("https://api.example.com/pets?"))
        assertTrue(url.contains("limit=10"))
        assertTrue(url.contains("status=available"))
    }

    @Test
    fun `should handle multiple path parameters`() {
        val url =
            builder.buildUrl(
                baseUrl = "https://api.example.com",
                path = "/users/{userId}/pets/{petId}",
                pathParams = mapOf("userId" to "user1", "petId" to 42),
            )

        assertEquals("https://api.example.com/users/user1/pets/42", url)
    }

    @Test
    fun `should handle trailing slash on base URL`() {
        val url =
            builder.buildUrl(
                baseUrl = "https://api.example.com/",
                path = "/pets",
            )

        assertEquals("https://api.example.com/pets", url)
    }

    @Test
    fun `should handle leading slash on path`() {
        val url =
            builder.buildUrl(
                baseUrl = "https://api.example.com",
                path = "pets",
            )

        assertEquals("https://api.example.com/pets", url)
    }

    @Test
    fun `should encode special characters in query params`() {
        val url =
            builder.buildUrl(
                baseUrl = "https://api.example.com",
                path = "/search",
                queryParams = mapOf("q" to "hello world", "filter" to "a&b"),
            )

        assertTrue(url.contains("q=hello+world") || url.contains("q=hello%20world"))
        assertTrue(url.contains("filter=a%26b"))
    }

    @Test
    fun `should combine path and query parameters`() {
        val url =
            builder.buildUrl(
                baseUrl = "https://api.example.com",
                path = "/pets/{petId}/orders",
                pathParams = mapOf("petId" to 123),
                queryParams = mapOf("status" to "pending"),
            )

        assertEquals("https://api.example.com/pets/123/orders?status=pending", url)
    }
}
