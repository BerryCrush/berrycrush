package org.berrycrush.context

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExecutionContextTest {
    @Test
    fun `should store and retrieve variables`() {
        val context = ExecutionContext()

        context["name"] = "Rex"
        context["age"] = 5

        assertEquals("Rex", context.get<String>("name"))
        assertEquals(5, context.get<Int>("age"))
    }

    @Test
    fun `should return null for missing variables`() {
        val context = ExecutionContext()

        assertNull(context.get<String>("missing"))
    }

    @Test
    fun `should return default for missing variables`() {
        val context = ExecutionContext()

        assertEquals("default", context.getOrDefault("missing", "default"))
    }

    @Test
    fun `should check if variable exists`() {
        val context = ExecutionContext()

        context["exists"] = "value"

        assertTrue(context.contains("exists"))
        assertFalse(context.contains("missing"))
    }

    @Test
    fun `should list all variable names`() {
        val context = ExecutionContext()

        context["a"] = 1
        context["b"] = 2
        context["c"] = 3

        val names = context.variableNames()

        assertEquals(setOf("a", "b", "c"), names)
    }

    @Test
    fun `should interpolate simple variables`() {
        val context = ExecutionContext()

        context["name"] = "Rex"
        context["age"] = 5

        val result = context.interpolate($$"Pet $name is $age years old")

        assertEquals("Pet Rex is 5 years old", result)
    }

    @Test
    fun `should interpolate bracketed variables`() {
        val context = ExecutionContext()

        context["petName"] = "Rex"

        val result = context.interpolate($$"Pet ${petName} is cute")

        assertEquals("Pet Rex is cute", result)
    }

    @Test
    fun `should leave unknown variables as-is`() {
        val context = ExecutionContext()

        context["known"] = "value"

        val result = context.interpolate($$"$known and $unknown")

        assertEquals($$"value and $unknown", result)
    }

    @Test
    fun `should clear all variables`() {
        val context = ExecutionContext()

        context["a"] = 1
        context["b"] = 2

        context.clear()

        assertFalse(context.contains("a"))
        assertFalse(context.contains("b"))
        assertTrue(context.variableNames().isEmpty())
    }

    @Test
    fun `should create child context with inherited variables`() {
        val parent = ExecutionContext()
        parent["inherited"] = "value"

        val child = parent.createChild()

        assertEquals("value", child.get<String>("inherited"))

        // Modifying child should not affect parent
        child["inherited"] = "modified"
        assertEquals("value", parent.get<String>("inherited"))
    }

    @Test
    fun `should interpolate mustache-style variables`() {
        val context = ExecutionContext()
        context["petId"] = 123
        context["name"] = "Fluffy"

        val result = context.interpolate("Pet {{petId}} is named {{name}}")

        assertEquals("Pet 123 is named Fluffy", result)
    }

    @Test
    fun `should leave unknown mustache variables as-is`() {
        val context = ExecutionContext()
        context["known"] = "value"

        val result = context.interpolate("{{known}} and {{unknown}}")

        assertEquals("value and {{unknown}}", result)
    }

    @Test
    fun `should create isolated copy with all state`() {
        val original = ExecutionContext()
        original["variable"] = "originalValue"

        val copy = original.createIsolatedCopy()

        // Copy should have all variables
        assertEquals("originalValue", copy.get<String>("variable"))

        // Modifying copy should not affect original
        copy["variable"] = "modifiedValue"
        copy["newVariable"] = "newValue"

        assertEquals("originalValue", original.get<String>("variable"))
        assertFalse(original.contains("newVariable"))

        // Original modification should not affect copy
        original["variable"] = "remodified"
        assertEquals("modifiedValue", copy.get<String>("variable"))
    }

    @Test
    fun `isolated copies should be independent for parallel execution`() {
        val shared = ExecutionContext()
        shared["sharedVar"] = "initial"

        // Simulate parallel scenario execution
        val copy1 = shared.createIsolatedCopy()
        val copy2 = shared.createIsolatedCopy()

        // Each copy modifies independently
        copy1["sharedVar"] = "copy1Value"
        copy1["copy1Only"] = "exclusive1"

        copy2["sharedVar"] = "copy2Value"
        copy2["copy2Only"] = "exclusive2"

        // Verify isolation
        assertEquals("copy1Value", copy1.get<String>("sharedVar"))
        assertEquals("copy2Value", copy2.get<String>("sharedVar"))
        assertEquals("initial", shared.get<String>("sharedVar"))

        assertTrue(copy1.contains("copy1Only"))
        assertFalse(copy1.contains("copy2Only"))

        assertTrue(copy2.contains("copy2Only"))
        assertFalse(copy2.contains("copy1Only"))
    }

    // =========================================================================
    // Webhook Variable Interpolation Tests
    // =========================================================================

    @Test
    fun `should interpolate webhook URL`() {
        val context = ExecutionContext()
        val server = org.berrycrush.webhook.MockWebhookServer(0)
        server.expect("onPaymentReceived")
        val port = server.start()

        try {
            context.registerWebhookServer("payments", server)

            val result = context.interpolate("{{payments.onPaymentReceived}}")

            assertEquals("http://localhost:$port/webhook/onPaymentReceived", result)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `should interpolate webhook call count`() {
        val context = ExecutionContext()
        val server = org.berrycrush.webhook.MockWebhookServer(0)
        server.expect("onEvent")
        val port = server.start()

        try {
            context.registerWebhookServer("events", server)

            // Initially 0 calls
            assertEquals("0", context.interpolate("{{events.onEvent.length}}"))

            // Simulate webhook call
            val client =
                HttpClient
                    .newHttpClient()
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://localhost:$port/webhook/onEvent"))
                    .POST(
                        HttpRequest.BodyPublishers
                            .ofString("""{"id": 1}"""),
                    ).build()
            client.send(
                request,
                HttpResponse.BodyHandlers
                    .ofString(),
            )

            // Now 1 call
            assertEquals("1", context.interpolate("{{events.onEvent.length}}"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `should interpolate webhook body`() {
        val context = ExecutionContext()
        val server = org.berrycrush.webhook.MockWebhookServer(0)
        server.expect("onOrder")
        val port = server.start()

        try {
            context.registerWebhookServer("orders", server)

            // Simulate webhook call
            val client =
                HttpClient
                    .newHttpClient()
            val body = """{"orderId": 123, "amount": 99.99}"""
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://localhost:$port/webhook/onOrder"))
                    .POST(
                        HttpRequest.BodyPublishers
                            .ofString(body),
                    ).build()
            client.send(
                request,
                HttpResponse.BodyHandlers
                    .ofString(),
            )

            // Get the body
            assertEquals(body, context.interpolate("{{orders.onOrder[0]}}"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `should interpolate webhook body field`() {
        val context = ExecutionContext()
        val server = org.berrycrush.webhook.MockWebhookServer(0)
        server.expect("onOrder")
        val port = server.start()

        try {
            context.registerWebhookServer("orders", server)

            // Simulate webhook call
            val client = HttpClient.newHttpClient()
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://localhost:$port/webhook/onOrder"))
                    .POST(
                        HttpRequest.BodyPublishers
                            .ofString("""{"orderId": 123, "amount": 99.99}"""),
                    ).build()
            client.send(
                request,
                HttpResponse.BodyHandlers
                    .ofString(),
            )

            // Get specific field
            assertEquals("123", context.interpolate("{{orders.onOrder[0].body.orderId}}"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `should cleanup webhook servers`() {
        val context = ExecutionContext()
        val server = org.berrycrush.webhook.MockWebhookServer(0)
        server.expect("onTest")
        server.start()

        context.registerWebhookServer("test", server)
        assertTrue(context.webhookServerNames().contains("test"))

        context.cleanupWebhookServers()

        assertTrue(context.webhookServerNames().isEmpty())
    }
}
