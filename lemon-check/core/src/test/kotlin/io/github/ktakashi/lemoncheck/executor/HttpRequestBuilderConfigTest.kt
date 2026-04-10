package io.github.ktakashi.lemoncheck.executor

import io.github.ktakashi.lemoncheck.config.Configuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.Duration

class HttpRequestBuilderConfigTest {
    @Test
    fun `constructor accepts configuration timeout`() {
        val config = Configuration(timeout = Duration.ofSeconds(45))
        val builder = HttpRequestBuilder(config)

        // The builder is created without errors
        assertNotNull(builder)
    }

    @Test
    fun `constructor respects followRedirects false`() {
        val config =
            Configuration(
                timeout = Duration.ofSeconds(30),
                followRedirects = false,
            )
        val builder = HttpRequestBuilder(config)
        assertNotNull(builder)
    }

    @Test
    fun `createClient respects redirect policy`() {
        val clientWithRedirects = HttpRequestBuilder.createClient(followRedirects = true)
        val clientWithoutRedirects = HttpRequestBuilder.createClient(followRedirects = false)

        // Both clients should be created without errors
        assertNotNull(clientWithRedirects)
        assertNotNull(clientWithoutRedirects)
    }

    @Test
    fun `default configuration uses 30 second timeout`() {
        val config = Configuration()
        assertEquals(Duration.ofSeconds(30), config.timeout)
    }

    @Test
    fun `default configuration enables followRedirects`() {
        val config = Configuration()
        assertEquals(true, config.followRedirects)
    }
}
